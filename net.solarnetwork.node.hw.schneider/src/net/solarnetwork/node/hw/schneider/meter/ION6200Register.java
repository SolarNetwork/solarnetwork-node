/* ==================================================================
 * ION6200Register.java - 14/05/2018 2:38:37 PM
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
 * Enumeration of Modbus register mappings for the ION6200 series meter.
 * 
 * @author matt
 * @version 2.0
 * @since 2.4
 */
public enum ION6200Register implements ModbusReference {

	/** Serial number. */
	InfoSerialNumber(0, UInt32),

	/** Firmware revision. */
	InfoFirmwareVersion(2, UInt16),

	/** Firmware revision. */
	InfoDeviceType(12, UInt16),

	/** Line-to-neutral voltage phase A, reported in PVS. */
	MeterVoltageLineNeutralPhaseA(99, UInt16),

	/** Line-to-neutral voltage phase B, reported in PVS. */
	MeterVoltageLineNeutralPhaseB(100, UInt16),

	/** Line-to-neutral voltage phase C, reported in PVS. */
	MeterVoltageLineNeutralPhaseC(101, UInt16),

	/** Line-to-neutral voltage average, reported in PVS. */
	MeterVoltageLineNeutralAverage(102, UInt16),

	/** Line-to-line voltage average, reported in PVS. */
	MeterVoltageLineLinePhaseAPhaseB(103, UInt16),

	/** Line-to-neutral voltage average, reported in PVS. */
	MeterVoltageLineLinePhaseBPhaseC(104, UInt16),

	/** Line-to-neutral voltage average, reported in PVS. */
	MeterVoltageLineLinePhaseCPhaseA(105, UInt16),

	/** Line-to-neutral voltage average, reported in PVS. */
	MeterVoltageLineLineAverage(106, UInt16),

	/** Current phase A, reported in PCS. */
	MeterCurrentPhaseA(107, UInt16),

	/** Current phase B, reported in PCS. */
	MeterCurrentPhaseB(108, UInt16),

	/** Current phase C, reported in PCS. */
	MeterCurrentPhaseC(109, UInt16),

	/** Current average, reported in PCS. */
	MeterCurrentAverage(110, UInt16),

	/** AC frequency, reported in x100 Hz scale. */
	MeterFrequency(114, Int16),

	/** Power factor total, in x100 decimal scale. */
	MeterPowerFactorTotal(115, Int16),

	/** Active power total, reported in PPS kW or MW. */
	MeterActivePowerTotal(119, Int16),

	/** Reactive power total, reported in PPS VAR. */
	MeterReactivePowerTotal(120, Int16),

	/** Apparent power total, reported in PPS VA. */
	MeterApparentPowerTotal(121, Int16),

	/** Active power total, reported in kW. */
	MeterActivePowerPhaseA(122, Int16),

	/** Active power total, reported in kW. */
	MeterActivePowerPhaseB(123, Int16),

	/** Active power total, reported in kW. */
	MeterActivePowerPhaseC(124, Int16),

	/** Total energy delivered, in kWh or MWh. */
	MeterActiveEnergyDelivered(137, UInt32),

	/** Total energy received, in kWh or MWh. */
	MeterActiveEnergyReceived(139, UInt32),

	/** Total reactive energy delivered, in kVARh or MVARh. */
	MeterReactiveEnergyDelivered(141, UInt32),

	/** Total reactive energy received, in kVARh or MVARh. */
	MeterReactiveEnergyReceived(143, UInt32),

	/**
	 * The voltage mode enumeration.
	 * 
	 * <dl>
	 * <dt>0</dt>
	 * <dd>4W (4-Wire WYE)</dd>
	 * <dt>1</dt>
	 * <dd>dELt (Delta)</dd>
	 * <dt>2</dt>
	 * <dd>2W (Single Phase)</dd>
	 * <dt>3</dt>
	 * <dd>dEM (Demonstration)</dd>
	 * <dt>4</dt>
	 * <dd>3W (3-Wire WYE)</dd>
	 * <dt>5</dt>
	 * <dd>dELd (Delta direct)</dd>
	 * </dl>
	 */
	ConfigVoltsMode(4000, UInt16),

	/**
	 * The programmable voltage scale (PVS) enumeration.
	 * 
	 * <p>
	 * All programmable scale enumerations follow this definition:
	 * </p>
	 * 
	 * <dl>
	 * <dt>0</dt>
	 * <dd>0.001</dd>
	 * <dt>1</dt>
	 * <dd>0.01</dd>
	 * <dt>2</dt>
	 * <dd>0.1</dd>
	 * <dt>3</dt>
	 * <dd>1</dd>
	 * <dt>4</dt>
	 * <dd>10</dd>
	 * <dt>5</dt>
	 * <dd>100</dd>
	 * <dt>6</dt>
	 * <dd>1000</dd>
	 * </dl>
	 */
	ConfigProgrammableVoltageScale(4011, UInt16),

	/** The programmable current scale (PCS) enumeration. */
	ConfigProgrammableCurrentScale(4012, UInt16),

	/** The programmable neutral current scale (PnS) enumeration. */
	ConfigProgrammableNeutralCurrentScale(4013, UInt16),

	/** The programmable power scale (PPS) enumeration. */
	ConfigProgrammablePowerScale(4014, UInt16);

	private static final IntRangeSet CONFIG_REGISTER_ADDRESS_SET = createAddressSet(
			ION6200Register.class, new HashSet<>(asList("Config", "Info"))).immutableCopy();
	private static final IntRangeSet METER_REGISTER_ADDRESS_SET = createAddressSet(
			ION6200Register.class, new HashSet<>(asList("ConfigProgrammable", "Meter"))).immutableCopy();

	private final int address;
	private final ModbusDataType dataType;

	private ION6200Register(int address, ModbusDataType dataType) {
		this.address = address;
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
		return dataType.getWordLength();
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
	 * Get an address range set that covers all the meter and programmable scale
	 * registers defined in this enumeration.
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
