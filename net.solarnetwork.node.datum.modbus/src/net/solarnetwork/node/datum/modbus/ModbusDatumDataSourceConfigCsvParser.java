/* ==================================================================
 * ModbusDatumDataSourceConfigCsvParser.java - 9/03/2022 2:46:52 PM
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

import static net.solarnetwork.node.datum.modbus.ModbusCsvColumn.DATA_LENGTH;
import static net.solarnetwork.node.datum.modbus.ModbusCsvColumn.DATA_TYPE;
import static net.solarnetwork.node.datum.modbus.ModbusCsvColumn.DECIMAL_SCALE;
import static net.solarnetwork.node.datum.modbus.ModbusCsvColumn.EXPRESSION;
import static net.solarnetwork.node.datum.modbus.ModbusCsvColumn.MAX_WORD_COUNT;
import static net.solarnetwork.node.datum.modbus.ModbusCsvColumn.MULTIPLIER;
import static net.solarnetwork.node.datum.modbus.ModbusCsvColumn.NETWORK_NAME;
import static net.solarnetwork.node.datum.modbus.ModbusCsvColumn.PROP_NAME;
import static net.solarnetwork.node.datum.modbus.ModbusCsvColumn.PROP_TYPE;
import static net.solarnetwork.node.datum.modbus.ModbusCsvColumn.REG_ADDR;
import static net.solarnetwork.node.datum.modbus.ModbusCsvColumn.REG_TYPE;
import static net.solarnetwork.node.datum.modbus.ModbusCsvColumn.SAMPLE_CACHE;
import static net.solarnetwork.node.datum.modbus.ModbusCsvColumn.SCHEDULE;
import static net.solarnetwork.node.datum.modbus.ModbusCsvColumn.SOURCE_ID;
import static net.solarnetwork.node.datum.modbus.ModbusCsvColumn.UNIT_ID;
import static net.solarnetwork.node.datum.modbus.ModbusCsvColumn.WORD_ORDER;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusWordOrder;

/**
 * Parse CSV data into {@link ModbusDatumDataSourceConfig} instances.
 *
 * @author matt
 * @version 2.0
 * @since 3.1
 */
public class ModbusDatumDataSourceConfigCsvParser {

	/** The default expression service ID used when parsing. */
	public static final String DEFAULT_EXPRESSION_SERVICE_ID = "net.solarnetwork.common.expr.spel.SpelExpressionService";

	private final List<ModbusDatumDataSourceConfig> results;
	private final MessageSource messageSource;
	private final List<String> messages;

	/**
	 * Constructor.
	 *
	 * @param results
	 *        the list to add the parsed results to
	 * @param messageSource
	 *        the message source
	 * @param messages
	 *        the list of output messages to add messages to
	 */
	public ModbusDatumDataSourceConfigCsvParser(List<ModbusDatumDataSourceConfig> results,
			MessageSource messageSource, List<String> messages) {
		super();
		this.messageSource = requireNonNullArgument(messageSource, "messageSource");
		this.results = requireNonNullArgument(results, "results");
		this.messages = requireNonNullArgument(messages, "messages");
	}

