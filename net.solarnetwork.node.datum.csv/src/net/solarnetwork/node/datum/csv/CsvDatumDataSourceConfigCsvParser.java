/* ==================================================================
 * CsvDatumDataSourceConfigCsvParser.java - 1/04/2023 4:38:07 pm
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

package net.solarnetwork.node.datum.csv;

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

/**
 * Parse CSV data into {@link CsvDatumDataSourceConfig} instances.
 *
 * @author matt
 * @version 2.0
 */
public class CsvDatumDataSourceConfigCsvParser {

	private final List<CsvDatumDataSourceConfig> results;
	private final MessageSource messageSource;
	private final List<String> messages;
	private final boolean locationMode;

	/**
	 * Constructor.
	 *
	 * @param results
	 *        the list to add the parsed results to
	 * @param messageSource
	 *        the message source
	 * @param messages
	 *        the list of output messages to add messages to
	 * @param locationMode
	 *        {@literal true} to parse location configuration
	 */
	public CsvDatumDataSourceConfigCsvParser(List<CsvDatumDataSourceConfig> results,
			MessageSource messageSource, List<String> messages, boolean locationMode) {
		super();
		this.messageSource = requireNonNullArgument(messageSource, "messageSource");
		this.results = requireNonNullArgument(results, "results");
		this.messages = requireNonNullArgument(messages, "messages");
		this.locationMode = locationMode;
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
		CsvDatumDataSourceConfig config = null;
		for ( CsvRecord row : csv ) {
			final int rowLen = row.getFieldCount();
			final long rowNum = row.getStartingLineNumber();
			final String key = rowKeyValue(row, config);
			if ( key == null || key.startsWith("#") ) {
				// either a comment line, or empty key but no active configuration
				continue;
			}
			if ( config == null || (key != null && !key.equals(config.getKey())) ) {
				// starting new component config
				config = new CsvDatumDataSourceConfig(locationMode);
				results.add(config);
				config.setKey(key);
				if ( locationMode ) {
					parseCsvLocationResourceConfig(row, config, rowLen, rowNum);
				} else {
					parseCsvResourceConfig(row, config, rowLen, rowNum);
				}
			}

			CsvPropertyConfig propConfig = new CsvPropertyConfig();
			if ( locationMode ) {
				parseCsvLocationResourcePropertyConfig(row, rowLen, rowNum, propConfig);
			} else {
				parseCsvResourcePropertyConfig(row, rowLen, rowNum, propConfig);
			}
			if ( propConfig.isEmpty() ) {
				continue;
			}
			if ( propConfig.isValid() ) {
				config.getPropertyConfigs().add(propConfig);
			} else if ( propConfig.getPropertyKey() != null ) {
				messages.add(messageSource.getMessage("message.invalidPropertyConfig",
						new Object[] { rowNum }, "Invalid property configuration.",
						Locale.getDefault()));
			}
		}
	}

	private void parseCsvResourceConfig(CsvRecord row, CsvDatumDataSourceConfig config, final int rowLen,
			final long rowNum) {
		config.setServiceName(parseStringValue(row, rowLen, rowNum,
				CsvDatumDataSourceCsvColumn.SERVICE_NAME.getCode()));
		config.setServiceGroup(parseStringValue(row, rowLen, rowNum,
				CsvDatumDataSourceCsvColumn.SERVICE_GROUP.getCode()));
		config.setSourceId(
				parseStringValue(row, rowLen, rowNum, CsvDatumDataSourceCsvColumn.SOURCE_ID.getCode()));
		config.setSourceIdColumn(parseStringValue(row, rowLen, rowNum,
				CsvDatumDataSourceCsvColumn.SOURCE_ID_COLUMN.getCode()));
		config.setSchedule(
				parseStringValue(row, rowLen, rowNum, CsvDatumDataSourceCsvColumn.SCHEDULE.getCode()));
		config.setUrl(parseStringValue(row, rowLen, rowNum, CsvDatumDataSourceCsvColumn.URL.getCode()));
		config.setHttpCustomizer(parseStringValue(row, rowLen, rowNum,
				CsvDatumDataSourceCsvColumn.HTTP_CUSTOMIZER.getCode()));
		config.setCharsetName(
				parseStringValue(row, rowLen, rowNum, CsvDatumDataSourceCsvColumn.ENCODING.getCode()));
		config.setConnectionTimeout(
				parseIntegerValue(row, rowLen, rowNum, CsvDatumDataSourceCsvColumn.TIMEOUT.getCode()));
		config.setSkipRows(
				parseIntegerValue(row, rowLen, rowNum, CsvDatumDataSourceCsvColumn.SKIP_ROWS.getCode()));
		config.setKeepRows(
				parseIntegerValue(row, rowLen, rowNum, CsvDatumDataSourceCsvColumn.KEEP_ROWS.getCode()));
		config.setUrlDateFormat(parseStringValue(row, rowLen, rowNum,
				CsvDatumDataSourceCsvColumn.URL_DATE_FORMAT.getCode()));
		config.setDateTimeColumn(parseStringValue(row, rowLen, rowNum,
				CsvDatumDataSourceCsvColumn.DATE_COLUMN.getCode()));
		config.setDateFormat(parseStringValue(row, rowLen, rowNum,
				CsvDatumDataSourceCsvColumn.DATE_FORMAT.getCode()));
		config.setTimeZoneId(
				parseStringValue(row, rowLen, rowNum, CsvDatumDataSourceCsvColumn.TIME_ZONE.getCode()));
		config.setSampleCacheMs(
				parseLongValue(row, rowLen, rowNum, CsvDatumDataSourceCsvColumn.SAMPLE_CACHE.getCode()));
	}

