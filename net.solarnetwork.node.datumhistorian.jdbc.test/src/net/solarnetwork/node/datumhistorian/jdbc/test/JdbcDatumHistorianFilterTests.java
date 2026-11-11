/* ==================================================================
 * JdbcDatumHistorianFilterTests.java - 11 Nov 2026 11:02:42 am
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

import static net.solarnetwork.node.service.FilterableDatumHistorian.SORT_BY_SOURCE;
import static net.solarnetwork.node.service.FilterableDatumHistorian.SORT_BY_TIME;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.thenIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SimpleSortDescriptor;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.node.dao.BasicNodeDatumFilter;
import net.solarnetwork.node.datumhistorian.jdbc.JdbcDatumHistorian;
import net.solarnetwork.node.domain.datum.AcEnergyDatum;
import net.solarnetwork.node.domain.datum.AtmosphericDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleAcEnergyDatum;
import net.solarnetwork.node.domain.datum.SimpleAtmosphericDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link JdbcDatumHistorian#findFiltered} method, using the
 * Postgres test database.
 *
 * <p>
 * Each test starts with this history, where {@code t0}, {@code t1}, and
 * {@code t2} are one minute apart:
 * </p>
 *
 * <table>
 * <tr>
 * <th>Node</th>
 * <th>Source</th>
 * <th>Location</th>
 * <th>Type</th>
 * <th>Timestamps</th>
 * </tr>
 * <tr>
 * <td>123</td>
 * <td>a/1, a/2</td>
 * <td></td>
 * <td>SimpleAcEnergyDatum</td>
 * <td>t0, t1, t2</td>
 * </tr>
 * <tr>
 * <td>123</td>
 * <td>b/1</td>
 * <td></td>
 * <td>SimpleDatum</td>
 * <td>t0, t1, t2</td>
 * </tr>
 * <tr>
 * <td>123</td>
 * <td>weather</td>
 * <td>345</td>
 * <td>SimpleAtmosphericDatum</td>
 * <td>t0, t1</td>
 * </tr>
 * <tr>
 * <td>234</td>
 * <td>a/1</td>
 * <td></td>
 * <td>SimpleDatum</td>
 * <td>t0, t1</td>
 * </tr>
 * <tr>
 * <td>234</td>
 * <td>weather</td>
 * <td>345</td>
 * <td>SimpleAtmosphericDatum</td>
 * <td>t0</td>
 * </tr>
 * </table>
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@RunWith(MockitoJUnitRunner.class)
public class JdbcDatumHistorianFilterTests extends AbstractPostgresHistorianTests {

	private static final Long TEST_NODE_ID = 123L;
	private static final Long TEST_NODE_ID_2 = 234L;
	private static final Long TEST_LOC_ID = 345L;

	@Mock(strictness = Mock.Strictness.LENIENT)
	private IdentityService identityService;

	@Mock(strictness = Mock.Strictness.LENIENT)
	private IdentityService identityService2;

	private JdbcDatumHistorian historian;
	private JdbcDatumHistorian otherHistorian;

	private Instant t0;
	private Instant t1;
	private Instant t2;

	@Before
	public void setup() {
		given(identityService.getNodeId()).willReturn(TEST_NODE_ID);
		given(identityService2.getNodeId()).willReturn(TEST_NODE_ID_2);
		historian = new JdbcDatumHistorian(jdbcTemplate, new StaticOptionalService<>(identityService));
		otherHistorian = new JdbcDatumHistorian(jdbcTemplate,
				new StaticOptionalService<>(identityService2));
		t0 = Instant.now().truncatedTo(ChronoUnit.MINUTES).minus(10, ChronoUnit.MINUTES);
		t1 = t0.plus(1, ChronoUnit.MINUTES);
		t2 = t1.plus(1, ChronoUnit.MINUTES);
		populate();
	}

	private static DatumSamples samples(int value) {
		DatumSamples s = new DatumSamples();
		s.putInstantaneousSampleValue("i", value);
		return s;
	}

	private void populate() {
		int i = 0;
		for ( Instant ts : List.of(t0, t1, t2) ) {
			historian.add(new SimpleAcEnergyDatum("a/1", ts, samples(i++)));
			historian.add(new SimpleAcEnergyDatum("a/2", ts, samples(i++)));
			historian.add(SimpleDatum.nodeDatum("b/1", ts, samples(i++)));
		}
		for ( Instant ts : List.of(t0, t1) ) {
			historian.add(new SimpleAtmosphericDatum(TEST_LOC_ID, "weather", ts, samples(i++)));
			otherHistorian.add(SimpleDatum.nodeDatum("a/1", ts, samples(i++)));
		}
		otherHistorian.add(new SimpleAtmosphericDatum(TEST_LOC_ID, "weather", t0, samples(i++)));
	}

	private static DatumId node(Long nodeId, String sourceId, Instant ts) {
		return DatumId.nodeId(nodeId, sourceId, ts);
	}

	private DatumId node(String sourceId, Instant ts) {
		return node(TEST_NODE_ID, sourceId, ts);
	}

	private DatumId weather(Instant ts) {
		return DatumId.locationId(TEST_LOC_ID, "weather", ts);
	}

	/**
	 * Get all the datum IDs of the running node, in the default order.
	 *
	 * <p>
	 * Location datum come first, as Postgres sorts {@code NULL} location IDs
	 * last.
	 * </p>
	 */
	private List<DatumId> allRunningNodeIds() {
		final List<DatumId> result = new ArrayList<>();
		result.add(weather(t0));
		result.add(weather(t1));
		for ( String sourceId : List.of("a/1", "a/2", "b/1") ) {
			for ( Instant ts : List.of(t0, t1, t2) ) {
				result.add(node(sourceId, ts));
			}
		}
		return result;
	}

	private static List<DatumId> ids(FilterResults<NodeDatum, DatumId> results) {
		final List<DatumId> ids = new ArrayList<>();
		for ( NodeDatum d : results ) {
			ids.add(d.getId());
		}
		return ids;
	}

	private static void thenPage(FilterResults<NodeDatum, DatumId> result, Long totalResults,
			int returnedResultCount, long startingOffset) {
		// @formatter:off
		and.then(result.getTotalResults())
			.as("Total results")
			.isEqualTo(totalResults)
			;
		and.then(result.getReturnedResultCount())
			.as("Returned result count")
			.isEqualTo(returnedResultCount)
			;
		and.then(result.getStartingOffset())
			.as("Starting offset")
			.isEqualTo(startingOffset)
			;
		// @formatter:on
	}

	private static SortDescriptor desc(String key) {
		return new SimpleSortDescriptor(key, true);
	}

	private static SortDescriptor asc(String key) {
		return new SimpleSortDescriptor(key);
	}

	@Test
	public void nullFilter() {
		thenIllegalArgumentException().isThrownBy(() -> historian.findFiltered(null));
	}

	@Test
	public void runningNode() {
		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(new BasicNodeDatumFilter());

		// THEN
		final List<DatumId> expected = allRunningNodeIds();
		// @formatter:off
		and.then(ids(result))
			.as("All datum of running node returned, by node, location, source, and time")
			.containsExactlyElementsOf(expected)
			;
		// @formatter:on
		thenPage(result, (long) expected.size(), expected.size(), 0L);
	}

	@Test
	public void runningNode_datumRestored() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setSourceIds(new String[] { "a/1", "weather" });
		filter.setMostRecent(true);

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(filter);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Datum of both sources returned")
			.hasSize(2)
			;
		and.then(result.getResults()).element(1)
			.as("Node datum class restored")
			.isExactlyInstanceOf(SimpleAcEnergyDatum.class)
			.as("Node datum has node ID")
			.returns(TEST_NODE_ID, from(NodeDatum::getObjectId))
			.returns(ObjectDatumKind.Node, from(NodeDatum::getKind))
			.as("Samples restored")
			.returns(6, from(d -> d.asSampleOperations().getSampleInteger(
					DatumSamplesType.Instantaneous, "i")))
			;
		and.then(result.getResults()).element(0)
			.as("Location datum class restored")
			.isExactlyInstanceOf(SimpleAtmosphericDatum.class)
			.as("Location datum has location ID")
			.returns(TEST_LOC_ID, from(NodeDatum::getObjectId))
			.returns(ObjectDatumKind.Location, from(NodeDatum::getKind))
			;
		// @formatter:on
	}

	@Test
	public void runningNode_notAvailable() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(null);

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(new BasicNodeDatumFilter());

		// THEN
		// @formatter:off
		and.then(result)
			.as("No results without node ID")
			.isEmpty()
			;
		// @formatter:on
		thenPage(result, 0L, 0, 0L);
	}

	@Test
	public void runningNode_notAvailable_nodeIds() {
		// GIVEN
		given(identityService.getNodeId()).willReturn(null);
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setObjectKind(ObjectDatumKind.Node);
		filter.setObjectId(TEST_NODE_ID_2);

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(filter);

		// THEN
		// @formatter:off
		and.then(ids(result))
			.as("Node IDs criteria used without running node ID")
			.containsExactly(node(TEST_NODE_ID_2, "a/1", t0), node(TEST_NODE_ID_2, "a/1", t1))
			;
		// @formatter:on
	}

	@Test
	public void nodeIds() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setObjectKind(ObjectDatumKind.Node);
		filter.setObjectIds(new Long[] { TEST_NODE_ID, TEST_NODE_ID_2 });
		filter.setSourceId("a/1");

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(filter);

		// THEN
		// @formatter:off
		and.then(ids(result))
			.as("Node datum of all given nodes returned")
			.containsExactly(
					node("a/1", t0), node("a/1", t1), node("a/1", t2),
					node(TEST_NODE_ID_2, "a/1", t0), node(TEST_NODE_ID_2, "a/1", t1))
			;
		// @formatter:on
	}

	@Test
	public void nodeIds_noKind() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setObjectId(TEST_NODE_ID_2);

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(filter);

		// THEN
		// @formatter:off
		and.then(ids(result))
			.as("Object IDs without kind treated as node IDs, without location datum")
			.containsExactly(node(TEST_NODE_ID_2, "a/1", t0), node(TEST_NODE_ID_2, "a/1", t1))
			;
		// @formatter:on
	}

	@Test
	public void nodeKind_runningNode() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setObjectKind(ObjectDatumKind.Node);

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(filter);

		// THEN
		// @formatter:off
		and.then(ids(result))
			.as("Only node datum of running node returned")
			.containsExactlyElementsOf(allRunningNodeIds().subList(2, 11))
			;
		// @formatter:on
	}

	@Test
	public void locationKind_runningNode() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setObjectKind(ObjectDatumKind.Location);

		// WHEN
		FilterResults<NodeDatum, DatumId> result = otherHistorian.findFiltered(filter);

		// THEN
		// @formatter:off
		and.then(ids(result))
			.as("Only location datum of running node returned")
			.containsExactly(weather(t0))
			;
		// @formatter:on
	}

	@Test
	public void locationIds() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setObjectKind(ObjectDatumKind.Location);
		filter.setObjectId(TEST_LOC_ID);

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(filter);

		// THEN
		// @formatter:off
		and.then(ids(result))
			.as("Location datum stored by all nodes returned, by node and then time")
			.containsExactly(weather(t0), weather(t1), weather(t0))
			;
		// @formatter:on
	}

	@Test
	public void sourceIds() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setSourceIds(new String[] { "b/1", "a/2" });

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(filter);

		// THEN
		// @formatter:off
		and.then(ids(result))
			.as("Datum of given sources returned")
			.containsExactly(
					node("a/2", t0), node("a/2", t1), node("a/2", t2),
					node("b/1", t0), node("b/1", t1), node("b/1", t2))
			;
		// @formatter:on
	}

	@Test
	public void sourceIds_pattern() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setSourceId("*/1");
		filter.setMostRecent(true);

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(filter);

		// THEN
		// @formatter:off
		and.then(ids(result))
			.as("Datum of sources matching pattern returned")
			.containsExactly(node("a/1", t2), node("b/1", t2))
			;
		// @formatter:on
	}

	@Test
	public void sourceIds_patterns() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setSourceIds(new String[] { "a/**", "w?ather" });
		filter.setMostRecent(true);

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(filter);

		// THEN
		// @formatter:off
		and.then(ids(result))
			.as("Datum of sources matching any pattern returned")
			.containsExactly(weather(t1), node("a/1", t2), node("a/2", t2))
			;
		// @formatter:on
	}

	@Test
	public void sourceIds_caseInsensitive() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setSourceIds(new String[] { "A/1", "B/*" });
		filter.setMostRecent(true);

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(filter);

		// THEN
		// @formatter:off
		and.then(ids(result))
			.as("Source IDs and patterns matched case-insensitively")
			.containsExactly(node("a/1", t2), node("b/1", t2))
			;
		// @formatter:on
	}

	@Test
	public void sourceIds_noMatch() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setSourceId("a");

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(filter);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Source ID is not a prefix match")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void types() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setType(AcEnergyDatum.class.getName());
		filter.setMostRecent(true);

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(filter);

		// THEN
		// @formatter:off
		and.then(ids(result))
			.as("Datum assignable to type returned")
			.containsExactly(node("a/1", t2), node("a/2", t2))
			;
		// @formatter:on
	}

	@Test
	public void types_multiple() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setTypes(new String[] { AtmosphericDatum.class.getName(), "a.b.c.Unknown",
				SimpleAcEnergyDatum.class.getName() });
		filter.setMostRecent(true);

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(filter);

		// THEN
		// @formatter:off
		and.then(ids(result))
			.as("Datum assignable to any type returned")
			.containsExactly(weather(t1), node("a/1", t2), node("a/2", t2))
			;
		// @formatter:on
	}

	@Test
	public void types_perDatum() {
		// GIVEN
		final Instant t3 = t2.plus(1, ChronoUnit.MINUTES);
		final Instant t4 = t3.plus(1, ChronoUnit.MINUTES);
		historian.add(new SimpleAcEnergyDatum("b/1", t3, samples(100)));
		historian.add(SimpleDatum.nodeDatum("b/1", t4, samples(101)));
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setSourceId("b/1");
		filter.setType(AcEnergyDatum.class.getName());

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(filter);
		filter.setMostRecent(true);
		FilterResults<NodeDatum, DatumId> mostRecent = historian.findFiltered(filter);

		// THEN
		// @formatter:off
		and.then(ids(result))
			.as("Only datum of type returned from stream with many types")
			.containsExactly(node("b/1", t3))
			;
		and.then(ids(mostRecent))
			.as("Most recent datum of type returned")
			.containsExactly(node("b/1", t3))
			;
		// @formatter:on
	}

	@Test
	public void dateRange() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setStartDate(t1);
		filter.setEndDate(t2);

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(filter);

		// THEN
		// @formatter:off
		and.then(ids(result))
			.as("Datum at or after start and before end returned")
			.containsExactly(weather(t1), node("a/1", t1), node("a/2", t1), node("b/1", t1))
			;
		// @formatter:on
	}

	@Test
	public void startDate() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setStartDate(t2);

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(filter);

		// THEN
		// @formatter:off
		and.then(ids(result))
			.as("Datum at or after start returned")
			.containsExactly(node("a/1", t2), node("a/2", t2), node("b/1", t2))
			;
		// @formatter:on
	}

	@Test
	public void endDate() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setEndDate(t1);

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(filter);

		// THEN
		// @formatter:off
		and.then(ids(result))
			.as("Datum before end returned")
			.containsExactly(weather(t0), node("a/1", t0), node("a/2", t0), node("b/1", t0))
			;
		// @formatter:on
	}

	@Test
	public void mostRecent() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setMostRecent(true);

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(filter);

		// THEN
		// @formatter:off
		and.then(ids(result))
			.as("Most recent datum of each stream of running node returned")
			.containsExactly(weather(t1), node("a/1", t2), node("a/2", t2), node("b/1", t2))
			;
		// @formatter:on
	}

	@Test
	public void mostRecent_dateRangeAndType() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setMostRecent(true);
		filter.setStartDate(t0);
		filter.setEndDate(t2);
		filter.setType(AcEnergyDatum.class.getName());

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(filter);

		// THEN
		// @formatter:off
		and.then(ids(result))
			.as("Most recent datum within date range and of type returned")
			.containsExactly(node("a/1", t1), node("a/2", t1))
			;
		// @formatter:on
	}

	@Test
	public void mostRecent_nodeIds() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setMostRecent(true);
		filter.setObjectKind(ObjectDatumKind.Node);
		filter.setObjectIds(new Long[] { TEST_NODE_ID_2, TEST_NODE_ID });
		filter.setSourceId("a/1");

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(filter);

		// THEN
		// @formatter:off
		and.then(ids(result))
			.as("Most recent datum of each node's stream returned")
			.containsExactly(node("a/1", t2), node(TEST_NODE_ID_2, "a/1", t1))
			;
		// @formatter:on
	}

	@Test
	public void sorts() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setSourceId("a/*");
		filter.setSorts(List.of(desc(SORT_BY_TIME), desc(SORT_BY_SOURCE)));

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(filter);

		// THEN
		// @formatter:off
		and.then(ids(result))
			.as("Datum sorted by given sorts")
			.containsExactly(
					node("a/2", t2), node("a/1", t2),
					node("a/2", t1), node("a/1", t1),
					node("a/2", t0), node("a/1", t0))
			;
		// @formatter:on
	}

	@Test
	public void sorts_nodeDescending() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setObjectIds(new Long[] { TEST_NODE_ID, TEST_NODE_ID_2 });
		filter.setSourceId("a/1");
		filter.setSorts(List.of(desc("node"), desc(SORT_BY_TIME)));

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(filter);

		// THEN
		// @formatter:off
		and.then(ids(result))
			.as("Datum sorted by node descending, then time descending")
			.containsExactly(
					node(TEST_NODE_ID_2, "a/1", t1), node(TEST_NODE_ID_2, "a/1", t0),
					node("a/1", t2), node("a/1", t1), node("a/1", t0))
			;
		// @formatter:on
	}

	@Test
	public void pagination_totalResults() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setWithoutTotalResultsCount(false);
		filter.setOffset(2L);
		filter.setMax(4);

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(filter);

		// THEN
		final List<DatumId> all = allRunningNodeIds();
		// @formatter:off
		and.then(ids(result))
			.as("Page of results returned")
			.containsExactlyElementsOf(all.subList(2, 6))
			;
		// @formatter:on
		thenPage(result, (long) all.size(), 4, 2L);
	}

	@Test
	public void pagination_withoutTotalResults() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setMax(3);

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(filter);

		// THEN
		// @formatter:off
		and.then(ids(result))
			.as("First page of results returned")
			.containsExactlyElementsOf(allRunningNodeIds().subList(0, 3))
			;
		// @formatter:on
		thenPage(result, null, 3, 0L);
	}

	@Test
	public void pagination_mostRecent_totalResults() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setMostRecent(true);
		filter.setWithoutTotalResultsCount(false);
		filter.setOffset(3L);
		filter.setMax(2);

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(filter);

		// THEN
		// @formatter:off
		and.then(ids(result))
			.as("Last page of most recent results returned")
			.containsExactly(node("b/1", t2))
			;
		// @formatter:on
		thenPage(result, 4L, 1, 3L);
	}

	@Test
	public void pagination_arguments() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setWithoutTotalResultsCount(false);
		filter.setSorts(List.of(asc(SORT_BY_SOURCE)));
		filter.setOffset(5L);
		filter.setMax(2);
		final BasicNodeDatumFilter orig = filter.clone();

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(filter,
				List.of(desc(SORT_BY_TIME), asc(SORT_BY_SOURCE)), 1L, 3);

		// THEN
		// @formatter:off
		and.then(ids(result))
			.as("Arguments used in place of filter pagination")
			.containsExactly(node("a/2", t2), node("b/1", t2), node("a/1", t1))
			;
		and.then(filter)
			.as("Filter not changed")
			.isEqualTo(orig)
			;
		// @formatter:on
		thenPage(result, 11L, 3, 1L);
	}

	@Test
	public void pagination_arguments_some() {
		// GIVEN
		BasicNodeDatumFilter filter = new BasicNodeDatumFilter();
		filter.setSorts(List.of(desc(SORT_BY_TIME), asc(SORT_BY_SOURCE)));
		filter.setOffset(5L);
		filter.setMax(2);

		// WHEN
		FilterResults<NodeDatum, DatumId> result = historian.findFiltered(filter, null, null, 1);

		// THEN
		// @formatter:off
		and.then(ids(result))
			.as("Max argument used with filter sorts and offset")
			.containsExactly(node("b/1", t1))
			;
		// @formatter:on
	}

}
