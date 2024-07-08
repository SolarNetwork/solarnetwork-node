/* ==================================================================
 * SolcastDatumDataSource.java - 14/10/2022 10:03:00 am
 *
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.solcast;

import static java.util.Collections.emptySet;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.solarnetwork.domain.Location;
import net.solarnetwork.node.domain.datum.AtmosphericDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.LocationService;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.StringUtils;

/**
 * Datum data source for Solcast data.
 *
 * @author matt
 * @version 1.1
 */
public class SolcastDatumDataSource extends DatumDataSourceSupport
		implements MultiDatumDataSource, SettingSpecifierProvider {

	/** The {@code parameters} default value. */
	public static final String DEFAULT_PARAMETERS_VALUE = "air_temp,dni,ghi";

	/** The {@code resolution} default value. */
	public static final Duration DEFAULT_RESOLUTION = Duration.ofMinutes(30);

	/** The Solcast supported resolutions. */
	public static final Set<Duration> SUPPORTED_RESOLUTIONS;
	static {
		SUPPORTED_RESOLUTIONS = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
		// @formatter:off
				Duration.ofMinutes(5),
				Duration.ofMinutes(10),
				Duration.ofMinutes(15),
				Duration.ofMinutes(20),
				Duration.ofMinutes(30),
				Duration.ofMinutes(60)
		// @formatter:on
		)));
	}

	private final SolcastClient client;
	private final OptionalService<LocationService> locationService;

	private String sourceId;
	private BigDecimal lat;
	private BigDecimal lon;
	private boolean useNodeLocation;
	private Set<String> parameters;
	private Duration resolution = DEFAULT_RESOLUTION;
	private Integer azimuth;
	private Integer tilt;
	private String arrayType;

	/**
	 * Constructor.
	 *
	 * @param locationService
	 *        the location service to use for node GPS support
	 */
	public SolcastDatumDataSource(OptionalService<LocationService> locationService) {
		this(new JsonSolcastClient(), locationService);
	}

	/**
	 * Constructor.
	 *
	 * @param client
	 *        the client to use
	 * @param locationService
	 *        the location service to use for node GPS support
	 */
	public SolcastDatumDataSource(SolcastClient client,
			OptionalService<LocationService> locationService) {
		super();
		this.client = requireNonNullArgument(client, "client");
		this.locationService = requireNonNullArgument(locationService, "locationService");
		setParametersValue(DEFAULT_PARAMETERS_VALUE);
	}

	@Override
	public Collection<String> publishedSourceIds() {
		final String sourceId = resolvePlaceholders(getSourceId());
		return (sourceId == null || sourceId.isEmpty() ? Collections.emptySet()
				: Collections.singleton(sourceId));
	}

	@Override
	public Class<? extends NodeDatum> getMultiDatumType() {
		return AtmosphericDatum.class;
	}

	@Override
	public Collection<NodeDatum> readMultipleDatum() {
		if ( !client.isConfigured() || sourceId == null || sourceId.isEmpty() ) {
			return emptySet();
		}
		SolcastCriteria criteria = new SolcastCriteria();
		criteria.setLat(lat);
		criteria.setLon(lon);
		criteria.setParameters(parameters);
		criteria.setPeriod(resolution);
		if ( useNodeLocation ) {
			LocationService s = OptionalService.service(locationService);
			if ( s != null ) {
				Location nodeLoc = s.getNodeLocation();
				if ( nodeLoc != null && nodeLoc.getLatitude() != null
						&& nodeLoc.getLongitude() != null ) {
					criteria.setLat(nodeLoc.getLatitude());
					criteria.setLon(nodeLoc.getLongitude());
				}
			}
		}
		if ( !criteria.isValid() ) {
			log.warn(
					"Settings are not valid: is the latitude/longitude configured or available in the node's location?");
			return emptySet();
		}
		AtmosphericDatum d = client.getMostRecentConditions(sourceId, criteria);
		return (d != null ? Collections.singleton(d) : emptySet());
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.solcast";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(8);
		results.addAll(baseIdentifiableSettings(null));
		results.add(new BasicTextFieldSettingSpecifier("sourceId", null));
		results.add(new BasicTextFieldSettingSpecifier("apiKey", null, true));
		results.add(new BasicToggleSettingSpecifier("useNodeLocation", false));
		results.add(new BasicTextFieldSettingSpecifier("lat", null));
		results.add(new BasicTextFieldSettingSpecifier("lon", null));
		results.add(new BasicTextFieldSettingSpecifier("parametersValue", DEFAULT_PARAMETERS_VALUE));
		results.add(new BasicTextFieldSettingSpecifier("azimuth", null));
		results.add(new BasicTextFieldSettingSpecifier("tilt", null));

		// drop-down menu for array type
		BasicMultiValueSettingSpecifier arrayTypeSpec = new BasicMultiValueSettingSpecifier("arrayType",
				"");
		Map<String, String> arrayTypeValues = new LinkedHashMap<>(3);
		arrayTypeValues.put("", "");
		arrayTypeValues.put("fixed",
				getMessageSource().getMessage("arrayType.fixed", null, "Fixed", Locale.getDefault()));
		arrayTypeValues.put("horizontal_single_axis",
				getMessageSource().getMessage("arrayType.horizontal_single_axis", null,
						"Horizontal Single Axis", Locale.getDefault()));
		arrayTypeSpec.setValueTitles(arrayTypeValues);
		results.add(arrayTypeSpec);

		// drop-down menu for all possible resolutions
		BasicMultiValueSettingSpecifier resolutionSpec = new BasicMultiValueSettingSpecifier(
				"resolutionValue", DEFAULT_RESOLUTION.toString());
		Map<String, String> resolutionMenuValues = new LinkedHashMap<>(3);
		for ( Duration d : SUPPORTED_RESOLUTIONS ) {
			String key = d.toString();
			resolutionMenuValues.put(key,
					getMessageSource().getMessage("resolution." + key, null, key, Locale.getDefault()));
		}
		resolutionSpec.setValueTitles(resolutionMenuValues);
		results.add(resolutionSpec);

		return results;
	}

	/**
	 * Get a single source ID to publish datum under.
	 *
	 * @return the sourceId to use, or {@literal null} to publish individual
	 *         sources per ping test
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set a single source ID to publish datum under.
	 *
	 * @param sourceId
	 *        the sourceId to use, or {@literal null} to publish individual
	 *        sources per ping test
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Get the node location toggle.
	 *
	 * @return {@code true} if the node's GPS coordinates should be used for the
	 *         GPS coordinates in Solcast API calls, or {@code false} to use the
	 *         GPS coordinates configured on this instance
	 */
	public boolean isUseNodeLocation() {
		return useNodeLocation;
	}

	/**
	 * Set the node location toggle.
	 *
	 * @param useNodeLocation
	 *        {@code true} if the node's GPS coordinates should be used for the
	 *        GPS coordinates in Solcast API calls, or {@code false} to use the
	 *        GPS coordinates configured on this instance
	 */
	public void setUseNodeLocation(boolean useNodeLocation) {
		this.useNodeLocation = useNodeLocation;
	}

	/**
	 * Get the GPS latitude to use in Solcast API calls.
	 *
	 * @return the latitude
	 */
	public BigDecimal getLat() {
		return lat;
	}

	/**
	 * Set the GPS latitude to use in Solcast API calls.
	 *
	 * @param lat
	 *        the latitude to set
	 */
	public void setLat(BigDecimal lat) {
		this.lat = lat;
	}

	/**
	 * Get the GPS longitude to use in Solcast API calls.
	 *
	 * @return the longitude
	 */
	public BigDecimal getLon() {
		return lon;
	}

	/**
	 * Set the GPS longitude to use in Solcast API calls.
	 *
	 * @param lon
	 *        the longitude to set
	 */
	public void setLon(BigDecimal lon) {
		this.lon = lon;
	}

	/**
	 * Set the Solcast API key to use.
	 *
	 * @param apiKey
	 *        the API key to use
	 */
	public void setApiKey(String apiKey) {
		if ( client instanceof ConfigurableSolcastClient ) {
			((ConfigurableSolcastClient) client).setApiKey(apiKey);
		}
	}

	/**
	 * Get the set of Solcast parameters to collect.
	 *
	 * @return the parameters
	 */
	public Set<String> getParameters() {
		return parameters;
	}

	/**
	 * Set the set of Solcast parameters to collect.
	 *
	 * @param parameters
	 *        the parameters to set
	 */
	public void setParameters(Set<String> parameters) {
		this.parameters = parameters;
	}

	/**
	 * Get the comma-delimited list of Solcast parameters to collect.
	 *
	 * @return the parameters as a comma-delimited list
	 */
	public String getParametersValue() {
		return StringUtils.commaDelimitedStringFromCollection(parameters);
	}

	/**
	 * Set the comma-delimited list of Solcast parameters to collect.
	 *
	 * @param parameters
	 *        the parameters to set, as a comma-delimited list
	 */
	public void setParametersValue(String parameters) {
		setParameters(StringUtils.commaDelimitedStringToSet(parameters));
	}

	/**
	 * Get the desired data resolution.
	 *
	 * @return the resolution
	 */
	public Duration getResolution() {
		return resolution;
	}

	/**
	 * Set the desired data resolution.
	 *
	 * @param resolution
	 *        the resolution to set
	 */
	public void setResolution(Duration resolution) {
		this.resolution = resolution;
	}

	/**
	 * Get the desired data resolution.
	 *
	 * @return the resolution
	 */
	public String getResolutionValue() {
		return (resolution != null ? resolution.toString() : null);
	}

	/**
	 * Set the desired data resolution.
	 *
	 * @param resolution
	 *        the resolution to set
	 */
	public void setResolutionValue(String resolution) {
		try {
			setResolution(resolution != null ? Duration.parse(resolution) : null);
		} catch ( DateTimeParseException e ) {
			log.warn("Invalid resoultion format [%s]; must be in ISO 8601 duration format like PT15m");
		}
	}

	/**
	 * Get the azimuth, for GTI calculations.
	 *
	 * @return the azimuth, between -180 and 180
	 */
	public Integer getAzimuth() {
		return azimuth;
	}

	/**
	 * Set the azimuth, for GTI calculations.
	 *
	 * @param azimuth
	 *        the azimuth to set, between -180 and 180
	 */
	public void setAzimuth(Integer azimuth) {
		this.azimuth = azimuth;
	}

	/**
	 * Get the tilt, for GTI calculations.
	 *
	 * @return the tilt, between 0 and 90
	 */
	public Integer getTilt() {
		return tilt;
	}

	/**
	 * Set the tilt, for GTI calculations.
	 *
	 * @param tilt
	 *        the tilt to set, between 0 and 90
	 */
	public void setTilt(Integer tilt) {
		this.tilt = tilt;
	}

	/**
	 * Get the array type, for GTI calculations.
	 *
	 * @return the arrayType the array type, e.g {@literal fixed} or
	 *         {@literal horizontal_single_axis}
	 */
	public String getArrayType() {
		return arrayType;
	}

	/**
	 * Set the array type, for GTI calculations.
	 *
	 * @param arrayType
	 *        the arrayType to set, e.g {@literal fixed} or
	 *        {@literal horizontal_single_axis}
	 */
	public void setArrayType(String arrayType) {
		this.arrayType = arrayType;
	}

}
