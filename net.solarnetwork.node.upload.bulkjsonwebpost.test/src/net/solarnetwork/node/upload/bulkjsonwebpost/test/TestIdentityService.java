/* ==================================================================
 * TestIdentityService.java - 8/08/2017 7:47:13 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.upload.bulkjsonwebpost.test;

import java.security.Principal;
import net.solarnetwork.node.service.IdentityService;

/**
 * Helper implementation of {@link IdentityService}.
 * 
 * @author matt
 * @version 2.0
 */
public class TestIdentityService implements IdentityService {

	private final Long nodeId;
	private final Integer port;

	/**
	 * Constructor.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param port
	 *        the HTTP port to use
	 */
	public TestIdentityService(Long nodeId, int port) {
		super();
		this.nodeId = nodeId;
		this.port = port;
	}

	@Override
	public Long getNodeId() {
		return nodeId;
	}

	@Override
	public Principal getNodePrincipal() {
		return new Principal() {

			@Override
			public String getName() {
				return nodeId.toString();
			}
		};
	}

	@Override
	public String getSolarNetHostName() {
		return "localhost";
	}

	@Override
	public Integer getSolarNetHostPort() {
		return port;
	}

	@Override
	public String getSolarNetSolarInUrlPrefix() {
		return "/solarin";
	}

	@Override
	public String getSolarInBaseUrl() {
		return "http://" + getSolarNetHostName() + ":" + port + getSolarNetSolarInUrlPrefix();
	}

	@Override
	public String getSolarInMqttUrl() {
		return null;
	}

}
