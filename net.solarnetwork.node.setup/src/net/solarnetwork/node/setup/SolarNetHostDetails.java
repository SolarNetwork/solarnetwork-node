/* ==================================================================
 * SolarNetHostDetails.java - Jun 1, 2010 3:32:45 PM
 * 
 * Copyright 2007-2010 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.setup;

import java.io.Serializable;

import org.joda.time.DateTime;

/**
 * Detail object for SolarNetwork setup.
 * 
 * @author matt
 * @version $Id$
 */
public class SolarNetHostDetails implements Serializable {

	private static final long serialVersionUID = -4023400824843376831L;

	private String hostName;
	private Integer hostPort;
	private boolean forceTLS = false;
	private String identity;
	private String confirmationKey;
	private String confirmationCode;
	private String tos;
	private String userName;
	private Long nodeId;
	private DateTime expiration;
	
	@Override
	public String toString() {
		return "SolarNetHostDetails{hostName=" + hostName 
			+ ",nodeId=" +nodeId +'}';
	}

	/**
	 * @return the tos
	 */
	public String getTos() {
		return tos;
	}

	/**
	 * @param tos the tos to set
	 */
	public void setTos(String tos) {
		this.tos = tos;
	}

	/**
	 * @return the hostName
	 */
	public String getHostName() {
		return hostName;
	}

	/**
	 * @param hostName the hostName to set
	 */
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	/**
	 * @return the identity
	 */
	public String getIdentity() {
		return identity;
	}
	
	/**
	 * @param identity the identity to set
	 */
	public void setIdentity(String identity) {
		this.identity = identity;
	}
	
	/**
	 * @return the nodeId
	 */
	public Long getNodeId() {
		return nodeId;
	}
	
	/**
	 * @param nodeId the nodeId to set
	 */
	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * @return the hostPort
	 */
	public Integer getHostPort() {
		return hostPort;
	}

	/**
	 * @param hostPort the hostPort to set
	 */
	public void setHostPort(Integer hostPort) {
		this.hostPort = hostPort;
	}

	/**
	 * @return the forceTLS
	 */
	public boolean isForceTLS() {
		return forceTLS;
	}

	/**
	 * @param forceTLS the forceTLS to set
	 */
	public void setForceTLS(boolean forceTLS) {
		this.forceTLS = forceTLS;
	}

	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * @return the confirmationKey
	 */
	public String getConfirmationKey() {
		return confirmationKey;
	}

	/**
	 * @param confirmationKey the confirmationKey to set
	 */
	public void setConfirmationKey(String confirmationKey) {
		this.confirmationKey = confirmationKey;
	}

	/**
	 * @return the expiration
	 */
	public DateTime getExpiration() {
		return expiration;
	}

	/**
	 * @param expiration the expiration to set
	 */
	public void setExpiration(DateTime expiration) {
		this.expiration = expiration;
	}

	/**
	 * @return the confirmationCode
	 */
	public String getConfirmationCode() {
		return confirmationCode;
	}

	/**
	 * @param confirmationCode the confirmationCode to set
	 */
	public void setConfirmationCode(String confirmationCode) {
		this.confirmationCode = confirmationCode;
	}
	
}
