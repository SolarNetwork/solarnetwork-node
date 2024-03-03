/* ==================================================================
 * SetupIdentityInfo.java - 3/11/2017 6:36:05 AM
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

package net.solarnetwork.node.setup.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Information about a SolarNode setup identity.
 *
 * @author matt
 * @version 1.0
 */
public class SetupIdentityInfo {

	/**
	 * An identity for when there is no real one availalble.
	 */
	public static final SetupIdentityInfo UNKNOWN_IDENTITY = new SetupIdentityInfo(null, null, null,
			null, false, null);

	private final Long nodeId;
	private final String confirmationCode;
	private final String solarNetHostName;
	private final Integer solarNetHostPort;
	private final boolean solarNetForceTls;
	private final String keyStorePassword;

	/**
	 * Constructor.
	 *
	 * @param nodeId
	 *        the node ID
	 * @param confirmationCode
	 *        the SolarNetwork association confirmation code
	 * @param solarNetHostName
	 *        the SolarNetwork host name
	 * @param solarNetHostPort
	 *        the SolarNetwork host port
	 * @param solarNetForceTls
	 *        {@literal true} to force TLS when {@code port} is not
	 *        {@literal 443}
	 * @param keyStorePassword
	 *        the password to use for the key store
	 */
	@JsonCreator
	public SetupIdentityInfo(@JsonProperty("nodeId") Long nodeId,
			@JsonProperty("confirmationCode") String confirmationCode,
			@JsonProperty("solarNetHostName") String solarNetHostName,
			@JsonProperty("solarNetHostPort") Integer solarNetHostPort,
			@JsonProperty("solarNetForceTls") boolean solarNetForceTls,
			@JsonProperty("keyStorePassword") String keyStorePassword) {
		super();
		this.nodeId = nodeId;
		this.confirmationCode = confirmationCode;
		this.solarNetHostName = solarNetHostName;
		this.solarNetHostPort = solarNetHostPort;
		this.solarNetForceTls = solarNetForceTls;
		this.keyStorePassword = keyStorePassword;
	}

	/**
	 * Instantiate a new info instance with a specific key store password.
	 *
	 * @param newPassword
	 *        the new password
	 * @return the new instance
	 */
	public SetupIdentityInfo withKeyStorePassword(String newPassword) {
		return new SetupIdentityInfo(nodeId, confirmationCode, solarNetHostName, solarNetHostPort,
				solarNetForceTls, newPassword);
	}

	@Override
	public String toString() {
		return "SetupIdentityInfo{" + nodeId + "@" + solarNetHostName + ":" + solarNetHostPort + "}";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((confirmationCode == null) ? 0 : confirmationCode.hashCode());
		result = prime * result + ((keyStorePassword == null) ? 0 : keyStorePassword.hashCode());
		result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
		result = prime * result + (solarNetForceTls ? 1231 : 1237);
		result = prime * result + ((solarNetHostName == null) ? 0 : solarNetHostName.hashCode());
		result = prime * result + ((solarNetHostPort == null) ? 0 : solarNetHostPort.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( !(obj instanceof SetupIdentityInfo) ) {
			return false;
		}
		SetupIdentityInfo other = (SetupIdentityInfo) obj;
		if ( confirmationCode == null ) {
			if ( other.confirmationCode != null ) {
				return false;
			}
		} else if ( !confirmationCode.equals(other.confirmationCode) ) {
			return false;
		}
		if ( keyStorePassword == null ) {
			if ( other.keyStorePassword != null ) {
				return false;
			}
		} else if ( !keyStorePassword.equals(other.keyStorePassword) ) {
			return false;
		}
		if ( nodeId == null ) {
			if ( other.nodeId != null ) {
				return false;
			}
		} else if ( !nodeId.equals(other.nodeId) ) {
			return false;
		}
		if ( solarNetForceTls != other.solarNetForceTls ) {
			return false;
		}
		if ( solarNetHostName == null ) {
			if ( other.solarNetHostName != null ) {
				return false;
			}
		} else if ( !solarNetHostName.equals(other.solarNetHostName) ) {
			return false;
		}
		if ( solarNetHostPort == null ) {
			if ( other.solarNetHostPort != null ) {
				return false;
			}
		} else if ( !solarNetHostPort.equals(other.solarNetHostPort) ) {
			return false;
		}
		return true;
	}

	/**
	 * Get the node ID.
	 *
	 * @return the node ID
	 */
	public Long getNodeId() {
		return nodeId;
	}

	/**
	 * Get the confirmation code.
	 *
	 * @return the code
	 */
	public String getConfirmationCode() {
		return confirmationCode;
	}

	/**
	 * Get the host name.
	 *
	 * @return the name
	 */
	public String getSolarNetHostName() {
		return solarNetHostName;
	}

	/**
	 * Get the host port.
	 *
	 * @return the port
	 */
	public Integer getSolarNetHostPort() {
		return solarNetHostPort;
	}

	/**
	 * Get the "force TLS" flag.
	 *
	 * @return the flag
	 */
	public boolean isSolarNetForceTls() {
		return solarNetForceTls;
	}

	/**
	 * Get the keystore password.
	 *
	 * @return the password
	 */
	public String getKeyStorePassword() {
		return keyStorePassword;
	}

}
