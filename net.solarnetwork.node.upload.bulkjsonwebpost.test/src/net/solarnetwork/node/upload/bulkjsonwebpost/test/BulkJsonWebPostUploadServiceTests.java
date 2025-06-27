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

import static java.lang.String.format;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.util.DigestUtils;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.datum.BasicObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.BasicStreamDatum;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleEnergyDatum;
import net.solarnetwork.node.reactor.BasicInstructionStatus;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.ReactorService;
import net.solarnetwork.node.service.BulkUploadResult;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.DatumMetadataService;
import net.solarnetwork.node.service.UploadService;
import net.solarnetwork.node.upload.bulkjsonwebpost.BulkJsonWebPostUploadService;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.test.http.AbstractHttpServerTests;
import net.solarnetwork.util.DateUtils;

/**
 * Unit tests for the {@link BulkJsonWebPostUploadService} class.
 *
 * @author matt
 * @version 3.0
 */
public class BulkJsonWebPostUploadServiceTests extends AbstractHttpServerTests {

	private static final Long TEST_NODE_ID = 123L;
	private static final String TEST_SOURCE_ID = "test-source";

	private BulkJsonWebPostUploadService service;

	private EventAdmin eventAdmin;
	private ReactorService reactorService;
	private DatumMetadataService datumMetadataService;
	private TestIdentityService identityService;

	@Before
	public void setupService() throws Exception {
		eventAdmin = EasyMock.createMock(EventAdmin.class);
		reactorService = EasyMock.createMock(ReactorService.class);
		datumMetadataService = EasyMock.createMock(DatumMetadataService.class);

		identityService = new TestIdentityService(TEST_NODE_ID, getHttpServerPort());

		service = new BulkJsonWebPostUploadService(new StaticOptionalService<>(reactorService),
				new StaticOptionalService<>(eventAdmin),
				new StaticOptionalService<>(datumMetadataService));
		service.setObjectMapper(JsonUtils.newDatumObjectMapper());
		service.setIdentityService(identityService);
		service.setUrl("/bulkupload");
	}

	@After
	public void finish() {
		EasyMock.verify(eventAdmin, reactorService, datumMetadataService);
	}

	private void replayAll() {
		EasyMock.replay(eventAdmin, reactorService, datumMetadataService);
	}

