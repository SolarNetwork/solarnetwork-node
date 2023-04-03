/* ==================================================================
 * BasicAuthHttpRequestCustomizerService.java - 3/04/2023 2:31:36 pm
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

package net.solarnetwork.node.io.http.req;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpRequest;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.ByteList;

/**
 * HTTP request customizer for basic authorization.
 * 
 * <p>
 * The {@link #configurationChanged(Map)} must be invoked after configuring the
 * credentials on this class because the encoded HTTP Authorization header value
 * will be computed then and cached.
 * </p>
 * 
 * <p>
 * Alternatively the credentials can be provided dynamically by passing the
 * {@link #USERNAME_PARAM} and {@link #PASSWORD_PARAM} parameters.
 * </p>
 * 
 * <p>
 * Placeholders are supported in the username/password fields as well.
 * </p>
 * 
 * <p>
 * The credentials are encoded using the UTF-8 character encoding.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class BasicAuthHttpRequestCustomizerService extends BaseHttpRequestCustomizerService {

	/** A parameter key for the username to use. */
	public static final String USERNAME_PARAM = "username";

	/** A parameter key for the password to use. */
	public static final String PASSWORD_PARAM = "password";

	private String username;
	private String password;

	private String auth;

	/**
	 * Constructor.
	 */
	public BasicAuthHttpRequestCustomizerService() {
		super();
		setGroupUid(AUTHORIZATION_GROUP_UID);
	}

	@Override
	public void configurationChanged(Map<String, Object> properties) {
		// cache the authorization value
		auth = encodeAuth(username, password);
	}

	@Override
	public HttpRequest customize(HttpRequest request, ByteList body, Map<String, ?> parameters) {
		String auth = auth(parameters);
		if ( auth == null ) {
			return request;
		}
		request.getHeaders().setBasicAuth(auth);
		return request;
	}

	private String auth(Map<String, ?> parameters) {
		if ( parameters != null && parameters.containsKey(USERNAME_PARAM)
				&& parameters.containsKey(PASSWORD_PARAM) ) {
			String auth = encodeAuth(parameters.get(USERNAME_PARAM).toString(),
					parameters.get(PASSWORD_PARAM).toString());
			if ( auth != null ) {
				return auth;
			}
		} else if ( hasPlaceholder(username) || hasPlaceholder(password) ) {
			String auth = encodeAuth(resolvePlaceholders(username), resolvePlaceholders(password));
			if ( auth != null ) {
				return auth;
			}
		}
		return this.auth;
	}

	private String encodeAuth(String username, String password) {
		if ( username == null || username.trim().isEmpty() || password == null
				|| password.trim().isEmpty() ) {
			return null;
		}
		byte[] utf8 = username.concat(":").concat(password).getBytes(StandardCharsets.UTF_8);
		return Base64.getEncoder().encodeToString(utf8);
	}

	@Override
	public String getSettingUid() {
		return "net.s10k.http.customizer.auth.basic";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = basicIdentifiableSettings("", "", AUTHORIZATION_GROUP_UID);
		result.add(new BasicTextFieldSettingSpecifier("username", null));
		result.add(new BasicTextFieldSettingSpecifier("password", null, true));
		return result;
	}

	/**
	 * Get the username.
	 * 
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Set the username.
	 * 
	 * @param username
	 *        the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Get the password.
	 * 
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Set the password.
	 * 
	 * @param password
	 *        the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

}
