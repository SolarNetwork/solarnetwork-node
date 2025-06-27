/* ==================================================================
 * JsonOwmClient.java - 17/09/2018 7:45:49 AM
 *
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.weather.owm;

import static net.solarnetwork.codec.JsonUtils.parseBigDecimalAttribute;
import static net.solarnetwork.codec.JsonUtils.parseIntegerAttribute;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.domain.datum.AtmosphericDatum;
import net.solarnetwork.node.domain.datum.DayDatum;
import net.solarnetwork.node.domain.datum.SimpleAtmosphericDatum;
import net.solarnetwork.node.domain.datum.SimpleDayDatum;
import net.solarnetwork.node.service.support.JsonHttpClientSupport;

/**
 * JSON implementation of {@link OwmClient}
 *
 * @author matt
 * @version 2.1
 */
public class JsonOwmClient extends JsonHttpClientSupport implements OwmClient {

	/** The default value for the {@code baseUrl} property. */
	public static final String DEFAULT_API_BASE_URL = "https://api.openweathermap.org";

	private String apiKey;
	private String baseUrl = DEFAULT_API_BASE_URL;

	/**
	 * Default constructor.
	 */
	public JsonOwmClient() {
		super();
		setObjectMapper(new ObjectMapper());
	}

	private UriComponentsBuilder uriForLocation(String identifier, String path) {
		return UriComponentsBuilder.fromUriString(baseUrl).path(path).queryParam("id", identifier)
				.queryParam("mode", "json").queryParam("units", "metric").queryParam("appid", apiKey);
	}

	@Override
	public DayDatum getCurrentDay(String identifier, String timeZoneId) {
		if ( identifier == null ) {
			return null;
		}
		final String url = uriForLocation(identifier, "/data/2.5/weather").build().toUriString();
		DayDatum result = null;
		try {
			JsonNode data = getObjectMapper().readTree(jsonGET(url));
			result = parseDay(data, timeZoneId);
		} catch ( IOException e ) {
			log.warn("Error reading OpenWeatherMap URL [{}]: {}", url, e.getMessage());
		}
		return result;
	}

	private DayDatum parseDay(JsonNode node, String timeZoneId) {
		if ( node == null ) {
			return null;
		}
		final ZoneId zone = (timeZoneId != null ? ZoneId.of(timeZoneId) : ZoneOffset.UTC);

		Instant ts = dayStart(parseTimestampNode(node, "dt"), zone);
		SimpleDayDatum datum = new SimpleDayDatum(null, ts, new DatumSamples());

		JsonNode sysNode = node.get("sys");
		datum.setSunriseTime(localTime(parseTimestampNode(sysNode, "sunrise"), zone));
		datum.setSunsetTime(localTime(parseTimestampNode(sysNode, "sunset"), zone));

		return datum;
	}

	@Override
	public AtmosphericDatum getCurrentConditions(String identifier) {
		if ( identifier == null ) {
			return null;
		}
		final String url = uriForLocation(identifier, "/data/2.5/weather").build().toUriString();
		AtmosphericDatum result = null;
		try {
			JsonNode data = getObjectMapper().readTree(jsonGET(url));
			result = parseWeatherData(data);
		} catch ( IOException e ) {
			log.warn("Error reading OpenWeatherMap URL [{}]: {}", url, e.getMessage());
		}
		return result;
	}

	private AtmosphericDatum parseWeatherData(JsonNode node) {
		AtmosphericDatum datum = parseForecastData(node);
		if ( datum != null ) {
			// remove min/max temps, which for conditions data is not what we want
			datum.asMutableSampleOperations().putSampleValue(DatumSamplesType.Instantaneous,
					DayDatum.TEMPERATURE_MINIMUM_KEY, null);
			datum.asMutableSampleOperations().putSampleValue(DatumSamplesType.Instantaneous,
					DayDatum.TEMPERATURE_MAXIMUM_KEY, null);

			// remove forecast tag (all tags)
			datum.asMutableSampleOperations().setTags(null);
		}
		return datum;
	}