	private void parseCsvLocationResourceConfig(CsvRecord row, CsvDatumDataSourceConfig config,
			final int rowLen, final long rowNum) {
		config.setServiceName(parseStringValue(row, rowLen, rowNum,
				CsvLocationDatumDataSourceCsvColumn.SERVICE_NAME.getCode()));
		config.setServiceGroup(parseStringValue(row, rowLen, rowNum,
				CsvLocationDatumDataSourceCsvColumn.SERVICE_GROUP.getCode()));
		config.setLocationKey(parseStringValue(row, rowLen, rowNum,
				CsvLocationDatumDataSourceCsvColumn.LOCATION_KEY.getCode()));
		config.setLocationType(parseStringValue(row, rowLen, rowNum,
				CsvLocationDatumDataSourceCsvColumn.LOCATION_TYPE.getCode()));
		config.setSchedule(parseStringValue(row, rowLen, rowNum,
				CsvLocationDatumDataSourceCsvColumn.SCHEDULE.getCode()));
		config.setUrl(parseStringValue(row, rowLen, rowNum,
				CsvLocationDatumDataSourceCsvColumn.URL.getCode()));
		config.setHttpCustomizer(parseStringValue(row, rowLen, rowNum,
				CsvLocationDatumDataSourceCsvColumn.HTTP_CUSTOMIZER.getCode()));
		config.setCharsetName(parseStringValue(row, rowLen, rowNum,
				CsvLocationDatumDataSourceCsvColumn.ENCODING.getCode()));
		config.setConnectionTimeout(parseIntegerValue(row, rowLen, rowNum,
				CsvLocationDatumDataSourceCsvColumn.TIMEOUT.getCode()));
		config.setSkipRows(parseIntegerValue(row, rowLen, rowNum,
				CsvLocationDatumDataSourceCsvColumn.SKIP_ROWS.getCode()));
		config.setKeepRows(parseIntegerValue(row, rowLen, rowNum,
				CsvLocationDatumDataSourceCsvColumn.KEEP_ROWS.getCode()));
		config.setUrlDateFormat(parseStringValue(row, rowLen, rowNum,
				CsvLocationDatumDataSourceCsvColumn.URL_DATE_FORMAT.getCode()));
		config.setDateTimeColumn(parseStringValue(row, rowLen, rowNum,
				CsvLocationDatumDataSourceCsvColumn.DATE_COLUMN.getCode()));
		config.setDateFormat(parseStringValue(row, rowLen, rowNum,
				CsvLocationDatumDataSourceCsvColumn.DATE_FORMAT.getCode()));
		config.setTimeZoneId(parseStringValue(row, rowLen, rowNum,
				CsvLocationDatumDataSourceCsvColumn.TIME_ZONE.getCode()));
		config.setSampleCacheMs(parseLongValue(row, rowLen, rowNum,
				CsvLocationDatumDataSourceCsvColumn.SAMPLE_CACHE.getCode()));
	}

	private void parseCsvResourcePropertyConfig(CsvRecord row, final int rowLen, final long rowNum,
			CsvPropertyConfig propConfig) {
		propConfig.setColumn(
				parseStringValue(row, rowLen, rowNum, CsvDatumDataSourceCsvColumn.COLUMN.getCode()));
		propConfig.setPropertyKey(
				parseStringValue(row, rowLen, rowNum, CsvDatumDataSourceCsvColumn.PROP_NAME.getCode()));
		propConfig.setPropertyType(parseDatumSamplesTypeValue(row, rowLen, rowNum,
				CsvDatumDataSourceCsvColumn.PROP_TYPE.getCode()));
		propConfig.setSlope(parseBigDecimalValue(row, rowLen, rowNum,
				CsvDatumDataSourceCsvColumn.MULTIPLIER.getCode()));
		propConfig.setIntercept(
				parseBigDecimalValue(row, rowLen, rowNum, CsvDatumDataSourceCsvColumn.OFFSET.getCode()));
		propConfig.setDecimalScale(parseIntegerValue(row, rowLen, rowNum,
				CsvDatumDataSourceCsvColumn.DECIMAL_SCALE.getCode()));
	}

	private void parseCsvLocationResourcePropertyConfig(CsvRecord row, final int rowLen,
			final long rowNum, CsvPropertyConfig propConfig) {
		propConfig.setColumn(parseStringValue(row, rowLen, rowNum,
				CsvLocationDatumDataSourceCsvColumn.COLUMN.getCode()));
		propConfig.setPropertyKey(parseStringValue(row, rowLen, rowNum,
				CsvLocationDatumDataSourceCsvColumn.PROP_NAME.getCode()));
		propConfig.setPropertyType(parseDatumSamplesTypeValue(row, rowLen, rowNum,
				CsvLocationDatumDataSourceCsvColumn.PROP_TYPE.getCode()));
		propConfig.setSlope(parseBigDecimalValue(row, rowLen, rowNum,
				CsvLocationDatumDataSourceCsvColumn.MULTIPLIER.getCode()));
		propConfig.setIntercept(parseBigDecimalValue(row, rowLen, rowNum,
				CsvLocationDatumDataSourceCsvColumn.OFFSET.getCode()));
		propConfig.setDecimalScale(parseIntegerValue(row, rowLen, rowNum,
				CsvLocationDatumDataSourceCsvColumn.DECIMAL_SCALE.getCode()));
	}

	private String rowKeyValue(CsvRecord row, CsvDatumDataSourceConfig currentConfig) {
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

}
