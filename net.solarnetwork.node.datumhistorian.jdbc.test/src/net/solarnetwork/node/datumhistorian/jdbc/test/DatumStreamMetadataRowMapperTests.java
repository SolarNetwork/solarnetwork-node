/* ==================================================================
 * DatumStreamMetadataRowMapperTests.java - 13 Nov 2026 10:18:37 am
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

import static net.solarnetwork.node.service.support.DatumStreamIdUtils.encodeLocalStreamId;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.node.datumhistorian.jdbc.DatumStreamMetadataRowMapper;

/**
 * Test cases for the {@link DatumStreamMetadataRowMapper} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@RunWith(MockitoJUnitRunner.class)
public class DatumStreamMetadataRowMapperTests {

	private static final Long TEST_NODE_ID = 123L;
	private static final Long TEST_LOC_ID = 345L;
	private static final int TEST_STREAM_ID = 3;
	private static final String TEST_TZ = "Pacific/Auckland";

	@Mock
	private ResultSet rs;

	@Mock
	private Array namesI;

	@Mock
	private Array namesA;

	@Mock
	private Array namesS;

	private void givenStream(String sourceId, Long locationId) throws SQLException {
		given(rs.getInt(1)).willReturn(TEST_STREAM_ID);
		given(rs.getLong(2)).willReturn(TEST_NODE_ID);
		given(rs.getString(3)).willReturn(sourceId);
		given(rs.getObject(4, Long.class)).willReturn(locationId);
	}

	@Test
	public void nodeStream() throws SQLException {
		// GIVEN
		givenStream("a/1", null);
		given(rs.getArray(5)).willReturn(namesI);
		given(rs.getArray(6)).willReturn(namesA);
		given(rs.getArray(7)).willReturn(namesS);
		given(namesI.getArray()).willReturn(new String[] { "watts", "current" });
		given(namesA.getArray()).willReturn(new String[] { "wattHours" });
		given(namesS.getArray()).willReturn(new String[] { "mode" });

		// WHEN
		ObjectDatumStreamMetadata result = new DatumStreamMetadataRowMapper(1, TEST_TZ).mapRow(rs, 0);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Stream ID encoded from node ID, domain, and stream ID")
			.returns(encodeLocalStreamId(TEST_NODE_ID, 1, TEST_STREAM_ID),
					from(ObjectDatumStreamMetadata::getStreamId))
			.as("Time zone given")
			.returns(TEST_TZ, from(ObjectDatumStreamMetadata::getTimeZoneId))
			.as("Node stream without location ID")
			.returns(ObjectDatumKind.Node, from(ObjectDatumStreamMetadata::getKind))
			.as("Node ID is object ID")
			.returns(TEST_NODE_ID, from(ObjectDatumStreamMetadata::getObjectId))
			.as("Source ID")
			.returns("a/1", from(ObjectDatumStreamMetadata::getSourceId))
			;
		and.then(result.propertyNamesForType(DatumSamplesType.Instantaneous))
			.as("Instantaneous property names")
			.containsExactly("watts", "current")
			;
		and.then(result.propertyNamesForType(DatumSamplesType.Accumulating))
			.as("Accumulating property names")
			.containsExactly("wattHours")
			;
		and.then(result.propertyNamesForType(DatumSamplesType.Status))
			.as("Status property names")
			.containsExactly("mode")
			;
		then(namesI).should().free();
		then(namesA).should().free();
		then(namesS).should().free();
		// @formatter:on
	}

	@Test
	public void locationStream() throws SQLException {
		// GIVEN
		givenStream("weather", TEST_LOC_ID);

		// WHEN
		ObjectDatumStreamMetadata result = new DatumStreamMetadataRowMapper(2, null).mapRow(rs, 0);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Stream ID encoded from node ID that stored stream, domain, and stream ID")
			.returns(encodeLocalStreamId(TEST_NODE_ID, 2, TEST_STREAM_ID),
					from(ObjectDatumStreamMetadata::getStreamId))
			.as("No time zone")
			.returns(null, from(ObjectDatumStreamMetadata::getTimeZoneId))
			.as("Location stream with location ID")
			.returns(ObjectDatumKind.Location, from(ObjectDatumStreamMetadata::getKind))
			.as("Location ID is object ID")
			.returns(TEST_LOC_ID, from(ObjectDatumStreamMetadata::getObjectId))
			.as("Source ID")
			.returns("weather", from(ObjectDatumStreamMetadata::getSourceId))
			;
		for ( DatumSamplesType type : new DatumSamplesType[] { DatumSamplesType.Instantaneous,
				DatumSamplesType.Accumulating, DatumSamplesType.Status } ) {
			and.then(result.propertyNamesForType(type))
				.as("No %s property names for NULL names", type)
				.isNull()
				;
		}
		// @formatter:on
	}

}
