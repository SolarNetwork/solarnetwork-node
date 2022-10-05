/* ==================================================================
 * BasicDatumPopulatorAction.java - 18/05/2018 5:33:37 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.node.hw.yaskawa.ecb;

import static java.lang.String.format;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.node.hw.yaskawa.ecb.PacketUtils.sendPacket;
import static net.solarnetwork.util.NumberUtils.narrow;
import static net.solarnetwork.util.NumberUtils.scaled;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DcEnergyDatum;
import net.solarnetwork.node.domain.datum.AcDcEnergyDatum;
import net.solarnetwork.node.domain.datum.SimpleAcDcEnergyDatum;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.io.serial.SerialConnectionAction;

/**
 * Serial port connection action to populate values onto a new
 * {@link SimpleAcDcEnergyDatum} object.
 * 
 * @author matt
 * @version 2.0
 */
public class BasicDatumPopulatorAction implements SerialConnectionAction<AcDcEnergyDatum> {

	private static final Logger log = LoggerFactory.getLogger(BasicDatumPopulatorAction.class);

	private final int unitId;
	private final String sourceId;

	/**
	 * Constructor.
	 * 
	 * @param unitId
	 *        the unit ID
	 * @param sourceId
	 *        the source ID
	 */
	public BasicDatumPopulatorAction(int unitId, String sourceId) {
		super();
		this.unitId = unitId;
		this.sourceId = sourceId;
	}

	private Packet sendForPacketWithLength(SerialConnection conn, PVI3800Command cmd, int len)
			throws IOException {
		Packet p = sendPacket(conn, cmd.request(unitId));
		if ( p == null ) {
			return null;
		}
		if ( !p.isAcceptedResponse() ) {
			log.warn("Unaccepted response to {}: {}", cmd, p.toDebugString());
			return null;
		}
		if ( p.getBodyLength() < len ) {
			log.warn("Not enough data in {} response (wanted {}): {}", cmd, len, p.toDebugString());
			return null;
		}
		log.debug("Got {} response: {}", cmd, p.toDebugString());
		return p;
	}

