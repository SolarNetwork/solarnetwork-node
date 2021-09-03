/* ==================================================================
 * DcEnergyDatum.java - Oct 28, 2014 6:26:29 AM
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

import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;

/**
 * Standardized API for direct current related energy datum to implement.
 * 
 * @author matt
 * @version 1.0
 */
public interface DcEnergyDatum extends EnergyDatum, net.solarnetwork.domain.datum.DcEnergyDatum {

	/**
	 * Set the instantaneous DC power output, in watts.
	 * 
	 * @param value
	 *        watts, or {@literal null} if not available
	 */
	default void setDcPower(Integer value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, DC_POWER_KEY, value);
	}

	/**
	 * Get the instantaneous DC voltage output, in volts.
	 * 
	 * @param value
	 *        DC voltage, or{@literal null} if not available
	 */
	default void setDcVoltage(Float value) {
		asMutableSampleOperations().putSampleValue(Instantaneous, DC_VOLTAGE_KEY, value);
	}

}
