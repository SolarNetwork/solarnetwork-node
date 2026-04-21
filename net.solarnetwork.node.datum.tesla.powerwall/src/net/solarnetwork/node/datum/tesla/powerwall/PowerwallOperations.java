/* ==================================================================
 * PowerwallOperations.java - 9/11/2023 6:40:15 am
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

package net.solarnetwork.node.datum.tesla.powerwall;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.EnergyStorageDatum;
import net.solarnetwork.node.domain.datum.AcDcEnergyDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleAcDcEnergyDatum;
import net.solarnetwork.node.service.PlaceholderService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.RemoteServiceException;

/**
 * Access to Powerwall APIs.
 *
 * @author matt
 * @version 1.2
 */
public class PowerwallOperations implements Closeable {

	/**
	 * The component placeholder name.
	 *
	 * <p>
	 * This placeholder is used to resolve the source ID for each component type
	 * supported by this class:
	 * </p>
	 *
	 * <ol>
	 * <li>battery</li>
	 * <li>load</li>
	 * <li>site</li>
	 * <li>solar</li>
	 * </ol>
	 *
	 * <p>
	 * For example, a source ID {@code /powerwall/{COMP}/1} would resolve source
	 * IDs like {@code /powerwall/battery/1} and {@code /powerwall/solar/1} and
	 * so on.
	 * </p>
	 *
	 * <p>
	 * If the given source ID does not have a {@code {COMP}} placeholder then
	 * the placeholder will be added as a suffix, preceeded by a {@code /}
	 * character. For example, a source ID {@code /powerwall/1} would resolve
	 * source IDs like {@code /powerwall/1/batter} and
	 * {@code /powerwall/1/solar} and so on.
	 * </p>
	 *
	 * @since 1.2
	 */
	public static final String COMPONENT_PLACEHOLDER_NAME = "COMP";

	/** The {@code batterySuffix} property default value. */
	public static final String DEFAULT_BATTERY_PLACEHOLDER = "battery";

	/** The {@code loadSuffix} property default value. */
	public static final String DEFAULT_LOAD_PLACEHOLDER = "load";

	/** The {@code siteSuffix} property default value. */
	public static final String DEFAULT_SITE_PLACEHOLDER = "site";

	/** The {@code solarSuffix} property default value. */
	public static final String DEFAULT_SOLAR_PLACEHOLDER = "solar";

	private static final Logger log = LoggerFactory.getLogger(PowerwallOperations.class);

	private final boolean useTls;
	private final String hostName;
	private final int port;
	private final @Nullable String username;
	private final @Nullable String password;
	private final ObjectMapper mapper;
	private @Nullable OptionalService<PlaceholderService> placeholderService;

	private String batteryPlaceholder = DEFAULT_BATTERY_PLACEHOLDER;
	private String loadPlaceholder = DEFAULT_LOAD_PLACEHOLDER;
	private String solarPlaceholder = DEFAULT_SOLAR_PLACEHOLDER;
	private String sitePlaceholder = DEFAULT_SITE_PLACEHOLDER;

	private final CloseableHttpClient httpClient;

	/**
	 * Constructor.
	 *
	 * @param hostName
	 *        the host name that is required; can include a port after a
	 *        {@literal :} delimiter
	 * @param username
	 *        the username
	 * @param password
	 *        the password
	 * @param requestConfig
	 *        the HTTP request configuration to use
	 * @param mapper
	 *        the JSON mapper to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public PowerwallOperations(String hostName, @Nullable String username, @Nullable String password,
			RequestConfig requestConfig, ObjectMapper mapper) {
		this(null, hostName, username, password, requestConfig, mapper);
	}

	/**
	 * Constructor.
	 *
	 * @param useTls
	 *        {@literal true} to use TLS (HTTPS), {@code false} to not, or
	 *        {@code null} to auto-detect based on {@code hostName}
	 * @param hostName
	 *        the host name that is required; can include a port after a
	 *        {@literal :} delimiter
	 * @param username
	 *        the username
	 * @param password
	 *        the password
	 * @param requestConfig
	 *        the HTTP request configuration to use
	 * @param mapper
	 *        the JSON mapper to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public PowerwallOperations(@Nullable Boolean useTls, String hostName, @Nullable String username,
			@Nullable String password, RequestConfig requestConfig, ObjectMapper mapper) {
		super();
		final String[] hostComponents = requireNonNullArgument(hostName, "hostName").split(":", 2);
		this.hostName = hostComponents[0].toLowerCase();
		this.useTls = shouldUseTls(useTls, hostComponents);
		this.port = hostComponents.length > 1 ? Integer.parseInt(hostComponents[1]) : 443;
		this.username = username;
		this.password = password;
		this.mapper = mapper;
		this.httpClient = createHttpClient(requestConfig);
	}

	/**
	 * Auto-detect if TLS should be used.
	 *
	 * <p>
	 * If the hostname is {@code localhost} or {@code 127.0.0.1} then this will
	 * return {@code false}. If a port is specified and it is not {@code 443}
	 * then this will return {@code false}. Otherwise {@code true} will be
	 * returned.
	 * </p>
	 *
	 * @param useTls
	 *        the requested use of TLS; if non-{@code null} this will be
	 *        returned directly
	 * @param hostComponents
	 *        array with the hostname and optionally the port
	 * @return {@code true} if TLS should be used
	 */
	private static boolean shouldUseTls(@Nullable Boolean useTls, String[] hostComponents) {
		if ( useTls != null ) {
			return useTls;
		}
		if ( "localhost".equalsIgnoreCase(hostComponents[0]) || "127.0.0.1".equals(hostComponents[0]) ) {
			return false;
		}
		if ( hostComponents.length > 1 ) {
			if ( !"443".equals(hostComponents[1]) ) {
				// non-TLS port specified, so do not use TLS
				return false;
			}
		}
		// default to true
		return true;
	}

