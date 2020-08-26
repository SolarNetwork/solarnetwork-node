/* ==================================================================
 * WattsOnRegister.java - 14/08/2020 7:33:24 AM
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

package net.solarnetwork.node.hw.elkor.upt;

import static java.util.Arrays.asList;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Float32;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import static net.solarnetwork.node.io.modbus.ModbusReference.createAddressSet;
import java.util.HashSet;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;
import net.solarnetwork.util.IntRangeSet;

/**
 * Enumeration of Modbus register mappings for the Elkor WattsOn series
 * universal power transducer.
 * 
 * @author matt
 * @version 1.0
 */
public enum WattsOnRegister implements ModbusReference {

	/** The power transformer primary ratio value. */
	ConfigPtRatioPrimary(0x80, UInt16),

	/** The power transformer secondary ratio value. */
	ConfigPtRatioSecondary(0x81, UInt16),

	/** The current transformer primary ratio value. */
	ConfigCtRatioPrimary(0x82, UInt16),

	/** The current transformer secondary ratio value. */
	ConfigCtRatioSecondary(0x83, UInt16),

	/** Serial number. */
	InfoSerialNumber(0x95, UInt16),

	/** Active power total, reported in kW. */
	MeterActivePowerTotal(0x302, Float32),

	/** Reactive power total, reported in kVAR. */
	MeterReactivePowerTotal(0x304, Float32),

	/** Apparent power total, reported in kVA. */
	MeterApparentPowerTotal(0x306, Float32),

	/** Line-to-neutral voltage average, reported in V. */
	MeterVoltageLineNeutralAverage(0x308, Float32),

	/** Line-to-neutral voltage average, reported in V. */
	MeterVoltageLineLineAverage(0x30a, Float32),

	/** Current average, reported in A. */
	MeterCurrentAverage(0x30c, Float32),

	/** Power factor total, in decimal scale. */
	MeterPowerFactorTotal(0x30e, Float32),

	/** AC frequency, reported in Hz. */
	MeterFrequency(0x310, Float32),

	/** Line-to-neutral voltage phase A, reported in V. */
	MeterVoltageLineNeutralPhaseA(0x314, Float32),

	/** Line-to-neutral voltage phase B, reported in V. */
	MeterVoltageLineNeutralPhaseB(0x316, Float32),

	/** Line-to-neutral voltage phase C, reported in V. */
	MeterVoltageLineNeutralPhaseC(0x318, Float32),

	/** Line-to-line voltage average, reported in V. */
	MeterVoltageLineLinePhaseAPhaseB(0x31a, Float32),

	/** Line-to-neutral voltage average, reported in V. */
	MeterVoltageLineLinePhaseBPhaseC(0x31c, Float32),

	/** Line-to-neutral voltage average, reported in V. */
	MeterVoltageLineLinePhaseCPhaseA(0x31e, Float32),

	/** Current phase A, reported in A. */
	MeterCurrentPhaseA(0x320, Float32),

	/** Current phase B, reported in A. */
	MeterCurrentPhaseB(0x322, Float32),

	/** Current phase C, reported in A. */
	MeterCurrentPhaseC(0x324, Float32),

	/** Active power phase A, reported in kW. */
	MeterActivePowerPhaseA(0x326, Float32),

	/** Active power phase B, reported in kW. */
	MeterActivePowerPhaseB(0x328, Float32),

	/** Active power phase C, reported in kW. */
	MeterActivePowerPhaseC(0x32a, Float32),

	/** Reactive power phase A, reported in kVAR. */
	MeterReactivePowerPhaseA(0x32c, Float32),

	/** Reactive power phase B, reported in kVAR. */
	MeterReactivePowerPhaseB(0x32e, Float32),

	/** Reactive power phase C, reported in kVAR. */
	MeterReactivePowerPhaseC(0x330, Float32),

	/** Apparent power phase A, reported in kVA. */
	MeterApparentPowerPhaseA(0x332, Float32),

	/** Apparent power phase B, reported in kVA. */
	MeterApparentPowerPhaseB(0x334, Float32),

	/** Apparent power phase C, reported in kVA. */
	MeterApparentPowerPhaseC(0x336, Float32),

	/** Power factor phase A, in decimal scale. */
	MeterPowerFactorPhaseA(0x338, Float32),

	/** Power factor phase B, in decimal scale. */
	MeterPowerFactorPhaseB(0x33a, Float32),

