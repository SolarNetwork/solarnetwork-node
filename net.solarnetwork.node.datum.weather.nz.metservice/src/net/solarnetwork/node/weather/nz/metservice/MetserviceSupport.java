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
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.reader.CsvRecordHandler;
import de.siegmar.fastcsv.reader.FieldModifiers;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Base class to support MetService Day and Weather data sources.
 *
 * @author matt
 * @version 2.1
 */
public abstract class MetserviceSupport extends DatumDataSourceSupport {

	/** The default value for the {@code locationIdentifier} property. */
	public static final String DEFAULT_LOCATION_IDENTIFIER = "wellington-city";

	/** A key to use for the "last datum" in the local cache. */
	protected static final String LAST_DATUM_CACHE_KEY = "last";

	private String locationIdentifier;
	private MetserviceClient client;

	private final Map<String, NodeDatum> datumCache;

	/**
	 * Constructor.
	 */
	public MetserviceSupport() {
		datumCache = new ConcurrentHashMap<>(2);
		locationIdentifier = DEFAULT_LOCATION_IDENTIFIER;
		client = new BasicMetserviceClient();
	}

	/**
	 * Get a list of setting specifiers suitable for configuring this class.
	 *
	 * @return List of setting specifiers.
	 */
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(8);
		result.addAll(getIdentifiableSettingSpecifiers());

		List<NewZealandWeatherLocation> locs = availableWeatherLocations();
		if ( locs != null ) {
			// drop-down menu for all possible location keys
			BasicMultiValueSettingSpecifier menuSpec = new BasicMultiValueSettingSpecifier(
					"locationIdentifier", DEFAULT_LOCATION_IDENTIFIER);
			Map<String, String> menuValues = new LinkedHashMap<>(3);
			for ( NewZealandWeatherLocation loc : locs ) {
				menuValues.put(loc.getKey(), loc.getName());
			}
			menuSpec.setValueTitles(menuValues);
			result.add(menuSpec);
		} else {
			// fall back to manual value
			result.add(new BasicTextFieldSettingSpecifier("locationIdentifier",
					DEFAULT_LOCATION_IDENTIFIER));
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
		try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			return parseCSVWeatherLocations(reader, DEFAULT_LOCATION_CSV_HEADERS);
		} catch ( IOException e ) {
			LOG.error("Unable to import weather CSV data: {}", e.getMessage());
		}
		return Collections.emptyList();
	}

	/**
	 * Parse CSV weather location data.
	 *
	 * @param in
	 *        the reader
	 * @param headers
	 *        the column headers
	 * @return the locations
	 */
	public static List<NewZealandWeatherLocation> parseCSVWeatherLocations(Reader in, String[] headers) {
		final List<NewZealandWeatherLocation> result = new ArrayList<NewZealandWeatherLocation>();
		try (CsvReader<CsvRecord> reader = CsvReader.builder().allowMissingFields(true)
				.allowExtraFields(true).skipEmptyLines(true).commentStrategy(CommentStrategy.NONE)
				.build(CsvRecordHandler.builder().fieldModifier(FieldModifiers.TRIM).build(), in)) {
			for ( CsvRecord row : reader ) {
				NewZealandWeatherLocation loc = new NewZealandWeatherLocation();
				PropertyAccessor bean = PropertyAccessorFactory.forBeanPropertyAccess(loc);
				for ( int i = 0; i < headers.length; i++ ) {
					String val = row.getField(i);
					if ( val != null && !val.isEmpty() ) {
						bean.setPropertyValue(headers[i], val);
					}
				}

				if ( loc.getKey() == null ) {
					continue;
				}
				result.add(loc);
			}
			Collections.sort(result);
		} catch ( UncheckedIOException | IOException e ) {
			LOG.error("Unable to import weather CSV data: {}", e.getMessage());
		}
		return result;
	}

	/**
	 * Get a cache to support queries resulting in unchanged data.
	 *
	 * @return the cache
	 */
	protected Map<String, NodeDatum> getDatumCache() {
		return datumCache;
	}

	/**
	 * Get the location identifier.
	 *
	 * @return the identifier
	 */
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
