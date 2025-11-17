/* ==================================================================
 * ModbusServerConfigCsvParser.java - 9/03/2022 2:46:52 PM
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

import static net.solarnetwork.node.io.modbus.server.domain.ModbusServerCsvColumn.BIND_ADDRESS;
import static net.solarnetwork.node.io.modbus.server.domain.ModbusServerCsvColumn.DATA_LENGTH;
import static net.solarnetwork.node.io.modbus.server.domain.ModbusServerCsvColumn.DATA_TYPE;
import static net.solarnetwork.node.io.modbus.server.domain.ModbusServerCsvColumn.DECIMAL_SCALE;
import static net.solarnetwork.node.io.modbus.server.domain.ModbusServerCsvColumn.MULTIPLIER;
import static net.solarnetwork.node.io.modbus.server.domain.ModbusServerCsvColumn.PORT;
import static net.solarnetwork.node.io.modbus.server.domain.ModbusServerCsvColumn.PROPERTY;
import static net.solarnetwork.node.io.modbus.server.domain.ModbusServerCsvColumn.REG_ADDR;
import static net.solarnetwork.node.io.modbus.server.domain.ModbusServerCsvColumn.REG_TYPE;
import static net.solarnetwork.node.io.modbus.server.domain.ModbusServerCsvColumn.SOURCE_ID;
import static net.solarnetwork.node.io.modbus.server.domain.ModbusServerCsvColumn.THROTTLE;
import static net.solarnetwork.node.io.modbus.server.domain.ModbusServerCsvColumn.UNIT_ID;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import org.springframework.context.MessageSource;
import org.supercsv.io.ICsvListReader;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusRegisterBlockType;
import net.solarnetwork.node.io.modbus.server.domain.MeasurementConfig;
import net.solarnetwork.node.io.modbus.server.domain.ModbusServerConfig;
import net.solarnetwork.node.io.modbus.server.domain.RegisterBlockConfig;
import net.solarnetwork.node.io.modbus.server.domain.UnitConfig;

/**
 * Parse CSV data into {@link ModbusServerConfig} instances.
 *
 * @author matt
 * @version 1.1
 * @since 2.3
 */
public class ModbusServerConfigCsvParser {

