/* ==================================================================
 * ModbusDatumDataSourceConfigCsvWriter.java - 10/03/2022 9:19:01 AM
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

package net.solarnetwork.node.datum.modbus;

import static java.util.Arrays.fill;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import de.siegmar.fastcsv.writer.CsvWriter;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusWordOrder;
import net.solarnetwork.util.ObjectUtils;

/**
 * Generate Modbus Device configuration CSV from settings.
 *
 * @author matt
 * @version 2.0
 */
public class ModbusDatumDataSourceConfigCsvWriter {

	private final CsvWriter writer;
	private final int rowLen;

	/**
	 * Constructor.
	 *
	 * @param writer
	 *        the writer; note the comment character should be set to something
	 *        <b>other</b> than {@code #} so comments can be generated manually
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 * @throws UncheckedIOException
	 *         if any IO error occurs
	 */
	public ModbusDatumDataSourceConfigCsvWriter(CsvWriter writer) throws UncheckedIOException {
		super();
		this.writer = ObjectUtils.requireNonNullArgument(writer, "writer");
		rowLen = ModbusCsvColumn.values().length;
		String[] row = new String[rowLen];
		for ( ModbusCsvColumn col : ModbusCsvColumn.values() ) {
			row[col.getCode()] = col.getName();
		}
		writer.writeRecord(row);
	}

	/**
	 * Generate Modbus Device CSV from settings.
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
		ModbusDatumDataSourceConfig config = new ModbusDatumDataSourceConfig();
		config.setKey(instanceId);
		for ( Setting s : settings ) {
			config.populateFromSetting(s);
		}
		String[] row = new String[rowLen];
		row[ModbusCsvColumn.INSTANCE_ID.getCode()] = config.getKey();
		row[ModbusCsvColumn.SOURCE_ID.getCode()] = config.getSourceId();
		row[ModbusCsvColumn.SCHEDULE.getCode()] = config.getSchedule();
		row[ModbusCsvColumn.NETWORK_NAME.getCode()] = config.getModbusNetworkName();
		row[ModbusCsvColumn.UNIT_ID
				.getCode()] = (config.getUnitId() != null ? config.getUnitId().toString() : null);
		row[ModbusCsvColumn.SAMPLE_CACHE.getCode()] = (config.getSampleCacheMs() != null
				? config.getSampleCacheMs().toString()
				: null);
		row[ModbusCsvColumn.MAX_WORD_COUNT.getCode()] = (config.getMaxReadWordCount() != null
				? config.getMaxReadWordCount().toString()
				: null);
		row[ModbusCsvColumn.WORD_ORDER.getCode()] = modbusWordOrderValue(config.getWordOrder());

		for ( ModbusPropertyConfig propConfig : config.getPropertyConfigs() ) {
			row[ModbusCsvColumn.PROP_NAME.getCode()] = propConfig.getName();
			row[ModbusCsvColumn.PROP_TYPE.getCode()] = propertyTypeValue(propConfig.getPropertyType());
			row[ModbusCsvColumn.REG_ADDR.getCode()] = (propConfig.getAddress() != null
					? propConfig.getAddress().toString()
					: null);
			row[ModbusCsvColumn.REG_TYPE.getCode()] = registerTypeValue(propConfig.getFunction());
			row[ModbusCsvColumn.DATA_TYPE.getCode()] = dataTypeValue(propConfig.getDataType());
			row[ModbusCsvColumn.DATA_LENGTH.getCode()] = dataLengthValue(propConfig);
			row[ModbusCsvColumn.MULTIPLIER.getCode()] = (propConfig.getUnitMultiplier() != null
					? propConfig.getUnitMultiplier().toPlainString()
					: null);
			row[ModbusCsvColumn.DECIMAL_SCALE.getCode()] = (propConfig.getDecimalScale() != null
					? propConfig.getDecimalScale().toString()
					: null);
			writer.writeRecord(row);
			fill(row, null);
		}
		for ( ExpressionConfig exprConfig : config.getExpressionConfigs() ) {
			if ( !ModbusDatumDataSourceConfigCsvParser.DEFAULT_EXPRESSION_SERVICE_ID
					.equals(exprConfig.getExpressionServiceId()) ) {
				continue;
			}
			row[ModbusCsvColumn.PROP_NAME.getCode()] = exprConfig.getName();
			row[ModbusCsvColumn.PROP_TYPE.getCode()] = propertyTypeValue(exprConfig.getPropertyType());
			row[ModbusCsvColumn.EXPRESSION.getCode()] = exprConfig.getExpression();
			writer.writeRecord(row);
			fill(row, null);
		}
	}

	private String dataLengthValue(ModbusPropertyConfig propConfig) {
		if ( propConfig == null ) {
			return null;
		}
		Integer len = propConfig.getWordLength();
		ModbusDataType dataType = propConfig.getDataType();
		if ( dataType != null ) {
			if ( dataType == ModbusDataType.Bytes || dataType == ModbusDataType.StringAscii
					|| dataType == ModbusDataType.StringUtf8 ) {
				return (len != null ? len.toString() : "1");
			}
		}
		return null;
	}

	private static String modbusWordOrderValue(ModbusWordOrder wordOrder) {
		if ( wordOrder == null ) {
			return null;
		}
		switch (wordOrder) {
			case MostToLeastSignificant:
				return "Most to least";

			case LeastToMostSignificant:
				return "Least to most";

			default:
				return null;
		}
	}

	private static String propertyTypeValue(DatumSamplesType type) {
		if ( type == null ) {
			return null;
		}
		return type.toString();
	}

	private static String registerTypeValue(ModbusReadFunction fn) {
		if ( fn == null ) {
			return null;
		}
		switch (fn) {
			case ReadCoil:
				return "Coil";
			case ReadDiscreteInput:
				return "Discrete Input";
			case ReadHoldingRegister:
				return "Holding";
			case ReadInputRegister:
				return "Input";
			default:
				return null;
		}
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
