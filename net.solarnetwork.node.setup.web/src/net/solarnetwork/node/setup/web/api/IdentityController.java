/* ==================================================================
 * IdentityController.java - 7/09/2023 12:53:08 pm
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

package net.solarnetwork.node.setup.web.api;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import net.solarnetwork.domain.Result;
import net.solarnetwork.node.domain.SecurityActor;
import net.solarnetwork.node.domain.SecurityToken;
import net.solarnetwork.node.setup.web.support.GlobalExceptionRestController;
import net.solarnetwork.web.domain.Response;

/**
 * API controller for identity functions.
 * 
 * @author matt
 * @version 1.0
 * @since 3.3
 */
@GlobalExceptionRestController
@RequestMapping("/api/v1/sec")
public class IdentityController {

	/**
	 * Constructor.
	 */
	public IdentityController() {
		super();
	}

	/**
	 * Check who the caller is.
	 * 
	 * <p>
	 * This is a convenient way to verify the credentials of a user.
	 * </p>
	 * 
	 * @param principal
	 *        the user principal
	 * @return a response that details who the authenticated caller is
	 */
	@GetMapping("/whoami")
	public Result<Map<String, ?>> whoAmI(Principal principal) {
		SecurityActor actor = SecurityActor.getCurrentActor();
		SecurityToken token = actor.getSecurityToken();
		Map<String, Object> data = new LinkedHashMap<>(3);
		if ( token != null ) {
			data.put("token", token.getId());
		}
		return Response.response(data);
	}

}
