/* ==================================================================
 * StoredDatumRowMapper.java - 10 Nov 2026 9:12:55 pm
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

import static net.solarnetwork.dao.jdbc.CommonJdbcUtils.timestampInstant;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.RowMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectDatum;
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectFilteredDatum;

/**
 * Row mapper for {@link StoredDatum} instances.
 *
 * <p>
 * Expects the columns returned by {@link SelectDatum}:
 * </p>
 *
 * <ol>
 * <li>{@code stream_id}</li>
 * <li>{@code source_id}</li>
 * <li>{@code loc_id}</li>
 * <li>{@code ts}</li>
 * <li>{@code class_name}</li>
 * <li>{@code type_names}</li>
 * <li>{@code names_i}</li>
 * <li>{@code names_a}</li>
 * <li>{@code names_s}</li>
 * <li>{@code data_i} (as text)</li>
 * <li>{@code data_a} (as text)</li>
 * <li>{@code data_s} (a JSON array)</li>
 * <li>{@code data_t}</li>
 * <li>{@code node_id} (only when created to read node IDs, as returned by
 * {@link SelectFilteredDatum})</li>
 * </ol>
 *
 * <p>
 * Instantaneous and accumulating values are returned as {@link BigDecimal},
 * except {@code NaN} and infinite values, which are returned as {@link Double}.
 * Status values keep their JSON type: strings, numbers, and booleans.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class StoredDatumRowMapper implements RowMapper<StoredDatum> {

	private final ObjectMapper objectMapper;
	private final boolean withNodeId;

	/**
	 * Constructor.
	 *
	 * <p>
	 * Node IDs will not be read.
	 * </p>
	 *
	 * @param objectMapper
	 *        the object mapper to parse status values with
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public StoredDatumRowMapper(ObjectMapper objectMapper) {
		this(objectMapper, false);
	}

	/**
	 * Constructor.
	 *
	 * @param objectMapper
	 *        the object mapper to parse status values with
	 * @param withNodeId
	 *        {@code true} to read the node ID from the {@code node_id} column
	 *        that follows the {@code data_t} column
	 * @throws IllegalArgumentException
	 *         if {@code objectMapper} is {@code null}
	 */
	public StoredDatumRowMapper(ObjectMapper objectMapper, boolean withNodeId) {
		super();
		this.objectMapper = requireNonNullArgument(objectMapper, "objectMapper");
		this.withNodeId = withNodeId;
	}

	@Override
	public StoredDatum mapRow(ResultSet rs, int rowNum) throws SQLException {
		final int streamId = rs.getInt(1);
		final String sourceId = rs.getString(2);
		final Long locationId = rs.getObject(3, Long.class);
		final Instant ts = timestampInstant(rs, 4);
		final String className = rs.getString(5);
		final String[] typeNames = stringArray(rs.getArray(6));
		final String[] namesI = stringArray(rs.getArray(7));
		final String[] namesA = stringArray(rs.getArray(8));
		final String[] namesS = stringArray(rs.getArray(9));
		final String[] dataI = stringArray(rs.getArray(10));
		final String[] dataA = stringArray(rs.getArray(11));
		final String dataS = rs.getString(12);
		final String[] dataT = stringArray(rs.getArray(13));
		Long nodeId = null;
		if ( withNodeId ) {
			nodeId = rs.getObject(14, Long.class);
		}

		final DatumSamples s = new DatumSamples();
		for ( int i = 0, len = Math.min(namesI.length, dataI.length); i < len; i++ ) {
			if ( dataI[i] != null ) {
				s.putInstantaneousSampleValue(namesI[i], number(dataI[i]));
			}
		}
		for ( int i = 0, len = Math.min(namesA.length, dataA.length); i < len; i++ ) {
			if ( dataA[i] != null ) {
				s.putAccumulatingSampleValue(namesA[i], number(dataA[i]));
			}
		}
		if ( dataS != null ) {
			final JsonNode array = statusArray(dataS);
			for ( int i = 0, len = Math.min(namesS.length, array.size()); i < len; i++ ) {
				final Object v = statusValue(array.get(i));
				if ( v != null ) {
					s.putStatusSampleValue(namesS[i], v);
				}
			}
		}
		for ( String tag : dataT ) {
			if ( tag != null ) {
				s.addTag(tag);
			}
		}

		final Set<String> types = new LinkedHashSet<>(Arrays.asList(typeNames));
		return new StoredDatum(streamId, nodeId, sourceId, locationId, ts, className, types, s);
	}

	private static String[] stringArray(@Nullable Array array) throws SQLException {
		if ( array == null ) {
			return new String[0];
		}
		try {
			return (String[]) array.getArray();
		} finally {
			array.free();
		}
	}

	/**
	 * Parse a number value.
	 *
	 * @param value
	 *        the value to parse, in the text form of a Postgres {@code NUMERIC}
	 * @return the number
	 * @throws DataRetrievalFailureException
	 *         if {@code value} is not a number
	 */
	public static Number number(String value) {
		return switch (value) {
			case "NaN" -> Double.NaN;
			case "Infinity" -> Double.POSITIVE_INFINITY;
			case "-Infinity" -> Double.NEGATIVE_INFINITY;
			default -> {
				try {
					yield new BigDecimal(value);
				} catch ( NumberFormatException e ) {
					throw new DataRetrievalFailureException(
							"Stored datum value [%s] is not a number.".formatted(value), e);
				}
			}
		};
	}

	private JsonNode statusArray(String json) {
		try {
			final JsonNode node = objectMapper.readTree(json);
			if ( !node.isArray() ) {
				throw new DataRetrievalFailureException(
						"Stored datum status values [%s] are not a JSON array.".formatted(json));
			}
			return node;
		} catch ( JsonProcessingException e ) {
			throw new DataRetrievalFailureException(
					"Stored datum status values [%s] are not valid JSON.".formatted(json), e);
		}
	}

	private @Nullable Object statusValue(@Nullable JsonNode node) {
		if ( node == null || node.isNull() ) {
			return null;
		}
		if ( node.isTextual() ) {
			return node.textValue();
		}
		if ( node.isNumber() ) {
			return node.numberValue();
		}
		if ( node.isBoolean() ) {
			return node.booleanValue();
		}
		try {
			return objectMapper.treeToValue(node, Object.class);
		} catch ( JsonProcessingException e ) {
			throw new DataRetrievalFailureException(
					"Stored datum status value [%s] is not supported.".formatted(node), e);
		}
	}

}
