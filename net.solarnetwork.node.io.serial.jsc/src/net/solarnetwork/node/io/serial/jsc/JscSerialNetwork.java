/* ==================================================================
 * JscSerialNetwork.java - 31/08/2020 7:46:36 AM
 *
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.serial.jsc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import net.solarnetwork.node.io.serial.ConfigurableSerialNetwork;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.io.serial.SerialConnectionAction;
import net.solarnetwork.node.io.serial.SerialNetwork;
import net.solarnetwork.node.service.LockTimeoutException;
import net.solarnetwork.node.service.support.SerialPortBean;
import net.solarnetwork.node.service.support.SerialPortBeanParameters;
import net.solarnetwork.service.support.BasicIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;

/**
 * jSerialComm implementation of {@link SerialNetwork}.
 *
 * @author matt
 * @version 1.1
 */
public class JscSerialNetwork extends BasicIdentifiable
		implements ConfigurableSerialNetwork, SettingSpecifierProvider {

	/** The {@code timeout} property default value. */
	public static final long DEFAULT_TIMEOUT_SECS = 10L;

	/** The {@code lockOnOpen} property default value. */
	public static final boolean DEFAULT_LOCK_ON_OPEN = true;

	private SerialPortBeanParameters serialParams = getDefaultSerialParametersInstance();
	private long timeout = DEFAULT_TIMEOUT_SECS;
	private TimeUnit unit = TimeUnit.SECONDS;
	private boolean lockOnOpen = DEFAULT_LOCK_ON_OPEN;

	private final ExecutorService executor = Executors
			.newSingleThreadExecutor(new CustomizableThreadFactory("JSC-SerialPort-"));
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

	/**
	 * Constructor.
	 */
	public JscSerialNetwork() {
		super();
		setUid(null);
		setDisplayName("JSC Serial Port");
	}

	/**
	 * Call to shut down internal resources. Once called, this network may not
	 * be used again.
	 */
	public void shutdown() {
		if ( executor.isShutdown() == false ) {
			executor.shutdown();
		}
	}

	@Override
	public String getPortName() {
		SerialPortBeanParameters params = getSerialParams();
		return (params != null ? params.getSerialPort() : null);
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
		return (lockOnOpen ? new LockingSerialConnection()
				: new JscSerialConnection(serialParams, executor));
	}

	/**
	 * Internal extension of {@link SerialPortConnection} that utilizes a
	 * {@link Lock} to serialize access to the connection between threads.
	 */
	private class LockingSerialConnection extends JscSerialConnection {

		/**
		 * Construct with {@link SerialParameters}.
		 *
		 * @param parameters
		 *        the parameters
		 */
		private LockingSerialConnection() {
			super(serialParams, executor);
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

	}

	/**
	 * Acquire the port lock, returning if lock acquired.
	 *
	 * @throws LockTimeoutException
	 *         if the lock cannot be obtained
	 */
	private void acquireLock() throws LockTimeoutException {
		if ( lock.isHeldByCurrentThread() ) {
			log.debug("Port {} lock already acquired", serialParams.getSerialPort());
			return;
		}
		log.debug("Acquiring lock on serial port {}; waiting at most {} {}",
				new Object[] { serialParams.getSerialPort(), timeout, unit });
		try {
			if ( lock.tryLock(timeout, unit) ) {
				log.debug("Acquired port {} lock", serialParams.getSerialPort());
				return;
			}
			log.debug("Timeout acquiring port {} lock", serialParams.getSerialPort());
		} catch ( InterruptedException e ) {
			log.debug("Interrupted waiting for port {} lock", serialParams.getSerialPort());
		}
		throw new LockTimeoutException(
				"Could not acquire port " + serialParams.getSerialPort() + " lock");
	}

	/**
	 * Release the lock previously obtained via {@link #acquireLock()}. This
	 * method is safe to call even if the lock has already been released.
	 */
	private void releaseLock() {
		if ( lock.isHeldByCurrentThread() ) {
			log.debug("Releasing lock on serial port {}", serialParams.getSerialPort());
			lock.unlock();
		}
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUid() {
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

	/**
	 * Get the default settings.
	 *
	 * @return the settings
	 */
	public static List<SettingSpecifier> getDefaultSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(20);
		results.addAll(basicIdentifiableSettings("", null, null));

		SerialPortBeanParameters defaultSerialParams = getDefaultSerialParametersInstance();
		results.add(new BasicTextFieldSettingSpecifier("serialParams.serialPort",
				defaultSerialParams.getSerialPort()));
		results.add(new BasicTextFieldSettingSpecifier("timeout", String.valueOf(DEFAULT_TIMEOUT_SECS)));
		results.add(new BasicTextFieldSettingSpecifier("serialParams.maxWait",
				String.valueOf(defaultSerialParams.getMaxWait())));
		results.addAll(SerialPortBean.getDefaultSettingSpecifiers(defaultSerialParams, "serialParams."));

		results.add(new BasicToggleSettingSpecifier("lockOnOpen", DEFAULT_LOCK_ON_OPEN));

		return results;
	}

	/**
	 * Set the serial parameters.
	 *
	 * @param serialParams
	 *        the serial parameters
	 */
	public void setSerialParams(SerialPortBeanParameters serialParams) {
		this.serialParams = serialParams;
	}

	@Override
	public SerialPortBeanParameters getSerialParams() {
		return serialParams;
	}

	/**
	 * Set the timeout value.
	 *
	 * @return the timeout
	 */
	public long getTimeout() {
		return timeout;
	}

	/**
	 * Set the timeout value.
	 *
	 * @param timeout
	 *        the timeout
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	/**
	 * Get the time unit.
	 *
	 * @return the time unit
	 */
	public TimeUnit getUnit() {
		return unit;
	}

	/**
	 * Set the time unit.
	 *
	 * @param unit
	 *        the time unit
	 */
	public void setUnit(TimeUnit unit) {
		this.unit = unit;
	}

	/**
	 * Get the lock-on-open flag.
	 *
	 * @return {@literal true} to use a thread lock when opening the connection,
	 *         releasing the lock when closing the connection; defaults to
	 *         {@link #DEFAULT_LOCK_ON_OPEN}
	 */
	public boolean isLockOnOpen() {
		return lockOnOpen;
	}

	/**
	 * Set the lock-on-open flag.
	 *
	 * @param lockOnOpen
	 *        {@literal true} to use a thread lock when opening the connection,
	 *        releasing the lock when closing the connection
	 */
	public void setLockOnOpen(boolean lockOnOpen) {
		this.lockOnOpen = lockOnOpen;
	}

}