	@Override
	public void close() throws IOException {
		httpClient.close();
	}

	private CloseableHttpClient createHttpClient(RequestConfig requestConfig) {
		// @formatter:off
	    return HttpClientBuilder.create()
	        .setDefaultRequestConfig(requestConfig)
	        .setSSLContext(buildSSLContext())
	        .build();
	    // @formatter:on
	}

	private SSLContext buildSSLContext() {
		SSLContext sslContext;
		try {
			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, new TrustManager[] { new PowerwallTrustManager(hostName) }, null);
		} catch ( NoSuchAlgorithmException | KeyManagementException e ) {
			throw new IllegalStateException("Unable to acquire a TLS context.", e);
		}
		return sslContext;
	}

	private UriComponentsBuilder baseUri() {
		// @formatter:off
		return UriComponentsBuilder.newInstance()
				.scheme(useTls ? "https" : "http")
				.host(hostName)
				.port(port);
		// @formatter:on
	}

	/**
	 * Get the status.
	 *
	 * @return the status
	 */
	public JsonNode status() {
		URI uri = baseUri().path("/api/status").build().toUri();
		JsonNode json = getJson(uri);
		log.debug("Got status response: {}", json);
		return json;
	}

	/**
	 * Get the meters aggregates.
	 *
	 * @return the meters
	 */
	public JsonNode metersAggregates() {
		URI uri = baseUri().path("/api/meters/aggregates").build().toUri();
		JsonNode json = getJson(uri);
		log.debug("Got meters/aggregates response: {}", json);
		return json;
	}

	/**
	 * Get the system status.
	 *
	 * @return the status
	 */
	public JsonNode systemStatus() {
		URI uri = baseUri().path("/api/system_status").build().toUri();
		JsonNode json = getJson(uri);
		log.debug("Got system_status response: {}", json);
		return json;
	}

	/**
	 * Get the system status SOE.
	 *
	 * @return the SOE
	 */
	public JsonNode systemStatusSoe() {
		URI uri = baseUri().path("/api/system_status/soe").build().toUri();
		JsonNode json = getJson(uri);
		log.debug("Got system_status/soe response: {}", json);
		return json;
	}

	private void populateBatteryDatum(SimpleAcDcEnergyDatum d) {
		JsonNode node = systemStatusSoe().path("percentage");
		if ( node.isNumber() ) {
			d.putSampleValue(DatumSamplesType.Instantaneous,
					EnergyStorageDatum.STATE_OF_CHARGE_PERCENTAGE_KEY, node.floatValue());
		}

		JsonNode status = systemStatus();
		node = status.path("nominal_full_pack_energy");
		if ( node.isNumber() ) {
			d.putSampleValue(DatumSamplesType.Instantaneous, EnergyStorageDatum.CAPACITY_WATT_HOURS_KEY,
					node.intValue());
		}

		node = status.path("nominal_energy_remaining");
		if ( node.isNumber() ) {
			d.putSampleValue(DatumSamplesType.Instantaneous, EnergyStorageDatum.AVAILABLE_WATT_HOURS_KEY,
					node.intValue());
		}

		node = status.path("system_island_state");
		if ( node.isTextual() ) {
			String state = node.textValue();
			d.putSampleValue(DatumSamplesType.Status, "gridConnected",
					state.toLowerCase().contains("gridconnected") ? 1 : 0);
		}
	}

	/**
	 * Read the meter aggregate API and return datum.
	 *
	 * @param sourceId
	 *        the base source ID to use; the various suffix values configured on
	 *        this class will be appended
	 * @return the datum, never {@literal null}
	 */
	public Collection<NodeDatum> datum(String sourceId) {
		JsonNode root = metersAggregates();
		List<NodeDatum> result = new ArrayList<>(3);

		JsonNode node = null;

		node = root.path("battery");
		if ( node.isObject() ) {
			SimpleAcDcEnergyDatum d = extractDatum(node, resolveSourceId(sourceId, batteryPlaceholder),
					false);
			if ( d != null ) {
				populateBatteryDatum(d);
				result.add(d);
			}
		}

		node = root.path("load");
		if ( node.isObject() ) {
			AcDcEnergyDatum d = extractDatum(node, resolveSourceId(sourceId, loadPlaceholder), true);
			if ( d != null ) {
				result.add(d);
			}
		}

		node = root.path("site");
		if ( node.isObject() ) {
			AcDcEnergyDatum d = extractDatum(node, resolveSourceId(sourceId, sitePlaceholder), true);
			if ( d != null ) {
				result.add(d);
			}
		}

		node = root.path("solar");
		if ( node.isObject() ) {
			AcDcEnergyDatum d = extractDatum(node, resolveSourceId(sourceId, solarPlaceholder), false);
			if ( d != null ) {
				result.add(d);
			}
		}

		return result;
	}

	private String resolveSourceId(String sourceId, String component) {
		String result = PlaceholderService.resolvePlaceholders(placeholderService, sourceId,
				Map.of(COMPONENT_PLACEHOLDER_NAME, component));
		if ( !result.contains(component) ) {
			result = result + '/' + component;
		}
		return result;
	}

	/**
	 * Extract a datum.
	 *
	 * @param root
	 *        the object node to extract from
	 * @param sourceId
	 *        the source ID to use
	 * @param consumer
	 *        {@literal true} if "export" energy should be considered the
	 *        "reverse" direction
	 * @return the datum, or {@literal null} if one could not be extracted
	 */
	private @Nullable SimpleAcDcEnergyDatum extractDatum(JsonNode root, String sourceId,
			boolean consumer) {
		SimpleAcDcEnergyDatum d = null;
		JsonNode node = null;

		node = root.path("last_communication_time");
		if ( node.isTextual() ) {
			Instant ts = ISO_DATE_TIME.parse(node.asText(), Instant::from);
			d = new SimpleAcDcEnergyDatum(sourceId, ts, new DatumSamples());
		} else {
			return null;
		}

		node = root.path("instant_power");
		if ( node.isNumber() ) {
			d.setWatts(node.intValue());
		}

		node = root.path("instant_reactive_power");
		if ( node.isNumber() ) {
			d.setReactivePower(node.intValue());
		}

		node = root.path("instant_apparent_power");
		if ( node.isNumber() ) {
			d.setApparentPower(node.intValue());
		}

		node = root.path("frequency");
		if ( node.isNumber() ) {
			BigDecimal f = node.decimalValue();
			if ( f.compareTo(BigDecimal.ZERO) > 0 ) {
				d.setFrequency(f.floatValue());
			}
		}

		node = root.path("energy_exported");
		if ( node.isNumber() ) {
			if ( consumer ) {
				d.setReverseWattHourReading(node.longValue());
			} else {
				d.setWattHourReading(node.asLong());
			}
		}

		node = root.path("energy_imported");
		if ( node.isNumber() ) {
			if ( consumer ) {
				d.setWattHourReading(node.longValue());
			} else {
				d.setReverseWattHourReading(node.asLong());
			}
		}

		node = root.path("instant_average_voltage");
		if ( node.isNumber() ) {
			d.setVoltage(node.floatValue());
		}

		node = root.path("instant_total_current");
		if ( node.isNumber() ) {
			d.setCurrent(node.floatValue());
		}

		node = root.path("i_a_current");
		if ( node.isNumber() ) {
			d.setCurrent(AcPhase.PhaseA, node.floatValue());
		}

		node = root.path("i_b_current");
		if ( node.isNumber() ) {
			d.setCurrent(AcPhase.PhaseB, node.floatValue());
		}

		node = root.path("i_c_current");
		if ( node.isNumber() ) {
			d.setCurrent(AcPhase.PhaseC, node.floatValue());
		}

		return (d.isEmpty() ? null : d);
	}

	private @Nullable JsonNode loginBasic() {
		if ( username == null || username.isEmpty() || password == null || password.isEmpty() ) {
			return null;
		}
		final Map<String, Object> data = new LinkedHashMap<>(2);
		data.put("username", username);
		data.put("password", password);
		final URI uri = baseUri().path("/api/login/Basic").build().toUri();
		JsonNode json = postJson(uri, data);
		log.debug("Got login/Basic response: {}", json);
		return json;
	}

	private JsonNode getJson(URI uri) {
		final HttpGet req = new HttpGet(uri);
		try {
			return forJson(req);
		} catch ( RemoteServiceException e ) {
			if ( e.getCause() instanceof HttpResponseException ) {
				HttpResponseException err = (HttpResponseException) e.getCause();
				if ( err.getStatusCode() == 401 || err.getStatusCode() == 403 ) {
					// attempt to retry, by logging in and then retry
					if ( loginBasic() != null ) {
						return forJson(req);
					}
				}
			}
			throw e;
		}
	}

	private JsonNode postJson(URI uri, Object data) {
		final String body;
		try {
			body = mapper.writeValueAsString(data);
		} catch ( IOException e ) {
			throw new IllegalArgumentException("Error encoding ["
					+ (data != null ? data.getClass().getName() : null) + "] into JSON: " + e.toString(),
					e);
		}

		final HttpPost req = new HttpPost(uri);
		req.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		req.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
		return forJson(req);
	}

	private JsonNode forJson(HttpUriRequest req) {
		try {
			req.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
			String json = httpClient.execute(req, new BasicResponseHandler());
			return mapper.readTree(json);
		} catch ( HttpResponseException e ) {
			String msg = String.format("Error reading status from Powerwall @ %s: %s", req.getURI(),
					HttpStatus.resolve(e.getStatusCode()));
			throw new RemoteServiceException(msg, e);
		} catch ( IOException e ) {
			String msg = "Error logging in to Powerwall @ " + req.getURI();
			throw new RemoteServiceException(msg, e);
		}
	}

	/**
	 * Get the {@link PlaceholderService}.
	 *
	 * @return the service
	 */
	public final @Nullable OptionalService<PlaceholderService> getPlaceholderService() {
		return placeholderService;
	}

	/**
	 * Set the {@link PlaceholderService}.
	 *
	 * @param placeholderService
	 *        the service to set
	 */
	public final void setPlaceholderService(
			@Nullable OptionalService<PlaceholderService> placeholderService) {
		this.placeholderService = placeholderService;
	}

	/**
	 * Get the source ID suffix used for battery data.
	 *
	 * @return the suffix; default to {@link #DEFAULT_BATTERY_PLACEHOLDER}
	 */
	public final String getBatteryPlaceholder() {
		return batteryPlaceholder;
	}

	/**
	 * Set the source ID used for battery data.
	 *
	 * @param batteryPlaceholder
	 *        the suffix to set
	 */
	public final void setBatteryPlaceholder(String batteryPlaceholder) {
		this.batteryPlaceholder = batteryPlaceholder;
	}

	/**
	 * Get the source ID suffix used for load data.
	 *
	 * @return the suffix; default to {@link #DEFAULT_LOAD_PLACEHOLDER}
	 */
	public final String getLoadPlaceholder() {
		return loadPlaceholder;
	}

	/**
	 * Set the source ID used for load data.
	 *
	 * @param loadPlaceholder
	 *        the suffix to set
	 */
	public final void setLoadPlaceholder(String loadPlaceholder) {
		this.loadPlaceholder = loadPlaceholder;
	}

	/**
	 * Get the source ID suffix used for solar data.
	 *
	 * @return the suffix; default to {@link #DEFAULT_SOLAR_PLACEHOLDER}
	 */
	public final String getSolarPlaceholder() {
		return solarPlaceholder;
	}

	/**
	 * Set the source ID used for solar data.
	 *
	 * @param solarPlaceholder
	 *        the suffix to set
	 */
	public final void setSolarPlaceholder(String solarPlaceholder) {
		this.solarPlaceholder = solarPlaceholder;
	}

	/**
	 * Get the source ID suffix used for site data.
	 *
	 * @return the suffix; default to {@link #DEFAULT_SITE_PLACEHOLDER}
	 */
	public final String getSitePlaceholder() {
		return sitePlaceholder;
	}

	/**
	 * Set the source ID used for site data.
	 *
	 * @param sitePlaceholder
	 *        the suffix to set
	 */
	public final void setSitePlaceholder(String sitePlaceholder) {
		this.sitePlaceholder = sitePlaceholder;
	}

}
