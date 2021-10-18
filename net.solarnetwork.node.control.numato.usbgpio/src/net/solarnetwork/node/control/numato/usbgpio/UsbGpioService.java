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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.BitSet;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.io.serial.SerialConnectionAction;
import net.solarnetwork.util.ByteList;

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

	private final SerialConnection conn;
	private long listenWaitMs = DEFAULT_LISTEN_WAIT_MS;
	private int gpioCount = DEFAULT_GPIO_COUNT;

	/**
	 * Constructor.
	 * 
	 * @param conn
	 *        the serial connection to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UsbGpioService(SerialConnection conn) {
		super();
		this.conn = requireNonNullArgument(conn, "conn");
	}

	private <T> T perform(SerialConnectionAction<T> action) throws IOException {
		return action.doWithConnection(conn);
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

	private static String responseValue(CharSequence cmd, byte[] response) {
		final int start = cmd.length() + 1; // +1 because response sends \n\r breaks
		if ( response == null || response.length <= start ) {
			return null;
		}
		int end = response.length;
		if ( end > 2 ) {
			if ( response[end - 1] == '>' ) {
				end--;
			}
			if ( response[end - 1] == '\r' ) {
				end--;
			}
			if ( response[end - 1] == '\n' ) {
				end--;
			}
		}
		if ( end <= start ) {
			return null;
		}
		return new String(response, start, end - start, US_ASCII);
	}

	private byte[] drainInputBuffer(SerialConnection conn) throws IOException {
		ByteList buf = new ByteList();
		while ( true ) {
			sleep();
			byte[] data = conn.drainInputBuffer();
			if ( data == null || data.length < 1 ) {
				break;
			}
			buf.addAll(data);
		}
		return buf.toArrayValue();
	}

	@Override
	public String getDeviceVersion() throws IOException {
		return perform(new SerialConnectionAction<String>() {

			@Override
			public String doWithConnection(SerialConnection conn) throws IOException {
				StringBuilder buf = new StringBuilder();
				buf.append(UsbGpioCommand.Version.getCommand());
				buf.append('\r');
				conn.writeMessage(buf.toString().getBytes(US_ASCII));
				byte[] data = drainInputBuffer(conn);
				return responseValue(buf, data);
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
				buf.append('\r');
				conn.writeMessage(buf.toString().getBytes(US_ASCII));
				byte[] data = drainInputBuffer(conn);
				return responseValue(buf, data);
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
				buf.append('\r');
				conn.writeMessage(buf.toString().getBytes(US_ASCII));
				drainInputBuffer(conn);
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
				buf.append('\r');
				conn.writeMessage(buf.toString().getBytes(US_ASCII));
				byte[] data = drainInputBuffer(conn);
				String resp = responseValue(buf, data);
				if ( "1".equals(resp) ) {
					return true;
				}
				return false;
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
				buf.append('\r');
				conn.writeMessage(buf.toString().getBytes(US_ASCII));
				drainInputBuffer(conn);
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
				buf.append('\r');
				conn.writeMessage(buf.toString().getBytes(US_ASCII));
				byte[] data = drainInputBuffer(conn);
				String resp = responseValue(buf, data);
				if ( resp != null && !resp.isEmpty() ) {
					try {
						return Integer.valueOf(resp);
					} catch ( NumberFormatException e ) {
						throw new IOException("Error parsing " + UsbGpioCommand.AdcRead.getCommand()
								+ " response [" + resp + "]: " + e.getMessage(), e);
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
				buf.append('\r');
				conn.writeMessage(buf.toString().getBytes(US_ASCII));
				byte[] data = drainInputBuffer(conn);
				String resp = responseValue(buf, data);
				final BitSet result;
				if ( resp != null && !resp.isEmpty() ) {
					try {
						long l = Long.parseUnsignedLong(resp, 16);
						result = BitSet.valueOf(new long[] { l });
					} catch ( NumberFormatException e ) {
						throw new IOException("Error parsing " + UsbGpioCommand.GpioReadAll.getCommand()
								+ " response [" + resp + "]: " + e.getMessage(), e);
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
				buf.append('\r');
				conn.writeMessage(buf.toString().getBytes(US_ASCII));
				drainInputBuffer(conn);
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
				buf.append('\r');
				conn.writeMessage(buf.toString().getBytes(US_ASCII));
				drainInputBuffer(conn);
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
				buf.append('\r');
				conn.writeMessage(buf.toString().getBytes(US_ASCII));
				drainInputBuffer(conn);
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
