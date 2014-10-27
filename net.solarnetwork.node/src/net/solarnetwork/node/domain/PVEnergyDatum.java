/* ==================================================================
 * PVEnergyDatum.java - Oct 28, 2014 6:26:29 AM
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

/**
 * Standardized API for photovoltaic system related energy datum to implement.
 * 
 * @author matt
 * @version 1.0
 */
public interface PVEnergyDatum extends EnergyDatum {

	/**
	 * The {@link net.solarnetwork.domain.GeneralNodeDatumSamples} instantaneous
	 * sample key for {@link #getDCPower()} values.
	 */
	static final String DC_POWER_KEY = "dcPower";

	/**
	 * The {@link net.solarnetwork.domain.GeneralNodeDatumSamples} instantaneous
	 * sample key for {@link #getDCVoltage()} values.
	 */
	static final String DC_VOLTAGE_KEY = "dcVoltage";

	/**
	 * The {@link net.solarnetwork.domain.GeneralNodeDatumSamples} instantaneous
	 * sample key for {@link #getVoltage()} values.
	 */
	static final String VOLTAGE_KEY = "voltage";

	/**
	 * Get the instantaneous DC power output, in watts.
	 * 
	 * @return watts, or <em>null</em> if not available
	 */
	Integer getDCPower();

	/**
	 * Get the instantaneous DC voltage output, in volts.
	 * 
	 * @return DC voltage, or <em>null</em> if not available
	 */
	Float getDCVoltage();

	/**
	 * Get the instantaneous AC voltage output, in volts.
	 * 
	 * @return AC voltage, or <em>null</em> if not available
	 */
	Float getVoltage();

}
