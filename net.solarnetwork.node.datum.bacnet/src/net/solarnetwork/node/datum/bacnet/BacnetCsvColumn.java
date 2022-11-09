/* ==================================================================
 * BacnetCsvColumn.java - 10/03/2022 9:30:49 AM
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

package net.solarnetwork.node.datum.bacnet;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import net.solarnetwork.domain.CodedValue;

/**
 * The defined column order for BACnet Device CSV.
 * 
 * @author matt
 * @version 1.0
 */
public enum BacnetCsvColumn implements CodedValue {

	/** The component instance ID. */
	INSTANCE_ID(0, "Instance ID"),

	/** The component service name. */
	SERVICE_NAME(1, "Service Name"),

	/** The component service group. */
	SERVICE_GROUP(2, "Service Group"),

	/** The source ID. */
	SOURCE_ID(3, "Source ID"),

	/** The schedule. */
	SCHEDULE(4, "Schedule"),

	/** The network connection service name. */
	NETWORK_NAME(5, "Connection"),

	/** The persist mode. */
	PERSIST_MODE(6, "Persist Mode"),

	/** The sample cache time. */
	SAMPLE_CACHE(7, "Sample Cache"),

	/** The device ID. */
	DEVICE_ID(8, "Device ID"),

	/** The object type. */
	OBJECT_TYPE(9, "Object Type"),

	/** The object number. */
	OBJECT_NUMBER(10, "Object Number"),

	/** The BACnet property ID (type). */
	PROPERTY_ID(11, "Property ID"),

	/** The change-of-value increment. */
	COV_INCREMENT(12, "COV Increment"),

	/** The datum property name. */
	PROP_NAME(13, "Property"),

	/** The datum property type. */
	PROP_TYPE(14, "Property Type"),

	/** A property value multipler. */
	MULTIPLIER(15, "Multiplier"),

	/** A property value decimal scale. */
	DECIMAL_SCALE(16, "Decimal Scale"),

	;

	private final int idx;
	private final String name;

	private BacnetCsvColumn(int idx, String name) {
		this.idx = idx;
		this.name = name;
	}

	/**
	 * The set of columns are specify device-wide settings.
	 */
	public static final Set<BacnetCsvColumn> DEVICE_WIDE_COLUMNS = Collections
			.unmodifiableSet(EnumSet.of(INSTANCE_ID, SERVICE_NAME, SERVICE_GROUP, SOURCE_ID, SCHEDULE,
					NETWORK_NAME, PERSIST_MODE, SAMPLE_CACHE));

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
