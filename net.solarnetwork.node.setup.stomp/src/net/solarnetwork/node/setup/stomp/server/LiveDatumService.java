/* ==================================================================
 * LiveDatumService.java - 10/10/2026 1:42:00 PM
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

package net.solarnetwork.node.setup.stomp.server;

import static net.solarnetwork.node.setup.stomp.server.LiveDatumPublisher.publishStatus;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.DatumService;
import net.solarnetwork.node.service.OperationalModesService;
import net.solarnetwork.node.setup.stomp.LiveHeader;
import net.solarnetwork.node.setup.stomp.SetupStatus;
import net.solarnetwork.node.setup.stomp.SetupTopic;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.ServiceLifecycleObserver;

/**
 * Stream live datum properties to STOMP setup sessions.
 *
 * <p>
 * Clients subscribe to the {@link SetupTopic#DatumLive} destination with a
 * {@link LiveHeader#SourceId} header. While at least one live subscription
 * exists, a {@link LiveDatumModeManager} keeps an operational mode active. An
 * operational mode data source scheduler should be configured to poll data
 * sources (without persisting) while that mode is active. This service listens
 * for {@link DatumDataSource#EVENT_TOPIC_DATUM_CAPTURED} events and publishes
 * the requested properties to each matching subscription, via a
 * {@link LiveDatumPublisher}.
 * </p>
 *
 * <p>
 * Operational mode changes and housekeeping run on the configured
 * {@link TaskScheduler}, as they perform database I/O.
 * {@link #handleEvent(Event)} only takes each subscription's monitor, which is
 * held while queuing a frame on a channel.
 * </p>
 *
 * @author elijah
 * @version 1.0
 * @since 4.1
 */
public class LiveDatumService implements EventHandler, ServiceLifecycleObserver {

	/** The default {@code maxDurationSecs} property value. */
	public static final int DEFAULT_MAX_DURATION_SECS = 900;

	/** The default {@code maxSubscriptions} property value. */
	public static final int DEFAULT_MAX_SUBSCRIPTIONS = 4;

	/** The default {@code maxSubscriptionsPerSession} property value. */
	public static final int DEFAULT_MAX_SUBSCRIPTIONS_PER_SESSION = 2;

	/** The default {@code minIntervalMs} property value. */
	public static final long DEFAULT_MIN_INTERVAL_MS = 1_000L;

	/** The default {@code maxIntervalMs} property value. */
	public static final long DEFAULT_MAX_INTERVAL_MS = 60_000L;

	/** The default {@code sourceGraceSecs} property value. */
	public static final int DEFAULT_SOURCE_GRACE_SECS = 15;

	/** The default {@code staleSecs} property value. */
	public static final int DEFAULT_STALE_SECS = 30;

	/** The default {@code housekeepingSecs} property value. */
	public static final int DEFAULT_HOUSEKEEPING_SECS = 5;

	/** The default {@code maxProperties} property value. */
	public static final int DEFAULT_MAX_PROPERTIES = 32;

	private static final Logger log = LoggerFactory.getLogger(LiveDatumService.class);

	private final OptionalService<DatumService> datumService;
	private final LiveDatumPublisher publisher;
	private final LiveDatumModeManager modeManager;

	private final ConcurrentMap<String, LiveDatumSubscription> subscriptions = new ConcurrentHashMap<>(8,
			0.9f, 2);
	private final ConcurrentMap<String, Set<LiveDatumSubscription>> bySource = new ConcurrentHashMap<>(8,
			0.9f, 2);

	/** Guards registry compound operations, not held during I/O. */
	private final Object registryLock = new Object();

	/** Guards {@code housekeepingFuture}. */
	private final Object taskLock = new Object();
	private final AtomicBoolean syncPending = new AtomicBoolean(false);
	private volatile boolean shutdown;
	private ScheduledFuture<?> housekeepingFuture;

	private TaskScheduler taskScheduler;
	private Clock clock = Clock.systemUTC();
	private int maxDurationSecs = DEFAULT_MAX_DURATION_SECS;
	private int maxSubscriptions = DEFAULT_MAX_SUBSCRIPTIONS;
	private int maxSubscriptionsPerSession = DEFAULT_MAX_SUBSCRIPTIONS_PER_SESSION;
	private long minIntervalMs = DEFAULT_MIN_INTERVAL_MS;
	private long maxIntervalMs = DEFAULT_MAX_INTERVAL_MS;
	private int sourceGraceSecs = DEFAULT_SOURCE_GRACE_SECS;
	private int staleSecs = DEFAULT_STALE_SECS;
	private int housekeepingSecs = DEFAULT_HOUSEKEEPING_SECS;
	private int maxProperties = DEFAULT_MAX_PROPERTIES;

