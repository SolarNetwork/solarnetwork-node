/* ==================================================================
 * BluetoothSetupService.java - 6/10/2026 8:30:00 AM
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.bluetooth;

import static net.solarnetwork.node.Constants.solarNodeHome;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.MetadataService;
import net.solarnetwork.node.service.OperationalModesService;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.service.FilterableService;
import net.solarnetwork.service.Identifiable;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.OptionalService.OptionalFilterableService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicTextAreaSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.DateUtils;

/**
 * Settings provider and instruction handler that controls when the Bluetooth
 * setup radio is on.
 *
 * <p>
 * The radio is always on unless {@link #isAlwaysOn()} is turned off. Then it is
 * on only while the {@link AvailabilitySchedule} applies, the
 * {@link #getOpMode()} override mode is active, or the
 * {@link #getOfflineOpMode()} mode has been active for
 * {@link #getOfflineThresholdMinutes()} minutes (until
 * {@link #getOnlineGraceMinutes()} minutes after it clears). The schedule comes
 * from metadata when {@link #getMetadataPath()} is configured and loaded,
 * otherwise from {@link #getSchedule()}.
 * </p>
 *
 * <p>
 * Handles the {@link InstructionHandler#TOPIC_SYSTEM_CONFIGURE} topic for the
 * {@link #BLUETOOTH_SERVICE_NAME} service, with the {@link #ACTION_STATUS},
 * {@link #ACTION_ENABLE}, {@link #ACTION_DISABLE} and {@link #ACTION_RESTART}
 * actions. The radio is controlled with {@code solarcfg bluetooth <action>}.
 * </p>
 *
 * @author elijah
 * @version 1.0
 */
