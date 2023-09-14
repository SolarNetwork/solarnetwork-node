/* ==================================================================
 * SecurityTokenDaoUserDetailsService.java - 7/09/2023 1:23:25 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.web.security;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import net.solarnetwork.node.domain.AuthenticatedToken;
import net.solarnetwork.node.domain.SecurityToken;
import net.solarnetwork.node.service.SecurityTokenService;

/**
 * {@link UserDetailsService} that delegates to a {@link SecurityTokenService}.
 * 
 * @author matt
 * @version 1.0
 * @since 3.3
 */
public class SecurityTokenServiceUserDetailsService implements UserDetailsService {

	private final SecurityTokenService securityTokenService;

	/**
	 * Constructor.
	 * 
	 * @param securityTokenService
	 *        the security token service to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SecurityTokenServiceUserDetailsService(SecurityTokenService securityTokenService) {
		super();
		this.securityTokenService = requireNonNullArgument(securityTokenService, "securityTokenService");
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		SecurityToken token = securityTokenService.tokenForId(username);
		if ( token == null ) {
			throw new UsernameNotFoundException("Unknown token ID.");
		}
		return new AuthenticatedToken(token, "USER");
	}

}
