/* ==================================================================
 * Fault.java - 11/09/2019 2:06:28 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.satcon;

import net.solarnetwork.domain.Bitmaskable;

/**
 * API for PowerGate faults.
 * 
 * @author matt
 * @version 1.0
 */
public interface Fault extends Bitmaskable {

	/**
	 * Get the raw data code (bitmap offset within data register).
	 * 
	 * @return the code
	 */
	int getCode();

	/**
	 * Get the raw data group this fault belongs to, starting from {@literal 0}.
	 * 
	 * <p>
	 * Faults are grouped into sets of 16 faults to fit within a 16-bit data
	 * value.
	 * </p>
	 * 
	 * @return the fault group
	 */
	int getFaultGroup();

	/**
	 * Get the raw data group this fault belongs to, starting from {@literal 1}.
	 * 
	 * @return the 1-based fault group number
	 */
	default int getFaultGroupNumber() {
		return getFaultGroup() + 1;
	}

	/**
	 * Get the overall fault number within all fault groups, starting from
	 * {@literal 1}.
	 * 
	 * @return the fault number
	 */
	default int getFaultNumber() {
		return (getFaultGroup() * 16) + bitmaskBitOffset() + 1;
	}

	/**
	 * Get a description.
	 * 
	 * @return the description
	 */
	String getDescription();
}
