/* ==================================================================
 * DefaultDatumServiceTests.java - 18/08/2021 10:48:38 AM
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

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static net.solarnetwork.node.service.DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED;
import static net.solarnetwork.node.service.DatumEvents.datumEvent;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.runtime.DefaultDatumService;

/**
 * Test cases for the {@link DefaultDatumService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DefaultDatumServiceTests {

	private DefaultDatumService service;

	private MultiValueMap<String, NodeDatum> population;

	@Before
	public void setup() {
		service = new DefaultDatumService(new AntPathMatcher(), new ObjectMapper());
	}

	private Map<String, NodeDatum> populateDatum() {
		return populateDatum(5);
	}

	private Map<String, NodeDatum> populateDatum(final int count) {
		population = new LinkedMultiValueMap<>();
		Map<String, NodeDatum> all = new LinkedHashMap<>();
		for ( int i = 0; i < count; i++ ) {
			for ( int j = 0; j < 11; j++ ) {
				SimpleDatum datum = SimpleDatum.nodeDatum(String.format("test.%d", j), Instant.now(),
						new DatumSamples());
				all.put(datum.getSourceId(), datum);
				population.add(datum.getSourceId(), datum);

				Event evt = datumEvent(EVENT_TOPIC_DATUM_CAPTURED, datum);
				service.handleEvent(evt);
			}
		}
		return all;
	}

	@Test
	public void latest_all() {
		// GIVEN
		Map<String, NodeDatum> all = populateDatum();

		// WHEN
		List<NodeDatum> latest = StreamSupport
				.stream(service.latest(emptySet(), NodeDatum.class).spliterator(), false)
				.collect(Collectors.toList());

		// THEN
		Datum[] expected = all.values().toArray(new Datum[all.values().size()]);
		assertThat("Latest datum returned", latest, containsInAnyOrder(expected));
	}

	@Test
	public void latest_filter_oneSource() {
		// GIVEN
		Map<String, NodeDatum> all = populateDatum();

		// WHEN
		String sourceId = "test.2";
		List<NodeDatum> latest = StreamSupport
				.stream(service.latest(singleton(sourceId), NodeDatum.class).spliterator(), false)
				.collect(Collectors.toList());

		// THEN
		assertThat("Latest datum returned for filtered source ID", latest,
				containsInAnyOrder(all.get(sourceId)));
	}

	@Test
	public void latest_filter_multiSource() {
		// GIVEN
		Map<String, NodeDatum> all = populateDatum();

		// WHEN
		Set<String> sourceIds = new LinkedHashSet<>(Arrays.asList("test.1", "test.3", "test.5"));
		List<NodeDatum> latest = StreamSupport
				.stream(service.latest(sourceIds, NodeDatum.class).spliterator(), false)
				.collect(Collectors.toList());

		// THEN
		assertThat("Latest datum returned for filtered source ID", latest,
				containsInAnyOrder(all.get("test.1"), all.get("test.3"), all.get("test.5")));
	}

	@Test
	public void latest_filter_pattern() {
		// GIVEN
		Map<String, NodeDatum> all = populateDatum();

		// WHEN
		List<NodeDatum> latest = StreamSupport
				.stream(service.latest(singleton("test.1*"), NodeDatum.class).spliterator(), false)
				.collect(Collectors.toList());

		// THEN
		assertThat("Latest datum returned", latest,
				containsInAnyOrder(all.get("test.1"), all.get("test.10")));
	}

	@Test
	public void offset_all() {
		// GIVEN
		final int count = 8;
		populateDatum(count);

		// WHEN
		List<NodeDatum> result = StreamSupport
				.stream(service.offset(emptySet(), 1, NodeDatum.class).spliterator(), false)
				.collect(Collectors.toList());

		// THEN
		Datum[] expected = population.values().stream().map(l -> l.get(count - 2)).toArray(Datum[]::new);
		assertThat("Offset datum returned", result, containsInAnyOrder(expected));
	}

	@Test
	public void offset_notAvailable() {
		// GIVEN
		final int count = 8;
		populateDatum(count);

		// WHEN
		List<NodeDatum> result = StreamSupport
				.stream(service.offset(emptySet(), 8, NodeDatum.class).spliterator(), false)
				.collect(Collectors.toList());

		// THEN
		assertThat("No offset datum available so far back", result, hasSize(0));
	}

}
