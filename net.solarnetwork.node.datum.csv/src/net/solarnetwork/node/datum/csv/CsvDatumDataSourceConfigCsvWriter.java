/* ==================================================================
 * CsvDatumDataSourceConfigCsvWriter.java - 1/04/2023 1:54:55 pm
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

import static java.util.Arrays.fill;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import de.siegmar.fastcsv.writer.CsvWriter;
import net.solarnetwork.domain.CodedValue;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.util.ObjectUtils;

/**
 * Generate CSV datum source configuration CSV from settings.
 *
 * @author matt
 * @version 2.0
 */
public class CsvDatumDataSourceConfigCsvWriter {

	private final CsvWriter writer;
	private final boolean locationMode;
	private final CodedValue[] cols;
	private final int rowLen;

	/**
	 * Constructor.
	 *
	 * @param writer
	 *        the writer; note the comment character should be set to something
	 *        <b>other</b> than {@code #} so comments can be generated manually
	 * @param locationMode
	 *        {@literal true} to generate location-based datum source
	 *        configuration
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 * @throws UncheckedIOException
	 *         if any IO error occurs
	 */
	public CsvDatumDataSourceConfigCsvWriter(CsvWriter writer, boolean locationMode)
			throws UncheckedIOException {
		super();
		this.writer = ObjectUtils.requireNonNullArgument(writer, "writer");
		this.locationMode = locationMode;
		this.cols = (locationMode ? CsvLocationDatumDataSourceCsvColumn.values()
				: CsvDatumDataSourceCsvColumn.values());

		rowLen = cols.length;
		String[] row = new String[rowLen];
		for ( CodedValue col : cols ) {
			row[col.getCode()] = col.toString();
		}
		writer.writeRecord(row);
	}

