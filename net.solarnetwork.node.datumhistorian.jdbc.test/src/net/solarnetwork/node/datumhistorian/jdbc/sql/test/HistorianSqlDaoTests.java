/* ==================================================================
 * HistorianSqlDaoTests.java - 10 Nov 2026 11:40:26 pm
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

package net.solarnetwork.node.datumhistorian.jdbc.sql.test;

import static net.solarnetwork.node.datumhistorian.jdbc.DefaultNodeDatumFactory.typeNamesForType;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.assertj.core.groups.Tuple.tuple;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.dao.DataIntegrityViolationException;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.datumhistorian.jdbc.DatumStreamInfo;
import net.solarnetwork.node.datumhistorian.jdbc.DatumStreamInfoRowMapper;
import net.solarnetwork.node.datumhistorian.jdbc.JdbcDatumHistorian;
import net.solarnetwork.node.datumhistorian.jdbc.StoredDatum;
import net.solarnetwork.node.datumhistorian.jdbc.StoredDatumRowMapper;
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectDatum;
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectDatumStreams;
import net.solarnetwork.node.datumhistorian.jdbc.sql.StoreDatum;
import net.solarnetwork.node.datumhistorian.jdbc.test.AbstractPostgresHistorianTests;
import net.solarnetwork.node.domain.datum.AcEnergyDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleAcEnergyDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;

/**
 * Test cases for the SQL classes and database functions, using the Postgres
 * test database.
 *
 * <p>
 * Each test is run against each historian schema.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@RunWith(Parameterized.class)
public class HistorianSqlDaoTests extends AbstractPostgresHistorianTests {

	private static final long TEST_NODE_ID = 123L;
	private static final long TEST_NODE_ID_2 = 234L;
	private static final Long TEST_LOC_ID = 345L;

	private static final Instant START = Instant.parse("2026-11-10T10:00:00Z");

	private static final Set<String> SIMPLE_TYPE_NAMES = typeNamesForType(SimpleDatum.class);
	private static final Set<String> AC_TYPE_NAMES = typeNamesForType(SimpleAcEnergyDatum.class);

	/** The schema to test. */
	@Parameter
	public String schema;

	/**
	 * Get the schemas to test.
	 *
	 * @return the schemas
	 */
	@Parameters(name = "{0}")
	public static Collection<Object[]> schemas() {
		return SCHEMAS.stream().map(s -> new Object[] { s }).toList();
	}

	private final StoredDatumRowMapper mapper = new StoredDatumRowMapper(
			JsonUtils.newDatumObjectMapper());

	private int storeType(String className, String... typeNames) {
		return jdbcTemplate.queryForObject(
				"SELECT %s.dh_store_datum_type(?, ?::TEXT[])".formatted(schema), Integer.class,
				className, "{" + String.join(",", typeNames) + "}");
	}

	private StoreDatum.Result store(StoreDatum sql) {
		List<StoreDatum.Result> results = jdbcTemplate.query(sql, StoreDatum.RESULT_MAPPER);
		and.then(results).as("One store result").hasSize(1);
		return results.get(0);
	}

	private StoreDatum.Result store(long nodeId, NodeDatum datum, Set<String> typeNames, String jdata) {
		return store(new StoreDatum(schema, nodeId, datum, typeNames, jdata));
	}

	private StoreDatum.Result store(NodeDatum datum, String jdata) {
		return store(TEST_NODE_ID, datum, typeNamesForType(datum.getClass()), jdata);
	}

	private static SimpleDatum datum(String sourceId, Instant ts) {
		return SimpleDatum.nodeDatum(sourceId, ts, new DatumSamples());
	}

	private static String watts(int watts) {
		return "{\"i\":{\"watts\":%d}}".formatted(watts);
	}

	private List<StoredDatum> select(SelectDatum sql) {
		return jdbcTemplate.query(sql, mapper);
	}

	private String text(String sql, Object... args) {
		return jdbcTemplate.queryForObject(sql.formatted(schema), String.class, args);
	}

	private long count(String table) {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM %s.%s".formatted(schema, table),
				Long.class);
	}

	private String otherSchema() {
		return SCHEMAS.stream().filter(s -> !s.equals(schema)).findFirst().get();
	}

	/* ==================================================================
	 * dh_store_datum_type
	 * ==================================================================
	 */

	@Test
	public void storeType_new() {
		// WHEN
		int typeId = storeType("foo.Bar", "foo.Bar", "foo.Datum");

		// THEN
		// @formatter:off
		and.then(text("SELECT class_name || ' ' || array_to_json(type_names) FROM %s.dh_datum_type WHERE type_id = ?", typeId))
			.as("Type stored")
			.isEqualTo("foo.Bar [\"foo.Bar\",\"foo.Datum\"]")
			;
		// @formatter:on
	}

	@Test
	public void storeType_existing() {
		// GIVEN
		int typeId = storeType("foo.Bar", "foo.Bar", "foo.Datum");

		// WHEN
		int result = storeType("foo.Bar", "foo.Datum", "foo.Bar");

		// THEN
		// @formatter:off
		and.then(result)
			.as("Same type ID returned for existing type")
			.isEqualTo(typeId)
			;
		and.then(count("dh_datum_type"))
			.as("No other type stored")
			.isEqualTo(1L)
			;
		and.then(text("SELECT array_to_json(type_names) FROM %s.dh_datum_type WHERE type_id = ?", typeId))
			.as("Type names not changed when the same in another order")
			.isEqualTo("[\"foo.Bar\",\"foo.Datum\"]")
			;
		// @formatter:on
	}

	@Test
	public void storeType_namesChanged() {
		// GIVEN
		int typeId = storeType("foo.Bar", "foo.Bar", "foo.Datum");

		// WHEN
		int result = storeType("foo.Bar", "foo.Bar", "foo.OtherDatum");

		// THEN
		// @formatter:off
		and.then(result)
			.as("Same type ID returned for existing type")
			.isEqualTo(typeId)
			;
		and.then(text("SELECT array_to_json(type_names) FROM %s.dh_datum_type WHERE type_id = ?", typeId))
			.as("Type names updated")
			.isEqualTo("[\"foo.Bar\",\"foo.OtherDatum\"]")
			;
		// @formatter:on
	}

	@Test
	public void storeType_many() {
		// WHEN
		int a = storeType("foo.A", "foo.A");
		int b = storeType("foo.B", "foo.B");

		// THEN
		// @formatter:off
		and.then(a)
			.as("Each type has its own ID")
			.isNotEqualTo(b)
			;
		// @formatter:on
	}

	/* ==================================================================
	 * StoreDatum / dh_store_datum
	 * ==================================================================
	 */

	@Test
	public void store_typeNames() {
		// WHEN
		StoreDatum.Result result = store(datum("a", START), watts(1));

		// THEN
		// @formatter:off
		and.then(text("SELECT type_id || ' ' || class_name FROM %s.dh_datum_type"))
			.as("Type registered with returned ID")
			.isEqualTo(result.typeId() + " " + SimpleDatum.class.getName())
			;
		and.then(text("SELECT stream_id || ' ' || node_id || ' ' || source_id || ' ' || (loc_id IS NULL) || ' ' || array_to_json(type_ids) FROM %s.dh_datum_meta"))
			.as("Stream registered with returned ID")
			.isEqualTo("%d %d a true [%d]".formatted(result.streamId(), TEST_NODE_ID, result.typeId()))
			;
		and.then(text("SELECT stream_id || ' ' || type_id || ' ' || (ts AT TIME ZONE 'UTC')::TEXT || ' ' || data_i::TEXT FROM %s.dh_datum"))
			.as("Datum stored")
			.isEqualTo("%d %d 2026-11-10 10:00:00 {1}".formatted(result.streamId(), result.typeId()))
			;
		// @formatter:on
	}

	@Test
	public void store_typeId() {
		// GIVEN
		StoreDatum.Result first = store(datum("a", START), watts(1));

		// WHEN
		StoreDatum.Result result = store(new StoreDatum(schema, TEST_NODE_ID,
				datum("a", START.plusSeconds(1)), first.typeId(), watts(2)));

		// THEN
		// @formatter:off
		and.then(result)
			.as("Same type and stream returned")
			.isEqualTo(first)
			;
		and.then(count("dh_datum"))
			.as("Both datum stored")
			.isEqualTo(2L)
			;
		// @formatter:on
	}

	@Test
	public void store_typeIdsAppended() {
		// GIVEN
		StoreDatum.Result simple = store(datum("a", START), watts(1));

		// WHEN
		StoreDatum.Result ac = store(new SimpleAcEnergyDatum("a", START.plusSeconds(1), null), watts(2));
		store(datum("a", START.plusSeconds(2)), watts(3));

		// THEN
		// @formatter:off
		and.then(ac.streamId())
			.as("Same stream for other datum type")
			.isEqualTo(simple.streamId())
			;
		and.then(text("SELECT array_to_json(type_ids) FROM %s.dh_datum_meta"))
			.as("Each type added to stream once")
			.isEqualTo("[%d,%d]".formatted(simple.typeId(), ac.typeId()))
			;
		// @formatter:on
	}

	@Test
	public void store_streams() {
		// WHEN
		StoreDatum.Result node = store(datum("a", START), watts(1));
		StoreDatum.Result loc = store(SimpleDatum.locationDatum(TEST_LOC_ID, "a", START, null),
				watts(2));
		StoreDatum.Result otherSource = store(datum("b", START), watts(3));
		StoreDatum.Result otherNode = store(TEST_NODE_ID_2, datum("a", START), SIMPLE_TYPE_NAMES,
				watts(4));
		StoreDatum.Result again = store(datum("a", START.plusSeconds(1)), watts(5));

		// THEN
		// @formatter:off
		and.then(Set.of(node.streamId(), loc.streamId(), otherSource.streamId(), otherNode.streamId()))
			.as("Each node, source, and location has its own stream")
			.hasSize(4)
			;
		and.then(again.streamId())
			.as("Existing stream used")
			.isEqualTo(node.streamId())
			;
		and.then(text("SELECT loc_id::TEXT FROM %s.dh_datum_meta WHERE stream_id = ?", loc.streamId()))
			.as("Location ID stored")
			.isEqualTo(TEST_LOC_ID.toString())
			;
		// @formatter:on
	}

	@Test
	public void store_upsert() {
		// GIVEN
		StoreDatum.Result first = store(datum("a", START), watts(1));

		// WHEN
		StoreDatum.Result second = store(new SimpleAcEnergyDatum("a", START, null),
				"{\"a\":{\"wattHours\":2},\"t\":[\"x\"]}");

		// THEN
		// @formatter:off
		and.then(count("dh_datum"))
			.as("Datum replaced")
			.isEqualTo(1L)
			;
		and.then(text("SELECT type_id || ' ' || (data_i IS NULL) || ' ' || data_a::TEXT || ' ' || data_t::TEXT FROM %s.dh_datum"))
			.as("All datum columns replaced")
			.isEqualTo("%d true {2} {x}".formatted(second.typeId()))
			;
		and.then(second.typeId())
			.isNotEqualTo(first.typeId())
			;
		// @formatter:on
	}

	@Test
	public void store_propertyNames() {
		// GIVEN
		store(datum("a", START), "{\"i\":{\"a\":1,\"b\":2},\"s\":{\"x\":\"one\"}}");

		// WHEN
		store(datum("a", START.plusSeconds(1)), "{\"i\":{\"c\":3,\"a\":4},\"a\":{\"d\":5}}");

		// THEN
		// @formatter:off
		and.then(text("SELECT array_to_json(names_i)::TEXT || array_to_json(names_a)::TEXT || array_to_json(names_s)::TEXT FROM %s.dh_datum_meta"))
			.as("New property names appended to stream")
			.isEqualTo("[\"a\",\"b\",\"c\"][\"d\"][\"x\"]")
			;
		and.then(text("SELECT string_agg(data_i::TEXT || ' ' || COALESCE(data_a::TEXT, '-') || ' ' || COALESCE(data_s::TEXT, '-'), '; ' ORDER BY ts) FROM %s.dh_datum"))
			.as("Values stored in stream property name order")
			.isEqualTo("{1,2} - [\"one\"]; {4,NULL,3} {5} -")
			;
		// @formatter:on
	}

	@Test
	public void store_nonNumeric() {
		// WHEN
		store(datum("a", START),
				"{\"i\":{\"a\":\"x\",\"b\":2,\"c\":\"y\"},\"a\":{\"n\":\"NaN\",\"z\":\"oops\",\"inf\":\"-Infinity\",\"q\":true}}");

		// THEN
		// @formatter:off
		and.then(text("SELECT data_i::TEXT || ' ' || data_a::TEXT FROM %s.dh_datum"))
			.as("Non-numeric values stored as NULL, without trailing NULLs")
			.isEqualTo("{NULL,2} {NaN,NULL,NULL,-Infinity}")
			;
		and.then(text("SELECT array_to_json(names_i)::TEXT || array_to_json(names_a)::TEXT FROM %s.dh_datum_meta"))
			.as("Names of non-numeric properties still added to stream, in JSONB key order")
			.isEqualTo("[\"a\",\"b\",\"c\"][\"n\",\"q\",\"z\",\"inf\"]")
			;
		// @formatter:on
	}

	@Test
	public void store_nullsAndEmpty() {
		// WHEN
		store(datum("a", START), "{\"i\":{\"a\":null,\"b\":1},\"a\":{},\"s\":null,\"t\":[\"x\",null]}");

		// THEN
		// @formatter:off
		and.then(text("SELECT COALESCE(array_to_json(names_i)::TEXT, '-') || ' ' || COALESCE(array_to_json(names_a)::TEXT, '-') || ' ' || COALESCE(array_to_json(names_s)::TEXT, '-') FROM %s.dh_datum_meta"))
			.as("NULL properties not added to stream")
			.isEqualTo("[\"b\"] - -")
			;
		and.then(text("SELECT data_i::TEXT || ' ' || (data_a IS NULL) || ' ' || (data_s IS NULL) || ' ' || data_t::TEXT FROM %s.dh_datum"))
			.as("NULL values not stored")
			.isEqualTo("{1} true true {x}")
			;
		// @formatter:on
	}

	@Test
	public void store_statusTypes() {
		// WHEN
		store(datum("a", START),
				"{\"s\":{\"str\":\"one\",\"int\":2,\"dec\":3.5,\"bool\":false,\"obj\":{\"x\":1},\"arr\":[1,2]}}");

		// THEN
		// @formatter:off
		and.then(text("SELECT array_to_json(names_s)::TEXT FROM %s.dh_datum_meta"))
			.as("Status property names stored in JSONB key order")
			.isEqualTo("[\"arr\",\"dec\",\"int\",\"obj\",\"str\",\"bool\"]")
			;
		and.then(text("SELECT data_s::TEXT FROM %s.dh_datum"))
			.as("Status values stored with JSON types")
			.isEqualTo("[[1, 2], 3.5, 2, {\"x\": 1}, \"one\", false]")
			;
		// @formatter:on
	}

	@Test
	public void store_partitions() {
		// GIVEN
		final Instant endOfMonth = Instant.parse("2001-03-31T23:59:59.999999Z");
		final Instant startOfMonth = Instant.parse("2001-04-01T00:00:00Z");

		// WHEN
		store(datum("a", endOfMonth), watts(1));
		store(datum("a", startOfMonth), watts(2));

		// THEN
		// @formatter:off
		and.then(text("SELECT string_agg(tableoid::regclass::TEXT, ',' ORDER BY ts) FROM %s.dh_datum"))
			.as("Monthly UTC partitions created as needed")
			.isEqualTo("%1$s.dh_datum_2001_03,%1$s.dh_datum_2001_04".formatted(schema))
			;
		and.then(text("SELECT string_agg((ts AT TIME ZONE 'UTC')::TEXT, ',' ORDER BY ts) FROM %s.dh_datum"))
			.as("Microsecond precision stored")
			.isEqualTo("2001-03-31 23:59:59.999999,2001-04-01 00:00:00")
			;
		// @formatter:on
	}

	@Test
	public void store_otherSchemaUnchanged() {
		// WHEN
		store(datum("a", START), watts(1));

		// THEN
		Long count = jdbcTemplate.queryForObject(
				"SELECT count(*) FROM %s.dh_datum_meta".formatted(otherSchema()), Long.class);
		// @formatter:off
		and.then(count)
			.as("Datum not stored in other schema")
			.isZero()
			;
		// @formatter:on
	}

	@Test
	public void store_unknownTypeId() {
		// GIVEN
		final StoreDatum sql = new StoreDatum(schema, TEST_NODE_ID, datum("a", START), Short.MAX_VALUE,
				watts(1));

		// WHEN
		// this aborts the test transaction, so nothing more can be queried
		thenExceptionOfType(DataIntegrityViolationException.class).as("Unknown type ID rejected")
				.isThrownBy(() -> store(sql)).withMessageContaining("does not exist");
	}

	/* ==================================================================
	 * SelectDatumStreams
	 * ==================================================================
	 */

	@Test
	public void selectStreams() {
		// GIVEN
		StoreDatum.Result a = store(datum("a", START), watts(1));
		StoreDatum.Result loc = store(SimpleDatum.locationDatum(TEST_LOC_ID, "a", START, null),
				watts(2));
		StoreDatum.Result b = store(datum("b", START), watts(3));
		store(TEST_NODE_ID_2, datum("c", START), SIMPLE_TYPE_NAMES, watts(4));

		// WHEN
		List<DatumStreamInfo> result = jdbcTemplate.query(new SelectDatumStreams(schema, TEST_NODE_ID),
				DatumStreamInfoRowMapper.INSTANCE);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Streams of node returned, by stream ID")
			.containsExactly(
					new DatumStreamInfo(a.streamId(), "a", null),
					new DatumStreamInfo(loc.streamId(), "a", TEST_LOC_ID),
					new DatumStreamInfo(b.streamId(), "b", null))
			;
		// @formatter:on
	}

	/* ==================================================================
	 * SelectDatum
	 * ==================================================================
	 */

	/**
	 * Store 5 datum each for sources a, b, c, 1 second apart, with watts values
	 * 0-4.
	 */
	private void storeHistory() {
		for ( int i = 0; i < 5; i++ ) {
			for ( String src : new String[] { "a", "b", "c" } ) {
				store(datum(src, START.plusSeconds(i)), watts(i));
			}
		}
	}

	private static List<Integer> watts(List<StoredDatum> list) {
		return list.stream().map(d -> d.samples().getInstantaneousSampleInteger("watts")).toList();
	}

	@Test
	public void select_mapsColumns() {
		// GIVEN
		StoreDatum.Result r = store(new SimpleAcEnergyDatum("a", START, null),
				"{\"i\":{\"watts\":1.5},\"a\":{\"wattHours\":\"NaN\"},\"s\":{\"mode\":\"x\",\"n\":2},\"t\":[\"t1\",\"t2\"]}");

		// WHEN
		List<StoredDatum> result = select(
				SelectDatum.forSource(schema, TEST_NODE_ID, "a", null, 0, 1, null));

		// THEN
		// @formatter:off
		and.then(result)
			.as("Datum returned")
			.hasSize(1)
			.element(0)
			.returns(r.streamId(), from(StoredDatum::streamId))
			.returns("a", from(StoredDatum::sourceId))
			.returns(null, from(StoredDatum::locationId))
			.returns(START, from(StoredDatum::timestamp))
			.returns(SimpleAcEnergyDatum.class.getName(), from(StoredDatum::className))
			.returns(AC_TYPE_NAMES, from(StoredDatum::typeNames))
			.extracting(StoredDatum::samples)
			.returns(Map.of("watts", new BigDecimal("1.5")), from(DatumSamples::getInstantaneous))
			.returns(Map.of("wattHours", Double.NaN), from(DatumSamples::getAccumulating))
			.returns(Map.of("mode", "x", "n", 2), from(DatumSamples::getStatus))
			.returns(Set.of("t1", "t2"), from(DatumSamples::getTags))
			;
		// @formatter:on
	}

	@Test
	public void select_location() {
		// GIVEN
		store(SimpleDatum.locationDatum(TEST_LOC_ID, "a", START, null), watts(1));

		// WHEN
		List<StoredDatum> result = select(
				SelectDatum.forSource(schema, TEST_NODE_ID, "a", null, 0, 1, null));

		// THEN
		// @formatter:off
		and.then(result)
			.as("Location datum returned")
			.hasSize(1)
			.element(0)
			.returns(TEST_LOC_ID, from(StoredDatum::locationId))
			;
		// @formatter:on
	}

	@Test
	public void select_source_offsets() {
		// GIVEN
		storeHistory();

		// WHEN
		List<Integer> latest = watts(
				select(SelectDatum.forSource(schema, TEST_NODE_ID, "b", null, 0, 1, null)));
		List<Integer> offset = watts(
				select(SelectDatum.forSource(schema, TEST_NODE_ID, "b", null, 3, 1, null)));
		List<Integer> slice = watts(
				select(SelectDatum.forSource(schema, TEST_NODE_ID, "b", null, 1, 3, null)));
		List<Integer> past = watts(
				select(SelectDatum.forSource(schema, TEST_NODE_ID, "b", null, 5, 1, null)));

		// THEN
		// @formatter:off
		and.then(latest).as("Latest datum").containsExactly(4);
		and.then(offset).as("Offset datum").containsExactly(1);
		and.then(slice).as("Slice of datum, newest first").containsExactly(3, 2, 1);
		and.then(past).as("No datum past history").isEmpty();
		// @formatter:on
	}

	@Test
	public void select_source_timestamp() {
		// GIVEN
		storeHistory();

		// WHEN
		List<Integer> exact = watts(select(
				SelectDatum.forSource(schema, TEST_NODE_ID, "b", START.plusSeconds(2), 0, 1, null)));
		List<Integer> between = watts(select(SelectDatum.forSource(schema, TEST_NODE_ID, "b",
				START.plusSeconds(2).plusMillis(1), 0, 1, null)));
		List<Integer> slice = watts(select(
				SelectDatum.forSource(schema, TEST_NODE_ID, "b", START.plusSeconds(3), 1, 5, null)));
		List<Integer> before = watts(select(
				SelectDatum.forSource(schema, TEST_NODE_ID, "b", START.minusMillis(1), 0, 1, null)));

		// THEN
		// @formatter:off
		and.then(exact).as("Datum at timestamp").containsExactly(2);
		and.then(between).as("Datum before timestamp").containsExactly(2);
		and.then(slice).as("Slice back from timestamp, newest first").containsExactly(2, 1, 0);
		and.then(before).as("No datum before history").isEmpty();
		// @formatter:on
	}

	@Test
	public void select_streams_all() {
		// GIVEN
		storeHistory();

		// WHEN
		List<StoredDatum> latest = select(
				SelectDatum.forStreams(schema, TEST_NODE_ID, null, null, 0, null));
		List<StoredDatum> offset = select(
				SelectDatum.forStreams(schema, TEST_NODE_ID, null, START.plusSeconds(2), 1, null));

		// THEN
		// @formatter:off
		and.then(latest)
			.as("Latest datum of each source, by source ID")
			.extracting(StoredDatum::sourceId, d -> d.samples().getInstantaneousSampleInteger("watts"))
			.containsExactly(
					tuple("a", 4),
					tuple("b", 4),
					tuple("c", 4))
			;
		and.then(watts(offset))
			.as("Offset datum of each source back from timestamp")
			.containsExactly(1, 1, 1)
			;
		// @formatter:on
	}

	@Test
	public void select_streams_ids() {
		// GIVEN
		storeHistory();
		List<DatumStreamInfo> streams = jdbcTemplate.query(new SelectDatumStreams(schema, TEST_NODE_ID),
				DatumStreamInfoRowMapper.INSTANCE);
		List<Integer> ids = streams.stream().filter(s -> !s.sourceId().equals("b"))
				.map(DatumStreamInfo::streamId).toList();

		// WHEN
		List<StoredDatum> result = select(
				SelectDatum.forStreams(schema, TEST_NODE_ID, ids, null, 0, null));
		List<StoredDatum> none = select(
				SelectDatum.forStreams(schema, TEST_NODE_ID, List.of(), null, 0, null));

		// THEN
		// @formatter:off
		and.then(result)
			.as("Latest datum of given streams")
			.extracting(StoredDatum::sourceId)
			.containsExactly("a", "c")
			;
		and.then(none)
			.as("No datum for empty stream list")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void select_otherNode() {
		// GIVEN
		storeHistory();

		// WHEN
		List<StoredDatum> source = select(
				SelectDatum.forSource(schema, TEST_NODE_ID_2, "a", null, 0, 1, null));
		List<StoredDatum> streams = select(
				SelectDatum.forStreams(schema, TEST_NODE_ID_2, null, null, 0, null));

		// THEN
		// @formatter:off
		and.then(source).as("No datum for other node").isEmpty();
		and.then(streams).as("No datum for other node").isEmpty();
		// @formatter:on
	}

	@Test
	public void select_mergesStreamsOfSource() {
		// GIVEN
		store(datum("a", START), watts(0));
		store(SimpleDatum.locationDatum(TEST_LOC_ID, "a", START.plusSeconds(1), null), watts(1));
		store(datum("a", START.plusSeconds(2)), watts(2));
		store(SimpleDatum.locationDatum(TEST_LOC_ID, "a", START.plusSeconds(3), null), watts(3));

		// WHEN
		List<StoredDatum> slice = select(
				SelectDatum.forSource(schema, TEST_NODE_ID, "a", null, 0, 4, null));
		List<StoredDatum> streams = select(
				SelectDatum.forStreams(schema, TEST_NODE_ID, null, null, 1, null));

		// THEN
		// @formatter:off
		and.then(slice)
			.as("Datum of both streams returned, newest first")
			.extracting(d -> d.samples().getInstantaneousSampleInteger("watts"), StoredDatum::locationId)
			.containsExactly(
					tuple(3, TEST_LOC_ID),
					tuple(2, null),
					tuple(1, TEST_LOC_ID),
					tuple(0, null))
			;
		and.then(watts(streams))
			.as("One offset datum for source of both streams")
			.containsExactly(2)
			;
		// @formatter:on
	}

	@Test
	public void select_sameTimestampOfSource() {
		// GIVEN
		StoreDatum.Result node = store(datum("a", START), watts(0));
		StoreDatum.Result loc = store(SimpleDatum.locationDatum(TEST_LOC_ID, "a", START, null),
				watts(1));

		// WHEN
		List<StoredDatum> slice = select(
				SelectDatum.forSource(schema, TEST_NODE_ID, "a", null, 0, 2, null));

		// THEN
		// @formatter:off
		and.then(slice)
			.as("Datum with same timestamp ordered by stream, newest stream first")
			.extracting(StoredDatum::streamId)
			.containsExactly(loc.streamId(), node.streamId())
			;
		// @formatter:on
	}

	@Test
	public void select_typeFilter() {
		// GIVEN
		store(datum("a", START), watts(1));
		store(new SimpleAcEnergyDatum("b", START, null), watts(2));
		store(datum("b", START.plusSeconds(1)), watts(3));

		final String type = AcEnergyDatum.class.getName();

		// WHEN
		List<StoredDatum> a = select(SelectDatum.forSource(schema, TEST_NODE_ID, "a", null, 0, 1, type));
		List<StoredDatum> b = select(SelectDatum.forSource(schema, TEST_NODE_ID, "b", null, 0, 2, type));
		List<StoredDatum> streams = select(
				SelectDatum.forStreams(schema, TEST_NODE_ID, null, null, 0, type));
		List<StoredDatum> unknown = select(
				SelectDatum.forStreams(schema, TEST_NODE_ID, null, null, 0, "foo.Unknown"));

		// THEN
		// @formatter:off
		and.then(a)
			.as("Source without type pruned")
			.isEmpty()
			;
		and.then(b)
			.as("All datum of source with type returned, to filter after choosing offset")
			.extracting(StoredDatum::className)
			.containsExactly(SimpleDatum.class.getName(), SimpleAcEnergyDatum.class.getName())
			;
		and.then(streams)
			.as("Only source with type returned")
			.extracting(StoredDatum::sourceId)
			.containsExactly("b")
			;
		and.then(unknown)
			.as("No datum for unknown type")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void select_otherSchema() {
		// GIVEN
		storeHistory();

		// WHEN
		String other = otherSchema();
		List<StoredDatum> result = select(
				SelectDatum.forStreams(other, TEST_NODE_ID, null, null, 0, null));

		// THEN
		// @formatter:off
		and.then(result)
			.as("No datum from other schema")
			.isEmpty()
			;
		and.then(Arrays.asList(JdbcDatumHistorian.DEFAULT_SCHEMA_NAME, JdbcDatumHistorian.UNFILTERED_SCHEMA_NAME))
			.as("Test schemas")
			.contains(schema, other)
			;
		// @formatter:on
	}

}
