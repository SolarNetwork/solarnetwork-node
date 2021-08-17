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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.Date;
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
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.runtime.DatumHistory;

/**
 * Test cases for the {@link DatumHistory} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumHistoryTests {

	private final DatumHistory.Configuration TINY_CONFIG = new DatumHistory.Configuration(3);

	private ConcurrentMap<String, Queue<Datum>> raw;

	@Before
	public void setup() {
		raw = new ConcurrentHashMap<>();
	}

	@Test
	public void add() {
		// GIVEN
		DatumHistory h = new DatumHistory(TINY_CONFIG, raw);

		// WHEN
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setSourceId("test");
		datum.setCreated(new Date());
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
		List<Datum> all = new ArrayList<>();
		for ( int i = 0; i < 4; i++ ) {
			GeneralNodeDatum datum = new GeneralNodeDatum();
			datum.setSourceId(sourceId);
			datum.setCreated(new Date());
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
			GeneralNodeDatum datum = new GeneralNodeDatum();
			datum.setSourceId(String.format("test.%d", i));
			datum.setCreated(new Date());
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

		// WHEN
		Map<String, Datum> all = new LinkedHashMap<>();
		for ( int i = 0; i < 4; i++ ) {
			for ( int j = 0; j < 4; j++ ) {
				GeneralNodeDatum datum = new GeneralNodeDatum();
				datum.setSourceId(String.format("test.%d", j));
				datum.setCreated(new Date());
				all.put(datum.getSourceId(), datum);
				h.add(datum);
			}
		}

		// THEN
		assertThat("Raw map added queue for each datum source ID", raw.keySet(),
				containsInAnyOrder(all.keySet().toArray(new String[all.size()])));
		List<Datum> latest = StreamSupport.stream(h.latest().spliterator(), false)
				.collect(Collectors.toList());
		Datum[] expected = all.values().toArray(new Datum[all.values().size()]);
		assertThat("Latest datum returned", latest, containsInAnyOrder(expected));
	}

}
