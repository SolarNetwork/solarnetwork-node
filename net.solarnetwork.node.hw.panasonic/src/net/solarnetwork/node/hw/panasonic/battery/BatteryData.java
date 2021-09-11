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

import java.time.Instant;
import net.solarnetwork.node.domain.datum.EnergyStorageDatum;

/**
 * Sample data for a battery.
 * 
 * @author matt
 * @version 2.0
 */
public class BatteryData {

	private final String deviceID;
	private final Instant date;
	private final String status;
	private final Integer availableCapacity;
	private final Integer totalCapacity;

	public BatteryData(String deviceID, Instant date, String status, Integer availableCapacity,
			Integer totalCapacity) {
		super();
		this.deviceID = deviceID;
		this.date = date;
		this.status = status;
		this.availableCapacity = availableCapacity;
		this.totalCapacity = totalCapacity;
	}

	/**
	 * Populate {@link EnergyStorageDatum} properties onto a datum.
	 * 
	 * @param datum
	 *        The datum to populate with values from this instance.
	 */
	public void populateMeasurements(final EnergyStorageDatum datum) {
		if ( availableCapacity != null ) {
			datum.setAvailableEnergy(availableCapacity.longValue());
			if ( totalCapacity != null && totalCapacity.intValue() > 0 ) {
				float percent = (float) (availableCapacity.doubleValue() / totalCapacity.doubleValue());
				datum.setAvailableEnergyPercentage(percent);
			}
		}
	}

	/**
	 * Get a brief information message about the operational status of the
	 * sample, such as the overall power being used, etc.
	 * 
	 * @return A brief status message, or {@literal null} if none available.
	 */
	public String getOperationStatusMessage() {
		StringBuilder buf = new StringBuilder();
		buf.append("Device = ").append(deviceID != null ? deviceID : "N/A");
		buf.append(", Status = ").append(status != null ? status : "N/A");
		if ( availableCapacity != null && totalCapacity != null && totalCapacity.intValue() > 0 ) {
			buf.append(String.format(", Available capacity = %d Wh (%d%%)", availableCapacity, (int) Math
					.round(100.0 * (availableCapacity.doubleValue() / totalCapacity.doubleValue()))));
		}
		return buf.toString();
	}

	@Override
	public String toString() {
		return "BatteryData{date=" + date + ", deviceID=" + deviceID + ", status=" + status + ", avail="
				+ availableCapacity + ", capacity=" + totalCapacity + "}";
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
	public Instant getDate() {
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
