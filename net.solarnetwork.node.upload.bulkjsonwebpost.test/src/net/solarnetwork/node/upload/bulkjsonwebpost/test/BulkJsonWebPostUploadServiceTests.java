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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.util.DigestUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.node.BulkUploadResult;
import net.solarnetwork.node.UploadService;
import net.solarnetwork.node.domain.BaseDatum;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.GeneralDatumSamplesTransformer;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.domain.GeneralNodeEnergyDatum;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.ReactorService;
import net.solarnetwork.node.reactor.support.BasicInstruction;
import net.solarnetwork.node.reactor.support.BasicInstructionStatus;
import net.solarnetwork.node.upload.bulkjsonwebpost.BulkJsonWebPostUploadService;
import net.solarnetwork.node.upload.bulkjsonwebpost.DatumSerializer;
import net.solarnetwork.node.upload.bulkjsonwebpost.GeneralNodeDatumSerializer;
import net.solarnetwork.node.upload.bulkjsonwebpost.InstructionSerializer;
import net.solarnetwork.node.upload.bulkjsonwebpost.NodeControlInfoSerializer;
import net.solarnetwork.util.ObjectMapperFactoryBean;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Unit tests for the {@link BulkJsonWebPostUploadService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class BulkJsonWebPostUploadServiceTests extends AbstractHttpTests {

	private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = ISODateTimeFormat.dateTime()
			.withZoneUTC();

	private static final Long TEST_NODE_ID = 123L;
	private static final String TEST_SOURCE_ID = "test-source";

	private GeneralNodeDatumSerializer generalNodeDatumSerializer;
	private BulkJsonWebPostUploadService service;

	private EventAdmin eventAdmin;
	private ReactorService reactorService;
	private TestIdentityService identityService;

	@Before
	public void setupService() throws Exception {
		eventAdmin = EasyMock.createMock(EventAdmin.class);
		reactorService = EasyMock.createMock(ReactorService.class);
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
		service.setReactorService(new StaticOptionalService<ReactorService>(reactorService));
		service.setEventAdmin(new StaticOptionalService<EventAdmin>(eventAdmin));
	}

	@After
	public void finish() {
		EasyMock.verify(eventAdmin, reactorService);
	}

	private void replayAll() {
		EasyMock.replay(eventAdmin, reactorService);
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

		replayAll();

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

	private static String snTimestampString(Date date) {
		return ISO_DATE_TIME_FORMATTER.print(date.getTime()).replace('T', ' ');
	}

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
						"{\"success\":true,\"data\":{\"datum\":[{\"id\":\"abc123\",\"created\":\""
								+ snTimestampString(now) + "\",\"sourceId\":\"" + TEST_SOURCE_ID + "\""
								+ "}]}}");
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

		Capture<Event> eventCaptor = new Capture<Event>(CaptureType.ALL);
		eventAdmin.postEvent(EasyMock.capture(eventCaptor));

		replayAll();

		List<BulkUploadResult> result = service.uploadBulkDatum(data);

		assertNotNull(result);
		assertEquals(1, result.size());

		BulkUploadResult datumResult = result.get(0);
		assertEquals(datumResult.getId(), "abc123");
		assertEquals(d, datumResult.getDatum());

		assertThat("Event count", eventCaptor.getValues(), hasSize(1));
		Event event = eventCaptor.getValue();
		assertDatumUploadEventEqualsDatum(event, d);
	}

	private String tid(Datum datum) {
		return DigestUtils.md5DigestAsHex(
				String.format("%tQ;%s", datum.getCreated(), datum.getSourceId()).getBytes());
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

		replayAll();

		List<BulkUploadResult> result = service.uploadBulkDatum(data);

		assertNotNull(result);
		assertEquals(1, result.size());

		BulkUploadResult datumResult = result.get(0);
		assertEquals(datumResult.getId(), tid(d));
		assertEquals(data.get(0), datumResult.getDatum());

		assertTrue("Network request made", handler.isHandled());
	}

	@Test
	public void uploadMultiDatumFirstSampleFilteredOut() throws Exception {
		GeneralDatumSamplesTransformer xform = new GeneralDatumSamplesTransformer() {

			@Override
			public GeneralDatumSamples transformSamples(Datum datum, GeneralDatumSamples samples) {
				return ("A".equals(datum.getSourceId()) ? null : samples);
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
								+ ",\"sourceId\":\"B\",\"samples\":{\"i\":{\"watts\":1}}},"
								+ "{\"created\":" + datumDate.getTime()
								+ ",\"sourceId\":\"C\",\"samples\":{\"i\":{\"watts\":1}}}" + "]",
						json, true);

				respondWithJsonString(response, true,
						"{\"success\":true,\"data\":{\"datum\":[" + "{\"id\":\"abc123\",\"created\":\""
								+ snTimestampString(datumDate)
								+ "\",\"sourceId\":\"B\"},{\"id\":\"def123\",\"created\":\""
								+ snTimestampString(datumDate) + "\",\"sourceId\":\"C\"}]}}");
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

		Capture<Event> eventCaptor = new Capture<Event>(CaptureType.ALL);
		eventAdmin.postEvent(EasyMock.capture(eventCaptor));
		EasyMock.expectLastCall().times(2);

		replayAll();

		// when we upload datum that get filtered out, we still want to treat the filtered out
		// datum as "uploaded" so it is marked as such and eventually deleted from the local db

		List<BulkUploadResult> result = service.uploadBulkDatum(data);
		assertNotNull(result);
		assertEquals("All 3 datum uploaded, even though A skipped", 3, result.size());

		String[] expectedTids = new String[] { tid(data.get(0)), "abc123", "def123" };

		for ( int i = 0; i < 3; i++ ) {
			BulkUploadResult datumResult = result.get(i);
			assertEquals(datumResult.getId(), expectedTids[i]);
			assertEquals(data.get(i), datumResult.getDatum());
		}

		assertThat("Event count", eventCaptor.getValues(), hasSize(2));
		assertDatumUploadEventEqualsDatum(eventCaptor.getValues().get(0),
				(GeneralNodeEnergyDatum) data.get(1));
		assertDatumUploadEventEqualsDatum(eventCaptor.getValues().get(1),
				(GeneralNodeEnergyDatum) data.get(2));
	}

	@Test
	public void uploadMultiDatumMiddleSampleFilteredOut() throws Exception {
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
						"{\"success\":true,\"data\":{\"datum\":[" + "{\"id\":\"abc123\",\"created\":\""
								+ snTimestampString(datumDate)
								+ "\",\"sourceId\":\"A\"},{\"id\":\"def123\",\"created\":\""
								+ snTimestampString(datumDate) + "\",\"sourceId\":\"C\"}]}}");
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

		Capture<Event> eventCaptor = new Capture<Event>(CaptureType.ALL);
		eventAdmin.postEvent(EasyMock.capture(eventCaptor));
		EasyMock.expectLastCall().times(2);

		replayAll();

		// when we upload datum that get filtered out, we still want to treat the filtered out
		// datum as "uploaded" so it is marked as such and eventually deleted from the local db

		List<BulkUploadResult> result = service.uploadBulkDatum(data);
		assertNotNull(result);
		assertEquals("All 3 datum uploaded, even though B skipped", 3, result.size());

		String[] expectedTids = new String[] { "abc123", tid(data.get(1)), "def123" };

		for ( int i = 0; i < 3; i++ ) {
			BulkUploadResult datumResult = result.get(i);
			assertEquals(datumResult.getId(), expectedTids[i]);
			assertEquals(data.get(i), datumResult.getDatum());
		}

		assertThat("Event count", eventCaptor.getValues(), hasSize(2));
		assertDatumUploadEventEqualsDatum(eventCaptor.getValues().get(0),
				(GeneralNodeEnergyDatum) data.get(0));
		assertDatumUploadEventEqualsDatum(eventCaptor.getValues().get(1),
				(GeneralNodeEnergyDatum) data.get(2));
	}

	@Test
	public void uploadMultiDatumLastSampleFilteredOut() throws Exception {
		GeneralDatumSamplesTransformer xform = new GeneralDatumSamplesTransformer() {

			@Override
			public GeneralDatumSamples transformSamples(Datum datum, GeneralDatumSamples samples) {
				return ("C".equals(datum.getSourceId()) ? null : samples);
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
								+ ",\"sourceId\":\"B\",\"samples\":{\"i\":{\"watts\":1}}}" + "]",
						json, true);

				respondWithJsonString(response, true,
						"{\"success\":true,\"data\":{\"datum\":[" + "{\"id\":\"abc123\",\"created\":\""
								+ snTimestampString(datumDate)
								+ "\",\"sourceId\":\"A\"},{\"id\":\"def123\",\"created\":\""
								+ snTimestampString(datumDate) + "\",\"sourceId\":\"B\"}]}}");
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

		Capture<Event> eventCaptor = new Capture<Event>(CaptureType.ALL);
		eventAdmin.postEvent(EasyMock.capture(eventCaptor));
		EasyMock.expectLastCall().times(2);

		replayAll();

		// when we upload datum that get filtered out, we still want to treat the filtered out
		// datum as "uploaded" so it is marked as such and eventually deleted from the local db

		List<BulkUploadResult> result = service.uploadBulkDatum(data);
		assertNotNull(result);
		assertEquals("All 3 datum uploaded, even though C skipped", 3, result.size());

		String[] expectedTids = new String[] { "abc123", "def123", tid(data.get(2)) };

		for ( int i = 0; i < 3; i++ ) {
			BulkUploadResult datumResult = result.get(i);
			assertThat(datumResult.getId(), equalTo(expectedTids[i]));
			assertThat(data.get(i), equalTo(datumResult.getDatum()));
		}

		assertThat("Event count", eventCaptor.getValues(), hasSize(2));
		assertDatumUploadEventEqualsDatum(eventCaptor.getValues().get(0),
				(GeneralNodeEnergyDatum) data.get(0));
		assertDatumUploadEventEqualsDatum(eventCaptor.getValues().get(1),
				(GeneralNodeEnergyDatum) data.get(1));
	}

	@Test
	public void uploadMultiDatumSampleAllFilteredOut() throws Exception {
		GeneralDatumSamplesTransformer xform = new GeneralDatumSamplesTransformer() {

			@Override
			public GeneralDatumSamples transformSamples(Datum datum, GeneralDatumSamples samples) {
				return null;
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
				JSONAssert.assertEquals("[]", json, true);

				respondWithJsonString(response, true, "{\"success\":true,\"data\":{}}");
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

		replayAll();

		// when we upload datum that get filtered out, we still want to treat the filtered out
		// datum as "uploaded" so it is marked as such and eventually deleted from the local db

		List<BulkUploadResult> result = service.uploadBulkDatum(data);
		assertNotNull(result);
		assertEquals("All 3 datum uploaded, even though all skipped", 3, result.size());

		for ( int i = 0; i < 3; i++ ) {
			BulkUploadResult datumResult = result.get(i);
			assertEquals(datumResult.getId(), tid(data.get(i)));
			assertEquals(data.get(i), datumResult.getDatum());
		}
	}

	private void assertDatumUploadEventEqualsDatum(Event event, GeneralNodeDatum datum) {
		String[] datumTypes = BaseDatum.getDatumTypes(datum.getClass());
		assertThat("Topic", event.getTopic(), equalTo(UploadService.EVENT_TOPIC_DATUM_UPLOADED));
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

	@Test
	public void acknowledgeSingleInstruction() throws Exception {
		final Date now = new Date();

		TestBulkUploadHttpHandler handler = new TestBulkUploadHttpHandler() {

			@Override
			protected void handleJsonPost(HttpServletRequest request, HttpServletResponse response,
					String json) throws Exception {
				JSONAssert.assertEquals(
						"[{\"__type__\":\"InstructionStatus\",\"id\":\"123\",\"instructionId\":\"abc\",\"topic\":\"test\",\"status\":\"Completed\"}]",
						json, true);

				respondWithJsonString(response, true,
						"{\"success\":true,\"data\":{\"datum\":[{\"id\":\"abc\"}]}}");
			}

		};
		getHttpServer().addHandler(handler);

		replayAll();

		List<Instruction> data = new ArrayList<Instruction>();
		BasicInstruction instr = new BasicInstruction(123L, "test", now, "abc", "def",
				new BasicInstructionStatus(123L, InstructionState.Completed, now));
		data.add(instr);

		service.acknowledgeInstructions(data);
	}

	@Test
	public void processInstruction() throws Exception {
		final Date now = new Date();

		TestBulkUploadHttpHandler handler = new TestBulkUploadHttpHandler() {

			@Override
			protected void handleJsonPost(HttpServletRequest request, HttpServletResponse response,
					String json) throws Exception {
				JSONAssert.assertEquals("[{\"created\":" + now.getTime() + ",\"sourceId\":\""
						+ TEST_SOURCE_ID + "\",\"samples\":{\"i\":{\"watts\":1}}}]", json, true);

				respondWithJsonString(response, true,
						"{\"success\":true,\"data\":{\"datum\":[{\"created\":\"" + snTimestampString(now)
								+ "\",\"sourceId\":\"" + TEST_SOURCE_ID + "\"}],\"instructions\":"
								+ "[{\"foo\":\"bar\"},{\"bim\":\"bam\"}]}}");
			}

		};
		getHttpServer().addHandler(handler);

		List<Datum> data = new ArrayList<Datum>();
		GeneralNodeEnergyDatum d = new GeneralNodeEnergyDatum();
		d.setCreated(now);
		d.setSourceId(TEST_SOURCE_ID);
		d.setWatts(1);
		data.add(d);

		List<InstructionStatus> statusResult = new ArrayList<InstructionStatus>(2);
		statusResult
				.add(new BasicInstructionStatus(123L, InstructionStatus.InstructionState.Received, now));
		statusResult
				.add(new BasicInstructionStatus(124L, InstructionStatus.InstructionState.Received, now));
		Capture<Object> instructionDataCapture = new Capture<Object>();
		expect(reactorService.processInstruction(eq(identityService.getSolarInBaseUrl()),
				capture(instructionDataCapture), eq(BulkJsonWebPostUploadService.JSON_MIME_TYPE),
				EasyMock.<Map<String, ?>> isNull())).andReturn(statusResult);

		Capture<Event> eventCaptor = new Capture<Event>(CaptureType.ALL);
		eventAdmin.postEvent(EasyMock.capture(eventCaptor));

		replayAll();

		List<BulkUploadResult> result = service.uploadBulkDatum(data);

		assertNotNull(result);
		assertEquals(1, result.size());

		BulkUploadResult datumResult = result.get(0);
		assertEquals(datumResult.getId(), tid(d));
		assertEquals(d, datumResult.getDatum());

		Object instructionData = instructionDataCapture.getValue();
		assertTrue("Instruction data is JsonNode", instructionData instanceof JsonNode);
		JSONAssert.assertEquals("[{\"foo\":\"bar\"},{\"bim\":\"bam\"}]",
				service.getObjectMapper().writeValueAsString(instructionData), true);

		assertThat("Event count", eventCaptor.getValues(), hasSize(1));
		Event event = eventCaptor.getValue();
		assertDatumUploadEventEqualsDatum(event, d);
	}

}
