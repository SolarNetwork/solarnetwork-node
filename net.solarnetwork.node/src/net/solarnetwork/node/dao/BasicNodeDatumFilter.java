/* ==================================================================
 * BasicNodeDatumFilter.java - 11 Nov 2026 11:02:42 am
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

package net.solarnetwork.node.dao;

import static net.solarnetwork.util.ArrayUtils.copyArray;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.domain.SimplePagination;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Basic implementation of {@link NodeDatumFilter}.
 *
 * <p>
 * The {@code withoutTotalResultsCount} property is {@code true} by default.
 * </p>
 *
 * @author matt
 * @version 1.1
 */
public class BasicNodeDatumFilter extends SimplePagination implements NodeDatumFilter {

	private @Nullable Instant startDate;
	private @Nullable Instant endDate;
	private @Nullable LocalDateTime localStartDate;
	private @Nullable LocalDateTime localEndDate;
	private @Nullable Aggregation aggregation;
	private @Nullable Aggregation partialAggregation;
	private Long @Nullable [] objectIds;
	private @Nullable ObjectDatumKind objectKind;
	private String @Nullable [] sourceIds;
	private UUID @Nullable [] streamIds;
	private String @Nullable [] types;
	private boolean withoutTotalResultsCount = true;
	private boolean mostRecent;

	/**
	 * Constructor.
	 */
	public BasicNodeDatumFilter() {
		super();
	}

	/**
	 * Create a copy of a filter.
	 *
	 * <p>
	 * The array and sort values of {@code filter} are copied, so the result can
	 * be changed without changing {@code filter}.
	 * </p>
	 *
	 * @param filter
	 *        the filter to copy
	 * @return the new filter
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@code null}
	 */
	public static BasicNodeDatumFilter copyOf(NodeDatumFilter filter) {
		requireNonNullArgument(filter, "filter");
		final BasicNodeDatumFilter f = new BasicNodeDatumFilter();
		f.setStartDate(filter.getStartDate());
		f.setEndDate(filter.getEndDate());
		f.setLocalStartDate(filter.getLocalStartDate());
		f.setLocalEndDate(filter.getLocalEndDate());
		f.setAggregation(filter.getAggregation());
		f.setPartialAggregation(filter.getPartialAggregation());
		f.setObjectIds(copyArray(filter.getObjectIds()));
		f.setObjectKind(filter.getObjectKind());
		f.setSourceIds(copyArray(filter.getSourceIds()));
		f.setStreamIds(copyArray(filter.getStreamIds()));
		f.setTypes(copyArray(filter.getTypes()));
		final List<SortDescriptor> sorts = filter.getSorts();
		f.setSorts(sorts != null ? new ArrayList<>(sorts) : null);
		f.setOffset(filter.getOffset());
		f.setMax(filter.getMax());
		f.setWithoutTotalResultsCount(filter.isWithoutTotalResultsCount());
		f.setMostRecent(filter.isMostRecent());
		return f;
	}

	@Override
	public BasicNodeDatumFilter clone() {
		return (BasicNodeDatumFilter) super.clone();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(objectIds);
		result = prime * result + Arrays.hashCode(sourceIds);
		result = prime * result + Arrays.hashCode(streamIds);
		result = prime * result + Arrays.hashCode(types);
		result = prime * result + Objects.hash(startDate, endDate, localStartDate, localEndDate,
				aggregation, partialAggregation, objectKind, withoutTotalResultsCount, mostRecent);
		return result;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals(obj) || !(obj instanceof BasicNodeDatumFilter other) ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(startDate, other.startDate)
				&& Objects.equals(endDate, other.endDate)
				&& Objects.equals(localStartDate, other.localStartDate)
				&& Objects.equals(localEndDate, other.localEndDate)
				&& aggregation == other.aggregation
				&& partialAggregation == other.partialAggregation
				&& Arrays.equals(objectIds, other.objectIds)
				&& objectKind == other.objectKind
				&& Arrays.equals(sourceIds, other.sourceIds)
				&& Arrays.equals(streamIds, other.streamIds)
				&& Arrays.equals(types, other.types)
				&& withoutTotalResultsCount == other.withoutTotalResultsCount
				&& mostRecent == other.mostRecent;
		// @formatter:on
	}

