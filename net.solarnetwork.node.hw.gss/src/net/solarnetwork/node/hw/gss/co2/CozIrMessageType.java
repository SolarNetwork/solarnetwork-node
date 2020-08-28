/* ==================================================================
 * CozIrMessageType.java - 28/08/2020 6:29:34 AM
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

/**
 * A CozIR message type enumeration.
 * 
 * @author matt
 * @version 1.0
 */
public enum CozIrMessageType {

	/** Get the altitude compensation value. */
	AltitudeCompensationGet('s'),

	/** Set the altitude compensation value. */
	AltitudeCompensationSet('S'),

	/** Calibrate the sensor to zero at fresh-air level. */
	CO2CalibrateZeroFreshAir('G'),

	/** The zero-level calibration. */
	CO2CalibrationZeroLevel('P'),

	/** The CO2 scale factor. */
	CO2ScaleFactor('.'),

	/** The firmware version. */
	FirmwareVersion('Y'),

	/** Get measurements to report. */
	MeasurementsGet('Q', true),

	/** Set the measurements to report. */
	MeasurementsSet('M'),

	/** The operational mode. */
	OperationalMode('K'),

	/** The serial number. */
	SerialNumber('B');

	/** The "line end" byte sequence. */
	public static final byte[] LINE_END = new byte[] { '\r', '\n' };

	private final String key;
	private final boolean variableResponseKey;

	private CozIrMessageType(char key) {
		this(key, false);
	}

	private CozIrMessageType(char key, boolean variableResponseKey) {
		this.key = String.valueOf(key);
		this.variableResponseKey = variableResponseKey;
	}

	/**
	 * Get the message key value.
	 * 
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Get the key as a byte.
	 * 
	 * @return the key data
	 */
	public byte keyData() {
		return (byte) key.charAt(0);
	}

	/**
	 * Get a byte sequence that can be used as "magic" message starting bytes.
	 * 
	 * <p>
	 * This essentially returns a space character followed by the key character.
	 * </p>
	 * 
	 * @return the message start bytes
	 */
	public byte[] getMessageStart() {
		return (isVariableResponseKey() ? new byte[] { ' ' } : new byte[] { ' ', keyData() });
	}

	/**
	 * Get the "variable response key" flag.
	 * 
	 * <p>
	 * If this method returns {@literal true} then the response to a message of
	 * this type does not return the same key in the response.
	 * </p>
	 * 
	 * @return the variable response key flag
	 */
	public boolean isVariableResponseKey() {
		return variableResponseKey;
	}

}
