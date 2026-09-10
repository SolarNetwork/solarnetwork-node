/* ==================================================================
 * SpiDevice.java - 10 Sept 2026 8:10:22 pm
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

/**
 * A minimal abstraction of a Linux SPI character device (for example
 * {@code /dev/spidev0.0}).
 *
 * @author matt
 * @version 1.0
 */
public interface SpiDevice extends AutoCloseable {

	/**
	 * Open the underlying device and apply the given bus configuration.
	 *
	 * @param mode
	 *        the SPI mode (0-3); mode 3 (CPOL=1, CPHA=1) for the ATM90E36
	 * @param maxSpeedHz
	 *        the maximum bus clock frequency, in Hz
	 * @param bitsPerWord
	 *        the word size, in bits (8 for the ATM90E36)
	 * @throws SpiException
	 *         if the device cannot be opened or configured
	 */
	void open(int mode, int maxSpeedHz, int bitsPerWord);

	/**
	 * Perform a single chip-select-asserted, full-duplex transfer.
	 *
	 * @param tx
	 *        the bytes to clock out
	 * @return the bytes clocked in, the same length as {@code tx}
	 * @throws SpiException
	 *         if the transfer fails
	 */
	byte[] transfer(byte[] tx);

	/**
	 * Perform several full-duplex transfers as one operation, each framed by
	 * its own chip-select assertion.
	 *
	 * <p>
	 * Implementations backed by Linux {@code spidev} issue this as a single
	 * {@code SPI_IOC_MESSAGE} {@code ioctl}, avoiding a system call per
	 * transfer. Between transfers the chip-select line is released and
	 * re-asserted, so each frame is an independent bus transaction (required by
	 * devices such as the ATM90E36, whose SPI protocol accesses one register
	 * per chip-select cycle).
	 * </p>
	 *
	 * <p>
	 * This is a one-shot convenience; to repeat the same batch frequently, hold
	 * a {@link #batch(byte[][], int)} so its native buffers are allocated once.
	 * </p>
	 *
	 * @param txFrames
	 *        the frames to clock out, one per transfer
	 * @param settleMicros
	 *        microseconds to hold after each transfer's last bit before
	 *        releasing chip-select ({@code 0} for none); clamped to a 16-bit
	 *        value
	 * @return one received frame per input frame, each the same length as its
	 *         corresponding input
	 * @throws SpiException
	 *         if any transfer fails
	 */
	default byte[][] transfer(byte[][] txFrames, int settleMicros) {
		try (Batch b = batch(txFrames, settleMicros)) {
			return b.transfer();
		}
	}

	/**
	 * Create a reusable multi-transfer batch for the given frames.
	 *
	 * <p>
	 * A {@link Batch} runs {@link Batch#transfer()} against the frames it was
	 * created with, as many times as needed, without re-allocating the native
	 * buffers a Linux {@code spidev} implementation needs per
	 * {@code SPI_IOC_MESSAGE}. Use it when a caller polls the same set of
	 * frames on a short interval; close it when finished.
	 * </p>
	 *
	 * <p>
	 * The frame contents are re-sent on every {@code transfer()}, so a caller
	 * may mutate the {@code byte[]} elements in place between calls (for
	 * example to write different register values); the number and length of the
	 * frames must not change. This default implementation simply loops
	 * {@link #transfer(byte[])} and holds nothing.
	 * </p>
	 *
	 * @param txFrames
	 *        the frames to clock out, one per transfer
	 * @param settleMicros
	 *        microseconds to hold after each transfer before releasing
	 *        chip-select ({@code 0} for none); clamped to a 16-bit value
	 * @return a batch bound to {@code txFrames}
	 */
	default Batch batch(byte[][] txFrames, int settleMicros) {
		return new Batch() {

			@Override
			public byte[][] transfer() {
				byte[][] rx = new byte[txFrames.length][];
				for ( int i = 0; i < txFrames.length; i++ ) {
					rx[i] = SpiDevice.this.transfer(txFrames[i]);
				}
				return rx;
			}

			@Override
			public void close() {
				// nothing to release
			}
		};
	}

	/**
	 * A reusable multi-transfer batch bound to a fixed set of frames. Obtained
	 * from {@link SpiDevice#batch(byte[][], int)}.
	 */
	interface Batch extends AutoCloseable {

		/**
		 * Run the batch's transfers once.
		 *
		 * @return one received frame per bound frame, each the same length as
		 *         its corresponding input
		 * @throws SpiException
		 *         if any transfer fails, or the batch is closed
		 */
		byte[][] transfer();

		/**
		 * Release any resources held for the batch. Never throws; idempotent.
		 */
		@Override
		void close();
	}

	/**
	 * Close the device, releasing the underlying file descriptor.
	 *
	 * <p>
	 * Never throws; overrides {@link AutoCloseable#close()} to drop the checked
	 * exception.
	 * </p>
	 */
	@Override
	void close();

}
