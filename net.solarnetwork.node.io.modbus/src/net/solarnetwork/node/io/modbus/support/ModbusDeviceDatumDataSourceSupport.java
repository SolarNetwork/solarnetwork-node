/* ==================================================================
 * ModbusDeviceDatumDataSourceSupport.java - 26/09/2017 11:16:38 AM
 *
 * Copyright 2017 SolarNetwork.net Dev Team
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
import net.solarnetwork.domain.BasicDeviceInfo;
import net.solarnetwork.domain.DeviceInfo;
import net.solarnetwork.node.domain.DataAccessor;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.StringUtils;

/**
 * A base helper class to support {@link ModbusNetwork} based
 * {@link DatumDataSource} implementations.
 *
 * @author matt
 * @version 3.2
 */
public abstract class ModbusDeviceDatumDataSourceSupport extends DatumDataSourceSupport {

	/** The {@code unitId} property default value. */
	public static final int DEFAULT_UNIT_ID = 1;

	private Map<String, Object> deviceInfo;
	private int unitId = DEFAULT_UNIT_ID;
	private OptionalService<ModbusNetwork> modbusNetwork;

	/**
	 * Get setting specifiers for the {@literal unitId} and
	 * {@literal modbusNetwork.propertyFilters['uid']} properties.
	 *
	 * @return list of setting specifiers
	 * @since 1.1
	 */
	protected List<SettingSpecifier> getModbusNetworkSettingSpecifiers() {
		return modbusNetworkSettingSpecifiers("Modbus Port", DEFAULT_UNIT_ID);
	}

	/**
	 * Get setting specifiers for the {@literal unitId} and
	 * {@literal modbusNetwork.propertyFilters['uid']} properties.
	 *
	 * @param uid
	 *        the Modbus Network UID property filter default value to use
	 * @param unitId
	 *        the unit ID default value to use
	 * @return list of setting specifiers
	 * @since 3.1
	 */
	public static List<SettingSpecifier> modbusNetworkSettingSpecifiers(String uid, int unitId) {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(16);
		results.add(new BasicTextFieldSettingSpecifier("modbusNetwork.propertyFilters['uid']", uid,
				false, "(objectClass=net.solarnetwork.node.io.modbus.ModbusNetwork)"));
		results.add(new BasicTextFieldSettingSpecifier("unitId", String.valueOf(unitId)));
		return results;
	}

	/**
	 * Get the {@link ModbusNetwork} from the configured {@code modbusNetwork}
	 * service, or {@literal null} if not available or not configured.
	 *
	 * @return ModbusNetwork
	 */
	protected final ModbusNetwork modbusNetwork() {
		return (modbusNetwork == null ? null : modbusNetwork.service());
	}

	/**
	 * Read general device info and return a map of the results.
	 *
	 * <p>
	 * See the various {@code INFO_KEY_*} constants for information on the
	 * values returned in the result map.
	 * </p>
	 *
	 * @param conn
	 *        the connection to use
	 * @return a map with general device information populated
	 * @throws IOException
	 *         if any communication error occurs
	 */
	protected abstract Map<String, Object> readDeviceInfo(ModbusConnection conn) throws IOException;

	/**
	 * Return an informational message composed of general device info.
	 *
	 * <p>
	 * This method will call {@link #getDeviceInfo()} and return a {@code /}
	 * (forward slash) delimited string of the resulting values, or
	 * {@literal null} if that method returns {@literal null}.
	 * </p>
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
	 * Get the device info data as a Map.
	 *
	 * <p>
	 * This method will call {@link #readDeviceInfo(ModbusConnection)}. The map
	 * is cached so subsequent calls will not attempt to read from the device.
	 * Note the returned map cannot be modified.
	 * </p>
	 *
	 * @return the device info, or {@literal null}
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
				log.warn("Communcation problem with {}: {}", getUid(), e.getMessage());
			}
		}
		return (info == null ? null : Collections.unmodifiableMap(info));
	}

	/**
	 * Perform some work with a Modbus {@link ModbusConnection}.
	 *
	 * <p>
	 * This method attempts to obtain a {@link ModbusNetwork} from the
	 * configured {@code modbusNetwork} service, calling
	 * {@link ModbusNetwork#performAction(int, ModbusConnectionAction)} if one
	 * can be obtained.
	 * </p>
	 *
	 * @param <T>
	 *        the result type
	 * @param action
	 *        the connection action
	 * @return the result of the callback, or {@literal null} if the action is
	 *         never invoked
	 * @throws IOException
	 *         if any IO error occurs
	 */
	protected final <T> T performAction(final ModbusConnectionAction<T> action) throws IOException {
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
	 * @return the device info, or {@literal null}
	 */
	protected Map<String, Object> getDeviceInfoMap() {
		return deviceInfo;
	}

	/**
	 * Set the device info data.
	 *
	 * <p>
	 * Setting the {@code deviceInfo} to {@literal null} will force the next
	 * call to {@link #getDeviceInfo()} to read from the device to populate this
	 * data, and setting this to anything else will force all subsequent calls
	 * to {@link #getDeviceInfo()} to simply return that map.
	 * </p>
	 *
	 * @param deviceInfo
	 *        the device info map to set
	 */
	protected void setDeviceInfoMap(Map<String, Object> deviceInfo) {
		this.deviceInfo = deviceInfo;
	}

	/**
	 * Get device info.
	 *
	 * @return the device info based on calling the {@link #getDeviceInfo()}
	 *         method
	 * @since 2.2
	 */
	public DeviceInfo deviceInfo() {
		Map<String, ?> info = getDeviceInfo();
		BasicDeviceInfo.Builder b = DataAccessor.deviceInfoBuilderForInfo(info);
		return (b.isEmpty() ? null : b.build());
	}

	/**
	 * Get the configured Modbus device name.
	 *
	 * @return the modbus device name
	 * @since 1.3
	 */
	public String modbusDeviceName() {
		return getUnitId() + "@" + modbusNetwork();
	}

	/**
	 * Get the configured {@link ModbusNetwork}.
	 *
	 * @return the modbus network
	 */
	public OptionalService<ModbusNetwork> getModbusNetwork() {
		return modbusNetwork;
	}

	/**
	 * Set the {@link ModbusNetwork} to use.
	 *
	 * @param modbusDevice
	 *        the modbus network
	 */
	public void setModbusNetwork(OptionalService<ModbusNetwork> modbusDevice) {
		this.modbusNetwork = modbusDevice;
	}

	/**
	 * Get the Modbus unit ID.
	 *
	 * @return the unit ID; defauts to {@link #DEFAULT_UNIT_ID}
	 */
	public int getUnitId() {
		return unitId;
	}

	/**
	 * Set the Modbus unit ID.
	 *
	 * @param unitId
	 *        the unit ID
	 */
	public void setUnitId(int unitId) {
		this.unitId = unitId;
	}

}