	private final List<ModbusServerConfig> results;
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
	public ModbusServerConfigCsvParser(List<ModbusServerConfig> results, MessageSource messageSource,
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
	 *        the CSV to parse
	 * @throws IOException
	 *         if any IO error occurs
	 */
	public void parse(ICsvListReader csv) throws IOException {
		if ( csv == null ) {
			return;
		}
		@SuppressWarnings("unused")
		final String[] headerRow = csv.getHeader(true);
		final Map<String, String> configMeta = new HashMap<>(8);
		List<String> row = null;
		ModbusServerConfig config = null;
		Map<String, Map<Integer, Map<ModbusRegisterBlockType, SortedMap<Integer, MeasurementConfig>>>> instUnitBlockMapping = new LinkedHashMap<>();
		ModbusRegisterBlockType blockType = null;
		int registerAddress = 0;
		int unitId = 0;
		while ( (row = csv.read()) != null ) {
			if ( row.isEmpty() ) {
				continue;
			}
			final int rowLen = row.size();
			final int rowNum = csv.getRowNumber();
			final String key = rowKeyValue(row, config);
			if ( key == null ) {
				// either a comment line, or empty key but no active configuration
				continue;
			}
			if ( key.startsWith("#") ) {
				if ( "#param".equalsIgnoreCase(key) && rowLen >= 3 ) {
					String metaKey = row.get(1);
					String metaVal = row.get(2);
					if ( metaKey != null && !metaKey.isEmpty() && metaVal != null
							&& !metaVal.isEmpty() ) {
						configMeta.put(metaKey, metaVal);
					}
				}
				continue;
			}

			if ( config == null || (key != null && !key.equals(config.getKey())) ) {
				// starting new modbus server config
				config = new ModbusServerConfig();
				results.add(config);
				config.setKey(key);
				config.setBindAddress(parseStringValue(row, rowLen, rowNum, BIND_ADDRESS.getCode()));
				config.setPort(parseIntegerValue(row, rowLen, rowNum, PORT.getCode()));
				config.setRequestThrottle(parseLongValue(row, rowLen, rowNum, THROTTLE.getCode()));
				config.getMeta().putAll(configMeta);
				configMeta.clear();
				instUnitBlockMapping.put(key, new LinkedHashMap<>());
			}

			final Integer unitIdVal = parseIntegerValue(row, rowLen, rowNum, UNIT_ID.getCode());
			if ( unitIdVal != null ) {
				unitId = unitIdVal.intValue();
			}
			UnitConfig unitConfig = config.unitConfig(unitId);
			if ( unitConfig == null ) {
				unitConfig = new UnitConfig();
				unitConfig.setUnitId(unitId);
				config.getUnitConfigs().add(unitConfig);
			}

			final String blockTypeVal = parseStringValue(row, rowLen, rowNum, REG_TYPE.getCode());
			if ( blockType == null
					|| (blockTypeVal != null && !blockTypeVal.equalsIgnoreCase(blockType.toString())) ) {
				if ( blockTypeVal == null ) {
					// no block type; skip row
					continue;
				}
				try {
					blockType = ModbusRegisterBlockType.valueOf(blockTypeVal);
				} catch ( IllegalArgumentException e ) {
					messages.add(messageSource.getMessage("message.registerTypeFormatError",
							new Object[] { rowNum }, "Invalid property configuration.",
							Locale.getDefault()));
					continue;
				}
			}

			Map<Integer, Map<ModbusRegisterBlockType, SortedMap<Integer, MeasurementConfig>>> unitBlockMapping = instUnitBlockMapping
					.get(config.getKey());
			Map<ModbusRegisterBlockType, SortedMap<Integer, MeasurementConfig>> blockMapping = unitBlockMapping
					.computeIfAbsent(unitId, k -> new LinkedHashMap<>());
			SortedMap<Integer, MeasurementConfig> measurementConfigs = blockMapping
					.computeIfAbsent(blockType, k -> new TreeMap<>());

			final Integer regAddrVal = parseIntegerValue(row, rowLen, rowNum, REG_ADDR.getCode());
			if ( regAddrVal != null ) {
				registerAddress = regAddrVal.intValue();
			}

			MeasurementConfig measConfig = measurementConfigs.computeIfAbsent(registerAddress,
					k -> new MeasurementConfig());
			measConfig.setDataType(parseModbusDataTypeValue(row, rowLen, rowNum, DATA_TYPE.getCode()));
			measConfig.setWordLength(parseIntegerValue(row, rowLen, rowNum, DATA_LENGTH.getCode()));
			measConfig.setSourceId(parseStringValue(row, rowLen, rowNum, SOURCE_ID.getCode()));
			measConfig.setPropertyName(parseStringValue(row, rowLen, rowNum, PROPERTY.getCode()));
			measConfig
					.setUnitMultiplier(parseBigDecimalValue(row, rowLen, rowNum, MULTIPLIER.getCode()));
			measConfig.setDecimalScale(parseIntegerValue(row, rowLen, rowNum, DECIMAL_SCALE.getCode()));
			if ( measConfig.isValid() ) {
				registerAddress += measConfig.getSize();
			} else {
				measurementConfigs.remove(registerAddress);
			}
		}

		for ( Entry<String, Map<Integer, Map<ModbusRegisterBlockType, SortedMap<Integer, MeasurementConfig>>>> instEntry : instUnitBlockMapping
				.entrySet() ) {
			config = results.stream().filter(c -> instEntry.getKey().equals(c.getKey())).findAny()
					.orElse(null);
			if ( config == null ) {
				continue;
			}
			Map<Integer, Map<ModbusRegisterBlockType, SortedMap<Integer, MeasurementConfig>>> unitBlockMapping = instEntry
					.getValue();
			for ( Entry<Integer, Map<ModbusRegisterBlockType, SortedMap<Integer, MeasurementConfig>>> unitEntry : unitBlockMapping
					.entrySet() ) {
				unitId = unitEntry.getKey().intValue();
				UnitConfig unitConfig = config.unitConfig(unitId);
				Map<ModbusRegisterBlockType, SortedMap<Integer, MeasurementConfig>> blockMapping = unitEntry
						.getValue();
				for ( Entry<ModbusRegisterBlockType, SortedMap<Integer, MeasurementConfig>> bockEntry : blockMapping
						.entrySet() ) {
					blockType = bockEntry.getKey();
					final SortedMap<Integer, MeasurementConfig> m = bockEntry.getValue();
					registerAddress = -1;
					RegisterBlockConfig blockConfig = null;
					for ( Entry<Integer, MeasurementConfig> me : m.entrySet() ) {
						final int addr = me.getKey().intValue();
						final int measCount = (blockConfig != null
								? blockConfig.getMeasurementConfigsCount()
								: 0);
						final MeasurementConfig measConfig = me.getValue();
						if ( measCount < 1
								|| (blockConfig != null && blockType != blockConfig.getBlockType())
								|| addr != (registerAddress
										+ blockConfig.getMeasurementConfigs()[measCount - 1]
												.getSize()) ) {
							// starting new block
							blockConfig = new RegisterBlockConfig();
							blockConfig.setBlockType(blockType);
							blockConfig.setStartAddress(registerAddress < 0 ? addr : registerAddress);
							unitConfig.setRegisterBlockConfigsCount(
									unitConfig.getRegisterBlockConfigsCount() + 1);
							unitConfig
									.getRegisterBlockConfigs()[unitConfig.getRegisterBlockConfigsCount()
											- 1] = blockConfig;
						}
						blockConfig.setMeasurementConfigsCount(
								blockConfig.getMeasurementConfigsCount() + 1);
						blockConfig.getMeasurementConfigs()[blockConfig.getMeasurementConfigsCount()
								- 1] = measConfig;
						registerAddress = addr;
					}

				}
			}
		}
	}

	private String rowKeyValue(List<String> row, ModbusServerConfig currentConfig) {
		String key = row.get(0);
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

	private String parseStringValue(List<String> row, int rowLen, int rowNum, int colNum) {
		if ( colNum < rowLen ) {
			String s = row.get(colNum);
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

	private Integer parseIntegerValue(List<String> row, int rowLen, int rowNum, int colNum) {
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

	private Long parseLongValue(List<String> row, int rowLen, int rowNum, int colNum) {
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

	private BigDecimal parseBigDecimalValue(List<String> row, int rowLen, int rowNum, int colNum) {
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

	private ModbusDataType parseModbusDataTypeValue(List<String> row, int rowLen, int rowNum,
			int colNum) {
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