	@Test
	public void uploadEmptyList() throws Exception {
		TestBulkUploadHttpHandler handler = new TestBulkUploadHttpHandler() {

			@Override
			protected void handleJsonPost(Request request, Response response, String json) {
				// nothing
			}

		};
		addHandler(handler);

		replayAll();

		List<NodeDatum> data = new ArrayList<>();
		List<BulkUploadResult> result = service.uploadBulkDatum(data);

		assertThat("Result returned", result, is(notNullValue()));
		assertThat("Result item count", result.size(), is(0));
		assertThat("No network request made", handler.isHandled(), is(false));
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

	private static String snTimestampString(Instant date) {
		return DateUtils.ISO_DATE_TIME_ALT_UTC.format(date.truncatedTo(ChronoUnit.MILLIS));
	}

	@Test
	public void uploadSingleDatum() throws Exception {
		// GIVEN
		final Instant now = Instant.now();

		TestBulkUploadHttpHandler handler = new TestBulkUploadHttpHandler() {

			@Override
			protected void handleJsonPost(Request request, Response response, String json)
					throws Exception {
				JSONAssert.assertEquals(
						"[{\"created\":\"" + snTimestampString(now) + "\",\"sourceId\":\""
								+ TEST_SOURCE_ID + "\",\"i\":{\"watts\":1},\"a\":{\"wattHours\":2}}]",
						json, true);

				respondWithJson(request, response,
						"{\"success\":true,\"data\":{\"datum\":[{\"id\":\"abc123\",\"created\":\""
								+ snTimestampString(now) + "\",\"sourceId\":\"" + TEST_SOURCE_ID + "\""
								+ "}]}}",
						true);
			}

		};
		addHandler(handler);

		List<NodeDatum> data = new ArrayList<>();
		SimpleEnergyDatum d = new SimpleEnergyDatum(TEST_SOURCE_ID, now, new DatumSamples());
		d.setWatts(1);
		d.setWattHourReading(2L);
		data.add(d);

		// no stream metadata available
		expect(datumMetadataService.getDatumStreamMetadata(ObjectDatumKind.Node, TEST_NODE_ID,
				TEST_SOURCE_ID)).andReturn(null);

		Capture<Event> eventCaptor = Capture.newInstance(CaptureType.ALL);
		eventAdmin.postEvent(EasyMock.capture(eventCaptor));

		// WHEN
		replayAll();
		List<BulkUploadResult> result = service.uploadBulkDatum(data);

		// THEN
		assertThat("Result returned", result, is(notNullValue()));
		assertThat("Result item count", result.size(), is(1));

		BulkUploadResult datumResult = result.get(0);
		assertThat("Result item has ID from response", datumResult.getId(), is("abc123"));
		assertThat("Result item datum is from request", datumResult.getDatum(), is(d));

		assertThat("Event count", eventCaptor.getValues(), hasSize(1));
		Event event = eventCaptor.getValue();
		assertDatumUploadEventEqualsDatum(event, d);
	}

	@Test
	public void uploadSingleDatum_streamDatum() throws Exception {
		// GIVEN
		final Instant now = Instant.now();
		final SimpleEnergyDatum d = new SimpleEnergyDatum(TEST_SOURCE_ID, now, new DatumSamples());
		d.setWatts(1);
		d.setWattHourReading(2L);

		final BasicObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(),
				"Pacific/Auckland", ObjectDatumKind.Node, TEST_NODE_ID, TEST_SOURCE_ID,
				new String[] { "watts" }, new String[] { "wattHours" }, null);

		TestBulkUploadHttpHandler handler = new TestBulkUploadHttpHandler() {

			@Override
			protected void handleJsonPost(Request request, Response response, String json)
					throws Exception {
				DatumProperties props = DatumProperties.propertiesFrom(d, meta);
				BasicStreamDatum streamDatum = new BasicStreamDatum(meta.getStreamId(), d.getTimestamp(),
						props);

				JSONAssert.assertEquals(
						"[" + service.getObjectMapper().writeValueAsString(streamDatum) + "]", json,
						true);

				respondWithJson(request, response,
						"{\"success\":true,\"data\":{\"datum\":[{\"streamId\":\""
								+ meta.getStreamId().toString() + "\",\"timestamp\":"
								+ now.toEpochMilli() + "}]}}",
						true);
			}

		};
		addHandler(handler);

		List<NodeDatum> data = new ArrayList<>();
		data.add(d);

		// stream metadata available
		expect(datumMetadataService.getDatumStreamMetadata(ObjectDatumKind.Node, TEST_NODE_ID,
				TEST_SOURCE_ID)).andReturn(meta).times(2);

		Capture<Event> eventCaptor = Capture.newInstance(CaptureType.ALL);
		eventAdmin.postEvent(EasyMock.capture(eventCaptor));

		// WHEN
		replayAll();
		List<BulkUploadResult> result = service.uploadBulkDatum(data);

		// THEN
		assertThat("Result size", result, hasSize(1));

		BulkUploadResult datumResult = result.get(0);
		assertThat("Upload ID is synthetic stream datum ID", datumResult.getId(), is(tid(d)));
		assertThat("Upload result datum", datumResult.getDatum(), is(d));

		assertThat("Event count", eventCaptor.getValues(), hasSize(1));
		Event event = eventCaptor.getValue();
		assertDatumUploadEventEqualsDatum(event, d);
	}

	@Test
	public void uploadSingleDatum_emptyResponse() throws Exception {
		// GIVEN
		final Instant now = Instant.now();

		TestBulkUploadHttpHandler handler = new TestBulkUploadHttpHandler() {

			@Override
			protected void handleJsonPost(Request request, Response response, String json)
					throws Exception {
				JSONAssert.assertEquals(
						"[{\"created\":\"" + snTimestampString(now) + "\",\"sourceId\":\""
								+ TEST_SOURCE_ID + "\",\"i\":{\"watts\":1},\"a\":{\"wattHours\":2}}]",
						json, true);

				respondWithJson(request, response, "{\"success\":true,\"data\":{}}", true);
			}

		};
		addHandler(handler);

		List<NodeDatum> data = new ArrayList<>();
		SimpleEnergyDatum d = new SimpleEnergyDatum(TEST_SOURCE_ID, now, new DatumSamples());
		d.setWatts(1);
		d.setWattHourReading(2L);
		data.add(d);

		// no stream metadata available
		expect(datumMetadataService.getDatumStreamMetadata(ObjectDatumKind.Node, TEST_NODE_ID,
				TEST_SOURCE_ID)).andReturn(null);

		// WHEN
		replayAll();
		List<BulkUploadResult> result = service.uploadBulkDatum(data);

		// THEN
		assertThat("Result returned", result, is(notNullValue()));
		assertThat("Result item count", result.size(), is(1));

		BulkUploadResult datumResult = result.get(0);
		assertThat("Result item has ID from response", datumResult.getId(), is(nullValue()));
		assertThat("Result item datum is from request", datumResult.getDatum(), is(d));
	}