	/** Power factor phase C, in decimal scale. */
	MeterPowerFactorPhaseC(0x33c, Float32),

	/** Firmware revision. */
	InfoFirmwareVersion(0x33e, Float32),

	/** Energy delivered phase A, in kWh. */
	MeterActiveEnergyDeliveredPhaseA(0x340, Float32),

	/** Energy delivered phase BA, in kWh. */
	MeterActiveEnergyDeliveredPhaseB(0x342, Float32),

	/** Energy delivered phase C, in kWh. */
	MeterActiveEnergyDeliveredPhaseC(0x344, Float32),

	/** Total energy delivered, in kWh. */
	MeterActiveEnergyDelivered(0x346, Float32),

	/** Energy received phase A, in kWh. */
	MeterActiveEnergyReceivedPhaseA(0x348, Float32),

	/** Energy received phase B, in kWh. */
	MeterActiveEnergyReceivedPhaseB(0x34a, Float32),

	/** Energy received phase C, in kWh. */
	MeterActiveEnergyReceivedPhaseC(0x34c, Float32),

	/** Total energy received, in kWh. */
	MeterActiveEnergyReceived(0x34e, Float32),

	/** Net energy (delivered and received) phase A, in kWh. */
	MeterActiveEnergyNetPhaseA(0x350, Float32),

	/** Net energy (delivered and received) phase B, in kWh. */
	MeterActiveEnergyNetPhaseB(0x352, Float32),

	/** Net energy (delivered and received) phase C, in kWh. */
	MeterActiveEnergyNetPhaseC(0x354, Float32),

	/** Net energy (delivered and received), in kWh. */
	MeterActiveEnergyNet(0x356, Float32),

	/** Reactive energy delivered phase A, in kVARh. */
	MeterReactiveEnergyDeliveredPhaseA(0x358, Float32),

	/** Reactive energy delivered phase B, in kVARh. */
	MeterReactiveEnergyDeliveredPhaseB(0x35a, Float32),

	/** Reactive energy delivered phase C, in kVARh. */
	MeterReactiveEnergyDeliveredPhaseC(0x35c, Float32),

	/** Total reactive energy delivered, in kVARh. */
	MeterReactiveEnergyDelivered(0x35e, Float32),

	/** Reactive energy received phase A, in kVARh. */
	MeterReactiveEnergyReceivedPhaseA(0x360, Float32),

	/** Reactive energy received phase B, in kVARh. */
	MeterReactiveEnergyReceivedPhaseB(0x362, Float32),

	/** Reactive energy received phase C, in kVARh. */
	MeterReactiveEnergyReceivedPhaseC(0x364, Float32),

	/** Total reactive energy received, in kVARh. */
	MeterReactiveEnergyReceived(0x366, Float32),

	/** Net reactive energy (delivered and received) phase A, in kVARh. */
	MeterReactiveEnergyNetPhaseA(0x368, Float32),

	/** Net reactive energy (delivered and received) phase B, in kVARh. */
	MeterReactiveEnergyNetPhaseB(0x36a, Float32),

	/** Net reactive energy (delivered and received) phase C, in kVARh. */
	MeterReactiveEnergyNetPhaseC(0x36c, Float32),

	/** Net reactive energy (delivered and received) total, in kVARh. */
	MeterReactiveEnergyNet(0x36e, Float32),

	/** Apparent energy phase A, in kVAh. */
	MeterApparentEnergyPhaseA(0x370, Float32),

	/** Apparent energy phase B, in kVAh. */
	MeterApparentEnergyPhaseB(0x372, Float32),

	/** Apparent energy phase C, in kVAh. */
	MeterApparentEnergyPhaseC(0x374, Float32),

	/** Apparent energy total, in kVAh. */
	MeterApparentEnergy(0x376, Float32),

	;

	private static final IntRangeSet CONFIG_REGISTER_ADDRESS_SET = createAddressSet(
			WattsOnRegister.class, new HashSet<>(asList("Config", "Info"))).immutableCopy();
	private static final IntRangeSet METER_REGISTER_ADDRESS_SET = createAddressSet(WattsOnRegister.class,
			new HashSet<>(asList("Config", "Meter"))).immutableCopy();

	private final int address;
	private final int length;
	private final ModbusDataType dataType;

	private WattsOnRegister(int address, ModbusDataType dataType) {
		this(address, 0, dataType);
	}

	private WattsOnRegister(int address, int length, ModbusDataType dataType) {
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
