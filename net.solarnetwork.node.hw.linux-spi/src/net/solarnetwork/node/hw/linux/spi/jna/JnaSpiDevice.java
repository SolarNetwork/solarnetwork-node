/* ==================================================================
 * JnaSpiDevice.java - 10 Sept 2026 8:14:50 pm
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

package net.solarnetwork.node.hw.linux.spi.jna;

import java.lang.ref.Reference;
import com.sun.jna.LastErrorException;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import net.solarnetwork.node.hw.linux.spi.SpiDevice;
import net.solarnetwork.node.hw.linux.spi.SpiException;

/**
 * A {@link SpiDevice} implementation that talks to the Linux {@code spidev}
 * character device directly, via {@code ioctl(2)} calls issued through JNA
 * (Java Native Access).
 *
 * <p>
 * The {@code ioctl} request codes and the {@code struct
 * spi_ioc_transfer} layout are transcribed from {@code linux/spi/spidev.h}.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class JnaSpiDevice implements SpiDevice {

	// --- open(2) flags (asm-generic, valid on arm/arm64) ---------------------
	private static final int O_RDWR = 0x0002;
	private static final int O_CLOEXEC = 0x80000;

	// --- ioctl encoding (asm-generic/ioctl.h; used by arm and arm64) ---------
	private static final int IOC_NRBITS = 8;
	private static final int IOC_TYPEBITS = 8;
	private static final int IOC_SIZEBITS = 14;
	private static final int IOC_NRSHIFT = 0;
	private static final int IOC_TYPESHIFT = IOC_NRSHIFT + IOC_NRBITS; // 8
	private static final int IOC_SIZESHIFT = IOC_TYPESHIFT + IOC_TYPEBITS; // 16
	private static final int IOC_DIRSHIFT = IOC_SIZESHIFT + IOC_SIZEBITS; // 30
	private static final int IOC_WRITE = 1;

	/**
	 * Low-level encode IOC message value.
	 *
	 * @param dir
	 *        the direction
	 * @param type
	 *        the type
	 * @param nr
	 *        the number of messages
	 * @param size
	 *        the size of each message
	 * @return the encoded IOC message
	 */
	public static long ioc(int dir, int type, int nr, int size) {
		return ((long) dir << IOC_DIRSHIFT) | ((long) type << IOC_TYPESHIFT) | ((long) nr << IOC_NRSHIFT)
				| ((long) size << IOC_SIZESHIFT);
	}

	/**
	 * Low-level encode IOW message value.
	 *
	 * @param type
	 *        the type
	 * @param nr
	 *        the number of messages
	 * @param size
	 *        the size of each message
	 * @return the encoded IOW message
	 */
	public static long iow(int type, int nr, int size) {
		return ioc(IOC_WRITE, type, nr, size);
	}

	// --- SPI_IOC_* request codes (linux/spi/spidev.h) -----------------------
	static final int SPI_IOC_MAGIC = 'k';

	/**
	 * The IOC transfer size.
	 * <p>
	 * struct spi_ioc_transfer is 32 bytes, identical on 32- and 64-bit.
	 */
	public static final int SPI_IOC_TRANSFER_SIZE = 32;

	/** IOW message to write MODE32. */
	public static final long SPI_IOC_WR_MODE32 = iow(SPI_IOC_MAGIC, 5, 4);

	/** IOW message to write max speed (Hz). */
	public static final long SPI_IOC_WR_MAX_SPEED_HZ = iow(SPI_IOC_MAGIC, 4, 4);

	/** IOW message to write bits per word size. */
	public static final long SPI_IOC_WR_BITS_PER_WORD = iow(SPI_IOC_MAGIC, 3, 1);

	/**
	 * Low-level encode IOC message.
	 *
	 * @param n
	 *        the number of messages
	 * @return the encoded IOW message
	 */
	public static long spiIocMessage(int n) {
		return iow(SPI_IOC_MAGIC, 0, n * SPI_IOC_TRANSFER_SIZE);
	}

	// --- struct spi_ioc_transfer field offsets -----------------------------
	private static final int XFER_TX_BUF = 0; // __u64
	private static final int XFER_RX_BUF = 8; // __u64
	private static final int XFER_LEN = 16; // __u32
	private static final int XFER_SPEED_HZ = 20; // __u32
	private static final int XFER_DELAY_USECS = 24; // __u16
	private static final int XFER_BITS_PER_WORD = 26; // __u8
	private static final int XFER_CS_CHANGE = 27; // __u8

	/**
	 * Minimal libc binding: just enough for spidev access.
	 *
	 * <p>
	 * Each method declares {@link LastErrorException} so JNA captures
	 * {@code errno} on failure. Using JNA direct mapping for better
	 * performance.
	 * </p>
	 */
	static class C {

		public static native int open(String pathname, int flags) throws LastErrorException;

		public static native int close(int fd) throws LastErrorException;

		public static native int ioctl(int fd, NativeLong request, Pointer arg)
				throws LastErrorException;

		static {
			Native.register(Platform.C_LIBRARY_NAME);
		}

	}

	private final String path;
	private int fd = -1;
	private boolean multiTransfer = true;

	/**
	 * Construct for a SPI bus and chip-select.
	 *
	 * @param busNumber
	 *        the SPI bus number (the {@code X} in {@code /dev/spidevX.Y})
	 * @param chipSelect
	 *        the chip-select number (the {@code Y} in {@code /dev/spidevX.Y})
	 */
	public JnaSpiDevice(int busNumber, int chipSelect) {
		this("/dev/spidev" + busNumber + "." + chipSelect);
	}

	/**
	 * Construct for an explicit device path.
	 *
	 * @param path
	 *        the device path, for example {@code /dev/spidev0.0}
	 */
	public JnaSpiDevice(String path) {
		super();
		this.path = path;
	}

	/**
	 * Enable or disable multi-transfer batching.
	 *
	 * <p>
	 * When enabled (the default), {@link #transfer(byte[][], int)} and
	 * {@link #batch(byte[][], int)} issue one {@code SPI_IOC_MESSAGE} for the
	 * whole batch. Disable it to fall back to one {@code ioctl} per frame if a
	 * particular SPI controller or device-tree chip-select configuration
	 * mishandles {@code cs_change} within a message. Only takes effect for
	 * batches created afterwards.
	 * </p>
	 *
	 * @param batch
	 *        {@code true} to batch, {@code false} to issue one transfer per
	 *        frame
	 */
	public void setMultiTransfer(boolean batch) {
		this.multiTransfer = batch;
	}

	/**
	 * Release the JNA direct-mapping trampolines bound to the private libc
	 * binding.
	 *
	 * <p>
	 * JNA's {@code Native.register} allocates a native handle per mapped method
	 * and keeps the mapping in a {@code WeakHashMap} keyed by the binding
	 * class. In a framework where this bundle may be stopped and reinstalled,
	 * call this once from the bundle's stop path (see {@code Activator}) so
	 * that class and its handles can be garbage-collected promptly rather than
	 * lingering. The shared {@code NativeLibrary} for libc is <em>not</em>
	 * touched — other bundles keep using it.
	 * </p>
	 *
	 * <p>
	 * A no-op if the binding was never used, and safe to call more than once.
	 * Do not call it while a {@link JnaSpiDevice} is still in use: the libc
	 * methods become unlinked, and only a fresh class load (the next bundle
	 * start) rebinds them.
	 * </p>
	 */
	public static void unregisterNativeMethods() {
		Native.unregister(C.class);
	}

	@Override
	public synchronized void open(int mode, int maxSpeedHz, int bitsPerWord) {
		if ( fd >= 0 ) {
			return;
		}
		int f;
		try {
			f = C.open(path, O_RDWR | O_CLOEXEC);
		} catch ( LastErrorException e ) {
			throw new SpiException("Unable to open SPI device " + path + ": " + e.getMessage(), e);
		}
		if ( f < 0 ) {
			throw new SpiException("Unable to open SPI device " + path);
		}
		this.fd = f;
		try {
			ioctlWriteInt(SPI_IOC_WR_MODE32, 4, mode);
			ioctlWriteInt(SPI_IOC_WR_BITS_PER_WORD, 1, bitsPerWord);
			ioctlWriteInt(SPI_IOC_WR_MAX_SPEED_HZ, 4, maxSpeedHz);
		} catch ( RuntimeException e ) {
			close();
			throw e;
		}
	}

	private void ioctlWriteInt(long request, int sizeBytes, int value) {
		try (Memory m = new Memory(sizeBytes)) {
			if ( sizeBytes == 1 ) {
				m.setByte(0, (byte) value);
			} else {
				m.setInt(0, value);
			}
			int rc = C.ioctl(fd, new NativeLong(request, true), m);
			if ( rc < 0 ) {
				throw new SpiException("ioctl(0x" + Long.toHexString(request) + ") failed on " + path);
			}
		} catch ( LastErrorException e ) {
			throw new SpiException("ioctl(0x" + Long.toHexString(request) + ") failed on " + path + ": "
					+ e.getMessage(), e);
		}
	}

	@Override
	public synchronized byte[] transfer(byte[] tx) {
		if ( fd < 0 ) {
			throw new SpiException("SPI device " + path + " is not open");
		}
		final int len = tx.length;
		if ( len == 0 ) {
			return new byte[0];
		}
		try (Memory txMem = new Memory(len);
				Memory rxMem = new Memory(len);
				Memory msg = new Memory(SPI_IOC_TRANSFER_SIZE)) {
			txMem.write(0, tx, 0, len);
			rxMem.clear();
			msg.clear();
			msg.setLong(XFER_TX_BUF, Pointer.nativeValue(txMem));
			msg.setLong(XFER_RX_BUF, Pointer.nativeValue(rxMem));
			msg.setInt(XFER_LEN, len);
			msg.setInt(XFER_SPEED_HZ, 0); // 0 == use the bus default set at open()
			msg.setByte(XFER_BITS_PER_WORD, (byte) 0); // 0 == use the bus default

			int rc = C.ioctl(fd, new NativeLong(spiIocMessage(1), true), msg);
			if ( rc < 0 ) {
				throw new SpiException("SPI_IOC_MESSAGE failed on " + path);
			}
			byte[] rx = rxMem.getByteArray(0, len);
			// keep the native buffers reachable until the ioctl has returned
			Reference.reachabilityFence(txMem);
			Reference.reachabilityFence(rxMem);
			return rx;
		} catch ( LastErrorException e ) {
			throw new SpiException("SPI transfer failed on " + path + ": " + e.getMessage(), e);
		}
	}

	@Override
	public synchronized SpiDevice.Batch batch(byte[][] txFrames, int settleMicros) {
		if ( fd < 0 ) {
			throw new SpiException("SPI device " + path + " is not open");
		}
		if ( txFrames.length > 1 && multiTransfer ) {
			return new MultiTransferBatch(txFrames, settleMicros);
		}
		// one frame, or batching disabled: fall back to one ioctl per frame
		return SpiDevice.super.batch(txFrames, settleMicros);
	}

	/**
	 * A {@link SpiDevice.Batch} that issues one {@code SPI_IOC_MESSAGE} per
	 * {@link #transfer()}, reusing the three native buffers it allocates up
	 * front: the transmit and receive data blocks and the
	 * {@code spi_ioc_transfer[]} array (populated once here, since only the
	 * transmit bytes change between calls).
	 */
	private final class MultiTransferBatch implements SpiDevice.Batch {

		private final byte[][] txFrames;
		private final int[] offsets;
		private final int n;
		private final Memory txMem;
		private final Memory rxMem;
		private final Memory msg;
		private boolean closed;

		MultiTransferBatch(byte[][] txFrames, int settleMicros) {
			this.n = txFrames.length;
			this.txFrames = txFrames;
			this.offsets = new int[n];
			int total = 0;
			for ( int i = 0; i < n; i++ ) {
				offsets[i] = total;
				total += txFrames[i].length;
			}
			short delay = (short) Math.max(0, Math.min(0xFFFF, settleMicros));
			this.txMem = new Memory(total == 0 ? 1 : total);
			this.rxMem = new Memory(total == 0 ? 1 : total);
			this.msg = new Memory((long) n * SPI_IOC_TRANSFER_SIZE);
			rxMem.clear();
			msg.clear();
			long txBase = Pointer.nativeValue(txMem);
			long rxBase = Pointer.nativeValue(rxMem);
			for ( int i = 0; i < n; i++ ) {
				int s = i * SPI_IOC_TRANSFER_SIZE;
				msg.setLong(s + XFER_TX_BUF, txBase + offsets[i]);
				msg.setLong(s + XFER_RX_BUF, rxBase + offsets[i]);
				msg.setInt(s + XFER_LEN, txFrames[i].length);
				msg.setInt(s + XFER_SPEED_HZ, 0); // use the bus default
				msg.setShort(s + XFER_DELAY_USECS, delay);
				msg.setByte(s + XFER_BITS_PER_WORD, (byte) 0);
				// cs_change=1 toggles chip-select before the next transfer, making
				// each frame its own bus transaction. On the last transfer its
				// meaning inverts (it would *hold* CS asserted), so leave it 0
				// there - CS is released after the final transfer regardless.
				msg.setByte(s + XFER_CS_CHANGE, (byte) (i < n - 1 ? 1 : 0));
			}
		}

		@Override
		public byte[][] transfer() {
			synchronized ( JnaSpiDevice.this ) {
				if ( closed ) {
					throw new SpiException("SPI batch on " + path + " is closed");
				}
				if ( fd < 0 ) {
					throw new SpiException("SPI device " + path + " is not open");
				}
				for ( int i = 0; i < n; i++ ) {
					byte[] frame = txFrames[i];
					if ( frame.length > 0 ) {
						txMem.write(offsets[i], frame, 0, frame.length);
					}
				}
				try {
					int rc = C.ioctl(fd, new NativeLong(spiIocMessage(n), true), msg);
					if ( rc < 0 ) {
						throw new SpiException("SPI_IOC_MESSAGE(" + n + ") failed on " + path);
					}
					byte[][] out = new byte[n][];
					for ( int i = 0; i < n; i++ ) {
						out[i] = rxMem.getByteArray(offsets[i], txFrames[i].length);
					}
					// keep the batch (and its native buffers) reachable across the ioctl
					Reference.reachabilityFence(this);
					return out;
				} catch ( LastErrorException e ) {
					throw new SpiException(
							"SPI batch transfer failed on " + path + ": " + e.getMessage(), e);
				}
			}
		}

		@Override
		public void close() {
			synchronized ( JnaSpiDevice.this ) {
				if ( closed ) {
					return;
				}
				closed = true;
				txMem.close();
				rxMem.close();
				msg.close();
			}
		}
	}

	@Override
	public synchronized void close() {
		if ( fd < 0 ) {
			return;
		}
		int f = fd;
		fd = -1;
		try {
			C.close(f);
		} catch ( LastErrorException e ) {
			// best effort on close
		}
	}

}
