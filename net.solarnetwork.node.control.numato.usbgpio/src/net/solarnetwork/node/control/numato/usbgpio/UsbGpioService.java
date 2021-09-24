/* ==================================================================
 * UsbGpioService.java - 24/09/2021 2:19:41 PM
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

package net.solarnetwork.node.control.numato.usbgpio;

import static net.solarnetwork.service.OptionalService.requiredService;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.BitSet;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.io.serial.SerialConnectionAction;
import net.solarnetwork.node.io.serial.SerialNetwork;
import net.solarnetwork.service.OptionalService;

/**
 * Implementation of the {@link GpioService}.
 * 
 * @author matt
 * @version 1.0
 */
public class UsbGpioService implements GpioService {

	/** The {@code listenWaitMs} property default value. */
	public static final long DEFAULT_LISTEN_WAIT_MS = 200;

	/** The {@code gpioCount} property default value. */
	public static final int DEFAULT_GPIO_COUNT = 8;

	private static final Charset US_ASCII = Charset.forName("US-ASCII");

	private final OptionalService<SerialNetwork> serialNetwork;
	private long listenWaitMs = DEFAULT_LISTEN_WAIT_MS;
	private int gpioCount = DEFAULT_GPIO_COUNT;

	/**
	 * Constructor.
	 * 
	 * @param serialNetwork
	 *        the serial network to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UsbGpioService(OptionalService<SerialNetwork> serialNetwork) {
		super();
		if ( serialNetwork == null ) {
			throw new IllegalArgumentException("The serialNetwork argument must not be null.");
		}
		this.serialNetwork = serialNetwork;
	}

	private <T> T perform(SerialConnectionAction<T> action) throws IOException {
		SerialNetwork n = requiredService(serialNetwork, "SerialNetwork");
		return n.performAction(action);
	}

	private void sleep() {
		sleep(listenWaitMs);
	}

	private static void sleep(long ms) {
		if ( ms < 1 ) {
			return;
		}
		try {
			Thread.sleep(ms);
		} catch ( InterruptedException e ) {
			// ignore
		}
	}

	@Override
	public String getDeviceVersion() throws IOException {
		return perform(new SerialConnectionAction<String>() {

			@Override
			public String doWithConnection(SerialConnection conn) throws IOException {
				conn.writeMessage(UsbGpioCommand.Version.getCommand().getBytes(US_ASCII));
				sleep();
				byte[] data = conn.drainInputBuffer();
				if ( data != null && data.length > 0 ) {
					return new String(data, US_ASCII);
				}
				return null;
			}

		});
	}

	@Override
	public String getId() throws IOException {
		return perform(new SerialConnectionAction<String>() {

			@Override
			public String doWithConnection(SerialConnection conn) throws IOException {
				StringBuilder buf = new StringBuilder();
				buf.append(UsbGpioCommand.IdGet.getCommand());
				conn.writeMessage(buf.toString().getBytes(US_ASCII));
				sleep();
				byte[] data = conn.drainInputBuffer();
				if ( data != null && data.length > 0 ) {
					return new String(data, US_ASCII);
				}
				return null;
			}

		});
	}

	@Override
	public void setId(String id) throws IOException {
		if ( id == null ) {
			return;
		}
		byte[] data = id.getBytes(US_ASCII);
		if ( data.length != 8 ) {
			throw new IllegalArgumentException("The ID value must be exactly 8 characters.");
		}
		perform(new SerialConnectionAction<Void>() {

			@Override
			public Void doWithConnection(SerialConnection conn) throws IOException {
				StringBuilder buf = new StringBuilder();
				buf.append(UsbGpioCommand.IdSet.getCommand());
				buf.append(" ");
				buf.append(id);
				conn.writeMessage(buf.toString().getBytes(US_ASCII));
				return null;
			}

		});
	}

	@Override
	public boolean read(int address) throws IOException {
		return perform(new SerialConnectionAction<Boolean>() {

			@Override
			public Boolean doWithConnection(SerialConnection conn) throws IOException {
				StringBuilder buf = new StringBuilder();
				buf.append(UsbGpioCommand.GpioRead.getCommand());
				buf.append(" ");
				buf.append(address);
				conn.writeMessage(buf.toString().getBytes(US_ASCII));
				sleep();
				byte[] data = conn.drainInputBuffer();
				if ( data != null && data.length > 1 && new String(data, US_ASCII).startsWith("on") ) {
					return true;
				}
				return Boolean.FALSE;
			}

		});
	}

	@Override
	public void set(int address, boolean value) throws IOException {
		perform(new SerialConnectionAction<Void>() {

			@Override
			public Void doWithConnection(SerialConnection conn) throws IOException {
				StringBuilder buf = new StringBuilder();
				if ( value ) {
					buf.append(UsbGpioCommand.GpioSet.getCommand());
				} else {
					buf.append(UsbGpioCommand.GpioClear.getCommand());
				}
				buf.append(" ");
				buf.append(address);
				conn.writeMessage(buf.toString().getBytes(US_ASCII));
				return null;
			}

		});
	}

	@Override
	public int readAnalog(int address) throws IOException {
		return perform(new SerialConnectionAction<Integer>() {

			@Override
			public Integer doWithConnection(SerialConnection conn) throws IOException {
				StringBuilder buf = new StringBuilder();
				buf.append(UsbGpioCommand.AdcRead.getCommand());
				buf.append(" ");
				buf.append(address);
				conn.writeMessage(buf.toString().getBytes(US_ASCII));
				sleep();
				byte[] data = conn.drainInputBuffer();
				if ( data != null && data.length > 1 ) {
					String s = new String(data, US_ASCII);
					try {
						return Integer.valueOf(s);
					} catch ( NumberFormatException e ) {
						throw new IOException("Error parsing " + UsbGpioCommand.AdcRead.getCommand()
								+ " response [" + s + "]: " + e.getMessage(), e);
					}
				}
				return 0;
			}

		});
	}

	@Override
	public BitSet readAll() throws IOException {
		return perform(new SerialConnectionAction<BitSet>() {

			@Override
			public BitSet doWithConnection(SerialConnection conn) throws IOException {
				StringBuilder buf = new StringBuilder();
				buf.append(UsbGpioCommand.GpioReadAll.getCommand());
				conn.writeMessage(buf.toString().getBytes(US_ASCII));
				sleep();
				byte[] data = conn.drainInputBuffer();
				final BitSet result;
				if ( data != null && data.length > 0 ) {
					String s = new String(data, US_ASCII);
					try {
						long l = Long.parseUnsignedLong(s, 16);
						result = BitSet.valueOf(new long[] { l });
					} catch ( NumberFormatException e ) {
						throw new IOException("Error parsing " + UsbGpioCommand.GpioReadAll.getCommand()
								+ " response [" + s + "]: " + e.getMessage(), e);
					}
				} else {
					result = new BitSet();
				}
				return result;
			}

		});
	}

	private String hexCommandValue(BitSet set) {
		long[] setData = (set != null ? set.toLongArray() : null);
		long data = (setData != null && setData.length > 0 ? setData[setData.length - 1] : 0L);
		String s = Long.toHexString(data);
		while ( s.length() < (gpioCount / 8) * 2 ) {
			s = "0" + s;
		}
		return s;
	}

	@Override
	public void writeAll(BitSet values) throws IOException {
		perform(new SerialConnectionAction<Void>() {

			@Override
			public Void doWithConnection(SerialConnection conn) throws IOException {
				StringBuilder buf = new StringBuilder();
				buf.append(UsbGpioCommand.GpioWriteAll.getCommand());
				buf.append(" ");
				buf.append(hexCommandValue(values));
				conn.writeMessage(buf.toString().getBytes(US_ASCII));
				return null;
			}

		});
	}

	@Override
	public void configureWriteMask(BitSet set) throws IOException {
		perform(new SerialConnectionAction<Void>() {

			@Override
			public Void doWithConnection(SerialConnection conn) throws IOException {
				StringBuilder buf = new StringBuilder();
				buf.append(UsbGpioCommand.GpioIoMask.getCommand());
				buf.append(" ");
				buf.append(hexCommandValue(set));
				conn.writeMessage(buf.toString().getBytes(US_ASCII));
				return null;
			}

		});
	}

	@Override
	public void configureIoDirection(BitSet set) throws IOException {
		perform(new SerialConnectionAction<Void>() {

			@Override
			public Void doWithConnection(SerialConnection conn) throws IOException {
				StringBuilder buf = new StringBuilder();
				buf.append(UsbGpioCommand.GpioIoDirection.getCommand());
				buf.append(" ");
				buf.append(hexCommandValue(set));
				conn.writeMessage(buf.toString().getBytes(US_ASCII));
				return null;
			}

		});
	}

	/**
	 * Get the length of time to wait to read responses after writing a command,
	 * in milliseconds.
	 * 
	 * @return the milliseconds to wait, or {@literal 0} to not wait at all;
	 *         defaults to {@link #DEFAULT_LISTEN_WAIT_MS}
	 * 
	 */
	public long getListenWaitMs() {
		return listenWaitMs;
	}

	/**
	 * Set the length of time to wait to read responses after writing a command,
	 * in milliseconds.
	 * 
	 * @param listenWaitMs
	 *        the milliseconds to set, or {@literal 0} to not wait at all
	 */
	public void setListenWaitMs(long listenWaitMs) {
		this.listenWaitMs = listenWaitMs;
	}

	/**
	 * Get the GPIO count.
	 * 
	 * @return the count; defaults to {@link #DEFAULT_GPIO_COUNT}
	 */
	public int getGpioCount() {
		return gpioCount;
	}

	/**
	 * Set the GPIO count.
	 * 
	 * @param gpioCount
	 *        the gpioCount to set
	 * @throws IllegalArgumentException
	 *         if {@code gpioCount} is less than {@literal 1}
	 */
	public void setGpioCount(int gpioCount) {
		if ( gpioCount < 1 ) {
			throw new IllegalArgumentException("The gpioCount value must be greater than 0.");
		}
		this.gpioCount = gpioCount;
	}

}
