/* ==================================================================
 * PowerGatePlusRegister.java - 11/09/2019 10:22:21 am
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
import static net.solarnetwork.node.io.modbus.IntRangeSetUtils.createRegisterAddressSet;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Bytes;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import java.util.HashSet;
import bak.pcj.set.IntRangeSet;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;

/**
 * Enumeration of Modbus register mappings for PowerGate Plus Series inverters.
 * 
 * @author matt
 * @version 1.0
 */
public enum PowerGatePlusRegister implements ModbusReference {

	/**
	 * The Digital Power Control Board firmware identification, in XXX.YY form.
	 */
	InfoDpcbFirmwareVersion(30010, UInt16),

	/** Faults 0, see {@link PowerGateFault0}. */
	StatusFault0Bitmask(30011, UInt16),

	/** Faults 1, see {@link PowerGateFault1}. */
	StatusFault1Bitmask(30012, UInt16),

	/** Faults 2, see {@link PowerGateFault2}. */
	StatusFault2Bitmask(30013, UInt16),

	/** Faults 3, see {@link PowerGateFault3}. */
	StatusFault3Bitmask(30014, UInt16),

	/** Faults 4, see {@link PowerGateFault4}. */
	StatusFault4Bitmask(30015, UInt16),

	/** Faults 5, see {@link PowerGateFault5}. */
	StatusFault5Bitmask(30016, UInt16),

	/** Faults 6, see {@link PowerGateFault6}. */
	StatusFault6Bitmask(30017, UInt16),

	/** Total number of active faults. */
	StatusFaultCount(30018, UInt16),

	/**
	 * Chesksum of the loaded program, see
	 * {@link PowerGateFault2#ProgramChecksum}.
	 */
	InfoProgramChecksum(30019, UInt16),

	/**
	 * Checksum of the non-volatile parameters in memory, see
	 * {@link PowerGateFault2#DataCopy1Checksum}.
	 */
	InfoParameterChecksum(30020, UInt16),

	/** The DC input voltage, in volts. */
	InverterDcVoltage(30021, Int16),

	/** The DC link voltage, in volts. */
	InverterDcLinkVoltage(30022, Int16),

	/** The DC link current, in amps. */
	InverterDcLinkCurrent(30023, Int16),

	/** The DC ground current, in amps. */
	InverterDcGroundCurrent(30024, Int16),

	/** AC line current, phase A, in amps. */
	InverterCurrentPhaseA(30033, Int16),

	/** AC line current, phase B, in amps. */
	InverterCurrentPhaseB(30034, Int16),

	/** AC line current, phase C, in amps. */
	InverterCurrentPhaseC(30035, Int16),

	/** AC line current average of all phases, in amps. */
	InverterCurrentAverage(30036, Int16),

	/** AC neutral line current, in deci-amps. */
	InverterCurrentNeutral(30037, Int16),

	/** AC line voltage, phase A, in amps. */
	InverterVoltagePhaseA(30038, Int16),

	/** AC line voltage, phase B, in volts. */
	InverterVoltagePhaseB(30039, Int16),

	/** AC line voltage, phase C, in volts. */
	InverterVoltagePhaseC(30040, Int16),

	/** AC line voltage average of all phases, in volts. */
	InverterVoltageAverage(30041, Int16),

	/** Line voltage unbalance, in 0-100 percentage. */
	InverterLineVoltageUnbalance(30042, Int16),

	/** Line current unbalance, in 0-100 percentage. */
	InverterLineCurrentUnbalance(30043, Int16),

	/** DC input power, in hecto-watts. */
	InverterDcPower(30044, Int16),

	/** AC output real power, in hecto-watts. */
	InverterActivePowerTotal(30045, Int16),

	/** AC output reactive power, in hecto-volt-amps-reactive. */
	InverterReactivePowerTotal(30046, Int16),

	/** AC output apparent power, in hecto-volt-amps. */
	InverterApparentPowerTotal(30047, Int16),

	/** AC power factor, from -1..1 in milli scale. */
	InverterPowerFactor(30048, Int16),

	/** Ground impedance, in kilo-Ohms. */
	InverterGroundImpedance(30050, Int16),

	/** Total active energy delivered, least 3 significant digits, in kWh. */
	InverterActiveEnergyDeliveredKilo(30132, Int16),

	/**
	 * Total active energy delivered, least 5 most significant digits, in MWh.
	 */
	InverterActiveEnergyDeliveredMega(30133, Int16),

	/** Active energy delivered today, in kWh. */
	InverterActiveEnergyDeliveredToday(30134, Int16),

	/** The AC frequency, in Hz. */
	InverterFrequency(30176, Int16),

	/** The operating state, see {@link PowerGateOperatingState}. */
	StatusOperatingState(30280, UInt16),

	/** Air temperature inside the general enclosure, in degrees celsius. */
	InternalInternalAirTemperature(30281, Int16),

	/** Air temperature inside the inverter enclosure, in degrees celsius. */
	InverterInternalAirTemperature(30282, Int16),