	/**
	 * Parse CSV.
	 *
	 * @param csv
	 *        the CSV to parse; note that the comment strategy should be set to
	 *        {@link CommentStrategy#NONE} so comments can be handled as
	 * @throws UncheckedIOException
	 *         if any IO error occurs
	 */
	public void parse(CsvReader<CsvRecord> csv) throws UncheckedIOException {
		if ( csv == null ) {
			return;
		}
		csv.skipLines(1);
		ModbusDatumDataSourceConfig config = null;
		for ( CsvRecord row : csv ) {
			final int rowLen = row.getFieldCount();
			final long rowNum = row.getStartingLineNumber();
			final String key = rowKeyValue(row, config);
			if ( key == null || key.startsWith("#") ) {
				// either a comment line, or empty key but no active configuration
				continue;
			}
			if ( config == null || (key != null && !key.equals(config.getKey())) ) {
				// starting new modbus config
				config = new ModbusDatumDataSourceConfig();
				results.add(config);
				config.setKey(key);
				config.setSourceId(parseStringValue(row, rowLen, rowNum, SOURCE_ID.getCode()));
				config.setSchedule(parseStringValue(row, rowLen, rowNum, SCHEDULE.getCode()));
				config.setModbusNetworkName(
						parseStringValue(row, rowLen, rowNum, NETWORK_NAME.getCode()));
				config.setUnitId(parseIntegerValue(row, rowLen, rowNum, UNIT_ID.getCode()));
				config.setSampleCacheMs(parseLongValue(row, rowLen, rowNum, SAMPLE_CACHE.getCode()));
				config.setMaxReadWordCount(
						parseIntegerValue(row, rowLen, rowNum, MAX_WORD_COUNT.getCode()));
				config.setWordOrder(
						parseModbusWordOrderValue(row, rowLen, rowNum, WORD_ORDER.getCode()));
			}

			String expr = parseStringValue(row, rowLen, rowNum, EXPRESSION.getCode());
			if ( expr != null ) {
				ExpressionConfig exprConfig = new ExpressionConfig();
				exprConfig.setName(parseStringValue(row, rowLen, rowNum, PROP_NAME.getCode()));
				exprConfig.setPropertyType(
						parseDatumSamplesTypeValue(row, rowLen, rowNum, PROP_TYPE.getCode()));
				exprConfig.setExpression(expr);
				exprConfig.setExpressionServiceId(DEFAULT_EXPRESSION_SERVICE_ID);
				if ( exprConfig.isValid() ) {
					config.getExpressionConfigs().add(exprConfig);
				} else if ( exprConfig.getName() != null ) {
					messages.add(messageSource.getMessage("message.invalidPropertyConfig",
							new Object[] { rowNum }, "Invalid property configuration.",
							Locale.getDefault()));
				}
			} else {
				ModbusPropertyConfig propConfig = new ModbusPropertyConfig();
				propConfig.setName(parseStringValue(row, rowLen, rowNum, PROP_NAME.getCode()));
				propConfig.setPropertyType(
						parseDatumSamplesTypeValue(row, rowLen, rowNum, PROP_TYPE.getCode()));
				propConfig.setAddress(parseIntegerValue(row, rowLen, rowNum, REG_ADDR.getCode()));
				propConfig.setFunction(
						parseModbusReadFunctionValue(row, rowLen, rowNum, REG_TYPE.getCode()));
				propConfig
						.setDataType(parseModbusDataTypeValue(row, rowLen, rowNum, DATA_TYPE.getCode()));
				propConfig.setWordLength(parseIntegerValue(row, rowLen, rowNum, DATA_LENGTH.getCode()));
				propConfig.setUnitMultiplier(
						parseBigDecimalValue(row, rowLen, rowNum, MULTIPLIER.getCode()));
				propConfig.setDecimalScale(
						parseIntegerValue(row, rowLen, rowNum, DECIMAL_SCALE.getCode()));
				if ( propConfig.isEmpty() ) {
					continue;
				}
				if ( propConfig.isValid() ) {
					config.getPropertyConfigs().add(propConfig);
				} else if ( propConfig.getName() != null ) {
					messages.add(messageSource.getMessage("message.invalidPropertyConfig",
							new Object[] { rowNum }, "Invalid property configuration.",
							Locale.getDefault()));
				}
			}
		}
	}

	private String rowKeyValue(CsvRecord row, ModbusDatumDataSourceConfig currentConfig) {
		String key = row.getField(0);
		if ( key != null ) {
			key = key.trim();
		}
		if ( key != null && !key.isEmpty() ) {
			if ( "-".equals(key) ) {
				return String.valueOf(results.size() + 1);
			}
			return key;
		}
		return (currentConfig != null ? currentConfig.getKey() : null);
	}

	private String parseStringValue(CsvRecord row, int rowLen, long rowNum, int colNum) {
		if ( colNum < rowLen ) {
			String s = row.getField(colNum);
			if ( s != null ) {
				s = s.trim();
			}
			if ( s == null || s.isEmpty() ) {
				return null;
			}
			return s;
		}
		return null;
	}

	private Integer parseIntegerValue(CsvRecord row, int rowLen, long rowNum, int colNum) {
		String s = parseStringValue(row, rowLen, rowNum, colNum);
		if ( s != null ) {
			try {
				return Integer.valueOf(s);
			} catch ( NumberFormatException e ) {
				messages.add(messageSource.getMessage("message.integerFormatError",
						new Object[] { s, rowNum, colNum }, "Malformed integer value.",
						Locale.getDefault()));
			}
		}
		return null;
	}

	private Long parseLongValue(CsvRecord row, int rowLen, long rowNum, int colNum) {
		String s = parseStringValue(row, rowLen, rowNum, colNum);
		if ( s != null ) {
			try {
				return Long.valueOf(s);
			} catch ( NumberFormatException e ) {
				messages.add(messageSource.getMessage("message.integerFormatError",
						new Object[] { s, rowNum, colNum }, "Malformed long value.",
						Locale.getDefault()));
			}
		}
		return null;
	}

	private BigDecimal parseBigDecimalValue(CsvRecord row, int rowLen, long rowNum, int colNum) {
		String s = parseStringValue(row, rowLen, rowNum, colNum);
		if ( s != null ) {
			try {
				return new BigDecimal(s);
			} catch ( NumberFormatException e ) {
				messages.add(messageSource.getMessage("message.decimalFormatError",
						new Object[] { s, rowNum, colNum }, "Malformed decimal value.",
						Locale.getDefault()));
			}
		}
		return null;
	}

