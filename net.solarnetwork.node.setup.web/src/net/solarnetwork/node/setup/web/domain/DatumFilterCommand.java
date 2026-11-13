/* ==================================================================
 * DatumFilterCommand.java - 13 Nov 2026 3:24:55 pm
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

package net.solarnetwork.node.setup.web.domain;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.domain.MutableSortDescriptor;
import net.solarnetwork.domain.SimpleSortDescriptor;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.node.dao.BasicNodeDatumFilter;
import net.solarnetwork.node.service.FilterableDatumHistorian;

/**
 * Search criteria for SolarQuery compatible datum queries.
 *
 * <p>
 * This binds the SolarQuery request parameters that the node supports, and
 * turns them into a {@link BasicNodeDatumFilter} with
 * {@link #toNodeDatumFilter(long, ZoneId)}. Other SolarQuery parameters have no
 * property here, so they are ignored.
 * </p>
 *
 * @author matt
 * @version 1.0
 * @since 5.4
 */
public class DatumFilterCommand {

	private Long @Nullable [] nodeIds;
	private String @Nullable [] sourceIds;
	private UUID @Nullable [] streamIds;
	private @Nullable Instant startDate;
	private @Nullable Instant endDate;
	private @Nullable LocalDateTime localStartDate;
	private @Nullable LocalDateTime localEndDate;
	private boolean mostRecent;
	private @Nullable Integer max;
	private @Nullable Long offset;
	private boolean withoutTotalResultsCount = true;
	private @Nullable List<MutableSortDescriptor> sorts;
	private @Nullable Aggregation aggregation;
	private @Nullable Aggregation partialAggregation;
	private String @Nullable [] propertyNames;

	/**
	 * Constructor.
	 */
	public DatumFilterCommand() {
		super();
	}

	/**
	 * Test if the node ID criteria allow a given node.
	 *
	 * @param nodeId
	 *        the node ID to test
	 * @return {@code true} if no node IDs are given, or {@code nodeId} is one
	 *         of them
	 */
	public boolean includesNode(long nodeId) {
		final Long[] ids = this.nodeIds;
		if ( ids == null ) {
			return true;
		}
		boolean any = false;
		for ( Long id : ids ) {
			if ( id != null ) {
				if ( id.longValue() == nodeId ) {
					return true;
				}
				any = true;
			}
		}
		return !any;
	}

	/**
	 * Create a datum historian filter from these criteria.
	 *
	 * <p>
	 * The filter is limited to node datum of {@code nodeId}, whatever node IDs
	 * these criteria have; use {@link #includesNode(long)} to test those first.
	 * A date is taken from the instant criteria when given, otherwise from the
	 * local date criteria in the {@code zone} time zone. Sorts are mapped to
	 * the {@link FilterableDatumHistorian} sort keys: {@code node},
	 * {@code source}, and {@code created} or {@code time}, in any case; other
	 * sorts are left out.
	 * </p>
	 *
	 * @param nodeId
	 *        the ID of the node to limit the filter to
	 * @param zone
	 *        the time zone of the local date criteria
	 * @return the filter
	 */
	public BasicNodeDatumFilter toNodeDatumFilter(long nodeId, ZoneId zone) {
		final BasicNodeDatumFilter f = new BasicNodeDatumFilter();
		f.setObjectKind(ObjectDatumKind.Node);
		f.setObjectIds(new Long[] { nodeId });
		f.setSourceIds(texts(sourceIds));
		f.setStreamIds(streamIds(streamIds));
		f.setStartDate(instant(startDate, localStartDate, zone));
		f.setEndDate(instant(endDate, localEndDate, zone));
		f.setMostRecent(mostRecent);
		f.setAggregation(aggregation);
		f.setPartialAggregation(partialAggregation);
		f.setWithoutTotalResultsCount(withoutTotalResultsCount);
		f.setSorts(historianSorts(sorts));
		f.setOffset(offset);
		f.setMax(max);
		return f;
	}

	/**
	 * Get the property names to restrict results to.
	 *
	 * @return the property names, or {@code null} for all properties
	 */
	public @Nullable Set<String> allowedPropertyNames() {
		final String[] names = texts(propertyNames);
		if ( names == null ) {
			return null;
		}
		final Set<String> result = new LinkedHashSet<>(names.length);
		for ( String name : names ) {
			result.add(name);
		}
		return result;
	}

	private static @Nullable Instant instant(@Nullable Instant date, @Nullable LocalDateTime localDate,
			ZoneId zone) {
		if ( date != null ) {
			return date;
		}
		return (localDate != null ? localDate.atZone(zone).toInstant() : null);
	}

	private static String @Nullable [] texts(String @Nullable [] values) {
		if ( values == null ) {
			return null;
		}
		final List<String> result = new ArrayList<>(values.length);
		for ( String value : values ) {
			if ( value != null && !value.isBlank() ) {
				result.add(value);
			}
		}
		return (result.isEmpty() ? null : result.toArray(String[]::new));
	}