	@Test
	public void uploadMultiDatum_noResultIds() throws Exception {
		// GIVEN
		final Instant now = Instant.now();

		final List<NodeDatum> data = new ArrayList<>();
		for ( int i = 0; i < 3; i++ ) {
			SimpleEnergyDatum d = new SimpleEnergyDatum(TEST_SOURCE_ID, now, new DatumSamples());
			d.setWatts(i + 1);
			d.setWattHourReading(100L + i);
			data.add(d);
		}

		TestBulkUploadHttpHandler handler = new TestBulkUploadHttpHandler() {

			@Override
			protected void handleJsonPost(Request request, Response response, String json)
					throws Exception {
				StringBuilder req = new StringBuilder("[");
				StringBuilder res = new StringBuilder("{\"success\":true,\"data\":{\"datum\":[");
				int i = 0;
				for ( NodeDatum d : data ) {
					if ( i > 0 ) {
						req.append(',');
						res.append(',');
					}
					req.append(format(
							"{\"created\":\"%s\",\"sourceId\":\"%s\",\"i\":{\"watts\":%d},\"a\":{\"wattHours\":%d}}",
							snTimestampString(d.getTimestamp()), TEST_SOURCE_ID,
							d.asSampleOperations().getSampleInteger(DatumSamplesType.Instantaneous,
									"watts"),
							d.asSampleOperations().getSampleInteger(DatumSamplesType.Accumulating,
									"wattHours")));
					res.append(format("{\"created\":\"%s\",\"sourceId\":\"%s\"}",
							snTimestampString(d.getTimestamp()), TEST_SOURCE_ID));
					i++;
				}
				req.append("]");
				res.append("]}}");
				JSONAssert.assertEquals(req.toString(), json, true);

				respondWithJson(request, response, res.toString(), true);
			}

		};
		addHandler(handler);

		// no stream metadata available
		expect(datumMetadataService.getDatumStreamMetadata(ObjectDatumKind.Node, TEST_NODE_ID,
				TEST_SOURCE_ID)).andReturn(null).anyTimes();

		Capture<Event> eventCaptor = Capture.newInstance(CaptureType.ALL);
		eventAdmin.postEvent(EasyMock.capture(eventCaptor));
		expectLastCall().times(3);

		// WHEN
		replayAll();
		List<BulkUploadResult> result = service.uploadBulkDatum(data);

		// THEN
		assertThat("Result returned", result, is(notNullValue()));
		assertThat("Result item count", result.size(), is(3));

		int i = 0;
		for ( NodeDatum d : data ) {
			BulkUploadResult datumResult = result.get(i);
			assertThat(format("Result item %d datum is from request", i), datumResult.getDatum(), is(d));
			assertThat(format("Result item %d has synthetic ID from response", i), datumResult.getId(),
					is(tid(d)));

			assertThat("Event count", eventCaptor.getValues(), hasSize(3));
			Event event = eventCaptor.getValues().get(i);
			assertDatumUploadEventEqualsDatum(event, d);
			i++;
		}
	}

