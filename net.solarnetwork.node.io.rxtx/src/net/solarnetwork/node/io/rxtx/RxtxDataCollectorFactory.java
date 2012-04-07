/* ==================================================================
 * RxtxDataCollectorFactory.java - Mar 24, 2012 9:04:39 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.io.rxtx;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import net.solarnetwork.node.ConversationalDataCollector;
import net.solarnetwork.node.DataCollector;
import net.solarnetwork.node.DataCollectorFactory;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.support.DataCollectorSerialPortBeanParameters;
import net.solarnetwork.node.support.SerialPortBeanParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Rxtx implementation of {@link DataCollectorFactory}, exposing serial ports
 * for communication.
 * 
 * <p>
 * This factory is designed to be deployed as a Configuration Admin backed
 * managed service factory, with one instance per serial port (identified by
 * {@link #getPortIdentifier()}).
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>portIdentifier</dt>
 * <dd>The port identifier to use for all serial communication.</dd>
 * </dl>
 * 
 * @author matt
 * @version $Revision$
 */
public class RxtxDataCollectorFactory implements DataCollectorFactory<SerialPortBeanParameters>,
		SettingSpecifierProvider {

	private static final Object MONITOR = new Object();
	private static MessageSource MESSAGE_SOURCE;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private String portIdentifier;

	@Override
	public String getUID() {
		return portIdentifier;
	}

	@Override
	public DataCollector getDataCollectorInstance(SerialPortBeanParameters params) {
		CommPortIdentifier portId = getCommPortIdentifier();
		try {
			// establish the serial port connection
			SerialPort port = (SerialPort) portId.open(params.getCommPortAppName(), 2000);
			SerialPortDataCollector obj;
			if ( params instanceof DataCollectorSerialPortBeanParameters ) {
				DataCollectorSerialPortBeanParameters dcParams = (DataCollectorSerialPortBeanParameters) params;
				obj = new SerialPortDataCollector(port, dcParams.getBufferSize(),
						dcParams.getMagic(), dcParams.getReadSize(), dcParams.getMaxWait());
			} else {
				obj = new SerialPortDataCollector(port);
			}
			setupSerialPortSupport(obj, params);
			if ( params instanceof DataCollectorSerialPortBeanParameters ) {
				DataCollectorSerialPortBeanParameters dcParams = (DataCollectorSerialPortBeanParameters) params;
				obj.setToggleDtr(dcParams.isToggleDtr());
				obj.setToggleRts(dcParams.isToggleRts());
			}
			return obj;
		} catch (PortInUseException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ConversationalDataCollector getConversationalDataCollectorInstance(
			SerialPortBeanParameters params) {
		CommPortIdentifier portId = getCommPortIdentifier();
		try {
			// establish the serial port connection
			SerialPort port = (SerialPort) portId.open(params.getCommPortAppName(), 2000);
			SerialPortConversationalDataCollector obj = new SerialPortConversationalDataCollector(
					port, params.getMaxWait());
			setupSerialPortSupport(obj, params);
			return obj;
		} catch (PortInUseException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.io.rxtx";
	}

	@Override
	public String getDisplayName() {
		return "Serial Port";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return getDefaultSettingSpecifiers();
	}

	@Override
	public MessageSource getMessageSource() {
		synchronized (MONITOR) {
			if ( MESSAGE_SOURCE == null ) {
				ResourceBundleMessageSource source = new ResourceBundleMessageSource();
				source.setBundleClassLoader(getClass().getClassLoader());
				source.setBasename(getClass().getName());
				MESSAGE_SOURCE = source;
			}
		}
		return MESSAGE_SOURCE;
	}

	public static List<SettingSpecifier> getDefaultSettingSpecifiers() {
		return Arrays.asList((SettingSpecifier) new BasicTextFieldSettingSpecifier(
				"portIdentifier", "/dev/ttyUSB0"));
	}

	/**
	 * Configure base properties on a {@link SerialPortSupport} instance.
	 * 
	 * @param obj
	 *            the object to configure
	 * @param params
	 *            the parameters to copy
	 */
	private void setupSerialPortSupport(SerialPortSupport obj, SerialPortBeanParameters params) {
		obj.setBaud(params.getBaud());
		obj.setDataBits(params.getDataBits());
		obj.setStopBits(params.getStopBits());
		obj.setParity(params.getParity());
		obj.setFlowControl(params.getFlowControl());
		obj.setReceiveFraming(params.getReceiveFraming());
		obj.setReceiveThreshold(params.getReceiveThreshold());
		obj.setReceiveTimeout(params.getReceiveTimeout());
		obj.setDtrFlag(params.getDtrFlag());
		obj.setRtsFlag(params.getRtsFlag());
	}

	/**
	 * Locate the {@link CommPortIdentifier} for the configured
	 * {@link #getSerialPort()} value.
	 * 
	 * <p>
	 * This method will throw a RuntimeException if the port is not found.
	 * </p>
	 * 
	 * @return the CommPortIdentifier
	 */
	@SuppressWarnings("unchecked")
	private CommPortIdentifier getCommPortIdentifier() {
		Enumeration<CommPortIdentifier> portIdentifiers = CommPortIdentifier.getPortIdentifiers();
		CommPortIdentifier commPortId = null;
		List<String> foundNames = new ArrayList<String>(5);
		while ( portIdentifiers.hasMoreElements() ) {
			commPortId = portIdentifiers.nextElement();
			log.trace("Inspecting available port identifier: {}", commPortId);
			foundNames.add(commPortId.getName());
			if ( commPortId.getPortType() == CommPortIdentifier.PORT_SERIAL
					&& this.portIdentifier.equals(commPortId.getName()) ) {
				if ( log.isDebugEnabled() ) {
					log.debug("Found port identifier: {}", this.portIdentifier);
				}
				break;
			}
		}
		if ( commPortId == null ) {
			throw new RuntimeException("Couldn't find port identifier for [" + this.portIdentifier
					+ "]; available ports: " + foundNames);
		}
		return commPortId;
	}

	public String getPortIdentifier() {
		return portIdentifier;
	}

	public void setPortIdentifier(String portIdentifier) {
		this.portIdentifier = portIdentifier;
	}

}
