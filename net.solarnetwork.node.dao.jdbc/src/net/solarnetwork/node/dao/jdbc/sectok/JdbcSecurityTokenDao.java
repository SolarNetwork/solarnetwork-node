/* ==================================================================
 * Jdbc.java - 6/09/2023 3:04:33 pm
 *
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.jdbc.sectok;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import net.solarnetwork.node.dao.SecurityTokenDao;
import net.solarnetwork.node.dao.jdbc.BaseJdbcGenericDao;
import net.solarnetwork.node.dao.jdbc.JdbcUtils;
import net.solarnetwork.node.domain.SecurityToken;

/**
 * JDBC implementation of {@link SecurityTokenDao}.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcSecurityTokenDao extends BaseJdbcGenericDao<SecurityToken, String>
		implements SecurityTokenDao {

	/** The table name for {@link SecurityToken} entities. */
	public static final String TABLE_NAME = "sectok";

	/** The charge point table version. */
	public static final int VERSION = 1;

	/**
	 * Constructor.
	 */
	public JdbcSecurityTokenDao() {
		super(SecurityToken.class, String.class, SecurityTokenRowMapper.INSTANCE, "sn_%s", TABLE_NAME,
				VERSION);
	}

	@Override
	protected void setStoreStatementValues(SecurityToken obj, PreparedStatement ps) throws SQLException {
		ps.setString(1, obj.getId());
		JdbcUtils.setUtcTimestampStatementValue(ps, 2,
				obj.getCreated() != null ? obj.getCreated() : Instant.now());
		obj.copySecret(secret -> {
			try {
				ps.setString(3, secret);
			} catch ( SQLException e ) {
				throw new RuntimeException(e);
			}
		});
		setUpdateStatementValues(obj, ps, 3);
	}

	@Override
	protected void setUpdateStatementValues(SecurityToken obj, PreparedStatement ps)
			throws SQLException {
		setUpdateStatementValues(obj, ps, 0);
		ps.setObject(3, obj.getId());
	}

	private void setUpdateStatementValues(SecurityToken obj, PreparedStatement ps, int offset)
			throws SQLException {
		ps.setString(1 + offset, obj.getName());
		ps.setString(2 + offset, obj.getDescription());
	}
}
