/* ==================================================================
 * GeneralNodeEnergyStorageDatum.java - 16/02/2016 7:51:54 pm
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

package net.solarnetwork.node.domain;

import net.solarnetwork.util.SerializeIgnore;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * GeneralNodeDatum that also implements {@link EnergyStorageDatum}.
 * 
 * @author matt
 * @version 1.0
 */
public class GeneralNodeEnergyStorageDatum extends GeneralNodeDatum implements EnergyStorageDatum {

	@Override
	@JsonIgnore
	@SerializeIgnore
	public Float getAvailableEnergyPercentage() {
		return getInstantaneousSampleFloat(PERCENTAGE_KEY);
	}

	public void setAvailableEnergyPercentage(Float value) {
		putInstantaneousSampleValue(PERCENTAGE_KEY, value);
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public Long getAvailableEnergy() {
		return getInstantaneousSampleLong(AVAILABLE_WATT_HOURS_KEY);
	}

	public void setAvailableEnergy(Long value) {
		putInstantaneousSampleValue(AVAILABLE_WATT_HOURS_KEY, value);
	}

}
