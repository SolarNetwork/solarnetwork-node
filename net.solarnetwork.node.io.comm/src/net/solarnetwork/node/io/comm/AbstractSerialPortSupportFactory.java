/* ===================================================================
 * AbstractSerialPortSupportFactory.java
 * 
 * Created Aug 19, 2009 12:51:24 PM
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

import java.util.Enumeration;

import javax.comm.CommPortIdentifier;
import javax.comm.SerialPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for factories for {@link SerialPortSupport} implementations.
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public abstract class AbstractSerialPortSupportFactory {
	
	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private String serialPort = "/dev/ttyUSB0";
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
	
	/**
	 * Configure base properties on a {@link SerialPortSupport} instance.
	 * 
	 * @param obj the object to configure
	 */
	protected void setupSerialPortSupport(SerialPortSupport obj) {
		obj.setBaud(baud);
		obj.setDataBits(dataBits);
		obj.setStopBits(stopBits);
		obj.setParity(parity);
		obj.setFlowControl(flowControl);
		obj.setReceiveThreshold(receiveThreshold);
		obj.setReceiveTimeout(receiveTimeout);
		obj.setDtr(dtr);
		obj.setRts(rts);
	}
	
	/**
	 * Locate the {@link CommPortIdentifier} for the configured 
	 * {@link #getSerialPort()} value.
	 * 
	 * <p>This method will throw a RuntimeException if the port is not found.</p>
	 * 
	 * @return the CommPortIdentifier
	 */
	@SuppressWarnings("unchecked")
	protected CommPortIdentifier getCommPortIdentifier() {
		Enumeration<CommPortIdentifier> portIdentifiers = CommPortIdentifier.getPortIdentifiers();
		CommPortIdentifier commPortId = null;
		while ( portIdentifiers.hasMoreElements() ) {
			commPortId = portIdentifiers.nextElement();
			if ( commPortId.getPortType() == CommPortIdentifier.PORT_SERIAL && 
					this.serialPort.equals(commPortId.getName()) ) {
				if ( log.isDebugEnabled() ) {
					log.debug("Found port identifier: " +this.serialPort);
				}
				break;
			} 
		}
		if ( commPortId == null ) {
			throw new RuntimeException("Couldn't find port identifier for ["
					+this.serialPort +']');
		}
		return commPortId;
	}

	/**
	 * @return the maxWait
	 */
	public long getMaxWait() {
		return maxWait;
	}
	
	/**
	 * @param maxWait the maxWait to set
	 */
	public void setMaxWait(long maxWait) {
		this.maxWait = maxWait;
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
	 * @return the serialPort
	 */
	public String getSerialPort() {
		return serialPort;
	}
	
	/**
	 * @param serialPort the serialPort to set
	 */
	public void setSerialPort(String serialPort) {
		this.serialPort = serialPort;
	}
	
}