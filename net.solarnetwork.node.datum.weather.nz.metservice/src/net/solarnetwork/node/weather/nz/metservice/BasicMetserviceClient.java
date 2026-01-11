/* ==================================================================
 * BasicMetserviceClient.java - 28/05/2016 1:37:46 pm
 *
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.weather.nz.metservice;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static net.solarnetwork.codec.JsonUtils.parseBigDecimalAttribute;
import static net.solarnetwork.codec.JsonUtils.parseDateAttribute;
import static net.solarnetwork.codec.JsonUtils.parseIntegerAttribute;
import static net.solarnetwork.codec.JsonUtils.parseStringAttribute;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.util.FileCopyUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.io.UnicodeReader;
import net.solarnetwork.node.domain.datum.AtmosphericDatum;
import net.solarnetwork.node.domain.datum.DayDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleAtmosphericDatum;
import net.solarnetwork.node.domain.datum.SimpleDayDatum;
import net.solarnetwork.node.service.support.HttpClientSupport;

/**
 * Basic implementation of {@link MetserviceClient}.
 *
 * @author matt
 * @version 2.1
 */
public class BasicMetserviceClient extends HttpClientSupport implements MetserviceClient {

	/** The default value for the {@code baseUrl} property. */
	public static final String DEFAULT_BASE_URL = "https://www.metservice.com/publicData";

	/** The default value for the {@code locationKey} property. */
	public static final String DEFAULT_LOCATION_KEY = "wellington-city";

	/** The default value for the {@code localObsTemplate} property. */
	public static final String DEFAULT_LOCAL_OBS_SET_TEMPLATE = "localObs_%s";

	/** The default value for the {@code localForecastTemplate} property. */
	public static final String DEFAULT_LOCAL_FORECAST_SET_TEMPLATE = "localForecast%s";

	/** The default value for the {@code oneMinObsTemplate} property. */
	public static final String DEFAULT_ONE_MINUTE_OBS_SET_TEMPLATE = "oneMinuteObs_%s";

	/** The default value for the {@code riseSetTemplate} property. */
	public static final String DEFAULT_RISE_SET_TEMPLATE = "riseSet_%s";

	/**
	 * The default value for the {@code hourlyObsAndForecastTemplate} property.
	 */
	public static final String DEFAULT_HOURLY_OBS_AND_FORECAST_TEMPLATE = "hourlyObsAndForecast_%s";

	/** The default value for the {@code dayDateFormat} property. */
	public static final String DEFAULT_DAY_DATE_FORMAT = "d MMMM yyyy";

	/** The default value for the {@code timestampHourDateFormat} property. */
	public static final String DEFAULT_TIMESTAMP_HOUR_DATE_FORMAT = "HH:mm EE d MMM yyyy";

	/** The default value for the {@code timeDateFormat} property. */
	public static final String DEFAULT_TIME_DATE_FORMAT = "h:mma";

	/** The default value for the {@code timestampDateFormat} property. */
	public static final String DEFAULT_TIMESTAMP_DATE_FORMAT = "h:mma EE, d MMM";

	/** The default value for the {@code timeZoneId} property. */
	public static final String DEFAULT_TIME_ZONE_ID = "Pacific/Auckland";

	private String baseUrl;

	private String localObsTemplate;
	private String oneMinuteObsTemplate;
	private String localForecastTemplate;
	private String hourlyObsAndForecastTemplate;
	private String riseSetTemplate;
	private String dayDateFormat;
	private String timeDateFormat;
	private String timestampDateFormat;
	private String timestampHourDateFormat;
	private String timeZoneId;

	private ObjectMapper objectMapper;

	/**
	 * Default constructor.
	 */
	public BasicMetserviceClient() {
		super();
		baseUrl = DEFAULT_BASE_URL;
		localObsTemplate = DEFAULT_LOCAL_OBS_SET_TEMPLATE;
		localForecastTemplate = DEFAULT_LOCAL_FORECAST_SET_TEMPLATE;
		oneMinuteObsTemplate = DEFAULT_ONE_MINUTE_OBS_SET_TEMPLATE;
		hourlyObsAndForecastTemplate = DEFAULT_HOURLY_OBS_AND_FORECAST_TEMPLATE;
		riseSetTemplate = DEFAULT_RISE_SET_TEMPLATE;
		dayDateFormat = DEFAULT_DAY_DATE_FORMAT;
		timestampHourDateFormat = DEFAULT_TIMESTAMP_HOUR_DATE_FORMAT;
		timeDateFormat = DEFAULT_TIME_DATE_FORMAT;
		timestampDateFormat = DEFAULT_TIMESTAMP_DATE_FORMAT;
		timeZoneId = DEFAULT_TIME_ZONE_ID;
	}

