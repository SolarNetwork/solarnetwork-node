/* ==================================================================
 * ModbusRegisterKey.java - 4/11/2024 8:47:38 am
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

package net.solarnetwork.node.io.modbus.server.dao;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serializable;
import java.util.Objects;
import net.solarnetwork.node.io.modbus.ModbusRegisterBlockType;

/**
 * A primary key for modbus register data.
 *
 * @author matt
 * @version 1.0
 */
public final class ModbusRegisterKey implements Comparable<ModbusRegisterKey>, Serializable {

	private static final long serialVersionUID = 1261700403455906179L;

	private final String serverId;
	private final int unitId;
	private final ModbusRegisterBlockType blockType;
	private final int address;

	/**
	 * Constructor.
	 *
	 * @param serverId
	 *        the server identifier
	 * @param unitId
	 *        the unit ID
	 * @param blockType
	 *        the block type
	 * @param address
	 *        the register address
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ModbusRegisterKey(String serverId, int unitId, ModbusRegisterBlockType blockType,
			int address) {
		super();
		this.serverId = requireNonNullArgument(serverId, "serverId");
		this.unitId = unitId;
		this.blockType = requireNonNullArgument(blockType, "blockType");
		this.address = address;
	}

	@Override
	public int hashCode() {
		return Objects.hash(address, blockType, serverId, unitId);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof ModbusRegisterKey) ) {
			return false;
		}
		ModbusRegisterKey other = (ModbusRegisterKey) obj;
		return address == other.address && blockType == other.blockType
				&& Objects.equals(serverId, other.serverId) && unitId == other.unitId;
	}

	@Override
	public int compareTo(ModbusRegisterKey o) {
		if ( o == null ) {
			return 1;
		}
		int result = serverId.compareTo(o.serverId);
		if ( result == 0 ) {
			result = Integer.compare(unitId, o.unitId);
			if ( result == 0 ) {
				result = blockType.compareTo(o.blockType);
				if ( result == 0 ) {
					result = Integer.compare(address, o.address);
				}
			}
		}
		return result;
	}

	/**
	 * Get the server identifier.
	 *
	 * @return the server identifier
	 */
	public String getServerId() {
		return serverId;
	}

	/**
	 * Get the unit ID.
	 *
	 * @return the unit ID
	 */
	public int getUnitId() {
		return unitId;
	}

	/**
	 * Get the block type.
	 *
	 * @return the block type
	 */
	public ModbusRegisterBlockType getBlockType() {
		return blockType;
	}

	/**
	 * Get the register address.
	 *
	 * @return the register address
	 */
	public int getAddress() {
		return address;
	}

}
