/* ==================================================================
 * EM5600Register.java - 23/01/2020 10:06:08 am
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

package net.solarnetwork.node.hw.hc;

import static java.util.Arrays.asList;
import static net.solarnetwork.node.io.modbus.IntRangeSetUtils.createRegisterAddressSet;
import static net.solarnetwork.node.io.modbus.ModbusDataType.StringAscii;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt32;
import java.util.HashSet;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;
import net.solarnetwork.util.IntRangeSet;

/**
 * Enumeration of Modbus register mappings for EM5600 series power meter
 * devices.
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public enum EM5600Register implements ModbusReference {

	// Information

	/** Meter model. */
	InfoModel(0, UInt16),

	/** Hardware version. */
	InfoHardwareVersion(0x2, StringAscii, 2),

	/** Serial number. */
	InfoSerialNumber(0x10, StringAscii, 4),

	/** Manufacture date, in HC F4 "date time" format. */
	InfoManufactureDate(0x18, UInt32),

	// Configuration

	/** Unit of energy. */
	ConfigEnergyUnit(0x17E, UInt16),

	/** PT ratio. */
	ConfigPtRatio(0x200A, UInt16),

	/** CT ratio. */
	ConfigCtRatio(0x200B, UInt16),

	// Meter data

	/** Current average, reported in {@link UnitFactor#getA()} A. */
	MeterCurrentPhaseA(0x130, UInt16),

	/** Current average, reported in {@link UnitFactor#getA()} A. */
	MeterCurrentPhaseB(0x131, UInt16),

	/** Current average, reported in {@link UnitFactor#getA()} A. */
	MeterCurrentPhaseC(0x132, UInt16),

	/** Neutral current, reported in {@link UnitFactor#getA()} A. */
	//MeterCurrentNeutral(0x133, UInt16),

	/** Current average, reported in {@link UnitFactor#getA()} A. */
	MeterCurrentAverage(0x133, UInt16),

	/**
	 * Line-to-neutral voltage for phase A, reported in
	 * {@link UnitFactor#getU()} V.
	 */
	MeterVoltageLineNeutralPhaseA(0x136, UInt16),

	/**
	 * Line-to-neutral voltage for phase A, reported in
	 * {@link UnitFactor#getU()} V.
	 */
	MeterVoltageLineNeutralPhaseB(0x137, UInt16),

	/**
	 * Line-to-neutral voltage for phase A, reported in
	 * {@link UnitFactor#getU()} V.
	 */
	MeterVoltageLineNeutralPhaseC(0x138, UInt16),

	/**
	 * Line-to-neutral voltage average, reported in {@link UnitFactor#getU()} V.
	 */
	MeterVoltageLineNeutralAverage(0x139, UInt16),

	/**
	 * Line-to-neutral voltage for phase A, reported in
	 * {@link UnitFactor#getU()} V.
	 */
	MeterVoltageLineLinePhaseAPhaseB(0x13B, UInt16),

	/**
	 * Line-to-neutral voltage for phase A, reported in
	 * {@link UnitFactor#getU()} V.
	 */
	MeterVoltageLineLinePhaseBPhaseC(0x13C, UInt16),

	/**
	 * Line-to-neutral voltage for phase A, reported in
	 * {@link UnitFactor#getU()} V.
	 */
	MeterVoltageLineLinePhaseCPhaseA(0x13D, UInt16),

	/** Line-to-neutral voltage average, reported in V. */
	MeterVoltageLineLineAverage(0x13E, UInt16),

	/** Active power for phase A, reported in {@link UnitFactor#getP()} W */
	MeterActivePowerPhaseA(0x145, UInt16),

	/** Active power for phase B, reported in {@link UnitFactor#getP()} W. */
	MeterActivePowerPhaseB(0x149, UInt16),

	/** Active power for phase C, reported in {@link UnitFactor#getP()} W. */
	MeterActivePowerPhaseC(0x14D, UInt16),

	/** Active power total, reported in {@link UnitFactor#getP()} W. */
	MeterActivePowerTotal(0x140, UInt16),

	/**
	 * Reactive power for phase A, reported in {@link UnitFactor#getP()} VAR.
	 */
	MeterReactivePowerPhaseA(0x146, UInt16),

	/**
	 * Reactive power for phase B, reported in {@link UnitFactor#getP()} VAR.
	 */
	MeterReactivePowerPhaseB(0x14A, UInt16),

	/**
	 * Reactive power for phase C, reported in {@link UnitFactor#getP()} VAR.
	 */
	MeterReactivePowerPhaseC(0x14E, UInt16),

	/** Reactive power total, reported in {@link UnitFactor#getP()} VAR. */
	MeterReactivePowerTotal(0x141, UInt16),

	/** Apparent power for phase A, reported in {@link UnitFactor#getP()} VA. */
	MeterApparentPowerPhaseA(0x147, UInt16),

	/** Apparent power for phase B, reported in {@link UnitFactor#getP()} VA. */
	MeterApparentPowerPhaseB(0x14B, UInt16),

	/** Apparent power for phase C, reported in {@link UnitFactor#getP()} VA. */
	MeterApparentPowerPhaseC(0x14F, UInt16),

	/** Apparent power total, reported in {@link UnitFactor#getP()} VA. */
	MeterApparentPowerTotal(0x142, UInt16),

	/** Power factor for phase A. */
	MeterPowerFactorPhaseA(0x148, UInt16),

	/** Power factor for phase B. */
	MeterPowerFactorPhaseB(0x14C, UInt16),

	/** Power factor for phase C. */
	MeterPowerFactorPhaseC(0x150, UInt16),

	/** Power factor total. */
	MeterPowerFactorTotal(0x143, UInt16),

	/** AC frequency, reported in milli Hz. */
	MeterFrequency(0x144, UInt16),

	/** The AC phase rotation. */
	MeterPhaseRotation(0x151, UInt16),

	/** Total energy delivered (imported), in {@link #ConfigEnergyUnit} Wh. */
	MeterActiveEnergyDelivered(0x160, UInt32),

	/** Total energy received (exported), in {@link #ConfigEnergyUnit} Wh. */
	MeterActiveEnergyReceived(0x162, UInt32),

	/**
	 * Total reactive energy delivered (imported), in {@link #ConfigEnergyUnit}
	 * VARh.
	 */
	MeterReactiveEnergyDelivered(0x164, UInt32),

	/**
	 * Total reactive energy received (exported), in {@link #ConfigEnergyUnit}
	 * VARh.
	 */
	MeterReactiveEnergyReceived(0x166, UInt32);

	private final int address;
	private final int length;
	private final ModbusDataType dataType;

	private EM5600Register(int address, ModbusDataType dataType) {
		this(address, dataType, 0);
	}

	private EM5600Register(int address, ModbusDataType dataType, int length) {
		this.address = address;
		this.dataType = dataType;
		this.length = length;
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
	private static final IntRangeSet METER_REGISTER_ADDRESS_SET = createMeterRegisterAddressSet();

	private static IntRangeSet createConfigRegisterAddressSet() {
		return createRegisterAddressSet(EM5600Register.class, new HashSet<>(asList("Info", "Config")))
				.immutableCopy();
	}

	private static IntRangeSet createMeterRegisterAddressSet() {
		return createRegisterAddressSet(EM5600Register.class, new HashSet<>(asList("Meter", "Config")))
				.immutableCopy();
	}

	/**
	 * Get an address range set that covers all the registers defined in this
	 * enumeration.
	 * 
	 * @return the range set
	 */
	public static IntRangeSet getRegisterAddressSet() {
		IntRangeSet s = new IntRangeSet(CONFIG_REGISTER_ADDRESS_SET);
		s.addAll(METER_REGISTER_ADDRESS_SET);
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
	 * Get an address range set that covers all the meter and configuration
	 * registers defined in this enumeration.
	 * 
	 * <p>
	 * Note the ranges in this set represent <i>inclusive</i> starting addresses
	 * and ending addresses.
	 * </p>
	 * 
	 * @return the range set
	 */
	public static IntRangeSet getMeterRegisterAddressSet() {
		return METER_REGISTER_ADDRESS_SET;
	}

}
