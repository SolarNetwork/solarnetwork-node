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

import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.DateUtils.formatHoursMinutesSeconds;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.springframework.context.MessageSource;
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
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.StatCounter;

/**
 * Default implementation of {@link DatumQueue}.
 *
 * <p>
 * Datum added to the queue are processed in FIFO order.
 * </p>
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
 * @version 3.2
 * @since 1.89
 */
public class DefaultDatumQueue extends BaseIdentifiable
		implements DatumQueue, SettingSpecifierProvider, UncaughtExceptionHandler {

	/** The default value for the {@code startupDelayMs} property. */
	public static final long DEFAULT_STARTUP_DELAY_MS = 20_000;

	/** The default {@code statisticLogFrequency} property. */
	public static final int DEFAULT_STAT_LOG_FREQUENCY = 250;

	/**
	 * The default queue size, before blocking occurs.
	 *
	 * @since 3.2
	 */
	public static final int DEFAULT_QUEUE_SIZE = 50;

	/**
	 * The {@code queueMaxWaitMs} property default value.
	 *
	 * @since 3.2
	 */
	public static final long DEFAULT_QUEUE_MAX_WAIT_MS = 60_000L;

	private final BlockingQueue<QueuedDatum> datumQueue;
	private final List<ConsumerThread> consumers = new CopyOnWriteArrayList<>();
	private final StatCounter stats = new StatCounter("DatumQueue", "", log, DEFAULT_STAT_LOG_FREQUENCY,
			QueueStats.values());

	private final DatumDao nodeDatumDao;
	private final OptionalService<EventAdmin> eventAdmin;
	private final OptionalService<DatumQueueProcessObserver> processObserver;
	private long startupDelayMs = DEFAULT_STARTUP_DELAY_MS;
	private @Nullable OptionalFilterableService<DatumFilterService> datumFilterService;
	private @Nullable UncaughtExceptionHandler datumProcessorExceptionHandler;

	private long processorStartupDelayMs;
	private @Nullable ProcessorThread datumProcessor;
	private boolean discardDatumOnFilterException;
	private long queueMaxWaitMs = DEFAULT_QUEUE_MAX_WAIT_MS;

	/**
	 * Constructor.
	 *
	 * @param nodeDatumDao
	 *        the node datum DAO to use
	 * @param eventAdmin
	 *        the event admin
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
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
	 *         if any argument is {@code null}
	 * @since 2.1
	 */
	public DefaultDatumQueue(DatumDao nodeDatumDao, OptionalService<EventAdmin> eventAdmin,
			OptionalService<DatumQueueProcessObserver> processObserver) {
		this(nodeDatumDao, eventAdmin, processObserver, DEFAULT_QUEUE_SIZE);
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
	 * @param queueSize
	 *        the internal queue size to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 * @since 3.2
	 */
	public DefaultDatumQueue(DatumDao nodeDatumDao, OptionalService<EventAdmin> eventAdmin,
			OptionalService<DatumQueueProcessObserver> processObserver, int queueSize) {
		super();
		this.nodeDatumDao = requireNonNullArgument(nodeDatumDao, "nodeDatumDao");
		this.eventAdmin = requireNonNullArgument(eventAdmin, "eventAdmin");
		this.processObserver = requireNonNullArgument(processObserver, "processObserver");
		this.processorStartupDelayMs = -1;
		this.datumQueue = new ArrayBlockingQueue<>(queueSize);
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

		/**
		 * Duplicates.
		 *
		 * @deprecated no longer used
		 */
		@Deprecated(since = "3.2")
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

		/**
		 * Discarded from over capacity.
		 *
		 * @since 3.2
		 */
		Discarded("discarded"),

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

	private static final record QueuedDatum(NodeDatum datum, boolean persist) {

		@Override
		public String toString() {
			return "QueuedDatum{" + datum.getTimestamp() + "," + datum.getSourceId() + "," + persist
					+ "}";
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
				do {
					final QueuedDatum event;
					try {
						event = datumQueue.poll(60, TimeUnit.SECONDS);
						if ( event == null ) {
							continue;
						}
						log.trace("Datum taken: {}", event.toString());
					} catch ( InterruptedException e ) {
						continue;
					}
					final DatumQueueProcessObserver procObserver = service(processObserver);
					final long start = System.currentTimeMillis();
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
						log.error("Error processing datum {}; {}.", event.datum,
								(discardDatumOnFilterException ? "discarding" : "continuing anyway"), t);
						if ( discardDatumOnFilterException ) {
							uncaughtException(Thread.currentThread(), t);
							result = null;
						} else {
							result = event.datum;
						}
					}
					if ( result != null ) {
						if ( procObserver != null ) {
							try {
								procObserver.datumQueueWillProcess(DefaultDatumQueue.this, result,
										Stage.PostFilter, event.persist);
							} catch ( Throwable t ) {
								stats.incrementAndGet(QueueStats.Errors);
								log.error("Direct consumer {} error on PostFilter datum {}; ignoring.",
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

	/**
	 * Apply the {@code datumFilterService} transform, if available.
	 *
	 * @param event
	 *        the datum event to apply the transform to
	 * @return {@code event.datum} if the transform is not available or returns
	 *         the same instance; {@code null} if the transform returns
	 *         {@code null}; the transform result if it returns a
	 *         {@code NodeDatum} instance; a new datum with the result of the
	 *         transform service
	 */
	private @Nullable NodeDatum applyTransform(QueuedDatum event) {
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
		try {
			final boolean result = datumQueue.offer(new QueuedDatum(datum, persist), queueMaxWaitMs,
					TimeUnit.MILLISECONDS);
			if ( !result ) {
				stats.incrementAndGet(QueueStats.Discarded);
			}
			return result;
		} catch ( InterruptedException e ) {
			return false;
		}
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
			if ( processor != null && !processor.isAlive() ) {
				datumProcessor = null;
				startup();
			}
		}
		final UncaughtExceptionHandler datumProcessorExceptionHandler = this.datumProcessorExceptionHandler;
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
		result.add(new BasicTextFieldSettingSpecifier("transformServiceUid", null, false,
				"(&(objectClass=net.solarnetwork.service.DatumFilterService)(role=user))"));
		result.add(new BasicToggleSettingSpecifier("discardDatumOnFilterException", Boolean.FALSE));
		result.add(new BasicTextFieldSettingSpecifier("queueMaxWaitMs",
				String.valueOf(DEFAULT_QUEUE_MAX_WAIT_MS)));
		return result;
	}

	private @Nullable String getStatusMessage() {
		final MessageSource msgSource = getMessageSource();
		if ( msgSource == null ) {
			return null;
		}

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

		return msgSource.getMessage("status.msg", params, Locale.getDefault());
	}

	/**
	 * Get the processing startup delay, in milliseconds.
	 *
	 * @return the startup delay; defaults to {@link #DEFAULT_STARTUP_DELAY_MS}
	 */
	public final long getStartupDelayMs() {
		return startupDelayMs;
	}

	/**
	 * Set the processing startup delay, in milliseconds.
	 *
	 * @param startupDelayMs
	 *        the delay to set
	 */
	public final void setStartupDelayMs(long startupDelayMs) {
		this.startupDelayMs = startupDelayMs;
	}

	/**
	 * Get the queue delay, in milliseconds.
	 *
	 * @return the delay
	 * @deprecated always returns {@code 0}
	 */
	@Deprecated(since = "3.2")
	public final long getQueueDelayMs() {
		return 0L;
	}

	/**
	 * Set the queue delay, in milliseconds.
	 *
	 * @param queueDelayMs
	 *        the delay to set
	 * @deprecated no longer used
	 */
	@Deprecated(since = "3.2")
	public final void setQueueDelayMs(long queueDelayMs) {
		// no-op for backwards compatibility
	}

	/**
	 * Set the configured transform service.
	 *
	 * @return the transform service, or {@code null}
	 */
	public final @Nullable OptionalFilterableService<DatumFilterService> getDatumFilterService() {
		return datumFilterService;
	}

	/**
	 * Set the configured transform service.
	 *
	 * @param transformService
	 *        the transform service to set
	 */
	public final void setDatumFilterService(
			@Nullable OptionalFilterableService<DatumFilterService> transformService) {
		this.datumFilterService = transformService;
	}

	/**
	 * Get the transform service filter UID.
	 *
	 * @return the service UID
	 */
	public final @Nullable String getTransformServiceUid() {
		final OptionalFilterableService<DatumFilterService> datumFilterService = getDatumFilterService();
		return (datumFilterService != null ? datumFilterService.getPropertyValue(UID_PROPERTY) : null);
	}

	/**
	 * Set the transform service filter UID.
	 *
	 * @param uid
	 *        the service UID
	 */
	public final void setTransformServiceUid(@Nullable String uid) {
		final OptionalFilterableService<DatumFilterService> datumFilterService = getDatumFilterService();
		if ( datumFilterService == null ) {
			return;
		}
		datumFilterService.setPropertyFilter(UID_PROPERTY, uid);
	}

	/**
	 * Get the DAO to persist node datum with.
	 *
	 * @return the DAO
	 */
	public final DatumDao getNodeDatumDao() {
		return nodeDatumDao;
	}

	/**
	 * Get the statistics log frequency.
	 *
	 * @return the frequency
	 */
	public final int getStatisticLogFrequency() {
		return stats.getLogFrequency();
	}

	/**
	 * Set the statistics log frequency.
	 *
	 * @param logFrequency
	 *        the frequency to set
	 */
	public final void setStatisticLogFrequency(int logFrequency) {
		stats.setLogFrequency(logFrequency);
	}

	/**
	 * Get an exception handler for the datum processor.
	 *
	 * @return the exception handler, or {@code null}
	 */
	public final @Nullable UncaughtExceptionHandler getDatumProcessorExceptionHandler() {
		return datumProcessorExceptionHandler;
	}

	/**
	 * Set an exception handler for the datum processor.
	 *
	 * @param datumProcessorExceptionHandler
	 *        the handler to set
	 */
	public final void setDatumProcessorExceptionHandler(
			@Nullable UncaughtExceptionHandler datumProcessorExceptionHandler) {
		this.datumProcessorExceptionHandler = datumProcessorExceptionHandler;
	}

	/**
	 * Get the internal statistics.
	 *
	 * @return the stats
	 * @see QueueStats
	 */
	public final StatCounter getStats() {
		return stats;
	}

	/**
	 * Get the discard-datum-on-filter-exception mode.
	 *
	 * @return {@code true} to discard datum after a filter exception occurs, or
	 *         {@code false} to continue processing; defaults to {@code false}
	 * @since 3.1
	 */
	public final boolean isDiscardDatumOnFilterException() {
		return discardDatumOnFilterException;
	}

	/**
	 * Set the discard-datum-on-filter-exception mode.
	 *
	 * @param discardDatumOnFilterException
	 *        {@code true} to discard datum after a filter exception occurs, or
	 *        {@code false} to continue processing
	 * @since 3.1
	 */
	public final void setDiscardDatumOnFilterException(boolean discardDatumOnFilterException) {
		this.discardDatumOnFilterException = discardDatumOnFilterException;
	}

	/**
	 * The maximum length of time, in milliseconds, to wait for to add a datum
	 * to the internal queue when {@link #offer(NodeDatum)} is called.
	 *
	 * @return the queueMaxWaitMs the maximum wait time, in milliseconds;
	 *         defaults to {@link #DEFAULT_QUEUE_MAX_WAIT_MS}
	 * @since 3.2
	 */
	public final long getQueueMaxWaitMs() {
		return queueMaxWaitMs;
	}

	/**
	 * Set the maximum length of time, in milliseconds, to wait for to add a
	 * datum to the internal queue when {@link #offer(NodeDatum)} is called.
	 *
	 * @param queueMaxWaitMs
	 *        the the maximum wait time to set; if less than {@code 0} then
	 *        {@link #DEFAULT_QUEUE_MAX_WAIT_MS} will be set instead
	 * @since 3.2
	 */
	public final void setQueueMaxWaitMs(long queueMaxWaitMs) {
		this.queueMaxWaitMs = (queueMaxWaitMs < 0 ? DEFAULT_QUEUE_MAX_WAIT_MS : queueMaxWaitMs);
	}

}