	@Override
	public String toString() {
		final StringBuilder buf = new StringBuilder("BasicNodeDatumFilter{");
		if ( objectKind != null ) {
			buf.append("objectKind=").append(objectKind).append(", ");
		}
		if ( objectIds != null ) {
			buf.append("objectIds=").append(Arrays.toString(objectIds)).append(", ");
		}
		if ( sourceIds != null ) {
			buf.append("sourceIds=").append(Arrays.toString(sourceIds)).append(", ");
		}
		if ( streamIds != null ) {
			buf.append("streamIds=").append(Arrays.toString(streamIds)).append(", ");
		}
		if ( types != null ) {
			buf.append("types=").append(Arrays.toString(types)).append(", ");
		}
		if ( startDate != null ) {
			buf.append("startDate=").append(startDate).append(", ");
		}
		if ( endDate != null ) {
			buf.append("endDate=").append(endDate).append(", ");
		}
		if ( localStartDate != null ) {
			buf.append("localStartDate=").append(localStartDate).append(", ");
		}
		if ( localEndDate != null ) {
			buf.append("localEndDate=").append(localEndDate).append(", ");
		}
		if ( aggregation != null ) {
			buf.append("aggregation=").append(aggregation).append(", ");
		}
		if ( partialAggregation != null ) {
			buf.append("partialAggregation=").append(partialAggregation).append(", ");
		}
		if ( mostRecent ) {
			buf.append("mostRecent=true, ");
		}
		if ( getSorts() != null ) {
			buf.append("sorts=").append(getSorts()).append(", ");
		}
		if ( getOffset() != null ) {
			buf.append("offset=").append(getOffset()).append(", ");
		}
		if ( getMax() != null ) {
			buf.append("max=").append(getMax()).append(", ");
		}
		buf.append("withoutTotalResultsCount=").append(withoutTotalResultsCount);
		buf.append('}');
		return buf.toString();
	}

	@Override
	public final @Nullable Instant getStartDate() {
		return startDate;
	}

	/**
	 * Set the start date (inclusive).
	 *
	 * @param startDate
	 *        the date to set
	 */
	public final void setStartDate(@Nullable Instant startDate) {
		this.startDate = startDate;
	}

	@Override
	public final @Nullable Instant getEndDate() {
		return endDate;
	}

	/**
	 * Set the end date (exclusive).
	 *
	 * @param endDate
	 *        the date to set
	 */
	public final void setEndDate(@Nullable Instant endDate) {
		this.endDate = endDate;
	}

	@Override
	public final @Nullable LocalDateTime getLocalStartDate() {
		return localStartDate;
	}

	/**
	 * Set the local start date (inclusive).
	 *
	 * @param localStartDate
	 *        the date to set
	 * @since 1.1
	 */
	public final void setLocalStartDate(@Nullable LocalDateTime localStartDate) {
		this.localStartDate = localStartDate;
	}

	@Override
	public final @Nullable LocalDateTime getLocalEndDate() {
		return localEndDate;
	}

	/**
	 * Set the local end date (exclusive).
	 *
	 * @param localEndDate
	 *        the date to set
	 * @since 1.1
	 */
	public final void setLocalEndDate(@Nullable LocalDateTime localEndDate) {
		this.localEndDate = localEndDate;
	}

	@Override
	public final @Nullable Aggregation getAggregation() {
		return aggregation;
	}

	/**
	 * Set the aggregation.
	 *
	 * @param aggregation
	 *        the aggregation to set
	 * @since 1.1
	 */
	public final void setAggregation(@Nullable Aggregation aggregation) {
		this.aggregation = aggregation;
	}

	@Override
	public final @Nullable Aggregation getPartialAggregation() {
		return partialAggregation;
	}

	/**
	 * Set the partial aggregation.
	 *
	 * @param partialAggregation
	 *        the partial aggregation to set
	 * @since 1.1
	 */
	public final void setPartialAggregation(@Nullable Aggregation partialAggregation) {
		this.partialAggregation = partialAggregation;
	}

