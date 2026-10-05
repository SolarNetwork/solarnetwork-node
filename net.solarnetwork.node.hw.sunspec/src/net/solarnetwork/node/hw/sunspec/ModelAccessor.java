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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.util.IntRange;

/**
 * API for accessing model data.
 *
 * @author matt
 * @version 2.1
 */
public interface ModelAccessor {

	/**
	 * Gets the time stamp of the data.
	 *
	 * @return the data time stamp, or {@code null} if not known
	 */
	@Nullable
	Instant getDataTimestamp();

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
	 * <p>
	 * This implementation calls {@link #getAddressRange(int, int)} from the
	 * block address to the end of the model. The ranges are based on the model
	 * data available when called, so when the layout of a model depends on data
	 * that has not been read yet, read each range before getting the next one
	 * with {@link #getAddressRange(int, int)} instead.
	 * </p>
	 *
	 * @param maxRangeLength
	 *        the maximum number of registers per returned range
	 * @return a set of all Modbus register addresses referenced by this model
	 * @throws IllegalArgumentException
	 *         if {@code maxRangeLength} is less than {@literal 1}
	 */
	default IntRange[] getAddressRanges(int maxRangeLength) {
		final List<IntRange> result = new ArrayList<>(4);
		final int end = getBlockAddress() + getModelLength();
		for ( int address = getBlockAddress(); address < end; ) {
			final IntRange range = getAddressRange(address, maxRangeLength);
			result.add(range);
			address = range.getMax() + 1;
		}
		return result.toArray(IntRange[]::new);
	}

	/**
	 * Get the next Modbus register address range to read for this model.
	 *
	 * <p>
	 * The range starts at {@code address} and ends at the end of the model, or
	 * after at most {@code maxRangeLength} registers. It does not end inside
	 * one of the {@link #getUnsplittableAddressRanges()} ranges, unless that
	 * range starts at or before {@code address} and the range would otherwise
	 * be longer than {@code maxRangeLength}.
	 * </p>
	 *
	 * <p>
	 * The range is based on the model data available when called. Some model
	 * layouts depend on data in the model itself, such as the number of points
	 * in a curve, so read each range before getting the next one.
	 * </p>
	 *
	 * @param address
	 *        the address of the first register of the range, from the block
	 *        address to the last register of the model
	 * @param maxRangeLength
	 *        the maximum number of registers in the range
	 * @return the range
	 * @throws IllegalArgumentException
	 *         if {@code maxRangeLength} is less than {@literal 1}, or
	 *         {@code address} is outside the model
	 * @since 2.1
	 */
	default IntRange getAddressRange(int address, int maxRangeLength) {
		if ( maxRangeLength < 1 ) {
			throw new IllegalArgumentException(
					String.format("The maximum range length %d is less than 1.", maxRangeLength));
		}
		final int end = getBlockAddress() + getModelLength();
		if ( address < getBlockAddress() || address >= end ) {
			throw new IllegalArgumentException(
					String.format("The address %d is outside the model registers %d - %d.", address,
							getBlockAddress(), end - 1));
		}
		if ( end - address <= maxRangeLength ) {
			return new IntRange(address, end - 1);
		}
		int rangeEnd = address + maxRangeLength;
		for ( IntRange r : mergedRanges(getUnsplittableAddressRanges()) ) {
			if ( r.getMin() < rangeEnd && rangeEnd <= r.getMax() ) {
				// the range would end inside r, so end before r if possible
				if ( r.getMin() > address ) {
					rangeEnd = r.getMin();
				}
				break;
			}
		}
		return new IntRange(address, rangeEnd - 1);
	}

	/**
	 * Get the register address ranges of this model that must not be split
	 * across separate read requests.
	 *
	 * <p>
	 * Each range covers the registers of a point whose value spans more than
	 * one register, such as a 32-bit number or a string, as some devices reject
	 * read requests that start or end inside such a value. Where part of the
	 * layout of a model depends on data that has not been read yet, such as the
	 * number of points in a curve, a range can instead cover that whole part of
	 * the model, so the data it depends on is read first.
	 * </p>
	 *
	 * @return the ranges, as absolute register addresses, in any order and
	 *         possibly overlapping, never {@code null}; this implementation
	 *         returns an empty list
	 * @since 2.1
	 */
	default List<IntRange> getUnsplittableAddressRanges() {
		return Collections.emptyList();
	}

	/**
	 * Merge overlapping ranges.
	 *
	 * <p>
	 * Ranges that are only adjacent are not merged, as the boundary between
	 * them is a valid place to split.
	 * </p>
	 *
	 * @param ranges
	 *        the ranges to merge
	 * @return the merged ranges, in ascending order
	 */
	private static List<IntRange> mergedRanges(List<IntRange> ranges) {
		if ( ranges.size() < 2 ) {
			return ranges;
		}
		final List<IntRange> sorted = new ArrayList<>(ranges);
		Collections.sort(sorted);
		final List<IntRange> result = new ArrayList<>(sorted.size());
		IntRange curr = sorted.get(0);
		for ( int i = 1, len = sorted.size(); i < len; i++ ) {
			final IntRange r = sorted.get(i);
			if ( curr.intersects(r) ) {
				curr = curr.mergeWith(r);
			} else {
				result.add(curr);
				curr = r;
			}
		}
		result.add(curr);
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
