/* ==================================================================
 * WebBoxService.java - 14/09/2020 10:01:52 AM
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

import static net.solarnetwork.node.hw.sma.modbus.SmaCommonDeviceRegister.SerialNumber;
import static net.solarnetwork.node.io.modbus.ModbusDataUtils.encodeUnsignedInt32;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.solarnetwork.node.hw.sma.domain.SmaDeviceDataAccessor;
import net.solarnetwork.node.hw.sma.domain.SmaDeviceKind;
import net.solarnetwork.node.hw.sma.domain.SmaDeviceType;
import net.solarnetwork.node.hw.sma.modbus.SmaScNnnUData;
import net.solarnetwork.node.hw.sma.modbus.SmaScStringMonitorControllerData;
import net.solarnetwork.node.hw.sma.modbus.SmaScStringMonitorUsData;
import net.solarnetwork.node.hw.sma.modbus.SmaSunnySensorboxData;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.support.ModbusDataDatumDataSourceSupport;

/**
 * Implementation of {@link WebBoxOperations}.
 * 
 * @author matt
 * @version 1.0
 */
public class WebBoxService extends ModbusDataDatumDataSourceSupport<WebBoxData>
		implements WebBoxOperations, WebBoxDevice {

	private final ConcurrentMap<Integer, WebBoxDataDevice<?>> deviceDataMap = new ConcurrentHashMap<>(8,
			0.9f, 4);

	/**
	 * Constructor.
	 */
	public WebBoxService() {
		super(new WebBoxData());
	}

	@Override
	protected void refreshDeviceInfo(ModbusConnection connection, WebBoxData sample) {
		sample.readInformationData(connection);
	}

	@Override
	protected void refreshDeviceData(ModbusConnection connection, WebBoxData sample) {
		sample.readDeviceData(connection);
	}

	@Override
	public WebBoxDataAccessor getDataAccessor() {
		try {
			return getCurrentSample();
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Long getSerialNumber() {
		WebBoxDataAccessor accessor = getDataAccessor();
		return accessor != null ? accessor.getSerialNumber() : null;
	}

	@Override
	public SmaDeviceKind getDeviceKind() {
		WebBoxDataAccessor accessor = getDataAccessor();
		return accessor != null ? accessor.getDeviceKind() : null;
	}

	@Override
	public SmaDeviceDataAccessor getDeviceDataAccessor() {
		return getDataAccessor();
	}

	@Override
	public SmaDeviceDataAccessor refreshData(long maxAge) throws IOException {
		return getCurrentSample();
	}

	@Override
	public Collection<WebBoxDevice> availableDevices() {
		WebBoxDataAccessor accessor = getDataAccessor();
		if ( accessor == null ) {
			return Collections.emptyList();
		}
		Collection<WebBoxDeviceReference> refs = accessor.availableDeviceReferences();
		if ( refs == null ) {
			return Collections.emptyList();
		}
		List<WebBoxDevice> result = new ArrayList<>(refs.size());
		for ( WebBoxDeviceReference ref : refs ) {
			WebBoxDevice device = deviceForReference(ref);
			if ( device != null ) {
				result.add(device);
			}
		}
		return result;
	}

	private WebBoxDevice deviceForReference(WebBoxDeviceReference ref) {
		if ( ref == null ) {
			return null;
		}
		// TODO: create lookup properties file to map device ID -> WebBoxDevice implementation class
		final int deviceId = ref.getDeviceId();
		final SmaDeviceType deviceType;
		try {
			deviceType = SmaDeviceType.forCode(deviceId);
		} catch ( IllegalArgumentException e ) {
			// not a known device ID
			return null;
		}
		final short[] serialNumberData = encodeUnsignedInt32(ref.getSerialNumber());
		switch (deviceType) {
			case SunnyWebBox:
				return this;

			case SunnyCentral250US:
				return deviceDataMap.computeIfAbsent(ref.getUnitId(), k -> {
					return new WebBoxDataDevice<>(getModbusNetwork(), ref.getUnitId(), deviceType,
							new SmaScNnnUData(deviceType, serialNumberData, SerialNumber.getAddress()));
				});

			case SunnyCentralStringMonitor:
				return deviceDataMap.computeIfAbsent(ref.getUnitId(), k -> {
					return new WebBoxDataDevice<>(getModbusNetwork(), ref.getUnitId(), deviceType,
							new SmaScStringMonitorControllerData(serialNumberData,
									SerialNumber.getAddress()));
				});

			case SunnyCentralStringMonitorUS:
				return deviceDataMap.computeIfAbsent(ref.getUnitId(), k -> {
					return new WebBoxDataDevice<>(getModbusNetwork(), ref.getUnitId(), deviceType,
							new SmaScStringMonitorUsData(serialNumberData, SerialNumber.getAddress()));
				});

			case SunnySensorbox:
				return deviceDataMap.computeIfAbsent(ref.getUnitId(), k -> {
					return new WebBoxDataDevice<>(getModbusNetwork(), ref.getUnitId(), deviceType,
							new SmaSunnySensorboxData(serialNumberData, SerialNumber.getAddress()));
				});

			default:
				// dunno
		}
		return null;
	}

}
