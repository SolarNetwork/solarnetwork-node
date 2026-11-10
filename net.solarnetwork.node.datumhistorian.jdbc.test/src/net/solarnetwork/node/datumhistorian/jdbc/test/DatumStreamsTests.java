/* ==================================================================
 * DatumStreamsTests.java - 11 Nov 2026 12:33:10 am
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
import static org.assertj.core.api.BDDAssertions.thenIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.node.datumhistorian.jdbc.DatumStreamInfo;
import net.solarnetwork.node.datumhistorian.jdbc.DatumStreamInfoRowMapper;
import net.solarnetwork.node.datumhistorian.jdbc.DatumStreams;
import net.solarnetwork.node.datumhistorian.jdbc.StoredDatum;
import net.solarnetwork.node.domain.datum.AcEnergyDatum;
import net.solarnetwork.node.domain.datum.AtmosphericDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;

/**
 * Test cases for the stream and stored datum domain classes.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@RunWith(MockitoJUnitRunner.class)
public class DatumStreamsTests {

	@Mock
	private ResultSet rs;

	@Test
	public void streams_contains() {
		// GIVEN
		DatumStreams streams = new DatumStreams(
				List.of(new DatumStreamInfo(1, "a", null), new DatumStreamInfo(3, "b", 123L)));

		// THEN
		// @formatter:off
		and.then(streams.contains(1)).as("Contains first stream").isTrue();
		and.then(streams.contains(3)).as("Contains second stream").isTrue();
		and.then(streams.contains(2)).as("Does not contain other stream").isFalse();
		// @formatter:on
	}

	@Test
	public void streams_copied() {
		// GIVEN
		List<DatumStreamInfo> list = new ArrayList<>(List.of(new DatumStreamInfo(1, "a", null)));
		DatumStreams streams = new DatumStreams(list);

		// WHEN
		list.add(new DatumStreamInfo(2, "b", null));

		// THEN
		// @formatter:off
		and.then(streams.streams())
			.as("Streams list copied")
			.hasSize(1)
			;
		and.then(streams.contains(2))
			.as("Change to given list not seen")
			.isFalse()
			;
		// @formatter:on
		thenIllegalArgumentException().isThrownBy(() -> new DatumStreams(null));
	}

	@Test
	public void streams_serializable() throws IOException, ClassNotFoundException {
		// GIVEN
		DatumStreams streams = new DatumStreams(
				List.of(new DatumStreamInfo(1, "a", null), new DatumStreamInfo(3, "b", 123L)));

		// WHEN
		ByteArrayOutputStream byos = new ByteArrayOutputStream();
		try (ObjectOutputStream out = new ObjectOutputStream(byos)) {
			out.writeObject(streams);
		}
		Object result;
		try (ObjectInputStream in = new ObjectInputStream(
				new ByteArrayInputStream(byos.toByteArray()))) {
			result = in.readObject();
		}

		// THEN
		// @formatter:off
		and.then(result)
			.as("Streams can be serialized, for caches that need to")
			.isEqualTo(streams)
			;
		// @formatter:on
	}

	@Test
	public void streamInfo_mapRow() throws SQLException {
		// GIVEN
		given(rs.getInt(1)).willReturn(1);
		given(rs.getString(2)).willReturn("a");
		given(rs.getLong(3)).willReturn(123L);
		given(rs.wasNull()).willReturn(false);

		// WHEN
		DatumStreamInfo result = DatumStreamInfoRowMapper.INSTANCE.mapRow(rs, 1);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Stream info mapped")
			.isEqualTo(new DatumStreamInfo(1, "a", 123L))
			;
		// @formatter:on
	}

	@Test
	public void streamInfo_mapRow_nodeStream() throws SQLException {
		// GIVEN
		given(rs.getInt(1)).willReturn(2);
		given(rs.getString(2)).willReturn("b");
		given(rs.getLong(3)).willReturn(0L);
		given(rs.wasNull()).willReturn(true);

		// WHEN
		DatumStreamInfo result = DatumStreamInfoRowMapper.INSTANCE.mapRow(rs, 1);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Node stream info mapped without location ID")
			.isEqualTo(new DatumStreamInfo(2, "b", null))
			;
		// @formatter:on
	}

	@Test
	public void streamInfo_requiresSourceId() {
		thenIllegalArgumentException().isThrownBy(() -> new DatumStreamInfo(1, null, null));
	}

	@Test
	public void storedDatum_assignable() {
		// GIVEN
		StoredDatum d = new StoredDatum(1, "a", null, Instant.now(), "foo.Bar",
				Set.of("foo.Bar", AcEnergyDatum.class.getName(), NodeDatum.class.getName()),
				new DatumSamples());

		// THEN
		// @formatter:off
		and.then(d.isAssignableTo(null)).as("Assignable to any type").isTrue();
		and.then(d.isAssignableTo(NodeDatum.class)).as("Assignable to stored type").isTrue();
		and.then(d.isAssignableTo(AcEnergyDatum.class)).as("Assignable to stored type").isTrue();
		and.then(d.isAssignableTo(AtmosphericDatum.class)).as("Not assignable to other type").isFalse();
		// @formatter:on
	}

	@Test
	public void storedDatum_datumId() {
		// GIVEN
		final Instant ts = Instant.now();
		StoredDatum node = new StoredDatum(1, "a", null, ts, "foo.Bar", Set.of("foo.Bar"),
				new DatumSamples());
		StoredDatum loc = new StoredDatum(1, "a", 123L, ts, "foo.Bar", Set.of("foo.Bar"),
				new DatumSamples());

		// THEN
		// @formatter:off
		and.then(node.datumId())
			.as("Node datum ID")
			.isEqualTo(DatumId.nodeId(null, "a", ts))
			.returns(ObjectDatumKind.Node, DatumId::getKind)
			;
		and.then(loc.datumId())
			.as("Location datum ID")
			.isEqualTo(DatumId.locationId(123L, "a", ts))
			;
		// @formatter:on
	}

	@Test
	public void storedDatum_requiredArguments() {
		final Instant ts = Instant.now();
		final DatumSamples s = new DatumSamples();
		final Set<String> t = Set.of("foo.Bar");
		thenIllegalArgumentException().isThrownBy(() -> new StoredDatum(1, null, null, ts, "x", t, s));
		thenIllegalArgumentException().isThrownBy(() -> new StoredDatum(1, "a", null, null, "x", t, s));
		thenIllegalArgumentException().isThrownBy(() -> new StoredDatum(1, "a", null, ts, null, t, s));
		thenIllegalArgumentException().isThrownBy(() -> new StoredDatum(1, "a", null, ts, "x", null, s));
		thenIllegalArgumentException().isThrownBy(() -> new StoredDatum(1, "a", null, ts, "x", t, null));
	}

}
