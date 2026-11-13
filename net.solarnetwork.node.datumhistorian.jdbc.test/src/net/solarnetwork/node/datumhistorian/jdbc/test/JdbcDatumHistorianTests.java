/* ==================================================================
 * JdbcDatumHistorianTests.java - 10 Nov 2026 10:44:20 pm
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

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static net.solarnetwork.node.datumhistorian.jdbc.JdbcDatumHistorian.UNFILTERED_SCHEMA_NAME;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.mockito.BDDMockito.given;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.node.datumhistorian.jdbc.DatumStreams;
import net.solarnetwork.node.datumhistorian.jdbc.JdbcDatumHistorian;
import net.solarnetwork.node.domain.datum.AcEnergyDatum;
import net.solarnetwork.node.domain.datum.AtmosphericDatum;
import net.solarnetwork.node.domain.datum.EnergyDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleAcEnergyDatum;
import net.solarnetwork.node.domain.datum.SimpleAtmosphericDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.domain.datum.SimpleEnergyDatum;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link JdbcDatumHistorian} class, using the Postgres test
 * database.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@RunWith(MockitoJUnitRunner.class)
public class JdbcDatumHistorianTests extends AbstractPostgresHistorianTests {

	private static final Long TEST_NODE_ID = 123L;
	private static final Long TEST_NODE_ID_2 = 234L;
	private static final Long TEST_LOC_ID = 345L;

	@Mock(strictness = Mock.Strictness.LENIENT)
	private IdentityService identityService;

	@Mock(strictness = Mock.Strictness.LENIENT)
	private IdentityService identityService2;

	private JdbcDatumHistorian historian;
	private JdbcDatumHistorian unfilteredHistorian;

	private Instant start;
	private MultiValueMap<String, NodeDatum> population;

	@Before
	public void setup() {
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		given(identityService2.getNodeId()).willReturn(TEST_NODE_ID_2);
		historian = new JdbcDatumHistorian(jdbcTemplate, new StaticOptionalService<>(identityService));
		unfilteredHistorian = new JdbcDatumHistorian(jdbcTemplate, UNFILTERED_SCHEMA_NAME,
				new StaticOptionalService<>(identityService));
		start = Instant.now().truncatedTo(ChronoUnit.MINUTES);
	}

	private static SimpleDatum datum(String sourceId, Instant timestamp) {
		return SimpleDatum.nodeDatum(sourceId, timestamp, new DatumSamples());
	}

	private static DatumSamples samples(String prop, Number value) {
		DatumSamples s = new DatumSamples();
		s.putInstantaneousSampleValue(prop, value);
		return s;
	}

	private Map<String, NodeDatum> populateDatum() {
		return populateDatum(historian, 5, "test.%d");
	}

	private Map<String, NodeDatum> populateDatum(final int count) {
		return populateDatum(historian, count, "test.%d");
	}

	private Map<String, NodeDatum> populateDatum(final int count, final String sourceIdTemplate) {
		return populateDatum(historian, count, sourceIdTemplate);
	}

	/**
	 * Add {@code count} datum to each of 11 sources, oldest to newest.
	 *
	 * <p>
	 * Unlike the in-memory historian, datum with the same source ID and
	 * timestamp replace each other, so each datum gets its own timestamp.
	 * </p>
	 *
	 * @return the latest datum of each source, by source ID
	 */
	private Map<String, NodeDatum> populateDatum(final JdbcDatumHistorian dest, final int count,
			final String sourceIdTemplate) {
		population = new LinkedMultiValueMap<>();
		Map<String, NodeDatum> all = new LinkedHashMap<>();
		for ( int i = 0; i < count; i++ ) {
			for ( int j = 0; j < 11; j++ ) {
				SimpleDatum datum = datum(String.format(sourceIdTemplate, j),
						start.plusSeconds(i).plusMillis(j));
				datum.getSamples().putInstantaneousSampleValue("i", i);
				all.put(datum.getSourceId(), datum);
				population.add(datum.getSourceId(), datum);
				dest.add(datum);
			}
		}
		return all;
	}

	/* ==================================================================
	 * Tests ported from DefaultDatumServiceTests
	 * ==================================================================
	 */

	@Test
	public void latest_all() {
		// GIVEN
		Map<String, NodeDatum> all = populateDatum();

		// WHEN
		Collection<NodeDatum> result = historian.latest(emptySet(), NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Latest datum for all sources returned")
			.containsExactlyInAnyOrderElementsOf(all.values())
			;
		// @formatter:on
	}

	@Test
	public void latest_all_unfiltered() {
		// GIVEN
		Map<String, NodeDatum> all = populateDatum(unfilteredHistorian, 5, "test.%d");

		// WHEN
		Collection<NodeDatum> latest = historian.latest(emptySet(), NodeDatum.class);
		Collection<NodeDatum> latestUnfiltered = unfilteredHistorian.latest(emptySet(), NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(latest)
			.as("Unfiltered datum not added to filtered history")
			.isEmpty()
			;
		and.then(latestUnfiltered)
			.as("Latest unfiltered datum for all sources returned")
			.containsExactlyInAnyOrderElementsOf(all.values())
			;
		// @formatter:on
	}

	@Test
	public void latest_filter_oneSource() {
		// GIVEN
		Map<String, NodeDatum> all = populateDatum();

		// WHEN
		final String sourceId = "test.2";
		Collection<NodeDatum> result = historian.latest(singleton(sourceId), NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Latest datum returned for filtered source ID")
			.containsExactly(all.get(sourceId))
			;
		// @formatter:on
	}

	@Test
	public void latest_filter_multiSource() {
		// GIVEN
		Map<String, NodeDatum> all = populateDatum();

		// WHEN
		Set<String> sourceIds = Set.of("test.1", "test.3", "test.5");
		Collection<NodeDatum> result = historian.latest(sourceIds, NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Latest datum returned for filtered source IDs")
			.containsExactlyInAnyOrder(all.get("test.1"), all.get("test.3"), all.get("test.5"))
			;
		// @formatter:on
	}

	@Test
	public void latest_filter_pattern() {
		// GIVEN
		Map<String, NodeDatum> all = populateDatum();

		// WHEN
		Collection<NodeDatum> result = historian.latest(singleton("test.1*"), NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Latest datum returned for source IDs matching pattern")
			.containsExactlyInAnyOrder(all.get("test.1"), all.get("test.10"))
			;
		// @formatter:on
	}

	@Test
	public void latest_filter_pattern_2() {
		// GIVEN
		Map<String, NodeDatum> all = populateDatum(5, "foo/bar/charger/%d/111111111");

		// add another that does not match pattern
		SimpleDatum outlier = datum("foo/bar/charger/1/111111111/MAX", start.plusSeconds(60));
		historian.add(outlier);

		// WHEN
		Collection<NodeDatum> result = historian.latest(singleton("**/charger/*/*"), NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Latest datum returned for source IDs matching pattern")
			.containsExactlyInAnyOrderElementsOf(all.values())
			;
		// @formatter:on
	}

	@Test
	public void latest_filter_pattern_3() {
		// GIVEN
		Map<String, NodeDatum> all = populateDatum(5, "/foo/bar/charger/%d");

		// add another that does not match pattern
		SimpleDatum outlier = datum("/foo/bar/other/1", start.plusSeconds(60));
		historian.add(outlier);

		// WHEN
		Collection<NodeDatum> result = historian.latest(singleton("/**/charger/*"), NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Latest datum returned for source IDs matching pattern")
			.containsExactlyInAnyOrderElementsOf(all.values())
			;
		// @formatter:on
	}

	@Test
	public void offset_all() {
		// GIVEN
		final int count = 8;
		populateDatum(count);

		// WHEN
		Collection<NodeDatum> result = historian.offset(emptySet(), 1, NodeDatum.class);

		// THEN
		List<NodeDatum> expected = population.values().stream().map(l -> l.get(count - 2)).toList();
		// @formatter:off
		and.then(result)
			.as("Offset datum for all sources returned")
			.containsExactlyInAnyOrderElementsOf(expected)
			;
		// @formatter:on
	}

	@Test
	public void offset_notAvailable() {
		// GIVEN
		final int count = 8;
		populateDatum(count);

		// WHEN
		Collection<NodeDatum> result = historian.offset(emptySet(), 8, NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("No offset datum available so far back")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void slice() {
		// GIVEN
		populateDatum();
		final String sourceId = "test.0";
		final List<NodeDatum> stream = population.get(sourceId);

		// WHEN
		Collection<NodeDatum> result = historian.slice(sourceId, 0, 3, NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Slice of latest 3 datum returned, oldest to newest")
			.containsExactlyElementsOf(stream.subList(2, 5))
			;
		// @formatter:on
	}

	@Test
	public void slice_timeOffset() {
		// GIVEN
		populateDatum();
		final String sourceId = "test.0";
		final List<NodeDatum> stream = population.get(sourceId);

		// WHEN
		Collection<NodeDatum> result = historian.slice(sourceId, stream.get(2).getTimestamp(), 0, 3,
				NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Slice of 3 datum up to timestamp returned, oldest to newest")
			.containsExactlyElementsOf(stream.subList(0, 3))
			;
		// @formatter:on
	}

	/* ==================================================================
	 * Source ID filters
	 * ==================================================================
	 */

	@Test
	public void latest_noData() {
		// WHEN
		Collection<NodeDatum> result = historian.latest(emptySet(), NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("No datum returned when nothing stored")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void latest_nullFilter() {
		// GIVEN
		Map<String, NodeDatum> all = populateDatum(2);

		// WHEN
		Collection<NodeDatum> result = historian.latest((Set<String>) null, NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Latest datum for all sources returned for null filter")
			.containsExactlyInAnyOrderElementsOf(all.values())
			;
		// @formatter:on
	}

	@Test
	public void latest_filter_noMatch() {
		// GIVEN
		populateDatum(2);

		// WHEN
		Collection<NodeDatum> result = historian.latest(singleton("nope/*"), NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("No datum returned when no source ID matches filter")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void latest_filter_caseInsensitive() {
		// GIVEN
		Map<String, NodeDatum> all = populateDatum(2, "Meter/%d");

		// WHEN
		Collection<NodeDatum> result = historian.latest(Set.of("METER/1", "meter/2*"), NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Source ID filters matched without case sensitivity, like DefaultDatumService")
			.containsExactlyInAnyOrder(all.get("Meter/1"), all.get("Meter/2"))
			;
		// @formatter:on
	}

	@Test
	public void latest_filter_overlappingPatterns() {
		// GIVEN
		Map<String, NodeDatum> all = populateDatum(2);

		// WHEN
		Collection<NodeDatum> result = historian.latest(Set.of("test.1", "test.1*", "*.1"),
				NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Each matching source returned once when matched by many patterns")
			.containsExactlyInAnyOrder(all.get("test.1"), all.get("test.10"))
			;
		// @formatter:on
	}

	@Test
	public void latest_filter_otherNode() {
		// GIVEN
		populateDatum(2);
		JdbcDatumHistorian otherNode = new JdbcDatumHistorian(jdbcTemplate,
				new StaticOptionalService<>(identityService2));
		SimpleDatum other = datum("test.1", start.plusSeconds(60));
		otherNode.add(other);

		// WHEN
		Collection<NodeDatum> result = otherNode.latest(emptySet(), NodeDatum.class);
		NodeDatum latest = historian.latest("test.1", NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Only datum of the node returned")
			.containsExactly(other)
			;
		and.then(latest)
			.as("Datum of other node not returned")
			.isEqualTo(population.get("test.1").get(1))
			;
		// @formatter:on
	}

	/* ==================================================================
	 * Single source queries
	 * ==================================================================
	 */

	@Test
	public void latest_source() {
		// GIVEN
		Map<String, NodeDatum> all = populateDatum(3);

		// WHEN
		NodeDatum result = historian.latest("test.4", NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Latest datum of source returned")
			.isEqualTo(all.get("test.4"))
			.as("Samples restored")
			.returns(new BigDecimal("2"), from(d -> d.asSampleOperations()
					.getSampleBigDecimal(DatumSamplesType.Instantaneous, "i")))
			;
		// @formatter:on
	}

	@Test
	public void latest_source_notFound() {
		// GIVEN
		populateDatum(1);

		// WHEN
		NodeDatum result = historian.latest("nope", NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("No datum returned for unknown source")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void latest_source_isNotPattern() {
		// GIVEN
		populateDatum(1);

		// WHEN
		NodeDatum result = historian.latest("test.*", NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Single source ID not treated as pattern")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void offset_source() {
		// GIVEN
		populateDatum(5);
		final List<NodeDatum> stream = population.get("test.3");

		// WHEN
		List<NodeDatum> result = new ArrayList<>();
		for ( int i = 0; i < 6; i++ ) {
			result.add(historian.offset("test.3", i, NodeDatum.class));
		}

		// THEN
		// @formatter:off
		and.then(result)
			.as("Datum at each offset returned, newest first, then null past history")
			.containsExactly(stream.get(4), stream.get(3), stream.get(2), stream.get(1), stream.get(0), null)
			;
		// @formatter:on
	}

	@Test
	public void offset_source_negative() {
		// GIVEN
		populateDatum(2);

		// WHEN
		NodeDatum result = historian.offset("test.3", -1, NodeDatum.class);
		Collection<NodeDatum> results = historian.offset(emptySet(), -1, NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("No datum for negative offset")
			.isNull()
			;
		and.then(results)
			.as("No datum for negative offset")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void offset_source_timestamp() {
		// GIVEN
		populateDatum(5);
		final List<NodeDatum> stream = population.get("test.3");

		// WHEN
		NodeDatum exact = historian.offset("test.3", stream.get(3).getTimestamp(), 0, NodeDatum.class);
		NodeDatum between = historian.offset("test.3", stream.get(3).getTimestamp().plusMillis(500), 0,
				NodeDatum.class);
		NodeDatum back = historian.offset("test.3", stream.get(3).getTimestamp(), 2, NodeDatum.class);
		NodeDatum tooFarBack = historian.offset("test.3", stream.get(3).getTimestamp(), 4,
				NodeDatum.class);
		NodeDatum beforeAll = historian.offset("test.3", stream.get(0).getTimestamp().minusMillis(1), 0,
				NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(exact)
			.as("Datum at timestamp returned")
			.isEqualTo(stream.get(3))
			;
		and.then(between)
			.as("Datum before timestamp returned")
			.isEqualTo(stream.get(3))
			;
		and.then(back)
			.as("Datum offset back from timestamp returned")
			.isEqualTo(stream.get(1))
			;
		and.then(tooFarBack)
			.as("No datum offset back past history")
			.isNull()
			;
		and.then(beforeAll)
			.as("No datum before history")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void offset_all_timestamp() {
		// GIVEN
		populateDatum(5);

		// the timestamp of the 3rd datum of test.5: sources > 5 have their 3rd datum
		// after this, so get their 2nd
		final Instant ts = population.get("test.5").get(2).getTimestamp();

		// WHEN
		Collection<NodeDatum> result = historian.offset(emptySet(), ts, 1, NodeDatum.class);

		// THEN
		List<NodeDatum> expected = new ArrayList<>();
		for ( int j = 0; j < 11; j++ ) {
			expected.add(population.get("test." + j).get(j <= 5 ? 1 : 0));
		}
		// @formatter:off
		and.then(result)
			.as("Datum offset back from timestamp returned for all sources")
			.containsExactlyInAnyOrderElementsOf(expected)
			;
		// @formatter:on
	}

	@Test
	public void offset_filter_timestamp() {
		// GIVEN
		populateDatum(5);
		final Instant ts = population.get("test.0").get(3).getTimestamp();

		// WHEN
		Collection<NodeDatum> result = historian.offset(Set.of("test.0", "test.1"), ts, 0,
				NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Datum at or before timestamp returned for matching sources")
			.containsExactlyInAnyOrder(population.get("test.0").get(3), population.get("test.1").get(2))
			;
		// @formatter:on
	}

	@Test
	public void slice_offset() {
		// GIVEN
		populateDatum(5);
		final List<NodeDatum> stream = population.get("test.7");

		// WHEN
		Collection<NodeDatum> result = historian.slice("test.7", 1, 2, NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Slice starting at offset returned, oldest to newest")
			.containsExactlyElementsOf(stream.subList(2, 4))
			;
		// @formatter:on
	}

	@Test
	public void slice_moreThanAvailable() {
		// GIVEN
		populateDatum(5);
		final List<NodeDatum> stream = population.get("test.7");

		// WHEN
		Collection<NodeDatum> result = historian.slice("test.7", 2, 10, NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("All datum from offset returned, oldest to newest")
			.containsExactlyElementsOf(stream.subList(0, 3))
			;
		// @formatter:on
	}

	@Test
	public void slice_timeOffset_offset() {
		// GIVEN
		populateDatum(5);
		final List<NodeDatum> stream = population.get("test.7");

		// WHEN
		Collection<NodeDatum> result = historian.slice("test.7", stream.get(3).getTimestamp(), 1, 2,
				NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Slice back from timestamp and offset returned, oldest to newest")
			.containsExactlyElementsOf(stream.subList(1, 3))
			;
		// @formatter:on
	}

	@Test
	public void slice_invalidArguments() {
		// GIVEN
		populateDatum(2);

		// WHEN
		Collection<NodeDatum> zeroCount = historian.slice("test.1", 0, 0, NodeDatum.class);
		Collection<NodeDatum> negOffset = historian.slice("test.1", -1, 2, NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(zeroCount)
			.as("Empty slice for zero count")
			.isEmpty()
			;
		and.then(negOffset)
			.as("Empty slice for negative offset")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void slice_notFound() {
		// GIVEN
		populateDatum(2);

		// WHEN
		Collection<NodeDatum> result = historian.slice("nope", 0, 2, NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Empty slice for unknown source")
			.isEmpty()
			;
		// @formatter:on
	}

	/* ==================================================================
	 * Adding datum
	 * ==================================================================
	 */

	@Test
	public void add_sameTimestamp_replaces() {
		// GIVEN
		historian.add(SimpleDatum.nodeDatum("a", start, samples("watts", 1)));

		// WHEN
		historian.add(new SimpleEnergyDatum("a", start, samples("watts", 2)));

		// THEN
		Collection<NodeDatum> slice = historian.slice("a", 0, 10, NodeDatum.class);
		// @formatter:off
		and.then(slice)
			.as("Datum with same source and timestamp replaced the first")
			.hasSize(1)
			.element(0)
			.as("Replacement type stored")
			.isExactlyInstanceOf(SimpleEnergyDatum.class)
			.as("Replacement samples stored")
			.returns(new BigDecimal("2"), from(d -> d.asSampleOperations().getSampleBigDecimal(
					DatumSamplesType.Instantaneous, "watts")))
			;
		// @formatter:on
	}

	@Test
	public void add_noNodeId() {
		// GIVEN
		JdbcDatumHistorian noNode = new JdbcDatumHistorian(jdbcTemplate,
				new StaticOptionalService<>(null));

		// WHEN
		noNode.add(datum("a", start));

		// THEN
		Long count = jdbcTemplate.queryForObject("SELECT count(*) FROM solarhist.dh_datum", Long.class);
		// @formatter:off
		and.then(count)
			.as("Datum not stored without node ID")
			.isZero()
			;
		and.then(noNode.latest(emptySet(), NodeDatum.class))
			.as("No datum returned without node ID")
			.isEmpty()
			;
		and.then(noNode.latest("a", NodeDatum.class))
			.as("No datum returned without node ID")
			.isNull()
			;
		and.then(noNode.slice("a", 0, 1, NodeDatum.class))
			.as("No datum returned without node ID")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void add_nodeIdNotAssigned() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(null);

		// WHEN
		historian.add(datum("a", start));

		// THEN
		Long count = jdbcTemplate.queryForObject("SELECT count(*) FROM solarhist.dh_datum", Long.class);
		// @formatter:off
		and.then(count)
			.as("Datum not stored before node ID assigned")
			.isZero()
			;
		// @formatter:on
	}

	@Test
	public void add_noSourceIdOrTimestamp() {
		// WHEN
		historian.add(datum(null, start));
		historian.add(datum("a", null));

		// THEN
		Long count = jdbcTemplate.queryForObject("SELECT count(*) FROM solarhist.dh_datum_meta",
				Long.class);
		// @formatter:off
		and.then(count)
			.as("Datum without source ID or timestamp not stored")
			.isZero()
			;
		// @formatter:on
	}

	@Test
	public void add_storesRows() {
		// GIVEN
		DatumSamples s = new DatumSamples();
		s.putInstantaneousSampleValue("watts", 123);
		s.putAccumulatingSampleValue("wattHours", 4567L);
		s.putStatusSampleValue("mode", "auto");
		s.addTag("foo");
		SimpleAcEnergyDatum d = new SimpleAcEnergyDatum("a", start, s);

		// WHEN
		historian.add(d);

		// THEN
		// @formatter:off
		List<Map<String, Object>> types = rows("solarhist", "dh_datum_type",
				"type_id, class_name, array_to_json(type_names)::TEXT AS type_names", "type_id");
		and.then(types)
			.as("Datum type stored")
			.hasSize(1)
			.element(0)
			.asInstanceOf(map(String.class, Object.class))
			.as("Class name stored")
			.containsEntry("class_name", SimpleAcEnergyDatum.class.getName())
			.as("Type names include interfaces")
			.hasEntrySatisfying("type_names", v -> and.then(v.toString())
					.contains("\"%s\"".formatted(AcEnergyDatum.class.getName()))
					.contains("\"%s\"".formatted(NodeDatum.class.getName())))
			;

		List<Map<String, Object>> meta = rows("solarhist", "dh_datum_meta",
				"stream_id, node_id, source_id, loc_id"
				+ ", array_to_json(names_i)::TEXT AS names_i"
				+ ", array_to_json(names_a)::TEXT AS names_a"
				+ ", array_to_json(names_s)::TEXT AS names_s"
				+ ", array_to_json(type_ids)::TEXT AS type_ids", "stream_id");
		and.then(meta)
			.as("Stream stored")
			.hasSize(1)
			.element(0)
			.asInstanceOf(map(String.class, Object.class))
			.containsEntry("node_id", TEST_NODE_ID)
			.containsEntry("source_id", "a")
			.containsEntry("loc_id", null)
			.containsEntry("names_i", "[\"watts\"]")
			.containsEntry("names_a", "[\"wattHours\"]")
			.containsEntry("names_s", "[\"mode\"]")
			.containsEntry("type_ids", "[%d]".formatted((Number) types.get(0).get("type_id")))
			;

		List<Map<String, Object>> data = rows("solarhist", "dh_datum",
				"stream_id, ts, type_id"
				+ ", array_to_json(data_i)::TEXT AS data_i"
				+ ", array_to_json(data_a)::TEXT AS data_a"
				+ ", data_s::TEXT AS data_s"
				+ ", array_to_json(data_t)::TEXT AS data_t", "stream_id, ts");
		and.then(data)
			.as("Datum stored")
			.hasSize(1)
			.element(0)
			.asInstanceOf(map(String.class, Object.class))
			.containsEntry("stream_id", meta.get(0).get("stream_id"))
			.containsEntry("type_id", types.get(0).get("type_id"))
			.containsEntry("data_i", "[123]")
			.containsEntry("data_a", "[4567]")
			.containsEntry("data_s", "[\"auto\"]")
			.containsEntry("data_t", "[\"foo\"]")
			;
		// @formatter:on
	}

	@Test
	public void add_error_ignored() {
		// GIVEN
		JdbcDatumHistorian broken = new JdbcDatumHistorian(jdbcTemplate, "no_such_schema",
				new StaticOptionalService<>(identityService));

		// WHEN
		broken.add(datum("a", start));

		// THEN
		// no exception thrown
	}

	/* ==================================================================
	 * Samples
	 * ==================================================================
	 */

	@Test
	public void samples_roundTrip() {
		// GIVEN
		DatumSamples s = new DatumSamples();
		s.putInstantaneousSampleValue("int", 123);
		s.putInstantaneousSampleValue("dec", new BigDecimal("1.2345678901234567890123"));
		s.putInstantaneousSampleValue("dbl", -0.25);
		s.putInstantaneousSampleValue("big", new BigDecimal("12345678901234567890"));
		s.putAccumulatingSampleValue("long", 1234567890123L);
		s.putAccumulatingSampleValue("frac", new BigDecimal("0.001"));
		s.putStatusSampleValue("str", "Hello, \"world\" ☀");
		s.putStatusSampleValue("num", 42);
		s.putStatusSampleValue("dec", new BigDecimal("1.5"));
		s.putStatusSampleValue("bool", true);
		s.addTag("foo");
		s.addTag("bar");
		SimpleDatum d = SimpleDatum.nodeDatum("a", start, s);

		// WHEN
		historian.add(d);
		NodeDatum result = historian.latest("a", NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Datum returned")
			.isEqualTo(d)
			;
		DatumSamples r = new DatumSamples(result.asSampleOperations());
		and.then(r.getInstantaneous())
			.as("Instantaneous property names restored")
			.containsOnlyKeys("int", "dec", "dbl", "big")
			;
		and.then(r.getInstantaneousSampleBigDecimal("int")).isEqualByComparingTo("123");
		and.then(r.getInstantaneousSampleBigDecimal("dec")).isEqualTo(new BigDecimal("1.2345678901234567890123"));
		and.then(r.getInstantaneousSampleBigDecimal("dbl")).isEqualByComparingTo("-0.25");
		and.then(r.getInstantaneousSampleBigDecimal("big")).isEqualTo(new BigDecimal("12345678901234567890"));
		and.then(r.getAccumulating())
			.as("Accumulating property names restored")
			.containsOnlyKeys("long", "frac")
			;
		and.then(r.getAccumulatingSampleLong("long")).isEqualTo(1234567890123L);
		and.then(r.getAccumulatingSampleBigDecimal("frac")).isEqualTo(new BigDecimal("0.001"));
		and.then(r.getStatus())
			.as("Status properties restored with JSON types")
			.containsOnlyKeys("str", "num", "dec", "bool")
			.containsEntry("str", "Hello, \"world\" ☀")
			.containsEntry("bool", true)
			;
		and.then(r.getStatusSampleInteger("num")).isEqualTo(42);
		and.then(r.getStatusSampleBigDecimal("dec")).isEqualByComparingTo("1.5");
		and.then(r.getTags())
			.as("Tags restored")
			.containsExactlyInAnyOrder("foo", "bar")
			;
		// @formatter:on
	}

	@Test
	public void samples_none() {
		// GIVEN
		SimpleDatum d = datum("a", start);

		// WHEN
		historian.add(d);
		NodeDatum result = historian.latest("a", NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Datum without samples returned")
			.isEqualTo(d)
			.extracting(NodeDatum::asSampleOperations)
			.as("No samples")
			.returns(null, from(o -> o.getSampleData(DatumSamplesType.Instantaneous)))
			.returns(null, from(o -> o.getSampleData(DatumSamplesType.Accumulating)))
			.returns(null, from(o -> o.getSampleData(DatumSamplesType.Status)))
			;
		// @formatter:on
	}

	@Test
	public void samples_nonFinite() {
		// GIVEN
		DatumSamples s = new DatumSamples();
		s.putInstantaneousSampleValue("nan", Double.NaN);
		s.putInstantaneousSampleValue("inf", Double.POSITIVE_INFINITY);
		s.putInstantaneousSampleValue("watts", 1);
		s.putAccumulatingSampleValue("ninf", Double.NEGATIVE_INFINITY);
		s.putAccumulatingSampleValue("fnan", Float.NaN);
		s.putAccumulatingSampleValue("wattHours", 2);

		// WHEN
		historian.add(SimpleDatum.nodeDatum("a", start, s));
		NodeDatum result = historian.latest("a", NodeDatum.class);

		// THEN
		DatumSamples r = new DatumSamples(result.asSampleOperations());
		// @formatter:off
		and.then(r.getInstantaneous())
			.as("Non-finite instantaneous values omitted, as a BigDecimal cannot hold them")
			.containsOnlyKeys("watts")
			;
		and.then(r.getAccumulating())
			.as("Non-finite accumulating values omitted, as a BigDecimal cannot hold them")
			.containsOnlyKeys("wattHours")
			;
		// @formatter:on
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void samples_nonNumeric() {
		// GIVEN
		DatumSamples s = new DatumSamples();
		s.putInstantaneousSampleValue("a", 1);
		s.putInstantaneousSampleValue("b", 2);
		s.putInstantaneousSampleValue("c", 3);
		// sneak in values that are not numbers
		((Map) s.getInstantaneous()).put("b", "two");
		((Map) s.getInstantaneous()).put("c", "three");

		// WHEN
		historian.add(SimpleDatum.nodeDatum("a", start, s));
		NodeDatum result = historian.latest("a", NodeDatum.class);

		// THEN
		DatumSamples r = new DatumSamples(result.asSampleOperations());
		// @formatter:off
		and.then(r.getInstantaneous())
			.as("Non-numeric instantaneous values not stored")
			.containsOnlyKeys("a")
			;
		Long trailingNulls = jdbcTemplate.queryForObject(
				"SELECT cardinality(data_i) FROM solarhist.dh_datum", Long.class);
		and.then(trailingNulls)
			.as("Trailing NULL values not stored")
			.isEqualTo(1L)
			;
		// @formatter:on
	}

	@Test
	public void samples_propertyNamesChange() {
		// GIVEN
		DatumSamples s1 = new DatumSamples();
		s1.putInstantaneousSampleValue("a", 1);
		s1.putInstantaneousSampleValue("b", 2);
		s1.putStatusSampleValue("x", "one");

		DatumSamples s2 = new DatumSamples();
		s2.putInstantaneousSampleValue("c", 3);
		s2.putAccumulatingSampleValue("d", 4);

		DatumSamples s3 = new DatumSamples();
		s3.putInstantaneousSampleValue("b", 5);
		s3.putInstantaneousSampleValue("e", 6);
		s3.putStatusSampleValue("y", "two");

		final List<DatumSamples> samples = List.of(s1, s2, s3);
		for ( int i = 0; i < samples.size(); i++ ) {
			historian.add(SimpleDatum.nodeDatum("a", start.plusSeconds(i), samples.get(i)));
		}

		// WHEN
		Collection<NodeDatum> result = historian.slice("a", 0, 3, NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("All datum returned")
			.hasSize(3)
			.map(d -> new DatumSamples(d.asSampleOperations()))
			.satisfiesExactly(
				r -> and.then(r)
					.returns(Map.of("a", new BigDecimal("1"), "b", new BigDecimal("2")), from(DatumSamples::getInstantaneous))
					.returns(null, from(DatumSamples::getAccumulating))
					.returns(Map.of("x", "one"), from(DatumSamples::getStatus)),
				r -> and.then(r)
					.returns(Map.of("c", new BigDecimal("3")), from(DatumSamples::getInstantaneous))
					.returns(Map.of("d", new BigDecimal("4")), from(DatumSamples::getAccumulating))
					.returns(null, from(DatumSamples::getStatus)),
				r -> and.then(r)
					.returns(Map.of("b", new BigDecimal("5"), "e", new BigDecimal("6")), from(DatumSamples::getInstantaneous))
					.returns(null, from(DatumSamples::getAccumulating))
					.returns(Map.of("y", "two"), from(DatumSamples::getStatus))
			)
			;
		String names = jdbcTemplate.queryForObject(
				"SELECT array_to_json(names_i)::TEXT FROM solarhist.dh_datum_meta", String.class);
		and.then(names)
			.as("Stream property names appended to")
			.isEqualTo("[\"a\",\"b\",\"c\",\"e\"]")
			;
		// @formatter:on
	}

	/* ==================================================================
	 * Datum types
	 * ==================================================================
	 */

	@Test
	public void type_restored() {
		// GIVEN
		SimpleAcEnergyDatum ac = new SimpleAcEnergyDatum("ac", start, samples("watts", 1));
		SimpleEnergyDatum energy = new SimpleEnergyDatum("energy", start, samples("watts", 2));
		SimpleDatum simple = SimpleDatum.nodeDatum("simple", start, samples("watts", 3));
		historian.add(ac);
		historian.add(energy);
		historian.add(simple);

		// WHEN
		Collection<NodeDatum> result = historian.latest(emptySet(), null);

		// THEN
		// @formatter:off
		and.then(result)
			.as("All datum returned for null type")
			.containsExactlyInAnyOrder(ac, energy, simple)
			.as("Datum classes restored")
			.extracting(d -> d.getClass().getName())
			.containsExactlyInAnyOrder(SimpleAcEnergyDatum.class.getName(),
					SimpleEnergyDatum.class.getName(), SimpleDatum.class.getName())
			;
		// @formatter:on
	}

	@Test
	public void type_filter_latest() {
		// GIVEN
		SimpleAcEnergyDatum ac = new SimpleAcEnergyDatum("ac", start, samples("watts", 1));
		SimpleEnergyDatum energy = new SimpleEnergyDatum("energy", start, samples("watts", 2));
		SimpleDatum simple = SimpleDatum.nodeDatum("simple", start, samples("watts", 3));
		historian.add(ac);
		historian.add(energy);
		historian.add(simple);

		// WHEN
		Collection<AcEnergyDatum> acResult = historian.latest(emptySet(), AcEnergyDatum.class);
		Collection<EnergyDatum> energyResult = historian.latest(emptySet(), EnergyDatum.class);
		Collection<SimpleDatum> simpleResult = historian.latest(emptySet(), SimpleDatum.class);
		Collection<AtmosphericDatum> atmosResult = historian.latest(emptySet(), AtmosphericDatum.class);

		// THEN
		// @formatter:off
		and.then(acResult)
			.as("Datum of interface type returned")
			.containsExactly(ac)
			;
		and.then(energyResult)
			.as("Datum assignable to interface type returned")
			.containsExactlyInAnyOrder(ac, energy)
			;
		and.then(simpleResult)
			.as("Datum assignable to class type returned")
			.containsExactlyInAnyOrder(ac, energy, simple)
			;
		and.then(atmosResult)
			.as("No datum returned for type not stored")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void type_filter_appliedAtOffset() {
		// GIVEN
		SimpleAcEnergyDatum older = new SimpleAcEnergyDatum("a", start, samples("watts", 1));
		SimpleDatum newer = SimpleDatum.nodeDatum("a", start.plusSeconds(1), samples("watts", 2));
		historian.add(older);
		historian.add(newer);

		// WHEN
		AcEnergyDatum latest = historian.latest("a", AcEnergyDatum.class);
		AcEnergyDatum offset = historian.offset("a", 1, AcEnergyDatum.class);
		Collection<AcEnergyDatum> latestAll = historian.latest(emptySet(), AcEnergyDatum.class);
		Collection<AcEnergyDatum> offsetAll = historian.offset(emptySet(), 1, AcEnergyDatum.class);
		Collection<AcEnergyDatum> slice = historian.slice("a", 0, 2, AcEnergyDatum.class);

		// THEN
		// @formatter:off
		and.then(latest)
			.as("Latest datum is not of requested type, so none returned (like in-memory historian)")
			.isNull()
			;
		and.then(offset)
			.as("Datum at offset is of requested type")
			.isEqualTo(older)
			.isExactlyInstanceOf(SimpleAcEnergyDatum.class)
			;
		and.then(latestAll)
			.as("Latest datum is not of requested type, so none returned (like in-memory historian)")
			.isEmpty()
			;
		and.then(offsetAll)
			.as("Datum at offset is of requested type")
			.containsExactly(older)
			;
		and.then(slice)
			.as("Slice filtered by type")
			.containsExactly(older)
			;
		// @formatter:on
	}

	@Test
	public void type_filter_slice() {
		// GIVEN
		List<NodeDatum> all = new ArrayList<>();
		for ( int i = 0; i < 6; i++ ) {
			NodeDatum d = (i % 2 == 0
					? new SimpleAcEnergyDatum("a", start.plusSeconds(i), samples("i", i))
					: SimpleDatum.nodeDatum("a", start.plusSeconds(i), samples("i", i)));
			all.add(d);
			historian.add(d);
		}

		// WHEN
		Collection<AcEnergyDatum> slice = historian.slice("a", 1, 4, AcEnergyDatum.class);

		// THEN
		// @formatter:off
		and.then(slice)
			.as("Slice of offsets 1-4 filtered by type, oldest to newest")
			.containsExactly((AcEnergyDatum) all.get(2), (AcEnergyDatum) all.get(4))
			;
		// @formatter:on
	}

	@Test
	public void type_filter_otherSourceHasType() {
		// GIVEN
		SimpleAcEnergyDatum ac = new SimpleAcEnergyDatum("a", start, samples("watts", 1));
		SimpleDatum simple = SimpleDatum.nodeDatum("b", start, samples("watts", 2));
		historian.add(ac);
		historian.add(simple);

		// WHEN
		AcEnergyDatum result = historian.latest("b", AcEnergyDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Source without datum of type returns nothing")
			.isNull()
			;
		// @formatter:on
	}

	/* ==================================================================
	 * Location datum
	 * ==================================================================
	 */

	@Test
	public void location_roundTrip() {
		// GIVEN
		SimpleAtmosphericDatum d = new SimpleAtmosphericDatum(TEST_LOC_ID, "weather", start,
				samples("temp", new BigDecimal("12.3")));

		// WHEN
		historian.add(d);
		AtmosphericDatum result = historian.latest("weather", AtmosphericDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Location datum returned")
			.isEqualTo(d)
			.isExactlyInstanceOf(SimpleAtmosphericDatum.class)
			.returns(ObjectDatumKind.Location, from(NodeDatum::getKind))
			.returns(TEST_LOC_ID, from(NodeDatum::getObjectId))
			;
		// @formatter:on
	}

	@Test
	public void location_mergedWithNodeStream() {
		// GIVEN
		List<NodeDatum> all = new ArrayList<>();
		for ( int i = 0; i < 4; i++ ) {
			NodeDatum d = (i % 2 == 0
					? SimpleDatum.locationDatum(TEST_LOC_ID, "a", start.plusSeconds(i), samples("i", i))
					: SimpleDatum.nodeDatum("a", start.plusSeconds(i), samples("i", i)));
			all.add(d);
			historian.add(d);
		}

		// WHEN
		NodeDatum latest = historian.latest("a", NodeDatum.class);
		NodeDatum offset = historian.offset("a", 1, NodeDatum.class);
		Collection<NodeDatum> latestAll = historian.latest(emptySet(), NodeDatum.class);
		Collection<NodeDatum> offsetFilter = historian.offset(singleton("a"), 3, NodeDatum.class);
		Collection<NodeDatum> slice = historian.slice("a", 0, 4, NodeDatum.class);

		// THEN
		Long streams = jdbcTemplate.queryForObject("SELECT count(*) FROM solarhist.dh_datum_meta",
				Long.class);
		// @formatter:off
		and.then(streams)
			.as("Node and location datum stored in their own streams")
			.isEqualTo(2L)
			;
		and.then(latest)
			.as("Latest datum across both streams returned")
			.isEqualTo(all.get(3))
			;
		and.then(offset)
			.as("Offset datum across both streams returned")
			.isEqualTo(all.get(2))
			.returns(ObjectDatumKind.Location, from(NodeDatum::getKind))
			;
		and.then(latestAll)
			.as("One datum returned for source with two streams")
			.containsExactly(all.get(3))
			;
		and.then(offsetFilter)
			.as("Offset datum across both streams returned")
			.containsExactly(all.get(0))
			;
		and.then(slice)
			.as("Slice across both streams returned, oldest to newest")
			.containsExactlyElementsOf(all)
			;
		// @formatter:on
	}

	/* ==================================================================
	 * Caches
	 * ==================================================================
	 */

	@Test
	public void caches_defaults() {
		// @formatter:off
		and.then(historian.getDatumTypeCache())
			.as("Type cache used by default")
			.isNotNull()
			.isEmpty()
			;
		and.then(historian.getDatumStreamsCache())
			.as("Streams cache used by default")
			.isNotNull()
			.isEmpty()
			;
		and.then(historian.getDatumTypeCache())
			.as("Each historian has its own type cache")
			.isNotSameAs(unfilteredHistorian.getDatumTypeCache())
			;
		and.then(historian.getDatumStreamsCache())
			.as("Each historian has its own streams cache")
			.isNotSameAs(unfilteredHistorian.getDatumStreamsCache())
			;
		// @formatter:on
	}

	@Test
	public void caches_disabled() {
		// GIVEN
		historian.setDatumTypeCache(null);
		historian.setDatumStreamsCache(null);
		populateDatum(2);
		historian.add(new SimpleAcEnergyDatum("ac", start, samples("watts", 1)));

		// WHEN
		Collection<NodeDatum> result1 = historian.latest(singleton("test.1*"), NodeDatum.class);
		Collection<NodeDatum> result2 = historian.latest(singleton("test.1*"), NodeDatum.class);
		AcEnergyDatum ac = historian.latest("ac", AcEnergyDatum.class);

		// THEN
		// @formatter:off
		and.then(result1)
			.as("Pattern query works without streams cache")
			.hasSize(2)
			;
		and.then(result2)
			.as("Repeated pattern query gives same result")
			.containsExactlyInAnyOrderElementsOf(result1)
			;
		and.then(ac)
			.as("Datum type stored without type cache")
			.isExactlyInstanceOf(SimpleAcEnergyDatum.class)
			;
		and.then(jdbcTemplate.queryForObject("SELECT count(*) FROM solarhist.dh_datum", Long.class))
			.as("All datum stored without type cache")
			.isEqualTo(23L)
			;
		// @formatter:on
	}

	@Test
	public void typeCache_populated() {
		// GIVEN
		final ConcurrentMap<String, Integer> typeCache = new ConcurrentHashMap<>();
		historian.setDatumTypeCache(typeCache);

		// WHEN
		historian.add(datum("a", start));
		historian.add(datum("b", start));
		historian.add(datum("a", start.plusSeconds(1)));

		// THEN
		Integer typeId = jdbcTemplate.queryForObject("SELECT type_id FROM solarhist.dh_datum_type",
				Integer.class);
		// @formatter:off
		and.then(typeCache)
			.as("Type ID cached")
			.hasSize(1)
			.containsEntry(SimpleDatum.class.getName(), typeId)
			;
		and.then(jdbcTemplate.queryForObject("SELECT count(*) FROM solarhist.dh_datum", Long.class))
			.as("All datum stored using cached type ID")
			.isEqualTo(3L)
			;
		and.then(historian.slice("a", 0, 5, null))
			.as("Datum stored with cached type ID restored with type")
			.hasSize(2)
			.allSatisfy(d -> and.then(d).isExactlyInstanceOf(SimpleDatum.class))
			;
		// @formatter:on
	}

	@Test
	public void typeCache_perClass() {
		// GIVEN
		final ConcurrentMap<String, Integer> cached = new ConcurrentHashMap<>();
		historian.setDatumTypeCache(cached);

		// WHEN
		historian.add(datum("a", start));
		historian.add(new SimpleAcEnergyDatum("b", start, samples("watts", 1)));

		// THEN
		List<Map<String, Object>> types = rows("solarhist", "dh_datum_type", "class_name, type_id",
				"type_id");
		Map<String, Integer> expected = new HashMap<>();
		for ( Map<String, Object> row : types ) {
			expected.put((String) row.get("class_name"), ((Number) row.get("type_id")).intValue());
		}
		// @formatter:off
		and.then(cached)
			.as("Type ID cached for each class")
			.isEqualTo(expected)
			.hasSize(2)
			;
		// @formatter:on
	}

	@Test
	public void typeCache_staleEntryRemoved() {
		// GIVEN
		final ConcurrentMap<String, Integer> typeCache = new ConcurrentHashMap<>();
		typeCache.put(SimpleDatum.class.getName(), Short.MAX_VALUE - 1);
		historian.setDatumTypeCache(typeCache);

		// WHEN
		historian.add(datum("a", start));

		// THEN
		// the error aborts the test transaction, so nothing more can be queried
		// @formatter:off
		and.then(typeCache)
			.as("Stale type ID removed after error")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void streamsCache_populated() {
		// GIVEN
		final ConcurrentMap<Long, DatumStreams> cached = new ConcurrentHashMap<>();
		historian.setDatumStreamsCache(cached);
		populateDatum(2);

		// WHEN
		Collection<NodeDatum> result1 = historian.latest(singleton("test.1*"), NodeDatum.class);
		DatumStreams streams1 = cached.get(TEST_NODE_ID);
		Collection<NodeDatum> result2 = historian.latest(singleton("test.2"), NodeDatum.class);
		DatumStreams streams2 = cached.get(TEST_NODE_ID);

		// THEN
		// @formatter:off
		and.then(result1).hasSize(2);
		and.then(result2).hasSize(1);
		and.then(cached)
			.as("Streams cached by node ID")
			.containsOnlyKeys(TEST_NODE_ID)
			;
		and.then(streams1)
			.as("Streams cached by first query")
			.isNotNull()
			;
		and.then(streams1.streams())
			.as("All streams of node cached")
			.hasSize(11)
			;
		and.then(streams2)
			.as("Cached streams reused by second query")
			.isSameAs(streams1)
			;
		// @formatter:on
	}

	@Test
	public void streamsCache_newStreamEvicts() {
		// GIVEN
		final ConcurrentMap<Long, DatumStreams> cached = new ConcurrentHashMap<>();
		historian.setDatumStreamsCache(cached);
		populateDatum(2);
		and.then(historian.latest(singleton("test.*"), NodeDatum.class)).hasSize(11);

		// WHEN
		historian.add(datum("test.1", start.plusSeconds(60)));
		boolean cachedAfterExistingStream = cached.containsKey(TEST_NODE_ID);
		historian.add(datum("test.new", start.plusSeconds(60)));
		boolean cachedAfterNewStream = cached.containsKey(TEST_NODE_ID);
		Collection<NodeDatum> result = historian.latest(singleton("test.*"), NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(cachedAfterExistingStream)
			.as("Cached streams kept after adding to existing stream")
			.isTrue()
			;
		and.then(cachedAfterNewStream)
			.as("Cached streams removed after adding to new stream")
			.isFalse()
			;
		and.then(result)
			.as("New stream found by pattern query after cache eviction")
			.hasSize(12)
			.contains(datum("test.new", start.plusSeconds(60)))
			;
		and.then(cached.get(TEST_NODE_ID))
			.as("Streams cached again by pattern query, with new stream")
			.isNotNull()
			.satisfies(s -> and.then(s.streams()).hasSize(12))
			;
		// @formatter:on
	}

	@Test
	public void streamsCache_notUsedWithoutFilter() {
		// GIVEN
		final ConcurrentMap<Long, DatumStreams> cached = new ConcurrentHashMap<>();
		historian.setDatumStreamsCache(cached);
		populateDatum(1);

		// WHEN
		Collection<NodeDatum> result = historian.latest(emptySet(), NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(result).hasSize(11);
		and.then(cached)
			.as("Streams not loaded for query without source ID filter")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void schemas_independent() {
		// GIVEN
		SimpleDatum filtered = SimpleDatum.nodeDatum("a", start, samples("watts", 1));
		SimpleDatum unfiltered = SimpleDatum.nodeDatum("a", start, samples("watts", 2));
		SimpleDatum unfilteredOnly = SimpleDatum.nodeDatum("b", start, samples("watts", 3));
		historian.add(filtered);
		unfilteredHistorian.add(unfiltered);
		unfilteredHistorian.add(unfilteredOnly);

		// WHEN
		NodeDatum filteredResult = historian.latest("a", NodeDatum.class);
		NodeDatum unfilteredResult = unfilteredHistorian.latest("a", NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(filteredResult.asSampleOperations().getSampleInteger(
				DatumSamplesType.Instantaneous, "watts"))
			.as("Filtered history has its own datum")
			.isEqualTo(1)
			;
		and.then(unfilteredResult.asSampleOperations().getSampleInteger(
				DatumSamplesType.Instantaneous, "watts"))
			.as("Unfiltered history has its own datum")
			.isEqualTo(2)
			;
		and.then(historian.latest("b", NodeDatum.class))
			.as("Unfiltered datum not in filtered history")
			.isNull()
			;
		// @formatter:on
	}

}
