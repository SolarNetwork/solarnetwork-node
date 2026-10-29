/* ==================================================================
 * DatumDataSourcePollManagedJobTests.java - 29 Oct 2026 6:10:00 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.job.test;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.same;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.BasicDeviceInfo;
import net.solarnetwork.domain.DeviceInfo;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.job.DatumDataSourcePollManagedJob;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.DatumMetadataService;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link DatumDataSourcePollManagedJob} class.
 *
 * @author matt
 * @version 1.0
 */
public class DatumDataSourcePollManagedJobTests {

	private static final class TestDatumDataSource extends BaseIdentifiable implements DatumDataSource {

		private final String sourceId;

		private TestDatumDataSource(String sourceId) {
			super();
			this.sourceId = sourceId;
		}

		@Override
		public Class<? extends NodeDatum> getDatumType() {
			return NodeDatum.class;
		}

		@Override
		public NodeDatum readCurrentDatum() {
			return SimpleDatum.nodeDatum(sourceId, Instant.now(), new DatumSamples());
		}

		@Override
		public String deviceInfoSourceId() {
			return sourceId;
		}

		@Override
		public DeviceInfo deviceInfo() {
			return BasicDeviceInfo.builder().withManufacturer("ACME").build();
		}

	}

	private DatumQueue datumQueue;
	private DatumMetadataService datumMetadataService;

	@Before
	public void setup() {
		datumQueue = EasyMock.createMock(DatumQueue.class);
		datumMetadataService = EasyMock.createMock(DatumMetadataService.class);
	}

	@After
	public void teardown() {
		EasyMock.verify(datumQueue, datumMetadataService);
	}

	private void replayAll() {
		EasyMock.replay(datumQueue, datumMetadataService);
	}

	private DatumDataSourcePollManagedJob newJob(String sourceId) {
		DatumDataSourcePollManagedJob job = new DatumDataSourcePollManagedJob();
		job.setDatumDataSource(new TestDatumDataSource(sourceId));
		job.setDatumQueue(new StaticOptionalService<>(datumQueue));
		job.setDatumMetadataService(new StaticOptionalService<>(datumMetadataService));
		return job;
	}

	@Test
	public void deviceInfo_publishedOncePerJob() throws Exception {
		// GIVEN
		final String sourceId = UUID.randomUUID().toString();
		final DatumDataSourcePollManagedJob job1 = newJob(sourceId);
		final DatumDataSourcePollManagedJob job2 = newJob(sourceId);

		expect(datumQueue.offer(EasyMock.anyObject(), eq(true))).andReturn(true).times(4);

		Capture<GeneralDatumMetadata> metaCaptor = Capture.newInstance(CaptureType.ALL);
		datumMetadataService.addSourceMetadata(same(sourceId), capture(metaCaptor));
		EasyMock.expectLastCall().times(2);

		// WHEN
		replayAll();
		job1.executeJobService();
		job1.executeJobService();

		// a new job, such as after a restart, publishes the device info again
		job2.executeJobService();
		job2.executeJobService();

		// THEN
		List<GeneralDatumMetadata> metas = metaCaptor.getValues();
		assertThat("Device info published once per job", metas.size(), equalTo(2));
		for ( GeneralDatumMetadata meta : metas ) {
			@SuppressWarnings("unchecked")
			Map<String, Object> info = (Map<String, Object>) meta.getPropertyInfo()
					.get(DeviceInfo.DEVICE_INFO_METADATA_KEY);
			assertThat("Device info published", info, hasEntry("manufacturer", "ACME"));
		}
	}

}
