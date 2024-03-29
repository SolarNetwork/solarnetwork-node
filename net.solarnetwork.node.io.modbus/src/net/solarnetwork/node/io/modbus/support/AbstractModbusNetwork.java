/* ==================================================================
 * AbstractModbusNetwork.java - 3/02/2018 8:01:09 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus.support;

import static java.lang.String.format;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.service.LockTimeoutException;
import net.solarnetwork.service.support.BasicIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;

/**
 * Abstract implementation of {@link ModbusNetwork}.
 * 
 * @author matt
 * @version 3.1
 * @since 2.4
 */
public abstract class AbstractModbusNetwork extends BasicIdentifiable implements ModbusNetwork {

	/** A default value for the {@code retryDelay} property. */
	public static final long DEFAULT_RETRY_DELAY_MILLIS = 60;

	/**
	 * A default value for the {@code uid} property.
	 * 
	 * @since 3.0
	 */
	public static final String DEFAULT_UID = "Modbus Port";

	/**
	 * A default value for the {@code timeout} property.
	 * 
	 * @since 3.0
	 */
	public static final long DEFAULT_TIMEOUT_SECS = 10L;

	/**
	 * A default value for the {@code retries} property.
	 * 
	 * @since 3.0
	 */
	public static int DEFAULT_RETRIES = 3;

	/**
	 * A default value for the {@code retries} property.
	 * 
	 * @since 3.0
	 */
	public static boolean DEFAULT_HEADLESS = true;

	/**
	 * A default value for the {@code retryReconnect} property.
	 * 
	 * @since 3.0
	 */
	public static boolean DEFAULT_RETRY_RECONNECT = false;

	private long timeout = DEFAULT_TIMEOUT_SECS;
	private TimeUnit timeoutUnit = TimeUnit.SECONDS;
	private boolean headless = DEFAULT_HEADLESS;
	private int retries = DEFAULT_RETRIES;
	private long retryDelay = DEFAULT_RETRY_DELAY_MILLIS;
	private TimeUnit retryDelayUnit = TimeUnit.MILLISECONDS;
	private boolean retryReconnect = DEFAULT_RETRY_RECONNECT;

	private Set<String> classNamesToTreatAsIoException = defaultClassNamesToTreatAsIoException();

	private static final Set<String> defaultClassNamesToTreatAsIoException() {
		return Collections.singleton("net.wimpi.modbus.ModbusIOException");
	}

	private final ReentrantLock lock = new ReentrantLock(true); // use fair lock to prevent starvation

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 */
	public AbstractModbusNetwork() {
		super();
		setUid(DEFAULT_UID);
	}

	/**
	 * Get the set of class names to convert to {@link IOException} instances if
	 * caught at runtime.
	 * 
	 * @return the set of class names
	 * @since 2.0
	 */
	protected final Set<String> getClassNamesToTreatAsIoException() {
		return classNamesToTreatAsIoException;
	}

	/**
	 * Add class names to the set of names to convert to {@link IOException} if
	 * caught at runtime.
	 * 
	 * @param classNames
	 *        the names to convert when caught
	 * @return the final set of configured names
	 * @since 2.0
	 */
	protected final Set<String> addClassNamesToTreatAsIoException(Iterable<String> classNames) {
		if ( classNames == null ) {
			return classNamesToTreatAsIoException;
		}
		Set<String> s = new LinkedHashSet<>(classNamesToTreatAsIoException);
		for ( String name : classNames ) {
			s.add(name);
		}
		s = Collections.unmodifiableSet(s);
		classNamesToTreatAsIoException = s;
		return s;
	}

	/**
	 * Remove class names from the set of names to convert to
	 * {@link IOException} if caught at runtime.
	 * 
	 * @param classNames
	 *        the names to no longer convert when caught
	 * @return the final set of configured names
	 * @since 2.0
	 */
	protected final Set<String> removeClassNamesToTreatAsIoException(Iterable<String> classNames) {
		if ( classNames == null ) {
			return classNamesToTreatAsIoException;
		}
		Set<String> s = new LinkedHashSet<>(classNamesToTreatAsIoException);
		for ( String name : classNames ) {
			s.remove(name);
		}
		s = Collections.unmodifiableSet(s);
		classNamesToTreatAsIoException = s;
		return s;
	}

