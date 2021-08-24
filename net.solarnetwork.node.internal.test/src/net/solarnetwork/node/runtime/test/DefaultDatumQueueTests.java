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

import static java.lang.Thread.sleep;
import static net.solarnetwork.node.support.DatumEvents.datumEvent;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Date;
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
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.GeneralDatumSamplesTransformService;
import net.solarnetwork.node.dao.DatumDao;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.GeneralDatum;
import net.solarnetwork.node.domain.GeneralLocationDatum;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.runtime.DefaultDatumQueue;
import net.solarnetwork.node.support.BaseSamplesTransformSupport;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link DefaultDatumQueue}.
 * 
 * @author matt
 * @version 1.0
 */
public class DefaultDatumQueueTests implements UncaughtExceptionHandler {

	private static final String TEST_SOURCE_ID = "test.source";

	private DatumDao<GeneralNodeDatum> datumDao;
	private DatumDao<GeneralLocationDatum> locationDatumDao;
	private Consumer<GeneralDatum> consumer;
	private DefaultDatumQueue queue;
	private Throwable datumProcessortException;

	@SuppressWarnings("unchecked")
	@Before
	public void setup() {
		datumDao = EasyMock.createMock(DatumDao.class);
		locationDatumDao = EasyMock.createMock(DatumDao.class);
		consumer = EasyMock.createMock(Consumer.class);
		queue = new DefaultDatumQueue(datumDao, locationDatumDao);
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
		EasyMock.verify(datumDao, locationDatumDao, consumer);
	}

	private void replayAll() {
		EasyMock.replay(datumDao, locationDatumDao, consumer);
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		datumProcessortException = e;
	}

