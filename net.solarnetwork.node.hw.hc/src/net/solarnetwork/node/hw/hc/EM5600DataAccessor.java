/* ==================================================================
 * EM5600DataAccessor.java - 23/01/2020 10:09:35 am
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

package net.solarnetwork.node.hw.hc;

import java.math.BigDecimal;
import org.joda.time.LocalDateTime;
import net.solarnetwork.node.domain.ACEnergyDataAccessor;

/**
 * API for reading EM5600 series power meter data.
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public interface EM5600DataAccessor extends ACEnergyDataAccessor {

	/**
	 * Get the device serial number.
	 * 
	 * @return the serial number
	 */
	String getSerialNumber();

	/**
	 * Get the device firmware revision.
	 * 
	 * @return the firmware revision, as {@literal X.Y.Z}.
	 */
	String getHardwareRevision();

	/**
	 * Get the model.
	 * 
	 * @return the model
	 */
	Integer getModel();

	/**
	 * Get the manufacture date.
	 * 
	 * @return the manufacture date
	 */
	LocalDateTime getManufactureDate();

	/**
	 * Get the energy unit.
	 * 
	 * @return the energy unit
	 */
	EnergyUnit getEnergyUnit();

	/**
	 * Get the unit factor.
	 * 
	 * @return the unit factor
	 */
	UnitFactor getUnitFactor();

	/**
	 * Get the configured CT ratio.
	 * 
	 * @return the CT ratio
	 */
	BigDecimal getCtRatio();

	/**
	 * Get the configured PT ratio.
	 * 
	 * @return the PT ratio
	 */
	BigDecimal getPtRatio();

}
