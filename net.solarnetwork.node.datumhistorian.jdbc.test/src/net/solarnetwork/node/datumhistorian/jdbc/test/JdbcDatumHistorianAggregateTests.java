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
import static org.assertj.core.api.BDDAssertions.thenIllegalArgumentException;
import static org.assertj.core.api.BDDAssertions.tuple;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
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
import net.solarnetwork.dao.jdbc.CommonJdbcUtils;
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
import net.solarnetwork.node.service.FilterableDatumHistorian;
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
 * otherwise, so running the tests against a database with the setup and against
 * one without it covers them all.
 * </p>
 *
 * <p>
 * The aggregate and reading values expected are those SolarNet's functions give
 * for the same datum, apart from an hour SolarNet's stale processing does not
 * compute either: the one that ends at a stream's first datum.
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
	private static final Tuple H_A2_22 = tuple(Node, TEST_NODE_ID, "a/2", T22, "{5}", "{0}", "{{1,5,5}}",
			"{{0,1000,1000}}");

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
	private static final Tuple D_A2 = tuple(Node, TEST_NODE_ID, "a/2", JAN_31, "{5}", "{0}", "{{1,5,5}}",
			"{{0,1000,1000}}");

	// the monthly aggregates of the running node's streams, in UTC
	private static final Tuple M_W = tuple(Location, TEST_LOC_ID, "weather", JAN, "{12}", null,
			"{{2,10,14}}", null);
	private static final Tuple M_A1_JAN = tuple(Node, TEST_NODE_ID, "a/1", JAN, "{2.5}", "{60}",
			"{{6,0,5}}", "{{60,100,160}}");
	private static final Tuple M_A1_FEB = tuple(Node, TEST_NODE_ID, "a/1", FEB_1, "{8.5}", "{50}",
			"{{6,6,11}}", "{{50,160,210}}");
	private static final Tuple M_A2 = tuple(Node, TEST_NODE_ID, "a/2", JAN, "{5}", "{0}", "{{1,5,5}}",
			"{{0,1000,1000}}");

	// the daily and monthly aggregates of the running node's streams, in Auckland
	private static final Tuple NZ_W = tuple(Location, TEST_LOC_ID, "weather", FEB_1_AUCKLAND, "{12}",
			null, "{{2,10,14}}", null);
	private static final Tuple NZ_A1 = tuple(Node, TEST_NODE_ID, "a/1", FEB_1_AUCKLAND, "{5.5}", "{110}",
			"{{12,0,11}}", "{{110,100,210}}");
	private static final Tuple NZ_A2 = tuple(Node, TEST_NODE_ID, "a/2", FEB_1_AUCKLAND, "{5}", "{0}",
			"{{1,5,5}}", "{{0,1000,1000}}");

	// the 15 minute aggregates of the running node's a/1 stream in the 22:00 hour, as stored
	private static final Tuple S_A1_22 = tuple(Node, TEST_NODE_ID, "a/1", T22, "{0}", "{7.5}",
			"{{1,0,0}}", "{{0,100,100}}");
	private static final Tuple S_A1_22_15 = tuple(Node, TEST_NODE_ID, "a/1", T22.plus(15, MINUTES),
			"{1}", "{7.5}", "{{1,1,1}}", "{{10,100,110}}");
	private static final Tuple S_A1_22_30 = tuple(Node, TEST_NODE_ID, "a/1", T22_30, "{2}", "{15}",
			"{{1,2,2}}", "{{20,110,130}}");

	// the 30 minute aggregates of the running node's a/1 and a/2 streams, combined from the
	// 15 minute aggregates: no datum fall in the slots from 22:45, 23:45, or 00:45, so the half
	// hours from 22:30, 23:30, and 00:30 have only the values of their first slot
	private static final Tuple T_A1_22 = tuple(Node, TEST_NODE_ID, "a/1", T22, "{0.5}", "{15}",
			"{{2,0,1}}", "{{10,100,110}}");
	private static final Tuple T_A1_22_30 = tuple(Node, TEST_NODE_ID, "a/1", T22_30, "{2}", "{15}",
			"{{1,2,2}}", "{{20,110,130}}");
	private static final Tuple T_A1_23 = tuple(Node, TEST_NODE_ID, "a/1", T23, "{3.5}", "{15}",
			"{{2,3,4}}", "{{10,130,140}}");
	private static final Tuple T_A1_23_30 = tuple(Node, TEST_NODE_ID, "a/1", T23.plus(30, MINUTES),
			"{5}", "{15}", "{{1,5,5}}", "{{20,140,160}}");
	private static final Tuple T_A1_00 = tuple(Node, TEST_NODE_ID, "a/1", T00, "{6.5}", "{15}",
			"{{2,6,7}}", "{{10,160,170}}");
	private static final Tuple T_A1_00_30 = tuple(Node, TEST_NODE_ID, "a/1", T00.plus(30, MINUTES),
			"{8}", "{15}", "{{1,8,8}}", "{{20,170,190}}");
	private static final Tuple T_A2_22_30 = tuple(Node, TEST_NODE_ID, "a/2", T22_30, "{5}", "{0}",
			"{{1,5,5}}", "{{0,1000,1000}}");

	/** The start of the hour of the slot edge cases, on 2 March. */
	private static final Instant MAR_2_10 = Instant.parse("2026-03-02T10:00:00Z");

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
			historian.add(
					new SimpleAcEnergyDatum("a/1", T22.plus(20L * k, MINUTES), energy(k, 100 + 10 * k)));
		}
		historian.add(new SimpleAcEnergyDatum("a/2", T22_30, energy(5, 1000)));
		historian.add(new SimpleAtmosphericDatum(TEST_LOC_ID, "weather", T22, temperature(10)));
		historian.add(new SimpleAtmosphericDatum(TEST_LOC_ID, "weather", T23, temperature(14)));
		otherHistorian.add(new SimpleAcEnergyDatum("a/1", T22, energy(1, 10)));
		otherHistorian.add(new SimpleAcEnergyDatum("a/1", T23, energy(3, 30)));
		otherHistorian.add(new SimpleAtmosphericDatum(TEST_LOC_ID, "weather", T22, temperature(11)));
		otherHistorian.add(new SimpleAtmosphericDatum(TEST_LOC_ID, "weather", T22_30, temperature(13)));
		if ( aggregates ) {
			processAll(SCHEMA);
		}
	}

	/**
	 * Add datum whose 15 minute slots combine into half hours in the less
	 * common ways, and with the aggregate setup compute their aggregates.
	 *
	 * <p>
	 * On 2 March the running node stores {@code b/1} at 10:05 and 10:20, the
	 * second datum with an instantaneous property, a status property, and a tag
	 * the first does not have, and a different status; so the two slots of the
	 * half hour have property arrays of different lengths, and statuses that
	 * are each as frequent. It stores {@code b/2} once, at 10:20, so only the
	 * second slot of its half hour has a row.
	 * </p>
	 */
	private void populateSlotEdgeCases() {
		final DatumSamples s1 = energy(1, 100);
		s1.putStatusSampleValue("mode", "on");
		s1.addTag("x");
		historian.add(new SimpleAcEnergyDatum("b/1", MAR_2_10.plus(5, MINUTES), s1));
		final DatumSamples s2 = energy(3, 110);
		s2.putInstantaneousSampleValue("temp", 20);
		s2.putStatusSampleValue("mode", "idle");
		s2.putStatusSampleValue("op", "y");
		s2.addTag("y");
		historian.add(new SimpleAcEnergyDatum("b/1", MAR_2_10.plus(20, MINUTES), s2));
		historian.add(new SimpleAcEnergyDatum("b/2", MAR_2_10.plus(20, MINUTES), energy(7, 500)));
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
	 * Get the stale aggregates of a schema, as kind, node ID, location ID,
	 * source ID, and time.
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
	 * Get the stored aggregates of the running node's streams in a table of the
	 * aggregate setup, as {@link Streamed#aggregates()} gives aggregate datum,
	 * in the default order of the historian.
	 */
	private List<Tuple> storedAggregates(String table) {
		return jdbcTemplate.query("""
				SELECT m.node_id, m.loc_id, m.source_id, d.ts_start
					, d.data_i, d.data_a, d.stat_i, d.read_a
				FROM %s.dh_datum_meta m
				INNER JOIN %s.%s d ON d.stream_id = m.stream_id
				WHERE m.node_id = ?
				ORDER BY m.node_id, m.loc_id, m.source_id, d.ts_start
				""".formatted(SCHEMA, SCHEMA, table), (rs, i) -> {
			final Long locId = rs.getObject(2, Long.class);
			final BigDecimal[] dataI = CommonJdbcUtils.getArray(rs, 5);
			final BigDecimal[] dataA = CommonJdbcUtils.getArray(rs, 6);
			final BigDecimal[][] statI = CommonJdbcUtils.getArray(rs, 7);
			final BigDecimal[][] readA = CommonJdbcUtils.getArray(rs, 8);
			return tuple(locId != null ? Location : Node, locId != null ? locId : rs.getLong(1),
					rs.getString(3), rs.getTimestamp(4).toInstant(), pg(dataI), pg(dataA), pg(statI),
					pg(readA));
		}, TEST_NODE_ID);
	}

	/**
	 * The values of one period that aggregate datum are rolled up into.
	 */
	private static final class Rollup {

		// for each instantaneous property: the sum of mean times count, count, min, max
		private final List<BigDecimal[]> inst = new ArrayList<>();

		// for each accumulating property: the sum, reading difference, start, end
		private final List<BigDecimal[]> acc = new ArrayList<>();

		private static BigDecimal[] row(List<BigDecimal[]> rows, int idx) {
			while ( rows.size() <= idx ) {
				rows.add(new BigDecimal[4]);
			}
			return rows.get(idx);
		}

		private static BigDecimal sum(BigDecimal a, BigDecimal b) {
			return (a == null ? b : b == null ? a : a.add(b));
		}

		private static BigDecimal min(BigDecimal a, BigDecimal b) {
			return (a == null ? b : b == null ? a : a.min(b));
		}

		private static BigDecimal max(BigDecimal a, BigDecimal b) {
			return (a == null ? b : b == null ? a : a.max(b));
		}

		private void add(AggregateStreamDatum d) {
			final BigDecimal[] i = d.getProperties().getInstantaneous();
			final BigDecimal[][] is = d.getStatistics().getInstantaneous();
			for ( int k = 0; i != null && k < i.length; k++ ) {
				final BigDecimal[] r = row(inst, k);
				final BigDecimal count = is[k][0];
				r[0] = sum(r[0], i[k] != null && count != null ? i[k].multiply(count) : null);
				r[1] = sum(r[1], count);
				r[2] = min(r[2], is[k][1]);
				r[3] = max(r[3], is[k][2]);
			}
			final BigDecimal[] a = d.getProperties().getAccumulating();
			final BigDecimal[][] as = d.getStatistics().getAccumulating();
			for ( int k = 0; a != null && k < a.length; k++ ) {
				final BigDecimal[] r = row(acc, k);
				r[0] = sum(r[0], a[k]);
				r[1] = sum(r[1], as[k][0]);
				if ( r[2] == null ) {
					r[2] = as[k][1];
				}
				if ( as[k][2] != null ) {
					r[3] = as[k][2];
				}
			}
		}

		private Tuple toTuple(List<Object> key) {
			final BigDecimal[] dataI = (inst.isEmpty() ? null
					: inst.stream()
							.map(r -> r[0] != null && r[1] != null && r[1].signum() != 0
									? r[0].divide(r[1], MathContext.DECIMAL128)
									: null)
							.toArray(BigDecimal[]::new));
			final BigDecimal[][] statI = (inst.isEmpty() ? null
					: inst.stream().map(r -> new BigDecimal[] { r[1], r[2], r[3] })
							.toArray(BigDecimal[][]::new));
			final BigDecimal[] dataA = (acc.isEmpty() ? null
					: acc.stream().map(r -> r[0]).toArray(BigDecimal[]::new));
			final BigDecimal[][] readA = (acc.isEmpty() ? null
					: acc.stream().map(r -> new BigDecimal[] { r[1], r[2], r[3] })
							.toArray(BigDecimal[][]::new));
			return tuple(key.get(0), key.get(1), key.get(2), key.get(3), pg(dataI), pg(dataA), pg(statI),
					pg(readA));
		}

	}

	/**
	 * Roll aggregate datum up into longer periods, by the rules the aggregate
	 * setup rolls hours up into days with: means weighted by count; counts,
	 * accumulations, and reading differences added; the least minimum and
	 * greatest maximum taken; the starting reading of the first datum that has
	 * one, and the ending reading of the last.
	 *
	 * <p>
	 * The status and tag properties are not rolled up. The means are divided to
	 * 34 digits, so are only exact for datum whose means divide exactly, as
	 * those of these tests do.
	 * </p>
	 *
	 * @param streamed
	 *        the aggregate datum, in time order within each stream
	 * @param periodSeconds
	 *        the length of the periods, which start at UTC multiples of it
	 * @return the rolled up aggregates, as {@link Streamed#aggregates()} gives
	 *         aggregate datum, in the order of their first datum
	 */
	private static List<Tuple> rollUp(Streamed streamed, long periodSeconds) {
		final Map<List<Object>, Rollup> periods = new LinkedHashMap<>();
		for ( StreamDatum d : streamed.datum() ) {
			final ObjectDatumStreamMetadata meta = streamed.meta(d);
			final long secs = d.getTimestamp().getEpochSecond();
			final Instant start = Instant.ofEpochSecond(secs - Math.floorMod(secs, periodSeconds));
			periods.computeIfAbsent(
					List.of(meta.getKind(), meta.getObjectId(), meta.getSourceId(), start),
					k -> new Rollup()).add((AggregateStreamDatum) d);
		}
		return periods.entrySet().stream().map(e -> e.getValue().toTuple(e.getKey())).toList();
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
		 * Get the kind, object ID, source ID, time, status values, and tags of
		 * the datum.
		 */
		private List<Tuple> statusesAndTags() {
			return datum.stream().map(d -> {
				final ObjectDatumStreamMetadata meta = meta(d);
				final String[] status = d.getProperties().getStatus();
				final String[] tags = d.getProperties().getTags();
				return tuple(meta.getKind(), meta.getObjectId(), meta.getSourceId(), d.getTimestamp(),
						status != null ? Arrays.asList(status) : null,
						tags != null ? Arrays.asList(tags) : null);
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

	private Streamed thenReadingsStreamed(Long totalResults, Integer startingOffset) throws IOException {
		final Streamed result = thenStreamed(totalResults, startingOffset);
		// @formatter:off
		and.then(result.datum())
			.as("Reading datum given")
			.allSatisfy(d -> and.then(d).isExactlyInstanceOf(BasicReadingStreamDatum.class))
			;
		// @formatter:on
		return result;
	}

	/**
	 * Query the datum of a filter with a processor of its own.
	 *
	 * @param filter
	 *        the filter
	 * @return what the processor was given
	 */
	private Streamed streamedOf(BasicNodeDatumFilter filter) throws IOException {
		return streamedOf(filter, null, null);
	}

	/**
	 * Query a page of the datum of a filter with a processor of its own.
	 *
	 * @param filter
	 *        the filter
	 * @param offset
	 *        the offset, or {@code null}
	 * @param max
	 *        the page size, or {@code null}
	 * @return what the processor was given
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Streamed streamedOf(BasicNodeDatumFilter filter, Long offset, Integer max)
			throws IOException {
		final StreamDatumFilteredResultsProcessor p = mock(StreamDatumFilteredResultsProcessor.class);
		historian.findFilteredStream(filter, p, null, offset, max);
		final ArgumentCaptor<Map<String, ?>> attrs = (ArgumentCaptor) ArgumentCaptor.forClass(Map.class);
		final ArgumentCaptor<StreamDatum> datum = ArgumentCaptor.forClass(StreamDatum.class);
		// the starting offset is given only for a page
		final Integer startingOffset = (max != null ? (offset != null ? offset.intValue() : 0) : null);
		then(p).should().start(isNull(), eq(startingOffset), isNull(), attrs.capture());
		then(p).should(atLeast(0)).handleResultItem(datum.capture());
		then(p).shouldHaveNoMoreInteractions();
		return new Streamed(
				(ObjectDatumStreamMetadataProvider) attrs.getValue().get(METADATA_PROVIDER_ATTR),
				datum.getAllValues());
	}

	/**
	 * Query the aggregates of a filter with a processor of its own.
	 *
	 * @param filter
	 *        the filter
	 * @return what the processor was given
	 */
	private Streamed aggregatesOf(BasicNodeDatumFilter filter) throws IOException {
		return aggregatesOf(filter, null, null);
	}

	/**
	 * Query a page of the aggregates of a filter with a processor of its own.
	 *
	 * @param filter
	 *        the filter
	 * @param offset
	 *        the offset, or {@code null}
	 * @param max
	 *        the page size, or {@code null}
	 * @return what the processor was given
	 */
	private Streamed aggregatesOf(BasicNodeDatumFilter filter, Long offset, Integer max)
			throws IOException {
		final Streamed result = streamedOf(filter, offset, max);
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

	/*
	 * ================================================================== Time
	 * zone ==================================================================
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
		historian.findFilteredStream(filter(Aggregation.Week), processor, null, null, null);

		// THEN
		final Streamed result = thenStreamed(null, null);
		// @formatter:off
		and.then(result.datum())
			.as("Raw datum of running node for aggregation without stored or computed aggregates")
			.hasSize(15)
			.allSatisfy(d -> and.then(d).isExactlyInstanceOf(BasicStreamDatum.class))
			;
		// @formatter:on
	}

	/*
	 * ==================================================================
	 * Sub-hour aggregates
	 * ==================================================================
	 */

	private boolean minuteAggregates() {
		return jdbcTemplate.queryForObject("SELECT to_regproc(?) IS NOT NULL", Boolean.class,
				SCHEMA + ".dh_rollup_datum_for_time_span_slots");
	}

	private void assumeMinuteAggregates() {
		assumeTrue("The %s schema does not have the sub-hour functions.".formatted(SCHEMA),
				minuteAggregates());
	}

	private static BasicNodeDatumFilter minuteFilter(Aggregation aggregation, Instant start,
			Instant end) {
		final BasicNodeDatumFilter f = filter(aggregation);
		f.setStartDate(start);
		f.setEndDate(end);
		return f;
	}

	@Test
	public void agg_thirtyMinute_list() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();

		// WHEN
		historian.findFilteredStream(minuteFilter(Aggregation.ThirtyMinute, T22, T01), processor, null,
				null, null);

		// THEN
		final Streamed result = thenAggregatesStreamed(null, null);
		// @formatter:off
		and.then(result.aggregates())
			.as("Half hours of running node's streams with stored 15 minute aggregates, combined from them, with readings, in default order")
			.containsExactly(
				  tuple(Location, TEST_LOC_ID, "weather", T22, "{10}", null, "{{1,10,10}}", null)
				, tuple(Location, TEST_LOC_ID, "weather", T23, "{14}", null, "{{1,14,14}}", null)
				// a/1 grows 10 every 20 minutes, so 15 every 30 minutes: a datum's difference that
				// crosses a slot boundary is split by time between the slots, while the readings
				// are those at the start of the first slot and the end of the last
				, T_A1_22
				, T_A1_22_30
				, T_A1_23
				, T_A1_23_30
				, T_A1_00
				, T_A1_00_30
				, T_A2_22_30
			)
			;
		and.then(result.provider().metadataStreamIds())
			.as("Metadata of running node's streams given")
			.containsExactlyInAnyOrder(streamId(TEST_NODE_ID, TEST_LOC_ID, "weather"),
					streamId(TEST_NODE_ID, null, "a/1"), streamId(TEST_NODE_ID, null, "a/2"))
			;
		// @formatter:on
	}

	@Test
	public void agg_fifteenMinute_list_pagination() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();
		final BasicNodeDatumFilter f = minuteFilter(Aggregation.FifteenMinute, T22, T01);
		f.setWithoutTotalResultsCount(false);

		// WHEN
		historian.findFilteredStream(f, processor, null, 1L, 3);

		// THEN
		// 12 stored slots: 2 of weather, 9 of a/1, and 1 of a/2
		final Streamed result = thenAggregatesStreamed(12L, 1);
		// @formatter:off
		and.then(result.aggregates())
			.as("Page of stored slots in default order, with readings")
			.containsExactly(
				  tuple(Location, TEST_LOC_ID, "weather", T23, "{14}", null, "{{1,14,14}}", null)
				, S_A1_22
				, S_A1_22_15
			)
			;
		// @formatter:on
	}

	@Test
	public void agg_fifteenMinute_list_storedRows() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();
		populateSlotEdgeCases();

		// WHEN
		historian.findFilteredStream(filter(Aggregation.FifteenMinute), processor, null, null, null);

		// THEN
		final Streamed result = thenAggregatesStreamed(null, null);
		// @formatter:off
		and.then(result.aggregates())
			.as("Stored 15 minute aggregates of running node's streams, with readings, without a date range")
			.hasSize(18)
			.containsExactlyElementsOf(storedAggregates("dh_agg_15m"))
			.contains(S_A1_22, S_A1_22_15, S_A1_22_30)
			;
		// @formatter:on
	}

	@Test
	public void agg_thirtyMinute_list_combinesSlotPairs() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();
		populateSlotEdgeCases();

		// WHEN
		final Streamed slots = aggregatesOf(filter(Aggregation.FifteenMinute));
		final Streamed halfHours = aggregatesOf(filter(Aggregation.ThirtyMinute));

		// THEN
		// @formatter:off
		and.then(halfHours.aggregates())
			.as("Each half hour combines the stored 15 minute aggregates of its two slots")
			.hasSize(13)
			.containsExactlyElementsOf(rollUp(slots, 1800))
			;
		// @formatter:on
	}

	@Test
	public void agg_thirtyMinute_list_addUpToHours() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();
		populateSlotEdgeCases();

		// WHEN
		final Streamed halfHours = aggregatesOf(filter(Aggregation.ThirtyMinute));
		final Streamed hours = aggregatesOf(filter(Aggregation.Hour));

		// THEN
		// @formatter:off
		and.then(rollUp(halfHours, 3600))
			.as("The half hours of each hour, rolled up, give the stored hour")
			.containsExactlyElementsOf(hours.aggregates())
			.contains(H_W22, H_W23, H_A1_22, H_A1_23, H_A1_00, H_A1_01, H_A2_22)
			;
		and.then(hours.aggregates())
			.as("The hours are those stored")
			.containsExactlyElementsOf(storedAggregates("dh_agg_hourly"))
			;
		// @formatter:on
	}

	@Test
	public void agg_thirtyMinute_list_slotEdgeCases() throws IOException {
		// GIVEN
		assumeAggregates();
		populateSlotEdgeCases();
		final BasicNodeDatumFilter f = filter(Aggregation.ThirtyMinute);
		f.setSourceIds(new String[] { "b/*" });

		// WHEN
		final Streamed result = aggregatesOf(f);

		// THEN
		// @formatter:off
		and.then(result.aggregates())
			.as("Half hours combined from slots with different properties, and from only the second slot")
			.containsExactly(
				  // watts from both slots, temp from the second only; the accumulation split
				  // 6.666... and 3.333... between the slots adds up again
				  tuple(Node, TEST_NODE_ID, "b/1", MAR_2_10, "{2,20}", "{10}", "{{2,1,3},{1,20,20}}",
						  "{{10,100,110}}")
				, tuple(Node, TEST_NODE_ID, "b/2", MAR_2_10, "{7}", "{0}", "{{1,7,7}}", "{{0,500,500}}")
			)
			;
		and.then(result.datum()).element(0)
			.as("Status of each property the more frequent of the two slots, the one that sorts first when each is as frequent")
			.returns(new String[] { "idle", "y" }, d -> d.getProperties().getStatus())
			.as("Tags of both slots")
			.returns(new String[] { "x", "y" }, d -> d.getProperties().getTags())
			;
		and.then(result.datum()).element(1)
			.as("No status or tags")
			.returns(null, d -> d.getProperties().getStatus())
			.returns(null, d -> d.getProperties().getTags())
			;
		// @formatter:on
	}

	@Test
	public void agg_thirtyMinute_list_sorts_pagination() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();
		final BasicNodeDatumFilter f = minuteFilter(Aggregation.ThirtyMinute, T22, T01);
		f.setSorts(List.of(new SimpleSortDescriptor(SORT_BY_TIME, true),
				new SimpleSortDescriptor(SORT_BY_SOURCE)));
		f.setWithoutTotalResultsCount(false);

		// WHEN
		historian.findFilteredStream(f, processor, null, 2L, 3);

		// THEN
		// 9 half hours: 2 of weather, 6 of a/1, and 1 of a/2
		// @formatter:off
		and.then(thenAggregatesStreamed(9L, 2).ids())
			.as("Page of half hours, newest first, then by source")
			.containsExactly(
				  tuple(Node, TEST_NODE_ID, "a/1", T23.plus(30, MINUTES))
				, tuple(Node, TEST_NODE_ID, "a/1", T23)
				, tuple(Location, TEST_LOC_ID, "weather", T23)
			)
			;
		// @formatter:on
	}

	@Test
	public void agg_thirtyMinute_list_sorts() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();
		final BasicNodeDatumFilter f = minuteFilter(Aggregation.ThirtyMinute, T22, T01);
		f.setSorts(List.of(new SimpleSortDescriptor(SORT_BY_TIME, true),
				new SimpleSortDescriptor(SORT_BY_SOURCE)));

		// WHEN
		historian.findFilteredStream(f, processor, null, null, null);

		// THEN
		// @formatter:off
		and.then(thenAggregatesStreamed(null, null).aggregates())
			.as("All half hours, newest first, then by source, without a page")
			.containsExactly(
				  T_A1_00_30
				, T_A1_00
				, T_A1_23_30
				, T_A1_23
				, tuple(Location, TEST_LOC_ID, "weather", T23, "{14}", null, "{{1,14,14}}", null)
				, T_A1_22_30
				, T_A2_22_30
				, T_A1_22
				, tuple(Location, TEST_LOC_ID, "weather", T22, "{10}", null, "{{1,10,10}}", null)
			)
			;
		// @formatter:on
	}

	@Test
	public void agg_thirtyMinute_list_pages() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();
		populateSlotEdgeCases();
		final BasicNodeDatumFilter f = filter(Aggregation.ThirtyMinute);
		f.setSorts(List.of(new SimpleSortDescriptor(SORT_BY_TIME, true),
				new SimpleSortDescriptor(SORT_BY_SOURCE)));
		final Streamed all = aggregatesOf(f);

		// WHEN
		// pages of 2, which split the streams' half hours of 23:00, of 22:30, and of 22:00
		// between pages
		final List<Tuple> pages = new ArrayList<>();
		final List<Tuple> pagesStatusesAndTags = new ArrayList<>();
		for ( long offset = 0; offset < all.datum().size(); offset += 2 ) {
			final Streamed page = aggregatesOf(f, offset, 2);
			pages.addAll(page.aggregates());
			pagesStatusesAndTags.addAll(page.statusesAndTags());
		}

		// THEN
		// @formatter:off
		and.then(all.aggregates())
			.as("All half hours, newest first, then by source, the first combined from slots with different properties")
			.hasSize(13)
			.startsWith(
				  tuple(Node, TEST_NODE_ID, "b/1", MAR_2_10, "{2,20}", "{10}", "{{2,1,3},{1,20,20}}",
						  "{{10,100,110}}")
				, tuple(Node, TEST_NODE_ID, "b/2", MAR_2_10, "{7}", "{0}", "{{1,7,7}}", "{{0,500,500}}")
			)
			;
		and.then(pages)
			.as("Pages, combined after the page is taken, give the half hours of the whole, in the same order")
			.containsExactlyElementsOf(all.aggregates())
			;
		and.then(pagesStatusesAndTags)
			.as("Pages give the statuses and tags of the whole")
			.containsExactlyElementsOf(all.statusesAndTags())
			.contains(tuple(Node, TEST_NODE_ID, "b/1", MAR_2_10, List.of("idle", "y"), List.of("x", "y")))
			;
		// @formatter:on
	}

	@Test
	public void agg_thirtyMinute_list_datesNotOnHalfHour() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();
		final BasicNodeDatumFilter f = minuteFilter(Aggregation.ThirtyMinute, T22.plus(10, MINUTES),
				T23.plus(40, MINUTES));

		// WHEN
		historian.findFilteredStream(f, processor, null, null, null);

		// THEN
		// @formatter:off
		and.then(thenAggregatesStreamed(null, null).aggregates())
			.as("Whole half hours that start from 22:10 and before 23:40, not the 22:00 half hour from its 22:15 slot")
			.containsExactly(
				  tuple(Location, TEST_LOC_ID, "weather", T23, "{14}", null, "{{1,14,14}}", null)
				, T_A1_22_30
				, T_A1_23
				, T_A1_23_30
				, T_A2_22_30
			)
			;
		// @formatter:on
	}

	@Test
	public void agg_thirtyMinute_list_localDates() throws IOException {
		// GIVEN
		assumeAggregates();
		populateOtherNodeInAuckland();
		final BasicNodeDatumFilter f = localDatesFilter(Aggregation.ThirtyMinute,
				LOCAL_FEB.plusHours(11).plusMinutes(10), LOCAL_FEB.plusHours(13));

		// WHEN
		historian.findFilteredStream(f, processor, null, null, null);

		// THEN
		// @formatter:off
		and.then(thenAggregatesStreamed(null, null).aggregates())
			.as("Half hours that start from 11:10 and before 13:00 on 1 February in the zone of each node: none of the running node in UTC, and of the other node in Auckland the 23:00 UTC half hour, not the 22:00 one")
			.containsExactly(
				  tuple(Node, TEST_NODE_ID_2, "a/1", T23, "{3}", "{0}", "{{1,3,3}}", "{{0,30,30}}")
			)
			;
		// @formatter:on
	}

	@Test
	public void agg_fifteenThirtyMinute_list_noRangeLimit() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();
		// more than the default 840 hours either side
		final Instant start = T22.minus(900, HOURS);
		final Instant end = T01.plus(900, HOURS);

		// WHEN
		final Streamed slots = aggregatesOf(minuteFilter(Aggregation.FifteenMinute, start, end));
		final Streamed halfHours = aggregatesOf(minuteFilter(Aggregation.ThirtyMinute, start, end));

		// THEN
		// @formatter:off
		and.then(slots.aggregates())
			.as("All stored slots of running node, the time span not limited")
			.containsExactlyElementsOf(storedAggregates("dh_agg_15m"))
			;
		and.then(halfHours.ids())
			.as("All half hours of running node, the time span not limited")
			.hasSize(11)
			;
		// @formatter:on
	}

	@Test
	public void agg_fiveTenMinute_list_defaultRangeLimit() throws IOException {
		// GIVEN
		assumeMinuteAggregates();
		populate();

		for ( Aggregation agg : List.of(Aggregation.FiveMinute, Aggregation.TenMinute) ) {
			// WHEN
			// @formatter:off
			thenIllegalArgumentException()
				.as("Span of 841 hours refused for %s, computed from raw datum", agg)
				.isThrownBy(() -> historian.findFilteredStream(minuteFilter(agg, T22, T22.plus(841, HOURS)),
						processor, null, null, null))
				.withMessage("Minute level aggregation time span must be at most 840 hours.")
				;
			// @formatter:on
		}

		// THEN
		then(processor).shouldHaveNoInteractions();
	}

	@Test
	public void agg_fifteenMinute_list_mostRecent() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();
		final BasicNodeDatumFilter f = filter(Aggregation.FifteenMinute);
		f.setMostRecent(true);

		// WHEN
		historian.findFilteredStream(f, processor, null, null, null);

		// THEN
		// @formatter:off
		and.then(thenAggregatesStreamed(null, null).aggregates())
			.as("Most recent stored slot of each stream of the running node, as for Hour")
			.containsExactly(
				  tuple(Location, TEST_LOC_ID, "weather", T23, "{14}", null, "{{1,14,14}}", null)
				, tuple(Node, TEST_NODE_ID, "a/1", T01.plus(30, MINUTES), "{11}", "{5}", "{{1,11,11}}",
						"{{10,200,210}}")
				, T_A2_22_30
			)
			;
		// @formatter:on
	}

	@Test
	public void agg_thirtyMinute_list_mostRecent_refused() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();
		final BasicNodeDatumFilter f = filter(Aggregation.ThirtyMinute);
		f.setMostRecent(true);

		// WHEN
		// @formatter:off
		thenIllegalArgumentException()
			.as("Most recent refused for ThirtyMinute, as in SolarNet")
			.isThrownBy(() -> historian.findFilteredStream(f, processor, null, null, null))
			.withMessage("The mostRecent flag cannot be used with aggregation ThirtyMinute.")
			;
		then(processor).shouldHaveNoInteractions();
		// @formatter:on
	}

	@Test
	public void agg_fiveMinute_list_sorts_sourcePattern() throws IOException {
		// GIVEN
		assumeMinuteAggregates();
		populate();
		final BasicNodeDatumFilter f = minuteFilter(Aggregation.FiveMinute, T23, T00);
		f.setSourceIds(new String[] { "a/*" });
		f.setSorts(List.of(new SimpleSortDescriptor(FilterableDatumHistorian.SORT_BY_TIME, true)));

		// WHEN
		historian.findFilteredStream(f, processor, null, null, null);

		// THEN
		// @formatter:off
		and.then(thenAggregatesStreamed(null, null).ids())
			.as("Slots of matching sources, newest first")
			.containsExactly(
				  tuple(Node, TEST_NODE_ID, "a/1", T23.plus(40, MINUTES))
				, tuple(Node, TEST_NODE_ID, "a/1", T23.plus(20, MINUTES))
				, tuple(Node, TEST_NODE_ID, "a/1", T23)
			)
			;
		// @formatter:on
	}

	@Test
	public void agg_tenMinute_list_localDates() throws IOException {
		// GIVEN
		assumeMinuteAggregates();
		populateOtherNodeInAuckland();

		// WHEN
		historian.findFilteredStream(localDatesFilter(Aggregation.TenMinute, LOCAL_FEB.plusHours(11),
				LOCAL_FEB.plusHours(13)), processor, null, null, null);

		// THEN
		// @formatter:off
		and.then(thenAggregatesStreamed(null, null).ids())
			.as("Slots of 11:00 to 13:00 on 1 February in the zone of each node: none of the running node in UTC, those of the other node in Auckland")
			.containsExactly(
				  tuple(Node, TEST_NODE_ID_2, "a/1", T22)
				, tuple(Node, TEST_NODE_ID_2, "a/1", T23)
			)
			;
		// @formatter:on
	}

	@Test
	public void agg_fiveMinute_list_rangeLimit() throws IOException {
		// GIVEN
		assumeMinuteAggregates();
		populate();
		historian.setMaxMinuteAggregationHours(2);

		// WHEN
		// @formatter:off
		thenIllegalArgumentException()
			.as("Span of 3 hours refused")
			.isThrownBy(() -> historian.findFilteredStream(minuteFilter(Aggregation.FiveMinute, T22, T01),
					processor, null, null, null))
			.withMessage("Minute level aggregation time span must be at most 2 hours.")
			;
		then(processor).shouldHaveNoInteractions();
		// @formatter:on
	}

	@Test
	public void agg_minute_list_refused() throws IOException {
		// GIVEN
		assumeMinuteAggregates();
		populate();

		// WHEN
		// @formatter:off
		thenIllegalArgumentException()
			.as("Minute aggregation refused, as in SolarNet")
			.isThrownBy(() -> historian.findFilteredStream(minuteFilter(Aggregation.Minute, T22, T01),
					processor, null, null, null))
			.withMessageStartingWith("The Minute aggregation is not supported")
			;
		then(processor).shouldHaveNoInteractions();
		// @formatter:on
	}

	@Test
	public void subHour_readings_refused() throws IOException {
		// GIVEN
		populate();
		final List<Aggregation> refused = new ArrayList<>(List.of(Aggregation.Minute,
				Aggregation.FiveMinute, Aggregation.TenMinute, Aggregation.ThirtyMinute));
		if ( !aggregates ) {
			refused.add(Aggregation.FifteenMinute);
		}
		final String message = (aggregates
				? "Only FifteenMinute/Hour/Day/Month aggregation is supported for reading queries."
				: "Only Hour/Day/Month aggregation is supported for reading queries.");

		for ( Aggregation agg : refused ) {
			final BasicNodeDatumFilter f = readingFilter(agg);
			f.setStartDate(T22);
			f.setEndDate(T01);

			// WHEN
			// @formatter:off
			thenIllegalArgumentException()
				.as("Sub-hour aggregation %s refused for readings, as in SolarQuery, naming the aggregations supported with or without aggregate setup", agg)
				.isThrownBy(() -> historian.findFilteredStreamReadings(f, processor, null, null, null))
				.withMessage(message)
				;
			// @formatter:on
		}

		// THEN
		then(processor).shouldHaveNoInteractions();
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
	public void agg_fifteenMinute_readings() throws IOException {
		// GIVEN
		assumeAggregates();
		populate();
		final BasicNodeDatumFilter f = readingFilter(Aggregation.FifteenMinute);
		f.setStartDate(T22);
		f.setEndDate(T23);

		// WHEN
		historian.findFilteredStreamReadings(f, processor, null, null, null);

		// THEN
		final Streamed result = thenReadingsStreamed(null, null);
		// @formatter:off
		and.then(result.aggregates())
			.as("Stored 15 minute aggregates of running node's streams in the 22:00 hour, with read_a as accumulating statistics, as for Hour")
			.containsExactly(
				  tuple(Location, TEST_LOC_ID, "weather", T22, "{10}", null, "{{1,10,10}}", null)
				, S_A1_22
				, S_A1_22_15
				, S_A1_22_30
				, T_A2_22_30
			)
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
	public void noAgg_fiveMinute_list_raw() throws IOException {
		// GIVEN
		assumeNoAggregates();
		populate();

		// WHEN
		historian.findFilteredStream(filter(Aggregation.FiveMinute), processor, null, null, null);

		// THEN
		final Streamed result = thenStreamed(null, null);
		// @formatter:off
		and.then(result.datum())
			.as("Raw datum without aggregate setup, without a date range being required")
			.hasSize(15)
			.allSatisfy(d -> and.then(d).isExactlyInstanceOf(BasicStreamDatum.class))
			;
		// @formatter:on
	}

	@Test
	public void noAgg_fifteenThirtyMinute_list_raw() throws IOException {
		// GIVEN
		assumeNoAggregates();
		populate();

		for ( Aggregation agg : List.of(Aggregation.FifteenMinute, Aggregation.ThirtyMinute) ) {
			// WHEN
			// beyond the range limit of the minute aggregations computed from raw datum
			final Streamed result = streamedOf(
					minuteFilter(agg, T22.minus(841, HOURS), T01.plus(1, HOURS)));

			// THEN
			// @formatter:off
			and.then(result.datum())
				.as("Raw datum for %s without aggregate setup, the time span not limited", agg)
				.hasSize(15)
				.allSatisfy(d -> and.then(d).isExactlyInstanceOf(BasicStreamDatum.class))
				;
			// @formatter:on
		}
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
	 * ================================================================== Local
	 * dates ==================================================================
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
	private static BasicNodeDatumFilter localDatesFilter(Aggregation aggregation, LocalDateTime start,
			LocalDateTime end) {
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
		historian.findFilteredStream(localDatesFilter(null, LOCAL_JAN, LOCAL_FEB), processor, null, null,
				null);

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
		historian.findFilteredStream(localDatesFilter(null, LOCAL_FEB, LOCAL_MAR), processor, null, null,
				null);

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
		historian.findFilteredStream(localDatesFilter(Aggregation.Day, LOCAL_JAN, LOCAL_FEB), processor,
				null, null, null);

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
