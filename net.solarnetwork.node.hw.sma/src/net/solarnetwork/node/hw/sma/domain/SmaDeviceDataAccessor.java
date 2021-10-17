/* ==================================================================
 * SmaDeviceDataAccessor.java - 11/09/2020 4:25:16 PM
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

package net.solarnetwork.node.hw.sma.domain;

import java.util.Map;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.MutableDatumSamplesOperations;
import net.solarnetwork.node.domain.DataAccessor;

/**
 * {@link DataAccessor} API for all SMA devices.
 * 
 * @author matt
 * @version 2.0
 */
public interface SmaDeviceDataAccessor extends DataAccessor {

	/**
	 * Test if this device supports the {@link SmaDeviceCommonDataAccessor} API.
	 * 
	 * <p>
	 * One can also simply test for adherence to the
	 * {@link SmaDeviceCommonDataAccessor} API.
	 * </p>
	 * 
	 * @return {@literal true} if this device supports the common API
	 */
	boolean hasCommonDataAccessorSupport();

	/**
	 * Get the device serial number.
	 * 
	 * @return the serial number
	 */
	Long getSerialNumber();

	/**
	 * Get the kind of device this accessor provides access to.
	 * 
	 * @return the device kind
	 */
	SmaDeviceKind getDeviceKind();

	/**
	 * Get the device operating state.
	 * 
	 * @return the device operating state
	 */
	DeviceOperatingState getDeviceOperatingState();

	/**
	 * Populate data into a {@link MutableDatumSamplesOperations} object.
	 * 
	 * @param samples
	 *        the samples to populate with data
	 * @param parameters
	 *        optional parameters to modify the data population; implementation
	 *        specific
	 */
	void populateDatumSamples(MutableDatumSamplesOperations samples, Map<String, ?> parameters);

	/**
	 * Get optional metadata about the device.
	 * 
	 * @param parameters
	 *        optional parameters to modify the data population; implementation
	 *        specific
	 * @return the device metadata, or {@literal null}
	 */
	GeneralDatumMetadata getDatumMetadata(Map<String, ?> parameters);

	/**
	 * Get a description of the data in this accessor.
	 * 
	 * @return the description, never {@literal null}
	 */
	default String getDataDescription() {
		StringBuilder buf = new StringBuilder();
		DeviceOperatingState opState = getDeviceOperatingState();
		if ( opState != null ) {
			buf.append(opState);
		}
		if ( this instanceof SmaDeviceCommonDataAccessor ) {
			SmaDeviceCommonDataAccessor common = (SmaDeviceCommonDataAccessor) this;
			buf.append("; W = ").append(common.getActivePower());
			buf.append("; Wh = ").append(common.getActiveEnergyExported());
			buf.append("; dcVoltage = ").append(common.getDcVoltage());
			buf.append("; dcCurrent = ").append(common.getDcCurrent());
			buf.append("; temp = ").append(common.getCabinetTemperature());
			buf.append("; event = ").append(common.getEventId());
		}
		return buf.toString();
	}
}
