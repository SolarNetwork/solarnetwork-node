/* ==================================================================
 * JnaSpiDeviceTests.java - 11 Sept 2026 7:25:18 am
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

package net.solarnetwork.node.hw.linux.spi.jna.test;

import static org.assertj.core.api.BDDAssertions.then;
import org.junit.Test;
import net.solarnetwork.node.hw.linux.spi.jna.JnaSpiDevice;

/**
 * Verify the {@code _IOR}/{@code _IOW} encoding against the constant values
 * that {@code linux/spi/spidev.h} expands to on arm/arm64 (asm-generic
 * ioctl.h).
 */
public class JnaSpiDeviceTests {

	@Test
	public void spiIocWrMode32() {
		then(JnaSpiDevice.SPI_IOC_WR_MODE32).isEqualTo(0x40046b05L);
	}

	@Test
	public void spiIocWrMaxSpeedHz() {
		then(JnaSpiDevice.SPI_IOC_WR_MAX_SPEED_HZ).isEqualTo(0x40046b04L);
	}

	@Test
	public void spiIocWrBitsPerWord() {
		then(JnaSpiDevice.SPI_IOC_WR_BITS_PER_WORD).isEqualTo(0x40016b03L);
	}

	@Test
	public void spiIocMessageForOneTransfer() {
		// _IOW('k', 0, char[32]) == 0x40206b00
		then(JnaSpiDevice.spiIocMessage(1)).isEqualTo(0x40206b00L);
	}

	@Test
	public void spiIocMessageForThreeTransfers() {
		// _IOW('k', 0, char[96])
		then(JnaSpiDevice.spiIocMessage(3)).isEqualTo(0x40606b00L);
	}

	@Test
	public void spiIocMessageForSixteenTransfers() {
		// _IOW('k', 0, char[512]) -- the batched CSV read
		then(JnaSpiDevice.spiIocMessage(16)).isEqualTo(0x42006b00L);
	}

	@Test
	public void transferStructIs32Bytes() {
		then(JnaSpiDevice.SPI_IOC_TRANSFER_SIZE).isEqualTo(32);
	}

	@Test
	public void unregisterNativeMethodsIsSafeWhenUnusedAndRepeated() {
		// the libc binding is not touched by these tests, so this is a no-op,
		// but it must not throw - and calling it twice must be fine
		JnaSpiDevice.unregisterNativeMethods();
		JnaSpiDevice.unregisterNativeMethods();
	}

}
