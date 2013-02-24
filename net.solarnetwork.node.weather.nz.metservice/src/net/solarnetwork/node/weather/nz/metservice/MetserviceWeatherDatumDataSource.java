/* ==================================================================
 * MetserviceWeatherDatumDataSource.java - Oct 18, 2011 4:58:27 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.weather.nz.metservice;

import java.io.IOException;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.weather.WeatherDatum;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * MetService implementation of a {@link WeatherDatum} {@link DatumDataSource}.
 * 
 * <p>
 * This implementation reads public data available on the MetService website to
 * collect weather information.
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>oneMinObs</dt>
 * <dd>The name of the "oneMinObs" file to parse. This file is expected to
 * contain a single JSON object declaration with the temperature, timestamp,
 * etc. attributes. Defaults to {@link #DEFAULT_ONE_MIN_OBS_SET}.</dd>
 * 
 * <dt>localObs</dt>
 * <dd>The name of the "localObs" file to parse. This file is expected to
 * contain a single JSON object declaration with the humidity, pressure, etc.
 * attributes. Defaults to {@link #DEFAULT_LOCAL_OBS_SET}.</dd>
 * 
 * <dt>uv</dt>
 * <dd>The name of the "uv" file to parse. This file is expected to contain a
 * single JSON object declaration with the UV attribute. Defaults to
 * {@link #DEFAULT_UV_SET}.</dd>
 * 
 * <dt>localForecast</dt>
 * <dd>The name of the "localForecast" file to parse. This file is expected to
 * contain a single JSON object declaration with an array of day JSON objects,
 * the first day from which the sky conditions are extracted. The real-time data
 * doesn't provide sky conditions, so we just use the presumably static value
 * for the day. Defaults to {@link #DEFAULT_LOCAL_FORECAST_SET}.</dd>
 * 
 * <dt>localForecastDayPattern</dt>
 * <dd>A regular expression used to extract a single day JSON object from the
 * {@code localForecast} file. This class doesn't perform actual JSON parsing,
 * and can only work with simple JSON objects. This regular expression must
 * include a single matching group that returns the appropriate day object from
 * the overall {@code localForecast} data. Defaults to
 * {@link #DEFAULT_LOCAL_FORECAST_DAY_PATTERN}.</dd>
 * 
 * <dt>timestampDateFormat</dt>
 * <dd>A {@link SimpleDateFormat} date and time pattern for parsing the
 * information date from the {@code oneMinObs} file. Defaults to
 * {@link #DEFAULT_TIMESTAMP_DATE_FORMAT}.</dd>
 * </dl>
 * 
 * @author matt
 * @version $Revision$
 */
