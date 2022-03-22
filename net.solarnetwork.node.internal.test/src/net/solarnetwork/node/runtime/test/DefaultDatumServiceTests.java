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

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumMetadataOperations;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.runtime.DefaultDatumService;
import net.solarnetwork.node.service.DatumMetadataService;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link DefaultDatumService} class.
 * 
 * @author matt
 * @version 1.1
 */
public class DefaultDatumServiceTests {

	private DefaultDatumService service;
	private DatumMetadataService datumMetadataService;

	private MultiValueMap<String, NodeDatum> population;

	@Before
	public void setup() {
		datumMetadataService = EasyMock.createMock(DatumMetadataService.class);
		service = new DefaultDatumService(new AntPathMatcher(), new ObjectMapper(),
				new StaticOptionalService<>(datumMetadataService));
	}

	@After
	public void teardown() {
		EasyMock.verify(datumMetadataService);
	}

	private void replayAll() {
		EasyMock.replay(datumMetadataService);
	}

	private Map<String, NodeDatum> populateDatum() {
		return populateDatum(5);
	}

	private Map<String, NodeDatum> populateDatum(final int count) {
		return populateDatum(count, "test.%d");
	}

	private Map<String, NodeDatum> populateDatum(final int count, String sourceIdTemplate) {
		population = new LinkedMultiValueMap<>();
		Map<String, NodeDatum> all = new LinkedHashMap<>();
		for ( int i = 0; i < count; i++ ) {
			for ( int j = 0; j < 11; j++ ) {
				SimpleDatum datum = SimpleDatum.nodeDatum(String.format(sourceIdTemplate, j),
						Instant.now(), new DatumSamples());
				all.put(datum.getSourceId(), datum);
				population.add(datum.getSourceId(), datum);

				service.accept(datum);
			}
		}
		return all;
	}

	@Test
	public void latest_all() {
		// GIVEN
		Map<String, NodeDatum> all = populateDatum();

		// WHEN
		replayAll();
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
		replayAll();
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
		replayAll();
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
		replayAll();
		List<NodeDatum> latest = StreamSupport
				.stream(service.latest(singleton("test.1*"), NodeDatum.class).spliterator(), false)
				.collect(Collectors.toList());

		// THEN
		assertThat("Latest datum returned", latest,
				containsInAnyOrder(all.get("test.1"), all.get("test.10")));
	}

	@Test
	public void latest_filter_pattern_2() {
		// GIVEN
		Map<String, NodeDatum> all = populateDatum(5, "foo/bar/charger/%d/111111111");

		// add another that does not match pattern
		SimpleDatum outlier = SimpleDatum.nodeDatum("foo/bar/charger/1/111111111/MAX", Instant.now(),
				new DatumSamples());
		service.accept(outlier);

		// WHEN
		replayAll();
		List<NodeDatum> latest = StreamSupport
				.stream(service.latest(singleton("**/charger/*/*"), NodeDatum.class).spliterator(),
						false)
				.collect(Collectors.toList());

		// THEN
		assertThat("Latest datum returned", new HashSet<>(latest), is(new HashSet<>(all.values())));
	}

	@Test
	public void offset_all() {
		// GIVEN
		final int count = 8;
		populateDatum(count);

		// WHEN
		replayAll();
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
		replayAll();
		List<NodeDatum> result = StreamSupport
				.stream(service.offset(emptySet(), 8, NodeDatum.class).spliterator(), false)
				.collect(Collectors.toList());

		// THEN
		assertThat("No offset datum available so far back", result, hasSize(0));
	}

	@Test
	public void metaForSource_missing() {
		// GIVEN
		final String sourceId = "test.source";

		expect(datumMetadataService.getSourceMetadata(sourceId)).andReturn(null);

		// WHEN
		replayAll();
		DatumMetadataOperations result = service.datumMetadata(sourceId);

		assertThat("null returned when DatumMetadataService returns null", result, is(nullValue()));
	}

	@Test
	public void metaForSource_found() {
		// GIVEN
		final String sourceId = "test.source";

		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		expect(datumMetadataService.getSourceMetadata(sourceId)).andReturn(meta);

		// WHEN
		replayAll();
		DatumMetadataOperations result = service.datumMetadata(sourceId);

		assertThat("Same instance returned as from DatumMetadataService", result,
				is(sameInstance(meta)));
	}

	@Test
	public void metaForSourcePattern_noAvailableSourceIds() {
		// GIVEN
		final String sourceIdPat = "test*";
		expect(datumMetadataService.availableSourceMetadataSourceIds())
				.andReturn(Collections.emptySet());

		// WHEN
		replayAll();
		Collection<DatumMetadataOperations> result = service.datumMetadata(singleton(sourceIdPat));

		// THEN
		assertThat("Empty result returned when no source IDs available", result, hasSize(0));
	}

	@Test
	public void metaForSourcePattern_noMatchingSourceIds() {
		// GIVEN
		final String sourceIdPat = "test*";

		Set<String> availSourceIds = new HashSet<>(Arrays.asList("foo", "bar"));
		expect(datumMetadataService.availableSourceMetadataSourceIds()).andReturn(availSourceIds);

		// WHEN
		replayAll();
		Collection<DatumMetadataOperations> result = service.datumMetadata(singleton(sourceIdPat));

		// THEN
		assertThat("Empty result returned when no source IDs match", result, hasSize(0));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void metaForSourcePattern_matchingSourceIds() {
		// GIVEN
		final String sourceIdPat = "**/bam/**";

		Set<String> availSourceIds = new HashSet<>(
				asList("foo/bar/bam/1/11111111", "foo/bar/bam/2/22222222", "bambi", "bar"));
		expect(datumMetadataService.availableSourceMetadataSourceIds()).andReturn(availSourceIds);

		GeneralDatumMetadata meta1 = new GeneralDatumMetadata();
		expect(datumMetadataService.getSourceMetadata("foo/bar/bam/1/11111111")).andReturn(meta1);
		GeneralDatumMetadata meta2 = new GeneralDatumMetadata();
		expect(datumMetadataService.getSourceMetadata("foo/bar/bam/2/22222222")).andReturn(meta2);

		// WHEN
		replayAll();
		Collection<DatumMetadataOperations> result = service.datumMetadata(singleton(sourceIdPat));

		// THEN
		assertThat("Matches returned when source IDs match", result,
				containsInAnyOrder(sameInstance(meta1), sameInstance(meta2)));
	}
}
