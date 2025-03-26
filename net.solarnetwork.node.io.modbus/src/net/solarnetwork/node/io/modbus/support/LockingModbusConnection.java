/* ==================================================================
 * LockingModbusConnection.java - 2/07/2021 11:30:59 AM
 *
 * Copyright 2021 SolarNetwork.net Dev Team
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
import java.nio.charset.Charset;
import java.util.BitSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusWriteFunction;
import net.solarnetwork.node.service.LockTimeoutException;

/**
 * A {@link ModbusConnection} that wraps another connection with a lock.
 *
 * <p>
 * The {@link #open()} method will acquire the lock, and the {@link #close()}
 * method will release the lock.
 * </p>
 *
 * @author matt
 * @version 2.2
 * @since 3.3
 */
public class LockingModbusConnection implements ModbusConnection {

	private final ModbusConnection delegate;
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
	 *        the lock to use
	 * @param timeout
	 *        the lock timeout
	 * @param timeoutUnit
	 *        the lock timeout unit
	 * @param description
	 *        a logging description
	 * @param log
	 *        the logger to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public LockingModbusConnection(ModbusConnection delegate, ReentrantLock lock, long timeout,
			TimeUnit timeoutUnit, String description, Logger log) {
		super();
		if ( delegate == null ) {
			throw new IllegalArgumentException("The delegate argument must not be null.");
		}
		this.delegate = delegate;
		if ( lock == null ) {
			throw new IllegalArgumentException("The lock argument must not be null.");
		}
		this.lock = lock;
		this.timeout = timeout;
		if ( timeoutUnit == null ) {
			throw new IllegalArgumentException("The timeoutUnit argument must not be null.");
		}
		this.timeoutUnit = timeoutUnit;
		if ( description == null ) {
			throw new IllegalArgumentException("The description argument must not be null.");
		}
		this.description = description;
		if ( log == null ) {
			throw new IllegalArgumentException("The log argument must not be null.");
		}
		this.log = log;
	}

	/**
	 * Acquire the lock, returning if lock acquired.
	 *
	 * @throws LockTimeoutException
	 *         if the lock cannot be obtained
	 */
	private void acquireLock() throws LockTimeoutException {
		if ( lock.isHeldByCurrentThread() ) {
			log.debug("Lock on Modbus port {} already acquired", description);
			return;
		}
		log.debug("Acquiring lock on Modbus port {}; waiting at most {} {}",
				new Object[] { description, timeout, timeoutUnit });
		try {
			final long ts = System.currentTimeMillis();
			if ( lock.tryLock(timeout, timeoutUnit) ) {
				if ( log.isDebugEnabled() ) {
					long t = System.currentTimeMillis() - ts;
					log.debug("Acquired lock on Modbus port {} in {}ms", description, t);
				}
				return;
			}
			if ( log.isDebugEnabled() ) {
				long t = System.currentTimeMillis() - ts;
				log.debug("Timeout acquiring lock on Modbus port {} after {}ms", description, t);
			}
		} catch ( InterruptedException e ) {
			log.debug("Interrupted waiting for lock on Modbus port {}", description);
		}
		throw new LockTimeoutException(format("Could not acquire lock on Modbus port %s within %d %s",
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
			log.debug("Releasing lock on Modbus port {}", description);
			lock.unlock();
		}
	}

	@Override
	public int getUnitId() {
		return delegate.getUnitId();
	}

	@Override
	public void open() throws IOException, LockTimeoutException {
		acquireLock();
		delegate.open();
	}

	@Override
	public void close() {
		try {
			delegate.close();
		} finally {
			releaseLock();
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public BitSet readDiscreetValues(int address, int count) throws IOException {
		return delegate.readDiscreetValues(address, count);
	}

	@SuppressWarnings("deprecation")
	@Override
	public BitSet readDiscreetValues(int[] addresses, int count) throws IOException {
		return delegate.readDiscreetValues(addresses, count);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void writeDiscreetValues(int[] addresses, BitSet bits) throws IOException {
		delegate.writeDiscreetValues(addresses, bits);
	}

	@Override
	public void writeDiscreteValues(ModbusWriteFunction function, int address, int count, BitSet bits)
			throws IOException {
		delegate.writeDiscreteValues(function, address, count, bits);
	}

	@Override
	public BitSet readInputDiscreteValues(int address, int count) throws IOException {
		return delegate.readInputDiscreteValues(address, count);
	}

	@Override
	public short[] readWords(ModbusReadFunction function, int address, int count) throws IOException {
		return delegate.readWords(function, address, count);
	}

	@Override
	public int[] readWordsUnsigned(ModbusReadFunction function, int address, int count)
			throws IOException {
		return delegate.readWordsUnsigned(function, address, count);
	}

	@Override
	public void writeWords(ModbusWriteFunction function, int address, short[] values)
			throws IOException {
		delegate.writeWords(function, address, values);
	}

	@Override
	public void writeWords(ModbusWriteFunction function, int address, int[] values) throws IOException {
		delegate.writeWords(function, address, values);
	}

	@Override
	public byte[] readBytes(ModbusReadFunction function, int address, int count) throws IOException {
		return delegate.readBytes(function, address, count);
	}

	@Override
	public void writeBytes(ModbusWriteFunction function, int address, byte[] values) throws IOException {
		delegate.writeBytes(function, address, values);
	}

	@Override
	public String readString(ModbusReadFunction function, int address, int count, boolean trim,
			Charset charset) throws IOException {
		return delegate.readString(function, address, count, trim, charset);
	}

	@Override
	public void writeString(ModbusWriteFunction function, int address, String value, Charset charset)
			throws IOException {
		delegate.writeString(function, address, value, charset);
	}

}
