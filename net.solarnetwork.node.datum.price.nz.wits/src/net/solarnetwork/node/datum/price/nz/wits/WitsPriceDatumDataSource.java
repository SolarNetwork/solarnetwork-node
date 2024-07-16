/* ==================================================================
 * WitsPriceDatumDataSource.java - 16/07/2024 6:17:32 am
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

package net.solarnetwork.node.datum.price.nz.wits;

import static java.lang.String.format;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.HttpAsyncClient;
import org.springframework.context.MessageSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.github.scribejava.httpclient.apache.ApacheHttpClient;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.PriceDatum;
import net.solarnetwork.node.domain.datum.SimplePriceDatum;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.RemoteServiceException;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.CachedResult;
import net.solarnetwork.util.ObjectUtils;

/**
 * Datum data source for New Zealand energy market prices through the
 * Electricity Authority WITS API.
 *
 * @author matt
 * @version 1.0
 */
public class WitsPriceDatumDataSource extends DatumDataSourceSupport implements DatumDataSource,
		SettingSpecifierProvider, SettingsChangeObserver, ServiceLifecycleObserver {

	/** The {@code marketType} property default value. */
	public static final MarketType DEFAULT_MARKET_TYPE = MarketType.Energy;

	/** The {@code fromOffset} property default value. */
	public static final Duration DEFAULT_FROM_OFFSET = Duration.ofMinutes(2);

	/** The {@code tokenUrl} property default value. */
	public static final String DEFAULT_TOKEN_URL = "https://api.electricityinfo.co.nz/login/oauth2/token";

	public static final String DEFAULT_PRICE_URL = "https://api.electricityinfo.co.nz/api/market-prices/v1/schedules/RTD/prices";

	private final Clock clock;
	private final ObjectMapper objectMapper;
	private final OptionalService<HttpAsyncClient> httpClient;

	private String clientId;
	private String clientSecret;
	private MarketType marketType = DEFAULT_MARKET_TYPE;
	private String node;
	private Duration fromOffset = DEFAULT_FROM_OFFSET;
	private String tokenUrl = DEFAULT_TOKEN_URL;
	private String priceUrl = DEFAULT_PRICE_URL;

	private CachedResult<PriceDatum> latestDatum;
	private OAuth20Service oauth;
	private OAuth2AccessToken accessToken;

	/**
	 * Constructor.
	 *
	 * <p>
	 * The default system clock and a default {@link ObjectMapper} will be used.
	 * </p>
	 */
	public WitsPriceDatumDataSource() {
		this(JsonUtils.newObjectMapper(), new StaticOptionalService<>(HttpAsyncClients.createSystem()));
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * The default system clock will be used.
	 * </p>
	 *
	 * @param objectMapper
	 *        the object mapper to use
	 * @param httpClient
	 *        the HTTP client to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public WitsPriceDatumDataSource(ObjectMapper objectMapper,
			OptionalService<HttpAsyncClient> httpClient) {
		this(Clock.systemUTC(), objectMapper, httpClient);
	}

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the clock to use
	 * @param objectMapper
	 *        the object mapper to use
	 * @param httpClient
	 *        the HTTP client to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public WitsPriceDatumDataSource(Clock clock, ObjectMapper objectMapper,
			OptionalService<HttpAsyncClient> httpClient) {
		super();
		this.clock = ObjectUtils.requireNonNullArgument(clock, "clock");
		this.objectMapper = ObjectUtils.requireNonNullArgument(objectMapper, "objectMapper");
		this.httpClient = ObjectUtils.requireNonNullArgument(httpClient, "httpClient");
	}

	@Override
	public void serviceDidStartup() {
		setupOAuth();
	}

	@Override
	public void serviceDidShutdown() {
		// nothing to do
	}

	@Override
	public void configurationChanged(Map<String, Object> properties) {
		setupOAuth();
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return PriceDatum.class;
	}

	@Override
	public PriceDatum readCurrentDatum() {
		final CachedResult<PriceDatum> cached = this.latestDatum;
		if ( cached != null && cached.isValid() ) {
			return cached.getResult();
		}
		try {
			final JsonNode json = requestData();
			if ( json != null ) {
				final JsonNode prices = json.path("prices");
				if ( prices.isArray() && !prices.isEmpty() ) {
					MarketPrice data = objectMapper.treeToValue(prices.get(0), MarketPrice.class);
					if ( data != null ) {
						SimplePriceDatum d = datum(data);
						CachedResult<PriceDatum> cd = new CachedResult<>(d, fromOffset.getSeconds(),
								TimeUnit.SECONDS);
						this.latestDatum = cd;
						return d;
					}
				}
			}
		} catch ( IOException e ) {
			log.warn("Communication problem requesting [{}] price data from [{}]: {}", node, tokenUrl,
					e.getMessage());
		}
		return null;
	}

	private synchronized void setupOAuth() {
		if ( clientId == null || clientId.isEmpty() || clientSecret == null || clientSecret.isEmpty() ) {
			oauth = null;
			return;
		}

		final ServiceBuilder builder = new ServiceBuilder(clientId).apiSecret(clientSecret);

		final HttpAsyncClient client = OptionalService.service(httpClient);
		if ( client instanceof CloseableHttpAsyncClient ) {
			builder.httpClient(new ApacheHttpClient((CloseableHttpAsyncClient) client));
		} else if ( client != null ) {
			log.warn("HttpAsyncClient [{}] is not a CloseableHttpAsyncClient; ignoring.", client);
		}
		oauth = builder.build(new DefaultApi20() {

			@Override
			public String getAccessTokenEndpoint() {
				return tokenUrl;
			}

			@Override
			protected String getAuthorizationBaseUrl() {
				throw new UnsupportedOperationException("Authorization base URL not supported.");
			}
		});
	}

	private synchronized OAuth2AccessToken acquireAccessToken() {
		final OAuth20Service service = this.oauth;
		if ( service == null ) {
			return accessToken;
		}
		try {
			accessToken = service.getAccessTokenClientCredentialsGrant();
			return accessToken;
		} catch ( Exception e ) {
			log.error("Error obtaining OAuth access token from [{}]: {}", tokenUrl, e.toString());
		}
		return accessToken;
	}

	private JsonNode requestData() throws IOException {
		final String node = getNode();
		if ( node == null || node.isEmpty() ) {
			return null;
		}
		final OAuth20Service service = this.oauth;
		if ( service == null ) {
			return null;
		}
		OAuth2AccessToken token = this.accessToken;
		if ( token == null /* TODO || expired */ ) {
			token = acquireAccessToken();
			if ( token == null ) {
				return null;
			}
		}
		final MarketType type = getMarketType();
		final Duration from = getFromOffset();
		final OAuthRequest request = new OAuthRequest(Verb.GET, priceUrl);
		request.addQuerystringParameter("marketType", type.getKey());
		request.addQuerystringParameter("nodes", node);
		request.addQuerystringParameter("from", clock.instant().minus(from).toString());
		service.signRequest(accessToken, request);
		try {
			final Response response = service.execute(request);
			return objectMapper.readTree(response.getBody());
		} catch ( InterruptedException e ) {
			log.warn("Interrupted requesting price data from [{}]", priceUrl);
			return null;
		} catch ( ExecutionException e ) {
			throw new RemoteServiceException(format("Error requesting price data from [%s]: %s",
					priceUrl, e.getCause().getMessage()), e.getCause());
		}
	}

	private SimplePriceDatum datum(MarketPrice price) {
		SimplePriceDatum d = new SimplePriceDatum(null, null, price.getTradingDateTime().toInstant(),
				new DatumSamples());
		d.setPrice(price.getPrice());
		return d;
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.price.nz.wits";
	}

	@Override
	public String getDisplayName() {
		return "New Zealand energy market price lookup";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>();
		results.add(new BasicTitleSettingSpecifier("status", statusMessage(), true));
		results.addAll(getIdentifiableSettingSpecifiers());
		results.add(new BasicTextFieldSettingSpecifier("clientId", null));
		results.add(new BasicTextFieldSettingSpecifier("clientSecret", null, true));

		// drop-down menu for marketTypeKey
		final MessageSource msg = getMessageSource();
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				"marketTypeKey", DEFAULT_MARKET_TYPE.getKey());
		Map<String, String> propTypeTitles = new LinkedHashMap<>(2);
		for ( MarketType e : MarketType.values() ) {
			String title = e.name();
			if ( msg != null ) {
				title = msg.getMessage("MarketType." + e.name(), null, e.name(), Locale.getDefault());
			}
			propTypeTitles.put(e.getKey(), title);
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		results.add(propTypeSpec);

		results.add(new BasicTextFieldSettingSpecifier("node", null));
		results.add(new BasicTextFieldSettingSpecifier("fromOffsetSeconds",
				String.valueOf(DEFAULT_FROM_OFFSET.getSeconds())));

		return results;
	}

	private String statusMessage() {
		final CachedResult<PriceDatum> cached = this.latestDatum;
		final MessageSource msg = getMessageSource();
		if ( cached != null ) {
			if ( msg != null ) {
				return msg.getMessage(
						"status.msg", new Object[] { cached.getResult().getPrice(), cached.getResult()
								.getTimestamp().atZone(MarketPrice.DEFAULT_TIME_ZONE) },
						Locale.getDefault());
			}
		}
		return msg != null ? msg.getMessage("status.na", null, "N/A", Locale.getDefault()) : "N/A";
	}

	/**
	 * Get the OAuth client credentials ID.
	 *
	 * @return the clientId
	 */
	public final String getClientId() {
		return clientId;
	}

	/**
	 * Set the OAuth client credentials ID.
	 *
	 * @param clientId
	 *        the ID to set
	 */
	public final void setClientId(String clientId) {
		this.clientId = clientId;
	}

	/**
	 * Set the OAuth client credentials secret.
	 *
	 * @return the secret
	 */
	public final String getClientSecret() {
		return clientSecret;
	}

	/**
	 * Get the OAuth client credentials secret.
	 *
	 * @param clientSecret
	 *        the secret to set
	 */
	public final void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	/**
	 * Get the market type.
	 *
	 * @return the type
	 */
	public final MarketType getMarketType() {
		return marketType;
	}

	/**
	 * Set the market type.
	 *
	 * @param marketType
	 *        the type to set; if {@literal null} then
	 *        {@link #DEFAULT_MARKET_TYPE} will be used
	 */
	public final void setMarketType(MarketType marketType) {
		this.marketType = (marketType != null ? marketType : DEFAULT_MARKET_TYPE);
	}

	/**
	 * Get the market type as a key value.
	 *
	 * @return the type key
	 */
	public final String getMarketTypeKey() {
		return marketType.getKey();
	}

	/**
	 * Set the market type as a key value.
	 *
	 * @param value
	 *        the type to set; if {@literal null} then
	 *        {@link #DEFAULT_MARKET_TYPE} will be used
	 */
	public final void setMarketTypeKey(String value) {
		MarketType type = null;
		try {
			type = MarketType.forKey(value);
		} catch ( IllegalArgumentException e ) {
			// ignore
		}
		setMarketType(type);
	}

	/**
	 * Get the grid node to collect pricing for.
	 *
	 * @return the node
	 */
	public final String getNode() {
		return node;
	}

	/**
	 * Set the grid node to collect pricing for.
	 *
	 * @param node
	 *        the node to set
	 */
	public final void setNode(String node) {
		this.node = node;
	}

	/**
	 * Get the from offset.
	 *
	 * @return the offset; defaults to {@link #DEFAULT_FROM_OFFSET}
	 */
	public final Duration getFromOffset() {
		return fromOffset;
	}

	/**
	 * Set the "from" offset.
	 *
	 * @param fromOffset
	 *        the offset to set; if {@literal null} then
	 *        {@link #DEFAULT_FROM_OFFSET} will be used
	 */
	public final void setFromOffset(Duration fromOffset) {
		this.fromOffset = (fromOffset != null ? fromOffset : DEFAULT_FROM_OFFSET);
	}

	/**
	 * Get the from offset, in seconds.
	 *
	 * @return the offset; defaults to {@link #DEFAULT_FROM_OFFSET}
	 */
	public final long getFromOffsetSeconds() {
		return fromOffset.getSeconds();
	}

	/**
	 * Set the "from" offset, in seconds.
	 *
	 * @param seconds
	 *        the offset to set; if less than 1 then
	 *        {@link #DEFAULT_FROM_OFFSET} will be used
	 */
	public final void setFromOffset(long seconds) {
		setFromOffset(seconds > 0 ? Duration.ofSeconds(seconds) : null);
	}

	/**
	 * Get the OAuth token URL.
	 *
	 * @return the OAuth token URL
	 */
	public final String getTokenUrl() {
		return tokenUrl;
	}

	/**
	 * Set the OAuth token URL.
	 *
	 * @param tokenUrl
	 *        the OAuth token URL to set; if {@literal null} then
	 *        {@link #DEFAULT_TOKEN_URL} will be used
	 */
	public final void setTokenUrl(String tokenUrl) {
		this.tokenUrl = (tokenUrl != null && !tokenUrl.isEmpty() ? tokenUrl : DEFAULT_TOKEN_URL);
	}

	/**
	 * Get the price lookup URL.
	 *
	 * @return the price URL
	 */
	public final String getPriceUrl() {
		return priceUrl;
	}

	/**
	 * Set the price lookup URL.
	 *
	 * @param priceUrl
	 *        the price URL to set; if {@literal null} then
	 *        {@link #DEFAULT_PRICE_URL} will be used
	 */
	public final void setPriceUrl(String priceUrl) {
		this.priceUrl = (priceUrl != null && !priceUrl.isEmpty() ? priceUrl : DEFAULT_PRICE_URL);
	}

}
