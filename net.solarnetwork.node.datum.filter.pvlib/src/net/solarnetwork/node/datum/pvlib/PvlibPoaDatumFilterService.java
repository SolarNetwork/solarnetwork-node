/* ==================================================================
 * PvlibPoaDatumFilterService.java - 16/11/2024 1:03:03 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.pvlib;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.solarnetwork.codec.JsonUtils.STRING_MAP_TYPE;
import static net.solarnetwork.node.Constants.solarNodeHome;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.nonEmptyString;
import static org.springframework.util.FileCopyUtils.copyToString;
import static org.springframework.util.StringUtils.arrayToDelimitedString;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.domain.Location;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.DatumMetadataService;
import net.solarnetwork.node.service.LocationService;
import net.solarnetwork.node.service.MetadataService;
import net.solarnetwork.node.service.support.BaseDatumFilterSupport;
import net.solarnetwork.service.DatumFilterService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.OptionalService.OptionalFilterableService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.NumberUtils;

/**
 * {@link DatumDataSource} for POA data derived from GHI data.
 *
 * <p>
 * This filter relies on an external OS command to perform the GHI to POA
 * irradiance calculation. The command is expected to return a JSON object with
 * a {@link #getPoaResultKey()} property with the result. The command will be
 * provided the arguments as defined in the {@link CommandOptions} enumeration.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class PvlibPoaDatumFilterService extends BaseDatumFilterSupport
		implements DatumFilterService, SettingSpecifierProvider {

	/** The {@code ghiPropertyName} property default value. */
	public static final String DEFAULT_GHI_PROPERTY_NAME = "irradiance";

	/** The {@code poaPropertyName} property default value. */
	public static final String DEFAULT_POA_PROPERTY_NAME = "irradiance_poa";

	/** The {@code command} property default value. */
	public static final String DEFAULT_COMMAND = solarNodeHome() + "/bin/ghi-to-poa";

	/** The {@code poaResultKey} property default value. */
	public static final String DEFAULT_POA_RESULT_KEY = "poa_global";

	private final OptionalService<LocationService> locationService;
	private final OptionalService<DatumMetadataService> datumMetadataService;
	private final OptionalFilterableService<MetadataService> metadataService;
	private final ObjectMapper objectMapper;

	private String metadataPath;
	private String alternateMetadataPath;
	private boolean useNodeLocation;
	private String ghiPropertyName = DEFAULT_GHI_PROPERTY_NAME;
	private String poaPropertyName = DEFAULT_POA_PROPERTY_NAME;

	private BigDecimal lat;
	private BigDecimal lon;
	private BigDecimal altitude;
	private ZoneId timeZone = ZoneId.systemDefault();
	private BigDecimal azimuth;
	private BigDecimal tilt;
	private BigDecimal minCosZenith;
	private BigDecimal maxZenith;

	private String command = DEFAULT_COMMAND;
	private String poaResultKey = DEFAULT_POA_RESULT_KEY;

	/**
	 * Constructor.
	 *
	 * @param objectMapper
	 *        the object mapper to use
	 * @param locationService
	 *        the location service to use
	 * @param datumMetadataService
	 *        the datum metadata service to use
	 * @param metadataService
	 *        the metadata service to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public PvlibPoaDatumFilterService(ObjectMapper objectMapper,
			OptionalService<LocationService> locationService,
			OptionalService<DatumMetadataService> datumMetadataService,
			OptionalFilterableService<MetadataService> metadataService) {
		super();
		this.objectMapper = requireNonNullArgument(objectMapper, "objectMapper");
		this.locationService = requireNonNullArgument(locationService, "locationService");
		this.datumMetadataService = requireNonNullArgument(datumMetadataService, "datumMetadataService");
		this.metadataService = requireNonNullArgument(metadataService, "metadataService");

	}

	@Override
	public DatumSamplesOperations filter(Datum datum, DatumSamplesOperations samples,
			Map<String, Object> parameters) {
		final long start = incrementInputStats();
		if ( !conditionsMatch(datum, samples, parameters) || !samples.hasSampleValue(ghiPropertyName) ) {
			incrementIgnoredStats(start);
			return samples;
		}

		final String sourceId = datum.getSourceId();

		final Map<String, String> cmdArguments = new HashMap<>(10);
		if ( lat != null ) {
			cmdArguments.put(CommandOptions.Latitude.getOption(), lat.toPlainString());
		}
		if ( lon != null ) {
			cmdArguments.put(CommandOptions.Lonitude.getOption(), lon.toPlainString());
		}
		if ( altitude != null ) {
			cmdArguments.put(CommandOptions.Altitude.getOption(), altitude.toPlainString());
		}
		if ( timeZone != null ) {
			cmdArguments.put(CommandOptions.TimeZone.getOption(), timeZone.getId());
		}
		if ( tilt != null ) {
			cmdArguments.put(CommandOptions.Tilt.getOption(), tilt.toPlainString());
		}
		if ( azimuth != null ) {
			cmdArguments.put(CommandOptions.Azimuth.getOption(), azimuth.toPlainString());
		}
		if ( minCosZenith != null ) {
			cmdArguments.put(CommandOptions.MinCosZenith.getOption(), minCosZenith.toPlainString());
		}
		if ( maxZenith != null ) {
			cmdArguments.put(CommandOptions.MaxZenith.getOption(), maxZenith.toPlainString());
		}

		final String metaPath = nonEmptyString(metadataPath);
		final String altMetaPath = nonEmptyString(alternateMetadataPath);
		if ( metaPath != null || altMetaPath != null ) {
			// first try the configured MetadataService
			if ( isMetadataServiceUidConfigured() ) {
				MetadataService metaService = service(metadataService);
				if ( metaService != null ) {
					GeneralDatumMetadata meta = metaService.getAllMetadata();
					populateArguments(cmdArguments, meta, metaPath);
					populateArguments(cmdArguments, meta, altMetaPath);
				}
			}
			// then look for more specific datum stream metadata (specific to this source ID)
			DatumMetadataService metaService = service(datumMetadataService);
			if ( metaService != null ) {
				GeneralDatumMetadata meta = metaService.getSourceMetadata(sourceId);
				populateArguments(cmdArguments, meta, metaPath);
				populateArguments(cmdArguments, meta, altMetaPath);
			}
		}

		// use node location, if configured to do so
		if ( useNodeLocation ) {
			LocationService locService = service(locationService);
			if ( locService != null ) {
				Location loc = locService.getNodeLocation();
				if ( loc != null ) {
					if ( loc.getLatitude() != null ) {
						cmdArguments.put(CommandOptions.Latitude.getOption(),
								loc.getLatitude().toPlainString());
					}
					if ( loc.getLongitude() != null ) {
						cmdArguments.put(CommandOptions.Lonitude.getOption(),
								loc.getLongitude().toPlainString());
					}
					if ( loc.getElevation() != null ) {
						cmdArguments.put(CommandOptions.Altitude.getOption(),
								loc.getElevation().toPlainString());
					}
				}
			}
		}

		DatumSamplesOperations s = samples;

		if ( !(cmdArguments.containsKey(CommandOptions.Latitude.getOption())
				&& cmdArguments.containsKey(CommandOptions.Lonitude.getOption())) ) {
			log.warn(
					"No lat/lon available to perform GHI -> POA irradiance calculation; discovered options are: {}",
					cmdArguments);
		} else {
			Object ghiVal = samples.findSampleValue(ghiPropertyName);
			if ( ghiVal instanceof BigDecimal ) {
				ghiVal = ((BigDecimal) ghiVal).toPlainString();
			}
			cmdArguments.put(CommandOptions.Ghi.getOption(), ghiVal.toString());

			cmdArguments.put(CommandOptions.Date.getOption(), datum.getTimestamp().atZone(timeZone)
					.truncatedTo(ChronoUnit.SECONDS).toLocalDateTime().toString());

			Map<String, ?> result = executeCommand(cmdArguments);
			if ( result != null ) {
				log.debug("GHI -> POA command options {} returned {}", cmdArguments, result);
				Object poaVal = result.get(poaResultKey);
				Number poa = null;
				if ( poaVal instanceof Number ) {
					poa = (Number) poaVal;
				} else if ( poaVal != null ) {
					try {
						poa = NumberUtils.parseNumber(poaVal.toString(), BigDecimal.class);
					} catch ( NumberFormatException e ) {
						log.warn(
								"Unable to parse [{}] POA irriadiance value [{}] as a number; discarding",
								poaResultKey, poaVal);
					}
				}
				if ( poa != null ) {
					DatumSamples copy = new DatumSamples(samples);
					s = new DatumSamples(samples);
					copy.putInstantaneousSampleValue(poaPropertyName, poa);
					s = copy;
				}
			}
		}
		incrementStats(start, samples, s);
		return s;
	}

	private void populateArguments(Map<String, String> cmdArguments, GeneralDatumMetadata meta,
			String metaPath) {
		if ( meta == null || metaPath == null ) {
			return;
		}

		Map<?, ?> params = meta.metadataAtPath(metaPath, Map.class);
		if ( params == null ) {
			return;
		}
		for ( CommandOptions opt : CommandOptions.values() ) {
			String metaKey = opt.getMetadataKey();
			if ( metaKey == null ) {
				continue;
			}
			Object metaVal = params.get(metaKey);
			if ( metaVal != null ) {
				cmdArguments.put(opt.getOption(),
						metaVal instanceof BigDecimal ? ((BigDecimal) metaVal).toPlainString()
								: metaVal.toString());
			}
		}
	}

	private Map<String, ?> executeCommand(final Map<String, String> args) {
		String[] cmd = new String[args.size() * 2 + 1];
		cmd[0] = command;
		int i = 0;
		for ( Entry<String, String> e : args.entrySet() ) {
			cmd[++i] = e.getKey();
			cmd[++i] = e.getValue();
		}
		if ( log.isDebugEnabled() ) {
			log.debug("Executing GHI -> POA irradiance command: {} ", arrayToDelimitedString(cmd, " "));
		}
		ProcessBuilder pb = new ProcessBuilder(cmd);
		try {
			Process pr = pb.start();

			String json = nonEmptyString(
					copyToString(new InputStreamReader(pr.getInputStream(), UTF_8)));

			String err = nonEmptyString(copyToString(new InputStreamReader(pr.getErrorStream(), UTF_8)));
			if ( err != null ) {
				throw new RuntimeException(
						String.format("GHI -> POA irradiance command [%s] error: %s", command, err));
			} else if ( json == null ) {
				throw new RuntimeException(
						String.format("GHI -> POA irradiance command [%s] produced no output", command));
			}

			return objectMapper.readValue(json, STRING_MAP_TYPE);
		} catch ( JacksonException e ) {
			throw new RuntimeException(
					String.format("Error parsing GHI -> POA irradiance command [%s] output: %s", command,
							e.getMessage()));
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.filter.pvlib.poa";
	}

	@Override
	public String getDisplayName() {
		return "pvlib POA datum filter";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = baseIdentifiableSettings("");
		populateStatusSettings(results);
		populateBaseSampleTransformSupportSettings(results);

		results.add(new BasicTextFieldSettingSpecifier("metadataServiceUid", null, false,
				"(objectClass=net.solarnetwork.node.service.MetadataService)"));
		results.add(new BasicTextFieldSettingSpecifier("metadataPath", null));
		results.add(new BasicTextFieldSettingSpecifier("alternateMetadataPath", null));
		results.add(new BasicToggleSettingSpecifier("useNodeLocation", Boolean.FALSE));

		results.add(new BasicTextFieldSettingSpecifier("ghiPropertyName", DEFAULT_GHI_PROPERTY_NAME));
		results.add(new BasicTextFieldSettingSpecifier("poaPropertyName", DEFAULT_POA_PROPERTY_NAME));

		results.add(new BasicTextFieldSettingSpecifier("lat", null));
		results.add(new BasicTextFieldSettingSpecifier("lon", null));
		results.add(new BasicTextFieldSettingSpecifier("altitude", null));
		results.add(new BasicTextFieldSettingSpecifier("timeZoneId", null));
		results.add(new BasicTextFieldSettingSpecifier("azimuth", null));
		results.add(new BasicTextFieldSettingSpecifier("tilt", null));
		results.add(new BasicTextFieldSettingSpecifier("minCosZenith", null));
		results.add(new BasicTextFieldSettingSpecifier("maxZenith", null));

		results.add(new BasicTextFieldSettingSpecifier("command", DEFAULT_COMMAND));
		results.add(new BasicTextFieldSettingSpecifier("poaResultKey", DEFAULT_POA_RESULT_KEY));

		return results;
	}

	/**
	 * Test if a {@code MetadataService} UID filter is configured.
	 *
	 * @return {@code true} if {@link #getMetadataServiceUid()} is not
	 *         {@code null} or empty
	 */
	private final boolean isMetadataServiceUidConfigured() {
		String uid = getMetadataServiceUid();
		return (uid != null && !uid.isEmpty());
	}

	/**
	 * Get the {@link MetadataService} service filter UID.
	 *
	 * @return the service UID
	 */
	public final String getMetadataServiceUid() {
		return metadataService.getPropertyValue(UID_PROPERTY);
	}

	/**
	 * Set the {@link MetadataService} service filter UID.
	 *
	 * @param uid
	 *        the service UID
	 */
	public final void setMetadataServiceUid(String uid) {
		metadataService.setPropertyFilter(UID_PROPERTY, uid);
	}

	/**
	 * Get the metadata path to extract parameters from.
	 *
	 * @return the metadata path
	 */
	public final String getMetadataPath() {
		return metadataPath;
	}

	/**
	 * Set the metadata path to extract parameters from.
	 *
	 * @param metadataPath
	 *        the metadata path to set
	 */
	public final void setMetadataPath(String metadataPath) {
		this.metadataPath = metadataPath;
	}

	/**
	 * Get the alternate metadata path to extract parameters from.
	 *
	 * @return the metadata path
	 */
	public final String getAlternateMetadataPath() {
		return alternateMetadataPath;
	}

	/**
	 * Set the alternate metadata path to extract parameters from.
	 *
	 * @param alternateMetadataPath
	 *        the metadata path to set
	 */
	public final void setAlternateMetadataPath(String alternateMetadataPath) {
		this.alternateMetadataPath = alternateMetadataPath;
	}

	/**
	 * Get the "use node location" mode.
	 *
	 * @return {@code true} if the {@link LocationService} should be used for
	 *         the latitude, longitude, and altitude parameters
	 */
	public final boolean isUseNodeLocation() {
		return useNodeLocation;
	}

	/**
	 * Set the "use node location" mode.
	 *
	 * @param useNodeLocation
	 *        {@code true} if the {@link LocationService} should be used for the
	 *        latitude, longitude, and altitude parameters
	 */
	public final void setUseNodeLocation(boolean useNodeLocation) {
		this.useNodeLocation = useNodeLocation;
	}

	/**
	 * Get the input GHI datum stream property name to read.
	 *
	 * @return the property name
	 */
	public final String getGhiPropertyName() {
		return ghiPropertyName;
	}

	/**
	 * Set the input GHI datum stream property name to read.
	 *
	 * @param ghiPropertyName
	 *        the property name to set
	 */
	public final void setGhiPropertyName(String ghiPropertyName) {
		this.ghiPropertyName = ghiPropertyName;
	}

	/**
	 * Get the output POA irradiance datum stream property to generate.
	 *
	 * @return the property name
	 */
	public final String getPoaPropertyName() {
		return poaPropertyName;
	}

	/**
	 * Set the output POA irradiance datum stream property to generate.
	 *
	 * @param poaPropertyName
	 *        the property name to set
	 */
	public final void setPoaPropertyName(String poaPropertyName) {
		this.poaPropertyName = poaPropertyName;
	}

	/**
	 * Get the GPS latitude to use.
	 *
	 * @return the latitude
	 */
	public final BigDecimal getLat() {
		return lat;
	}

	/**
	 * Set the GPS latitude to use.
	 *
	 * @param lat
	 *        the latitude to set
	 */
	public final void setLat(BigDecimal lat) {
		this.lat = lat;
	}

	/**
	 * Get the GPS longitude to use.
	 *
	 * @return the longitude
	 */
	public final BigDecimal getLon() {
		return lon;
	}

	/**
	 * Set the GPS longitude to use.
	 *
	 * @param lon
	 *        the longitude to set
	 */
	public void setLon(BigDecimal lon) {
		this.lon = lon;
	}

	/**
	 * Get the altitude.
	 *
	 * @return the altitude
	 */
	public final BigDecimal getAltitude() {
		return altitude;
	}

	/**
	 * Set the altitude.
	 *
	 * @param altitude
	 *        the altitude to set
	 */
	public final void setAltitude(BigDecimal altitude) {
		this.altitude = altitude;
	}

	/**
	 * Get the time zone.
	 *
	 * @return the zone, never {@code null}
	 */
	public final ZoneId getTimeZone() {
		return timeZone;
	}

	/**
	 * Set the time zone.
	 *
	 * @param timeZone
	 *        the zone to set; if {@code null} then the system default will be
	 *        used
	 */
	public final void setTimeZone(ZoneId timeZone) {
		this.timeZone = (timeZone != null ? timeZone : ZoneId.systemDefault());
	}

	/**
	 * Get the time zone.
	 *
	 * @return the zone, never {@code null}
	 */
	public final String getTimeZoneId() {
		return getTimeZone().getId();
	}

	/**
	 * Set the time zone.
	 *
	 * @param timeZoneId
	 *        the zone to set; if {@code null} then the system default will be
	 *        used
	 */
	public final void setTimeZoneId(String timeZoneId) {
		setTimeZone(timeZoneId == null || timeZoneId.isEmpty() ? ZoneId.systemDefault()
				: ZoneId.of(timeZoneId));
	}

	/**
	 * Get the PV array azimuth.
	 *
	 * @return the azimuth, between -180 and 180
	 */
	public final BigDecimal getAzimuth() {
		return azimuth;
	}

	/**
	 * Set the PV array azimuth.
	 *
	 * @param azimuth
	 *        the azimuth to set, between -180 and 180
	 */
	public final void setAzimuth(BigDecimal azimuth) {
		this.azimuth = azimuth;
	}

	/**
	 * Get the PV array tilt, for GTI calculations.
	 *
	 * @return the tilt, between 0 and 90
	 */
	public final BigDecimal getTilt() {
		return tilt;
	}

	/**
	 * Set the PV tilt, for GTI calculations.
	 *
	 * @param tilt
	 *        the tilt to set, between 0 and 90
	 */
	public final void setTilt(BigDecimal tilt) {
		this.tilt = tilt;
	}

	/**
	 * Get the minimum cos(zenith) value to use when calculating global
	 * clearness index.
	 *
	 * @return the minimum value
	 */
	public final BigDecimal getMinCosZenith() {
		return minCosZenith;
	}

	/**
	 * Set the minimum cos(zenith) value to use when calculating global
	 * clearness index.
	 *
	 * @param minCosZenith
	 *        the value to set
	 */
	public final void setMinCosZenith(BigDecimal minCosZenith) {
		this.minCosZenith = minCosZenith;
	}

	/**
	 * Get the maximum zenith value to allow in DNI calculation.
	 *
	 * @return the value
	 */
	public final BigDecimal getMaxZenith() {
		return maxZenith;
	}

	/**
	 * Set the maximum zenith value to allow in DNI calculation.
	 *
	 * @param maxZenith
	 *        the value
	 */
	public final void setMaxZenith(BigDecimal maxZenith) {
		this.maxZenith = maxZenith;
	}

	/**
	 * Get the OS command to run.
	 *
	 * @return the command
	 */
	public final String getCommand() {
		return command;
	}

	/**
	 * Set the OS command to run.
	 *
	 * @param command
	 *        the command to set; if {@code null} or empty then
	 *        {@link #DEFAULT_COMMAND} will be used
	 */
	public final void setCommand(String command) {
		this.command = (command != null && !command.isEmpty() ? command : DEFAULT_COMMAND);
	}

	/**
	 * Get the POA irradiance result key.
	 *
	 * @return the key
	 */
	public final String getPoaResultKey() {
		return poaResultKey;
	}

	/**
	 * Set the POA irradiance result key.
	 *
	 * @param poaResultKey
	 *        the key to set; if {@code null} or empty then
	 *        {@link #DEFAULT_POA_RESULT_KEY} will be used
	 */
	public final void setPoaResultKey(String poaResultKey) {
		this.poaResultKey = (poaResultKey != null && !poaResultKey.isEmpty() ? poaResultKey
				: DEFAULT_POA_RESULT_KEY);
	}

}
