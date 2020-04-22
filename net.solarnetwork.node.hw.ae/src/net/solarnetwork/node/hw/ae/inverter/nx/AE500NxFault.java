/* ==================================================================
 * AE500NxFault.java - 22/04/2020 11:51:31 am
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

package net.solarnetwork.node.hw.ae.inverter.nx;

import net.solarnetwork.domain.GroupedBitmaskable;

/**
 * API for AE500Nx faults.
 * 
 * @author matt
 * @version 1.0
 * @since 2.1
 */
public interface AE500NxFault extends GroupedBitmaskable {

	/**
	 * Get the raw data group this fault belongs to, starting from {@literal 0}.
	 * 
	 * <p>
	 * Faults are grouped into sets of 32 faults to fit within a 32-bit data
	 * value.
	 * </p>
	 * 
	 * @return the fault group
	 */
	int getFaultGroup();

	/**
	 * Get the overall fault number within all fault groups, starting from
	 * {@literal 0}.
	 * 
	 * @return the fault number
	 */
	default int getFaultNumber() {
		return (getFaultGroup() * getGroupSize()) + bitmaskBitOffset();
	}

	@Override
	default int getGroupIndex() {
		return getFaultGroup();
	}

	@Override
	default int getGroupSize() {
		return 32;
	}

	/**
	 * Get a description.
	 * 
	 * @return the description
	 */
	String getDescription();

}
