/* ==================================================================
 * SDM630Register.java - 22/01/2020 9:22:47 am
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

package net.solarnetwork.node.hw.deson.meter;

import static java.util.Arrays.asList;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Float32;
import static net.solarnetwork.node.io.modbus.ModbusReference.createAddressSet;
import java.util.HashSet;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;
import net.solarnetwork.util.IntRangeSet;

/**
 * Enumeration of Modbus register mappings for the SDM 630 series meter.
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public enum SDM630Register implements ModbusReference {

	// Configuration

	/** Power system, see {@link SDMWiringMode}. */
	ConfigWiringMode(10),

	/** Serial number. */
	InfoSerialNumber(42),

	// Meter data

	/** Line-to-neutral voltage for phase A, reported in V. */
	MeterVoltageLineNeutralPhaseA(0),

	/** Line-to-neutral voltage for phase A, reported in V. */
	MeterVoltageLineNeutralPhaseB(2),

	/** Line-to-neutral voltage for phase A, reported in V. */
	MeterVoltageLineNeutralPhaseC(4),

	/** Line-to-neutral voltage average, reported in V. */
	MeterVoltageLineNeutralAverage(42),

	/** Line-to-neutral voltage for phase A, reported in V. */
	MeterVoltageLineLinePhaseAPhaseB(200),

	/** Line-to-neutral voltage for phase A, reported in V. */
	MeterVoltageLineLinePhaseBPhaseC(202),

	/** Line-to-neutral voltage for phase A, reported in V. */
	MeterVoltageLineLinePhaseCPhaseA(204),

	/** Line-to-neutral voltage average, reported in V. */
	MeterVoltageLineLineAverage(206),

	/** Current average, reported in A. */
	MeterCurrentPhaseA(6),

	/** Current average, reported in A. */
	MeterCurrentPhaseB(8),

	/** Current average, reported in A. */
	MeterCurrentPhaseC(10),

	/** Neutral current, reported in A. */
	MeterCurrentNeutral(224),

	/** Current average, reported in A. */
	MeterCurrentAverage(46),

	/** Active power for phase A, reported in W. */
	MeterActivePowerPhaseA(12),

	/** Active power for phase B, reported in W. */
	MeterActivePowerPhaseB(14),

	/** Active power for phase C, reported in W. */
	MeterActivePowerPhaseC(16),

	/** Active power total, reported in W. */
	MeterActivePowerTotal(52),

	/** Apparent power for phase A, reported in VA. */
	MeterApparentPowerPhaseA(18),

	/** Apparent power for phase B, reported in VA. */
	MeterApparentPowerPhaseB(20),

	/** Apparent power for phase C, reported in VA. */
	MeterApparentPowerPhaseC(22),

	/** Apparent power total, reported in VA. */
	MeterApparentPowerTotal(56),

	/** Reactive power for phase A, reported in VAR. */
	MeterReactivePowerPhaseA(24),

	/** Reactive power for phase B, reported in VAR. */
	MeterReactivePowerPhaseB(26),

	/** Reactive power for phase C, reported in VAR. */
	MeterReactivePowerPhaseC(28),

	/** Reactive power total, reported in VAR. */
	MeterReactivePowerTotal(60),

	/** Power factor for phase A. */
	MeterPowerFactorPhaseA(30),

	/** Power factor for phase B. */
	MeterPowerFactorPhaseB(32),

	/** Power factor for phase C. */
	MeterPowerFactorPhaseC(34),

	/** Power factor total. */
	MeterPowerFactorTotal(62),

	/** AC frequency, reported in Hz. */
	MeterFrequency(70),

	/** Total energy delivered (imported), in kWh. */
	MeterActiveEnergyDelivered(72),

	/** Total energy received (exported), in kWh. */
	MeterActiveEnergyReceived(74),

	/** Total reactive energy delivered (imported), in kVARh. */
	MeterReactiveEnergyDelivered(76),

	/** Total reactive energy received (exported), in kVARh. */
	MeterReactiveEnergyReceived(78);

	private static final IntRangeSet CONFIG_REGISTER_ADDRESS_SET = createAddressSet(
			SDM630Register.class, new HashSet<>(asList("Config", "Info"))).immutableCopy();
	private static final IntRangeSet METER_REGISTER_ADDRESS_SET = createAddressSet(
			SDM630Register.class, new HashSet<>(asList("Meter"))).immutableCopy();

	private final int address;
	private final ModbusDataType dataType;
	private final int wordLength;

	private SDM630Register(int address) {
		this(address, Float32, Float32.getWordLength());
	}

	private SDM630Register(int address, ModbusDataType dataType) {
		this(address, dataType, dataType.getWordLength());
	}

	private SDM630Register(int address, ModbusDataType dataType, int wordLength) {
		this.address = address;
		this.dataType = dataType;
		this.wordLength = wordLength;
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
		if ( !name().startsWith("Meter") ) {
			return ModbusReadFunction.ReadHoldingRegister;
		}
		return ModbusReadFunction.ReadInputRegister;
	}

	@Override
	public int getWordLength() {
		return wordLength;
	}

	/**
	 * Get an address range set that covers all the non-control registers
	 * defined in this enumeration.
	 * 
	 * @return the range set
	 */
	public static IntRangeSet getRegisterAddressSet() {
		return getMeterRegisterAddressSet();
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
	public static IntRangeSet getConfigRegisterAddressSet() {
		return CONFIG_REGISTER_ADDRESS_SET;
	}

	/**
	 * Get an address range set that covers all the meter registers defined in
	 * this enumeration.
	 * 
	 * <p>
	 * Note the ranges in this set represent <i>inclusive</i> starting addresses
	 * and <i>exclusive</i> ending addresses.
	 * </p>
	 * 
	 * @return the range set
	 */
	public static IntRangeSet getMeterRegisterAddressSet() {
		return METER_REGISTER_ADDRESS_SET;
	}

}