	@Override
	public final Long @Nullable [] getObjectIds() {
		return objectIds;
	}

	/**
	 * Set the object IDs.
	 *
	 * @param objectIds
	 *        the node or location IDs to set, depending on the object kind
	 */
	public final void setObjectIds(Long @Nullable [] objectIds) {
		this.objectIds = objectIds;
	}

	/**
	 * Set a single object ID.
	 *
	 * <p>
	 * This replaces any object IDs already set.
	 * </p>
	 *
	 * @param objectId
	 *        the node or location ID to set, depending on the object kind, or
	 *        {@code null} to clear the object IDs
	 */
	public final void setObjectId(@Nullable Long objectId) {
		setObjectIds(objectId != null ? new Long[] { objectId } : null);
	}

	@Override
	public final @Nullable ObjectDatumKind getObjectKind() {
		return objectKind;
	}

	/**
	 * Set the object kind.
	 *
	 * @param objectKind
	 *        the kind to set
	 */
	public final void setObjectKind(@Nullable ObjectDatumKind objectKind) {
		this.objectKind = objectKind;
	}

	@Override
	public final @Nullable String getSourceId() {
		final String[] array = getSourceIds();
		return (array != null && array.length > 0 ? array[0] : null);
	}

	/**
	 * Set a single source ID.
	 *
	 * <p>
	 * This replaces any source IDs already set.
	 * </p>
	 *
	 * @param sourceId
	 *        the source ID or pattern to set, or {@code null} to clear the
	 *        source IDs
	 */
	public final void setSourceId(@Nullable String sourceId) {
		setSourceIds(sourceId != null ? new String[] { sourceId } : null);
	}

	@Override
	public final String @Nullable [] getSourceIds() {
		return sourceIds;
	}

	/**
	 * Set the source IDs.
	 *
	 * @param sourceIds
	 *        the source IDs or patterns to set
	 */
	public final void setSourceIds(String @Nullable [] sourceIds) {
		this.sourceIds = sourceIds;
	}

	@Override
	public final UUID @Nullable [] getStreamIds() {
		return streamIds;
	}

	/**
	 * Set the stream IDs.
	 *
	 * @param streamIds
	 *        the stream IDs to set
	 * @since 1.1
	 */
	public final void setStreamIds(UUID @Nullable [] streamIds) {
		this.streamIds = streamIds;
	}

	/**
	 * Set a single stream ID.
	 *
	 * <p>
	 * This replaces any stream IDs already set.
	 * </p>
	 *
	 * @param streamId
	 *        the stream ID to set, or {@code null} to clear the stream IDs
	 * @since 1.1
	 */
	public final void setStreamId(@Nullable UUID streamId) {
		setStreamIds(streamId != null ? new UUID[] { streamId } : null);
	}

	@Override
	public final String @Nullable [] getTypes() {
		return types;
	}

	/**
	 * Set the datum types.
	 *
	 * @param types
	 *        the datum class or interface names to set
	 */
	public final void setTypes(String @Nullable [] types) {
		this.types = types;
	}

	/**
	 * Set a single datum type.
	 *
	 * <p>
	 * This replaces any types already set.
	 * </p>
	 *
	 * @param type
	 *        the datum class or interface name to set, or {@code null} to clear
	 *        the types
	 */
	public final void setType(@Nullable String type) {
		setTypes(type != null && !type.isEmpty() ? new String[] { type } : null);
	}

	@Override
	public final boolean isWithoutTotalResultsCount() {
		return withoutTotalResultsCount;
	}

	/**
	 * Set the "without total results count" mode.
	 *
	 * @param withoutTotalResultsCount
	 *        {@code true} to not count the total results
	 */
	public final void setWithoutTotalResultsCount(boolean withoutTotalResultsCount) {
		this.withoutTotalResultsCount = withoutTotalResultsCount;
	}

	@Override
	public final boolean isMostRecent() {
		return mostRecent;
	}

	/**
	 * Set the "most recent" mode.
	 *
	 * @param mostRecent
	 *        {@code true} to return only the most recent datum
	 */
	public final void setMostRecent(boolean mostRecent) {
		this.mostRecent = mostRecent;
	}

}