	/**
	 * Get a date formatter suitable for parsing the configured day date format.
	 *
	 * @return the formatter
	 */
	public DateTimeFormatter dayFormatter() {
		final ZoneId zone = ZoneId.of(getTimeZoneId());
		// @formatter:off
		return new DateTimeFormatterBuilder()
				.parseCaseInsensitive()
				.appendPattern(getDayDateFormat())
				.toFormatter().withZone(zone)
				.withLocale(Locale.ENGLISH);
		// @formatter:on
	}

	/**
	 * Get a date formatter suitable for parsing the configured day date format.
	 *
	 * @return the formatter
	 */
	public DateTimeFormatter timeFormatter() {
		final ZoneId zone = ZoneId.of(getTimeZoneId());
		// @formatter:off
		return new DateTimeFormatterBuilder()
				.parseCaseInsensitive()
				.appendPattern(getTimeDateFormat())
				.toFormatter().withZone(zone)
				.withLocale(Locale.ENGLISH);
		// @formatter:on
	}

	/**
	 * Get a date formatter suitable for parsing the configured day date format.
	 *
	 * @return the formatter
	 */
	public DateTimeFormatter timestampFormatter() {
		final ZoneId zone = ZoneId.of(getTimeZoneId());
		final long currYear = ZonedDateTime.now(zone).getLong(ChronoField.YEAR);
		// @formatter:off
		return new DateTimeFormatterBuilder()
				.parseCaseInsensitive()
				.appendPattern(getTimestampDateFormat())
				.parseDefaulting(ChronoField.YEAR, currYear)
				.toFormatter().withZone(zone)
				.withLocale(Locale.ENGLISH);
		// @formatter:on
	}

	/**
	 * Get a date formatter suitable for parsing the configured day date format.
	 *
	 * @return the formatter
	 */
	public DateTimeFormatter timestampHourFormatter() {
		final ZoneId zone = ZoneId.of(getTimeZoneId());
		// @formatter:off
		return new DateTimeFormatterBuilder()
				.parseCaseInsensitive()
				.appendPattern(getTimestampHourDateFormat())
				.toFormatter().withZone(zone)
				.withLocale(Locale.ENGLISH);
		// @formatter:on
	}

	private String getURLForLocationTemplate(String template, String locationKey) {
		return getBaseUrl() + '/' + String.format(template, locationKey);
	}

	@Override
	public DayDatum readCurrentRiseSet(final String locationKey) {
		if ( locationKey == null ) {
			return null;
		}
		final String url = getURLForLocationTemplate(getRiseSetTemplate(), locationKey);
		final DateTimeFormatter dayFormat = dayFormatter();
		final DateTimeFormatter timeFormat = timeFormatter();
		DayDatum result = null;
		try {
			URLConnection conn = getURLConnection(url, HTTP_METHOD_GET);
			JsonNode data = getObjectMapper().readTree(getInputStreamFromURLConnection(conn));
			result = parseRiseSet(data, dayFormat, timeFormat, dayFormat.getZone());
		} catch ( IOException e ) {
			log.warn("Error reading MetService URL [{}]: {}", url, e.getMessage());
		}
		return result;
	}

	private DayDatum parseRiseSet(JsonNode data, DateTimeFormatter dayFormat,
			DateTimeFormatter timeFormat, ZoneId zone) {
		if ( data == null ) {
			return null;
		}
		SimpleDayDatum result = null;
		LocalDate day = parseDateAttribute(data, "day", dayFormat, LocalDate::from);
		LocalTime sunrise = parseDateAttribute(data, "sunRise", timeFormat, LocalTime::from);
		LocalTime sunset = parseDateAttribute(data, "sunSet", timeFormat, LocalTime::from);
		LocalTime moonrise = parseDateAttribute(data, "moonRise", timeFormat, LocalTime::from);
		LocalTime moonset = parseDateAttribute(data, "moonSet", timeFormat, LocalTime::from);
		if ( day != null && sunrise != null && sunset != null ) {
			result = new SimpleDayDatum(null, day.atStartOfDay(zone).toInstant(), new DatumSamples());
			result.setSunriseTime(sunrise);
			result.setSunsetTime(sunset);
			if ( moonrise != null ) {
				result.setMoonriseTime(moonrise);
			}
			if ( moonset != null ) {
				result.setMoonsetTime(moonset);
			}
			log.debug("Parsed DayDatum from rise set: {}", result);
		}
		return result;
	}

