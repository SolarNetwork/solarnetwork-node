/* ==================================================================
 * StoredDatumRowMapperTests.java - 11 Nov 2026 12:21:37 am
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

import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.dao.DataRetrievalFailureException;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.datumhistorian.jdbc.StoredDatum;
import net.solarnetwork.node.datumhistorian.jdbc.StoredDatumRowMapper;

/**
 * Test cases for the {@link StoredDatumRowMapper} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@RunWith(MockitoJUnitRunner.class)
public class StoredDatumRowMapperTests {

	@Mock
	private ResultSet rs;

	private final List<Array> arrays = new ArrayList<>();

	private final StoredDatumRowMapper mapper = new StoredDatumRowMapper(
			JsonUtils.newDatumObjectMapper());

	private final Instant ts = Instant.now().truncatedTo(ChronoUnit.MICROS);

	@After
	public void teardown() throws SQLException {
		for ( Array a : arrays ) {
			then(a).should().free();
		}
	}

	private Array array(String... values) throws SQLException {
		Array a = mock(Array.class);
		given(a.getArray()).willReturn(values);
		arrays.add(a);
		return a;
	}

	private void givenRow(Long locId, String[] namesI, String[] dataI, String[] namesA,
			String[] dataA, String[] namesS, String dataS, String[] tags) throws SQLException {
		given(rs.getInt(1)).willReturn(1);
		given(rs.getString(2)).willReturn("a");
		given(rs.getLong(3)).willReturn(locId != null ? locId : 0L);
		given(rs.wasNull()).willReturn(locId == null);
		given(rs.getTimestamp(4)).willReturn(Timestamp.from(ts));
		given(rs.getString(5)).willReturn("foo.Bar");
		Array typeNames = array("foo.Bar", "foo.Datum");
		given(rs.getArray(6)).willReturn(typeNames);
		Array ni = (namesI != null ? array(namesI) : null);
		Array na = (namesA != null ? array(namesA) : null);
		Array ns = (namesS != null ? array(namesS) : null);
		Array di = (dataI != null ? array(dataI) : null);
		Array da = (dataA != null ? array(dataA) : null);
		Array dt = (tags != null ? array(tags) : null);
		given(rs.getArray(7)).willReturn(ni);
		given(rs.getArray(8)).willReturn(na);
		given(rs.getArray(9)).willReturn(ns);
		given(rs.getArray(10)).willReturn(di);
		given(rs.getArray(11)).willReturn(da);
		given(rs.getString(12)).willReturn(dataS);
		given(rs.getArray(13)).willReturn(dt);
	}

	@Test
	public void map_all() throws SQLException {
		// GIVEN
		givenRow(123L, new String[] { "a", "b", "c" }, new String[] { "1.5", null, "NaN" },
				new String[] { "x", "y" }, new String[] { "Infinity", "-Infinity" },
				new String[] { "s1", "s2", "s3", "s4" }, "[\"one\", 2, 3.25, true]",
				new String[] { "t1", "t2" });

		// WHEN
		StoredDatum result = mapper.mapRow(rs, 1);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Stream ID mapped")
			.returns(1, from(StoredDatum::streamId))
			.as("Source ID mapped")
			.returns("a", from(StoredDatum::sourceId))
			.as("Location ID mapped")
			.returns(123L, from(StoredDatum::locationId))
			.as("Timestamp mapped")
			.returns(ts, from(StoredDatum::timestamp))
			.as("Class name mapped")
			.returns("foo.Bar", from(StoredDatum::className))
			.as("Type names mapped")
			.returns(Set.of("foo.Bar", "foo.Datum"), from(StoredDatum::typeNames))
			;
		DatumSamples s = result.samples();
		and.then(s.getInstantaneous())
			.as("Instantaneous values mapped by name, skipping NULL")
			.containsOnlyKeys("a", "c")
			.containsEntry("a", new BigDecimal("1.5"))
			.containsEntry("c", Double.NaN)
			;
		and.then(s.getAccumulating())
			.as("Accumulating values mapped by name")
			.containsOnlyKeys("x", "y")
			.containsEntry("x", Double.POSITIVE_INFINITY)
			.containsEntry("y", Double.NEGATIVE_INFINITY)
			;
		and.then(s.getStatus())
			.as("Status values mapped with JSON types")
			.containsEntry("s1", "one")
			.containsEntry("s2", 2)
			.containsEntry("s4", true)
			.hasSize(4)
			;
		and.then(s.getStatusSampleBigDecimal("s3")).isEqualByComparingTo("3.25");
		and.then(s.getTags()).containsExactlyInAnyOrder("t1", "t2");
		// @formatter:on
	}

	@Test
	public void map_nodeDatum_noData() throws SQLException {
		// GIVEN
		givenRow(null, null, null, null, null, null, null, null);

		// WHEN
		StoredDatum result = mapper.mapRow(rs, 1);

		// THEN
		// @formatter:off
		and.then(result)
			.as("No location ID for node datum")
			.returns(null, from(StoredDatum::locationId))
			.extracting(StoredDatum::samples)
			.as("No samples")
			.returns(null, from(DatumSamples::getInstantaneous))
			.returns(null, from(DatumSamples::getAccumulating))
			.returns(null, from(DatumSamples::getStatus))
			.returns(null, from(DatumSamples::getTags))
			;
		// @formatter:on
	}

	@Test
	public void map_valuesShorterThanNames() throws SQLException {
		// GIVEN
		givenRow(null, new String[] { "a", "b", "c" }, new String[] { "1" }, null, null,
				new String[] { "s1", "s2" }, "[null, \"two\"]", null);

		// WHEN
		StoredDatum result = mapper.mapRow(rs, 1);

		// THEN
		// @formatter:off
		and.then(result.samples())
			.as("Only names with values mapped")
			.returns(Map.of("a", new BigDecimal("1")), from(DatumSamples::getInstantaneous))
			.returns(Map.of("s2", "two"), from(DatumSamples::getStatus))
			;
		// @formatter:on
	}

	@Test
	public void map_statusNotArray() throws SQLException {
		// GIVEN
		givenRow(null, null, null, null, null, new String[] { "s1" }, "{\"s1\":1}", null);

		// THEN
		thenExceptionOfType(DataRetrievalFailureException.class).isThrownBy(() -> mapper.mapRow(rs, 1))
				.withMessageContaining("not a JSON array");
	}

	@Test
	public void map_statusNotJson() throws SQLException {
		// GIVEN
		givenRow(null, null, null, null, null, new String[] { "s1" }, "[nope", null);

		// THEN
		thenExceptionOfType(DataRetrievalFailureException.class).isThrownBy(() -> mapper.mapRow(rs, 1))
				.withMessageContaining("not valid JSON");
	}

	@Test
	public void number() {
		// @formatter:off
		and.then(StoredDatumRowMapper.number("123")).isEqualTo(new BigDecimal("123"));
		and.then(StoredDatumRowMapper.number("-1.2300")).isEqualTo(new BigDecimal("-1.2300"));
		and.then(StoredDatumRowMapper.number("1.5E+10")).isEqualTo(new BigDecimal("1.5E+10"));
		and.then(StoredDatumRowMapper.number("NaN")).isEqualTo(Double.NaN);
		and.then(StoredDatumRowMapper.number("Infinity")).isEqualTo(Double.POSITIVE_INFINITY);
		and.then(StoredDatumRowMapper.number("-Infinity")).isEqualTo(Double.NEGATIVE_INFINITY);
		thenExceptionOfType(DataRetrievalFailureException.class)
			.isThrownBy(() -> StoredDatumRowMapper.number("abc"))
			.withMessageContaining("abc")
			;
		// @formatter:on
	}


	@Test
	public void map_withoutNodeId() throws SQLException {
		// GIVEN
		givenRow(null, null, null, null, null, null, null, null);

		// WHEN
		StoredDatum result = mapper.mapRow(rs, 1);

		// THEN
		// @formatter:off
		and.then(result)
			.as("No node ID")
			.returns(null, from(StoredDatum::nodeId))
			;
		then(rs).should(never()).getLong(14);
		// @formatter:on
	}

	private void givenMinimalRow(long locId, boolean locIdNull, long nodeId, boolean nodeIdNull)
			throws SQLException {
		given(rs.getInt(1)).willReturn(1);
		given(rs.getString(2)).willReturn("a");
		given(rs.getLong(3)).willReturn(locId);
		given(rs.getTimestamp(4)).willReturn(Timestamp.from(ts));
		given(rs.getString(5)).willReturn("foo.Bar");
		given(rs.getLong(14)).willReturn(nodeId);
		given(rs.wasNull()).willReturn(locIdNull, nodeIdNull);
	}

	@Test
	public void map_withNodeId() throws SQLException {
		// GIVEN
		givenMinimalRow(0L, true, 234L, false);
		StoredDatumRowMapper nodeMapper = new StoredDatumRowMapper(JsonUtils.newDatumObjectMapper(),
				true);

		// WHEN
		StoredDatum result = nodeMapper.mapRow(rs, 1);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Node ID mapped")
			.returns(234L, from(StoredDatum::nodeId))
			.as("No location ID")
			.returns(null, from(StoredDatum::locationId))
			.as("Node datum ID has node ID")
			.returns(234L, from(d -> d.datumId().getObjectId()))
			;
		// @formatter:on
	}

	@Test
	public void map_withNodeId_null() throws SQLException {
		// GIVEN
		givenMinimalRow(123L, false, 0L, true);
		StoredDatumRowMapper nodeMapper = new StoredDatumRowMapper(JsonUtils.newDatumObjectMapper(),
				true);

		// WHEN
		StoredDatum result = nodeMapper.mapRow(rs, 1);

		// THEN
		// @formatter:off
		and.then(result)
			.as("NULL node ID mapped")
			.returns(null, from(StoredDatum::nodeId))
			.as("Location ID mapped")
			.returns(123L, from(StoredDatum::locationId))
			;
		// @formatter:on
	}

}
