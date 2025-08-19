/* ==================================================================
 * DatumControlCenterConfigCsvParser.java - 19/08/2025 1:16:09 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.dnp3.impl;

import static net.solarnetwork.node.io.dnp3.impl.DatumControlCenterCsvColumn.ADDRESS;
import static net.solarnetwork.node.io.dnp3.impl.DatumControlCenterCsvColumn.CONNECTION_NAME;
import static net.solarnetwork.node.io.dnp3.impl.DatumControlCenterCsvColumn.DATUM_EVENTS;
import static net.solarnetwork.node.io.dnp3.impl.DatumControlCenterCsvColumn.DECIMAL_SCALE;
import static net.solarnetwork.node.io.dnp3.impl.DatumControlCenterCsvColumn.EVENT_CLASSES;
import static net.solarnetwork.node.io.dnp3.impl.DatumControlCenterCsvColumn.INDEX;
import static net.solarnetwork.node.io.dnp3.impl.DatumControlCenterCsvColumn.MEASUREMENT_TYPE;
import static net.solarnetwork.node.io.dnp3.impl.DatumControlCenterCsvColumn.MULTIPLIER;
import static net.solarnetwork.node.io.dnp3.impl.DatumControlCenterCsvColumn.POLL_FREQUENCY;
import static net.solarnetwork.node.io.dnp3.impl.DatumControlCenterCsvColumn.PROP_NAME;
import static net.solarnetwork.node.io.dnp3.impl.DatumControlCenterCsvColumn.PROP_TYPE;
import static net.solarnetwork.node.io.dnp3.impl.DatumControlCenterCsvColumn.SCHEDULE;
import static net.solarnetwork.node.io.dnp3.impl.DatumControlCenterCsvColumn.SERVICE_GROUP;
import static net.solarnetwork.node.io.dnp3.impl.DatumControlCenterCsvColumn.SERVICE_NAME;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.parseBoolean;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.supercsv.io.ICsvListReader;
import net.solarnetwork.codec.CsvUtils;
import net.solarnetwork.domain.CodedValue;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.io.dnp3.domain.ClassType;
import net.solarnetwork.node.io.dnp3.domain.DatumConfig;
import net.solarnetwork.node.io.dnp3.domain.MeasurementConfig;
import net.solarnetwork.node.io.dnp3.domain.MeasurementType;
import net.solarnetwork.util.IntRangeSet;

/**
 * Parse CSV data into {@link ModbusDatumDataSourceConfig} instances.
 *
 * @author matt
 * @version 1.0
 */
public class DatumControlCenterConfigCsvParser {

	private final List<DatumControlCenterConfig> results;
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
	public DatumControlCenterConfigCsvParser(List<DatumControlCenterConfig> results,
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
		List<String> row = null;
		DatumControlCenterConfig config = null;
		DatumConfig datumConfig = null;
		while ( (row = csv.read()) != null ) {
			if ( row.isEmpty() ) {
				continue;
			}
			final int rowLen = row.size();
			final int rowNum = csv.getRowNumber();
			final String key = rowKeyValue(row, config);
			if ( key == null || key.startsWith("#") ) {
				// either a comment line, or empty key but no active configuration
				continue;
			}
			if ( config == null || (key != null && !key.equals(config.getKey())) ) {
				// starting new overall configuration
				config = new DatumControlCenterConfig();
				results.add(config);
				config.setKey(key);
				config.setServiceName(parseStringValue(row, rowLen, rowNum, SERVICE_NAME));
				config.setServiceGroup(parseStringValue(row, rowLen, rowNum, SERVICE_GROUP));
				config.setConnectionName(parseStringValue(row, rowLen, rowNum, CONNECTION_NAME));
				config.setAddress(parseIntegerValue(row, rowLen, rowNum, ADDRESS));
				config.setUnsolicitedEventClasses(praseEventClasses(row, rowLen, rowNum, EVENT_CLASSES));
				config.setSchedule(parseStringValue(row, rowLen, rowNum, SCHEDULE));

				datumConfig = null;
			}

			final String sourceId = parseStringValue(row, rowLen, rowNum,
					DatumControlCenterCsvColumn.SOURCE_ID);

			if ( datumConfig == null && sourceId == null ) {
				continue;
			} else if ( datumConfig == null
					|| (sourceId != null && !sourceId.equals(datumConfig.getSourceId())) ) {
				// starting new datum configuration
				datumConfig = new DatumConfig();
				datumConfig.setSourceId(sourceId);
				datumConfig.setGenerateDatumOnEvents(
						parseBoolean(parseStringValue(row, rowLen, rowNum, DATUM_EVENTS)));
				datumConfig.setPollFrequencySeconds(parseLongValue(row, rowLen, rowNum, POLL_FREQUENCY));

				config.getDatumConfigs().add(datumConfig);
			}

			final String propName = parseStringValue(row, rowLen, rowNum, PROP_NAME);
			if ( propName == null ) {
				continue;
			}
			MeasurementConfig measConfig = new MeasurementConfig();
			measConfig.setPropertyName(propName);
			measConfig.setPropertyType(parseDatumSamplesType(row, rowLen, rowNum, PROP_TYPE));
			measConfig.setType(parseMeasurementType(row, rowLen, rowNum, MEASUREMENT_TYPE));
			measConfig.setIndex(parseIntegerValue(row, rowLen, rowNum, INDEX));
			measConfig.setUnitMultiplier(parseBigDecimalValue(row, rowLen, rowNum, MULTIPLIER));
			measConfig.setDecimalScale(parseIntegerValue(row, rowLen, rowNum, DECIMAL_SCALE,
					MeasurementConfig.DEFAULT_DECIMAL_SCALE));
			if ( measConfig.isValidForClient() ) {
				int measIdx = datumConfig.getMeasurementConfigsCount();
				datumConfig.setMeasurementConfigsCount(measIdx + 1);
				datumConfig.getMeasurementConfigs()[measIdx] = measConfig;
			}
		}
	}

