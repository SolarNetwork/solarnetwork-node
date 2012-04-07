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
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.node.io.rxtx;

import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import net.solarnetwork.node.DataCollector;

/**
 * Read the serial port stream, looking for a "magic" byte sequence, and then
 * collecting a fixed number of bytes after that.
 * 
 * <p>After constructing an instance of this class, you can configure various
 * communication settings from the class properties (e.g. {@link #setStopBits(int)}).
 * Then call the {@link #collectData()} method to start collecting the serial 
 * data. This method will start collecting serial data via a separate thread,
 * and block until {@code readSize} bytes have been collected after finding the 
 * "magic" byte sequence or until </p>
 * 
 * <p>The {@code dataBits}, {@code stopBits}, and {@code parity} class properties
 * should be initialized to values corresponding to the constants defined in the
 * {@link SerialPort} class (e.g. {@link SerialPort#DATABITS_8}, etc.).</p>
 * 
 * <p>The configurable propertis of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt>toggleDtr</dt>
 *   <dd>If <em>true</em> then toggle the DTR setting before closing the SerialPort.
 *   Defaults to {@code true}.</dd>
 *   
 *   <dt>toggleRts</dt>
 *   <dd>If <em>true</em> then toggle the RTS setting before closing the SerialPort.
 *   Defaults to {@code true}.</dd>
 * </dl>
 * 
 * @author matt
 * @version $Revision$ $Date$
 */
public class SerialPortDataCollector extends SerialPortSupport
implements DataCollector, SerialPortEventListener {

	private byte[] buf;
	private byte[] magic;
	private int readSize;
	
	private boolean toggleDtr = true;
	private boolean toggleRts = true;
	
	private boolean collectData = false;
	private int bufPtr = 0;
	private InputStream in = null;
	private boolean magicFound = false;
	
	/**
	 * Construct with a port and default settings.
	 * 
	 * @param port
	 *            the port
	 */
	public SerialPortDataCollector(SerialPort port) {
		this(port, 4096, new byte[] { 0 }, 8, 2000);
	}

	/**
	 * Constructor.
	 * 
	 * @param port
	 *            the port to read
	 * @param bufferSize
	 *            the maximum number of bytes to read at one time
	 * @param magic
	 *            the "magic" byte sequence to start reading after
	 * @param readSize
	 *            the desired number of bytes to return
	 * @param maxWait
	 *            the maximum number of milliseconds to wait for the data
	 *            to be collected
	 */
	public SerialPortDataCollector(SerialPort port, int bufferSize, 
			byte[] magic, int readSize, long maxWait) {
		super(port, maxWait);
		if ( bufferSize < readSize ) {
			throw new IllegalArgumentException(
					"The bufferSize value must not be less than the readSize value");
		}
		this.buf = new byte[bufferSize];
		this.magic = magic;
		this.readSize = readSize;
	}
	
	@Override
	public int bytesRead() {
		return bufPtr;
	}
	
	@Override
	public byte[] getCollectedData() {
		if ( bufPtr < readSize ) {
			return null;
		}
		// return copy of array so can't modify internal buffer
		byte[] data = new byte[readSize];
		System.arraycopy(buf, 0, data, 0, readSize);
		return data;
	}

	@Override
	public void stopCollecting() {
		closeSerialPort();
	}

	@Override
	public String getCollectedDataAsString() {
		if ( bufPtr < readSize ) {
			return null;
		}
		return new String(buf, 0, readSize);
	}
	
	/**
	 * Collect the data from the serial port.
	 */
	public void collectData() {
		setupSerialPortParameters(this);
		
		// open the input stream
		try {
			this.in = serialPort.getInputStream();
			
			// sleep until we have data
			synchronized (this) {
				this.magicFound = false;
				bufPtr = 0;
				Arrays.fill(buf, (byte)0);
				this.collectData = true;
				this.wait(getMaxWait());
				this.collectData = false;
			}
		} catch ( InterruptedException e ) {
			log.warn("Interrupted, stopping data collection");
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		} finally {
			serialPort.removeEventListener();
			if ( this.in != null ) {
				if ( toggleDtr ) {
					serialPort.setDTR(!isDtr());
				}
				if ( toggleRts ) {
					serialPort.setRTS(!isRts());
				}
				try {
					this.in.close();
				} catch ( IOException e ) {
					// ignore this one
				}
			}
		}
	}

	public void serialEvent(SerialPortEvent event) {
		if ( !collectData || event.getEventType() != SerialPortEvent.DATA_AVAILABLE ) {
			return;
		}
		
		try {
			while ( bufPtr < buf.length && in.available() > 0 ) {
				bufPtr += in.read(buf, bufPtr, buf.length - bufPtr);
			}
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		
		if ( magicFound ) {
			// if we've collected at least desiredSize bytes, we're done
			if ( bufPtr >= readSize ) {
				if ( log.isDebugEnabled() ) {
					log.debug("Got desired " +readSize +" bytes of data");
				}
				bufPtr = readSize;
				synchronized (this) {
					notifyAll();
					return;
				}
			}
			if ( log.isDebugEnabled() ) {
				log.debug("Looking for " +(readSize - bufPtr) +" more bytes of data");
			}
			return;
		}
		
		// look for magic in the buffer
		int magicIdx = 0;
		boolean found = false;
		for ( ; magicIdx < (bufPtr - magic.length); magicIdx++ ) {
			found = true;
			for ( int j = 0; j < magic.length; j++ ) {
				if ( buf[magicIdx+j] != magic[j] ) {
					found = false;
					break;
				}
			}
			if ( found ) {
				break;
			}
		}
		
		if ( found ) {
			// magic found!
			if ( log.isTraceEnabled() ) {
				log.trace("Found magic bytes " +Arrays.toString(magic)
						+" at buffer index " +magicIdx
						+", bufPtr at " +bufPtr);
			}
			magicFound = true;
			if ( magicIdx > 0 ) {
				byte[] tmp = new byte[bufPtr - magicIdx];
				System.arraycopy(buf, magicIdx, tmp, 0, tmp.length);
				System.arraycopy(tmp, 0, buf, 0, tmp.length);
				bufPtr = tmp.length;
			}
			if ( bufPtr >= readSize ) {
				// we got all the data here... we're done
				synchronized (this) {
					notifyAll();
				}
				return;
			}
		} else {
			// magic not found at all, so reset bufPtr back to 0
			bufPtr = 0;
		}
		
		if ( log.isTraceEnabled() ) {
			log.trace("Buffer: " +new String(buf, 0, bufPtr));
		}
	}
	
	/**
	 * @return the toggleDtr
	 */
	public boolean isToggleDtr() {
		return toggleDtr;
	}
	
	/**
	 * @param toggleDtr the toggleDtr to set
	 */
	public void setToggleDtr(boolean toggleDtr) {
		this.toggleDtr = toggleDtr;
	}
	
	/**
	 * @return the toggleRts
	 */
	public boolean isToggleRts() {
		return toggleRts;
	}
	
	/**
	 * @param toggleRts the toggleRts to set
	 */
	public void setToggleRts(boolean toggleRts) {
		this.toggleRts = toggleRts;
	}
	
}