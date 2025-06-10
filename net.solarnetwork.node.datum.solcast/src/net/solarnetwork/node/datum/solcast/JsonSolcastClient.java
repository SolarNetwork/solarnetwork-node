/* ==================================================================
 * JsonSolcastClient.java - 14/10/2022 9:51:33 am
 *
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.solcast;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import java.io.IOException;
import java.net.URLConnection;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.function.Consumer;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.AtmosphericDatum;
import net.solarnetwork.node.domain.datum.SimpleAtmosphericDatum;
import net.solarnetwork.node.service.support.JsonHttpClientSupport;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.util.StringUtils;

/**
 * Basic JSON client implementation of {@link SolcastClient}.
 *
 * @author matt
 * @version 1.1
 */
public class JsonSolcastClient extends JsonHttpClientSupport
		implements ConfigurableSolcastClient, Consumer<URLConnection> {

	/** The default value for the {@code baseUrl} property. */
	public static final String DEFAULT_API_BASE_URL = "https://api.solcast.com.au";

	private String apiKey;
	private String baseUrl = DEFAULT_API_BASE_URL;

	/**
	 * Default constructor.
	 */
	public JsonSolcastClient() {
		super();
		setObjectMapper(JsonUtils.newObjectMapper());
	}

	private UriComponentsBuilder uri(String path) {
		return UriComponentsBuilder.fromUriString(baseUrl).path(path);
	}

	@Override
	public void accept(URLConnection conn) {
		// add API request header
		if ( apiKey != null ) {
			conn.setRequestProperty("Authorization", "Bearer ".concat(apiKey));
		}
	}

	@Override
	public boolean isConfigured() {
		return apiKey != null && !apiKey.isEmpty();
	}

	@Override
	public AtmosphericDatum getMostRecentConditions(String sourceId, SolcastCriteria criteria) {
		if ( sourceId == null || sourceId.isEmpty() ) {
			return null;
		}
		// @formatter:off
		final UriComponentsBuilder uriBuilder = uri("/data/live/radiation_and_weather")
				.queryParam("latitude", criteria.getLat())
				.queryParam("longitude", criteria.getLon())
				.queryParam("hours", 1);
		// @formatter:on
		if ( criteria.getAzimuth() != null ) {
			uriBuilder.queryParam("azimuth", criteria.getAzimuth());
		}
		if ( criteria.getTilt() != null ) {
			uriBuilder.queryParam("tilt", criteria.getTilt());
		}
		if ( criteria.getArrayType() != null && !criteria.getArrayType().isEmpty() ) {
			uriBuilder.queryParam("array_type", criteria.getArrayType());
		}
		if ( criteria.getParameters() != null && !criteria.getParameters().isEmpty() ) {
			uriBuilder.queryParam("output_parameters",
					StringUtils.commaDelimitedStringFromCollection(criteria.getParameters()));
		} else {
			uriBuilder.queryParam("output_parameters", "air_temp,dni,ghi");
		}
		if ( criteria.getPeriod() != null ) {
			uriBuilder.queryParam("period", criteria.getPeriod().toString());
		}
		final String url = uriBuilder.build().toUriString();
		AtmosphericDatum result = null;
		try {
			JsonNode data = getObjectMapper().readTree(jsonGET(url, this));
			Instant now = Instant.now();
			AtmosphericDatum mostRecentOnOrBeforeNow = null;
			for ( JsonNode node : data.path("estimated_actuals") ) {
				String end = node.path("period_end").asText(null);
				if ( end == null ) {
					continue;
				}
				String per = node.path("period").asText(null);
				Duration d = Duration.ofMinutes(30);
				if ( per != null ) {
					d = Duration.parse(per);
				}
				Instant ts = ISO_INSTANT.parse(end, Instant::from).minus(d);
				if ( mostRecentOnOrBeforeNow != null
						&& ts.isBefore(mostRecentOnOrBeforeNow.getTimestamp()) ) {
					break;
				} else if ( !ts.isAfter(now) && (mostRecentOnOrBeforeNow == null
						|| ts.isAfter(mostRecentOnOrBeforeNow.getTimestamp())) ) {
					DatumSamples s = new DatumSamples();
					for ( Iterator<Entry<String, JsonNode>> itr = node.fields(); itr.hasNext(); ) {
						Entry<String, JsonNode> f = itr.next();
						String fieldName = f.getKey();
						JsonNode n = f.getValue();
						if ( "period_end".equals(fieldName) ) {
							continue;
						} else if ( "period".equals(fieldName) ) {
							try {
								Duration period = Duration.parse(n.asText());
								s.putInstantaneousSampleValue("duration", period.getSeconds());
							} catch ( DateTimeParseException e ) {
								// ignore
							}
						} else if ( "air_temp".equals(fieldName) ) {
							if ( n.isNumber() ) {
								s.putInstantaneousSampleValue(AtmosphericDatum.TEMPERATURE_KEY,
										n.numberValue());
							}
						} else if ( "dewpoint_temp".equals(fieldName) ) {
							if ( n.isNumber() ) {
								s.putInstantaneousSampleValue(AtmosphericDatum.DEW_POINT_KEY,
										n.numberValue());
							}
						} else if ( "ghi".equals(fieldName) ) {
							if ( n.isNumber() ) {
								s.putInstantaneousSampleValue(AtmosphericDatum.IRRADIANCE_KEY,
										n.numberValue());
							}
						} else if ( "relative_humidity".equals(fieldName) ) {
							if ( n.isNumber() ) {
								s.putInstantaneousSampleValue(AtmosphericDatum.HUMIDITY_KEY,
										n.numberValue());
							}
						} else if ( "surface_pressure".equals(fieldName) ) {
							// convert hPa to Pa
							if ( n.isNumber() ) {
								s.putInstantaneousSampleValue(AtmosphericDatum.ATMOSPHERIC_PRESSURE_KEY,
										NumberUtils.scaled(n.numberValue(), 2));
							}
						} else if ( "wind_direction_10m".equals(fieldName) ) {
							if ( n.isNumber() ) {
								s.putInstantaneousSampleValue(AtmosphericDatum.WIND_DIRECTION_KEY,
										n.numberValue());
							}
						} else if ( "wind_speed_10m".equals(fieldName) ) {
							if ( n.isNumber() ) {
								s.putInstantaneousSampleValue(AtmosphericDatum.WIND_SPEED_KEY,
										n.numberValue());
							}
						} else if ( n.isNumber() ) {
							s.putInstantaneousSampleValue(fieldName, n.numberValue());
						} else if ( n.isTextual() ) {
							s.putStatusSampleValue(fieldName, n.textValue());
						}
					}
					if ( !s.isEmpty() ) {
						mostRecentOnOrBeforeNow = new SimpleAtmosphericDatum(sourceId, ts, s);
					}
				}
			}
			return mostRecentOnOrBeforeNow;
		} catch ( IOException e ) {
			log.warn("Error reading Solcast URL [{}]: {}", url, e.getMessage());
		}
		return result;
	}

	@Override
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	/**
	 * Get the base URL to the Solcasts API.
	 *
	 * @return the base URL
	 */
	public String getBaseUrl() {
		return baseUrl;
	}

	/**
	 * Set the base URL to the Solcasts API
	 *
	 * @param baseUrl
	 *        the base URL to set
	 */
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

}
