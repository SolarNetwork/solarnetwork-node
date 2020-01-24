/* ==================================================================
 * SubCombinerRegister.java - 12/09/2019 6:41:43 am
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

import static java.util.Arrays.asList;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt32;
import static net.solarnetwork.node.io.modbus.ModbusReference.createAddressSet;
import java.util.HashSet;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;
import net.solarnetwork.util.IntRangeSet;

/**
 * Enumeration of Modbus register mappings for Smart SubCombiner and Solstice
 * Subcombiner devices.
 * 
 * @author matt
 * @version 2.0
 */
public enum SubCombinerRegister implements ModbusReference {

	/** The firmware identification, in {@literal XXX.YY} form. */
	InfoFirmwareVersion(30010, UInt16),

	/** The serial number, in least-to-most significant word order. */
	InfoSerialNumber(30011, UInt32),

	/** Faults 0, see {@link SubCombinerFault0}. */
	StatusFault0Bitmask(30040, UInt16),

	/** Faults 1, see {@link SubCombinerFault1}. */
	StatusFault1Bitmask(30041, UInt16),

	/** Count of string DC/DC converter communication errors since startup. */
	StatusSsbStatus1(30042, UInt16),

	/** Bitmask for each string set if DC/DC converter is communicating. */
	StatusSsbStatus2(30043, UInt16),

	/**
	 * Set to {@literal 0} if operating normally, {@literal 1} if calibrating.
	 */
	StatusCombinerStatus(30045, UInt16),

	/** Internal temperature of device, in centi-degrees celsius. */
	CombinerInternalTemperature(30052, Int16),

	/**
	 * Highest recorded internal temperature of device, in centi-degrees
	 * celsius.
	 */
	CombinerInternalTemperatureHigh(30070, Int16),

	/**
	 * Lowest recorded internal temperature of device, in centi-degrees celsius.
	 */
	CombinerInternalTemperatureLow(30071, Int16),

	/** External temperature 1, in centi-degrees celsius. */
	CombinerExternalTemperature1(30055, Int16),

	/** Highest recorded external temperature 1, in centi-degrees celsius. */
	CombinerExternalTemperature1High(30072, Int16),

	/** Lowest recorded external temperature 1, in centi-degrees celsius. */
	CombinerExternalTemperature1Low(30073, Int16),

	/** External temperature 2, in centi-degrees celsius. */
	CombinerExternalTemperature2(30058, Int16),

	/** Highest recorded internal temperature 2, in centi-degrees celsius. */
	CombinerExternalTemperature2High(30074, Int16),

	/** Lowest recorded internal temperature 2, in centi-degrees celsius. */
	CombinerExternalTemperature2Low(30075, Int16),

	/**
	 * The percentage 0..100 that a single string must be out of tolerance from
	 * the average to declare a fault.
	 */
	StatusInputCurrentDeviationTolerance(30098, Int16),

	/**
	 * The minimum current for a string to have present in order to declare a
	 * fault, in centi-amps.
	 */
	StatusInputCurrentDeviationMinimum(30099, Int16),

	/** String 1 output current, in centi-amps. */
	CombinerDcOutputCurrent1(36061, Int16),

	/** String 2 output current, in centi-amps. */
	CombinerDcOutputCurrent2(36062, Int16),

	/** String 3 output current, in centi-amps. */
	CombinerDcOutputCurrent3(36063, Int16),

	/** String 4 output current, in centi-amps. */
	CombinerDcOutputCurrent4(36064, Int16),

	/** String 5 output current, in centi-amps. */
	CombinerDcOutputCurrent5(36065, Int16),

	/** String 6 output current, in centi-amps. */
	CombinerDcOutputCurrent6(36066, Int16),

	/** String 7 output current, in centi-amps. */
	CombinerDcOutputCurrent7(36067, Int16),

	/** String 8 output current, in centi-amps. */
	CombinerDcOutputCurrent8(36068, Int16),

	/** String 9 output current, in centi-amps. */
	CombinerDcOutputCurrent9(36069, Int16),

	/** String 10 output current, in centi-amps. */
	CombinerDcOutputCurrent10(36070, Int16),

	/** String 11 output current, in centi-amps. */
	CombinerDcOutputCurrent11(36071, Int16),

	/** String 12 output current, in centi-amps. */
	CombinerDcOutputCurrent12(36072, Int16),

	/** String 1 output voltage, in deci-volts. */
	CombinerDcOutputVoltage1(36101, Int16),

	/** String 2 output voltage, in deci-volts. */
	CombinerDcOutputVoltage2(36102, Int16),

	/** String 3 output voltage, in deci-volts. */
	CombinerDcOutputVoltage3(36103, Int16),

	/** String 4 output voltage, in deci-volts. */
	CombinerDcOutputVoltage4(36104, Int16),

	/** String 5 output voltage, in deci-volts. */
	CombinerDcOutputVoltage5(36105, Int16),

	/** String 6 output voltage, in deci-volts. */
	CombinerDcOutputVoltage6(36106, Int16),

	/** String 7 output voltage, in deci-volts. */
	CombinerDcOutputVoltage7(36107, Int16),

