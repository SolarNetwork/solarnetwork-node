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
 */

package net.solarnetwork.node.weather.nz.metservice;

import java.io.IOException;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.weather.WeatherDatum;
import org.codehaus.jackson.JsonNode;
import org.springframework.context.MessageSource;

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
 * @version 1.1
 */
public class MetserviceWeatherDatumDataSource extends MetserviceSupport<WeatherDatum> implements
		DatumDataSource<WeatherDatum>, SettingSpecifierProvider {

	/** The default value for the {@code oneMinObs} property. */
	public static final String DEFAULT_LOCAL_OBS_SET = "localObs_wellington-city";

	/** The default value for the {@code localForecast} property. */
	public static final String DEFAULT_LOCAL_FORECAST_SET = "localForecastwellington-city";

	/** The default value for the {@code timestampDateFormat} property. */
	public static final String DEFAULT_TIMESTAMP_DATE_FORMAT = "h:mma EEEE d MMM yyyy";

	private MessageSource messageSource;
	private String timestampDateFormat;
	private String localObs;
	private String localObsContainerKey = "threeHour";
	private String localForecast;

	public MetserviceWeatherDatumDataSource() {
		super();
		timestampDateFormat = DEFAULT_TIMESTAMP_DATE_FORMAT;
		localObs = DEFAULT_LOCAL_OBS_SET;
		localForecast = DEFAULT_LOCAL_FORECAST_SET;
	}

	@Override
	public Class<? extends WeatherDatum> getDatumType() {
		return WeatherDatum.class;
	}

	@Override
	public WeatherDatum readCurrentDatum() {
		WeatherDatum result = null;

		String url = getBaseUrl() + '/' + localObs;
		final SimpleDateFormat tsFormat = new SimpleDateFormat(getTimestampDateFormat());
		try {
			URLConnection conn = getURLConnection(url, HTTP_METHOD_GET);
			JsonNode root = getObjectMapper().readTree(getInputStreamFromURLConnection(conn));
			JsonNode data = root.get(localObsContainerKey);
			if ( data == null ) {
				log.warn("Local observation container key '{}' not found in {}", localObsContainerKey,
						url);
				return null;
			}
			Date infoDate = parseDateAttribute("dateTime", data, tsFormat);
			Double temp = parseDoubleAttribute("temp", data);

			if ( infoDate == null || temp == null ) {
				log.debug("Date and/or temperature missing from {}", url);
				return null;
			}
			result = new WeatherDatum();
			result.setCreated(infoDate);
			result.setTemperatureCelsius(temp);

			result.setHumidity(parseDoubleAttribute("humidity", data));
			result.setBarometricPressure(parseDoubleAttribute("pressure", data));

			// get local forecast
			try {
				url = getBaseUrl() + '/' + localForecast;
				conn = getURLConnection(url, HTTP_METHOD_GET);
				root = getObjectMapper().readTree(getInputStreamFromURLConnection(conn));

				JsonNode forecastWord = root.findValue("forecastWord");
				if ( forecastWord != null ) {
					result.setSkyConditions(forecastWord.asText());
				}
			} catch ( IOException e ) {
				log.warn("Error reading MetService URL [{}]: {}", url, e.getMessage());
			}

			log.debug("Obtained WeatherDatum: {}", result);

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
		return messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		MetserviceWeatherDatumDataSource defaults = new MetserviceWeatherDatumDataSource();
		return Arrays
				.asList((SettingSpecifier) new BasicTextFieldSettingSpecifier("uid", null),
						(SettingSpecifier) new BasicTextFieldSettingSpecifier("groupUID", null),
						(SettingSpecifier) new BasicTextFieldSettingSpecifier("baseUrl", defaults
								.getBaseUrl()),
						(SettingSpecifier) new BasicTextFieldSettingSpecifier("localForecast", defaults
								.getLocalForecast()),
						(SettingSpecifier) new BasicTextFieldSettingSpecifier("localObs", defaults
								.getLocalObs()), (SettingSpecifier) new BasicTextFieldSettingSpecifier(
								"localObsContainerKey", defaults.getLocalObsContainerKey()),
						(SettingSpecifier) new BasicTextFieldSettingSpecifier("timestampDateFormat",
								defaults.getTimestampDateFormat()));
	}

	public String getTimestampDateFormat() {
		return timestampDateFormat;
	}

	public void setTimestampDateFormat(String timestampDateFormat) {
		this.timestampDateFormat = timestampDateFormat;
	}

	public String getLocalObs() {
		return localObs;
	}

	public void setLocalObs(String localObs) {
		this.localObs = localObs;
	}

	public String getLocalForecast() {
		return localForecast;
	}

	public void setLocalForecast(String localForecast) {
		this.localForecast = localForecast;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public String getLocalObsContainerKey() {
		return localObsContainerKey;
	}

	public void setLocalObsContainerKey(String localObsContainerKey) {
		this.localObsContainerKey = localObsContainerKey;
	}

}