	@Override
	public AcDcEnergyDatum doWithConnection(SerialConnection conn) throws IOException {
		SimpleAcDcEnergyDatum d = new SimpleAcDcEnergyDatum(sourceId, Instant.now(), new DatumSamples());

		Packet p = sendForPacketWithLength(conn, PVI3800Command.MeterReadAcCombinedActivePower, 2);
		if ( p != null ) {
			d.setWatts(new BigInteger(1, p.getBody()).intValue());
		}

		p = sendForPacketWithLength(conn, PVI3800Command.MeterReadLifetimeTotalEnergy, 8);
		if ( p != null ) {
			d.setWattHourReading(new BigInteger(1, p.getBody()).longValue());
		}

		p = sendForPacketWithLength(conn, PVI3800Command.MeterReadAcCombinedFrequency, 2);
		if ( p != null ) {
			d.setFrequency(scaled(new BigInteger(1, p.getBody()), -2).floatValue());
		}

		p = sendForPacketWithLength(conn, PVI3800Command.MeterReadAcCombinedCurrent, 2);
		if ( p != null ) {
			d.setCurrent(scaled(new BigInteger(1, p.getBody()), -1).floatValue());
		}

		p = sendForPacketWithLength(conn, PVI3800Command.MeterReadAcCombinedVoltage, 2);
		if ( p != null ) {
			d.setVoltage(new BigInteger(1, p.getBody()).floatValue());
		}

		p = sendForPacketWithLength(conn, PVI3800Command.MeterReadAcCombinedPowerFactor, 2);
		if ( p != null ) {
			d.setPowerFactor(scaled(ByteBuffer.wrap(p.getBody()).getShort(), -3).floatValue());
		}

		p = sendForPacketWithLength(conn, PVI3800Command.MeterReadAcCombinedReactivePower, 2);
		if ( p != null ) {
			d.setReactivePower((int) ByteBuffer.wrap(p.getBody()).getShort());
		}

		p = sendForPacketWithLength(conn, PVI3800Command.MeterReadTemperatureAmbient, 2);
		if ( p != null ) {
			d.putSampleValue(Instantaneous, "temp", ByteBuffer.wrap(p.getBody()).getShort());
		}

		p = sendForPacketWithLength(conn, PVI3800Command.MeterReadTemperatureHeatsink, 2);
		if ( p != null ) {
			d.putSampleValue(Instantaneous, "temp_heatSink", ByteBuffer.wrap(p.getBody()).getShort());
		}

		int totalVoltage = 0;
		int totalVoltageCount = 0;

		float totalCurrent = 0f;
		int totalCurrentCount = 0;

		int totalPower = 0;
		int totalPowerCount = 0;

		p = sendForPacketWithLength(conn, PVI3800Command.MeterReadPv1Voltage, 2);
		if ( p != null ) {
			short n = ByteBuffer.wrap(p.getBody()).getShort();
			totalVoltage += n;
			totalVoltageCount++;
			d.putSampleValue(Instantaneous, format("%s_%d", DcEnergyDatum.DC_VOLTAGE_KEY, 1), n);
		}

		p = sendForPacketWithLength(conn, PVI3800Command.MeterReadPv1Current, 2);
		if ( p != null ) {
			Number n = narrow(scaled(ByteBuffer.wrap(p.getBody()).getShort(), -1), 2);
			totalCurrent += n.floatValue();
			totalCurrentCount++;
			d.putSampleValue(Instantaneous, format("%s_%d", DcEnergyDatum.DC_CURRENT_KEY, 1), n);
		}

		p = sendForPacketWithLength(conn, PVI3800Command.MeterReadPv1Power, 2);
		if ( p != null ) {
			short n = ByteBuffer.wrap(p.getBody()).getShort();
			totalPower += n;
			totalPowerCount++;
			d.putSampleValue(Instantaneous, format("%s_%d", DcEnergyDatum.DC_POWER_KEY, 1), n);
		}

		p = sendForPacketWithLength(conn, PVI3800Command.MeterReadPv2Voltage, 2);
		if ( p != null ) {
			short n = ByteBuffer.wrap(p.getBody()).getShort();
			totalVoltage += n;
			totalVoltageCount++;
			d.putSampleValue(Instantaneous, format("%s_%d", DcEnergyDatum.DC_VOLTAGE_KEY, 2), n);
		}

		p = sendForPacketWithLength(conn, PVI3800Command.MeterReadPv2Current, 2);
		if ( p != null ) {
			Number n = narrow(scaled(ByteBuffer.wrap(p.getBody()).getShort(), -1), 2);
			totalCurrent += n.floatValue();
			totalCurrentCount++;
			d.putSampleValue(Instantaneous, format("%s_%d", DcEnergyDatum.DC_CURRENT_KEY, 2), n);
		}

		p = sendForPacketWithLength(conn, PVI3800Command.MeterReadPv2Power, 2);
		if ( p != null ) {
			short n = ByteBuffer.wrap(p.getBody()).getShort();
			totalPower += n;
			totalPowerCount++;
			d.putSampleValue(Instantaneous, format("%s_%d", DcEnergyDatum.DC_POWER_KEY, 2), n);
		}

		p = sendForPacketWithLength(conn, PVI3800Command.MeterReadPv3Voltage, 2);
		if ( p != null ) {
			short n = ByteBuffer.wrap(p.getBody()).getShort();
			totalVoltage += n;
			totalVoltageCount++;
			d.putSampleValue(Instantaneous, format("%s_%d", DcEnergyDatum.DC_VOLTAGE_KEY, 3), n);
		}

		p = sendForPacketWithLength(conn, PVI3800Command.MeterReadPv3Current, 2);
		if ( p != null ) {
			Number n = narrow(scaled(ByteBuffer.wrap(p.getBody()).getShort(), -1), 2);
			totalCurrent += n.floatValue();
			totalCurrentCount++;
			d.putSampleValue(Instantaneous, format("%s_%d", DcEnergyDatum.DC_CURRENT_KEY, 3), n);
		}

		p = sendForPacketWithLength(conn, PVI3800Command.MeterReadPv3Power, 2);
		if ( p != null ) {
			short n = ByteBuffer.wrap(p.getBody()).getShort();
			totalPower += n;
			totalPowerCount++;
			d.putSampleValue(Instantaneous, format("%s_%d", DcEnergyDatum.DC_POWER_KEY, 3), n);
		}

		if ( totalVoltageCount > 0 ) {
			float n = ((float) totalVoltage / (float) totalVoltageCount);
			d.setDcVoltage(n);
		}
		if ( totalCurrentCount > 0 ) {
			d.setDcCurrent(totalCurrent);
		}
		if ( totalPowerCount > 0 ) {
			d.setDcPower(totalPower);
		}

		return d;
	}

}
