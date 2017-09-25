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

import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;
import javax.sql.DataSource;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.domain.GeneralNodeDatumSamples;
import net.solarnetwork.node.UploadService;
import net.solarnetwork.node.dao.DatumDao;
import net.solarnetwork.node.dao.jdbc.DatabaseSetup;
import net.solarnetwork.node.dao.jdbc.general.JdbcGeneralNodeDatumDao;
import net.solarnetwork.node.domain.ACEnergyDatum;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.EnergyDatum;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link JdbcGeneralNodeDatumDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcGeneralNodeDatumDaoTest extends AbstractNodeTransactionalTest {

	@Resource(name = "dataSource")
	private DataSource dataSource;

	private JdbcGeneralNodeDatumDao dao;
	private EventAdmin eventAdmin;

	@Before
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

	private GeneralNodeDatumSamples samplesInstance() {
		GeneralNodeDatumSamples samples = new GeneralNodeDatumSamples();

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
		Capture<Event> captor = new Capture<Event>();
		eventAdmin.postEvent(EasyMock.capture(captor));

		replayAll();

		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(new Date());
		datum.setSourceId("Test");
		datum.setSamples(samplesInstance());
		dao.storeDatum(datum);

		assertThat("Event captured", captor.hasCaptured(), equalTo(true));
		Event event = captor.getValue();
		assertDatumStoredEventEqualsDatum(event, datum);
	}

	@Test
	public void insertSubclass() {
		Capture<Event> captor = new Capture<Event>();
		eventAdmin.postEvent(EasyMock.capture(captor));

		replayAll();

		GeneralNodeACEnergyDatum datum = new GeneralNodeACEnergyDatum();
		datum.setCreated(new Date());
		datum.setSourceId("Test");
		datum.setWatts(123);
		datum.setWattHourReading(12345L);
		dao.storeDatum(datum);

		assertThat("Event captured", captor.hasCaptured(), equalTo(true));
		Event event = captor.getValue();
		assertDatumEventEqualsDatum(event, DatumDao.EVENT_TOPIC_DATUM_STORED, datum, new String[] {
				ACEnergyDatum.class.getName(), EnergyDatum.class.getName(), Datum.class.getName() });
	}

	private void assertDatumStoredEventEqualsDatum(Event event, GeneralNodeDatum datum) {
		assertDatumEventEqualsDatum(event, DatumDao.EVENT_TOPIC_DATUM_STORED, datum,
				new String[] { Datum.class.getName() });
	}

	private void assertDatumEventEqualsDatum(Event event, String topic, GeneralNodeDatum datum,
			String[] datumTypes) {
		assertThat("Topic", event.getTopic(), equalTo(topic));
		assertThat("Datum type", (String) event.getProperty(Datum.DATUM_TYPE_PROPERTY),
				equalTo(datumTypes[0]));
		assertThat("Datum types", (String[]) event.getProperty(Datum.DATUM_TYPES_PROPERTY),
				arrayContaining(datumTypes));
		assertThat("Source ID", (String) event.getProperty("sourceId"), equalTo(datum.getSourceId()));
		assertThat("Created", (Long) event.getProperty("created"),
				equalTo(datum.getCreated().getTime()));
		for ( Map.Entry<String, ?> me : datum.getSamples().getSampleData().entrySet() ) {
			assertThat(me.getKey(), event.getProperty(me.getKey()), equalTo((Object) me.getValue()));
		}
		Set<String> tags = datum.getSamples().getTags();
		if ( tags != null && !tags.isEmpty() ) {
			String[] expectedTags = tags.toArray(new String[tags.size()]);
			assertThat("Tags", (String[]) event.getProperty("tags"), arrayContaining(expectedTags));
		}
	}

	private void assertDatumUploadedEventEqualsDatum(Event event, GeneralNodeDatum datum) {
		assertDatumEventEqualsDatum(event, UploadService.EVENT_TOPIC_DATUM_UPLOADED, datum,
				new String[] { Datum.class.getName() });
	}

	@Test
	public void findForUpload() {
		final int numDatum = 5;
		final long now = System.currentTimeMillis();
		final GeneralNodeDatumSamples samples = samplesInstance();

		Capture<Event> captor = new Capture<Event>(CaptureType.ALL);
		eventAdmin.postEvent(EasyMock.capture(captor));
		EasyMock.expectLastCall().times(numDatum);

		replayAll();

		for ( int i = 0; i < numDatum; i++ ) {
			GeneralNodeDatum datum = new GeneralNodeDatum();
			datum.setCreated(new Date(now));
			datum.setSourceId(String.valueOf(i));
			datum.setSamples(samples);
			dao.storeDatum(datum);
		}
		List<GeneralNodeDatum> results = dao.getDatumNotUploaded("test");
		assertThat(results, hasSize(numDatum));
		assertThat(captor.getValues(), hasSize(numDatum));

		for ( int i = 0; i < numDatum; i++ ) {
			GeneralNodeDatum datum = results.get(i);
			Assert.assertEquals(now, datum.getCreated().getTime());
			Assert.assertEquals(String.valueOf(i), datum.getSourceId());
			Assert.assertEquals(samples, datum.getSamples());
			Assert.assertNull(datum.getUploaded());

			assertDatumStoredEventEqualsDatum(captor.getValues().get(i), datum);
		}
	}

	@Test
	public void markUploaded() {
		final int numDatum = 5;
		final int numUploaded = 3;
		final long now = System.currentTimeMillis();
		final GeneralNodeDatumSamples samples = samplesInstance();

		Capture<Event> captor = new Capture<Event>(CaptureType.ALL);
		eventAdmin.postEvent(EasyMock.capture(captor));
		EasyMock.expectLastCall().times(numDatum + numUploaded); // stored + uploaded

		replayAll();

		List<GeneralNodeDatum> stored = new ArrayList<GeneralNodeDatum>();
		for ( int i = 0; i < numDatum; i++ ) {
			GeneralNodeDatum datum = new GeneralNodeDatum();
			datum.setCreated(new Date(now));
			datum.setSourceId(String.valueOf(i));
			datum.setSamples(samples);
			dao.storeDatum(datum);
			stored.add(datum);
		}
		List<GeneralNodeDatum> results = dao.getDatumNotUploaded("test");
		Assert.assertNotNull(results);
		Assert.assertEquals(numDatum, results.size());
		final Date uploadDate = new Date(System.currentTimeMillis() + 1000L);
		for ( int i = 0; i < numUploaded; i++ ) {
			GeneralNodeDatum datum = results.get(i);
			dao.setDatumUploaded(datum, uploadDate, "test", String.valueOf(i + 10));
		}

		// now find not uploaded again, should be just 2
		results = dao.getDatumNotUploaded("test");
		Assert.assertNotNull(results);
		Assert.assertEquals(numDatum - numUploaded, results.size());
		for ( int i = 0; i < (numDatum - numUploaded); i++ ) {
			GeneralNodeDatum datum = results.get(i);
			Assert.assertEquals(now, datum.getCreated().getTime());
			Assert.assertEquals(String.valueOf(i + numUploaded), datum.getSourceId());
			Assert.assertEquals(samples, datum.getSamples());
			Assert.assertNull(datum.getUploaded());
		}

		assertThat(captor.getValues(), hasSize(numDatum + numUploaded));
		for ( int i = 0; i < numDatum + numUploaded; i++ ) {
			if ( i < numDatum ) {
				assertDatumStoredEventEqualsDatum(captor.getValues().get(i), stored.get(i));
			} else {
				assertDatumUploadedEventEqualsDatum(captor.getValues().get(i), stored.get(i - numDatum));
			}
		}
	}

	@Test
	public void deleteOld() {
		final int numDatum = 5;
		final int numUploaded = 3;
		final long start = System.currentTimeMillis() - (1000 * 60 * 60 * numDatum);
		final GeneralNodeDatumSamples samples = samplesInstance();

		Capture<Event> captor = new Capture<Event>(CaptureType.ALL);
		eventAdmin.postEvent(EasyMock.capture(captor));
		EasyMock.expectLastCall().times(numDatum + numUploaded); // stored + uploaded

		replayAll();

		List<GeneralNodeDatum> stored = new ArrayList<GeneralNodeDatum>();
		for ( int i = 0; i < numDatum; i++ ) {
			GeneralNodeDatum datum = new GeneralNodeDatum();
			datum.setCreated(new Date(start + (1000 * 60 * 60 * i)));
			datum.setSourceId(String.valueOf(i));
			datum.setSamples(samples);
			dao.storeDatum(datum);
			stored.add(datum);
		}

		// mark 3 uploaded
		List<GeneralNodeDatum> results = dao.getDatumNotUploaded("test");
		Assert.assertNotNull(results);
		Assert.assertEquals(numDatum, results.size());
		for ( int i = 0; i < numUploaded; i++ ) {
			GeneralNodeDatum datum = results.get(i);
			Date uploadDate = new Date(datum.getCreated().getTime() + 1000L);
			dao.setDatumUploaded(datum, uploadDate, "test", String.valueOf(i + 10));
		}

		// now delete any older than 1 hour; should only delete the 3 uploaded ones
		int deleted = dao.deleteUploadedDataOlderThan(1);
		Assert.assertEquals(3, deleted);

		results = dao.getDatumNotUploaded("test");
		Assert.assertNotNull(results);
		Assert.assertEquals(numDatum - numUploaded, results.size());
		for ( int i = 0; i < (numDatum - numUploaded); i++ ) {
			GeneralNodeDatum datum = results.get(i);
			Assert.assertEquals(String.valueOf(i + numUploaded), datum.getSourceId());
			Assert.assertEquals(samples, datum.getSamples());
			Assert.assertNull(datum.getUploaded());
		}

		assertThat(captor.getValues(), hasSize(numDatum + numUploaded));
		for ( int i = 0; i < numDatum + numUploaded; i++ ) {
			if ( i < numDatum ) {
				assertDatumStoredEventEqualsDatum(captor.getValues().get(i), stored.get(i));
			} else {
				assertDatumUploadedEventEqualsDatum(captor.getValues().get(i), stored.get(i - numDatum));
			}
		}
	}

	@Test
	public void update() {
		Capture<Event> captor = new Capture<Event>(CaptureType.ALL);
		eventAdmin.postEvent(EasyMock.capture(captor));
		expectLastCall().times(3); // two store, one upload

		replayAll();

		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(new Date());
		datum.setSourceId("Test");
		datum.setSamples(samplesInstance());

		// insert
		dao.storeDatum(datum);

		// mark as uploaded
		dao.setDatumUploaded(datum, new Date(), "test", "test_id");

		// now change data and update
		GeneralNodeDatum update = new GeneralNodeDatum();
		update.setCreated(datum.getCreated());
		update.setSourceId(datum.getSourceId());
		update.setSamples(samplesInstance());
		update.getSamples().addTag("foo");
		dao.storeDatum(update);

		String jdata = jdbcTemplate.queryForObject(
				"select jdata from solarnode.sn_general_node_datum where created = ? and source_id = ?",
				new Object[] { new Timestamp(datum.getCreated().getTime()), datum.getSourceId() },
				String.class);
		assertThat("jdata", jdata,
				equalTo("{\"i\":{\"watts\":231},\"a\":{\"watt_hours\":4123},\"t\":[\"foo\"]}"));

		List<GeneralNodeDatum> local = dao.getDatumNotUploaded("test");
		assertThat(local, hasSize(1));
		assertThat(local.get(0), equalTo(update));

		assertThat("Event captured", captor.getValues(), hasSize(3));
		assertDatumStoredEventEqualsDatum(captor.getValues().get(0), datum);
		assertDatumUploadedEventEqualsDatum(captor.getValues().get(1), datum);
		assertDatumStoredEventEqualsDatum(captor.getValues().get(2), update);
	}

	@Test
	public void updateUnchangedSamples() {
		Capture<Event> captor = new Capture<Event>(CaptureType.ALL);
		eventAdmin.postEvent(EasyMock.capture(captor));
		EasyMock.expectLastCall().times(2);

		replayAll();

		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(new Date());
		datum.setSourceId("Test");
		datum.setSamples(samplesInstance());

		// insert
		dao.storeDatum(datum);

		// mark as uploaded
		dao.setDatumUploaded(datum, new Date(), "test", "test_id");

		// now update
		dao.storeDatum(datum);

		String jdata = jdbcTemplate.queryForObject(
				"select jdata from solarnode.sn_general_node_datum where created = ? and source_id = ?",
				new Object[] { new Timestamp(datum.getCreated().getTime()), datum.getSourceId() },
				String.class);
		assertThat("jdata", jdata, equalTo("{\"i\":{\"watts\":231},\"a\":{\"watt_hours\":4123}}"));

		List<GeneralNodeDatum> local = dao.getDatumNotUploaded("test");
		assertThat(local, hasSize(0));

		assertThat("Event captured", captor.getValues(), hasSize(2));
		assertDatumStoredEventEqualsDatum(captor.getValues().get(0), datum);
		assertDatumUploadedEventEqualsDatum(captor.getValues().get(1), datum);
	}

}
