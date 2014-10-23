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

package net.solarnetwork.node.datum.hc.em5600;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.domain.ACEnergyDatum;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.hw.hc.EM5600Data;
import net.solarnetwork.node.hw.hc.EM5600Support;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
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
 * 
 * <dt>sampleCacheMs</dt>
 * <dd>The maximum number of milliseconds to cache data read from the meter,
 * until the data will be read from the meter again.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.2
 */
public class EM5600ConsumptionDatumDataSource extends EM5600Support implements
		DatumDataSource<GeneralNodeACEnergyDatum>, MultiDatumDataSource<GeneralNodeACEnergyDatum>,
		SettingSpecifierProvider {

	private static final long MIN_TIME_READ_ENERGY_RATIOS = 1000L * 60L * 60L; // 1 hour

	private MessageSource messageSource;
	private long sampleCacheMs = 5000;

	private EM5600Data getCurrentSample() {
		EM5600Data currSample;
		if ( isCachedSampleExpired() ) {
			try {
				currSample = performAction(new ModbusConnectionAction<EM5600Data>() {

					@Override
					public EM5600Data doWithConnection(ModbusConnection conn) throws IOException {
						if ( getUnitFactor() == null ) {
							Integer model = getMeterModel(conn);
							log.debug("Found meter model {}", model);
						}
						final long lastReadDiff = System.currentTimeMillis() - sample.getDataTimestamp();
						if ( lastReadDiff > MIN_TIME_READ_ENERGY_RATIOS ) {
							sample.readEnergyRatios(conn);
						}
						sample.readMeterData(conn);
						return new EM5600Data(sample);
					}

				});
				if ( log.isTraceEnabled() ) {
					log.trace(currSample.dataDebugString());
				}
				log.debug("Read EM5600 data: {}", currSample);
			} catch ( IOException e ) {
				throw new RuntimeException("Communication problem reading from Modbus device "
						+ modbusNetwork(), e);
			}
		} else {
			currSample = new EM5600Data(sample);
		}
		return currSample;
	}

	private boolean isCachedSampleExpired() {
		final long lastReadDiff = System.currentTimeMillis() - sample.getDataTimestamp();
		if ( lastReadDiff > sampleCacheMs ) {
			return true;
		}
		return false;
	}

	@Override
	public Class<? extends GeneralNodeACEnergyDatum> getDatumType() {
		return EM5600ConsumptionDatum.class;
	}

	@Override
	public GeneralNodeACEnergyDatum readCurrentDatum() {
		final long start = System.currentTimeMillis();
		final EM5600Data currSample = getCurrentSample();
		EM5600ConsumptionDatum d = new EM5600ConsumptionDatum(currSample, ACPhase.Total);
		d.setSourceId(getSourceMapping().get(ACPhase.Total));
		if ( currSample.getDataTimestamp() >= start ) {
			// we read from the meter
			postDatumCapturedEvent(d, ACEnergyDatum.class);
		}
		return d;
	}

	@Override
	public Class<? extends GeneralNodeACEnergyDatum> getMultiDatumType() {
		return EM5600ConsumptionDatum.class;
	}

	@Override
	public Collection<GeneralNodeACEnergyDatum> readMultipleDatum() {
		final long start = System.currentTimeMillis();
		final EM5600Data currSample = getCurrentSample();
		final List<GeneralNodeACEnergyDatum> results = new ArrayList<GeneralNodeACEnergyDatum>(4);
		if ( currSample == null ) {
			return results;
		}
		final boolean postCapturedEvent = (currSample.getDataTimestamp() >= start);
		if ( isCaptureTotal() || postCapturedEvent ) {
			EM5600ConsumptionDatum d = new EM5600ConsumptionDatum(currSample, ACPhase.Total);
			d.setSourceId(getSourceMapping().get(ACPhase.Total));
			if ( postCapturedEvent ) {
				// we read from the meter
				postDatumCapturedEvent(d, ACEnergyDatum.class);
			}
			if ( isCaptureTotal() ) {
				results.add(d);
			}
		}
		if ( isCapturePhaseA() || postCapturedEvent ) {
			EM5600ConsumptionDatum d = new EM5600ConsumptionDatum(currSample, ACPhase.PhaseA);
			d.setSourceId(getSourceMapping().get(ACPhase.PhaseA));
			if ( postCapturedEvent ) {
				// we read from the meter
				postDatumCapturedEvent(d, ACEnergyDatum.class);
			}
			if ( isCapturePhaseA() ) {
				results.add(d);
			}
		}
		if ( isCapturePhaseB() || postCapturedEvent ) {
			EM5600ConsumptionDatum d = new EM5600ConsumptionDatum(currSample, ACPhase.PhaseB);
			d.setSourceId(getSourceMapping().get(ACPhase.PhaseB));
			if ( postCapturedEvent ) {
				// we read from the meter
				postDatumCapturedEvent(d, ACEnergyDatum.class);
			}
			if ( isCapturePhaseB() ) {
				results.add(d);
			}
		}
		if ( isCapturePhaseC() || postCapturedEvent ) {
			EM5600ConsumptionDatum d = new EM5600ConsumptionDatum(currSample, ACPhase.PhaseC);
			d.setSourceId(getSourceMapping().get(ACPhase.PhaseC));
			if ( postCapturedEvent ) {
				// we read from the meter
				postDatumCapturedEvent(d, ACEnergyDatum.class);
			}
			if ( isCapturePhaseC() ) {
				results.add(d);
			}
		}
		return results;
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.datum.hc.em5600";
	}

	@Override
	public String getDisplayName() {
		return "EM5600 Series Meter";
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		EM5600ConsumptionDatumDataSource defaults = new EM5600ConsumptionDatumDataSource();
		List<SettingSpecifier> results = super.getSettingSpecifiers();
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs", String.valueOf(defaults
				.getSampleCacheMs())));
		return results;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public long getSampleCacheMs() {
		return sampleCacheMs;
	}

	public void setSampleCacheMs(long sampleCacheMs) {
		this.sampleCacheMs = sampleCacheMs;
	}

}
