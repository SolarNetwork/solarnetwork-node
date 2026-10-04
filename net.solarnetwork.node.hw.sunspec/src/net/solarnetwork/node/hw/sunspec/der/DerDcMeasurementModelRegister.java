/* ==================================================================
 * DerDcMeasurementModelRegister.java - 5/10/2026 10:24:03 am
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

package net.solarnetwork.node.hw.sunspec.der;

import static net.solarnetwork.node.hw.sunspec.DataClassification.Bitfield;
import static net.solarnetwork.node.hw.sunspec.DataClassification.Enumeration;
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
 * Enumeration of Modbus register mappings for the SunSpec DER DC measurement
 * model.
 *
 * <p>
 * These mappings correspond to the SunSpec model number <b>714</b>.
 * </p>
 *
 * <p>
 * Note that all register addresses are encoded as an offset from the block
 * address of the model block, except for the {@code Port*} registers, which
 * are encoded as an offset from the start of each DC port block.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public enum DerDcMeasurementModelRegister implements SunspecModbusReference {

	/** Bitmask of the ports with active alarms, where bit 0 is the first port. */
	AlarmedPortsBitmask(0, UInt32, Bitfield),

	/** The number of DC ports. */
	PortCount(2, UInt16),

	/** Total DC current for all ports, in A. */
	DcCurrent(3, Int16),

	/** Total DC power for all ports, in W. */
	DcPower(4, Int16),

	/** Total DC energy injected for all ports, in Wh. */
	DcEnergyInjected(5, UInt64),

	/** Total DC energy absorbed for all ports, in Wh. */
	DcEnergyAbsorbed(9, UInt64),

	/** DC current scale factor. */
	ScaleFactorDcCurrent(13, Int16, ScaleFactor),

	/** DC voltage scale factor. */
	ScaleFactorDcVoltage(14, Int16, ScaleFactor),

	/** DC power scale factor. */
	ScaleFactorDcPower(15, Int16, ScaleFactor),

	/** DC energy scale factor. */
	ScaleFactorDcEnergy(16, Int16, ScaleFactor),

	/** Temperature scale factor. */
	ScaleFactorTemperature(17, Int16, ScaleFactor),

	// DC ports

	/** The port type, see {@link DerDcPortType}. */
	PortType(0, UInt16, Enumeration),

	/** The port ID. */
	PortId(1, UInt16),

	/** The port name. */
	PortName(2, StringUtf8, 8),

	/** The port DC current, in A. */
	PortDcCurrent(10, Int16),

	/** The port DC voltage, in V. */
	PortDcVoltage(11, UInt16),

	/** The port DC power, in W. */
	PortDcPower(12, Int16),

	/** The port DC energy injected, in Wh. */
	PortDcEnergyInjected(13, UInt64),

	/** The port DC energy absorbed, in Wh. */
	PortDcEnergyAbsorbed(17, UInt64),

	/** The port temperature, in degrees Celsius. */
	PortTemperature(21, Int16),

	/** The port status, see {@link DerDcPortStatus}. */
	PortStatus(22, UInt16, Enumeration),

	/** Bitmask of port alarms, see {@link DerDcPortAlarm}. */
	PortAlarmsBitmask(23, UInt32, Bitfield),

	;

	private final int address;
	private final ModbusDataType dataType;
	private final int wordLength;
	private final @Nullable DataClassification classification;

	private DerDcMeasurementModelRegister(int address, ModbusDataType dataType) {
		this(address, dataType, dataType.getWordLength(), null);
	}

	private DerDcMeasurementModelRegister(int address, ModbusDataType dataType,
			@Nullable DataClassification classification) {
		this(address, dataType, dataType.getWordLength(), classification);
	}

	private DerDcMeasurementModelRegister(int address, ModbusDataType dataType, int wordLength) {
		this(address, dataType, wordLength, null);
	}

	private DerDcMeasurementModelRegister(int address, ModbusDataType dataType, int wordLength,
			@Nullable DataClassification classification) {
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
