/* ==================================================================
 * PM3200ConsumptionDatumDataSource.java - 1/03/2014 8:42:02 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.consumption.schneider.pm3200;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.consumption.ConsumptionDatum;
import net.solarnetwork.node.hw.schneider.meter.PM3200Support;
import net.solarnetwork.node.io.modbus.ModbusConnectionCallback;
import net.solarnetwork.node.io.modbus.ModbusHelper;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.wimpi.modbus.net.SerialConnection;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * DatumDataSource for ConsumptionDatum with the Schneider Electric PM3200
 * series kWh meter.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt></dt>
 * <dd></dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class PM3200ConsumptionDatumDataSource extends PM3200Support implements
		DatumDataSource<ConsumptionDatum>, MultiDatumDataSource<ConsumptionDatum>,
		SettingSpecifierProvider {

	/** The default source ID applied for the total reading values. */
	public static final String MAIN_SOURCE_ID = "Main";

	private static MessageSource MESSAGE_SOURCE;

	private Map<Integer, String> sourceMapping = getDefaulSourceMapping();

	/**
	 * Get a default {@code sourceMapping} value. This maps only the {@code 0}
	 * source to the value {@code Main}.
	 * 
	 * @return mapping
	 */
	public static Map<Integer, String> getDefaulSourceMapping() {
		Map<Integer, String> result = new LinkedHashMap<Integer, String>(1);
		result.put(0, MAIN_SOURCE_ID);
		return result;
	}

	@Override
	public Class<? extends ConsumptionDatum> getDatumType() {
		return PM3200ConsumptionDatum.class;
	}

	@Override
	public ConsumptionDatum readCurrentDatum() {
		if ( !shouldCollectSource(0) ) {
			return null;
		}
		return ModbusHelper.execute(getConnectionFactory(),
				new ModbusConnectionCallback<ConsumptionDatum>() {

					@Override
					public ConsumptionDatum doInConnection(SerialConnection conn) throws IOException {
						// current, voltage
						final Integer[][] data = readMeterValues(conn);
						PM3200ConsumptionDatum d = new PM3200ConsumptionDatum();
						fillDatumValues(d, data, 0);
						if ( d.isValid() ) {
							d.setSourceId(getSourceId(0));
						} else {
							d = null;
						}
						return d;
					}
				});
	}

	@Override
	public Class<? extends ConsumptionDatum> getMultiDatumType() {
		return PM3200ConsumptionDatum.class;
	}

	private static final int METER_VALUES_CURRENT_VOLTAGE = 0;
	private static final int METER_VALUES_CURRENT_VOLTAGE_START = ADDR_DATA_I1;
	private static final int METER_VALUES_TOTAL_ENERGY = 1;
	private static final int METER_VALUES_TOTAL_ENERGY_START = ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT;

	private Integer[][] readMeterValues(SerialConnection conn) {
		Integer[] ivData = ModbusHelper.readValues(conn, ADDR_DATA_I1, 38, getUnitId());
		Integer[] eData = ModbusHelper.readValues(conn, ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT, 48,
				getUnitId());
		return new Integer[][] { ivData, eData };
	}

	private void fillDatumValues(PM3200ConsumptionDatum d, Integer[][] data, int n) {
		fillCurrentVoltageValues(d, data[METER_VALUES_CURRENT_VOLTAGE],
				METER_VALUES_CURRENT_VOLTAGE_START, n);
		fillEnergyValues(d, data[METER_VALUES_TOTAL_ENERGY], METER_VALUES_TOTAL_ENERGY_START, n);
	}

	@Override
	public Collection<ConsumptionDatum> readMultipleDatum() {
		final List<ConsumptionDatum> results = new ArrayList<ConsumptionDatum>(3);

		ModbusHelper.execute(getConnectionFactory(), new ModbusConnectionCallback<Object>() {

			@Override
			public Object doInConnection(SerialConnection conn) throws IOException {
				final Integer[][] data = readMeterValues(conn);
				PM3200ConsumptionDatum d = new PM3200ConsumptionDatum();
				for ( int n = 0; n < 4; n++ ) {
					if ( shouldCollectSource(n) ) {
						fillDatumValues(d, data, n);
						if ( d.isValid() ) {
							d.setSourceId(getSourceId(n));
							results.add(d);
						}
					}
				}

				return null;
			}
		});

		return results;
	}

	private boolean shouldCollectSource(int num) {
		return (sourceMapping == null || sourceMapping.containsKey(num));
	}

	private String getSourceId(int num) {
		String result = null;
		if ( sourceMapping != null ) {
			result = sourceMapping.get(num);
		}
		if ( result == null ) {
			if ( num == 0 ) {
				result = MAIN_SOURCE_ID;
			} else {
				result = String.valueOf(num);
			}
		}
		return result;
	}

	private void fillCurrentVoltageValues(final PM3200ConsumptionDatum d, final Integer[] data,
			final int startingAddress, final int num) {
		assert (num >= 0 && num <= 3);
		if ( num == 0 ) {
			d.setAmps(parseFloat32(data, ADDR_DATA_I_AVERAGE - startingAddress));
			d.setVolts(parseFloat32(data, ADDR_DATA_V_NEUTRAL_AVERAGE - startingAddress));
			//d.setWattHourReading(parseInt64(data, ADDR_DATA_START - ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT));
		} else {
			d.setAmps(parseFloat32(data, ADDR_DATA_I1 + ((num - 1) * 2)) - startingAddress);
			d.setVolts(parseFloat32(data, ADDR_DATA_V_L1_NEUTRAL + ((num - 1) * 2)) - startingAddress);
		}
	}

	private void fillEnergyValues(final PM3200ConsumptionDatum d, final Integer[] data,
			final int startingAddress, final int num) {
		assert (num >= 0 && num <= 3);
		if ( num == 0 ) {
			d.setWattHourReading(parseInt64(data, ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT - startingAddress));
		}
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.consumption.schneider.pm3200";
	}

	@Override
	public String getDisplayName() {
		return "PM3200 Series Meter";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = super.getSettingSpecifiers();

		// TODO: other settings

		return results;
	}

	@Override
	public MessageSource getMessageSource() {
		if ( MESSAGE_SOURCE == null ) {
			ResourceBundleMessageSource source = new ResourceBundleMessageSource();
			source.setBundleClassLoader(getClass().getClassLoader());
			source.setBasename(getClass().getName());

			ResourceBundleMessageSource parent = new ResourceBundleMessageSource();
			parent.setBundleClassLoader(PM3200Support.class.getClassLoader());
			parent.setBasename(PM3200Support.class.getName());
			source.setParentMessageSource(parent);

			MESSAGE_SOURCE = source;
		}
		return MESSAGE_SOURCE;
	}

	public Map<Integer, String> getSourceMapping() {
		return sourceMapping;
	}

	public void setSourceMapping(Map<Integer, String> sourceMapping) {
		this.sourceMapping = sourceMapping;
	}

}
