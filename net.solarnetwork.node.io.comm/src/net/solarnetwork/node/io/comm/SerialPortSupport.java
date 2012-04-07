/* ===================================================================
 * SerialPortSupport.java
 * 
 * Created Aug 19, 2009 11:25:27 AM
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

package net.solarnetwork.node.io.comm;

import java.util.TooManyListenersException;

import javax.comm.SerialPort;
import javax.comm.SerialPortEventListener;
import javax.comm.UnsupportedCommOperationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A base class with properties to support {@link SerialPort} communication.
 * 
 * <p>The {@code dataBits}, {@code stopBits}, and {@code parity} class properties
 * should be initialized to values corresponding to the constants defined in the
 * {@link SerialPort} class (e.g. {@link SerialPort#DATABITS_8}, etc.).</p>
 * 
 * <p>The configurable propertis of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt>buad</dt>
 *   <dd>The SerialPort coumminication speed. Defaults to {@code 19200}.</dd>
 *   
 *   <dt>dataBits</dt>
 *   <dd>The SerialPort number of data bits. Defaults to 
 *   {@link SerialPort#DATABITS_8}.</dd>
 *   
 *   <dt>stopBits</dt>
 *   <dd>The SerialPort number of stop bits. Defaults to 
 *   {@link SerialPort#STOPBITS_1}.</dd>
 *   
 *   <dt>parity</dt>
 *   <dd>The SerialPort partiy setting to use. Defaults to 
 *   {@link SerialPort#PARITY_NONE}.</dd>
 *   
 *   <dt>flowControl</dt>
 *   <dd>The SerialPort flow control setting to use. If less than 0 will not be 
 *   configured. Defaults to -1.</dd>
 *   
 *   <dt>receiveThreshold</dt>
 *   <dd>The SerialPort receive threshold setting. Defaults to {@code 40}. If 
 *   set to anything less than 0 then no receive threshold will be disabled.</dd>
 *   
 *   <dt>receiveTimeout</dt>
 *   <dd>The SerialPort receive timeout setting. Defaults to {@code -1}. If 
 *   set to anything less than 0 then no receive timeout will be disabled.</dd>
 *   
 *   <dt>dtr</dt>
 *   <dd>The SerialPort DTR setting to use. Defaults to {@code true}.</dd>
 *   
 *   <dt>rts</dt>
 *   <dd>The SerialPort RTS setting to use. Defaults to {@code false}.</dd>
 *   
 * </dl>
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public abstract class SerialPortSupport {

	/** The SerialPort. */
	protected SerialPort serialPort;
	
	private long maxWait;
	
	private int baud = 19200;
	private int dataBits = SerialPort.DATABITS_8;
	private int stopBits = SerialPort.STOPBITS_1;
	private int parity = SerialPort.PARITY_NONE;
	private int flowControl = -1;
	private int receiveThreshold = 40;
	private int receiveTimeout = -1;
	private boolean dtr = true;
	private boolean rts = false;
	
	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	/**
	 * Constructor.
	 * 
	 * @param serialPort the SerialPort to use
	 * @param maxWait the maximum number of milliseconds to wait when waiting
	 * to read data
	 */
	public SerialPortSupport(SerialPort serialPort, long maxWait) {
		this.serialPort = serialPort;
		this.maxWait = maxWait;
	}
	
	/**
	 * Close the connected serial port.
	 */
	protected void closeSerialPort() {
		if ( this.serialPort != null ) {
			this.serialPort.close();
		}
	}

	/**
	 * Set up the SerialPort for use, configuring with class properties.
	 * 
	 * <p>This method can be called once when wanting to start using the 
	 * serial port.</p>
	 * 
	 * @param listener a listener to pass to {@link SerialPort#addEventListener(SerialPortEventListener)}
	 */
	protected void setupSerialPortParameters(SerialPortEventListener listener) {
		if ( listener != null ) {
			try {
				serialPort.addEventListener(listener);
			} catch ( TooManyListenersException e ) {
				throw new RuntimeException(e);
			}
		}

		//make sure dataavailable triggers work
		serialPort.notifyOnDataAvailable(true);

		//set serial port specifics
		try {

			//these timing settings might be important
			serialPort.disableReceiveFraming();
			
			if ( this.receiveTimeout >= 0 ) {
				serialPort.enableReceiveTimeout(this.receiveTimeout);
				if ( !serialPort.isReceiveTimeoutEnabled() ) {
					log.warn("Receive timeout configured as ["
							+this.receiveTimeout +"] but not supported by driver.");
				} else if ( log.isDebugEnabled() ) {
					log.debug("Receive timeout set to [" +this.receiveTimeout +']');
				}
			} else {
				serialPort.disableReceiveTimeout();
			}
			if ( this.receiveThreshold >= 0 ) {
				serialPort.enableReceiveThreshold(this.receiveThreshold);
				if ( !serialPort.isReceiveThresholdEnabled() ) {
					log.warn("Receive threshold configured as ["
							+this.receiveThreshold +"] but not supported by driver.");
				} else if ( log.isDebugEnabled() ) {
					log.debug("Receive threshold set to [" +this.receiveThreshold +']');
				}
			} else {
				serialPort.disableReceiveThreshold();
			}

			serialPort.setSerialPortParams(this.baud, this.dataBits,
					this.stopBits, this.parity);

			if ( getFlowControl() >= 0 ) {
				serialPort.setFlowControlMode(this.flowControl);
			}

			serialPort.setDTR(this.dtr);
			serialPort.setRTS(this.rts);
			
		} catch ( UnsupportedCommOperationException e ) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @return the serialPort
	 */
	public SerialPort getSerialPort() {
		return serialPort;
	}
	
	/**
	 * @return the maxWait
	 */
	public long getMaxWait() {
		return maxWait;
	}

	/**
	 * @return the baud
	 */
	public int getBaud() {
		return baud;
	}
	
	/**
	 * @param baud the baud to set
	 */
	public void setBaud(int baud) {
		this.baud = baud;
	}
	
	/**
	 * @return the dataBits
	 */
	public int getDataBits() {
		return dataBits;
	}
	
	/**
	 * @param dataBits the dataBits to set
	 */
	public void setDataBits(int dataBits) {
		this.dataBits = dataBits;
	}
	
	/**
	 * @return the stopBits
	 */
	public int getStopBits() {
		return stopBits;
	}
	
	/**
	 * @param stopBits the stopBits to set
	 */
	public void setStopBits(int stopBits) {
		this.stopBits = stopBits;
	}
	
	/**
	 * @return the parity
	 */
	public int getParity() {
		return parity;
	}
	
	/**
	 * @param parity the parity to set
	 */
	public void setParity(int parity) {
		this.parity = parity;
	}
	
	/**
	 * @return the receiveThreshold
	 */
	public int getReceiveThreshold() {
		return receiveThreshold;
	}
	
	/**
	 * @param receiveThreshold the receiveThreshold to set
	 */
	public void setReceiveThreshold(int receiveThreshold) {
		this.receiveThreshold = receiveThreshold;
	}
	
	/**
	 * @return the dtr
	 */
	public boolean isDtr() {
		return dtr;
	}
	
	/**
	 * @param dtr the dtr to set
	 */
	public void setDtr(boolean dtr) {
		this.dtr = dtr;
	}
	
	/**
	 * @return the rts
	 */
	public boolean isRts() {
		return rts;
	}

	/**
	 * @param rts the rts to set
	 */
	public void setRts(boolean rts) {
		this.rts = rts;
	}
	
	/**
	 * @return the flowControl
	 */
	public int getFlowControl() {
		return flowControl;
	}
	
	/**
	 * @param flowControl the flowControl to set
	 */
	public void setFlowControl(int flowControl) {
		this.flowControl = flowControl;
	}
	
	/**
	 * @return the receiveTimeout
	 */
	public int getReceiveTimeout() {
		return receiveTimeout;
	}
	
	/**
	 * @param receiveTimeout the receiveTimeout to set
	 */
	public void setReceiveTimeout(int receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}
	
}
