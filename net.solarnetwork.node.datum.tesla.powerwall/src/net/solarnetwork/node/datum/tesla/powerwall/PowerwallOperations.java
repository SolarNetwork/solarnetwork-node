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
import net.solarnetwork.service.RemoteServiceException;

/**
 * Access to Powerwall APIs.
 * 
 * @author matt
 * @version 1.0
 */
public class PowerwallOperations implements Closeable {

	/** The {@code batterySuffix} property default value. */
	public static final String DEFAULT_BATTERY_SUFFIX = "/battery";

	/** The {@code loadSuffix} property default value. */
	public static final String DEFAULT_LOAD_SUFFIX = "/load";

	/** The {@code siteSuffix} property default value. */
	public static final String DEFAULT_SITE_SUFFIX = "/site";

	/** The {@code solarSuffix} property default value. */
	public static final String DEFAULT_SOLAR_SUFFIX = "/solar";

	private static final Logger log = LoggerFactory.getLogger(PowerwallOperations.class);

	private final boolean useTls;
	private final String hostName;
	private final int port;
	private String username;
	private String password;
	private final ObjectMapper mapper;

	private String batterySuffix = DEFAULT_BATTERY_SUFFIX;
	private String loadSuffix = DEFAULT_LOAD_SUFFIX;
	private String solarSuffix = DEFAULT_SOLAR_SUFFIX;
	private String siteSuffix = DEFAULT_SITE_SUFFIX;

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
	public PowerwallOperations(String hostName, String username, String password,
			RequestConfig requestConfig, ObjectMapper mapper) {
		this(true, hostName, username, password, requestConfig, mapper);
	}

	/**
	 * Constructor.
	 * 
	 * @param useTls
	 *        {@literal true} to use TLS (HTTPS)
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
	public PowerwallOperations(boolean useTls, String hostName, String username, String password,
			RequestConfig requestConfig, ObjectMapper mapper) {
		super();
		this.useTls = useTls;
		final String[] hostComponents = requireNonNullArgument(hostName, "hostName").split(":", 2);
		this.hostName = hostComponents[0].toLowerCase();
		this.port = hostComponents.length > 1 ? Integer.parseInt(hostComponents[1]) : 443;
		this.username = requireNonNullArgument(username, "username");
		this.password = requireNonNullArgument(password, "password");
		this.mapper = mapper;
		this.httpClient = createHttpClient(requestConfig);
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

	public JsonNode status() {
		URI uri = baseUri().path("/api/status").build().toUri();
		JsonNode json = getJson(uri);
		log.debug("Got status response: {}", json);
		return json;
	}

	public JsonNode metersAggregates() {
		URI uri = baseUri().path("/api/meters/aggregates").build().toUri();
		JsonNode json = getJson(uri);
		log.debug("Got meters/aggregates response: {}", json);
		return json;
	}

	public JsonNode systemStatus() {
		URI uri = baseUri().path("/api/system_status").build().toUri();
		JsonNode json = getJson(uri);
		log.debug("Got system_status response: {}", json);
		return json;
	}

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
			SimpleAcDcEnergyDatum d = extractDatum(node, sourceId + getBatterySuffix(), false);
			if ( d != null ) {
				populateBatteryDatum(d);
				result.add(d);
			}
		}

		node = root.path("load");
		if ( node.isObject() ) {
			AcDcEnergyDatum d = extractDatum(node, sourceId + getLoadSuffix(), true);
			if ( d != null ) {
				result.add(d);
			}
		}

		node = root.path("site");
		if ( node.isObject() ) {
			AcDcEnergyDatum d = extractDatum(node, sourceId + getSiteSuffix(), true);
			if ( d != null ) {
				result.add(d);
			}
		}

		node = root.path("solar");
		if ( node.isObject() ) {
			AcDcEnergyDatum d = extractDatum(node, sourceId + getSolarSuffix(), false);
			if ( d != null ) {
				result.add(d);
			}
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
	private SimpleAcDcEnergyDatum extractDatum(JsonNode root, String sourceId, boolean consumer) {
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

	private JsonNode loginBasic() {
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
					loginBasic();
					return forJson(req);
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
	 * Get the source ID suffix used for battery data.
	 * 
	 * @return the suffix; default to {@link #DEFAULT_BATTERY_SUFFIX}
	 */
	public String getBatterySuffix() {
		return batterySuffix;
	}

	/**
	 * Set the source ID used for battery data.
	 * 
	 * @param batterySuffix
	 *        the suffix to set
	 */
	public void setBatterySuffix(String batterySuffix) {
		this.batterySuffix = batterySuffix;
	}

	/**
	 * Get the source ID suffix used for load data.
	 * 
	 * @return the suffix; default to {@link #DEFAULT_LOAD_SUFFIX}
	 */
	public String getLoadSuffix() {
		return loadSuffix;
	}

	/**
	 * Set the source ID used for load data.
	 * 
	 * @param loadSuffix
	 *        the suffix to set
	 */
	public void setLoadSuffix(String loadSuffix) {
		this.loadSuffix = loadSuffix;
	}

	/**
	 * Get the source ID suffix used for solar data.
	 * 
	 * @return the suffix; default to {@link #DEFAULT_SOLAR_SUFFIX}
	 */
	public String getSolarSuffix() {
		return solarSuffix;
	}

	/**
	 * Set the source ID used for solar data.
	 * 
	 * @param solarSuffix
	 *        the suffix to set
	 */
	public void setSolarSuffix(String solarSuffix) {
		this.solarSuffix = solarSuffix;
	}

	/**
	 * Get the source ID suffix used for site data.
	 * 
	 * @return the suffix; default to {@link #DEFAULT_SITE_SUFFIX}
	 */
	public String getSiteSuffix() {
		return siteSuffix;
	}

	/**
	 * Set the source ID used for site data.
	 * 
	 * @param siteSuffix
	 *        the suffix to set
	 */
	public void setSiteSuffix(String siteSuffix) {
		this.siteSuffix = siteSuffix;
	}

}
