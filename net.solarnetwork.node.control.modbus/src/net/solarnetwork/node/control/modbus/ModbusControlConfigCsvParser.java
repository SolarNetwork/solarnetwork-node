/* ==================================================================
 * ModbusControlConfigCsvParser.java - 9/03/2022 2:46:52 PM
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

package net.solarnetwork.node.control.modbus;

import static net.solarnetwork.node.control.modbus.ModbusControlCsvColumn.CONTROL_ID;
import static net.solarnetwork.node.control.modbus.ModbusControlCsvColumn.DATA_LENGTH;
import static net.solarnetwork.node.control.modbus.ModbusControlCsvColumn.DATA_TYPE;
import static net.solarnetwork.node.control.modbus.ModbusControlCsvColumn.DECIMAL_SCALE;
import static net.solarnetwork.node.control.modbus.ModbusControlCsvColumn.MULTIPLIER;
import static net.solarnetwork.node.control.modbus.ModbusControlCsvColumn.NETWORK_NAME;
import static net.solarnetwork.node.control.modbus.ModbusControlCsvColumn.PROP_TYPE;
import static net.solarnetwork.node.control.modbus.ModbusControlCsvColumn.REG_ADDR;
import static net.solarnetwork.node.control.modbus.ModbusControlCsvColumn.REG_TYPE;
import static net.solarnetwork.node.control.modbus.ModbusControlCsvColumn.SAMPLE_CACHE;
import static net.solarnetwork.node.control.modbus.ModbusControlCsvColumn.UNIT_ID;
import static net.solarnetwork.node.control.modbus.ModbusControlCsvColumn.WORD_ORDER;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusWordOrder;
import net.solarnetwork.node.io.modbus.ModbusWriteFunction;

/**
 * Parse CSV data into {@link ModbusControlConfig} instances.
 *
 * @author matt
 * @version 2.0
 * @since 3.1
 */
public class ModbusControlConfigCsvParser {

	private final List<ModbusControlConfig> results;
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
	public ModbusControlConfigCsvParser(List<ModbusControlConfig> results, MessageSource messageSource,
			List<String> messages) {
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
		ModbusControlConfig config = null;
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
				config = new ModbusControlConfig();
				results.add(config);
				config.setKey(key);
				config.setModbusNetworkName(
						parseStringValue(row, rowLen, rowNum, NETWORK_NAME.getCode()));
				config.setUnitId(parseIntegerValue(row, rowLen, rowNum, UNIT_ID.getCode()));
				config.setSampleCacheMs(parseLongValue(row, rowLen, rowNum, SAMPLE_CACHE.getCode()));
				config.setWordOrder(
						parseModbusWordOrderValue(row, rowLen, rowNum, WORD_ORDER.getCode()));
			}

			ModbusWritePropertyConfig propConfig = new ModbusWritePropertyConfig();
			propConfig.setControlId(parseStringValue(row, rowLen, rowNum, CONTROL_ID.getCode()));
			propConfig.setControlPropertyType(
					parseControlPropertyTypeValue(row, rowLen, rowNum, PROP_TYPE.getCode()));
			propConfig.setAddress(parseIntegerValue(row, rowLen, rowNum, REG_ADDR.getCode()));
			propConfig
					.setFunction(parseModbusWriteFunctionValue(row, rowLen, rowNum, REG_TYPE.getCode()));
			propConfig.setDataType(parseModbusDataTypeValue(row, rowLen, rowNum, DATA_TYPE.getCode()));
			propConfig.setWordLength(parseIntegerValue(row, rowLen, rowNum, DATA_LENGTH.getCode()));
			propConfig
					.setUnitMultiplier(parseBigDecimalValue(row, rowLen, rowNum, MULTIPLIER.getCode()));
			propConfig.setDecimalScale(parseIntegerValue(row, rowLen, rowNum, DECIMAL_SCALE.getCode()));
			if ( propConfig.isEmpty() ) {
				continue;
			}
			if ( propConfig.isValid() ) {
				config.getPropertyConfigs().add(propConfig);
			} else if ( propConfig.getControlId() != null ) {
				messages.add(messageSource.getMessage("message.invalidPropertyConfig",
						new Object[] { rowNum }, "Invalid property configuration.",
						Locale.getDefault()));
			}
		}
	}

	private String rowKeyValue(CsvRecord row, ModbusControlConfig currentConfig) {
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

	private NodeControlPropertyType parseControlPropertyTypeValue(CsvRecord row, int rowLen, long rowNum,
			int colNum) {
		String s = parseStringValue(row, rowLen, rowNum, colNum);
		if ( s == null ) {
			return null;
		}
		try {
			return NodeControlPropertyType.forKey(Character.toLowerCase(s.charAt(0)));
		} catch ( IllegalArgumentException e ) {
			try {
				return NodeControlPropertyType.valueOf(s);
			} catch ( IllegalArgumentException e2 ) {
				messages.add(messageSource.getMessage("message.controlPropertyTypeFormatError",
						new Object[] { s, rowNum, colNum }, "Malformed property type value.",
						Locale.getDefault()));
			}
		}
		return null;
	}

	private ModbusWriteFunction parseModbusWriteFunctionValue(CsvRecord row, int rowLen, long rowNum,
			int colNum) {
		String s = parseStringValue(row, rowLen, rowNum, colNum);
		if ( s == null ) {
			return null;
		}
		try {
			return ModbusWriteFunction.forCode(Integer.parseInt(s));
		} catch ( IllegalArgumentException e ) {
			try {
				return ModbusWriteFunction.valueOf(s);
			} catch ( IllegalArgumentException e2 ) {
				// last try, search for keyword
				String lc = s.toLowerCase();
				if ( lc.contains("coil") ) {
					return ModbusWriteFunction.WriteCoil;
				} else if ( lc.contains("holding") ) {
					return ModbusWriteFunction.WriteMultipleHoldingRegisters;
				}
				messages.add(messageSource.getMessage("message.functionFormatError",
						new Object[] { s, rowNum, colNum }, "Malformed Modbus function value.",
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
