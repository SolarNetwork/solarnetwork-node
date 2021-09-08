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
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.AtmosphericDatum;
import net.solarnetwork.node.domain.datum.DayDatum;
import net.solarnetwork.node.domain.datum.SimpleAtmosphericDatum;
import net.solarnetwork.node.domain.datum.SimpleDayDatum;
import net.solarnetwork.node.service.support.JsonHttpClientSupport;

/**
 * This client implementation connects to the IBM Weather Channel endpoints, and
 * parses the information as datum.
 * 
 * @author matt frost
 * @version 2.0
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

	/**
	 * The datum instantaneous property key for cloud cover.
	 * 
	 * @since 2.0
	 */
	public static final String CLOUD_COVER_PROPERTY = "cloudCover";

	/** The default value for the {@code units} property. */
	public static final MeasurementUnit DEFAULT_UNITS = MeasurementUnit.Metric;

	/**
	 * The date time format used by the Weather Channel API.
	 */
	// @formatter:off
	public static final DateTimeFormatter OFFSET_DATE_TIME;
	static {
		OFFSET_DATE_TIME = new DateTimeFormatterBuilder()
				.parseCaseInsensitive()
				.parseStrict()
				.append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
				.appendOffset("+HHMM", "Z")
				.toFormatter();
	}
	// @formatter:on

	private String baseUrl;
	private String dailyForecastUrl;
	private String hourlyForecastUrl;
	private final String format;
	private final String language;
	private MeasurementUnit units;

	public BasicWCClient() {
		this.baseUrl = DEFAULT_BASE_URL;
		this.dailyForecastUrl = DEFAULT_DAILY_FORECAST_URL;
		this.hourlyForecastUrl = DEFAULT_HOURLY_FORECAST_URL;
		this.format = DEFAULT_FORMAT;
		this.language = DEFAULT_LANGUAGE;
		this.units = DEFAULT_UNITS;

		// set default compression
		this.setCompress(true);
	}

	private String getURL(String requestURL, String location, String apiKey, String datumPeriod) {
		return this.baseUrl + "/" + String.format(requestURL, datumPeriod, location, language, format,
				units.getKey(), apiKey);
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
			return OFFSET_DATE_TIME.parse(n.asText(), LocalTime::from);
		} catch ( IllegalArgumentException e ) {
			return null;
		}

	}

	private Instant parseDateValue(JsonNode n) {
		if ( n == null || n.isNull() ) {
			return null;
		}

		return OFFSET_DATE_TIME.parse(n.asText(), Instant::from);

	}

	private Integer parseInt(JsonNode n) {
		if ( n.isNull() ) {
			return null;
		}
		return n.asInt();
	}

	private JsonNode requestWcUrl(String url) {
		log.debug("Querying IBM Weather API with URL {}", url);

		try {
			return getObjectMapper().readTree(jsonGET(url));
		} catch ( IOException e ) {
			log.warn("Communication error requesting IBM Weather API URL [{}]: {}", url, e.getMessage());
			return null;
		}
	}

	@Override
	public Collection<DayDatum> readDailyForecast(String locationIdentifier, String apiKey,
			DailyDatumPeriod datumPeriod) {
		if ( locationIdentifier == null || locationIdentifier.isEmpty() || apiKey == null
				|| apiKey.isEmpty() || datumPeriod == null ) {
			return Collections.emptyList();
		}

		final List<DayDatum> result = new ArrayList<>();
		final String url = getURL(this.dailyForecastUrl, locationIdentifier, apiKey,
				datumPeriod.getPeriod());
		final JsonNode root = requestWcUrl(url);
		JsonNode data;
		for ( int i = 0; i < root.get("sunriseTimeLocal").size(); i++ ) {
			Instant ts = null;
			data = root.get("validTimeLocal");
			if ( data.isArray() ) {
				ts = parseDateValue(data.get(i));
			}

			SimpleDayDatum current = new SimpleDayDatum(null, ts, new DatumSamples());
			data = root.get("sunriseTimeLocal");
			if ( data.isArray() ) {
				LocalTime t = parseTimeValue(data.get(i));
				if ( t != null ) {
					current.setSunriseTime(t);
				}

			}
			data = root.get("sunsetTimeLocal");
			if ( data.isArray() ) {
				LocalTime t = parseTimeValue(data.get(i));
				if ( t != null ) {
					current.setSunsetTime(t);
				}

			}
			data = root.get("moonriseTimeLocal");
			if ( data.isArray() ) {
				LocalTime t = parseTimeValue(data.get(i));
				if ( t != null ) {
					current.setMoonriseTime(t);
				}

			}
			data = root.get("moonsetTimeLocal");
			if ( data.isArray() ) {
				LocalTime t = parseTimeValue(data.get(i));
				if ( t != null ) {
					current.setMoonsetTime(t);
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

		log.debug("Finished IBM Weather API retrieval for {}", url);
		return result;
	}

	@Override
	public Collection<AtmosphericDatum> readHourlyForecast(String locationIdentifier, String apiKey,
			HourlyDatumPeriod datumPeriod) {
		if ( locationIdentifier == null || locationIdentifier.isEmpty() || apiKey == null
				|| apiKey.isEmpty() || datumPeriod == null ) {
			return Collections.emptyList();
		}

		final List<AtmosphericDatum> result = new ArrayList<>();
		final String url = getURL(this.hourlyForecastUrl, locationIdentifier, apiKey,
				datumPeriod.getPeriod());
		final JsonNode root = requestWcUrl(url);

		JsonNode data;
		for ( int i = 0; i < root.get("validTimeLocal").size(); i++ ) {
			Instant ts = null;
			data = root.get("validTimeLocal");
			if ( data.isArray() ) {
				ts = parseDateValue(data.get(i));
			}
			SimpleAtmosphericDatum current = new SimpleAtmosphericDatum(null, ts, new DatumSamples());
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
				current.getSamples().putInstantaneousSampleValue(CLOUD_COVER_PROPERTY,
						parseBigDecimal(data.get(i)));
			}
			result.add(current);
		}

		log.debug("Finished IBM Weather API retrieval for {}", url);
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

	/**
	 * Get the measurement unit to use.
	 * 
	 * @return the measurement unit
	 */
	public MeasurementUnit getUnits() {
		return units;
	}

	/**
	 * Set the measurement unit to use.
	 * 
	 * @param units
	 *        the unit to use
	 */
	public void setUnits(MeasurementUnit units) {
		this.units = units;
	}

	/**
	 * Get the measurement unit key value.
	 * 
	 * @return the measurement unit key value
	 */
	public String getUnitsKey() {
		return units.getKey();
	}

	/**
	 * Set the measurement unit via a key value.
	 * 
	 * @param key
	 *        the key value to set; {@link MeasurementUnit#Metric} will be set
	 *        if {@code key} is not supported
	 */
	public void setUnitsKey(String key) {
		MeasurementUnit u;
		try {
			u = MeasurementUnit.forKey(key);
		} catch ( IllegalArgumentException e ) {
			u = DEFAULT_UNITS;
		}
		setUnits(u);
	}

}
