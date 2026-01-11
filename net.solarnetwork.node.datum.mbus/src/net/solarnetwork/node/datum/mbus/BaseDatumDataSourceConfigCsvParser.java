/* ==================================================================
 * BaseDatumDataSourceConfigCsvParser.java - 30/09/2022 2:49:16 pm
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

package net.solarnetwork.node.datum.mbus;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import de.siegmar.fastcsv.reader.CsvRecord;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.io.mbus.MBusDataDescription;
import net.solarnetwork.node.io.mbus.MBusDataType;

/**
 * Base class for CSV parsing.
 *
 * @author matt
 * @version 2.0
 * @since 1.1
 */
public abstract class BaseDatumDataSourceConfigCsvParser {

	/** The message source to use for errors. */
	protected final MessageSource messageSource;

	/** A list of error messages. */
	protected final List<String> messages;

	/**
	 * Constructor.
	 *
	 * @param messageSource
	 *        the message source
	 * @param messages
	 *        the list of output messages to add messages to
	 */
	public BaseDatumDataSourceConfigCsvParser(MessageSource messageSource, List<String> messages) {
		super();
		this.messageSource = requireNonNullArgument(messageSource, "messageSource");
		this.messages = requireNonNullArgument(messages, "messages");
	}

	/**
	 * Get the instance ID value from a CSV row.
	 *
	 * @param row
	 *        the row
	 * @param results
	 *        the current list of parsed configurations
	 * @param currentConfig
	 *        the current configuration
	 * @return the instance ID
	 */
	protected String rowKeyValue(CsvRecord row, List<? extends BaseDatumDataSourceConfig> results,
			BaseDatumDataSourceConfig currentConfig) {
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

	/**
	 * Parse a string column value.
	 *
	 * @param row
	 *        the row
	 * @param rowLen
	 *        the row length
	 * @param rowNum
	 *        the row offset
	 * @param colNum
	 *        the column to parse
	 * @return the parsed column value, or {@literal null} if the column is
	 *         missing or empty
	 */
	protected String parseStringValue(CsvRecord row, int rowLen, long rowNum, int colNum) {
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

	/**
	 * Parse an integer column value.
	 *
	 * @param row
	 *        the row
	 * @param rowLen
	 *        the row length
	 * @param rowNum
	 *        the row offset
	 * @param colNum
	 *        the column to parse
	 * @return the parsed column value, or {@literal null} if the column value
	 *         cannot be parsed as an Integer
	 */
	protected Integer parseIntegerValue(CsvRecord row, int rowLen, long rowNum, int colNum) {
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

	/**
	 * Parse a long column value.
	 *
	 * @param row
	 *        the row
	 * @param rowLen
	 *        the row length
	 * @param rowNum
	 *        the row offset
	 * @param colNum
	 *        the column to parse
	 * @return the parsed column value, or {@literal null} if the column value
	 *         cannot be parsed as an Long
	 */
	protected Long parseLongValue(CsvRecord row, int rowLen, long rowNum, int colNum) {
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

	/**
	 * Parse a decimal column value.
	 *
	 * @param row
	 *        the row
	 * @param rowLen
	 *        the row length
	 * @param rowNum
	 *        the row offset
	 * @param colNum
	 *        the column to parse
	 * @return the parsed column value, or {@literal null} if the column value
	 *         cannot be parsed as an BigDecimal
	 */
	protected BigDecimal parseBigDecimalValue(CsvRecord row, int rowLen, long rowNum, int colNum) {
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

	/**
	 * Parse a datum samples type column value.
	 *
	 * @param row
	 *        the row
	 * @param rowLen
	 *        the row length
	 * @param rowNum
	 *        the row offset
	 * @param colNum
	 *        the column to parse
	 * @return the parsed column value, or {@literal null} if the column value
	 *         cannot be parsed as a DatumSamplesType
	 */
	protected DatumSamplesType parseDatumSamplesTypeValue(CsvRecord row, int rowLen, long rowNum,
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

	/**
	 * Parse a MBus data type column value.
	 *
	 * @param row
	 *        the row
	 * @param rowLen
	 *        the row length
	 * @param rowNum
	 *        the row offset
	 * @param colNum
	 *        the column to parse
	 * @return the parsed column value, or {@literal null} if the column value
	 *         cannot be parsed as an MBusDataType
	 */
	protected MBusDataType parseMBusDataTypeValue(CsvRecord row, int rowLen, long rowNum, int colNum) {
		String s = parseStringValue(row, rowLen, rowNum, colNum);
		if ( s == null ) {
			return null;
		}
		try {
			return MBusDataType.valueOf(s);
		} catch ( IllegalArgumentException e ) {
			messages.add(messageSource.getMessage("message.dataTypeFormatError",
					new Object[] { s, rowNum, colNum }, "Malformed data type value.",
					Locale.getDefault()));
		}
		return null;
	}

	/**
	 * Parse a MBus data description column value.
	 *
	 * @param row
	 *        the row
	 * @param rowLen
	 *        the row length
	 * @param rowNum
	 *        the row offset
	 * @param colNum
	 *        the column to parse
	 * @return the parsed column value, or {@literal null} if the column value
	 *         cannot be parsed as an MBusDataDescription
	 */
	protected MBusDataDescription parseMBusDataDescriptionValue(CsvRecord row, int rowLen, long rowNum,
			int colNum) {
		String s = parseStringValue(row, rowLen, rowNum, colNum);
		if ( s == null ) {
			return null;
		}
		try {
			return MBusDataDescription.valueOf(s);
		} catch ( IllegalArgumentException e ) {
			messages.add(messageSource.getMessage("message.dataDescriptionFormatError",
					new Object[] { s, rowNum, colNum }, "Malformed data description value.",
					Locale.getDefault()));
		}
		return null;
	}

}
