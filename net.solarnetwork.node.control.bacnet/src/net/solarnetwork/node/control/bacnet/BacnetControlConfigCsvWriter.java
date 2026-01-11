/* ==================================================================
 * BacnetControlConfigCsvWriter.java - 10/11/2022 10:16:18 am
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

import static java.util.Arrays.fill;
import java.io.UncheckedIOException;
import java.util.List;
import de.siegmar.fastcsv.writer.CsvWriter;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.util.ObjectUtils;

/**
 * Generate BACnet Control configuration CSV from settings.
 *
 * @author matt
 * @version 2.0
 */
public class BacnetControlConfigCsvWriter {

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
	public BacnetControlConfigCsvWriter(CsvWriter writer) throws UncheckedIOException {
		super();
		this.writer = ObjectUtils.requireNonNullArgument(writer, "writer");
		rowLen = BacnetControlCsvColumn.values().length;
		String[] row = new String[rowLen];
		for ( BacnetControlCsvColumn col : BacnetControlCsvColumn.values() ) {
			row[col.getCode()] = col.getName();
		}
		writer.writeRecord(row);
	}

	/**
	 * Generate Bacnet Device CSV from settings.
	 *
	 * @param factoryId
	 *        the Bacnet Device factory ID
	 * @param instanceId
	 *        the instance ID
	 * @param settings
	 *        the settings to generate CSV for
	 * @throws UncheckedIOException
	 *         if any IO error occurs
	 */
	public void generateCsv(String factoryId, String instanceId, List<Setting> settings)
			throws UncheckedIOException {
		if ( settings == null || settings.isEmpty() ) {
			return;
		}
		BacnetControlConfig config = new BacnetControlConfig();
		config.setKey(instanceId);
		for ( Setting s : settings ) {
			config.populateFromSetting(s);
		}
		String[] row = new String[rowLen];
		row[BacnetControlCsvColumn.INSTANCE_ID.getCode()] = config.getKey();
		row[BacnetControlCsvColumn.SERVICE_NAME.getCode()] = config.getServiceName();
		row[BacnetControlCsvColumn.SERVICE_GROUP.getCode()] = config.getServiceGroup();
		row[BacnetControlCsvColumn.NETWORK_NAME.getCode()] = config.getBacnetNetworkName();
		row[BacnetControlCsvColumn.SAMPLE_CACHE.getCode()] = (config.getSampleCacheMs() != null
				? config.getSampleCacheMs().toString()
				: null);

		for ( BacnetWritePropertyConfig propConfig : config.getPropertyConfigs() ) {
			row[BacnetControlCsvColumn.CONTROL_ID.getCode()] = propConfig.getControlId();
			row[BacnetControlCsvColumn.PROP_TYPE.getCode()] = controlPropertyTypeValue(
					propConfig.getControlPropertyType());
			row[BacnetControlCsvColumn.DEVICE_ID.getCode()] = (propConfig.getDeviceId() != null
					? propConfig.getDeviceId().toString()
					: null);
			row[BacnetControlCsvColumn.OBJECT_TYPE.getCode()] = propConfig.getObjectTypeValue();
			row[BacnetControlCsvColumn.OBJECT_NUMBER.getCode()] = (propConfig.getObjectNumber() != null
					? propConfig.getObjectNumber().toString()
					: null);
			row[BacnetControlCsvColumn.PROPERTY_ID.getCode()] = propConfig.getPropertyIdValue();
			row[BacnetControlCsvColumn.PRIORITY.getCode()] = (propConfig.getPriority() != null
					? propConfig.getPriority().toString()
					: null);
			row[BacnetControlCsvColumn.MULTIPLIER.getCode()] = (propConfig.getUnitMultiplier() != null
					? propConfig.getUnitMultiplier().toPlainString()
					: null);
			row[BacnetControlCsvColumn.DECIMAL_SCALE.getCode()] = (propConfig.getDecimalScale() != null
					? propConfig.getDecimalScale().toString()
					: null);
			writer.writeRecord(row);
			fill(row, null);
		}
	}

	private static String controlPropertyTypeValue(NodeControlPropertyType type) {
		if ( type == null ) {
			return null;
		}
		return type.toString();
	}

}
