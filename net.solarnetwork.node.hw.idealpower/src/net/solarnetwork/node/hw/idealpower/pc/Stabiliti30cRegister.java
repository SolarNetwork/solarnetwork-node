/* ==================================================================
 * Stabiliti30cRegister.java - 27/08/2019 3:06:01 pm
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

package net.solarnetwork.node.hw.idealpower.pc;

import static java.util.Arrays.asList;
import static net.solarnetwork.node.io.modbus.IntRangeSetUtils.createRegisterAddressSet;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.StringAscii;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt32;
import java.util.HashSet;
import bak.pcj.set.IntRangeSet;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;

/**
 * Enumeration of Modbus register mappings for Stabiliti Series power conversion
 * system devices.
 * 
 * @author matt
 * @version 1.0
 */
public enum Stabiliti30cRegister implements ModbusReference {

	/** Firmware version. */
	InfoFirmwareVersion(296, UInt16),

	/** Build version. */
	InfoBuildVersion(297, UInt16),

	/** Build version. */
	InfoCommsVersion(2087, UInt16),

	/** Build version. */
	InfoCommsBuildVersion(2088, UInt16),

	/** Serial number. */
	InfoSerialNumber(2001, 8, StringAscii),

	/** P1 port type, see {@link Stabiliti30cAcPortType}. */
	ConfigP1PortType(64, UInt16),

	/** P1 real power, positive when exporting to grid, in deca-watts. */
	PowerControlP1RealPower(119, Int16),

	/** P2 voltage, positive to negative, in volts. */
	PowerControlP2Voltage(173, Int16),

	/**
	 * P2 power, positive when exporting to battery (charging), in deca-watts.
	 */
	PowerControlP2Power(185, Int16),

	/** P2 current, in ceni-amps. */
	PowerControlP2Current(186, Int16),

	/** P3 voltage, positive to negative, in volts. */
	PowerControlP3Voltage(237, Int16),

	/** P3 power, negative when importing from PV, in deca-watts. */
	PowerControlP3Power(249, Int16),

	/** P3 current, in centi-amps. */
	PowerControlP3Current(250, Int16),

	/** The active fault group 0, see {@link Stabiliti30cFault0}. */
	StatusFaultActive0(16, UInt16),

	/** The active fault group 1, see {@link Stabiliti30cFault1}. */
	StatusFaultActive1(17, UInt16),

	/** The active fault group 2, see {@link Stabiliti30cFault2}. */
	StatusFaultActive2(18, UInt16),

	/** The active fault group 3, see {@link Stabiliti30cFault3}. */
	StatusFaultActive3(19, UInt16),

	/** System operating mode, see {@link Stabiliti30cOperatingMode}. */
	StatusOperatingMode(267, UInt16),

	/** System status, see {@link Stabiliti30cSystemStatus}. */
	StatusSystem(298, UInt16),

	/** System info, see {@link Stabiliti30cSystemInfo}. */
	StatusInfo(438, UInt16),

	/** Countdown watchdog, in seconds, or {@literal 0} to disable. */
	ControlWatchdogSeconds(40, Int16),

	/** Start manual mode by setting to {@literal 1}. */
	ControlUserStart(263, UInt16),

	/** Stop manual mode by setting to {@literal 1}. */
	ControlUserSetop(264, UInt16),

	/** P1 control method, see {@link Stabiliti30cAcControlMethod}. */
	ControlP1ControlMethod(65, UInt16),

	/**
	 * P1 real power setpoint while voltage-following, if the P1 control method
	 * is GPWR or FPWR, in deca-watts.
	 */
	ControlP1RealPowerSetpoint(68, UInt16),

	/**
	 * P1 line-to-line voltage setpoint for voltage-forming mode, if the P1
	 * control method is FPWR, in volts.
	 */
	ControlP1VoltageSetpoint(71, Int16),

	/**
	 * P1 output frequency setpoint for voltage-forming mode, if the P1 control
	 * method is FPWR, in milli-hertz.
	 */
	ControlP1FrequencySetpoint(72, Int16),

	/** Soft current limit, in centi-amps. */
	ControlP1CurrentLimit(90, Int16),

	/** P2 control method, see {@link Stabiliti30cDcControlMethod}. */
	ControlP2ControlMethod(129, UInt16),

	/**
	 * P2 current setpoint if the P2 control method is Current, in centi-amps.
	 */
	ControlP2CurrentSetpoint(132, Int16),

	/** P2 power setpoint, if the P2 control method is Power, in deca-watts. */
	ControlP2PowerSetpoint(133, Int16),

	/** P2 maximum DC operating voltage, in volts. */
	ControlP2VoltageMax(141, Int16),

