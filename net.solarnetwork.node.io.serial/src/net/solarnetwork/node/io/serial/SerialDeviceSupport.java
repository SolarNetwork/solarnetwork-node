/* ==================================================================
 * SerialDeviceSupport.java - 25/10/2014 7:25:24 AM
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

package net.solarnetwork.node.io.serial;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A base helper class to support {@link SerialNetwork} based services.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>serialNetwork</dt>
 * <dd>The {@link SerialNetwork} to use.</dd>
 * 
 * <dt>uid</dt>
 * <dd>A service name to use.</dd>
 * 
 * <dt>groupUID</dt>
 * <dd>A service group to use.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public abstract class SerialDeviceSupport {

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
	private OptionalService<SerialNetwork> serialNetwork;

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Get the {@link SerialNetwork} from the configured {@code serialNetwork}
	 * service, or <em>null</em> if not available or not configured.
	 * 
	 * @return SerialNetwork
	 */
	protected final SerialNetwork serialNetwork() {
		return (serialNetwork == null ? null : serialNetwork.service());
	}

	/**
	 * Read general device info and return a map of the results. See the various
	 * {@code INFO_KEY_*} constants for information on the values returned in
	 * the result map.
	 * 
	 * @param conn
	 *        the connection to use
	 * @return a map with general device information populated
	 * @throws IOException
	 *         if any IO error occurrs
	 */
	protected abstract Map<String, Object> readDeviceInfo(SerialConnection conn) throws IOException;

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
	 * {@link #readMeterInfo(SerialConnection)}. The map is cached so subsequent
	 * calls will not attempt to read from the device. Note the returned map
	 * cannot be modified.
	 * 
	 * @return the device info, or <em>null</em>
	 * @see #readDeviceInfo(SerialConnection)
	 */
	public Map<String, ?> getDeviceInfo() {
		Map<String, Object> info = deviceInfo;
		if ( info == null ) {
			try {
				info = performAction(new SerialConnectionAction<Map<String, Object>>() {

					@Override
					public Map<String, Object> doWithConnection(SerialConnection conn)
							throws IOException {
						return readDeviceInfo(conn);
					}
				});
				deviceInfo = info;
			} catch ( Exception e ) {
				log.warn("Communcation problem with {}: {}", uid, e.getMessage());
			}
		}
		return (info == null ? null : Collections.unmodifiableMap(info));
	}

	/**
	 * Perform some work with a Serial {@link SerialConnection}. This method
	 * attempts to obtain a {@link SerialNetwork} from the configured
	 * {@code serialNetwork} service, calling
	 * {@link SerialNetwork#performAction(SerialConnectionAction)} if one can be
	 * obtained.
	 * 
	 * @param action
	 *        the connection action
	 * @return the result of the callback, or <em>null</em> if the action is
	 *         never invoked
	 */
	protected final <T> T performAction(final SerialConnectionAction<T> action) throws IOException {
		T result = null;
		SerialNetwork device = (serialNetwork == null ? null : serialNetwork.service());
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

	public OptionalService<SerialNetwork> getSerialNetwork() {
		return serialNetwork;
	}

	public void setSerialNetwork(OptionalService<SerialNetwork> serialDevice) {
		this.serialNetwork = serialDevice;
	}

}
