/* ==================================================================
 * EM5600ConsumptionDatumDataSource.java - Mar 26, 2014 10:13:12 AM
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

package net.solarnetwork.node.consumption.hc.em5600;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.consumption.ConsumptionDatum;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.hw.hc.EM5600Data;
import net.solarnetwork.node.hw.hc.EM5600Support;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import org.springframework.context.MessageSource;

/**
 * {@link DatumDataSource} implementation for {@link ConsumptionDatum} with the
 * EM5600 series watt meter.
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
 * @version 1.1
 */
public class EM5600ConsumptionDatumDataSource extends EM5600Support implements
		DatumDataSource<ConsumptionDatum>, MultiDatumDataSource<ConsumptionDatum>,
		SettingSpecifierProvider {

	private static final long MIN_TIME_READ_ENERGY_RATIOS = 1000L * 60L * 60L; // 1 hour
	private static final long MIN_TIME_READ_DATA = 1000L * 5L; // 5 seconds

	private MessageSource messageSource;

	@Override
	public Class<? extends ConsumptionDatum> getDatumType() {
		return EM5600ConsumptionDatum.class;
	}

	@Override
	public ConsumptionDatum readCurrentDatum() {
		try {
			return performAction(new ModbusConnectionAction<ConsumptionDatum>() {

				@Override
				public ConsumptionDatum doWithConnection(ModbusConnection conn) throws IOException {
					final EM5600Data currSample = getCurrentSample(conn);
					EM5600ConsumptionDatum d = new EM5600ConsumptionDatum(currSample, ACPhase.Total);
					d.setSourceId(getSourceMapping().get(ACPhase.Total));
					return d;
				}
			});
		} catch ( IOException e ) {
			log.error("Error communicating with meter: {}", e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public Class<? extends ConsumptionDatum> getMultiDatumType() {
		return EM5600ConsumptionDatum.class;
	}

	@Override
	public Collection<ConsumptionDatum> readMultipleDatum() {
		try {
			return performAction(new ModbusConnectionAction<List<ConsumptionDatum>>() {

				@Override
				public List<ConsumptionDatum> doWithConnection(ModbusConnection conn) throws IOException {
					final List<ConsumptionDatum> results = new ArrayList<ConsumptionDatum>(4);
					final EM5600Data currSample = getCurrentSample(conn);
					if ( isCaptureTotal() ) {
						EM5600ConsumptionDatum d = new EM5600ConsumptionDatum(currSample, ACPhase.Total);
						d.setSourceId(getSourceMapping().get(ACPhase.Total));
						results.add(d);
					}
					if ( isCapturePhaseA() ) {
						EM5600ConsumptionDatum d = new EM5600ConsumptionDatum(currSample, ACPhase.PhaseA);
						d.setSourceId(getSourceMapping().get(ACPhase.PhaseA));
						results.add(d);
					}
					if ( isCapturePhaseB() ) {
						EM5600ConsumptionDatum d = new EM5600ConsumptionDatum(currSample, ACPhase.PhaseB);
						d.setSourceId(getSourceMapping().get(ACPhase.PhaseB));
						results.add(d);
					}
					if ( isCapturePhaseC() ) {
						EM5600ConsumptionDatum d = new EM5600ConsumptionDatum(currSample, ACPhase.PhaseC);
						d.setSourceId(getSourceMapping().get(ACPhase.PhaseC));
						results.add(d);
					}
					return results;
				}
			});
		} catch ( IOException e ) {
			log.error("Error communicating with meter: {}", e);
			throw new RuntimeException(e);
		}
	}

	private EM5600Data getCurrentSample(final ModbusConnection conn) {
		if ( getUnitFactor() == null ) {
			Integer model = getMeterModel(conn);
			log.debug("Found meter model {}", model);
		}
		final long lastReadDiff = System.currentTimeMillis() - sample.getDataTimestamp();
		if ( lastReadDiff > MIN_TIME_READ_ENERGY_RATIOS ) {
			sample.readEnergyRatios(conn);
		}
		if ( lastReadDiff > MIN_TIME_READ_DATA ) {
			sample.readMeterData(conn);
			log.debug("Read EM5600 data: {}", sample);
		}
		return new EM5600Data(sample);
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.consumption.hc.em5600";
	}

	@Override
	public String getDisplayName() {
		return "EM5600 Series Meter";
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

}
