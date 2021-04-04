/* ==================================================================
 * ModelAccessor.java - 22/05/2018 9:54:15 AM
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

package net.solarnetwork.node.hw.sunspec;

import net.solarnetwork.util.IntRange;

/**
 * API for accessing model data.
 * 
 * @author matt
 * @version 2.0
 */
public interface ModelAccessor {

	/**
	 * Gets the time stamp of the data.
	 * 
	 * @return the data time stamp
	 */
	long getDataTimestamp();

	/**
	 * Get the base address of this model.
	 * 
	 * @return the base address
	 */
	int getBaseAddress();

	/**
	 * Get the block address of this model.
	 * 
	 * @return the block address
	 */
	int getBlockAddress();

	/**
	 * Get a Modbus register address range list for all properties described by
	 * this model.
	 * 
	 * @param maxRangeLength
	 *        the maximum number of registers per returned range
	 * @return a set of all Modbus register addresses referenced by this model
	 */
	default IntRange[] getAddressRanges(int maxRangeLength) {
		int s = getBlockAddress();
		int end = getBlockAddress() + getModelLength();
		if ( maxRangeLength == Integer.MAX_VALUE ) {
			return new IntRange[] { new IntRange(s, end - 1) };
		}
		IntRange[] result = new IntRange[(int) Math.ceil((end - s) / (double) maxRangeLength)];
		for ( int i = 0; s < end; s += maxRangeLength, i++ ) {
			int e = s + maxRangeLength;
			if ( e > end ) {
				e = end;
			}
			result[i] = new IntRange(s, e - 1);
		}
		return result;
	}

	/**
	 * Get the model ID.
	 * 
	 * @return the model ID
	 */
	ModelId getModelId();

	/**
	 * Get the number of Modbus words the model fixed block uses.
	 * 
	 * <p>
	 * This is a constant defined in the SunSpec model itself.
	 * </p>
	 * 
	 * @return the model fixed block length
	 */
	int getFixedBlockLength();

	/**
	 * Get the number of Modbus words the model fixed block + repeating blocks
	 * use.
	 * 
	 * <p>
	 * This value is returned by the device, and can be used to determine how
	 * many repeating blocks there are.
	 * </p>
	 * 
	 * @return the overall model length
	 */
	int getModelLength();

	/**
	 * Get the number of Modbus words the model repeating block uses.
	 * 
	 * <p>
	 * This is a constant defined in the SunSpec model itself, but is
	 * implemented here with a default of {@literal 0} for convenience.
	 * </p>
	 * 
	 * @return the model fixed block length; this implementation returns
	 *         {@literal 0}
	 */
	default int getRepeatingBlockInstanceLength() {
		return 0;
	}

	/**
	 * Get the number of repeating block instances.
	 * 
	 * <p>
	 * This is derived from the model length reported by the device and the
	 * specification lengths for the fixed and repeating block lengths.
	 * </p>
	 * 
	 * @return the number of repeating block instances
	 */
	default int getRepeatingBlockInstanceCount() {
		int repeatBlockLength = getRepeatingBlockInstanceLength();
		if ( repeatBlockLength < 1 ) {
			return 0;
		}
		return (getModelLength() - getFixedBlockLength()) / repeatBlockLength;
	}

}
