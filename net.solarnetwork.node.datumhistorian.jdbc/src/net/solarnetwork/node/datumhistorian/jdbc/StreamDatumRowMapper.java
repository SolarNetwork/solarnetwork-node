/* ==================================================================
 * StreamDatumRowMapper.java - 13 Nov 2026 10:18:37 am
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
import static net.solarnetwork.dao.jdbc.CommonJdbcUtils.timestampInstant;
import static net.solarnetwork.node.service.support.DatumStreamIdUtils.encodeLocalStreamId;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.RowMapper;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.domain.datum.BasicStreamDatum;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider;
import net.solarnetwork.domain.datum.StreamDatum;
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectFilteredDatum;
import net.solarnetwork.node.service.support.DatumStreamIdUtils;

/**
 * Row mapper for {@link StreamDatum} instances.
 *
 * <p>
 * Expects the columns returned by a
 * {@link SelectFilteredDatum#forStream(String, net.solarnetwork.node.dao.NodeDatumFilter, Long, Integer[], int)}
 * statement:
 * </p>
 *
 * <ol>
 * <li>{@code stream_id}</li>
 * <li>{@code node_id}</li>
 * <li>{@code ts}</li>
 * <li>{@code data_i}</li>
 * <li>{@code data_a}</li>
 * <li>{@code data_s} (a JSON array)</li>
 * <li>{@code data_t}</li>
 * </ol>
 *
 * <p>
 * Each datum is given the UUID stream ID that {@link DatumStreamIdUtils}
 * encodes from its node ID, a historian domain, and its {@code stream_id}, as
 * {@link DatumStreamMetadataRowMapper} does. A row of a stream that the
 * metadata provider has no metadata for, such as a stream added after the
 * metadata was loaded, is mapped to {@code null}.
 * </p>
 *
 * <p>
 * Instantaneous and accumulating values are returned as {@link BigDecimal},
 * except {@code NaN} and infinite values, which a {@code BigDecimal} cannot
 * hold, so are returned as {@code null}. Status values are returned as strings,
 * as SolarNetwork returns them: a JSON string as its value, and any other JSON
 * value as its JSON text.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class StreamDatumRowMapper implements RowMapper<StreamDatum> {

	private final ObjectDatumStreamMetadataProvider metadataProvider;
	private final int streamIdDomain;
	private final ObjectMapper objectMapper;

	/**
	 * Constructor.
	 *
	 * @param metadataProvider
	 *        the provider of the metadata of the streams to map the rows of
	 * @param streamIdDomain
	 *        the historian domain to encode stream IDs with, from 1 to 255
	 * @param objectMapper
	 *        the object mapper to parse status values with
	 * @throws IllegalArgumentException
	 *         if {@code metadataProvider} or {@code objectMapper} is
	 *         {@code null}
	 */
	public StreamDatumRowMapper(ObjectDatumStreamMetadataProvider metadataProvider, int streamIdDomain,
			ObjectMapper objectMapper) {
		super();
		this.metadataProvider = requireNonNullArgument(metadataProvider, "metadataProvider");
		this.streamIdDomain = streamIdDomain;
		this.objectMapper = requireNonNullArgument(objectMapper, "objectMapper");
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return the datum, or {@code null} if the metadata provider has no
	 *         metadata for the stream of the row
	 */
	@Override
	public @Nullable StreamDatum mapRow(ResultSet rs, int rowNum) throws SQLException {
		final UUID streamId = encodeLocalStreamId(rs.getLong(2), streamIdDomain, rs.getInt(1));
		if ( metadataProvider.metadataForStreamId(streamId) == null ) {
			return null;
		}
		final Instant ts = timestampInstant(rs, 3);
		final DatumProperties props = DatumProperties.propertiesOf(getArray(rs, 4), getArray(rs, 5),
				statusArray(rs.getString(6)), getArray(rs, 7));
		return new BasicStreamDatum(streamId, ts, props);
	}

	private @Nullable String @Nullable [] statusArray(@Nullable String json) {
		if ( json == null ) {
			return null;
		}
		try (JsonParser parser = objectMapper.createParser(json)) {
			if ( parser.nextToken() != JsonToken.START_ARRAY ) {
				throw new DataRetrievalFailureException(
						"Stored datum status values [%s] are not a JSON array.".formatted(json));
			}
			final List<@Nullable String> result = new ArrayList<>(8);
			for ( JsonToken t = parser.nextToken(); t != null
					&& t != JsonToken.END_ARRAY; t = parser.nextToken() ) {
				result.add(switch (t) {
					case VALUE_NULL -> null;
					case START_OBJECT, START_ARRAY -> objectMapper.readTree(parser).toString();
					// the text of a number is as stored, unlike the text of a parsed number
					default -> parser.getText();
				});
			}
			return result.toArray(new @Nullable String[result.size()]);
		} catch ( IOException e ) {
			throw new DataRetrievalFailureException(
					"Stored datum status values [%s] are not valid JSON.".formatted(json), e);
		}
	}

}
