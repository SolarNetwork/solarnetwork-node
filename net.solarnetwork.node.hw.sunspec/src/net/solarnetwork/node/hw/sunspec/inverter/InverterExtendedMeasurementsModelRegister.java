/* ==================================================================
 * InverterExtendedMeasurementsModelRegister.java - 6/10/2026 7:36:48 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sunspec.inverter;

import static net.solarnetwork.node.hw.sunspec.DataClassification.Accumulator;
import static net.solarnetwork.node.hw.sunspec.DataClassification.Bitfield;
import static net.solarnetwork.node.hw.sunspec.DataClassification.ScaleFactor;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.StringUtf8;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt32;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt64;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.DataClassification;
import net.solarnetwork.node.hw.sunspec.SunspecModbusReference;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Enumeration of Modbus register mappings for the SunSpec inverter controls
 * extended measurements and status model.
 *
 * <p>
 * These mappings correspond to the SunSpec model number <b>122</b>.
 * </p>
 *
 * <p>
 * Note that all register addresses are encoded as an offset from the block
 * address of the model block.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public enum InverterExtendedMeasurementsModelRegister implements SunspecModbusReference {

	/** PV inverter connection status, see {@link InverterConnectionStatus}. */
	PvConnectionStatus(0, UInt16, Bitfield),

	/**
	 * Storage inverter connection status, see {@link InverterConnectionStatus}.
	 */
	StorageConnectionStatus(1, UInt16, Bitfield),

	/**
	 * Electrical connection point (ECP) connection status, with bit 0 set when
	 * connected.
	 */
	EcpConnectionStatus(2, UInt16, Bitfield),

	/** Lifetime active energy output, in Wh. */
	ActiveEnergyExported(3, UInt64, Accumulator),

	/** Lifetime apparent energy output, in VAh. */
	ApparentEnergyExported(7, UInt64, Accumulator),

	/** Lifetime reactive energy output in quadrant 1, in VARh. */
	ReactiveEnergyQ1(11, UInt64, Accumulator),

	/** Lifetime reactive energy output in quadrant 2, in VARh. */
	ReactiveEnergyQ2(15, UInt64, Accumulator),

	/** Lifetime reactive energy output in quadrant 3, in VARh. */
	ReactiveEnergyQ3(19, UInt64, Accumulator),

	/** Lifetime reactive energy output in quadrant 4, in VARh. */
	ReactiveEnergyQ4(23, UInt64, Accumulator),

	/**
	 * Reactive power available without affecting active power output, in VAR.
	 */
	ReactivePowerAvailable(27, Int16),

	/** Available reactive power scale factor, as *10^X. */
	ScaleFactorReactivePowerAvailable(28, Int16, ScaleFactor),

	/**
	 * Active power available, in W.
	 *
	 * <p>
	 * SunSpec gives the units as {@literal var}, but describes the point as an
	 * amount of watts.
	 * </p>
	 */
	ActivePowerAvailable(29, UInt16),

	/** Available active power scale factor, as *10^X. */
	ScaleFactorActivePowerAvailable(30, Int16, ScaleFactor),

	/** Setpoint limits reached, see {@link InverterSetpointLimit}. */
	SetpointLimitsReached(31, UInt32, Bitfield),

	/** Active inverter controls, see {@link InverterControlFunction}. */
	ActiveControls(33, UInt32, Bitfield),

	/** Time synchronization source. */
	TimeSource(35, StringUtf8, 4),

	/** Device time, in seconds since 1 January 2000 00:00 UTC. */
	DeviceTime(39, UInt32),

	/** Active ride-throughs, see {@link InverterRideThrough}. */
	ActiveRideThroughs(41, UInt16, Bitfield),

	/** Isolation resistance, in ohms. */
	IsolationResistance(42, UInt16),

	/** Isolation resistance scale factor, as *10^X. */
	ScaleFactorIsolationResistance(43, Int16, ScaleFactor),

	;

	private final int address;
	private final ModbusDataType dataType;
	private final int wordLength;
	private final @Nullable DataClassification classification;

	private InverterExtendedMeasurementsModelRegister(int address, ModbusDataType dataType) {
		this(address, dataType, dataType.getWordLength(), null);
	}

	private InverterExtendedMeasurementsModelRegister(int address, ModbusDataType dataType,
			@Nullable DataClassification classification) {
		this(address, dataType, dataType.getWordLength(), classification);
	}

	private InverterExtendedMeasurementsModelRegister(int address, ModbusDataType dataType,
			int wordLength) {
		this(address, dataType, wordLength, null);
	}

	private InverterExtendedMeasurementsModelRegister(int address, ModbusDataType dataType,
			int wordLength, @Nullable DataClassification classification) {
		this.address = address;
		this.dataType = dataType;
		this.wordLength = wordLength;
		this.classification = classification;
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
		return wordLength;
	}

	@Override
	public @Nullable DataClassification getClassification() {
		return classification;
	}

}
