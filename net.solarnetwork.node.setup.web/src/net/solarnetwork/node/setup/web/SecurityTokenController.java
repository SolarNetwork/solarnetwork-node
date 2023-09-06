/* ==================================================================
 * SecurityTokenController.java - 7/09/2023 6:59:35 am
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

package net.solarnetwork.node.setup.web;

import static java.util.Collections.singletonMap;
import static net.solarnetwork.domain.Result.success;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.domain.Result;
import net.solarnetwork.node.domain.SecurityToken;
import net.solarnetwork.node.service.SecurityTokenService;
import net.solarnetwork.node.setup.web.support.SecurityTokenInfo;

/**
 * Controller to manage security tokens.
 * 
 * @author matt
 * @version 1.0
 */
@Controller
@RequestMapping("/a/security-tokens")
public class SecurityTokenController extends BaseSetupWebServiceController {

	private final SecurityTokenService securityTokenService;

	/**
	 * Constructor.
	 * 
	 * @param securityTokenService
	 *        the service to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SecurityTokenController(SecurityTokenService securityTokenService) {
		super();
		this.securityTokenService = requireNonNullArgument(securityTokenService, "securityTokenService");
	}

	/**
	 * Security Tokens UI.
	 * 
	 * @return the Security Tokens view name
	 */
	@GetMapping(value = { "", "/" })
	public ModelAndView securityTokensUi() {
		return new ModelAndView("sectoks",
				singletonMap("tokens", securityTokenService.getAvailableTokens()));
	}

	/**
	 * Create a new security token.
	 * 
	 * @param tokenInfo
	 *        the token info
	 * @return the newly created token ID and secret
	 */
	@PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, params = "!id")
	@ResponseBody
	public Result<KeyValuePair> createSecurityToken(SecurityTokenInfo tokenInfo) {
		SecurityToken input = null;
		if ( tokenInfo != null ) {
			input = SecurityToken.tokenDetails(tokenInfo.getName(), tokenInfo.getDescription());
		}
		return success(securityTokenService.createToken(input));
	}

	/**
	 * Update a security token.
	 * 
	 * @param tokenInfo
	 *        the token info
	 * @return the newly created token ID and secret
	 */
	@PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, params = "id")
	@ResponseBody
	public Result<Void> updateSecurityToken(SecurityTokenInfo tokenInfo) {
		SecurityToken input = null;
		if ( tokenInfo != null ) {
			input = SecurityToken.tokenDetails(tokenInfo.getId(), tokenInfo.getName(),
					tokenInfo.getDescription());
		}
		securityTokenService.updateToken(input);
		return success();
	}

	/**
	 * Delete a security token.
	 * 
	 * @param tokenId
	 *        the ID of the token to delete
	 * @return the result
	 */
	@DeleteMapping
	@ResponseBody
	public Result<Void> deleteSecurityToken(@RequestParam("tokenId") String tokenId) {
		securityTokenService.deleteToken(tokenId);
		return success();
	}

}
