/* ===================================================================
 * SerialPortDataCollectorFactory.java
 * 
 * Created Jul 23, 2009 9:23:57 AM
 * 
 * Copyright (c) 2009 Solarnetwork.net Dev Team.
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

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;

/**
 * {@link ObjectFactory} for {@link SerialPortDataCollector} objects.
 * 
 * <p>Configure the properties of this class, then calls to {@link #getObject()} will
 * return new instances of {@link SerialPortDataCollector} for each invocation, 
 * configured with this object's property values.</p>
 * 
 * <p>At a minimum, the following properties should be configured:</p>
 * 
 * <dl class="class-properties">
 *   <dt>serialPort</dt>
 *   <dd>The name of the serial port device to use, e.g. {@code /dev/ttyS0}.
 *   Defaults to {@code /dev/ttyUSB0}.</dd>
 *   
 *   <dt>magic</dt>
 *   <dd>The "magic" byte sequence to look for in the serial data stream
 *   that signals the starting point of the data to collect. The collector
 *   will examine discard bytes from the serial data stream until this 
 *   "magic" sequence is found. Use the {@code magicBytes} property to
 *   configure this value as a {@code String}.</dd>
 *   
 *   <dt>readSize</dt>
 *   <dd>The maximum number of bytes of data to collect. The collector will
 *   attempt to collect this number of bytes from the serial port, but the
 *   actual amount collected could be less if the {@code receiveTimeout}
 *   property is configured and the timeout value is reached before the
 *   data buffer is filled.</dd>
 *   
 *   <dt>bufferSize</dt>
 *   <dd>The maximum number of bytes of read at one time. This must be 
 *   equal to or larger than the {@code readSize} property. Often this
 *   can be specified as the same value as {@code readSize}.</dd>
 *   
 *   <dt>maxWait</dt>
 *   <dd>The maximum number of milliseconds to wait for serial data to 
 *   be collected. This is different from the {@code receiveTimeout}
 *   property that is configured on the actual {@code SerialPort} object,
 *   which is not supported by all serial drivers. This property can
 *   be configured for any device.</dd>
 *   
 * </dl>
 * 
 * <p>The other configurable properties are the same as available on the
 * {@link SerialPortDataCollector} class, see that class for more information.</p>
 *
 * @author matt
 * @see SerialPortDataCollector
 * @version $Revision$ $Date$
 */
public class SerialPortDataCollectorFactory extends AbstractSerialPortSupportFactory
implements ObjectFactory<SerialPortDataCollector> {

	private String commPortAppName = getClass().getName();

	private int bufferSize;
	private byte[] magic;
	private int readSize;
	private boolean toggleDtr = true;
	private boolean toggleRts = true;
	
	private CommPortIdentifier portId = null;

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.ObjectFactory#getObject()
	 */
	public SerialPortDataCollector getObject() throws BeansException {
		if ( this.portId == null ) {
			this.portId = getCommPortIdentifier();
		}
		try {
			//establish the serial port connection
			SerialPort port = (SerialPort)portId.open(this.commPortAppName, 2000);
			SerialPortDataCollector obj = new SerialPortDataCollector(
					port, bufferSize, magic, readSize, getMaxWait());
			setupSerialPortSupport(obj);
			obj.setToggleDtr(toggleDtr);
			obj.setToggleRts(toggleRts);
			return obj;
		} catch ( PortInUseException e ) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Set the magic bytes as a String, using the default character encoding.
	 * 
	 * @param magicBytes the magic bytes String
	 */
	public void setMagicBytes(String magicBytes) {
		this.magic = magicBytes.getBytes();
	}
	
	/**
	 * Get the magic bytes as a String, using the default character encoding.
	 * 
	 * @return the magic bytes as a String
	 */
	public String getMagicBytes() {
		return new String(this.magic);
	}
	
	/**
	 * @return the readSize
	 */
	public int getReadSize() {
		return readSize;
	}
	
	/**
	 * @param readSize the readSize to set
	 */
	public void setReadSize(int readSize) {
		this.readSize = readSize;
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
	
	/**
	 * @return the bufferSize
	 */
	public int getBufferSize() {
		return bufferSize;
	}
	
	/**
	 * @param bufferSize the bufferSize to set
	 */
	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}
	
	/**
	 * @return the commPortAppName
	 */
	public String getCommPortAppName() {
		return commPortAppName;
	}
	
	/**
	 * @param commPortAppName the commPortAppName to set
	 */
	public void setCommPortAppName(String commPortAppName) {
		this.commPortAppName = commPortAppName;
	}
	
}
