/* ==================================================================
 * SecurityTokenRowMapper.java - 6/09/2023 3:29:28 pm
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

import static net.solarnetwork.node.dao.jdbc.BaseJdbcGenericDao.getInstantColumn;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.node.domain.SecurityToken;

/**
 * Row mapper for {@link SecurityToken} entities.
 * 
 * <p>
 * Expects column in the following order:
 * </p>
 * 
 * <ol>
 * <li>id</li>
 * <li>created</li>
 * <li>tok_sec</li>
 * <li>disp_name</li>
 * <li>description</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 */
public class SecurityTokenRowMapper implements RowMapper<SecurityToken> {

	/** A default instance. */
	public static final RowMapper<SecurityToken> INSTANCE = new SecurityTokenRowMapper();

	@Override
	public SecurityToken mapRow(ResultSet rs, int rowNum) throws SQLException {
		String tokenId = rs.getString(1);
		Instant created = getInstantColumn(rs, 2);
		String tokenSecret = rs.getString(3);
		String name = rs.getString(4);
		String description = rs.getString(5);
		SecurityToken token = new SecurityToken(tokenId, created, tokenSecret, name, description);
		if ( tokenSecret.isEmpty() ) {
			token = token.copyWithoutSecret(null, null);
		}
		return token;
	}

}
