/* ==================================================================
 * UserMetadataService.java - 7/05/2021 12:48:01 PM
 *
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.service.support;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.NetworkIdentity;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.node.domain.NodeAppConfiguration;
import net.solarnetwork.node.service.MetadataService;
import net.solarnetwork.node.setup.SetupService;
import net.solarnetwork.security.Snws2AuthorizationBuilder;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.CachedResult;

/**
 * Implementation of {@link MetadataService} that uses SolarQuery to find
 * user-level metadata.
 *
 * @author matt
 * @version 1.1
 */
public class UserMetadataService extends JsonHttpClientSupport
		implements MetadataService, SettingSpecifierProvider, SettingsChangeObserver {

	/** The the user metadata path. */
	public static final String USER_METADATA_PATH = "/api/v1/sec/users/meta";

	/** The {@code cacheSeconds} property default value. */
	public static final int DEFAULT_CACHE_SECONDS = 3600;

	private final OptionalService<SetupService> setupService;
	private int cacheSeconds = DEFAULT_CACHE_SECONDS;
	private String token = null;
	private String tokenSecret = null;

	private CachedResult<GeneralDatumMetadata> cachedMetadata;

	/**
	 * Constructor.
	 *
	 * @param setupService
	 *        the setup service
	 */
	public UserMetadataService(OptionalService<SetupService> setupService) {
		super();
		this.setupService = setupService;
	}

	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		cachedMetadata = null;
	}

	private String solarQueryUrl() {
		SetupService s = OptionalService.service(setupService);
		NodeAppConfiguration cfg = (s != null ? s.getAppConfiguration() : null);
		Map<String, String> urls = (cfg != null ? cfg.getNetworkServiceUrls() : null);
		return (urls != null ? urls.get(NetworkIdentity.SOLARQUERY_NETWORK_SERVICE_KEY) : null);
	}

	@Override
	public synchronized GeneralDatumMetadata getAllMetadata() {
		if ( cachedMetadata != null && cachedMetadata.isValid() ) {
			return cachedMetadata.getResult();
		}
		// fetch now
		final String solarQueryUrl = solarQueryUrl();
		final String token = this.token;
		final String secret = this.tokenSecret;
		if ( solarQueryUrl == null || solarQueryUrl.isEmpty() ) {
			// not set up yet?
			return null;
		}
		final String url = solarQueryUrl + USER_METADATA_PATH;
		try {
			final InputStream in = jsonGET(url, conn -> {
				if ( token != null && !token.isEmpty() && secret != null && !secret.isEmpty() ) {
					Instant now = Instant.now();
					Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(token)
							.saveSigningKey(secret);
					setupTokenAuthorization(conn, auth, now, null);
				}
			});
			GeneralDatumMetadata meta = extractResponseData(in, GeneralDatumMetadata.class);
			if ( cacheSeconds > 0 ) {
				this.cachedMetadata = new CachedResult<GeneralDatumMetadata>(meta, cacheSeconds,
						TimeUnit.SECONDS);
			}
			return meta;
		} catch ( IOException e ) {
			if ( log.isTraceEnabled() ) {
				log.trace("IOException querying for user metadata at {}: {}", url, e.toString());
			} else {
				log.warn("Unable to get user metadata: {}", e.getMessage());
			}
			return null;
		}
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.metadata.user";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		getAllMetadata();
		List<SettingSpecifier> result = baseIdentifiableSettings("");
		result.add(0, new BasicTitleSettingSpecifier("status", getStatusMessage(), true));
		result.add(new BasicTextFieldSettingSpecifier("cacheSeconds",
				String.valueOf(DEFAULT_CACHE_SECONDS)));
		result.add(new BasicTextFieldSettingSpecifier("token", null));
		result.add(new BasicTextFieldSettingSpecifier("tokenSecret", null, true));
		return result;
	}

	private String getStatusMessage() {
		final CachedResult<GeneralDatumMetadata> cached = this.cachedMetadata;
		final MessageSource msgSource = getMessageSource();
		if ( cached == null ) {
			return msgSource.getMessage("status.noneCached", null, Locale.getDefault());
		}
		GeneralDatumMetadata meta = cached.getResult();
		if ( meta == null ) {
			return msgSource.getMessage("status.none",
					new Object[] { new Date(cached.getCreated()), new Date(cached.getExpires()) },
					Locale.getDefault());
		}
		Map<String, Object> info = meta.getInfo();
		Map<String, Map<String, Object>> propInfo = meta.getPropertyInfo();
		return msgSource.getMessage("status.msg",
				new Object[] { new Date(cached.getCreated()), new Date(cached.getExpires()),
						info != null ? info.size() : 0, propInfo != null ? propInfo.size() : 0 },
				Locale.getDefault());
	}

	/**
	 * Set the number of seconds to cache metadata.
	 *
	 * @param cacheSeconds
	 *        the maximum number of seconds to cache metadata for, or anything
	 *        less than {@literal 1} to disable
	 */
	public void setCacheSeconds(int cacheSeconds) {
		this.cacheSeconds = cacheSeconds;
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