	@Override
	public Collection<AtmosphericDatum> getHourlyForecast(String identifier) {
		if ( identifier == null ) {
			return Collections.emptyList();
		}
		final String url = uriForLocation(identifier, "/data/2.5/forecast").build().toUriString();
		List<AtmosphericDatum> results = new ArrayList<>(50);
		try {
			JsonNode data = getObjectMapper().readTree(jsonGET(url));
			JsonNode list = data.path("list");
			if ( list.isArray() ) {
				for ( JsonNode item : list ) {
					AtmosphericDatum d = parseForecastData(item);
					if ( d != null ) {
						results.add(d);
					}
				}
			}
		} catch ( IOException e ) {
			log.warn("Error reading OpenWeatherMap URL [{}]: {}", url, e.getMessage());
		}
		return results;
	}

	private AtmosphericDatum parseForecastData(JsonNode node) {
		if ( node == null || !node.isObject() ) {
			return null;
		}
		Instant ts = parseTimestampNode(node, "dt");
		SimpleAtmosphericDatum d = new SimpleAtmosphericDatum(null, ts, new DatumSamples());
		d.addTag(AtmosphericDatum.TAG_FORECAST);

		JsonNode main = node.path("main");
		d.setTemperature(JsonUtils.parseBigDecimalAttribute(main, "temp"));
		d.asMutableSampleOperations().putSampleValue(DatumSamplesType.Instantaneous,
				DayDatum.TEMPERATURE_MINIMUM_KEY, parseBigDecimalAttribute(main, "temp_min"));
		d.asMutableSampleOperations().putSampleValue(DatumSamplesType.Instantaneous,
				DayDatum.TEMPERATURE_MAXIMUM_KEY, parseBigDecimalAttribute(main, "temp_max"));
		BigDecimal hPa = parseBigDecimalAttribute(main, "pressure");
		if ( hPa != null ) {
			d.setAtmosphericPressure(hPa.multiply(new BigDecimal(100)).intValue());
		}
		d.setHumidity(parseIntegerAttribute(main, "humidity"));

		d.setVisibility(parseIntegerAttribute(node, "visibility"));

		JsonNode weather = node.path("weather");
		if ( weather.isArray() && weather.size() > 0 ) {
			weather = weather.iterator().next();
			d.setSkyConditions(weather.path("main").asText());
			d.asMutableSampleOperations().putSampleValue(DatumSamplesType.Status, "iconId",
					weather.path("icon").asText());
		}

		JsonNode wind = node.path("wind");
		BigDecimal wdir = parseBigDecimalAttribute(wind, "deg");
		if ( wdir != null ) {
			d.setWindDirection(wdir.setScale(0, RoundingMode.HALF_UP).intValue());
		}
		d.setWindSpeed(parseBigDecimalAttribute(wind, "speed"));
		d.asMutableSampleOperations().putSampleValue(DatumSamplesType.Instantaneous, "wgust",
				parseBigDecimalAttribute(wind, "gust"));

		JsonNode clouds = node.path("clouds");
		d.asMutableSampleOperations().putSampleValue(DatumSamplesType.Instantaneous, "cloudiness",
				parseIntegerAttribute(clouds, "all"));

		JsonNode rain = node.path("rain");
		BigDecimal threeHourRain = parseBigDecimalAttribute(rain, "3h");
		if ( threeHourRain != null ) {
			d.setRain(threeHourRain.setScale(0, RoundingMode.HALF_UP).intValue());
		}

		return d;
	}

	private Instant parseTimestampNode(JsonNode node, String key) {
		Integer val = parseIntegerAttribute(node, key);
		if ( val == null ) {
			return null;
		}
		return Instant.ofEpochMilli(val * 1000L);
	}

	private Instant dayStart(Instant ts, ZoneId zone) {
		if ( ts == null ) {
			return null;
		}
		return ts.atZone(zone).truncatedTo(ChronoUnit.DAYS).toInstant();
	}

	private LocalTime localTime(Instant ts, ZoneId zone) {
		if ( ts == null ) {
			return null;
		}
		return ts.atZone(zone).toLocalTime();
	}

	@Override
	public String getApiKey() {
		return apiKey;
	}

	/**
	 * Set the OWM API key to use.
	 *
	 * @param apiKey
	 *        the API key
	 */
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	/**
	 * Set the base URL to use.
	 *
	 * <p>
	 * This defaults to {@link #DEFAULT_API_BASE_URL} which should be sufficient
	 * for most cases.
	 * </p>
	 *
	 * @param baseUrl
	 *        the baseUrl to set
	 */
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

}