	@Test
	public void uploadMultiDatum_noResultIds_partialResponse() throws Exception {
		// GIVEN
		final Instant now = Instant.now();

		final List<NodeDatum> data = new ArrayList<>();
		for ( int i = 0; i < 3; i++ ) {
			SimpleEnergyDatum d = new SimpleEnergyDatum(TEST_SOURCE_ID, now, new DatumSamples());
			d.setWatts(i + 1);
			d.setWattHourReading(100L + i);
			data.add(d);
		}

		TestBulkUploadHttpHandler handler = new TestBulkUploadHttpHandler() {

			@Override
			protected void handleJsonPost(Request request, Response response, String json)
					throws Exception {
				StringBuilder req = new StringBuilder("[");
				StringBuilder res = new StringBuilder("{\"success\":true,\"data\":{\"datum\":[");
				int i = 0;
				for ( NodeDatum d : data ) {
					if ( i > 0 ) {
						req.append(',');
						res.append(',');
					}
					req.append(format(
							"{\"created\":\"%s\",\"sourceId\":\"%s\",\"i\":{\"watts\":%d},\"a\":{\"wattHours\":%d}}",
							snTimestampString(d.getTimestamp()), TEST_SOURCE_ID,
							d.asSampleOperations().getSampleInteger(DatumSamplesType.Instantaneous,
									"watts"),
							d.asSampleOperations().getSampleInteger(DatumSamplesType.Accumulating,
									"wattHours")));
					if ( i < 2 ) {
						res.append(format("{\"created\":\"%s\",\"sourceId\":\"%s\"}",
								snTimestampString(d.getTimestamp()), TEST_SOURCE_ID));
					} else {
						res.append("{}");
					}
					i++;
				}
				req.append("]");
				res.append("]}}");
				JSONAssert.assertEquals(req.toString(), json, true);

				respondWithJson(request, response, res.toString(), true);
			}

		};
		addHandler(handler);

		// no stream metadata available
		expect(datumMetadataService.getDatumStreamMetadata(ObjectDatumKind.Node, TEST_NODE_ID,
				TEST_SOURCE_ID)).andReturn(null).anyTimes();

		Capture<Event> eventCaptor = Capture.newInstance(CaptureType.ALL);
		eventAdmin.postEvent(EasyMock.capture(eventCaptor));
		expectLastCall().times(2);

		// WHEN
		replayAll();
		List<BulkUploadResult> result = service.uploadBulkDatum(data);

		// THEN
		assertThat("Result returned", result, is(notNullValue()));
		assertThat("Result item count", result.size(), is(3));
		assertThat("Uploaded event count", eventCaptor.getValues(), hasSize(2));

		int i = 0;
		for ( NodeDatum d : data ) {
			BulkUploadResult datumResult = result.get(i);
			assertThat(format("Result item %d datum is from request", i), datumResult.getDatum(), is(d));
			if ( i < 2 ) {
				assertThat(format("Result item %d has synthetic ID from response", i),
						datumResult.getId(), is(tid(d)));
				Event event = eventCaptor.getValues().get(i);
				assertDatumUploadEventEqualsDatum(event, d);
			} else {
				assertThat(format("Result item %d has no ID because missing from response", i),
						datumResult.getId(), is(nullValue()));
			}

			i++;
		}
	}

	private String tid(Datum datum) {
		return DigestUtils.md5DigestAsHex(
				String.format("%tQ;%s", datum.getTimestamp(), datum.getSourceId()).getBytes());
	}

	private void assertDatumUploadEventEqualsDatum(Event event, NodeDatum datum) {
		String[] datumTypes = DatumEvents.datumTypes(datum.getClass());
		assertThat("Topic", event.getTopic(), equalTo(UploadService.EVENT_TOPIC_DATUM_UPLOADED));
		assertThat("Datum type", (String) event.getProperty(Datum.DATUM_TYPE_PROPERTY),
				equalTo(datumTypes[0]));
		assertThat("Datum types", (String[]) event.getProperty(Datum.DATUM_TYPES_PROPERTY),
				arrayContaining(datumTypes));
		assertThat("Source ID", (String) event.getProperty("sourceId"), equalTo(datum.getSourceId()));
		assertThat("Created", (String) event.getProperty("created"),
				equalTo(snTimestampString(datum.getTimestamp())));
		for ( Map.Entry<String, ?> me : datum.getSampleData().entrySet() ) {
			assertThat("Event prop" + me.getKey(), event.getProperty(me.getKey()),
					equalTo((Object) me.getValue()));
		}
		Set<String> tags = datum.asSampleOperations().getTags();
		if ( tags != null && !tags.isEmpty() ) {
			String[] expectedTags = tags.toArray(new String[tags.size()]);
			assertThat("Tags", (String[]) event.getProperty("tags"), arrayContaining(expectedTags));
		}
	}

