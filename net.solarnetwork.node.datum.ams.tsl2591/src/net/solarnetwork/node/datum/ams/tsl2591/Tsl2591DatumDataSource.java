/* ==================================================================
 * Tsl2591DatumDataSource.java - 1/09/2020 2:18:03 PM
 *
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.ams.tsl2591;

import static net.solarnetwork.util.DateUtils.formatForLocalDisplay;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.AtmosphericDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleAtmosphericDatum;
import net.solarnetwork.node.hw.ams.lux.tsl2591.Gain;
import net.solarnetwork.node.hw.ams.lux.tsl2591.IntegrationTime;
import net.solarnetwork.node.hw.ams.lux.tsl2591.Tsl2591Factory;
import net.solarnetwork.node.hw.ams.lux.tsl2591.Tsl2591Operations;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;

/**
 * Datum data source for the TSL2591 light sensor.
 *
 * @author matt
 * @version 2.2
 */
public class Tsl2591DatumDataSource extends DatumDataSourceSupport implements DatumDataSource,
		SettingSpecifierProvider, SettingsChangeObserver, ServiceLifecycleObserver {

	/** The {@code deviceName} property default value. */
	public static final String DEFAULT_DEVICE_NAME = "/dev/i2c-0";

	/** The {@code sampleCacheMs} property default value. */
	public static final long DEFAULT_SAMPLE_CACHE_MS = 5000L;

	/** The {@code sourceId} property default value. */
	public static final String DEFAULT_SOURCE_ID = "TSL2591";

	/** The {@code gain} property default value. */
	public static final Gain DEFAULT_GAIN = Gain.Medium;

	/** The {@code integrationTime} property default value. */
	public static final IntegrationTime DEFAULT_INT_TIME = IntegrationTime.Time200ms;

	/** The {@code configureDelay} property default value. */
	public static final long DEFAULT_CONFIGURE_DELAY = 15000L;

	private final AtomicReference<AtmosphericDatum> sample;

	private String deviceName = DEFAULT_DEVICE_NAME;
	private long sampleCacheMs = DEFAULT_SAMPLE_CACHE_MS;
	private String sourceId;
	private Gain gain = DEFAULT_GAIN;
	private IntegrationTime integrationTime = DEFAULT_INT_TIME;
	private final long configureDelay = DEFAULT_CONFIGURE_DELAY;

	private ScheduledFuture<?> configureFuture;

	/**
	 * Default constructor.
	 */
	public Tsl2591DatumDataSource() {
		this(new AtomicReference<>());
	}

	/**
	 * Construct with a specific sample data instance.
	 *
	 * @param sample
	 *        the sample data to use
	 */
	public Tsl2591DatumDataSource(AtomicReference<AtmosphericDatum> sample) {
		super();
		this.sample = sample;
		setDisplayName("TSL2591 Sensor");
	}

	@Override
	public synchronized void serviceDidStartup() {
		scheduleConfigurationTask();
	}

	@Override
	public synchronized void serviceDidShutdown() {
		if ( configureFuture != null && !configureFuture.isDone() ) {
			configureFuture.cancel(false);
			configureFuture = null;
		}
	}

	@Override
	public void configurationChanged(Map<String, Object> properties) {
		scheduleConfigurationTask();
	}

	@Override
	public Collection<String> publishedSourceIds() {
		final String sourceId = resolvePlaceholders(getSourceId());
		return (sourceId == null || sourceId.isEmpty() ? Collections.emptySet()
				: Collections.singleton(sourceId));
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.ams.tsl2591";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(12);
		results.add(new BasicTitleSettingSpecifier("sample", getSampleMessage(sample.get()), true));

		results.addAll(getIdentifiableSettingSpecifiers());

		results.add(new BasicTextFieldSettingSpecifier("deviceName", DEFAULT_DEVICE_NAME));
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(DEFAULT_SAMPLE_CACHE_MS)));
		results.add(new BasicTextFieldSettingSpecifier("sourceId", null));

		// drop-down menu for Gain
		BasicMultiValueSettingSpecifier gainSpec = new BasicMultiValueSettingSpecifier("gainCode",
				String.valueOf(DEFAULT_GAIN.getCode()));
		Map<String, String> gainTitles = new LinkedHashMap<String, String>(3);
		for ( Gain e : Gain.values() ) {
			gainTitles.put(String.valueOf(e.getCode()), e.toString());
		}
		gainSpec.setValueTitles(gainTitles);
		results.add(gainSpec);

		// drop-down menu for IntegrationTime
		BasicMultiValueSettingSpecifier intTimeSpec = new BasicMultiValueSettingSpecifier(
				"integrationTimeCode", String.valueOf(DEFAULT_GAIN.getCode()));
		Map<String, String> intTimeTitles = new LinkedHashMap<String, String>(3);
		for ( IntegrationTime e : IntegrationTime.values() ) {
			intTimeTitles.put(String.valueOf(e.getCode()), String.format("%dms", e.getDuration()));
		}
		intTimeSpec.setValueTitles(intTimeTitles);
		results.add(intTimeSpec);

		return results;
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return AtmosphericDatum.class;
	}

	@Override
	public AtmosphericDatum readCurrentDatum() {
		return getCurrentSample();
	}

	private AtmosphericDatum getCurrentSample() {
		AtmosphericDatum currSample = sample.get();
		if ( currSample == null
				|| currSample.getTimestamp().until(Instant.now(), ChronoUnit.MILLIS) > sampleCacheMs ) {
			try {
				currSample = readCurrentSample();
				if ( currSample != null ) {
					sample.set(currSample);
				}
				if ( log.isTraceEnabled() && currSample != null ) {
					log.trace("Sample: {}", currSample.asSimpleMap());
				}
				log.debug("Read TSL2591 data: {}", currSample);
			} catch ( IOException e ) {
				throw new RuntimeException(
						"Communication problem reading from TSL2591 device " + getDeviceName(), e);
			}
		}
		return currSample;
	}

	private synchronized void scheduleConfigurationTask() {
		if ( configureFuture != null && !configureFuture.isDone() ) {
			configureFuture.cancel(false);
			configureFuture = null;
		}
		Runnable r = new Runnable() {

			@Override
			public void run() {
				try {
					synchronized ( Tsl2591DatumDataSource.this ) {
						configureDevice();
						configureFuture = null;
					}
				} catch ( Exception e ) {
					log.warn("Error configuring TSL2591 device {}: {}", deviceName, e.toString());
					scheduleConfigurationTask();
				}
			}
		};
		log.info("Scheduling TSL2591 sensor {} configuration for {}ms from now.", deviceName,
				configureDelay);
		getTaskScheduler().schedule(r,
				Instant.ofEpochMilli(System.currentTimeMillis() + configureDelay));
	}

	private synchronized void configureDevice() throws IOException {
		final Gain g = getGain();
		final IntegrationTime t = getIntegrationTime();
		log.info("Configuring TSL2591 sensor {} with gain {}, integration time {}", deviceName, g, t);
		try (Tsl2591Operations ops = Tsl2591Factory.createOperations(getDeviceName())) {
			ops.setup(g, t);
			ops.enableAmbientLightSensor();
		}
	}

	private synchronized AtmosphericDatum readCurrentSample() throws IOException {
		try (Tsl2591Operations ops = Tsl2591Factory.createOperations(getDeviceName())) {
			BigDecimal lux = ops.getLux();
			if ( lux == null ) {
				return null;
			}

			// TODO: could make the desired output scale configurable
			if ( lux.scale() > 0 ) {
				lux = lux.setScale(0, RoundingMode.HALF_UP);
			}

			SimpleAtmosphericDatum d = new SimpleAtmosphericDatum(resolvePlaceholders(sourceId),
					Instant.now(), new DatumSamples());
			d.setLux(lux);
			return d;
		}
	}

	private String getSampleMessage(AtmosphericDatum data) {
		if ( data == null ) {
			return "N/A";
		}
		StringBuilder buf = new StringBuilder();
		buf.append("lux = ").append(data.getLux());
		buf.append("; sampled at ").append(formatForLocalDisplay(data.getTimestamp()));
		return buf.toString();
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
	 * @param sampleCacheMs
	 *        the cache milliseconds
	 */
	public void setSampleCacheMs(long sampleCacheMs) {
		this.sampleCacheMs = sampleCacheMs;
	}

	/**
	 * Get the source ID.
	 *
	 * @return the source ID; defaults to {@link #DEFAULT_SOURCE_ID}
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the source ID to use for returned datum.
	 *
	 * @param sourceId
	 *        the source ID to use
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Get the I2C device name.
	 *
	 * @return the device name; defaults to {@link #DEFAULT_DEVICE_NAME}
	 */
	public String getDeviceName() {
		return deviceName;
	}

	/**
	 * Set the I2C device name.
	 *
	 * @param deviceName
	 *        the device name to set
	 */
	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	/**
	 * Get the gain.
	 *
	 * @return the gain; defaults to {@link #DEFAULT_GAIN}
	 */
	public Gain getGain() {
		return gain;
	}

	/**
	 * Set the gain.
	 *
	 * @param gain
	 *        the gain to set
	 */
	public void setGain(Gain gain) {
		if ( gain == null ) {
			gain = DEFAULT_GAIN;
		}
		this.gain = gain;
	}

	/**
	 * Get the gain.
	 *
	 * @return the gain code value
	 */
	public int getGainCode() {
		return getGain().getCode();
	}

	/**
	 * Set the gain as a code value.
	 *
	 * @param code
	 *        the code to set
	 */
	public void setGainCode(int code) {
		Gain g;
		try {
			g = Gain.forCode(code);
		} catch ( IllegalArgumentException e ) {
			log.warn("Invalid Gain gain code value [{}]; setting to {}", code, DEFAULT_GAIN);
			g = DEFAULT_GAIN;
		}
		setGain(g);
	}

	/**
	 * Get the integration time.
	 *
	 * @return the integration time
	 */
	public IntegrationTime getIntegrationTime() {
		return integrationTime;
	}

	/**
	 * Set the integration time.
	 *
	 * @param integrationTime
	 *        the time to set
	 */
	public void setIntegrationTime(IntegrationTime integrationTime) {
		if ( integrationTime == null ) {
			integrationTime = DEFAULT_INT_TIME;
		}
		this.integrationTime = integrationTime;
	}

	/**
	 * Get the integration time as a code value.
	 *
	 * @return the integration time code
	 */
	public int getIntegrationTimeCode() {
		return getIntegrationTime().getCode();
	}

	/**
	 * Set the integration time as a code value.
	 *
	 * @param code
	 *        the code to set
	 */
	public void setIntegrationTimeCode(int code) {
		IntegrationTime t;
		try {
			t = IntegrationTime.forCode(code);
		} catch ( IllegalArgumentException e ) {
			log.warn("Invalid IntegrationTime code value [{}]; setting to {}", code, DEFAULT_INT_TIME);
			t = DEFAULT_INT_TIME;
		}
		setIntegrationTime(t);
	}

}