public class MetserviceWeatherDatumDataSource extends MetserviceSupport<WeatherDatum> implements
		DatumDataSource<WeatherDatum>, SettingSpecifierProvider {

	/** The default value for the {@code oneMinObs} property. */
	public static final String DEFAULT_ONE_MIN_OBS_SET = "oneMinObs93437";

	/** The default value for the {@code oneMinObs} property. */
	public static final String DEFAULT_LOCAL_OBS_SET = "localObs93437";

	/** The default value for the {@code oneMinObs} property. */
	public static final String DEFAULT_UV_SET = "uvIndexWellington";

	/** The default value for the {@code localForecast} property. */
	public static final String DEFAULT_LOCAL_FORECAST_SET = "localForecastWellington";

	/** The default value for the {@code timestampDateFormat} property. */
	public static final String DEFAULT_TIMESTAMP_DATE_FORMAT = "h:mma EEEE d MMM yyyy";

	public static final String DEFAULT_LOCAL_FORECAST_DAY_PATTERN = "\"?days\"?\\s*:\\s*\\[([^}]+})";

	private static final Object MONITOR = new Object();
	private static MessageSource MESSAGE_SOURCE;

	private String timestampDateFormat;
	private String oneMinObs;
	private String localObs;
	private String uv;
	private String localForecast;
	private Pattern localForecastDayPattern;

	public MetserviceWeatherDatumDataSource() {
		super();
		timestampDateFormat = DEFAULT_TIMESTAMP_DATE_FORMAT;
		oneMinObs = DEFAULT_ONE_MIN_OBS_SET;
		localObs = DEFAULT_LOCAL_OBS_SET;
		uv = DEFAULT_UV_SET;
		localForecast = DEFAULT_LOCAL_FORECAST_SET;
		setLocalForecastDayPattern(DEFAULT_LOCAL_FORECAST_DAY_PATTERN);
	}

	@Override
	public Class<? extends WeatherDatum> getDatumType() {
		return WeatherDatum.class;
	}

	@Override
	public WeatherDatum readCurrentDatum() {
		WeatherDatum result = null;

		String url = getBaseUrl() + '/' + oneMinObs;
		final SimpleDateFormat tsFormat = new SimpleDateFormat(getTimestampDateFormat());
		try {
			URLConnection conn = getURLConnection(url, HTTP_METHOD_GET);
			Map<String, String> data = parseSimpleJavaScriptObjectProperties(getInputStreamFromURLConnection(conn));

			Date infoDate = parseDateAttribute("time", data, tsFormat);
			Double temp = parseDoubleAttribute("temperature", data);

			if ( infoDate != null && temp != null ) {
				result = new WeatherDatum();
				result.setInfoDate(infoDate);
				result.setTemperatureCelcius(temp);

				// UV data discontinued? Removed for now.

				// get local obs data
				try {
					url = getBaseUrl() + '/' + localObs;
					conn = getURLConnection(url, HTTP_METHOD_GET);
					data = parseSimpleJavaScriptObjectProperties(getInputStreamFromURLConnection(conn));

					result.setHumidity(parseDoubleAttribute("humidity", data));
					result.setBarometricPressure(parseDoubleAttribute("pressure", data));
				} catch ( IOException e ) {
					log.warn("Error reading MetService URL [{}]: {}", url, e.getMessage());
				}

				// get local forecast
				try {
					url = getBaseUrl() + '/' + localForecast;
					conn = getURLConnection(url, HTTP_METHOD_GET);
					String localForecast = readUnicodeInputStream(getInputStreamFromURLConnection(conn));
					if ( localForecast != null ) {
						// we have to extract just the current day to get the "conditions" value
						Matcher m = localForecastDayPattern.matcher(localForecast);
						if ( m.find() ) {
							localForecast = m.group(1);

							data = parseSimpleJavaScriptObjectProperties(localForecast);
							result.setSkyConditions(data.get("forecastWord"));
						}
					}
				} catch ( IOException e ) {
					log.warn("Error reading MetService URL [{}]: {}", url, e.getMessage());
				}

				log.debug("Obtained WeatherDatum: {}", result);
			}

		} catch ( IOException e ) {
			log.warn("Error reading MetService URL [{}]: {}", url, e.getMessage());
		}
		return result;
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.weather.nz.metservice.weather";
	}

	@Override
	public String getDisplayName() {
		return "New Zealand Metservice weather information";
	}

	@Override
	public MessageSource getMessageSource() {
		synchronized ( MONITOR ) {
			if ( MESSAGE_SOURCE == null ) {
				ResourceBundleMessageSource source = new ResourceBundleMessageSource();
				source.setBundleClassLoader(getClass().getClassLoader());
				source.setBasename(getClass().getName());
				MESSAGE_SOURCE = source;
			}
		}
		return MESSAGE_SOURCE;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return Arrays
				.asList((SettingSpecifier) new BasicTextFieldSettingSpecifier("baseUrl",
						DEFAULT_BASE_URL), (SettingSpecifier) new BasicTextFieldSettingSpecifier(
						"localForecast", DEFAULT_LOCAL_FORECAST_SET),
						(SettingSpecifier) new BasicTextFieldSettingSpecifier("localForecastDayPattern",
								DEFAULT_LOCAL_FORECAST_DAY_PATTERN),
						(SettingSpecifier) new BasicTextFieldSettingSpecifier("localObs",
								DEFAULT_LOCAL_OBS_SET),
						(SettingSpecifier) new BasicTextFieldSettingSpecifier("oneMinObs",
								DEFAULT_ONE_MIN_OBS_SET),
						(SettingSpecifier) new BasicTextFieldSettingSpecifier("timestampDateFormat",
								DEFAULT_TIMESTAMP_DATE_FORMAT),
						(SettingSpecifier) new BasicTextFieldSettingSpecifier("uv", DEFAULT_UV_SET));
	}

	public String getLocalForecastDayPattern() {
		return localForecastDayPattern.pattern();
	}

	public void setLocalForecastDayPattern(String localForecastDayPattern) {
		this.localForecastDayPattern = Pattern.compile(localForecastDayPattern);
	}

	public String getTimestampDateFormat() {
		return timestampDateFormat;
	}

	public void setTimestampDateFormat(String timestampDateFormat) {
		this.timestampDateFormat = timestampDateFormat;
	}

	public String getOneMinObs() {
		return oneMinObs;
	}

	public void setOneMinObs(String oneMinObs) {
		this.oneMinObs = oneMinObs;
	}

	public String getLocalObs() {
		return localObs;
	}

	public void setLocalObs(String localObs) {
		this.localObs = localObs;
	}

	public String getUv() {
		return uv;
	}

	public void setUv(String uv) {
		this.uv = uv;
	}

	public String getLocalForecast() {
		return localForecast;
	}

	public void setLocalForecast(String localForecast) {
		this.localForecast = localForecast;
	}

}
