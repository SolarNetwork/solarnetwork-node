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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private ByteArrayOutputStream buffer;
	private byte[] magic;
	private int readSize;
	
	private boolean toggleDtr = true;
	private boolean toggleRts = true;
	
	private boolean collectData = false;
	private InputStream in = null;
	private boolean doneCollecting = false;

	private Logger eventLog = LoggerFactory.getLogger(getClass().getName()+".SERIAL_EVENT");

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
		this.buffer = new ByteArrayOutputStream(bufferSize);
		this.magic = magic;
		this.readSize = readSize;
	}
	
	@Override
	public int bytesRead() {
		return buffer.size();
	}
	
	@Override
	public byte[] getCollectedData() {
		if ( buffer.size() < readSize ) {
			return null;
		}
		return buffer.toByteArray();
	}

	@Override
	public void stopCollecting() {
		closeSerialPort();
	}

	@Override
	public String getCollectedDataAsString() {
		if ( buffer.size() < readSize ) {
			return null;
		}
		return buffer.toString();
	}
	
	/**
	 * Collect the data from the serial port.
	 */
	public void collectData() {
		setupSerialPortParameters(this);
		
		// open the input stream
		try {
			this.in = serialPort.getInputStream();
			this.buffer.reset();
			
			// sleep until we have data
			synchronized (this) {
				this.doneCollecting = false;
				this.collectData = true;
				this.wait(getMaxWait());
				this.collectData = false;
			}
			if ( log.isDebugEnabled() && !doneCollecting ) {
				log.debug("Timeout collecting serial data");
			}
		} catch ( InterruptedException e ) {
			log.warn("Interrupted, stopping data collection");
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		} finally {
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
			serialPort.removeEventListener();
		}
	}

	public void serialEvent(SerialPortEvent event) {
		eventLog.trace("SerialPortEvent {}", event.getEventType());
		if ( !collectData || event.getEventType() != SerialPortEvent.DATA_AVAILABLE ) {
			return;
		}
		
		boolean done = handleSerialEvent(event, in, buffer, magic, readSize);
		if ( done ) {
			synchronized (this) {
				doneCollecting = true;
				notifyAll();
			}
			return;
		}
		
		if ( eventLog.isTraceEnabled() ) {
			log.trace("Buffer: {}", Arrays.toString(buffer.toByteArray()));
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