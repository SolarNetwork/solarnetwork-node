/* ==================================================================
 * BacnetDatumDataSourceConfigCsvWriter.java - 9/11/2022 7:32:59 am
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

package net.solarnetwork.node.datum.bacnet;

import static java.util.Arrays.fill;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import de.siegmar.fastcsv.writer.CsvWriter;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.util.ObjectUtils;

/**
 * Generate BACnet Device configuration CSV from settings
 *
 * @author matt
 * @version 2.0
 */
public class BacnetDatumDataSourceConfigCsvWriter {

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
	public BacnetDatumDataSourceConfigCsvWriter(CsvWriter writer) throws UncheckedIOException {
		super();
		this.writer = ObjectUtils.requireNonNullArgument(writer, "writer");
		rowLen = BacnetCsvColumn.values().length;
		String[] row = new String[rowLen];
		for ( BacnetCsvColumn col : BacnetCsvColumn.values() ) {
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
		BacnetDatumDataSourceConfig config = new BacnetDatumDataSourceConfig();
		config.setKey(instanceId);
		for ( Setting s : settings ) {
			config.populateFromSetting(s);
		}
		String[] row = new String[rowLen];
		row[BacnetCsvColumn.INSTANCE_ID.getCode()] = config.getKey();
		row[BacnetCsvColumn.SERVICE_NAME.getCode()] = config.getServiceName();
		row[BacnetCsvColumn.SERVICE_GROUP.getCode()] = config.getServiceGroup();
		row[BacnetCsvColumn.SOURCE_ID.getCode()] = config.getSourceId();
		row[BacnetCsvColumn.SCHEDULE.getCode()] = config.getSchedule();
		row[BacnetCsvColumn.NETWORK_NAME.getCode()] = config.getBacnetNetworkName();
		row[BacnetCsvColumn.PERSIST_MODE.getCode()] = datumModeValue(config.getDatumMode());
		row[BacnetCsvColumn.SAMPLE_CACHE.getCode()] = (config.getSampleCacheMs() != null
				? config.getSampleCacheMs().toString()
				: null);

		for ( BacnetDeviceConfig deviceConfig : config.getDeviceConfigs() ) {
			row[BacnetCsvColumn.DEVICE_ID.getCode()] = (deviceConfig.getDeviceId() != null
					? deviceConfig.getDeviceId().toString()
					: null);
			BacnetPropertyConfig[] propConfigs = deviceConfig.getPropConfigs();
			if ( propConfigs == null ) {
				continue;
			}
			for ( BacnetPropertyConfig propConfig : propConfigs ) {
				row[BacnetCsvColumn.OBJECT_TYPE.getCode()] = propConfig.getObjectTypeValue();
				row[BacnetCsvColumn.OBJECT_NUMBER.getCode()] = (propConfig.getObjectNumber() != null
						? propConfig.getObjectNumber().toString()
						: null);
				row[BacnetCsvColumn.PROPERTY_ID.getCode()] = propConfig.getPropertyIdValue();
				row[BacnetCsvColumn.COV_INCREMENT.getCode()] = (propConfig.getCovIncrement() != null
						? propConfig.getCovIncrement().toString()
						: null);
				row[BacnetCsvColumn.PROP_NAME.getCode()] = propConfig.getPropertyKey();
				row[BacnetCsvColumn.PROP_TYPE.getCode()] = (propConfig.getPropertyType() != null
						? propConfig.getPropertyType().name()
						: null);
				row[BacnetCsvColumn.MULTIPLIER.getCode()] = (propConfig.getSlope() != null
						? propConfig.getSlope().toPlainString()
						: null);
				row[BacnetCsvColumn.DECIMAL_SCALE.getCode()] = String
						.valueOf(propConfig.getDecimalScale());
				writer.writeRecord(row);
				fill(row, null);
			}
		}
	}

	private String datumModeValue(BacnetDatumMode mode) {
		if ( mode == null ) {
			return null;
		}
		switch (mode) {
			case EventAndPoll:
				return "Events and polling";

			case EventOnly:
				return "Events only";

			default:
				return "Polling only";
		}
	}

}
