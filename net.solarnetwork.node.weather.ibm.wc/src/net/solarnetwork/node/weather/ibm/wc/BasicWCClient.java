
package net.solarnetwork.node.weather.ibm.wc;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.joda.time.LocalTime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.node.domain.GeneralDayDatum;
import net.solarnetwork.node.support.HttpClientSupport;

public class BasicWCClient extends HttpClientSupport implements WCClient {

	/** The default value for the {@code baseUrl} property. */
	public static final String DEFAULT_BASE_URL = "https://api.weather.com";

	/** The daily forecast url format */
	public static final String DEFAULT_DAILY_FORECAST_URL = "v3/wx/forecast/daily/%s";

	/** The default output format */
	public static final String DEFAULT_FORMAT = "format=json";

	/** The default value for the {@code locationTemplate} property */
	public static final String DEFAULT_LOCATION_TEMPLATE = "icaoCode=%s";

	/** The hourly forecast url format */
	public static final String DEFAULT_HOURLY_FORECAST_URL = "v3/wx/forecast/hourly/%s";

	/** The default value for the {@code apiKeyTemplate} property. */
	public static final String DEFAULT_API_KEY_TEMPLATE = "apiKey=%s";

	/** The default value for the {@code languageTemplate} property. */
	public static final String DEFAULT_LANGUAGE_TEMPLATE = "language=en-US";

	/** The default value for the {@code units} property. */
	public static final String DEFAULT_UNITS = "units=e";

	private String baseUrl;
	private String dailyForecastUrl;
	private String hourlyForecastUrl;
	private String defaultFormat;
	private String apiKeyTemplate;
	private String locationTemplate;
	private String languageTemplate;
	private String units;
	private Boolean test;

	private ObjectMapper objectMapper;

	public BasicWCClient() {
		this.baseUrl = DEFAULT_BASE_URL;
		this.dailyForecastUrl = DEFAULT_DAILY_FORECAST_URL;
		this.hourlyForecastUrl = DEFAULT_HOURLY_FORECAST_URL;
		this.defaultFormat = DEFAULT_FORMAT;
		this.apiKeyTemplate = DEFAULT_API_KEY_TEMPLATE;
		this.locationTemplate = DEFAULT_LOCATION_TEMPLATE;
		this.languageTemplate = DEFAULT_LANGUAGE_TEMPLATE;
		this.units = DEFAULT_UNITS;
		this.test = false;
	}

	private String getURL(String requestURL, String location, String apiKey, String datumPeriod) {
		if ( test ) {
			return this.baseUrl + "/" + String.format(requestURL, datumPeriod);
		}
		return this.baseUrl + "/" + String.format(requestURL, datumPeriod) + "?"
				+ String.format(this.locationTemplate, location) + "&" + this.languageTemplate + "&"
				+ this.defaultFormat + "&" + this.units + "&"
				+ String.format(this.apiKeyTemplate, apiKey);
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
		return new BigDecimal(n.asLong());
	}

	private LocalTime parseTimeValue(JsonNode n) {
		if ( n.isNull() ) {
			return null;
		}
		return new LocalTime(n.asLong() * 1000);

	}

	private Integer parseInt(JsonNode n) {
		if ( n.isNull() ) {
			return null;
		}
		return n.asInt();
	}

	//TODO valid time UTC
	@Override
	public Collection<GeneralDayDatum> readDailyForecast(String locationIdentifier, String apiKey,
			String datumPeriod) {

		if ( locationIdentifier == null ) {
			return null;
		}
		final List<GeneralDayDatum> result = new ArrayList<GeneralDayDatum>();
		final String url = getURL(this.dailyForecastUrl, locationIdentifier, apiKey, datumPeriod);
		JsonNode root;
		try {
			log.debug("opening IBM Weather API connection");
			log.debug(url);
			URLConnection conn = getURLConnection(url, HTTP_METHOD_GET);
			root = getObjectMapper().readTree(getInputStreamFromURLConnection(conn));
		} catch ( IOException e ) {
			log.warn("Error reading WC URL [{}]: {}", url, e.getMessage());
			return result;
		}

		for ( JsonNode n : root.get("dayOfWeek") ) {
			result.add(new GeneralDayDatum());
		}

		for ( int i = 0; i < result.size(); i++ ) {

			GeneralDayDatum current = result.get(i);
			JsonNode data = root.get("sunriseTimeUtc");
			if ( data.isArray() ) {
				current.setSunrise(parseTimeValue(data.get(i)));
			}
			data = root.get("sunsetTimeUtc");
			if ( data.isArray() ) {
				current.setSunset(parseTimeValue(data.get(i)));

			}
			data = root.get("moonriseTimeUtc");
			if ( data.isArray() ) {
				current.setMoonrise(parseTimeValue(data.get(i)));
			}
			data = root.get("moonsetTimeUtc");
			if ( data.isArray() ) {
				current.setMoonset(parseTimeValue(data.get(i)));
			}
			data = root.get("validTimeUtc");
			if ( data.isArray() ) {
				current.setCreated(new Date(data.get(i).asLong() * 1000));
			}
			data = root.get("temperatureMax");
			if ( data.isArray() ) {
				current.setTemperatureMaximum(parseBigDecimal(data.get(i)));
			}
			data = root.get("temperatureMin");
			if ( data.isArray() ) {
				current.setTemperatureMinimum(new BigDecimal(data.get(i).asLong()));
			}

		}

		log.debug("Finished IBM Weather retrieval");
		return result;
	}

	@Override
	public Collection<WCHourlyDatum> readHourlyForecast(String locationIdentifier, String apiKey,
			String datumPeriod) {
		if ( locationIdentifier == null ) {
			return null;
		}
		final List<WCHourlyDatum> result = new ArrayList<WCHourlyDatum>();
		final String url = getURL(this.hourlyForecastUrl, locationIdentifier, apiKey, datumPeriod);
		JsonNode root;
		try {
			log.debug("opening IBM Weather API connection");
			log.debug(url);
			URLConnection conn = getURLConnection(url, HTTP_METHOD_GET);
			root = getObjectMapper().readTree(getInputStreamFromURLConnection(conn));
		} catch ( IOException e ) {
			log.warn("Error reading WC URL [{}]: {}", url, e.getMessage());
			return result;
		}

		for ( JsonNode n : root.get("dayOfWeek") ) {
			result.add(new WCHourlyDatum());
		}

		for ( int i = 0; i < result.size(); i++ ) {

			WCHourlyDatum current = result.get(i);
			JsonNode data = root.get("validTimeUtc");
			if ( data.isArray() ) {
				current.setCreated(new Date(data.get(i).asLong() * 1000));
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

			//rainfall?
			/*
			 * data = root.get("precipChance"); if(data.isArray()) {
			 * current.setRain(parseInt()); }
			 */

		}

		log.debug("Finished IBM Hourly Weather retrieval");
		return result;
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	/**
	 * Set the {@link ObjectMapper} to use for parsing JSON.
	 * 
	 * @param objectMapper
	 *        The object mapper.
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
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

	public Boolean getTest() {
		return test;
	}

	public void setTest(Boolean test) {
		this.test = test;
	}

}
