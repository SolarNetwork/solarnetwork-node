/* ==================================================================
 * TestSerialPortOutputStream.java - Oct 27, 2014 12:29:11 PM
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
import java.io.OutputStream;

/**
 * Implementation of {@link OutputStream} to help with serial testing.
 * 
 * @author matt
 * @version 1.0
 */
public class TestSerialPortOutputStream extends OutputStream {

	private final OutputStream stream;
	private long initialDelay = 200;

	private long bytesWritten;

	/**
	 * Constructor.
	 * 
	 * @param stream
	 *        the OutputStream to delegate to
	 * @param initialDelay
	 *        the inital delay, in milliseconds, before returning the first byte
	 */
	public TestSerialPortOutputStream(OutputStream stream, long initialDelay) {
		super();
		this.stream = stream;
		this.initialDelay = initialDelay;
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
	public void write(int b) throws IOException {
		if ( bytesWritten == 0 ) {
			snooze(initialDelay);
		}
		stream.write(b);
	}

	@Override
	public void flush() throws IOException {
		stream.flush();
	}

}
