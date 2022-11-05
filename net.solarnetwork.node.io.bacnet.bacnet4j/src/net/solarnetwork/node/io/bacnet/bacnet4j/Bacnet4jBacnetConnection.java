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
import java.util.Objects;
import com.serotonin.bacnet4j.LocalDevice;
import net.solarnetwork.node.io.bacnet.BacnetConnection;
import net.solarnetwork.node.io.bacnet.BacnetDeviceObjectPropertyRef;

/**
 * BACnet4J implementation of {@link BacnetConnection}.
 * 
 * @author matt
 * @version 1.0
 */
public class Bacnet4jBacnetConnection implements BacnetConnection {

	private final Integer id;
	private final Bacnet4jNetworkOps networkOps;
	private final LocalDevice localDevice;
	private boolean closed;

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
		networkOps.releaseConnection(this);
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
	public int covSubscribe(Collection<BacnetDeviceObjectPropertyRef> refs, int maxDelay) {
		int subId = networkOps.nextSubscriptionId();
		networkOps.covSubscribe(subId, refs, maxDelay);
		return subId;
	}

	@Override
	public void covUnsubscribe(int subscriptionId) {
		// TODO Auto-generated method stub

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
