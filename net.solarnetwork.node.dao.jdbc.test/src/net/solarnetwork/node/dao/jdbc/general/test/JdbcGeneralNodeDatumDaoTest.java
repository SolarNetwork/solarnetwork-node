/* ==================================================================
 * JdbcGeneralNodeDatumDaoTest.java - Aug 26, 2014 9:09:51 AM
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

package net.solarnetwork.node.dao.jdbc.general.test;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.springframework.test.context.transaction.BeforeTransaction;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.dao.DatumDao;
import net.solarnetwork.node.dao.jdbc.DatabaseSetup;
import net.solarnetwork.node.dao.jdbc.general.JdbcGeneralNodeDatumDao;
import net.solarnetwork.node.domain.datum.MutableNodeDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleAcEnergyDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link JdbcGeneralNodeDatumDao} class.
 *
 * @author matt
 * @version 2.1
 */
public class JdbcGeneralNodeDatumDaoTest extends AbstractNodeTransactionalTest {

	private JdbcGeneralNodeDatumDao dao;
	private EventAdmin eventAdmin;

	@BeforeTransaction
	public void setup() {
		eventAdmin = EasyMock.createMock(EventAdmin.class);

		DatabaseSetup setup = new DatabaseSetup();
		setup.setDataSource(dataSource);
		setup.init();

		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);