	@Test
	public void offer_datum() throws InterruptedException {
		// GIVEN
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(new Date());
		datum.setSourceId(TEST_SOURCE_ID);
		datum.putInstantaneousSampleValue("watts", 1234);

		datumDao.storeDatum(datum);
		consumer.accept(datum);

		// WHEN
		replayAll();
		queue.offer(datum);

		sleep(queue.getQueueDelayMs() + 300L);

		// THEN
		assertThat("One datum recorded as processed",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Processed), is(1L));
		assertThat("One datum recorded as persisted",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Persisted), is(1L));
	}

	@Test
	public void offer_locationDatum() throws InterruptedException {
		// GIVEN
		GeneralLocationDatum datum = new GeneralLocationDatum();
		datum.setCreated(new Date());
		datum.setLocationId(1L);
		datum.setSourceId(TEST_SOURCE_ID);
		datum.putInstantaneousSampleValue("watts", 1234);

		locationDatumDao.storeDatum(datum);
		consumer.accept(datum);

		// WHEN
		replayAll();
		queue.offer(datum);

		sleep(queue.getQueueDelayMs() + 300L);

		// THEN
		assertThat("One datum recorded as processed",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Processed), is(1L));
		assertThat("One datum recorded as persisted",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Persisted), is(1L));
	}

	@Test
	public void capture_datum() throws InterruptedException {
		// GIVEN
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(new Date());
		datum.setSourceId(TEST_SOURCE_ID);
		datum.putInstantaneousSampleValue("watts", 1234);

		consumer.accept(datum);

		// WHEN
		replayAll();
		Event event = datumEvent(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED, datum);
		queue.handleEvent(event);

		sleep(queue.getQueueDelayMs() + 300L);

		// THEN
		assertThat("One datum recorded as processed",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Processed), is(1L));
		assertThat("No datum recorded as persisted",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Persisted), is(0L));
	}

	@Test
	public void capture_locationDatum() throws InterruptedException {
		// GIVEN
		GeneralLocationDatum datum = new GeneralLocationDatum();
		datum.setCreated(new Date());
		datum.setLocationId(1L);
		datum.setSourceId(TEST_SOURCE_ID);
		datum.putInstantaneousSampleValue("watts", 1234);

		consumer.accept(datum);

		// WHEN
		replayAll();
		Event event = datumEvent(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED, datum);
		queue.handleEvent(event);

		sleep(queue.getQueueDelayMs() + 300L);

		// THEN
		assertThat("One datum recorded as processed",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Processed), is(1L));
		assertThat("No datum recorded as persisted",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Persisted), is(0L));
	}

	@Test
	public void capture_datum_duplicate() throws InterruptedException {
		// GIVEN
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(new Date());
		datum.setSourceId(TEST_SOURCE_ID);
		datum.putInstantaneousSampleValue("watts", 1234);

		datumDao.storeDatum(datum);
		consumer.accept(datum);

		// WHEN
		replayAll();

		// 2 different threads providing the same datum at close to the same time;
		// one should be discarded
		new Thread(new Runnable() {

			@Override
			public void run() {
				Event event = datumEvent(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED, datum);
				queue.handleEvent(event);
			}

		}).start();
		queue.offer(datum);

		sleep(queue.getQueueDelayMs() + 300L);

		// THEN
		assertThat("One datum recorded as duplicate",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Duplicates), is(1L));
	}

	@Test
	public void capture_datum_concurrently() throws InterruptedException {
		// GIVEN
		ExecutorService executor = Executors.newCachedThreadPool();

		final int count = 20;
		final int sources = 2;

		Capture<GeneralNodeDatum> datumCaptor = new Capture<>(CaptureType.ALL);
		datumDao.storeDatum(capture(datumCaptor));
		expectLastCall().anyTimes();//.times(count);
		Capture<GeneralDatum> generalDatumCaptor = new Capture<>(CaptureType.ALL);
		consumer.accept(capture(generalDatumCaptor));
		expectLastCall().anyTimes();//.times(count);

		// WHEN
		replayAll();

		final List<GeneralNodeDatum> datumList = new ArrayList<>();

		// different threads providing the same datum for different sources at close to the same time
		// this test depends on no more than 'sources' number of datum getting generated with the same
		// date: the faster the machines running the test is, the more datum will be generated at the
		// same date so the queue needs to deal with potentially many datum from same date even for
		// same source ID
		for ( int i = 0; i < count; i++ ) {
			GeneralNodeDatum datum = new GeneralNodeDatum();
			datum.setCreated(new Date());
			datum.setSourceId(String.valueOf(i % sources));
			datum.putInstantaneousSampleValue("watts", 1234);
			datumList.add(datum);

			executor.execute(new Runnable() {

				@Override
				public void run() {
					Event event = datumEvent(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED, datum);
					queue.handleEvent(event);
				}

			});
			queue.offer(datum);
		}

		executor.shutdown();
		executor.awaitTermination(5, TimeUnit.SECONDS);

		sleep(queue.getQueueDelayMs() + 300L);

		// THEN
		assertThat("No duplicates persisted", datumCaptor.getValues(), hasSize(count));
		assertThat("No duplicates consumed", generalDatumCaptor.getValues(), hasSize(count));
		assertThat("Half of all datum recorded as duplicate",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Duplicates), is((long) count));
		assertThat("Half of all datum recorded persisted",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Persisted), is((long) count));
	}

	private static abstract class TestTransformService extends BaseSamplesTransformSupport
			implements GeneralDatumSamplesTransformService {

	}

	@Test
	public void offer_datum_filtered() throws InterruptedException {
		// GIVEN
		queue.setTransformService(new StaticOptionalService<>(new TestTransformService() {

			@Override
			public GeneralDatumSamples transformSamples(Datum datum, GeneralDatumSamples samples,
					Map<String, Object> parameters) {
				return null;
			}

		}));

		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(new Date());
		datum.setSourceId(TEST_SOURCE_ID);
		datum.putInstantaneousSampleValue("watts", 1234);

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
	}

	@Test
	public void offer_datum_transformed() throws InterruptedException {
		// GIVEN
		final GeneralDatumSamples transformed = new GeneralDatumSamples();
		transformed.putInstantaneousSampleValue("foo", 1234);
		queue.setTransformService(new StaticOptionalService<>(new TestTransformService() {

			@Override
			public GeneralDatumSamples transformSamples(Datum datum, GeneralDatumSamples samples,
					Map<String, Object> parameters) {
				return transformed;
			}

		}));

		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(new Date());
		datum.setSourceId(TEST_SOURCE_ID);
		datum.putInstantaneousSampleValue("watts", 1234);

		Capture<GeneralNodeDatum> datumCaptor = new Capture<>();
		datumDao.storeDatum(capture(datumCaptor));

		Capture<GeneralDatum> generalDatumCaptor = new Capture<>();
		consumer.accept(capture(generalDatumCaptor));

		// WHEN
		replayAll();
		queue.offer(datum);

		sleep(queue.getQueueDelayMs() + 300L);

		// THEN
		assertThat("One datum recorded as processed",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Processed), is(1L));
		assertThat("One datum recorded as persisted",
				queue.getStats().get(DefaultDatumQueue.QueueStats.Persisted), is(1L));

		assertThat("Persisted datum has changed", datumCaptor.getValue(), is(not(sameInstance(datum))));
		assertThat("Persisted datum samples has been transformed", datumCaptor.getValue().getSamples(),
				is(sameInstance(transformed)));
		assertThat("Consumed datum has changed", generalDatumCaptor.getValue(),
				is(not(sameInstance(datum))));
		assertThat("Consumed datum samples has been transformed",
				((GeneralNodeDatum) generalDatumCaptor.getValue()).getSamples(),
				is(sameInstance(transformed)));
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

		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(new Date(System.currentTimeMillis() - 10L));
		datum.setSourceId(TEST_SOURCE_ID);
		datum.putInstantaneousSampleValue("watts", 1234);

		GeneralNodeDatum datum2 = new GeneralNodeDatum();
		datum2.setCreated(new Date());
		datum2.setSourceId(TEST_SOURCE_ID);
		datum2.putInstantaneousSampleValue("watts", 1234);

		datumDao.storeDatum(datum);
		expectLastCall().andThrow(new RuntimeException("test"));

		datumDao.storeDatum(datum2);
		consumer.accept(datum2);

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
	}

	@Test
	public void exception_transform() throws InterruptedException {
		// GIVEN
		final AtomicReference<Throwable> exceptionRef = new AtomicReference<>();
		queue.setDatumProcessorExceptionHandler(new UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(Thread t, Throwable e) {
				exceptionRef.set(e);
			}
		});
		queue.setTransformService(new StaticOptionalService<>(new TestTransformService() {

			private int count = 0;

			@Override
			public GeneralDatumSamples transformSamples(Datum datum, GeneralDatumSamples samples,
					Map<String, Object> parameters) {
				if ( 0 == count++ ) {
					throw new RuntimeException("test");
				}
				return samples;
			}

		}));

		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(new Date(System.currentTimeMillis() - 10L));
		datum.setSourceId(TEST_SOURCE_ID);
		datum.putInstantaneousSampleValue("watts", 1234);

		GeneralNodeDatum datum2 = new GeneralNodeDatum();
		datum2.setCreated(new Date());
		datum2.setSourceId(TEST_SOURCE_ID);
		datum2.putInstantaneousSampleValue("watts", 1234);

		datumDao.storeDatum(datum2);
		consumer.accept(datum2);

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

		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(new Date());
		datum.setSourceId(TEST_SOURCE_ID);
		datum.putInstantaneousSampleValue("watts", 1234);

		GeneralNodeDatum datum2 = new GeneralNodeDatum();
		datum2.setCreated(new Date());
		datum2.setSourceId(TEST_SOURCE_ID);
		datum2.putInstantaneousSampleValue("watts", 1234);

		datumDao.storeDatum(datum);
		consumer.accept(datum);
		expectLastCall().andThrow(new RuntimeException("test"));

		datumDao.storeDatum(datum2);
		consumer.accept(datum2);

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
	}

}
