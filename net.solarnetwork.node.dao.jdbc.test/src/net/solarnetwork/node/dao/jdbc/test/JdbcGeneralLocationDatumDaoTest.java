/* ==================================================================
 * JdbcGeneralLocationDatumDaoTest.java - Oct 20, 2014 12:22:20 PM
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

package net.solarnetwork.node.dao.jdbc.test;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.sql.DataSource;
import net.solarnetwork.domain.GeneralLocationDatumSamples;
import net.solarnetwork.node.dao.jdbc.DatabaseSetup;
import net.solarnetwork.node.dao.jdbc.general.JdbcGeneralLocationDatumDao;
import net.solarnetwork.node.domain.GeneralLocationDatum;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test cases for the {@link JdbcGeneralLocationDatumDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcGeneralLocationDatumDaoTest extends AbstractNodeTransactionalTest {

	@Resource(name = "dataSource")
	private DataSource dataSource;

	private JdbcGeneralLocationDatumDao dao;

	@Before
	public void setup() {
		DatabaseSetup setup = new DatabaseSetup();
		setup.setDataSource(dataSource);
		setup.init();

		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);

		dao = new JdbcGeneralLocationDatumDao();
		dao.setDataSource(dataSource);
		dao.setObjectMapper(mapper);
		dao.init();
	}

	private GeneralLocationDatumSamples samplesInstance() {
		GeneralLocationDatumSamples samples = new GeneralLocationDatumSamples();

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
		GeneralLocationDatum datum = new GeneralLocationDatum();
		datum.setCreated(new Date());
		datum.setLocationId(TEST_LOC_ID);
		datum.setSourceId("Test");
		datum.setSamples(samplesInstance());
		dao.storeDatum(datum);
	}

	@Test
	public void findForUpload() {
		final int numDatum = 5;
		final long now = System.currentTimeMillis();
		final GeneralLocationDatumSamples samples = samplesInstance();
		for ( int i = 0; i < numDatum; i++ ) {
			GeneralLocationDatum datum = new GeneralLocationDatum();
			datum.setCreated(new Date(now));
			datum.setLocationId(TEST_LOC_ID);
			datum.setSourceId(String.valueOf(i));
			datum.setSamples(samples);
			dao.storeDatum(datum);
		}
		List<GeneralLocationDatum> results = dao.getDatumNotUploaded("test");
		Assert.assertNotNull(results);
		Assert.assertEquals(numDatum, results.size());
		for ( int i = 0; i < numDatum; i++ ) {
			GeneralLocationDatum datum = results.get(i);
			Assert.assertEquals(now, datum.getCreated().getTime());
			Assert.assertEquals(String.valueOf(i), datum.getSourceId());
			Assert.assertEquals(samples, datum.getSamples());
			Assert.assertNull(datum.getUploaded());
		}
	}

	@Test
	public void markUploaded() {
		final int numDatum = 5;
		final long now = System.currentTimeMillis();
		final GeneralLocationDatumSamples samples = samplesInstance();
		for ( int i = 0; i < numDatum; i++ ) {
			GeneralLocationDatum datum = new GeneralLocationDatum();
			datum.setCreated(new Date(now));
			datum.setLocationId(TEST_LOC_ID);
			datum.setSourceId(String.valueOf(i));
			datum.setSamples(samples);
			dao.storeDatum(datum);
		}
		List<GeneralLocationDatum> results = dao.getDatumNotUploaded("test");
		Assert.assertNotNull(results);
		Assert.assertEquals(numDatum, results.size());
		final int numUploaded = 3;
		final Date uploadDate = new Date(System.currentTimeMillis() + 1000L);
		for ( int i = 0; i < numUploaded; i++ ) {
			GeneralLocationDatum datum = results.get(i);
			dao.setDatumUploaded(datum, uploadDate, "test", String.valueOf(i + 10));
		}

		// now find not uploaded again, should be just 2
		results = dao.getDatumNotUploaded("test");
		Assert.assertNotNull(results);
		Assert.assertEquals(numDatum - numUploaded, results.size());
		for ( int i = 0; i < (numDatum - numUploaded); i++ ) {
			GeneralLocationDatum datum = results.get(i);
			Assert.assertEquals(now, datum.getCreated().getTime());
			Assert.assertEquals(String.valueOf(i + numUploaded), datum.getSourceId());
			Assert.assertEquals(samples, datum.getSamples());
			Assert.assertNull(datum.getUploaded());
		}
	}

	@Test
	public void deleteOld() {
		final int numDatum = 5;
		final long start = System.currentTimeMillis() - (1000 * 60 * 60 * numDatum);
		final GeneralLocationDatumSamples samples = samplesInstance();
		for ( int i = 0; i < numDatum; i++ ) {
			GeneralLocationDatum datum = new GeneralLocationDatum();
			datum.setCreated(new Date(start + (1000 * 60 * 60 * i)));
			datum.setLocationId(TEST_LOC_ID);
			datum.setSourceId(String.valueOf(i));
			datum.setSamples(samples);
			dao.storeDatum(datum);
		}

		// mark 3 uploaded
		List<GeneralLocationDatum> results = dao.getDatumNotUploaded("test");
		Assert.assertNotNull(results);
		Assert.assertEquals(numDatum, results.size());
		final int numUploaded = 3;
		for ( int i = 0; i < numUploaded; i++ ) {
			GeneralLocationDatum datum = results.get(i);
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
			GeneralLocationDatum datum = results.get(i);
			Assert.assertEquals(String.valueOf(i + numUploaded), datum.getSourceId());
			Assert.assertEquals(samples, datum.getSamples());
			Assert.assertNull(datum.getUploaded());
		}
	}

	@Test
	public void update() {
		GeneralLocationDatum datum = new GeneralLocationDatum();
		datum.setCreated(new Date());
		datum.setLocationId(TEST_LOC_ID);
		datum.setSourceId("Test");
		datum.setSamples(samplesInstance());

		// insert
		dao.storeDatum(datum);

		// mark as uploaded
		dao.setDatumUploaded(datum, new Date(), "test", "test_id");

		// now change data and update
		datum.getSamples().addTag("foo");
		dao.storeDatum(datum);

		String jdata = jdbcTemplate.queryForObject(
				"select jdata from solarnode.sn_general_loc_datum where created = ? and source_id = ?",
				new Object[] { new Timestamp(datum.getCreated().getTime()), datum.getSourceId() },
				String.class);
		Assert.assertEquals("{\"i\":{\"watts\":231},\"a\":{\"watt_hours\":4123},\"t\":[\"foo\"]}", jdata);

		List<GeneralLocationDatum> local = dao.getDatumNotUploaded("test");
		Assert.assertNotNull(local);
		Assert.assertEquals(1, local.size());
		Assert.assertEquals(datum, local.get(0));
	}

	@Test
	public void updateUnchangedSamples() {
		GeneralLocationDatum datum = new GeneralLocationDatum();
		datum.setCreated(new Date());
		datum.setLocationId(TEST_LOC_ID);
		datum.setSourceId("Test");
		datum.setSamples(samplesInstance());

		// insert
		dao.storeDatum(datum);

		// mark as uploaded
		dao.setDatumUploaded(datum, new Date(), "test", "test_id");

		// now update
		dao.storeDatum(datum);

		String jdata = jdbcTemplate.queryForObject(
				"select jdata from solarnode.sn_general_loc_datum where created = ? and source_id = ?",
				new Object[] { new Timestamp(datum.getCreated().getTime()), datum.getSourceId() },
				String.class);
		Assert.assertEquals("{\"i\":{\"watts\":231},\"a\":{\"watt_hours\":4123}}", jdata);

		// the datum should still be marked as "uploaded"
		List<GeneralLocationDatum> local = dao.getDatumNotUploaded("test");
		Assert.assertNotNull(local);
		Assert.assertEquals(0, local.size());
	}

}
