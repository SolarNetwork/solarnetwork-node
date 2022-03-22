/* ==================================================================
 * ModbusCsvColumn.java - 10/03/2022 9:30:49 AM
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

package net.solarnetwork.node.datum.modbus;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import net.solarnetwork.domain.CodedValue;

/**
 * The defined column order for Modbus Device CSV.
 * 
 * @author matt
 * @version 1.0
 */
public enum ModbusCsvColumn implements CodedValue {

	INSTANCE_ID(0, "Instance ID"),
	SOURCE_ID(1, "Source ID"),
	SCHEDULE(2, "Schedule"),
	NETWORK_NAME(3, "Connection"),
	UNIT_ID(4, "Unit ID"),
	SAMPLE_CACHE(5, "Sample Cache"),
	MAX_WORD_COUNT(6, "Max Read"),
	WORD_ORDER(7, "Word Order"),

	PROP_NAME(8, "Property"),
	PROP_TYPE(9, "Property Type"),
	REG_ADDR(10, "Register"),
	REG_TYPE(11, "Register Type"),
	DATA_TYPE(12, "Data Type"),
	DATA_LENGTH(13, "Data Length"),
	MULTIPLIER(14, "Multiplier"),
	DECIMAL_SCALE(15, "Decimal Scale"),
	EXPRESSION(16, "Expression"),

	;

	private final int idx;
	private final String name;

	private ModbusCsvColumn(int idx, String name) {
		this.idx = idx;
		this.name = name;
	}

	/**
	 * The set of columns are specify device-wide settings.
	 */
	public static final Set<ModbusCsvColumn> DEVICE_WIDE_COLUMNS = Collections
			.unmodifiableSet(EnumSet.of(INSTANCE_ID, SOURCE_ID, SCHEDULE, NETWORK_NAME, UNIT_ID,
					SAMPLE_CACHE, MAX_WORD_COUNT, WORD_ORDER));

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
