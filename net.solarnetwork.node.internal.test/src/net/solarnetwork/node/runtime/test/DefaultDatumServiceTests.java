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

import static java.util.Collections.singleton;
import static net.solarnetwork.node.DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED;
import static net.solarnetwork.node.support.DatumEvents.datumEvent;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import java.util.Arrays;
import java.util.Date;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.runtime.DefaultDatumService;

/**
 * Test cases for the {@link DefaultDatumService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DefaultDatumServiceTests {

	private DefaultDatumService service;

	@Before
	public void setup() {
		service = new DefaultDatumService(new AntPathMatcher(), new ObjectMapper());
	}

	private Map<String, Datum> populateDatum() {
		Map<String, Datum> all = new LinkedHashMap<>();
		for ( int i = 0; i < 4; i++ ) {
			for ( int j = 0; j < 11; j++ ) {
				GeneralNodeDatum datum = new GeneralNodeDatum();
				datum.setSourceId(String.format("test.%d", j));
				datum.setCreated(new Date());
				all.put(datum.getSourceId(), datum);

				Event evt = datumEvent(EVENT_TOPIC_DATUM_CAPTURED, datum);
				service.handleEvent(evt);
			}
		}
		return all;
	}

	@Test
	public void latest_all() {
		// GIVEN
		Map<String, Datum> all = populateDatum();

		// WHEN
		List<Datum> latest = StreamSupport.stream(service.latest(null, Datum.class).spliterator(), false)
				.collect(Collectors.toList());

		// THEN
		Datum[] expected = all.values().toArray(new Datum[all.values().size()]);
		assertThat("Latest datum returned", latest, containsInAnyOrder(expected));
	}

	@Test
	public void latest_filter_oneSource() {
		// GIVEN
		Map<String, Datum> all = populateDatum();

		// WHEN
		String sourceId = "test.2";
		List<Datum> latest = StreamSupport
				.stream(service.latest(singleton(sourceId), Datum.class).spliterator(), false)
				.collect(Collectors.toList());

		// THEN
		assertThat("Latest datum returned for filtered source ID", latest,
				containsInAnyOrder(all.get(sourceId)));
	}

	@Test
	public void latest_filter_multiSource() {
		// GIVEN
		Map<String, Datum> all = populateDatum();

		// WHEN
		Set<String> sourceIds = new LinkedHashSet<>(Arrays.asList("test.1", "test.3", "test.5"));
		List<Datum> latest = StreamSupport
				.stream(service.latest(sourceIds, Datum.class).spliterator(), false)
				.collect(Collectors.toList());

		// THEN
		assertThat("Latest datum returned for filtered source ID", latest,
				containsInAnyOrder(all.get("test.1"), all.get("test.3"), all.get("test.5")));
	}

	@Test
	public void latest_filter_pattern() {
		// GIVEN
		Map<String, Datum> all = populateDatum();

		// WHEN
		List<Datum> latest = StreamSupport
				.stream(service.latest(singleton("test.1*"), Datum.class).spliterator(), false)
				.collect(Collectors.toList());

		// THEN
		assertThat("Latest datum returned", latest,
				containsInAnyOrder(all.get("test.1"), all.get("test.10")));
	}

}