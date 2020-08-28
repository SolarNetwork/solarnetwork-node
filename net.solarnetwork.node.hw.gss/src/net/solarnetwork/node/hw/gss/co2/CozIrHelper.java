/* ==================================================================
 * CozIrHelper.java - 27/08/2020 3:56:15 PM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.gss.co2;

import static java.lang.String.format;
import static net.solarnetwork.node.hw.gss.co2.CozIrMessageType.AltitudeCompensationGet;
import static net.solarnetwork.node.hw.gss.co2.CozIrMessageType.AltitudeCompensationSet;
import static net.solarnetwork.node.hw.gss.co2.CozIrMessageType.CO2CalibrationZeroLevel;
import static net.solarnetwork.node.hw.gss.co2.CozIrMessageType.CO2ScaleFactor;
import static net.solarnetwork.node.hw.gss.co2.CozIrMessageType.LINE_END;
import static net.solarnetwork.node.hw.gss.co2.CozIrMessageType.MeasurementsGet;
import static net.solarnetwork.node.hw.gss.co2.CozIrMessageType.MeasurementsSet;
import static net.solarnetwork.node.hw.gss.co2.CozIrMessageType.OperationalMode;
import static net.solarnetwork.node.hw.gss.co2.CozIrMessageType.SerialNumber;
import static net.solarnetwork.util.ByteUtils.ASCII;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.Bitmaskable;
import net.solarnetwork.node.io.serial.SerialConnection;

/**
 * Helper class for communicating with a CozIR sensor.
 * 
 * @author matt
 * @version 1.0
 */
public class CozIrHelper implements CozIrOperations {

	/** The {@code co2FreshAirLevel} property default value. */
	public static final int DEFAULT_CO2_FRESH_AIR_LEVEL = 400;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final SerialConnection conn;
	private int co2FreshAirLevel;
	private FirmwareVersion firmwareVersion;
	private String serialNumber;

	/**
	 * Constructor.
	 * 
	 * @param conn
	 *        the connection to use; the connection must already be open
	 */
	public CozIrHelper(SerialConnection conn) {
		this(conn, DEFAULT_CO2_FRESH_AIR_LEVEL);
	}

	/**
	 * Constructor.
	 * 
	 * @param conn
	 *        the connection to use; the connection must already be open
	 * @param co2FreshAirLevel
	 *        the initial fresh air level to assume the device is set to
	 */
	public CozIrHelper(SerialConnection conn, int co2FreshAirLevel) {
		super();
		if ( conn == null ) {
			throw new IllegalArgumentException("The conn argument must not be null.");
		}
		this.conn = conn;
		this.co2FreshAirLevel = co2FreshAirLevel;
	}

	@Override
	public void setMode(CozIrMode mode) throws IOException {
		sendCommand(OperationalMode, String.valueOf(mode.getCode()));
	}

	private String sendCommand(CozIrMessageType type, String cmd) throws IOException {
		if ( type == null ) {
			return null;
		}
		if ( cmd == null ) {
			cmd = "";
		}
		log.debug("Sending command to {}: {} {}", conn.getPortName(), type.getKey(), cmd);
		byte[] cmdBytes = cmd.getBytes(ASCII);
		byte[] msg = new byte[(cmd.isEmpty() ? 1 : 2) + cmdBytes.length + LINE_END.length];
		msg[0] = type.keyData();
		int start = 1;
		if ( !cmd.isEmpty() ) {
			msg[1] = ' ';
			start = 2;
		}
		System.arraycopy(cmdBytes, 0, msg, start, cmdBytes.length);
		System.arraycopy(LINE_END, 0, msg, start + cmdBytes.length, LINE_END.length);
		conn.writeMessage(msg);

		// read response
		byte[] response = conn.readMarkedMessage(type.getMessageStart(), LINE_END);
		String responseMsg = (response != null ? new String(response, ASCII) : null);
		if ( responseMsg != null ) {
			log.debug("Command response from {}: {}", conn.getPortName(), responseMsg.trim());
		}
		return responseMsg;
	}

