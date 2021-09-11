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

import static net.solarnetwork.service.OptionalService.service;
import java.io.IOException;
import net.solarnetwork.node.hw.sma.domain.SmaDeviceDataAccessor;
import net.solarnetwork.node.hw.sma.domain.SmaDeviceKind;
import net.solarnetwork.node.hw.sma.modbus.SmaDeviceData;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.service.OptionalService;

/**
 * Implementation of {@link WebBoxDevice} based on a {@link SmaDeviceData}
 * instance.
 * 
 * @author matt
 * @version 2.0
 */
public class WebBoxDataDevice<T extends SmaDeviceData & SmaDeviceDataAccessor> implements WebBoxDevice {

	/** The frequency at which to refrech the device info registers. */
	public static final long INFO_READ_FREQUENCY = 60 * 60 * 1000L;

	private final OptionalService<ModbusNetwork> modbusNetwork;
	private final int unitId;
	private final SmaDeviceKind deviceKind;
	private final T data;

	private long lastInfoRead = 0;

	/**
	 * Constructor.
	 * 
	 * @param modbusNetwork
	 *        the Modbus network to use
	 * @param unitId
	 *        the Modbus unit ID
	 * @param deviceKind
	 *        the device kind
	 * @param data
	 *        the data
	 * @throws IllegalArgumentException
	 *         any argument is {@literal null}
	 */
	public WebBoxDataDevice(OptionalService<ModbusNetwork> modbusNetwork, int unitId,
			SmaDeviceKind deviceKind, T data) {
		super();
		if ( modbusNetwork == null ) {
			throw new IllegalArgumentException("The modbusNetwork argument must not be null.");
		}
		this.modbusNetwork = modbusNetwork;
		this.unitId = unitId;
		if ( deviceKind == null ) {
			throw new IllegalArgumentException("The deviceKind argument must not be null.");
		}
		this.deviceKind = deviceKind;
		if ( data == null ) {
			throw new IllegalArgumentException("The data argument must not be null.");
		}
		this.data = data;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("WebBoxDataDevice{");
		builder.append(service(modbusNetwork));
		builder.append(", unitId=");
		builder.append(unitId);
		builder.append("}");
		return builder.toString();
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
	public synchronized SmaDeviceDataAccessor refreshData(long maxAge) throws IOException {
		if ( maxAge > 0 ) {
			long now = System.currentTimeMillis();
			long ts = data.getDataTimestamp();
			if ( ts > 0 && ts + maxAge > now ) {
				// data has not expired
				return data.copy();
			}
		}
		ModbusNetwork modbus = OptionalService.service(modbusNetwork);
		if ( modbus == null ) {
			return data.copy();
		}
		return modbus.performAction(unitId, new ModbusConnectionAction<SmaDeviceDataAccessor>() {

			@Override
			public SmaDeviceDataAccessor doWithConnection(ModbusConnection conn) throws IOException {
				final long now = System.currentTimeMillis();
				if ( lastInfoRead + INFO_READ_FREQUENCY < now ) {
					data.readInformationData(conn);
					lastInfoRead = now;
				}
				data.readDeviceData(conn);
				return data.copy();
			}
		});
	}
}
