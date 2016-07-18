/* ==================================================================
 * SDMDatumDataSource.java - 26/01/2016 3:06:48 pm
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.deson.sdm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.context.MessageSource;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.domain.ACEnergyDatum;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.hw.deson.meter.SDMData;
import net.solarnetwork.node.hw.deson.meter.SDMSupport;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * {@link DatumDataSource} implementation for {@link GeneralNodeACEnergyDatum}
 * with the SDM series watt meter.
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
 * </dl>
 * 
 * @author matt
 * @version 1.1
 */
public class SDMDatumDataSource extends SDMSupport implements DatumDataSource<GeneralNodeACEnergyDatum>,
		MultiDatumDataSource<GeneralNodeACEnergyDatum>, SettingSpecifierProvider {

	private MessageSource messageSource;
	private long sampleCacheMs = 5000;

	private SDMData getCurrentSample() {
		SDMData currSample;
		if ( isCachedSampleExpired() ) {
			try {
				currSample = performAction(new ModbusConnectionAction<SDMData>() {

					@Override
					public SDMData doWithConnection(ModbusConnection conn) throws IOException {
						if ( sample.getControlDataTimestamp() < 0 ) {
							// we need to know what kind of meter we are dealing with
							sample.readControlData(conn);
						}
						sample.readMeterData(conn);
						return sample.getSnapshot();
					}

				});
				if ( log.isTraceEnabled() ) {
					log.trace(currSample.dataDebugString());
				}
				log.debug("Read SDM data: {}", currSample);
			} catch ( IOException e ) {
				throw new RuntimeException(
						"Communication problem reading from Modbus device " + modbusNetwork(), e);
			}
		} else {
			currSample = sample.getSnapshot();
		}
		return currSample;
	}

	private boolean isCachedSampleExpired() {
		final long lastReadDiff = System.currentTimeMillis() - sample.getMeterDataTimestamp();
		if ( lastReadDiff > sampleCacheMs ) {
			return true;
		}
		return false;
	}

	@Override
	public Class<? extends GeneralNodeACEnergyDatum> getDatumType() {
		return SDMDatum.class;
	}

	@Override
	public GeneralNodeACEnergyDatum readCurrentDatum() {
		final long start = System.currentTimeMillis();
		final SDMData currSample = getCurrentSample();
		SDMDatum d = new SDMDatum(currSample, ACPhase.Total);
		d.setSourceId(getSourceMapping().get(ACPhase.Total));
		if ( currSample.getMeterDataTimestamp() >= start ) {
			// we read from the meter
			postDatumCapturedEvent(d, ACEnergyDatum.class);
		}
		return d;
	}

	@Override
	public Class<? extends GeneralNodeACEnergyDatum> getMultiDatumType() {
		return SDMDatum.class;
	}

	@Override
	public Collection<GeneralNodeACEnergyDatum> readMultipleDatum() {
		final long start = System.currentTimeMillis();
		final SDMData currSample = getCurrentSample();
		final List<GeneralNodeACEnergyDatum> results = new ArrayList<GeneralNodeACEnergyDatum>(4);
		if ( currSample == null ) {
			return results;
		}
		final boolean postCapturedEvent = (currSample.getMeterDataTimestamp() >= start);
		if ( isCaptureTotal() || postCapturedEvent ) {
			SDMDatum d = new SDMDatum(currSample, ACPhase.Total);
			d.setSourceId(getSourceMapping().get(ACPhase.Total));
			if ( postCapturedEvent ) {
				// we read from the meter
				postDatumCapturedEvent(d, ACEnergyDatum.class);
			}
			if ( isCaptureTotal() ) {
				results.add(d);
			}
		}
		if ( currSample.supportsPhase(ACPhase.PhaseA) && (isCapturePhaseA() || postCapturedEvent) ) {
			SDMDatum d = new SDMDatum(currSample, ACPhase.PhaseA);
			d.setSourceId(getSourceMapping().get(ACPhase.PhaseA));
			if ( postCapturedEvent ) {
				// we read from the meter
				postDatumCapturedEvent(d, ACEnergyDatum.class);
			}
			if ( isCapturePhaseA() ) {
				results.add(d);
			}
		}
		if ( currSample.supportsPhase(ACPhase.PhaseB) && (isCapturePhaseB() || postCapturedEvent) ) {
			SDMDatum d = new SDMDatum(currSample, ACPhase.PhaseB);
			d.setSourceId(getSourceMapping().get(ACPhase.PhaseB));
			if ( postCapturedEvent ) {
				// we read from the meter
				postDatumCapturedEvent(d, ACEnergyDatum.class);
			}
			if ( isCapturePhaseB() ) {
				results.add(d);
			}
		}
		if ( currSample.supportsPhase(ACPhase.PhaseC) && (isCapturePhaseC() || postCapturedEvent) ) {
			SDMDatum d = new SDMDatum(currSample, ACPhase.PhaseC);
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
		return "net.solarnetwork.node.datum.deson.sdm";
	}

	@Override
	public String getDisplayName() {
		return "Deson SDM Series Meter";
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		SDMDatumDataSource defaults = new SDMDatumDataSource();
		List<SettingSpecifier> results = super.getSettingSpecifiers();
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(defaults.getSampleCacheMs())));
		return results;
	}

	/**
	 * Get the sample cache maximum age, in milliseconds.
	 * 
	 * @return the cache milliseconds
	 */
	public long getSampleCacheMs() {
		return sampleCacheMs;
	}

	/**
	 * Set the sample cache maximum age, in milliseconds.
	 * 
	 * @param sampleCacheSecondsMs
	 *        the cache milliseconds
	 */
	public void setSampleCacheMs(long sampleCacheMs) {
		this.sampleCacheMs = sampleCacheMs;
	}

}
