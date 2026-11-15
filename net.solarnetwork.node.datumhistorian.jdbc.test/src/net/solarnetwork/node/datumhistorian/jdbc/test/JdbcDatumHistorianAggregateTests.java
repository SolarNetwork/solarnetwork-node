/* ==================================================================
 * JdbcDatumHistorianAggregateTests.java - 15 Nov 2026 6:40:18 pm
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

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.stream.Collectors.joining;
import static net.solarnetwork.dao.StreamDatumFilteredResultsProcessor.METADATA_PROVIDER_ATTR;
import static net.solarnetwork.domain.datum.ObjectDatumKind.Location;
import static net.solarnetwork.domain.datum.ObjectDatumKind.Node;
import static net.solarnetwork.node.service.FilterableDatumHistorian.SORT_BY_SOURCE;
import static net.solarnetwork.node.service.FilterableDatumHistorian.SORT_BY_TIME;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.fail;
import static org.assertj.core.api.BDDAssertions.tuple;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.inOrder;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import net.solarnetwork.dao.StreamDatumFilteredResultsProcessor;
import net.solarnetwork.domain.SimpleSortDescriptor;
import net.solarnetwork.domain.datum.AggregateStreamDatum;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.BasicAggregateStreamDatum;
import net.solarnetwork.domain.datum.BasicReadingStreamDatum;
import net.solarnetwork.domain.datum.BasicStreamDatum;
import net.solarnetwork.domain.datum.DatumPropertiesStatistics;
import net.solarnetwork.domain.datum.DatumReadingType;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider;
import net.solarnetwork.domain.datum.StreamDatum;
import net.solarnetwork.node.dao.BasicNodeDatumFilter;
import net.solarnetwork.node.datumhistorian.jdbc.JdbcDatumHistorian;
import net.solarnetwork.node.domain.datum.SimpleAcEnergyDatum;
import net.solarnetwork.node.domain.datum.SimpleAtmosphericDatum;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the stored aggregate and reading results of
 * {@link JdbcDatumHistorian#findFilteredStream} and
 * {@link JdbcDatumHistorian#findFilteredStreamReadings}, the time zones the
 * historian stores, and the local date criteria in those zones, using the
 * Postgres test database.
 *
 * <p>
 * Tests named {@code agg_} need the aggregate setup of
 * {@code postgres-init-agg.sql} in the {@code solarhist} schema, tests named
 * {@code noAgg_} need a schema without it, and the reading tests need the
 * reading functions of {@code postgres-init.sql}. Each test is skipped
 * otherwise, so running the tests against a database with the setup and
 * against one without it covers them all.
 * </p>
 *
 * <p>
 * The aggregate and reading values expected are those SolarNet's functions
 * give for the same datum, apart from an hour SolarNet's stale processing does
 * not compute either: the one that ends at a stream's first datum.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@RunWith(MockitoJUnitRunner.class)
public class JdbcDatumHistorianAggregateTests extends AbstractPostgresHistorianTests {

	private static final String SCHEMA = JdbcDatumHistorian.DEFAULT_SCHEMA_NAME;
	private static final Long TEST_NODE_ID = 123L;
	private static final Long TEST_NODE_ID_2 = 234L;
	private static final Long TEST_LOC_ID = 345L;
	private static final ZoneId AUCKLAND = ZoneId.of("Pacific/Auckland");

	/** The time of the first datum, 11am on 1 February in Auckland. */
	private static final Instant T22 = Instant.parse("2026-01-31T22:00:00Z");
	private static final Instant T22_30 = T22.plus(30, MINUTES);
	private static final Instant T23 = T22.plus(1, HOURS);
	private static final Instant T00 = T22.plus(2, HOURS);
	private static final Instant T01 = T22.plus(3, HOURS);
	private static final Instant JAN = Instant.parse("2026-01-01T00:00:00Z");
	private static final Instant JAN_31 = Instant.parse("2026-01-31T00:00:00Z");
	private static final Instant FEB_1 = Instant.parse("2026-02-01T00:00:00Z");

	/** The start of 1 February, and of February, in Auckland. */
	private static final Instant FEB_1_AUCKLAND = Instant.parse("2026-01-31T11:00:00Z");

	// the local dates of the start of January, February, and March
	private static final LocalDateTime LOCAL_JAN = LocalDateTime.of(2026, 1, 1, 0, 0);
	private static final LocalDateTime LOCAL_FEB = LocalDateTime.of(2026, 2, 1, 0, 0);
	private static final LocalDateTime LOCAL_MAR = LocalDateTime.of(2026, 3, 1, 0, 0);

	// the hourly aggregates of the running node's streams, as kind, object ID, source ID,
	// time, data_i, data_a, stat_i, and read_a
	private static final Tuple H_W22 = tuple(Location, TEST_LOC_ID, "weather", T22, "{10}", null,
			"{{1,10,10}}", null);
	private static final Tuple H_W23 = tuple(Location, TEST_LOC_ID, "weather", T23, "{14}", null,
			"{{1,14,14}}", null);
	private static final Tuple H_A1_22 = tuple(Node, TEST_NODE_ID, "a/1", T22, "{1}", "{30}",
			"{{3,0,2}}", "{{30,100,130}}");
	private static final Tuple H_A1_23 = tuple(Node, TEST_NODE_ID, "a/1", T23, "{4}", "{30}",
			"{{3,3,5}}", "{{30,130,160}}");
	private static final Tuple H_A1_00 = tuple(Node, TEST_NODE_ID, "a/1", T00, "{7}", "{30}",
			"{{3,6,8}}", "{{30,160,190}}");
	private static final Tuple H_A1_01 = tuple(Node, TEST_NODE_ID, "a/1", T01, "{10}", "{20}",
			"{{3,9,11}}", "{{20,190,210}}");
	private static final Tuple H_A2_22 = tuple(Node, TEST_NODE_ID, "a/2", T22, "{5}", "{0}",
			"{{1,5,5}}", "{{0,1000,1000}}");

	// the hourly aggregates of the other node's streams
	private static final Tuple H_O_W22 = tuple(Location, TEST_LOC_ID, "weather", T22, "{12}", null,
			"{{2,11,13}}", null);
	private static final Tuple H_O_A1_22 = tuple(Node, TEST_NODE_ID_2, "a/1", T22, "{1}", "{20}",
			"{{1,1,1}}", "{{20,10,30}}");
	private static final Tuple H_O_A1_23 = tuple(Node, TEST_NODE_ID_2, "a/1", T23, "{3}", "{0}",
			"{{1,3,3}}", "{{0,30,30}}");

	// the daily aggregates of the running node's streams, in UTC
	private static final Tuple D_W = tuple(Location, TEST_LOC_ID, "weather", JAN_31, "{12}", null,
			"{{2,10,14}}", null);
	private static final Tuple D_A1_31 = tuple(Node, TEST_NODE_ID, "a/1", JAN_31, "{2.5}", "{60}",
			"{{6,0,5}}", "{{60,100,160}}");
	private static final Tuple D_A1_01 = tuple(Node, TEST_NODE_ID, "a/1", FEB_1, "{8.5}", "{50}",
			"{{6,6,11}}", "{{50,160,210}}");
	private static final Tuple D_A2 = tuple(Node, TEST_NODE_ID, "a/2", JAN_31, "{5}", "{0}",
			"{{1,5,5}}", "{{0,1000,1000}}");

	// the monthly aggregates of the running node's streams, in UTC
	private static final Tuple M_W = tuple(Location, TEST_LOC_ID, "weather", JAN, "{12}", null,
			"{{2,10,14}}", null);
	private static final Tuple M_A1_JAN = tuple(Node, TEST_NODE_ID, "a/1", JAN, "{2.5}", "{60}",
			"{{6,0,5}}", "{{60,100,160}}");
	private static final Tuple M_A1_FEB = tuple(Node, TEST_NODE_ID, "a/1", FEB_1, "{8.5}", "{50}",
			"{{6,6,11}}", "{{50,160,210}}");
	private static final Tuple M_A2 = tuple(Node, TEST_NODE_ID, "a/2", JAN, "{5}", "{0}",
			"{{1,5,5}}", "{{0,1000,1000}}");

	// the daily and monthly aggregates of the running node's streams, in Auckland
	private static final Tuple NZ_W = tuple(Location, TEST_LOC_ID, "weather", FEB_1_AUCKLAND, "{12}",
			null, "{{2,10,14}}", null);
	private static final Tuple NZ_A1 = tuple(Node, TEST_NODE_ID, "a/1", FEB_1_AUCKLAND, "{5.5}",
			"{110}", "{{12,0,11}}", "{{110,100,210}}");
	private static final Tuple NZ_A2 = tuple(Node, TEST_NODE_ID, "a/2", FEB_1_AUCKLAND, "{5}", "{0}",
			"{{1,5,5}}", "{{0,1000,1000}}");

	// the reading differences from 22:10 to 01:10, as kind, object ID, source ID, time, end
	// time, data_i, data_a, and read_a
	private static final Instant READING_START = T22.plus(10, MINUTES);
	private static final Instant READING_END = T01.plus(10, MINUTES);
	private static final Tuple R_A1 = tuple(Node, TEST_NODE_ID, "a/1", T22, T01, null, "{90}",
			"{{90,100,190}}");
	private static final Tuple R_A2 = tuple(Node, TEST_NODE_ID, "a/2", T22_30, T22_30, null, "{0}",
			"{{0,1000,1000}}");
	private static final Tuple R_O_A1 = tuple(Node, TEST_NODE_ID_2, "a/1", T22, T23, null, "{20}",
			"{{20,10,30}}");

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

	private boolean aggregates;
	private boolean readings;
	private JdbcDatumHistorian historian;
	private JdbcDatumHistorian otherHistorian;

	@Before
	public void setup() {
		aggregates = jdbcTemplate.queryForObject("SELECT to_regclass(?) IS NOT NULL", Boolean.class,
				SCHEMA + ".dh_agg_hourly");
		readings = jdbcTemplate.queryForObject("SELECT to_regproc(?) IS NOT NULL", Boolean.class,
				SCHEMA + ".dh_find_datum_diff_rows");
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		given(identityService2.getNodeId()).willReturn(TEST_NODE_ID_2);
		historian = historian(SCHEMA, identityService, ZoneOffset.UTC);
		otherHistorian = historian(SCHEMA, identityService2, ZoneOffset.UTC);
	}

	private void assumeAggregates() {
		assumeTrue("The %s schema does not have the aggregate setup.".formatted(SCHEMA), aggregates);
	}

	private void assumeNoAggregates() {
		assumeFalse("The %s schema has the aggregate setup.".formatted(SCHEMA), aggregates);
	}

	private void assumeReadings() {
		assumeTrue("The %s schema does not have the reading functions.".formatted(SCHEMA), readings);
	}

	/**
	 * Create and start a historian.
	 */
	private JdbcDatumHistorian historian(String schema, IdentityService ident, ZoneId zone) {
		final JdbcDatumHistorian h = new JdbcDatumHistorian(jdbcTemplate, schema,
				new StaticOptionalService<>(ident));
		h.setTimeZone(zone);
		h.serviceDidStartup();
		return h;
	}

	private static DatumSamples energy(int watts, int wattHours) {
		final DatumSamples s = new DatumSamples();
		s.putInstantaneousSampleValue("watts", watts);
		s.putAccumulatingSampleValue("wattHours", wattHours);
		return s;
	}

	private static DatumSamples temperature(int temp) {
		final DatumSamples s = new DatumSamples();
		s.putInstantaneousSampleValue("temp", temp);
		return s;
	}

	/**
	 * Add the datum of the tests, and with the aggregate setup compute their
	 * aggregates.
	 *
	 * <p>
	 * The running node stores {@code a/1} every 20 minutes from 22:00 to 01:40
	 * UTC, with {@code watts} counting up from 0 and {@code wattHours} from 100
	 * by 10, one {@code a/2} datum, and two datum of a location. The other node
	 * stores two datum of its own {@code a/1} and two of the same location.
	 * </p>
	 */
	private void populate() {
		for ( int k = 0; k < 12; k++ ) {
			historian.add(new SimpleAcEnergyDatum("a/1", T22.plus(20L * k, MINUTES),
					energy(k, 100 + 10 * k)));
		}
		historian.add(new SimpleAcEnergyDatum("a/2", T22_30, energy(5, 1000)));
		historian.add(new SimpleAtmosphericDatum(TEST_LOC_ID, "weather", T22, temperature(10)));
		historian.add(new SimpleAtmosphericDatum(TEST_LOC_ID, "weather", T23, temperature(14)));
		otherHistorian.add(new SimpleAcEnergyDatum("a/1", T22, energy(1, 10)));
		otherHistorian.add(new SimpleAcEnergyDatum("a/1", T23, energy(3, 30)));
		otherHistorian.add(new SimpleAtmosphericDatum(TEST_LOC_ID, "weather", T22, temperature(11)));
		otherHistorian
				.add(new SimpleAtmosphericDatum(TEST_LOC_ID, "weather", T22_30, temperature(13)));
		if ( aggregates ) {
			processAll(SCHEMA);
		}
	}

	/**
	 * Process the stale aggregates of a schema: hours, then days, then months.
	 */
	private void processAll(String schema) {
		for ( String kind : List.of("h", "d", "M") ) {
			int count = 0;
			while ( !jdbcTemplate
					.queryForList("SELECT * FROM %s.dh_process_one_agg_stale(?)".formatted(schema), kind)
					.isEmpty() ) {
				if ( ++count > 1000 ) {
					fail("The %s stale aggregates of %s were not all processed.", kind, schema);
				}
			}
		}
	}

	/**
	 * Get the stale aggregates of a schema, as kind, node ID, location ID, source
	 * ID, and time.
	 */
	private List<Tuple> stale(String schema) {
		return jdbcTemplate.query("""
				SELECT s.agg_kind, m.node_id, m.loc_id, m.source_id, s.ts_start
				FROM %s.dh_agg_stale s
				INNER JOIN %s.dh_datum_meta m ON m.stream_id = s.stream_id
				ORDER BY s.agg_kind, m.node_id, m.loc_id, m.source_id, s.ts_start
				""".formatted(schema, schema),
				(rs, i) -> tuple(rs.getString(1), rs.getObject(2, Long.class),
						rs.getObject(3, Long.class), rs.getString(4), rs.getTimestamp(5).toInstant()));
	}

	/**
	 * Get the stored time zone of a node, and the row's {@code ctid}, which
	 * changes when the row is updated.
	 */
	private Map<String, Object> storedTimeZone(String schema, Long nodeId) {
		return jdbcTemplate.queryForMap(
				"SELECT time_zone, ctid::TEXT AS ctid FROM %s.dh_node_time_zone WHERE node_id = ?"
						.formatted(schema),
				nodeId);
	}

	private static BasicNodeDatumFilter filter(Aggregation aggregation) {
		final BasicNodeDatumFilter f = new BasicNodeDatumFilter();
		f.setAggregation(aggregation);
		return f;
	}

	private static BasicNodeDatumFilter readingFilter(Aggregation aggregation) {
		final BasicNodeDatumFilter f = filter(aggregation);
		f.setReadingType(DatumReadingType.Difference);
		return f;
	}

	private UUID streamId(Long nodeId, Long locationId, String sourceId) {
		return streamId(SCHEMA, JdbcDatumHistorian.DEFAULT_STREAM_ID_DOMAIN, nodeId, locationId,
				sourceId);
	}

	/**
	 * Format numbers as Postgres formats a {@code NUMERIC} array.
	 */
	private static String pg(BigDecimal[] values) {
		if ( values == null ) {
			return null;
		}
		return Arrays.stream(values)
				.map(n -> n == null ? "NULL" : n.stripTrailingZeros().toPlainString())
				.collect(joining(",", "{", "}"));
	}

	/**
	 * Format numbers as Postgres formats a two-dimensional {@code NUMERIC}
	 * array.
	 */
	private static String pg(BigDecimal[][] values) {
		if ( values == null ) {
			return null;
		}
		return Arrays.stream(values).map(JdbcDatumHistorianAggregateTests::pg)
				.collect(joining(",", "{", "}"));
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
		 * Get the kind, object ID, source ID, and time of the datum.
		 */
		private List<Tuple> ids() {
			return datum.stream().map(d -> {
				final ObjectDatumStreamMetadata meta = meta(d);
				return tuple(meta.getKind(), meta.getObjectId(), meta.getSourceId(), d.getTimestamp());
			}).toList();
		}

		/**
		 * Get the kind, object ID, source ID, time, instantaneous and
		 * accumulating values, and instantaneous and accumulating statistics of
		 * aggregate datum.
		 */
		private List<Tuple> aggregates() {
			return datum.stream().map(d -> {
				final ObjectDatumStreamMetadata meta = meta(d);
				final DatumPropertiesStatistics stats = ((AggregateStreamDatum) d).getStatistics();
				return tuple(meta.getKind(), meta.getObjectId(), meta.getSourceId(), d.getTimestamp(),
						pg(d.getProperties().getInstantaneous()),
						pg(d.getProperties().getAccumulating()), pg(stats.getInstantaneous()),
						pg(stats.getAccumulating()));
			}).toList();
		}

		/**
		 * Get the kind, object ID, source ID, time, end time, instantaneous and
		 * accumulating values, and accumulating statistics of reading datum.
		 */
		private List<Tuple> readings() {
			return datum.stream().map(d -> {
				final ObjectDatumStreamMetadata meta = meta(d);
				final AggregateStreamDatum r = (AggregateStreamDatum) d;
				return tuple(meta.getKind(), meta.getObjectId(), meta.getSourceId(), d.getTimestamp(),
						r.getEndTimestamp(), pg(d.getProperties().getInstantaneous()),
						pg(d.getProperties().getAccumulating()),
						pg(r.getStatistics().getAccumulating()));
			}).toList();
		}

		private List<Instant> endTimestamps() {
			return datum.stream().map(d -> ((AggregateStreamDatum) d).getEndTimestamp()).toList();
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
			;
		// @formatter:on
		return new Streamed((ObjectDatumStreamMetadataProvider) attrs.get(METADATA_PROVIDER_ATTR),
				datumCaptor.getAllValues());
	}

	private Streamed thenAggregatesStreamed(Long totalResults, Integer startingOffset)
			throws IOException {
		final Streamed result = thenStreamed(totalResults, startingOffset);
		// @formatter:off
		and.then(result.datum())
			.as("Aggregate datum given")
			.allSatisfy(d -> and.then(d).isExactlyInstanceOf(BasicAggregateStreamDatum.class))
			;
		and.then(result.endTimestamps())
			.as("No end timestamps")
			.containsOnlyNulls()
			;
		// @formatter:on
		return result;
	}

	private Streamed thenReadingsStreamed(Long totalResults, Integer startingOffset)
			throws IOException {
		final Streamed result = thenStreamed(totalResults, startingOffset);
		// @formatter:off
		and.then(result.datum())
			.as("Reading datum given")
			.allSatisfy(d -> and.then(d).isExactlyInstanceOf(BasicReadingStreamDatum.class))
			;
		// @formatter:on
		return result;
	}

	/*
	 * ==================================================================
	 * Time zone
	 * ==================================================================
	 */

	@Test
	public void timeZone_storedAtStartup() {
		// THEN
		// @formatter:off
		and.then(storedTimeZone(SCHEMA, TEST_NODE_ID))
			.as("Time zone of running node stored at startup, with or without aggregate setup")
			.containsEntry("time_zone", "UTC")
			;
		and.then(storedTimeZone(SCHEMA, TEST_NODE_ID_2))
			.as("Time zone of other node stored at startup")
			.containsEntry("time_zone", "UTC")
			;
		// @formatter:on
	}

	@Test
	public void timeZone_storedOnlyWhenChanged() {
		// GIVEN
		populate();
		final Map<String, Object> stored = storedTimeZone(SCHEMA, TEST_NODE_ID);

		// WHEN
		historian(SCHEMA, identityService, ZoneOffset.UTC);
		final Map<String, Object> storedSame = storedTimeZone(SCHEMA, TEST_NODE_ID);
		final JdbcDatumHistorian changed = historian(SCHEMA, identityService, AUCKLAND);
		final Map<String, Object> storedChanged = storedTimeZone(SCHEMA, TEST_NODE_ID);

		// THEN
		// @formatter:off
		and.then(storedSame)
			.as("Same time zone not updated")
			.isEqualTo(stored)
			;
		and.then(storedChanged)
			.as("Changed time zone stored")
			.containsEntry("time_zone", "Pacific/Auckland")
			;
		and.then(changed.findDatumStreamMetadata(new BasicNodeDatumFilter()))
			.as("Stream metadata of the running node")
			.hasSize(3)
			.extracting(ObjectDatumStreamMetadata::getTimeZoneId)
			.as("Stream metadata time zone is the zone stored")
			.containsOnly("Pacific/Auckland")
			;
		// @formatter:on
	}

	@Test
	public void agg_timeZone_changed_daysStale() {
		// GIVEN
		assumeAggregates();
		populate();

		// WHEN
		historian(SCHEMA, identityService, ZoneOffset.UTC);
		final List<Tuple> staleSame = stale(SCHEMA);
		historian(SCHEMA, identityService, AUCKLAND);
		final List<Tuple> staleChanged = stale(SCHEMA);

		// THEN
		// @formatter:off
		and.then(staleSame)
			.as("Nothing stale for same time zone")
			.isEmpty()
			;
		and.then(staleChanged)
			.as("Days of the node's streams in the changed zone stale")
			.containsExactly(
				  tuple("d", TEST_NODE_ID, TEST_LOC_ID, "weather", FEB_1_AUCKLAND)
				, tuple("d", TEST_NODE_ID, null, "a/1", FEB_1_AUCKLAND)
				, tuple("d", TEST_NODE_ID, null, "a/2", FEB_1_AUCKLAND)
			)
			;
		// @formatter:on
	}

	@Test
	public void agg_day_list_timeZoneChanged() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();
		final JdbcDatumHistorian changed = historian(SCHEMA, identityService, AUCKLAND);
		processAll(SCHEMA);

		// WHEN
		changed.findFilteredStream(filter(Aggregation.Day), processor, null, null, null);

		// THEN
		// @formatter:off
		and.then(thenAggregatesStreamed(null, null).aggregates())
			.as("Days computed again in the changed zone, replacing the days in the previous zone")
			.containsExactly(NZ_W, NZ_A1, NZ_A2)
			;
		// @formatter:on
	}

	@Test
	public void agg_month_list_timeZoneChanged() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();
		final JdbcDatumHistorian changed = historian(SCHEMA, identityService, AUCKLAND);
		processAll(SCHEMA);

		// WHEN
		changed.findFilteredStream(filter(Aggregation.Month), processor, null, null, null);

		// THEN
		// @formatter:off
		and.then(thenAggregatesStreamed(null, null).aggregates())
			.as("Months computed again in the changed zone, replacing the months in the previous zone")
			.containsExactly(NZ_W, NZ_A1, NZ_A2)
			;
		// @formatter:on
	}

	/*
	 * ==================================================================
	 * Aggregate lists
	 * ==================================================================
	 */

	@Test
	public void agg_hour_list() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();

		// WHEN
		historian.findFilteredStream(filter(Aggregation.Hour), processor, null, null, null);

		// THEN
		final Streamed result = thenAggregatesStreamed(null, null);
		// @formatter:off
		and.then(result.aggregates())
			.as("Hourly aggregates of running node's streams, in default order")
			.containsExactly(H_W22, H_W23, H_A1_22, H_A1_23, H_A1_00, H_A1_01, H_A2_22)
			;
		and.then(result.provider().metadataStreamIds())
			.as("Metadata of running node's streams given")
			.containsExactlyInAnyOrder(streamId(TEST_NODE_ID, TEST_LOC_ID, "weather"),
					streamId(TEST_NODE_ID, null, "a/1"), streamId(TEST_NODE_ID, null, "a/2"))
			;
		// @formatter:on
	}

	@Test
	public void agg_day_list() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();

		// WHEN
		historian.findFilteredStream(filter(Aggregation.Day), processor, null, null, null);

		// THEN
		// @formatter:off
		and.then(thenAggregatesStreamed(null, null).aggregates())
			.as("Daily aggregates of running node's streams, in UTC")
			.containsExactly(D_W, D_A1_31, D_A1_01, D_A2)
			;
		// @formatter:on
	}

	@Test
	public void agg_month_list() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();

		// WHEN
		historian.findFilteredStream(filter(Aggregation.Month), processor, null, null, null);

		// THEN
		// @formatter:off
		and.then(thenAggregatesStreamed(null, null).aggregates())
			.as("Monthly aggregates of running node's streams, in UTC")
			.containsExactly(M_W, M_A1_JAN, M_A1_FEB, M_A2)
			;
		// @formatter:on
	}

	@Test
	public void agg_hour_list_dateRange() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();
		final BasicNodeDatumFilter f = filter(Aggregation.Hour);
		f.setStartDate(T23);
		f.setEndDate(T01);

		// WHEN
		historian.findFilteredStream(f, processor, null, null, null);

		// THEN
		// @formatter:off
		and.then(thenAggregatesStreamed(null, null).aggregates())
			.as("Hours starting from start date (inclusive) to end date (exclusive)")
			.containsExactly(H_W23, H_A1_23, H_A1_00)
			;
		// @formatter:on
	}

	@Test
	public void agg_hour_list_pagination() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();
		final BasicNodeDatumFilter f = filter(Aggregation.Hour);
		f.setWithoutTotalResultsCount(false);

		// WHEN
		historian.findFilteredStream(f, processor, null, 2L, 3);

		// THEN
		// @formatter:off
		and.then(thenAggregatesStreamed(7L, 2).aggregates())
			.as("Page of hourly aggregates, with total count of aggregates")
			.containsExactly(H_A1_22, H_A1_23, H_A1_00)
			;
		// @formatter:on
	}

	@Test
	public void agg_hour_list_sorts() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();

		// WHEN
		historian.findFilteredStream(filter(Aggregation.Hour), processor,
				List.of(new SimpleSortDescriptor(SORT_BY_TIME, true),
						new SimpleSortDescriptor(SORT_BY_SOURCE)),
				null, null);

		// THEN
		// @formatter:off
		and.then(thenAggregatesStreamed(null, null).aggregates())
			.as("Hourly aggregates by time descending, then source")
			.containsExactly(H_A1_01, H_A1_00, H_A1_23, H_W23, H_A1_22, H_A2_22, H_W22)
			;
		// @formatter:on
	}

	@Test
	public void agg_hour_list_sourcePattern() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();
		final BasicNodeDatumFilter f = filter(Aggregation.Hour);
		f.setSourceIds(new String[] { "a/*" });

		// WHEN
		historian.findFilteredStream(f, processor, null, null, null);

		// THEN
		// @formatter:off
		and.then(thenAggregatesStreamed(null, null).aggregates())
			.as("Hourly aggregates of streams matching source pattern")
			.containsExactly(H_A1_22, H_A1_23, H_A1_00, H_A1_01, H_A2_22)
			;
		// @formatter:on
	}

	@Test
	public void agg_hour_list_streamIds() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();
		final BasicNodeDatumFilter f = filter(Aggregation.Hour);
		f.setStreamIds(new UUID[] { streamId(TEST_NODE_ID, null, "a/2") });

		// WHEN
		historian.findFilteredStream(f, processor, null, null, null);

		// THEN
		// @formatter:off
		and.then(thenAggregatesStreamed(null, null).aggregates())
			.as("Hourly aggregates of stream ID")
			.containsExactly(H_A2_22)
			;
		// @formatter:on
	}

	@Test
	public void agg_hour_list_otherNode() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();
		final BasicNodeDatumFilter f = filter(Aggregation.Hour);
		f.setObjectKind(ObjectDatumKind.Node);
		f.setObjectIds(new Long[] { TEST_NODE_ID_2 });

		// WHEN
		historian.findFilteredStream(f, processor, null, null, null);

		// THEN
		// @formatter:off
		and.then(thenAggregatesStreamed(null, null).aggregates())
			.as("Hourly aggregates of other node's node streams")
			.containsExactly(H_O_A1_22, H_O_A1_23)
			;
		// @formatter:on
	}

	@Test
	public void agg_hour_list_location() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();
		final BasicNodeDatumFilter f = filter(Aggregation.Hour);
		f.setObjectKind(ObjectDatumKind.Location);
		f.setObjectIds(new Long[] { TEST_LOC_ID });

		// WHEN
		historian.findFilteredStream(f, processor, null, null, null);

		// THEN
		final Streamed result = thenAggregatesStreamed(null, null);
		final UUID runningNodeStreamId = streamId(TEST_NODE_ID, TEST_LOC_ID, "weather");
		// @formatter:off
		and.then(result.aggregates())
			.as("Hourly aggregates of the location streams of each node, not merged")
			.containsExactly(H_W22, H_W23, H_O_W22)
			;
		and.then(result.streamIds())
			.as("Stream of each node")
			.containsExactly(runningNodeStreamId, runningNodeStreamId,
					streamId(TEST_NODE_ID_2, TEST_LOC_ID, "weather"))
			;
		// @formatter:on
	}

	@Test
	public void agg_hour_list_mostRecent() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();
		final BasicNodeDatumFilter f = filter(Aggregation.Hour);
		f.setMostRecent(true);

		// WHEN
		historian.findFilteredStream(f, processor, null, null, null);

		// THEN
		// @formatter:off
		and.then(thenAggregatesStreamed(null, null).aggregates())
			.as("Most recent hourly aggregate of each stream")
			.containsExactly(H_W23, H_A1_01, H_A2_22)
			;
		// @formatter:on
	}

	@Test
	public void agg_unsupportedAggregation_raw() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();

		// WHEN
		historian.findFilteredStream(filter(Aggregation.FiveMinute), processor, null, null, null);

		// THEN
		final Streamed result = thenStreamed(null, null);
		// @formatter:off
		and.then(result.datum())
			.as("Raw datum of running node for aggregation without stored aggregates")
			.hasSize(15)
			.allSatisfy(d -> and.then(d).isExactlyInstanceOf(BasicStreamDatum.class))
			;
		// @formatter:on
	}

	@Test
	public void agg_unfilteredSchema_day_list() throws IOException {
		// GIVEN
		final String schema = JdbcDatumHistorian.UNFILTERED_SCHEMA_NAME;
		assumeTrue("The %s schema does not have the aggregate setup.".formatted(schema),
				jdbcTemplate.queryForObject("SELECT to_regclass(?) IS NOT NULL", Boolean.class,
						schema + ".dh_agg_hourly"));
		final JdbcDatumHistorian h = new JdbcDatumHistorian(jdbcTemplate, schema,
				new StaticOptionalService<>(identityService));
		h.setStreamIdDomain(JdbcDatumHistorian.UNFILTERED_STREAM_ID_DOMAIN);
		h.setTimeZone(AUCKLAND);
		h.serviceDidStartup();
		h.add(new SimpleAcEnergyDatum("a/1", T22, energy(1, 100)));
		h.add(new SimpleAcEnergyDatum("a/1", T22_30, energy(3, 110)));
		processAll(schema);

		// WHEN
		h.findFilteredStream(filter(Aggregation.Day), processor, null, null, null);

		// THEN
		// @formatter:off
		and.then(storedTimeZone(schema, TEST_NODE_ID))
			.as("Time zone stored in historian schema")
			.containsEntry("time_zone", "Pacific/Auckland")
			;
		and.then(thenAggregatesStreamed(null, null).aggregates())
			.as("Daily aggregates of historian schema, in stored zone")
			.containsExactly(tuple(Node, TEST_NODE_ID, "a/1", FEB_1_AUCKLAND, "{2}", "{10}",
					"{{2,1,3}}", "{{10,100,110}}"))
			;
		// @formatter:on
	}

	/*
	 * ==================================================================
	 * Aggregate readings
	 * ==================================================================
	 */

	@Test
	public void agg_hour_readings() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();

		// WHEN
		historian.findFilteredStreamReadings(readingFilter(Aggregation.Hour), processor, null, null,
				null);

		// THEN
		final Streamed result = thenReadingsStreamed(null, null);
		// @formatter:off
		and.then(result.aggregates())
			.as("Hourly aggregates of running node's streams, with read_a as accumulating statistics")
			.containsExactly(H_W22, H_W23, H_A1_22, H_A1_23, H_A1_00, H_A1_01, H_A2_22)
			;
		and.then(result.endTimestamps())
			.as("No end timestamps for aggregate readings")
			.containsOnlyNulls()
			;
		// @formatter:on
	}

	@Test
	public void agg_day_readings() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();

		// WHEN
		historian.findFilteredStreamReadings(readingFilter(Aggregation.Day), processor, null, null,
				null);

		// THEN
		final Streamed result = thenReadingsStreamed(null, null);
		// @formatter:off
		and.then(result.aggregates())
			.as("Daily aggregates of running node's streams")
			.containsExactly(D_W, D_A1_31, D_A1_01, D_A2)
			;
		and.then(result.endTimestamps())
			.as("No end timestamps for aggregate readings")
			.containsOnlyNulls()
			;
		// @formatter:on
	}

	@Test
	public void agg_month_readings() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();

		// WHEN
		historian.findFilteredStreamReadings(readingFilter(Aggregation.Month), processor, null, null,
				null);

		// THEN
		final Streamed result = thenReadingsStreamed(null, null);
		// @formatter:off
		and.then(result.aggregates())
			.as("Monthly aggregates of running node's streams")
			.containsExactly(M_W, M_A1_JAN, M_A1_FEB, M_A2)
			;
		and.then(result.endTimestamps())
			.as("No end timestamps for aggregate readings")
			.containsOnlyNulls()
			;
		// @formatter:on
	}

	@Test
	public void agg_day_readings_mostRecent_pagination() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();
		final BasicNodeDatumFilter f = readingFilter(Aggregation.Day);
		f.setMostRecent(true);
		f.setWithoutTotalResultsCount(false);

		// WHEN
		historian.findFilteredStreamReadings(f, processor, null, 1L, 5);

		// THEN
		// @formatter:off
		and.then(thenReadingsStreamed(3L, 1).aggregates())
			.as("Most recent day of each stream, after offset")
			.containsExactly(D_A1_01, D_A2)
			;
		// @formatter:on
	}

	/*
	 * ==================================================================
	 * Reading differences
	 * ==================================================================
	 */

	@Test
	public void readings_difference() throws IOException {
		// GIVEN
		assumeReadings();
		populate();
		final BasicNodeDatumFilter f = readingFilter(null);
		f.setStartDate(READING_START);
		f.setEndDate(READING_END);

		// WHEN
		historian.findFilteredStreamReadings(f, processor, null, null, null);

		// THEN
		final Streamed result = thenReadingsStreamed(null, null);
		// @formatter:off
		and.then(result.readings())
			.as("Reading difference of each stream of the running node with readings")
			.containsExactly(R_A1, R_A2)
			;
		and.then(result.provider().metadataStreamIds())
			.as("Metadata of all running node's streams given")
			.hasSize(3)
			;
		// @formatter:on
	}

	@Test
	public void readings_difference_nodeIds_pagination() throws IOException {
		// GIVEN
		assumeReadings();
		populate();
		final BasicNodeDatumFilter f = readingFilter(null);
		f.setObjectKind(ObjectDatumKind.Node);
		f.setObjectIds(new Long[] { TEST_NODE_ID, TEST_NODE_ID_2 });
		f.setStartDate(READING_START);
		f.setEndDate(READING_END);
		f.setWithoutTotalResultsCount(false);

		// WHEN
		historian.findFilteredStreamReadings(f, processor, null, 1L, 2);

		// THEN
		// @formatter:off
		and.then(thenReadingsStreamed(3L, 1).readings())
			.as("Page of reading differences of node streams of each node, with total count of streams with readings")
			.containsExactly(R_A2, R_O_A1)
			;
		// @formatter:on
	}

	@Test
	public void readings_difference_unsupportedAggregation() throws IOException {
		// GIVEN
		assumeReadings();
		populate();
		final BasicNodeDatumFilter f = readingFilter(Aggregation.Week);
		f.setStartDate(READING_START);
		f.setEndDate(READING_END);

		// WHEN
		historian.findFilteredStreamReadings(f, processor, null, null, null);

		// THEN
		// @formatter:off
		and.then(thenReadingsStreamed(null, null).readings())
			.as("Aggregation without stored aggregates treated as no aggregation")
			.containsExactly(R_A1, R_A2)
			;
		// @formatter:on
	}

	/*
	 * ==================================================================
	 * Without aggregate setup
	 * ==================================================================
	 */

	@Test
	public void noAgg_hour_list_raw() throws IOException {
		// GIVEN
		assumeNoAggregates();
		populate();

		// WHEN
		historian.findFilteredStream(filter(Aggregation.Hour), processor, null, null, null);

		// THEN
		final Streamed result = thenStreamed(null, null);
		// @formatter:off
		and.then(result.datum())
			.as("Raw datum without aggregate setup")
			.hasSize(15)
			.allSatisfy(d -> and.then(d).isExactlyInstanceOf(BasicStreamDatum.class))
			;
		and.then(result.ids())
			.as("Raw datum of running node, in default order")
			.startsWith(
				  tuple(Location, TEST_LOC_ID, "weather", T22)
				, tuple(Location, TEST_LOC_ID, "weather", T23)
				, tuple(Node, TEST_NODE_ID, "a/1", T22)
			)
			.endsWith(
				  tuple(Node, TEST_NODE_ID, "a/1", T22.plus(220, MINUTES))
				, tuple(Node, TEST_NODE_ID, "a/2", T22_30)
			)
			;
		// @formatter:on
	}

	@Test
	public void noAgg_day_readings_difference() throws IOException {
		// GIVEN
		assumeNoAggregates();
		assumeReadings();
		populate();
		final BasicNodeDatumFilter f = readingFilter(Aggregation.Day);
		f.setStartDate(READING_START);
		f.setEndDate(READING_END);

		// WHEN
		historian.findFilteredStreamReadings(f, processor, null, null, null);

		// THEN
		// @formatter:off
		and.then(thenReadingsStreamed(null, null).readings())
			.as("Reading difference of each stream without aggregate setup")
			.containsExactly(R_A1, R_A2)
			;
		// @formatter:on
	}

	@Test
	public void noAgg_month_readings_difference_noDates() throws IOException {
		// GIVEN
		assumeNoAggregates();
		assumeReadings();
		populate();

		// WHEN
		historian.findFilteredStreamReadings(readingFilter(Aggregation.Month), processor, null, null,
				null);

		// THEN
		final Instant last = T22.plus(220, MINUTES);
		// @formatter:off
		and.then(thenReadingsStreamed(null, null).readings())
			.as("Reading difference from now to now is of the last readings")
			.containsExactly(
				  tuple(Node, TEST_NODE_ID, "a/1", last, last, null, "{0}", "{{0,210,210}}")
				, tuple(Node, TEST_NODE_ID, "a/2", T22_30, T22_30, null, "{0}", "{{0,1000,1000}}")
			)
			;
		// @formatter:on
	}

	/*
	 * ==================================================================
	 * Startup
	 * ==================================================================
	 */

	private Long storedTimeZoneCount() {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM %s.dh_node_time_zone".formatted(SCHEMA),
				Long.class);
	}

	@Test
	public void startup_noNodeId_timeZoneNotStored() {
		// GIVEN
		final Long count = storedTimeZoneCount();

		// WHEN
		historian(SCHEMA, null, AUCKLAND);

		// THEN
		// @formatter:off
		and.then(storedTimeZoneCount())
			.as("No time zone stored without node ID")
			.isEqualTo(count)
			;
		// @formatter:on
	}

	/*
	 * ==================================================================
	 * Local dates
	 * ==================================================================
	 */

	/**
	 * Add the datum of the tests, with the other node in Auckland.
	 *
	 * <p>
	 * All the datum of the other node are then on 1 February in its zone, while
	 * the datum of the running node are on 31 January and 1 February in UTC.
	 * </p>
	 */
	private void populateOtherNodeInAuckland() {
		otherHistorian = historian(SCHEMA, identityService2, AUCKLAND);
		populate();
	}

	/**
	 * Get a filter of the node streams of both nodes, within a local date
	 * range.
	 */
	private static BasicNodeDatumFilter localDatesFilter(Aggregation aggregation,
			LocalDateTime start, LocalDateTime end) {
		final BasicNodeDatumFilter f = filter(aggregation);
		f.setObjectKind(Node);
		f.setObjectIds(new Long[] { TEST_NODE_ID, TEST_NODE_ID_2 });
		f.setLocalStartDate(start);
		f.setLocalEndDate(end);
		return f;
	}

	@Test
	public void localDates_list_january() throws IOException {
		// GIVEN
		populateOtherNodeInAuckland();

		// WHEN
		historian.findFilteredStream(localDatesFilter(null, LOCAL_JAN, LOCAL_FEB), processor, null,
				null, null);

		// THEN
		// @formatter:off
		and.then(thenStreamed(null, null).ids())
			.as("Datum of January in the zone of each node: those before midnight UTC of the running node, none of the other node")
			.containsExactly(
				  tuple(Node, TEST_NODE_ID, "a/1", T22)
				, tuple(Node, TEST_NODE_ID, "a/1", T22.plus(20, MINUTES))
				, tuple(Node, TEST_NODE_ID, "a/1", T22.plus(40, MINUTES))
				, tuple(Node, TEST_NODE_ID, "a/1", T23)
				, tuple(Node, TEST_NODE_ID, "a/1", T23.plus(20, MINUTES))
				, tuple(Node, TEST_NODE_ID, "a/1", T23.plus(40, MINUTES))
				, tuple(Node, TEST_NODE_ID, "a/2", T22_30)
			)
			;
		// @formatter:on
	}

	@Test
	public void localDates_list_february() throws IOException {
		// GIVEN
		populateOtherNodeInAuckland();

		// WHEN
		historian.findFilteredStream(localDatesFilter(null, LOCAL_FEB, LOCAL_MAR), processor, null,
				null, null);

		// THEN
		// @formatter:off
		and.then(thenStreamed(null, null).ids())
			.as("Datum of February in the zone of each node: those from midnight UTC of the running node, all of the other node")
			.containsExactly(
				  tuple(Node, TEST_NODE_ID, "a/1", T00)
				, tuple(Node, TEST_NODE_ID, "a/1", T00.plus(20, MINUTES))
				, tuple(Node, TEST_NODE_ID, "a/1", T00.plus(40, MINUTES))
				, tuple(Node, TEST_NODE_ID, "a/1", T01)
				, tuple(Node, TEST_NODE_ID, "a/1", T01.plus(20, MINUTES))
				, tuple(Node, TEST_NODE_ID, "a/1", T01.plus(40, MINUTES))
				, tuple(Node, TEST_NODE_ID_2, "a/1", T22)
				, tuple(Node, TEST_NODE_ID_2, "a/1", T23)
			)
			;
		// @formatter:on
	}

	@Test
	public void agg_day_list_localDates_january() throws IOException {
		// GIVEN
		assumeAggregates();
		populateOtherNodeInAuckland();

		// WHEN
		historian.findFilteredStream(localDatesFilter(Aggregation.Day, LOCAL_JAN, LOCAL_FEB),
				processor, null, null, null);

		// THEN
		// @formatter:off
		and.then(thenAggregatesStreamed(null, null).aggregates())
			.as("Days of January in the zone of each node, without the other node's 1 February, which starts on 31 January in UTC")
			.containsExactly(D_A1_31, D_A2)
			;
		// @formatter:on
	}

	@Test
	public void agg_month_list_localDates_february() throws IOException {
		// GIVEN
		assumeAggregates();
		populateOtherNodeInAuckland();

		// WHEN
		historian.findFilteredStream(localDatesFilter(Aggregation.Month, LOCAL_FEB, LOCAL_MAR),
				processor, null, null, null);

		// THEN
		// @formatter:off
		and.then(thenAggregatesStreamed(null, null).ids())
			.as("Months of February in the zone of each node, each starting at midnight there")
			.containsExactly(
				  tuple(Node, TEST_NODE_ID, "a/1", FEB_1)
				, tuple(Node, TEST_NODE_ID_2, "a/1", FEB_1_AUCKLAND)
			)
			;
		// @formatter:on
	}

	@Test
	public void readings_difference_localDates_january() throws IOException {
		// GIVEN
		assumeReadings();
		populateOtherNodeInAuckland();
		final BasicNodeDatumFilter f = localDatesFilter(null, LOCAL_JAN, LOCAL_FEB);
		f.setReadingType(DatumReadingType.Difference);

		// WHEN
		historian.findFilteredStreamReadings(f, processor, null, null, null);

		// THEN
		// @formatter:off
		and.then(thenReadingsStreamed(null, null).readings())
			.as("Reading differences of January in the zone of each node, none for the other node, whose readings are all after")
			.containsExactly(
				  tuple(Node, TEST_NODE_ID, "a/1", T22, T00, null, "{60}", "{{60,100,160}}")
				, R_A2
			)
			;
		// @formatter:on
	}

	@Test
	public void readings_difference_localDates_february() throws IOException {
		// GIVEN
		assumeReadings();
		populateOtherNodeInAuckland();
		final BasicNodeDatumFilter f = localDatesFilter(null, LOCAL_FEB, LOCAL_MAR);
		f.setReadingType(DatumReadingType.Difference);

		// WHEN
		historian.findFilteredStreamReadings(f, processor, null, null, null);

		// THEN
		// @formatter:off
		and.then(thenReadingsStreamed(null, null).readings())
			.as("Reading differences of February in the zone of each node")
			.containsExactly(
				  tuple(Node, TEST_NODE_ID, "a/1", T00, T01.plus(40, MINUTES), null, "{50}",
						  "{{50,160,210}}")
				, R_A2
				, R_O_A1
			)
			;
		// @formatter:on
	}

}
