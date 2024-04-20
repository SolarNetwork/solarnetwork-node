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

package net.solarnetwork.node.io.modbus.server.domain;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.BitSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusDataUtils;

/**
 * Data for a Modbus register set.
 *
 * @author matt
 * @version 2.0
 */
public class ModbusRegisterData {

	private static final Logger log = LoggerFactory.getLogger(ModbusRegisterData.class);

	private final BitSet coils;
	private final BitSet discretes;
	private final ModbusData inputs;
	private final ModbusData holdings;

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
	public void writeBit(RegisterBlockType blockType, int address, boolean value) {
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
	public void writeValue(RegisterBlockType blockType, ModbusDataType dataType, int address, int count,
			Object value) {
		short[] dataValue = encodeValue(dataType, address, count, value);
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
					result = limitLength(ModbusDataUtils.encodeBytes((byte[]) value), count);
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
					result = limitLength(ModbusDataUtils.encodeBytes(strBytes), count);
				} catch ( UnsupportedEncodingException e ) {
					// should not get here
				}
				break;

			default:
				if ( value instanceof Number ) {
					result = ModbusDataUtils.encodeNumber(dataType, (Number) value);
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
		}
	}

	private void writeBits(int address, int count, BitSet values, BitSet set) {
		synchronized ( set ) {
			for ( int i = 0; i < count; i++ ) {
				set.set(address + i, values.get(i));
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
	 * @return the register data
	 */
	public short[] readHoldings(int address, int count) {
		return readShorts(address, count, holdings);
	}

	/**
	 * Read holding register data into a byte array.
	 *
	 * @param address
	 *        the starting address
	 * @param count
	 *        the number of registers
	 * @return the register data
	 */
	public short[] readInputs(int address, int count) {
		return readShorts(address, count, inputs);
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

	private BitSet readBits(int address, int count, BitSet set) {
		BitSet result = new BitSet();
		synchronized ( set ) {
			for ( int a = address, i = 0, len = count; i < len; a++, i++ ) {
				result.set(i, set.get(a));
			}
		}
		return result;
	}

	private short[] readShorts(int address, int count, ModbusData data) {
		short[] result = new short[count];
		try {
			// use performUpdates for synchronization
			data.performUpdates(new ModbusDataUpdateAction() {

				@Override
				public boolean updateModbusData(MutableModbusData m) {
					for ( int i = 0, len = count; i < len; i++ ) {
						result[i] = data.dataRegisters().getValue(i + address);
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