	@Override
	public FirmwareVersion getFirmwareVersion() throws IOException {
		if ( firmwareVersion == null ) {
			readDeviceInfo();
		}
		return firmwareVersion;
	}

	@Override
	public String getSerialNumber() throws IOException {
		if ( serialNumber == null ) {
			readDeviceInfo();
		}
		return serialNumber;
	}

	private synchronized void readDeviceInfo() throws IOException {
		setMode(CozIrMode.Command);
		String versionMsg = sendCommand(CozIrMessageType.FirmwareVersion, null);
		if ( versionMsg != null ) {
			this.firmwareVersion = FirmwareVersion.parseMessage(versionMsg);
			byte[] serData = conn.readMarkedMessage(SerialNumber.getMessageStart(), LINE_END);
			if ( serData != null ) {
				this.serialNumber = CozIrUtils.parseSerialNumberMessage(new String(serData, ASCII));
			}
		}
		setMode(CozIrMode.Polling);
	}

	@Override
	public int getCo2FreshAirLevel() {
		return co2FreshAirLevel;
	}

	@Override
	public void setCo2FreshAirLevel(int value) throws IOException {
		final String msgTemplate = "%d %d";
		// send MSB, then LSB
		sendCommand(CO2CalibrationZeroLevel, format(msgTemplate, 10, ((value >> 8) & 0xFF)));
		sendCommand(CO2CalibrationZeroLevel, format(msgTemplate, 11, value & 0xFF));
		this.co2FreshAirLevel = value;
	}

	@Override
	public void calibrateAsCo2FreshAirLevel() throws IOException {
		sendCommand(CozIrMessageType.CO2CalibrateZeroFreshAir, null);
	}

	@Override
	public int getAltitudeCompensation() throws IOException {
		String response = sendCommand(AltitudeCompensationGet, null);
		if ( response != null ) {
			Map<String, Integer> d = CozIrUtils.parseKeyValueIntegerLine(response, 10);
			if ( d.containsKey(AltitudeCompensationGet.getKey()) ) {
				return d.get(AltitudeCompensationGet.getKey());
			}
		}
		return -1;
	}

	@Override
	public void setAltitudeCompensation(int value) throws IOException {
		sendCommand(AltitudeCompensationSet, String.valueOf(value));
	}

	@Override
	public void setMeasurementOutput(Set<MeasurementType> types) throws IOException {
		int mask = Bitmaskable.bitmaskValue(types);
		sendCommand(MeasurementsSet, String.valueOf(mask));
	}

	@Override
	public CozIrData getMeasurements() throws IOException {
		// first get scale factor for CO2
		String response = sendCommand(CO2ScaleFactor, null);
		if ( response == null || response.isEmpty() ) {
			return null;
		}
		Map<String, Integer> d = CozIrUtils.parseKeyValueIntegerLine(response, 10);
		if ( !d.containsKey(CO2ScaleFactor.getKey()) ) {
			return null;
		}
		final int scaleFactor = d.get(CO2ScaleFactor.getKey());

		response = sendCommand(MeasurementsGet, null);
		d = CozIrUtils.parseKeyValueIntegerLine(response, 10);
		if ( d == null || d.isEmpty() ) {
			return null;
		}
		Integer co2 = null;
		Integer co2Unfiltered = null;
		Integer humidity = null;
		Integer temperature = null;
		for ( Map.Entry<String, Integer> me : d.entrySet() ) {
			MeasurementType type = MeasurementType.forKey(me.getKey());
			if ( type != null ) {
				Integer rawValue = me.getValue();
				switch (type) {
					case Co2Filtered:
						co2 = rawValue;
						break;

					case Co2Unfiltered:
						co2Unfiltered = rawValue;
						break;

					case Humidity:
						humidity = rawValue;
						break;

					case Temperature:
						temperature = rawValue;
						break;

					default:
						// nothing to do
				}
			}
		}
		return CozIrData.forRawValue(co2, co2Unfiltered, scaleFactor, humidity, temperature);
	}

}
