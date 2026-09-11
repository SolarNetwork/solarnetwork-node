/* ==================================================================
 * SpiDeviceFactory.java - 11 Sept 2026 1:21:23 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.linux.spi;

import net.solarnetwork.node.hw.linux.spi.jna.JnaSpiDevice;

/**
 * Create SPI devices.
 *
 * @author matt
 * @version 1.0
 */
public final class SpiDeviceFactory {

	private SpiDeviceFactory() {
		// not available
	}

	/**
	 * Get a {@link SpiDevice} instance.
	 *
	 * @param busNum
	 *        the bus number
	 * @param chipSelect
	 *        the chip selection
	 * @return the device
	 */
	public static SpiDevice spiDeviceFor(final int busNum, final int chipSelect) {
		return new JnaSpiDevice(busNum, chipSelect);
	}

	/**
	 * Get a {@link SpiDevice} instance.
	 *
	 * @param devicePath
	 *        the device path, for example {@code /dev/spidev0.0}
	 * @return the device
	 */
	public static SpiDevice spiDeviceFor(final String devicePath) {
		return new JnaSpiDevice(devicePath);
	}

}
