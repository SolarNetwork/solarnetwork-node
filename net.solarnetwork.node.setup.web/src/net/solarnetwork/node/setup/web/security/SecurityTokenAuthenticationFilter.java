/* ==================================================================
 * SecurityTokenAuthenticationFilter.java - 7/09/2023 1:56:35 pm
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

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.transaction.TransactionException;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.unit.DataSize;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.solarnetwork.node.domain.AuthenticatedToken;
import net.solarnetwork.web.jakarta.security.AuthenticationData;
import net.solarnetwork.web.jakarta.security.AuthenticationDataFactory;
import net.solarnetwork.web.jakarta.security.SecurityHttpServletRequestWrapper;
import net.solarnetwork.web.jakarta.security.SecurityTokenAuthenticationEntryPoint;

/**
 * Authentication filter for "SolarNetworkWS" style authentication.
 *
 * @author matt
 * @version 1.1
 * @since 3.3
 */
public class SecurityTokenAuthenticationFilter extends OncePerRequestFilter implements Filter {

	/** The fixed length of the auth token. */
	public static final int AUTH_TOKEN_LENGTH = 20;

	/** The default value for the {@code maxRequestBodySize} property. */
	public static final int DEFAULT_MAX_REQUEST_BODY_SIZE = 65535;

	private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new WebAuthenticationDetailsSource();
	private SecurityTokenAuthenticationEntryPoint authenticationEntryPoint;
	private UserDetailsService userDetailsService;
	private final SecurityTokenFilterSettings settings;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 */
	public SecurityTokenAuthenticationFilter() {
		this(null);
	}

	/**
	 * Constructor.
	 *
	 * @param settings,
	 *        or {@literal null} to create a default instance
	 */
	public SecurityTokenAuthenticationFilter(SecurityTokenFilterSettings settings) {
		super();
		this.settings = (settings != null ? settings : new SecurityTokenFilterSettings());
	}

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(userDetailsService, "A UserDetailsService is required");
		Assert.notNull(authenticationEntryPoint, "A SecurityTokenAuthenticationEntryPoint is required");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
			throws ServletException, IOException {
		SecurityHttpServletRequestWrapper request = new SecurityHttpServletRequestWrapper(req,
				(int) settings.getMaxRequestBodySize().toBytes(), true,
				(int) settings.getMinimumCompressLength().toBytes(),
				settings.getCompressibleContentTypePattern(),
				(int) settings.getMinimumSpoolLength().toBytes(), settings.getSpoolDirectory());
		HttpServletResponse response = res;

		// for multipart requests, force the InputStream to be resolved now so the parameters
		// are not parsed by the servlet container
		if ( req.getContentType() != null && MediaType.MULTIPART_FORM_DATA
				.isCompatibleWith(MimeType.valueOf(req.getContentType())) ) {
			request.getContentSHA256();
		}

		AuthenticationData data;
		try {
			data = AuthenticationDataFactory.authenticationDataForAuthorizationHeader(request);
		} catch ( net.solarnetwork.web.security.SecurityException e ) {
			deny(request, response, new MaxUploadSizeExceededException(
					(int) settings.getMaxRequestBodySize().toBytes(), e));
			return;
		} catch ( SecurityException e ) {
			deny(request, response, e);
			return;
		} catch ( AuthenticationException e ) {
			fail(request, response, e);
			return;
		}

		if ( data == null ) {
			log.trace("Missing Authorization header or unsupported scheme");
			chain.doFilter(request, response);
			return;
		}

		final UserDetails user;
		try {
			user = userDetailsService.loadUserByUsername(data.getAuthTokenId());
		} catch ( AuthenticationException e ) {
			log.debug("Auth token [{}] exception: {}", data.getAuthTokenId(), e.getMessage());
			fail(request, response, new BadCredentialsException("Bad credentials"));
			return;
		} catch ( DataAccessException | TransactionException e ) {
			log.debug("Auth token [{}] transient DAO exception: {}", data.getAuthTokenId(),
					e.getMessage());
			failDao(request, response, e);
			return;
		} catch ( Exception e ) {
			log.debug("Auth token [{}] exception: {}", data.getAuthTokenId(), e.getMessage());
			fail(request, response,
					new AuthenticationServiceException("Unable to verify credentials", e));
			return;
		}

		if ( !(user instanceof AuthenticatedToken) ) {
			fail(request, response, new BadCredentialsException("Access denied"));
			return;

		}

		AuthenticatedToken tokenUser = (AuthenticatedToken) user;
		final String computedDigest = tokenUser.getSecurityToken()
				.applySecret(data::computeSignatureDigest);
		if ( !computedDigest.equals(data.getSignatureDigest()) ) {
			log.debug("Expected response: [{}] but received: [{}]", computedDigest,
					data.getSignatureDigest());
			fail(request, response, new BadCredentialsException("Bad credentials"));
			return;
		}

		if ( !data.isDateValid(settings.getMaxDateSkew()) ) {
			log.debug("Request date [{}] diff too large: {}", data.getDate(), data.getDateSkew());
			fail(request, response, new BadCredentialsException("Date skew too large"));
			return;
		}

		log.debug("Authentication success for user: [{}]", user.getUsername());

		SecurityContextHolder.getContext()
				.setAuthentication(createSuccessfulAuthentication(request, tokenUser));

		chain.doFilter(request, response);
	}

