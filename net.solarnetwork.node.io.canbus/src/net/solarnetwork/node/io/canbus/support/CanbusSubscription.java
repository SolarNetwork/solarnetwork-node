/* ==================================================================
 * CanbusSubscription.java - 23/09/2019 10:07:45 am
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

package net.solarnetwork.node.io.canbus.support;

import static java.lang.String.format;
import static net.solarnetwork.node.io.canbus.CanbusConnection.DATA_FILTER_NONE;
import java.time.Duration;
import java.util.Objects;
import net.solarnetwork.node.io.canbus.Addressed;
import net.solarnetwork.node.io.canbus.CanbusFrameListener;

/**
 * Immutable information about a CAN bus subscription.
 *
 * <p>
 * This class implements equality based on just the {@code address} property.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class CanbusSubscription implements Comparable<CanbusSubscription> {

	private final int address;
	private final boolean forceExtendedAddress;
	private final Duration limit;
	private final long dataFilter;
	private final Iterable<Long> dataFilters;
	private final CanbusFrameListener listener;

	/**
	 * Constructor.
	 *
	 * @param address
	 *        the CAN address to subscribe to
	 * @param forceExtendedAddress
	 *        {@literal true} to force {@code address} to be treated as an
	 *        extended address, even it if would otherwise fit
	 * @param limit
	 *        an optional rate to limit update messages to
	 * @param dataFilter
	 *        a bitmask filter to limit update message to, or {@literal 0} to
	 *        receive all frames
	 * @param listener
	 *        the listener to be notified of frame changes
	 */
	public CanbusSubscription(int address, boolean forceExtendedAddress, Duration limit, long dataFilter,
			CanbusFrameListener listener) {
		super();
		this.address = address;
		this.forceExtendedAddress = forceExtendedAddress;
		this.limit = limit;
		this.dataFilter = dataFilter;
		this.dataFilters = null;
		this.listener = listener;
	}

	/**
	 * Constructor.
	 *
	 * @param address
	 *        the CAN address to subscribe to
	 * @param forceExtendedAddress
	 *        {@literal true} to force {@code address} to be treated as an
	 *        extended address, even it if would otherwise fit
	 * @param limit
	 *        an optional rate to limit update messages to
	 * @param identifierMask
	 *        an 8-byte bitmask that represents which data bits represent the
	 *        multiplex identifier
	 * @param dataFilters
	 *        a list of bitmask filters to limit update message to; must have at
	 *        least one value
	 * @param listener
	 *        the listener to be notified of frame changes
	 * @throws IllegalArgumentException
	 *         if {@code dataFilters} is {@literal null} or empty
	 */
	public CanbusSubscription(int address, boolean forceExtendedAddress, Duration limit,
			long identifierMask, Iterable<Long> dataFilters, CanbusFrameListener listener) {
		super();
		if ( dataFilters == null || !dataFilters.iterator().hasNext() ) {
			throw new IllegalArgumentException("At least one multiplex data filter must be provided.");
		}
		this.address = address;
		this.forceExtendedAddress = forceExtendedAddress;
		this.limit = limit;
		this.dataFilter = identifierMask;
		this.dataFilters = dataFilters;
		this.listener = listener;
	}

	@Override
	public int compareTo(CanbusSubscription o) {
		if ( o == null ) {
			return -1;
		}
		int otherAddress = o.getAddress();
		return (otherAddress > address ? -1 : otherAddress < address ? 1 : 0);
	}

	/**
	 * Get the number of seconds in the configured limit.
	 *
	 * @return the number of seconds in the limit
	 */
	public int getLimitSeconds() {
		return (limit != null ? (int) limit.getSeconds() : 0);
	}

	/**
	 * Get the number of microseconds in the configured limit.
	 *
	 * @return the number of microseconds in the limit
	 */
	public int getLimitMicroseconds() {
		int nanoseconds = (limit != null ? limit.getNano() : 0);
		return nanoseconds / 1000;
	}

	/**
	 * Test if a limit is configured.
	 *
	 * @return {@literal true} if a {@code limit} is configured and is not a
	 *         zero length
	 */
	public boolean hasLimit() {
		return (limit != null && !limit.isZero());
	}

	/**
	 * Test if a data filter is configured.
	 *
	 * @return {@literal true} if a data filter is configured
	 */
	public boolean hasFilter() {
		return dataFilter != DATA_FILTER_NONE;
	}

	/**
	 * Get the "force extended" flag.
	 *
	 * @return {@literal true} if extended address values should be used always
	 */
	public boolean isForceExtendedAddress() {
		return forceExtendedAddress;
	}

	/**
	 * Test if multiplex data filters are configured.
	 *
	 * <p>
	 * If this method returns {@literal true} then {@link #getDataFilter()}
	 * represents the multiplex identifier value used with the subscription.
	 * </p>
	 *
	 * @return {@literal true} if
	 */
	public boolean isMultiplexFilter() {
		return dataFilters != null;
	}

	/**
	 * Get the multiplex data filters.
	 *
	 * @return the multiplex data filters, or {@literal null} if this is not a
	 *         multiplex subscription
	 */
	public Iterable<Long> getDataFilters() {
		return dataFilters;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CanbusSubscription{");
		builder.append(format(
				address > Addressed.MAX_STANDARD_ADDRESS || forceExtendedAddress ? "0x%08X" : "0x%X",
				address));
		if ( hasLimit() ) {
			builder.append(",limit=");
			builder.append(format("%d.%06d", getLimitSeconds(), getLimitMicroseconds()));
		}
		if ( hasFilter() ) {
			builder.append(",filter=");
			builder.append(format("0x%016X", dataFilter));
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(address);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof CanbusSubscription) ) {
			return false;
		}
		CanbusSubscription other = (CanbusSubscription) obj;
		return address == other.address;
	}

	/**
	 * Get the address.
	 *
	 * @return the address
	 */
	public int getAddress() {
		return address;
	}

	/**
	 * Get the limit.
	 *
	 * @return the limit
	 */
	public Duration getLimit() {
		return limit;
	}

	/**
	 * Get the data filter.
	 *
	 * @return the filter
	 */
	public long getDataFilter() {
		return dataFilter;
	}

	/**
	 * Get the listener.
	 *
	 * @return the listener
	 */
	public CanbusFrameListener getListener() {
		return listener;
	}

}
