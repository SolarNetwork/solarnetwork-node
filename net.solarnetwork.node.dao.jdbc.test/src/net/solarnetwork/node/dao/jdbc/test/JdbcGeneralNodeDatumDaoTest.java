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

package net.solarnetwork.node.dao.jdbc.test;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.sql.DataSource;
import net.solarnetwork.domain.GeneralNodeDatumSamples;
import net.solarnetwork.node.dao.jdbc.DatabaseSetup;
import net.solarnetwork.node.dao.jdbc.general.JdbcGeneralNodeDatumDao;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

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

	@Before
	public void setup() {
		DatabaseSetup setup = new DatabaseSetup();
		setup.setDataSource(dataSource);
		setup.init();

		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);

		dao = new JdbcGeneralNodeDatumDao();
		dao.setDataSource(dataSource);
		dao.setObjectMapper(mapper);
		dao.init();
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
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(new Date());
		datum.setSourceId("Test");
		datum.setSamples(samplesInstance());
		dao.storeDatum(datum);
	}

	@Test
	public void findForUpload() {
		final int numDatum = 5;
		final long now = System.currentTimeMillis();
		final GeneralNodeDatumSamples samples = samplesInstance();
		for ( int i = 0; i < numDatum; i++ ) {
			GeneralNodeDatum datum = new GeneralNodeDatum();
			datum.setCreated(new Date(now));
			datum.setSourceId(String.valueOf(i));
			datum.setSamples(samples);
			dao.storeDatum(datum);
		}
		List<GeneralNodeDatum> results = dao.getDatumNotUploaded("test");
		Assert.assertNotNull(results);
		Assert.assertEquals(numDatum, results.size());
		for ( int i = 0; i < numDatum; i++ ) {
			GeneralNodeDatum datum = results.get(i);
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
		final GeneralNodeDatumSamples samples = samplesInstance();
		for ( int i = 0; i < numDatum; i++ ) {
			GeneralNodeDatum datum = new GeneralNodeDatum();
			datum.setCreated(new Date(now));
			datum.setSourceId(String.valueOf(i));
			datum.setSamples(samples);
			dao.storeDatum(datum);
		}
		List<GeneralNodeDatum> results = dao.getDatumNotUploaded("test");
		Assert.assertNotNull(results);
		Assert.assertEquals(numDatum, results.size());
		final int numUploaded = 3;
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
	}

	@Test
	public void deleteOld() {
		final int numDatum = 5;
		final long start = System.currentTimeMillis() - (1000 * 60 * 60 * numDatum);
		final GeneralNodeDatumSamples samples = samplesInstance();
		for ( int i = 0; i < numDatum; i++ ) {
			GeneralNodeDatum datum = new GeneralNodeDatum();
			datum.setCreated(new Date(start + (1000 * 60 * 60 * i)));
			datum.setSourceId(String.valueOf(i));
			datum.setSamples(samples);
			dao.storeDatum(datum);
		}

		// mark 3 uploaded
		List<GeneralNodeDatum> results = dao.getDatumNotUploaded("test");
		Assert.assertNotNull(results);
		Assert.assertEquals(numDatum, results.size());
		final int numUploaded = 3;
		final Date uploadDate = new Date(System.currentTimeMillis() + 1000L);
		for ( int i = 0; i < numUploaded; i++ ) {
			GeneralNodeDatum datum = results.get(i);
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
	}

}