	/**
	 * Constructor.
	 *
	 * @param opModesService
	 *        the operational modes service
	 * @param datumService
	 *        the datum service, used to send the latest available datum when a
	 *        subscription starts
	 * @param objectMapper
	 *        the object mapper to encode message bodies with
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public LiveDatumService(OptionalService<OperationalModesService> opModesService,
			OptionalService<DatumService> datumService, ObjectMapper objectMapper) {
		super();
		this.datumService = requireNonNullArgument(datumService, "datumService");
		this.publisher = new LiveDatumPublisher(objectMapper);
		this.modeManager = new LiveDatumModeManager(opModesService, () -> !subscriptions.isEmpty());
	}

	@Override
	public void serviceDidStartup() {
		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * All subscriptions are closed, with a best-effort
	 * {@link SetupStatus#ServiceUnavailable} message sent to each, and the
	 * operational mode is disabled if this service enabled it.
	 * </p>
	 */
	@Override
	public void serviceDidShutdown() {
		final List<LiveDatumSubscription> subs;
		synchronized ( registryLock ) {
			// set flag with snapshot so subscribe() cannot add after it
			shutdown = true;
			subs = new ArrayList<>(subscriptions.values());
		}
		for ( LiveDatumSubscription sub : subs ) {
			synchronized ( sub ) {
				if ( sub.close() ) {
					publishStatus(sub, SetupStatus.ServiceUnavailable, "Service shutting down.");
				}
			}
			deregister(sub);
		}
		synchronized ( taskLock ) {
			cancelHousekeeping();
		}
		modeManager.shutdown();
	}

	/**
	 * Subscribe a session to live datum.
	 *
	 * <p>
	 * If the subscription cannot be created, a status message is published to
	 * the session for the given subscription ID and the status is returned. If
	 * the session already has a live subscription with the same ID, that
	 * subscription is ended as well, as the status is terminal for the ID.
	 * </p>
	 *
	 * @param session
	 *        the session
	 * @param subscriptionId
	 *        the subscription ID
	 * @param sourceId
	 *        the {@link LiveHeader#SourceId} header value
	 * @param properties
	 *        the {@link LiveHeader#Properties} header value
	 * @param interval
	 *        the {@link LiveHeader#Interval} header value
	 * @return the result status, never {@literal null}
	 * @throws IllegalArgumentException
	 *         if {@code session} or {@code subscriptionId} are {@literal null}
	 */
	public SetupStatus subscribe(SetupSession session, String subscriptionId, String sourceId,
			String properties, String interval) {
		requireNonNullArgument(session, "session");
		requireNonNullArgument(subscriptionId, "subscriptionId");
		final String src = (sourceId != null ? sourceId.trim() : "");
		if ( src.isEmpty() ) {
			return reject(session, subscriptionId, null, SetupStatus.Unprocessable,
					"Missing " + LiveHeader.SourceId.getValue() + " header.");
		}
		final long intervalMs;
		try {
			intervalMs = parseInterval(interval);
		} catch ( NumberFormatException e ) {
			return reject(session, subscriptionId, src, SetupStatus.Unprocessable,
					"Invalid " + LiveHeader.Interval.getValue() + " header.");
		}
		final List<String> props = parseProperties(properties);
		if ( props.size() > maxProperties ) {
			return reject(session, subscriptionId, src, SetupStatus.Unprocessable,
					"Too many properties; at most " + maxProperties + " allowed.");
		}
		if ( shutdown || !modeManager.isAvailable() ) {
			return reject(session, subscriptionId, src, SetupStatus.ServiceUnavailable,
					"Live datum not available.");
		}

		final long now = clock.millis();
		final LiveDatumSubscription sub = new LiveDatumSubscription(session, subscriptionId, src, props,
				intervalMs, now, now + (maxDurationSecs * 1000L));
		final String key = sub.getKey();
		SetupStatus rejectStatus = null;
		String rejectMessage = null;
		LiveDatumSubscription duplicate = null;
		synchronized ( registryLock ) {
			final UUID sessionId = session.getSessionId();
			duplicate = subscriptions.get(key);
			if ( shutdown ) {
				// check again, as serviceDidShutdown() sets the flag under this lock
				rejectStatus = SetupStatus.ServiceUnavailable;
				rejectMessage = "Live datum not available.";
			} else if ( duplicate != null ) {
				rejectStatus = SetupStatus.Unprocessable;
				rejectMessage = "Subscription ID already in use.";
			} else if ( subscriptions.size() >= maxSubscriptions ) {
				rejectStatus = SetupStatus.TooManyRequests;
				rejectMessage = "Too many live subscriptions.";
			} else if ( subscriptions.values().stream()
					.filter(s -> sessionId.equals(s.getSession().getSessionId()))
					.count() >= maxSubscriptionsPerSession ) {
				rejectStatus = SetupStatus.TooManyRequests;
				rejectMessage = "Too many live subscriptions for this session.";
			} else {
				subscriptions.put(key, sub);
				bySource.computeIfAbsent(src, k -> ConcurrentHashMap.newKeySet()).add(sub);
			}
		}
		if ( duplicate != null && rejectStatus == SetupStatus.Unprocessable ) {
			// a 422 is terminal for this ID, so close the existing subscription first
			synchronized ( duplicate ) {
				duplicate.close();
				reject(session, subscriptionId, src, rejectStatus, rejectMessage);
			}
			deregister(duplicate);
			return rejectStatus;
		}
		if ( rejectStatus != null ) {
			// publish outside of the registry lock
			return reject(session, subscriptionId, src, rejectStatus, rejectMessage);
		}
		log.info("Live datum subscription added: {}", sub);

		// send latest datum now rather than wait for the next poll
		final DatumService ds = service(datumService);
		if ( ds != null ) {
			try {
				NodeDatum latest = ds.unfiltered().latest(src, NodeDatum.class);
				if ( latest != null ) {
					// only a recent datum confirms the source is still producing datum
					final Instant ts = latest.getTimestamp();
					final boolean recent = (ts != null && now - ts.toEpochMilli() < (staleSecs * 1000L));
					offer(sub, latest, now, recent);
				}
			} catch ( RuntimeException e ) {
				log.warn("Error getting latest datum for live subscription {}: {}", sub, e.toString());
			}
		}

		scheduleSync();
		return SetupStatus.Ok;
	}

