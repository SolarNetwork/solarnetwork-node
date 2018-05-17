/* ==================================================================
 * ModbusData.java - 20/12/2017 7:12:16 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;
import bak.pcj.map.IntKeyShortMap;
import bak.pcj.map.IntKeyShortOpenHashMap;

/**
 * Object to hold raw data extracted from a Modbus device.
 * 
 * <p>
 * This class is designed to operate as a cache of data read from a Modbus
 * device. The data is modeled as a sparse array of register address keys with
 * associated 16-bit values. It supports thread-safe write access to the saved
 * data and thread-safe read access if {@link #ModbusData(ModbusData)} or
 * {@link #copy()} are invoked to get a copy of the data.
 * </p>
 * 
 * @author matt
 * @version 1.4
 * @since 2.3
 */
public class ModbusData {

	private final IntKeyShortMap dataRegisters;
	private long dataTimestamp = 0;
	private ModbusWordOrder wordOrder;

	/**
	 * Default constructor.
	 */
	public ModbusData() {
		super();
		this.dataRegisters = new IntKeyShortOpenHashMap(64);
		this.wordOrder = ModbusWordOrder.MostToLeastSignificant;
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
	public ModbusData(ModbusData other) {
		synchronized ( other.dataRegisters ) {
			this.dataRegisters = new IntKeyShortOpenHashMap(other.dataRegisters);
			this.dataTimestamp = other.dataTimestamp;
			this.wordOrder = other.wordOrder;
		}
	}

	/**
	 * Get the data update timestamp.
	 * 
	 * @return the update timestamp, as an epoch value
	 */
	public long getDataTimestamp() {
		return dataTimestamp;
	}

	/**
	 * Create a copy of this object.
	 * 
	 * <p>
	 * This method provides a thread-safe way to get a copy of the current data.
	 * </p>
	 * 
	 * @return the new instance
	 * @see #ModbusData(ModbusData)
	 */
	public ModbusData copy() {
		return new ModbusData(this);
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
	public final Number getNumber(ModbusReference ref) {
		if ( ref == null ) {
			return null;
		}
		ModbusDataType type = ref.getDataType();
		if ( type == null ) {
			type = ModbusDataType.UInt16;
		}
		final int addr = ref.getAddress();
		switch (type) {
			case Boolean:
				return getBoolean(addr) ? 1 : 0;

			case Float32:
				return getFloat32(addr);

			case Float64:
				return getFloat64(addr);

			case Int16:
				return getSignedInt16(addr);

			case Int32:
				return getSignedInt32(addr);

			case Int64:
				return getInt64(addr);

			case UInt16:
				return getInt16(addr);

			case UInt32:
				return getInt32(addr);

			case UInt64:
				return getUnsignedInt64(addr);

			default:
				throw new IllegalArgumentException("Cannot get number for " + type + " type reference");
		}
	}

	/**
	 * Construct a 1-bit boolean from a data register address.
	 * 
	 * @param addr
	 *        the address
	 * @return the boolean, never {@literal null}
	 */
	public final Boolean getBoolean(final int addr) {
		short s = dataRegisters.get(addr);
		return (s != 0);
	}

	/**
	 * Construct an unsigned 16-bit integer from a data register address.
	 * 
	 * @param addr
	 * @return the integer, never {@literal null}
	 */
	public final Integer getInt16(final int addr) {
		short s = dataRegisters.get(addr);
		return Short.toUnsignedInt(s);
	}

	/**
	 * Construct a signed 16-bit integer from a data register address.
	 * 
	 * @param addr
	 * @return the short, never {@literal null}
	 */
	public final Short getSignedInt16(final int addr) {
		return dataRegisters.get(addr);
	}

	/**
	 * Construct an unsigned 32-bit integer from data register addresses.
	 * 
	 * @param hiAddr
	 *        the address of the high 16 bits
	 * @param loAddr
	 *        the address of the low 16 bits
	 * @return the parsed value, or {@literal null} if not available
	 */
	public final Long getInt32(final int hiAddr, final int loAddr) {
		return ModbusDataUtils.parseUnsignedInt32(dataRegisters.get(hiAddr), dataRegisters.get(loAddr));
	}

	/**
	 * Construct a signed 32-bit integer from data register addresses.
	 * 
	 * @param hiAddr
	 *        the address of the high 16 bits
	 * @param loAddr
	 *        the address of the low 16 bits
	 * @return the parsed value, or {@literal null} if not available
	 * @since 1.1
	 */
	public final Integer getSignedInt32(final int hiAddr, final int loAddr) {
		return ModbusDataUtils.parseInt32(dataRegisters.get(hiAddr), dataRegisters.get(loAddr));
	}

	/**
	 * Construct a signed 32-bit integer from data register addresses.
	 * 
	 * <p>
	 * This method will respect the configured {@link #getWordOrder()} value.
	 * </p>
	 * 
	 * @param addr
	 *        the address of the first register; the second register is assumed
	 *        to be {@code addr + 1}
	 * @return the parsed value, or {@literal null} if not available
	 * @since 1.1
	 */
	public final Integer getSignedInt32(final int addr) {
		return (wordOrder == ModbusWordOrder.MostToLeastSignificant ? getSignedInt32(addr, addr + 1)
				: getSignedInt32(addr + 1, addr));
	}

	/**
	 * Construct an unsigned 32-bit integer from a starting data register
	 * address.
	 * 
	 * <p>
	 * This method will respect the configured {@link #getWordOrder()} value.
	 * </p>
	 * 
	 * @param addr
	 *        the address of the first register; the second register is assumed
	 *        to be {@code addr + 1}
	 * @return the parsed value, or {@literal null} if not available
	 */
	public final Long getInt32(final int addr) {
		return (wordOrder == ModbusWordOrder.MostToLeastSignificant ? getInt32(addr, addr + 1)
				: getInt32(addr + 1, addr));
	}

	/**
	 * Construct a 32-bit float from data register addresses.
	 * 
	 * @param hiAddr
	 *        the address of the high 16 bits
	 * @param loAddr
	 *        the address of the low 16 bits
	 * @return the parsed value, or {@literal null} if not available.
	 */
	public final Float getFloat32(final int hiAddr, final int loAddr) {
		return ModbusDataUtils.parseFloat32(dataRegisters.get(hiAddr), dataRegisters.get(loAddr));
	}

	/**
	 * Construct a 32-bit float from a starting data register address.
	 * 
	 * <p>
	 * This method will respect the configured {@link #getWordOrder()} value.
	 * </p>
	 * 
	 * @param addr
	 *        the address of the first register; the second register is assumed
	 *        to be {@code addr + 1}
	 * @return The parsed value, or {@literal null} if not available.
	 */
	public final Float getFloat32(final int addr) {
		return (wordOrder == ModbusWordOrder.MostToLeastSignificant ? getFloat32(addr, addr + 1)
				: getFloat32(addr + 1, addr));
	}

	/**
	 * Construct a signed 64-bit long value from data register addresses.
	 * 
	 * @param h1Addr
	 *        the address of bits 63-48
	 * @param h2Addr
	 *        the address of bits 47-32
	 * @param l1Addr
	 *        the address of bits 31-16
	 * @param l2Addr
	 *        the address of bits 15-0
	 * @return the parsed long
	 */
	public final Long getInt64(final int h1Addr, final int h2Addr, final int l1Addr, final int l2Addr) {
		return ModbusDataUtils.parseInt64(dataRegisters.get(h1Addr), dataRegisters.get(h2Addr),
				dataRegisters.get(l1Addr), dataRegisters.get(l2Addr));
	}

	/**
	 * Construct a signed 64-bit integer from a starting data register address.
	 * 
	 * <p>
	 * This method will respect the configured {@link #getWordOrder()} value.
	 * </p>
	 * 
	 * @param addr
	 *        the address of the first register; the remaining three registers
	 *        are assumed to be {@code addr + 1}, {@code addr + 2}, and
	 *        {@code addr + 3}
	 * @return the parsed value, or {@literal null} if not available
	 */
	public final Long getInt64(final int addr) {
		return (wordOrder == ModbusWordOrder.MostToLeastSignificant
				? getInt64(addr, addr + 1, addr + 2, addr + 3)
				: getInt64(addr + 3, addr + 2, addr + 1, addr));
	}

	/**
	 * Construct an unsigned 64-bit integer from data register addresses.
	 * 
	 * @param h1Addr
	 *        the address of bits 63-48
	 * @param h2Addr
	 *        the address of bits 47-32
	 * @param l1Addr
	 *        the address of bits 31-16
	 * @param l2Addr
	 *        the address of bits 15-0
	 * @return the parsed value, or {@literal null} if not available
	 * @since 1.1
	 */
	public final BigInteger getUnsignedInt64(final int h1Addr, final int h2Addr, final int l1Addr,
			final int l2Addr) {
		return ModbusDataUtils.parseUnsignedInt64(dataRegisters.get(h1Addr), dataRegisters.get(h2Addr),
				dataRegisters.get(l1Addr), dataRegisters.get(l2Addr));
	}

	/**
	 * Construct an unsigned 64-bit integer from data register addresses.
	 * 
	 * <p>
	 * This method will respect the configured {@link #getWordOrder()} value.
	 * </p>
	 * 
	 * @param addr
	 *        the address of the first register; the remaining three registers
	 *        are assumed to be {@code addr + 1}, {@code addr + 2}, and
	 *        {@code addr + 3}
	 * @return the parsed value, or {@literal null} if not available
	 * @since 1.1
	 */
	public final BigInteger getUnsignedInt64(final int addr) {
		return (wordOrder == ModbusWordOrder.MostToLeastSignificant
				? getUnsignedInt64(addr, addr + 1, addr + 2, addr + 3)
				: getUnsignedInt64(addr + 3, addr + 2, addr + 1, addr));
	}

	/**
	 * Construct a 32-bit float from data register addresses.
	 * 
	 * @param h1Addr
	 *        the address of bits 63-48
	 * @param h2Addr
	 *        the address of bits 47-32
	 * @param l1Addr
	 *        the address of bits 31-16
	 * @param l2Addr
	 *        the address of bits 15-0
	 * @return the parsed value, or {@literal null} if not available
	 */
	public final Double getFloat64(final int h1Addr, final int h2Addr, final int l1Addr,
			final int l2Addr) {
		return ModbusDataUtils.parseFloat64(dataRegisters.get(h1Addr), dataRegisters.get(h2Addr),
				dataRegisters.get(l1Addr), dataRegisters.get(l2Addr));
	}

	/**
	 * Construct a 32-bit float from a starting data register address.
	 * 
	 * <p>
	 * This method will respect the configured {@link #getWordOrder()} value.
	 * </p>
	 * 
	 * @param addr
	 *        the address of the first register; the remaining three registers
	 *        are assumed to be {@code addr + 1}, {@code addr + 2}, and
	 *        {@code addr + 3}
	 * @return The parsed value, or {@literal null} if not available.
	 */
	public final Double getFloat64(final int addr) {
		return (wordOrder == ModbusWordOrder.MostToLeastSignificant
				? getFloat64(addr, addr + 1, addr + 2, addr + 3)
				: getFloat64(addr + 3, addr + 2, addr + 1, addr));
	}

	/**
	 * Construct a byte array out of a data address range.
	 * 
	 * <p>
	 * This method will respect the configured {@link #getWordOrder()} value.
	 * </p>
	 * 
	 * @param addr
	 *        the starting address of the 16-bit register to read
	 * @param count
	 *        the number of 16-bit registers to read
	 * @return the byte array, which will have a length of {@code count * 2}
	 */
	public byte[] getBytes(final int addr, final int count) {
		byte[] result = new byte[count * 2];
		for ( int i = addr, end = addr + count; i < end; i++ ) {
			short word = (wordOrder == ModbusWordOrder.MostToLeastSignificant ? dataRegisters.get(i)
					: dataRegisters.get(end - i - 1));
			result[i * 2] = (byte) ((word >> 8) & 0xFF);
			result[i * 2 + 1] = (byte) (word & 0xFF);
		}
		return result;
	}

	/**
	 * Construct a string out of a data address range.
	 * 
	 * <p>
	 * This method calls {@link #getBytes(int, int)} to interpret the data
	 * addresses as a raw byte array, and then interprets the result as a string
	 * in the given character set.
	 * </p>
	 * 
	 * @param addr
	 *        the starting address of the 16-bit register to read
	 * @param count
	 *        the number of 16-bit registers to read
	 * @param trim
	 *        if {@literal true} then remove leading/trailing whitespace from
	 *        the resulting string
	 * @param charsetName
	 *        the character set to interpret the bytes as
	 * @return the parsed string, or {@literal null} if {@code charsetName} is
	 *         not supported
	 */
	public String getString(final int addr, final int count, final boolean trim,
			final String charsetName) {
		final byte[] bytes = getBytes(addr, count);
		String result = null;
		if ( bytes != null ) {
			try {
				result = new String(bytes, charsetName);
				if ( trim ) {
					result = result.trim();
				}
			} catch ( UnsupportedEncodingException e ) {
				throw new RuntimeException(e);
			}
		}
		return result;
	}

	/**
	 * Construct a UTF-8 string out of a data address range.
	 * 
	 * @param addr
	 *        the starting address of the 16-bit register to read
	 * @param count
	 *        the number of 16-bit registers to read
	 * @param trim
	 *        if {@literal true} then remove leading/trailing whitespace from
	 *        the resulting string
	 * @return the parsed string
	 */
	public String getUtf8String(final int addr, final int count, final boolean trim) {
		return getString(addr, count, trim, ModbusDataUtils.UTF8_CHARSET);
	}

	/**
	 * Construct an ASCII string out of a data address range.
	 * 
	 * @param addr
	 *        the starting address of the 16-bit register to read
	 * @param count
	 *        the number of 16-bit registers to read
	 * @param trim
	 *        if {@literal true} then remove leading/trailing whitespace from
	 *        the resulting string
	 * @return the parsed string
	 */
	public String getAsciiString(final int addr, final int count, final boolean trim) {
		return getString(addr, count, trim, ModbusDataUtils.ASCII_CHARSET);
	}

	/**
	 * Perform a set of updates to saved register data.
	 * 
	 * @param action
	 *        the callback to perform the updates on
	 * @return this object to allow method chaining
	 */
	public final ModbusData performUpdates(ModbusDataUpdateAction action) {
		synchronized ( dataRegisters ) {
			final long now = System.currentTimeMillis();
			if ( action.updateModbusData(new MutableModbusDataView()) ) {
				dataTimestamp = now;
			}
		}
		return this;
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
	 * @since 1.2
	 */
	public final ModbusData expire() {
		synchronized ( dataRegisters ) {
			dataTimestamp = 0;
		}
		return this;
	}

	/**
	 * API for performing updates to the data.
	 */
	public static interface MutableModbusData {

		/**
		 * Store a mapping of addresses to associated 16-bit integer register
		 * data values.
		 * 
		 * @param data
		 *        the data map to save; all values will be treated as 16-bit
		 *        integer
		 */
		public void saveDataMap(final Map<Integer, ? extends Number> data);

		/**
		 * Store an array of 16-bit integer register data values, starting at a
		 * given address.
		 * 
		 * @param data
		 *        the data array to save, as shorts
		 * @param addr
		 *        the starting address of the data
		 */
		public void saveDataArray(final short[] data, int addr);

		/**
		 * Store an array of 16-bit integer register data values, starting at a
		 * given address.
		 * 
		 * <p>
		 * Note that the data values will be treated as 16-bit unsigned values.
		 * </p>
		 * 
		 * @param data
		 *        the data array to save, as ints
		 * @param addr
		 *        the starting address of the data
		 */
		public void saveDataArray(final int[] data, int addr);

		/**
		 * Store an array of 16-bit integer register data values, starting at a
		 * given address.
		 * 
		 * <p>
		 * Note that the data values will be treated as 16-bit unsigned values.
		 * </p>
		 * 
		 * @param data
		 *        the data array to save, as ints
		 * @param addr
		 *        the starting address of the data
		 */
		public void saveDataArray(final Integer[] data, int addr);

		/**
		 * Store an array of bytes into 16-bit register data values, starting at
		 * a given address.
		 * 
		 * <p>
		 * The bytes are packed into 16-bit values, so the number of register
		 * addresses written to will be <i>half</i> of the length of
		 * {@code bytes} (rounded up to nearest even even length).
		 * </p>
		 * 
		 * <p>
		 * This method will respect the configured {@link #getWordOrder()}
		 * value.
		 * </p>
		 * 
		 * @param data
		 *        the bytes to save
		 * @param addr
		 *        the starting address of the data
		 */
		public void saveBytes(final byte[] data, int addr);
	}

	/**
	 * API for performing updates to the saved data.
	 */
	public static interface ModbusDataUpdateAction {

		/**
		 * Perform updates to the data.
		 * 
		 * @param m
		 *        a mutable version of the data to update
		 * @return {@literal true} if {@code dataTimestamp} should be updated to
		 *         the current time
		 */
		public boolean updateModbusData(MutableModbusData m);
	}

	/**
	 * Internal mutable view of this class, meant to be used for thread-safe
	 * writes.
	 * 
	 * <p>
	 * All methods are assumed to be synchronized on {@code dataRegsiters}.
	 * </p>
	 */
	private class MutableModbusDataView implements MutableModbusData {

		@Override
		public final void saveDataMap(final Map<Integer, ? extends Number> data) {
			for ( Map.Entry<Integer, ? extends Number> me : data.entrySet() ) {
				dataRegisters.put(me.getKey(), me.getValue().shortValue());
			}
		}

		@Override
		public final void saveDataArray(final short[] data, int addr) {
			if ( data == null || data.length < 1 ) {
				return;
			}
			for ( short v : data ) {
				dataRegisters.put(addr, v);
				addr++;
			}
		}

		@Override
		public final void saveDataArray(final int[] data, int addr) {
			if ( data == null || data.length < 1 ) {
				return;
			}
			for ( int v : data ) {
				dataRegisters.put(addr, (short) (v & 0xFFFF));
				addr++;
			}
		}

		@Override
		public final void saveDataArray(final Integer[] data, int addr) {
			if ( data == null || data.length < 1 ) {
				return;
			}
			for ( Integer v : data ) {
				dataRegisters.put(addr, (short) (v.intValue() & 0xFFFF));
				addr++;
			}
		}

		@Override
		public void saveBytes(final byte[] data, final int addr) {
			if ( data == null || data.length < 1 ) {
				return;
			}
			final int wordLength = (int) Math.ceil(data.length / 2.0);
			for ( int i = 0, j = 0; i < data.length; i += 2, j++ ) {
				int n = ((data[i] & 0xFF) << 8);
				if ( i + 1 < data.length ) {
					n = n | (data[i + 1] & 0xFF);
				}
				int idx = (wordOrder == ModbusWordOrder.MostToLeastSignificant ? addr + j
						: addr + wordLength - j - 1);
				dataRegisters.put(idx, (short) n);
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
	 * ModbusData{
	 *      30000: 0x4141, 0x727E
	 *      30006: 0xFFC0, 0x0000
	 *      ...
	 *      30344: 0x0000, 0x0000
	 * }
	 * </pre>
	 * 
	 * @return debug string
	 */
	public final String dataDebugString() {
		final StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append("{");
		int[] keys = dataRegisters.keySet().toArray();
		if ( keys.length > 0 ) {
			Arrays.sort(keys);
			boolean odd = false;
			int last = -2;
			for ( int k : keys ) {
				odd = k % 2 == 1 ? true : false;
				if ( k > last + 1 ) {
					int rowAddr = odd ? k - 1 : k;
					buf.append("\n\t").append(String.format("%5d", rowAddr)).append(": ");
					if ( odd ) {
						// fill in empty space for start of row
						buf.append("      , ");
					}
					last = k;
					if ( odd ) {
						last -= 1;
					}
				} else if ( odd ) {
					buf.append(", ");
				}
				buf.append(String.format("0x%04X", dataRegisters.get(k)));
			}
			buf.append("\n");
		}
		buf.append("}");
		return buf.toString();
	}

	/**
	 * Get the word ordering to use when reading multi-register data types.
	 * 
	 * @return the word order
	 * @since 1.3
	 */
	public ModbusWordOrder getWordOrder() {
		return wordOrder;
	}

	/**
	 * Set the word ordering to use when reading multi-register data types.
	 * 
	 * @param wordOrder
	 *        the word order to use; {@literal null} will be ignored
	 * @since 1.3
	 */
	public void setWordOrder(ModbusWordOrder wordOrder) {
		if ( wordOrder == null ) {
			return;
		}
		this.wordOrder = wordOrder;
	}

}
