/* ==================================================================
 * WMBusCsvColumn.java - 30/09/2022 11:21:21 am
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

package net.solarnetwork.node.datum.mbus;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import net.solarnetwork.domain.CodedValue;

/**
 * The defined column order for Wireless M-Bus Device CSV.
 * 
 * @author matt
 * @version 1.0
 * @since 1.1
 */
public enum WMBusCsvColumn implements CodedValue {

	/** The component instance ID. */
	INSTANCE_ID(0, "Instance ID"),

	/** The service name. */
	SERVICE_NAME(1, "Service Name"),

	/** The service name. */
	SERVICE_GROUP(2, "Service Group"),

	/** The source ID. */
	SOURCE_ID(3, "Source ID"),

	/** The poll schedule. */
	SCHEDULE(4, "Schedule"),

	/** The M-Bus network name. */
	NETWORK_NAME(5, "Connection"),

	/** The secondary Wireless M-Bus address, in hex format. */
	ADDRESS(6, "Address"),

	/** The Wireless M-Bus decryption key, in hex format. */
	KEY(7, "Key"),

	/** The datum property name. */
	PROP_NAME(8, "Property"),

	/** The datum property type. */
	PROP_TYPE(9, "Property Type"),

	/**
	 * The M-Bus data type ({@link net.solarnetwork.node.io.mbus.MBusDataType}).
	 */
	DATA_TYPE(10, "Data Type"),

	/**
	 * The M-Bus data description
	 * ({@link net.solarnetwork.node.io.mbus.MBusDataDescription}).
	 */
	DATA_DESCRIPTION(11, "Data Description"),

	/** The property value multiplier. */
	MULTIPLIER(12, "Multiplier"),

	/** The property value decimal scale. */
	DECIMAL_SCALE(13, "Decimal Scale"),

	;

	private final int idx;
	private final String name;

	private WMBusCsvColumn(int idx, String name) {
		this.idx = idx;
		this.name = name;
	}

	/**
	 * The set of columns are specify device-wide settings.
	 */
	public static final Set<WMBusCsvColumn> DEVICE_WIDE_COLUMNS = Collections.unmodifiableSet(EnumSet.of(
			INSTANCE_ID, SERVICE_NAME, SERVICE_GROUP, SOURCE_ID, SCHEDULE, NETWORK_NAME, ADDRESS, KEY));

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
