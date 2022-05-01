/* ==================================================================
 * ModbusServerCsvColumn.java - 10/03/2022 9:30:49 AM
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus.server.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import net.solarnetwork.domain.CodedValue;

/**
 * The defined column order for Modbus Server CSV.
 * 
 * @author matt
 * @version 1.0
 */
public enum ModbusServerCsvColumn implements CodedValue {

	INSTANCE_ID(0, "Instance ID"),
	BIND_ADDRESS(1, "Bind Address"),
	PORT(2, "Port"),
	THROTTLE(3, "Throttle"),
	UNIT_ID(4, "Unit ID"),

	REG_TYPE(5, "Register Type"),
	REG_ADDR(6, "Register"),
	DATA_TYPE(7, "Data Type"),
	DATA_LENGTH(8, "Data Length"),
	SOURCE_ID(9, "Source ID"),
	PROPERTY(10, "Property"),
	MULTIPLIER(11, "Multiplier"),
	DECIMAL_SCALE(12, "Decimal Scale"),

	;

	private final int idx;
	private final String name;

	private ModbusServerCsvColumn(int idx, String name) {
		this.idx = idx;
		this.name = name;
	}

	/**
	 * The set of columns are specify server-wide settings.
	 */
	public static final Set<ModbusServerCsvColumn> SERVER_WIDE_COLUMNS = Collections
			.unmodifiableSet(EnumSet.of(INSTANCE_ID, BIND_ADDRESS, PORT, THROTTLE));

	@Override
	public int getCode() {
		return idx;
	}

	/**
	 * Get a friendly name for the column.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

}
