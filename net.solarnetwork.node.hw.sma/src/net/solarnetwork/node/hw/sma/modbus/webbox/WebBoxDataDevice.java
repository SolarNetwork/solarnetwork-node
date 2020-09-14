/* ==================================================================
 * WebBoxDataDevice.java - 14/09/2020 11:12:53 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sma.modbus.webbox;

import net.solarnetwork.node.hw.sma.domain.SmaDeviceDataAccessor;
import net.solarnetwork.node.hw.sma.domain.SmaDeviceKind;
import net.solarnetwork.node.hw.sma.modbus.SmaDeviceData;
import net.solarnetwork.node.io.modbus.ModbusConnection;

/**
 * Implementation of {@link WebBoxDevice} based on a {@link SmaDeviceData}
 * instance.
 * 
 * @author matt
 * @version 1.0
 */
public class WebBoxDataDevice<T extends SmaDeviceData & SmaDeviceDataAccessor> implements WebBoxDevice {

	private final int unitId;
	private final SmaDeviceKind deviceKind;
	private final T data;

	/**
	 * Constructor.
	 * 
	 * @param unitId
	 *        the Modbus unit ID
	 * @param deviceKind
	 *        the device kind
	 * @param data
	 *        the data
	 */
	public WebBoxDataDevice(int unitId, SmaDeviceKind deviceKind, T data) {
		super();
		this.unitId = unitId;
		this.deviceKind = deviceKind;
		this.data = data;
	}

	@Override
	public int getUnitId() {
		return unitId;
	}

	@Override
	public Long getSerialNumber() {
		T accessor = getDeviceDataAccessor();
		return (accessor != null ? accessor.getSerialNumber() : null);
	}

	@Override
	public SmaDeviceKind getDeviceKind() {
		return deviceKind;
	}

	@Override
	public T getDeviceDataAccessor() {
		return data;
	}

	@Override
	public void readInformationData(ModbusConnection conn) {
		data.readInformationData(conn);

	}

	@Override
	public void readDeviceData(ModbusConnection conn) {
		data.readDeviceData(conn);
	}

}