	private String rowKeyValue(List<String> row, DatumControlCenterConfig currentConfig) {
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

	private String parseStringValue(List<String> row, int rowLen, int rowNum, CodedValue col) {
		if ( col.getCode() < rowLen ) {
			String s = row.get(col.getCode());
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

	private Integer parseIntegerValue(List<String> row, int rowLen, int rowNum, CodedValue col) {
		String s = parseStringValue(row, rowLen, rowNum, col);
		if ( s != null ) {
			try {
				return Integer.valueOf(s);
			} catch ( NumberFormatException e ) {
				messages.add(messageSource.getMessage("message.integerFormatError",
						new Object[] { s, rowNum, col.getCode() }, "Malformed integer value.",
						Locale.getDefault()));
			}
		}
		return null;
	}

	private int parseIntegerValue(List<String> row, int rowLen, int rowNum, CodedValue col,
			int defaultValue) {
		Integer intVal = parseIntegerValue(row, rowLen, rowNum, col);
		return (intVal != null ? intVal : defaultValue);
	}

	private Long parseLongValue(List<String> row, int rowLen, int rowNum, CodedValue col) {
		String s = parseStringValue(row, rowLen, rowNum, col);
		if ( s != null ) {
			try {
				return Long.valueOf(s);
			} catch ( NumberFormatException e ) {
				messages.add(messageSource.getMessage("message.integerFormatError",
						new Object[] { s, rowNum, col.getCode() }, "Malformed long value.",
						Locale.getDefault()));
			}
		}
		return null;
	}

	private BigDecimal parseBigDecimalValue(List<String> row, int rowLen, int rowNum, CodedValue col) {
		String s = parseStringValue(row, rowLen, rowNum, col);
		if ( s != null ) {
			try {
				return new BigDecimal(s);
			} catch ( NumberFormatException e ) {
				messages.add(messageSource.getMessage("message.decimalFormatError",
						new Object[] { s, rowNum, col.getCode() }, "Malformed decimal value.",
						Locale.getDefault()));
			}
		}
		return null;
	}

	private Set<ClassType> praseEventClasses(List<String> row, int rowLen, int rowNum, CodedValue col) {
		// parse into IntRangeSet, to support ranges like "1-3"
		IntRangeSet ranges = CsvUtils.parseColumnsReference(parseStringValue(row, rowLen, rowNum, col));
		if ( ranges == null || ranges.isEmpty() ) {
			return null;
		}
		// @formatter:off
		return ranges.stream()
				.filter(n -> n >= 1 && n <= 3)
				.map(n -> ClassType.forCode(n))
				.collect(Collectors.toCollection(TreeSet::new));
		// @formatter:on
	}

	private DatumSamplesType parseDatumSamplesType(List<String> row, int rowLen, int rowNum,
			CodedValue col) {
		String s = parseStringValue(row, rowLen, rowNum, col);
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
						new Object[] { s, rowNum, col.getCode() }, "Malformed property type value.",
						Locale.getDefault()));
			}
		}
		return null;
	}

	private MeasurementType parseMeasurementType(List<String> row, int rowLen, int rowNum,
			CodedValue col) {
		String s = parseStringValue(row, rowLen, rowNum, col);
		if ( s == null || s.isEmpty() ) {
			return null;
		}

		s = s.toLowerCase();

		try {
			if ( s.length() == 1 ) {
				return MeasurementType.forCode(s.charAt(0));
			}
			if ( s.contains("analog") ) {
				if ( s.contains("output") ) {
					return MeasurementType.AnalogOutputStatus;
				}
				return MeasurementType.AnalogInput;
			} else if ( s.contains("double") ) {
				return MeasurementType.DoubleBitBinaryInput;
			} else if ( s.contains("binary") ) {
				if ( s.contains("output") ) {
					return MeasurementType.BinaryOutputStatus;
				}
				return MeasurementType.BinaryInput;
			} else if ( s.contains("count") ) {
				if ( s.contains("froz") ) {
					return MeasurementType.FrozenCounter;
				}
				return MeasurementType.Counter;
			}
		} catch ( IllegalArgumentException e ) {
			// ignore and continue
		}
		return null;
	}

}