	@Override
	public Collection<NodeDatum> readCurrentLocalObservations(String locationKey) {
		if ( locationKey == null ) {
			return null;
		}
		final List<NodeDatum> result = new ArrayList<>(4);
		final String url = getURLForLocationTemplate(getLocalObsTemplate(), locationKey);
		final DateTimeFormatter tsFormat = timestampFormatter();

		JsonNode root;
		try {
			URLConnection conn = getURLConnection(url, HTTP_METHOD_GET);
			root = getObjectMapper().readTree(getInputStreamFromURLConnection(conn));
		} catch ( IOException e ) {
			log.warn("Error reading MetService URL [{}]: {}", url, e.getMessage());
			return result;
		}

		JsonNode data = root.get("threeHour");
		if ( data == null ) {
			log.warn("Local observation container key 'threeHour' not found in {}", url);
		} else {
			Instant infoDate = parseDateAttribute(data, "dateTimeISO", ISO_DATE_TIME, Instant::from);
			if ( infoDate == null ) {
				infoDate = parseDateAttribute(data, "dateTime", tsFormat, Instant::from);
			}
			BigDecimal temp = parseBigDecimalAttribute(data, "temp");

			if ( infoDate == null || temp == null ) {
				log.debug("Date and/or temperature missing from key 'threeHour' in {}", url);
			} else {
				SimpleAtmosphericDatum weather = new SimpleAtmosphericDatum(null, infoDate,
						new DatumSamples());
				weather.setTemperature(temp);
				weather.setHumidity(parseIntegerAttribute(data, "humidity"));

				BigDecimal millibar = parseBigDecimalAttribute(data, "pressure");
				if ( millibar != null ) {
					int pascals = (millibar.multiply(new BigDecimal(100))).intValue();
					weather.setAtmosphericPressure(pascals);
				}
				// TODO: rainfall?
				result.add(weather);
			}
		}

		data = root.get("twentyFourHour");
		if ( data == null ) {
			log.warn("Local observation container key 'twentyFourHour' not found in {}", url);
		} else {
			ZonedDateTime infoDate = parseDateAttribute(data, "dateTime", tsFormat, ZonedDateTime::from);
			BigDecimal maxTemp = parseBigDecimalAttribute(data, "maxTemp");
			BigDecimal minTemp = parseBigDecimalAttribute(data, "minTemp");
			if ( infoDate == null || minTemp == null || maxTemp == null ) {
				log.debug("Date and/or temperature extremes missing from key 'twentyFourHour' in {}",
						url);
			} else {
				SimpleDayDatum day = new SimpleDayDatum(null,
						infoDate.truncatedTo(ChronoUnit.DAYS).toInstant(), new DatumSamples());
				day.setTemperatureMinimum(minTemp);
				day.setTemperatureMaximum(maxTemp);
				// TODO: rainfall?
				result.add(day);
			}
		}

		return result;
	}

	@Override
	public Collection<DayDatum> readLocalForecast(String locationKey) {
		// get local forecast
		final String url = getURLForLocationTemplate(getLocalForecastTemplate(), locationKey);
		final List<DayDatum> result = new ArrayList<>(4);
		final DateTimeFormatter dayFormat = dayFormatter();
		final DateTimeFormatter timeFormat = timeFormatter();

		JsonNode root;
		try {
			URLConnection conn = getURLConnection(url, HTTP_METHOD_GET);
			root = getObjectMapper().readTree(getInputStreamFromURLConnection(conn));
		} catch ( IOException e ) {
			log.warn("Error reading MetService URL [{}]: {}", url, e.getMessage());
			return result;
		}

		JsonNode days = root.get("days");
		if ( days.isArray() ) {
			for ( JsonNode dayNode : days ) {
				DayDatum day = parseRiseSet(dayNode.get("riseSet"), dayFormat, timeFormat,
						dayFormat.getZone());
				if ( day != null ) {
					day.setSkyConditions(parseStringAttribute(dayNode, "forecastWord"));
					day.setTemperatureMinimum(parseBigDecimalAttribute(dayNode, "min"));
					day.setTemperatureMaximum(parseBigDecimalAttribute(dayNode, "max"));
					result.add(day);
				}
			}
		}

		return result;
	}

