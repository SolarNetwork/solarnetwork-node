/* ==================================================================
 * GeneralNodePVEnergyDatum.java - Oct 28, 2014 6:34:26 AM
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
 * GeneralNodeDatum that also implements {@link PVEnergyDatum}.
 * 
 * @author matt
 * @version 1.0
 */
public class GeneralNodePVEnergyDatum extends GeneralNodeEnergyDatum implements PVEnergyDatum {

	@Override
	@JsonIgnore
	@SerializeIgnore
	public Integer getDCPower() {
		return getInstantaneousSampleInteger(DC_POWER_KEY);
	}

	public void setDCPower(Integer value) {
		putInstantaneousSampleValue(DC_POWER_KEY, value);
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public Float getDCVoltage() {
		return getInstantaneousSampleFloat(DC_VOLTAGE_KEY);
	}

	public void setDCVoltage(Float value) {
		putInstantaneousSampleValue(DC_VOLTAGE_KEY, value);
	}

	@Override
	@JsonIgnore
	@SerializeIgnore
	public Float getVoltage() {
		return getInstantaneousSampleFloat(VOLTAGE_KEY);
	}

	public void setVoltage(Float value) {
		putInstantaneousSampleValue(VOLTAGE_KEY, value);
	}

}
