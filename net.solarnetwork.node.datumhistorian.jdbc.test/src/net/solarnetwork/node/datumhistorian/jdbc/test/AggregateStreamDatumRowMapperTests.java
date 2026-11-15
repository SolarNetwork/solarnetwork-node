/* ==================================================================
 * AggregateStreamDatumRowMapperTests.java - 15 Nov 2026 5:52:09 pm
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
import static org.assertj.core.api.BDDAssertions.thenIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyInt;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.datum.AggregateStreamDatum;
import net.solarnetwork.domain.datum.BasicAggregateStreamDatum;
import net.solarnetwork.domain.datum.BasicObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.BasicReadingStreamDatum;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumPropertiesStatistics;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ReadingStreamDatum;
import net.solarnetwork.domain.datum.StreamDatum;
import net.solarnetwork.node.datumhistorian.jdbc.AggregateStreamDatumRowMapper;
import net.solarnetwork.node.datumhistorian.jdbc.AggregateStreamDatumRowMapper.Style;
import net.solarnetwork.node.datumhistorian.jdbc.DatumStreamMetadataProvider;

/**
 * Test cases for the {@link AggregateStreamDatumRowMapper} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@RunWith(MockitoJUnitRunner.class)
public class AggregateStreamDatumRowMapperTests {

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

	@Mock
	private Array statI;

	@Mock
	private Array readA;

	private final ObjectMapper objectMapper = JsonUtils.newDatumObjectMapper();
	private final Instant ts = Instant.now().truncatedTo(ChronoUnit.HOURS);
	private final Instant endTs = ts.plus(1, ChronoUnit.HOURS).minusSeconds(60);
	private final UUID streamId = encodeLocalStreamId(TEST_NODE_ID, TEST_DOMAIN, TEST_STREAM_ID);

	private DatumStreamMetadataProvider provider;

	@Before
	public void setup() {
		provider = new DatumStreamMetadataProvider(List.of(meta(streamId)));
	}

	private static ObjectDatumStreamMetadata meta(UUID streamId) {
		return new BasicObjectDatumStreamMetadata(streamId, "UTC", ObjectDatumKind.Node, TEST_NODE_ID,
				"a", new String[] { "i", "j" }, new String[] { "a" }, new String[] { "s" });
	}

	private AggregateStreamDatumRowMapper mapper(Style style) {
		return new AggregateStreamDatumRowMapper(provider, TEST_DOMAIN, objectMapper, style);
	}

	private static BigDecimal[] decimals(String... values) {
		final BigDecimal[] result = new BigDecimal[values.length];
		for ( int i = 0; i < values.length; i++ ) {
			result[i] = (values[i] != null ? new BigDecimal(values[i]) : null);
		}
		return result;
	}

	private void givenStream() throws SQLException {
		given(rs.getInt(1)).willReturn(TEST_STREAM_ID);
		given(rs.getLong(2)).willReturn(TEST_NODE_ID);
	}

	private void givenRow() throws SQLException {
		givenStream();
		given(rs.getTimestamp(3)).willReturn(Timestamp.from(ts));
		given(rs.getArray(4)).willReturn(dataI);
		givenNumericArray(dataI, "1.5", "20");
		given(rs.getArray(5)).willReturn(dataA);
		givenNumericArray(dataA, "300");
		given(rs.getString(6)).willReturn("[\"on\"]");
		given(rs.getArray(7)).willReturn(dataT);
		given(dataT.getArray()).willReturn(new String[] { "t" });
		given(rs.getArray(8)).willReturn(statI);
		given(statI.getArray()).willReturn(new BigDecimal[][] { decimals("60", "1", "2"),
				decimals("60", "10", "30") });
		given(rs.getArray(9)).willReturn(readA);
		given(readA.getArray()).willReturn(new BigDecimal[][] { decimals("310", "1000", "1310") });
	}

	private void thenRowValues(AggregateStreamDatum result) throws SQLException {
		// @formatter:off
		and.then(result)
			.as("Stream ID encoded from node ID, domain, and stream ID")
			.returns(streamId, from(StreamDatum::getStreamId))
			.as("Timestamp from period start")
			.returns(ts, from(StreamDatum::getTimestamp))
			;
		final DatumProperties props = result.getProperties();
		and.then(props.getInstantaneous())
			.as("Instantaneous averages")
			.containsExactly(new BigDecimal("1.5"), new BigDecimal("20"))
			;
		and.then(props.getAccumulating())
			.as("Accumulating values")
			.containsExactly(new BigDecimal("300"))
			;
		and.then(props.getStatus())
			.as("Status values from JSON array")
			.containsExactly("on")
			;
		and.then(props.getTags())
			.as("Tags")
			.containsExactly("t")
			;
		final DatumPropertiesStatistics stats = result.getStatistics();
		and.then(stats.getInstantaneous())
			.as("Instantaneous statistics from stat_i")
			.isDeepEqualTo(new BigDecimal[][] { decimals("60", "1", "2"), decimals("60", "10", "30") })
			;
		and.then(stats.getAccumulating())
			.as("Accumulating statistics from read_a")
			.isDeepEqualTo(new BigDecimal[][] { decimals("310", "1000", "1310") })
			;
		then(dataI).should().free();
		then(dataA).should().free();
		then(dataT).should().free();
		then(statI).should().free();
		then(readA).should().free();
		// @formatter:on
	}

	@Test
	public void construct_invalid() {
		thenIllegalArgumentException().isThrownBy(
				() -> new AggregateStreamDatumRowMapper(null, TEST_DOMAIN, objectMapper, Style.Aggregate));
		thenIllegalArgumentException().isThrownBy(
				() -> new AggregateStreamDatumRowMapper(provider, TEST_DOMAIN, null, Style.Aggregate));
		thenIllegalArgumentException().isThrownBy(
				() -> new AggregateStreamDatumRowMapper(provider, TEST_DOMAIN, objectMapper, null));
	}

	@Test
	public void style() {
		for ( Style style : Style.values() ) {
			and.then(mapper(style)).as("Style %s", style).returns(style,
					from(AggregateStreamDatumRowMapper::getStyle));
		}
	}

	@Test
	public void aggregate() throws SQLException {
		// GIVEN
		givenRow();

		// WHEN
		AggregateStreamDatum result = mapper(Style.Aggregate).mapRow(rs, 0);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Aggregate datum")
			.isExactlyInstanceOf(BasicAggregateStreamDatum.class)
			.as("No end timestamp")
			.returns(null, from(AggregateStreamDatum::getEndTimestamp))
			;
		thenRowValues(result);
		then(rs).should(org.mockito.Mockito.never()).getTimestamp(10);
		// @formatter:on
	}

	@Test
	public void reading() throws SQLException {
		// GIVEN
		givenRow();

		// WHEN
		AggregateStreamDatum result = mapper(Style.Reading).mapRow(rs, 0);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Reading datum")
			.isExactlyInstanceOf(BasicReadingStreamDatum.class)
			.as("No end timestamp for stored aggregate reading")
			.returns(null, from(AggregateStreamDatum::getEndTimestamp))
			;
		thenRowValues(result);
		then(rs).should(org.mockito.Mockito.never()).getTimestamp(10);
		// @formatter:on
	}

	@Test
	public void readingDifference() throws SQLException {
		// GIVEN
		givenRow();
		given(rs.getTimestamp(10)).willReturn(Timestamp.from(endTs));

		// WHEN
		AggregateStreamDatum result = mapper(Style.ReadingDifference).mapRow(rs, 0);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Reading datum")
			.isInstanceOf(ReadingStreamDatum.class)
			.isExactlyInstanceOf(BasicReadingStreamDatum.class)
			.as("End timestamp from time of ending reading")
			.returns(endTs, from(AggregateStreamDatum::getEndTimestamp))
			;
		thenRowValues(result);
		// @formatter:on
	}

	@Test
	public void readingDifference_noEnd() throws SQLException {
		// GIVEN
		givenStream();
		given(rs.getTimestamp(3)).willReturn(Timestamp.from(ts));
		given(rs.getTimestamp(10)).willReturn(null);

		// WHEN
		AggregateStreamDatum result = mapper(Style.ReadingDifference).mapRow(rs, 0);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Reading datum")
			.isExactlyInstanceOf(BasicReadingStreamDatum.class)
			.as("No end timestamp for NULL column")
			.returns(null, from(AggregateStreamDatum::getEndTimestamp))
			;
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
		and.then(result.getStatistics())
			.as("No instantaneous statistics")
			.returns(null, from(DatumPropertiesStatistics::getInstantaneous))
			.as("No accumulating statistics")
			.returns(null, from(DatumPropertiesStatistics::getAccumulating))
			;
		// @formatter:on
	}

	@Test
	public void unknownStream_null() throws SQLException {
		// GIVEN
		given(rs.getInt(1)).willReturn(TEST_STREAM_ID + 1);
		given(rs.getLong(2)).willReturn(TEST_NODE_ID);

		// WHEN
		AggregateStreamDatum result = mapper(Style.Aggregate).mapRow(rs, 0);

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
	public void otherDomain_noMetadata_null() throws SQLException {
		// GIVEN
		givenStream();

		// WHEN
		AggregateStreamDatum result = new AggregateStreamDatumRowMapper(provider, 2, objectMapper,
				Style.Reading).mapRow(rs, 0);

		// THEN
		// @formatter:off
		and.then(result)
			.as("No datum when metadata is only for stream ID of other domain")
			.isNull()
			;
		then(rs).should(org.mockito.Mockito.never()).getTimestamp(anyInt());
		// @formatter:on
	}

}