public class BluetoothSetupService extends BaseIdentifiable
		implements SettingSpecifierProvider, SettingsChangeObserver, InstructionHandler, EventHandler {

	/** The {@code solarcfg} service name for the Bluetooth radio. */
	public static final String CONFIG_SERVICE = "bluetooth";

	/** The default value for the {@code command} property. */
	public static final String DEFAULT_COMMAND = solarNodeHome() + "/bin/solarcfg";

	/** The {@literal service} instruction parameter value for this service. */
	public static final String BLUETOOTH_SERVICE_NAME = "/setup/bluetooth";

	/** The {@literal action} instruction parameter name. */
	public static final String PARAM_ACTION = "action";

	/** The {@literal duration} instruction parameter name, in seconds. */
	public static final String PARAM_DURATION = "duration";

	/** The {@literal action} parameter value to return the current status. */
	public static final String ACTION_STATUS = "status";

	/** The {@literal action} parameter value to enable the radio. */
	public static final String ACTION_ENABLE = "enable";

	/** The {@literal action} parameter value to disable the radio. */
	public static final String ACTION_DISABLE = "disable";

	/** The {@literal action} parameter value to restart the peripheral. */
	public static final String ACTION_RESTART = "restart";

	/** The default value for the {@code opMode} property. */
	public static final String DEFAULT_OP_MODE = "bt-setup";

	/**
	 * The default value for the {@code offlineOpMode} property: the mode an
	 * HTTP Ping control enables when its ping fails.
	 */
	public static final String DEFAULT_OFFLINE_OP_MODE = "net-offline";

	/** The default value for the {@code offlineThresholdMinutes} property. */
	public static final int DEFAULT_OFFLINE_THRESHOLD_MINUTES = 15;

	/** The default value for the {@code onlineGraceMinutes} property. */
	public static final int DEFAULT_ONLINE_GRACE_MINUTES = 10;

	/** The default value for the {@code watchdogIntervalSeconds} property. */
	public static final int DEFAULT_WATCHDOG_INTERVAL_SECONDS = 60;

	/** The default value for the {@code defaultDurationSeconds} property. */
	public static final long DEFAULT_DEFAULT_DURATION_SECONDS = 1800L;

	/** The default value for the {@code maxDurationSeconds} property. */
	public static final long DEFAULT_MAX_DURATION_SECONDS = 14400L;

	/** The default value for the {@code commandTimeoutSeconds} property. */
	public static final long DEFAULT_COMMAND_TIMEOUT_SECONDS = 60L;

	/** The number of seconds between availability schedule checks. */
	public static final int SCHEDULE_TICK_SECONDS = 15;

	/** The default value for the {@code metadataServiceUid} property. */
	public static final String DEFAULT_METADATA_SERVICE_UID = "Node Metadata Service";

	/**
	 * The number of seconds to wait before reading the metadata schedule again
	 * after the metadata could not be loaded.
	 */
	public static final int METADATA_RETRY_SECONDS = 300;

	/** The {@code scheduleSource} status value for the Schedule setting. */
	public static final String SCHEDULE_SOURCE_SETTING = "setting";

	/** The {@code scheduleSource} status value for a metadata schedule. */
	public static final String SCHEDULE_SOURCE_METADATA = "metadata";

	/**
	 * The time to wait for the output reader threads after the process exits.
	 */
	private static final long READER_JOIN_MILLISECONDS = 5000L;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final OperationalModesService opModesService;
	private final TaskScheduler taskScheduler;
	private final @Nullable OptionalFilterableService<MetadataService> metadataService;

	private Clock clock = Clock.systemUTC();

	private String command = DEFAULT_COMMAND;
	private long commandTimeoutSeconds = DEFAULT_COMMAND_TIMEOUT_SECONDS;
	private String opMode = DEFAULT_OP_MODE;
	private boolean alwaysOn = true;
	private String schedule = "";
	private String timeZone = "";
	private String metadataServiceUid = DEFAULT_METADATA_SERVICE_UID;
	private String metadataPath = "";
	private boolean offlineTriggerEnabled = true;
	private int offlineThresholdMinutes = DEFAULT_OFFLINE_THRESHOLD_MINUTES;
	private int onlineGraceMinutes = DEFAULT_ONLINE_GRACE_MINUTES;
	private int watchdogIntervalSeconds = DEFAULT_WATCHDOG_INTERVAL_SECONDS;
	private String offlineOpMode = DEFAULT_OFFLINE_OP_MODE;
	private long defaultDurationSeconds = DEFAULT_DEFAULT_DURATION_SECONDS;
	private long maxDurationSeconds = DEFAULT_MAX_DURATION_SECONDS;
	private int enableMinutes = 0;

	private boolean opModeActive = false;
	private boolean offline = false;
	private Instant offlineSince = null;
	private Instant onlineSince = null;
	private String lastOfflineOpMode = DEFAULT_OFFLINE_OP_MODE;
	private ScheduledFuture<?> watchdogFuture = null;
	private int watchdogScheduledIntervalSeconds = 0;
	private @Nullable AvailabilitySchedule availability = null;
	private @Nullable String scheduleError = null;
	private String scheduleSource = SCHEDULE_SOURCE_SETTING;
	private @Nullable ZoneId zone = null;
	private @Nullable String zoneError = null;
	private @Nullable AvailabilitySchedule inlineSchedule = null;
	private @Nullable String inlineError = null;

	// the last metadata schedule result: a schedule or a parse error, kept while
	// the metadata is unreachable so an outage never restores an older schedule
	private boolean metadataInEffect = false;
	private @Nullable AvailabilitySchedule metadataSchedule = null;
	private @Nullable String metadataError = null;
	private @Nullable Object lastMetadataValue = null;
	private @Nullable String metadataReadError = null;
	private @Nullable Instant metadataRetryAfter = null;
	private long metadataGeneration = 0;
	private boolean metadataReadInFlight = false;
	private String lastMetadataServiceUid = DEFAULT_METADATA_SERVICE_UID;
	private String lastMetadataPath = "";
	private ScheduledFuture<?> scheduleFuture = null;
	private @Nullable Boolean lastScheduleAllows = null;
	private boolean lastReconcileOk = false;
	private boolean stopped = false;

	/**
	 * Constructor.
	 *
	 * @param opModesService
	 *        the operational modes service
	 * @param taskScheduler
	 *        the task scheduler
	 * @param metadataService
	 *        the metadata service to read a schedule from, or {@literal null}
	 */
	public BluetoothSetupService(OperationalModesService opModesService, TaskScheduler taskScheduler,
			@Nullable OptionalFilterableService<MetadataService> metadataService) {
		super();
		this.opModesService = opModesService;
		this.taskScheduler = taskScheduler;
		this.metadataService = metadataService;
		FilterableService.setFilterProp((FilterableService) metadataService, Identifiable.UID_PROPERTY,
				DEFAULT_METADATA_SERVICE_UID);
		setUid("net.solarnetwork.node.setup.bluetooth.BluetoothSetupService");
		setDisplayName("Bluetooth Setup");
	}

	/**
	 * Start up the service.
	 *
	 * <p>
	 * Reconciles the radio and starts the watchdog and schedule checks. A
	 * metadata schedule is read afterwards, without holding the service lock.
	 * </p>
	 */
	public void startup() {
		synchronized ( this ) {
			stopped = false;
			opModeActive = opModesService.isOperationalModeActive(opMode);
			lastOfflineOpMode = offlineOpMode;
			lastMetadataServiceUid = metadataServiceUid;
			lastMetadataPath = metadataPath;
			applySchedule();
			reconcileQuietly();
			scheduleWatchdog();
			scheduleScheduleTick();
		}
		requestMetadataRefresh();
	}

	/**
	 * Shut down the service, leaving the radio in its current state.
	 */
	public synchronized void shutdown() {
		// a tick already running, or a one-off metadata read already queued, must
		// not change the radio after shutdown
		stopped = true;
		cancelWatchdog();
		cancelScheduleTick();
	}

	@Override
	public void configurationChanged(Map<String, Object> properties) {
		synchronized ( this ) {
			configurationChangedInternal();
		}
		requestMetadataRefresh();
	}

	private void configurationChangedInternal() {
		opModeActive = opModesService.isOperationalModeActive(opMode);
		// transient setting: apply once, then reset so the UI shows 0 again
		if ( enableMinutes > 0 ) {
			final long seconds = enableMinutes * 60L;
			enableMinutes = 0;
			log.info("Bluetooth setup radio enable for {}s requested via settings", seconds);
			try {
				enableForSeconds(seconds);
			} catch ( Exception e ) {
				log.warn("Error enabling Bluetooth setup radio: {}", e.getMessage());
			}
		}
		if ( watchdogFuture != null && watchdogScheduledIntervalSeconds != watchdogIntervalSeconds ) {
			cancelWatchdog();
		}
		if ( watchdogFuture == null ) {
			scheduleWatchdog();
		}
		if ( !offlineDetectionActive() || !offlineOpMode.equals(lastOfflineOpMode) ) {
			// stale state from a disabled trigger or a different mode must not
			// hold the radio on
			clearOfflineState();
		}
		lastOfflineOpMode = offlineOpMode;
		if ( !metadataServiceUid.equals(lastMetadataServiceUid)
				|| !metadataPath.equals(lastMetadataPath) ) {
			// a different source: forget the old source's schedule and drop any
			// read still in flight for it
			metadataGeneration++;
			clearMetadataState();
			metadataReadInFlight = false;
		}
		lastMetadataServiceUid = metadataServiceUid;
		lastMetadataPath = metadataPath;
		metadataRetryAfter = null;
		applySchedule();
		reconcileQuietly();
		scheduleScheduleTick();
	}

	@Override
	public void handleEvent(Event event) {
		if ( event == null || !OperationalModesService.EVENT_TOPIC_OPERATIONAL_MODES_CHANGED
				.equals(event.getTopic()) ) {
			return;
		}
		synchronized ( this ) {
			final boolean active = OperationalModesService.hasActiveOperationalMode(event, opMode);
			final boolean opModeChanged = (active != opModeActive);
			if ( opModeChanged ) {
				opModeActive = active;
				log.info("Bluetooth setup operational mode [{}] is now {}", opMode,
						active ? "active" : "inactive");
			}
			final boolean wasOffline = offline;
			if ( offlineDetectionActive() ) {
				updateOfflineState(
						OperationalModesService.hasActiveOperationalMode(event, offlineOpMode));
			}
			if ( !opModeChanged && offline == wasOffline ) {
				return;
			}
		}
		// reconcile on another thread so the event delivery thread is not blocked
		// on the (slow) OS helper command
		if ( taskScheduler != null ) {
			taskScheduler.schedule(this::reconcileQuietly, clock.instant());
		} else {
			reconcileQuietly();
		}
	}

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SYSTEM_CONFIGURE.equals(topic);
	}

	@Override
	public synchronized InstructionStatus processInstruction(Instruction instruction) {
		if ( instruction == null || !handlesTopic(instruction.getTopic())
				|| !BLUETOOTH_SERVICE_NAME.equals(instruction.getParameterValue(PARAM_SERVICE)) ) {
			return null;
		}
		String action = instruction.getParameterValue(PARAM_ACTION);
		if ( action == null || action.isEmpty() ) {
			action = ACTION_STATUS;
		}
		Map<String, Object> resultParams = new LinkedHashMap<>(2);
		InstructionState resultState = InstructionState.Completed;
		try {
			switch (action.toLowerCase(Locale.ENGLISH)) {
				case ACTION_STATUS:
					resultParams.put(PARAM_SERVICE_RESULT, currentStatus());
					break;
				case ACTION_ENABLE:
					enableForSeconds(BluetoothSetupUtils
							.parseDuration(instruction.getParameterValue(PARAM_DURATION)));
					resultParams.put(PARAM_SERVICE_RESULT, currentStatus());
					break;
				case ACTION_DISABLE:
					opModesService.disableOperationalModes(Collections.singleton(opMode));
					opModeActive = false;
					reconcile();
					resultParams.put(PARAM_SERVICE_RESULT, currentStatus());
					break;
				case ACTION_RESTART:
					// the helper only restarts a running peripheral; reconcile afterwards
					// so the radio always ends in its gated state
					executeAction(ACTION_RESTART);
					reconcile();
					resultParams.put(PARAM_SERVICE_RESULT, currentStatus());
					break;
				default:
					resultParams.put(PARAM_MESSAGE,
							getMessageSource().getMessage("error.unsupportedAction",
									new Object[] { action }, "Unsupported action.",
									Locale.getDefault()));
					resultState = InstructionState.Declined;
			}
		} catch ( Exception e ) {
			resultParams.put(PARAM_MESSAGE, e.toString());
			resultState = InstructionState.Declined;
		}
		return InstructionUtils.createStatus(instruction, resultState, Instant.now(), resultParams);
	}

	/**
	 * Enable the operational mode, and so the radio, for a period of time.
	 *
	 * @param requestedSeconds
	 *        the requested duration, in seconds; {@literal 0} or less for the
	 *        default duration; capped at the maximum duration
	 */
	public synchronized void enableForSeconds(long requestedSeconds) {
		long seconds = (requestedSeconds <= 0 ? defaultDurationSeconds : requestedSeconds);
		if ( maxDurationSeconds > 0 && seconds > maxDurationSeconds ) {
			seconds = maxDurationSeconds;
		}
		final Instant expire = clock.instant().plusSeconds(seconds);
		log.info("Enabling Bluetooth setup operational mode [{}] until {}", opMode, expire);
		opModesService.enableOperationalModes(Collections.singleton(opMode), expire);
		opModeActive = true;
		reconcile();
	}

	/**
	 * Get the desired radio state.
	 *
	 * @return {@literal true} if the radio should be on
	 */
	public synchronized boolean desiredState() {
		return (alwaysOn || scheduleAllows() || opModeActive || (offlineDetectionActive() && offline));
	}

	/**
	 * Test if the availability schedule applies now.
	 *
	 * <p>
	 * A blank schedule, an unparseable schedule, or an invalid time zone never
	 * applies.
	 * </p>
	 *
	 * @return {@literal true} if the schedule allows the radio on now
	 */
	public synchronized boolean scheduleAllows() {
		final AvailabilitySchedule sched = this.availability;
		final ZoneId z = this.zone;
		if ( sched == null || sched.isEmpty() || scheduleError != null || z == null ) {
			return false;
		}
		return sched.applies(LocalDateTime.ofInstant(clock.instant(), z));
	}

	/**
	 * Parse the configured schedule and time zone.
	 *
	 * <p>
	 * Never throws: a parse error is recorded in {@code scheduleError} and the
	 * schedule then never applies.
	 * </p>
	 */
	private void applySchedule() {
		AvailabilitySchedule sched = null;
		String error = null;
		try {
			sched = AvailabilitySchedule.parse(schedule);
		} catch ( IllegalArgumentException e ) {
			error = e.getMessage();
		}
		ZoneId z = null;
		String zError = null;
		try {
			z = DateUtils.tz(timeZone == null || timeZone.isBlank() ? null : timeZone.trim());
		} catch ( IllegalArgumentException e ) {
			zError = e.getMessage();
		}
		this.inlineSchedule = sched;
		this.inlineError = error;
		this.zone = z;
		this.zoneError = zError;
		updateEffectiveSchedule();
		this.lastScheduleAllows = null;
	}

	/**
	 * Choose the schedule in effect: the metadata schedule when one is
	 * configured and loaded, otherwise the Schedule setting.
	 *
	 * @return {@literal true} if the schedule in effect changed
	 */
	private boolean updateEffectiveSchedule() {
		final boolean useMetadata = metadataConfigured() && metadataInEffect;
		final AvailabilitySchedule sched = (useMetadata ? metadataSchedule : inlineSchedule);
		String error = (useMetadata ? metadataError : inlineError);
		if ( zoneError != null ) {
			error = (error != null ? error + "; " : "") + zoneError;
		}
		final String source = (useMetadata ? SCHEDULE_SOURCE_METADATA : SCHEDULE_SOURCE_SETTING);
		final boolean changed = (sched != availability || !Objects.equals(error, scheduleError)
				|| !source.equals(scheduleSource));
		if ( error != null && !error.equals(this.scheduleError) ) {
			log.warn("Bluetooth setup schedule is invalid and will not apply: {}", error);
		}
		this.availability = sched;
		this.scheduleError = error;
		this.scheduleSource = source;
		if ( changed ) {
			this.lastScheduleAllows = null;
		}
		return changed;
	}

	private boolean metadataConfigured() {
		return (metadataService != null && !metadataServiceUid.isEmpty() && !metadataPath.isEmpty());
	}

	private void clearMetadataState() {
		metadataInEffect = false;
		metadataSchedule = null;
		metadataError = null;
		lastMetadataValue = null;
		metadataReadError = null;
		metadataRetryAfter = null;
	}

	/**
	 * Read the metadata schedule soon, without holding the service lock.
	 */
	private void requestMetadataRefresh() {
		synchronized ( this ) {
			// with Always On the schedule is ignored, so there is nothing to read;
			// turning Always On off is a configuration change that requests a read
			if ( alwaysOn || !metadataConfigured() ) {
				return;
			}
		}
		if ( taskScheduler != null ) {
			taskScheduler.schedule(this::scheduleTick, clock.instant());
		} else {
			scheduleTick();
		}
	}

	/** The outcome of reading the metadata schedule. */
	private enum MetadataReadKind {
		/** No metadata service is available. */
		NO_SERVICE,
		/** The metadata could not be loaded. */
		UNREACHABLE,
		/** The metadata loaded but has no value at the path. */
		ABSENT,
		/** The metadata has a value at the path. */
		VALUE,
	}

	/**
	 * Read the metadata schedule, if one is configured and due.
	 *
	 * <p>
	 * The metadata service is called without holding the service lock, because
	 * loading metadata can mean a slow network request. At most one read runs
	 * at a time, and a result for a source that has since been reconfigured is
	 * ignored.
	 * </p>
	 */
	private void refreshMetadataSchedule() {
		final String path;
		final long generation;
		synchronized ( this ) {
			if ( stopped || !metadataConfigured() || metadataReadInFlight ) {
				return;
			}
			if ( metadataRetryAfter != null && clock.instant().isBefore(metadataRetryAfter) ) {
				return;
			}
			path = metadataPath;
			generation = metadataGeneration;
			metadataReadInFlight = true;
		}
		MetadataReadKind kind;
		Object value = null;
		String message = null;
		try {
			final MetadataService service = OptionalService.service(metadataService);
			if ( service == null ) {
				kind = MetadataReadKind.NO_SERVICE;
			} else {
				final GeneralDatumMetadata meta = service.getAllMetadata();
				if ( meta == null ) {
					kind = MetadataReadKind.UNREACHABLE;
				} else {
					value = meta.metadataAtPath(path);
					kind = (value != null ? MetadataReadKind.VALUE : MetadataReadKind.ABSENT);
				}
			}
		} catch ( Exception e ) {
			kind = MetadataReadKind.UNREACHABLE;
			message = e.getMessage();
		} catch ( Throwable t ) {
			// release the in-flight flag so an Error does not block later reads
			synchronized ( this ) {
				if ( generation == metadataGeneration ) {
					metadataReadInFlight = false;
				}
			}
			throw t;
		}
		applyMetadataResult(generation, path, kind, value, message);
	}

	private synchronized void applyMetadataResult(long generation, String path, MetadataReadKind kind,
			@Nullable Object value, @Nullable String message) {
		if ( generation != metadataGeneration ) {
			// the source was reconfigured while reading; a newer read applies
			return;
		}
		metadataReadInFlight = false;
		if ( stopped ) {
			return;
		}
		switch (kind) {
			case NO_SERVICE:
				// cheap to retry, so no back-off
				metadataReadError = "no Metadata Service available";
				break;

			case UNREACHABLE:
				// keep the last result, schedule or error alike
				metadataReadError = "metadata unavailable" + (message != null ? ": " + message : "");
				metadataRetryAfter = clock.instant().plusSeconds(METADATA_RETRY_SECONDS);
				log.warn("Bluetooth setup metadata schedule at [{}] could not be loaded: {}", path,
						metadataReadError);
				break;

			case ABSENT:
				if ( metadataInEffect ) {
					log.info(
							"Bluetooth setup metadata schedule at [{}] removed; using the Schedule setting",
							path);
				}
				clearMetadataState();
				metadataReadError = "no schedule at " + path;
				break;

			case VALUE:
				metadataReadError = null;
				if ( !metadataInEffect || !Objects.equals(value, lastMetadataValue) ) {
					try {
						metadataSchedule = AvailabilitySchedule.fromMetadata(value);
						metadataError = null;
						log.info("Bluetooth setup schedule loaded from metadata at [{}]", path);
					} catch ( RuntimeException e ) {
						// fail closed: an invalid metadata schedule never applies
						metadataSchedule = null;
						metadataError = "metadata schedule invalid: " + e.getMessage();
					}
					lastMetadataValue = value;
					metadataInEffect = true;
				}
				break;
		}
		updateEffectiveSchedule();
	}

	/**
	 * Check the availability schedule and reconcile the radio if it has crossed
	 * a window edge since the last check.
	 */
	public void scheduleTick() {
		refreshMetadataSchedule();
		synchronized ( this ) {
			if ( stopped ) {
				return;
			}
			final boolean allows = scheduleAllows();
			if ( lastScheduleAllows != null && lastScheduleAllows.booleanValue() == allows ) {
				return;
			}
			log.info("Bluetooth setup schedule now {} the radio on",
					allows ? "allows" : "does not allow");
			// only treat the edge as handled once the radio really followed it, so a
			// failed helper call is retried on the next tick
			lastScheduleAllows = (reconcileQuietly() ? Boolean.valueOf(allows) : null);
		}
	}

	private void scheduleScheduleTick() {
		final AvailabilitySchedule sched = this.availability;
		final boolean wanted = (!alwaysOn && taskScheduler != null && (metadataConfigured()
				|| (sched != null && !sched.isEmpty() && scheduleError == null)));
		if ( !wanted ) {
			cancelScheduleTick();
			return;
		}
		// the caller has just reconciled; seed the edge detector from that result so
		// the first tick does not report a false edge (or does retry a failed one)
		lastScheduleAllows = (lastReconcileOk ? Boolean.valueOf(scheduleAllows()) : null);
		if ( scheduleFuture != null ) {
			return;
		}
		final Duration interval = Duration.ofSeconds(SCHEDULE_TICK_SECONDS);
		scheduleFuture = taskScheduler.scheduleWithFixedDelay(this::scheduleTick,
				clock.instant().plus(interval), interval);
	}

	private void cancelScheduleTick() {
		if ( scheduleFuture != null ) {
			scheduleFuture.cancel(false);
			scheduleFuture = null;
		}
	}

	/**
	 * Apply the desired radio state, if it differs from the actual state.
	 *
	 * <p>
	 * The actual state is always queried from the OS so this is idempotent and
	 * self-healing.
	 * </p>
	 */
	public synchronized boolean reconcile() {
		final boolean want = desiredState();
		final BluetoothSetupStatus status = currentStatus();
		if ( status == null ) {
			log.warn("Unable to determine Bluetooth setup radio status; not changing state.");
			return false;
		}
		if ( want != status.isRadioActive() ) {
			log.info("Turning Bluetooth setup radio {}", want ? "ON" : "OFF");
			executeAction(want ? ACTION_ENABLE : ACTION_DISABLE);
		} else if ( !want && (status.isDiscoverable() || status.isPowered()) ) {
			// peripheral is stopped but the adapter was left on (e.g. after boot or
			// an upgrade): turn it off
			log.info("Bluetooth adapter is on while the setup radio should be off; turning it off");
			executeAction(ACTION_DISABLE);
		}
		return true;
	}

	/**
	 * Reconcile, logging instead of throwing on failure.
	 *
	 * @return {@literal true} if the radio is known to be in the desired state
	 */
	private boolean reconcileQuietly() {
		boolean ok = false;
		try {
			ok = reconcile();
		} catch ( Exception e ) {
			log.warn("Error reconciling Bluetooth setup radio state: {}", e.getMessage());
		}
		lastReconcileOk = ok;
		return ok;
	}

	private void scheduleWatchdog() {
		if ( taskScheduler == null || watchdogIntervalSeconds < 1 ) {
			return;
		}
		final Duration interval = Duration.ofSeconds(watchdogIntervalSeconds);
		watchdogFuture = taskScheduler.scheduleWithFixedDelay(this::watchdogTick,
				clock.instant().plus(interval), interval);
		watchdogScheduledIntervalSeconds = watchdogIntervalSeconds;
	}

	private void cancelWatchdog() {
		if ( watchdogFuture != null ) {
			watchdogFuture.cancel(false);
			watchdogFuture = null;
		}
	}

	/**
	 * Check the offline operational mode and reconcile the radio state.
	 */
	public synchronized void watchdogTick() {
		try {
			if ( offlineDetectionActive() ) {
				updateOfflineState(opModesService.isOperationalModeActive(offlineOpMode));
			}
		} catch ( Exception e ) {
			log.warn("Error checking offline operational mode [{}]: {}", offlineOpMode, e.getMessage());
		}
		reconcileQuietly();
	}

	/**
	 * Test if offline detection is active.
	 *
	 * <p>
	 * The watchdog must be running, because only it advances the threshold and
	 * grace periods.
	 * </p>
	 *
	 * @return {@literal true} if offline detection is active
	 */
	private boolean offlineDetectionActive() {
		return (offlineTriggerEnabled && !offlineOpMode.isEmpty() && watchdogIntervalSeconds > 0);
	}

	private void clearOfflineState() {
		if ( offline || offlineSince != null ) {
			log.info("Bluetooth setup offline trigger cleared by a configuration change");
		}
		offline = false;
		offlineSince = null;
		onlineSince = null;
	}

	/**
	 * Update the offline state.
	 *
	 * @param down
	 *        {@literal true} if the offline operational mode is active
	 */
	private void updateOfflineState(final boolean down) {
		final Instant now = clock.instant();
		if ( down ) {
			onlineSince = null;
			if ( offlineSince == null ) {
				offlineSince = now;
				log.info("Offline mode [{}] active; Bluetooth setup radio will turn on after {} minutes",
						offlineOpMode, offlineThresholdMinutes);
			}
			if ( !offline && !Duration.between(offlineSince, now)
					.minus(Duration.ofMinutes(offlineThresholdMinutes)).isNegative() ) {
				offline = true;
				log.info("Offline mode [{}] active since {}; turning Bluetooth setup radio on",
						offlineOpMode, offlineSince);
			}
		} else {
			offlineSince = null;
			if ( offline ) {
				if ( onlineSince == null ) {
					onlineSince = now;
					log.info(
							"Offline mode [{}] inactive; Bluetooth setup radio will turn off after {} minutes",
							offlineOpMode, onlineGraceMinutes);
				}
				if ( !Duration.between(onlineSince, now).minus(Duration.ofMinutes(onlineGraceMinutes))
						.isNegative() ) {
					log.info("Offline mode [{}] inactive since {}; offline trigger cleared",
							offlineOpMode, onlineSince);
					offline = false;
					onlineSince = null;
				}
			}
		}
	}

	@Override
	public String getSettingUid() {
		return getUid();
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		final BluetoothSetupStatus status = currentStatus();
		final List<SettingSpecifier> result = new ArrayList<>(16);
		result.add(new BasicTitleSettingSpecifier("status",
				BluetoothSetupUtils.statusMessage(getMessageSource(), status, opMode, offlineOpMode,
						offlineThresholdMinutes, metadataConfigured() ? metadataPath : null)));

		BasicTextFieldSettingSpecifier enable = new BasicTextFieldSettingSpecifier("enableMinutes", "0");
		enable.setTransient(true);
		result.add(enable);

		result.add(new BasicToggleSettingSpecifier("alwaysOn", Boolean.TRUE));
		result.add(new BasicTextAreaSettingSpecifier("schedule", "", true));
		result.add(new BasicTextFieldSettingSpecifier("timeZone", ""));
		result.add(new BasicTextFieldSettingSpecifier("metadataServiceUid", DEFAULT_METADATA_SERVICE_UID,
				false, "(objectClass=net.solarnetwork.node.service.MetadataService)"));
		result.add(new BasicTextFieldSettingSpecifier("metadataPath", ""));
		result.add(new BasicTextFieldSettingSpecifier("opMode", DEFAULT_OP_MODE));
		result.add(new BasicTextFieldSettingSpecifier("defaultDurationSeconds",
				String.valueOf(DEFAULT_DEFAULT_DURATION_SECONDS)));
		result.add(new BasicTextFieldSettingSpecifier("maxDurationSeconds",
				String.valueOf(DEFAULT_MAX_DURATION_SECONDS)));
		result.add(new BasicToggleSettingSpecifier("offlineTriggerEnabled", Boolean.TRUE));
		result.add(new BasicTextFieldSettingSpecifier("offlineThresholdMinutes",
				String.valueOf(DEFAULT_OFFLINE_THRESHOLD_MINUTES)));
		result.add(new BasicTextFieldSettingSpecifier("onlineGraceMinutes",
				String.valueOf(DEFAULT_ONLINE_GRACE_MINUTES)));
		result.add(new BasicTextFieldSettingSpecifier("watchdogIntervalSeconds",
				String.valueOf(DEFAULT_WATCHDOG_INTERVAL_SECONDS)));
		result.add(new BasicTextFieldSettingSpecifier("offlineOpMode", DEFAULT_OFFLINE_OP_MODE));
		result.add(new BasicTextFieldSettingSpecifier("command", DEFAULT_COMMAND));
		result.add(new BasicTextFieldSettingSpecifier("commandTimeoutSeconds",
				String.valueOf(DEFAULT_COMMAND_TIMEOUT_SECONDS)));
		return result;
	}

	/**
	 * Get the current status.
	 *
	 * @return the status, or {@literal null} if the OS status cannot be
	 *         determined
	 */
	public synchronized @Nullable BluetoothSetupStatus currentStatus() {
		boolean active = false;
		boolean discoverable = false;
		boolean powered = false;
		boolean enabled = false;
		try {
			List<String> result = executeAction(ACTION_STATUS);
			if ( result != null ) {
				for ( String line : result ) {
					int idx = line.indexOf(':');
					if ( idx < 0 ) {
						continue;
					}
					String key = line.substring(0, idx).trim().toLowerCase(Locale.ENGLISH);
					String value = line.substring(idx + 1).trim();
					switch (key) {
						case "active":
							active = "true".equalsIgnoreCase(value);
							break;
						case "discoverable":
							discoverable = "true".equalsIgnoreCase(value);
							break;
						case "powered":
							powered = "true".equalsIgnoreCase(value);
							break;
						case "enabled":
							enabled = "true".equalsIgnoreCase(value);
							break;
						default:
							// ignore
					}
				}
			}
		} catch ( Throwable t ) {
			log.warn("Error getting current Bluetooth setup radio status: {}", t.getMessage());
			return null;
		}
		Long expires = null;
		if ( opModeActive ) {
			try {
				Map<String, Long> expirations = opModesService.activeOperationalModesWithExpirations();
				if ( expirations != null ) {
					expires = expirations.get(opMode);
				}
			} catch ( Exception e ) {
				log.debug("Error getting operational mode expirations: {}", e.getMessage());
			}
		}
		final AvailabilitySchedule sched = this.availability;
		final ZoneId z = this.zone;
		Long next = null;
		boolean schedActive = false;
		if ( sched != null && !sched.isEmpty() && scheduleError == null && z != null ) {
			LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), z);
			schedActive = sched.applies(now);
			LocalDateTime boundary = sched.nextBoundary(now, z);
			if ( boundary != null ) {
				next = boundary.atZone(z).toInstant().toEpochMilli();
			}
		}
		return new BluetoothSetupStatus(active, discoverable, powered, enabled, opModeActive, expires,
				offlineDetectionActive() && offline, offlineDetectionActive() ? offlineSince : null,
				alwaysOn, sched != null ? sched.toString() : "", schedActive, scheduleError, next,
				scheduleSource, metadataConfigured() ? metadataReadError : null,
				z != null ? z.getId() : null);
	}

	private synchronized List<String> executeAction(final String action, String... args) {
		log.debug("Executing bluetooth action {}", action);
		List<String> cmd = new ArrayList<>(8);
		cmd.add(command);
		cmd.add(CONFIG_SERVICE);
		cmd.add(action);
		if ( args != null && args.length > 0 ) {
			for ( String arg : args ) {
				cmd.add(arg);
			}
		}
		final List<String> result = new ArrayList<>(8);
		final StringBuilder errBuf = new StringBuilder();
		ProcessBuilder pb = new ProcessBuilder(cmd);
		// never let the helper wait on input (e.g. an unexpected sudo prompt)
		pb.redirectInput(ProcessBuilder.Redirect.from(new File("/dev/null")));
		Process pr = null;
		try {
			pr = pb.start();
			// drain both streams concurrently so a chatty helper cannot fill a pipe
			// and block before the timeout
			Thread outReader = captureInputStream(pr.getInputStream(), result::add,
					"BluetoothSetup-" + action + "-stdout");
			Thread errReader = captureInputStream(pr.getErrorStream(), line -> {
				if ( errBuf.length() > 0 ) {
					errBuf.append('\n');
				}
				errBuf.append(line);
			}, "BluetoothSetup-" + action + "-stderr");
			if ( !pr.waitFor(commandTimeoutSeconds, TimeUnit.SECONDS) ) {
				pr.destroyForcibly();
				throw new RuntimeException("Bluetooth action " + action + " timed out after "
						+ commandTimeoutSeconds + "s.");
			}
			// the process has exited so its pipes are closed; the readers finish promptly
			outReader.join(READER_JOIN_MILLISECONDS);
			errReader.join(READER_JOIN_MILLISECONDS);
			final int exitCode = pr.exitValue();
			if ( exitCode != 0 ) {
				throw new RuntimeException("Bluetooth action " + action + " failed with exit code "
						+ exitCode + (errBuf.length() > 0 ? ": " + errBuf : "."));
			}
			if ( errBuf.length() > 0 ) {
				log.warn("Bluetooth action {} reported: {}", action, errBuf);
			}
			return result;
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		} catch ( InterruptedException e ) {
			if ( pr != null ) {
				pr.destroyForcibly();
			}
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted waiting for bluetooth action " + action, e);
		}
	}

	/**
	 * Start a daemon thread that reads lines from a stream until it closes.
	 *
	 * @param in
	 *        the stream to read
	 * @param consumer
	 *        the consumer of each line; it is only safe to read what the
	 *        consumer collected after the thread has been joined
	 * @param name
	 *        the thread name
	 * @return the started thread
	 */
	private Thread captureInputStream(final InputStream in, final Consumer<String> consumer,
			final String name) {
		Thread t = new Thread(() -> {
			try (BufferedReader r = new BufferedReader(new InputStreamReader(in))) {
				String line;
				while ( (line = r.readLine()) != null ) {
					consumer.accept(line);
				}
			} catch ( IOException e ) {
				log.debug("Error reading {}: {}", name, e.getMessage());
			}
		}, name);
		t.setDaemon(true);
		t.start();
		return t;
	}

	/**
	 * Set the clock to use.
	 *
	 * @param clock
	 *        the clock to set; defaults to the system UTC clock
	 */
	public void setClock(Clock clock) {
		if ( clock != null ) {
			this.clock = clock;
		}
	}

	/**
	 * Set the command to use.
	 *
	 * @param command
	 *        the command to set; defaults to {@link #DEFAULT_COMMAND}
	 */
	public void setCommand(String command) {
		this.command = command;
	}

	/**
	 * Get the command timeout.
	 *
	 * @return the maximum seconds to wait for the command to complete
	 */
	public long getCommandTimeoutSeconds() {
		return commandTimeoutSeconds;
	}

	/**
	 * Set the command timeout.
	 *
	 * @param commandTimeoutSeconds
	 *        the maximum seconds to wait for the command to complete; defaults
	 *        to {@link #DEFAULT_COMMAND_TIMEOUT_SECONDS}
	 */
	public void setCommandTimeoutSeconds(long commandTimeoutSeconds) {
		if ( commandTimeoutSeconds > 0 ) {
			this.commandTimeoutSeconds = commandTimeoutSeconds;
		}
	}

	/**
	 * Get the operational mode that turns the radio on.
	 *
	 * @return the mode name
	 */
	public String getOpMode() {
		return opMode;
	}

	/**
	 * Set the operational mode that turns the radio on.
	 *
	 * @param opMode
	 *        the mode name to set; defaults to {@link #DEFAULT_OP_MODE}
	 */
	public void setOpMode(String opMode) {
		if ( opMode != null && !opMode.isEmpty() ) {
			this.opMode = opMode.trim().toLowerCase(Locale.ENGLISH);
		}
	}

	/**
	 * Get the always-on setting.
	 *
	 * @return {@literal true} to keep the radio on at all times
	 */
	public boolean isAlwaysOn() {
		return alwaysOn;
	}

	/**
	 * Set the always-on setting.
	 *
	 * @param alwaysOn
	 *        {@literal true} to keep the radio on at all times
	 */
	public void setAlwaysOn(boolean alwaysOn) {
		this.alwaysOn = alwaysOn;
	}

	/**
	 * Get the availability schedule text.
	 *
	 * @return the schedule
	 */
	public String getSchedule() {
		return schedule;
	}

	/**
	 * Set the availability schedule text.
	 *
	 * @param schedule
	 *        the schedule to set; see {@link AvailabilitySchedule} for the
	 *        syntax
	 */
	public void setSchedule(String schedule) {
		this.schedule = (schedule != null ? schedule : "");
	}

	/**
	 * Get the metadata service UID.
	 *
	 * @return the UID of the metadata service to read a schedule from, such as
	 *         {@link #DEFAULT_METADATA_SERVICE_UID}
	 */
	public String getMetadataServiceUid() {
		return metadataServiceUid;
	}

	/**
	 * Set the metadata service UID.
	 *
	 * @param metadataServiceUid
	 *        the UID of the metadata service to read a schedule from; empty
	 *        means no metadata schedule
	 */
	public void setMetadataServiceUid(String metadataServiceUid) {
		this.metadataServiceUid = (metadataServiceUid != null ? metadataServiceUid.trim() : "");
		FilterableService.setFilterProp((FilterableService) metadataService, Identifiable.UID_PROPERTY,
				this.metadataServiceUid);
	}

	/**
	 * Get the metadata path.
	 *
	 * @return the metadata path to read a schedule from, or an empty string for
	 *         none
	 */
	public String getMetadataPath() {
		return metadataPath;
	}

	/**
	 * Set the metadata path.
	 *
	 * @param metadataPath
	 *        the metadata path to read a schedule from, such as
	 *        {@code /pm/bluetooth/schedule}; empty means no metadata schedule
	 */
	public void setMetadataPath(String metadataPath) {
		this.metadataPath = (metadataPath != null ? metadataPath.trim() : "");
	}

	/**
	 * Get the schedule time zone.
	 *
	 * @return the zone ID, or empty for the system default zone
	 */
	public String getTimeZone() {
		return timeZone;
	}

	/**
	 * Set the schedule time zone.
	 *
	 * @param timeZone
	 *        the zone ID to set, or empty for the system default zone
	 */
	public void setTimeZone(String timeZone) {
		this.timeZone = (timeZone != null ? timeZone : "");
	}

	/**
	 * Get the offline trigger enabled setting.
	 *
	 * @return {@literal true} to turn the radio on while the offline mode is
	 *         active
	 */
	public boolean isOfflineTriggerEnabled() {
		return offlineTriggerEnabled;
	}

	/**
	 * Set the offline trigger enabled setting.
	 *
	 * @param offlineTriggerEnabled
	 *        {@literal true} to turn the radio on while the offline mode is
	 *        active
	 */
	public void setOfflineTriggerEnabled(boolean offlineTriggerEnabled) {
		this.offlineTriggerEnabled = offlineTriggerEnabled;
	}

	/**
	 * Get the offline threshold.
	 *
	 * @return the minutes the offline mode must be active before the radio
	 *         turns on
	 */
	public int getOfflineThresholdMinutes() {
		return offlineThresholdMinutes;
	}

	/**
	 * Set the offline threshold.
	 *
	 * @param offlineThresholdMinutes
	 *        the minutes the offline mode must be active before the radio turns
	 *        on
	 */
	public void setOfflineThresholdMinutes(int offlineThresholdMinutes) {
		this.offlineThresholdMinutes = offlineThresholdMinutes;
	}

	/**
	 * Get the online grace period.
	 *
	 * @return the minutes to keep the radio on after the offline mode becomes
	 *         inactive
	 */
	public int getOnlineGraceMinutes() {
		return onlineGraceMinutes;
	}

	/**
	 * Set the online grace period.
	 *
	 * @param onlineGraceMinutes
	 *        the minutes to keep the radio on after the offline mode becomes
	 *        inactive
	 */
	public void setOnlineGraceMinutes(int onlineGraceMinutes) {
		this.onlineGraceMinutes = onlineGraceMinutes;
	}

	/**
	 * Get the watchdog interval.
	 *
	 * @return the seconds between checks of the offline mode and the radio
	 *         state
	 */
	public int getWatchdogIntervalSeconds() {
		return watchdogIntervalSeconds;
	}

	/**
	 * Set the watchdog interval.
	 *
	 * @param watchdogIntervalSeconds
	 *        the seconds between checks of the offline mode and the radio
	 *        state; {@literal 0} disables the watchdog, including the offline
	 *        trigger
	 */
	public void setWatchdogIntervalSeconds(int watchdogIntervalSeconds) {
		this.watchdogIntervalSeconds = watchdogIntervalSeconds;
	}

	/**
	 * Get the offline operational mode.
	 *
	 * @return the mode that means the network is offline, or an empty string if
	 *         offline detection is disabled
	 */
	public String getOfflineOpMode() {
		return offlineOpMode;
	}

	/**
	 * Set the offline operational mode.
	 *
	 * <p>
	 * A {@code !} prefix is not supported, because the operational modes
	 * service and modes-changed events treat it differently; such a value
	 * disables offline detection, as does an empty value.
	 * </p>
	 *
	 * @param offlineOpMode
	 *        the mode that means the network is offline; defaults to
	 *        {@link #DEFAULT_OFFLINE_OP_MODE}
	 */
	public void setOfflineOpMode(String offlineOpMode) {
		String mode = (offlineOpMode != null ? offlineOpMode.trim().toLowerCase(Locale.ENGLISH) : "");
		if ( mode.startsWith("!") ) {
			log.warn(
					"Offline mode [{}] is not supported: use a plain mode name; offline trigger disabled",
					mode);
			mode = "";
		}
		this.offlineOpMode = mode;
	}

	/**
	 * Get the default enable duration.
	 *
	 * @return the seconds to enable the radio for when no duration is given
	 */
	public long getDefaultDurationSeconds() {
		return defaultDurationSeconds;
	}

	/**
	 * Set the default enable duration.
	 *
	 * @param defaultDurationSeconds
	 *        the seconds to enable the radio for when no duration is given
	 */
	public void setDefaultDurationSeconds(long defaultDurationSeconds) {
		this.defaultDurationSeconds = defaultDurationSeconds;
	}

	/**
	 * Get the maximum enable duration.
	 *
	 * @return the maximum seconds the radio can be enabled for in one request
	 */
	public long getMaxDurationSeconds() {
		return maxDurationSeconds;
	}

	/**
	 * Set the maximum enable duration.
	 *
	 * @param maxDurationSeconds
	 *        the maximum seconds the radio can be enabled for in one request;
	 *        {@literal 0} for no limit
	 */
	public void setMaxDurationSeconds(long maxDurationSeconds) {
		this.maxDurationSeconds = maxDurationSeconds;
	}

	/**
	 * Set the transient enable duration.
	 *
	 * @param enableMinutes
	 *        the minutes to enable the radio for, applied on the next
	 *        configuration change and then reset to {@literal 0}
	 */
	public void setEnableMinutes(int enableMinutes) {
		this.enableMinutes = enableMinutes;
	}

}
