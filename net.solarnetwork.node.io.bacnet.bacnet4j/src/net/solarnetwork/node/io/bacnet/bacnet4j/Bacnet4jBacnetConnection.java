/* ==================================================================
 * Bacnet4jBacnetConnection.java - 2/11/2022 11:10:42 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.bacnet.bacnet4j;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.serotonin.bacnet4j.LocalDevice;
import net.solarnetwork.node.io.bacnet.BacnetConnection;
import net.solarnetwork.node.io.bacnet.BacnetCovHandler;
import net.solarnetwork.node.io.bacnet.BacnetDeviceObjectPropertyRef;

/**
 * BACnet4J implementation of {@link BacnetConnection}.
 * 
 * @author matt
 * @version 1.0
 */
public class Bacnet4jBacnetConnection implements BacnetConnection, BacnetCovHandler {

	private final Integer id;
	private final Bacnet4jNetworkOps networkOps;
	private final LocalDevice localDevice;
	private boolean closed;
	private final Set<Integer> subscriptions = new HashSet<>();
	private final Set<BacnetCovHandler> covHandlers = new HashSet<>();

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        a unique ID for this connection
	 * @param networkOps
	 *        API to network operations
	 * @param localDevice
	 *        the local device
	 */
	public Bacnet4jBacnetConnection(Integer id, Bacnet4jNetworkOps networkOps, LocalDevice localDevice) {
		super();
		this.id = requireNonNullArgument(id, "id");
		this.networkOps = requireNonNullArgument(networkOps, "networkOps");
		this.localDevice = requireNonNullArgument(localDevice, "localDevice");
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Bacnet4jBacnetConnection{id=");
		builder.append(id);
		builder.append(", network=");
		builder.append(networkOps.getNetworkDescription());
		builder.append("}");
		return builder.toString();
	}

	@Override
	public void close() throws IOException {
		this.closed = true;
		synchronized ( subscriptions ) {
			for ( Integer subscriptionId : subscriptions ) {
				covUnsubscribe(subscriptionId);
			}
			subscriptions.clear();
		}
		networkOps.releaseConnection(this);
		covHandlers.clear();
		networkOps.removeCovHandler(this);
	}

	@Override
	public void open() throws IOException {
		// nothing to do
	}

	@Override
	public boolean isEstablished() {
		return !closed && localDevice.isInitialized();
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof Bacnet4jBacnetConnection) ) {
			return false;
		}
		Bacnet4jBacnetConnection other = (Bacnet4jBacnetConnection) obj;
		return Objects.equals(id, other.id);
	}

	@Override
	public void addCovHandler(BacnetCovHandler handler) {
		if ( covHandlers.add(handler) ) {
			networkOps.addCovHandler(this);
		}
	}

	@Override
	public void removeCovHandler(BacnetCovHandler handler) {
		if ( covHandlers.remove(handler) && covHandlers.isEmpty() ) {
			networkOps.addCovHandler(this);
		}
	}

	@Override
	public int covSubscribe(Collection<BacnetDeviceObjectPropertyRef> refs, int maxDelay) {
		int subId = networkOps.nextSubscriptionId();
		networkOps.covSubscribe(subId, refs, maxDelay);
		synchronized ( subscriptions ) {
			subscriptions.add(subId);
		}
		return subId;
	}

	@Override
	public void covUnsubscribe(int subscriptionId) {
		networkOps.covUnsubscribe(subscriptionId);
	}

	@Override
	public Map<BacnetDeviceObjectPropertyRef, ?> propertyValues(
			Collection<BacnetDeviceObjectPropertyRef> refs) {
		return networkOps.propertyValues(refs);
	}

	@Override
	public void accept(Integer subscriptionId, Map<BacnetDeviceObjectPropertyRef, ?> updates) {
		if ( subscriptions.contains(subscriptionId) ) {
			for ( BacnetCovHandler handler : covHandlers ) {
				handler.accept(subscriptionId, updates);
			}
		}
	}

	/**
	 * Get the connection ID.
	 * 
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}

}
