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
import org.springframework.context.MessageSource;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.hw.schneider.meter.PM3200Data;
import net.solarnetwork.node.hw.schneider.meter.PM3200Support;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * DatumDataSource for GeneralNodeACEnergyDatum with the Schneider Electric
 * PM3200 series kWh meter.
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
 * @version 1.0
 */
public class PM3200ConsumptionDatumDataSource extends PM3200Support
		implements DatumDataSource<GeneralNodeACEnergyDatum>,
		MultiDatumDataSource<GeneralNodeACEnergyDatum>, SettingSpecifierProvider {

	private MessageSource messageSource;
	private long sampleCacheMs = 5000;

	private PM3200Data getCurrentSample() {
		PM3200Data currSample;
		if ( isCachedSampleExpired() ) {
			try {
				currSample = performAction(new ModbusConnectionAction<PM3200Data>() {

					@Override
					public PM3200Data doWithConnection(ModbusConnection conn) throws IOException {
						sample.readMeterData(conn);
						return new PM3200Data(sample);
					}

				});
				if ( log.isTraceEnabled() ) {
					log.trace(currSample.dataDebugString());
				}
				log.debug("Read PM3200 data: {}", currSample);
			} catch ( IOException e ) {
				throw new RuntimeException(
						"Communication problem reading from Modbus device " + modbusNetwork(), e);
			}
		} else {
			currSample = new PM3200Data(sample);
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
		return PM3200ConsumptionDatum.class;
	}

	@Override
	public GeneralNodeACEnergyDatum readCurrentDatum() {
		final long start = System.currentTimeMillis();
		final PM3200Data currSample = getCurrentSample();
		PM3200ConsumptionDatum d = new PM3200ConsumptionDatum(currSample, ACPhase.Total);
		d.setSourceId(getSourceMapping().get(ACPhase.Total));
		if ( currSample.getDataTimestamp() >= start ) {
			// we read from the meter
			postDatumCapturedEvent(d);
		}
		return d;
	}

	@Override
	public Class<? extends GeneralNodeACEnergyDatum> getMultiDatumType() {
		return PM3200ConsumptionDatum.class;
	}

	@Override
	public Collection<GeneralNodeACEnergyDatum> readMultipleDatum() {
		final long start = System.currentTimeMillis();
		final PM3200Data currSample = getCurrentSample();
		final List<GeneralNodeACEnergyDatum> results = new ArrayList<GeneralNodeACEnergyDatum>(4);
		if ( currSample == null ) {
			return results;
		}
		final boolean postCapturedEvent = (currSample.getDataTimestamp() >= start);
		if ( isCaptureTotal() || postCapturedEvent ) {
			PM3200ConsumptionDatum d = new PM3200ConsumptionDatum(currSample, ACPhase.Total);
			d.setSourceId(getSourceMapping().get(ACPhase.Total));
			if ( postCapturedEvent ) {
				// we read from the meter
				postDatumCapturedEvent(d);
			}
			if ( isCaptureTotal() ) {
				results.add(d);
			}
		}
		if ( isCapturePhaseA() || postCapturedEvent ) {
			PM3200ConsumptionDatum d = new PM3200ConsumptionDatum(currSample, ACPhase.PhaseA);
			d.setSourceId(getSourceMapping().get(ACPhase.PhaseA));
			if ( postCapturedEvent ) {
				// we read from the meter
				postDatumCapturedEvent(d);
			}
			if ( isCapturePhaseA() ) {
				results.add(d);
			}
		}
		if ( isCapturePhaseB() || postCapturedEvent ) {
			PM3200ConsumptionDatum d = new PM3200ConsumptionDatum(currSample, ACPhase.PhaseB);
			d.setSourceId(getSourceMapping().get(ACPhase.PhaseB));
			if ( postCapturedEvent ) {
				// we read from the meter
				postDatumCapturedEvent(d);
			}
			if ( isCapturePhaseB() ) {
				results.add(d);
			}
		}
		if ( isCapturePhaseC() || postCapturedEvent ) {
			PM3200ConsumptionDatum d = new PM3200ConsumptionDatum(currSample, ACPhase.PhaseC);
			d.setSourceId(getSourceMapping().get(ACPhase.PhaseC));
			if ( postCapturedEvent ) {
				// we read from the meter
				postDatumCapturedEvent(d);
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
		return "net.solarnetwork.node.consumption.schneider.pm3200";
	}

	@Override
	public String getDisplayName() {
		return "PM3200 Series Meter";
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		PM3200ConsumptionDatumDataSource defaults = new PM3200ConsumptionDatumDataSource();
		List<SettingSpecifier> results = super.getSettingSpecifiers();
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(defaults.getSampleCacheMs())));
		return results;
	}

	/**
	 * Get the sample cache maximum age, in seconds.
	 * 
	 * @return the cache seconds
	 * @deprecated use {@link #getSampleCacheMs()}
	 */
	@Deprecated
	public int getSampleCacheSeconds() {
		return (int) (getSampleCacheMs() / 1000);
	}

	/**
	 * Set the sample cache maximum age, in seconds.
	 * 
	 * @param sampleCacheSeconds
	 *        the cache seconds
	 * @deprecated use {@link #setSampleCacheMs(long)}
	 */
	@Deprecated
	public void setSampleCacheSeconds(int sampleCacheSeconds) {
		setSampleCacheMs(sampleCacheSeconds * 1000L);
	}

	public long getSampleCacheMs() {
		return sampleCacheMs;
	}

	public void setSampleCacheMs(long sampleCacheMs) {
		this.sampleCacheMs = sampleCacheMs;
	}

}
