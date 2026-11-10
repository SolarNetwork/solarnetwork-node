/* ==================================================================
 * DatumStreamInfoRowMapper.java - 10 Nov 2026 9:10:02 pm
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

import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.node.datumhistorian.jdbc.sql.SelectDatumStreams;

/**
 * Row mapper for {@link DatumStreamInfo} instances.
 *
 * <p>
 * Expects the columns returned by {@link SelectDatumStreams}:
 * </p>
 *
 * <ol>
 * <li>{@code stream_id}</li>
 * <li>{@code source_id}</li>
 * <li>{@code loc_id}</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 */
public final class DatumStreamInfoRowMapper implements RowMapper<DatumStreamInfo> {

	/** A default instance. */
	public static final RowMapper<DatumStreamInfo> INSTANCE = new DatumStreamInfoRowMapper();

	/**
	 * Constructor.
	 */
	public DatumStreamInfoRowMapper() {
		super();
	}

	@Override
	public DatumStreamInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
		int streamId = rs.getInt(1);
		String sourceId = rs.getString(2);
		long locId = rs.getLong(3);
		return new DatumStreamInfo(streamId, sourceId, rs.wasNull() ? null : locId);
	}

}
