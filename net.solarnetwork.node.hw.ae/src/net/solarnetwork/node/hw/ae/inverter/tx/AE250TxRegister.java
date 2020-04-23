/* ==================================================================
 * AE250TxRegister.java - 27/07/2018 2:49:04 PM
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

package net.solarnetwork.node.hw.ae.inverter.tx;

import static java.util.Arrays.asList;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Float32;
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
 * Enumeration of Modbus register mappings for the AE 250 TX series inverter.
 * 
 * @author matt
 * @version 2.1
 */
public enum AE250TxRegister implements ModbusReference {

	/** Serial number. */
	InfoInverterIdNumber(0, 8, StringAscii),

	/** Firmware revision. */
	InfoFirmwareVersion(8, 4, StringAscii),

	/** Map version. */
	InfoMapVersion(13, UInt16),

	/** Meter configuration bitmap. */
	InfoInverterConfiguration(14, UInt16),

	/** Serial number. */
	InfoSerialNumber(15, 10, StringAscii),

	/** Rated power, in kW. */
	InfoRatedPower(25, UInt16),

	/** AC line voltage with neutral, phase A, in V. */
	InverterVoltageLineNeutralPhaseA(1000, Float32),

	/** AC line voltage with neutral, phase B, in V. */
	InverterVoltageLineNeutralPhaseB(1002, Float32),

	/** AC line voltage with neutral, phase C, in V. */
	InverterVoltageLineNeutralPhaseC(1004, Float32),

	/** AC line voltage with neutral, phase A, in A. */
	InverterCurrentPhaseA(1006, Float32),

	/** AC line voltage with neutral, phase B, in A. */
	InverterCurrentPhaseB(1008, Float32),

	/** AC line voltage with neutral, phase C, in A. */
	InverterCurrentPhaseC(1010, Float32),

	/** The DC input voltage, in V. */
	InverterDcVoltage(1012, Float32),

	/** The DC input current, in A. */
	InverterDcCurrent(1014, Float32),

	/** The AC line frequency, in Hz. */
	InverterFrequency(1016, Float32),

	/** The meter active power total, in kW. */
	InverterActivePowerTotal(1018, Float32),

	/** The meter active energy delivered, in kWh. */
	InverterActiveEnergyDelivered(1020, UInt32),

	/** The PV input voltage, in V. */
	InverterPvVoltage(1022, Float32),

	/** The DC power output, in kW. */
	InverterDcPower(1024, Float32),

	StatusOperatingState(2100, UInt16),

	StatusMainFault(2101, UInt16),

	StatusDriveFault(2102, UInt16),

	StatusVoltageFault(2103, UInt16),

	StatusGridFault(2104, UInt16),

	StatusTemperatureFault(2105, UInt16),

	StatusSystemFault(2106, UInt16),

	StatusSystemWarnings(2107, UInt16),

	StatusPvMonitoringStatus(2108, UInt16);

	private static final IntRangeSet CONFIG_REGISTER_ADDRESS_SET = createAddressSet(
			AE250TxRegister.class, new HashSet<>(asList("Config", "Info"))).immutableCopy();
	private static final IntRangeSet INVERTER_REGISTER_ADDRESS_SET = createAddressSet(
			AE250TxRegister.class, new HashSet<>(asList("Inverter"))).immutableCopy();
	private static final IntRangeSet STATUS_REGISTER_ADDRESS_SET = createAddressSet(
			AE250TxRegister.class, new HashSet<>(asList("Status"))).immutableCopy();

	private final int address;
	private final int length;
	private final ModbusDataType dataType;

	private AE250TxRegister(int address, ModbusDataType dataType) {
		this(address, 0, dataType);
	}

	private AE250TxRegister(int address, int length, ModbusDataType dataType) {
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
