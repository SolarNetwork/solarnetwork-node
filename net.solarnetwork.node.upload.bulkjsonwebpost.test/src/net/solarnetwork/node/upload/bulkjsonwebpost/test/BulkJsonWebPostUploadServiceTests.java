/* ==================================================================
 * BulkJsonWebPostUploadServiceTests.java - 8/08/2017 7:44:39 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.upload.bulkjsonwebpost.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.node.BulkUploadResult;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.GeneralDatumSamplesTransformer;
import net.solarnetwork.node.domain.GeneralNodeEnergyDatum;
import net.solarnetwork.node.upload.bulkjsonwebpost.BulkJsonWebPostUploadService;
import net.solarnetwork.node.upload.bulkjsonwebpost.DatumSerializer;
import net.solarnetwork.node.upload.bulkjsonwebpost.GeneralNodeDatumSerializer;
import net.solarnetwork.node.upload.bulkjsonwebpost.InstructionSerializer;
import net.solarnetwork.node.upload.bulkjsonwebpost.NodeControlInfoSerializer;
import net.solarnetwork.util.ObjectMapperFactoryBean;

/**
 * Unit tests for the {@link BulkJsonWebPostUploadService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class BulkJsonWebPostUploadServiceTests extends AbstractHttpTests {

	private static final Long TEST_NODE_ID = 123L;
	private static final String TEST_SOURCE_ID = "test-source";

	private GeneralNodeDatumSerializer generalNodeDatumSerializer;
	private BulkJsonWebPostUploadService service;

	private TestIdentityService identityService;

	@Before
	public void setupService() throws Exception {
		generalNodeDatumSerializer = new GeneralNodeDatumSerializer();

		@SuppressWarnings("unchecked")
		List<JsonSerializer<? extends Object>> serializers = Arrays.asList(
				(JsonSerializer<?>) generalNodeDatumSerializer, new DatumSerializer(),
				new InstructionSerializer(), new NodeControlInfoSerializer());

		ObjectMapperFactoryBean omFactory = new ObjectMapperFactoryBean();
		omFactory.setSerializers(serializers);
		ObjectMapper objectMapper = omFactory.getObject();

		identityService = new TestIdentityService(TEST_NODE_ID, getHttpServerPort());

		service = new BulkJsonWebPostUploadService();
		service.setObjectMapper(objectMapper);
		service.setIdentityService(identityService);
		service.setUrl("/bulkupload");
	}

	@Test
	public void uploadEmptyList() throws Exception {
		TestBulkUploadHttpHandler handler = new TestBulkUploadHttpHandler() {

			@Override
			protected void handleJsonPost(HttpServletRequest request, HttpServletResponse response,
					String json) {
				// nothing
			}

		};
		getHttpServer().addHandler(handler);

		List<Datum> data = new ArrayList<Datum>();
		List<BulkUploadResult> result = service.uploadBulkDatum(data);

		assertNotNull(result);
		assertEquals(0, result.size());
		assertFalse("No network request made", handler.isHandled());
	}

	/*-
	 * <pre>
	 * {
	 * 	"success" : true,
	 *  "message" : "some message",
	 * 	"data" : {
	 * 		"datum" : [
	 * 			{ "id" : "abc" ... },
	 * 			...
	 * 		],
	 * 		"instructions" : [
	 * 
	 * 		]
	 * }
	 * </pre>
	 
	 */

	@Test
	public void uploadSingleDatum() throws Exception {
		final Date now = new Date();

		TestBulkUploadHttpHandler handler = new TestBulkUploadHttpHandler() {

			@Override
			protected void handleJsonPost(HttpServletRequest request, HttpServletResponse response,
					String json) throws Exception {
				JSONAssert.assertEquals(
						"[{\"created\":" + now.getTime() + ",\"sourceId\":\"" + TEST_SOURCE_ID
								+ "\",\"samples\":{\"i\":{\"watts\":1},\"a\":{\"wattHours\":2}}}]",
						json, true);

				respondWithJsonString(response, true,
						"{\"success\":true,\"data\":{\"datum\":[{\"created\":" + now.getTime()
								+ ",\"sourceId\":\"" + TEST_SOURCE_ID + "\"" + "}]}}");
			}

		};
		getHttpServer().addHandler(handler);

		List<Datum> data = new ArrayList<Datum>();
		GeneralNodeEnergyDatum d = new GeneralNodeEnergyDatum();
		d.setCreated(now);
		d.setSourceId(TEST_SOURCE_ID);
		d.setWatts(1);
		d.setWattHourReading(2L);
		data.add(d);

		List<BulkUploadResult> result = service.uploadBulkDatum(data);

		assertNotNull(result);
		assertEquals(1, result.size());

		BulkUploadResult datumResult = result.get(0);
		Assert.assertNull(datumResult.getId());
		assertEquals(d, datumResult.getDatum());
	}

	@Test
	public void uploadSingleDatumSampleFilteredOut() throws Exception {
		GeneralDatumSamplesTransformer xform = new GeneralDatumSamplesTransformer() {

			@Override
			public GeneralDatumSamples transformSamples(Datum datum, GeneralDatumSamples samples) {
				return null;
			}
		};

		generalNodeDatumSerializer.setSampleTransformers(Collections.singletonList(xform));

		TestBulkUploadHttpHandler handler = new TestBulkUploadHttpHandler() {

			@Override
			protected void handleJsonPost(HttpServletRequest request, HttpServletResponse response,
					String json) throws Exception {
				JSONAssert.assertEquals("[]", json, true);

				respondWithJsonString(response, true, "{\"success\":true,\"data\":{\"datum\":[]}}");
			}

		};
		getHttpServer().addHandler(handler);

		List<Datum> data = new ArrayList<Datum>();
		GeneralNodeEnergyDatum d = new GeneralNodeEnergyDatum();
		d.setCreated(new Date());
		d.setSourceId(TEST_SOURCE_ID);
		d.setWatts(1);
		d.setWattHourReading(2L);
		data.add(d);

		List<BulkUploadResult> result = service.uploadBulkDatum(data);

		assertNotNull(result);
		assertEquals(1, result.size());

		BulkUploadResult datumResult = result.get(0);
		Assert.assertNull(datumResult.getId());
		assertEquals(data.get(0), datumResult.getDatum());

		assertTrue("Network request made", handler.isHandled());
	}

	@Test
	public void uploadMultiDatumSampleFilteredOut() throws Exception {
		GeneralDatumSamplesTransformer xform = new GeneralDatumSamplesTransformer() {

			@Override
			public GeneralDatumSamples transformSamples(Datum datum, GeneralDatumSamples samples) {
				return ("B".equals(datum.getSourceId()) ? null : samples);
			}
		};

		generalNodeDatumSerializer.setSampleTransformers(Collections.singletonList(xform));

		final Date now = new Date();
		final Date datumDate = new Date(now.getTime() - 1000);

		TestBulkUploadHttpHandler handler = new TestBulkUploadHttpHandler() {

			// we expect to receive just 2 datum in request, and respond with 2 datum results

			@Override
			protected void handleJsonPost(HttpServletRequest request, HttpServletResponse response,
					String json) throws Exception {
				JSONAssert.assertEquals(
						"[" + "{\"created\":" + datumDate.getTime()
								+ ",\"sourceId\":\"A\",\"samples\":{\"i\":{\"watts\":1}}},"
								+ "{\"created\":" + datumDate.getTime()
								+ ",\"sourceId\":\"C\",\"samples\":{\"i\":{\"watts\":1}}}" + "]",
						json, true);

				respondWithJsonString(response, true,
						"{\"success\":true,\"data\":{\"datum\":[" + "{\"created\":" + datumDate.getTime()
								+ ",\"sourceId\":\"A\"}," + "{\"created\":" + datumDate.getTime()
								+ ",\"sourceId\":\"C\"}" + "]}}");
			}

		};
		getHttpServer().addHandler(handler);

		List<Datum> data = new ArrayList<Datum>();
		GeneralNodeEnergyDatum d = new GeneralNodeEnergyDatum();
		d.setCreated(datumDate);
		d.setSourceId("A");
		d.setWatts(1);
		data.add(d);

		d = new GeneralNodeEnergyDatum();
		d.setCreated(datumDate);
		d.setSourceId("B");
		d.setWatts(1);
		data.add(d);

		d = new GeneralNodeEnergyDatum();
		d.setCreated(datumDate);
		d.setSourceId("C");
		d.setWatts(1);
		data.add(d);

		// when we upload datum that get filtered out, we still want to treat the filtered out
		// datum as "uploaded" so it is marked as such and eventually deleted from the local db

		List<BulkUploadResult> result = service.uploadBulkDatum(data);
		assertNotNull(result);
		assertEquals("All 3 datum uploaded, even though B skipped", 3, result.size());

		for ( int i = 0; i < 3; i++ ) {
			BulkUploadResult datumResult = result.get(i);
			Assert.assertNull(datumResult.getId());
			assertEquals(data.get(i), datumResult.getDatum());
		}
	}

}
