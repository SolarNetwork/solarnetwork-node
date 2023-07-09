/* ==================================================================
 * BomTemperatureModelRegister.java - 5/07/2023 10:30:42 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sunspec.environmental;

import static net.solarnetwork.node.io.modbus.ModbusDataType.Int16;
import net.solarnetwork.node.hw.sunspec.DataClassification;
import net.solarnetwork.node.hw.sunspec.SunspecModbusReference;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Enumeration of Modbus register mappings for SunSpec compliant irradiance
 * model 303.
 * 
 * <p>
 * Note that all register addresses are encoded as an offset from the block
 * address of the model block.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 4.2
 */
public enum BomTemperatureModelRegister implements SunspecModbusReference {

	/** Temperature, back of module, in degrees Celsius. */
	TemperatureBOM(0, Int16),

	;

	private final int address;
	private final ModbusDataType dataType;
	private final int wordLength;
	private final DataClassification classification;

	private BomTemperatureModelRegister(int address, ModbusDataType dataType) {
		this(address, dataType, dataType.getWordLength());
	}

	private BomTemperatureModelRegister(int address, ModbusDataType dataType,
			DataClassification classification) {
		this(address, dataType, dataType.getWordLength(), classification);
	}

	private BomTemperatureModelRegister(int address, ModbusDataType dataType, int wordLength) {
		this(address, dataType, wordLength, null);
	}

	private BomTemperatureModelRegister(int address, ModbusDataType dataType, int wordLength,
			DataClassification classification) {
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
	public DataClassification getClassification() {
		return classification;
	}

}