	@Override
	public Collection<AtmosphericDatum> readHourlyForecast(String locationKey) {
		final String url = getURLForLocationTemplate(getHourlyObsAndForecastTemplate(), locationKey);
		final List<AtmosphericDatum> result = new ArrayList<>(4);
		final DateTimeFormatter hourTimestampFormat = timestampHourFormatter();

		JsonNode root;
		try {
			URLConnection conn = getURLConnection(url, HTTP_METHOD_GET);
			root = getObjectMapper().readTree(getInputStreamFromURLConnection(conn));
		} catch ( IOException e ) {
			log.warn("Error reading MetService URL [{}]: {}", url, e.getMessage());
			return result;
		}

		JsonNode hours = root.get("forecastData");
		if ( hours.isArray() ) {
			for ( JsonNode hourNode : hours ) {
				BigDecimal temp = parseBigDecimalAttribute(hourNode, "temperature");
				Instant infoDate = parseDateAttribute(hourNode, "dateISO", ISO_DATE_TIME, Instant::from);
				if ( infoDate == null ) {
					String time = parseStringAttribute(hourNode, "timeFrom");
					String date = parseStringAttribute(hourNode, "date");
					if ( time != null && date != null ) {
						String dateString = time + " " + date;
						try {
							infoDate = hourTimestampFormat.parse(dateString, Instant::from);
						} catch ( DateTimeParseException e ) {
							log.debug(
									"Error parsing date attribute [timeFrom date] value [{}] using pattern {}: {}",
									dateString, hourTimestampFormat, e.getMessage());
						}
					}
				}
				if ( infoDate == null || temp == null ) {
					continue;
				}

				SimpleAtmosphericDatum weather = new SimpleAtmosphericDatum(null, infoDate,
						new DatumSamples());
				weather.setTemperature(temp);
				result.add(weather);
			}
		}

		return result;
	}

	/**
	 * Get the base URL.
	 *
	 * @return the base URL
	 */
	public String getBaseUrl() {
		return baseUrl;
	}

	/**
	 * Read an InputStream as Unicode text and return as a String.
	 *
	 * @param in
	 *        the InputStream to read
	 * @return the text
	 * @throws IOException
	 *         if an IO error occurs
	 */
	protected String readUnicodeInputStream(InputStream in) throws IOException {
		UnicodeReader reader = new UnicodeReader(in, null);
		String data = FileCopyUtils.copyToString(reader);
		reader.close();
		return data;
	}

	/**
	 * The base URL for queries to MetService. Defaults to
	 * {@link #DEFAULT_BASE_URL}.
	 *
	 * @param baseUrl
	 *        The base URL to use.
	 */
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	/**
	 * Get the object mapper.
	 *
	 * @return the mapper
	 */
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

	/**
	 * Get the local observation template.
	 *
	 * @return the template
	 */
	public String getLocalObsTemplate() {
		return localObsTemplate;
	}

	/**
	 * The name of the "localObs" file to parse, using a single string parameter
	 * for the location key. This file is expected to contain a single JSON
	 * object declaration with the humidity, pressure, etc. attributes. Defaults
	 * to {@link #DEFAULT_LOCAL_OBS_SET_TEMPLATE}.
	 *
	 * @param localObsTemplate
	 *        The file name template to use.
	 */
	public void setLocalObsTemplate(String localObsTemplate) {
		this.localObsTemplate = localObsTemplate;
	}

	/**
	 * Get the local forecast template.
	 *
	 * @return the template
	 */
	public String getLocalForecastTemplate() {
		return localForecastTemplate;
	}

	/**
	 * The name of the "localForecast" file to parse, using a single string
	 * parameter for the location key. This file is expected to contain a single
	 * JSON object declaration with an array of day JSON objects, the first day
	 * from which the sky conditions are extracted. The real-time data doesn't
	 * provide sky conditions, so we just use the presumably static value for
	 * the day. Defaults to {@link #DEFAULT_LOCAL_FORECAST_SET_TEMPLATE}.
	 *
	 * @param localForecastTemplate
	 *        The file name template to use.
	 */
	public void setLocalForecastTemplate(String localForecastTemplate) {
		this.localForecastTemplate = localForecastTemplate;
	}

	/**
	 * Get the sunrise set template.
	 *
	 * @return the template
	 */
	public String getRiseSetTemplate() {
		return riseSetTemplate;
	}

	/**
	 * The name of the "riseSet" file to parse, using a single string parameter
	 * for the location key. This file is expected to contain a single JSON
	 * object declaration with the sunrise, sunset, and date attributes.
	 * Defaults to {@link #DEFAULT_RISE_SET_TEMPLATE}.
	 *
	 * @param riseSetTemplate
	 *        The file name template to use.
	 */
	public void setRiseSetTemplate(String riseSetTemplate) {
		this.riseSetTemplate = riseSetTemplate;
	}

