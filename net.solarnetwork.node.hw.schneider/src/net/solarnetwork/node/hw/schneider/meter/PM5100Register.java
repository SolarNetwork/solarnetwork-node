/* ==================================================================
 * PM5100Register.java - 17/05/2018 3:13:57 PM
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

package net.solarnetwork.node.hw.schneider.meter;

import static java.util.Arrays.asList;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Float32;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int64;
import static net.solarnetwork.node.io.modbus.ModbusDataType.StringUtf8;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt32;
import static net.solarnetwork.node.io.modbus.ModbusReference.createAddressSet;
import java.util.HashSet;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;
import net.solarnetwork.util.IntRangeSet;

/**
 * Enumeration of Modbus register mappings for the PM5100 series meter.
 * 
 * @author matt
 * @version 2.0
 * @since 2.4
 */
public enum PM5100Register implements ModbusReference {

	// Information

	/** Meter name. */
	InfoName(29, StringUtf8, 20),

	/** Meter model, see {@link PM5100Model}. */
	InfoModel(89, UInt16),

	/** Manufacturing unit ID. */
	InfoManufacturingUnitId(128, UInt16),

	/** Serial number. */
	InfoSerialNumber(129, UInt32),

	/** Manufacture date, in Schneider "date time" format. */
	InfoManufactureDate(131, UInt16, 4),

	/** Firmware major revision. */
	InfoFirmwareRevisionMajor(1637, UInt16),

	/** Firmware minor revision. */
	InfoFirmwareRevisionMinor(1638, UInt16),

	/** Firmware patch revision. */
	InfoFirmwareRevisionPatch(1640, UInt16),

	// Configuration

	/** Number of phases. */
	ConfigNumPhases(2013, UInt16),

	/** Number of wires. */
	ConfigNumWires(2014, UInt16),

	/** Power system, see {@link PowerSystem}. */
	ConfigPowerSystem(2015, UInt16),

	// Meter data

	/** Current average, reported in A. */
	MeterCurrentPhaseA(2999, Float32),

	/** Current average, reported in A. */
	MeterCurrentPhaseB(3001, Float32),

	/** Current average, reported in A. */
	MeterCurrentPhaseC(3003, Float32),

	/**
	 * Neutral current, reported in A.
	 * 
	 * @since 1.2
	 */
	MeterCurrentNeutral(3005, Float32),

	/**
	 * Ground current, reported in A.
	 * 
	 * @since 1.2
	 */
	MeterCurrentGround(3007, Float32),

	/** Current average, reported in A. */
	MeterCurrentAverage(3009, Float32),

	/** Line-to-neutral voltage for phase A, reported in V. */
	MeterVoltageLineLinePhaseAPhaseB(3019, Float32),

	/** Line-to-neutral voltage for phase A, reported in V. */
	MeterVoltageLineLinePhaseBPhaseC(3021, Float32),

	/** Line-to-neutral voltage for phase A, reported in V. */
	MeterVoltageLineLinePhaseCPhaseA(3023, Float32),

	/** Line-to-neutral voltage average, reported in V. */
	MeterVoltageLineLineAverage(3025, Float32),

	/** Line-to-neutral voltage for phase A, reported in V. */
	MeterVoltageLineNeutralPhaseA(3027, Float32),

	/** Line-to-neutral voltage for phase A, reported in V. */
	MeterVoltageLineNeutralPhaseB(3029, Float32),

	/** Line-to-neutral voltage for phase A, reported in V. */
	MeterVoltageLineNeutralPhaseC(3031, Float32),

	/** Line-to-neutral voltage average, reported in V. */
	MeterVoltageLineNeutralAverage(3035, Float32),

	/** Active power total, reported in kW. */
	MeterActivePowerPhaseA(3053, Float32),

	/** Active power total, reported in kW. */
	MeterActivePowerPhaseB(3055, Float32),

	/** Active power total, reported in kW. */
	MeterActivePowerPhaseC(3057, Float32),

	/** Active power total, reported in kW. */
	MeterActivePowerTotal(3059, Float32),

	/** Reactive power total, reported in kVAR. */
	MeterReactivePowerTotal(3067, Float32),

	/** Apparent power total, reported in kVA. */
	MeterApparentPowerTotal(3075, Float32),

	/** Power factor total, in 4Q FP PF. */
	MeterPowerFactorTotal(3083, Float32),

	/** AC frequency, reported in Hz. */
	MeterFrequency(3109, Float32),

	/** Total energy delivered (imported), in Wh. */
	MeterActiveEnergyDelivered(3203, Int64),

	/** Total energy received (exported), in Wh. */
	MeterActiveEnergyReceived(3207, Int64),

	/** Total reactive energy delivered (imported), in VARh. */
	MeterReactiveEnergyDelivered(3219, Int64),

	/** Total reactive energy received (exported), in VARh. */
	MeterReactiveEnergyReceived(3223, Int64),

	/** Total apparent energy delivered (imported), in VAh. */
	MeterApparentEnergyDelivered(3235, Int64),

	/** Total apparent energy received (exported), in VAh. */
	MeterApparentEnergyReceived(3239, Int64);

	private static final IntRangeSet CONFIG_REGISTER_ADDRESS_SET = createAddressSet(
			PM5100Register.class, new HashSet<>(asList("Config", "Info"))).immutableCopy();
	private static final IntRangeSet METER_REGISTER_ADDRESS_SET = createAddressSet(
			PM5100Register.class, new HashSet<>(asList("Meter"))).immutableCopy();

	private final int address;
	private final ModbusDataType dataType;
	private final int wordLength;

	private PM5100Register(int address, ModbusDataType dataType) {
		this(address, dataType, dataType.getWordLength());
	}

	private PM5100Register(int address, ModbusDataType dataType, int wordLength) {
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
		return ModbusReadFunction.ReadHoldingRegister;
	}

	/**
	 * Get the data type word length.
	 * 
	 * @return the word length
	 */
	@Override
	public int getWordLength() {
		return wordLength;
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
	 * and <i>exclusive</i> ending addresses.
	 * </p>
	 * 
	 * @return the range set
	 */
	public static IntRangeSet getMeterRegisterAddressSet() {
		return METER_REGISTER_ADDRESS_SET;
	}

}
