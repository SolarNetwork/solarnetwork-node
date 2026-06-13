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

package net.solarnetwork.node.io.modbus.support;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.OptionalService.OptionalFilterableService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.StringUtils;

/**
 * A base helper class to support {@link ModbusNetwork} based services.
 *
 * @author matt
 * @version 3.1
 * @since 2.0
 */
public abstract class ModbusDeviceSupport extends BaseIdentifiable {

	/**
	 * The default value for the {@code unitId} property.
	 *
	 * @since 1.2
	 */
	public static final int DEFAULT_UNIT_ID = 1;

	/**
	 * The default value for the {@link ModbusNetwork#getUid()} property filter
	 * value.
	 *
	 * @since 1.2
	 */
	public static final String DEFAULT_NETWORK_UID = "Modbus Port";

	/**
	 * Key for the device manufacture date, as a {@link java.time.LocalDate}.
	 */
	public static final String INFO_KEY_DEVICE_MANUFACTURE_DATE = "Manufacture Date";

	private @Nullable Map<String, Object> deviceInfo;
	private int unitId = DEFAULT_UNIT_ID;
	private @Nullable OptionalFilterableService<ModbusNetwork> modbusNetwork;

	/**
	 * Constructor.
	 */
	public ModbusDeviceSupport() {
		super();
	}

	/**
	 * Get setting specifiers for the {@literal unitId} and
	 * {@literal modbusNetwork.propertyFilters['uid']} properties.
	 *
	 * @param prefix
	 *        the setting prefix to prepend
	 * @return list of setting specifiers
	 * @since 1.2
	 */
	public static List<SettingSpecifier> modbusNetworkSettings(String prefix) {
		if ( prefix == null ) {
			prefix = "";
		}
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(2);
		results.add(new BasicTextFieldSettingSpecifier(prefix + "modbusNetwork.propertyFilters['uid']",
				DEFAULT_NETWORK_UID, false,
				"(objectClass=net.solarnetwork.node.io.modbus.ModbusNetwork)"));
		results.add(
				new BasicTextFieldSettingSpecifier(prefix + "unitId", String.valueOf(DEFAULT_UNIT_ID)));
		return results;
	}

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Get the {@link ModbusNetwork} from the configured {@code modbusNetwork}
	 * service, or {@code null} if not available or not configured.
	 *
	 * @return ModbusNetwork
	 */
	protected final @Nullable ModbusNetwork modbusNetwork() {
		return OptionalService.service(modbusNetwork);
	}

	/**
	 * Read general device info and return a map of the results. See the various
	 * {@code INFO_KEY_*} constants for information on the values returned in
	 * the result map.
	 *
	 * @param conn
	 *        the connection to use
	 * @return a map with general device information populated, or {@code null}
	 *         if one cannot be obtained
	 * @throws IOException
	 *         if any IO error occurs
	 */
	protected abstract @Nullable Map<String, Object> readDeviceInfo(ModbusConnection conn)
			throws IOException;

	/**
	 * Return an informational message composed of general device info. This
	 * method will call {@link #getDeviceInfo()} and return a {@code /} (forward
	 * slash) delimited string of the resulting values, or {@code null} if that
	 * method returns {@code null}.
	 *
	 * @return info message
	 */
	public @Nullable String getDeviceInfoMessage() {
		Map<String, ?> info = getDeviceInfo();
		if ( info == null ) {
			return null;
		}
		return StringUtils.delimitedStringFromCollection(info.values(), " / ");
	}

	/**
	 * Get the device info data as a Map. This method will call
	 * {@link #readDeviceInfo(ModbusConnection)}. The map is cached so
	 * subsequent calls will not attempt to read from the device. Note the
	 * returned map cannot be modified.
	 *
	 * @return the device info, or {@code null}
	 * @see #readDeviceInfo(ModbusConnection)
	 */
	public @Nullable Map<String, ?> getDeviceInfo() {
		Map<String, Object> info = deviceInfo;
		if ( info == null ) {
			try {
				info = performAction(new ModbusConnectionAction<Map<String, Object>>() {

					@Override
					public @Nullable Map<String, Object> doWithConnection(ModbusConnection conn)
							throws IOException {
						return readDeviceInfo(conn);
					}
				});
				deviceInfo = info;
			} catch ( IOException e ) {
				log.warn("Communcation problem with {}: {}", getUid(), e.getMessage());
			}
		}
		return (info == null ? null : Collections.unmodifiableMap(info));
	}

	/**
	 * Perform some work with a Modbus {@link ModbusConnection}. This method
	 * attempts to obtain a {@link ModbusNetwork} from the configured
	 * {@code modbusNetwork} service, calling
	 * {@link ModbusNetwork#performAction(int, ModbusConnectionAction)} if one
	 * can be obtained.
	 *
	 * @param <T>
	 *        the result type
	 * @param action
	 *        the connection action
	 * @return the result of the callback, or {@code null} if the action is
	 *         never invoked
	 * @throws IOException
	 *         if any IO error occurs
	 */
	protected final <T> @Nullable T performAction(final ModbusConnectionAction<T> action)
			throws IOException {
		T result = null;
		ModbusNetwork device = (modbusNetwork == null ? null : modbusNetwork.service());
		if ( device != null ) {
			result = device.performAction(unitId, action);
		}
		return result;
	}

	/**
	 * Get direct access to the device info data.
	 *
	 * @return the device info, or {@code null}
	 */
	protected final @Nullable Map<String, Object> getDeviceInfoMap() {
		return deviceInfo;
	}

	/**
	 * Set the device info data. Setting the {@code deviceInfo} to {@code null}
	 * will force the next call to {@link #getDeviceInfo()} to read from the
	 * device to populate this data, and setting this to anything else will
	 * force all subsequent calls to {@link #getDeviceInfo()} to simply return
	 * that map.
	 *
	 * @param deviceInfo
	 *        the device info map to set
	 */
	protected final void setDeviceInfoMap(@Nullable Map<String, Object> deviceInfo) {
		this.deviceInfo = deviceInfo;
	}

	/**
	 * Get the configured Modbus device name.
	 *
	 * @return the modbus device name
	 * @since 1.1
	 */
	public String modbusDeviceName() {
		return getUnitId() + "@" + modbusNetwork();
	}

	/**
	 * Get the Modbus network to use.
	 *
	 * @return the network
	 */
	public final @Nullable OptionalFilterableService<ModbusNetwork> getModbusNetwork() {
		return modbusNetwork;
	}

	/**
	 * Set the Modbus network to use.
	 *
	 * @param modbusDevice
	 *        the network
	 */
	public final void setModbusNetwork(@Nullable OptionalFilterableService<ModbusNetwork> modbusDevice) {
		this.modbusNetwork = modbusDevice;
	}

	/**
	 * Get the Modbus unit ID.
	 *
	 * @return the unit ID; defaults to {@link #DEFAULT_UNIT_ID}
	 */
	public final int getUnitId() {
		return unitId;
	}

	/**
	 * Set the Modbus unit ID.
	 *
	 * <p>
	 * The <i>unit ID</i> is the unique ID of the device on the Modbus network.
	 * </p>
	 *
	 * @param unitId
	 *        the ID to use
	 */
	public final void setUnitId(int unitId) {
		this.unitId = unitId;
	}

}
