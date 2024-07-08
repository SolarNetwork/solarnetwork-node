/* ==================================================================
 * PM3200DatumDataSource.java - 1/03/2014 8:42:02 AM
 *
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.schneider.pm3200;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.node.domain.datum.AcEnergyDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.hw.schneider.meter.PM3200Data;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.support.ModbusDataDatumDataSourceSupport;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.StringUtils;

/**
 * DatumDataSource for GeneralNodeACEnergyDatum with the Schneider Electric
 * PM3200 series kWh meter.
 *
 * @author matt
 * @version 2.5
 */
public class PM3200DatumDataSource extends ModbusDataDatumDataSourceSupport<PM3200Data>
		implements DatumDataSource, MultiDatumDataSource, SettingSpecifierProvider {

	/** The main source ID. */
	public static final String MAIN_SOURCE_ID = "Main";

	private Map<AcPhase, String> sourceMapping = getDefaulSourceMapping();
	private boolean backwards = false;

	/**
	 * Get a default {@code sourceMapping} value. This maps only the {@code 0}
	 * source to the value {@code Main}.
	 *
	 * @return mapping
	 */
	public static Map<AcPhase, String> getDefaulSourceMapping() {
		Map<AcPhase, String> result = new EnumMap<AcPhase, String>(AcPhase.class);
		result.put(AcPhase.Total, MAIN_SOURCE_ID);
		return result;
	}

	/**
	 * Default constructor.
	 */
	public PM3200DatumDataSource() {
		this(new PM3200Data());
	}

	/**
	 * Construct with a specific sample data instance.
	 *
	 * @param sample
	 *        the sample data to use
	 */
	public PM3200DatumDataSource(PM3200Data sample) {
		super(sample);
	}

	@Override
	public Collection<String> publishedSourceIds() {
		final Map<AcPhase, String> mapping = getSourceMapping();
		if ( mapping == null || mapping.isEmpty() ) {
			return Collections.emptySet();
		}
		final Set<String> result = new TreeSet<>();
		for ( String sourceId : mapping.values() ) {
			result.add(resolvePlaceholders(sourceId));
		}
		return result;
	}

	@Override
	protected void refreshDeviceInfo(ModbusConnection connection, PM3200Data sample) throws IOException {
		sample.readConfigurationData(connection);
	}

	@Override
	protected void refreshDeviceData(ModbusConnection connection, PM3200Data sample) throws IOException {
		sample.readMeterData(connection);
	}

	private String getInfoMessage() {
		String msg = null;
		try {
			msg = getDeviceInfoMessage();
		} catch ( RuntimeException e ) {
			log.debug("Error reading info: {}", e.getMessage());
		}
		return (msg == null ? "N/A" : msg);
	}

	private String getSampleMessage(PM3200Data data) {
		if ( data.getDataTimestamp() == null ) {
			return "N/A";
		}
		StringBuilder buf = new StringBuilder();
		buf.append("W = ").append(data.getActivePower());
		buf.append(", VAR = ").append(data.getReactivePower());
		buf.append(", Wh rec = ").append(data.getActiveEnergyReceived());
		buf.append(", Wh del = ").append(data.getActiveEnergyDelivered());
		buf.append(", cos \ud835\udf11 = ").append(data.getEffectiveTotalPowerFactor());
		buf.append("; sampled at ").append(data.getDataTimestamp());
		return buf.toString();
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return AcEnergyDatum.class;
	}

	@Override
	public AcEnergyDatum readCurrentDatum() {
		final String sourceId = resolvePlaceholders(getSourceMapping().get(AcPhase.Total));
		try {
			final PM3200Data currSample = getCurrentSample();
			if ( currSample == null ) {
				return null;
			}
			PM3200Datum d = new PM3200Datum(currSample, sourceId, AcPhase.Total, backwards);
			return d;
		} catch ( IOException e ) {
			log.error("Communication problem reading source {} from PM3200 device {}: {}", sourceId,
					modbusDeviceName(), e.getMessage());
			return null;
		}
	}

	@Override
	public Class<? extends NodeDatum> getMultiDatumType() {
		return AcEnergyDatum.class;
	}

	@Override
	public Collection<NodeDatum> readMultipleDatum() {
		final List<NodeDatum> results = new ArrayList<>(4);
		final PM3200Data currSample;
		try {
			currSample = getCurrentSample();
		} catch ( IOException e ) {
			log.error("Communication problem reading from PM3200 device {}: {}", modbusDeviceName(),
					e.getMessage());
			return results;
		}
		if ( currSample == null ) {
			return results;
		}
		if ( isCaptureTotal() ) {
			PM3200Datum d = new PM3200Datum(currSample,
					resolvePlaceholders(getSourceMapping().get(AcPhase.Total)), AcPhase.Total,
					backwards);
			if ( isCaptureTotal() ) {
				results.add(d);
			}
		}
		if ( isCapturePhaseA() ) {
			PM3200Datum d = new PM3200Datum(currSample,
					resolvePlaceholders(getSourceMapping().get(AcPhase.PhaseA)), AcPhase.PhaseA,
					backwards);
			if ( isCapturePhaseA() ) {
				results.add(d);
			}
		}
		if ( isCapturePhaseB() ) {
			PM3200Datum d = new PM3200Datum(currSample,
					resolvePlaceholders(getSourceMapping().get(AcPhase.PhaseB)), AcPhase.PhaseB,
					backwards);
			if ( isCapturePhaseB() ) {
				results.add(d);
			}
		}
		if ( isCapturePhaseC() ) {
			PM3200Datum d = new PM3200Datum(currSample,
					resolvePlaceholders(getSourceMapping().get(AcPhase.PhaseC)), AcPhase.PhaseC,
					backwards);
			if ( isCapturePhaseC() ) {
				results.add(d);
			}
		}
		return results;
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.consumption.schneider.pm3200";
	}

	@Override
	public String getDisplayName() {
		return "PM3200 Series Meter";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(12);
		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(), true));
		results.add(new BasicTitleSettingSpecifier("sample", getSampleMessage(getSample()), true));

		results.addAll(getIdentifiableSettingSpecifiers());
		results.addAll(getModbusNetworkSettingSpecifiers());

		PM3200DatumDataSource defaults = new PM3200DatumDataSource();
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(defaults.getSampleCacheMs())));
		results.add(new BasicTextFieldSettingSpecifier("sourceMappingValue",
				defaults.getSourceMappingValue()));
		return results;
	}

	/**
	 * Get the source ID mapping.
	 *
	 * @return the mapping
	 */
	public Map<AcPhase, String> getSourceMapping() {
		return sourceMapping;
	}

	/**
	 * Set the source ID mapping.
	 *
	 * @param sourceMapping
	 *        the mapping to set
	 */
	public void setSourceMapping(Map<AcPhase, String> sourceMapping) {
		this.sourceMapping = sourceMapping;
	}

	/**
	 * Set a {@code sourceMapping} Map via an encoded String value.
	 *
	 * <p>
	 * The format of the {@code mapping} String should be:
	 * </p>
	 *
	 * <pre>
	 * key=val[,key=val,...]
	 * </pre>
	 *
	 * <p>
	 * Whitespace is permitted around all delimiters, and will be stripped from
	 * the keys and values.
	 * </p>
	 *
	 * @param mapping
	 *        the encoding mapping
	 * @see #getSourceMappingValue()
	 */
	public void setSourceMappingValue(String mapping) {
		Map<String, String> m = StringUtils.commaDelimitedStringToMap(mapping);
		Map<AcPhase, String> kindMap = new EnumMap<>(AcPhase.class);
		if ( m != null )
			for ( Map.Entry<String, String> me : m.entrySet() ) {
				String k = me.getKey();
				AcPhase mk;
				try {
					mk = AcPhase.valueOf(k);
				} catch ( RuntimeException e ) {
					log.info("'{}' is not a valid AcPhase value, ignoring.", k);
					continue;
				}
				kindMap.put(mk, me.getValue());
			}
		setSourceMapping(kindMap);
	}

	/**
	 * Get a delimited string representation of the {@link #getSourceMapping()}
	 * map.
	 *
	 * <p>
	 * The format of the {@code mapping} String should be:
	 * </p>
	 *
	 * <pre>
	 * key=val[,key=val,...]
	 * </pre>
	 *
	 * @return the encoded mapping
	 * @see #setSourceMappingValue(String)
	 */
	public String getSourceMappingValue() {
		return StringUtils.delimitedStringFromMap(sourceMapping);
	}

	/**
	 * Get a source ID value for a given measurement kind.
	 *
	 * @param kind
	 *        the measurement kind
	 * @return the source ID value, or {@literal null} if not available
	 */
	public String getSourceIdForACPhase(AcPhase kind) {
		return (sourceMapping == null ? null : sourceMapping.get(kind));
	}

	/**
	 * Test if the {@code Total} phase should be captured.
	 *
	 * @return {@literal true} if the {@code sourceMapping} contains a
	 *         {@code Total} key
	 */
	public boolean isCaptureTotal() {
		return (sourceMapping != null && sourceMapping.containsKey(AcPhase.Total));
	}

	/**
	 * Test if the {@code PhaseA} phase should be captured.
	 *
	 * @return {@literal true} if the {@code sourceMapping} contains a
	 *         {@code PhaseA} key
	 */
	public boolean isCapturePhaseA() {
		return (sourceMapping != null && sourceMapping.containsKey(AcPhase.PhaseA));
	}

	/**
	 * Test if the {@code PhaseB} phase should be captured.
	 *
	 * @return {@literal true} if the {@code sourceMapping} contains a
	 *         {@code PhaseB} key
	 */
	public boolean isCapturePhaseB() {
		return (sourceMapping != null && sourceMapping.containsKey(AcPhase.PhaseB));
	}

	/**
	 * Test if the {@code PhaseC} phase should be captured.
	 *
	 * @return {@literal true} if the {@code sourceMapping} contains a
	 *         {@code PhaseC} key
	 */
	public boolean isCapturePhaseC() {
		return (sourceMapping != null && sourceMapping.containsKey(AcPhase.PhaseC));
	}

	/**
	 * Get the "backwards" current direction flag.
	 *
	 * @return the backwards flag
	 * @since 2.0
	 */
	public boolean isBackwards() {
		return backwards;
	}

	/**
	 * Toggle the "backwards" current direction flag.
	 *
	 * @param backwards
	 *        {@literal true} to swap energy delivered and received values
	 * @since 2.0
	 */
	public void setBackwards(boolean backwards) {
		this.backwards = backwards;
	}

}
