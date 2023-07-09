/* ==================================================================
 * MeteorologicalModelRegister.java - 10/07/2023 7:42:41 am
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
 * Enumeration of Modbus register mappings for SunSpec compliant meteorological
 * model 308.
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
public enum MeteorologicalModelRegister implements SunspecModbusReference {

	/** Ambient temperature, in degrees Celsius. */
	TemperatureAmbient(0, Int16),

	/** Relative humidity, in integer percentage . */
	RelativeHumidity(1, Int16),

	/** Barometric Pressure, in HPa. */
	BarometricPressure(2, Int16),

	/** Wind speed, in m/s. */
	WindSpeed(3, Int16),

	/** Wind direction, in degrees. */
	WindDirection(4, Int16),

	/** Rain accumulation since last poll, in mm. */
	Rain(5, Int16),

	/** Snow accumulation since last poll, in mm. */
	Snow(6, Int16),

	/** Precipitation type, WMO 4680 SYNOP code. */
	PrecipitationType(7, Int16),

	/** Electric field, in v/m. */
	ElectricField(8, Int16),

	/** Surface wetness, in kOhms. */
	SurfaceWetness(9, Int16),

	/** Soil moisture, in integer percentage. */
	SoilMoisture(10, Int16),

	;

	private final int address;
	private final ModbusDataType dataType;
	private final int wordLength;
	private final DataClassification classification;

	private MeteorologicalModelRegister(int address, ModbusDataType dataType) {
		this(address, dataType, dataType.getWordLength());
	}

	private MeteorologicalModelRegister(int address, ModbusDataType dataType,
			DataClassification classification) {
		this(address, dataType, dataType.getWordLength(), classification);
	}

	private MeteorologicalModelRegister(int address, ModbusDataType dataType, int wordLength) {
		this(address, dataType, wordLength, null);
	}

	private MeteorologicalModelRegister(int address, ModbusDataType dataType, int wordLength,
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