	private ModbusWordOrder parseModbusWordOrderValue(CsvRecord row, int rowLen, long rowNum,
			int colNum) {
		String s = parseStringValue(row, rowLen, rowNum, colNum);
		if ( s == null ) {
			return null;
		}
		try {
			return ModbusWordOrder.forKey(Character.toLowerCase(s.charAt(0)));
		} catch ( IllegalArgumentException e ) {
			try {
				return ModbusWordOrder.valueOf(s);
			} catch ( IllegalArgumentException e2 ) {
				messages.add(messageSource.getMessage("message.wordOrderFormatError",
						new Object[] { s, rowNum, colNum }, "Malformed word order value.",
						Locale.getDefault()));
			}
		}
		return null;
	}

	private DatumSamplesType parseDatumSamplesTypeValue(CsvRecord row, int rowLen, long rowNum,
			int colNum) {
		String s = parseStringValue(row, rowLen, rowNum, colNum);
		if ( s == null ) {
			return null;
		}
		try {
			return DatumSamplesType.valueOf(Character.toLowerCase(s.charAt(0)));
		} catch ( IllegalArgumentException e ) {
			try {
				return DatumSamplesType.valueOf(s);
			} catch ( IllegalArgumentException e2 ) {
				messages.add(messageSource.getMessage("message.datumSamplesTypeFormatError",
						new Object[] { s, rowNum, colNum }, "Malformed property type value.",
						Locale.getDefault()));
			}
		}
		return null;
	}

	private ModbusReadFunction parseModbusReadFunctionValue(CsvRecord row, int rowLen, long rowNum,
			int colNum) {
		String s = parseStringValue(row, rowLen, rowNum, colNum);
		if ( s == null ) {
			return null;
		}
		try {
			return ModbusReadFunction.forCode(Integer.parseInt(s));
		} catch ( IllegalArgumentException e ) {
			try {
				return ModbusReadFunction.valueOf(s);
			} catch ( IllegalArgumentException e2 ) {
				// last try, search for keyword
				String lc = s.toLowerCase();
				if ( lc.contains("coil") ) {
					return ModbusReadFunction.ReadCoil;
				} else if ( lc.contains("discrete") ) {
					return ModbusReadFunction.ReadDiscreteInput;
				} else if ( lc.contains("input") ) {
					return ModbusReadFunction.ReadInputRegister;
				} else if ( lc.contains("holding") ) {
					return ModbusReadFunction.ReadHoldingRegister;
				}
				messages.add(messageSource.getMessage("message.functionFormatError",
						new Object[] { s, rowNum, colNum }, "Malformed property type value.",
						Locale.getDefault()));
			}
		}
		return null;
	}

	private ModbusDataType parseModbusDataTypeValue(CsvRecord row, int rowLen, long rowNum, int colNum) {
		String s = parseStringValue(row, rowLen, rowNum, colNum);
		if ( s == null ) {
			return null;
		}
		try {
			return ModbusDataType.forKey(s);
		} catch ( IllegalArgumentException e ) {
			try {
				return ModbusDataType.valueOf(s);
			} catch ( IllegalArgumentException e2 ) {
				// last try, search for keyword
				String lc = s.toLowerCase();
				if ( lc.equals("bool") ) {
					return ModbusDataType.Boolean;
				} else if ( lc.contains("float") ) {
					if ( lc.contains("16") ) {
						return ModbusDataType.Float16;
					} else if ( lc.contains("64") ) {
						return ModbusDataType.Float64;
					}
					return ModbusDataType.Float32;
				} else if ( lc.contains("int") ) {
					if ( lc.contains("unsigned") ) {
						if ( lc.contains("32") ) {
							return ModbusDataType.UInt32;
						} else if ( lc.contains("64") ) {
							return ModbusDataType.UInt64;
						}
						return ModbusDataType.UInt16;
					} else {
						if ( lc.contains("32") ) {
							return ModbusDataType.Int32;
						} else if ( lc.contains("64") ) {
							return ModbusDataType.Int64;
						}
						return ModbusDataType.Int16;
					}
				} else if ( lc.contains("utf") ) {
					return ModbusDataType.StringUtf8;
				} else if ( lc.contains("ascii") ) {
					return ModbusDataType.StringAscii;
				} else if ( lc.contains("byte") ) {
					return ModbusDataType.Bytes;
				}
				messages.add(messageSource.getMessage("message.dataTypeFormatError",
						new Object[] { s, rowNum, colNum }, "Malformed data type value.",
						Locale.getDefault()));
			}
		}
		return null;
	}

}
