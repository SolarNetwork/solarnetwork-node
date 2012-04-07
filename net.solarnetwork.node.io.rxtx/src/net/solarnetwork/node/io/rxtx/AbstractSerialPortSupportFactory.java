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

package net.solarnetwork.node.io.rxtx;

import gnu.io.CommPortIdentifier;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import net.solarnetwork.node.support.SerialPortBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for factories for {@link SerialPortSupport} implementations.
 *
 * @author matt
 * @version $Revision$ $Date$
 */
public abstract class AbstractSerialPortSupportFactory extends SerialPortBean {
	
	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private String serialPort = "/dev/ttyUSB0";
	private long maxWait;
	
	/**
	 * Configure base properties on a {@link SerialPortSupport} instance.
	 * 
	 * @param obj the object to configure
	 */
	protected void setupSerialPortSupport(SerialPortSupport obj) {
		obj.setBaud(getBaud());
		obj.setDataBits(getDataBits());
		obj.setStopBits(getStopBits());
		obj.setParity(getParity());
		obj.setFlowControl(getFlowControl());
		obj.setReceiveFraming(getReceiveFraming());
		obj.setReceiveThreshold(getReceiveThreshold());
		obj.setReceiveTimeout(getReceiveTimeout());
		obj.setDtrFlag(getDtrFlag());
		obj.setRtsFlag(getRtsFlag());
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
		List<String> foundNames = new ArrayList<String>(5);
		while ( portIdentifiers.hasMoreElements() ) {
			commPortId = portIdentifiers.nextElement();
			log.trace("Inspecting available port identifier: {}", commPortId);
			foundNames.add(commPortId.getName());
			if ( commPortId.getPortType() == CommPortIdentifier.PORT_SERIAL && 
					this.serialPort.equals(commPortId.getName()) ) {
				if ( log.isDebugEnabled() ) {
					log.debug("Found port identifier: {}", this.serialPort);
				}
				break;
			} 
		}
		if ( commPortId == null ) {
			throw new RuntimeException("Couldn't find port identifier for ["
					+this.serialPort +"]; available ports: " +foundNames);
		}
		return commPortId;
	}

	public long getMaxWait() {
		return maxWait;
	}
	public void setMaxWait(long maxWait) {
		this.maxWait = maxWait;
	}
	public String getSerialPort() {
		return serialPort;
	}
	public void setSerialPort(String serialPort) {
		this.serialPort = serialPort;
	}
	
}