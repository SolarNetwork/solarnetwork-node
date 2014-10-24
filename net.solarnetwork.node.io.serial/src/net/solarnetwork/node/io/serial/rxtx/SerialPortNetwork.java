/* ==================================================================
 * SerialPortNetwork.java - Oct 23, 2014 3:58:36 PM
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

package net.solarnetwork.node.io.serial.rxtx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.solarnetwork.node.LockTimeoutException;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.io.serial.SerialConnectionAction;
import net.solarnetwork.node.io.serial.SerialNetwork;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.support.SerialPortBean;
import net.solarnetwork.node.support.SerialPortBeanParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;

/**
 * RXTX implementation of {@link SerialNetwork}.
 * 
 * @author matt
 * @version 1.0
 */
public class SerialPortNetwork implements SerialNetwork, SettingSpecifierProvider {

	private SerialPortBeanParameters serialParams = getDefaultSerialParametersInstance();
	private String uid = "Serial Port";
	private String groupUID;
	private long timeout = 10L;
	private TimeUnit unit = TimeUnit.SECONDS;
	private MessageSource messageSource;

	private final ReentrantLock lock = new ReentrantLock(true); // use fair lock to prevent starvation
	private final Logger log = LoggerFactory.getLogger(getClass());

	private static SerialPortBeanParameters getDefaultSerialParametersInstance() {
		SerialPortBeanParameters params = new SerialPortBeanParameters();
		params.setSerialPort("/dev/ttyS0");
		params.setBaud(9600);
		params.setDataBits(8);
		params.setReceiveThreshold(4);
		params.setReceiveTimeout(9000);
		params.setMaxWait(90000);
		return params;
	}

	@Override
	public String getUID() {
		return uid;
	}

	@Override
	public String getGroupUID() {
		return groupUID;
	}

	@Override
	public <T> T performAction(SerialConnectionAction<T> action) throws IOException {
		SerialConnection conn = null;
		try {
			conn = createConnection();
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
	public SerialConnection createConnection() {
		return new LockingSerialConnection();
	}

	/**
	 * Internal extension of {@link SerialPortConnection} that utilizes a
	 * {@link Lock} to serialize access to the connection between threads.
	 */
	private class LockingSerialConnection extends SerialPortConnection {

		/**
		 * Construct with {@link SerialParameters}.
		 * 
		 * @param parameters
		 *        the parameters
		 */
		private LockingSerialConnection() {
			super(serialParams, unit.toMillis(timeout));
		}

		@Override
		public void open() throws IOException, LockTimeoutException {
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
		if ( lock.isLocked() ) {
			log.debug("Port {} lock already acquired", serialParams.getSerialPort());
			return;
		}
		log.debug("Acquiring lock on serial port {}; waiting at most {} {}",
				serialParams.getSerialPort(), timeout, unit);
		try {
			if ( lock.tryLock(timeout, unit) ) {
				log.debug("Acquired port {} lock", serialParams.getSerialPort());
				return;
			}
			log.debug("Timeout acquiring port {} lock", serialParams.getSerialPort());
		} catch ( InterruptedException e ) {
			log.debug("Interrupted waiting for port {} lock", serialParams.getSerialPort());
		}
		throw new LockTimeoutException("Could not acquire port " + serialParams.getSerialPort()
				+ " lock");
	}

	/**
	 * Release the lock previously obtained via {@link #acquireLock()}. This
	 * method is safe to call even if the lock has already been released.
	 */
	private void releaseLock() {
		if ( lock.isLocked() ) {
			log.debug("Releasing lock on serial port {}", serialParams.getSerialPort());
			lock.unlock();
		}
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.io.serial";
	}

	@Override
	public String getDisplayName() {
		return "Serial port";
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
		SerialPortNetwork defaults = new SerialPortNetwork();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);
		results.add(new BasicTextFieldSettingSpecifier("uid", defaults.uid));
		results.add(new BasicTextFieldSettingSpecifier("groupUID", defaults.groupUID));

		SerialPortBeanParameters defaultSerialParams = getDefaultSerialParametersInstance();
		results.add(new BasicTextFieldSettingSpecifier("serialParams.serialPort", defaultSerialParams
				.getSerialPort()));
		results.add(new BasicTextFieldSettingSpecifier("timeout", String.valueOf(defaults.timeout)));
		results.addAll(SerialPortBean.getDefaultSettingSpecifiers(defaultSerialParams, "serialParams."));

		return results;
	}

	public void setSerialParams(SerialPortBeanParameters serialParams) {
		this.serialParams = serialParams;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public void setGroupUID(String groupUID) {
		this.groupUID = groupUID;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public void setUnit(TimeUnit unit) {
		this.unit = unit;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

}
