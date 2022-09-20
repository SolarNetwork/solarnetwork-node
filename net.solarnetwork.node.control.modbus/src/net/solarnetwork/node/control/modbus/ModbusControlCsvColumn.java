/* ==================================================================
 * ModbusControlCsvColumn.java - 10/03/2022 9:30:49 AM
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

package net.solarnetwork.node.control.modbus;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import net.solarnetwork.domain.CodedValue;

/**
 * The defined column order for Modbus Control CSV.
 * 
 * @author matt
 * @version 1.0
 */
public enum ModbusControlCsvColumn implements CodedValue {

	/** The instance ID. */
	INSTANCE_ID(0, "Instance ID"),

	/** The connection name. */
	NETWORK_NAME(1, "Connection"),

	/** The unit ID. */
	UNIT_ID(2, "Unit ID"),

	/** The sample cache milliseconds. */
	SAMPLE_CACHE(3, "Sample Cache"),

	/** The word order. */
	WORD_ORDER(4, "Word Order"),

	/** The control ID. */
	CONTROL_ID(5, "Control ID"),

	/** The property type. */
	PROP_TYPE(6, "Property Type"),

	/** The register address. */
	REG_ADDR(7, "Register"),

	/** The register type. */
	REG_TYPE(8, "Register Type"),

	/** The data type. */
	DATA_TYPE(9, "Data Type"),

	/** The data length. */
	DATA_LENGTH(10, "Data Length"),

	/** The unit multiplier. */
	MULTIPLIER(11, "Multiplier"),

	/** The decimal scale. */
	DECIMAL_SCALE(12, "Decimal Scale"),

	;

	private final int idx;
	private final String name;

	private ModbusControlCsvColumn(int idx, String name) {
		this.idx = idx;
		this.name = name;
	}

	/**
	 * The set of columns are specify device-wide settings.
	 */
	public static final Set<ModbusControlCsvColumn> DEVICE_WIDE_COLUMNS = Collections
			.unmodifiableSet(EnumSet.of(INSTANCE_ID, NETWORK_NAME, UNIT_ID, SAMPLE_CACHE, WORD_ORDER));

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
