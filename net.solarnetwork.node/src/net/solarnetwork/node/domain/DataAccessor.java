/* ==================================================================
 * DataAccessor.java - 30/07/2018 9:22:08 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.domain;

import java.util.Map;

/**
 * API for accessing photovoltaic energy properties from a snapshot of data
 * captured from a device.
 * 
 * @author matt
 * @version 1.0
 * @since 1.60
 */
public interface DataAccessor {

	/** Key for the device name, as a String. */
	public static final String INFO_KEY_DEVICE_NAME = "Name";

	/** Key for the device model, as a String. */
	public static final String INFO_KEY_DEVICE_MODEL = "Model";

	/** Key for the device serial number, as a Long. */
	public static final String INFO_KEY_DEVICE_SERIAL_NUMBER = "Serial Number";

	/** Key for the device manufacturer, as a String. */
	public static final String INFO_KEY_DEVICE_MANUFACTURER = "Manufacturer";

	/**
	 * Key for the device manufacture date, as a
	 * {@link org.joda.time.ReadablePartial}.
	 */
	public static final String INFO_KEY_DEVICE_MANUFACTURE_DATE = "Manufacture Date";

	/**
	 * Gets the time stamp of the data.
	 * 
	 * @return the data time stamp
	 */
	long getDataTimestamp();

	/**
	 * Get descriptive information about the device the data was captured from.
	 * 
	 * <p>
	 * The various {@literal INFO_*} constants defined on this interface provide
	 * some standardized keys to use in the returned map.
	 * </p>
	 * 
	 * @return a map of device information, never {@literal null}
	 */
	Map<String, Object> getDeviceInfo();

}
