/* ==================================================================
 * PM3200Register.java - 20/01/2020 5:49:24 pm
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

package net.solarnetwork.node.hw.schneider.meter;

import static java.util.Arrays.asList;
import static net.solarnetwork.node.io.modbus.IntRangeSetUtils.createRegisterAddressSet;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Float32;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int64;
import static net.solarnetwork.node.io.modbus.ModbusDataType.StringUtf8;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt32;
import java.util.HashSet;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;
import net.solarnetwork.util.IntRangeSet;

/**
 * Enumeration of Modbus register mappings for the PM3200 series meter.
 * 
 * @author matt
 * @version 1.0
 * @since 3.0
 */
public enum PM3200Register implements ModbusReference {

	/*
	 * NOTE: constructor indexes are 1-baesd to match Schneider documentation.
	 */

	// Information

	/** Meter name. */
	InfoName(30, StringUtf8, 20),

	/** Meter model. */
	InfoModel(50, StringUtf8, 20),

	/** Manufacturer. */
	InfoManufacturer(70, StringUtf8, 20),

	/** Serial number. */
	InfoSerialNumber(130, UInt32),

	/** Manufacture date, in Schneider "date time" format. */
	InfoManufactureDate(132, UInt16, 4),

	/** Firmware revision in X.Y.ZTT format. */
	InfoFirmwareRevision(1637, UInt16),

	// Configuration

	/** Number of phases. */
	ConfigNumPhases(2014, UInt16),

	/** Number of wires. */
	ConfigNumWires(2015, UInt16),

	/** Power system, see {@link PowerSystem}. */
	ConfigPowerSystem(2016, UInt16),

	// Meter data

	/** Current average, reported in A. */
	MeterCurrentPhaseA(3000, Float32),

	/** Current average, reported in A. */
	MeterCurrentPhaseB(3002, Float32),

	/** Current average, reported in A. */
	MeterCurrentPhaseC(3004, Float32),

	/** Neutral current, reported in A. */
	MeterCurrentNeutral(3006, Float32),

	/** Current average, reported in A. */
	MeterCurrentAverage(3010, Float32),

	/** Line-to-neutral voltage for phase A, reported in V. */
	MeterVoltageLineLinePhaseAPhaseB(3020, Float32),

	/** Line-to-neutral voltage for phase A, reported in V. */
	MeterVoltageLineLinePhaseBPhaseC(3022, Float32),

	/** Line-to-neutral voltage for phase A, reported in V. */
	MeterVoltageLineLinePhaseCPhaseA(3024, Float32),

	/** Line-to-neutral voltage average, reported in V. */
	MeterVoltageLineLineAverage(3026, Float32),

	/** Line-to-neutral voltage for phase A, reported in V. */
	MeterVoltageLineNeutralPhaseA(3028, Float32),

	/** Line-to-neutral voltage for phase A, reported in V. */
	MeterVoltageLineNeutralPhaseB(3030, Float32),

	/** Line-to-neutral voltage for phase A, reported in V. */
	MeterVoltageLineNeutralPhaseC(3032, Float32),

	/** Line-to-neutral voltage average, reported in V. */
	MeterVoltageLineNeutralAverage(3036, Float32),

	/** Active power for phase A, reported in kW. */
	MeterActivePowerPhaseA(3054, Float32),

	/** Active power for phase B, reported in kW. */
	MeterActivePowerPhaseB(3056, Float32),

	/** Active power for phase C, reported in kW. */
	MeterActivePowerPhaseC(3058, Float32),

	/** Active power total, reported in kW. */
	MeterActivePowerTotal(3060, Float32),

	/** Reactive power for phase A, reported in kVAR. */
	MeterReactivePowerPhaseA(3062, Float32),

	/** Reactive power for phase B, reported in kVAR. */
	MeterReactivePowerPhaseB(3064, Float32),

	/** Reactive power for phase C, reported in kVAR. */
	MeterReactivePowerPhaseC(3066, Float32),

