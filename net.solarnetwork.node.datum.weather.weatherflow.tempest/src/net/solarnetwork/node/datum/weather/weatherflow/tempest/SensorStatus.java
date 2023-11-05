/* ==================================================================
 * SensorStatus.java - 5/11/2023 7:18:10 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.weather.weatherflow.tempest;

import net.solarnetwork.domain.Bitmaskable;

/**
 * Enumeration of sensor status values.
 * 
 * @author matt
 * @version 1.0
 */
public enum SensorStatus implements Bitmaskable {

	/** Lightning failed. */
	LightningFailed(0, "Lightning failed"),

	/** Lightning noise. */
	LightningNoise(1, "Lightning noise"),

	/** Lightning disturber. */
	LightningDisturber(2, "Lightning disturber"),

	/** Pressure failed. */
	PressureFailed(3, "Pressure failed"),

	/** Temperature failed. */
	TemperatureFailed(4, "Temperature failed"),

	/** Rh failed. */
	RhFailed(5, "Rh failed"),

	/** Wind failed. */
	WindFailed(6, "Wind failed"),

	/** Precipitation failed. */
	PrecipFailed(7, "Precipitation failed"),

	/** Light/UV failed. */
	LightFailed(8, "Light/UV failed"),

	/** Power booster depleted. */
	PowerBoosterDepleted(15, "Power booster depleted"),

	/** Power booster shore power. */
	PowerBoosterShorePower(16, "Power booster shore power"),

	;

	private final int bit;
	private final String description;

	private SensorStatus(int bit, String description) {
		this.bit = bit;
		this.description = description;
	}

	@Override
	public int bitmaskBitOffset() {
		return bit;
	}

	/**
	 * Get a description of the status.
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

}
