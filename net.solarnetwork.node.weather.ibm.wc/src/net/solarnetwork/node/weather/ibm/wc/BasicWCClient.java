/* ===================================================================
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
 * ===================================================================
 */

package net.solarnetwork.node.weather.ibm.wc;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import com.fasterxml.jackson.databind.JsonNode;
import net.solarnetwork.node.domain.GeneralDayDatum;
import net.solarnetwork.node.support.JsonHttpClientSupport;

/**
 * This client implemetation connects to the IBM Weather Channel endpoints, and
 * stores the information in datum.
 * 
 * @author matt frost
 *
 */
public class BasicWCClient extends JsonHttpClientSupport implements WCClient {

	/** The default value for the {@code baseUrl} property. */
	public static final String DEFAULT_BASE_URL = "https://api.weather.com";

	/** The daily forecast url format */
	public static final String DEFAULT_DAILY_FORECAST_URL = "v3/wx/forecast/daily/%s?icaoCode=%s"
			+ "&language=%s&format=%s&units=%s&apiKey=%s";

	/** The default output format */
	public static final String DEFAULT_FORMAT = "json";

	/** The default value for the {@code locationTemplate} property */
	public static final String DEFAULT_LOCATION = "AU";

	/** The hourly forecast url format */
	public static final String DEFAULT_HOURLY_FORECAST_URL = "v3/wx/forecast/hourly/%s?icaoCode=%s"
			+ "&language=%s&format=%s&units=%s&apiKey=%s";

	/** The default value for the {@code apiKeyTemplate} property. */
	public static final String DEFAULT_API_KEY = "abc123";

	/** The default value for the {@code languageTemplate} property. */
	public static final String DEFAULT_LANGUAGE = "en-US";

	/** The default value for the {@code units} property. */
	public static final String DEFAULT_UNITS = "e";

	public static final String DEFAULT_TIMESTAMP_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssX";

	public static final boolean DEFAULT_COMPRESSION = true;

	private String baseUrl;
	private String dailyForecastUrl;
	private String hourlyForecastUrl;
	private String format;
	private String language;
	private String units;
	private DateTimeFormatter dateFormat;

	public BasicWCClient() {
		this.baseUrl = DEFAULT_BASE_URL;
		this.dailyForecastUrl = DEFAULT_DAILY_FORECAST_URL;
		this.hourlyForecastUrl = DEFAULT_HOURLY_FORECAST_URL;
		this.format = DEFAULT_FORMAT;
		this.language = DEFAULT_LANGUAGE;
		this.dateFormat = ISODateTimeFormat.dateTimeParser().withOffsetParsed();
		this.units = DEFAULT_UNITS;

		// set default compression
		this.setCompress(DEFAULT_COMPRESSION);
	}

	private String getURL(String requestURL, String location, String apiKey, String datumPeriod) {

		return this.baseUrl + "/"
				+ String.format(requestURL, datumPeriod, location, language, format, units, apiKey);
	}

	/**
	 * For the daily forecast, each part of the data (ie. max temp, min temp,
	 * narrative) is an array, with each entry corresponding with a day of the
	 * week. The more detailed features in "daypart" such as cloud cover are
	 * split into day and night, meaning two entries for each day.
	 */
	private BigDecimal parseBigDecimal(JsonNode n) {
		if ( n.isNull() ) {
			return null;
		}
		return n.decimalValue();
	}

	private LocalTime parseTimeValue(JsonNode n) {
		if ( n == null || n.isNull() ) {
			return null;
		}
		try {
			return this.dateFormat.parseDateTime(n.asText()).toLocalTime();
		} catch ( IllegalArgumentException e ) {
			return null;
		}

	}

	private Date parseDateValue(JsonNode n) {
		if ( n == null || n.isNull() ) {
			return null;
		}

		return this.dateFormat.parseDateTime(n.asText()).toDate();

	}

	private Integer parseInt(JsonNode n) {
		if ( n.isNull() ) {
			return null;
		}
		return n.asInt();
	}

