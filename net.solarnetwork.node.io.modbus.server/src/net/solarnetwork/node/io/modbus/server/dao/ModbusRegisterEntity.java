/* ==================================================================
 * ModbusRegisterEntity.java - 4/11/2024 8:44:43 am
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

import java.time.Instant;
import net.solarnetwork.dao.BasicEntity;
import net.solarnetwork.node.io.modbus.ModbusRegisterBlockType;
import net.solarnetwork.util.ObjectUtils;

/**
 * A Modbus register entity.
 *
 * @author matt
 * @version 1.0
 */
public final class ModbusRegisterEntity extends BasicEntity<ModbusRegisterKey> implements Cloneable {

	private static final long serialVersionUID = 4674814596838327572L;

	private Instant modified;
	private short value;

	/**
	 * Create a new entity instance.
	 *
	 * @param serverId
	 *        the server identifier
	 * @param unitId
	 *        the unit ID
	 * @param blockType
	 *        the block type
	 * @param address
	 *        the register address
	 * @param timestamp
	 *        the timestamp
	 * @param value
	 *        the value
	 * @return the new instance
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public static ModbusRegisterEntity newRegisterEntity(String serverId, int unitId,
			ModbusRegisterBlockType blockType, int address, Instant timestamp, short value) {
		ModbusRegisterEntity result = new ModbusRegisterEntity(
				new ModbusRegisterKey(serverId, unitId, blockType, address), timestamp);
		result.setModified(timestamp);
		result.setValue(value);
		return result;
	}

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the primary key
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ModbusRegisterEntity(ModbusRegisterKey id, Instant created) {
		super(ObjectUtils.requireNonNullArgument(id, "id"), created);
	}

	@Override
	public ModbusRegisterEntity clone() {
		return (ModbusRegisterEntity) super.clone();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ModbusRegisterEntity{serverId=");
		builder.append(getServerId());
		builder.append(", unitId=");
		builder.append(getUnitId());
		builder.append(", blockType=");
		builder.append(getBlockType());
		builder.append(", address=");
		builder.append(getAddress());
		builder.append(", modified=");
		builder.append(modified);
		builder.append(", value=");
		builder.append(String.format("0x%04X", Short.toUnsignedInt(value)));
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the server identifier.
	 *
	 * @return the server identifier
	 */
	public String getServerId() {
		return getId().getServerId();
	}

	/**
	 * Get the unit ID.
	 *
	 * @return the unit ID
	 */
	public int getUnitId() {
		return getId().getUnitId();
	}

	/**
	 * Get the block type.
	 *
	 * @return the block type
	 */
	public ModbusRegisterBlockType getBlockType() {
		return getId().getBlockType();
	}

	/**
	 * Get the register address.
	 *
	 * @return the register address
	 */
	public int getAddress() {
		return getId().getAddress();
	}

	/**
	 * Get the modification date.
	 *
	 * @return the modified date, or {@literal null}
	 */
	public Instant getModified() {
		return modified;
	}

	/**
	 * Set the modification date.
	 *
	 * @param modified
	 *        the modified to set
	 */
	public void setModified(Instant modified) {
		this.modified = modified;
	}

	/**
	 * Get the register value.
	 *
	 * @return the value
	 */
	public short getValue() {
		return value;
	}

	/**
	 * Set the register value.
	 *
	 * @param value
	 *        the value to set
	 */
	public void setValue(short value) {
		this.value = value;
	}

}