	private Authentication createSuccessfulAuthentication(HttpServletRequest request,
			AuthenticatedToken user) {
		// create copy of token without secret
		AuthenticatedToken authUser = new AuthenticatedToken(
				user.getSecurityToken().copyWithoutSecret(null, null), user.getAuthorities());
		UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(
				authUser, null, user.getAuthorities());
		authRequest.setDetails(authenticationDetailsSource.buildDetails(request));
		return authRequest;
	}

	private void fail(SecurityHttpServletRequestWrapper request, HttpServletResponse response,
			AuthenticationException failed) throws IOException, ServletException {
		SecurityContextHolder.getContext().setAuthentication(null);
		request.deleteCachedContent();
		authenticationEntryPoint.commence(request, response, failed);
	}

	private void deny(SecurityHttpServletRequestWrapper request, HttpServletResponse response,
			Exception e) throws IOException, ServletException {
		SecurityContextHolder.getContext().setAuthentication(null);
		request.deleteCachedContent();
		String msg = e.getMessage();
		if ( msg == null ) {
			msg = "Access denied.";
		}
		authenticationEntryPoint.handle(request, response, new AccessDeniedException(msg, e));
	}

	private void failDao(SecurityHttpServletRequestWrapper request, HttpServletResponse response,
			Exception failed) throws IOException, ServletException {
		SecurityContextHolder.getContext().setAuthentication(null);
		request.deleteCachedContent();
		authenticationEntryPoint.handleTransientResourceException(request, response, failed);
	}

	/**
	 * Set the details service, which must return users with valid SolarNetwork
	 * usernames (email addresses) and plain-text authorization token secret
	 * passwords via {@link UserDetails#getUsername()} and
	 * {@link UserDetails#getPassword()}.
	 *
	 * <p>
	 * After validating the request authorization, this filter will authenticate
	 * the user with Spring Security.
	 * </p>
	 *
	 * @param userDetailsService
	 *        the service
	 */
	public void setUserDetailsService(UserDetailsService userDetailsService) {
		this.userDetailsService = userDetailsService;
	}

	/**
	 * Set the details source to use.
	 *
	 * <p>
	 * Defaults to a {@link WebAuthenticationDetailsSource}.
	 * </p>
	 *
	 * @param authenticationDetailsSource
	 *        the source to use
	 */
	public void setAuthenticationDetailsSource(
			AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource) {
		this.authenticationDetailsSource = authenticationDetailsSource;
	}

	/**
	 * Set the maximum amount of difference in the supplied HTTP {@code Date}
	 * (or {@code X-SN-Date}) header value with the current time as reported by
	 * the system.
	 *
	 * <p>
	 * If this difference is exceeded, authorization fails.
	 * </p>
	 *
	 * @param maxDateSkew
	 *        the maximum allowed date skew
	 */
	public void setMaxDateSkew(long maxDateSkew) {
		this.settings.setMaxDateSkew(maxDateSkew);
	}

	/**
	 * The {@link SecurityTokenAuthenticationEntryPoint} to use as the entry
	 * point.
	 *
	 * @param entryPoint
	 *        the entry point to use
	 */
	public void setAuthenticationEntryPoint(SecurityTokenAuthenticationEntryPoint entryPoint) {
		this.authenticationEntryPoint = entryPoint;
	}

	/**
	 * Set the maximum allowed request body size.
	 *
	 * @param maxRequestBodySize
	 *        the maximum request body size allowed
	 */
	public void setMaxRequestBodySize(int maxRequestBodySize) {
		this.settings.setMaxRequestBodySize(DataSize.ofBytes(maxRequestBodySize));
	}

	/**
	 * Get the filter settings.
	 *
	 * @return the settings, never {@literal null}
	 */
	public SecurityTokenFilterSettings getSettings() {
		return settings;
	}

}
