/* ==================================================================
 * JamodSerialModbusNetwork.java - Jul 29, 2014 12:54:53 PM
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

package net.solarnetwork.node.io.modbus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.node.LockTimeoutException;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.wimpi.modbus.net.SerialConnection;
import net.wimpi.modbus.util.SerialParameters;

/**
 * Jamod implementation of {@link ModbusNetwork}.
 * 
 * @author matt
 * @version 1.1.20191125_A
 * @since 2.0
 */
public class JamodSerialModbusNetwork implements ModbusNetwork, SettingSpecifierProvider {

	private SerialParametersBean serialParams = getDefaultSerialParametersInstance();
	private String uid = "Serial Port";
	private String groupUID;
	private long timeout = 10L;
	private TimeUnit unit = TimeUnit.SECONDS;
	private MessageSource messageSource;

	private final ReentrantLock lock = new ReentrantLock(true); // use fair lock to prevent starvation
	private final Logger log = LoggerFactory.getLogger(getClass());

	private static SerialParametersBean getDefaultSerialParametersInstance() {
		SerialParametersBean params = new SerialParametersBean();
		params.setPortName("/dev/ttyS0");
		params.setBaudRate(9600);
		params.setDatabits(8);
		params.setParityString("None");
		params.setStopbits(1);
		params.setEncoding("rtu");
		params.setEcho(false);
		params.setReceiveTimeout(1600);
		return params;
	}

	@Override
	public String toString() {
		return "JamodSerialModbusNetwork{port=" + serialParams.getPortName() + '}';
	}

	@Override
	public String getUID() {
		if ( uid != null ) {
			return uid;
		}
		return serialParams.getPortName();
	}

	@Override
	public <T> T performAction(ModbusConnectionAction<T> action, final int unitId) throws IOException {
		ModbusConnection conn = null;
		try {
			conn = createConnection(unitId);
			conn.open();
			return action.doWithConnection(conn);
		} finally {
			if ( conn != null ) {
				try {
					conn.close();
				} catch ( RuntimeException e ) {
					// ignore this
				}
			}
		}
	}

	@Override
	public ModbusConnection createConnection(int unitId) {
		return new JamodModbusConnection(new LockingSerialConnection(serialParams), unitId);
	}

	/**
	 * Internal extension of {@link SerialConnection} that utilizes a
	 * {@link Lock} to serialize access to the connection between threads.
	 */
	private class LockingSerialConnection extends SerialConnection {

		/**
		 * Construct with {@link SerialParameters}.
		 * 
		 * @param parameters
		 *        the parameters
		 */
		private LockingSerialConnection(SerialParameters parameters) {
			super(parameters);
		}

		@Override
		public void open() throws Exception {
			if ( !isOpen() ) {
				acquireLock();
				super.open();
			}
		}

		@Override
		public void close() {
			try {
				if ( isOpen() ) {
					super.close();
				}
			} finally {
				releaseLock();
			}
		}

		@Override
		protected void finalize() throws Throwable {
			releaseLock(); // as a catch-all
			super.finalize();
		}
	}

	/**
	 * Acquire the port lock, returning if lock acquired.
	 * 
	 * @throws LockTimeoutException
	 *         if the lock cannot be obtained
	 */
	private void acquireLock() throws LockTimeoutException {
		if ( lock.isHeldByCurrentThread() ) {
			log.debug("Port {} lock already acquired", serialParams.getPortName());
			return;
		}
		log.debug("Acquiring lock on Modbus port {}; waiting at most {} {}",
				new Object[] { serialParams.getPortName(), timeout, unit });
		try {
			if ( lock.tryLock(timeout, unit) ) {
				log.debug("Acquired port {} lock", serialParams.getPortName());
				return;
			}
			log.debug("Timeout acquiring port {} lock", serialParams.getPortName());
		} catch ( InterruptedException e ) {
			log.debug("Interrupted waiting for port {} lock", serialParams.getPortName());
		}
		throw new LockTimeoutException("Could not acquire port " + serialParams.getPortName() + " lock");
	}

	/**
	 * Release the lock previously obtained via {@link #acquireLock()}. This
	 * method is safe to call even if the lock has already been released.
	 */
	private void releaseLock() {
		if ( lock.isHeldByCurrentThread() ) {
			log.debug("Releasing lock on Modbus port {}", serialParams.getPortName());
			lock.unlock();
		}
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.io.modbus";
	}

	@Override
	public String getDisplayName() {
		return "Modbus port";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return getDefaultSettingSpecifiers();
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	public static List<SettingSpecifier> getDefaultSettingSpecifiers() {
		JamodSerialModbusNetwork defaults = new JamodSerialModbusNetwork();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);
		results.add(new BasicTextFieldSettingSpecifier("uid", String.valueOf(defaults.uid)));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.portName",
				defaults.serialParams.getPortName()));
		results.add(new BasicTextFieldSettingSpecifier("timeout", String.valueOf(defaults.timeout)));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.baudRate",
				String.valueOf(defaults.serialParams.getBaudRate())));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.databits",
				String.valueOf(defaults.serialParams.getDatabits())));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.stopbits",
				String.valueOf(defaults.serialParams.getStopbits())));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.parityString",
				defaults.serialParams.getParityString()));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.encoding",
				defaults.serialParams.getEncoding()));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.receiveTimeout",
				String.valueOf(defaults.serialParams.getReceiveTimeout())));

		results.add(new BasicTextFieldSettingSpecifier("serialParams.echo",
				String.valueOf(defaults.serialParams.isEcho())));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.flowControlInString",
				defaults.serialParams.getFlowControlInString()));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.flowControlOutString",
				defaults.serialParams.getFlowControlInString()));

		return results;
	}

	// Accessors

	public SerialParametersBean getSerialParams() {
		return serialParams;
	}

	public void setSerialParams(SerialParametersBean serialParams) {
		this.serialParams = serialParams;
	}

	@Override
	public String getGroupUID() {
		return groupUID;
	}

	public void setGroupUID(String groupUID) {
		this.groupUID = groupUID;
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

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

}