	/**
	 * Get the one minute observation template.
	 *
	 * @return the template
	 */
	public String getOneMinuteObsTemplate() {
		return oneMinuteObsTemplate;
	}

	/**
	 * The name of the "oneMinuteObs" file to parse, using a single string
	 * parameter for the location key. This file is expected to contain a single
	 * JSON object declaration with the weather date attributes. Defaults to
	 * {@link #DEFAULT_ONE_MINUTE_OBS_SET_TEMPLATE}.
	 *
	 * @param oneMinuteObsTemplate
	 *        the template to use
	 */
	public void setOneMinuteObsTemplate(String oneMinuteObsTemplate) {
		this.oneMinuteObsTemplate = oneMinuteObsTemplate;
	}

	/**
	 * Get the hourly observation and forecast template.
	 *
	 * @return the template
	 */
	public String getHourlyObsAndForecastTemplate() {
		return hourlyObsAndForecastTemplate;
	}

	/**
	 * The name of the "hourlyObsAndForecast" file to parse, using a single
	 * string parameter for the location key. Defaults to
	 * {@link #DEFAULT_ONE_MINUTE_OBS_SET_TEMPLATE}.
	 *
	 * @param hourlyObsAndForecastTemplate
	 *        the template to use
	 */
	public void setHourlyObsAndForecastTemplate(String hourlyObsAndForecastTemplate) {
		this.hourlyObsAndForecastTemplate = hourlyObsAndForecastTemplate;
	}

	/**
	 * Get the day date format.
	 *
	 * @return the date format
	 */
	public String getDayDateFormat() {
		return dayDateFormat;
	}

	/**
	 * The {@link SimpleDateFormat} date format to use to parse the day date.
	 * Defaluts to {@link #DEFAULT_DAY_DATE_FORMAT}.
	 *
	 * @param dayDateFormat
	 *        The date format to use.
	 */
	public void setDayDateFormat(String dayDateFormat) {
		this.dayDateFormat = dayDateFormat;
	}

	/**
	 * Get the time date format.
	 *
	 * @return the date format
	 */
	public String getTimeDateFormat() {
		return timeDateFormat;
	}

	/**
	 * Set a {@link SimpleDateFormat} time format to use to parse sunrise/sunset
	 * times. Defaults to {@link #DEFAULT_TIME_DATE_FORMAT}.
	 *
	 * @param timeDateFormat
	 *        The date format to use.
	 */
	public void setTimeDateFormat(String timeDateFormat) {
		this.timeDateFormat = timeDateFormat;
	}

	/**
	 * Get the timestamp date format.
	 *
	 * @return the date format
	 */
	public String getTimestampDateFormat() {
		return timestampDateFormat;
	}

	/**
	 * Set a {@link SimpleDateFormat} date and time pattern for parsing the
	 * information date from the {@code oneMinObs} file. Defaults to
	 * {@link #DEFAULT_TIMESTAMP_DATE_FORMAT}.
	 *
	 * @param timestampDateFormat
	 *        The date format to use.
	 */
	public void setTimestampDateFormat(String timestampDateFormat) {
		this.timestampDateFormat = timestampDateFormat;
	}

	/**
	 * GEt the timestamp hour date format.
	 *
	 * @return the date format
	 */
	public String getTimestampHourDateFormat() {
		return timestampHourDateFormat;
	}

	/**
	 * Set a {@link SimpleDateFormat} date and time pattern for parsing the
	 * information date from the {@code hourlyObsAndForecast} file. Defaults to
	 * {@link #DEFAULT_TIMESTAMP_HOUR_DATE_FORMAT}.
	 *
	 * @param timestampHourDateFormat
	 *        The date format to use.
	 */
	public void setTimestampHourDateFormat(String timestampHourDateFormat) {
		this.timestampHourDateFormat = timestampHourDateFormat;
	}

	/**
	 * Get the time zone ID.
	 *
	 * @return the time zone ID
	 */
	public String getTimeZoneId() {
		return timeZoneId;
	}

	/**
	 * Set the time zone ID used for parsing date strings. Defaults to
	 * {@link #DEFAULT_TIME_ZONE_ID}.
	 *
	 * @param timeZoneId
	 *        The time zone ID to use.
	 */
	public void setTimeZoneId(String timeZoneId) {
		this.timeZoneId = timeZoneId;
	}

}
