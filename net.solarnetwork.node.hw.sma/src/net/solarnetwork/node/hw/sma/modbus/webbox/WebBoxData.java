/* ==================================================================
 * WebBoxData.java - 14/09/2020 10:07:26 AM
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

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.domain.MutableGeneralDatumSamplesOperations;
import net.solarnetwork.node.domain.DataAccessor;
import net.solarnetwork.node.hw.sma.domain.SmaDeviceType;
import net.solarnetwork.node.hw.sma.modbus.SmaDeviceData;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * {@link DataAccessor} for a WebBox.
 * 
 * @author matt
 * @version 1.0
 */
public class WebBoxData extends SmaDeviceData implements WebBoxDataAccessor {

	private Collection<WebBoxDeviceReference> deviceReferences;

	/**
	 * Constructor.
	 */
	public WebBoxData() {
		super(SmaDeviceType.SunnyWebBox);
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the meter data to copy
	 */
	public WebBoxData(ModbusData other) {
		super(other, SmaDeviceType.SunnyWebBox);
		if ( other instanceof WebBoxData ) {
			synchronized ( other ) {
				this.deviceReferences = ((WebBoxData) other).deviceReferences;
			}
		}
	}

	@Override
	public WebBoxData copy() {
		return new WebBoxData(this);
	}

	@Override
	public final void readInformationData(final ModbusConnection conn) throws IOException {
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				WebBoxRegister.INFO_REGISTER_ADDRESS_SET, MAX_RESULTS);
	}

	@Override
	public final void readDeviceData(final ModbusConnection conn) throws IOException {
		performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) throws IOException {
				Collection<WebBoxDeviceReference> refs = WebBoxUtils.readAvailableDevices(conn, m);
				synchronized ( WebBoxData.this ) {
					if ( deviceReferences != null ) {
						deviceReferences.clear();
						deviceReferences.addAll(refs);
					} else {
						deviceReferences = refs;
					}
				}
				return true;
			}
		});
	}

	@Override
	public DeviceOperatingState getDeviceOperatingState() {
		return DeviceOperatingState.Unknown;
	}

	@Override
	public Long getModbusProfileVersion() {
		Number n = getNumber(WebBoxRegister.ModbusProfileVersion);
		return (n instanceof Long ? (Long) n : n != null ? n.longValue() : null);
	}

	@Override
	public Long getModbusDataChangeCounter() {
		Number n = getNumber(WebBoxRegister.DataChange);
		return (n instanceof Long ? (Long) n : n != null ? n.longValue() : null);
	}

	@Override
	public Collection<WebBoxDeviceReference> availableDeviceReferences() {
		return deviceReferences;
	}

	@Override
	public void populateDatumSamples(MutableGeneralDatumSamplesOperations samples,
			Map<String, ?> parameters) {
		// TODO Auto-generated method stub

	}

}
