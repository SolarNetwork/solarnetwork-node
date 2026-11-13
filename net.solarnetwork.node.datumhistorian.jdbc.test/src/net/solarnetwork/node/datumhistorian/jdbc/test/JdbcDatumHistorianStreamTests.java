/* ==================================================================
 * JdbcDatumHistorianStreamTests.java - 13 Nov 2026 10:18:37 am
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

package net.solarnetwork.node.datumhistorian.jdbc.test;

import static net.solarnetwork.dao.StreamDatumFilteredResultsProcessor.METADATA_PROVIDER_ATTR;
import static net.solarnetwork.node.service.FilterableDatumHistorian.SORT_BY_SOURCE;
import static net.solarnetwork.node.service.FilterableDatumHistorian.SORT_BY_TIME;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.entry;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.thenIllegalArgumentException;
import static org.assertj.core.api.BDDAssertions.tuple;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.inOrder;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.dao.StreamDatumFilteredResultsProcessor;
import net.solarnetwork.domain.SimpleSortDescriptor;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider;
import net.solarnetwork.domain.datum.StreamDatum;
import net.solarnetwork.node.dao.BasicNodeDatumFilter;
import net.solarnetwork.node.datumhistorian.jdbc.JdbcDatumHistorian;
import net.solarnetwork.node.domain.datum.AcEnergyDatum;
import net.solarnetwork.node.domain.datum.AtmosphericDatum;
import net.solarnetwork.node.domain.datum.SimpleAcEnergyDatum;
import net.solarnetwork.node.domain.datum.SimpleAtmosphericDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link JdbcDatumHistorian#findDatumStreamMetadata} and
 * {@link JdbcDatumHistorian#findFilteredStream} methods, using the Postgres
 * test database.
 *
 * <p>
 * Each test starts with the same history as
 * {@link JdbcDatumHistorianFilterTests}.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@RunWith(MockitoJUnitRunner.class)
public class JdbcDatumHistorianStreamTests extends AbstractPostgresHistorianTests {

	private static final Long TEST_NODE_ID = 123L;
	private static final Long TEST_NODE_ID_2 = 234L;
	private static final Long TEST_LOC_ID = 345L;

	@Mock(strictness = Mock.Strictness.LENIENT)
	private IdentityService identityService;

	@Mock(strictness = Mock.Strictness.LENIENT)
	private IdentityService identityService2;

	@Mock
	private StreamDatumFilteredResultsProcessor processor;

	@Captor
	private ArgumentCaptor<Map<String, ?>> attrsCaptor;

	@Captor
	private ArgumentCaptor<StreamDatum> datumCaptor;

	private JdbcDatumHistorian historian;
	private JdbcDatumHistorian otherHistorian;

	private Instant t0;
	private Instant t1;
	private Instant t2;

	@Before
	public void setup() {
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		given(identityService2.getNodeId()).willReturn(TEST_NODE_ID_2);
		historian = new JdbcDatumHistorian(jdbcTemplate, new StaticOptionalService<>(identityService));
		otherHistorian = new JdbcDatumHistorian(jdbcTemplate,
				new StaticOptionalService<>(identityService2));
		t0 = Instant.now().truncatedTo(ChronoUnit.MINUTES).minus(10, ChronoUnit.MINUTES);
		t1 = t0.plus(1, ChronoUnit.MINUTES);
		t2 = t1.plus(1, ChronoUnit.MINUTES);
		populate();
	}

	private static DatumSamples samples(int value) {
		DatumSamples s = new DatumSamples();
		s.putInstantaneousSampleValue("i", value);
		return s;
	}

	private void populate() {
		int i = 0;
		for ( Instant ts : List.of(t0, t1, t2) ) {
			historian.add(new SimpleAcEnergyDatum("a/1", ts, samples(i++)));
			historian.add(new SimpleAcEnergyDatum("a/2", ts, samples(i++)));
			historian.add(SimpleDatum.nodeDatum("b/1", ts, samples(i++)));
		}
		for ( Instant ts : List.of(t0, t1) ) {
			historian.add(new SimpleAtmosphericDatum(TEST_LOC_ID, "weather", ts, samples(i++)));
			otherHistorian.add(SimpleDatum.nodeDatum("a/1", ts, samples(i++)));
		}
		otherHistorian.add(new SimpleAtmosphericDatum(TEST_LOC_ID, "weather", t0, samples(i++)));
		otherHistorian.add(new SimpleAtmosphericDatum(TEST_LOC_ID, "weather", t2, samples(i++)));
	}

	private UUID streamId(Long nodeId, Long locationId, String sourceId) {
		return streamId(JdbcDatumHistorian.DEFAULT_SCHEMA_NAME,
				JdbcDatumHistorian.DEFAULT_STREAM_ID_DOMAIN, nodeId, locationId, sourceId);
	}

	/**
	 * Get the expected stream ID, kind, object ID, and source ID of the
	 * metadata of a stream.
	 *
	 * @see #tuples(Collection)
	 */
	private Tuple meta(Long nodeId, Long locationId, String sourceId) {
		return tuple(streamId(nodeId, locationId, sourceId),
				(locationId != null ? ObjectDatumKind.Location : ObjectDatumKind.Node),
				(locationId != null ? locationId : nodeId), sourceId);
	}

	/**
	 * Get the stream ID, kind, object ID, and source ID of stream metadata.
	 *
	 * @see #meta(Long, Long, String)
	 */
	private static List<Tuple> tuples(Collection<ObjectDatumStreamMetadata> metadata) {
		return metadata.stream()
				.map(m -> tuple(m.getStreamId(), m.getKind(), m.getObjectId(), m.getSourceId()))
				.toList();
	}

	private static DatumId node(Long nodeId, String sourceId, Instant ts) {
		return DatumId.nodeId(nodeId, sourceId, ts);
	}

	private DatumId node(String sourceId, Instant ts) {
		return node(TEST_NODE_ID, sourceId, ts);
	}

	private DatumId weather(Instant ts) {
		return DatumId.locationId(TEST_LOC_ID, "weather", ts);
	}

	/**
	 * Get all the datum IDs of the running node, in the default order.
	 */
	private List<DatumId> allRunningNodeIds() {
		final List<DatumId> result = new ArrayList<>();
		result.add(weather(t0));
		result.add(weather(t1));
		for ( String sourceId : List.of("a/1", "a/2", "b/1") ) {
			for ( Instant ts : List.of(t0, t1, t2) ) {
				result.add(node(sourceId, ts));
			}
		}
		return result;
	}

	private static SortDescriptor desc(String key) {
		return new SimpleSortDescriptor(key, true);
	}

	private static SortDescriptor asc(String key) {
		return new SimpleSortDescriptor(key);
	}

	/**
	 * The results given to the processor.
	 *
	 * @param provider
	 *        the metadata provider given to the processor
	 * @param datum
	 *        the datum given to the processor
	 */
	private record Streamed(ObjectDatumStreamMetadataProvider provider, List<StreamDatum> datum) {

		private ObjectDatumStreamMetadata meta(StreamDatum d) {
			final ObjectDatumStreamMetadata meta = provider.metadataForStreamId(d.getStreamId());
			// @formatter:off
			and.then(meta)
				.as("Metadata provided for stream %s of datum", d.getStreamId())
				.isNotNull()
				;
			// @formatter:on
			return meta;
		}

		/**
		 * Get the IDs of the datum, from the metadata of their streams.
		 */
		private List<DatumId> ids() {
			final List<DatumId> ids = new ArrayList<>(datum.size());
			for ( StreamDatum d : datum ) {
				final ObjectDatumStreamMetadata meta = meta(d);
				ids.add(DatumId.datumId(meta.getKind(), meta.getObjectId(), meta.getSourceId(),
						d.getTimestamp()));
			}
			return ids;
		}

		/**
		 * Get the {@code i} property values of the datum.
		 */
		private List<Integer> values() {
			final List<Integer> values = new ArrayList<>(datum.size());
			for ( StreamDatum d : datum ) {
				final Object value = properties(d, DatumSamplesType.Instantaneous).get("i");
				values.add(value instanceof BigDecimal n ? n.intValue() : null);
			}
			return values;
		}

		/**
		 * Get the property values of a datum by name, from the metadata of its
		 * stream.
		 */
		private Map<String, Object> properties(StreamDatum d, DatumSamplesType type) {
			final String[] names = meta(d).propertyNamesForType(type);
			final Object[] values = switch (type) {
				case Instantaneous -> d.getProperties().getInstantaneous();
				case Accumulating -> d.getProperties().getAccumulating();
				case Status -> d.getProperties().getStatus();
				default -> null;
			};
			final Map<String, Object> result = new LinkedHashMap<>(8);
			if ( names != null ) {
				for ( int i = 0; i < names.length; i++ ) {
					result.put(names[i], (values != null && i < values.length ? values[i] : null));
				}
			}
			return result;
		}

		private List<UUID> streamIds() {
			return datum.stream().map(StreamDatum::getStreamId).toList();
		}

	}

	/**
	 * Verify the processor was started and then only given datum.
	 *
	 * @param totalResults
	 *        the expected total result count
	 * @param startingOffset
	 *        the expected starting offset
	 * @return what the processor was given
	 */
	private Streamed thenStreamed(Long totalResults, Integer startingOffset) throws IOException {
		final InOrder ordered = inOrder(processor);
		// @formatter:off
		then(processor).should(ordered).start(eq(totalResults), eq(startingOffset), isNull(),
				attrsCaptor.capture());
		then(processor).should(ordered, atLeast(0)).handleResultItem(datumCaptor.capture());
		then(processor).shouldHaveNoMoreInteractions();
		final Map<String, ?> attrs = attrsCaptor.getValue();
		and.then(attrs)
			.as("Only metadata provider attribute given")
			.containsOnlyKeys(METADATA_PROVIDER_ATTR)
			.extractingByKey(METADATA_PROVIDER_ATTR)
			.isInstanceOf(ObjectDatumStreamMetadataProvider.class)
			;
		// @formatter:on
		return new Streamed((ObjectDatumStreamMetadataProvider) attrs.get(METADATA_PROVIDER_ATTR),
				datumCaptor.getAllValues());
	}

	/* ==================================================================
	 * findDatumStreamMetadata
	 * ==================================================================
	 */

	@Test
	public void metadata_nullFilter() {
		thenIllegalArgumentException().isThrownBy(() -> historian.findDatumStreamMetadata(null));
	}

	@Test
	public void metadata_runningNode() {
		// WHEN
		Collection<ObjectDatumStreamMetadata> result = historian
				.findDatumStreamMetadata(new BasicNodeDatumFilter());

		// THEN
		// @formatter:off
		and.then(tuples(result))
			.as("Metadata of all streams of running node, by node, location, and source")
			.containsExactly(
					meta(TEST_NODE_ID, TEST_LOC_ID, "weather"),
					meta(TEST_NODE_ID, null, "a/1"),
					meta(TEST_NODE_ID, null, "a/2"),
					meta(TEST_NODE_ID, null, "b/1"))
			;
		for ( ObjectDatumStreamMetadata meta : result ) {
			and.then(meta)
				.as("System time zone of stream %s", meta.getSourceId())
				.returns(ZoneId.systemDefault().getId(), from(ObjectDatumStreamMetadata::getTimeZoneId))
				;
			and.then(meta.propertyNamesForType(DatumSamplesType.Instantaneous))
				.as("Instantaneous property names of stream %s", meta.getSourceId())
				.containsExactly("i")
				;
			and.then(meta.propertyNamesForType(DatumSamplesType.Accumulating))
				.as("No accumulating property names of stream %s", meta.getSourceId())
				.isNull()
				;
			and.then(meta.propertyNamesForType(DatumSamplesType.Status))
				.as("No status property names of stream %s", meta.getSourceId())
				.isNull()
				;
		}
		// @formatter:on
	}

	@Test
	public void metadata_noNodeId() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(null);

		// WHEN
		Collection<ObjectDatumStreamMetadata> result = historian
				.findDatumStreamMetadata(new BasicNodeDatumFilter());

		// THEN
		// @formatter:off
		and.then(result)
			.as("No metadata without node ID")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void metadata_sourceIds_pattern() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setSourceIds(new String[] { "A/*" });

		// WHEN
		Collection<ObjectDatumStreamMetadata> result = historian.findDatumStreamMetadata(filter);

		// THEN
		// @formatter:off
		and.then(tuples(result))
			.as("Metadata of streams matching source pattern, case-insensitively")
			.containsExactly(meta(TEST_NODE_ID, null, "a/1"), meta(TEST_NODE_ID, null, "a/2"))
			;
		// @formatter:on
	}

	@Test
	public void metadata_types() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setObjectKind(ObjectDatumKind.Node);
		filter.setObjectIds(new Long[] { TEST_NODE_ID, TEST_NODE_ID_2 });
		filter.setType(AcEnergyDatum.class.getName());

		// WHEN
		Collection<ObjectDatumStreamMetadata> result = historian.findDatumStreamMetadata(filter);

		// THEN
		// @formatter:off
		and.then(tuples(result))
			.as("Metadata of streams that stored datum of the given type")
			.containsExactly(meta(TEST_NODE_ID, null, "a/1"), meta(TEST_NODE_ID, null, "a/2"))
			;
		// @formatter:on
	}

	@Test
	public void metadata_nodeIds() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setObjectKind(ObjectDatumKind.Node);
		filter.setObjectIds(new Long[] { TEST_NODE_ID, TEST_NODE_ID_2 });

		// WHEN
		Collection<ObjectDatumStreamMetadata> result = historian.findDatumStreamMetadata(filter);

		// THEN
		// @formatter:off
		and.then(tuples(result))
			.as("Metadata of node streams of all given nodes")
			.containsExactly(
					meta(TEST_NODE_ID, null, "a/1"),
					meta(TEST_NODE_ID, null, "a/2"),
					meta(TEST_NODE_ID, null, "b/1"),
					meta(TEST_NODE_ID_2, null, "a/1"))
			;
		// @formatter:on
	}

	@Test
	public void metadata_locationIds() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setObjectKind(ObjectDatumKind.Location);
		filter.setObjectId(TEST_LOC_ID);

		// WHEN
		Collection<ObjectDatumStreamMetadata> result = historian.findDatumStreamMetadata(filter);

		// THEN
		// @formatter:off
		and.then(tuples(result))
			.as("Metadata of location streams stored by all nodes")
			.containsExactly(
					meta(TEST_NODE_ID, TEST_LOC_ID, "weather"),
					meta(TEST_NODE_ID_2, TEST_LOC_ID, "weather"))
			;
		// @formatter:on
	}

	@Test
	public void metadata_streamIds() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setStreamIds(new UUID[] { streamId(TEST_NODE_ID, null, "a/2"),
				streamId(TEST_NODE_ID, TEST_LOC_ID, "weather") });

		// WHEN
		Collection<ObjectDatumStreamMetadata> result = historian.findDatumStreamMetadata(filter);

		// THEN
		// @formatter:off
		and.then(tuples(result))
			.as("Metadata of given streams, by node, location, and source")
			.containsExactly(
					meta(TEST_NODE_ID, TEST_LOC_ID, "weather"),
					meta(TEST_NODE_ID, null, "a/2"))
			;
		// @formatter:on
	}

	@Test
	public void metadata_streamIds_notDecoded() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setStreamIds(new UUID[] { streamId(TEST_NODE_ID_2, null, "a/1"),
				streamId(JdbcDatumHistorian.DEFAULT_SCHEMA_NAME,
						JdbcDatumHistorian.UNFILTERED_STREAM_ID_DOMAIN, TEST_NODE_ID, null, "a/1") });

		// WHEN
		Collection<ObjectDatumStreamMetadata> result = historian.findDatumStreamMetadata(filter);

		// THEN
		// @formatter:off
		and.then(result)
			.as("No metadata for stream IDs of other node and historian domain")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void metadata_otherDomain() {
		// GIVEN
		JdbcDatumHistorian domainHistorian = new JdbcDatumHistorian(jdbcTemplate,
				new StaticOptionalService<>(identityService));
		domainHistorian.setStreamIdDomain(JdbcDatumHistorian.UNFILTERED_STREAM_ID_DOMAIN);
		final UUID streamId = streamId(JdbcDatumHistorian.DEFAULT_SCHEMA_NAME,
				JdbcDatumHistorian.UNFILTERED_STREAM_ID_DOMAIN, TEST_NODE_ID, null, "a/1");
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setStreamIds(new UUID[] { streamId, streamId(TEST_NODE_ID, null, "a/2") });

		// WHEN
		Collection<ObjectDatumStreamMetadata> result = domainHistorian.findDatumStreamMetadata(filter);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Only stream ID of historian domain decoded, and given to metadata")
			.extracting(ObjectDatumStreamMetadata::getStreamId)
			.containsExactly(streamId)
			;
		// @formatter:on
	}

	/* ==================================================================
	 * findFilteredStream
	 * ==================================================================
	 */

	@Test
	public void stream_runningNode() throws IOException {
		// WHEN
		historian.findFilteredStream(new BasicNodeDatumFilter(), processor, null, null, null);

		// THEN
		final Streamed result = thenStreamed(null, null);
		// @formatter:off
		and.then(result.provider().metadataStreamIds())
			.as("Metadata of all streams of running node given")
			.containsExactly(
					streamId(TEST_NODE_ID, TEST_LOC_ID, "weather"),
					streamId(TEST_NODE_ID, null, "a/1"),
					streamId(TEST_NODE_ID, null, "a/2"),
					streamId(TEST_NODE_ID, null, "b/1"))
			;
		and.then(result.ids())
			.as("All datum of running node given, by node, location, source, and time")
			.containsExactlyElementsOf(allRunningNodeIds())
			;
		// @formatter:on
	}

	@Test
	public void stream_values() throws IOException {
		// GIVEN
		DatumSamples s = new DatumSamples();
		s.putInstantaneousSampleValue("watts", new BigDecimal("1.5"));
		s.putInstantaneousSampleValue("nan", Double.NaN);
		s.putAccumulatingSampleValue("wattHours", 100);
		s.putStatusSampleValue("mode", "on");
		s.putStatusSampleValue("count", 3);
		s.putStatusSampleValue("flag", true);
		s.putStatusSampleValue("ratio", new BigDecimal("1.50"));
		s.addTag("t1");
		s.addTag("t2");
		historian.add(SimpleDatum.nodeDatum("c/1", t0, s));
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setSourceId("c/1");

		// WHEN
		historian.findFilteredStream(filter, processor, null, null, null);

		// THEN
		final Streamed result = thenStreamed(null, null);
		// @formatter:off
		and.then(result.datum())
			.as("Datum of source given")
			.hasSize(1)
			;
		final StreamDatum d = result.datum().get(0);
		and.then(d)
			.as("Datum has stream ID of its stream")
			.returns(streamId(TEST_NODE_ID, null, "c/1"), from(StreamDatum::getStreamId))
			.as("Datum has timestamp")
			.returns(t0, from(StreamDatum::getTimestamp))
			;
		and.then(result.properties(d, DatumSamplesType.Instantaneous))
			.as("Instantaneous values, with NaN given as null")
			.containsOnly(entry("watts", new BigDecimal("1.5")), entry("nan", null))
			;
		and.then(result.properties(d, DatumSamplesType.Accumulating))
			.as("Accumulating values")
			.containsOnly(entry("wattHours", new BigDecimal("100")))
			;
		and.then(result.properties(d, DatumSamplesType.Status))
			.as("Status values given as strings, numbers as stored")
			.containsOnly(entry("mode", "on"), entry("count", "3"), entry("flag", "true"),
					entry("ratio", "1.50"))
			;
		and.then(d.getProperties().getTags())
			.as("Tags given")
			.containsExactlyInAnyOrder("t1", "t2")
			;
		// @formatter:on
	}

	@Test
	public void stream_pagination_totalResults() throws IOException {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setWithoutTotalResultsCount(false);
		filter.setOffset(2L);
		filter.setMax(4);

		// WHEN
		historian.findFilteredStream(filter, processor, null, null, null);

		// THEN
		final List<DatumId> all = allRunningNodeIds();
		final Streamed result = thenStreamed((long) all.size(), 2);
		// @formatter:off
		and.then(result.ids())
			.as("Page of datum given")
			.containsExactlyElementsOf(all.subList(2, 6))
			;
		// @formatter:on
	}

	@Test
	public void stream_pagination_withoutTotalResults() throws IOException {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setMax(3);

		// WHEN
		historian.findFilteredStream(filter, processor, null, null, null);

		// THEN
		final Streamed result = thenStreamed(null, 0);
		// @formatter:off
		and.then(result.ids())
			.as("First page of datum given")
			.containsExactlyElementsOf(allRunningNodeIds().subList(0, 3))
			;
		// @formatter:on
	}

	@Test
	public void stream_pagination_arguments() throws IOException {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setWithoutTotalResultsCount(false);
		filter.setSorts(List.of(asc(SORT_BY_SOURCE)));
		filter.setOffset(5L);
		filter.setMax(2);

		// WHEN
		historian.findFilteredStream(filter, processor, List.of(desc(SORT_BY_TIME), asc(SORT_BY_SOURCE)),
				1L, 3);

		// THEN
		final Streamed result = thenStreamed(11L, 1);
		// @formatter:off
		and.then(result.ids())
			.as("Arguments used in place of filter pagination")
			.containsExactly(node("a/2", t2), node("b/1", t2), node("a/1", t1))
			;
		// @formatter:on
	}

	@Test
	public void stream_mostRecent() throws IOException {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setMostRecent(true);

		// WHEN
		historian.findFilteredStream(filter, processor, null, null, null);

		// THEN
		final Streamed result = thenStreamed(null, null);
		// @formatter:off
		and.then(result.ids())
			.as("Most recent datum of each stream given")
			.containsExactly(weather(t1), node("a/1", t2), node("a/2", t2), node("b/1", t2))
			;
		// @formatter:on
	}

	@Test
	public void stream_sourceIds_pattern() throws IOException {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setSourceIds(new String[] { "A/*" });

		// WHEN
		historian.findFilteredStream(filter, processor, null, null, null);

		// THEN
		final Streamed result = thenStreamed(null, null);
		// @formatter:off
		and.then(result.provider().metadataStreamIds())
			.as("Metadata of streams matching source pattern given")
			.containsExactly(streamId(TEST_NODE_ID, null, "a/1"), streamId(TEST_NODE_ID, null, "a/2"))
			;
		and.then(result.ids())
			.as("Datum matching source pattern given")
			.containsExactlyElementsOf(allRunningNodeIds().subList(2, 8))
			;
		// @formatter:on
	}

	@Test
	public void stream_streamIds() throws IOException {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setStreamIds(new UUID[] { streamId(TEST_NODE_ID, null, "b/1"),
				streamId(TEST_NODE_ID, TEST_LOC_ID, "weather") });

		// WHEN
		historian.findFilteredStream(filter, processor, null, null, null);

		// THEN
		final Streamed result = thenStreamed(null, null);
		// @formatter:off
		and.then(result.provider().metadataStreamIds())
			.as("Metadata of given streams given")
			.containsExactly(streamId(TEST_NODE_ID, TEST_LOC_ID, "weather"),
					streamId(TEST_NODE_ID, null, "b/1"))
			;
		and.then(result.ids())
			.as("Datum of given streams given")
			.containsExactly(weather(t0), weather(t1), node("b/1", t0), node("b/1", t1), node("b/1", t2))
			;
		// @formatter:on
	}

	@Test
	public void stream_types() throws IOException {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setType(AtmosphericDatum.class.getName());

		// WHEN
		historian.findFilteredStream(filter, processor, null, null, null);

		// THEN
		final Streamed result = thenStreamed(null, null);
		// @formatter:off
		and.then(result.ids())
			.as("Datum of given type given")
			.containsExactly(weather(t0), weather(t1))
			;
		// @formatter:on
	}

	@Test
	public void stream_locationIds() throws IOException {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setObjectKind(ObjectDatumKind.Location);
		filter.setObjectId(TEST_LOC_ID);

		// WHEN
		historian.findFilteredStream(filter, processor, null, null, null);

		// THEN
		final Streamed result = thenStreamed(null, null);
		final UUID streamId = streamId(TEST_NODE_ID, TEST_LOC_ID, "weather");
		final UUID otherStreamId = streamId(TEST_NODE_ID_2, TEST_LOC_ID, "weather");
		// @formatter:off
		and.then(result.provider().metadataStreamIds())
			.as("Metadata of location streams of all nodes given")
			.containsExactly(streamId, otherStreamId)
			;
		and.then(result.ids())
			.as("Location datum of all nodes merged, one per timestamp")
			.containsExactly(weather(t0), weather(t1), weather(t2))
			;
		and.then(result.values())
			.as("Datum of running node preferred, other node's used where running node has none")
			.containsExactly(9, 11, 14)
			;
		and.then(result.streamIds())
			.as("Each datum has stream ID of stream it was read from")
			.containsExactly(streamId, streamId, otherStreamId)
			;
		// @formatter:on
	}

	@Test
	public void stream_locationIds_mostRecent() throws IOException {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setObjectKind(ObjectDatumKind.Location);
		filter.setObjectId(TEST_LOC_ID);
		filter.setMostRecent(true);

		// WHEN
		historian.findFilteredStream(filter, processor, null, null, null);

		// THEN
		final Streamed result = thenStreamed(null, null);
		// @formatter:off
		and.then(result.ids())
			.as("Most recent merged location datum given")
			.containsExactly(weather(t2))
			;
		and.then(result.streamIds())
			.as("Other node's datum given as running node has none as recent")
			.containsExactly(streamId(TEST_NODE_ID_2, TEST_LOC_ID, "weather"))
			;
		// @formatter:on
	}

	@Test
	public void stream_noMatch() throws IOException {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setSourceId("nope");
		filter.setWithoutTotalResultsCount(false);
		filter.setMax(5);

		// WHEN
		historian.findFilteredStream(filter, processor, null, null, null);

		// THEN
		final Streamed result = thenStreamed(0L, 0);
		// @formatter:off
		and.then(result.provider().metadataStreamIds())
			.as("No metadata given")
			.isEmpty()
			;
		and.then(result.datum())
			.as("No datum given")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void stream_noNodeId() throws IOException {
		// GIVEN
		given(identityService.getNodeId()).willReturn(null);

		// WHEN
		historian.findFilteredStream(new BasicNodeDatumFilter(), processor, null, null, null);

		// THEN
		final Streamed result = thenStreamed(null, null);
		// @formatter:off
		and.then(result.provider().metadataStreamIds())
			.as("No metadata given without node ID")
			.isEmpty()
			;
		and.then(result.datum())
			.as("No datum given without node ID")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void stream_moreThanFetchSize() throws IOException {
		// GIVEN
		historian.setFetchSize(10);
		final int count = historian.getFetchSize() * 2 + 5;
		for ( int i = 0; i < count; i++ ) {
			historian.add(SimpleDatum.nodeDatum("big", t0.plusSeconds(i), samples(i)));
		}
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setSourceId("big");

		// WHEN
		historian.findFilteredStream(filter, processor, null, null, null);

		// THEN
		final Streamed result = thenStreamed(null, null);
		// @formatter:off
		and.then(result.values())
			.as("All datum given, in time order")
			.containsExactlyElementsOf(IntStream.range(0, count).boxed().toList())
			;
		// @formatter:on
	}

	@Test
	public void stream_transactionTemplate() throws IOException {
		// GIVEN
		// the template joins the test transaction
		final TransactionTemplate tt = new TransactionTemplate(
				new DataSourceTransactionManager(dataSource()));
		tt.setReadOnly(true);
		historian.setTransactionTemplate(tt);

		// WHEN
		historian.findFilteredStream(new BasicNodeDatumFilter(), processor, null, null, null);

		// THEN
		final Streamed result = thenStreamed(null, null);
		// @formatter:off
		and.then(result.ids())
			.as("All datum of running node given within transaction")
			.containsExactlyElementsOf(allRunningNodeIds())
			;
		// @formatter:on
	}

}
