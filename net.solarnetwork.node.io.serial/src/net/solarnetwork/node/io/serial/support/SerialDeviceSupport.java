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

package net.solarnetwork.node.io.serial.support;

import static java.util.Collections.singletonList;
import static net.solarnetwork.service.FilterableService.filterPropValue;
import static net.solarnetwork.service.FilterableService.setFilterProp;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.io.serial.SerialConnectionAction;
import net.solarnetwork.node.io.serial.SerialNetwork;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.service.FilterableService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.OptionalService.OptionalFilterableService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.StringUtils;

/**
 * A base helper class to support {@link SerialNetwork} based services.
 *
 * @author matt
 * @version 2.1
 */
public abstract class SerialDeviceSupport extends BaseIdentifiable {

	/**
	 * Get setting specifiers for the serial network UID filter.
	 *
	 * @param prefix
	 *        an optional prefix to add to each setting key
	 * @param defaultUid
	 *        the default UID setting value
	 * @return list of setting specifiers
	 * @since 2.0
	 */
	public static List<SettingSpecifier> serialNetworkSettings(String prefix, String defaultUid) {
		prefix = (prefix != null ? prefix : "");
		return singletonList(new BasicTextFieldSettingSpecifier(prefix + "serialNetworkUid", defaultUid,
				false, "(objectClass=net.solarnetwork.node.io.serial.SerialNetwork)"));
	}

	private Map<String, Object> deviceInfo;
	private OptionalFilterableService<SerialNetwork> serialNetwork;
	private OptionalService<EventAdmin> eventAdmin;

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

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
	 * slash) delimited string of the resulting values, or {@literal null} if
	 * that method returns {@literal null}.
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
	 * {@link #readDeviceInfo(SerialConnection)}. The map is cached so
	 * subsequent calls will not attempt to read from the device. Note the
	 * returned map cannot be modified.
	 *
	 * @return the device info, or {@literal null}
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
				String desc = getUid();
				if ( desc == null || desc.isEmpty() ) {
					desc = this.toString();
				}
				log.warn("Communcation problem getting [{}] info on [{}]: {}", desc,
						getSerialNetworkUid(), e.getMessage());
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
	 * @param <T>
	 *        the action result type
	 * @param action
	 *        the connection action
	 * @return the result of the callback, or {@literal null} if the action is
	 *         never invoked
	 * @throws IOException
	 *         if any IO error occurs
	 */
	protected <T> T performAction(final SerialConnectionAction<T> action) throws IOException {
		T result = null;
		SerialNetwork device = OptionalService.service(serialNetwork);
		if ( device != null ) {
			result = device.performAction(action);
		}
		return result;
	}

	/**
	 * Get direct access to the device info data.
	 *
	 * @return the device info, or {@literal null}
	 */
	protected Map<String, Object> getDeviceInfoMap() {
		return deviceInfo;
	}

	/**
	 * Set the device info data. Setting the {@code deviceInfo} to
	 * {@literal null} will force the next call to {@link #getDeviceInfo()} to
	 * read from the device to populate this data, and setting this to anything
	 * else will force all subsequent calls to {@link #getDeviceInfo()} to
	 * simply return that map.
	 *
	 * @param deviceInfo
	 *        the device info map to set
	 */
	protected void setDeviceInfoMap(Map<String, Object> deviceInfo) {
		this.deviceInfo = deviceInfo;
	}

	/**
	 * Post an {@link Event}.
	 *
	 * <p>
	 * This method only works if a {@link EventAdmin} has been configured via
	 * {@link #setEventAdmin(OptionalService)}. Otherwise the event is silently
	 * ignored.
	 * </p>
	 *
	 * @param event
	 *        the event to post
	 */
	protected final void postEvent(Event event) {
		EventAdmin ea = (eventAdmin == null ? null : eventAdmin.service());
		if ( ea == null || event == null ) {
			return;
		}
		ea.postEvent(event);
	}

	/**
	 * Get the serial network.
	 *
	 * @return the network
	 */
	public OptionalFilterableService<SerialNetwork> getSerialNetwork() {
		return serialNetwork;
	}

	/**
	 * Set the serial network.
	 *
	 * @param serialDevice
	 *        the network
	 */
	public void setSerialNetwork(OptionalFilterableService<SerialNetwork> serialDevice) {
		this.serialNetwork = serialDevice;
	}

	/**
	 * Get the serial network UID.
	 *
	 * @return the serial network UID
	 */
	public String getSerialNetworkUid() {
		return filterPropValue((FilterableService) serialNetwork, BaseIdentifiable.UID_PROPERTY);
	}

	/**
	 * Set the serial network UID.
	 *
	 * @param uid
	 *        the serial network UID
	 */
	public void setSerialNetworkUid(String uid) {
		setFilterProp((FilterableService) serialNetwork, BaseIdentifiable.UID_PROPERTY, uid);
	}

	/**
	 * Get the configured {@link EventAdmin}.
	 *
	 * @return the event admin service
	 */
	public OptionalService<EventAdmin> getEventAdmin() {
		return eventAdmin;
	}

	/**
	 * Set an {@link EventAdmin} to use for posting control provider events.
	 *
	 * @param eventAdmin
	 *        The service to use.
	 */
	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

}
