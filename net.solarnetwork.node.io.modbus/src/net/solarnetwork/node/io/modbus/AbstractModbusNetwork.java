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

package net.solarnetwork.node.io.modbus;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.node.LockTimeoutException;
import net.wimpi.modbus.ModbusIOException;

/**
 * Abstract implementation of {@link ModbusNetwork}.
 * 
 * @author matt
 * @version 1.0
 * @since 2.4
 */
public abstract class AbstractModbusNetwork implements ModbusNetwork {

	private String uid = "Modbus Port";
	private String groupUID;
	private long timeout = 10L;
	private TimeUnit timeoutUnit = TimeUnit.SECONDS;
	private MessageSource messageSource;
	private boolean headless = true;

	private final ReentrantLock lock = new ReentrantLock(true); // use fair lock to prevent starvation

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public <T> T performAction(ModbusConnectionAction<T> action, int unitId) throws IOException {
		ModbusConnection conn = null;
		try {
			conn = createConnection(unitId);
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

			if ( t instanceof ModbusIOException ) {
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
		if ( lock.isLocked() ) {
			log.debug("Port {} lock already acquired", desc);
			return;
		}
		log.debug("Acquiring lock on Modbus port {}; waiting at most {} {}",
				new Object[] { desc, getTimeout(), getTimeoutUnit() });
		try {
			if ( lock.tryLock(getTimeout(), getTimeoutUnit()) ) {
				log.debug("Acquired port {} lock", desc);
				return;
			}
			log.debug("Timeout acquiring port {} lock", desc);
		} catch ( InterruptedException e ) {
			log.debug("Interrupted waiting for port {} lock", desc);
		}
		throw new LockTimeoutException("Could not acquire port " + desc + " lock");
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
		if ( lock.isLocked() ) {
			final String desc = getNetworkDescription();
			log.debug("Releasing lock on {}", desc);
			lock.unlock();
		}
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
	 * Alias for the {@link #getUID()} method.
	 * 
	 * @return the unique ID
	 * @see #getUID()
	 */
	public String getUid() {
		return uid;
	}

	/**
	 * Set the unique ID to identify this service with.
	 * 
	 * @param uid
	 *        the unique ID; defaults to {@literal Modbus Port}
	 */
	public void setUid(String uid) {
		this.uid = uid;
	}

	@Override
	public String getUID() {
		return uid;
	}

	@Override
	public String getGroupUID() {
		return groupUID;
	}

	/**
	 * Set the group unique ID to identify this service with.
	 * 
	 * @param groupUID
	 *        the group unique ID
	 */
	public void setGroupUID(String groupUID) {
		this.groupUID = groupUID;
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
	 * Get a message source to resolve localized strings with.
	 * 
	 * @return a message source, or {@literal null}
	 */
	public MessageSource getMessageSource() {
		return messageSource;
	}

	/**
	 * Set a message source to resolve localized strings with.
	 * 
	 * @param messageSource
	 *        the message source
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
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

}
