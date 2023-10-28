/* ==================================================================
 * AuthenticatedToken.java - Mar 22, 2013 3:23:20 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

import static net.solarnetwork.util.ObjectUtils.requireNonEmptyArgument;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * {@link SecurityActor} implementation for authenticated tokens.
 * 
 * @author matt
 * @version 1.0
 * @since 3.4
 */
public class AuthenticatedToken extends User implements SecurityActor {

	private static final long serialVersionUID = -3282529010625928648L;

	/** The token. */
	private final SecurityToken token;

	/**
	 * Construct with values.
	 * 
	 * @param token
	 *        the token
	 * @param roles
	 *        the granted roles
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null} or empty
	 */
	public AuthenticatedToken(SecurityToken token, String... roles) {
		super(requireNonNullArgument(token, "token").getId(), "", true, true, true, true, roles(roles));
		this.token = token;
	}

	/**
	 * Construct with values.
	 * 
	 * @param token
	 *        the token
	 * @param authorities
	 *        the granted authorities
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public AuthenticatedToken(SecurityToken token, Collection<? extends GrantedAuthority> authorities) {
		super(requireNonNullArgument(token, "token").getId(), "", true, true, true, true,
				requireNonNullArgument(authorities, "authorities"));
		this.token = token;
	}

	private static List<GrantedAuthority> roles(String... roles) {
		requireNonEmptyArgument(roles, "roles");
		List<GrantedAuthority> authorities = new ArrayList<>(roles.length);
		for ( String role : roles ) {
			authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
		}
		return authorities;

	}

	@Override
	public boolean isAuthenticatedWithToken() {
		return true;
	}

	@Override
	public SecurityToken getSecurityToken() {
		return token;
	}

}
