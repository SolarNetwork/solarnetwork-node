/* ==================================================================
 * BasicWeatherUndergoundClient.java - 7/04/2017 4:32:46 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.weather.wu;

import static net.solarnetwork.util.JsonNodeUtils.parseBigDecimalAttribute;
import static net.solarnetwork.util.JsonNodeUtils.parseIntegerAttribute;
import static net.solarnetwork.util.JsonNodeUtils.parseLongAttribute;
import static net.solarnetwork.util.JsonNodeUtils.parseStringAttribute;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.node.domain.AtmosphericDatum;
import net.solarnetwork.node.domain.DayDatum;
import net.solarnetwork.node.domain.GeneralAtmosphericDatum;
import net.solarnetwork.node.domain.GeneralDayDatum;
import net.solarnetwork.support.HttpClientSupport;

/**
 * Basic implementation of {@link WeatherUndergroundClient}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicWeatherUndergoundClient extends HttpClientSupport implements WeatherUndergroundClient {

	/** The default value for the {@code baseUrl} property. */
	public static final String DEFAULT_API_BASE_URL = "http://api.wunderground.com/api";

	/** The default value for the {@code baseAutocompleteUrl} property. */
	public static final String DEFAULT_AUTOCOMPLETE_BASE_URL = "http://autocomplete.wunderground.com/aq";

	private String apiKey;
	private String baseUrl = DEFAULT_API_BASE_URL;
	private String baseAutocompleteUrl = DEFAULT_AUTOCOMPLETE_BASE_URL;
	private ObjectMapper objectMapper = new ObjectMapper();

	public BasicWeatherUndergoundClient() {
		super();
	}

	private String urlForActionPath(String action, String path) {
		return baseUrl + '/' + apiKey + '/' + action + path;
	}

	private String urlForActionsPath(String[] actions, String path) {
		return baseUrl + '/' + apiKey + '/' + StringUtils.arrayToDelimitedString(actions, "/") + path;
	}

	private String urlForAutocomplete(String query, String country) {
		StringBuilder buf = new StringBuilder(baseAutocompleteUrl);
		buf.append("?query=");
		try {
			if ( query != null ) {
				buf.append(URLEncoder.encode(query, "UTF-8"));
			}
			if ( country != null && country.length() > 0 ) {
				buf.append("&c=").append(URLEncoder.encode(country, "UTF-8"));
			}
		} catch ( UnsupportedEncodingException e ) {
			// should not get here ever
		}
		return buf.toString();
	}

	@Override
	public Collection<WeatherUndergroundLocation> findLocationsForIpAddress() {
		final String url = urlForActionPath("geolookup", "/q/autoip.json");
		Collection<WeatherUndergroundLocation> results = new ArrayList<WeatherUndergroundLocation>();
		try {
			URLConnection conn = getURLConnection(url, HTTP_METHOD_GET);
			JsonNode data = objectMapper.readTree(getInputStreamFromURLConnection(conn));
			JsonNode locNode = data.get("location");
			if ( locNode != null ) {
				BasicWeatherUndergroundLocation loc = parseLocation(locNode);
				if ( loc != null ) {
					results.add(loc);
				}
			}
		} catch ( IOException e ) {
			log.warn("Error reading Weather Underground URL [{}]: {}", url, e.getMessage());
		}
		return results;
	}

	@Override
	public Collection<WeatherUndergroundLocation> findLocations(String name, String country) {
		final String url = urlForAutocomplete(name, country);
		Collection<WeatherUndergroundLocation> results = new ArrayList<WeatherUndergroundLocation>();
		try {
			URLConnection conn = getURLConnection(url, HTTP_METHOD_GET);
			JsonNode data = objectMapper.readTree(getInputStreamFromURLConnection(conn));
			JsonNode locArrayNode = data.get("RESULTS");
			if ( locArrayNode != null && locArrayNode.isArray() ) {
				for ( JsonNode locNode : locArrayNode ) {
					BasicWeatherUndergroundLocation loc = parseLocation(locNode);
					if ( loc != null ) {
						results.add(loc);
					}
				}
			}
		} catch ( IOException e ) {
			log.warn("Error reading Weather Underground URL [{}]: {}", url, e.getMessage());
		}
		return results;
	}

	@Override
	public AtmosphericDatum getCurrentConditions(String identifier) {
		final String url = urlForActionPath("conditions", identifier + ".json");
		GeneralAtmosphericDatum result = null;
		try {
			URLConnection conn = getURLConnection(url, HTTP_METHOD_GET);
			JsonNode data = objectMapper.readTree(getInputStreamFromURLConnection(conn));
			JsonNode conditionsNode = data.get("current_observation");
			result = parseConditions(conditionsNode);
		} catch ( IOException e ) {
			log.warn("Error reading Weather Underground URL [{}]: {}", url, e.getMessage());
		}
		return result;
	}

	@Override
	public Collection<AtmosphericDatum> getHourlyForecast(String identifier) {
		final String url = urlForActionPath("hourly", identifier + ".json");
		Collection<AtmosphericDatum> results = new ArrayList<AtmosphericDatum>();
		try {
			URLConnection conn = getURLConnection(url, HTTP_METHOD_GET);
			JsonNode data = objectMapper.readTree(getInputStreamFromURLConnection(conn));
			JsonNode datumArrayNode = data.get("hourly_forecast");
			if ( datumArrayNode != null && datumArrayNode.isArray() ) {
				for ( JsonNode datumNode : datumArrayNode ) {
					GeneralAtmosphericDatum datum = parseHourlyForecast(datumNode);
					if ( datum != null ) {
						results.add(datum);
					}
				}
			}
		} catch ( IOException e ) {
			log.warn("Error reading Weather Underground URL [{}]: {}", url, e.getMessage());
		}
		return results;
	}

	@Override
	public DayDatum getCurrentDay(String identifier) {
		final String url = urlForActionsPath(new String[] { "astronomy", "forecast" },
				identifier + ".json");
		GeneralDayDatum result = null;
		try {
			URLConnection conn = getURLConnection(url, HTTP_METHOD_GET);
			JsonNode data = objectMapper.readTree(getInputStreamFromURLConnection(conn));
			result = parseDay(data);
		} catch ( IOException e ) {
			log.warn("Error reading Weather Underground URL [{}]: {}", url, e.getMessage());
		}
		return result;
	}

	@Override
	public Collection<DayDatum> getThreeDayForecast(String identifier) {
		final String url = urlForActionPath("forecast", identifier + ".json");
		Collection<DayDatum> results = Collections.emptyList();
		try {
			URLConnection conn = getURLConnection(url, HTTP_METHOD_GET);
			JsonNode data = objectMapper.readTree(getInputStreamFromURLConnection(conn));
			results = parseForecasts(data.get("forecast"));
		} catch ( IOException e ) {
			log.warn("Error reading Weather Underground URL [{}]: {}", url, e.getMessage());
		}
		return results;
	}

	@Override
	public Collection<DayDatum> getTenDayForecast(String identifier) {
		final String url = urlForActionPath("/forecast10day", identifier + ".json");
		Collection<DayDatum> results = new ArrayList<DayDatum>();
		try {
			URLConnection conn = getURLConnection(url, HTTP_METHOD_GET);
			JsonNode data = objectMapper.readTree(getInputStreamFromURLConnection(conn));
			results = parseForecasts(data.get("forecast"));
		} catch ( IOException e ) {
			log.warn("Error reading Weather Underground URL [{}]: {}", url, e.getMessage());
		}
		return results;
	}

	private Collection<DayDatum> parseForecasts(JsonNode node) {
		if ( node == null ) {
			return Collections.emptyList();
		}
		JsonNode simpleForecast = node.get("simpleforecast");
		if ( simpleForecast == null ) {
			return Collections.emptyList();
		}
		JsonNode dayArrayNode = simpleForecast.get("forecastday");
		if ( dayArrayNode == null || !dayArrayNode.isArray() ) {
			return Collections.emptyList();
		}
		Collection<DayDatum> results = new ArrayList<DayDatum>();
		final int dayCount = dayArrayNode.size();
		for ( int i = 0; i < dayCount; i++ ) {
			GeneralDayDatum day = parseForecast(node, i);
			if ( day != null ) {
				results.add(day);
			}
		}
		return results;
	}

	private GeneralDayDatum parseDay(JsonNode node) {
		if ( node == null ) {
			return null;
		}
		GeneralDayDatum datum = parseForecast(node.get("forecast"), 0);
		if ( datum == null ) {
			return null;
		}

		JsonNode moonNode = node.get("moon_phase");
		datum.setMoonrise(parseHourMinuteNode(moonNode.get("moonrise")));
		datum.setMoonset(parseHourMinuteNode(moonNode.get("moonset")));
		datum.setSunrise(parseHourMinuteNode(moonNode.get("sunrise")));
		datum.setSunset(parseHourMinuteNode(moonNode.get("sunset")));

		return datum;
	}

	private LocalTime parseHourMinuteNode(JsonNode node) {
		if ( node == null ) {
			return null;
		}
		Integer hour = parseIntegerAttribute(node, "hour");
		Integer min = parseIntegerAttribute(node, "minute");
		return (hour != null && min != null ? new LocalTime(hour.intValue(), min.intValue()) : null);
	}

	private GeneralDayDatum parseForecast(final JsonNode node, final int dayOffset) {
		if ( node == null ) {
			return null;
		}
		GeneralDayDatum datum = new GeneralDayDatum();

		JsonNode forecastNode = node.get("simpleforecast");
		if ( forecastNode == null ) {
			return null;
		}

		JsonNode dayArrayNode = forecastNode.get("forecastday");
		if ( dayArrayNode == null || !dayArrayNode.isArray() ) {
			return null;
		}
		JsonNode dayNode = dayArrayNode.get(dayOffset);

		JsonNode dateNode = dayNode.get("date");
		String tz = parseStringAttribute(dateNode, "tz_long");
		Long epoch = parseLongAttribute(dateNode, "epoch");
		if ( tz != null && epoch != null ) {
			LocalDate date = new LocalDate(epoch.longValue() * 1000, DateTimeZone.forID(tz));
			datum.setCreated(date.toDate());
		}

		datum.setRain(parseIntegerAttribute(dayNode.get("qpf_allday"), "mm"));

		datum.setSkyConditions(parseStringAttribute(dayNode, "conditions"));

		JsonNode snowNode = dayNode.get("snow_allday");
		if ( snowNode != null ) {
			Integer snowCm = parseIntegerAttribute(snowNode, "cm");
			if ( snowCm != null ) {
				// convert snow to mm
				datum.setSnow(snowCm.intValue() * 10);
			}
		}

		JsonNode tempNode = dayNode.get("high");
		if ( tempNode != null ) {
			datum.setTemperatureMaximum(parseBigDecimalAttribute(tempNode, "celsius"));
		}

		tempNode = dayNode.get("low");
		if ( tempNode != null ) {
			datum.setTemperatureMinimum(parseBigDecimalAttribute(tempNode, "celsius"));
		}

		JsonNode windNode = dayNode.get("avewind");
		if ( windNode != null ) {
			datum.setWindDirection(parseIntegerAttribute(windNode, "degrees"));
			datum.setWindSpeed(parseWindSpeed(windNode, "kph"));
		}

		JsonNode txtNode = node.get("txt_forecast");
		if ( txtNode != null ) {
			JsonNode txtDayArrayNode = txtNode.get("forecastday");
			if ( txtDayArrayNode != null && txtDayArrayNode.isArray() ) {
				// txt day nodes come in pairs, one for day one for night; we only get day values
				JsonNode txtDayNode = txtDayArrayNode.get(dayOffset * 2);
				if ( txtDayNode != null ) {
					// TODO: support imperial description gathering via class property
					datum.setBriefOverview(parseStringAttribute(txtDayNode, "fcttext_metric"));
				}
			}
		}

		return datum;
	}

	private BigDecimal parseWindSpeed(JsonNode windNode, String key) {
		if ( windNode == null ) {
			return null;
		}
		BigDecimal wspeed = parseBigDecimalAttribute(windNode, key);
		if ( wspeed == null ) {
			return null;
		}
		// convert kph to mps
		return wspeed.multiply(new BigDecimal(10)).divide(new BigDecimal(36), 3, RoundingMode.HALF_UP);
	}

	private GeneralAtmosphericDatum parseHourlyForecast(JsonNode node) {
		if ( node == null ) {
			return null;
		}
		JsonNode objNode = node.get("FCTTIME");
		if ( objNode == null ) {
			return null;
		}
		GeneralAtmosphericDatum datum = new GeneralAtmosphericDatum();

		Long ts = parseLongAttribute(objNode, "epoch");
		if ( ts != null ) {
			datum.setCreated(new Date(ts.longValue() * 1000));
		}
		datum.addTag(AtmosphericDatum.TAG_FORECAST);

		objNode = node.get("mslp");
		Integer pres = parseIntegerAttribute(objNode, "metric");
		if ( pres != null ) {
			// convert to pascals
			datum.setAtmosphericPressure(pres.intValue() * 100);
		}

		objNode = node.get("dewpoint");
		datum.setDewPoint(parseBigDecimalAttribute(objNode, "metric"));

		datum.setHumidity(parseIntegerAttribute(node, "humidity"));

		objNode = node.get("qpf");
		datum.setRain(parseIntegerAttribute(objNode, "metric"));

		datum.setSkyConditions(parseStringAttribute(node, "condition"));

		objNode = node.get("snow");
		datum.setSnow(parseIntegerAttribute(objNode, "metric"));

		objNode = node.get("temp");
		datum.setTemperature(parseBigDecimalAttribute(objNode, "metric"));

		objNode = node.get("wdir");
		datum.setWindDirection(parseIntegerAttribute(objNode, "degrees"));

		objNode = node.get("wspd");
		datum.setWindSpeed(parseWindSpeed(objNode, "metric"));

		return datum;
	}

	private GeneralAtmosphericDatum parseConditions(JsonNode node) {
		if ( node == null ) {
			return null;
		}
		GeneralAtmosphericDatum datum = new GeneralAtmosphericDatum();

		Long ts = parseLongAttribute(node, "observation_epoch");
		if ( ts != null ) {
			datum.setCreated(new Date(ts.longValue() * 1000));
		}

		Integer mb = parseIntegerAttribute(node, "pressure_mb");
		if ( mb != null ) {
			// convert millibar to pascals
			datum.setAtmosphericPressure(mb.intValue() * 100);
		}

		BigDecimal dp = parseBigDecimalAttribute(node, "dewpoint_c");
		if ( dp != null ) {
			datum.setDewPoint(dp);
		}

		String hum = parseStringAttribute(node, "relative_humidity");
		if ( hum != null ) {
			if ( hum.endsWith("%") ) {
				hum = hum.substring(0, hum.length() - 1);
			}
			try {
				datum.setHumidity(Integer.valueOf(hum));
			} catch ( NumberFormatException e ) {
				log.warn("Unable to parse 'relative_humidity' attribute [{}]", hum);
			}
		}

		datum.setRain(parseIntegerAttribute(node, "precip_1hr_metric"));

		datum.setSkyConditions(parseStringAttribute(node, "weather"));
		datum.setTemperature(parseBigDecimalAttribute(node, "temp_c"));

		BigDecimal vis = parseBigDecimalAttribute(node, "visibility_km");
		if ( vis != null ) {
			// convert km to meters
			datum.setVisibility(vis.multiply(new BigDecimal(1000)).intValue());
		}

		datum.setWindDirection(parseIntegerAttribute(node, "wind_degrees"));
		datum.setWindSpeed(parseWindSpeed(node, "wind_kph"));

		return datum;
	}

	private BasicWeatherUndergroundLocation parseLocation(JsonNode node) {
		if ( node == null ) {
			return null;
		}
		BasicWeatherUndergroundLocation loc = new BasicWeatherUndergroundLocation();
		loc.setIdentifier(parseStringAttribute(node, "l"));

		loc.setCountry(parseStringAttribute(node, "country"));
		loc.setStateOrProvince(parseStringAttribute(node, "state"));
		loc.setLocality(parseStringAttribute(node, "city"));
		loc.setPostalCode(parseStringAttribute(node, "zip"));
		if ( "00000".equals(loc.getPostalCode()) ) {
			loc.setPostalCode(null);
		}
		loc.setTimeZoneId(parseStringAttribute(node, "tz_long"));

		loc.setLatitude(parseBigDecimalAttribute(node, "lat"));
		loc.setLongitude(parseBigDecimalAttribute(node, "lon"));

		// the autocomplete endpoint has the following
		loc.setName(parseStringAttribute(node, "name")); // from autocomplete endpoint
		if ( loc.getCountry() == null ) {
			loc.setCountry(parseStringAttribute(node, "c"));
		}
		if ( loc.getTimeZoneId() == null ) {
			loc.setTimeZoneId(parseStringAttribute(node, "tz"));
		}

		return loc;
	}

	/**
	 * Set the Weather Underground API key to use.
	 * 
	 * @param apiKey
	 *        the apiKey to set
	 */
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	/**
	 * Set the base URL to use. This defaults to {@link #DEFAULT_API_BASE_URL}
	 * which should be sufficient for most cases.
	 * 
	 * @param baseUrl
	 *        the baseUrl to set
	 */
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	/**
	 * Set the base autocomplete URL to use. This defaults to
	 * {@link #DEFAULT_AUTOCOMPLETE_BASE_URL} which should be sufficient for
	 * most cases.
	 * 
	 * @param baseAutocompleteUrl
	 *        the baseAutocompleteUrl to set
	 */
	public void setBaseAutocompleteUrl(String baseAutocompleteUrl) {
		this.baseAutocompleteUrl = baseAutocompleteUrl;
	}

	/**
	 * Set the ObjectMapper to use for JSON parsing.
	 * 
	 * @param objectMapper
	 *        the objectMapper to set
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

}