	/**
	 * Remove a live subscription.
	 *
	 * @param session
	 *        the session
	 * @param subscriptionId
	 *        the subscription ID
	 * @return {@literal true} if a live subscription was removed
	 */
	public boolean unsubscribe(SetupSession session, String subscriptionId) {
		if ( session == null || subscriptionId == null ) {
			return false;
		}
		LiveDatumSubscription sub = subscriptions
				.get(LiveDatumSubscription.key(session.getSessionId(), subscriptionId));
		if ( sub == null ) {
			return false;
		}
		return removeSubscription(sub);
	}

	/**
	 * Remove all live subscriptions for a session.
	 *
	 * <p>
	 * This should be called when a session's channel is closed.
	 * </p>
	 *
	 * @param sessionId
	 *        the ID of the session that closed
	 * @return the number of subscriptions removed
	 */
	public int sessionClosed(UUID sessionId) {
		if ( sessionId == null ) {
			return 0;
		}
		int count = 0;
		for ( LiveDatumSubscription sub : new ArrayList<>(subscriptions.values()) ) {
			if ( sessionId.equals(sub.getSession().getSessionId()) && removeSubscription(sub) ) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Get the number of active subscriptions.
	 *
	 * @return the count
	 */
	public int getSubscriptionCount() {
		return subscriptions.size();
	}

	@Override
	public void handleEvent(Event event) {
		if ( event == null || !DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED.equals(event.getTopic()) ) {
			return;
		}
		final Object o = event.getProperty(DatumEvents.DATUM_PROPERTY);
		if ( !(o instanceof NodeDatum) ) {
			return;
		}
		final NodeDatum datum = (NodeDatum) o;
		final String sourceId = datum.getSourceId();
		if ( sourceId == null ) {
			return;
		}
		final Set<LiveDatumSubscription> subs = bySource.get(sourceId);
		if ( subs == null || subs.isEmpty() ) {
			return;
		}
		final long now = clock.millis();
		for ( LiveDatumSubscription sub : subs ) {
			try {
				offer(sub, datum, now, true);
			} catch ( RuntimeException e ) {
				log.warn("Error publishing live datum to {}: {}", sub, e.toString());
			}
		}
	}

	/**
	 * Offer a datum to a subscription.
	 *
	 * @param sub
	 *        the subscription
	 * @param datum
	 *        the datum
	 * @param now
	 *        the current time
	 * @param confirm
	 *        {@literal true} to treat the datum as confirmation the source is
	 *        producing datum
	 * @return {@literal true} if a message was published
	 */
	private boolean offer(LiveDatumSubscription sub, NodeDatum datum, long now, boolean confirm) {
		final Instant ts = datum.getTimestamp();
		final long sampleTs = (ts != null ? ts.toEpochMilli() : now);
		synchronized ( sub ) {
			if ( sub.isClosed() ) {
				return false;
			}
			if ( confirm ) {
				sub.datumReceived(now);
			}
			if ( !sub.shouldSend(sampleTs) ) {
				return false;
			}
			if ( !publisher.publishDatum(sub, datum, sampleTs) ) {
				return false;
			}
			sub.sampleSent(sampleTs);
			return true;
		}
	}

	private SetupStatus reject(SetupSession session, String subscriptionId, String sourceId,
			SetupStatus status, String message) {
		log.debug("Live datum subscription {} on session {} rejected with {}: {}", subscriptionId,
				session.getSessionId(), status, message);
		publishStatus(session, subscriptionId, sourceId, status, message);
		return status;
	}

	private boolean removeSubscription(LiveDatumSubscription sub) {
		if ( !sub.close() ) {
			return false;
		}
		deregister(sub);
		return true;
	}

	/**
	 * Remove a closed subscription from the registry.
	 *
	 * @param sub
	 *        the subscription, which must already be closed
	 */
	private void deregister(LiveDatumSubscription sub) {
		boolean removed;
		synchronized ( registryLock ) {
			removed = subscriptions.remove(sub.getKey(), sub);
			Set<LiveDatumSubscription> subs = bySource.get(sub.getSourceId());
			if ( subs != null ) {
				subs.remove(sub);
				if ( subs.isEmpty() ) {
					bySource.remove(sub.getSourceId(), subs);
				}
			}
		}
		if ( removed ) {
			log.info("Live datum subscription removed: {}", sub);
			scheduleSync();
		}
	}

	private long parseInterval(String interval) {
		long result = minIntervalMs;
		if ( interval != null && !interval.trim().isEmpty() ) {
			result = Long.parseLong(interval.trim());
		}
		if ( result < minIntervalMs ) {
			result = minIntervalMs;
		} else if ( result > maxIntervalMs ) {
			result = maxIntervalMs;
		}
		return result;
	}

	private static List<String> parseProperties(String properties) {
		if ( properties == null || properties.trim().isEmpty() ) {
			return Collections.emptyList();
		}
		Set<String> result = new LinkedHashSet<>(8);
		for ( String p : properties.split(",") ) {
			String s = p.trim();
			if ( !s.isEmpty() ) {
				result.add(s);
			}
		}
		return new ArrayList<>(result);
	}

	/**
	 * Request a sync of the operational mode and housekeeping task, to run on
	 * the task scheduler.
	 *
	 * <p>
	 * Only one sync task is queued at a time. If no task scheduler is
	 * configured the sync is performed on the calling thread.
	 * </p>
	 */
	private void scheduleSync() {
		if ( shutdown ) {
			return;
		}
		if ( !syncPending.compareAndSet(false, true) ) {
			return;
		}
		final TaskScheduler scheduler = this.taskScheduler;
		final Runnable task = () -> {
			syncPending.set(false);
			syncTasks();
		};
		if ( scheduler == null ) {
			task.run();
			return;
		}
		try {
			scheduler.schedule(task, clock.instant());
		} catch ( RuntimeException e ) {
			syncPending.set(false);
			log.warn("Unable to schedule live datum sync: {}", e.toString());
		}
	}

	/**
	 * Bring the operational mode and housekeeping task in line with the current
	 * subscriptions.
	 */
	private void syncTasks() {
		if ( shutdown ) {
			return;
		}
		try {
			modeManager.sync();
		} catch ( RuntimeException e ) {
			log.warn("Error syncing live datum operational mode: {}", e.toString());
		}
		synchronized ( taskLock ) {
			if ( shutdown ) {
				return;
			}
			if ( subscriptions.isEmpty() ) {
				cancelHousekeeping();
			} else {
				ensureHousekeeping();
			}
		}
	}

	private void ensureHousekeeping() {
		final TaskScheduler scheduler = this.taskScheduler;
		if ( housekeepingFuture != null || scheduler == null || housekeepingSecs < 1 ) {
			return;
		}
		housekeepingFuture = scheduler.scheduleWithFixedDelay(this::housekeeping,
				clock.instant().plusSeconds(housekeepingSecs), Duration.ofSeconds(housekeepingSecs));
	}

	private void cancelHousekeeping() {
		if ( housekeepingFuture != null ) {
			housekeepingFuture.cancel(false);
			housekeepingFuture = null;
		}
	}

	/**
	 * Perform periodic housekeeping.
	 *
	 * <p>
	 * This removes subscriptions on closed channels, expires subscriptions past
	 * their maximum duration, rejects subscriptions whose source never produced
	 * a datum, flags stalled subscriptions, and syncs the operational mode
	 * (refreshing its expiration).
	 * </p>
	 */
	public void housekeeping() {
		if ( shutdown ) {
			return;
		}
		final long now = clock.millis();
		for ( LiveDatumSubscription sub : new ArrayList<>(subscriptions.values()) ) {
			try {
				housekeeping(sub, now);
			} catch ( RuntimeException e ) {
				log.warn("Error performing housekeeping on live subscription {}: {}", sub, e.toString());
			}
		}
		syncTasks();
	}

	private void housekeeping(LiveDatumSubscription sub, long now) {
		if ( !sub.getSession().getChannel().isActive() ) {
			removeSubscription(sub);
			return;
		}
		boolean remove = false;
		synchronized ( sub ) {
			if ( sub.isClosed() ) {
				return;
			}
			// close first so no datum follows a terminal status
			if ( now >= sub.getExpires() ) {
				remove = sub.close();
				publishStatus(sub, SetupStatus.Gone, "Maximum subscription duration reached.");
			} else if ( !sub.isConfirmed() ) {
				if ( now - sub.getCreated() >= (sourceGraceSecs * 1000L) ) {
					remove = sub.close();
					publishStatus(sub, SetupStatus.NotFound, "No datum available for source.");
				}
			} else {
				final long staleMs = Math.max(staleSecs * 1000L, 3 * sub.getIntervalMs());
				if ( now - sub.getLastDatumAt() >= staleMs && sub.markStalled() ) {
					publishStatus(sub, SetupStatus.ServiceUnavailable, "No recent data.");
				}
			}
		}
		if ( remove ) {
			deregister(sub);
		}
	}

	/**
	 * Get the operational mode manager.
	 *
	 * @return the manager, never {@literal null}
	 */
	public LiveDatumModeManager getModeManager() {
		return modeManager;
	}

	/**
	 * Get the task scheduler.
	 *
	 * @return the task scheduler
	 */
	public TaskScheduler getTaskScheduler() {
		return taskScheduler;
	}

	/**
	 * Set the task scheduler.
	 *
	 * <p>
	 * Operational mode changes and housekeeping are performed on this
	 * scheduler. It is also set on the mode manager.
	 * </p>
	 *
	 * @param taskScheduler
	 *        the task scheduler to set
	 */
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
		modeManager.setTaskScheduler(taskScheduler);
	}

	/**
	 * Get the clock.
	 *
	 * @return the clock
	 */
	public Clock getClock() {
		return clock;
	}

	/**
	 * Set the clock.
	 *
	 * <p>
	 * It is also set on the mode manager.
	 * </p>
	 *
	 * @param clock
	 *        the clock to set; if {@literal null} then the system UTC clock
	 *        will be used
	 */
	public void setClock(Clock clock) {
		this.clock = (clock != null ? clock : Clock.systemUTC());
		modeManager.setClock(this.clock);
	}

	/**
	 * Get the maximum subscription duration, in seconds.
	 *
	 * @return the maximum duration; defaults to
	 *         {@link #DEFAULT_MAX_DURATION_SECS}
	 */
	public int getMaxDurationSecs() {
		return maxDurationSecs;
	}

	/**
	 * Set the maximum subscription duration, in seconds.
	 *
	 * @param maxDurationSecs
	 *        the maximum duration to set
	 */
	public void setMaxDurationSecs(int maxDurationSecs) {
		this.maxDurationSecs = maxDurationSecs;
	}

	/**
	 * Get the maximum number of subscriptions, across all sessions.
	 *
	 * @return the maximum; defaults to {@link #DEFAULT_MAX_SUBSCRIPTIONS}
	 */
	public int getMaxSubscriptions() {
		return maxSubscriptions;
	}

	/**
	 * Set the maximum number of subscriptions, across all sessions.
	 *
	 * @param maxSubscriptions
	 *        the maximum to set
	 */
	public void setMaxSubscriptions(int maxSubscriptions) {
		this.maxSubscriptions = maxSubscriptions;
	}

	/**
	 * Get the maximum number of subscriptions per session.
	 *
	 * @return the maximum; defaults to
	 *         {@link #DEFAULT_MAX_SUBSCRIPTIONS_PER_SESSION}
	 */
	public int getMaxSubscriptionsPerSession() {
		return maxSubscriptionsPerSession;
	}

	/**
	 * Set the maximum number of subscriptions per session.
	 *
	 * @param maxSubscriptionsPerSession
	 *        the maximum to set
	 */
	public void setMaxSubscriptionsPerSession(int maxSubscriptionsPerSession) {
		this.maxSubscriptionsPerSession = maxSubscriptionsPerSession;
	}

	/**
	 * Get the minimum message interval, in milliseconds.
	 *
	 * @return the minimum interval; defaults to
	 *         {@link #DEFAULT_MIN_INTERVAL_MS}
	 */
	public long getMinIntervalMs() {
		return minIntervalMs;
	}

	/**
	 * Set the minimum message interval, in milliseconds.
	 *
	 * <p>
	 * Requested intervals are clamped to at least this value, which is also
	 * used when no interval is requested.
	 * </p>
	 *
	 * @param minIntervalMs
	 *        the minimum interval to set
	 */
	public void setMinIntervalMs(long minIntervalMs) {
		this.minIntervalMs = minIntervalMs;
	}

	/**
	 * Get the maximum message interval, in milliseconds.
	 *
	 * @return the maximum interval; defaults to
	 *         {@link #DEFAULT_MAX_INTERVAL_MS}
	 */
	public long getMaxIntervalMs() {
		return maxIntervalMs;
	}

	/**
	 * Set the maximum message interval, in milliseconds.
	 *
	 * @param maxIntervalMs
	 *        the maximum interval to set
	 */
	public void setMaxIntervalMs(long maxIntervalMs) {
		this.maxIntervalMs = maxIntervalMs;
	}

	/**
	 * Get the source grace period, in seconds.
	 *
	 * @return the grace period; defaults to {@link #DEFAULT_SOURCE_GRACE_SECS}
	 */
	public int getSourceGraceSecs() {
		return sourceGraceSecs;
	}

	/**
	 * Set the source grace period, in seconds.
	 *
	 * <p>
	 * A subscription whose source produces no datum within this time is closed
	 * with a {@link SetupStatus#NotFound} status.
	 * </p>
	 *
	 * @param sourceGraceSecs
	 *        the grace period to set
	 */
	public void setSourceGraceSecs(int sourceGraceSecs) {
		this.sourceGraceSecs = sourceGraceSecs;
	}

	/**
	 * Get the stale data threshold, in seconds.
	 *
	 * @return the threshold; defaults to {@link #DEFAULT_STALE_SECS}
	 */
	public int getStaleSecs() {
		return staleSecs;
	}

	/**
	 * Set the stale data threshold, in seconds.
	 *
	 * <p>
	 * When no datum has been received for a subscription's source for the
	 * larger of this time or three times the subscription interval, a single
	 * advisory {@link SetupStatus#ServiceUnavailable} status is published.
	 * </p>
	 *
	 * @param staleSecs
	 *        the threshold to set
	 */
	public void setStaleSecs(int staleSecs) {
		this.staleSecs = staleSecs;
	}

	/**
	 * Get the housekeeping frequency, in seconds.
	 *
	 * @return the frequency; defaults to {@link #DEFAULT_HOUSEKEEPING_SECS}
	 */
	public int getHousekeepingSecs() {
		return housekeepingSecs;
	}

	/**
	 * Set the housekeeping frequency, in seconds.
	 *
	 * @param housekeepingSecs
	 *        the frequency to set
	 */
	public void setHousekeepingSecs(int housekeepingSecs) {
		this.housekeepingSecs = housekeepingSecs;
	}

	/**
	 * Get the maximum number of properties a subscription may request.
	 *
	 * @return the maximum; defaults to {@link #DEFAULT_MAX_PROPERTIES}
	 */
	public int getMaxProperties() {
		return maxProperties;
	}

	/**
	 * Set the maximum number of properties a subscription may request.
	 *
	 * @param maxProperties
	 *        the maximum to set
	 */
	public void setMaxProperties(int maxProperties) {
		this.maxProperties = maxProperties;
	}

}
