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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.domain.GeneralDatumSamplesOperations;
import net.solarnetwork.domain.MutableGeneralDatumSamplesOperations;
import net.solarnetwork.domain.datum.GeneralDatumSamplesContainer;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.DatumQueue;
import net.solarnetwork.node.GeneralDatumSamplesTransformService;
import net.solarnetwork.node.dao.DatumDao;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.GeneralDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.support.BaseIdentifiable;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.OptionalService.OptionalFilterableService;

/**
 * Default implementation of {@link DatumQueue} for {@link GeneralDatum}.
 * 
 * @author matt
 * @version 1.0
 * @since 1.89
 */
public class DefaultDatumQueue extends BaseIdentifiable
		implements DatumQueue<GeneralDatum>, EventHandler, SettingSpecifierProvider {

	/** The default value for the {@code queueDelayMs} property. */
	public static final long DEFAULT_QUEUE_DELAY_MS = 200;

	// a queue of datum events ordered by datum creation date using a configurable delay
	// so that concurrent producer events can be processed in datum creation time order
	private final BlockingQueue<DelayedDatum> datumQueue = new DelayQueue<>();
	private final Set<Consumer<GeneralDatum>> consumers = new CopyOnWriteArraySet<>();

	private final Executor executor;
	private long queueDelayMs = DEFAULT_QUEUE_DELAY_MS;
	private OptionalFilterableService<GeneralDatumSamplesTransformService> transformService;
	private OptionalService<DatumDao<GeneralDatum>> datumDao;

	private ProcessorThread eventProcessor;

	/**
	 * Constructor.
	 * 
	 * @param executor
	 *        the executor to use
	 */
	public DefaultDatumQueue(Executor executor) {
		super();
		this.executor = executor;
	}

	/**
	 * Startup once configured.
	 */
	public synchronized void startup() {
		if ( eventProcessor != null ) {
			eventProcessor.processing = false;
			eventProcessor.interrupt();
		}
		eventProcessor = new ProcessorThread();
		eventProcessor.start();
	}

	/**
	 * Shutdown after no longer needed.
	 */
	public synchronized void shutdown() {
		if ( eventProcessor != null ) {
			eventProcessor.processing = false;
			eventProcessor.interrupt();
			eventProcessor = null;
		}
	}

	private static final class DelayedDatum implements Delayed, GeneralDatum {

		private final GeneralDatum datum;
		private final long ts;

		private DelayedDatum(GeneralDatum datum, long delayMs) {
			super();
			this.datum = datum;
			Date date = datum.getCreated();
			this.ts = (date != null ? date.getTime() : System.currentTimeMillis()) + delayMs;
		}

		@Override
		public int compareTo(Delayed o) {
			return Long.compare(ts, ((DelayedDatum) o).ts);
		}

		@Override
		public long getDelay(TimeUnit unit) {
			long ms = ts - System.currentTimeMillis();
			return unit.convert(ms, TimeUnit.MILLISECONDS);
		}

		@Override
		public GeneralDatumSamplesOperations asSampleOperations() {
			return datum.asSampleOperations();
		}

		@Override
		public MutableGeneralDatumSamplesOperations asMutableSampleOperations() {
			return datum.asMutableSampleOperations();
		}

		@Override
		public Instant getTimestamp() {
			return datum.getTimestamp();
		}

		@Override
		public Date getCreated() {
			return datum.getCreated();
		}

		@Override
		public String getSourceId() {
			return datum.getSourceId();
		}

		@Override
		public Date getUploaded() {
			return datum.getUploaded();
		}

		@Override
		public Map<String, ?> getSampleData() {
			return datum.getSampleData();
		}

		@Override
		public Map<String, ?> asSimpleMap() {
			return datum.asSimpleMap();
		}

	}

	private static final class ConsumerTask implements Runnable {

		private final Consumer<GeneralDatum> consumer;
		private final GeneralDatum datum;

		private ConsumerTask(Consumer<GeneralDatum> consumer, GeneralDatum datum) {
			super();
			this.consumer = consumer;
			this.datum = datum;
		}

		@Override
		public void run() {
			consumer.accept(datum);
		}

	}

	private final class ProcessorThread extends Thread {

		private boolean processing;

		private ProcessorThread() {
			super("DatumQueue Processor");
			setDaemon(true);
			processing = true;
		}

		@Override
		public void run() {
			log.info("Starting DatumQueue processor " + getName());
			try {
				DelayedDatum event = null;
				do {
					try {
						event = datumQueue.poll(60, TimeUnit.SECONDS);
						if ( event != null ) {
							GeneralDatum result = applyTransform(event);
							if ( result != null ) {
								final DatumDao<GeneralDatum> dao = OptionalService.service(datumDao);
								if ( dao != null ) {
									dao.storeDatum(result);
								}
								for ( Consumer<GeneralDatum> consumer : consumers ) {
									executor.execute(new ConsumerTask(consumer, result));
								}
							}
						}
					} catch ( InterruptedException e ) {
						// keep going
					}
				} while ( processing );
			} finally {
				log.info("Finished DatumQueue processor " + getName());
			}
		}

	}

	private GeneralDatum applyTransform(DelayedDatum event) {
		if ( !(event.datum instanceof GeneralDatumSamplesContainer) ) {
			return event.datum;
		}
		GeneralDatumSamplesTransformService xform = OptionalService.service(transformService);
		if ( xform == null ) {
			return event.datum;
		}
		GeneralDatumSamples in = ((GeneralDatumSamplesContainer) event.datum).getSamples();
		GeneralDatumSamples out = xform.transformSamples(event.datum, in, new HashMap<>(4));
		if ( out == null ) {
			return null;
		}
		if ( out == in ) {
			return event.datum;
		}
		return (GeneralDatum) ((GeneralDatumSamplesContainer) event.datum).copyWithSamples(out);
	}

	@Override
	public boolean offer(GeneralDatum datum) {
		if ( datum == null ) {
			return false;
		}
		return datumQueue.offer(new DelayedDatum(datum, queueDelayMs));
	}

	@Override
	public void addConsumer(Consumer<GeneralDatum> consumer) {
		consumers.add(consumer);
	}

	@Override
	public void removeConsumer(Consumer<GeneralDatum> consumer) {
		consumers.remove(consumer);
	}

	@Override
	public void handleEvent(Event event) {
		final String topic = event.getTopic();
		if ( DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED.equals(topic) ) {
			GeneralDatum datum = datumForEvent(event);
			if ( datum == null ) {
				return;
			}
			offer(datum);
		}
	}

	private static GeneralDatum datumForEvent(Event event) {
		if ( event == null ) {
			return null;
		}
		Object o = event.getProperty(Datum.DATUM_PROPERTY);
		return (o instanceof GeneralDatum ? (GeneralDatum) o : null);
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.runtime.dq";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(4);
		result.add(new BasicTextFieldSettingSpecifier("queueDelayMs",
				String.valueOf(DEFAULT_QUEUE_DELAY_MS)));
		result.add(new BasicTextFieldSettingSpecifier("transformServiceUid", null));
		return result;
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
	public OptionalFilterableService<GeneralDatumSamplesTransformService> getTransformService() {
		return transformService;
	}

	/**
	 * Set the configured transform service.
	 * 
	 * @param transformService
	 *        the transform service to set
	 */
	public void setTransformService(
			OptionalFilterableService<GeneralDatumSamplesTransformService> transformService) {
		this.transformService = transformService;
	}

	/**
	 * Get the transform service filter UID.
	 * 
	 * @return the service UID
	 */
	public String getTransformServiceUid() {
		return transformService.getPropertyValue(UID_PROPERTY);
	}

	/**
	 * Set the transform service filter UID.
	 * 
	 * @param uid
	 *        the service UID
	 */
	public void setTransformServiceUid(String uid) {
		transformService.setPropertyFilter(UID_PROPERTY, uid);
	}

	/**
	 * Get the DAO to persist datum with.
	 * 
	 * @return the DAO
	 */
	public OptionalService<DatumDao<GeneralDatum>> getDatumDao() {
		return datumDao;
	}

	/**
	 * Set the DAO to persist datum with.
	 * 
	 * @param datumDao
	 *        the DAO to set
	 */
	public void setDatumDao(OptionalService<DatumDao<GeneralDatum>> datumDao) {
		this.datumDao = datumDao;
	}

}
