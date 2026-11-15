/* ==================================================================
 * AggregateStreamDatumRowMapper.java - 15 Nov 2026 4:12:08 pm
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

package net.solarnetwork.node.datumhistorian.jdbc;

import static net.solarnetwork.dao.jdbc.CommonJdbcUtils.getArray;
import static net.solarnetwork.dao.jdbc.CommonJdbcUtils.getTimestampInstant;
import static net.solarnetwork.dao.jdbc.CommonJdbcUtils.timestampInstant;
import static net.solarnetwork.node.service.support.DatumStreamIdUtils.encodeLocalStreamId;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.RowMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.domain.datum.AggregateStreamDatum;
import net.solarnetwork.domain.datum.BasicAggregateStreamDatum;
import net.solarnetwork.domain.datum.BasicReadingStreamDatum;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumPropertiesStatistics;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider;
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectAggregateDatum;
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectReadingDifference;

/**
 * Row mapper for {@link AggregateStreamDatum} instances, from stored aggregate
 * rows or reading difference rows.
 *
 * <p>
 * Expects the columns returned by a {@link SelectAggregateDatum} or
 * {@link SelectReadingDifference} statement, which are those of the SolarNet
 * aggregate and reading row mappers, with the node ID of the stream second:
 * </p>
 *
 * <ol>
 * <li>{@code stream_id}</li>
 * <li>{@code node_id}</li>
 * <li>{@code ts_start}</li>
 * <li>{@code data_i}</li>
 * <li>{@code data_a}</li>
 * <li>{@code data_s} (a JSON array)</li>
 * <li>{@code data_t}</li>
 * <li>{@code stat_i} (two-dimensional)</li>
 * <li>{@code read_a} (two-dimensional)</li>
 * <li>{@code ts_end} (only for {@link Style#ReadingDifference})</li>
 * </ol>
 *
 * <p>
 * Stream IDs, rows of streams without metadata, and property values are handled
 * as {@link StreamDatumRowMapper} handles them. The {@code stat_i} values
 * become the instantaneous statistics and the {@code read_a} values the
 * accumulating statistics, as SolarNet maps them.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class AggregateStreamDatumRowMapper implements RowMapper<AggregateStreamDatum> {

	/**
	 * The style of results to map.
	 */
	public enum Style {

		/**
		 * Aggregate list results: {@link BasicAggregateStreamDatum} instances,
		 * from rows without an end timestamp column.
		 */
		Aggregate,

		/**
		 * Aggregate reading results: {@link BasicReadingStreamDatum} instances
		 * without an end timestamp, from rows without an end timestamp column.
		 */
		Reading,

		/**
		 * Reading difference results: {@link BasicReadingStreamDatum}
		 * instances, from rows with an end timestamp column.
		 */
		ReadingDifference,

	}

	private final ObjectDatumStreamMetadataProvider metadataProvider;
	private final int streamIdDomain;
	private final ObjectMapper objectMapper;
	private final Style style;

	/**
	 * Constructor.
	 *
	 * @param metadataProvider
	 *        the provider of the metadata of the streams to map the rows of
	 * @param streamIdDomain
	 *        the historian domain to encode stream IDs with, from 1 to 255
	 * @param objectMapper
	 *        the object mapper to parse status values with
	 * @param style
	 *        the style of results to map
	 * @throws IllegalArgumentException
	 *         if {@code metadataProvider}, {@code objectMapper}, or
	 *         {@code style} is {@code null}
	 */
	public AggregateStreamDatumRowMapper(ObjectDatumStreamMetadataProvider metadataProvider,
			int streamIdDomain, ObjectMapper objectMapper, Style style) {
		super();
		this.metadataProvider = requireNonNullArgument(metadataProvider, "metadataProvider");
		this.streamIdDomain = streamIdDomain;
		this.objectMapper = requireNonNullArgument(objectMapper, "objectMapper");
		this.style = requireNonNullArgument(style, "style");
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the datum, or {@code null} if the metadata provider has no
	 *         metadata for the stream of the row
	 */
	@Override
	public @Nullable AggregateStreamDatum mapRow(ResultSet rs, int rowNum) throws SQLException {
		final UUID streamId = encodeLocalStreamId(rs.getLong(2), streamIdDomain, rs.getInt(1));
		if ( metadataProvider.metadataForStreamId(streamId) == null ) {
			return null;
		}
		final Instant ts = timestampInstant(rs, 3);
		final DatumProperties props = DatumProperties.propertiesOf(getArray(rs, 4), getArray(rs, 5),
				StreamDatumRowMapper.statusArray(objectMapper, rs.getString(6)), getArray(rs, 7));
		final DatumPropertiesStatistics stats = DatumPropertiesStatistics.statisticsOf(getArray(rs, 8),
				getArray(rs, 9));
		return switch (style) {
			case Aggregate -> new BasicAggregateStreamDatum(streamId, ts, props, null, stats);
			case Reading -> new BasicReadingStreamDatum(streamId, ts, props, null, stats);
			case ReadingDifference -> new BasicReadingStreamDatum(streamId, ts, props,
					getTimestampInstant(rs, 10), stats);
		};
	}

	/**
	 * Get the style of results mapped.
	 *
	 * @return the style
	 */
	public Style getStyle() {
		return style;
	}

}
