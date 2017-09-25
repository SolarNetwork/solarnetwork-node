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
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.GeneralDatumMetadata;
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
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;

/**
 * {@link DatumDataSource} implementation for {@link ACEnergyDatum} with the
 * EM5600 series watt meter.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>messageSource</dt>
 * <dd>The {@link MessageSource} to use with
 * {@link SettingSpecifierProvider}.</dd>
 * 
 * <dt>sampleCacheMs</dt>
 * <dd>The maximum number of milliseconds to cache data read from the meter,
 * until the data will be read from the meter again.</dd>
 * 
 * <dt>tagConsumption</dt>
 * <dd>If {@link #getDatumMetadataService()} is available, then tag the
 * configured source with
 * </dl>
 * 
 * @author matt
 * @version 1.2
 */
public class EM5600ConsumptionDatumDataSource extends EM5600Support implements
		DatumDataSource<ACEnergyDatum>, MultiDatumDataSource<ACEnergyDatum>, SettingSpecifierProvider {

	private static final long MIN_TIME_READ_ENERGY_RATIOS = 1000L * 60L * 60L; // 1 hour

	private long sampleCacheMs = 5000;
	private boolean tagConsumption = true;
	private long energyRatioReadTime = 0;

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
						final long lastReadDiff = System.currentTimeMillis() - energyRatioReadTime;
						if ( lastReadDiff > MIN_TIME_READ_ENERGY_RATIOS ) {
							sample.readEnergyRatios(conn);
							log.info("Refreshed energy ratios from meter: PT {} CT {}",
									sample.getPtRatio(), sample.getCtRatio());
							energyRatioReadTime = System.currentTimeMillis();
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
				throw new RuntimeException(
						"Communication problem reading from Modbus device " + modbusNetwork(), e);
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
			postDatumCapturedEvent(d);
			addEnergyDatumSourceMetadata(d);
		}
		return d;
	}

	@Override
	public Class<? extends GeneralNodeACEnergyDatum> getMultiDatumType() {
		return EM5600ConsumptionDatum.class;
	}

	@Override
	public Collection<ACEnergyDatum> readMultipleDatum() {
		final long start = System.currentTimeMillis();
		final EM5600Data currSample = getCurrentSample();
		final List<ACEnergyDatum> results = new ArrayList<ACEnergyDatum>(4);
		final List<EM5600ConsumptionDatum> capturedResults = new ArrayList<EM5600ConsumptionDatum>(4);
		if ( currSample == null ) {
			return results;
		}
		final boolean postCapturedEvent = (currSample.getDataTimestamp() >= start);
		if ( isCaptureTotal() || postCapturedEvent ) {
			EM5600ConsumptionDatum d = new EM5600ConsumptionDatum(currSample, ACPhase.Total);
			d.setSourceId(getSourceMapping().get(ACPhase.Total));
			if ( postCapturedEvent ) {
				// we read from the meter
				postDatumCapturedEvent(d);
			}
			if ( isCaptureTotal() ) {
				results.add(d);
				if ( postCapturedEvent ) {
					capturedResults.add(d);
				}
			}
		}
		if ( isCapturePhaseA() || postCapturedEvent ) {
			EM5600ConsumptionDatum d = new EM5600ConsumptionDatum(currSample, ACPhase.PhaseA);
			d.setSourceId(getSourceMapping().get(ACPhase.PhaseA));
			if ( postCapturedEvent ) {
				// we read from the meter
				postDatumCapturedEvent(d);
			}
			if ( isCapturePhaseA() ) {
				results.add(d);
				if ( postCapturedEvent ) {
					capturedResults.add(d);
				}
			}
		}
		if ( isCapturePhaseB() || postCapturedEvent ) {
			EM5600ConsumptionDatum d = new EM5600ConsumptionDatum(currSample, ACPhase.PhaseB);
			d.setSourceId(getSourceMapping().get(ACPhase.PhaseB));
			if ( postCapturedEvent ) {
				// we read from the meter
				postDatumCapturedEvent(d);
			}
			if ( isCapturePhaseB() ) {
				results.add(d);
				if ( postCapturedEvent ) {
					capturedResults.add(d);
				}
			}
		}
		if ( isCapturePhaseC() || postCapturedEvent ) {
			EM5600ConsumptionDatum d = new EM5600ConsumptionDatum(currSample, ACPhase.PhaseC);
			d.setSourceId(getSourceMapping().get(ACPhase.PhaseC));
			if ( postCapturedEvent ) {
				// we read from the meter
				postDatumCapturedEvent(d);
			}
			if ( isCapturePhaseC() ) {
				results.add(d);
				if ( postCapturedEvent ) {
					capturedResults.add(d);
				}
			}
		}

		for ( EM5600ConsumptionDatum d : capturedResults ) {
			addEnergyDatumSourceMetadata(d);
		}

		return results;
	}

	private void addEnergyDatumSourceMetadata(EM5600ConsumptionDatum d) {
		// associate consumption/generation tags with this source
		GeneralDatumMetadata sourceMeta = new GeneralDatumMetadata();
		if ( isTagConsumption() ) {
			sourceMeta.addTag(net.solarnetwork.node.domain.EnergyDatum.TAG_CONSUMPTION);
		} else {
			sourceMeta.addTag(net.solarnetwork.node.domain.EnergyDatum.TAG_GENERATION);
		}
		addSourceMetadata(d.getSourceId(), sourceMeta);
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
	public List<SettingSpecifier> getSettingSpecifiers() {
		EM5600ConsumptionDatumDataSource defaults = new EM5600ConsumptionDatumDataSource();
		List<SettingSpecifier> results = super.getSettingSpecifiers();
		SettingSpecifier energyTag = new BasicToggleSettingSpecifier("tagConsumption",
				Boolean.valueOf(defaults.isTagConsumption()));
		results.add(energyTag);
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(defaults.getSampleCacheMs())));
		return results;
	}

	public long getSampleCacheMs() {
		return sampleCacheMs;
	}

	public void setSampleCacheMs(long sampleCacheMs) {
		this.sampleCacheMs = sampleCacheMs;
	}

	public boolean isTagConsumption() {
		return tagConsumption;
	}

	public void setTagConsumption(boolean tagConsumption) {
		this.tagConsumption = tagConsumption;
	}

}
