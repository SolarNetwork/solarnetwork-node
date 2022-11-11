/* ==================================================================
 * BacnetControlCsvColumn.java - 10/11/2022 8:57:22 am
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

package net.solarnetwork.node.control.bacnet;

import net.solarnetwork.domain.CodedValue;

/**
 * The defined column order for BACnet Control CSV.
 * 
 * @author matt
 * @version 1.0
 */
public enum BacnetControlCsvColumn implements CodedValue {

	/** The instance ID. */
	INSTANCE_ID(0, "Instance ID"),

	/** The component service name. */
	SERVICE_NAME(1, "Service Name"),

	/** The component service group. */
	SERVICE_GROUP(2, "Service Group"),

	/** The connection name. */
	NETWORK_NAME(3, "Connection"),

	/** The sample cache milliseconds. */
	SAMPLE_CACHE(4, "Sample Cache"),

	/** The control ID. */
	CONTROL_ID(5, "Control ID"),

	/** The property type. */
	PROP_TYPE(6, "Property Type"),

	/** The device ID. */
	DEVICE_ID(7, "Device ID"),

	/** The object type. */
	OBJECT_TYPE(8, "Object Type"),

	/** The object number. */
	OBJECT_NUMBER(9, "Object Number"),

	/** The BACnet property ID (type). */
	PROPERTY_ID(10, "Property ID"),

	/** The unit multiplier. */
	MULTIPLIER(11, "Multiplier"),

	/** The decimal scale. */
	DECIMAL_SCALE(12, "Decimal Scale"),

	;

	private final int idx;
	private final String name;

	private BacnetControlCsvColumn(int idx, String name) {
		this.idx = idx;
		this.name = name;
	}

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
