/* ==================================================================
 * FakeSpiDevice.java - 11 Sept 2026 7:23:49 am
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

package net.solarnetwork.node.hw.linux.spi.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.node.hw.linux.spi.SpiDevice;

/**
 * In-memory {@link SpiDevice} that emulates the ATM90E36 SPI framing, for
 * host-side unit tests (no hardware).
 *
 * <p>
 * It understands the 4-byte frame produced by {@code ATM90E36.readRegister} /
 * {@code ATM90E36.writeRegister}: a 16-bit big-endian register address (with
 * bit 15 set for reads) followed by a 16-bit big-endian value. Reads are
 * answered from {@link #registers}; writes are recorded in {@link #writes}.
 * </p>
 */
public class FakeSpiDevice implements SpiDevice {

	/** A recorded register write. */
	public static final class Write {

		public final int address;
		public final int value;

		Write(int address, int value) {
			this.address = address;
			this.value = value;
		}

		@Override
		public String toString() {
			return String.format("Write{0x%02X <- 0x%04X}", address, value);
		}
	}

	/** Register address (0x00-0xFF) to 16-bit value returned on read. */
	public final Map<Integer, Integer> registers = new HashMap<>();

	/** All register writes, in order. */
	public final List<Write> writes = new ArrayList<>();

	/** Raw transmit frames passed to {@link #transfer(byte[])}, in order. */
	public final List<byte[]> txFrames = new ArrayList<>();

	public boolean open = false;
	public int mode = -1;
	public int maxSpeedHz = -1;
	public int bitsPerWord = -1;

	/** Number of {@link #batch(byte[][], int)} handles created. */
	public int batchOpens = 0;

	/** Number of {@code Batch.transfer()} calls across all handles. */
	public int batchTransfers = 0;

	/** Number of {@code Batch.close()} calls. */
	public int batchCloses = 0;

	/**
	 * The {@code settleMicros} argument from the last
	 * {@link #batch(byte[][], int)}.
	 */
	public int lastSettleMicros = -1;

	private static int swap16(int v) {
		return ((v >> 8) & 0xFF) | ((v << 8) & 0xFF00);
	}

	@Override
	public void open(int mode, int maxSpeedHz, int bitsPerWord) {
		this.open = true;
		this.mode = mode;
		this.maxSpeedHz = maxSpeedHz;
		this.bitsPerWord = bitsPerWord;
	}

	@Override
	public byte[] transfer(byte[] tx) {
		txFrames.add(tx.clone());
		if ( tx.length != 4 ) {
			throw new IllegalArgumentException("Expected a 4-byte frame, got " + tx.length);
		}
		int wireAddr = ((tx[0] & 0xFF) << 8) | (tx[1] & 0xFF);
		int decoded = swap16(wireAddr);
		boolean read = (decoded & 0x8000) != 0;
		int address = decoded & 0x7FFF;

		if ( read ) {
			int v = registers.getOrDefault(address, 0) & 0xFFFF;
			// Atm90E36 reads response[2],response[3] then swap16s the result,
			// so place swap16(v) into those bytes.
			int wire = swap16(v);
			return new byte[] { 0, 0, (byte) ((wire >> 8) & 0xFF), (byte) (wire & 0xFF) };
		}

		int wireVal = ((tx[2] & 0xFF) << 8) | (tx[3] & 0xFF);
		int value = swap16(wireVal) & 0xFFFF;
		writes.add(new Write(address, value));
		registers.put(address, value);
		return new byte[4];
	}

	@Override
	public SpiDevice.Batch batch(byte[][] txFrames, int settleMicros) {
		batchOpens++;
		lastSettleMicros = settleMicros;
		return new SpiDevice.Batch() {

			@Override
			public byte[][] transfer() {
				batchTransfers++;
				byte[][] rx = new byte[txFrames.length][];
				for ( int i = 0; i < txFrames.length; i++ ) {
					rx[i] = FakeSpiDevice.this.transfer(txFrames[i]);
				}
				return rx;
			}

			@Override
			public void close() {
				batchCloses++;
			}
		};
	}

	@Override
	public void close() {
		open = false;
	}

}
