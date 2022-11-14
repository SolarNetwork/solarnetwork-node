/* ==================================================================
 * ModbusRegisterData.java - 18/09/2020 7:27:27 AM
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

package net.solarnetwork.node.io.modbus;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.Arrays;
import java.util.BitSet;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;

/**
 * Data for a Modbus register set.
 * 
 * @author matt
 * @version 1.0
 * @since 4.2
 */
public class ModbusRegisterData {

	private static final Logger log = LoggerFactory.getLogger(ModbusRegisterData.class);

	private final BitSet coils;
	private long coilsTimestamp;
	private final BitSet discretes;
	private long discretesTimestamp;
	private final ModbusData inputs;
	private final ModbusData holdings;

	/**
	 * API for performing updates to a bit register block.
	 */
	public static interface ModbusBitsUpdateAction {

		/**
		 * Perform updates to the data.
		 * 
		 * @param bits
		 *        the bits to update
		 * @return {@literal true} if {@code dataTimestamp} should be updated to
		 *         the current time
		 * @throws IOException
		 *         if any communication error occurs
		 */
		public boolean updateModbusBits(BitSet bits) throws IOException;
	}

	/**
	 * Constructor.
	 */
	public ModbusRegisterData() {
		this(new BitSet(), new BitSet(), new ModbusData(), new ModbusData());
	}