	private static UUID @Nullable [] streamIds(UUID @Nullable [] values) {
		if ( values == null ) {
			return null;
		}
		final List<UUID> result = new ArrayList<>(values.length);
		for ( UUID value : values ) {
			if ( value != null ) {
				result.add(value);
			}
		}
		return (result.isEmpty() ? null : result.toArray(UUID[]::new));
	}

	private static @Nullable List<SortDescriptor> historianSorts(
			@Nullable List<MutableSortDescriptor> sorts) {
		if ( sorts == null ) {
			return null;
		}
		final List<SortDescriptor> result = new ArrayList<>(sorts.size());
		for ( MutableSortDescriptor sort : sorts ) {
			final String key = (sort != null ? historianSortKey(sort.getSortKey()) : null);
			if ( sort != null && key != null ) {
				result.add(new SimpleSortDescriptor(key, sort.isDescending()));
			}
		}
		return (result.isEmpty() ? null : result);
	}

	private static @Nullable String historianSortKey(@Nullable String key) {
		if ( key == null ) {
			return null;
		}
		return switch (key.toLowerCase(Locale.ENGLISH)) {
			case "node" -> FilterableDatumHistorian.SORT_BY_NODE;
			case "source" -> FilterableDatumHistorian.SORT_BY_SOURCE;
			case "created", "time" -> FilterableDatumHistorian.SORT_BY_TIME;
			default -> null;
		};
	}

	/**
	 * Get the node IDs.
	 *
	 * @return the node IDs
	 */
	public final Long @Nullable [] getNodeIds() {
		return nodeIds;
	}

	/**
	 * Set the node IDs.
	 *
	 * @param nodeIds
	 *        the node IDs to set
	 */
	public final void setNodeIds(Long @Nullable [] nodeIds) {
		this.nodeIds = nodeIds;
	}

	/**
	 * Get the first node ID.
	 *
	 * @return the first node ID, or {@code null}
	 */
	public final @Nullable Long getNodeId() {
		final Long[] ids = this.nodeIds;
		return (ids != null && ids.length > 0 ? ids[0] : null);
	}

	/**
	 * Set a single node ID.
	 *
	 * @param nodeId
	 *        the node ID to set, replacing any node IDs
	 */
	public final void setNodeId(@Nullable Long nodeId) {
		this.nodeIds = (nodeId != null ? new Long[] { nodeId } : null);
	}

	/**
	 * Get the source IDs.
	 *
	 * @return the source IDs, which can be Ant style patterns
	 */
	public final String @Nullable [] getSourceIds() {
		return sourceIds;
	}

	/**
	 * Set the source IDs.
	 *
	 * @param sourceIds
	 *        the source IDs to set, which can be Ant style patterns
	 */
	public final void setSourceIds(String @Nullable [] sourceIds) {
		this.sourceIds = sourceIds;
	}

	/**
	 * Get the first source ID.
	 *
	 * @return the first source ID, or {@code null}
	 */
	public final @Nullable String getSourceId() {
		final String[] ids = this.sourceIds;
		return (ids != null && ids.length > 0 ? ids[0] : null);
	}

	/**
	 * Set a single source ID.
	 *
	 * @param sourceId
	 *        the source ID to set, replacing any source IDs
	 */
	public final void setSourceId(@Nullable String sourceId) {
		this.sourceIds = (sourceId != null ? new String[] { sourceId } : null);
	}

	/**
	 * Get the stream IDs.
	 *
	 * @return the stream IDs
	 */
	public final UUID @Nullable [] getStreamIds() {
		return streamIds;
	}

	/**
	 * Set the stream IDs.
	 *
	 * @param streamIds
	 *        the stream IDs to set
	 */
	public final void setStreamIds(UUID @Nullable [] streamIds) {
		this.streamIds = streamIds;
	}

	/**
	 * Get the first stream ID.
	 *
	 * @return the first stream ID, or {@code null}
	 */
	public final @Nullable UUID getStreamId() {
		final UUID[] ids = this.streamIds;
		return (ids != null && ids.length > 0 ? ids[0] : null);
	}

	/**
	 * Set a single stream ID.
	 *
	 * @param streamId
	 *        the stream ID to set, replacing any stream IDs
	 */
	public final void setStreamId(@Nullable UUID streamId) {
		this.streamIds = (streamId != null ? new UUID[] { streamId } : null);
	}

	/**
	 * Get the start date.
	 *
	 * @return the start date (inclusive)
	 */
	public final @Nullable Instant getStartDate() {
		return startDate;
	}

	/**
	 * Set the start date.
	 *
	 * @param startDate
	 *        the start date to set (inclusive)
	 */
	public final void setStartDate(@Nullable Instant startDate) {
		this.startDate = startDate;
	}

	/**
	 * Get the end date.
	 *
	 * @return the end date (exclusive)
	 */
	public final @Nullable Instant getEndDate() {
		return endDate;
	}

