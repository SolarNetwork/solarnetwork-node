/* ==================================================================
 * BacnetControlConfigCsvParser.java - 10/11/2022 10:09:36 am
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

package net.solarnetwork.node.control.bacnet;

import static net.solarnetwork.node.control.bacnet.BacnetControlCsvColumn.CONTROL_ID;
import static net.solarnetwork.node.control.bacnet.BacnetControlCsvColumn.DECIMAL_SCALE;
import static net.solarnetwork.node.control.bacnet.BacnetControlCsvColumn.DEVICE_ID;
import static net.solarnetwork.node.control.bacnet.BacnetControlCsvColumn.INSTANCE_ID;
import static net.solarnetwork.node.control.bacnet.BacnetControlCsvColumn.MULTIPLIER;
import static net.solarnetwork.node.control.bacnet.BacnetControlCsvColumn.NETWORK_NAME;
import static net.solarnetwork.node.control.bacnet.BacnetControlCsvColumn.OBJECT_NUMBER;
import static net.solarnetwork.node.control.bacnet.BacnetControlCsvColumn.OBJECT_TYPE;
import static net.solarnetwork.node.control.bacnet.BacnetControlCsvColumn.PRIORITY;
import static net.solarnetwork.node.control.bacnet.BacnetControlCsvColumn.PROPERTY_ID;
import static net.solarnetwork.node.control.bacnet.BacnetControlCsvColumn.PROP_TYPE;
import static net.solarnetwork.node.control.bacnet.BacnetControlCsvColumn.SAMPLE_CACHE;
import static net.solarnetwork.node.control.bacnet.BacnetControlCsvColumn.SERVICE_GROUP;
import static net.solarnetwork.node.control.bacnet.BacnetControlCsvColumn.SERVICE_NAME;
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

/**
 * Parse CSV data into {@link BacnetControlConfig} instances.
 *
 * @author matt
 * @version 2.0
 */
public class BacnetControlConfigCsvParser {

	private final List<BacnetControlConfig> results;
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
	public BacnetControlConfigCsvParser(List<BacnetControlConfig> results, MessageSource messageSource,
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
		BacnetControlConfig config = null;
		for ( CsvRecord row : csv ) {
			final int rowLen = row.getFieldCount();
			final long rowNum = row.getStartingLineNumber();
			final String key = rowKeyValue(row, config);
			if ( key == null || key.startsWith("#") ) {
				// either a comment line, or empty key but no active configuration
				continue;
			}
			if ( config == null || (key != null && !key.equals(config.getKey())) ) {
				// starting new config
				config = new BacnetControlConfig();
				results.add(config);
				config.setKey(key);
				config.setServiceName(parseStringValue(row, rowLen, rowNum, SERVICE_NAME.getCode()));
				config.setServiceGroup(parseStringValue(row, rowLen, rowNum, SERVICE_GROUP.getCode()));
				config.setBacnetNetworkName(
						parseStringValue(row, rowLen, rowNum, NETWORK_NAME.getCode()));
				config.setSampleCacheMs(parseLongValue(row, rowLen, rowNum, SAMPLE_CACHE.getCode()));
			}

			BacnetWritePropertyConfig propConfig = new BacnetWritePropertyConfig();
			propConfig.setControlId(parseStringValue(row, rowLen, rowNum, CONTROL_ID.getCode()));
			propConfig.setControlPropertyType(
					parseControlPropertyTypeValue(row, rowLen, rowNum, PROP_TYPE.getCode()));
			propConfig.setDeviceId(parseIntegerValue(row, rowLen, rowNum, DEVICE_ID.getCode()));
			propConfig.setObjectTypeValue(parseStringValue(row, rowLen, rowNum, OBJECT_TYPE.getCode()));
			propConfig.setObjectNumber(parseIntegerValue(row, rowLen, rowNum, OBJECT_NUMBER.getCode()));
			propConfig.setPropertyIdValue(parseStringValue(row, rowLen, rowNum, PROPERTY_ID.getCode()));
			propConfig.setPriority(parseIntegerValue(row, rowLen, rowNum, PRIORITY.getCode()));
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

	private String rowKeyValue(CsvRecord row, BacnetControlConfig currentConfig) {
		String key = row.getField(INSTANCE_ID.getCode());
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

}
