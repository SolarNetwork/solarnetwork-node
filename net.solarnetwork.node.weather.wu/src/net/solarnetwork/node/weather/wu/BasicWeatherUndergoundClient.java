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
import static net.solarnetwork.util.JsonNodeUtils.parseStringAttribute;
import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.support.HttpClientSupport;

/**
 * Basic implementation of {@link WeatherUndergroundClient}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicWeatherUndergoundClient extends HttpClientSupport implements WeatherUndergroundClient {

	/** The default value for the {@code baseUrl} property. */
	public static final String DEFAULT_API_BASE_URL = "http://api.wunderground.com/api/";

	private String apiKey;
	private String baseUrl = DEFAULT_API_BASE_URL;
	private ObjectMapper objectMapper = new ObjectMapper();

	public BasicWeatherUndergoundClient() {
		super();
	}

	private String urlForActionPath(String action, String path) {
		return baseUrl + '/' + apiKey + action + path;
	}

	@Override
	public Collection<WeatherUndergroundLocation> findLocationsForIpAddress() {
		final String url = urlForActionPath("/geolookup", "/q/autoip.json");
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

	/*-
	 * Parse a node like:
	 * {
	    "type": "INTLCITY",
	    "country": "NZ",
	    "country_iso3166": "NZ",
	    "country_name": "New Zealand",
	    "state": "WGN",
	    "city": "Wellington",
	    "tz_short": "NZST",
	    "tz_long": "Pacific/Auckland",
	    "lat": "-41.29000092",
	    "lon": "174.77999878",
	    "zip": "00000",
	    "magic": "2",
	    "wmo": "93436",
	    "l": "/q/zmw:00000.2.93436"
	   }
	 */
	private BasicWeatherUndergroundLocation parseLocation(JsonNode node) {
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
	 * Set the ObjectMapper to use for JSON parsing.
	 * 
	 * @param objectMapper
	 *        the objectMapper to set
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

}
