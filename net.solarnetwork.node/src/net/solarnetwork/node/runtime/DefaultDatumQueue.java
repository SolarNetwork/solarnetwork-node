/* ==================================================================
 * DefaultDatumQueue.java - 21/08/2021 3:59:35 PM
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

package net.solarnetwork.node.runtime;

import static java.util.stream.Collectors.joining;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.DateUtils.formatHoursMinutesSeconds;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.lang.Thread.UncaughtExceptionHandler;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.node.dao.DatumDao;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.node.service.DatumQueueProcessObserver;
import net.solarnetwork.node.service.DatumQueueProcessObserver.Stage;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.service.DatumFilterService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.OptionalService.OptionalFilterableService;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.StatCounter;

/**
 * Default implementation of {@link DatumQueue}.
 *
 * <p>
 * Datum passed to {@link #offer(NodeDatum)} will be persisted via one of the
 * configured {@link DatumDao} services, while Datum received via
 * {@link #offer(NodeDatum, boolean)} with {@code persist} set to
 * {@literal false} will not be persisted. All datum will then be passed to all
 * registered consumers.
 * </p>
 *
 * <p>
 * The {@code processObserver} passed to the constructor will receive datum
 * before and after filters have been applied, sequentially in queue order
 * directly on the queue processing thread.
 * </p>
 *
 * <p>
 * Each registered {@link Consumer} will receive datum sequentially in queue
 * order via a single thread.
 * </p>
 *
 * @author matt
 * @version 3.0
 * @since 1.89
 */
