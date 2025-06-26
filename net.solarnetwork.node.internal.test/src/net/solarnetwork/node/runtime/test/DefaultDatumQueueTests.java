/* ==================================================================
 * DefaultDatumQueueTests.java - 24/08/2021 6:08:27 AM
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

package net.solarnetwork.node.runtime.test;

import static java.lang.String.format;
import static java.lang.Thread.sleep;
import static java.util.Collections.emptyMap;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.same;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.fail;
import java.lang.Thread.UncaughtExceptionHandler;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesContainer;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.dao.DatumDao;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.runtime.DefaultDatumQueue;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.node.service.DatumQueueProcessObserver;
import net.solarnetwork.node.service.DatumQueueProcessObserver.Stage;
import net.solarnetwork.node.service.support.BaseDatumFilterSupport;
import net.solarnetwork.service.DatumFilterService;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link DefaultDatumQueue}.
 *
 * @author matt
 * @version 1.3
 */
public class DefaultDatumQueueTests implements UncaughtExceptionHandler {

	private static final String TEST_SOURCE_ID = "test.source";

	private DatumDao datumDao;
	private EventAdmin eventAdmin;
	private Consumer<NodeDatum> consumer;
	private DatumQueueProcessObserver directConsumer;
	private DatumFilterService filter;
	private DefaultDatumQueue queue;
	private Throwable datumProcessortException;

	@SuppressWarnings("unchecked")
	@Before
	public void setup() {
		datumDao = EasyMock.createMock(DatumDao.class);
		eventAdmin = EasyMock.createMock(EventAdmin.class);
		consumer = EasyMock.createMock(Consumer.class);
		directConsumer = EasyMock.createMock(DatumQueueProcessObserver.class);
		filter = EasyMock.createMock(DatumFilterService.class);
		queue = new DefaultDatumQueue(datumDao, new StaticOptionalService<>(eventAdmin),
				new StaticOptionalService<>(directConsumer));
		queue.setStartupDelayMs(0);
		queue.setDatumProcessorExceptionHandler(this);
		queue.addConsumer(consumer);
		queue.startup();
	}

	@After
	public void teardown() {
		queue.shutdown();
		if ( datumProcessortException != null ) {
			datumProcessortException.printStackTrace();
			fail("Datum processor threw exception: " + datumProcessortException);
		}
		EasyMock.verify(datumDao, consumer, eventAdmin, directConsumer, filter);
	}

	private void replayAll() {
		EasyMock.replay(datumDao, consumer, eventAdmin, directConsumer, filter);
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		datumProcessortException = e;
	}

	private void assertCapturedAcquiredEvents(List<Event> events, int index, NodeDatum expected) {
		assertCapturedAcquiredEvents(events, index, expected, expected);
	}

	private void assertCapturedAcquiredEvents(List<Event> events, int index, NodeDatum expectedCaptured,
			NodeDatum expectedAcquired) {
		assertThat("Expected event count posted for datum", events.size(),
				greaterThanOrEqualTo(index + 1 + (expectedAcquired != null ? 1 : 0)));
		assertThat(format("Event %d is DATUM_CAPTURED", index), events.get(index).getTopic(),
				is(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED));
		assertThat(format("Event %d datum", index),
				events.get(index).getProperty(DatumEvents.DATUM_PROPERTY),
				sameInstance(expectedCaptured));
		if ( expectedAcquired != null ) {
			assertThat(format("Event %d is DATUM_ACQUIRED", index + 1), events.get(index + 1).getTopic(),
					is(DatumQueue.EVENT_TOPIC_DATUM_ACQUIRED));
			assertThat(format("Event %d datum", index + 1),
					events.get(index + 1).getProperty(DatumEvents.DATUM_PROPERTY),
					sameInstance(expectedAcquired));
		}
	}

