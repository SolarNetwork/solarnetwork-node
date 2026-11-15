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
import java.time.temporal.ChronoUnit;
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
import net.solarnetwork.domain.datum.DatumReadingType;
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
	private static final String READING_PATH = "/solarquery/api/v1/sec/datum/stream/reading";
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

	private void givenAggregateStream(FilterableDatumHistorian historian, ObjectDatumStreamMetadata meta,
			Instant ts) throws IOException {
		willAnswer(streamAnswer(List.of(meta), List.of(fixtureAggregateDatum(meta, ts))))
				.given(historian)
				.findFilteredStream(filterCaptor.capture(), any(), isNull(), isNull(), isNull());
	}

	private void givenReadingStream(FilterableDatumHistorian historian, ObjectDatumStreamMetadata meta,
			Instant ts) throws IOException {
		willAnswer(streamAnswer(List.of(meta),
				List.of(fixtureReadingDatum(meta, ts, ts.plus(1, ChronoUnit.HOURS))))).given(historian)
						.findFilteredStreamReadings(filterCaptor.capture(), any(), isNull(), isNull(),
								isNull());
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
		and.then(filter.getLocalStartDate()).as("Local start date passed to historian")
				.isEqualTo(LocalDateTime.of(2026, 11, 1, 6, 0));
		and.then(filter.getLocalEndDate()).as("Local end date passed to historian")
				.isEqualTo(LocalDateTime.of(2026, 11, 2, 0, 0));
		and.then(filter.getStartDate()).as("Local start date not converted to an instant").isNull();
		and.then(filter.getEndDate()).as("Local end date not converted to an instant").isNull();
	}

	@Test
	public void stream_aggregate_json() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		final ObjectDatumStreamMetadata meta = fixtureMeta();
		givenAggregateStream(filtered, meta, fixtureAggregateJsonTimestamp());

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(
						get(STREAM_PATH).param("aggregation", "Hour").accept(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentType()).as("JSON content").isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		and.then(res.getContentAsString(StandardCharsets.UTF_8))
				.as("Aggregate datum stream provided as JSON in aggregate form")
				.isEqualTo(fixtureAggregateStreamJson(meta));
		and.then(filterCaptor.getValue().getAggregation()).as("Aggregation passed to historian")
				.isEqualTo(Aggregation.Hour);
	}

	@Test
	public void stream_aggregate_cbor() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		final ObjectDatumStreamMetadata meta = fixtureMeta();
		givenAggregateStream(filtered, meta, fixtureAggregateJsonTimestamp());

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(STREAM_PATH).param("aggregation", "Day").accept(MediaType.APPLICATION_CBOR))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentType()).as("CBOR content").isEqualTo(MediaType.APPLICATION_CBOR_VALUE);
		and.then(cborJson(res.getContentAsByteArray()))
				.as("Aggregate datum stream provided as CBOR in aggregate form")
				.isEqualTo(fixtureAggregateStreamJson(meta));
		and.then(filterCaptor.getValue().getAggregation()).as("Aggregation passed to historian")
				.isEqualTo(Aggregation.Day);
	}

	@Test
	public void stream_aggregate_csv() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		final ObjectDatumStreamMetadata meta = fixtureMeta();
		givenAggregateStream(filtered, meta, fixtureAggregateCsvTimestamp());

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(STREAM_PATH).param("aggregation", "Month").accept(TEXT_CSV)).andReturn()
				.getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentType()).as("CSV content").isEqualTo(TEXT_CSV.toString());
		and.then(res.getContentAsString(StandardCharsets.UTF_8))
				.as("Aggregate datum stream provided as CSV in aggregate form")
				.isEqualTo(fixtureAggregateStreamCsv(meta));
		and.then(filterCaptor.getValue().getAggregation()).as("Aggregation passed to historian")
				.isEqualTo(Aggregation.Month);
	}

	@Test
	public void stream_minuteAggregate_json() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		final ObjectDatumStreamMetadata meta = fixtureMeta();
		givenAggregateStream(filtered, meta, fixtureAggregateJsonTimestamp());

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(STREAM_PATH).param("aggregation", "15m").accept(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentAsString(StandardCharsets.UTF_8))
				.as("Sub-hour aggregate datum stream provided as JSON in aggregate form")
				.isEqualTo(fixtureAggregateStreamJson(meta));
		and.then(filterCaptor.getValue().getAggregation())
				.as("Aggregation key passed to historian as aggregation")
				.isEqualTo(Aggregation.FifteenMinute);
	}

	/**
	 * Stub the filtered historian to refuse a stream query, as it refuses a
	 * sub-hour aggregation over too long a time span.
	 */
	private void givenStreamRefused(String message) throws IOException {
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		willThrow(new IllegalArgumentException(message)).given(filtered)
				.findFilteredStream(filterCaptor.capture(), any(), isNull(), isNull(), isNull());
	}

	private static final String RANGE_REFUSED_MESSAGE = "Minute level aggregation time span must be at most 840 hours.";

	private static final String RANGE_REFUSED_JSON = """
			{"success":false,"message":"Illegal argument: \
			Minute level aggregation time span must be at most 840 hours."}""";

	@Test
	public void stream_minuteAggregate_rangeRefused() throws Exception {
		// GIVEN
		givenStreamRefused(RANGE_REFUSED_MESSAGE);

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(STREAM_PATH).param("aggregation", "FiveMinute")
						.param("startDate", "2026-01-01").param("endDate", "2026-03-01")
						.accept(MediaType.APPLICATION_JSON).header(HttpHeaders.ACCEPT_ENCODING, "gzip"))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("Refused range can not be processed, as in SolarQuery")
				.isEqualTo(422);
		and.then(res.getContentType()).as("JSON content").isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		and.then(res.getHeader(HttpHeaders.CONTENT_ENCODING)).as("Error result not compressed").isNull();
		and.then(res.getContentAsString()).as("Error result provided, as in SolarQuery")
				.isEqualTo(RANGE_REFUSED_JSON);
		and.then(filterCaptor.getValue().getAggregation()).as("Aggregation passed to historian")
				.isEqualTo(Aggregation.FiveMinute);
	}

	@Test
	public void stream_minuteAggregate_rangeRefused_cbor() throws Exception {
		// GIVEN
		givenStreamRefused(RANGE_REFUSED_MESSAGE);

		// WHEN
		final MockHttpServletResponse res = mvc.perform(
				get(STREAM_PATH).param("aggregation", "FiveMinute").param("startDate", "2026-01-01")
						.param("endDate", "2026-03-01").accept(MediaType.APPLICATION_CBOR))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("Refused range can not be processed").isEqualTo(422);
		and.then(res.getContentType()).as("CBOR content").isEqualTo(MediaType.APPLICATION_CBOR_VALUE);
		and.then(cborJson(res.getContentAsByteArray())).as("Error result provided as CBOR")
				.isEqualTo(RANGE_REFUSED_JSON);
	}

	@Test
	public void stream_minuteAggregate_rangeRefused_csv() throws Exception {
		// GIVEN
		givenStreamRefused(RANGE_REFUSED_MESSAGE);

		// WHEN
		final MockHttpServletResponse res = mvc.perform(
				get(STREAM_PATH).param("aggregation", "FiveMinute").param("startDate", "2026-01-01")
						.param("endDate", "2026-03-01").accept(TEXT_CSV))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("Refused range can not be processed").isEqualTo(422);
		and.then(res.getContentType()).as("CSV content").startsWith(TEXT_CSV.toString());
		and.then(res.getContentAsString()).as("Error result provided as CSV").isEqualTo("""
				message,success\r
				Illegal argument: Minute level aggregation time span must be at most 840 hours.,false\r
				""");
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

	@Test
	public void reading_json() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		final ObjectDatumStreamMetadata meta = fixtureMeta();
		givenReadingStream(filtered, meta, fixtureAggregateJsonTimestamp());

		// WHEN
		final MockHttpServletResponse res = mvc.perform(
				get(READING_PATH).param("readingType", "Difference").accept(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentType()).as("JSON content").isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		and.then(res.getHeader(HttpHeaders.CONTENT_ENCODING)).as("Not compressed").isNull();
		and.then(res.getContentAsString(StandardCharsets.UTF_8))
				.as("Reading stream provided as JSON in reading form")
				.isEqualTo(fixtureReadingStreamJson(meta));
		final NodeDatumFilter filter = filterCaptor.getValue();
		and.then(filter.getReadingType()).as("Reading type passed to historian")
				.isEqualTo(DatumReadingType.Difference);
		and.then(filter.getObjectKind()).as("Node streams queried").isEqualTo(ObjectDatumKind.Node);
		and.then(filter.getObjectIds()).as("The node's own ID queried").containsExactly(NODE_ID);
		and.then(filter.getAggregation()).as("No aggregation").isNull();
	}

	@Test
	public void reading_cbor() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		final ObjectDatumStreamMetadata meta = fixtureMeta();
		givenReadingStream(filtered, meta, fixtureAggregateJsonTimestamp());

		// WHEN
		final MockHttpServletResponse res = mvc.perform(
				get(READING_PATH).param("readingType", "Difference").accept(MediaType.APPLICATION_CBOR))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentType()).as("CBOR content").isEqualTo(MediaType.APPLICATION_CBOR_VALUE);
		and.then(cborJson(res.getContentAsByteArray()))
				.as("Reading stream provided as CBOR in reading form")
				.isEqualTo(fixtureReadingStreamJson(meta));
	}

	@Test
	public void reading_noAccept() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		final ObjectDatumStreamMetadata meta = fixtureMeta();
		givenReadingStream(filtered, meta, fixtureAggregateJsonTimestamp());

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(READING_PATH).param("readingType", "Difference")).andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentType()).as("CBOR content when no Accept header")
				.isEqualTo(MediaType.APPLICATION_CBOR_VALUE);
		and.then(cborJson(res.getContentAsByteArray())).as("Reading stream provided as CBOR")
				.isEqualTo(fixtureReadingStreamJson(meta));
	}

	@Test
	public void reading_csv() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		final ObjectDatumStreamMetadata meta = fixtureMeta();
		givenReadingStream(filtered, meta, fixtureAggregateCsvTimestamp());

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(READING_PATH).param("readingType", "Difference").accept(TEXT_CSV))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentType()).as("CSV content").isEqualTo(TEXT_CSV.toString());
		and.then(res.getContentAsString(StandardCharsets.UTF_8))
				.as("Reading stream provided as CSV in reading form")
				.isEqualTo(fixtureReadingStreamCsv(meta));
	}

	@Test
	public void reading_gzip() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		final ObjectDatumStreamMetadata meta = fixtureMeta();
		givenReadingStream(filtered, meta, fixtureAggregateJsonTimestamp());

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(READING_PATH).param("readingType", "Difference")
						.accept(MediaType.APPLICATION_JSON).header(HttpHeaders.ACCEPT_ENCODING, "gzip"))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getHeader(HttpHeaders.CONTENT_ENCODING)).as("Compressed with gzip")
				.isEqualTo("gzip");
		and.then(gunzip(res.getContentAsByteArray())).as("Reading stream provided as compressed JSON")
				.isEqualTo(fixtureReadingStreamJson(meta));
	}

	@Test
	public void reading_criteria() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		givenReadingStream(filtered, fixtureMeta(), fixtureAggregateJsonTimestamp());

		// WHEN
		final MockHttpServletResponse res = mvc.perform(get(READING_PATH)
				.param("readingType", "Difference").param("tolerance", "P1D")
				.param("nodeId", NODE_ID.toString()).param("sourceIds", "a/*,b")
				.param("startDate", "2026-11-01T00:00:00Z").param("endDate", "2026-11-02T00:00:00+13:00")
				.param("aggregation", "Hour").param("max", "10").param("offset", "5")
				.param("withoutTotalResultsCount", "false").param("sorts[0].sortKey", "created")
				.param("sorts[0].descending", "true").param("sorts[1].sortKey", "source")
				.accept(MediaType.APPLICATION_JSON)).andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		final NodeDatumFilter filter = filterCaptor.getValue();
		and.then(filter.getReadingType()).as("Reading type parsed")
				.isEqualTo(DatumReadingType.Difference);
		and.then(filter.getObjectKind()).as("Node streams queried").isEqualTo(ObjectDatumKind.Node);
		and.then(filter.getObjectIds()).as("The node's own ID queried").containsExactly(NODE_ID);
		and.then(filter.getSourceIds()).as("Source IDs parsed from list").containsExactly("a/*", "b");
		and.then(filter.getStartDate()).as("Start date parsed")
				.isEqualTo(Instant.parse("2026-11-01T00:00:00Z"));
		and.then(filter.getEndDate()).as("End date parsed with offset")
				.isEqualTo(Instant.parse("2026-11-01T11:00:00Z"));
		and.then(filter.getAggregation()).as("Aggregation parsed").isEqualTo(Aggregation.Hour);
		and.then(filter.getMax()).as("Max parsed").isEqualTo(10);
		and.then(filter.getOffset()).as("Offset parsed").isEqualTo(5L);
		and.then(filter.isWithoutTotalResultsCount()).as("Without total results count parsed").isFalse();
		and.then(filter.getSorts()).as("Sorts mapped to historian sort keys").containsExactly(
				new SimpleSortDescriptor(FilterableDatumHistorian.SORT_BY_TIME, true),
				new SimpleSortDescriptor(FilterableDatumHistorian.SORT_BY_SOURCE, false));
	}

	@Test
	public void reading_localDates() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		givenReadingStream(filtered, fixtureMeta(), fixtureAggregateJsonTimestamp());

		// WHEN
		final MockHttpServletResponse res = mvc.perform(get(READING_PATH)
				.param("readingType", "Difference").param("localStartDate", "2026-01-01")
				.param("localEndDate", "2026-02-01").accept(MediaType.APPLICATION_JSON)).andReturn()
				.getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		final NodeDatumFilter filter = filterCaptor.getValue();
		and.then(filter.getLocalStartDate()).as("Local start date passed to historian")
				.isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
		and.then(filter.getLocalEndDate()).as("Local end date passed to historian")
				.isEqualTo(LocalDateTime.of(2026, 2, 1, 0, 0));
		and.then(filter.getStartDate()).as("Local start date not converted to an instant").isNull();
		and.then(filter.getEndDate()).as("Local end date not converted to an instant").isNull();
	}

	@Test
	public void reading_ownNodeAmongOthers() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		final ObjectDatumStreamMetadata meta = fixtureMeta();
		givenReadingStream(filtered, meta, fixtureAggregateJsonTimestamp());

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(READING_PATH).param("readingType", "Difference")
						.param("nodeIds", "1," + NODE_ID + ",2").accept(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentAsString(StandardCharsets.UTF_8)).as("Readings of the node provided")
				.isEqualTo(fixtureReadingStreamJson(meta));
		and.then(filterCaptor.getValue().getObjectIds()).as("Only the node's own ID queried")
				.containsExactly(NODE_ID);
	}

	@Test
	public void reading_otherNodesOnly() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(READING_PATH).param("readingType", "Difference").param("nodeIds", "1,2")
						.accept(MediaType.APPLICATION_JSON))
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
	public void reading_notAssociated() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(null);

		// WHEN
		final MockHttpServletResponse res = mvc.perform(
				get(READING_PATH).param("readingType", "Difference").accept(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentAsString()).as("Empty results provided when not associated").isEqualTo("""
				{"success":true}""");
		then(filteredService).shouldHaveNoInteractions();
	}

	@Test
	public void reading_historianNotAvailable() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);

		// WHEN
		final MockHttpServletResponse res = mvc.perform(
				get(READING_PATH).param("readingType", "Difference").accept(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentAsString()).as("Empty results provided without a historian").isEqualTo("""
				{"success":true}""");
	}

	@Test
	public void reading_unfiltered() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(unfilteredService.service()).willReturn(unfiltered);
		final ObjectDatumStreamMetadata meta = fixtureMeta();
		givenReadingStream(unfiltered, meta, fixtureAggregateJsonTimestamp());

		// WHEN
		final MockHttpServletResponse res = mvc.perform(get("/unfiltered" + READING_PATH)
				.param("readingType", "Difference").accept(MediaType.APPLICATION_JSON)).andReturn()
				.getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentAsString(StandardCharsets.UTF_8))
				.as("Reading stream of unfiltered historian provided")
				.isEqualTo(fixtureReadingStreamJson(meta));
		then(filteredService).shouldHaveNoInteractions();
	}

	@Test
	public void reading_apiPath() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		final ObjectDatumStreamMetadata meta = fixtureMeta();
		givenReadingStream(filtered, meta, fixtureAggregateJsonTimestamp());

		// WHEN
		final MockHttpServletResponse res = mvc.perform(get("/api/v1/sec/datum/stream/reading")
				.param("readingType", "Difference").accept(MediaType.APPLICATION_JSON)).andReturn()
				.getResponse();

		// THEN
		and.then(res.getStatus()).as("OK").isEqualTo(200);
		and.then(res.getContentAsString(StandardCharsets.UTF_8))
				.as("Reading stream provided under API path").isEqualTo(fixtureReadingStreamJson(meta));
	}

	@Test
	public void reading_toleranceIgnored() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		final ObjectDatumStreamMetadata meta = fixtureMeta();
		givenReadingStream(filtered, meta, fixtureAggregateJsonTimestamp());

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(READING_PATH).param("readingType", "Difference")
						.param("tolerance", "not a period").accept(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK with a tolerance that is not used").isEqualTo(200);
		and.then(res.getContentAsString(StandardCharsets.UTF_8)).as("Reading stream provided")
				.isEqualTo(fixtureReadingStreamJson(meta));
	}

	@Test
	public void reading_ignoredParameters() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		givenReadingStream(filtered, fixtureMeta(), fixtureAggregateJsonTimestamp());

		// WHEN
		final MockHttpServletResponse res = mvc.perform(
				get(READING_PATH).param("readingType", "Difference").param("aggregation", "Fortnightly")
						.param("combiningType", "Sum").param("metadataFilter", "(foo=bar)")
						.param("foo", "bar").accept(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK with unsupported parameters").isEqualTo(200);
		and.then(filterCaptor.getValue().getAggregation()).as("Unknown aggregation ignored").isNull();
	}

	@Test
	public void reading_missingReadingType() throws Exception {
		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(READING_PATH).accept(MediaType.APPLICATION_JSON)).andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("Missing reading type is a bad request, as in SolarQuery")
				.isEqualTo(400);
		and.then(res.getContentType()).as("JSON content").isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		and.then(res.getContentAsString()).as("Error result provided").isEqualTo("""
				{"success":false,"message":"Required request parameter 'readingType' for method \
				parameter type DatumReadingType is not present"}""");
		then(identityService).shouldHaveNoInteractions();
		then(filteredService).shouldHaveNoInteractions();
	}

	@Test
	public void reading_missingReadingType_cbor() throws Exception {
		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(READING_PATH).accept(MediaType.APPLICATION_CBOR)).andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("Missing reading type is a bad request").isEqualTo(400);
		and.then(res.getContentType()).as("CBOR content").isEqualTo(MediaType.APPLICATION_CBOR_VALUE);
		and.then(cborJson(res.getContentAsByteArray())).as("Error result provided as CBOR").isEqualTo("""
				{"success":false,"message":"Required request parameter 'readingType' for method \
				parameter type DatumReadingType is not present"}""");
	}

	@Test
	public void reading_emptyReadingType() throws Exception {
		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(READING_PATH).param("readingType", "").accept(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("Empty reading type is a bad request").isEqualTo(400);
		and.then(res.getContentAsString()).as("Error result provided").startsWith("""
				{"success":false,"message":"Required request parameter 'readingType'""");
		then(identityService).shouldHaveNoInteractions();
		then(filteredService).shouldHaveNoInteractions();
	}

	@Test
	public void reading_invalidReadingType() throws Exception {
		// WHEN
		final MockHttpServletResponse res = mvc.perform(
				get(READING_PATH).param("readingType", "delta").accept(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("Unknown reading type rejected, as in SolarQuery").isEqualTo(422);
		and.then(res.getContentAsString()).as("Error result provided")
				.startsWith("{\"success\":false,\"message\":\"Illegal argument: ");
		then(identityService).shouldHaveNoInteractions();
		then(filteredService).shouldHaveNoInteractions();
	}

	@Test
	public void reading_unsupportedReadingType() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		willThrow(new UnsupportedOperationException(
				"The NearestDifference reading type is not supported.")).given(filtered)
						.findFilteredStreamReadings(filterCaptor.capture(), any(), isNull(), isNull(),
								isNull());

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(READING_PATH).param("readingType", "NearestDifference")
						.accept(MediaType.APPLICATION_JSON).header(HttpHeaders.ACCEPT_ENCODING, "gzip"))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("Unsupported reading type can not be processed").isEqualTo(422);
		and.then(res.getContentType()).as("JSON content").isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		and.then(res.getHeader(HttpHeaders.CONTENT_ENCODING)).as("Error result not compressed").isNull();
		and.then(res.getContentAsString()).as("Error result provided").isEqualTo("""
				{"success":false,"code":"422",\
				"message":"The NearestDifference reading type is not supported."}""");
		and.then(filterCaptor.getValue().getReadingType()).as("Reading type passed to historian")
				.isEqualTo(DatumReadingType.NearestDifference);
	}

	@Test
	public void reading_minuteAggregate_refused() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		willThrow(new IllegalArgumentException(
				"Only Hour/Day/Month aggregation is supported for reading queries.")).given(filtered)
						.findFilteredStreamReadings(filterCaptor.capture(), any(), isNull(), isNull(),
								isNull());

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(READING_PATH).param("readingType", "Difference").param("aggregation", "5m")
						.param("startDate", "2026-01-01").param("endDate", "2026-01-02")
						.accept(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus())
				.as("Sub-hour aggregation reading query can not be processed, as in SolarQuery")
				.isEqualTo(422);
		and.then(res.getContentType()).as("JSON content").isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		and.then(res.getContentAsString()).as("Error result provided").isEqualTo("""
				{"success":false,"message":"Illegal argument: \
				Only Hour/Day/Month aggregation is supported for reading queries."}""");
		and.then(filterCaptor.getValue().getAggregation()).as("Aggregation passed to historian")
				.isEqualTo(Aggregation.FiveMinute);
	}

	@Test
	public void reading_unsupportedReadingType_otherNodesOnly() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);

		// WHEN
		final MockHttpServletResponse res = mvc
				.perform(get(READING_PATH).param("readingType", "NearestDifference").param("nodeId", "1")
						.accept(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("OK, as nothing is queried").isEqualTo(200);
		and.then(res.getContentAsString()).as("Empty results provided for other nodes").isEqualTo("""
				{"success":true}""");
		then(filteredService).shouldHaveNoInteractions();
	}

	@Test
	public void reading_xml() throws Exception {
		// WHEN
		final MockHttpServletResponse res = mvc.perform(
				get(READING_PATH).param("readingType", "Difference").accept(MediaType.APPLICATION_XML))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("XML is not acceptable").isEqualTo(406);
		then(identityService).shouldHaveNoInteractions();
		then(filteredService).shouldHaveNoInteractions();
	}

	@Test
	public void reading_errorAfterOutput() throws Exception {
		// GIVEN
		given(identityService.getNodeId()).willReturn(NODE_ID);
		given(filteredService.service()).willReturn(filtered);
		willAnswer(manyDatumAnswer(fixtureMeta(), new DataAccessResourceFailureException("Lost")))
				.given(filtered).findFilteredStreamReadings(any(), any(), isNull(), isNull(), isNull());

		// WHEN
		final MockHttpServletResponse res = mvc.perform(
				get(READING_PATH).param("readingType", "Difference").accept(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// THEN
		and.then(res.getStatus()).as("Response started as success").isEqualTo(200);
		and.then(res.getContentAsString(StandardCharsets.UTF_8))
				.as("Partial results left incomplete, without an error result added")
				.startsWith("{\"success\":true,\"meta\":[").doesNotEndWith("]}")
				.doesNotContain("\"success\":false");
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