public class DefaultDatumQueue extends BaseIdentifiable
		implements DatumQueue, SettingSpecifierProvider, UncaughtExceptionHandler {

	/** The default value for the {@code queueDelayMs} property. */
	public static final long DEFAULT_QUEUE_DELAY_MS = 200;

	/** The default value for the {@code startupDelayMs} property. */
	public static final long DEFAULT_STARTUP_DELAY_MS = 20_000;

	/** The default {@code statisticLogFrequency} property. */
	public static final int DEFAULT_STAT_LOG_FREQUENCY = 250;

	// a queue of datum events ordered by datum creation date using a configurable delay
	// so that concurrent producer events can be processed in datum creation time order
	private final BlockingQueue<DelayedDatum> datumQueue = new DelayQueue<>();
	private final List<ConsumerThread> consumers = new CopyOnWriteArrayList<>();
	private final StatCounter stats = new StatCounter("DatumQueue", "", log, DEFAULT_STAT_LOG_FREQUENCY,
			QueueStats.values());

	private final DatumDao nodeDatumDao;
	private final OptionalService<EventAdmin> eventAdmin;
	private final OptionalService<DatumQueueProcessObserver> processObserver;
	private long startupDelayMs = DEFAULT_STARTUP_DELAY_MS;
	private long queueDelayMs = DEFAULT_QUEUE_DELAY_MS;
	private OptionalFilterableService<DatumFilterService> datumFilterService;
	private UncaughtExceptionHandler datumProcessorExceptionHandler;

	private long processorStartupDelayMs;
	private ProcessorThread datumProcessor;

	/**
	 * Constructor.
	 *
	 * @param nodeDatumDao
	 *        the node datum DAO to use
	 * @param eventAdmin
	 *        the event admin
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DefaultDatumQueue(DatumDao nodeDatumDao, OptionalService<EventAdmin> eventAdmin) {
		this(nodeDatumDao, eventAdmin, new StaticOptionalService<>(null));
	}

	/**
	 * Constructor.
	 *
	 * @param nodeDatumDao
	 *        the node datum DAO to use
	 * @param eventAdmin
	 *        the event admin
	 * @param processObserver
	 *        the direct consumer, which is invoked directly on the queue
	 *        processor thread
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 * @since 2.1
	 */
	public DefaultDatumQueue(DatumDao nodeDatumDao, OptionalService<EventAdmin> eventAdmin,
			OptionalService<DatumQueueProcessObserver> processObserver) {
		super();
		this.nodeDatumDao = requireNonNullArgument(nodeDatumDao, "nodeDatumDao");
		this.eventAdmin = requireNonNullArgument(eventAdmin, "eventAdmin");
		this.processObserver = requireNonNullArgument(processObserver, "processObserver");
		this.processorStartupDelayMs = -1;
	}

	/**
	 * Startup once configured.
	 */
	public synchronized void startup() {
		if ( datumProcessor != null ) {
			datumProcessor.processing = false;
			datumProcessor.interrupt();
		}
		if ( processorStartupDelayMs < 0 ) {
			processorStartupDelayMs = getStartupDelayMs();
		}
		datumProcessor = new ProcessorThread();
		datumProcessor.setUncaughtExceptionHandler(this);
		datumProcessor.start();

		// restart any registered consumers
		List<ConsumerThread> newConsumers = new ArrayList<>();
		for ( ConsumerThread t : consumers ) {
			t.shutdown(); // should already be done; just a precaution
			ConsumerThread newThread = new ConsumerThread(t.consumer);
			newThread.start();
			newConsumers.add(newThread);
		}
		consumers.clear();
		consumers.addAll(newConsumers);
	}

	/**
	 * Shutdown after no longer needed.
	 */
	public synchronized void shutdown() {
		if ( datumProcessor != null ) {
			datumProcessor.processing = false;
			datumProcessor.interrupt();
			datumProcessor = null;
		}
		for ( ConsumerThread t : consumers ) {
			t.shutdown();
		}
	}

	/**
	 * Queue statistics.
	 */
	public static enum QueueStats implements StatCounter.Stat {

		/** Added datum. */
		Added("added datum"),

		/** Captured. */
		Captured("captured datum"),

		/** Processed. */
		Processed("processed"),

		/** Duplicates. */
		Duplicates("duplicates"),

		/** Filtered. */
		Filtered("filtered"),

		/** Persisted. */
		Persisted("persisted"),

		/** Errors. */
		Errors("errors"),

		/** Milliseconds spent processing all input datum. */
		ProcessingTimeTotal("processing ms"),

		/** Milliseconds spent persisting datum. */
		PersistingTimeTotal("persisting ms"),

		;

		private String description;

		private QueueStats(String description) {
			this.description = description;
		}

		@Override
		public int getIndex() {
			return ordinal();
		}

		@Override
		public String getDescription() {
			return description;
		}

	}

	private static final class DelayedDatum implements Delayed {

		private final NodeDatum datum;
		private final long ts;
		private final boolean persist;

		private DelayedDatum(NodeDatum datum, long delayMs, boolean persist) {
			super();
			this.datum = datum;
			Instant date = datum.getTimestamp();
			// we really don't expect date to be null here, but just to be pragmatic we test;
			// future dates are forced to the current time, so they are not delayed
			long now = System.currentTimeMillis();
			this.ts = (date != null && date.toEpochMilli() <= now ? date.toEpochMilli() : now) + delayMs;
			this.persist = persist;
		}

		@Override
		public int compareTo(Delayed o) {
			DelayedDatum other = (DelayedDatum) o;
			int result = Long.compare(ts, other.ts);
			if ( result == 0 ) {
				// fall back to sort by source ID when ts are equal
				result = datum.getSourceId().compareTo(other.datum.getSourceId());
				if ( result == 0 ) {
					result = Boolean.compare(other.persist, persist);
				}
			}
			return result;
		}

		@Override
		public long getDelay(TimeUnit unit) {
			long ms = ts - System.currentTimeMillis();
			return unit.convert(ms, TimeUnit.MILLISECONDS);
		}

		@Override
		public String toString() {
			return "DelayedDatum{" + ts + "," + datum.getSourceId() + "," + persist + "}";
		}

	}

	private final class ConsumerThread extends Thread implements Consumer<NodeDatum> {

		private final Consumer<NodeDatum> consumer;
		private final BlockingQueue<NodeDatum> queue;

		private boolean processing;

		private ConsumerThread(Consumer<NodeDatum> consumer) {
			super("DatumQueue Consumer " + consumer);
			setDaemon(true);
			this.consumer = consumer;
			this.queue = new LinkedBlockingDeque<>();
			this.processing = true;
		}

		@Override
		public void accept(NodeDatum datum) {
			this.queue.add(datum);
		}

		private void shutdown() {
			processing = false;
			this.interrupt();
		}

		@Override
		public void run() {
			do {
				NodeDatum datum = null;
				try {
					datum = queue.take();
					consumer.accept(datum);
				} catch ( InterruptedException e ) {
					// ignore
				} catch ( Throwable t ) {
					stats.incrementAndGet(QueueStats.Errors);
					log.error("Consumer error on datum {}; discarding.", datum, t);
				}
			} while ( processing );
		}

	}

	private final class ProcessorThread extends Thread {

		private boolean processing;

		private ProcessorThread() {
			super("DatumQueue Processor");
			setDaemon(true);
			this.processing = true;
		}

		@Override
		public void run() {
			try {
				// the first time we start, include the processor startup delay
				if ( processorStartupDelayMs > 0 ) {
					try {
						log.info("Waiting {}s before starting DatumQueue processor",
								processorStartupDelayMs / 1000);
						Thread.sleep(processorStartupDelayMs);
					} catch ( Exception e ) {
						// ignore
					}
					processorStartupDelayMs = -1;
				}
				log.info("Starting DatumQueue processor {}", Integer.toHexString(hashCode()));

				/*-
				 We are assuming there will be many pairs of identical datum received by the queue,
				 because most DatumDataSource services, when polled for a Datum, will both return
				 the Datum which is passed to offer() and ALSO emit a DATUM_CAPTURED event which
				 gets passed to handleEvent(). Datum passed to offer() have persist == true and
				 Datum passed to handleEvent() have persist == false; hence we are expecting pairs
				 of events where one should be persisted and the other not (just passed to consumers).
				 Since all events are passed to consumers, we can discard (persist == false) events
				 from a matching pair event with (persist == true).
				
				 The approach taken relies on the ordering of our queue, which is ordered by
				 date, source ID, persist. Potential pairs will differ only by the persist flag,
				 and will have identical datum objects. The algorithm thus does:
				
				 1. Poll for the next available event.
				 2. Peek/take all next available events with a matching date.
				 3. Sort the collected events (by date, source, persist)
				 4. For each collected event where persist == false, search previous collected
				    events for an identical datum with persist == true. If found, discard.
				
				 The approach holds up in highly-concurrent environments where even a single
				 source ID has events at the same date (i.e. >1 event within 1ms on a JVM with
				 millisecond precision dates).
				 */

				DelayedDatum event = null;
				List<DelayedDatum> events = new ArrayList<>(16);
				do {
					try {
						event = datumQueue.poll(60, TimeUnit.SECONDS);
						if ( event == null ) {
							continue;
						}
						// pull out all events with same date so we can find duplicates
						final long ts = event.ts;
						events.add(event);
						while ( true ) {
							event = datumQueue.peek();
							if ( event == null || event.ts != ts ) {
								break;
							}
							events.add(datumQueue.take());
						}
						if ( events.size() > 1 ) {
							Collections.sort(events);
							if ( log.isTraceEnabled() ) {
								log.trace("Datum taken: [\n  {}\n]",
										events.stream().map(Object::toString).collect(joining(",\n  ")));
							}
						}
					} catch ( InterruptedException e ) {
						// keep going
					}
					final DatumQueueProcessObserver procObserver = service(processObserver);
					final long start = System.currentTimeMillis();
					DelayedDatum p;
					EVENT: for ( int i = 0, len = events.size(); i < len; i++ ) {
						event = events.get(i);
						if ( !event.persist ) {
							for ( int j = i - 1; j >= 0; j-- ) {
								p = events.get(j);
								if ( p.datum == event.datum && p.persist ) {
									// optimization to skip duplicate of persisted+unpersisted pair;
									// this can happen when DatumDataSource services are polled for datum
									// which is then received via both offer() and handleEvent(DATUM_CAPTURED)
									stats.incrementAndGet(QueueStats.Duplicates);
									continue EVENT;
								} else if ( !p.datum.getSourceId().equals(event.datum.getSourceId()) ) {
									// as events sorted by time,source,persist then we can stop looking now
									break;
								}
							}
						}
						stats.incrementAndGet(QueueStats.Processed);
						if ( procObserver != null ) {
							try {
								procObserver.datumQueueWillProcess(DefaultDatumQueue.this, event.datum,
										Stage.PreFilter, event.persist);
							} catch ( Throwable t ) {
								stats.incrementAndGet(QueueStats.Errors);
								log.error("Direct consumer {} error on PreFilter datum {}; ignoring.",
										procObserver, event.datum, t);
							}
						}
						postEvent(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED, event.datum);
						NodeDatum result;
						try {
							result = applyTransform(event);
						} catch ( Throwable t ) {
							stats.incrementAndGet(QueueStats.Errors);
							log.error("Error processing datum {}; discarding.", event.datum, t);
							uncaughtException(Thread.currentThread(), t);
							result = null;
						}
						if ( result != null ) {
							if ( procObserver != null ) {
								try {
									procObserver.datumQueueWillProcess(DefaultDatumQueue.this, result,
											Stage.PostFilter, event.persist);
								} catch ( Throwable t ) {
									stats.incrementAndGet(QueueStats.Errors);
									log.error(
											"Direct consumer {} error on PostFilter datum {}; ignoring.",
											procObserver, result, t);
								}
							}
							postEvent(DatumQueue.EVENT_TOPIC_DATUM_ACQUIRED, result);
							if ( event.persist ) {
								try {
									persistDatum(result);
								} catch ( Throwable t ) {
									stats.incrementAndGet(QueueStats.Errors);
									log.error("Error persisting datum {}; discarding.", event.datum, t);
									uncaughtException(Thread.currentThread(), t);
									result = null;
								}
							}
							if ( result != null ) {
								for ( Consumer<NodeDatum> consumer : consumers ) {
									consumer.accept(result);
								}
							}
						}
					}
					events.clear();
					stats.addAndGet(QueueStats.ProcessingTimeTotal, System.currentTimeMillis() - start,
							true);
				} while ( processing );
			} finally {
				log.info("Finished DatumQueue processor {}", Integer.toHexString(hashCode()));
			}
		}

	}

	private void postEvent(String topic, NodeDatum datum) {
		final EventAdmin service = service(eventAdmin);
		if ( service != null ) {
			Event event = DatumEvents.datumEvent(topic, datum);
			if ( event != null ) {
				service.postEvent(event);
			}
		}
	}

	private NodeDatum applyTransform(DelayedDatum event) {
		DatumFilterService xform = service(datumFilterService);
		if ( xform == null ) {
			return event.datum;
		}
		DatumSamplesOperations in = event.datum.asSampleOperations();
		DatumSamplesOperations out = xform.filter(event.datum, in, new HashMap<>(4));
		if ( out == null ) {
			stats.incrementAndGet(QueueStats.Filtered);
			return null;
		}
		if ( out == in ) {
			return event.datum;
		} else if ( out instanceof NodeDatum ) {
			return (NodeDatum) out;
		}
		return event.datum.copyWithSamples(out);
	}

	private void persistDatum(NodeDatum result) {
		final long start = System.currentTimeMillis();
		final DatumDao dao = getNodeDatumDao();
		dao.storeDatum(result);
		stats.incrementAndGet(QueueStats.Persisted);
		stats.addAndGet(QueueStats.PersistingTimeTotal, System.currentTimeMillis() - start, true);
	}

	@Override
	public boolean offer(NodeDatum datum) {
		return offer(datum, true);
	}

	@Override
	public boolean offer(NodeDatum datum, boolean persist) {
		if ( datum == null || datum.getSourceId() == null ) {
			return false;
		}
		if ( persist ) {
			stats.incrementAndGet(QueueStats.Added);
		} else {
			stats.incrementAndGet(QueueStats.Captured);
		}
		return datumQueue.offer(new DelayedDatum(datum, queueDelayMs, persist));
	}

	@Override
	public synchronized void addConsumer(Consumer<NodeDatum> consumer) {
		for ( ConsumerThread t : consumers ) {
			if ( t.consumer == consumer ) {
				return;
			}
		}
		ConsumerThread t = new ConsumerThread(consumer);
		consumers.add(t);
		if ( datumProcessor != null ) {
			t.start();
		}
	}

	@Override
	public synchronized void removeConsumer(Consumer<NodeDatum> consumer) {
		ConsumerThread threadToRemove = null;
		for ( ConsumerThread t : consumers ) {
			if ( t.consumer == consumer ) {
				threadToRemove = t;
				break;
			}
		}
		if ( threadToRemove != null ) {
			consumers.remove(threadToRemove);
			threadToRemove.shutdown();
		}
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		synchronized ( this ) {
			final Thread processor = this.datumProcessor;
			if ( !processor.isAlive() ) {
				datumProcessor = null;
				startup();
			}
		}
		if ( datumProcessorExceptionHandler != null ) {
			datumProcessorExceptionHandler.uncaughtException(t, e);
		}
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.runtime.dq";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(4);
		result.add(new BasicTitleSettingSpecifier("status", getStatusMessage(), true, true));
		result.add(new BasicTextFieldSettingSpecifier("queueDelayMs",
				String.valueOf(DEFAULT_QUEUE_DELAY_MS)));
		result.add(new BasicTextFieldSettingSpecifier("transformServiceUid", null, false,
				"(&(objectClass=net.solarnetwork.service.DatumFilterService)(role=user))"));
		return result;
	}

	private String getStatusMessage() {
		final int len = QueueStats.values().length;
		Object[] params = new Object[len + 2];
		for ( int i = 0; i < len; i++ ) {
			params[i] = stats.get(QueueStats.values()[i]);
		}

		// convert processing times to friendly strings and add averages

		long processCount = (long) params[QueueStats.Processed.ordinal()];
		long totalTime = (Long) params[QueueStats.ProcessingTimeTotal.ordinal()];
		params[QueueStats.ProcessingTimeTotal.ordinal()] = formatHoursMinutesSeconds(totalTime);
		params[params.length - 2] = (processCount > 0 ? String.format("%dms", totalTime / processCount)
				: "-");

		long persistCount = (long) params[QueueStats.Persisted.ordinal()];
		long persistTime = (Long) params[QueueStats.PersistingTimeTotal.ordinal()];
		params[QueueStats.PersistingTimeTotal.ordinal()] = formatHoursMinutesSeconds(persistTime);
		params[params.length - 1] = (persistCount > 0 ? String.format("%dms", persistTime / persistCount)
				: "-");

		return getMessageSource().getMessage("status.msg", params, Locale.getDefault());
	}

	/**
	 * Get the processing startup delay, in milliseconds.
	 *
	 * @return the startup delay; defaults to {@link #DEFAULT_STARTUP_DELAY_MS}
	 */
	public long getStartupDelayMs() {
		return startupDelayMs;
	}

	/**
	 * Set the processing startup delay, in milliseconds.
	 *
	 * @param startupDelayMs
	 *        the delay to set
	 */
	public void setStartupDelayMs(long startupDelayMs) {
		this.startupDelayMs = startupDelayMs;
	}

	/**
	 * Get the queue delay, in milliseconds.
	 *
	 * @return the delay; defaults to {@link #DEFAULT_QUEUE_DELAY_MS}
	 */
	public long getQueueDelayMs() {
		return queueDelayMs;
	}

	/**
	 * Set the queue delay, in milliseconds.
	 *
	 * @param queueDelayMs
	 *        the delay to set; setting to anything less than {@code 1}
	 *        essentially disables the delay
	 */
	public void setQueueDelayMs(long queueDelayMs) {
		this.queueDelayMs = queueDelayMs;
	}

	/**
	 * Set the configured transform service.
	 *
	 * @return the transform service, or {@literal null}
	 */
	public OptionalFilterableService<DatumFilterService> getDatumFilterService() {
		return datumFilterService;
	}

	/**
	 * Set the configured transform service.
	 *
	 * @param transformService
	 *        the transform service to set
	 */
	public void setDatumFilterService(OptionalFilterableService<DatumFilterService> transformService) {
		this.datumFilterService = transformService;
	}

	/**
	 * Get the transform service filter UID.
	 *
	 * @return the service UID
	 */
	public String getTransformServiceUid() {
		return datumFilterService.getPropertyValue(UID_PROPERTY);
	}

	/**
	 * Set the transform service filter UID.
	 *
	 * @param uid
	 *        the service UID
	 */
	public void setTransformServiceUid(String uid) {
		datumFilterService.setPropertyFilter(UID_PROPERTY, uid);
	}

	/**
	 * Get the DAO to persist node datum with.
	 *
	 * @return the DAO
	 */
	public DatumDao getNodeDatumDao() {
		return nodeDatumDao;
	}

	/**
	 * Get the statistics log frequency.
	 *
	 * @return the frequency
	 */
	public int getStatisticLogFrequency() {
		return stats.getLogFrequency();
	}

	/**
	 * Set the statistics log frequency.
	 *
	 * @param logFrequency
	 *        the frequency to set
	 */
	public void setStatisticLogFrequency(int logFrequency) {
		stats.setLogFrequency(logFrequency);
	}

	/**
	 * Get an exception handler for the datum processor.
	 *
	 * @return the exception handler, or {@literal null}
	 */
	public UncaughtExceptionHandler getDatumProcessorExceptionHandler() {
		return datumProcessorExceptionHandler;
	}

	/**
	 * Set an exception handler for the datum processor.
	 *
	 * @param datumProcessorExceptionHandler
	 *        the handler to set
	 */
	public void setDatumProcessorExceptionHandler(
			UncaughtExceptionHandler datumProcessorExceptionHandler) {
		this.datumProcessorExceptionHandler = datumProcessorExceptionHandler;
	}

	/**
	 * Get the internal statistics.
	 *
	 * @return the stats
	 * @see QueueStats
	 */
	public StatCounter getStats() {
		return stats;
	}

}
