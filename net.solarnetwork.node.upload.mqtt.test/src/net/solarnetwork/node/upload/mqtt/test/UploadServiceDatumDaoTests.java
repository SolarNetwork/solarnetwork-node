/* ==================================================================
 * UploadServiceDatumDaoTests.java - 8/06/2018 3:37:47 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.upload.mqtt.test;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.UploadService;
import net.solarnetwork.node.dao.DatumDao;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.upload.mqtt.UploadServiceDatumDao;

/**
 * Test cases for the {@link UploadServiceDatumDao} class.
 * 
 * @author matt
 * @version 1.1
 */
public class UploadServiceDatumDaoTests {

	private UploadService uploadService;
	private DatumDao<GeneralNodeDatum> delegate;
	private UploadServiceDatumDao<GeneralNodeDatum> dao;

	@SuppressWarnings("unchecked")
	@Before
	public void setup() {
		uploadService = EasyMock.createMock(UploadService.class);
		delegate = EasyMock.createMock(DatumDao.class);
		dao = new UploadServiceDatumDao<>(uploadService, delegate);
	}

	@After
	public void teardown() {
		EasyMock.verify(uploadService, delegate);
	}

	private void replayAll() {
		EasyMock.replay(uploadService, delegate);
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void delegateGetDatumType() {
		// given
		expect((Class) delegate.getDatumType()).andReturn(GeneralNodeDatum.class);

		replayAll();

		// when
		Class<? extends GeneralNodeDatum> clazz = dao.getDatumType();

		// then
		assertThat("Datum class", clazz, equalTo(GeneralNodeDatum.class));
	}

	@Test
	public void delegateDeleteUploadedDataOlderThan() {
		// given
		expect(delegate.deleteUploadedDataOlderThan(1)).andReturn(1);

		replayAll();

		// when
		int count = dao.deleteUploadedDataOlderThan(1);

		// then
		assertThat("Delete count", count, equalTo(1));
	}

	@Test
	public void delegateGetDatumNotUploaded() {
		// given
		String dest = "foobar";
		List<GeneralNodeDatum> list = Collections.emptyList();
		expect(delegate.getDatumNotUploaded(dest)).andReturn(list);

		replayAll();

		// when
		List<GeneralNodeDatum> results = dao.getDatumNotUploaded(dest);

		// then
		assertThat("Not uploaded", results, sameInstance(list));
	}

	@Test
	public void delegateSetDatumUploaded() {
		// given
		String dest = "foobar";
		Date date = new Date();
		String txId = "test.id";
		GeneralNodeDatum datum = new GeneralNodeDatum();
		delegate.setDatumUploaded(datum, date, dest, txId);

		replayAll();

		// when
		dao.setDatumUploaded(datum, date, dest, txId);
	}

	@Test
	public void storeDatumWithUploadSuccess() {
		// given
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.putInstantaneousSampleValue("foo", 1);
		String id = UUID.randomUUID().toString();
		expect(uploadService.uploadDatum(datum)).andReturn(id);

		replayAll();

		// when
		dao.storeDatum(datum);
	}

	@Test
	public void storeDatumWithUploadNoId() {
		// given
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.putInstantaneousSampleValue("foo", 1);
		expect(uploadService.uploadDatum(datum)).andReturn(null);

		// because no ID returned from UploadService, we fall back to persisting datum for later upload
		delegate.storeDatum(datum);

		replayAll();

		// when
		dao.storeDatum(datum);
	}

	@Test
	public void storeDatumWithUploadThrowsException() {
		// given
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.putInstantaneousSampleValue("foo", 1);
		RuntimeException e = new RuntimeException("boo");
		expect(uploadService.uploadDatum(datum)).andThrow(e);
		delegate.storeDatum(datum);

		replayAll();

		// when
		dao.storeDatum(datum);
	}

}
