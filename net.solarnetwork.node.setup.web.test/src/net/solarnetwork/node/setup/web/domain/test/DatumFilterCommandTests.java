/* ==================================================================
 * DatumFilterCommandTests.java - 13 Nov 2026 4:05:12 pm
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

package net.solarnetwork.node.setup.web.domain.test;

import static org.assertj.core.api.BDDAssertions.then;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.UUID;
import org.junit.Test;
import net.solarnetwork.domain.MutableSortDescriptor;
import net.solarnetwork.domain.SimpleSortDescriptor;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.node.dao.BasicNodeDatumFilter;
import net.solarnetwork.node.service.FilterableDatumHistorian;
import net.solarnetwork.node.setup.web.domain.DatumFilterCommand;

/**
 * Test cases for the {@link DatumFilterCommand} class.
 *
 * @author matt
 * @version 1.0
 */
public class DatumFilterCommandTests {

	private static final long NODE_ID = 123L;
	private static final ZoneId ZONE = ZoneId.of("Pacific/Auckland");

	@Test
	public void includesNode_none() {
		// GIVEN
		final DatumFilterCommand cmd = new DatumFilterCommand();

		// WHEN
		final boolean result = cmd.includesNode(NODE_ID);

		// THEN
		then(result).as("No node IDs given allows any node").isTrue();
	}

	@Test
	public void includesNode_empty() {
		// GIVEN
		final DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setNodeIds(new Long[0]);

		// WHEN
		final boolean result = cmd.includesNode(NODE_ID);

		// THEN
		then(result).as("Empty node IDs allows any node").isTrue();
	}

	@Test
	public void includesNode_ownAmongOthers() {
		// GIVEN
		final DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setNodeIds(new Long[] { 1L, NODE_ID, 2L });

		// WHEN
		final boolean result = cmd.includesNode(NODE_ID);

		// THEN
		then(result).as("Node among node IDs is allowed").isTrue();
	}

	@Test
	public void includesNode_othersOnly() {
		// GIVEN
		final DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setNodeIds(new Long[] { 1L, 2L });

		// WHEN
		final boolean result = cmd.includesNode(NODE_ID);

		// THEN
		then(result).as("Node not among node IDs is not allowed").isFalse();
	}

	@Test
	public void includesNode_singular() {
		// GIVEN
		final DatumFilterCommand cmd = new DatumFilterCommand();

		// WHEN
		cmd.setNodeId(NODE_ID);

		// THEN
		then(cmd.getNodeIds()).as("Singular node ID sets node IDs").containsExactly(NODE_ID);
		then(cmd.getNodeId()).as("Singular node ID is first node ID").isEqualTo(NODE_ID);
		then(cmd.includesNode(NODE_ID)).as("Node allowed").isTrue();
		then(cmd.includesNode(1L)).as("Other node not allowed").isFalse();
	}

	@Test
	public void toNodeDatumFilter_forcesNode() {
		// GIVEN
		final DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setNodeIds(new Long[] { 1L, NODE_ID });
		cmd.setSourceIds(new String[] { "a/**", "b" });

		// WHEN
		final BasicNodeDatumFilter result = cmd.toNodeDatumFilter(NODE_ID, ZONE);

		// THEN
		then(result.getObjectKind()).as("Node kind forced").isEqualTo(ObjectDatumKind.Node);
		then(result.getObjectIds()).as("Only the given node ID is used").containsExactly(NODE_ID);
		then(result.getSourceIds()).as("Source IDs copied").containsExactly("a/**", "b");
		then(result.getStreamIds()).as("No stream IDs").isNull();
		then(result.getStartDate()).as("No start date").isNull();
		then(result.getEndDate()).as("No end date").isNull();
		then(result.getSorts()).as("No sorts").isNull();
		then(result.getMax()).as("No max").isNull();
		then(result.getOffset()).as("No offset").isNull();
		then(result.isMostRecent()).as("Not most recent").isFalse();
		then(result.isWithoutTotalResultsCount()).as("Total results counted, as in SolarQuery")
				.isFalse();
	}

