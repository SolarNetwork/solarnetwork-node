/* ==================================================================
 * DatumControllerTests.java - 13 Nov 2026 5:31:08 pm
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

package net.solarnetwork.node.setup.web.api.test;

import static net.solarnetwork.dao.StreamDatumFilteredResultsProcessor.METADATA_PROVIDER_ATTR;
import static net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider.staticProvider;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import net.solarnetwork.dao.StreamDatumFilteredResultsProcessor;
import net.solarnetwork.domain.SimpleSortDescriptor;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.StreamDatum;
import net.solarnetwork.node.dao.NodeDatumFilter;
import net.solarnetwork.node.service.FilterableDatumHistorian;
import net.solarnetwork.node.setup.web.api.DatumController;
import net.solarnetwork.node.setup.web.domain.DatumFilterCommand;

/**
 * Test cases for the {@link DatumController} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@RunWith(MockitoJUnitRunner.class)
public class DatumControllerTests extends SolarQueryApiTestSupport {

	private static final String META_PATH = "/solarquery/api/v1/sec/datum/stream/meta/node";
	private static final String STREAM_PATH = "/solarquery/api/v1/sec/datum/stream/datum";
	private static final MediaType TEXT_CSV = MediaType.valueOf("text/csv");

	@Captor
	private ArgumentCaptor<NodeDatumFilter> filterCaptor;

	private DatumController controller;
	private MockMvc mvc;

	@Before
	public void setup() {
		setupSupport();
		controller = new DatumController(identityService, historianSelector);
		mvc = mockMvc(controller);
	}

	private static String fixtureMetaJson(ObjectDatumStreamMetadata meta) {
		return """
				{"streamId":"%s","zone":"Pacific/Auckland","kind":"n","objectId":123,\
				"sourceId":"test/source","i":["a","b"],"a":["c"],"s":["d"]}"""
				.formatted(meta.getStreamId());
	}

	private void givenFixtureMeta(ObjectDatumStreamMetadata meta) {
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		given(filtered.findDatumStreamMetadata(filterCaptor.capture())).willReturn(List.of(meta));
	}

	private void givenFixtureStream(FilterableDatumHistorian historian, ObjectDatumStreamMetadata meta)
			throws IOException {
		willAnswer(streamAnswer(List.of(meta), fixtureDatum(meta))).given(historian)
				.findFilteredStream(filterCaptor.capture(), any(), isNull(), isNull(), isNull());
	}

	/**
	 * Create an answer that gives the processor enough datum for it to write to
	 * the response, and then throws an exception.
	 */
	private static Answer<Void> manyDatumAnswer(ObjectDatumStreamMetadata meta, RuntimeException ex) {
		final List<StreamDatum> datum = fixtureDatum(meta);
		return invocation -> {
			final StreamDatumFilteredResultsProcessor processor = invocation.getArgument(1);
			processor.start(null, null, null,
					Map.of(METADATA_PROVIDER_ATTR, staticProvider(List.of(meta))));
			for ( int i = 0; i < 1000; i++ ) {
				for ( StreamDatum d : datum ) {
					processor.handleResultItem(d);
				}
			}
			throw ex;
		};
	}

	private static String gunzip(byte[] data) throws IOException {
		try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(data))) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	@Test
	public void meta() throws Exception {
		// GIVEN
		final ObjectDatumStreamMetadata meta = fixtureMeta();
		givenFixtureMeta(meta);

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(META_PATH).param("sourceIds", "test/*").accept(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentAsString()).as("Stream metadata provided").isEqualTo("""
				{"success":true,"data":[%s]}""".formatted(fixtureMetaJson(meta)));
		final NodeDatumFilter filter = filterCaptor.getValue();
		and.then(filter.getObjectKind()).as("Node streams queried").isEqualTo(ObjectDatumKind.Node);
		and.then(filter.getObjectIds()).as("The node's own ID queried").containsExactly(NODE_ID);
		and.then(filter.getSourceIds()).as("Source IDs criteria applied").containsExactly("test/*");
	}

	@Test
	public void meta_cbor() throws Exception {
		// GIVEN
		final ObjectDatumStreamMetadata meta = fixtureMeta();
		givenFixtureMeta(meta);

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(META_PATH).accept(MediaType.APPLICATION_CBOR)).andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentType()).as("CBOR content").isEqualTo(MediaType.APPLICATION_CBOR_VALUE);
		and.then(cborJson(res.getContentAsByteArray())).as("Stream metadata provided as CBOR")
				.isEqualTo("""
						{"success":true,"data":[%s]}""".formatted(fixtureMetaJson(meta)));
	}

	@Test
	public void meta_otherNodesOnly() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(META_PATH).param("nodeId", "1").accept(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentAsString()).as("No metadata provided for other nodes").isEqualTo("""
				{"success":true,"data":[]}""");
		then(filteredService).shouldHaveNoInteractions();
		then(unfilteredService).shouldHaveNoInteractions();
	}

	@Test
	public void meta_notAssociated() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(null);

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(META_PATH).accept(MediaType.APPLICATION_JSON)).andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentAsString()).as("No metadata provided when not associated").isEqualTo("""
				{"success":true,"data":[]}""");
		then(filteredService).shouldHaveNoInteractions();
	}

	@Test
	public void stream_json() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		final ObjectDatumStreamMetadata meta = fixtureMeta();
		givenFixtureStream(filtered, meta);

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(STREAM_PATH).accept(MediaType.APPLICATION_JSON)).andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentType()).as("JSON content").isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		and.then(res.getHeader(HttpHeaders.CONTENT_ENCODING)).as("Not compressed").isNull();
		and.then(res.getContentAsString(StandardCharsets.UTF_8)).as("Datum stream provided as JSON")
				.isEqualTo(fixtureStreamJson(meta));
		final NodeDatumFilter filter = filterCaptor.getValue();
		and.then(filter.getObjectKind()).as("Node streams queried").isEqualTo(ObjectDatumKind.Node);
		and.then(filter.getObjectIds()).as("The node's own ID queried").containsExactly(NODE_ID);
	}

	@Test
	public void stream_cbor() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		final ObjectDatumStreamMetadata meta = fixtureMeta();
		givenFixtureStream(filtered, meta);

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(STREAM_PATH).accept(MediaType.APPLICATION_CBOR)).andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentType()).as("CBOR content").isEqualTo(MediaType.APPLICATION_CBOR_VALUE);
		and.then(cborJson(res.getContentAsByteArray())).as("Datum stream provided as CBOR")
				.isEqualTo(fixtureStreamJson(meta));
	}

	@Test
	public void stream_noAccept() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		final ObjectDatumStreamMetadata meta = fixtureMeta();
		givenFixtureStream(filtered, meta);

		// WHEN
		final MockHttpServletResponse res = mvc.perform(get(STREAM_PATH)).andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentType()).as("CBOR content when no Accept header")
				.isEqualTo(MediaType.APPLICATION_CBOR_VALUE);
		and.then(cborJson(res.getContentAsByteArray())).as("Datum stream provided as CBOR")
				.isEqualTo(fixtureStreamJson(meta));
	}

	@Test
	public void stream_csv() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		final ObjectDatumStreamMetadata meta = fixtureMeta();
		givenFixtureStream(filtered, meta);

		// WHEN
		final MockHttpServletResponse res = mvc.perform(get(STREAM_PATH).accept(TEXT_CSV)).andReturn()
				.getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentType()).as("CSV content").isEqualTo(TEXT_CSV.toString());
		and.then(res.getContentAsString(StandardCharsets.UTF_8)).as("Datum stream provided as CSV")
				.isEqualTo(fixtureStreamCsv(meta));
	}

	@Test
	public void stream_acceptHeaderOrder() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		final ObjectDatumStreamMetadata meta = fixtureMeta();
		givenFixtureStream(filtered, meta);

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(STREAM_PATH).accept(TEXT_CSV, MediaType.APPLICATION_JSON)).andReturn()
				.getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentType()).as("First supported Accept type used")
				.isEqualTo(TEXT_CSV.toString());
		and.then(res.getContentAsString(StandardCharsets.UTF_8)).as("Datum stream provided as CSV")
				.isEqualTo(fixtureStreamCsv(meta));
	}

	@Test
	public void stream_gzip() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		final ObjectDatumStreamMetadata meta = fixtureMeta();
		givenFixtureStream(filtered, meta);

		// WHEN
		final MockHttpServletResponse res = mvc.perform(get(STREAM_PATH)
				.accept(MediaType.APPLICATION_JSON).header(HttpHeaders.ACCEPT_ENCODING, "deflate, gzip"))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentType()).as("JSON content").isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		and.then(res.getHeader(HttpHeaders.CONTENT_ENCODING)).as("Compressed with gzip")
				.isEqualTo("gzip");
		and.then(gunzip(res.getContentAsByteArray())).as("Datum stream provided as compressed JSON")
				.isEqualTo(fixtureStreamJson(meta));
	}

	@Test
	public void stream_otherNodesOnly() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(STREAM_PATH).param("nodeIds", "1,2").accept(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentType()).as("JSON content").isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		and.then(res.getContentAsString()).as("Empty results provided for other nodes").isEqualTo("""
				{"success":true}""");
		then(filteredService).shouldHaveNoInteractions();
		then(unfilteredService).shouldHaveNoInteractions();
	}

	@Test
	public void stream_notAssociated() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(null);

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(STREAM_PATH).accept(MediaType.APPLICATION_JSON)).andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentAsString()).as("Empty results provided when not associated").isEqualTo("""
				{"success":true}""");
		then(filteredService).shouldHaveNoInteractions();
	}

	@Test
	public void stream_historianNotAvailable() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(STREAM_PATH).accept(MediaType.APPLICATION_JSON)).andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentAsString()).as("Empty results provided without a historian").isEqualTo("""
				{"success":true}""");
	}

	@Test
	public void stream_unfiltered() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(unfilteredService.service()).willReturn(unfiltered);
		final ObjectDatumStreamMetadata meta = fixtureMeta();
		givenFixtureStream(unfiltered, meta);

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get("/unfiltered" + STREAM_PATH).accept(MediaType.APPLICATION_JSON)).andReturn()
				.getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentAsString(StandardCharsets.UTF_8))
				.as("Datum stream of unfiltered historian provided").isEqualTo(fixtureStreamJson(meta));
		then(filteredService).shouldHaveNoInteractions();
	}

	@Test
	public void stream_apiPath() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		final ObjectDatumStreamMetadata meta = fixtureMeta();
		givenFixtureStream(filtered, meta);

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get("/api/v1/sec/datum/stream/datum").accept(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentAsString(StandardCharsets.UTF_8))
				.as("Datum stream provided under API path").isEqualTo(fixtureStreamJson(meta));
	}

	@Test
	public void stream_criteria() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		givenFixtureStream(filtered, fixtureMeta());

		// WHEN
		final MockHttpServletResponse res = mvc.perform(get(STREAM_PATH)
				.param("nodeId", NODE_ID.toString()).param("sourceIds", "a/*,b")
				.param("startDate", "2026-11-01T00:00:00Z").param("endDate", "2026-11-02T00:00:00+13:00")
				.param("aggregation", "Hour").param("mostRecent", "true").param("max", "10")
				.param("offset", "5").param("withoutTotalResultsCount", "false")
				.param("sorts[0].sortKey", "created").param("sorts[0].descending", "true")
				.param("sorts[1].sortKey", "source").accept(MediaType.APPLICATION_JSON)).andReturn()
				.getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		final NodeDatumFilter filter = filterCaptor.getValue();
		and.then(filter.getObjectIds()).as("The node's own ID queried").containsExactly(NODE_ID);
		and.then(filter.getSourceIds()).as("Source IDs parsed from list").containsExactly("a/*", "b");
		and.then(filter.getStartDate()).as("Start date parsed")
				.isEqualTo(Instant.parse("2026-11-01T00:00:00Z"));
		and.then(filter.getEndDate()).as("End date parsed with offset")
				.isEqualTo(Instant.parse("2026-11-01T11:00:00Z"));
		and.then(filter.getAggregation()).as("Aggregation parsed").isEqualTo(Aggregation.Hour);
		and.then(filter.isMostRecent()).as("Most recent parsed").isTrue();
		and.then(filter.getMax()).as("Max parsed").isEqualTo(10);
		and.then(filter.getOffset()).as("Offset parsed").isEqualTo(5L);
		and.then(filter.isWithoutTotalResultsCount()).as("Without total results count parsed").isFalse();
		and.then(filter.getSorts()).as("Sorts mapped to historian sort keys").containsExactly(
				new SimpleSortDescriptor(FilterableDatumHistorian.SORT_BY_TIME, true),
				new SimpleSortDescriptor(FilterableDatumHistorian.SORT_BY_SOURCE, false));
	}

	@Test
	public void stream_max_totalResultsNotCountedByDefault() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		givenFixtureStream(filtered, fixtureMeta());

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(STREAM_PATH).param("max", "10").accept(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(filterCaptor.getValue().isWithoutTotalResultsCount())
				.as("Total results not counted by default").isTrue();
	}

	@Test
	public void stream_localDates() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		givenFixtureStream(filtered, fixtureMeta());

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(STREAM_PATH).param("localStartDate", "2026-11-01T06:00")
						.param("localEndDate", "2026-11-02").accept(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		final NodeDatumFilter filter = filterCaptor.getValue();
		and.then(filter.getStartDate()).as("Local start date in node time zone").isEqualTo(
				LocalDateTime.of(2026, 11, 1, 6, 0).atZone(ZoneId.systemDefault()).toInstant());
		and.then(filter.getEndDate()).as("Local end date in node time zone").isEqualTo(
				LocalDateTime.of(2026, 11, 2, 0, 0).atZone(ZoneId.systemDefault()).toInstant());
	}

	@Test
	public void stream_propertyNames() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		final ObjectDatumStreamMetadata meta = fixtureMeta();
		givenFixtureStream(filtered, meta);

		// WHEN
		final MockHttpServletResponse res = mvc.perform(
				get(STREAM_PATH).param("propertyNames", "b,c").accept(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentAsString(StandardCharsets.UTF_8))
				.as("Datum stream restricted to property names").isEqualTo("""
						{"success":true,"meta":[\
						{"streamId":"%s","zone":"Pacific/Auckland","kind":"n","objectId":123,\
						"sourceId":"test/source","i":["b"],"a":["c"]}\
						],"data":[\
						[0,1651197120000,2.34,3.45,"a"],\
						[0,1651197121000,4.32,5.43]\
						]}""".formatted(meta.getStreamId()));
	}

	@Test
	public void stream_ignoredParameters() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		givenFixtureStream(filtered, fixtureMeta());

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(STREAM_PATH).param("aggregation", "Fortnightly")
						.param("combiningType", "Sum").param("metadataFilter", "(foo=bar)")
						.param("foo", "bar").accept(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK with unsupported parameters").isEqualTo(200);
		and.then(filterCaptor.getValue().getAggregation()).as("Unknown aggregation ignored").isNull();
	}

	@Test
	public void stream_invalidDate() throws Exception {
		// WHEN
		final MockHttpServletResponse res = mvc.perform(
				get(STREAM_PATH).param("startDate", "not a date").accept(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("Invalid date rejected").isEqualTo(422);
		and.then(res.getContentAsString()).as("Error result provided").startsWith("{\"success\":false,");
		then(identityService).shouldHaveNoInteractions();
		then(filteredService).shouldHaveNoInteractions();
	}

	@Test
	public void stream_xml() throws Exception {
		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(STREAM_PATH).accept(MediaType.APPLICATION_XML)).andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("XML is not acceptable").isEqualTo(406);
		then(identityService).shouldHaveNoInteractions();
		then(filteredService).shouldHaveNoInteractions();
	}

	@Test
	public void stream_errorBeforeOutput() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		willThrow(new InvalidDataAccessResourceUsageException("Bad SQL")).given(filtered)
				.findFilteredStream(any(), any(), isNull(), isNull(), isNull());

		// WHEN
		final MockHttpServletResponse res = mvc.perform(get(STREAM_PATH)
				.accept(MediaType.APPLICATION_JSON).header(HttpHeaders.ACCEPT_ENCODING, "gzip"))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("Error status").isEqualTo(500);
		and.then(res.getContentType()).as("JSON content").isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		and.then(res.getHeader(HttpHeaders.CONTENT_ENCODING)).as("Error result not compressed").isNull();
		and.then(res.getContentAsString()).as("Error result provided").isEqualTo("""
				{"success":false,"code":"DAO.00500","message":"Internal error"}""");
	}

	@Test
	public void stream_errorAfterOutput() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		willAnswer(manyDatumAnswer(fixtureMeta(), new DataAccessResourceFailureException("Lost")))
				.given(filtered).findFilteredStream(any(), any(), isNull(), isNull(), isNull());

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(STREAM_PATH).accept(MediaType.APPLICATION_JSON)).andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("Response started as success").isEqualTo(200);
		and.then(res.getContentAsString(StandardCharsets.UTF_8))
				.as("Partial results left incomplete, without an error result added")
				.startsWith("{\"success\":true,\"meta\":[").doesNotEndWith("]}")
				.doesNotContain("\"success\":false");
	}

	@Test
	public void stream_errorAfterOutput_gzip() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		willAnswer(manyDatumAnswer(fixtureMeta(), new DataAccessResourceFailureException("Lost")))
				.given(filtered).findFilteredStream(any(), any(), isNull(), isNull(), isNull());

		// WHEN
		final MockHttpServletResponse res = mvc.perform(get(STREAM_PATH)
				.accept(MediaType.APPLICATION_JSON).header(HttpHeaders.ACCEPT_ENCODING, "gzip"))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("Response started as success").isEqualTo(200);
		and.then(res.getHeader(HttpHeaders.CONTENT_ENCODING)).as("Results compressed").isEqualTo("gzip");
		and.then(catchThrowable(() -> gunzip(res.getContentAsByteArray())))
				.as("Compressed results left incomplete, so they can not be decompressed")
				.isInstanceOf(EOFException.class);
	}

	@Test
	public void stream_clientDisconnected() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		willAnswer(manyDatumAnswer(fixtureMeta(), new IllegalStateException("Not disconnected")))
				.given(filtered).findFilteredStream(any(), any(), isNull(), isNull(), isNull());

		final MockHttpServletRequest req = new MockHttpServletRequest("GET", STREAM_PATH);
		final DisconnectedResponse res = new DisconnectedResponse(new MockHttpServletResponse());

		// WHEN
		controller.findDatum(req, res, new DatumFilterCommand(), MediaType.APPLICATION_JSON_VALUE, null);

		// THEN
		and.then(res.written).as("Response written to before the client disconnected").isTrue();
	}

	/**
	 * A response whose output stream fails, as when the client has
	 * disconnected.
	 */
	private static final class DisconnectedResponse extends HttpServletResponseWrapper {

		private boolean written;

		private DisconnectedResponse(HttpServletResponse response) {
			super(response);
		}

		@Override
		public ServletOutputStream getOutputStream() {
			return new ServletOutputStream() {

				@Override
				public void write(int b) throws IOException {
					written = true;
					throw new IOException("Broken pipe");
				}

				@Override
				public boolean isReady() {
					return true;
				}

				@Override
				public void setWriteListener(WriteListener writeListener) {
					// not needed
				}

			};
		}

	}

}
