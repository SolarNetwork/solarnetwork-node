/* ==================================================================
 * MetserviceSupport.java - Oct 18, 2011 2:47:44 PM
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.io.ICsvBeanReader;
import org.supercsv.prefs.CsvPreference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Base class to support MetService Day and Weather data sources.
 * 
 * @param T
 *        the datum type
 * @author matt
 * @version 2.0
 */
public abstract class MetserviceSupport<T extends Datum> {

	/** The default value for the {@code locationIdentifier} property. */
	public static final String DEFAULT_LOCATION_IDENTIFIER = "wellington-city";

	/** A key to use for the "last datum" in the local cache. */
	protected static final String LAST_DATUM_CACHE_KEY = "last";

	private String uid;
	private String groupUID;
	private String locationIdentifier;
	private MetserviceClient client;
	private MessageSource messageSource;

	private final Map<String, T> datumCache;

	/** A class-level logger. */
	protected Logger log = LoggerFactory.getLogger(getClass());

	public MetserviceSupport() {
		datumCache = new ConcurrentHashMap<String, T>(2);
		locationIdentifier = DEFAULT_LOCATION_IDENTIFIER;
		client = new BasicMetserviceClient();
	}

	/**
	 * Get a list of setting specifiers suitable for configuring this class.
	 * 
	 * @return List of setting specifiers.
	 */
	public List<SettingSpecifier> getSettingSpecifiers() {
		MetserviceDayDatumDataSource defaults = new MetserviceDayDatumDataSource();
		List<SettingSpecifier> result = new ArrayList<SettingSpecifier>(8);
		result.add(new BasicTextFieldSettingSpecifier("uid", null));
		result.add(new BasicTextFieldSettingSpecifier("groupUID", null));

		List<NewZealandWeatherLocation> locs = availableWeatherLocations();
		if ( locs != null ) {
			// drop-down menu for all possible location keys
			BasicMultiValueSettingSpecifier menuSpec = new BasicMultiValueSettingSpecifier(
					"locationIdentifier", defaults.getLocationIdentifier());
			Map<String, String> menuValues = new LinkedHashMap<String, String>(3);
			for ( NewZealandWeatherLocation loc : locs ) {
				menuValues.put(loc.getKey(), loc.getName());
			}
			menuSpec.setValueTitles(menuValues);
			result.add(menuSpec);
		} else {
			// fall back to manual value
			result.add(new BasicTextFieldSettingSpecifier("locationIdentifier",
					defaults.getLocationIdentifier()));
		}

		return result;
	}

	private static final Logger LOG = LoggerFactory.getLogger(MetserviceSupport.class);

	/** The default weather location data column mappings to parse. */
	public static final String[] DEFAULT_LOCATION_CSV_HEADERS = new String[] { "island", "name", "key" };

	/**
	 * Get an ordered list of known weather locations. The
	 * {@link NewZealandWeatherLocation#getKey()} value can then be passed to
	 * other methods requiring a location key.
	 * 
	 * @return The list of known weather locations.
	 */
	public static List<NewZealandWeatherLocation> availableWeatherLocations() {
		InputStream in = MetserviceSupport.class.getResourceAsStream("metservice-locations.csv");
		if ( in == null ) {
			LOG.warn("Metservice location CSV data not available.");
			return Collections.emptyList();
		}
		Reader reader = null;
		try {
			reader = new InputStreamReader(in, "UTF-8");
			return parseCSVWeatherLocations(reader, DEFAULT_LOCATION_CSV_HEADERS);
		} catch ( IOException e ) {
			LOG.error("Unable to import weather CSV data: {}", e.getMessage());
		} finally {
			if ( reader != null ) {
				try {
					reader.close();
				} catch ( IOException e ) {
					// ignore
				}
			}
		}
		return Collections.emptyList();
	}

	public static List<NewZealandWeatherLocation> parseCSVWeatherLocations(Reader in, String[] headers) {
		final List<NewZealandWeatherLocation> result = new ArrayList<NewZealandWeatherLocation>();
		final ICsvBeanReader reader = new CsvBeanReader(in, CsvPreference.STANDARD_PREFERENCE);
		try {
			NewZealandWeatherLocation loc = null;
			while ( (loc = reader.read(NewZealandWeatherLocation.class, headers)) != null ) {
				if ( loc.getKey() == null ) {
					continue;
				}
				result.add(loc);
			}
			Collections.sort(result);
		} catch ( IOException e ) {
			LOG.error("Unable to import weather CSV data: {}", e.getMessage());
		} finally {
			try {
				if ( reader != null ) {
					reader.close();
				}
			} catch ( IOException e ) {
				// ingore
			}
		}
		return result;
	}

	/**
	 * Get a cache to support queries resulting in unchanged data.
	 * 
	 * @return the cache
	 */
	protected Map<String, T> getDatumCache() {
		return datumCache;
	}

