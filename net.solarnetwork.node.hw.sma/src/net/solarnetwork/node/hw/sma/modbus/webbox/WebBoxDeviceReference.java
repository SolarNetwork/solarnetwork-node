/* ==================================================================
 * WebBoxDeviceReference.java - 11/09/2020 1:38:38 PM
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

package net.solarnetwork.node.hw.sma.modbus.webbox;

/**
 * A WebBox device reference.
 * 
 * <p>
 * These are read from the WebBox itself, starting from
 * {@link WebBoxRegister#DEVICE_UNIT_IDS_STARTING_ADDRESS}.
 * 
 * @author matt
 * @version 1.0
 */
public class WebBoxDeviceReference {

	private final int deviceId;
	private final int unitId;
	private final long serialNumber;

	/**
	 * Constructor.
	 * 
	 * @param deviceId
	 *        the device ID
	 * @param unitId
	 *        the unit ID
	 * @param serialNumber
	 *        the serial number
	 */
	public WebBoxDeviceReference(int deviceId, int unitId, long serialNumber) {
		super();
		this.deviceId = deviceId;
		this.unitId = unitId;
		this.serialNumber = serialNumber;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("WebBoxDevice{deviceId=");
		builder.append(deviceId);
		builder.append(", unitId=");
		builder.append(unitId);
		builder.append(", serialNumber=");
		builder.append(serialNumber);
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the device ID.
	 * 
	 * @return the device ID
	 */
	public int getDeviceId() {
		return deviceId;
	}

	/**
	 * Get the device unit ID.
	 * 
	 * @return the device unit ID
	 */
	public int getUnitId() {
		return unitId;
	}

	/**
	 * Get the device serial number .
	 * 
	 * @return the device serial number
	 */
	public long getSerialNumber() {
		return serialNumber;
	}

}
