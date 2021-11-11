/* ==================================================================
 * DcEnergyDataAccessor.java - 30/07/2018 9:20:34 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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
 * API for accessing photovoltaic energy data from an object, such as an
 * inverter.
 * 
 * @author matt
 * @version 1.0
 * @since 1.60
 */
public interface DcEnergyDataAccessor extends DataAccessor {

	/**
	 * Get the DC voltage, in volts.
	 * 
	 * @return the DC voltage
	 */
	Float getDcVoltage();

	/**
	 * Get the DC power, in W.
	 * 
	 * @return the DC power
	 */
	Integer getDcPower();

}