	/** Reactive power total, reported in kVAR. */
	MeterReactivePowerTotal(3068, Float32),

	/** Apparent power for phase A, reported in kVA. */
	MeterApparentPowerPhaseA(3070, Float32),

	/** Apparent power for phase B, reported in kVA. */
	MeterApparentPowerPhaseB(3072, Float32),

	/** Apparent power for phase C, reported in kVA. */
	MeterApparentPowerPhaseC(3074, Float32),

	/** Apparent power total, reported in kVA. */
	MeterApparentPowerTotal(3076, Float32),

	/** Power factor for phase A, in 4Q FP PF. */
	MeterPowerFactorPhaseA(3078, Float32),

	/** Power factor for phase B, in 4Q FP PF. */
	MeterPowerFactorPhaseB(3080, Float32),

	/** Power factor for phase C, in 4Q FP PF. */
	MeterPowerFactorPhaseC(3082, Float32),

	/** Power factor total, in 4Q FP PF. */
	MeterPowerFactorTotal(3084, Float32),

	/** Reactive power factor total, in tangent phi. */
	MeterReactivePowerFactorTotal(3108, Float32),

	/** AC frequency, reported in Hz. */
	MeterFrequency(3110, Float32),

	/** Meter temperature, in degrees celsius. */
	MeterTemperature(3132, Float32),

	/** Total energy delivered (imported), in Wh. */
	MeterActiveEnergyDelivered(3204, Int64),

	/** Total energy received (exported), in Wh. */
	MeterActiveEnergyReceived(3208, Int64),

	/** Total reactive energy delivered (imported), in VARh. */
	MeterReactiveEnergyDelivered(3220, Int64),

	/** Total reactive energy received (exported), in VARh. */
	MeterReactiveEnergyReceived(3224, Int64),

	/** Total apparent energy delivered (imported), in VAh. */
	MeterApparentEnergyDelivered(3236, Int64),

	/** Total apparent energy received (exported), in VAh. */
	MeterApparentEnergyReceived(3240, Int64),

	/** Phase A energy delivered (imported), in Wh. */
	MeterActiveEnergyDeliveredPhaseA(3518, Int64),

	/** Phase B energy delivered (imported), in Wh. */
	MeterActiveEnergyDeliveredPhaseB(3522, Int64),

	/** Phase C energy delivered (imported), in Wh. */
	MeterActiveEnergyDeliveredPhaseC(3526, Int64),

	/** Phase A reactive energy delivered (imported), in VARh. */
	MeterReactiveEnergyDeliveredPhaseA(3530, Int64),

	/** Phase B reactive energy delivered (imported), in VARh. */
	MeterReactiveEnergyDeliveredPhaseB(3534, Int64),

	/** Phase C reactive energy delivered (imported), in VARh. */
	MeterReactiveEnergyDeliveredPhaseC(3538, Int64),

	/** Phase A apparent energy delivered (imported), in VAh. */
	MeterApparentEnergyDeliveredPhaseA(3542, Int64),

	/** Phase B apparent energy delivered (imported), in VAh. */
	MeterApparentEnergyDeliveredPhaseB(3546, Int64),

	/** Phase C apparent energy delivered (imported), in VAh. */
	MeterApparentEnergyDeliveredPhaseC(3550, Int64);

	private static final IntRangeSet CONFIG_REGISTER_ADDRESS_SET = createRegisterAddressSet(
			PM3200Register.class, new HashSet<>(asList("Config", "Info"))).immutableCopy();
	private static final IntRangeSet METER_REGISTER_ADDRESS_SET = createRegisterAddressSet(
			PM3200Register.class, new HashSet<>(asList("Meter"))).immutableCopy();

	private final int address;
	private final ModbusDataType dataType;
	private final int wordLength;

	private PM3200Register(int address, ModbusDataType dataType) {
		this(address, dataType, dataType.getWordLength());
	}

	private PM3200Register(int address, ModbusDataType dataType, int wordLength) {
		this.address = address - 1;
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