	/** String 8 output voltage, in deci-volts. */
	CombinerDcOutputVoltage8(36108, Int16),

	/** String 9 output voltage, in deci-volts. */
	CombinerDcOutputVoltage9(36109, Int16),

	/** String 10 output voltage, in deci-volts. */
	CombinerDcOutputVoltage10(36110, Int16),

	/** String 11 output voltage, in deci-volts. */
	CombinerDcOutputVoltage11(36111, Int16),

	/** String 12 output voltage, in deci-volts. */
	CombinerDcOutputVoltage12(36112, Int16),

	/** String 1 input power, in watts. */
	CombinerDcInputPower1(36131, Int16),

	/** String 2 input power, in watts. */
	CombinerDcInputPower2(36132, Int16),

	/** String 3 input power, in watts. */
	CombinerDcInputPower3(36133, Int16),

	/** String 4 input power, in watts. */
	CombinerDcInputPower4(36134, Int16),

	/** String 5 input power, in watts. */
	CombinerDcInputPower5(36135, Int16),

	/** String 6 input power, in watts. */
	CombinerDcInputPower6(36136, Int16),

	/** String 7 input power, in watts. */
	CombinerDcInputPower7(36137, Int16),

	/** String 8 input power, in watts. */
	CombinerDcInputPower8(36138, Int16),

	/** String 9 input power, in watts. */
	CombinerDcInputPower9(36139, Int16),

	/** String 10 input power, in watts. */
	CombinerDcInputPower10(36140, Int16),

	/** String 11 input power, in watts. */
	CombinerDcInputPower11(36141, Int16),

	/** String 12 input power, in watts. */
	CombinerDcInputPower12(36142, Int16),

	/** String 1 input power, in watts. */
	CombinerDcOutputPower1(36161, Int16),

	/** String 2 input power, in watts. */
	CombinerDcOutputPower2(36162, Int16),

	/** String 3 input power, in watts. */
	CombinerDcOutputPower3(36163, Int16),

	/** String 4 input power, in watts. */
	CombinerDcOutputPower4(36164, Int16),

	/** String 5 input power, in watts. */
	CombinerDcOutputPower5(36165, Int16),

	/** String 6 input power, in watts. */
	CombinerDcOutputPower6(36166, Int16),

	/** String 7 input power, in watts. */
	CombinerDcOutputPower7(36167, Int16),

	/** String 8 input power, in watts. */
	CombinerDcOutputPower8(36168, Int16),

	/** String 9 input power, in watts. */
	CombinerDcOutputPower9(36169, Int16),

	/** String 10 input power, in watts. */
	CombinerDcOutputPower10(36170, Int16),

	/** String 11 input power, in watts. */
	CombinerDcOutputPower11(36171, Int16),

	/** String 12 input power, in watts. */
	CombinerDcOutputPower12(36172, Int16);

	private final int address;
	private final int length;
	private final ModbusDataType dataType;

	private SubCombinerRegister(int address, ModbusDataType dataType) {
		this(address, 0, dataType);
	}

	private SubCombinerRegister(int address, int length, ModbusDataType dataType) {
		this.address = address;
		this.length = length;
		this.dataType = dataType;
	}

	@Override
	public int getAddress() {
		return address;
	}

	@Override
	public ModbusDataType getDataType() {
		return dataType;
	}

	@Override
	public ModbusReadFunction getFunction() {
		return ModbusReadFunction.ReadInputRegister;
	}

	@Override
	public int getWordLength() {
		return (this.length > 0 ? this.length : dataType.getWordLength());
	}

	private static final IntRangeSet CONFIG_REGISTER_ADDRESS_SET = createAddressSet(
			SubCombinerRegister.class, new HashSet<>(asList("Info"))).immutableCopy();
	private static final IntRangeSet COMBINER_REGISTER_ADDRESS_SET = createAddressSet(
			SubCombinerRegister.class, new HashSet<>(asList("Combiner", "Status"))).immutableCopy();

	/**
	 * Get an address range set that covers all the registers defined in this
	 * enumeration.
	 * 
	 * @return the range set
	 */
	public static IntRangeSet getRegisterAddressSet() {
		IntRangeSet s = new IntRangeSet(CONFIG_REGISTER_ADDRESS_SET);
		s.addAll(COMBINER_REGISTER_ADDRESS_SET);
		return s;
	}

	/**
	 * Get an address range set that covers all the configuration and info
	 * registers defined in this enumeration.
	 * 
	 * <p>
	 * Note the ranges in this set represent <i>inclusive</i> starting addresses
	 * and ending addresses.
	 * </p>
	 * 
	 * @return the range set
	 */
	public static IntRangeSet getConfigRegisterAddressSet() {
		return CONFIG_REGISTER_ADDRESS_SET;
	}

	/**
	 * Get an address range set that covers all the subcombiner registers
	 * defined in this enumeration.
	 * 
	 * <p>
	 * Note the ranges in this set represent <i>inclusive</i> starting addresses
	 * and ending addresses.
	 * </p>
	 * 
	 * @return the range set
	 */
	public static IntRangeSet getCombinerRegisterAddressSet() {
		return COMBINER_REGISTER_ADDRESS_SET;
	}

}
