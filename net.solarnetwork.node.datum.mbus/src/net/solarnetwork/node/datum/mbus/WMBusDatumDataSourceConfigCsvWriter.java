/* ==================================================================
 * WMBusDatumDataSourceConfigCsvWriter.java - 30/09/2022 4:28:55 pm
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

import static java.util.Arrays.fill;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.util.List;
import de.siegmar.fastcsv.writer.CsvWriter;
import net.solarnetwork.node.domain.Setting;

/**
 * Generate M-Bus Wireless Device configuration CSV from settings.
 *
 * @author matt
 * @version 2.0
 * @since 1.1
 */
public class WMBusDatumDataSourceConfigCsvWriter {

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
	 * @throws IOException
	 *         if any IO error occurs
	 */
	public WMBusDatumDataSourceConfigCsvWriter(CsvWriter writer) throws IOException {
		super();
		this.writer = requireNonNullArgument(writer, "writer");
		rowLen = WMBusCsvColumn.values().length;
		String[] row = new String[rowLen];
		for ( WMBusCsvColumn col : WMBusCsvColumn.values() ) {
			row[col.getCode()] = col.getName();
		}
		writer.writeRecord(row);
	}

	/**
	 * Generate M-Bus Wireless Device CSV from settings.
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
		WMBusDatumDataSourceConfig config = new WMBusDatumDataSourceConfig();
		config.setKey(instanceId);
		for ( Setting s : settings ) {
			config.populateFromSetting(s);
		}
		String[] row = new String[rowLen];
		row[WMBusCsvColumn.INSTANCE_ID.getCode()] = config.getKey();
		row[WMBusCsvColumn.SERVICE_NAME.getCode()] = config.getServiceName();
		row[WMBusCsvColumn.SERVICE_GROUP.getCode()] = config.getServiceGroup();
		row[WMBusCsvColumn.SOURCE_ID.getCode()] = config.getSourceId();
		row[WMBusCsvColumn.SCHEDULE.getCode()] = config.getSchedule();
		row[WMBusCsvColumn.NETWORK_NAME.getCode()] = config.getNetworkName();
		row[WMBusCsvColumn.ADDRESS.getCode()] = config.getAddress();
		row[WMBusCsvColumn.KEY.getCode()] = config.getDecryptionKey();

		for ( MBusPropertyConfig propConfig : config.getPropertyConfigs() ) {
			row[WMBusCsvColumn.PROP_NAME.getCode()] = propConfig.getName();
			row[WMBusCsvColumn.PROP_TYPE.getCode()] = propConfig.getPropertyTypeKey();
			row[WMBusCsvColumn.DATA_TYPE.getCode()] = propConfig.getDataTypeKey();
			row[WMBusCsvColumn.DATA_DESCRIPTION.getCode()] = propConfig.getDataDescriptionKey();
			row[WMBusCsvColumn.MULTIPLIER.getCode()] = (propConfig.getUnitMultiplier() != null
					? propConfig.getUnitMultiplier().toPlainString()
					: null);
			row[WMBusCsvColumn.DECIMAL_SCALE.getCode()] = (propConfig.getDecimalScale() != null
					? propConfig.getDecimalScale().toString()
					: null);
			writer.writeRecord(row);
			fill(row, null);
		}
	}

}
