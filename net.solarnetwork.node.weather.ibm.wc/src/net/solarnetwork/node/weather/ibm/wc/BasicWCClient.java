
package net.solarnetwork.node.weather.ibm.wc;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.joda.time.LocalTime;
import com.fasterxml.jackson.databind.JsonNode;
import net.solarnetwork.node.domain.GeneralDayDatum;
import net.solarnetwork.node.support.JsonHttpClientSupport;

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

	public static final boolean DEFAULT_COMPRESSION = true;

	private String baseUrl;
	private String dailyForecastUrl;
	private String hourlyForecastUrl;
	private String format;
	private String language;
	private String units;

	public BasicWCClient() {
		this.baseUrl = DEFAULT_BASE_URL;
		this.dailyForecastUrl = DEFAULT_DAILY_FORECAST_URL;
		this.hourlyForecastUrl = DEFAULT_HOURLY_FORECAST_URL;
		this.format = DEFAULT_FORMAT;
		this.language = DEFAULT_LANGUAGE;
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
		return new BigDecimal(n.asLong());
	}

	private LocalTime parseTimeValue(JsonNode n) {
		if ( n == null || n.isNull() ) {
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
			DailyDatumPeriod datumPeriod) {

		if ( locationIdentifier == null ) {
			return null;
		}
		final List<GeneralDayDatum> result = new ArrayList<GeneralDayDatum>();
		final String url = getURL(this.dailyForecastUrl, locationIdentifier, apiKey,
				datumPeriod.toString());
		JsonNode root;
		try {
			log.debug("opening IBM Weather API connection at URL {}", url);
			root = getObjectMapper().readTree(jsonGET(url));
		} catch ( IOException e ) {
			log.warn("Error reading WC URL [{}]: {}", url, e.getMessage());
			return result;
		}

		for ( int i = 0; i < root.get("sunriseTimeUtc").size(); i++ ) {

			GeneralDayDatum current = new GeneralDayDatum();
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
				datumPeriod.toString());
		JsonNode root;
		try {
			log.debug("opening IBM Weather API connection URL: [{}]", url);
			root = getObjectMapper().readTree(jsonGET(url));
		} catch ( IOException e ) {
			log.warn("Error reading WC URL [{}]: {}", url, e.getMessage());
			return result;
		}

		for ( int i = 0; i < root.get("validTimeUtc").size(); i++ ) {

			WCHourlyDatum current = new WCHourlyDatum();
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
			result.add(current);

			//rainfall?
			/*
			 * data = root.get("precipChance"); if(data.isArray()) {
			 * current.setRain(parseInt()); }
			 */

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
