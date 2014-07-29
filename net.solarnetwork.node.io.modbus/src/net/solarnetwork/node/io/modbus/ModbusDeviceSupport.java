/* ==================================================================
 * ModbusDeviceSupport.java - Jul 29, 2014 2:29:54 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A base helper class to support {@link ModbusDevice} based services.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>modbusDevice</dt>
 * <dd>The {@link ModbusDevice} to use.</dd>
 * <dt>uid</dt>
 * <dd>A service name to use.</dd>
 * <dt>groupUID</dt>
 * <dd>A service group to use.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public abstract class ModbusDeviceSupport {

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

	private Map<String, Object> deviceInfo;
	private String uid;
	private String groupUID;
	private OptionalService<ModbusDevice> modbusDevice;

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Get the {@link ModbusDevice} from the configured {@code modbusDevice}
	 * service, or <em>null</em> if not available or not configured.
	 * 
	 * @return ModbusDevice
	 */
	protected final ModbusDevice modbusDevice() {
		return (modbusDevice == null ? null : modbusDevice.service());
	}

	/**
	 * Read general device info and return a map of the results. See the various
	 * {@code INFO_KEY_*} constants for information on the values returned in
	 * the result map.
	 * 
	 * @param conn
	 *        the connection to use
	 * @return a map with general device information populated
	 */
	protected abstract Map<String, Object> readDeviceInfo(ModbusConnection conn);

	/**
	 * Parse a big-endian 32-bit float value from a data array.
	 * 
	 * @param data
	 *        the data array
	 * @param offset
	 *        the offset within the array to parse the value from
	 * @return the float, or <em>null</em> if not available
	 */
	public static Float parseBigEndianFloat32(Integer[] data, int offset) {
		Float result = null;
		if ( data != null && offset >= 0 && data.length > (offset + 1) ) {
			result = ModbusHelper.parseFloat32(new Integer[] { data[offset], data[offset + 1] });
		}
		return result;
	}

	/**
	 * Parse a big-endian 64-bit integer value from a data array.
	 * 
	 * @param data
	 *        the data array
	 * @param offset
	 *        the offset within the array to parse the value from
	 * @return the long, or <em>null</em> if not available
	 */
	public static Long parseBigEndianInt64(Integer[] data, int offset) {
		Long result = null;
		if ( data != null && offset >= 0 && data.length > (offset + 3) ) {
			result = ModbusHelper.parseInt64(new Integer[] { data[offset], data[offset + 1],
					data[offset + 2], data[offset + 3] });
		}
		return result;
	}

	/**
	 * Return an informational message composed of general device info. This
	 * method will call {@link #getDeviceInfo()} and return a {@code /} (forward
	 * slash) delimited string of the resulting values, or <em>null</em> if that
	 * method returns <em>null</em>.
	 * 
	 * @return info message
	 */
	public String getDeviceInfoMessage() {
		Map<String, ?> info = getDeviceInfo();
		if ( info == null ) {
			return null;
		}
		return StringUtils.delimitedStringFromCollection(info.values(), " / ");
	}

	/**
	 * Get the device info data as a Map. This method will call
	 * {@link #readMeterInfo(ModbusConnection)}. The map is cached so subsequent
	 * calls will not attempt to read from the device. Note the returned map
	 * cannot be modified.
	 * 
	 * @return the device info, or <em>null</em>
	 * @see #readDeviceInfo(ModbusConnection)
	 */
	public Map<String, ?> getDeviceInfo() {
		Map<String, Object> info = deviceInfo;
		if ( info == null ) {
			try {
				info = performAction(new ModbusConnectionAction<Map<String, Object>>() {

					@Override
					public Map<String, Object> doWithConnection(ModbusConnection conn)
							throws IOException {
						return readDeviceInfo(conn);
					}
				});
				deviceInfo = info;
			} catch ( IOException e ) {
				log.warn("Communcation problem with {}: {}", uid, e.getMessage());
			}
		}
		return (info == null ? null : Collections.unmodifiableMap(info));
	}

	/**
	 * Perform some work with a Modbus {@link ModbusConnection}. This method
	 * attempts to obtain a {@link ModbusDevice} from the configured
	 * {@code modbusDevice} service, calling
	 * {@link ModbusDevice#performAction(ModbusConnectionAction)} if one can be
	 * obtained.
	 * 
	 * @param action
	 *        the connection action
	 * @return the result of the callback, or <em>null</em> if the action is
	 *         never invoked
	 */
	protected final <T> T performAction(ModbusConnectionAction<T> action) throws IOException {
		T result = null;
		ModbusDevice device = (modbusDevice == null ? null : modbusDevice.service());
		if ( device != null ) {
			result = device.performAction(action);
		}
		return result;
	}

	/**
	 * Get direct access to the device info data.
	 * 
	 * @return the device info, or <em>null</em>
	 */
	protected Map<String, Object> getDeviceInfoMap() {
		return deviceInfo;
	}

	/**
	 * Set the device info data. Setting the {@code deviceInfo} to <em>null</em>
	 * will force the next call to {@link #getDeviceInfo()} to read from the
	 * device to populate this data, and setting this to anything else will
	 * force all subsequent calls to {@link #getDeviceInfo()} to simply return
	 * that map.
	 * 
	 * @param deviceInfo
	 *        the device info map to set
	 */
	protected void setDeviceInfoMap(Map<String, Object> deviceInfo) {
		this.deviceInfo = deviceInfo;
	}

	/**
	 * Get a UID value. Returns {@link #getUid()}.
	 * 
	 * @return UID
	 */
	public String getUID() {
		return getUid();
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getGroupUID() {
		return groupUID;
	}

	public void setGroupUID(String groupUID) {
		this.groupUID = groupUID;
	}

	public OptionalService<ModbusDevice> getModbusDevice() {
		return modbusDevice;
	}

	public void setModbusDevice(OptionalService<ModbusDevice> modbusDevice) {
		this.modbusDevice = modbusDevice;
	}

}