	@Override
	public Collection<GeneralDayDatum> readDailyForecast(String locationIdentifier, String apiKey,
			DailyDatumPeriod datumPeriod) {

		if ( locationIdentifier == null ) {
			return null;
		}
		final List<GeneralDayDatum> result = new ArrayList<GeneralDayDatum>();
		final String url = getURL(this.dailyForecastUrl, locationIdentifier, apiKey,
				datumPeriod.getPeriod());
		JsonNode root;
		try {
			log.debug("opening IBM Weather API connection at URL {}", url);
			root = getObjectMapper().readTree(jsonGET(url));
		} catch ( IOException e ) {
			log.warn("Error reading WC URL [{}]: {}", url, e.getMessage());
			return result;
		}

		for ( int i = 0; i < root.get("sunriseTimeLocal").size(); i++ ) {

			GeneralDayDatum current = new GeneralDayDatum();
			JsonNode data = root.get("sunriseTimeLocal");
			if ( data.isArray() ) {
				LocalTime t = parseTimeValue(data.get(i));
				if ( t != null ) {
					current.setSunrise(t);
				}

			}
			data = root.get("sunsetTimeLocal");
			if ( data.isArray() ) {
				LocalTime t = parseTimeValue(data.get(i));
				if ( t != null ) {
					current.setSunset(t);
				}

			}
			data = root.get("moonriseTimeLocal");
			if ( data.isArray() ) {
				LocalTime t = parseTimeValue(data.get(i));
				if ( t != null ) {
					current.setMoonrise(t);
				}

			}
			data = root.get("moonsetTimeLocal");
			if ( data.isArray() ) {
				LocalTime t = parseTimeValue(data.get(i));
				if ( t != null ) {
					current.setMoonset(t);
				}
			}
			data = root.get("validTimeLocal");
			if ( data.isArray() ) {
				Date t = parseDateValue(data.get(i));
				if ( t != null ) {
					current.setCreated(t);
				}
			}
			data = root.get("temperatureMax");
			if ( data.isArray() ) {
				current.setTemperatureMaximum(parseBigDecimal(data.get(i)));
			}
			data = root.get("temperatureMin");
			if ( data.isArray() ) {
				current.setTemperatureMinimum(parseBigDecimal(data.get(i)));
			}
			result.add(current);

		}

		log.debug("Finished IBM Weather retrieval");
		return result;
	}

	@Override
	public Collection<WCHourlyDatum> readHourlyForecast(String locationIdentifier, String apiKey,
			HourlyDatumPeriod datumPeriod) {
		if ( locationIdentifier == null ) {
			return null;
		}
		final List<WCHourlyDatum> result = new ArrayList<WCHourlyDatum>();
		final String url = getURL(this.hourlyForecastUrl, locationIdentifier, apiKey,
				datumPeriod.getPeriod());
		JsonNode root;
		try {
			log.debug("opening IBM Weather API connection URL: [{}]", url);
			root = getObjectMapper().readTree(jsonGET(url));
		} catch ( IOException e ) {
			log.warn("Error reading WC URL [{}]: {}", url, e.getMessage());
			return result;
		}

		for ( int i = 0; i < root.get("validTimeLocal").size(); i++ ) {

			WCHourlyDatum current = new WCHourlyDatum();
			JsonNode data = root.get("validTimeLocal");
			if ( data.isArray() ) {
				Date t = parseDateValue(data.get(i));
				if ( t != null ) {
					current.setCreated(t);
				}
			}
			data = root.get("temperature");
			if ( data.isArray() ) {
				current.setTemperature(parseBigDecimal(data.get(i)));
			}
			data = root.get("visibility");
			if ( data.isArray() ) {
				current.setVisibility(parseInt(data.get(i)));
			}
			data = root.get("windDirection");
			if ( data.isArray() ) {
				current.setWindDirection(parseInt(data.get(i)));
			}
			data = root.get("windSpeed");
			if ( data.isArray() ) {
				current.setWindSpeed(parseBigDecimal(data.get(i)));
			}
			data = root.get("relativeHumidity");
			if ( data.isArray() ) {
				current.setHumidity(parseInt(data.get(i)));
			}
			data = root.get("cloudCover");
			if ( data.isArray() ) {
				current.setCloudCover(parseBigDecimal(data.get(i)));
			}
			result.add(current);

		}

		log.debug("Finished IBM Hourly Weather retrieval");
		return result;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getDailyForecastUrl() {
		return dailyForecastUrl;
	}

	public void setDailyForecastUrl(String dailyForecastUrl) {
		this.dailyForecastUrl = dailyForecastUrl;
	}

	public String getHourlyForecastUrl() {
		return hourlyForecastUrl;
	}

	public void setHourlyForecastUrl(String hourlyForecastUrl) {
		this.hourlyForecastUrl = hourlyForecastUrl;
	}

}
