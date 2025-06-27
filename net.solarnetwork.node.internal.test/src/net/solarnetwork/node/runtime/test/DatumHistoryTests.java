/* ==================================================================
 * DatumHistoryTests.java - 18/08/2021 9:59:37 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.runtime.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.fail;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.runtime.DatumHistory;

/**
 * Test cases for the {@link DatumHistory} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumHistoryTests {

	private final DatumHistory.Configuration TINY_CONFIG = new DatumHistory.Configuration(3);

	private ConcurrentMap<String, Queue<NodeDatum>> raw;

	@Before
	public void setup() {
		raw = new ConcurrentHashMap<>();
	}

	@Test
	public void add() {
		// GIVEN
		DatumHistory h = new DatumHistory(TINY_CONFIG, raw);

		// WHEN
		SimpleDatum datum = SimpleDatum.nodeDatum("test", Instant.now(), new DatumSamples());
		h.add(datum);

		// THEN
		assertThat("Raw map added queue for datum source ID", raw.keySet(),
				contains(datum.getSourceId()));
		assertThat("Raw map queue created for datum", raw.get(datum.getSourceId()), contains(datum));
	}

	@Test
	public void add_wrapAround() {
		// GIVEN
		DatumHistory h = new DatumHistory(TINY_CONFIG, raw);

		// WHEN
		String sourceId = "test";
		List<NodeDatum> all = new ArrayList<>();
		for ( int i = 0; i < 4; i++ ) {
			SimpleDatum datum = SimpleDatum.nodeDatum(sourceId, Instant.now(), new DatumSamples());
			all.add(datum);
			h.add(datum);
		}

		// THEN
		assertThat("Raw map added queue for datum source ID", raw.keySet(), contains(sourceId));
		assertThat("Raw map queue contains only 3 latest", raw.get(sourceId),
				contains(all.get(1), all.get(2), all.get(3)));
	}

	@Test
	public void add_multipleSources() {
		// GIVEN
		DatumHistory h = new DatumHistory(TINY_CONFIG, raw);

		// WHEN
		Map<String, Datum> all = new LinkedHashMap<>();
		for ( int i = 0; i < 4; i++ ) {
			SimpleDatum datum = SimpleDatum.nodeDatum(String.format("test.%d", i), Instant.now(),
					new DatumSamples());
			all.put(datum.getSourceId(), datum);
			h.add(datum);
		}

		// THEN
		assertThat("Raw map added queue for each datum source ID", raw.keySet(),
				containsInAnyOrder(all.keySet().toArray(new String[all.size()])));
		for ( Entry<String, Datum> e : all.entrySet() ) {
			assertThat(String.format("Raw map queue %s contains datum", e.getKey()), raw.get(e.getKey()),
					contains(e.getValue()));
		}
	}

	@Test
	public void latest() {
		// GIVEN
		DatumHistory h = new DatumHistory(TINY_CONFIG, raw);

		Map<String, Datum> all = new LinkedHashMap<>();
		for ( int i = 0; i < 4; i++ ) {
			for ( int j = 0; j < 4; j++ ) {
				SimpleDatum datum = SimpleDatum.nodeDatum(String.format("test.%d", i), Instant.now(),
						new DatumSamples());
				all.put(datum.getSourceId(), datum);
				h.add(datum);
			}
		}

		// WHEN
		List<NodeDatum> latest = StreamSupport.stream(h.latest().spliterator(), false)
				.collect(Collectors.toList());

		// THEN
		assertThat("Raw map added queue for each datum source ID", raw.keySet(),
				containsInAnyOrder(all.keySet().toArray(new String[all.size()])));
		NodeDatum[] expected = all.values().toArray(new NodeDatum[all.values().size()]);
		assertThat("Latest datum returned", latest, containsInAnyOrder(expected));
	}

	@Test
	public void latest_source() {
		// GIVEN
		DatumHistory h = new DatumHistory(TINY_CONFIG, raw);

		Map<String, NodeDatum> all = new LinkedHashMap<>();
		for ( int i = 0; i < 4; i++ ) {
			for ( int j = 0; j < 4; j++ ) {
				SimpleDatum datum = SimpleDatum.nodeDatum(String.format("test.%d", i), Instant.now(),
						new DatumSamples());
				all.put(datum.getSourceId(), datum);
				h.add(datum);
			}
		}

		// WHEN
		NodeDatum latest = h.latest("test.0");

		// THEN
		NodeDatum expected = all.get("test.0");
		assertThat("Latest datum returned", latest, is(sameInstance(expected)));
	}

	@Test
	public void offsetTime_latest_exact() {
		// GIVEN
		DatumHistory h = new DatumHistory(TINY_CONFIG, raw);

		Instant start = Instant.now().truncatedTo(ChronoUnit.HOURS);
		for ( int j = 0; j < 4; j++ ) {
			SimpleDatum datum = SimpleDatum.nodeDatum(String.format("test.%d", 0), start.plusSeconds(j),
					new DatumSamples());
			h.add(datum);
		}

		// WHEN
		NodeDatum offset = h.offset("test.0", start.plusSeconds(3), 0);

		// THEN
		assertThat("Latest datum returned", offset.getTimestamp(), is(start.plusSeconds(3)));
	}

	@Test
	public void offsetTime_latest_lessThan() {
		// GIVEN
		DatumHistory h = new DatumHistory(TINY_CONFIG, raw);

		Instant start = Instant.now().truncatedTo(ChronoUnit.HOURS);
		for ( int j = 0; j < 4; j++ ) {
			SimpleDatum datum = SimpleDatum.nodeDatum(String.format("test.%d", 0), start.plusSeconds(j),
					new DatumSamples());
			h.add(datum);
		}

		// WHEN
		NodeDatum offset = h.offset("test.0", start.plusSeconds(4), 0);

		// THEN
		assertThat("Offset datum returned", offset.getTimestamp(), is(start.plusSeconds(3)));
	}

	@Test
	public void offsetTime_noneEarlier() {
		// GIVEN
		DatumHistory h = new DatumHistory(TINY_CONFIG, raw);

		Instant start = Instant.now().truncatedTo(ChronoUnit.HOURS);
		for ( int j = 0; j < 4; j++ ) {
			SimpleDatum datum = SimpleDatum.nodeDatum(String.format("test.%d", 0), start.plusSeconds(j),
					new DatumSamples());
			h.add(datum);
		}

		// WHEN
		NodeDatum offset = h.offset("test.0", start.minusSeconds(1), 0);

		// THEN
		assertThat("Offset datum not available", offset, is(nullValue()));
	}

	@Test
	public void offsetTime_offsetTooFar() {
		// GIVEN
		DatumHistory h = new DatumHistory(TINY_CONFIG, raw);

		Instant start = Instant.now().truncatedTo(ChronoUnit.HOURS);
		for ( int j = 0; j < 4; j++ ) {
			SimpleDatum datum = SimpleDatum.nodeDatum(String.format("test.%d", 0), start.plusSeconds(j),
					new DatumSamples());
			h.add(datum);
		}

		// WHEN
		NodeDatum offset = h.offset("test.0", start.plusSeconds(3), 10);

		// THEN
		assertThat("Offset datum not available", offset, is(nullValue()));
	}

	@Test
	public void offsetTime_all() {
		// GIVEN
		DatumHistory h = new DatumHistory(TINY_CONFIG, raw);

		Instant start = Instant.now().truncatedTo(ChronoUnit.HOURS);
		for ( int i = 0; i < 4; i++ ) {
			for ( int j = 0; j < 4; j++ ) {
				SimpleDatum datum = SimpleDatum.nodeDatum(String.format("test.%d", i),
						start.plusMillis(i * 200 + j * 100), new DatumSamples());
				h.add(datum);
			}
		}

		// WHEN
		Iterable<NodeDatum> offsets = h.offset(start.plusMillis(450), 0);

		// THEN
		for ( NodeDatum d : offsets ) {
			final String sourceId = d.getSourceId();
			switch (sourceId) {
				case "test.0":
					assertThat("Source 0 timestamp", d.getTimestamp(),
							is(start.plusMillis(0 * 200 + 3 * 100)));
					break;

				case "test.1":
					assertThat("Source 1 timestamp", d.getTimestamp(),
							is(start.plusMillis(1 * 200 + 2 * 100)));
					break;

				case "test.2":
					assertThat("Source 2 timestamp", d.getTimestamp(),
							is(start.plusMillis(2 * 200 + 0 * 100)));
					break;

				default:
					fail("Unknown source: " + sourceId);
			}
		}
	}

	@Test
	public void slice_head() {
		// GIVEN
		DatumHistory h = new DatumHistory(TINY_CONFIG, raw);

		String sourceId = "test";
		Instant start = Instant.now().truncatedTo(ChronoUnit.HOURS);
		final int captureCount = 3;
		for ( int i = 0; i < captureCount; i++ ) {
			SimpleDatum datum = SimpleDatum.nodeDatum(sourceId, start.plusSeconds(i),
					new DatumSamples());
			h.add(datum);
		}

		// WHEN
		final int count = 2;
		Iterable<NodeDatum> slice = h.slice(sourceId, 0, count);

		// THEN
		List<NodeDatum> datum = StreamSupport.stream(slice.spliterator(), false)
				.collect(Collectors.toList());
		assertThat("Expected count", datum.size(), is(equalTo(count)));
		for ( int i = 0; i < count; i++ ) {
			assertThat(String.format("Expected datum %d returned", i), datum.get(i).getTimestamp(),
					is(equalTo(start.plusSeconds(captureCount - count + i))));
		}
	}

	@Test
	public void slice_head_countLimitedByCapture() {
		// GIVEN
		DatumHistory h = new DatumHistory(TINY_CONFIG, raw);

		String sourceId = "test";
		Instant start = Instant.now().truncatedTo(ChronoUnit.HOURS);
		final int captureCount = 2;
		for ( int i = 0; i < captureCount; i++ ) {
			SimpleDatum datum = SimpleDatum.nodeDatum(sourceId, start.plusSeconds(i),
					new DatumSamples());
			h.add(datum);
		}

		// WHEN
		//final int count = 2;
		Iterable<NodeDatum> slice = h.slice(sourceId, 0, 5);

		// THEN
		List<NodeDatum> datum = StreamSupport.stream(slice.spliterator(), false)
				.collect(Collectors.toList());
		assertThat("Expected count limited by capture count", datum.size(), is(equalTo(captureCount)));
		for ( int i = 0; i < captureCount; i++ ) {
			assertThat(String.format("Expected datum %d returned", i), datum.get(i).getTimestamp(),
					is(equalTo(start.plusSeconds(i))));
		}
	}

	@Test
	public void slice_head_countLimitedByConfig() {
		// GIVEN
		DatumHistory h = new DatumHistory(TINY_CONFIG, raw);

		String sourceId = "test";
		Instant start = Instant.now().truncatedTo(ChronoUnit.HOURS);
		final int captureCount = 4;
		for ( int i = 0; i < captureCount; i++ ) {
			SimpleDatum datum = SimpleDatum.nodeDatum(sourceId, start.plusSeconds(i),
					new DatumSamples());
			h.add(datum);
		}

		// WHEN
		Iterable<NodeDatum> slice = h.slice(sourceId, 0, 5);

		// THEN
		List<NodeDatum> datum = StreamSupport.stream(slice.spliterator(), false)
				.collect(Collectors.toList());
		assertThat("Expected count limited by config count", datum.size(),
				is(equalTo(TINY_CONFIG.getRawCount())));
		for ( int i = 0; i < TINY_CONFIG.getRawCount(); i++ ) {
			assertThat(String.format("Expected datum %d returned", i), datum.get(i).getTimestamp(),
					is(equalTo(start.plusSeconds(i + (captureCount - TINY_CONFIG.getRawCount())))));
		}
	}

	@Test
	public void slice_offset() {
		// GIVEN
		DatumHistory h = new DatumHistory(TINY_CONFIG, raw);

		String sourceId = "test";
		Instant start = Instant.now().truncatedTo(ChronoUnit.HOURS);
		final int captureCount = 3;
		for ( int i = 0; i < captureCount; i++ ) {
			SimpleDatum datum = SimpleDatum.nodeDatum(sourceId, start.plusSeconds(i),
					new DatumSamples());
			h.add(datum);
		}

		// WHEN
		final int offset = 1;
		final int count = 2;
		Iterable<NodeDatum> slice = h.slice(sourceId, offset, count);

		// THEN
		List<NodeDatum> datum = StreamSupport.stream(slice.spliterator(), false)
				.collect(Collectors.toList());
		assertThat("Expected count", datum.size(), is(equalTo(count)));
		for ( int i = 0; i < count; i++ ) {
			assertThat(String.format("Expected datum %d returned", i), datum.get(i).getTimestamp(),
					is(equalTo(start.plusSeconds(captureCount - offset - count + i))));
		}
	}

	@Test
	public void slice_offsetLimitedByCapture() {
		// GIVEN
		DatumHistory h = new DatumHistory(TINY_CONFIG, raw);

		String sourceId = "test";
		Instant start = Instant.now().truncatedTo(ChronoUnit.HOURS);
		final int captureCount = 2;
		for ( int i = 0; i < captureCount; i++ ) {
			SimpleDatum datum = SimpleDatum.nodeDatum(sourceId, start.plusSeconds(i),
					new DatumSamples());
			h.add(datum);
		}

		// WHEN
		final int offset = 1;
		Iterable<NodeDatum> slice = h.slice(sourceId, offset, 10);

		// THEN
		List<NodeDatum> datum = StreamSupport.stream(slice.spliterator(), false)
				.collect(Collectors.toList());
		assertThat("Expected count", datum.size(), is(equalTo(1)));
		assertThat("Expected datum returned", datum.get(0).getTimestamp(),
				is(equalTo(start.plusSeconds(captureCount - offset - 1))));
	}

	@Test
	public void slice_offsetLimitedByConfig() {
		// GIVEN
		DatumHistory h = new DatumHistory(TINY_CONFIG, raw);

		String sourceId = "test";
		Instant start = Instant.now().truncatedTo(ChronoUnit.HOURS);
		final int captureCount = TINY_CONFIG.getRawCount() + 1;
		for ( int i = 0; i < captureCount; i++ ) {
			SimpleDatum datum = SimpleDatum.nodeDatum(sourceId, start.plusSeconds(i),
					new DatumSamples());
			h.add(datum);
		}

		// WHEN
		final int offset = 1;
		Iterable<NodeDatum> slice = h.slice(sourceId, offset, 10);

		// THEN
		List<NodeDatum> datum = StreamSupport.stream(slice.spliterator(), false)
				.collect(Collectors.toList());
		assertThat("Expected count limited by config count", datum.size(),
				is(equalTo(TINY_CONFIG.getRawCount() - offset)));
		for ( int i = 0; i < TINY_CONFIG.getRawCount() - offset; i++ ) {
			assertThat(String.format("Expected datum %d returned", i), datum.get(i).getTimestamp(),
					is(equalTo(start
							.plusSeconds(i + (captureCount - offset - TINY_CONFIG.getRawCount() + 1)))));
		}
	}

	@Test
	public void slice_offsetTime_head() {
		// GIVEN
		DatumHistory h = new DatumHistory(TINY_CONFIG, raw);

		String sourceId = "test";
		Instant start = Instant.now().truncatedTo(ChronoUnit.HOURS);
		final int captureCount = 3;
		for ( int i = 0; i < captureCount; i++ ) {
			SimpleDatum datum = SimpleDatum.nodeDatum(sourceId, start.plusSeconds(i),
					new DatumSamples());
			h.add(datum);
		}

		// WHEN
		final int count = 2;
		Iterable<NodeDatum> slice = h.slice(sourceId, start.plusSeconds(captureCount - 1), 0, count);

		// THEN
		List<NodeDatum> datum = StreamSupport.stream(slice.spliterator(), false)
				.collect(Collectors.toList());
		assertThat("Expected count", datum.size(), is(equalTo(count)));
		for ( int i = 0; i < count; i++ ) {
			assertThat(String.format("Expected datum %d returned", i), datum.get(i).getTimestamp(),
					is(equalTo(start.plusSeconds(captureCount - count + i))));
		}
	}

	@Test
	public void slice_offsetTime_offset() {
		// GIVEN
		DatumHistory h = new DatumHistory(TINY_CONFIG, raw);

		String sourceId = "test";
		Instant start = Instant.now().truncatedTo(ChronoUnit.HOURS);
		final int captureCount = 3;
		for ( int i = 0; i < captureCount; i++ ) {
			SimpleDatum datum = SimpleDatum.nodeDatum(sourceId, start.plusSeconds(i),
					new DatumSamples());
			h.add(datum);
		}

		// WHEN
		final int count = 2;
		Iterable<NodeDatum> slice = h.slice(sourceId, start.plusSeconds(1), 0, count);

		// THEN
		List<NodeDatum> datum = StreamSupport.stream(slice.spliterator(), false)
				.collect(Collectors.toList());
		assertThat("Expected count", datum.size(), is(equalTo(count)));
		for ( int i = 0; i < count; i++ ) {
			assertThat(String.format("Expected datum %d returned", i), datum.get(i).getTimestamp(),
					is(equalTo(start.plusSeconds(i))));
		}
	}

	@Test
	public void slice_offsetTime_offset_offset() {
		// GIVEN
		DatumHistory h = new DatumHistory(TINY_CONFIG, raw);

		String sourceId = "test";
		Instant start = Instant.now().truncatedTo(ChronoUnit.HOURS);
		final int captureCount = 3;
		for ( int i = 0; i < captureCount; i++ ) {
			SimpleDatum datum = SimpleDatum.nodeDatum(sourceId, start.plusSeconds(i),
					new DatumSamples());
			h.add(datum);
		}

		// WHEN
		final int count = 2;
		Iterable<NodeDatum> slice = h.slice(sourceId, start.plusSeconds(1), 1, count);

		// THEN
		List<NodeDatum> datum = StreamSupport.stream(slice.spliterator(), false)
				.collect(Collectors.toList());
		assertThat("Expected count", datum.size(), is(equalTo(1)));
		assertThat("Expected datum returned", datum.get(0).getTimestamp(), is(equalTo(start)));
	}

	@Test
	public void slice_offsetTime_offsetTooFar_offset() {
		// GIVEN
		DatumHistory h = new DatumHistory(TINY_CONFIG, raw);

		String sourceId = "test";
		Instant start = Instant.now().truncatedTo(ChronoUnit.HOURS);
		final int captureCount = 3;
		for ( int i = 0; i < captureCount; i++ ) {
			SimpleDatum datum = SimpleDatum.nodeDatum(sourceId, start.plusSeconds(i),
					new DatumSamples());
			h.add(datum);
		}

		// WHEN
		final int count = 2;
		Iterable<NodeDatum> slice = h.slice(sourceId, start.minusSeconds(1), 0, count);

		// THEN
		List<NodeDatum> datum = StreamSupport.stream(slice.spliterator(), false)
				.collect(Collectors.toList());
		assertThat("Expected count", datum.size(), is(equalTo(0)));
	}

	@Test
	public void slice_offsetTime_offset_offsetTooFar() {
		// GIVEN
		DatumHistory h = new DatumHistory(TINY_CONFIG, raw);

		String sourceId = "test";
		Instant start = Instant.now().truncatedTo(ChronoUnit.HOURS);
		final int captureCount = 3;
		for ( int i = 0; i < captureCount; i++ ) {
			SimpleDatum datum = SimpleDatum.nodeDatum(sourceId, start.plusSeconds(i),
					new DatumSamples());
			h.add(datum);
		}

		// WHEN
		final int count = 2;
		Iterable<NodeDatum> slice = h.slice(sourceId, start.plusSeconds(1), 2, count);

		// THEN
		List<NodeDatum> datum = StreamSupport.stream(slice.spliterator(), false)
				.collect(Collectors.toList());
		assertThat("Expected count", datum.size(), is(equalTo(0)));
	}

}