	@Test
	public void acknowledgeSingleInstruction() throws Exception {
		final Instant now = Instant.now();
		final String nowString = DateUtils.ISO_DATE_TIME_ALT_UTC.format(now);

		TestBulkUploadHttpHandler handler = new TestBulkUploadHttpHandler() {

			@Override
			protected void handleJsonPost(Request request, Response response, String json)
					throws Exception {
				JSONAssert
						.assertEquals("[{\"instructionId\":123,\"state\":\"Completed\",\"statusDate\":\""
								+ nowString + "\"}]", json, true);

				respondWithJson(request, response,
						"{\"success\":true,\"data\":{\"datum\":[{\"id\":\"123\"}]}}", true);
			}

		};
		addHandler(handler);

		replayAll();

		List<InstructionStatus> data = new ArrayList<>();
		data.add(new BasicInstructionStatus(123L, InstructionState.Completed, now));

		service.acknowledgeInstructions(data);
	}

	@Test
	public void processInstruction() throws Exception {
		// GIVEN
		final Instant now = Instant.now();

		TestBulkUploadHttpHandler handler = new TestBulkUploadHttpHandler() {

			@Override
			protected void handleJsonPost(Request request, Response response, String json)
					throws Exception {
				JSONAssert.assertEquals("[{\"created\":\"" + snTimestampString(now)
						+ "\",\"sourceId\":\"" + TEST_SOURCE_ID + "\",\"i\":{\"watts\":1}}]", json,
						true);

				// @formatter:off
				respondWithJson(request, response,
						"{\"success\":true,\"data\":{\"datum\":[{\"created\":\"" + snTimestampString(now)
								+ "\",\"sourceId\":\"" + TEST_SOURCE_ID + "\"}],\"instructions\":["
								+ "{\"id\":123,\"topic\":\"foo\"}"
								+ ",{\"id\":234,\"topic\":\"bar\",\"parameters\":["
									+ "{\"name\":\"p1\",\"value\":\"v1\"}"
									+ ",{\"name\":\"p2\",\"value\":\"v2\"}"
								+ "]}]}}", true);
				// @formatter:on
			}

		};
		addHandler(handler);

		List<NodeDatum> data = new ArrayList<>();
		SimpleEnergyDatum d = new SimpleEnergyDatum(TEST_SOURCE_ID, now, new DatumSamples());
		d.setWatts(1);
		data.add(d);

		// no stream metadata available
		expect(datumMetadataService.getDatumStreamMetadata(ObjectDatumKind.Node, TEST_NODE_ID,
				TEST_SOURCE_ID)).andReturn(null);

		List<InstructionStatus> statusResult = new ArrayList<>(2);
		statusResult
				.add(new BasicInstructionStatus(123L, InstructionStatus.InstructionState.Received, now));
		statusResult
				.add(new BasicInstructionStatus(124L, InstructionStatus.InstructionState.Received, now));
		Capture<Instruction> instructionCaptor = Capture.newInstance(CaptureType.ALL);
		expect(reactorService.processInstruction(capture(instructionCaptor)))
				.andReturn(statusResult.get(0)).andReturn(statusResult.get(1));

		Capture<Event> eventCaptor = Capture.newInstance(CaptureType.ALL);
		eventAdmin.postEvent(EasyMock.capture(eventCaptor));

		// WHEN
		replayAll();
		List<BulkUploadResult> result = service.uploadBulkDatum(data);

		// THEN
		assertThat("Result provided", result, is(notNullValue()));
		assertThat("Result item count", result.size(), is(1));

		BulkUploadResult datumResult = result.get(0);
		assertThat("Upload result ID", tid(d), is(datumResult.getId()));
		assertThat("Datum", datumResult.getDatum(), is(d));

		Instruction instr1 = instructionCaptor.getValues().get(0);
		assertThat("Instruction 1 parsed ID", instr1.getId(), is(123L));
		assertThat("Instruction 1 parsed topic", instr1.getTopic(), is("foo"));

		Instruction instr2 = instructionCaptor.getValues().get(1);
		assertThat("Instruction 2 parsed ID", instr2.getId(), is(234L));
		assertThat("Instruction 2 parsed topic", instr2.getTopic(), is("bar"));

		Map<String, String> instr2Params = instr2.getParameterMap();
		assertThat("Instruction 2 parsed params", instr2Params, hasEntry("p1", "v1"));
		assertThat("Instruction 2 parsed params", instr2Params, hasEntry("p2", "v2"));

		assertThat("Event count", eventCaptor.getValues(), hasSize(1));
		Event event = eventCaptor.getValue();
		assertDatumUploadEventEqualsDatum(event, d);
	}

}
