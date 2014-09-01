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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.support.UnicodeReader;
import net.solarnetwork.node.support.XmlServiceSupport;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.util.FileCopyUtils;

/**
 * Base class to support MetService Day and Weather data sources.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>baseUrl</dt>
 * <dd>The base URL for queries to MetService. Defaults to
 * {@link #DEFAULT_BASE_URL}.</dd>
 * 
 * <dt>objectMapper</dt>
 * <dd>The {@link ObjectMapper} to use for parsing JSON.</dd>
 * </dl>
 * 
 * @param T
 *        the datum type
 * @author matt
 * @version 1.2
 */
public abstract class MetserviceSupport<T extends Datum> extends XmlServiceSupport {

	/** The default value for the {@code baseUrl} property. */
	public static final String DEFAULT_BASE_URL = "http://www.metservice.com/publicData";

	/** A key to use for the "last datum" in the local cache. */
	protected static final String LAST_DATUM_CACHE_KEY = "last";

	private final Map<String, T> datumCache;
	private String baseUrl;
	private ObjectMapper objectMapper;

	public MetserviceSupport() {
		datumCache = new ConcurrentHashMap<String, T>(2);
		baseUrl = DEFAULT_BASE_URL;
	}

	/**
	 * an InputStream as Unicode text and return as a String.
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
	 * Parse a Date from an attribute value.
	 * 
	 * <p>
	 * If the date cannot be parsed, <em>null</em> will be returned.
	 * </p>
	 * 
	 * @param key
	 *        the attribute key to obtain from the {@code data} Map
	 * @param data
	 *        the attributes
	 * @param dateFormat
	 *        the date format to use to parse the date string
	 * @return the parsed {@link Date} instance, or <em>null</em> if an error
	 *         occurs or the specified attribute {@code key} is not available
	 */
	protected Date parseDateAttribute(String key, JsonNode data, SimpleDateFormat dateFormat) {
		Date result = null;
		if ( data != null ) {
			JsonNode node = data.get(key);
			if ( node != null ) {
				try {
					result = dateFormat.parse(node.asText());
				} catch ( ParseException e ) {
					log.debug("Error parsing date attribute [{}] value [{}] using pattern {}: {}",
							new Object[] { key, data.get(key), dateFormat.toPattern(), e.getMessage() });
				}
			}
		}
		return result;
	}

	/**
	 * Parse a Double from an attribute value.
	 * 
	 * <p>
	 * If the Double cannot be parsed, <em>null</em> will be returned.
	 * </p>
	 * 
	 * @param key
	 *        the attribute key to obtain from the {@code data} Map
	 * @param data
	 *        the attributes
	 * @return the parsed {@link Double}, or <em>null</em> if an error occurs or
	 *         the specified attribute {@code key} is not available
	 */
	protected Double parseDoubleAttribute(String key, JsonNode data) {
		Double num = null;
		if ( data != null ) {
			JsonNode node = data.get(key);
			if ( node != null ) {
				try {
					num = Double.valueOf(node.asText());
				} catch ( NumberFormatException e ) {
					log.debug("Error parsing double attribute [{}] value [{}]: {}", new Object[] { key,
							data.get(key), e.getMessage() });
				}
			}
		}
		return num;
	}

	/**
	 * Get a cache to support queries resulting in unchanged data.
	 * 
	 * @return
	 */
	protected Map<String, T> getDatumCache() {
		return datumCache;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

}
