/* ==================================================================
 * LoginController.java - 30/10/2019 9:34:59 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.node.setup.web.support.LoginKey;
import net.solarnetwork.node.setup.web.support.LoginKeyHelper;
import net.solarnetwork.web.jakarta.domain.Response;

/**
 * Controller to support alternative login methods, designed for integration
 * with external management applications.
 * 
 * <p>
 * External login works by first calling the {@literal GET /pub/session/key}
 * endpoint with {@code username} and {@code salt} parameters to obtain a login
 * key. The {@code salt} parameter must be a Base64-encoded value of exactly 12
 * bytes. Using the returned key data, the client can then call the
 * {@literal GET /pub/session/login} endpoint, passing {@code username} and
 * {@code password} parameters. The {@code username} value is a Base64-encoded
 * combination of the {@code salt} and UTF-8 bytes of the original
 * {@code username} passed to {@literal /pub/session/key}. The {@code password}
 * parameter is an Base64-encoded AES-CBC-PKCS7Padding encrypted password value.
 * The AES encryption key and CBC initialization vector are returned from the
 * original {@literal /pub/session/key} call.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 1.41
 */
@RestController
@RequestMapping("/pub/session")
public class LoginController extends BaseSetupWebServiceController {

	private final LoginKeyHelper helper;

	/**
	 * Constructor.
	 * 
	 * @param helper
	 *        the helper to use
	 */
	@Autowired
	public LoginController(LoginKeyHelper helper) {
		super();
		this.helper = helper;
	}

	/**
	 * Generate a login key value to be used for later calling the
	 * {@literal GET /pub/session/login} endpoint.
	 * 
	 * @param username
	 *        the username to generate the key for
	 * @param salt
	 *        a salt value, which should be random bytes
	 * @return the key value
	 * @see LoginKeyHelper#generateKey(String, String)
	 */
	@RequestMapping(value = "/key", method = RequestMethod.GET)
	public Response<LoginKey> generateKey(@RequestParam("username") String username,
			@RequestParam("salt") String salt) {
		return Response.response(helper.generateKey(username, salt));
	}

}
