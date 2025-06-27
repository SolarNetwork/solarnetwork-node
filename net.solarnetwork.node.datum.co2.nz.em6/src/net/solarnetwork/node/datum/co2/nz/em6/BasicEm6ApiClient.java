/* ==================================================================
 * BasicEm6ApiClient.java - 10/03/2023 12:08:51 pm
 *
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.co2.nz.em6;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.support.JsonHttpClientSupport;
import net.solarnetwork.service.RemoteServiceException;
import net.solarnetwork.util.NumberUtils;

/**
 * Basic implementation of the {@link Em6ApiClient}.
 *
 * <p>
 * Note the {@link NodeDatum} returned by this source will be of type
 * {@link net.solarnetwork.domain.datum.ObjectDatumKind#Location} and will
 * <b>not</b> have a location ID or source ID assigned.
 * </p>
 *
 * @author matt
 * @version 1.1
 */
public class BasicEm6ApiClient extends JsonHttpClientSupport implements Em6ApiClient {

	/** The default base URL to the API. */
	public static final URI DEFAULT_BASE_URI = URI.create("https://api.em6.co.nz/ords/em6/data_api");

	/** The "current carbon intensity" API relative path. */
	public static final String CURRENT_CARBON_INTENSITY_PATH = "/current_carbon_intensity";

	private static final Logger log = LoggerFactory.getLogger(BasicEm6ApiClient.class);

	private final URI baseUri;

	/**
	 * Default constructor.
	 */
	public BasicEm6ApiClient() {
		this(DEFAULT_BASE_URI, JsonUtils.newObjectMapper());
	}

	/**
	 * Constructor.
	 *
	 * @param objectMapper
	 *        the object mapper to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public BasicEm6ApiClient(ObjectMapper objectMapper) {
		this(DEFAULT_BASE_URI, objectMapper);
	}

	/**
	 * Constructor.
	 *
	 * @param baseUri
	 *        the base URI to use
	 * @param objectMapper
	 *        the object mapper to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public BasicEm6ApiClient(URI baseUri, ObjectMapper objectMapper) {
		super();
		this.baseUri = requireNonNullArgument(baseUri, "baseUri");
		setObjectMapper(requireNonNullArgument(objectMapper, "objectMapper"));
	}

	@Override
	public Collection<NodeDatum> currentCarbonIntensity() {
		final String url = baseUri.toString() + CURRENT_CARBON_INTENSITY_PATH;
		final List<NodeDatum> result = new ArrayList<>(4);
		try (InputStream in = jsonGET(url)) {
			final JsonNode root = getObjectMapper().readTree(in);
			if ( root != null ) {
				final JsonNode items = root.path("items");
				if ( items.isArray() ) {
					for ( JsonNode item : items ) {
						if ( !item.isObject() ) {
							continue;
						}
						final DatumSamples samples = new DatumSamples();
						Instant ts = null;
						for ( final Entry<String, JsonNode> prop : item.properties() ) {
							final String key = prop.getKey();
							if ( key.endsWith("_prev") || key.equals("trading_period")
									|| key.equals("trading_date") ) {
								// ignore values not needed
								continue;
							}
							final JsonNode val = prop.getValue();
							String propName = key;
							if ( "timestamp".equals(key) ) {
								try {
									ts = DateTimeFormatter.ISO_INSTANT.parse(val.asText(),
											Instant::from);
								} catch ( DateTimeParseException e ) {
									log.warn(
											"Error parsing date from 'timestamp' property value {} in object {}: {}",
											val, item, e.getMessage());
								}
							} else if ( val.isNumber() ) {
								BigDecimal n = val.decimalValue();
								if ( key.endsWith("_t") ) {
									// convert metric ton to g
									n = NumberUtils.scaled(n, 3);
									propName = propName.substring(0, key.length() - 2) + "_g";
								}

								if ( propName.startsWith("nz_") ) {
									// remove nz_ prefix
									propName = propName.substring(3);
								}
								if ( propName.startsWith("carbon_") ) {
									// remove carbon_ prefix
									propName = "co2_" + propName.substring(7);
								}
								samples.putInstantaneousSampleValue(propName, NumberUtils.narrow(n, 2));
							} else if ( val.isTextual() ) {
								samples.putStatusSampleValue(key, val.textValue());
							}
						}
						if ( ts != null && !samples.isEmpty() ) {
							result.add(SimpleDatum.locationDatum(null, null, ts, samples));
						}
					}
				}
			}
		} catch ( IOException e ) {
			String msg = String.format("Communication error reading em6 data from [%s]: %s", url,
					e.getMessage());
			throw new RemoteServiceException(msg, e);
		}
		return result;
	}

}
