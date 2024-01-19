/* ==================================================================
 * LockingMBusConnection.java - 19/01/2024 12:44:39 pm
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.mbus.jmbus;

import static java.lang.String.format;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import net.solarnetwork.node.io.mbus.MBusConnection;
import net.solarnetwork.node.io.mbus.MBusData;
import net.solarnetwork.node.service.LockTimeoutException;

/**
 * A {@link MBusConnection} that wraps another connection with a lock so only
 * one thread at a time can access the network underlying the connection.
 * 
 * <p>
 * The {@link #open()} method will acquire the lock, and the {@link #close()}
 * method will release the lock.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 2.1
 */
public class LockingMBusConnection implements MBusConnection {

	private final MBusConnection delegate;
	private final ReentrantLock lock;
	private final long timeout;
	private final TimeUnit timeoutUnit;
	private final String description;
	private final Logger log;

	/**
	 * Constructor.
	 * 
	 * @param delegate
	 *        the delegate connection
	 * @param lock
	 *        the lock
	 * @param timeout
	 *        a timeout duration
	 * @param timeoutUnit
	 *        the timeout unit
	 * @param description
	 *        a description of the connection to use in logs
	 * @param log
	 *        a logger to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public LockingMBusConnection(MBusConnection delegate, ReentrantLock lock, long timeout,
			TimeUnit timeoutUnit, String description, Logger log) {
		super();
		this.delegate = requireNonNullArgument(delegate, "delegate");
		this.lock = requireNonNullArgument(lock, "lock");
		this.timeout = timeout;
		this.timeoutUnit = requireNonNullArgument(timeoutUnit, "timeoutUnit");
		this.description = requireNonNullArgument(description, "description");
		this.log = requireNonNullArgument(log, "log");
	}

	/**
	 * Acquire the lock, returning if lock acquired.
	 * 
	 * @throws LockTimeoutException
	 *         if the lock cannot be obtained
	 */
	private void acquireLock() throws LockTimeoutException {
		if ( lock.isHeldByCurrentThread() ) {
			log.debug("Lock on M-Bus network {} already acquired", description);
			return;
		}
		log.debug("Acquiring lock on M-Bus network {}; waiting at most {} {}",
				new Object[] { description, timeout, timeoutUnit });
		try {
			final long ts = System.currentTimeMillis();
			if ( lock.tryLock(timeout, timeoutUnit) ) {
				if ( log.isDebugEnabled() ) {
					long t = System.currentTimeMillis() - ts;
					log.debug("Acquired lock on M-Bus network {} in {}ms", description, t);
				}
				return;
			}
			if ( log.isDebugEnabled() ) {
				long t = System.currentTimeMillis() - ts;
				log.debug("Timeout acquiring lock on M-Bus network {} after {}ms", description, t);
			}
		} catch ( InterruptedException e ) {
			log.debug("Interrupted waiting for lock on M-Bus network {}", description);
		}
		throw new LockTimeoutException(format("Could not acquire lock on M-Bus network %s within %d %s",
				description, timeout, timeoutUnit.toString().toLowerCase()));
	}

	/**
	 * Release the lock previously obtained via {@link #acquireLock()}.
	 * 
	 * <p>
	 * This method is safe to call even if the lock has already been released.
	 * </p>
	 */
	private void releaseLock() {
		if ( lock.isHeldByCurrentThread() ) {
			log.debug("Releasing lock on M-Bus network {}", description);
			lock.unlock();
		}
	}

	@Override
	public void open() throws IOException {
		acquireLock();
		delegate.open();
	}

	@Override
	public void close() throws IOException {
		try {
			delegate.close();
		} finally {
			releaseLock();
		}
	}

	@Override
	public MBusData read() {
		return delegate.read();
	}

}
