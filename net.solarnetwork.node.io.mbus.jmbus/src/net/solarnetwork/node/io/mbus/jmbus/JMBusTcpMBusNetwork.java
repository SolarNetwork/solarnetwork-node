/* ==================================================================
 * JMBusTcpMBusNetwork.java - 5/02/2024 9:03:56 am
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

import java.io.IOException;
import java.util.List;
import org.openmuc.jmbus.MBusConnection;
import org.openmuc.jmbus.MBusConnection.MBusTcpBuilder;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * FIXME
 * 
 * <p>
 * TODO
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class JMBusTcpMBusNetwork extends JMBusMBusNetwork implements SettingSpecifierProvider {

	private String host;
	private Integer port;

	/**
	 * Constructor.
	 */
	public JMBusTcpMBusNetwork() {
		super();
	}

	@Override
	protected org.openmuc.jmbus.MBusConnection createJMBusConnection() throws IOException {
		final Integer port = this.port;
		final MBusTcpBuilder builder = MBusConnection.newTcpBuilder(host, port != null ? port : 0);
		return builder.build();
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.io.mbus.tcp";
	}

	@Override
	public String getDisplayName() {
		return "M-Bus TCP Connection";
	}

	@Override
	protected String getNetworkDescription() {
		final String host = this.host;
		final Integer port = this.port;
		if ( host != null && !host.isEmpty() ) {
			return host + (port != null && port.intValue() > 0 ? ":" + port : "");
		} else if ( getUid() != null ) {
			return getUid();
		}
		return super.toString();
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder("JMBusTcpMBusNetwork{");
		buf.append(getNetworkDescription());
		buf.append("}");
		return buf.toString();
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = basicIdentifiableSettings();
		results.add(new BasicTextFieldSettingSpecifier("host", null));
		results.add(new BasicTextFieldSettingSpecifier("port", null));
		results.addAll(super.getSettingSpecifiers());
		return results;
	}

	/**
	 * Get the host to connect to.
	 * 
	 * @return the host name or IP address
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Set the host to connect to.
	 * 
	 * @param host
	 *        the host name or IP address to set
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Get the IP port to connect on.
	 * 
	 * @return the port
	 */
	public Integer getPort() {
		return port;
	}

	/**
	 * Set the IP port to connect on.
	 * 
	 * @param port
	 *        the port to set
	 */
	public void setPort(Integer port) {
		this.port = port;
	}

}