	/**
	 * Generate CSV datum source CSV from settings.
	 *
	 * @param factoryId
	 *        the CSV component factory ID
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
		CsvDatumDataSourceConfig config = new CsvDatumDataSourceConfig(locationMode);
		config.setKey(instanceId);
		config.setCharsetName(StandardCharsets.UTF_8.name());
		config.setSkipRows(CsvDatumDataSource.DEFAULT_SKIP_ROWS);
		config.setKeepRows(CsvDatumDataSource.DEFAULT_KEEP_ROWS);
		for ( Setting s : settings ) {
			config.populateFromSetting(s);
		}
		String[] row = new String[rowLen];
		if ( locationMode ) {
			row[CsvLocationDatumDataSourceCsvColumn.INSTANCE_ID.getCode()] = config.getKey();
			row[CsvLocationDatumDataSourceCsvColumn.SERVICE_NAME.getCode()] = config.getServiceName();
			row[CsvLocationDatumDataSourceCsvColumn.SERVICE_GROUP.getCode()] = config.getServiceGroup();
			row[CsvLocationDatumDataSourceCsvColumn.LOCATION_KEY.getCode()] = config.getLocationKey();
			row[CsvLocationDatumDataSourceCsvColumn.LOCATION_TYPE.getCode()] = config.getLocationType();
			row[CsvLocationDatumDataSourceCsvColumn.SCHEDULE.getCode()] = config.getSchedule();
			row[CsvLocationDatumDataSourceCsvColumn.URL.getCode()] = config.getUrl();
			row[CsvLocationDatumDataSourceCsvColumn.HTTP_CUSTOMIZER.getCode()] = config
					.getHttpCustomizer();
			row[CsvLocationDatumDataSourceCsvColumn.ENCODING.getCode()] = config.getCharsetName();
			row[CsvLocationDatumDataSourceCsvColumn.TIMEOUT
					.getCode()] = config.getConnectionTimeout() != null
							? config.getConnectionTimeout().toString()
							: null;
			row[CsvLocationDatumDataSourceCsvColumn.SKIP_ROWS.getCode()] = config.getSkipRows() != null
					? config.getSkipRows().toString()
					: null;
			row[CsvLocationDatumDataSourceCsvColumn.KEEP_ROWS.getCode()] = config.getKeepRows() != null
					? config.getKeepRows().toString()
					: null;
			row[CsvLocationDatumDataSourceCsvColumn.URL_DATE_FORMAT.getCode()] = config
					.getUrlDateFormat();
			row[CsvLocationDatumDataSourceCsvColumn.DATE_COLUMN.getCode()] = config.getDateTimeColumn();
			row[CsvLocationDatumDataSourceCsvColumn.DATE_FORMAT.getCode()] = config.getDateFormat();
			row[CsvLocationDatumDataSourceCsvColumn.TIME_ZONE.getCode()] = config.getTimeZoneId();
			row[CsvLocationDatumDataSourceCsvColumn.SAMPLE_CACHE
					.getCode()] = config.getSampleCacheMs() != null
							? config.getSampleCacheMs().toString()
							: null;
		} else {
			row[CsvDatumDataSourceCsvColumn.INSTANCE_ID.getCode()] = config.getKey();
			row[CsvDatumDataSourceCsvColumn.SERVICE_NAME.getCode()] = config.getServiceName();
			row[CsvDatumDataSourceCsvColumn.SERVICE_GROUP.getCode()] = config.getServiceGroup();
			row[CsvDatumDataSourceCsvColumn.SOURCE_ID.getCode()] = config.getSourceId();
			row[CsvDatumDataSourceCsvColumn.SOURCE_ID_COLUMN.getCode()] = config.getSourceIdColumn();
			row[CsvDatumDataSourceCsvColumn.SCHEDULE.getCode()] = config.getSchedule();
			row[CsvDatumDataSourceCsvColumn.URL.getCode()] = config.getUrl();
			row[CsvDatumDataSourceCsvColumn.HTTP_CUSTOMIZER.getCode()] = config.getHttpCustomizer();
			row[CsvDatumDataSourceCsvColumn.ENCODING.getCode()] = config.getCharsetName();
			row[CsvDatumDataSourceCsvColumn.TIMEOUT.getCode()] = config.getConnectionTimeout() != null
					? config.getConnectionTimeout().toString()
					: null;
			row[CsvDatumDataSourceCsvColumn.SKIP_ROWS.getCode()] = config.getSkipRows() != null
					? config.getSkipRows().toString()
					: null;
			row[CsvDatumDataSourceCsvColumn.KEEP_ROWS.getCode()] = config.getKeepRows() != null
					? config.getKeepRows().toString()
					: null;
			row[CsvDatumDataSourceCsvColumn.URL_DATE_FORMAT.getCode()] = config.getUrlDateFormat();
			row[CsvDatumDataSourceCsvColumn.DATE_COLUMN.getCode()] = config.getDateTimeColumn();
			row[CsvDatumDataSourceCsvColumn.DATE_FORMAT.getCode()] = config.getDateFormat();
			row[CsvDatumDataSourceCsvColumn.TIME_ZONE.getCode()] = config.getTimeZoneId();
			row[CsvDatumDataSourceCsvColumn.SAMPLE_CACHE.getCode()] = config.getSampleCacheMs() != null
					? config.getSampleCacheMs().toString()
					: null;
		}

		for ( CsvPropertyConfig propConfig : config.getPropertyConfigs() ) {
			if ( locationMode ) {
				row[CsvLocationDatumDataSourceCsvColumn.COLUMN.getCode()] = propConfig.getColumn();
				row[CsvLocationDatumDataSourceCsvColumn.PROP_NAME.getCode()] = propConfig
						.getPropertyKey();
				row[CsvLocationDatumDataSourceCsvColumn.PROP_TYPE.getCode()] = propertyTypeValue(
						propConfig.getPropertyType());
				row[CsvLocationDatumDataSourceCsvColumn.MULTIPLIER
						.getCode()] = (propConfig.getSlope() != null
								? propConfig.getSlope().toPlainString()
								: null);
				row[CsvLocationDatumDataSourceCsvColumn.OFFSET
						.getCode()] = (propConfig.getIntercept() != null
								? propConfig.getIntercept().toPlainString()
								: null);
				row[CsvLocationDatumDataSourceCsvColumn.DECIMAL_SCALE.getCode()] = String
						.valueOf(propConfig.getDecimalScale());
			} else {
				row[CsvDatumDataSourceCsvColumn.COLUMN.getCode()] = propConfig.getColumn();
				row[CsvDatumDataSourceCsvColumn.PROP_NAME.getCode()] = propConfig.getPropertyKey();
				row[CsvDatumDataSourceCsvColumn.PROP_TYPE.getCode()] = propertyTypeValue(
						propConfig.getPropertyType());
				row[CsvDatumDataSourceCsvColumn.MULTIPLIER.getCode()] = (propConfig.getSlope() != null
						? propConfig.getSlope().toPlainString()
						: null);
				row[CsvDatumDataSourceCsvColumn.OFFSET.getCode()] = (propConfig.getIntercept() != null
						? propConfig.getIntercept().toPlainString()
						: null);
				row[CsvDatumDataSourceCsvColumn.DECIMAL_SCALE.getCode()] = String
						.valueOf(propConfig.getDecimalScale());
			}
			writer.writeRecord(row);
			fill(row, null);
		}
	}

	private static String propertyTypeValue(DatumSamplesType type) {
		if ( type == null ) {
			return null;
		}
		return type.toString();
	}

}
