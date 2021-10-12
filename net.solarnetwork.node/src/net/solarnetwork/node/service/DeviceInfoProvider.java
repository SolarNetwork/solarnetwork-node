/* ==================================================================
 * DeviceInfoProvider.java - 27/08/2021 4:56:11 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.service;

import net.solarnetwork.domain.DeviceInfo;

/**
 * API for something that can provide device info.
 * 
 * @author matt
 * @version 1.0
 * @since 1.90
 */
public interface DeviceInfoProvider {

	/**
	 * Get a {@link DeviceInfo} instance.
	 * 
	 * <p>
	 * This method returns {@literal null}. Extending classes must override to
	 * provide device info metadata.
	 * </p>
	 * 
	 * @return the device info, or {@literal null} if none available
	 */
	default DeviceInfo deviceInfo() {
		return null;
	}

	/**
	 * Get the source ID to publish device info under.
	 * 
	 * <p>
	 * This method returns {@literal null}. Extending classes must override to
	 * provide a source ID value if they wish to publish device info metadata.
	 * </p>
	 * 
	 * @return the source ID to publish device info metadata to
	 */
	default String deviceInfoSourceId() {
		return null;
	}

	/**
	 * Get the publish setting for the device info.
	 * 
	 * @return {@literal true} if the device info can be published, typically as
	 *         metadata; defaults to {@literal true}
	 */
	default boolean canPublishDeviceInfo() {
		return true;
	}

}
