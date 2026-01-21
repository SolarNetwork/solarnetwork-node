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

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.solarnetwork.node.domain.DataAccessor;
import net.solarnetwork.util.ByteUtils;
import net.solarnetwork.util.CollectionUtils;
import net.solarnetwork.util.Half;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.IntRangeSet;
import net.solarnetwork.util.IntShortMap;

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
 * @version 3.2
 * @since 2.3
 */
public class ModbusData implements DataAccessor {

	private final IntShortMap dataRegisters;
	private long dataTimestamp = 0;
	private ModbusWordOrder wordOrder;

	/**
	 * Default constructor.
	 */
	public ModbusData() {
		this(false);
	}

	/**
	 * Constructor.
	 *
	 * @param strictAddresses
	 *        if {@code true} then throw {@code NoSuchElementException} when
	 *        attempting to read from an address that does not already have an
	 *        associated value
	 * @since 3.2
	 */
	public ModbusData(boolean strictAddresses) {
		super();
		this.dataRegisters = new IntShortMap(64,
				strictAddresses ? IntShortMap.VALUE_NO_SUCH_ELEMENT : (short) 0);
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
			this.dataRegisters = (IntShortMap) other.dataRegisters.clone();
			this.dataTimestamp = other.dataTimestamp;
			this.wordOrder = other.wordOrder;
		}
	}

	/**
	 * Get the number of registers with a set value.
	 *
	 * @return the number of set registers
	 */
	public int size() {
		return dataRegisters.size();
	}

	/**
	 * Test if the register data is empty.
	 *
	 * @return {@literal true} if no registers have been set
	 */
	public boolean isEmpty() {
		return dataRegisters.isEmpty();
	}

	@Override
	public Instant getDataTimestamp() {
		return dataTimestamp > 0 ? Instant.ofEpochMilli(dataTimestamp) : null;
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
	 * @since 1.4
	 */
	public final Number getNumber(ModbusReference ref) {
		return getNumber(ref, 0);
	}

	/**
	 * Get a number value from a relative reference.
	 *
	 * @param ref
	 *        the relative reference to get the number value for
	 * @param offset
	 *        the address offset to add to {@link ModbusReference#getAddress()}
	 * @return the value, or {@literal null} if {@code ref} is {@literal null}
	 * @throws IllegalArgumentException
	 *         if the reference data type is not numeric
	 * @since 1.4
	 */
	public final Number getNumber(ModbusReference ref, int offset) {
		if ( ref == null ) {
			return null;
		}
		ModbusDataType type = ref.getDataType();
		if ( type == null ) {
			type = ModbusDataType.UInt16;
		}
		final int addr = ref.getAddress() + offset;
		switch (type) {
			case Boolean:
				return getBoolean(addr) ? 1 : 0;

			case Float16:
				return getFloat16(addr);

			case Float32:
				return getFloat32(addr);

			case Float64:
				return getFloat64(addr);

			case Int16:
				return getInt16(addr);

			case Int32:
				return getInt32(addr);

			case Int64:
				return getInt64(addr);

			case UInt16:
				return getUnsignedInt16(addr);

			case UInt32:
				return getUnsignedInt32(addr);

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
		short s = dataRegisters.getValue(addr);
		return (s != 0);
	}

	/**
	 * Construct an unsigned 16-bit integer from a data register address.
	 *
	 * @param addr
	 *        the register address
	 * @return the integer, never {@literal null}
	 */
	public final Integer getUnsignedInt16(final int addr) {
		short s = dataRegisters.getValue(addr);
		return s & 0xFFFF;
	}

	/**
	 * Construct a signed 16-bit integer from a data register address.
	 *
	 * @param addr
	 *        the register address
	 * @return the short, never {@literal null}
	 */
	public final Short getInt16(final int addr) {
		return dataRegisters.getValue(addr);
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
	public final Long getUnsignedInt32(final int hiAddr, final int loAddr) {
		return ModbusDataUtils.parseUnsignedInt32(dataRegisters.getValue(hiAddr),
				dataRegisters.getValue(loAddr));
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
	public final Integer getInt32(final int hiAddr, final int loAddr) {
		return ModbusDataUtils.parseInt32(dataRegisters.getValue(hiAddr),
				dataRegisters.getValue(loAddr));
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
	public final Integer getInt32(final int addr) {
		return (wordOrder == ModbusWordOrder.MostToLeastSignificant ? getInt32(addr, addr + 1)
				: getInt32(addr + 1, addr));
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
	public final Long getUnsignedInt32(final int addr) {
		return (wordOrder == ModbusWordOrder.MostToLeastSignificant ? getUnsignedInt32(addr, addr + 1)
				: getUnsignedInt32(addr + 1, addr));
	}

	/**
	 * Construct a 16-bit float from data register addresses.
	 *
	 * @param addr
	 *        the address of the 16 bits
	 * @return the parsed value, or {@literal null} if not available.
	 * @since 1.4
	 */
	public final Half getFloat16(final int addr) {
		return ModbusDataUtils.parseFloat16(dataRegisters.getValue(addr));
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
		return ModbusDataUtils.parseFloat32(dataRegisters.getValue(hiAddr),
				dataRegisters.getValue(loAddr));
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
		return ModbusDataUtils.parseInt64(dataRegisters.getValue(h1Addr), dataRegisters.getValue(h2Addr),
				dataRegisters.getValue(l1Addr), dataRegisters.getValue(l2Addr));
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
		return ModbusDataUtils.parseUnsignedInt64(dataRegisters.getValue(h1Addr),
				dataRegisters.getValue(h2Addr), dataRegisters.getValue(l1Addr),
				dataRegisters.getValue(l2Addr));
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
		return ModbusDataUtils.parseFloat64(dataRegisters.getValue(h1Addr),
				dataRegisters.getValue(h2Addr), dataRegisters.getValue(l1Addr),
				dataRegisters.getValue(l2Addr));
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
	 * This method will assume {@link ModbusWordOrder#MostToLeastSignificant}
	 * word ordering.
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
		for ( int i = addr, end = addr + count, index = 0; i < end; i++, index += 2 ) {
			short word = dataRegisters.getValue(i);
			result[index] = (byte) ((word >> 8) & 0xFF);
			result[index + 1] = (byte) (word & 0xFF);
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
	 * @return the parsed string, or {@literal null} if {@code count} is
	 *         {@literal 0}
	 * @throws IllegalCharsetNameException
	 *         If the given character set name is illegal
	 * @throws IllegalArgumentException
	 *         If {@code charsetName} is null
	 * @throws UnsupportedCharsetException
	 *         If no support for the named character set is available
	 */
	public final String getString(final int addr, final int count, final boolean trim,
			final String charsetName) {
		return getString(addr, count, trim, Charset.forName(charsetName));
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
	 *        the resulting string as well as any null bytes, i.e. {@literal \0}
	 * @param charset
	 *        the character set to interpret the bytes as
	 * @return the parsed string, or {@literal null} if {@code count} is
	 *         {@literal 0}
	 */
	public final String getString(final int addr, final int count, final boolean trim,
			final Charset charset) {
		final byte[] bytes = getBytes(addr, count);
		String result = null;
		if ( bytes != null ) {
			result = new String(bytes, charset);
			if ( trim ) {
				result = result.trim().replace("\0", "");
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
	public final String getUtf8String(final int addr, final int count, final boolean trim) {
		return getString(addr, count, trim, ByteUtils.UTF8);
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
	public final String getAsciiString(final int addr, final int count, final boolean trim) {
		return getString(addr, count, trim, ByteUtils.ASCII);
	}

	/**
	 * Construct a UTF-8 string from a reference.
	 *
	 * @param ref
	 *        the reference to get the string value for
	 * @param trim
	 *        if {@literal true} then remove leading/trailing whitespace from
	 *        the resulting string
	 * @return the parsed string
	 * @since 1.4
	 */
	public final String getUtf8String(final ModbusReference ref, final boolean trim) {
		return getUtf8String(ref, 0, trim);
	}

	/**
	 * Construct a UTF-8 string from a reference.
	 *
	 * @param ref
	 *        the reference to get the string value for
	 * @param offset
	 *        the address offset to add to {@link ModbusReference#getAddress()}
	 * @param trim
	 *        if {@literal true} then remove leading/trailing whitespace from
	 *        the resulting string
	 * @return the parsed string
	 * @since 1.4
	 */
	public final String getUtf8String(final ModbusReference ref, int offset, final boolean trim) {
		return getString(ref.getAddress() + offset, ref.getWordLength(), trim, ByteUtils.UTF8);
	}

	/**
	 * Construct an ASCII string from a reference.
	 *
	 * @param ref
	 *        the reference to get the string value for
	 * @param trim
	 *        if {@literal true} then remove leading/trailing whitespace from
	 *        the resulting string
	 * @return the parsed string
	 * @since 1.4
	 */
	public final String getAsciiString(final ModbusReference ref, final boolean trim) {
		return getAsciiString(ref, 0, trim);
	}

	/**
	 * Construct an ASCII string from a relative reference.
	 *
	 * @param ref
	 *        the reference to get the string value for
	 * @param offset
	 *        the address offset to add to {@link ModbusReference#getAddress()}
	 * @param trim
	 *        if {@literal true} then remove leading/trailing whitespace from
	 *        the resulting string
	 * @return the parsed string
	 * @since 1.4
	 */
	public final String getAsciiString(final ModbusReference ref, final int offset, final boolean trim) {
		return getString(ref.getAddress() + offset, ref.getWordLength(), trim, ByteUtils.ASCII);
	}

	/**
	 * Construct an ISO-LATIN-1 string from a relative reference.
	 *
	 * @param ref
	 *        the reference to get the string value for
	 * @param offset
	 *        the address offset to add to {@link ModbusReference#getAddress()}
	 * @param trim
	 *        if {@literal true} then remove leading/trailing whitespace from
	 *        the resulting string
	 * @return the parsed string
	 * @since 1.8
	 */
	public final String getLatin1String(final ModbusReference ref, final int offset,
			final boolean trim) {
		return getString(ref.getAddress() + offset, ref.getWordLength(), trim, ByteUtils.LATIN1);
	}

	/**
	 * Perform a set of updates to saved register data.
	 *
	 * @param action
	 *        the callback to perform the updates on
	 * @return this object to allow method chaining
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public final ModbusData performUpdates(ModbusDataUpdateAction action) throws IOException {
		synchronized ( dataRegisters ) {
			final long now = System.currentTimeMillis();
			if ( action.updateModbusData(new MutableModbusDataView(dataRegisters)) ) {
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
	 * Get a value for a reference.
	 *
	 * @param ref
	 *        the reference to get the number value for
	 * @return the value, or {@literal null} if {@code ref} is {@literal null}
	 * @since 3.1
	 */
	public Object getValue(ModbusReference ref) {
		return getValue(ref.getDataType(), ref.getAddress(), ref.getWordLength());
	}

	/**
	 * Get a value.
	 *
	 * @param dataType
	 *        the data type
	 * @param address
	 *        the register address
	 * @param count
	 *        the word length, for variable-length data types only
	 * @return the decoded value, or {@literal null}
	 * @since 3.1
	 */
	public Object getValue(ModbusDataType dataType, int address, int count) {
		switch (dataType) {
			case Boolean:
				return getBoolean(address);

			case Bytes:
				return getBytes(address, count);

			case Float16:
				return getFloat16(address);

			case Float32:
				return getFloat32(address);

			case Float64:
				return getFloat64(address);

			case Int16:
				return getInt16(address);

			case UInt16:
				return getUnsignedInt16(address);

			case Int32:
				return getInt32(address);

			case UInt32:
				return getUnsignedInt32(address);

			case Int64:
				return getInt64(address);

			case UInt64:
				return getUnsignedInt64(address);

			case StringAscii:
				return getAsciiString(address, count, true);

			case StringUtf8:
				return getUtf8String(address, count, true);

			default:
				return null;
		}
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
		 * This method will assume
		 * {@link ModbusWordOrder#MostToLeastSignificant} word ordering.
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
		 * @throws IOException
		 *         if any communication error occurs
		 */
		public boolean updateModbusData(MutableModbusData m) throws IOException;
	}

	/**
	 * Mutable view of Modbus data registers, meant to be used for thread-safe
	 * writes.
	 *
	 * <p>
	 * All methods are assumed to be synchronized on {@code dataRegsiters}.
	 * </p>
	 */
	public static class MutableModbusDataView implements MutableModbusData {

		private final IntShortMap dataRegisters;

		/**
		 * Construct with data registers to mutate.
		 *
		 * @param dataRegisters
		 *        the registers to mutate; calling code should by synchronized
		 *        on this instance
		 */
		public MutableModbusDataView(IntShortMap dataRegisters) {
			super();
			this.dataRegisters = dataRegisters;
		}

		/**
		 * Construct with data registers to mutate.
		 *
		 * @param dataRegisters
		 *        the registers to mutate; calling code should by synchronized
		 *        on this instance
		 * @param wordOrder
		 *        this parameter is not used, but maintained for backwards
		 *        compatibility
		 */
		public MutableModbusDataView(IntShortMap dataRegisters, ModbusWordOrder wordOrder) {
			this(dataRegisters);
		}

		@Override
		public final void saveDataMap(final Map<Integer, ? extends Number> data) {
			for ( Map.Entry<Integer, ? extends Number> me : data.entrySet() ) {
				dataRegisters.putValue(me.getKey(), me.getValue().shortValue());
			}
		}

		@Override
		public final void saveDataArray(final short[] data, int addr) {
			if ( data == null || data.length < 1 ) {
				return;
			}
			for ( short v : data ) {
				dataRegisters.putValue(addr, v);
				addr++;
			}
		}

		@Override
		public final void saveDataArray(final int[] data, int addr) {
			if ( data == null || data.length < 1 ) {
				return;
			}
			for ( int v : data ) {
				dataRegisters.putValue(addr, (short) (v & 0xFFFF));
				addr++;
			}
		}

		@Override
		public final void saveDataArray(final Integer[] data, int addr) {
			if ( data == null || data.length < 1 ) {
				return;
			}
			for ( Integer v : data ) {
				dataRegisters.putValue(addr, (short) (v.intValue() & 0xFFFF));
				addr++;
			}
		}

		@Override
		public void saveBytes(final byte[] data, final int addr) {
			if ( data == null || data.length < 1 ) {
				return;
			}
			for ( int i = 0, j = 0; i < data.length; i += 2, j++ ) {
				int n = ((data[i] & 0xFF) << 8);
				if ( i + 1 < data.length ) {
					n = n | (data[i + 1] & 0xFF);
				}
				int idx = addr + j;
				dataRegisters.putValue(idx, (short) n);
			}
		}

	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ModbusData{dataTimestamp=");
		builder.append(dataTimestamp);
		builder.append(", ");
		if ( wordOrder != null ) {
			builder.append("wordOrder=");
			builder.append(wordOrder);
			builder.append(", ");
		}
		if ( dataRegisters != null ) {
			builder.append("dataRegisters=");
			builder.append(dataRegisters);
		}
		builder.append("}");
		return builder.toString();
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
		if ( !dataRegisters.isEmpty() ) {
			final int[] last = new int[] { -2 };
			dataRegisters.forEachOrdered((k, v) -> {
				boolean odd = k % 2 == 1 ? true : false;
				if ( k > last[0] + 1 ) {
					int rowAddr = odd ? k - 1 : k;
					buf.append("\n\t").append(String.format("%5d", rowAddr)).append(": ");
					if ( odd ) {
						// fill in empty space for start of row
						buf.append("      , ");
					}
					last[0] = k;
					if ( odd ) {
						last[0] -= 1;
					}
				} else if ( odd ) {
					buf.append(", ");
				}
				buf.append(String.format("0x%04X", v));
			});
			buf.append("\n");
		}
		buf.append("}");
		return buf.toString();
	}

	/**
	 * Copy the raw modbus data for a set of addresses into an array.
	 *
	 * @param dest
	 *        the destination array
	 * @param destFrom
	 *        the starting destination array index to populate
	 * @param address
	 *        the Modbus address to start from
	 * @param length
	 *        the number of Modbus registers to copy
	 * @since 2.0
	 */
	public final void slice(short[] dest, int destFrom, int address, int length) {
		dataRegisters.forEachOrdered(address, address + length, (i, v) -> {
			int idx = i - address + destFrom;
			if ( idx < dest.length ) {
				dest[idx] = v;
			}
		});
	}

	/**
	 * Get the word ordering to use when reading multi-register data types.
	 *
	 * @return the word order
	 * @since 1.3
	 */
	public final ModbusWordOrder getWordOrder() {
		return wordOrder;
	}

	/**
	 * Set the word ordering to use when reading multi-register data types.
	 *
	 * @param wordOrder
	 *        the word order to use; {@literal null} will be ignored
	 * @since 1.3
	 */
	public final void setWordOrder(ModbusWordOrder wordOrder) {
		if ( wordOrder == null ) {
			return;
		}
		this.wordOrder = wordOrder;
	}

	/**
	 * Refresh a range of data from the Modbus device into this object.
	 *
	 * <p>
	 * This method uses the {@link ModbusReadFunction#ReadHoldingRegister} with
	 * a maximum of {@literal 64} addresses read per transaction.
	 * </p>
	 *
	 * @param conn
	 *        the connection
	 * @param rangeSet
	 *        the Modbus registers to read, where each {@link IntRange} in the
	 *        set defines <i>inclusive</i> address ranges to read
	 * @since 1.6
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public final void refreshData(final ModbusConnection conn, final IntRangeSet rangeSet)
			throws IOException {
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister, rangeSet, 64);
	}

	/**
	 * Refresh a range of data from the Modbus device into this object.
	 *
	 * @param conn
	 *        the connection
	 * @param readFunction
	 *        the Modbus read function to use
	 * @param rangeSet
	 *        the Modbus registers to read, where each {@link IntRange} in the
	 *        set defines <i>inclusive</i> address ranges to read
	 * @param maxRangeLength
	 *        the maximum length of any combined range in the resulting set
	 * @since 1.6
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public final void refreshData(final ModbusConnection conn, final ModbusReadFunction readFunction,
			final IntRangeSet rangeSet, final int maxRangeLength) throws IOException {
		final List<IntRange> ranges = CollectionUtils.coveringIntRanges(rangeSet, maxRangeLength);
		refreshData(conn, readFunction, ranges);
	}

	/**
	 * Refresh a range of data from the Modbus device into this object.
	 *
	 * @param conn
	 *        the connection
	 * @param readFunction
	 *        the Modbus read function to use
	 * @param ranges
	 *        the Modbus registers to read, where each {@link IntRange} in the
	 *        set defines <i>inclusive</i> address ranges to read
	 * @since 2.0
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public final void refreshData(final ModbusConnection conn, final ModbusReadFunction readFunction,
			final Collection<IntRange> ranges) throws IOException {
		performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) throws IOException {
				refreshData(conn, readFunction, ranges, m);
				return true;
			}
		});
	}

	/**
	 * Read data from the device and update a mutable data instance.
	 *
	 * @param conn
	 *        the connection
	 * @param readFunction
	 *        the read function
	 * @param ranges
	 *        the ranges of Modbus addresses to read/update
	 * @param m
	 *        the mutable data to update
	 * @since 2.0
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public final void refreshData(final ModbusConnection conn, final ModbusReadFunction readFunction,
			final Collection<IntRange> ranges, final MutableModbusData m) throws IOException {
		for ( IntRange r : ranges ) {
			short[] data = conn.readWords(readFunction, r.getMin(), r.length());
			m.saveDataArray(data, r.getMin());
		}
	}

	/**
	 * Get a read-only Map view of all modbus registers as unsigned integer
	 * values.
	 *
	 * @return the data map, never {@literal null}
	 * @since 1.7
	 */
	public final Map<Integer, Integer> getUnsignedDataMap() {
		final IntShortMap data = (IntShortMap) this.dataRegisters.clone();
		return data.unsignedMap();
	}

	/**
	 * Get direct access to all modbus registers.
	 *
	 * @return the data map, never {@literal null}
	 * @since 3.1
	 */
	public final IntShortMap dataRegisters() {
		return dataRegisters;
	}

}
