/* ==================================================================
 * DerWattVarModelRegister.java - 5/10/2026 4:58:20 pm
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

import static net.solarnetwork.node.hw.sunspec.DataClassification.Enumeration;
import static net.solarnetwork.node.hw.sunspec.DataClassification.ScaleFactor;
import static net.solarnetwork.node.hw.sunspec.PointAccess.ReadWrite;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.DataClassification;
import net.solarnetwork.node.hw.sunspec.PointAccess;
import net.solarnetwork.node.hw.sunspec.SunspecModbusReference;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Enumeration of Modbus register mappings for the SunSpec DER watt-var model.
 *
 * <p>
 * These mappings correspond to the SunSpec model number <b>712</b>, along with
 * the shared {@link DerCurveModelRegister} mappings.
 * </p>
 *
 * <p>
 * Note that all register addresses are encoded as an offset from the block
 * address of the model block, except for the {@code Curve*} registers, which
 * are encoded as an offset from the start of each curve block, and the
 * {@code Point*} registers, which are encoded as an offset from the start of
 * each curve point.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.2
 */
public enum DerWattVarModelRegister implements SunspecModbusReference {

	/** The curve active power scale factor. */
	ScaleFactorActivePower(10, Int16, ScaleFactor),

	/** The curve reactive power scale factor. */
	ScaleFactorReactivePower(11, Int16, ScaleFactor),

	// curves

	/** The dependent reference, see {@link DerReactivePowerReference}. */
	CurveDependentReference(1, UInt16, Enumeration, ReadWrite),

	/** The power priority, see {@link DerReactivePowerPriority}. */
	CurvePowerPriority(2, UInt16, Enumeration, ReadWrite),

	/** The curve read-only setting. */
	CurveReadOnly(3, UInt16, Enumeration),

	// curve points

	/** The point active power, as a percentage of maximum active power. */
	PointActivePower(0, Int16, ReadWrite),

	/** The point reactive power, as a percentage of the dependent reference. */
	PointReactivePower(1, Int16, ReadWrite),

	;

	private final int address;
	private final ModbusDataType dataType;
	private final @Nullable DataClassification classification;
	private final PointAccess access;

	private DerWattVarModelRegister(int address, ModbusDataType dataType,
			DataClassification classification) {
		this(address, dataType, classification, PointAccess.ReadOnly);
	}

	private DerWattVarModelRegister(int address, ModbusDataType dataType, PointAccess access) {
		this(address, dataType, null, access);
	}

	private DerWattVarModelRegister(int address, ModbusDataType dataType,
			@Nullable DataClassification classification, PointAccess access) {
		this.address = address;
		this.dataType = dataType;
		this.classification = classification;
		this.access = access;
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

	@Override
	public @Nullable DataClassification getClassification() {
		return classification;
	}

	@Override
	public PointAccess getAccess() {
		return access;
	}

}
