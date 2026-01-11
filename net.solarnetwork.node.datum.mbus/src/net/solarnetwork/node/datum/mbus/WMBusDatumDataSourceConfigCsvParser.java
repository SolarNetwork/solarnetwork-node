/* ==================================================================
 * MBusDatumDataSourceConfigCsvParser.java - 30/09/2022 12:01:56 pm
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

import static net.solarnetwork.node.datum.mbus.WMBusCsvColumn.ADDRESS;
import static net.solarnetwork.node.datum.mbus.WMBusCsvColumn.DATA_DESCRIPTION;
import static net.solarnetwork.node.datum.mbus.WMBusCsvColumn.DATA_TYPE;
import static net.solarnetwork.node.datum.mbus.WMBusCsvColumn.DECIMAL_SCALE;
import static net.solarnetwork.node.datum.mbus.WMBusCsvColumn.KEY;
import static net.solarnetwork.node.datum.mbus.WMBusCsvColumn.MULTIPLIER;
import static net.solarnetwork.node.datum.mbus.WMBusCsvColumn.NETWORK_NAME;
import static net.solarnetwork.node.datum.mbus.WMBusCsvColumn.PROP_NAME;
import static net.solarnetwork.node.datum.mbus.WMBusCsvColumn.PROP_TYPE;
import static net.solarnetwork.node.datum.mbus.WMBusCsvColumn.SCHEDULE;
import static net.solarnetwork.node.datum.mbus.WMBusCsvColumn.SERVICE_GROUP;
import static net.solarnetwork.node.datum.mbus.WMBusCsvColumn.SERVICE_NAME;
import static net.solarnetwork.node.datum.mbus.WMBusCsvColumn.SOURCE_ID;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;

/**
 * Parse CSV data into {@link MBusDatumDataSourceConfig} instances.
 *
 * @author matt
 * @version 2.0
 * @since 1.1
 */
public class WMBusDatumDataSourceConfigCsvParser extends BaseDatumDataSourceConfigCsvParser {

	final List<WMBusDatumDataSourceConfig> results;

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
	public WMBusDatumDataSourceConfigCsvParser(List<WMBusDatumDataSourceConfig> results,
			MessageSource messageSource, List<String> messages) {
		super(messageSource, messages);
		this.results = requireNonNullArgument(results, "results");
	}

	/**
	 * Parse CSV.
	 *
	 * @param csv
	 *        the CSV to parse; note that the comment strategy should be set to
	 *        {@link CommentStrategy#NONE} so comments can be handled as
	 * @throws IOException
	 *         if any IO error occurs
	 */
	public void parse(CsvReader<CsvRecord> csv) throws IOException {
		if ( csv == null ) {
			return;
		}
		csv.skipLines(1);
		WMBusDatumDataSourceConfig config = null;
		for ( CsvRecord row : csv ) {
			final int rowLen = row.getFieldCount();
			final long rowNum = row.getStartingLineNumber();
			final String key = rowKeyValue(row, results, config);
			if ( key == null || key.startsWith("#") ) {
				// either a comment line, or empty key but no active configuration
				continue;
			}
			if ( config == null || (key != null && !key.equals(config.getKey())) ) {
				// starting new config
				config = new WMBusDatumDataSourceConfig();
				results.add(config);
				config.setKey(key);
				popoulateBaseConfig(row, rowLen, rowNum, config);
				config.setAddress(parseStringValue(row, rowLen, rowNum, ADDRESS.getCode()));
				config.setDecryptionKey(parseStringValue(row, rowLen, rowNum, KEY.getCode()));
			}

			MBusPropertyConfig propConfig = parseMBusPropertyConfig(row, rowLen, rowNum);
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

	private void popoulateBaseConfig(final CsvRecord row, final int rowLen, final long rowNum,
			final BaseDatumDataSourceConfig config) {
		config.setServiceName(parseStringValue(row, rowLen, rowNum, SERVICE_NAME.getCode()));
		config.setServiceGroup(parseStringValue(row, rowLen, rowNum, SERVICE_GROUP.getCode()));
		config.setSourceId(parseStringValue(row, rowLen, rowNum, SOURCE_ID.getCode()));
		config.setSchedule(parseStringValue(row, rowLen, rowNum, SCHEDULE.getCode()));
		config.setNetworkName(parseStringValue(row, rowLen, rowNum, NETWORK_NAME.getCode()));
	}

	private MBusPropertyConfig parseMBusPropertyConfig(final CsvRecord row, final int rowLen,
			final long rowNum) {
		MBusPropertyConfig propConfig = new MBusPropertyConfig();
		propConfig.setName(parseStringValue(row, rowLen, rowNum, PROP_NAME.getCode()));
		propConfig.setPropertyType(parseDatumSamplesTypeValue(row, rowLen, rowNum, PROP_TYPE.getCode()));
		propConfig.setDataType(parseMBusDataTypeValue(row, rowLen, rowNum, DATA_TYPE.getCode()));
		propConfig.setDataDescription(
				parseMBusDataDescriptionValue(row, rowLen, rowNum, DATA_DESCRIPTION.getCode()));
		propConfig.setUnitMultiplier(parseBigDecimalValue(row, rowLen, rowNum, MULTIPLIER.getCode()));
		propConfig.setDecimalScale(parseIntegerValue(row, rowLen, rowNum, DECIMAL_SCALE.getCode()));
		return propConfig;
	}

}
