/* ==================================================================
 * AbstractCanbusNetwork.java - 19/09/2019 4:19:37 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.canbus.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.LockTimeoutException;
import net.solarnetwork.node.io.canbus.CanbusNetwork;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.support.BaseIdentifiable;

/**
 * Base implementation of {@link CanbusNetwork} for other implementations to
 * extend.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class AbstractCanbusNetwork extends BaseIdentifiable implements CanbusNetwork {

	/** The default value for the {@code timeout} property. */
	public static final long DEFAULT_TIMEOUT = 10L;

	private final ReentrantLock lock = new ReentrantLock(true); // use fair lock to prevent starvation

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private long timeout = 10L;
	private TimeUnit timeoutUnit = TimeUnit.SECONDS;

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
	 * Get a list of base network settings.
	 * 
	 * @return the base network settings
	 */
	public static List<SettingSpecifier> getBaseNetworkSettings(String prefix) {
		if ( prefix == null ) {
			prefix = "";
		}
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(4);
		results.add(
				new BasicTextFieldSettingSpecifier(prefix + "timeout", String.valueOf(DEFAULT_TIMEOUT)));
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

}
