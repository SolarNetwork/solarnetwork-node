/* ==================================================================
 * SmaScNnnUData.java - 14/09/2020 2:50:18 PM
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

package net.solarnetwork.node.hw.sma.modbus;

import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.node.hw.sma.domain.SmaCommonStatusCode;
import net.solarnetwork.node.hw.sma.domain.SmaDeviceKind;
import net.solarnetwork.node.hw.sma.domain.SmaScNnnUDataAccessor;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * {@link SmaDeviceData} for Sunny Central nnnU devices.
 * 
 * @author matt
 * @version 1.0
 */
public class SmaScNnnUData extends SmaDeviceData implements SmaScNnnUDataAccessor {

	private final SmaDeviceKind deviceKind;

	/**
	 * Constructor.
	 */
	public SmaScNnnUData(SmaDeviceKind deviceKind) {
		super();
		this.deviceKind = deviceKind;
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the meter data to copy
	 * @param deviceKind
	 *        the device kind
	 */
	public SmaScNnnUData(ModbusData other, SmaDeviceKind deviceKind) {
		super(other);
		this.deviceKind = deviceKind;
	}

	@Override
	public SmaScNnnUData copy() {
		return new SmaScNnnUData(this, deviceKind);
	}

	/**
	 * Read the informational registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 */
	@Override
	public final void readInformationData(final ModbusConnection conn) {
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				SmaScNnnURegister.INFO_REGISTER_ADDRESS_SET, MAX_RESULTS);
	}

	/**
	 * Read the registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 */
	@Override
	public final void readDeviceData(final ModbusConnection conn) {
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				SmaScNnnURegister.DATA_REGISTER_ADDRESS_SET, MAX_RESULTS);
	}

	@Override
	public SmaDeviceKind getDeviceKind() {
		return deviceKind;
	}

	@Override
	public DeviceOperatingState getDeviceOperatingState() {
		SmaCommonStatusCode c = getOperatingState();
		if ( c != null ) {
			switch (c) {
				case Operation:
					return DeviceOperatingState.Normal;

				case Warning:
				case Error:
					return DeviceOperatingState.Fault;

				case Disruption:
					return DeviceOperatingState.Recovery;

				default:
					// nothing to do
			}
		}
		return DeviceOperatingState.Unknown;
	}

	@Override
	public SmaCommonStatusCode getOperatingState() {
		return getStatusCode(SmaScNnnURegister.OperatingState);
	}

	@Override
	public Long getGridReconnectTime() {
		Number n = getNumber(SmaScNnnURegister.GridConnectTimeRemaining);
		return (n instanceof Long ? (Long) n : n != null ? n.longValue() : null);
	}

	@Override
	public SmaCommonStatusCode getRecommendedAction() {
		return getStatusCode(SmaScNnnURegister.RecommendedAction, null, SmaCommonStatusCode.Invalid);
	}

	@Override
	public SmaCommonStatusCode getGridContactorStatus() {
		return getStatusCode(SmaScNnnURegister.GridContactorStatus);
	}

	@Override
	public SmaCommonStatusCode getError() {
		return getStatusCode(SmaScNnnURegister.ErrorState, null, SmaCommonStatusCode.NotSet);
	}

}
