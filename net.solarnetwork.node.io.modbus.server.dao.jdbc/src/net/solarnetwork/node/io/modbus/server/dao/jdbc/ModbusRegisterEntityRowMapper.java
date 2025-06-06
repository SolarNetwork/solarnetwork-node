/* ==================================================================
 * ModbusRegisterEntityRowMapper.java - 4/11/2024 9:32:07 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus.server.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.node.io.modbus.ModbusRegisterBlockType;
import net.solarnetwork.node.io.modbus.server.dao.ModbusRegisterEntity;
import net.solarnetwork.node.io.modbus.server.dao.ModbusRegisterKey;

/**
 * Row mapper for {@link ModbusRegisterEntity} objects.
 *
 * @author matt
 * @version 1.1
 */
public class ModbusRegisterEntityRowMapper implements RowMapper<ModbusRegisterEntity> {

	/** A default instance. */
	public static final RowMapper<ModbusRegisterEntity> INSTANCE = new ModbusRegisterEntityRowMapper();

	/**
	 * Constructor.
	 */
	public ModbusRegisterEntityRowMapper() {
		super();
	}

	@Override
	public ModbusRegisterEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		String serverId = rs.getString(1);
		int unitId = Byte.toUnsignedInt(rs.getByte(2));
		ModbusRegisterBlockType blockType = ModbusRegisterBlockType
				.forCode(Byte.toUnsignedInt(rs.getByte(3)));
		int addr = rs.getInt(4);
		Timestamp created = rs.getTimestamp(5);
		Timestamp modified = rs.getTimestamp(6);
		short val = rs.getShort(7);
		ModbusRegisterEntity result = new ModbusRegisterEntity(
				new ModbusRegisterKey(serverId, unitId, blockType, addr), created.toInstant());
		result.setModified(modified.toInstant());
		result.setValue(val);
		return result;
	}

}
