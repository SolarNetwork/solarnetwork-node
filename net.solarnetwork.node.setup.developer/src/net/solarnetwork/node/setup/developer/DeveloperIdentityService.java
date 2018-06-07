/* ==================================================================
 * DeveloperIdentityService.java - Mar 8, 2014 4:07:36 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.developer;

import java.security.Principal;
import net.solarnetwork.node.IdentityService;

/**
 * Implementation of {@link IdentityService} for development and testing
 * purposes.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>nodeId</dt>
 * <dd>The node ID to use. Defaults to {@code -11}.</dd>
 * 
 * <dt>hostName</dt>
 * <dd>The host name to use. Defaults to {@code localhost}.</dd>
 * 
 * <dt>port</dt>
 * <dd>The default port to use. Defaults to {@code 8080}.</dd>
 * 
 * <dt>solarInUrlPrefix</dt>
 * <dd>The SolarIn URL prefix to ues. Defaults to {@code /solarin}.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.2
 */
public class DeveloperIdentityService implements IdentityService {

	private Long nodeId = -11L;
	private String hostName = "localhost";
	private int port = 8080;
	private String solarInUrlPrefix = "/solarin";
	private int mqttPort = 1883;

	@Override
	public Long getNodeId() {
		return nodeId;
	}

	@Override
	public Principal getNodePrincipal() {
		return null;
	}

	@Override
	public String getSolarNetHostName() {
		return hostName;
	}

	@Override
	public Integer getSolarNetHostPort() {
		return port;
	}

	@Override
	public String getSolarNetSolarInUrlPrefix() {
		return solarInUrlPrefix;
	}

	@Override
	public String getSolarInBaseUrl() {
		return "http" + (port == 443 ? "s" : "") + "://" + hostName
				+ (port == 443 || port == 80 ? "" : (":" + port)) + solarInUrlPrefix;
	}

	@Override
	public String getSolarInMqttUrl() {
		return "mqtt://" + hostName + ":" + mqttPort;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setSolarInUrlPrefix(String solarInUrlPrefix) {
		this.solarInUrlPrefix = solarInUrlPrefix;
	}

	/**
	 * Set the MQTT port to use.
	 * 
	 * @param mqttPort
	 *        the port to use
	 */
	public void setMqttPort(int mqttPort) {
		this.mqttPort = mqttPort;
	}

}
