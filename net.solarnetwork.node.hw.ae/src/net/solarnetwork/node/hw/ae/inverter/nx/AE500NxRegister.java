/* ==================================================================
 * AE500NxRegister.java - 22/04/2020 10:47:47 am
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

package net.solarnetwork.node.hw.ae.inverter.nx;

import static java.util.Arrays.asList;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Float32;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int32;
import static net.solarnetwork.node.io.modbus.ModbusDataType.StringAscii;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt32;
import static net.solarnetwork.node.io.modbus.ModbusReference.createAddressSet;
import java.util.HashSet;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;
import net.solarnetwork.util.IntRangeSet;

/**
 * Enumeration of Modbus register mappings for the AE 500 NX series inverter.
 * 
 * <p>
 * <b>Note</b> that the inverter uses <b>little endian</b> (or <b>least to most
 * significant</b> register word ordering.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 2.1
 */
public enum AE500NxRegister implements ModbusReference {

	/** The last restart of the server in Unix epoch seconds. */
	InfoLastRestartDate(0, UInt32),

	/** The number of seconds since the last restart of the server. */
	InfoUptime(2, UInt32),

	/** The current year, e.g. {@literal 2020}. */
	InfoClockYear(4, UInt16),

	/** The current month of the year (1 - 12). */
	InfoClockMonth(5, UInt16),

	/** The current day of the month (1 - 31). */
	InfoClockDay(6, UInt16),

	/** The current hour of the day (0 - 23). */
	InfoClockHour(7, UInt16),

	/** The current minute of the hour (0 - 59). */
	InfoClockMinute(8, UInt16),

	/** The current second of the minute, (0 - 59). */
	InfoClockSecond(9, UInt16),

	/** The current date in Unix epoch seconds. */
	InfoDate(10, UInt32),

	/** The current date as a string in the form {@literal HH:MM:SS}. */
	InfoTimeString(12, 4, StringAscii),

	/** The active power total, in kW. */
	InverterActivePowerTotal(16, Float32),

	/** The AC line frequency, in Hz. */
	InverterFrequency(18, Float32),

	/** The PV voltage, in V. */
	InverterPvVoltage(20, Int32),

	/** The PV current, in A. */
	InverterPvCurrent(22, Float32),

	/** The common mode voltage, in V. */
	InverterVoltageCommonMode(24, Float32),

	/** The ambient temperature, in degrees C. */
	InverterTemperatureAmbient(26, Float32),

	/** The coolant temperature, in degrees C. */
	InverterTemperatureCoolant(28, Float32),

	/** The line reactor temperature, in degrees C. */
	InverterTemperatureReactor(30, Float32),

	/** The cabinet temperature, in degrees C. */
	InverterTemperatureCabinet(32, Float32),

	/** The bus voltage, in V. */
	InverterBusVoltage(34, Int32),

	/** The ground current, in A. */
	InverterGroundCurrent(36, Float32),

	/** The reactive power, in kVAR. */
	InverterReactivePowerTotal(38, Float32),

	/** Active faults bitmask 0 - 31, see {@link AE500NxFault1}. */
	StatusFaults1(40, UInt32),

	/** Active faults bitmask 32 - 63, see {@link AE500NxFault2}. */
	StatusFaults2(42, UInt32),

	/** Active faults bitmask 64 - 95, see {@link AE500NxFault3}. */
	StatusFaults3(44, UInt32),

	/** System status bitmask, see {@link AE500NxSystemStatus}. */
	StatusSystemStatus(46, UInt32),

	/** Active warnings bitmask 0 - 31, see {@link AE500NxWarning1}. */
	StatusWarnings1(48, UInt32),

	/** System limits bitmask 0 - 31, see {@link AE500NxSystemLimit}. */
	StatusSystemLimits(54, UInt32),

	/** The common mode current, in A. */
	InverterCurrentCommonMode(56, Float32),

	/** Software versions string. */
	InfoSoftwareVersions(60, 64, StringAscii),

	/** IDS (FE) software version. */
	InfoFeVersion(124, 32, StringAscii),

	/** The serial number. */
	InfoSerialNumber(156, 32, StringAscii),

	/** Lifetime energy delivered, in kWh. */
	InverterActiveEnergyDelivered(204, Float32);

	private static final IntRangeSet CONFIG_REGISTER_ADDRESS_SET = createAddressSet(
			AE500NxRegister.class, new HashSet<>(asList("Info"))).immutableCopy();
	private static final IntRangeSet INVERTER_REGISTER_ADDRESS_SET = createAddressSet(
			AE500NxRegister.class, new HashSet<>(asList("Inverter"))).immutableCopy();
	private static final IntRangeSet STATUS_REGISTER_ADDRESS_SET = createAddressSet(
			AE500NxRegister.class, new HashSet<>(asList("Status"))).immutableCopy();

	private final int address;
	private final int length;
	private final ModbusDataType dataType;

	private AE500NxRegister(int address, ModbusDataType dataType) {
		this(address, 0, dataType);
	}

	private AE500NxRegister(int address, int length, ModbusDataType dataType) {
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
		return ModbusReadFunction.ReadHoldingRegister;
	}

	@Override
	public int getWordLength() {
		return (this.length > 0 ? this.length : dataType.getWordLength());
	}

	/**
	 * Get an address range set that covers all the registers defined in this
	 * enumeration.
	 * 
	 * @return the range set
	 */
	public static IntRangeSet getRegisterAddressSet() {
		IntRangeSet s = new IntRangeSet(CONFIG_REGISTER_ADDRESS_SET);
		s.addAll(INVERTER_REGISTER_ADDRESS_SET);
		s.addAll(STATUS_REGISTER_ADDRESS_SET);
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
	 * Get an address range set that covers all the inverter registers defined
	 * in this enumeration.
	 * 
	 * <p>
	 * Note the ranges in this set represent <i>inclusive</i> starting addresses
	 * and ending addresses.
	 * </p>
	 * 
	 * @return the range set
	 */
	public static IntRangeSet getInverterRegisterAddressSet() {
		return INVERTER_REGISTER_ADDRESS_SET;
	}

	/**
	 * Get an address range set that covers all the status registers defined in
	 * this enumeration.
	 * 
	 * <p>
	 * Note the ranges in this set represent <i>inclusive</i> starting addresses
	 * and ending addresses.
	 * </p>
	 * 
	 * @return the range set
	 */
	public static IntRangeSet getStatusRegisterAddressSet() {
		return STATUS_REGISTER_ADDRESS_SET;
	}

}
