/* ==================================================================
 * StompAuthenticationDetails.java - 16/08/2021 9:40:30 AM
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

package net.solarnetwork.node.setup.stomp;

import java.io.Serializable;
import java.util.UUID;

/**
 * Authentication details for STOMP.
 * 
 * @author matt
 * @version 1.0
 */
public class StompAuthenticationDetails implements Serializable {

	private static final long serialVersionUID = -6685762423353189279L;

	private final UUID sessionId;

	/**
	 * Constructor.
	 * 
	 * @param sessionId
	 *        the session ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public StompAuthenticationDetails(UUID sessionId) {
		super();
		if ( sessionId == null ) {
			throw new IllegalArgumentException("The sessionId argument must not be null.");
		}
		this.sessionId = sessionId;
	}

	/**
	 * Get the session ID
	 * 
	 * @return the session ID
	 */
	public UUID getSessionId() {
		return sessionId;
	}

}