	/** P2 minimum DC operating voltage, in volts. */
	ControlP2VoltageMin(142, Int16),

	/** P2 import (discharge) soft power limit, in deca-watts. */
	ControlP2ImportPowerLimit(152, Int16),

	/** P2 export (charge) soft power limit, in deca-watts. */
	ControlP2ExportPowerLimit(153, Int16),

	/** P2 soft current limit absolute value, in centi-amps. */
	ControlP2CurrentLimit(154, UInt16),

	/** Command, see {@link Stabiliti30cCommand}. */
	ControlCommand(2000, UInt16),

	/** P3 control method, see {@link Stabiliti30cDcControlMethod}. */
	ControlP3ControlMethod(193, UInt16),

	/**
	 * P3 MPPT start time setpoint, if the P3 control method is MPPT, in minutes
	 * from midnight (1 - 1440).
	 */
	ControlP3MpptStart(199, UInt16),

	/**
	 * P3 MPPT stop time setpoint, if the P3 control method is MPPT, in minutes
	 * from midnight (1 - 1440).
	 */
	ControlP3MpptStop(200, UInt16),

	/** P3 MPPT minimum voltage, if the P3 control method is MPPT, in volts. */
	ControlP3MpptVoltageMin(202, UInt16),

	/** P3 maximum DC operating voltage, in volts. */
	ControlP3VoltageMax(205, Int16),

	/** P3 minimum DC operating voltage, in volts. */
	ControlP3VoltageMin(206, Int16),

	/** P3 import soft power limit, in deca-watts. */
	ControlP3ImportPowerLimit(216, Int16),

	/** P3 soft current limit absolute value, in centi-amps. */
	ControlP3CurrentLimit(218, UInt16),

	/**
	 * Set which fault to show information for in the other {@code FaultStatus*}
	 * registers.
	 */
	FaultControlIndex(0, UInt16),

	/** For over/under limit faults, the limit value that was exceeded. */
	FaultStatusLimit(1, UInt16),

	/**
	 * The measured value that exceeded a limit and triggered the fault flag.
	 */
	FaultStatusValue(2, UInt16),

	/** The number of times this fault occurred since the last fault reset. */
	FaultStatusCount(3, UInt16),

	/**
	 * The Unix timestamp of the most recent occurrence of this type of fault,
	 * in least-to-most-significant word order.
	 */
	FaultStatusUtime(4, UInt32),

	/**
	 * In the case a fault can apply to multiple sources, the source that
	 * triggered the fault.
	 */
	FaultStatusSelector(8, UInt16),

	/**
	 * A fault status combination register.
	 * 
	 * <ul>
	 * <li>Bits 0-2 represent the fault severity, see
	 * {@link Stabiliti30cFaultSeverity}.</li>
	 * <li>Bits 3-4 represent the fault status, see
	 * {@link Stabiliti30cFaultStatus}.</li>
	 * </ul>
	 */
	FaultStatusStatus(9, UInt16);

	private final int address;
	private final int length;
	private final ModbusDataType dataType;

	private Stabiliti30cRegister(int address, ModbusDataType dataType) {
		this(address, 0, dataType);
	}

	private Stabiliti30cRegister(int address, int length, ModbusDataType dataType) {
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
	private static final IntRangeSet POWER_CONTROL_REGISTER_ADDRESS_SET = createPowerControlRegisterAddressSet();
	private static final IntRangeSet CONTROL_REGISTER_ADDRESS_SET = createControlRegisterAddressSet();

	private static IntRangeSet createConfigRegisterAddressSet() {
		return createRegisterAddressSet(Stabiliti30cRegister.class,
				new HashSet<>(asList("Info", "Config")));
	}

	private static IntRangeSet createPowerControlRegisterAddressSet() {
		return createRegisterAddressSet(Stabiliti30cRegister.class,
				new HashSet<>(asList("PowerControl", "Status")));
	}

	private static IntRangeSet createControlRegisterAddressSet() {
		return createRegisterAddressSet(Stabiliti30cRegister.class, new HashSet<>(asList("Control")));
	}

	/**
	 * Get an address range set that covers all the registers defined in this
	 * enumeration <b>except<b> controls.
	 * 
	 * @return the range set
	 */
	public static IntRangeSet getRegisterAddressSet() {
		IntRangeSet s = new IntRangeSet(CONFIG_REGISTER_ADDRESS_SET);
		s.addAll(POWER_CONTROL_REGISTER_ADDRESS_SET);
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
	public static IntRangeSet getPowerControlRegisterAddressSet() {
		return (IntRangeSet) POWER_CONTROL_REGISTER_ADDRESS_SET.clone();
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