	/**
	 * Constructor.
	 * 
	 * <p>
	 * If any argument is {@literal null} a new instance will be created and
	 * used for that value.
	 * </p>
	 * 
	 * @param coils
	 *        the coil register data
	 * @param discretes
	 *        the discrete register data
	 * @param holdings
	 *        the holding register data
	 * @param inputs
	 *        the input register data
	 */
	public ModbusRegisterData(BitSet coils, BitSet discretes, ModbusData holdings, ModbusData inputs) {
		super();
		this.coils = coils != null ? coils : new BitSet();
		this.discretes = discretes != null ? discretes : new BitSet();
		this.holdings = holdings != null ? holdings : new ModbusData();
		this.inputs = inputs != null ? inputs : new ModbusData();
		this.coilsTimestamp = 0;
		this.discretesTimestamp = 0;
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
	public ModbusRegisterData(ModbusRegisterData other) {
		super();
		synchronized ( other.coils ) {
			this.coils = (BitSet) other.coils.clone();
			this.coilsTimestamp = other.coilsTimestamp;
		}
		synchronized ( other.discretes ) {
			this.discretes = (BitSet) other.discretes.clone();
			this.discretesTimestamp = other.discretesTimestamp;
		}
		this.holdings = other.holdings.copy();
		this.inputs = other.inputs.copy();
	}

	/**
	 * Create a copy of this object.
	 * 
	 * <p>
	 * This method provides a thread-safe way to get a copy of the current data.
	 * </p>
	 * 
	 * @return the new instance
	 * @see #ModbusRegisterData(ModbusRegisterData)
	 */
	public ModbusRegisterData copy() {
		return new ModbusRegisterData(this);
	}

	/**
	 * Test if the registers are all empty.
	 * 
	 * @return {@literal true} if no registers or bits have been set
	 */
	public boolean isEmpty() {
		synchronized ( coils ) {
			if ( !coils.isEmpty() ) {
				return false;
			}
		}
		synchronized ( discretes ) {
			if ( !discretes.isEmpty() ) {
				return false;
			}
		}
		if ( !holdings.isEmpty() ) {
			return false;
		}
		if ( !inputs.isEmpty() ) {
			return false;
		}
		return true;
	}

	/**
	 * Test if there is holding or input register data.
	 * 
	 * @return {@literal true} if either holding or input registers have been
	 *         set
	 */
	public boolean hasRegisterData() {
		return !(holdings.isEmpty() && inputs.isEmpty());
	}

	/**
	 * Get the data timestamp (last update time) for a given data block.
	 * 
	 * @param blockType
	 *        the block type
	 * @return the update timestamp, or {@literal null} if never updated
	 */
	public Instant getDataTimestamp(ModbusRegisterBlockType blockType) {
		long ts = 0;
		switch (blockType) {
			case Coil:
				synchronized ( coils ) {
					ts = coilsTimestamp;
				}
				break;

			case Discrete:
				synchronized ( discretes ) {
					ts = discretesTimestamp;
				}
				break;

			case Holding:
				return holdings.getDataTimestamp();

			case Input:
				return inputs.getDataTimestamp();
		}

		return ts > 0 ? Instant.ofEpochMilli(ts) : null;
	}

	/**
	 * Force a data timestamp to be expired.
	 * 
	 * <p>
	 * Calling this method will reset the data timestamp to zero, effectively
	 * expiring the data.
	 * </p>
	 * 
	 * @param blockType
	 *        the type of register block to expire
	 */
	public final void expire(ModbusRegisterBlockType blockType) {
		switch (blockType) {
			case Coil:
				synchronized ( coils ) {
					coilsTimestamp = 0;
				}
				break;

			case Discrete:
				synchronized ( discretes ) {
					discretesTimestamp = 0;
				}
				break;

			case Holding:
				holdings.expire();

			case Input:
				inputs.expire();
		}
	}

	/**
	 * Test if a block of data was updated before a specific date.
	 * 
	 * @param blockType
	 *        the type of register block to compare the data timestamp against
	 * @param date
	 *        the date to compare
	 * @return {@literal true} if the block's data timestamp is older than
	 *         {@code date}
	 */
	public final boolean isOlderThan(ModbusRegisterBlockType blockType, long date) {
		long ts = 0;
		Instant inst = null;
		switch (blockType) {
			case Coil:
				synchronized ( coils ) {
					ts = coilsTimestamp;
				}
				break;

			case Discrete:
				synchronized ( discretes ) {
					ts = discretesTimestamp;
				}
				break;

			case Holding:
				inst = holdings.getDataTimestamp();

			case Input:
				inst = inputs.getDataTimestamp();
		}

		if ( inst != null ) {
			return inst.toEpochMilli() < date;
		}
		return ts < date;
	}

	/**
	 * Get the word order.
	 * 
	 * @return the word order
	 */
	public ModbusWordOrder getWordOrder() {
		return holdings.getWordOrder();
	}

	/**
	 * Set the word order.
	 * 
	 * @param order
	 *        the word order
	 */
	public void setWordOrder(ModbusWordOrder order) {
		holdings.setWordOrder(order);
		inputs.setWordOrder(order);
	}

	/**
	 * Write a bit to a coil or discrete block type.
	 * 
	 * @param blockType
	 *        the block type, must be either {@literal Coil} or
	 *        {@literal Discrete}
	 * @param address
	 *        the address to write to
	 * @param value
	 *        the bit value
	 * @throws IllegalArgumentException
	 *         if {@code blockType} is not valid
	 */
	public void writeBit(ModbusRegisterBlockType blockType, int address, boolean value) {
		switch (blockType) {
			case Coil:
				writeCoil(address, value);
				break;

			case Discrete:
				writeDiscrete(address, value);
				break;

			default:
				throw new IllegalArgumentException("Cannot write bit value to block type " + blockType);
		}
	}

	/**
	 * Write a set of bits to a coil or discrete block type.
	 * 
	 * @param blockType
	 *        the block type, must be either {@literal Coil} or
	 *        {@literal Discrete}
	 * @param address
	 *        the address to write to
	 * @param count
	 *        the number of bits to update
	 * @param set
	 *        the bit values
	 * @throws IllegalArgumentException
	 *         if {@code blockType} is not valid
	 */
	public void writeBits(ModbusRegisterBlockType blockType, int address, int count, BitSet set) {
		switch (blockType) {
			case Coil:
				writeCoils(address, count, set);
				break;

			case Discrete:
				writeDiscretes(address, count, set);
				break;

			default:
				throw new IllegalArgumentException("Cannot write bit values to block type " + blockType);
		}
	}

	/**
	 * Write a non-bit register value.
	 * 
	 * @param blockType
	 *        the block type, must be either {@literal Holding} or
	 *        {@literal Input}
	 * @param dataType
	 *        the desired data type to write
	 * @param address
	 *        the address to write to
	 * @param count
	 *        the number of registers to write to, for variable-length data
	 *        types
	 * @param value
	 *        the value to write
	 * @throws IllegalArgumentException
	 *         if {@code blockType} is not valid
	 */
	public void writeValue(ModbusRegisterBlockType blockType, ModbusDataType dataType, int address,
			int count, Object value) {
		short[] dataValue = encodeValue(dataType, address, count, value);
		writeRegisters(blockType, address, dataValue);
	}

	/**
	 * Write to a set of registers.
	 * 
	 * @param blockType
	 *        the block type to update
	 * @param address
	 *        the starting register address to write to
	 * @param dataValue
	 *        the register values to write
	 */
	public void writeRegisters(ModbusRegisterBlockType blockType, int address, short[] dataValue) {
		if ( dataValue == null || dataValue.length < 1 ) {
			return;
		}
		switch (blockType) {
			case Holding:
				writeHoldings(address, dataValue);
				break;

			case Input:
				writeInputs(address, dataValue);
				break;

			default:
				throw new IllegalArgumentException(
						"Cannot write value value to block type " + blockType);
		}
	}

	private short[] encodeValue(ModbusDataType dataType, int address, int count, Object value) {
		if ( value == null ) {
			return null;
		}
		short[] result = null;
		switch (dataType) {
			case Bytes:
				if ( value instanceof byte[] ) {
					result = limitLength(ModbusDataUtils.encodeBytes((byte[]) value, getWordOrder()),
							count);
				}
				break;

			case StringAscii:
			case StringUtf8:
				try {
					byte[] strBytes;
					if ( dataType == ModbusDataType.StringAscii ) {
						strBytes = value.toString().getBytes("US-ASCII");
					} else {
						strBytes = value.toString().getBytes("UTF-8");
					}
					result = limitLength(ModbusDataUtils.encodeBytes(strBytes, getWordOrder()), count);
				} catch ( UnsupportedEncodingException e ) {
					// should not get here
				}
				break;

			default:
				if ( value instanceof Number ) {
					result = ModbusDataUtils.encodeNumber(dataType, (Number) value, getWordOrder());
				}
		}
		return result;
	}

	private static short[] limitLength(short[] array, int max) {
		if ( array == null || array.length <= max ) {
			return array;
		}
		short[] truncated = new short[max];
		System.arraycopy(array, 0, truncated, 0, max);
		return truncated;
	}

	/**
	 * Read a set of coil values.
	 * 
	 * <p>
	 * The returned set starts at index {@code 0}, meaning the indexes are
	 * shifted down by {@code address} positions.
	 * </p>
	 * 
	 * @param address
	 *        the starting register address
	 * @param count
	 *        the number of registers to read
	 * @return the result set
	 */
	public BitSet readCoils(int address, int count) {
		return readBits(address, count, coils);
	}

	/**
	 * Write a single coil register value.
	 * 
	 * @param address
	 *        the register address
	 * @param value
	 *        the value
	 */
	public void writeCoil(int address, boolean value) {
		writeBit(address, value, coils);
	}

	/**
	 * Write a set of coil register values.
	 * 
	 * @param address
	 *        the register address to start at
	 * @param count
	 *        the count of registers to read
	 * @param set
	 *        the set of coil values to write, starting from index {@literal 0}
	 */
	public void writeCoils(int address, int count, BitSet set) {
		writeBits(address, count, set, coils);
	}

	/**
	 * Write a single discrete register value.
	 * 
	 * @param address
	 *        the register address
	 * @param value
	 *        the value
	 */
	public void writeDiscrete(int address, boolean value) {
		writeBit(address, value, discretes);
	}

	/**
	 * Write a set of discrete register values.
	 * 
	 * @param address
	 *        the register address to start at
	 * @param count
	 *        the count of registers to read
	 * @param set
	 *        the set of coil values to write, starting from index {@literal 0}
	 */
	public void writeDiscretes(int address, int count, BitSet set) {
		writeBits(address, count, set, discretes);
	}

	private void writeBit(int address, boolean value, BitSet set) {
		synchronized ( set ) {
			set.set(address, value);
			if ( set == coils ) {
				coilsTimestamp = System.currentTimeMillis();
			} else {
				discretesTimestamp = System.currentTimeMillis();
			}
		}
	}

	private void writeBits(int address, int count, BitSet values, BitSet set) {
		synchronized ( set ) {
			for ( int i = 0; i < count; i++ ) {
				set.set(address + i, set.get(i));
			}
			if ( set == coils ) {
				coilsTimestamp = System.currentTimeMillis();
			} else {
				discretesTimestamp = System.currentTimeMillis();
			}
		}
	}

	/**
	 * Read a set of discrete values.
	 * 
	 * <p>
	 * The returned set starts at index {@code 0}, meaning the indexes are
	 * shifted down by {@code address} positions.
	 * </p>
	 * 
	 * @param address
	 *        the starting register address
	 * @param count
	 *        the number of registers to read
	 * @return the result set
	 */
	public BitSet readDiscretes(int address, int count) {
		return readBits(address, count, discretes);
	}

	/**
	 * Read holding register data into a byte array.
	 * 
	 * @param address
	 *        the starting address
	 * @param count
	 *        the number of registers
	 * @return the register data, of length {@code count * 2}
	 */
	public byte[] readHoldings(int address, int count) {
		return readBytes(address, count, holdings);
	}

	/**
	 * Read holding register data into a byte array.
	 * 
	 * @param address
	 *        the starting address
	 * @param count
	 *        the number of registers
	 * @return the register data, of length {@code count * 2}
	 */
	public byte[] readInputs(int address, int count) {
		return readBytes(address, count, inputs);
	}

	/**
	 * Perform a thread-safe read action on a register block.
	 * 
	 * @param <T>
	 *        the result type
	 * @param blockType
	 *        the block type, must be {@code Holding} or {@code Input}
	 * @param action
	 *        the action to perform
	 * @return the action result
	 */
	public <T> T readRegisters(ModbusRegisterBlockType blockType, Function<ModbusData, T> action) {
		ModbusData data = null;
		switch (blockType) {
			case Holding:
				data = holdings.copy();
				break;

			case Input:
				data = inputs.copy();
				break;

			default:
				throw new IllegalArgumentException("Cannot read value from block type " + blockType);
		}
		return action.apply(data);
	}

	/**
	 * Perform a thread-safe read action on a register block.
	 * 
	 * @param <T>
	 *        the result type
	 * @param blockType
	 *        the block type, must be {@code Coil} or {@code Discrete}
	 * @param action
	 *        the action to perform
	 * @return the action result
	 */
	public <T> T readBits(ModbusRegisterBlockType blockType, Function<BitSet, T> action) {
		BitSet data = null;
		switch (blockType) {
			case Coil:
				data = coils;
				break;

			case Discrete:
				data = discretes;
				break;

			default:
				throw new IllegalArgumentException("Cannot read bits from block type " + blockType);
		}
		synchronized ( data ) {
			return action.apply(data);
		}
	}

	/**
	 * Write a holding register value.
	 * 
	 * @param address
	 *        the register address to save
	 * @param value
	 *        the value to save
	 */
	public void writeHolding(int address, short value) {
		writeHoldings(address, new short[] { value });
	}

	/**
	 * Write holding register values.
	 * 
	 * @param address
	 *        the starting register address to save
	 * @param values
	 *        the values to save
	 */
	public void writeHoldings(int address, short[] values) {
		if ( log.isDebugEnabled() ) {
			log.debug("Writing Holding registers {}-{} values: {}", address, address + values.length - 1,
					Arrays.toString(hexValues(values)));
		}
		writeRegisters(address, values, holdings);
	}

	private static String[] hexValues(short[] values) {
		if ( values == null ) {
			return null;
		}
		String[] result = new String[values.length];
		for ( int i = 0, len = values.length; i < len; i++ ) {
			result[i] = String.format("0x%04x", values[i]);
		}
		return result;
	}

	/**
	 * Write an input register value.
	 * 
	 * @param address
	 *        the register address to save
	 * @param value
	 *        the value to save
	 */
	public void writeInput(int address, short value) {
		writeInputs(address, new short[] { value });
	}

	/**
	 * Write input register values.
	 * 
	 * @param address
	 *        the starting register address to save
	 * @param values
	 *        the values to save
	 */
	public void writeInputs(int address, short[] values) {
		if ( log.isDebugEnabled() ) {
			log.debug("Writing Input registers {}-{} values: {}", address, address + values.length - 1,
					Arrays.toString(hexValues(values)));
		}
		writeRegisters(address, values, inputs);
	}

	private void writeRegisters(int address, short[] values, ModbusData data) {
		try {
			data.performUpdates(new ModbusDataUpdateAction() {

				@Override
				public boolean updateModbusData(MutableModbusData m) {
					m.saveDataArray(values, address);
					return true;
				}
			});
		} catch ( IOException e ) {
			// should not get here
			log.error("Error writing register {} data: {}", address, Arrays.toString(values), e);
		}
	}

	/**
	 * Perform a set of updates to saved register data.
	 * 
	 * @param blockType
	 *        the register block type to perform updates on; must be
	 *        {@code Holding} or {@code Input}
	 * @param action
	 *        the callback to perform the updates on
	 * @return this updated data
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public final ModbusData performRegisterUpdates(ModbusRegisterBlockType blockType,
			ModbusDataUpdateAction action) throws IOException {
		ModbusData d;
		switch (blockType) {
			case Holding:
				d = holdings;
				break;

			case Input:
				d = inputs;
				break;

			default:
				throw new IllegalArgumentException(
						"Cannot perform register updates to block type " + blockType);
		}
		return d.performUpdates(action);
	}

	/**
	 * Perform a set of updates to saved register data.
	 * 
	 * @param blockType
	 *        the register block type to perform updates on; must be
	 *        {@code Coil} or {@code Discrete}
	 * @param action
	 *        the callback to perform the updates on; return {@literal true} to
	 *        update the associated data timestamp
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public final void performBitUpdates(ModbusRegisterBlockType blockType, ModbusBitsUpdateAction action)
			throws IOException {
		BitSet bits;
		switch (blockType) {
			case Coil:
				bits = coils;
				break;

			case Discrete:
				bits = discretes;
				break;

			default:
				throw new IllegalArgumentException(
						"Cannot perform bit updates to block type " + blockType);
		}
		synchronized ( bits ) {
			boolean result = action.updateModbusBits(bits);
			if ( result ) {
				if ( blockType == ModbusRegisterBlockType.Coil ) {
					coilsTimestamp = System.currentTimeMillis();
				} else {
					discretesTimestamp = System.currentTimeMillis();
				}
			}
		}
	}

	private BitSet readBits(int address, int count, BitSet set) {
		BitSet result = new BitSet();
		synchronized ( set ) {
			for ( int a = address, i = 0, len = count; i < len; a++, i++ ) {
				result.set(i, set.get(a));
			}
		}
		return result;
	}

	private byte[] readBytes(int address, int count, ModbusData data) {
		byte[] result = new byte[count * 2];
		try {
			// use performUpdates for synchronization
			data.performUpdates(new ModbusDataUpdateAction() {

				@Override
				public boolean updateModbusData(MutableModbusData m) {
					for ( int i = 0, len = count; i < len; i++ ) {
						System.arraycopy(data.getBytes(i + address, 2), 0, result, i * 2, 2);
					}
					return true;
				}
			});
		} catch ( IOException e ) {
			// should not get here
		}
		return result;
	}

	/**
	 * Get the coil register data.
	 * 
	 * @return the coil registers, never {@literal null}
	 */
	public BitSet getCoils() {
		return coils;
	}

	/**
	 * Get the discrete register data.
	 * 
	 * @return the discrete registers, never {@literal null}
	 */
	public BitSet getDiscretes() {
		return discretes;
	}

	/**
	 * Get the input register data.
	 * 
	 * @return the input registers, never {@literal null}
	 */
	public ModbusData getInputs() {
		return inputs;
	}

	/**
	 * Get the holding register data.
	 * 
	 * @return the holding registers, never {@literal null}
	 */
	public ModbusData getHoldings() {
		return holdings;
	}

}
