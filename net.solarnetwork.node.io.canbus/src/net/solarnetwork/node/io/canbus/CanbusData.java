/* ==================================================================
 * CanbusData.java - 9/10/2019 9:51:50 am
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

package net.solarnetwork.node.io.canbus;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import net.solarnetwork.domain.BitDataType;
import net.solarnetwork.domain.ByteOrdering;
import net.solarnetwork.node.domain.DataAccessor;
import net.solarnetwork.util.ByteUtils;

/**
 * Object to hold raw data extracted from a CAN bus device.
 * 
 * <p>
 * This class is designed to operate as a cache of data read from a CAN bus
 * device. The data is modeled as a sparse array of message address keys with
 * associated 16-bit values. It supports thread-safe write access to the saved
 * data and thread-safe read access if {@link #CanbusData(CanbusData)} or
 * {@link #copy()} are invoked to get a copy of the data.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class CanbusData implements DataAccessor {

	private final Map<Integer, CanbusFrame> dataFrames;
	private long dataTimestamp;

	/**
	 * Default constructor.
	 */
	public CanbusData() {
		super();
		this.dataFrames = new HashMap<>(64);
		this.dataTimestamp = 0;
	}

	/**
	 * Copy constructor.
	 * 
	 * <p>
	 * This method provides a thread-safe way to get a copy of the current data.
	 * </p>
	 * 
	 * @param other
	 *        the object to copy
	 */
	public CanbusData(CanbusData other) {
		synchronized ( other.dataFrames ) {
			this.dataFrames = new HashMap<>(other.dataFrames);
			this.dataTimestamp = other.dataTimestamp;
		}
	}

	@Override
	public long getDataTimestamp() {
		return dataTimestamp;
	}

	@Override
	public Map<String, Object> getDeviceInfo() {
		return Collections.emptyMap();
	}

	/**
	 * Create a copy of this object.
	 * 
	 * <p>
	 * This method provides a thread-safe way to get a copy of the current data.
	 * </p>
	 * 
	 * @return the new instance
	 * @see #CanbusData(CanbusData)
	 */
	public CanbusData copy() {
		return new CanbusData(this);
	}

	/**
	 * Force the data timestamp to be expired.
	 * 
	 * <p>
	 * Calling this method will reset the {@code dataTimestamp} to zero,
	 * effectively expiring the data.
	 * </p>
	 * 
	 * @return this object to allow method chaining
	 */
	public final CanbusData expire() {
		synchronized ( dataFrames ) {
			dataTimestamp = 0;
		}
		return this;
	}

	/**
	 * Perform a set of updates to saved register data.
	 * 
	 * @param action
	 *        the callback to perform the updates on
	 * @return this object to allow method chaining
	 */
	public final CanbusData performUpdates(CanbusDataUpdateAction action) {
		synchronized ( dataFrames ) {
			final long now = System.currentTimeMillis();
			if ( action.updateCanbusData(new MutableCanbusDataView()) ) {
				dataTimestamp = now;
			}
		}
		return this;
	}

	/**
	 * API for performing updates to the data.
	 */
	public static interface MutableCanbusData {

		/**
		 * Store raw frame data values, using {@link CanbusFrame#getAddress()}
		 * for the data keys.
		 * 
		 * @param data
		 *        the data to save
		 */
		public void saveData(final Iterable<CanbusFrame> data);

		/**
		 * Store a mapping of addresses to associated frame data values.
		 * 
		 * @param data
		 *        the data map entries to save
		 */
		public void saveDataMap(final Iterable<Entry<Integer, CanbusFrame>> data);

	}

	/**
	 * API for performing updates to the saved data.
	 */
	public static interface CanbusDataUpdateAction {

		/**
		 * Perform updates to the data.
		 * 
		 * @param m
		 *        a mutable version of the data to update
		 * @return {@literal true} if {@code dataTimestamp} should be updated to
		 *         the current time
		 */
		public boolean updateCanbusData(MutableCanbusData m);
	}

	/**
	 * Internal mutable view of this class, meant to be used for thread-safe
	 * writes.
	 * 
	 * <p>
	 * All methods are assumed to be synchronized on {@code dataRegsiters}.
	 * </p>
	 */
	private class MutableCanbusDataView implements MutableCanbusData {

		@Override
		public void saveData(Iterable<CanbusFrame> data) {
			for ( CanbusFrame f : data ) {
				dataFrames.put(f.getAddress(), f);
			}

		}

		@Override
		public void saveDataMap(Iterable<Entry<Integer, CanbusFrame>> data) {
			for ( Entry<Integer, CanbusFrame> me : data ) {
				dataFrames.put(me.getKey(), me.getValue());
			}
		}

	}

	/**
	 * Get a string of data values, useful for debugging.
	 * 
	 * <p>
	 * The generated string will contain a register address followed by two
	 * register values per line, printed as hexidecimal integers, with a prefix
	 * and suffix line. For example:
	 * </p>
	 * 
	 * <pre>
	 * CanbusData{
	 *      0x30000: 4141727E
	 *      0x30006: FFC00000
	 *      ...
	 *      0x30344: 00000000
	 * }
	 * </pre>
	 * 
	 * @return debug string
	 */
	public final String dataDebugString() {
		final StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append("{");
		synchronized ( dataFrames ) {
			Set<Integer> keySet = dataFrames.keySet();
			Integer[] keys = keySet.toArray(new Integer[keySet.size()]);
			if ( keys.length > 0 ) {
				Arrays.sort(keys);
				for ( Integer k : keys ) {
					buf.append("\n\t").append(String.format("0x%08X", k)).append(": ");
					byte[] data = dataFrames.get(k).getData();
					int len = (data != null ? data.length : 0);
					if ( len > 0 ) {
						for ( byte b : data ) {
							buf.append(String.format("%02X", Byte.toUnsignedInt(b)));
						}
					}
				}
				buf.append("\n");
			}
		}
		buf.append("}");
		return buf.toString();
	}

	/**
	 * Get a number value from a reference.
	 * 
	 * @param ref
	 *        the reference to get the number value for
	 * @return the value, or {@literal null} if {@code ref} is {@literal null}
	 * @throws IllegalArgumentException
	 *         if the reference data type is not numeric
	 */
	public final Number getNumber(CanbusSignalReference ref) {
		if ( ref == null ) {
			return null;
		}
		BitDataType type = ref.getDataType();
		if ( type == null ) {
			type = BitDataType.UInt32;
		}
		final int addr = ref.getAddress();
		final int bitOffset = ref.getBitOffset();
		final int bitLength = ref.getBitLength();
		final ByteOrdering ordering = ref.getByteOrdering();
		final CanbusFrame message = dataFrames.get(addr);
		final byte[] data = (message != null ? message.getData() : null);
		if ( data == null ) {
			return null;
		}
		final int bitOffsetRemain = bitOffset % 8;
		final int bitLengthRemain = bitLength % 8;
		if ( bitOffsetRemain != 0 || bitLengthRemain != 0 ) {
			long mask = 0L;
			for ( int i = 0; i < bitLength; i++ ) {
				mask |= (1 << i);
			}
			BigInteger bi = ByteUtils.parseUnsignedInteger(data, 0, data.length, ordering)
					.shiftRight(bitOffset).and(BigInteger.valueOf(mask));
			// try to return the smallest possible number type
			try {
				return bi.shortValueExact();
			} catch ( ArithmeticException e ) {
				try {
					return bi.intValueExact();
				} catch ( ArithmeticException e2 ) {
					try {
						return bi.longValueExact();
					} catch ( ArithmeticException e3 ) {
						return bi;
					}
				}
			}
		}
		final int offset = bitOffset / 8;
		final int length = bitLength / 8;
		return ByteUtils.parseNumber(type, data, offset, length, ordering);
	}

	/**
	 * Get a byte array value from a reference.
	 * 
	 * @param ref
	 *        the reference to get the byte value for
	 * @return the value, or {@literal null} if {@code ref} is {@literal null}
	 */
	public final byte[] getBytes(CanbusSignalReference ref) {
		if ( ref == null ) {
			return null;
		}
		final int addr = ref.getAddress();
		final int bitOffset = ref.getBitOffset();
		final int bitLength = ref.getBitLength();
		final ByteOrdering ordering = ref.getByteOrdering();
		final CanbusFrame message = dataFrames.get(addr);
		final byte[] data = (message != null ? message.getData() : null);
		if ( data == null ) {
			return null;
		}
		final int offset = bitOffset / 8;
		final int length = bitLength / 8;
		return ByteUtils.parseBytes(data, offset, length, ordering);
	}

}
