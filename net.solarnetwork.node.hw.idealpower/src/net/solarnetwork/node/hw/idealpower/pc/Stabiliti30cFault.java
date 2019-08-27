/* ==================================================================
 * Stabiliti30cFault.java - 28/08/2019 6:35:18 am
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

package net.solarnetwork.node.hw.idealpower.pc;

import net.solarnetwork.domain.Bitmaskable;

/**
 * API for Stabiliti 30C fault encodings.
 * 
 * @author matt
 * @version 1.0
 */
public interface Stabiliti30cFault extends Bitmaskable {

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
	 * Get the overall fault number within all fault groups, starting from
	 * {@literal 0}.
	 * 
	 * @return the fault number
	 */
	default int getFaultNumber() {
		return (getFaultGroup() * 16) + bitmaskBitOffset();
	}

	/**
	 * Get a description.
	 * 
	 * @return the description
	 */
	String getDescription();

}
