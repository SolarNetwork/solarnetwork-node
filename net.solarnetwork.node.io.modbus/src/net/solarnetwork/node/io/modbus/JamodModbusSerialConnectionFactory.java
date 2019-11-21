/* ==================================================================
 * JamodModbusSerialConnectionFactory.java - Jul 10, 2013 7:35:26 AM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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
import org.springframework.context.support.ResourceBundleMessageSource;
import net.solarnetwork.node.LockTimeoutException;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.wimpi.modbus.net.SerialConnection;
import net.wimpi.modbus.util.SerialParameters;

/**
 * Default implementation of {@link ModbusSerialConnectionFactory}.
 * 
 * @author matt
 * @version 1.2
 */
public class JamodModbusSerialConnectionFactory
		implements ModbusSerialConnectionFactory, SettingSpecifierProvider {

	private static MessageSource MESSAGE_SOURCE;
	private final ReentrantLock lock = new ReentrantLock(true); // use fair lock to prevent starvation

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

	private final Logger log = LoggerFactory.getLogger(getClass());

	private SerialParametersBean serialParams = getDefaultSerialParametersInstance();
	private long timeout = 10L;
	private TimeUnit unit = TimeUnit.SECONDS;

	@Override
	public String getUID() {
		return serialParams.getPortName();
	}

	@Override
	public SerialConnection getSerialConnection() {
		LockingSerialConnection conn = new LockingSerialConnection(serialParams);
		try {
			openConnection(conn, 2);
		} catch ( RuntimeException e ) {
			if ( conn != null ) {
				try {
					conn.close(); // to release the lock
				} catch ( Exception e2 ) {
					// ignore this
				}
			}
			throw e;
		}
		return conn;
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

	private void openConnection(SerialConnection conn, int tries) {
		if ( conn.isOpen() ) {
			return;
		}
		if ( log.isDebugEnabled() ) {
			log.debug("Opening serial connection to [" + serialParams.getPortName() + "], " + tries
					+ " tries remaining");
		}
		Exception exception = null;
		do {
			try {
				conn.open();
				return;
			} catch ( Exception e ) {
				exception = e;
				tries--;
				try {
					conn.close();
				} catch ( Exception e2 ) {
					// ignore this one
				}
			}
		} while ( tries > 0 );
		throw new RuntimeException(
				"Unable to open serial connection to [" + serialParams.getPortName() + "]", exception);
	}

	@Override
	public <T> T execute(ModbusConnectionCallback<T> action) {
		T result = null;
		SerialConnection conn = getSerialConnection();
		if ( conn != null ) {
			try {
				result = action.doInConnection(conn);
			} catch ( IOException e ) {
				throw new RuntimeException(e);
			} finally {
				conn.close();
			}
		}
		return result;
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
		if ( MESSAGE_SOURCE == null ) {
			ResourceBundleMessageSource source = new ResourceBundleMessageSource();
			source.setBundleClassLoader(getClass().getClassLoader());
			source.setBasename(getClass().getName());
			MESSAGE_SOURCE = source;
		}
		return MESSAGE_SOURCE;
	}

	public static List<SettingSpecifier> getDefaultSettingSpecifiers() {
		JamodModbusSerialConnectionFactory defaults = new JamodModbusSerialConnectionFactory();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);
		results.add(new BasicTextFieldSettingSpecifier("timeout", String.valueOf(defaults.timeout)));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.portName",
				defaults.serialParams.getPortName()));
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

	public SerialParametersBean getSerialParams() {
		return serialParams;
	}

	public void setSerialParams(SerialParametersBean serialParams) {
		this.serialParams = serialParams;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public void setUnit(TimeUnit unit) {
		this.unit = unit;
	}

}
