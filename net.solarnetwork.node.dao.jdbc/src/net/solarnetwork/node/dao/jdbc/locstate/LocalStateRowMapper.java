/* ==================================================================
 * LocalStateRowMapper.java - 14/04/2025 11:36:49 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.jdbc.locstate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.node.dao.jdbc.JdbcUtils;
import net.solarnetwork.node.domain.LocalState;
import net.solarnetwork.node.domain.LocalStateType;
import net.solarnetwork.node.domain.SecurityToken;

/**
 * Row mapper for {@link SecurityToken} entities.
 *
 * <p>
 * Expects column in the following order:
 * </p>
 *
 * <ol>
 * <li>skey</li>
 * <li>created</li>
 * <li>modified</li>
 * <li>stype</li>
 * <li>sdata</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 * @since 3.4
 */
public class LocalStateRowMapper implements RowMapper<LocalState> {

	/** A default instance. */
	public static final RowMapper<LocalState> INSTANCE = new LocalStateRowMapper();

	/**
	 * Constructor.
	 */
	public LocalStateRowMapper() {
		super();
	}

	@Override
	public LocalState mapRow(ResultSet rs, int rowNum) throws SQLException {
		String key = rs.getString(1);
		Instant created = JdbcUtils.getUtcTimestampColumnValue(rs, 2);
		Instant modified = JdbcUtils.getUtcTimestampColumnValue(rs, 3);
		String typeKey = rs.getString(4);
		byte[] data = rs.getBytes(5);
		LocalState state = new LocalState(key, created);
		state.setModified(modified);
		state.setType(LocalStateType.forKey(typeKey));
		state.setData(data);
		return state;
	}

}
