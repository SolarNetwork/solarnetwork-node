/* ==================================================================
 * TestSerialPortInputStream.java - Oct 27, 2014 8:33:32 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.serial.rxtx.test;

import java.io.IOException;
import java.io.InputStream;

/**
 * Implementation of {@link InputStream} to help with serial testing.
 * 
 * @author matt
 * @version 1.0
 */
public class TestSerialPortInputStream extends InputStream {

	private final InputStream stream;
	private long initialDelay = 200;
	private long chunkDelay = 100;
	private int chunkSize = 4;

	private long bytesRead;
	private int chunkCounter;

	/**
	 * Constructor.
	 * 
	 * @param stream
	 *        the InputStream to read bytes from
	 * @param initialDelay
	 *        the inital delay, in milliseconds, before returning the first byte
	 * @param chunkSize
	 *        the size of the input "buffer" before {@code chunkDelay} is
	 *        applied
	 * @param chunkDelay
	 *        a delay, in milliseconds, to apply after {@code chunkSize} bytes
	 *        have been returned
	 */
	public TestSerialPortInputStream(InputStream stream, long initialDelay, int chunkSize,
			long chunkDelay) {
		super();
		this.stream = stream;
		this.initialDelay = initialDelay;
		this.chunkSize = chunkSize;
		this.chunkDelay = chunkDelay;
	}

	private void snooze(long delay) {
		if ( delay < 1 ) {
			return;
		}
		try {
			Thread.sleep(delay);
		} catch ( InterruptedException e ) {
			// ignore this
		}
	}

	@Override
	public int read() throws IOException {
		if ( bytesRead == 0 ) {
			snooze(initialDelay);
		}
		chunkCounter += 1;
		if ( chunkCounter >= chunkSize ) {
			chunkCounter = 0;
			snooze(chunkDelay);
		}
		int result = stream.read();
		bytesRead += 1;
		return result;
	}

	@Override
	public int read(byte[] b) throws IOException {
		if ( bytesRead == 0 ) {
			snooze(initialDelay);
		}
		int readSize = (chunkSize - chunkCounter);
		if ( readSize < 1 ) {
			chunkCounter = 0;
			snooze(chunkDelay);
			readSize = chunkSize;
		}
		if ( readSize > b.length ) {
			readSize = b.length;
		}
		int result = stream.read(b, 0, readSize);
		chunkCounter += result;
		bytesRead += result;
		return result;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if ( bytesRead == 0 ) {
			snooze(initialDelay);
		}
		int readSize = (chunkSize - chunkCounter);
		if ( readSize < 1 ) {
			chunkCounter = 0;
			snooze(chunkDelay);
			readSize = chunkSize;
		}
		if ( readSize + off + len > b.length ) {
			readSize = b.length - off;
		}
		int result = stream.read(b, off, readSize);
		chunkCounter += result;
		bytesRead += result;
		return result;
	}

	@Override
	public int available() throws IOException {
		return stream.available();
	}

}
