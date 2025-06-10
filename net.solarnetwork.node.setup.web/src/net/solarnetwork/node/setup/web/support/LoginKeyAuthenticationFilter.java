/* ==================================================================
 * LoginKeyAuthenticationFilter.java - 30/10/2019 3:31:04 pm
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

package net.solarnetwork.node.setup.web.support;

import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authentication filter that expects a Base64 salted username and AES-encrypted
 * password be provided.
 * 
 * <p>
 * This filter is designed to work with the {@link LoginKey} data returned by
 * {@link LoginKeyHelper#generateKey(String, String)}. Once a {@link LoginKey}
 * has been obtained, then invoking a HTTP {@literal GET /pub/session/login}
 * will call {@link LoginKeyHelper#decrypt(String, String)} to obtain the login
 * credentials.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 1.41
 */
public class LoginKeyAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

	/** A username parameter. */
	public static final String USERNAME_PARAM = "username";

	/** A password parameter. */
	public static final String PASSWORD_PARAM = "password";

	private final LoginKeyHelper loginHelper;

	private String usernameParameter = USERNAME_PARAM;
	private String passwordParameter = PASSWORD_PARAM;

	/**
	 * Default constructor.
	 * 
	 * <p>
	 * Registers a request matcher for {@code GET} on
	 * {@literal /pub/session/login}.
	 * </p>
	 * 
	 * @param loginHelper
	 *        the helper
	 */
	public LoginKeyAuthenticationFilter(LoginKeyHelper loginHelper) {
		this(loginHelper, new AntPathRequestMatcher("/pub/session/login", "GET"));
	}

	/**
	 * Constructor.
	 * 
	 * @param loginHelper
	 *        the helper
	 * @param requiresAuthenticationRequestMatcher
	 *        the request matcher
	 */
	public LoginKeyAuthenticationFilter(LoginKeyHelper loginHelper,
			RequestMatcher requiresAuthenticationRequestMatcher) {
		super(requiresAuthenticationRequestMatcher);
		this.loginHelper = loginHelper;
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException, IOException, ServletException {
		String username = request.getParameter(usernameParameter);
		String encryptedPassword = request.getParameter(passwordParameter);

		if ( username == null ) {
			username = "";
		}

		if ( encryptedPassword == null ) {
			encryptedPassword = "";
		}

		username = username.trim();

		UsernamePasswordAuthenticationToken authRequest = loginHelper.decrypt(username,
				encryptedPassword);

		authRequest.setDetails(authenticationDetailsSource.buildDetails(request));

		return this.getAuthenticationManager().authenticate(authRequest);
	}

	/**
	 * Get the username request parameter name.
	 * 
	 * @return the username parameter name; defaults to {@link #USERNAME_PARAM}
	 */
	public String getUsernameParameter() {
		return usernameParameter;
	}

	/**
	 * Set the username parameter name.
	 * 
	 * @param usernameParameter
	 *        the username parameter name to set
	 */
	public void setUsernameParameter(String usernameParameter) {
		this.usernameParameter = usernameParameter;
	}

	/**
	 * Get the password parameter name.
	 * 
	 * @return the password parameter name; defaults to {@link #PASSWORD_PARAM}
	 */
	public String getPasswordParameter() {
		return passwordParameter;
	}

	/**
	 * Set the password parameter name.
	 * 
	 * @param passwordParameter
	 *        the passwordParameter to set
	 */
	public void setPasswordParameter(String passwordParameter) {
		this.passwordParameter = passwordParameter;
	}

}