	/**
	 * Set the end date.
	 *
	 * @param endDate
	 *        the end date to set (exclusive)
	 */
	public final void setEndDate(@Nullable Instant endDate) {
		this.endDate = endDate;
	}

	/**
	 * Get the local start date.
	 *
	 * @return the local start date (inclusive)
	 */
	public final @Nullable LocalDateTime getLocalStartDate() {
		return localStartDate;
	}

	/**
	 * Set the local start date.
	 *
	 * <p>
	 * This is only used when no start date is set.
	 * </p>
	 *
	 * @param localStartDate
	 *        the local start date to set (inclusive)
	 */
	public final void setLocalStartDate(@Nullable LocalDateTime localStartDate) {
		this.localStartDate = localStartDate;
	}

	/**
	 * Get the local end date.
	 *
	 * @return the local end date (exclusive)
	 */
	public final @Nullable LocalDateTime getLocalEndDate() {
		return localEndDate;
	}

	/**
	 * Set the local end date.
	 *
	 * <p>
	 * This is only used when no end date is set.
	 * </p>
	 *
	 * @param localEndDate
	 *        the local end date to set (exclusive)
	 */
	public final void setLocalEndDate(@Nullable LocalDateTime localEndDate) {
		this.localEndDate = localEndDate;
	}

	/**
	 * Get the most recent flag.
	 *
	 * @return {@code true} for only the most recent datum of each stream
	 */
	public final boolean isMostRecent() {
		return mostRecent;
	}

	/**
	 * Set the most recent flag.
	 *
	 * @param mostRecent
	 *        {@code true} for only the most recent datum of each stream
	 */
	public final void setMostRecent(boolean mostRecent) {
		this.mostRecent = mostRecent;
	}

	/**
	 * Get the maximum result count.
	 *
	 * @return the maximum result count
	 */
	public final @Nullable Integer getMax() {
		return max;
	}

	/**
	 * Set the maximum result count.
	 *
	 * @param max
	 *        the maximum result count to set
	 */
	public final void setMax(@Nullable Integer max) {
		this.max = max;
	}

	/**
	 * Get the result offset.
	 *
	 * @return the result offset
	 */
	public final @Nullable Long getOffset() {
		return offset;
	}

	/**
	 * Set the result offset.
	 *
	 * @param offset
	 *        the result offset to set
	 */
	public final void setOffset(@Nullable Long offset) {
		this.offset = offset;
	}

	/**
	 * Get the "without total results count" flag.
	 *
	 * @return {@code true} to not count the total results of a query with a
	 *         maximum result count
	 */
	public final boolean isWithoutTotalResultsCount() {
		return withoutTotalResultsCount;
	}

	/**
	 * Set the "without total results count" flag.
	 *
	 * @param withoutTotalResultsCount
	 *        {@code true} to not count the total results of a query with a
	 *        maximum result count; defaults to {@code true}, unlike SolarQuery,
	 *        as counting the total results queries all the matching datum, not
	 *        only those returned
	 */
	public final void setWithoutTotalResultsCount(boolean withoutTotalResultsCount) {
		this.withoutTotalResultsCount = withoutTotalResultsCount;
	}

	/**
	 * Get the sorts.
	 *
	 * @return the sorts
	 */
	public final @Nullable List<MutableSortDescriptor> getSorts() {
		return sorts;
	}

	/**
	 * Set the sorts.
	 *
	 * @param sorts
	 *        the sorts to set
	 */
	public final void setSorts(@Nullable List<MutableSortDescriptor> sorts) {
		this.sorts = sorts;
	}

	/**
	 * Get the aggregation.
	 *
	 * @return the aggregation
	 */
	public final @Nullable Aggregation getAggregation() {
		return aggregation;
	}

	/**
	 * Set the aggregation.
	 *
	 * @param aggregation
	 *        the aggregation to set
	 */
	public final void setAggregation(@Nullable Aggregation aggregation) {
		this.aggregation = aggregation;
	}

	/**
	 * Get the partial aggregation.
	 *
	 * @return the partial aggregation
	 */
	public final @Nullable Aggregation getPartialAggregation() {
		return partialAggregation;
	}

	/**
	 * Set the partial aggregation.
	 *
	 * @param partialAggregation
	 *        the partial aggregation to set
	 */
	public final void setPartialAggregation(@Nullable Aggregation partialAggregation) {
		this.partialAggregation = partialAggregation;
	}

	/**
	 * Get the property names to restrict results to.
	 *
	 * @return the property names
	 */
	public final String @Nullable [] getPropertyNames() {
		return propertyNames;
	}

	/**
	 * Set the property names to restrict results to.
	 *
	 * @param propertyNames
	 *        the property names to set
	 */
	public final void setPropertyNames(String @Nullable [] propertyNames) {
		this.propertyNames = propertyNames;
	}

}