	@Test
	public void offer_datum() throws InterruptedException {
		// GIVEN
		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID, Instant.now(), new DatumSamples());
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);

		directConsumer.datumQueueWillProcess(queue, datum, Stage.PreFilter, true);
		directConsumer.datumQueueWillProcess(queue, datum, Stage.PostFilter, true);
		datumDao.storeDatum(datum);
		consumer.accept(datum);

		Capture<Event> eventCaptor = Capture.newInstance(CaptureType.ALL);
		eventAdmin.postEvent(capture(eventCaptor));
		expectLastCall().times(2);

		// WHEN
		replayAll();
		queue.offer(datum);

		sleep(queue.getQueueDelayMs() + 300L);

		// THEN
		assertThat("One datum recorded as processed",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Processed), is(1L));
		assertThat("One datum recorded as persisted",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Persisted), is(1L));

		assertCapturedAcquiredEvents(eventCaptor.getValues(), 0, datum);
	}

	@Test
	public void offer_datum_futureDate() throws InterruptedException {
		// GIVEN
		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID,
				Instant.now().plus(1, ChronoUnit.HOURS), new DatumSamples());
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);

		directConsumer.datumQueueWillProcess(queue, datum, Stage.PreFilter, true);
		directConsumer.datumQueueWillProcess(queue, datum, Stage.PostFilter, true);
		datumDao.storeDatum(datum);
		consumer.accept(datum);

		Capture<Event> eventCaptor = Capture.newInstance(CaptureType.ALL);
		eventAdmin.postEvent(capture(eventCaptor));
		expectLastCall().times(2);

		// WHEN
		replayAll();
		queue.offer(datum);

		sleep(queue.getQueueDelayMs() + 300L);

		// THEN
		assertThat("One datum recorded as processed",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Processed), is(1L));
		assertThat("One datum recorded as persisted",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Persisted), is(1L));

		assertCapturedAcquiredEvents(eventCaptor.getValues(), 0, datum);
	}

	@Test
	public void offer_locationDatum() throws InterruptedException {
		// GIVEN
		SimpleDatum datum = SimpleDatum.locationDatum(1L, TEST_SOURCE_ID, Instant.now(),
				new DatumSamples());
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);

		directConsumer.datumQueueWillProcess(queue, datum, Stage.PreFilter, true);
		directConsumer.datumQueueWillProcess(queue, datum, Stage.PostFilter, true);
		datumDao.storeDatum(datum);
		consumer.accept(datum);

		Capture<Event> eventCaptor = Capture.newInstance(CaptureType.ALL);
		eventAdmin.postEvent(capture(eventCaptor));
		expectLastCall().times(2);

		// WHEN
		replayAll();
		queue.offer(datum);

		sleep(queue.getQueueDelayMs() + 300L);

		// THEN
		assertThat("One datum recorded as processed",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Processed), is(1L));
		assertThat("One datum recorded as persisted",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Persisted), is(1L));

		assertCapturedAcquiredEvents(eventCaptor.getValues(), 0, datum);
	}

	@Test
	public void capture_datum() throws InterruptedException {
		// GIVEN
		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID, Instant.now(), new DatumSamples());
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);

		directConsumer.datumQueueWillProcess(queue, datum, Stage.PreFilter, false);
		directConsumer.datumQueueWillProcess(queue, datum, Stage.PostFilter, false);
		consumer.accept(datum);

		Capture<Event> eventCaptor = Capture.newInstance(CaptureType.ALL);
		eventAdmin.postEvent(capture(eventCaptor));
		expectLastCall().times(2);

		// WHEN
		replayAll();
		queue.offer(datum, false);

		sleep(queue.getQueueDelayMs() + 300L);

		// THEN
		assertThat("One datum recorded as processed",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Processed), is(1L));
		assertThat("No datum recorded as persisted",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Persisted), is(0L));

		assertCapturedAcquiredEvents(eventCaptor.getValues(), 0, datum);
	}

	@Test
	public void capture_locationDatum() throws InterruptedException {
		// GIVEN
		SimpleDatum datum = SimpleDatum.locationDatum(1L, TEST_SOURCE_ID, Instant.now(),
				new DatumSamples());
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);

		directConsumer.datumQueueWillProcess(queue, datum, Stage.PreFilter, false);
		directConsumer.datumQueueWillProcess(queue, datum, Stage.PostFilter, false);
		consumer.accept(datum);

		Capture<Event> eventCaptor = Capture.newInstance(CaptureType.ALL);
		eventAdmin.postEvent(capture(eventCaptor));
		expectLastCall().times(2);

		// WHEN
		replayAll();
		queue.offer(datum, false);

		sleep(queue.getQueueDelayMs() + 300L);

		// THEN
		assertThat("One datum recorded as processed",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Processed), is(1L));
		assertThat("No datum recorded as persisted",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Persisted), is(0L));

		assertCapturedAcquiredEvents(eventCaptor.getValues(), 0, datum);
	}

	@Test
	public void capture_datum_duplicate() throws InterruptedException {
		// GIVEN
		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID, Instant.now(), new DatumSamples());
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);

		directConsumer.datumQueueWillProcess(queue, datum, Stage.PreFilter, true);
		directConsumer.datumQueueWillProcess(queue, datum, Stage.PostFilter, true);
		datumDao.storeDatum(datum);
		consumer.accept(datum);

		Capture<Event> eventCaptor = Capture.newInstance(CaptureType.ALL);
		eventAdmin.postEvent(capture(eventCaptor));
		expectLastCall().times(2);

		// WHEN
		replayAll();

		// 2 different threads providing the same datum at close to the same time;
		// one should be discarded
		new Thread(new Runnable() {

			@Override
			public void run() {
				queue.offer(datum, false);
			}

		}).start();
		queue.offer(datum);

		sleep(queue.getQueueDelayMs() + 300L);

		// THEN
		assertThat("One datum recorded as duplicate",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Duplicates), is(1L));

		assertCapturedAcquiredEvents(eventCaptor.getValues(), 0, datum);
	}

	@Test
	public void capture_datum_concurrently() throws InterruptedException {
		// GIVEN
		ExecutorService executor = Executors.newCachedThreadPool();

		final int count = 20;
		final int sources = 2;

		Capture<NodeDatum> preDatumCaptor = Capture.newInstance(CaptureType.ALL);
		directConsumer.datumQueueWillProcess(same(queue), capture(preDatumCaptor), eq(Stage.PreFilter),
				eq(true));
		expectLastCall().anyTimes();//.times(count);

		Capture<NodeDatum> directDatumCaptor = Capture.newInstance(CaptureType.ALL);
		directConsumer.datumQueueWillProcess(same(queue), capture(directDatumCaptor),
				eq(Stage.PostFilter), eq(true));
		expectLastCall().anyTimes();//.times(count);

		Capture<NodeDatum> datumCaptor = Capture.newInstance(CaptureType.ALL);
		datumDao.storeDatum(capture(datumCaptor));
		expectLastCall().anyTimes();//.times(count);

		Capture<NodeDatum> generalDatumCaptor = Capture.newInstance(CaptureType.ALL);
		consumer.accept(capture(generalDatumCaptor));
		expectLastCall().anyTimes();//.times(count);

		Capture<Event> eventCaptor = Capture.newInstance(CaptureType.ALL);
		eventAdmin.postEvent(capture(eventCaptor));
		expectLastCall().times(count * 2);

		// WHEN
		replayAll();

		final List<NodeDatum> datumList = new ArrayList<>();

		// different threads providing the same datum for different sources at close to the same time
		// this test depends on no more than 'sources' number of datum getting generated with the same
		// date: the faster the machines running the test is, the more datum will be generated at the
		// same date so the queue needs to deal with potentially many datum from same date even for
		// same source ID
		for ( int i = 0; i < count; i++ ) {
			SimpleDatum datum = SimpleDatum.nodeDatum(String.valueOf(i % sources), Instant.now(),
					new DatumSamples());
			datum.getSamples().putInstantaneousSampleValue("watts", 1234);
			datumList.add(datum);

			executor.execute(new Runnable() {

				@Override
				public void run() {
					queue.offer(datum, false);
				}

			});
			queue.offer(datum);
		}

		executor.shutdown();
		executor.awaitTermination(5, TimeUnit.SECONDS);

		sleep(queue.getQueueDelayMs() + 300L);

		// THEN
		assertThat("No duplicates PreFilter consumed", preDatumCaptor.getValues(), hasSize(count));
		assertThat("No duplicates PostFilter consumed", directDatumCaptor.getValues(), hasSize(count));
		assertThat("No duplicates persisted", datumCaptor.getValues(), hasSize(count));
		assertThat("No duplicates consumed", generalDatumCaptor.getValues(), hasSize(count));
		assertThat("Half of all datum recorded as duplicate",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Duplicates), is((long) count));
		assertThat("Half of all datum recorded persisted",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Persisted), is((long) count));
	}

	private static abstract class TestTransformService extends BaseDatumFilterSupport
			implements DatumFilterService {

	}

	@Test
	public void offer_datum_filtered() throws InterruptedException {
		// GIVEN
		queue.setDatumFilterService(new StaticOptionalService<>(new TestTransformService() {

			@Override
			public DatumSamplesOperations filter(Datum datum, DatumSamplesOperations samples,
					Map<String, Object> parameters) {
				return null;
			}

		}));

		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID, Instant.now(), new DatumSamples());
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);

		directConsumer.datumQueueWillProcess(queue, datum, Stage.PreFilter, true);

		Capture<Event> eventCaptor = Capture.newInstance(CaptureType.ALL);
		eventAdmin.postEvent(capture(eventCaptor));
		expectLastCall();

		// WHEN
		replayAll();
		queue.offer(datum);

		sleep(queue.getQueueDelayMs() + 300L);

		// THEN
		assertThat("One datum recorded as processed",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Processed), is(1L));
		assertThat("One datum recorded as filtered",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Filtered), is(1L));
		assertThat("No datum recorded as persisted",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Persisted), is(0L));

		assertThat("Event 1 is DATUM_CAPTURED", eventCaptor.getValue().getTopic(),
				is(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED));
		assertThat("Event 1 datum", eventCaptor.getValue().getProperty(DatumEvents.DATUM_PROPERTY),
				sameInstance(datum));
		// No ACQUIRED event because datum filtered
	}

	@Test
	public void offer_datum_transformed() throws InterruptedException {
		// GIVEN
		final DatumSamples transformed = new DatumSamples();
		transformed.putInstantaneousSampleValue("foo", 1234);
		queue.setDatumFilterService(new StaticOptionalService<>(new TestTransformService() {

			@Override
			public DatumSamplesOperations filter(Datum datum, DatumSamplesOperations samples,
					Map<String, Object> parameters) {
				return transformed;
			}

		}));

		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID, Instant.now(), new DatumSamples());
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);

		Capture<NodeDatum> preDatumCaptor = Capture.newInstance(CaptureType.ALL);
		directConsumer.datumQueueWillProcess(same(queue), capture(preDatumCaptor), eq(Stage.PreFilter),
				eq(true));

		Capture<NodeDatum> directDatumCaptor = Capture.newInstance();
		directConsumer.datumQueueWillProcess(same(queue), capture(directDatumCaptor),
				eq(Stage.PostFilter), eq(true));

		Capture<NodeDatum> datumCaptor = Capture.newInstance();
		datumDao.storeDatum(capture(datumCaptor));

		Capture<NodeDatum> generalDatumCaptor = Capture.newInstance();
		consumer.accept(capture(generalDatumCaptor));

		Capture<Event> eventCaptor = Capture.newInstance(CaptureType.ALL);
		eventAdmin.postEvent(capture(eventCaptor));
		expectLastCall().times(2);

		// WHEN
		replayAll();
		queue.offer(datum);

		sleep(queue.getQueueDelayMs() + 300L);

		// THEN
		assertThat("One datum recorded as processed",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Processed), is(1L));
		assertThat("One datum recorded as persisted",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Persisted), is(1L));

		assertThat("PreFilter consumed datum unchanged", preDatumCaptor.getValue(),
				is(sameInstance(datum)));

		assertThat("Direct consumed datum has changed", directDatumCaptor.getValue(),
				is(not(sameInstance(datum))));
		assertThat("Direct consumed datum samples has been transformed",
				((DatumSamplesContainer) directDatumCaptor.getValue()).getSamples(), is(transformed));
		assertThat("Persisted datum has changed", datumCaptor.getValue(), is(not(sameInstance(datum))));
		assertThat("Persisted datum samples has been transformed",
				((DatumSamplesContainer) datumCaptor.getValue()).getSamples(), is(transformed));
		assertThat("Consumed datum has changed", generalDatumCaptor.getValue(),
				is(not(sameInstance(datum))));
		assertThat("Consumed datum samples has been transformed",
				((DatumSamplesContainer) generalDatumCaptor.getValue()).getSamples(), is(transformed));

		assertCapturedAcquiredEvents(eventCaptor.getValues(), 0, datum, datumCaptor.getValue());
	}

	@Test
	public void exception_persist() throws InterruptedException {
		// GIVEN
		final AtomicReference<Throwable> exceptionRef = new AtomicReference<>();
		queue.setDatumProcessorExceptionHandler(new UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(Thread t, Throwable e) {
				exceptionRef.set(e);
			}
		});

		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID, Instant.now().minusMillis(10),
				new DatumSamples());
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);

		SimpleDatum datum2 = SimpleDatum.nodeDatum(TEST_SOURCE_ID, Instant.now(), new DatumSamples());
		datum.getSamples().putInstantaneousSampleValue("watts", 2345);

		directConsumer.datumQueueWillProcess(queue, datum, Stage.PreFilter, true);
		directConsumer.datumQueueWillProcess(queue, datum, Stage.PostFilter, true);

		datumDao.storeDatum(datum);
		expectLastCall().andThrow(new RuntimeException("test"));

		directConsumer.datumQueueWillProcess(queue, datum2, Stage.PreFilter, true);
		directConsumer.datumQueueWillProcess(queue, datum2, Stage.PostFilter, true);
		datumDao.storeDatum(datum2);
		consumer.accept(datum2);

		Capture<Event> eventCaptor = Capture.newInstance(CaptureType.ALL);
		eventAdmin.postEvent(capture(eventCaptor));
		expectLastCall().times(4);

		// WHEN
		replayAll();
		queue.offer(datum);
		queue.offer(datum2);

		sleep(queue.getQueueDelayMs() + 300L);

		// THEN
		assertThat("Two datum recorded as processed",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Processed), is(2L));
		assertThat("One datum recorded as persisted",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Persisted), is(1L));
		assertThat("One datum recorded as error",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Errors), is(1L));
		assertThat("Exception passed to handler", exceptionRef.get(), is(notNullValue()));

		assertCapturedAcquiredEvents(eventCaptor.getValues(), 0, datum);
		assertCapturedAcquiredEvents(eventCaptor.getValues(), 2, datum2);
	}

	@Test
	public void exception_transform_continue() throws InterruptedException {
		// GIVEN
		queue.setDatumFilterService(new StaticOptionalService<>(filter));
		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID, Instant.now(), new DatumSamples());
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);

		Capture<NodeDatum> preDatumCaptor = Capture.newInstance(CaptureType.ALL);
		directConsumer.datumQueueWillProcess(same(queue), capture(preDatumCaptor), eq(Stage.PreFilter),
				eq(true));

		final DatumSamples filterSamples = new DatumSamples();
		filterSamples.putInstantaneousSampleValue("foo", 321);
		Capture<Map<String, Object>> filterParametersCaptor = Capture.newInstance();
		RuntimeException filterException = new RuntimeException("Boom!");
		expect(filter.filter(same(datum), same(datum.getSamples()), capture(filterParametersCaptor)))
				.andThrow(filterException);

		Capture<NodeDatum> filteredDatumCaptor = Capture.newInstance(CaptureType.ALL);
		directConsumer.datumQueueWillProcess(same(queue), capture(filteredDatumCaptor),
				eq(Stage.PostFilter), eq(true));
		datumDao.storeDatum(capture(filteredDatumCaptor));
		consumer.accept(capture(filteredDatumCaptor));

		Capture<Event> eventCaptor = Capture.newInstance(CaptureType.ALL);
		eventAdmin.postEvent(capture(eventCaptor));
		expectLastCall().times(2);

		// WHEN
		replayAll();
		queue.offer(datum);

		sleep(queue.getQueueDelayMs() + 300L);

		// THEN
		assertThat("One datum recorded as processed",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Processed), is(1L));
		assertThat("One datum recorded as persisted",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Persisted), is(1L));

		List<NodeDatum> preDatumList = preDatumCaptor.getValues();
		assertThat("Unfiltered datum passed to observer", preDatumList, hasSize(1));

		List<NodeDatum> filteredDatumList = filteredDatumCaptor.getValues();
		assertThat("Filtered datum passed to all output methods", filteredDatumList, hasSize(3));

		assertThat("Provided filter parameters is empty", filterParametersCaptor.getValue(),
				is(equalTo(emptyMap())));

		NodeDatum filteredDatum = filteredDatumList.get(0);
		assertThat("Same filtered datum passed to DAO", filteredDatumList.get(1),
				is(sameInstance(filteredDatum)));
		assertThat("Same filtered datum passed to consumer", filteredDatumList.get(2),
				is(sameInstance(filteredDatum)));

		assertThat("Persisted datum ID from input datum", filteredDatum, is(equalTo(datum)));

		// capture offered event, persist filtered event
		assertCapturedAcquiredEvents(eventCaptor.getValues(), 0, datum, filteredDatum);
	}

	@Test
	public void exception_transform_discard() throws InterruptedException {
		// GIVEN
		queue.setDiscardDatumOnFilterException(true);
		final AtomicReference<Throwable> exceptionRef = new AtomicReference<>();
		queue.setDatumProcessorExceptionHandler(new UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(Thread t, Throwable e) {
				exceptionRef.set(e);
			}
		});
		queue.setDatumFilterService(new StaticOptionalService<>(new TestTransformService() {

			private int count = 0;

			@Override
			public DatumSamplesOperations filter(Datum datum, DatumSamplesOperations samples,
					Map<String, Object> parameters) {
				if ( 0 == count++ ) {
					throw new RuntimeException("test");
				}
				return samples;
			}

		}));

		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID, Instant.now().minusMillis(10),
				new DatumSamples());
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);

		directConsumer.datumQueueWillProcess(queue, datum, Stage.PreFilter, true);

		SimpleDatum datum2 = SimpleDatum.nodeDatum(TEST_SOURCE_ID, Instant.now(), new DatumSamples());
		datum.getSamples().putInstantaneousSampleValue("watts", 2345);

		directConsumer.datumQueueWillProcess(queue, datum2, Stage.PreFilter, true);
		directConsumer.datumQueueWillProcess(queue, datum2, Stage.PostFilter, true);
		datumDao.storeDatum(datum2);
		consumer.accept(datum2);

		Capture<Event> eventCaptor = Capture.newInstance(CaptureType.ALL);
		eventAdmin.postEvent(capture(eventCaptor));
		expectLastCall().times(3);

		// WHEN
		replayAll();
		queue.offer(datum);
		queue.offer(datum2);

		sleep(queue.getQueueDelayMs() + 300L);

		// THEN
		assertThat("Two datum recorded as processed",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Processed), is(2L));
		assertThat("One datum recorded as persisted",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Persisted), is(1L));
		assertThat("One datum recorded as error",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Errors), is(1L));
		assertThat("Exception passed to handler", exceptionRef.get(), is(notNullValue()));

		assertThat("Event 1 is DATUM_CAPTURED", eventCaptor.getValues().get(0).getTopic(),
				is(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED));
		assertThat("Event 1 datum",
				eventCaptor.getValues().get(0).getProperty(DatumEvents.DATUM_PROPERTY),
				sameInstance(datum));
		// No ACQUIRED event because datum filter threw exception
		assertCapturedAcquiredEvents(eventCaptor.getValues(), 1, datum2);
	}

	@Test
	public void exception_consume() throws InterruptedException {
		// GIVEN
		final AtomicReference<Throwable> exceptionRef = new AtomicReference<>();
		queue.setDatumProcessorExceptionHandler(new UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(Thread t, Throwable e) {
				exceptionRef.set(e);
			}
		});

		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID, Instant.now(), new DatumSamples());
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);

		SimpleDatum datum2 = SimpleDatum.nodeDatum(TEST_SOURCE_ID, Instant.now(), new DatumSamples());
		datum.getSamples().putInstantaneousSampleValue("watts", 2345);

		directConsumer.datumQueueWillProcess(queue, datum, Stage.PreFilter, true);
		directConsumer.datumQueueWillProcess(queue, datum, Stage.PostFilter, true);
		datumDao.storeDatum(datum);
		consumer.accept(datum);
		expectLastCall().andThrow(new RuntimeException("test"));

		directConsumer.datumQueueWillProcess(queue, datum2, Stage.PreFilter, true);
		directConsumer.datumQueueWillProcess(queue, datum2, Stage.PostFilter, true);
		datumDao.storeDatum(datum2);
		consumer.accept(datum2);

		Capture<Event> eventCaptor = Capture.newInstance(CaptureType.ALL);
		eventAdmin.postEvent(capture(eventCaptor));
		expectLastCall().times(4);

		// WHEN
		replayAll();
		queue.offer(datum);
		queue.offer(datum2);

		sleep(queue.getQueueDelayMs() + 300L);

		// THEN
		assertThat("Two datum recorded as processed",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Processed), is(2L));
		assertThat("Two datum recorded as persisted",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Persisted), is(2L));
		assertThat("One datum recorded as error",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Errors), is(1L));
		assertThat("Exception NOT passed to handler", exceptionRef.get(), is(nullValue()));

		assertCapturedAcquiredEvents(eventCaptor.getValues(), 0, datum);
		assertCapturedAcquiredEvents(eventCaptor.getValues(), 2, datum2);
	}

	@Test
	public void exception_directConsume() throws InterruptedException {
		// GIVEN
		final AtomicReference<Throwable> exceptionRef = new AtomicReference<>();
		queue.setDatumProcessorExceptionHandler(new UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(Thread t, Throwable e) {
				exceptionRef.set(e);
			}
		});

		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID, Instant.now(), new DatumSamples());
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);

		SimpleDatum datum2 = SimpleDatum.nodeDatum(TEST_SOURCE_ID, Instant.now(), new DatumSamples());
		datum.getSamples().putInstantaneousSampleValue("watts", 2345);

		directConsumer.datumQueueWillProcess(queue, datum, Stage.PreFilter, true);
		directConsumer.datumQueueWillProcess(queue, datum, Stage.PostFilter, true);
		expectLastCall().andThrow(new RuntimeException("test"));
		datumDao.storeDatum(datum);
		consumer.accept(datum);

		directConsumer.datumQueueWillProcess(queue, datum2, Stage.PreFilter, true);
		directConsumer.datumQueueWillProcess(queue, datum2, Stage.PostFilter, true);
		datumDao.storeDatum(datum2);
		consumer.accept(datum2);

		Capture<Event> eventCaptor = Capture.newInstance(CaptureType.ALL);
		eventAdmin.postEvent(capture(eventCaptor));
		expectLastCall().times(4);

		// WHEN
		replayAll();
		queue.offer(datum);
		queue.offer(datum2);

		sleep(queue.getQueueDelayMs() + 300L);

		// THEN
		assertThat("Two datum recorded as processed",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Processed), is(2L));
		assertThat("Two datum recorded as persisted",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Persisted), is(2L));
		assertThat("One datum recorded as error",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Errors), is(1L));
		assertThat("Exception NOT passed to handler", exceptionRef.get(), is(nullValue()));

		assertCapturedAcquiredEvents(eventCaptor.getValues(), 0, datum);
		assertCapturedAcquiredEvents(eventCaptor.getValues(), 2, datum2);
	}

	@Test
	public void filter_datum() throws InterruptedException {
		// GIVEN
		queue.setDatumFilterService(new StaticOptionalService<>(filter));
		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID, Instant.now(), new DatumSamples());
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);

		Capture<NodeDatum> preDatumCaptor = Capture.newInstance(CaptureType.ALL);
		directConsumer.datumQueueWillProcess(same(queue), capture(preDatumCaptor), eq(Stage.PreFilter),
				eq(true));

		final DatumSamples filterSamples = new DatumSamples();
		filterSamples.putInstantaneousSampleValue("foo", 321);
		Capture<Map<String, Object>> filterParametersCaptor = Capture.newInstance();
		expect(filter.filter(same(datum), same(datum.getSamples()), capture(filterParametersCaptor)))
				.andReturn(filterSamples);

		Capture<NodeDatum> filteredDatumCaptor = Capture.newInstance(CaptureType.ALL);
		directConsumer.datumQueueWillProcess(same(queue), capture(filteredDatumCaptor),
				eq(Stage.PostFilter), eq(true));
		datumDao.storeDatum(capture(filteredDatumCaptor));
		consumer.accept(capture(filteredDatumCaptor));

		Capture<Event> eventCaptor = Capture.newInstance(CaptureType.ALL);
		eventAdmin.postEvent(capture(eventCaptor));
		expectLastCall().times(2);

		// WHEN
		replayAll();
		queue.offer(datum);

		sleep(queue.getQueueDelayMs() + 300L);

		// THEN
		assertThat("One datum recorded as processed",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Processed), is(1L));
		assertThat("One datum recorded as persisted",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Persisted), is(1L));

		List<NodeDatum> preDatumList = preDatumCaptor.getValues();
		assertThat("Unfiltered datum passed to observer", preDatumList, hasSize(1));

		List<NodeDatum> filteredDatumList = filteredDatumCaptor.getValues();
		assertThat("Filtered datum passed to all output methods", filteredDatumList, hasSize(3));

		assertThat("Provided filter parameters is empty", filterParametersCaptor.getValue(),
				is(equalTo(emptyMap())));

		NodeDatum filteredDatum = filteredDatumList.get(0);
		assertThat("Same filtered datum passed to DAO", filteredDatumList.get(1),
				is(sameInstance(filteredDatum)));
		assertThat("Same filtered datum passed to consumer", filteredDatumList.get(2),
				is(sameInstance(filteredDatum)));

		assertThat("Persisted datum ID from input datum", filteredDatum, is(equalTo(datum)));

		// capture offered event, persist filtered event
		assertCapturedAcquiredEvents(eventCaptor.getValues(), 0, datum, filteredDatum);
	}

	@Test
	public void filter_datum_eliminated() throws InterruptedException {
		// GIVEN
		queue.setDatumFilterService(new StaticOptionalService<>(filter));
		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID, Instant.now(), new DatumSamples());
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);

		directConsumer.datumQueueWillProcess(queue, datum, Stage.PreFilter, true);

		final DatumSamples filterSamples = new DatumSamples();
		filterSamples.putInstantaneousSampleValue("foo", 321);
		Capture<Map<String, Object>> filterParametersCaptor = Capture.newInstance();
		expect(filter.filter(same(datum), same(datum.getSamples()), capture(filterParametersCaptor)))
				.andReturn(null);

		Capture<Event> eventCaptor = Capture.newInstance(CaptureType.ALL);
		eventAdmin.postEvent(capture(eventCaptor));
		expectLastCall().times(1);

		// WHEN
		replayAll();
		queue.offer(datum);

		sleep(queue.getQueueDelayMs() + 300L);

		// THEN
		assertThat("One datum recorded as processed",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Processed), is(1L));
		assertThat("No datum recorded as persisted",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Persisted), is(0L));

		assertThat("Provided filter parameters is empty", filterParametersCaptor.getValue(),
				is(equalTo(emptyMap())));

		// capture offered event, no persist filtered event
		assertCapturedAcquiredEvents(eventCaptor.getValues(), 0, datum, null);
	}

	@Test
	public void filter_datum_newDatumProduced() throws InterruptedException {
		// GIVEN
		queue.setDatumFilterService(new StaticOptionalService<>(filter));
		SimpleDatum datum = SimpleDatum.nodeDatum(TEST_SOURCE_ID, Instant.now(), new DatumSamples());
		datum.getSamples().putInstantaneousSampleValue("watts", 1234);

		directConsumer.datumQueueWillProcess(queue, datum, Stage.PreFilter, true);

		final SimpleDatum filterOutput = SimpleDatum.nodeDatum(TEST_SOURCE_ID,
				datum.getTimestamp().truncatedTo(ChronoUnit.HOURS).minus(1, ChronoUnit.HOURS),
				new DatumSamples());
		filterOutput.putSampleValue(DatumSamplesType.Instantaneous, "foo", 321);
		Capture<Map<String, Object>> filterParametersCaptor = Capture.newInstance();
		expect(filter.filter(same(datum), same(datum.getSamples()), capture(filterParametersCaptor)))
				.andReturn(filterOutput);

		Capture<NodeDatum> filteredDatumCaptor = Capture.newInstance(CaptureType.ALL);
		directConsumer.datumQueueWillProcess(same(queue), capture(filteredDatumCaptor),
				eq(Stage.PostFilter), eq(true));
		datumDao.storeDatum(capture(filteredDatumCaptor));
		consumer.accept(capture(filteredDatumCaptor));

		Capture<Event> eventCaptor = Capture.newInstance(CaptureType.ALL);
		eventAdmin.postEvent(capture(eventCaptor));
		expectLastCall().times(2);

		// WHEN
		replayAll();
		queue.offer(datum);

		sleep(queue.getQueueDelayMs() + 300L);

		// THEN
		assertThat("One datum recorded as processed",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Processed), is(1L));
		assertThat("One datum recorded as persisted",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Persisted), is(1L));

		assertThat("Provided filter parameters is empty", filterParametersCaptor.getValue(),
				is(equalTo(emptyMap())));

		List<NodeDatum> filteredDatumList = filteredDatumCaptor.getValues();
		assertThat("Filtered datum passed to all output methods", filteredDatumList, hasSize(3));

		NodeDatum filteredDatum = filteredDatumList.get(0);
		assertThat("Same filtered datum passed to DAO", filteredDatumList.get(1),
				is(sameInstance(filteredDatum)));
		assertThat("Same filtered datum passed to consumer", filteredDatumList.get(2),
				is(sameInstance(filteredDatum)));

		assertThat("Persisted datum ID from filter output", filteredDatum, is(equalTo(filterOutput)));

		// capture offered event, persist filtered event
		assertCapturedAcquiredEvents(eventCaptor.getValues(), 0, datum, filteredDatum);
	}

}
