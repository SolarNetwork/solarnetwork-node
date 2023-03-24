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
import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import static net.solarnetwork.node.hw.yaskawa.ecb.PacketUtils.sendPacket;
import static net.solarnetwork.util.NumberUtils.narrow;
import static net.solarnetwork.util.NumberUtils.scaled;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.Bitmaskable;
import net.solarnetwork.domain.GroupedBitmaskable;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.DcEnergyDatum;
import net.solarnetwork.node.domain.datum.AcDcEnergyDatum;
import net.solarnetwork.node.domain.datum.SimpleAcDcEnergyDatum;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.inverter.InverterModelEvent;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.io.serial.SerialConnectionAction;
import net.solarnetwork.util.NumberUtils;

/**
 * Serial port connection action to populate values onto a new
 * {@link SimpleAcDcEnergyDatum} object.
 * 
 * @author matt
 * @version 2.1
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

	private Packet sendForPacket(SerialConnection conn, PVI3800Command cmd) throws IOException {
		return sendForPacketWithLength(conn, cmd, cmd.getBodyLength());
	}

	private Packet sendForPacketWithLength(SerialConnection conn, PVI3800Command cmd, int len)
			throws IOException {
		Packet p = sendPacket(conn, cmd.request(unitId));
		if ( p == null ) {
			return null;
		}
		if ( !p.isAcceptedResponse() ) {
			log.debug("Unaccepted response to {}: {}", cmd, p.toDebugString());
			return null;
		}
		if ( p.getBodyLength() < len ) {
			log.warn("Not enough data in {} response (wanted {}): {}", cmd, len, p.toDebugString());
			return null;
		}
		if ( !p.isValid() ) {
			log.warn("Invalid checksum in {} response (wanted {}): {}", cmd,
					Integer.toString(p.getCalculatedCrc(), 16), Integer.toString(p.getCrc(), 16));
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

		Set<PvStatus> pv1Status = readBitset(conn, PVI3800Command.MeterReadPv1Status, PvStatus.class, d,
				"status_pv1");
		Set<PvStatus> pv2Status = readBitset(conn, PVI3800Command.MeterReadPv2Status, PvStatus.class, d,
				"status_pv2");
		Set<PvStatus> pv3Status = readBitset(conn, PVI3800Command.MeterReadPv3Status, PvStatus.class, d,
				"status_pv3");

		Set<PvIsoStatus> pv1IsoStatus = readBitset(conn, PVI3800Command.MeterReadPv1IsoStatus,
				PvIsoStatus.class, d, "status_pvIso1");
		Set<PvIsoStatus> pv2IsoStatus = readBitset(conn, PVI3800Command.MeterReadPv2IsoStatus,
				PvIsoStatus.class, d, "status_pvIso2");
		Set<PvIsoStatus> pv3IsoStatus = readBitset(conn, PVI3800Command.MeterReadPv3IsoStatus,
				PvIsoStatus.class, d, "status_pvIso3");

		Set<AcStatus> ac1Status = readBitset(conn, PVI3800Command.MeterReadAc1Status, AcStatus.class, d,
				"status_ac1");
		Set<AcStatus> ac2Status = readBitset(conn, PVI3800Command.MeterReadAc2Status, AcStatus.class, d,
				"status_ac2");
		Set<AcStatus> ac3Status = readBitset(conn, PVI3800Command.MeterReadAc3Status, AcStatus.class, d,
				"status_ac3");

		// SunSpec compatibility

		SortedSet<PVIStatus> faults = merge(pv1Status, pv2Status, pv3Status, pv1IsoStatus, pv2IsoStatus,
				pv3IsoStatus, ac1Status, ac2Status, ac3Status);
		if ( faults != null && !faults.isEmpty() ) {
			long bitmask = ModelEvent.bitField32Value(events(faults));
			d.putSampleValue(Status, "events", bitmask);
		}

		if ( faults != null && !faults.isEmpty() ) {
			BigInteger v = NumberUtils
					.bigIntegerForBitSet(GroupedBitmaskable.overallBitmaskValue(faults));
			if ( v != null ) {
				d.putSampleValue(Status, "vendorEvents", "0x" + v.toString(16));
			}
		}

		return d;
	}

	private Set<ModelEvent> events(Set<PVIStatus> faults) {
		if ( faults == null || faults.isEmpty() ) {
			return null;
		}
		Set<ModelEvent> events = new LinkedHashSet<>(16);

		if ( faults.contains(PvStatus.TemperatureDerating) ) {
			events.add(InverterModelEvent.OverTemperature);
		}
		if ( faults.contains(PvIsoStatus.PVPositiveGroundingFailure)
				|| faults.contains(PvIsoStatus.PVNegativeGroundingFailure) ) {
			events.add(InverterModelEvent.GroundFault);
		}
		if ( faults.contains(AcStatus.CriticalOverVoltage) || faults.contains(AcStatus.OverVoltage) ) {
			events.add(InverterModelEvent.AcOverVoltage);
		}
		if ( faults.contains(AcStatus.CriticalUnderVoltage) || faults.contains(AcStatus.UnderVoltage) ) {
			events.add(InverterModelEvent.AcUnderVoltage);
		}
		if ( faults.contains(AcStatus.HighFrequency) ) {
			events.add(InverterModelEvent.OverFrequency);
		}
		if ( faults.contains(AcStatus.LowFrequency) ) {
			events.add(InverterModelEvent.UnderFrequency);
		}
		if ( faults.contains(AcStatus.IslandingDetected)
				|| faults.contains(AcStatus.GridSynchronisationError) ) {
			events.add(InverterModelEvent.GridDisconnect);
		}
		return events;
	}

	private <T extends Enum<T> & Bitmaskable> Set<T> readBitset(SerialConnection conn,
			PVI3800Command cmd, Class<T> enumClass, SimpleAcDcEnergyDatum d, String propName)
			throws IOException {
		Packet p = sendForPacket(conn, cmd);
		if ( p != null ) {
			int n = ByteBuffer.wrap(p.getBody()).getInt();
			if ( n > 0 ) {
				d.putSampleValue(DatumSamplesType.Status, propName, Integer.toString(n));
			}
			return Bitmaskable.setForBitmask(n, enumClass);
		}
		return Collections.emptySet();
	}

	@SafeVarargs
	private final <T extends GroupedBitmaskable> SortedSet<T> merge(final Set<? extends T>... sets) {
		SortedSet<T> s = new TreeSet<>(GroupedBitmaskable.SORT_BY_OVERALL_INDEX);
		if ( sets != null ) {
			for ( Set<? extends T> set : sets ) {
				if ( set == null ) {
					continue;
				}
				s.addAll(set);
			}
		}
		return s;
	}

}
