/* ==================================================================
 * GeneralNodeEnergyDatum.java - Aug 26, 2014 10:32:27 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.domain;

import net.solarnetwork.util.SerializeIgnore;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * GeneralNodeDatum that also implements {@link EnergyDatum}.
 * 
 * @author matt
 * @version 1.0
 */
public class GeneralNodeEnergyDatum extends GeneralNodeDatum implements EnergyDatum {

	/**
	 * Return <em>true</em> if this datum is tagged with
	 * {@link EnergyDatum#TAG_CONSUMPTION}.
	 * 
	 * @return boolean
	 */
	public boolean isConsumption() {
		return hasTag(TAG_CONSUMPTION);
	}

	/**
	 * Tag this datum with {@link EnergyDatum#TAG_CONSUMPTION}. This will also
	 * remove {@link EnergyDatum#TAG_GENERATION}.
	 */
	public void tagAsConsumption() {
		addTag(TAG_CONSUMPTION);
		removeTag(TAG_GENERATION);
	}

	/**
	 * Return <em>true</em> if this datum is tagged with
	 * {@link EnergyDatum#TAG_GENERATION}.
	 * 
	 * @return boolean
	 */
	public boolean isGeneration() {
		return hasTag(TAG_GENERATION);
	}

	/**
	 * Tag this datum with {@link EnergyDatum#TAG_GENERATION}. This will also
	 * remove {@link EnergyDatum#TAG_CONSUMPTION}.
	 */
	public void tagAsGeneration() {
		removeTag(TAG_CONSUMPTION);
		addTag(TAG_GENERATION);
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public Long getWattHourReading() {
		return getAccumulatingSampleLong(WATT_HOUR_READING_KEY);
	}

	public void setWattHourReading(Long wattHourReading) {
		putAccumulatingSampleValue(WATT_HOUR_READING_KEY, wattHourReading);
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public Integer getWatts() {
		return getInstantaneousSampleInteger(WATTS_KEY);
	}

	public void setWatts(Integer watts) {
		putInstantaneousSampleValue(WATTS_KEY, watts);
	}

}
