/* ==================================================================
 * ModbusServerConfigCsvWriter.java - 10/03/2022 9:19:01 AM
 *
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus.server.impl;

import static java.util.Arrays.fill;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import org.supercsv.io.ICsvListWriter;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusRegisterBlockType;
import net.solarnetwork.node.io.modbus.server.domain.MeasurementConfig;
import net.solarnetwork.node.io.modbus.server.domain.ModbusServerConfig;
import net.solarnetwork.node.io.modbus.server.domain.ModbusServerCsvColumn;
import net.solarnetwork.node.io.modbus.server.domain.RegisterBlockConfig;
import net.solarnetwork.node.io.modbus.server.domain.UnitConfig;
import net.solarnetwork.util.ObjectUtils;

/**
 * Generate Modbus Server configuration CSV from settings.
 *
 * @author matt
 * @version 1.0
 */
public class ModbusServerConfigCsvWriter {

	private final ICsvListWriter writer;
	private final int rowLen;

	/**
	 * Constructor.
	 *
	 * @param writer
	 *        the writer
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 * @throws IOException
	 *         if any IO error occurs
	 */
	public ModbusServerConfigCsvWriter(ICsvListWriter writer) throws IOException {
		super();
		this.writer = ObjectUtils.requireNonNullArgument(writer, "writer");
		rowLen = ModbusServerCsvColumn.values().length;
		String[] row = new String[rowLen];
		for ( ModbusServerCsvColumn col : ModbusServerCsvColumn.values() ) {
			row[col.getCode()] = col.getName();
		}
		writer.writeHeader(row);
	}

	/**
	 * Generate Modbus Server CSV from settings.
	 *
	 * @param factoryId
	 *        the Modbus Device factory ID
	 * @param instanceId
	 *        the instance ID
	 * @param settings
	 *        the settings to generate CSV for
	 * @throws IOException
	 *         if any IO error occurs
	 */
	public void generateCsv(String factoryId, String instanceId, List<Setting> settings)
			throws IOException {
		if ( settings == null || settings.isEmpty() ) {
			return;
		}

		final ModbusServerConfig config = new ModbusServerConfig();
		config.setKey(instanceId);
		for ( Setting s : settings ) {
			config.populateFromSetting(s);
		}
		String[] row = new String[rowLen];
		row[ModbusServerCsvColumn.INSTANCE_ID.getCode()] = config.getKey();
		row[ModbusServerCsvColumn.BIND_ADDRESS.getCode()] = config.getBindAddress();
		row[ModbusServerCsvColumn.PORT
				.getCode()] = (config.getPort() != null ? config.getPort().toString() : "5020");
		row[ModbusServerCsvColumn.THROTTLE.getCode()] = (config.getRequestThrottle() != null
				? config.getRequestThrottle().toString()
				: null);

		// generate #param rows for unhandled settings, like uid etc
		for ( Entry<String, String> e : config.getMeta().entrySet() ) {
			writer.write(new String[] { "#param", e.getKey(), e.getValue() });
		}

		for ( UnitConfig unitConfig : config.getUnitConfigs() ) {
			row[ModbusServerCsvColumn.UNIT_ID.getCode()] = String.valueOf(unitConfig.getUnitId());

			RegisterBlockConfig[] blockConfigs = unitConfig.getRegisterBlockConfigs();
			if ( blockConfigs == null ) {
				continue;
			}
			for ( int blockIdx = 0; blockIdx < blockConfigs.length; blockIdx++ ) {
				final RegisterBlockConfig blockConfig = blockConfigs[blockIdx];
				final ModbusRegisterBlockType blockType = blockConfig.getBlockType();
				int regAddr = blockConfig.getStartAddress();
				MeasurementConfig[] measConfigs = blockConfig.getMeasurementConfigs();
				if ( measConfigs == null ) {
					continue;
				}
				for ( int measIdx = 0; measIdx < measConfigs.length; measIdx++ ) {
					final MeasurementConfig measConfig = measConfigs[measIdx];
					row[ModbusServerCsvColumn.REG_TYPE.getCode()] = blockType.toString();
					row[ModbusServerCsvColumn.REG_ADDR.getCode()] = String.valueOf(regAddr);
					row[ModbusServerCsvColumn.DATA_TYPE.getCode()] = dataTypeValue(
							measConfig.getDataType());
					row[ModbusServerCsvColumn.DATA_LENGTH.getCode()] = dataLengthValue(measConfig);
					row[ModbusServerCsvColumn.SOURCE_ID.getCode()] = measConfig.getSourceId();
					row[ModbusServerCsvColumn.PROPERTY.getCode()] = measConfig.getPropertyName();
					row[ModbusServerCsvColumn.MULTIPLIER
							.getCode()] = (measConfig.getUnitMultiplier() != null
									? measConfig.getUnitMultiplier().toPlainString()
									: null);
					row[ModbusServerCsvColumn.DECIMAL_SCALE
							.getCode()] = (measConfig.getDecimalScale() != null
									? measConfig.getDecimalScale().toString()
									: null);
					writer.write(row);
					fill(row, null);
					regAddr += measConfig.getSize();
				}
			}
		}
	}

	private String dataLengthValue(MeasurementConfig propConfig) {
		if ( propConfig == null ) {
			return null;
		}
		Integer len = propConfig.getWordLength();
		ModbusDataType dataType = propConfig.getDataType();
		if ( dataType != null ) {
			if ( dataType == ModbusDataType.Bytes || dataType == ModbusDataType.StringAscii
					|| dataType == ModbusDataType.StringUtf8 ) {
				return (len != null ? len.toString() : "");
			}
		}
		return null;
	}

	private static String dataTypeValue(ModbusDataType type) {
		if ( type == null ) {
			return null;
		}
		switch (type) {
			case Boolean:
				return "Boolean";
			case Float16:
				return "16-bit float";
			case Float32:
				return "32-bit float";
			case Float64:
				return "64-bit float";
			case Int16:
				return "16-bit signed int";
			case Int32:
				return "32-bit signed int";
			case Int64:
				return "64-bit signed int";
			case UInt16:
				return "16-bit unsigned int";
			case UInt32:
				return "32-bit unsigned int";
			case UInt64:
				return "64-bit unsigned int";
			case Bytes:
				return "Bytes";
			case StringUtf8:
				return "String UTF-8";
			case StringAscii:
				return "String ASCII";
			default:
				return null;
		}
	}

}
