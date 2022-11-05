/* ==================================================================
 * Bacnet4jBacnetIpNetwork.java - 2/11/2022 10:42:19 am
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

import java.util.List;
import com.serotonin.bacnet4j.npdu.Network;
import com.serotonin.bacnet4j.npdu.ip.IpNetwork;
import com.serotonin.bacnet4j.npdu.ip.IpNetworkBuilder;
import com.serotonin.bacnet4j.type.constructed.Address;
import net.solarnetwork.node.io.bacnet.BacnetNetwork;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * BACnet4J implementation of {@link BacnetNetwork} for IP transport.
 * 
 * @author matt
 * @version 1.0
 */
public class Bacnet4jIpBacnetNetwork extends AbstractBacnet4jBacnetNetwork {

	/** The {@code subnet} property default value. */
	public static final String DEFAULT_SUBNET = "192.168.1.0";

	/** The {@code networkPrefixLength} property default value. */
	public static final int DEFAULT_NETWORK_PREFIX_LENGTH = 24;

	private String bindAddress = IpNetwork.DEFAULT_BIND_IP;
	private String subnet = DEFAULT_SUBNET;
	private int networkPrefixLength = DEFAULT_NETWORK_PREFIX_LENGTH;
	private int port = IpNetwork.DEFAULT_PORT;
	private int networkNumber = Address.LOCAL_NETWORK;

	public Bacnet4jIpBacnetNetwork() {
		super();
		setDisplayName("BACnet/IP");
	}

	@Override
	public String getNetworkDescription() {
		return subnet + "/" + networkPrefixLength + ":" + port;
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.io.bacnet.ip";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = super.getSettingSpecifiers();
		result.add(new BasicTextFieldSettingSpecifier("bindAddress", IpNetwork.DEFAULT_BIND_IP));
		result.add(new BasicTextFieldSettingSpecifier("subnet", DEFAULT_SUBNET));
		result.add(new BasicTextFieldSettingSpecifier("networkPrefixLength",
				String.valueOf(DEFAULT_NETWORK_PREFIX_LENGTH)));
		result.add(new BasicTextFieldSettingSpecifier("port", String.valueOf(IpNetwork.DEFAULT_PORT)));
		result.add(new BasicTextFieldSettingSpecifier("networkNumber",
				String.valueOf(Address.LOCAL_NETWORK)));
		return result;
	}

	@Override
	protected Network createNetwork() {
		// @formatter:off
        return new IpNetworkBuilder()
                .withLocalBindAddress(bindAddress)
                .withSubnet(subnet, networkPrefixLength)
                .withPort(port)
                .withLocalNetworkNumber(networkNumber)
                .withReuseAddress(true)
                .build();
        // @formatter:on
	}

	/**
	 * Get the bind address.
	 * 
	 * @return the bind address
	 */
	public String getBindAddress() {
		return bindAddress;
	}

	/**
	 * Set the bind address.
	 * 
	 * @param bindAddress
	 *        the bind address to set
	 */
	public void setBindAddress(String bindAddress) {
		this.bindAddress = bindAddress;
	}

	/**
	 * Get the subnet.
	 * 
	 * @return the subnet
	 */
	public String getSubnet() {
		return subnet;
	}

	/**
	 * Set the subnet.
	 * 
	 * @param subnet
	 *        the subnet to set
	 */
	public void setSubnet(String subnet) {
		this.subnet = subnet;
	}

	/**
	 * Get the network prefix length.
	 * 
	 * @return the network prefix length
	 */
	public int getNetworkPrefixLength() {
		return networkPrefixLength;
	}

	/**
	 * Set the network prefix length.
	 * 
	 * @param networkPrefixLength
	 *        the network prefix length to set
	 */
	public void setNetworkPrefixLength(int networkPrefixLength) {
		this.networkPrefixLength = networkPrefixLength;
	}

	/**
	 * Get the port.
	 * 
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Set the port.
	 * 
	 * @param port
	 *        the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Get the network number.
	 * 
	 * @return the network number
	 */
	public int getNetworkNumber() {
		return networkNumber;
	}

	/**
	 * Set the network number.
	 * 
	 * @param networkNumber
	 *        the network number to set
	 */
	public void setNetworkNumber(int networkNumber) {
		this.networkNumber = networkNumber;
	}

}
