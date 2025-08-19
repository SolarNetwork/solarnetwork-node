/* ==================================================================
 * DatumControlCenterCsvColumn.java - 19/08/2025 10:51:33 am
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

package net.solarnetwork.node.io.dnp3.impl;

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
public enum DatumControlCenterCsvColumn implements CodedValue {

	/** The component instance ID. */
	INSTANCE_ID(0, "Instance ID"),

	/** The source ID. */
	CONNECTION_NAME(1, "Connection"),

	/** The event classes. */
	EVENT_CLASSES(2, "Event Classes"),

	/** The datum poll schedule. */
	SCHEDULE(3, "Schedule"),

	/** The datum source ID. */
	SOURCE_ID(4, "Source ID"),

	/** The generate-datum-on-events mode. */
	DATUM_EVENTS(5, "Datum Events"),

	/** A frequency at which to refresh DNP3 data. */
	POLL_FREQUENCY(6, "Poll Frequency"),

	/** The datum property name. */
	PROP_NAME(7, "Property"),

	/** The datum property type. */
	PROP_TYPE(8, "Property Type"),

	/** The register address. */
	MEASUREMENT_TYPE(9, "Measurement Type"),

	/** The register type. */
	INDEX(10, "Index"),

	/** A property value multiplier. */
	MULTIPLIER(11, "Multiplier"),

	/** A property value decimal scale. */
	DECIMAL_SCALE(12, "Decimal Scale"),

	;

	private final int idx;
	private final String name;

	private DatumControlCenterCsvColumn(int idx, String name) {
		this.idx = idx;
		this.name = name;
	}

	/**
	 * The set of columns are specify intance-wide settings.
	 */
	public static final Set<DatumControlCenterCsvColumn> INSTANCE_WIDE_COLUMNS = Collections
			.unmodifiableSet(EnumSet.of(INSTANCE_ID, CONNECTION_NAME, EVENT_CLASSES));

	/**
	 * The set of columns are specify datum-wide settings.
	 */
	public static final Set<DatumControlCenterCsvColumn> DATUM_WIDE_COLUMNS = Collections
			.unmodifiableSet(EnumSet.of(SCHEDULE, SOURCE_ID, DATUM_EVENTS, POLL_FREQUENCY));

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
