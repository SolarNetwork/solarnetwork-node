/* ==================================================================
 * DerStorageCapacityModelRegister.java - 5/10/2026 10:18:41 am
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
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.node.hw.sunspec.DataClassification;
import net.solarnetwork.node.hw.sunspec.SunspecModbusReference;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Enumeration of Modbus register mappings for the SunSpec DER storage capacity
 * model.
 *
 * <p>
 * These mappings correspond to the SunSpec model number <b>713</b>.
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
public enum DerStorageCapacityModelRegister implements SunspecModbusReference {

	/** Energy rating of the storage, in Wh. */
	EnergyRating(0, UInt16),

	/** Energy available in the storage, in Wh. */
	EnergyAvailable(1, UInt16),

	/** State of charge, as a percentage. */
	StateOfCharge(2, UInt16),

	/** State of health, as a percentage. */
	StateOfHealth(3, UInt16),

	/** Storage status, see {@link DerStorageStatus}. */
	Status(4, UInt16, Enumeration),

	/** Energy scale factor. */
	ScaleFactorEnergy(5, Int16, ScaleFactor),

	/** Percentage scale factor. */
	ScaleFactorPercent(6, Int16, ScaleFactor),

	;

	private final int address;
	private final ModbusDataType dataType;
	private final @Nullable DataClassification classification;

	private DerStorageCapacityModelRegister(int address, ModbusDataType dataType) {
		this(address, dataType, null);
	}

	private DerStorageCapacityModelRegister(int address, ModbusDataType dataType,
			@Nullable DataClassification classification) {
		this.address = address;
		this.dataType = dataType;
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
		return dataType.getWordLength();
	}

	@Override
	public @Nullable DataClassification getClassification() {
		return classification;
	}

}