	/**
	 * The base URL for queries to MetService.
	 * 
	 * @param baseUrl
	 *        The base URL to use.
	 * @deprecated Configure {@link #setClient(MetserviceClient)} instead.
	 */
	@Deprecated
	public void setBaseUrl(String baseUrl) {
		if ( client instanceof BasicMetserviceClient ) {
			((BasicMetserviceClient) client).setBaseUrl(baseUrl);
		}
	}

	/**
	 * Set the {@link ObjectMapper} to use for parsing JSON.
	 * 
	 * @param objectMapper
	 *        The object mapper.
	 * @deprecated Configure {@link #setClient(MetserviceClient)} instead.
	 */
	@Deprecated
	public void setObjectMapper(ObjectMapper objectMapper) {
		if ( client instanceof BasicMetserviceClient ) {
			((BasicMetserviceClient) client).setObjectMapper(objectMapper);
		}
	}

	private void setLocationIdentifierFromSuffix(final String prefix, final String value) {
		if ( prefix == null || value == null ) {
			return;
		}
		if ( value.startsWith(prefix) && value.length() > prefix.length() ) {
			setLocationIdentifier(value.substring(prefix.length()));
		}
	}

	/**
	 * The name of the "localObs" file to parse.
	 * 
	 * @param localObs
	 *        The file name to use.
	 * @deprecated Configure {@link #setLocationIdentifier(String)} instead.
	 */
	@Deprecated
	public void setLocalObs(String localObs) {
		setLocationIdentifierFromSuffix("localObs_", localObs);
	}

	/**
	 * The name of the "localForecast" file to parse.
	 * 
	 * @param localForecast
	 *        The file name to use.
	 * @deprecated Configure {@link #setLocationIdentifier(String)} instead.
	 */
	@Deprecated
	public void setLocalForecastTemplate(String localForecast) {
		setLocationIdentifierFromSuffix("localForecast", localForecast);
	}

	/**
	 * The name of the "riseSet" file to parse.
	 * 
	 * @param riseSet
	 *        The file name to use.
	 * @deprecated Configure {@link #setLocationIdentifier(String)} instead.
	 */
	@Deprecated
	public void setRiseSetTemplate(String riseSet) {
		setLocationIdentifierFromSuffix("riseSet_", riseSet);
	}

	/**
	 * The {@link SimpleDateFormat} date format to use to parse the day date.
	 * 
	 * @param dayDateFormat
	 *        The date format to use.
	 * @deprecated Configure {@link #setClient(MetserviceClient)} instead.
	 */
	@Deprecated
	public void setDayDateFormat(String dayDateFormat) {
		if ( client instanceof BasicMetserviceClient ) {
			((BasicMetserviceClient) client).setDayDateFormat(dayDateFormat);
		}
	}

	/**
	 * Set a {@link SimpleDateFormat} time format to use to parse sunrise/sunset
	 * times.
	 * 
	 * @param timeDateFormat
	 *        The date format to use.
	 * @deprecated Configure {@link #setClient(MetserviceClient)} instead.
	 */
	@Deprecated
	public void setTimeDateFormat(String timeDateFormat) {
		if ( client instanceof BasicMetserviceClient ) {
			((BasicMetserviceClient) client).setTimeDateFormat(timeDateFormat);
		}
	}

	/**
	 * Set a {@link SimpleDateFormat} date and time pattern for parsing the
	 * information date from the {@code oneMinObs} file.
	 * 
	 * @param timestampDateFormat
	 *        The date format to use.
	 * @deprecated Configure {@link #setClient(MetserviceClient)} instead.
	 */
	@Deprecated
	public void setTimestampDateFormat(String timestampDateFormat) {
		if ( client instanceof BasicMetserviceClient ) {
			((BasicMetserviceClient) client).setTimestampDateFormat(timestampDateFormat);
		}
	}

	public MessageSource getMessageSource() {
		return messageSource;
	}

	/**
	 * Set a message source to use for resolving messages.
	 * 
	 * @param messageSource
	 *        The message source to use.
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public String getUID() {
		return getUid();
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getGroupUID() {
		return groupUID;
	}

	public void setGroupUID(String groupUID) {
		this.groupUID = groupUID;
	}

	public String getLocationIdentifier() {
		return locationIdentifier;
	}

	/**
	 * Set the Metservice weather location identifer to use, which determines
	 * the URL to use for loading weather data files. This should be one of the
	 * keys returned by {@link MetserviceSupport#availableWeatherLocations()}.
	 * 
	 * @param locationIdentifier
	 *        The location identifier to use.
	 */
	public void setLocationIdentifier(String locationIdentifier) {
		this.locationIdentifier = locationIdentifier;
	}

	/**
	 * Get the Metservice client.
	 * 
	 * @return The Metservice client.
	 */
	public MetserviceClient getClient() {
		return client;
	}

	/**
	 * Set the Metservice client.
	 * 
	 * @param client
	 *        The client to use.
	 */
	public void setClient(MetserviceClient client) {
		this.client = client;
	}

}
