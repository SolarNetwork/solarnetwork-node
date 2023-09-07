/* ==================================================================
 * SecurityActor.java - Dec 11, 2012 1:36:04 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.domain;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import net.solarnetwork.security.SecurityException;

/**
 * Base interface for other authenticated detail interfaces to extend.
 * 
 * @author matt
 * @version 1.0
 * @since 3.4
 */
public interface SecurityActor {

	/**
	 * Return {@code true} if the actor authenticated via a token.
	 * 
	 * @return boolean
	 */
	boolean isAuthenticatedWithToken();

	/**
	 * Get the token.
	 * 
	 * @return the token, or {@literal null} if not authenticated with a token
	 */
	SecurityToken getSecurityToken();

	/**
	 * Get the current active authentication.
	 * 
	 * @return the active Authentication, or {@literal null} if none available
	 */
	static Authentication getCurrentAuthentication() {
		return SecurityContextHolder.getContext().getAuthentication();
	}

	/**
	 * Get the current {@link SecurityActor}.
	 * 
	 * @return the current actor, never {@literal null}
	 * @throws SecurityException
	 *         if the actor is not available
	 */
	static SecurityActor getCurrentActor() throws SecurityException {
		Authentication auth = getCurrentAuthentication();
		if ( auth != null && auth.getPrincipal() instanceof SecurityActor ) {
			return (SecurityActor) auth.getPrincipal();
		} else if ( auth != null && auth.getDetails() instanceof SecurityActor ) {
			return (SecurityActor) auth.getDetails();
		}
		throw new SecurityException("Actor not available");
	}

}
