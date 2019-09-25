/* ==================================================================
 * NonClosingCanbusConnection.java - 25/09/2019 7:24:13 am
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

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Future;
import net.solarnetwork.node.io.canbus.CanbusConnection;
import net.solarnetwork.node.io.canbus.CanbusFrameListener;

/**
 * A {@link CanbusConnection} that delegates to another connection, except for
 * ignoring the {@code close} method.
 * 
 * @author matt
 * @version 1.0
 */
public class NonClosingCanbusConnection implements CanbusConnection {

	private final CanbusConnection delegate;

	/**
	 * Constructor.
	 * 
	 * @param delegate
	 *        the delegate connection
	 * @throws IllegalArgumentException
	 *         if {@code delegate} is {@literal null}
	 */
	public NonClosingCanbusConnection(CanbusConnection delegate) {
		super();
		if ( delegate == null ) {
			throw new IllegalArgumentException("The delegate connection must not be null.");
		}
		this.delegate = delegate;
	}

	/**
	 * Get the delegate connection.
	 * 
	 * @return the delegate, never {@literal null}
	 */
	public CanbusConnection getDelegate() {
		return delegate;
	}

	/**
	 * This method does nothing in this class.
	 */
	@Override
	public void close() throws IOException {
		// this is ignored
	}

	@Override
	public String getBusName() {
		return delegate.getBusName();
	}

	@Override
	public void open() throws IOException {
		delegate.open();
	}

	@Override
	public boolean isEstablished() {
		return delegate.isEstablished();
	}

	@Override
	public boolean isClosed() {
		return delegate.isClosed();
	}

	@Override
	public Future<Boolean> verifyConnectivity() {
		return delegate.verifyConnectivity();
	}

	@Override
	public void subscribe(int address, boolean forceExtendedAddress, Duration limit, long dataFilter,
			CanbusFrameListener listener) throws IOException {
		delegate.subscribe(address, forceExtendedAddress, limit, dataFilter, listener);
	}

	@Override
	public void subscribe(int address, boolean forceExtendedAddress, Duration limit, long identifierMask,
			Iterable<Long> dataFilters, CanbusFrameListener listener) throws IOException {
		delegate.subscribe(address, forceExtendedAddress, limit, identifierMask, dataFilters, listener);
	}

	@Override
	public void unsubscribe(int address, boolean forceExtendedAddress) throws IOException {
		delegate.unsubscribe(address, forceExtendedAddress);
	}

	@Override
	public void monitor(CanbusFrameListener listener) throws IOException {
		delegate.monitor(listener);
	}

	@Override
	public void unmonitor() throws IOException {
		delegate.unmonitor();
	}

}
