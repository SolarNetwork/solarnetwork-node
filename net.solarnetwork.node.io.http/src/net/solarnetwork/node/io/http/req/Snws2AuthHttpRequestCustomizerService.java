/* ==================================================================
 * Snws2AuthHttpRequestCustomizerService.java - 5/08/2024 4:52:04 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpRequest;
import net.solarnetwork.security.Snws2AuthorizationBuilder;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.ByteList;
import net.solarnetwork.util.ObjectUtils;
import net.solarnetwork.web.security.AuthorizationCredentialsProvider;
import net.solarnetwork.web.support.AuthorizationV2RequestInterceptor;

/**
 * HTTP request customizer for SNWS2 token authorization.
 *
 * @author matt
 * @version 1.0
 * @since 1.2
 */
public class Snws2AuthHttpRequestCustomizerService extends BaseHttpRequestCustomizerService {

	private final Clock clock;
	private String token;
	private String tokenSecret;

	/**
	 * Constructor.
	 *
	 * <p>
	 * The system clock will be used.
	 * </p>
	 */
	public Snws2AuthHttpRequestCustomizerService() {
		this(Clock.systemUTC());
	}

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the clock to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public Snws2AuthHttpRequestCustomizerService(Clock clock) {
		super();
		this.clock = ObjectUtils.requireNonNullArgument(clock, "clock");
		setGroupUid(AUTHORIZATION_GROUP_UID);
	}

	@Override
	public void configurationChanged(Map<String, Object> properties) {
		// nothing
	}

	@Override
	public HttpRequest customize(HttpRequest request, ByteList body, Map<String, ?> parameters) {
		final String token = this.token;
		final String tokenSecret = this.tokenSecret;
		if ( !(request == null || token == null || token.isEmpty() || tokenSecret == null
				|| tokenSecret.isEmpty()) ) {

			// we override a few things below so we can use the configured Clock
			final Instant now = clock.instant();
			final AuthorizationV2RequestInterceptor interceptor = new AuthorizationV2RequestInterceptor(
					new AuthorizationCredentialsProvider() {

						@Override
						public String getAuthorizationId() {
							return token;
						}

						@Override
						public String getAuthorizationSecret() {
							return tokenSecret;
						}

						@Override
						public byte[] getAuthorizationSigningKey() {
							return new Snws2AuthorizationBuilder(token).computeSigningKey(now,
									tokenSecret);
						}

						@Override
						public Instant getAuthorizationSigningDate() {
							return now;
						}

					});

			try {
				interceptor.intercept(request, body != null ? body.toArrayValue() : null, (req, bod) -> {
					// we only want to modify the request, not execute it here
					return null;
				});
			} catch ( IOException e ) {
				throw new RuntimeException(
						String.format("Error computing SNWS2 Authorization header on request [%s]: %s",
								request.getURI(), e.getMessage()),
						e);
			}
		}
		return request;
	}

	@Override
	public String getSettingUid() {
		return "net.s10k.http.customizer.auth.snws2";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = basicIdentifiableSettings("", "", AUTHORIZATION_GROUP_UID);
		result.add(new BasicTextFieldSettingSpecifier("token", null));
		result.add(new BasicTextFieldSettingSpecifier("tokenSecret", null, true));
		return result;
	}

	/**
	 * Set the SolarNetwork token to use.
	 *
	 * @param token
	 *        the token to set
	 */
	public void setToken(String token) {
		this.token = token;
	}

	/**
	 * Set the SolarNetwork token secret to use.
	 *
	 * @param tokenSecret
	 *        the token secret to set
	 */
	public void setTokenSecret(String tokenSecret) {
		this.tokenSecret = tokenSecret;
	}

}
