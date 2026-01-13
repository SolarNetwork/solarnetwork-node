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
 * @version 1.1
 */
public enum ModbusServerCsvColumn implements CodedValue {

	/** The component instance ID. */
	INSTANCE_ID(0, "Instance ID"),

	/** The bind address. */
	BIND_ADDRESS(1, "Bind Address"),

	/** The IP port. */
	PORT(2, "Port"),

	/** The throttle time. */
	THROTTLE(3, "Throttle"),

	/** The unit ID. */
	UNIT_ID(4, "Unit ID"),

	/** The register type. */
	REG_TYPE(5, "Register Type"),

	/** The register address. */
	REG_ADDR(6, "Register"),

	/** The data type. */
	DATA_TYPE(7, "Data Type"),

	/** The data length. */
	DATA_LENGTH(8, "Data Length"),

	/** The source ID. */
	SOURCE_ID(9, "Source ID"),

	/** The datum property name. */
	PROPERTY(10, "Property"),

	/** A value multiplier. */
	MULTIPLIER(11, "Multiplier"),

	/** A value decimal scale. */
	DECIMAL_SCALE(12, "Decimal Scale"),

	/**
	 * An optional control ID.
	 *
	 * @since 1.1
	 */
	CONTROL_ID(13, "Control ID"),

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