	private final boolean shouldConvertToIoException(Throwable t) {
		Class<?>[] classes = t.getClass().getClasses();
		for ( Class<?> c : classes ) {
			if ( classNamesToTreatAsIoException.contains(c.getName()) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public <T> T performAction(int unitId, ModbusConnectionAction<T> action) throws IOException {
		ModbusConnection conn = null;
		try {
			conn = createConnection(unitId);
			if ( conn == null ) {
				return null;
			}
			conn.open();
			return action.doWithConnection(conn);
		} catch ( RuntimeException e ) {
			// unwrap ModbusIOException into IOException to cut down chatter
			Throwable t = e;
			while ( t.getCause() != null ) {
				t = t.getCause();
			}

			log.warn("{} performing action {} on device {}", t.getClass().getSimpleName(), action,
					unitId);

			if ( shouldConvertToIoException(t) ) {
				throw new IOException(t.getMessage(), t);
			}
			if ( t instanceof RuntimeException ) {
				throw (RuntimeException) t;
			}
			throw e;
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

	/**
	 * Acquire a network-wide lock, returning if lock acquired.
	 * 
	 * @throws LockTimeoutException
	 *         if the lock cannot be obtained
	 */
	protected void acquireLock() throws LockTimeoutException {
		final String desc = getNetworkDescription();
		if ( lock.isHeldByCurrentThread() ) {
			log.debug("Port {} lock already acquired", desc);
			return;
		}
		final long timeout = getTimeout();
		log.debug("Acquiring lock on Modbus port {}; waiting at most {} {}",
				new Object[] { desc, timeout, getTimeoutUnit() });
		try {
			final long ts = System.currentTimeMillis();
			if ( lock.tryLock(timeout, getTimeoutUnit()) ) {
				if ( log.isDebugEnabled() ) {
					long t = System.currentTimeMillis() - ts;
					log.debug("Acquired port {} lock in {}ms", desc, t);
				}
				return;
			}
			if ( log.isDebugEnabled() ) {
				long t = System.currentTimeMillis() - ts;
				log.debug("Timeout acquiring port {} lock after {}ms", desc, t);
			}
		} catch ( InterruptedException e ) {
			log.debug("Interrupted waiting for port {} lock", desc);
		}
		throw new LockTimeoutException(format("Could not acquire port %s lock within %d %s", desc,
				timeout, getTimeoutUnit().toString().toLowerCase()));
	}

	/**
	 * Release the network-wide lock previously obtained via
	 * {@link #acquireLock()}.
	 * 
	 * <p>
	 * This method is safe to call even if the lock has already been released.
	 * </p>
	 */
	protected void releaseLock() {
		if ( lock.isHeldByCurrentThread() ) {
			final String desc = getNetworkDescription();
			log.debug("Releasing lock on {}", desc);
			lock.unlock();
		}
	}

	/**
	 * Create a new connection that wraps the network-wide lock around another
	 * connection.
	 * 
	 * <p>
	 * The {@link ModbusConnection#open()} call will acquire the lock, and
	 * {@link ModbusConnection#close()} will release it.
	 * </p>
	 * 
	 * @param connection
	 *        the connection to warp
	 * @return a new connection that uses the network-wide lock
	 * @since 2.1
	 */
	protected ModbusConnection createLockingConnection(ModbusConnection connection) {
		return new LockingModbusConnection(connection, lock, timeout, timeoutUnit,
				getNetworkDescription(), log);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "{" + getNetworkDescription() + '}';
	}

	/**
	 * Get a description of this network.
	 * 
	 * <p>
	 * This implementation simply calls {@code toString()} on this object.
	 * Extending classes may want to provide something more meaningful.
	 * </p>
	 * 
	 * @return a description of this network
	 */
	protected String getNetworkDescription() {
		return this.toString();
	}

	/**
	 * Get a list of base settings.
	 * 
	 * @return the base settings
	 */
	protected List<SettingSpecifier> getBaseSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(5);
		results.add(new BasicToggleSettingSpecifier("headless", DEFAULT_HEADLESS));
		results.add(new BasicTextFieldSettingSpecifier("timeout", String.valueOf(DEFAULT_TIMEOUT_SECS)));
		results.add(new BasicTextFieldSettingSpecifier("retries", String.valueOf(DEFAULT_RETRIES)));
		results.add(new BasicTextFieldSettingSpecifier("retryDelay",
				String.valueOf(DEFAULT_RETRY_DELAY_MILLIS)));
		results.add(new BasicToggleSettingSpecifier("retryReconnect", DEFAULT_RETRY_RECONNECT));
		return results;
	}

	/**
	 * Get the timeout value.
	 * 
	 * @return the timeout value, defaults to {@literal 10}
	 */
	public long getTimeout() {
		return timeout;
	}

	/**
	 * Set a timeout value.
	 * 
	 * @param timeout
	 *        the timeout
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	/**
	 * Get the timeout unit.
	 * 
	 * @return the timeout unit; defaults to seconds
	 */
	public TimeUnit getTimeoutUnit() {
		return timeoutUnit;
	}

	/**
	 * Set the timeout unit.
	 * 
	 * @param unit
	 *        the unit
	 */
	public void setTimeoutUnit(TimeUnit unit) {
		this.timeoutUnit = unit;
	}

	/**
	 * Get the "headless" operation flag.
	 * 
	 * @return the headless mode; defaults to {@literal true}
	 * @see #setHeadless(boolean)
	 */
	public boolean isHeadless() {
		return headless;
	}

	/**
	 * Set the "headless" operation flag.
	 * 
	 * <p>
	 * When {@literal true}, a 6-byte Modbus header with a transaction ID, etc.
	 * is left off requests. This is most often used for Modbus RTU over serial
	 * connections. When {@literal false} the header is included. This is most
	 * often used with Modbus TCP.
	 * </p>
	 * 
	 * @param headless
	 *        {@literal true} for headless operation, {@literal false} otherwise
	 */
	public void setHeadless(boolean headless) {
		this.headless = headless;
	}

	/**
	 * Get the number of "retries" to perform on each transaction in the event
	 * of errors.
	 * 
	 * @return the number of retries; defaults to {@literal 3}
	 */
	public int getRetries() {
		return retries;
	}

	/**
	 * Set the number of "retries" to perform on each transaction in the event
	 * of errors.
	 * 
	 * @param retries
	 *        the number of retries
	 */
	public void setRetries(int retries) {
		this.retries = retries;
	}

	/**
	 * Get the retry delay.
	 * 
	 * @return the retry delay
	 */
	public long getRetryDelay() {
		return retryDelay;
	}

	/**
	 * Set a retry delay between error retries.
	 * 
	 * @param retryDelay
	 *        the delay, or {@literal 0} for no delay
	 */
	public void setRetryDelay(long retryDelay) {
		this.retryDelay = retryDelay;
	}

	/**
	 * Get the retry delay time unit.
	 * 
	 * @return the time unit; defaults to {@link TimeUnit#MILLISECONDS}
	 */
	public TimeUnit getRetryDelayUnit() {
		return retryDelayUnit;
	}

	/**
	 * Set the retry delay time unit.
	 * 
	 * @param retryDelayUnit
	 *        the unit to set
	 */
	public void setRetryDelayUnit(TimeUnit retryDelayUnit) {
		this.retryDelayUnit = retryDelayUnit;
	}

	/**
	 * Get the retry reconnect mode.
	 * 
	 * @return {@literal} true to reconnect between error retries,
	 *         {@literal false} to continue using the same connection; defaults
	 *         to {@literal false}
	 */
	public boolean isRetryReconnect() {
		return retryReconnect;
	}

	/**
	 * Toggle the mode to reconnect between error retries.
	 * 
	 * <p>
	 * When enabled, if an IO error occurs while executing a transaction the
	 * connection will be closed and reopened.
	 * </p>
	 * 
	 * @param retryReconnect
	 *        {@literal} true to reconnect between error retries,
	 *        {@literal false} to continue using the same connection
	 */
	public void setRetryReconnect(boolean retryReconnect) {
		this.retryReconnect = retryReconnect;
	}
}
