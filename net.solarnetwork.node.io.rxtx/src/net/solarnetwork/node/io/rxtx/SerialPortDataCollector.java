/* ===================================================================
 * SerialPortDataCollector.java
 * 
 * Created Aug 7, 2008 9:48:26 PM
 * 
 * Copyright (c) 2008 Solarnetwork.net Dev Team.
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
 * ===================================================================
 */

package net.solarnetwork.node.io.rxtx;

import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Read the serial port stream, looking for a "magic" byte sequence, and then
 * collecting a fixed number of bytes after that.
 * 
 * @author matt
 * @version 1.1
 */
public class SerialPortDataCollector extends AbstractSerialPortDataCollector {

	private final byte[] magic;
	private final int readSize;

	/**
	 * Construct with a port and default settings.
	 * 
	 * @param port
	 *        the port
	 */
	public SerialPortDataCollector(SerialPort port) {
		this(port, 4096, new byte[] { 0 }, 8, 2000);
	}

	/**
	 * Constructor.
	 * 
	 * @param port
	 *        the port to read
	 * @param bufferSize
	 *        the maximum number of bytes to read at one time
	 * @param magic
	 *        the "magic" byte sequence to start reading after
	 * @param readSize
	 *        the desired number of bytes to return
	 * @param maxWait
	 *        the maximum number of milliseconds to wait for the data to be
	 *        collected
	 */
	public SerialPortDataCollector(SerialPort port, int bufferSize, byte[] magic, int readSize,
			long maxWait) {
		super(port, maxWait, bufferSize);
		if ( bufferSize < readSize ) {
			throw new IllegalArgumentException(
					"The bufferSize value must not be less than the readSize value");
		}
		this.magic = magic;
		this.readSize = readSize;
	}

	@Override
	public byte[] getCollectedData() {
		byte[] result = super.getCollectedData();
		if ( result.length < readSize ) {
			return null;
		}
		return result;
	}

	@Override
	public void stopCollecting() {
		closeSerialPort();
	}

	@Override
	public String getCollectedDataAsString() {
		String result = super.getCollectedDataAsString();
		if ( result == null || result.length() < readSize ) {
			return null;
		}
		return result;
	}

	@Override
	protected boolean handleSerialEventInternal(SerialPortEvent event, InputStream in,
			ByteArrayOutputStream buffer) {
		boolean done;
		try {
			done = handleSerialEvent(event, in, buffer, magic, readSize);
		} catch ( Exception e ) {
			done = true;
		}
		return done;
	}

}
