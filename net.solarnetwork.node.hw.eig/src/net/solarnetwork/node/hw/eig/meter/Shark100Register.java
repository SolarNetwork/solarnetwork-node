/* ==================================================================
 * Shark100Register.java - 26/07/2018 10:43:06 AM
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

package net.solarnetwork.node.hw.eig.meter;

import static java.util.Arrays.asList;
import static net.solarnetwork.node.io.modbus.IntRangeSetUtils.createRegisterAddressSet;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Float32;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int32;
import static net.solarnetwork.node.io.modbus.ModbusDataType.StringAscii;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import java.util.HashSet;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;
import net.solarnetwork.util.IntRangeSet;

/**
 * Enumeration of Modbus register mappings for the Shark 100 series meter.
 * 
 * @author matt
 * @version 2.0
 */
public enum Shark100Register implements ModbusReference {

	/** Display name. */
	InfoMeterName(0x0, 8, StringAscii),

	/** Serial number. */
	InfoSerialNumber(0x8, 8, StringAscii),

	/** Meter type bitmap. */
	InfoMeterType(0x10, UInt16),

	/** Firmware revision. */
	InfoFirmwareVersion(0x11, 2, StringAscii),

	/** Meter configuration bitmap. */
	InfoMeterConfiguration(0x14, UInt16),

	/** ASIC version. */
	InfoAsicVersion(0x15, UInt16),

	/**
	 * A bitmap with both PT multiplier and power system (wiring type).
	 * 
	 * <p>
	 * The 12 high bits represent the PT multiplier. The low 4 bits form the
	 * power system (wiring type) enumeration.
	 * </p>
	 * 
	 * @see SharkPowerSystem
	 */
	ConfigPtMultiplierAndPowerSystem(0x7533, UInt16),

	/**
	 * A bitmap with the power scale, number of energy digits, energy scale, and
	 * energy digits after decimal point.
	 * 
	 * @see SharkPowerEnergyFormat
	 */
	ConfigPowerEnergyFormats(0x7535, UInt16),

	/** Active power total, reported in W. */
	MeterActivePowerTotal(0x383, Float32),

	/** Reactive power total, reported in VAR. */
	MeterReactivePowerTotal(0x385, Float32),

	/** Apparent power total, reported in VA. */
	MeterApparentPowerTotal(0x387, Float32),

	/** Line-to-neutral voltage phase A, reported in V. */
	MeterVoltageLineNeutralPhaseA(0x03E7, Float32),

	/** Line-to-neutral voltage phase B, reported in V. */
	MeterVoltageLineNeutralPhaseB(0x03E9, Float32),

	/** Line-to-neutral voltage phase C, reported in V. */
	MeterVoltageLineNeutralPhaseC(0x03EB, Float32),

	/**
	 * Line-to-line voltage phase A - phase B, reported in V.
	 * 
	 * @since 1.1
	 */
	MeterVoltageLineLinePhaseAPhaseB(0x03ED, Float32),

	/**
	 * Line-to-line voltage phase B - phase C, reported in V.
	 * 
	 * @since 1.1
	 */
	MeterVoltageLineLinePhaseBPhaseC(0x03EF, Float32),

	/**
	 * Line-to-line voltage phase C - phase A, reported in V.
	 * 
	 * @since 1.1
	 */
	MeterVoltageLineLinePhaseCPhaseA(0x03F1, Float32),

	/** Current phase A, reported in A. */
	MeterCurrentPhaseA(0x3F3, Float32),

	/** Current phase B, reported in A. */
	MeterCurrentPhaseB(0x3F5, Float32),

	/** Current phase C, reported in A. */
	MeterCurrentPhaseC(0x3F7, Float32),

	/** Power factor total, -1.0 to 1.0. */
	MeterPowerFactorTotal(0x3FF, Float32),

	/** AC frequency, reported in Hz. */
	MeterFrequency(0x401, Float32),

	/**
	 * The neutral current, reported in A.
	 *
	 * @since 1.2
	 */
	MeterNeutralCurrent(0x403, Float32),

	/** Total energy received, in energy scale factor * Wh. */
	MeterActiveEnergyReceived(0x44B, Int32),

	/** Total energy delivered, in energy scale factor * Wh. */
	MeterActiveEnergyDelivered(0x44D, Int32),

	/**
	 * Total reactive energy received (exported), in energy scale factor * VARh.
	 */
	MeterReactiveEnergyReceived(0x453, Int32),

	/**
	 * Total reactive energy delivered (imported), in energy scale factor *
	 * VARh.
	 */
	MeterReactiveEnergyDelivered(0x455, Int32);

	private static final IntRangeSet CONFIG_REGISTER_ADDRESS_SET = createRegisterAddressSet(
			Shark100Register.class, new HashSet<>(asList("Config", "Info"))).immutableCopy();
	private static final IntRangeSet METER_REGISTER_ADDRESS_SET = createRegisterAddressSet(
			Shark100Register.class, new HashSet<>(asList("Meter"))).immutableCopy();

	private final int address;
	private final int length;
	private final ModbusDataType dataType;

	private Shark100Register(int address, ModbusDataType dataType) {
		this(address, 0, dataType);
	}

	private Shark100Register(int address, int length, ModbusDataType dataType) {
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
	 * Get an address range set that covers all the meter registers defined in
	 * this enumeration.
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
