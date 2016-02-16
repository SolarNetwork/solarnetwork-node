/* ==================================================================
 * BatteryData.java - 16/02/2016 7:13:42 am
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.panasonic.battery;

import org.joda.time.DateTime;

/**
 * Sample data for a battery.
 * 
 * @author matt
 * @version 1.0
 */
public class BatteryData {

	private final String deviceID;
	private final DateTime date;
	private final String status;
	private final Integer availableCapacity;
	private final Integer totalCapacity;

	public BatteryData(String deviceID, DateTime date, String status, Integer availableCapacity,
			Integer totalCapacity) {
		super();
		this.deviceID = deviceID;
		this.date = date;
		this.status = status;
		this.availableCapacity = availableCapacity;
		this.totalCapacity = totalCapacity;
	}

	/**
	 * Get the unique device ID of the battery.
	 * 
	 * @return The device ID.
	 */
	public String getDeviceID() {
		return deviceID;
	}

	/**
	 * Get the date this sample data was collected.
	 * 
	 * @return The date of the sample data.
	 */
	public DateTime getDate() {
		return date;
	}

	/**
	 * Get a status message.
	 * 
	 * @return The status message.
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Get the available capacity of the battery, in watt hours (Wh).
	 * 
	 * @return The available capacity.
	 */
	public Integer getAvailableCapacity() {
		return availableCapacity;
	}

	/**
	 * Get the total capacity of the battery, in watt hours (Wh).
	 * 
	 * @return The total capacity.
	 */
	public Integer getTotalCapacity() {
		return totalCapacity;
	}

}
