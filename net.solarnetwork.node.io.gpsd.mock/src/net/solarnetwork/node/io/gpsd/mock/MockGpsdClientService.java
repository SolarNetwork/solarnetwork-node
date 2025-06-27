/* ==================================================================
 * MockGpsdClientService.java - 30/08/2021 10:27:33 AM
 *
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.gpsd.mock;

import static java.lang.String.valueOf;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.node.io.gpsd.domain.GpsdMessage;
import net.solarnetwork.node.io.gpsd.domain.GpsdReportMessage;
import net.solarnetwork.node.io.gpsd.domain.NmeaMode;
import net.solarnetwork.node.io.gpsd.domain.TpvReportMessage;
import net.solarnetwork.node.io.gpsd.domain.VersionMessage;
import net.solarnetwork.node.io.gpsd.domain.WatchMessage;
import net.solarnetwork.node.io.gpsd.service.GpsdClientConnection;
import net.solarnetwork.node.io.gpsd.service.GpsdClientStatus;
import net.solarnetwork.node.io.gpsd.service.GpsdMessageListener;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.support.BasicIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Mock implementation of {@link GpsdClientConnection} for testing purposes.
 *
 * @author matt
 * @version 2.1
 */
public class MockGpsdClientService extends BasicIdentifiable
		implements GpsdClientConnection, SettingSpecifierProvider, SettingsChangeObserver, Runnable {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final ConcurrentMap<Class<? extends GpsdMessage>, Set<GpsdMessageListener<GpsdMessage>>> messageListeners = new ConcurrentHashMap<>(
			8, 0.9f, 1);

	private final SecureRandom random = new SecureRandom();

	/** The default {@code uid} property value. */
	public static final String DEFAULT_UID = "GPSD";

	/** The default {@code updatePeriodMs} property value. */
	public static final long DEFAULT_UPDATE_PERIOD = 1_000L;

	public static final long DEFAULT_FIX_DELAY = 20_000L;

	public static final long DEFAULT_FIX_LOCK = 40_000L;

	public static final double DEFAULT_FIX_UNLOCKED_VARIATION_MULTIPLIER = 10.0;

	public static final double DEFAULT_ELEVATION = 100.0;

	public static final double DEFAULT_LATITUDE = 51.178882;

	public static final double DEFAULT_LONGITUDE = -1.828409;

	public static final double DEFAULT_ELEVATION_VARIATION = 10.0;

	public static final double DEFAULT_LATITUDE_VARIATION = 10.0;

	public static final double DEFAULT_LONGITUDE_VARIATION = 10.0;

	private final TaskScheduler taskScheduler;
	private long updatePeriodMs = DEFAULT_UPDATE_PERIOD;
	private long fixDelayMs = DEFAULT_FIX_DELAY;
	private long fixLockMs = DEFAULT_FIX_LOCK;
	private double fixUnlockedVariationMultiplier = DEFAULT_FIX_UNLOCKED_VARIATION_MULTIPLIER;
	private double elevation = DEFAULT_ELEVATION;
	private double latitude = DEFAULT_LATITUDE;
	private double longitude = DEFAULT_LONGITUDE;
	private double elevationVariation = DEFAULT_ELEVATION_VARIATION;
	private double latitudeVariation = DEFAULT_LATITUDE_VARIATION;
	private double longitudeVariation = DEFAULT_LONGITUDE_VARIATION;
	private OptionalService<EventAdmin> eventAdmin;

	private long taskStart;
	private ScheduledFuture<?> taskFuture;

	/**
	 * Constructor.
	 *
	 * @param taskScheduler
	 *        the task scheduler to use
	 * @throws IllegalArgumentException
	 *         if {@code taskScheduler} is {@literal null}
	 */
	public MockGpsdClientService(TaskScheduler taskScheduler) {
		super();
		if ( taskScheduler == null ) {
			throw new IllegalArgumentException("The taskScheduler argument must not be null.");
		}
		this.taskScheduler = taskScheduler;
		setUid(DEFAULT_UID);
	}

	/**
	 * Call after properties configured to startup the service.
	 */
	public synchronized void startup() {
		if ( taskFuture != null ) {
			shutdown();
		}
		if ( updatePeriodMs < 1 ) {
			return;
		}
		Instant start = Instant.now().truncatedTo(ChronoUnit.MINUTES).plusMillis(fixDelayMs);
		taskStart = start.toEpochMilli();
		taskFuture = taskScheduler.scheduleAtFixedRate(this, start, Duration.ofMillis(updatePeriodMs));
		postClientStatusChangeEvent(GpsdClientStatus.Connected);
	}

	/**
	 * Call to shut down the service.
	 */
	public synchronized void shutdown() {
		if ( taskFuture != null && !taskFuture.isDone() ) {
			taskFuture.cancel(true);
			taskFuture = null;
			postClientStatusChangeEvent(GpsdClientStatus.Closed);
		}
	}

	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		shutdown();
		startup();
	}

	/**
	 * Return a base number randomly varied +- by a given variation.
	 *
	 * @param age
	 *        the age since startup
	 * @param base
	 *        the base number
	 * @param variation
	 *        the maximum amount of +- variation
	 * @param projection
	 *        for earth distance, the projection to use; will be passed
	 *        {@code latitude} and the variable amount
	 * @param consumer
	 *        the result consumer
	 * @param errorConsumer
	 *        the error result consumer
	 * @return the varied result
	 */
	private void variableValue(final long age, final double base, final double variation,
			DoubleBinaryOperator projection, Consumer<Number> consumer, Consumer<Number> errorConsumer) {
		double val = base;
		if ( variation > 0.0 ) {
			double var = variation;
			if ( age < (fixDelayMs + fixLockMs) ) {
				double lockVar = (age < fixDelayMs ? fixLockMs : age - fixDelayMs - fixLockMs);
				var += (1.0 - ((random.nextDouble() * lockVar) / lockVar)) * var
						* fixUnlockedVariationMultiplier;
			}
			var = (random.nextDouble() * var) * (random.nextBoolean() ? -1.0 : 1.0);
			errorConsumer.accept(Math.abs(var));
			if ( projection != null ) {
				var = projection.applyAsDouble(latitude, var);
			}
			val += var;
		}
		consumer.accept(val);
	}

	private static final double EARTH_RADIUS = 6_378_137.0;

	private static final DoubleBinaryOperator LAT_PROJECTION = (lat, len) -> {
		return (len / EARTH_RADIUS) * 180.0 / Math.PI;
	};

	private static final DoubleBinaryOperator LON_PROJECTION = (lat, len) -> {
		return (len / (EARTH_RADIUS * Math.cos(Math.PI * lat / 180.0)));
	};

	@Override
	public void run() {
		final long now = System.currentTimeMillis();
		final long age = now - taskStart;
		TpvReportMessage.Builder builder = TpvReportMessage.builder().withDevice(getUid());

		if ( age < fixDelayMs ) {
			builder.withMode(NmeaMode.NoFix);
		} else if ( age < (fixDelayMs + fixLockMs) ) {
			builder.withMode(NmeaMode.TwoDimensional);
		} else {
			builder.withMode(NmeaMode.ThreeDimensional);
		}

		variableValue(age, elevation, elevationVariation, null, builder::withAltitude,
				builder::withAltitudeError);
		variableValue(age, latitude, latitudeVariation, LAT_PROJECTION, builder::withLatitude,
				builder::withLatitudeError);
		variableValue(age, longitude, longitudeVariation, LON_PROJECTION, builder::withLongitude,
				builder::withLongitudeError);

		builder.withTimestamp(Instant.ofEpochMilli(now));

		TpvReportMessage message = builder.build();
		handleGpsdMessage(message);
	}

	public final void handleGpsdMessage(GpsdMessage message) {
		if ( message instanceof GpsdReportMessage ) {
			postReportMessageCapturedEvent((GpsdReportMessage) message);
		}
		Class<? extends GpsdMessage> messageType = message.getClass();
		for ( Entry<Class<? extends GpsdMessage>, Set<GpsdMessageListener<GpsdMessage>>> me : messageListeners
				.entrySet() ) {
			if ( me.getKey().isAssignableFrom(messageType) ) {
				Set<GpsdMessageListener<GpsdMessage>> listeners = me.getValue();
				if ( listeners != null ) {
					for ( GpsdMessageListener<GpsdMessage> listener : listeners ) {
						try {
							listener.onGpsdMessage(message);
						} catch ( Exception e ) {
							log.error("GPS listener error handling message {}: {}", message,
									e.toString(), e);
						}
					}
				}
			}
		}
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public <M extends GpsdMessage> void addMessageListener(Class<? extends M> messageType,
			GpsdMessageListener<M> listener) {
		messageListeners.computeIfAbsent(messageType, k -> new CopyOnWriteArraySet<>())
				.add((GpsdMessageListener<GpsdMessage>) listener);
	}

	@Override
	public <M extends GpsdMessage> void removeMessageListener(Class<? extends M> messageType,
			GpsdMessageListener<M> listener) {
		messageListeners.compute(messageType, (k, v) -> {
			if ( v != null && v.remove(listener) ) {
				if ( v.isEmpty() ) {
					return null;
				}
			}
			return v;
		});
	}

	private void postClientStatusChangeEvent(GpsdClientStatus status) {
		String uid = getUid();
		if ( uid == null ) {
			return;
		}
		Map<String, Object> props = new HashMap<String, Object>();
		props.put(UID_PROPERTY, uid);
		if ( getGroupUid() != null ) {
			props.put(GROUP_UID_PROPERTY, getGroupUid());
		}
		props.put(STATUS_PROPERTY, status);
		postEvent(new Event(EVENT_TOPIC_CLIENT_STATUS_CHANGE, props));
	}

	private void postReportMessageCapturedEvent(GpsdReportMessage message) {
		String uid = getUid();
		if ( uid == null ) {
			return;
		}
		Map<String, Object> props = new HashMap<String, Object>();
		props.put(UID_PROPERTY, uid);
		if ( getGroupUid() != null ) {
			props.put(GROUP_UID_PROPERTY, getGroupUid());
		}
		props.put(MESSAGE_PROPERTY, message);
		postEvent(new Event(EVENT_TOPIC_REPORT_MESSAGE_CAPTURED, props));
	}

	private void postEvent(Event event) {
		EventAdmin ea = (eventAdmin != null ? eventAdmin.service() : null);
		if ( ea != null ) {
			ea.postEvent(event);
		}
	}

	@Override
	public GpsdClientStatus getClientStatus() {
		return GpsdClientStatus.Connected;
	}

	@Override
	public Future<VersionMessage> requestGpsdVersion() {
		VersionMessage vers = VersionMessage.builder().withRelease("1.0").withRemoteUrl("localhost")
				.withProtocolMajor(1).withProtocolMinor(0).build();
		return CompletableFuture.completedFuture(vers);
	}

	@Override
	public Future<WatchMessage> configureWatchMode(WatchMessage config) {
		return CompletableFuture.completedFuture(config);
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.io.gpsd.mock";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(16);
		results.addAll(basicIdentifiableSettings("", DEFAULT_UID, ""));
		results.add(
				new BasicTextFieldSettingSpecifier("updatePeriodMs", valueOf(DEFAULT_UPDATE_PERIOD)));
		results.add(new BasicTextFieldSettingSpecifier("fixDelayMs", valueOf(DEFAULT_FIX_DELAY)));
		results.add(new BasicTextFieldSettingSpecifier("fixLockMs", valueOf(DEFAULT_FIX_LOCK)));
		results.add(new BasicTextFieldSettingSpecifier("fixUnlockedVariationMultiplier",
				valueOf(DEFAULT_FIX_UNLOCKED_VARIATION_MULTIPLIER)));
		results.add(new BasicTextFieldSettingSpecifier("latitude", valueOf(DEFAULT_LATITUDE)));
		results.add(new BasicTextFieldSettingSpecifier("longitude", valueOf(DEFAULT_LONGITUDE)));
		results.add(new BasicTextFieldSettingSpecifier("elevation", valueOf(DEFAULT_ELEVATION)));
		results.add(new BasicTextFieldSettingSpecifier("latitudeVariation",
				valueOf(DEFAULT_LATITUDE_VARIATION)));
		results.add(new BasicTextFieldSettingSpecifier("longitudeVariation",
				valueOf(DEFAULT_LONGITUDE_VARIATION)));
		results.add(new BasicTextFieldSettingSpecifier("elevationVariation",
				valueOf(DEFAULT_ELEVATION_VARIATION)));
		return results;
	}

	/**
	 * Get the GPS update period.
	 *
	 * @return the update period, in milliseconds
	 */
	public long getUpdatePeriodMs() {
		return updatePeriodMs;
	}

	/**
	 * Set the GPS update period, in milliseconds.
	 *
	 * @param updatePeriodMs
	 *        the period to set; setting to {@literal 0} will disable updates
	 */
	public void setUpdatePeriodMs(long updatePeriodMs) {
		this.updatePeriodMs = updatePeriodMs;
	}

	/**
	 * Get the {@link EventAdmin} service.
	 *
	 * @return the EventAdmin service
	 */
	public OptionalService<EventAdmin> getEventAdmin() {
		return eventAdmin;
	}

	/**
	 * Set an {@link EventAdmin} service to use.
	 *
	 * @param eventAdmin
	 *        the EventAdmin to use
	 */
	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	/**
	 * Get the base elevation.
	 *
	 * @return the elevation
	 */
	public double getElevation() {
		return elevation;
	}

	/**
	 * Set the base elevation.
	 *
	 * @param elevation
	 *        the elevation to set
	 */
	public void setElevation(double elevation) {
		this.elevation = elevation;
	}

	/**
	 * Get the base latitude.
	 *
	 * @return the latitude
	 */
	public double getLatitude() {
		return latitude;
	}

	/**
	 * Set the base latitude.
	 *
	 * @param latitude
	 *        the latitude to set
	 */
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	/**
	 * Get the base longitude.
	 *
	 * @return the longitude
	 */
	public double getLongitude() {
		return longitude;
	}

	/**
	 * Set the base longitude.
	 *
	 * @param longitude
	 *        the longitude to set
	 */
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	/**
	 * Get the elevation variation.
	 *
	 * @return the variation
	 */
	public double getElevationVariation() {
		return elevationVariation;
	}

	/**
	 * Set the elevation variation.
	 *
	 * @param elevationVariation
	 *        the variation to set
	 */
	public void setElevationVariation(double elevationVariation) {
		this.elevationVariation = elevationVariation;
	}

	/**
	 * Get the latitude variation.
	 *
	 * @return the variation
	 */
	public double getLatitudeVariation() {
		return latitudeVariation;
	}

	/**
	 * Set the latitude variation.
	 *
	 * @param latitudeVariation
	 *        the variation to set
	 */
	public void setLatitudeVariation(double latitudeVariation) {
		this.latitudeVariation = latitudeVariation;
	}

	/**
	 * Get the longitude variation.
	 *
	 * @return the variation
	 */
	public double getLongitudeVariation() {
		return longitudeVariation;
	}

	/**
	 * Set the longitude variation.
	 *
	 * @param longitudeVariation
	 *        the variation to set
	 */
	public void setLongitudeVariation(double longitudeVariation) {
		this.longitudeVariation = longitudeVariation;
	}

	/**
	 * Get the fix delay.
	 *
	 * @return the fix delay, in milliseconds
	 */
	public long getFixDelayMs() {
		return fixDelayMs;
	}

	/**
	 * Set the fix delay.
	 *
	 * @param fixDelayMs
	 *        the delay to set, in milliseconds
	 */
	public void setFixDelayMs(long fixDelayMs) {
		this.fixDelayMs = fixDelayMs;
	}

	/**
	 * Get the fix lock.
	 *
	 * @return the fix lock duration, in milliseconds
	 */
	public long getFixLockMs() {
		return fixLockMs;
	}

	/**
	 * Set the fix lock.
	 *
	 * @param fixLockMs
	 *        the fix lock to set, in milliseconds
	 */
	public void setFixLockMs(long fixLockMs) {
		this.fixLockMs = fixLockMs;
	}

	/**
	 * Get the fix unlocked variation multiplier.
	 *
	 * @return the multiplier
	 */
	public double getFixUnlockedVariationMultiplier() {
		return fixUnlockedVariationMultiplier;
	}

	/**
	 * Set the fix unlocked variation multiplier.
	 *
	 * @param fixUnlockedVariationMultiplier
	 *        the multiplier to set
	 */
	public void setFixUnlockedVariationMultiplier(double fixUnlockedVariationMultiplier) {
		this.fixUnlockedVariationMultiplier = fixUnlockedVariationMultiplier;
	}

}