	@Test
	public void toNodeDatumFilter_criteria() {
		// GIVEN
		final UUID streamId = UUID.randomUUID();
		final DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setStreamId(streamId);
		cmd.setMostRecent(true);
		cmd.setMax(10);
		cmd.setOffset(20L);
		cmd.setWithoutTotalResultsCount(true);
		cmd.setAggregation(Aggregation.Hour);
		cmd.setPartialAggregation(Aggregation.Day);

		// WHEN
		final BasicNodeDatumFilter result = cmd.toNodeDatumFilter(NODE_ID, ZONE);

		// THEN
		then(result.getStreamIds()).as("Stream IDs copied").containsExactly(streamId);
		then(result.isMostRecent()).as("Most recent copied").isTrue();
		then(result.getMax()).as("Max copied").isEqualTo(10);
		then(result.getOffset()).as("Offset copied").isEqualTo(20L);
		then(result.isWithoutTotalResultsCount()).as("Without total results count copied").isTrue();
		then(result.getAggregation()).as("Aggregation copied").isEqualTo(Aggregation.Hour);
		then(result.getPartialAggregation()).as("Partial aggregation copied")
				.isEqualTo(Aggregation.Day);
	}

	@Test
	public void toNodeDatumFilter_dates() {
		// GIVEN
		final Instant start = Instant.parse("2026-11-01T00:00:00Z");
		final LocalDateTime localEnd = LocalDateTime.of(2026, 11, 13, 0, 0);
		final DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setStartDate(start);
		cmd.setLocalStartDate(LocalDateTime.of(2020, 1, 1, 0, 0));
		cmd.setLocalEndDate(localEnd);

		// WHEN
		final BasicNodeDatumFilter result = cmd.toNodeDatumFilter(NODE_ID, ZONE);

		// THEN
		then(result.getStartDate()).as("Start date used over local start date").isEqualTo(start);
		then(result.getEndDate()).as("Local end date converted in time zone")
				.isEqualTo(localEnd.atZone(ZONE).toInstant());
		then(result.getLocalStartDate()).as("Local start date not used").isNull();
		then(result.getLocalEndDate()).as("Local end date not used").isNull();
	}

	@Test
	public void toNodeDatumFilter_sorts() {
		// GIVEN
		final DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setSorts(Arrays.asList(new MutableSortDescriptor("Created", true),
				new MutableSortDescriptor("node"), new MutableSortDescriptor("foo"),
				new MutableSortDescriptor("SOURCE"), new MutableSortDescriptor(null),
				new MutableSortDescriptor("time")));

		// WHEN
		final BasicNodeDatumFilter result = cmd.toNodeDatumFilter(NODE_ID, ZONE);

		// THEN
		then(result.getSorts()).as("Supported sorts mapped to historian sort keys")
				.containsExactly(new SimpleSortDescriptor(FilterableDatumHistorian.SORT_BY_TIME, true),
						new SimpleSortDescriptor(FilterableDatumHistorian.SORT_BY_NODE),
						new SimpleSortDescriptor(FilterableDatumHistorian.SORT_BY_SOURCE),
						new SimpleSortDescriptor(FilterableDatumHistorian.SORT_BY_TIME));
	}

	@Test
	public void toNodeDatumFilter_sorts_noneSupported() {
		// GIVEN
		final DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setSorts(Arrays.asList(new MutableSortDescriptor("foo")));

		// WHEN
		final BasicNodeDatumFilter result = cmd.toNodeDatumFilter(NODE_ID, ZONE);

		// THEN
		then(result.getSorts()).as("No sorts when none supported").isNull();
	}

	@Test
	public void toNodeDatumFilter_blankValues() {
		// GIVEN
		final DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setSourceIds(new String[] { "", " " });
		cmd.setStreamIds(new UUID[0]);

		// WHEN
		final BasicNodeDatumFilter result = cmd.toNodeDatumFilter(NODE_ID, ZONE);

		// THEN
		then(result.getSourceIds()).as("Blank source IDs ignored").isNull();
		then(result.getStreamIds()).as("Empty stream IDs ignored").isNull();
	}

	@Test
	public void allowedPropertyNames() {
		// GIVEN
		final DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setPropertyNames(new String[] { "b", "", "a", "b" });

		// WHEN
		final var result = cmd.allowedPropertyNames();

		// THEN
		then(result).as("Distinct property names in order given").containsExactly("b", "a");
	}

	@Test
	public void allowedPropertyNames_none() {
		// GIVEN
		final DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setPropertyNames(new String[] { "" });

		// WHEN
		final var result = cmd.allowedPropertyNames();

		// THEN
		then(result).as("No property names when only blank ones given").isNull();
	}

}
