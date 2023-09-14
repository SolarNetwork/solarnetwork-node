/* ==================================================================
 * SecurityTokenService.java - 7/09/2023 6:04:43 am
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

package net.solarnetwork.node.service;

import java.util.Collection;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.node.domain.SecurityToken;

/**
 * Service API for {@link SecurityToken} management.
 * 
 * @author matt
 * @version 1.0
 * @since 3.4
 */
public interface SecurityTokenService {

	/**
	 * Get all available security tokens.
	 * 
	 * <p>
	 * The token secrets are not returned by this method.
	 * </p>
	 * 
	 * @return the tokens, never {@literal null}
	 */
	Collection<SecurityToken> getAvailableTokens();

	/**
	 * Get a security token for a given ID.
	 * 
	 * @param tokenId
	 *        the token ID
	 * @return the token, or {@literal null} if not found
	 */
	SecurityToken tokenForId(String tokenId);

	/**
	 * Create a new token.
	 * 
	 * <p>
	 * This method will generate a new security token and return the generated
	 * token ID and secret.
	 * </p>
	 * 
	 * @param details
	 *        the optional details (name, description) to use
	 * @return the token ID and token secret
	 */
	KeyValuePair createToken(SecurityToken details);

	/**
	 * Update the modifiable values of a security token.
	 * 
	 * <p>
	 * The token ID and secret cannot be changed by this method, but properties
	 * like {@code name} and {@code description} can.
	 * </p>
	 * 
	 * @param token
	 *        the token details to update
	 */
	void updateToken(SecurityToken token);

	/**
	 * Delete a security token.
	 * 
	 * <p>
	 * No error is thrown if the given {@code tokenId} does not exist.
	 * </p>
	 * 
	 * @param tokenId
	 *        the ID of the token to delete
	 */
	void deleteToken(String tokenId);

}
