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

import static net.solarnetwork.util.JsonUtils.parseBigDecimalAttribute;
import static net.solarnetwork.util.JsonUtils.parseIntegerAttribute;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.node.domain.AtmosphericDatum;
import net.solarnetwork.node.domain.DayDatum;
import net.solarnetwork.node.domain.GeneralAtmosphericDatum;
import net.solarnetwork.node.domain.GeneralDayDatum;
import net.solarnetwork.node.support.JsonHttpClientSupport;
import net.solarnetwork.util.JsonUtils;

/**
 * JSON implementation of {@link OwmClient}
 * 
 * @author matt
 * @version 1.0
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
		return UriComponentsBuilder.fromHttpUrl(baseUrl).path(path).queryParam("id", identifier)
				.queryParam("mode", "json").queryParam("units", "metric").queryParam("appid", apiKey);
	}

	@Override
	public DayDatum getCurrentDay(String identifier, String timeZoneId) {
		if ( identifier == null ) {
			return null;
		}
		final String url = uriForLocation(identifier, "/data/2.5/weather").build().toUriString();
		GeneralDayDatum result = null;
		try {
			JsonNode data = getObjectMapper().readTree(jsonGET(url));
			result = parseDay(data, timeZoneId);
		} catch ( IOException e ) {
			log.warn("Error reading OpenWeatherMap URL [{}]: {}", url, e.getMessage());
		}
		return result;
	}

	private GeneralDayDatum parseDay(JsonNode node, String timeZoneId) {
		if ( node == null ) {
			return null;
		}
		final DateTimeZone zone = (timeZoneId != null ? DateTimeZone.forID(timeZoneId)
				: DateTimeZone.UTC);
		GeneralDayDatum datum = new GeneralDayDatum();

		datum.setCreated(date(dayStart(parseTimestampNode(node, "dt"), zone)));

		JsonNode sysNode = node.get("sys");
		datum.setSunrise(localTime(parseTimestampNode(sysNode, "sunrise"), zone));
		datum.setSunset(localTime(parseTimestampNode(sysNode, "sunset"), zone));

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
					GeneralAtmosphericDatum d = parseForecastData(item);
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

	/*-
		{
			"dt": 1537142400,
			"main": {
				"temp": 15.44,
				"temp_min": 13.72,
				"temp_max": 15.44,
				"pressure": 1025.13,
				"sea_level": 1029.38,
				"grnd_level": 1025.13,
				"humidity": 88,
				"temp_kf": 1.72
			},
			"weather": [
				{
					"id": 500,
					"main": "Rain",
					"description": "light rain",
					"icon": "10d"
				}
			],
			"clouds": {
				"all": 44
			},
			"wind": {
				"speed": 10.31,
				"deg": 340.502
			},
			"rain": {
				"3h": 0.18
			},
			"sys": {
				"pod": "d"
			},
			"dt_txt": "2018-09-17 00:00:00"
		}
	*/
	private GeneralAtmosphericDatum parseForecastData(JsonNode node) {
		if ( node == null || !node.isObject() ) {
			return null;
		}
		GeneralAtmosphericDatum d = new GeneralAtmosphericDatum();
		d.setCreated(date(parseTimestampNode(node, "dt")));
		d.addTag(AtmosphericDatum.TAG_FORECAST);

		JsonNode main = node.path("main");
		d.setTemperature(JsonUtils.parseBigDecimalAttribute(main, "temp"));
		d.putInstantaneousSampleValue(DayDatum.TEMPERATURE_MINIMUM_KEY,
				parseBigDecimalAttribute(main, "temp_min"));
		d.putInstantaneousSampleValue(DayDatum.TEMPERATURE_MAXIMUM_KEY,
				parseBigDecimalAttribute(main, "temp_max"));
		BigDecimal hPa = parseBigDecimalAttribute(main, "pressure");
		if ( hPa != null ) {
			d.setAtmosphericPressure(hPa.multiply(new BigDecimal(100)).intValue());
		}
		d.setHumidity(parseIntegerAttribute(main, "humidity"));

		JsonNode weather = node.path("weather");
		if ( weather.isArray() && weather.size() > 0 ) {
			weather = weather.iterator().next();
			d.setSkyConditions(weather.path("main").asText());
			d.putStatusSampleValue("iconId", weather.path("icon").asText());
		}

		JsonNode wind = node.path("wind");
		BigDecimal wdir = parseBigDecimalAttribute(wind, "deg");
		if ( wdir != null ) {
			d.setWindDirection(wdir.setScale(0, RoundingMode.HALF_UP).intValue());
		}
		d.setWindSpeed(parseBigDecimalAttribute(wind, "speed"));

		JsonNode rain = node.path("rain");
		BigDecimal threeHourRain = parseBigDecimalAttribute(rain, "3h");
		if ( threeHourRain != null ) {
			d.setRain(threeHourRain.setScale(0, RoundingMode.HALF_UP).intValue());
		}

		return d;
	}

	private DateTime parseTimestampNode(JsonNode node, String key) {
		Integer val = parseIntegerAttribute(node, key);
		if ( val == null ) {
			return null;
		}
		return new DateTime(val * 1000L, DateTimeZone.UTC);
	}

	private DateTime dayStart(DateTime ts, DateTimeZone zone) {
		if ( ts == null ) {
			return null;
		}
		return ts.withZone(zone).dayOfMonth().roundFloorCopy();
	}

	private LocalTime localTime(DateTime ts, DateTimeZone zone) {
		if ( ts == null ) {
			return null;
		}
		return ts.withZone(zone).toLocalTime();
	}

	private Date date(DateTime ts) {
		if ( ts == null ) {
			return null;
		}
		return ts.toDate();
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