		dao = new JdbcGeneralNodeDatumDao();
		dao.setDataSource(dataSource);
		dao.setObjectMapper(mapper);
		dao.setEventAdmin(new StaticOptionalService<EventAdmin>(eventAdmin));
		dao.init();
	}

	@After
	public void teardown() {
		EasyMock.verify(eventAdmin);
	}

	private void replayAll() {
		EasyMock.replay(eventAdmin);
	}

	private DatumSamples samplesInstance() {
		DatumSamples samples = new DatumSamples();

		// some sample data
		Map<String, Number> instants = new HashMap<String, Number>(2);
		instants.put("watts", 231);
		samples.setInstantaneous(instants);

		Map<String, Number> accum = new HashMap<String, Number>(2);
		accum.put("watt_hours", 4123);
		samples.setAccumulating(accum);

		return samples;
	}

	@Test
	public void insert() {
		Capture<Event> captor = Capture.newInstance();
		eventAdmin.postEvent(EasyMock.capture(captor));

		replayAll();

		SimpleDatum datum = SimpleDatum.nodeDatum("Test", Instant.now().truncatedTo(MILLIS),
				samplesInstance());
		dao.storeDatum(datum);

		assertThat("Event captured", captor.hasCaptured(), equalTo(true));
		Event event = captor.getValue();
		assertDatumStoredEventEqualsDatum(event, datum);
	}

	@Test
	public void insert_location() {
		Capture<Event> captor = Capture.newInstance();
		eventAdmin.postEvent(EasyMock.capture(captor));

		replayAll();

		SimpleDatum datum = SimpleDatum.locationDatum(1L, "Test", Instant.now().truncatedTo(MILLIS),
				samplesInstance());
		dao.storeDatum(datum);

		assertThat("Event captured", captor.hasCaptured(), equalTo(true));
		Event event = captor.getValue();
		assertDatumStoredEventEqualsDatum(event, datum);
	}

	@Test
	public void insertSubclass() {
		Capture<Event> captor = Capture.newInstance();
		eventAdmin.postEvent(EasyMock.capture(captor));

		replayAll();

		SimpleAcEnergyDatum datum = new SimpleAcEnergyDatum("Test", Instant.now().truncatedTo(MILLIS),
				new DatumSamples());
		datum.setWatts(123);
		datum.setWattHourReading(12345L);
		dao.storeDatum(datum);

		assertThat("Event captured", captor.hasCaptured(), equalTo(true));
		Event event = captor.getValue();
		assertDatumEventEqualsDatum(event, DatumDao.EVENT_TOPIC_DATUM_STORED, datum);
	}

	private void assertDatumStoredEventEqualsDatum(Event event, NodeDatum datum) {
		assertDatumEventEqualsDatum(event, DatumDao.EVENT_TOPIC_DATUM_STORED, datum);
	}

	private void assertDatumEventEqualsDatum(Event event, String topic, NodeDatum datum) {
		assertThat("Topic", event.getTopic(), equalTo(topic));
		assertThat("Datum", event.getProperty(DatumEvents.DATUM_PROPERTY), is(sameInstance(datum)));
	}

	@Test
	public void findForUpload() {
		final int numDatum = 5;
		final long now = System.currentTimeMillis();
		final DatumSamples samples = samplesInstance();

		Capture<Event> captor = Capture.newInstance(CaptureType.ALL);
		eventAdmin.postEvent(capture(captor));
		EasyMock.expectLastCall().times(numDatum);

		replayAll();

		List<NodeDatum> stored = new ArrayList<>();
		for ( int i = 0; i < numDatum; i++ ) {
			SimpleDatum datum = SimpleDatum.nodeDatum(String.valueOf(i), Instant.ofEpochMilli(now),
					samples);
			dao.storeDatum(datum);
			stored.add(datum);
		}
		List<NodeDatum> results = dao.getDatumNotUploaded("test");
		assertThat(results, hasSize(numDatum));
		assertThat(captor.getValues(), hasSize(numDatum));

		for ( int i = 0; i < numDatum; i++ ) {
			NodeDatum datum = results.get(i);
			assertThat("Timestamp", datum.getTimestamp().toEpochMilli(), is(now));
			assertThat("Source ID", datum.getSourceId(), is(String.valueOf(i)));
			assertThat("Samples", datum.asSampleOperations(), is(samples));
			assertThat("Not uploaded", datum.getUploaded(), is(nullValue()));

			assertDatumStoredEventEqualsDatum(captor.getValues().get(i), stored.get(i));
		}
	}

	@Test
	public void findForUpload_withLocationDatum() {
		final int numDatum = 10;
		final long now = System.currentTimeMillis();
		final DatumSamples samples = samplesInstance();

		Capture<Event> captor = Capture.newInstance(CaptureType.ALL);
		eventAdmin.postEvent(EasyMock.capture(captor));
		EasyMock.expectLastCall().times(numDatum);

		replayAll();

		List<NodeDatum> stored = new ArrayList<>();
		for ( int i = 0; i < numDatum; i++ ) {
			SimpleDatum datum;
			if ( i % 2 == 0 ) {
				datum = SimpleDatum.nodeDatum(String.valueOf(i), Instant.ofEpochMilli(now), samples);
			} else {
				datum = SimpleDatum.locationDatum((long) i, String.valueOf(i), Instant.ofEpochMilli(now),
						samples);
			}
			dao.storeDatum(datum);
			stored.add(datum);
		}
		List<NodeDatum> results = dao.getDatumNotUploaded("test");
		assertThat(results, hasSize(numDatum));
		assertThat(captor.getValues(), hasSize(numDatum));

		for ( int i = 0; i < numDatum; i++ ) {
			NodeDatum datum = results.get(i);
			assertThat("Timestamp", datum.getTimestamp().toEpochMilli(), is(now));
			assertThat("Source ID", datum.getSourceId(), is(String.valueOf(i)));
			if ( i % 2 == 0 ) {
				assertThat("Object ID missing for node datum", datum.getObjectId(), is(nullValue()));
			} else {
				assertThat("Object ID present for location datum", datum.getObjectId(), is((long) i));
			}
			assertThat("Samples", datum.asSampleOperations(), is(samples));
			assertThat("Not uploaded", datum.getUploaded(), is(nullValue()));

			assertDatumStoredEventEqualsDatum(captor.getValues().get(i), stored.get(i));
		}
	}

	@Test
	public void markUploaded() {
		final int numDatum = 5;
		final int numUploaded = 3;
		final long now = System.currentTimeMillis();
		final DatumSamples samples = samplesInstance();

		Capture<Event> captor = Capture.newInstance(CaptureType.ALL);
		eventAdmin.postEvent(EasyMock.capture(captor));
		EasyMock.expectLastCall().times(numDatum);

		replayAll();

		List<NodeDatum> stored = new ArrayList<>();
		for ( int i = 0; i < numDatum; i++ ) {
			SimpleDatum datum = SimpleDatum.nodeDatum(String.valueOf(i), Instant.ofEpochMilli(now),
					samples);
			dao.storeDatum(datum);
			stored.add(datum);
		}
		List<NodeDatum> results = dao.getDatumNotUploaded("test");
		assertThat("Results returned", results, is(notNullValue()));
		assertThat("Result count", results.size(), is(numDatum));
		final Instant uploadDate = Instant.now().truncatedTo(MILLIS).plusMillis(1000L);
		for ( int i = 0; i < numUploaded; i++ ) {
			NodeDatum datum = results.get(i);
			dao.setDatumUploaded(datum, uploadDate, "test", String.valueOf(i + 10));
		}

		// now find not uploaded again, should be just 2
		results = dao.getDatumNotUploaded("test");
		assertThat("Results returned", results, is(notNullValue()));
		assertThat("Result count decreased by number uploaded", results.size(),
				is(numDatum - numUploaded));
		for ( int i = 0; i < (numDatum - numUploaded); i++ ) {
			NodeDatum datum = results.get(i);
			assertThat("Timestamp", datum.getTimestamp().toEpochMilli(), is(now));
			assertThat("Source ID", datum.getSourceId(), is(String.valueOf(i + numUploaded)));
			assertThat("Samples", datum.asSampleOperations(), is(samples));
			assertThat("Not uploaded", datum.getUploaded(), is(nullValue()));
		}

		assertThat(captor.getValues(), hasSize(numDatum));
		for ( int i = 0; i < numDatum; i++ ) {
			assertDatumStoredEventEqualsDatum(captor.getValues().get(i), stored.get(i));
		}
	}

	@Test
	public void deleteOld() {
		final int numDatum = 5;
		final int numUploaded = 3;
		final long start = System.currentTimeMillis() - (1000 * 60 * 60 * numDatum);
		final DatumSamples samples = samplesInstance();

		Capture<Event> captor = Capture.newInstance(CaptureType.ALL);
		eventAdmin.postEvent(EasyMock.capture(captor));
		EasyMock.expectLastCall().times(numDatum);

		replayAll();

		List<NodeDatum> stored = new ArrayList<>();
		for ( int i = 0; i < numDatum; i++ ) {
			SimpleDatum datum = SimpleDatum.nodeDatum(String.valueOf(i),
					Instant.ofEpochMilli(start + (1000 * 60 * 60 * i)), samples);
			dao.storeDatum(datum);
			stored.add(datum);
		}

		// mark 3 uploaded
		List<NodeDatum> results = dao.getDatumNotUploaded("test");
		assertThat("Results returned", results, is(notNullValue()));
		assertThat("Result count", results.size(), is(numDatum));
		for ( int i = 0; i < numUploaded; i++ ) {
			NodeDatum datum = results.get(i);
			Instant uploadDate = datum.getTimestamp().plusMillis(1000L);
			dao.setDatumUploaded(datum, uploadDate, "test", String.valueOf(i + 10));
		}

		// now delete any older than 1 hour; should only delete the 3 uploaded ones
		int deleted = dao.deleteUploadedDataOlderThan(1);
		assertThat("Uploaded old delete count", deleted, is(3));

		results = dao.getDatumNotUploaded("test");
		assertThat("Results returned", results, is(notNullValue()));
		assertThat("Result count decreased by uploaded count", results.size(),
				is(numDatum - numUploaded));
		for ( int i = 0; i < (numDatum - numUploaded); i++ ) {
			NodeDatum datum = results.get(i);
			assertThat("Source ID", datum.getSourceId(), is(String.valueOf(i + numUploaded)));
			assertThat("Samples", datum.asSampleOperations(), is(samples));
			assertThat("Not uploaded", datum.getUploaded(), is(nullValue()));
		}

		assertThat(captor.getValues(), hasSize(numDatum));
		for ( int i = 0; i < numDatum; i++ ) {
			assertDatumStoredEventEqualsDatum(captor.getValues().get(i), stored.get(i));
		}
	}

	@Test
	public void update() {
		Capture<Event> captor = Capture.newInstance(CaptureType.ALL);
		eventAdmin.postEvent(EasyMock.capture(captor));
		expectLastCall().times(2); // two store, one upload

		replayAll();

		SimpleDatum datum = SimpleDatum.nodeDatum("Test", Instant.now().truncatedTo(MILLIS),
				samplesInstance());

		// insert
		dao.storeDatum(datum);

		// mark as uploaded
		dao.setDatumUploaded(datum, Instant.now().truncatedTo(MILLIS), "test", "test_id");

		// now change data and update
		MutableNodeDatum update = datum.clone();
		update.asMutableSampleOperations().addTag("foo");
		dao.storeDatum(update);

		String jdata = jdbcTemplate.queryForObject(
				"select jdata from solarnode.sn_general_node_datum where created = ? and source_id = ?",
				String.class, datum.getTimestamp().atOffset(UTC).toLocalDateTime(), datum.getSourceId());
		assertThat("jdata", jdata,
				equalTo("{\"i\":{\"watts\":231},\"a\":{\"watt_hours\":4123},\"t\":[\"foo\"]}"));

		List<NodeDatum> local = dao.getDatumNotUploaded("test");
		assertThat(local, hasSize(1));
		assertThat(local.get(0), equalTo(update));

		assertThat("Event captured", captor.getValues(), hasSize(2));
		assertDatumStoredEventEqualsDatum(captor.getValues().get(0), datum);
		assertDatumStoredEventEqualsDatum(captor.getValues().get(1), update);
	}

	@Test
	public void updateUploaded_unchangedSamples() {
		Capture<Event> captor = Capture.newInstance(CaptureType.ALL);
		eventAdmin.postEvent(EasyMock.capture(captor));
		EasyMock.expectLastCall().times(2);

		replayAll();

		SimpleDatum datum = SimpleDatum.nodeDatum("Test", Instant.now().truncatedTo(MILLIS),
				samplesInstance());

		// insert
		dao.storeDatum(datum);

		// mark as uploaded
		dao.setDatumUploaded(datum, Instant.now().truncatedTo(MILLIS), "test", "test_id");

		// now update with same data; should leave uploaded as it was
		dao.storeDatum(datum);

		String jdata = jdbcTemplate.queryForObject(
				"select jdata from solarnode.sn_general_node_datum where created = ? and source_id = ?",
				String.class, datum.getTimestamp().atOffset(UTC).toLocalDateTime(), datum.getSourceId());
		assertThat("jdata", jdata, equalTo("{\"i\":{\"watts\":231},\"a\":{\"watt_hours\":4123}}"));

		List<NodeDatum> local = dao.getDatumNotUploaded("test");
		assertThat(local, hasSize(0));

		assertThat("Event captured", captor.getValues(), hasSize(2));
		assertDatumStoredEventEqualsDatum(captor.getValues().get(0), datum);
		assertDatumStoredEventEqualsDatum(captor.getValues().get(1), datum);
	}

}
