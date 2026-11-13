/* ==================================================================
 * StreamDatumRowMapperTests.java - 13 Nov 2026 10:18:37 am
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

import static net.solarnetwork.node.datumhistorian.jdbc.test.PgArrayStubs.givenNumericArray;
import static net.solarnetwork.node.service.support.DatumStreamIdUtils.encodeLocalStreamId;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.assertj.core.api.BDDAssertions.thenIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.dao.DataRetrievalFailureException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.datum.BasicObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.StreamDatum;
import net.solarnetwork.node.datumhistorian.jdbc.DatumStreamMetadataProvider;
import net.solarnetwork.node.datumhistorian.jdbc.StreamDatumRowMapper;

/**
 * Test cases for the {@link StreamDatumRowMapper} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@RunWith(MockitoJUnitRunner.class)
public class StreamDatumRowMapperTests {

	private static final Long TEST_NODE_ID = 123L;
	private static final int TEST_STREAM_ID = 3;
	private static final int TEST_DOMAIN = 1;

	@Mock
	private ResultSet rs;

	@Mock
	private Array dataI;

	@Mock
	private Array dataA;

	@Mock
	private Array dataT;

	private final ObjectMapper objectMapper = JsonUtils.newDatumObjectMapper();
	private final Instant ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);
	private final UUID streamId = encodeLocalStreamId(TEST_NODE_ID, TEST_DOMAIN, TEST_STREAM_ID);

	private DatumStreamMetadataProvider provider;
	private StreamDatumRowMapper mapper;

	@Before
	public void setup() {
		provider = new DatumStreamMetadataProvider(List.of(meta(streamId)));
		mapper = new StreamDatumRowMapper(provider, TEST_DOMAIN, objectMapper);
	}

	private static ObjectDatumStreamMetadata meta(UUID streamId) {
		return new BasicObjectDatumStreamMetadata(streamId, "UTC", ObjectDatumKind.Node, TEST_NODE_ID,
				"a", new String[] { "i" }, null, null);
	}

	private void givenStream() throws SQLException {
		given(rs.getInt(1)).willReturn(TEST_STREAM_ID);
		given(rs.getLong(2)).willReturn(TEST_NODE_ID);
	}

	// the i/a values are given as Postgres formats a NUMERIC, and read as the driver reads them
	private void givenRow(String[] i, String[] a, String s, String[] t) throws SQLException {
		givenStream();
		given(rs.getTimestamp(3)).willReturn(Timestamp.from(ts));
		if ( i != null ) {
			given(rs.getArray(4)).willReturn(dataI);
			givenNumericArray(dataI, i);
		}
		if ( a != null ) {
			given(rs.getArray(5)).willReturn(dataA);
			givenNumericArray(dataA, a);
		}
		if ( s != null ) {
			given(rs.getString(6)).willReturn(s);
		}
		if ( t != null ) {
			given(rs.getArray(7)).willReturn(dataT);
			given(dataT.getArray()).willReturn(t);
		}
	}

	@Test
	public void construct_invalid() {
		thenIllegalArgumentException()
				.isThrownBy(() -> new StreamDatumRowMapper(null, TEST_DOMAIN, objectMapper));
		thenIllegalArgumentException()
				.isThrownBy(() -> new StreamDatumRowMapper(provider, TEST_DOMAIN, null));
	}

	@Test
	public void row() throws SQLException {
		// GIVEN
		givenRow(new String[] { "1.23", "4" }, new String[] { "5000" }, "[\"on\", \"3\"]",
				new String[] { "a", "b" });

		// WHEN
		StreamDatum result = mapper.mapRow(rs, 0);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Stream ID encoded from node ID, domain, and stream ID")
			.returns(streamId, from(StreamDatum::getStreamId))
			.as("Timestamp")
			.returns(ts, from(StreamDatum::getTimestamp))
			;
		final DatumProperties props = result.getProperties();
		and.then(props.getInstantaneous())
			.as("Instantaneous values")
			.containsExactly(new BigDecimal("1.23"), new BigDecimal("4"))
			;
		and.then(props.getAccumulating())
			.as("Accumulating values")
			.containsExactly(new BigDecimal("5000"))
			;
		and.then(props.getStatus())
			.as("Status values")
			.containsExactly("on", "3")
			;
		and.then(props.getTags())
			.as("Tags")
			.containsExactly("a", "b")
			;
		then(dataI).should().free();
		then(dataA).should().free();
		then(dataT).should().free();
		// @formatter:on
	}

	@Test
	public void nonFiniteValues_null() throws SQLException {
		// GIVEN
		givenRow(new String[] { "NaN", "1", null }, new String[] { "Infinity", "-Infinity" }, null,
				null);

		// WHEN
		StreamDatum result = mapper.mapRow(rs, 0);

		// THEN
		// @formatter:off
		and.then(result.getProperties().getInstantaneous())
			.as("NaN and NULL values given as null")
			.containsExactly(null, BigDecimal.ONE, null)
			;
		and.then(result.getProperties().getAccumulating())
			.as("Infinite values given as null")
			.containsExactly(null, null)
			;
		then(dataI).should().free();
		then(dataA).should().free();
		// @formatter:on
	}

	@Test
	public void nullColumns() throws SQLException {
		// GIVEN
		givenRow(null, null, null, null);

		// WHEN
		StreamDatum result = mapper.mapRow(rs, 0);

		// THEN
		// @formatter:off
		and.then(result.getProperties())
			.as("No instantaneous values")
			.returns(null, from(DatumProperties::getInstantaneous))
			.as("No accumulating values")
			.returns(null, from(DatumProperties::getAccumulating))
			.as("No status values")
			.returns(null, from(DatumProperties::getStatus))
			.as("No tags")
			.returns(null, from(DatumProperties::getTags))
			;
		// @formatter:on
	}

	@Test
	public void statusValues() throws SQLException {
		// GIVEN
		givenRow(null, null, """
				["on", 0.00000015, -1.50, false, null, {"a":1}, [1,"b"], ""]""", null);

		// WHEN
		StreamDatum result = mapper.mapRow(rs, 0);

		// THEN
		// @formatter:off
		and.then(result.getProperties().getStatus())
			.as("Strings given as their value, other JSON values as their JSON text, numbers as stored")
			.containsExactly("on", "0.00000015", "-1.50", "false", null, "{\"a\":1}", "[1,\"b\"]", "")
			;
		// @formatter:on
	}

	@Test
	public void unknownStream_null() throws SQLException {
		// GIVEN
		given(rs.getInt(1)).willReturn(TEST_STREAM_ID + 1);
		given(rs.getLong(2)).willReturn(TEST_NODE_ID);

		// WHEN
		StreamDatum result = mapper.mapRow(rs, 0);

		// THEN
		// @formatter:off
		and.then(result)
			.as("No datum for stream without metadata")
			.isNull()
			;
		then(rs).should().getInt(1);
		then(rs).should().getLong(2);
		then(rs).shouldHaveNoMoreInteractions();
		// @formatter:on
	}

	@Test
	public void otherNode_null() throws SQLException {
		// GIVEN
		given(rs.getInt(1)).willReturn(TEST_STREAM_ID);
		given(rs.getLong(2)).willReturn(TEST_NODE_ID + 1);

		// WHEN
		StreamDatum result = mapper.mapRow(rs, 0);

		// THEN
		// @formatter:off
		and.then(result)
			.as("No datum for same stream ID of other node without metadata")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void otherDomain() throws SQLException {
		// GIVEN
		final UUID otherStreamId = encodeLocalStreamId(TEST_NODE_ID, 2, TEST_STREAM_ID);
		final StreamDatumRowMapper otherMapper = new StreamDatumRowMapper(
				new DatumStreamMetadataProvider(List.of(meta(streamId), meta(otherStreamId))), 2,
				objectMapper);
		givenRow(null, null, null, null);

		// WHEN
		StreamDatum result = otherMapper.mapRow(rs, 0);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Stream ID encoded with domain of mapper")
			.returns(otherStreamId, from(StreamDatum::getStreamId))
			;
		// @formatter:on
	}

	@Test
	public void otherDomain_noMetadata_null() throws SQLException {
		// GIVEN
		givenStream();

		// WHEN
		StreamDatum result = new StreamDatumRowMapper(provider, 2, objectMapper).mapRow(rs, 0);

		// THEN
		// @formatter:off
		and.then(result)
			.as("No datum when metadata is only for stream ID of other domain")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void status_notArray() throws SQLException {
		// GIVEN
		givenRow(null, null, "{\"a\":1}", null);

		// THEN
		// @formatter:off
		thenExceptionOfType(DataRetrievalFailureException.class)
			.isThrownBy(() -> mapper.mapRow(rs, 0))
			.withMessageContaining("not a JSON array")
			;
		// @formatter:on
	}

	@Test
	public void status_invalidJson() throws SQLException {
		// GIVEN
		givenRow(null, null, "[on]", null);

		// THEN
		// @formatter:off
		thenExceptionOfType(DataRetrievalFailureException.class)
			.isThrownBy(() -> mapper.mapRow(rs, 0))
			.withMessageContaining("not valid JSON")
			;
		// @formatter:on
	}

	@Test
	public void instantaneous_notNumber() throws SQLException {
		// GIVEN
		givenRow(new String[] { "abc" }, null, null, null);

		// THEN
		// @formatter:off
		thenExceptionOfType(SQLException.class)
			.as("Driver error for value that is not a number or non-finite thrown")
			.isThrownBy(() -> mapper.mapRow(rs, 0))
			.withMessage("Bad value for type BigDecimal : abc")
			;
		then(dataI).should().free();
		// @formatter:on
	}

}
