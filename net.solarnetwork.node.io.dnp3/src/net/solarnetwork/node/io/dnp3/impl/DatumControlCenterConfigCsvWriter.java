/* ==================================================================
 * DatumControlCenterConfigCsvWriter.java - 19/08/2025 1:13:35 pm
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

import static java.util.Arrays.fill;
import java.io.IOException;
import java.util.List;
import org.supercsv.io.ICsvListWriter;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.io.dnp3.domain.DatumConfig;
import net.solarnetwork.node.io.dnp3.domain.MeasurementConfig;
import net.solarnetwork.util.ObjectUtils;

/**
 * Generate DNP3 Control Center configuration CSV from settings.
 *
 * @author matt
 * @version 1.0
 */
public class DatumControlCenterConfigCsvWriter {

	private final ICsvListWriter writer;
	private final int rowLen;

	/**
	 * Constructor.
	 *
	 * @param writer
	 *        the writer
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 * @throws IOException
	 *         if any IO error occurs
	 */
	public DatumControlCenterConfigCsvWriter(ICsvListWriter writer) throws IOException {
		super();
		this.writer = ObjectUtils.requireNonNullArgument(writer, "writer");
		rowLen = DatumControlCenterCsvColumn.values().length;
		String[] row = new String[rowLen];
		for ( DatumControlCenterCsvColumn col : DatumControlCenterCsvColumn.values() ) {
			row[col.getCode()] = col.getName();
		}
		writer.writeHeader(row);
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
		DatumControlCenterConfig config = new DatumControlCenterConfig();
		config.setKey(instanceId);
		for ( Setting s : settings ) {
			config.populateFromSetting(s);
		}
		String[] row = new String[rowLen];
		row[DatumControlCenterCsvColumn.INSTANCE_ID.getCode()] = config.getKey();
		row[DatumControlCenterCsvColumn.CONNECTION_NAME.getCode()] = config.getConnectionName();
		row[DatumControlCenterCsvColumn.EVENT_CLASSES.getCode()] = config.getUnsolicitedEventClassesValue();
		row[DatumControlCenterCsvColumn.SCHEDULE.getCode()] = config.getSchedule();

		for ( DatumConfig datumConfig : config.getDatumConfigs() ) {
			row[DatumControlCenterCsvColumn.SOURCE_ID.getCode()] = datumConfig.getSourceId();
			row[DatumControlCenterCsvColumn.DATUM_EVENTS.getCode()] = String
					.valueOf(datumConfig.isGenerateDatumOnEvents());
			row[DatumControlCenterCsvColumn.POLL_FREQUENCY
					.getCode()] = (datumConfig.getPollFrequencySeconds() != null
							? datumConfig.getPollFrequencySeconds().toString()
							: null);

			final MeasurementConfig[] measConfs = datumConfig.getMeasurementConfigs();
			if ( measConfs == null || measConfs.length < 1 ) {
				continue;
			}

			for ( int measIdx = 0; measIdx < measConfs.length; measIdx++ ) {
				MeasurementConfig measConf = measConfs[measIdx];
				row[DatumControlCenterCsvColumn.PROP_NAME.getCode()] = measConf.getPropertyName();
				row[DatumControlCenterCsvColumn.PROP_TYPE.getCode()] = (measConf.getPropertyType() != null
						? measConf.getPropertyType().name()
						: null);
				row[DatumControlCenterCsvColumn.MEASUREMENT_TYPE.getCode()] = (measConf.getType() != null
						? measConf.getType().getTitle()
						: null);
				row[DatumControlCenterCsvColumn.INDEX.getCode()] = (measConf.getIndex() != null
						? measConf.getIndex().toString()
						: null);
				row[DatumControlCenterCsvColumn.MULTIPLIER.getCode()] = (measConf.getIndex() != null
						? measConf.getIndex().toString()
						: null);
				row[DatumControlCenterCsvColumn.MULTIPLIER.getCode()] = (measConf.getUnitMultiplier() != null
						? measConf.getUnitMultiplier().toPlainString()
						: null);
				row[DatumControlCenterCsvColumn.DECIMAL_SCALE.getCode()] = (measConf.getDecimalScale() >= 0
						? String.valueOf(measConf.getDecimalScale())
						: null);
				writer.write(row);
				fill(row, null);
			}
		}
	}

}
