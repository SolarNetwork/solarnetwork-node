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

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Map;
import net.solarnetwork.domain.BasicDeviceInfo;
import net.solarnetwork.domain.DeviceInfo;

/**
 * API for accessing properties from a snapshot of data captured from a device.
 * 
 * @author matt
 * @version 2.0
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
	 * @return the data time stamp, or {@literal null} if no data has been
	 *         collected yet
	 */
	Instant getDataTimestamp();

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

	/**
	 * Get a {@link DeviceInfo} instance based on the {@link #getDeviceInfo()}
	 * data.
	 * 
	 * @return the device info, or {@literal null} if no device properties are
	 *         available
	 * @since 1.1
	 */
	default DeviceInfo deviceInfo() {
		BasicDeviceInfo.Builder b = deviceInfoBuilder();
		return (b.isEmpty() ? null : b.build());
	}

	/**
	 * Get a {@link BasicDeviceInfo} builder, populated from
	 * {@link #getDeviceInfo()}.
	 * 
	 * @return the builder, never {@literal null}
	 * @since 1.1
	 */
	default BasicDeviceInfo.Builder deviceInfoBuilder() {
		Map<String, Object> info = getDeviceInfo();
		return deviceInfoBuilderForInfo(info);
	}

	/**
	 * Get a {@link BasicDeviceInfo} builder, populated from an info map, like
	 * those returned from {@link DataAccessor#getDeviceInfo()}.
	 * 
	 * @param info
	 *        the info to extract device properties from
	 * @return the builder, never {@literal null}
	 * @since 1.1
	 */
	static BasicDeviceInfo.Builder deviceInfoBuilderForInfo(Map<String, ?> info) {
		BasicDeviceInfo.Builder b = BasicDeviceInfo.builder();
		if ( info != null && !info.isEmpty() ) {
			Object o = info.get(INFO_KEY_DEVICE_NAME);
			if ( o != null ) {
				b.withName(o.toString());
			}
			o = info.get(INFO_KEY_DEVICE_MANUFACTURER);
			if ( o != null ) {
				b.withManufacturer(o.toString());
			}
			o = info.get(INFO_KEY_DEVICE_MODEL);
			if ( o != null ) {
				b.withModelName(o.toString());
			}
			o = info.get(INFO_KEY_DEVICE_SERIAL_NUMBER);
			if ( o != null ) {
				b.withVersion(o.toString());
			}
			o = info.get(INFO_KEY_DEVICE_MANUFACTURE_DATE);
			if ( o instanceof TemporalAccessor ) {
				TemporalAccessor p = (TemporalAccessor) o;
				try {
					b.withManufactureDate(LocalDate.of(p.get(ChronoField.YEAR),
							p.get(ChronoField.MONTH_OF_YEAR), p.get(ChronoField.DAY_OF_MONTH)));
				} catch ( DateTimeException e ) {
					// ignore
				}
			}

		}
		return b;
	}

}
