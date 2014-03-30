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
import java.util.List;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.consumption.ConsumptionDatum;
import net.solarnetwork.node.hw.schneider.meter.MeasurementKind;
import net.solarnetwork.node.hw.schneider.meter.PM3200Data;
import net.solarnetwork.node.hw.schneider.meter.PM3200Support;
import net.solarnetwork.node.io.modbus.ModbusConnectionCallback;
import net.solarnetwork.node.io.modbus.ModbusHelper;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.wimpi.modbus.net.SerialConnection;
import org.springframework.context.MessageSource;

/**
 * DatumDataSource for ConsumptionDatum with the Schneider Electric PM3200
 * series kWh meter.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>messageSource</dt>
 * <dd>The {@link MessageSource} to use with {@link SettingSpecifierProvider}.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class PM3200ConsumptionDatumDataSource extends PM3200Support implements
		DatumDataSource<ConsumptionDatum>, MultiDatumDataSource<ConsumptionDatum>,
		SettingSpecifierProvider {

	private static final long MIN_TIME_READ_DATA = 1000L * 5L; // 5 seconds

	private MessageSource messageSource;

	private PM3200Data getCurrentSample(final SerialConnection conn) {
		final long lastReadDiff = System.currentTimeMillis() - sample.getDataTimestamp();
		if ( lastReadDiff > MIN_TIME_READ_DATA ) {
			sample.readMeterData(conn, getUnitId());
			if ( log.isTraceEnabled() ) {
				log.trace(sample.dataDebugString());
			}
			log.debug("Read PM3200 data: {}", sample);
		}
		return new PM3200Data(sample);
	}

	@Override
	public Class<? extends ConsumptionDatum> getDatumType() {
		return PM3200ConsumptionDatum.class;
	}

	@Override
	public ConsumptionDatum readCurrentDatum() {
		return ModbusHelper.execute(getConnectionFactory(),
				new ModbusConnectionCallback<ConsumptionDatum>() {

					@Override
					public ConsumptionDatum doInConnection(SerialConnection conn) throws IOException {
						final PM3200Data currSample = getCurrentSample(conn);
						PM3200ConsumptionDatum d = new PM3200ConsumptionDatum(currSample,
								MeasurementKind.Total);
						d.setSourceId(getSourceMapping().get(MeasurementKind.Total));
						return d;
					}
				});
	}

	@Override
	public Class<? extends ConsumptionDatum> getMultiDatumType() {
		return PM3200ConsumptionDatum.class;
	}

	@Override
	public Collection<ConsumptionDatum> readMultipleDatum() {
		return ModbusHelper.execute(getConnectionFactory(),
				new ModbusConnectionCallback<List<ConsumptionDatum>>() {

					@Override
					public List<ConsumptionDatum> doInConnection(SerialConnection conn)
							throws IOException {
						final List<ConsumptionDatum> results = new ArrayList<ConsumptionDatum>(4);
						final PM3200Data currSample = getCurrentSample(conn);
						if ( isCaptureTotal() ) {
							PM3200ConsumptionDatum d = new PM3200ConsumptionDatum(currSample,
									MeasurementKind.Total);
							d.setSourceId(getSourceMapping().get(MeasurementKind.Total));
							results.add(d);
						}
						if ( isCapturePhaseA() ) {
							PM3200ConsumptionDatum d = new PM3200ConsumptionDatum(currSample,
									MeasurementKind.PhaseA);
							d.setSourceId(getSourceMapping().get(MeasurementKind.PhaseA));
							results.add(d);
						}
						if ( isCapturePhaseB() ) {
							PM3200ConsumptionDatum d = new PM3200ConsumptionDatum(currSample,
									MeasurementKind.PhaseB);
							d.setSourceId(getSourceMapping().get(MeasurementKind.PhaseB));
							results.add(d);
						}
						if ( isCapturePhaseC() ) {
							PM3200ConsumptionDatum d = new PM3200ConsumptionDatum(currSample,
									MeasurementKind.PhaseC);
							d.setSourceId(getSourceMapping().get(MeasurementKind.PhaseC));
							results.add(d);
						}
						return results;
					}
				});
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

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

}