	/** Temperature of inverter heatsink 1, in degrees celsius. */
	InverterHeatsinkTemperature1(30283, Int16),

	/** Temperature of inverter heatsink 2, in degrees celsius. */
	InverterHeatsinkTemperature2(30284, Int16),

	/** Temperature of inverter heatsink 3, in degrees celsius. */
	InverterHeatsinkTemperature3(30285, Int16),

	/** Temperature of inverter heatsink 4, in degrees celsius. */
	InverterHeatsinkTemperature4(30286, Int16),

	/** Temperature of inverter heatsink 5, in degrees celsius. */
	InverterHeatsinkTemperature5(30287, Int16),

	/** Temperature of inverter heatsink 6, in degrees celsius. */
	InverterHeatsinkTemperature6(30288, Int16),

	/** Highest heatsink 1-3 temperature, in degrees celsius. */
	InverterHeatsinkMaximumTemperature1(30289, Int16),

	/** Fan speed for first/only fan, as integer percentage 0..100. */
	InverterHeatsinkFanSpeed1(30290, Int16),

	/** Highest heatsink 4-6 temperature, in degrees celsius. */
	InverterHeatsinkMaximumTemperature2(30291, Int16),

	/** Fan speed for second fan, as integer percentage 0-100. */
	InverterHeatsinkFanSpeed2(30292, Int16),

	/**
	 * Serial number, as 4 16-bit numbers where the 3rd number is code for 1 =
	 * A, 2 = B, etc.
	 */
	InfoSerialNumber(30302, 4, Bytes),

	/** Number of temperature sensors (0..8). */
	InfoTemperatureSensorCount(30324, UInt16),

	/** Number of PV zones available (0..32). */
	InfoZoneCount(30328, UInt16),

	/** Rated AC output power, in kilowatts. */
	InfoRatedOutputPower(30311, UInt16),

	/** Rated AC frequency, in Hz. */
	InfoRatedLineFrequency(30312, UInt16),

	/** Rated AC line-to-line output voltage, in volts. */
	InfoRatedLineVoltage(30313, UInt16),

	/**
	 * Tap setting on the inverter side of the output transformer in percent of
	 * nominal, as an integer percentage -100..100.
	 */
	InfoVoltageTapSetting(30315, Int16),

	/** Perform a function, see {@link PowerGateRemoteCommand}. */
	ControlRemoteCommand(40437, UInt16),

	/** Set the access code to access restricted registers. */
	ControlAccessCode(40476, UInt16);

	private final int address;
	private final int length;
	private final ModbusDataType dataType;

	private PowerGatePlusRegister(int address, ModbusDataType dataType) {
		this(address, 0, dataType);
	}

	private PowerGatePlusRegister(int address, int length, ModbusDataType dataType) {
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

	private static final IntRangeSet CONFIG_REGISTER_ADDRESS_SET = createConfigRegisterAddressSet();
	private static final IntRangeSet INVERTER_REGISTER_ADDRESS_SET = createInverterRegisterAddressSet();
	private static final IntRangeSet CONTROL_REGISTER_ADDRESS_SET = createControlRegisterAddressSet();

	private static IntRangeSet createConfigRegisterAddressSet() {
		return createRegisterAddressSet(PowerGatePlusRegister.class,
				new HashSet<>(asList("Info", "Config")));
	}

	private static IntRangeSet createInverterRegisterAddressSet() {
		return createRegisterAddressSet(PowerGatePlusRegister.class,
				new HashSet<>(asList("PowerControl", "Status")));
	}

	private static IntRangeSet createControlRegisterAddressSet() {
		return createRegisterAddressSet(PowerGatePlusRegister.class, new HashSet<>(asList("Control")));
	}

	/**
	 * Get an address range set that covers all the registers defined in this
	 * enumeration <b>except<b> controls.
	 * 
	 * @return the range set
	 */
	public static IntRangeSet getRegisterAddressSet() {
		IntRangeSet s = new IntRangeSet(CONFIG_REGISTER_ADDRESS_SET);
		s.addAll(INVERTER_REGISTER_ADDRESS_SET);
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
		return (IntRangeSet) CONFIG_REGISTER_ADDRESS_SET.clone();
	}

	/**
	 * Get an address range set that covers all the power control registers
	 * defined in this enumeration.
	 * 
	 * <p>
	 * Note the ranges in this set represent <i>inclusive</i> starting addresses
	 * and ending addresses.
	 * </p>
	 * 
	 * @return the range set
	 */
	public static IntRangeSet getInverterRegisterAddressSet() {
		return (IntRangeSet) INVERTER_REGISTER_ADDRESS_SET.clone();
	}

	/**
	 * Get an address range set that covers all the control registers defined in
	 * this enumeration.
	 * 
	 * <p>
	 * Note the ranges in this set represent <i>inclusive</i> starting addresses
	 * and ending addresses.
	 * </p>
	 * 
	 * @return the range set
	 */
	public static IntRangeSet getControlRegisterAddressSet() {
		return (IntRangeSet) CONTROL_REGISTER_ADDRESS_SET.clone();
	}
}
