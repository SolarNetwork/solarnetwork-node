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
 */

package net.solarnetwork.node.io.rxtx;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.solarnetwork.node.ConversationalDataCollector;
import net.solarnetwork.node.DataCollector;
import net.solarnetwork.node.DataCollectorFactory;
import net.solarnetwork.node.LockTimeoutException;
import net.solarnetwork.node.PortLockedConversationalDataCollector;
import net.solarnetwork.node.PortLockedDataCollector;
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
 * <dt>groupUID</dt>
 * <dd>The service group to use. Defaults to <em>null</em>.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.1
 */
public class RxtxDataCollectorFactory implements DataCollectorFactory<SerialPortBeanParameters>,
		SettingSpecifierProvider {

	/** The default value for the {@code portIdentifier} property. */
	public static final String DEFAULT_PORT_IDENTIFIER = "/dev/ttyUSB0";

	private static final Map<String, Lock> PORT_LOCKS = new HashMap<String, Lock>(3);
	private static final Object MONITOR = new Object();
	private static MessageSource MESSAGE_SOURCE;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private String portIdentifier = DEFAULT_PORT_IDENTIFIER;
	private long timeout = 50L;
	private TimeUnit unit = TimeUnit.SECONDS;
	private String groupUID = null;

	@Override
	public String getUID() {
		return portIdentifier;
	}

	@Override
	public String getGroupUID() {
		return groupUID;
	}

	private Lock acquireLock() throws LockTimeoutException {
		log.debug("Acquiring lock on port {}; waiting at most {} {}", new Object[] { portIdentifier,
				timeout, unit });
		synchronized ( PORT_LOCKS ) {
			if ( !PORT_LOCKS.containsKey(portIdentifier) ) {
				PORT_LOCKS.put(portIdentifier, new ReentrantLock(true));
			}
			Lock lock = PORT_LOCKS.get(portIdentifier);
			try {
				if ( lock.tryLock(timeout, unit) ) {
					log.debug("Acquired port {} lock", portIdentifier);
					return lock;
				}
				log.debug("Timeout acquiring port {} lock", portIdentifier);
			} catch ( InterruptedException e ) {
				log.debug("Interrupted waiting for port {} lock", portIdentifier);
			}
		}
		throw new LockTimeoutException("Could not acquire port " + portIdentifier + " lock");
	}

	@Override
	public DataCollector getDataCollectorInstance(SerialPortBeanParameters params) {
		Lock lock = acquireLock();
		try {
			CommPortIdentifier portId = getCommPortIdentifier();
			// establish the serial port connection
			SerialPort port = (SerialPort) portId.open(params.getCommPortAppName(), 2000);
			AbstractSerialPortDataCollector obj;
			if ( params instanceof DataCollectorSerialPortBeanParameters ) {
				DataCollectorSerialPortBeanParameters dcParams = (DataCollectorSerialPortBeanParameters) params;
				if ( dcParams.getMagicEOF() != null ) {
					obj = new SerialPortVariableDataCollector(port, dcParams.getBufferSize(),
							dcParams.getMagic(), dcParams.getMagicEOF(), dcParams.getMaxWait());
				} else {
					obj = new SerialPortDataCollector(port, dcParams.getBufferSize(),
							dcParams.getMagic(), dcParams.getReadSize(), dcParams.getMaxWait());
				}
			} else {
				obj = new SerialPortDataCollector(port);
			}
			setupSerialPortSupport(obj, params);
			if ( params instanceof DataCollectorSerialPortBeanParameters ) {
				DataCollectorSerialPortBeanParameters dcParams = (DataCollectorSerialPortBeanParameters) params;
				obj.setToggleDtr(dcParams.isToggleDtr());
				obj.setToggleRts(dcParams.isToggleRts());
			}
			return new PortLockedDataCollector(obj, portId.getName(), lock);
		} catch ( PortInUseException e ) {
			lock.unlock();
			throw new RuntimeException(e);
		} catch ( RuntimeException e ) {
			lock.unlock();
			throw e;
		}
	}

	@Override
	public ConversationalDataCollector getConversationalDataCollectorInstance(
			SerialPortBeanParameters params) {
		Lock lock = acquireLock();
		try {
			CommPortIdentifier portId = getCommPortIdentifier();
			// establish the serial port connection
			SerialPort port = (SerialPort) portId.open(params.getCommPortAppName(), 2000);
			SerialPortConversationalDataCollector obj = new SerialPortConversationalDataCollector(port,
					params.getMaxWait());
			setupSerialPortSupport(obj, params);
			return new PortLockedConversationalDataCollector(obj, portId.getName(), lock);
		} catch ( PortInUseException e ) {
			lock.unlock();
			throw new RuntimeException(e);
		} catch ( IllegalArgumentException e ) {
			lock.unlock();
			throw new RuntimeException(e);
		} catch ( RuntimeException e ) {
			lock.unlock();
			throw e;
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
		synchronized ( MONITOR ) {
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
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(2);
		RxtxDataCollectorFactory defaults = new RxtxDataCollectorFactory();
		results.add(new BasicTextFieldSettingSpecifier("portIdentifier", defaults.portIdentifier));
		results.add(new BasicTextFieldSettingSpecifier("groupUID", defaults.groupUID));
		results.add(new BasicTextFieldSettingSpecifier("timeout", String.valueOf(defaults.timeout)));
		return results;
	}

	/**
	 * Configure base properties on a {@link SerialPortSupport} instance.
	 * 
	 * @param obj
	 *        the object to configure
	 * @param params
	 *        the parameters to copy
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
		// first try directly
		CommPortIdentifier commPortId = null;
		try {
			commPortId = CommPortIdentifier.getPortIdentifier(this.portIdentifier);
			if ( commPortId != null ) {
				log.debug("Found port identifier: {}", this.portIdentifier);
				return commPortId;
			}
		} catch ( NoSuchPortException e ) {
			log.debug("Port {} not found, inspecting available ports...", this.portIdentifier);
		}
		Enumeration<CommPortIdentifier> portIdentifiers = CommPortIdentifier.getPortIdentifiers();
		List<String> foundNames = new ArrayList<String>(5);
		while ( portIdentifiers.hasMoreElements() ) {
			commPortId = portIdentifiers.nextElement();
			log.trace("Inspecting available port identifier: {}", commPortId.getName());
			foundNames.add(commPortId.getName());
			if ( commPortId.getPortType() == CommPortIdentifier.PORT_SERIAL
					&& this.portIdentifier.equals(commPortId.getName()) ) {
				log.debug("Found port identifier: {}", this.portIdentifier);
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

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public TimeUnit getUnit() {
		return unit;
	}

	public void setUnit(TimeUnit unit) {
		this.unit = unit;
	}

	public void setGroupUID(String groupUID) {
		this.groupUID = groupUID;
	}

}
