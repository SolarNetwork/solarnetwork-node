/* ==================================================================
 * SDMDatumDataSource.java - 26/01/2016 3:06:48 pm
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

package net.solarnetwork.node.datum.deson.sdm;

import static net.solarnetwork.util.DateUtils.formatForLocalDisplay;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.node.domain.datum.AcEnergyDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.hw.deson.meter.SDMData;
import net.solarnetwork.node.hw.deson.meter.SDMDeviceType;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.support.ModbusDataDatumDataSourceSupport;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.node.service.support.ExpressionConfig;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicRadioGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.StringUtils;

/**
 * {@link DatumDataSource} implementation for {@link AcEnergyDatum} with the SDM
 * series watt meter.
 *
 * @author matt
 * @version 3.1
 */
public class SDMDatumDataSource extends ModbusDataDatumDataSourceSupport<SDMData>
		implements DatumDataSource, MultiDatumDataSource, SettingSpecifierProvider {

	/** The default source ID applied for the total reading values. */
	public static final String MAIN_SOURCE_ID = "Main";

	// a mapping of AC phase to source ID
	private Map<AcPhase, String> sourceMapping = getDefaulSourceMapping();

	// the "installed backwards" setting
	private boolean backwards = false;

	/**
	 * Get a default {@code sourceMapping} value. This maps only the
	 * {@code Total} phase to the value {@code Main}.
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
	public SDMDatumDataSource() {
		this(new SDMData());
	}

	/**
	 * Construct with a specific sample data instance.
	 *
	 * @param sample
	 *        the sample data to use
	 */
	public SDMDatumDataSource(SDMData sample) {
		super(sample);
	}

	@Override
	protected void refreshDeviceInfo(ModbusConnection connection, SDMData sample) throws IOException {
		sample.readConfigurationData(connection);
	}

	@Override
	protected void refreshDeviceData(ModbusConnection connection, SDMData sample) throws IOException {
		sample.readMeterData(connection);
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return AcEnergyDatum.class;
	}

	@Override
	public AcEnergyDatum readCurrentDatum() {
		final String sourceId = resolvePlaceholders(getSourceMapping().get(AcPhase.Total));
		try {
			final SDMData currSample = getCurrentSample();
			if ( currSample == null ) {
				return null;
			}
			return new SDMDatum(currSample, resolvePlaceholders(sourceId), AcPhase.Total, backwards);
		} catch ( IOException e ) {
			log.error("Communication problem reading source {} from SDM device {}: {}", sourceId,
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
		final List<NodeDatum> results = new ArrayList<NodeDatum>(4);
		final SDMData currSample;
		try {
			currSample = getCurrentSample();
		} catch ( IOException e ) {
			log.error("Communication problem reading from SDM device {}: {}", modbusDeviceName(),
					e.getMessage());
			return results;
		}
		if ( currSample == null ) {
			return results;
		}
		if ( isCaptureTotal() ) {
			SDMDatum d = new SDMDatum(currSample,
					resolvePlaceholders(getSourceMapping().get(AcPhase.Total)), AcPhase.Total,
					backwards);
			populateExpressionDatumProperties(d, getExpressionConfigs());
			if ( isCaptureTotal() ) {
				results.add(d);
			}
		}
		if ( currSample.supportsPhase(AcPhase.PhaseA) && (isCapturePhaseA()) ) {
			SDMDatum d = new SDMDatum(currSample,
					resolvePlaceholders(getSourceMapping().get(AcPhase.PhaseA)), AcPhase.PhaseA,
					backwards);
			populateExpressionDatumProperties(d, getExpressionConfigs());
			if ( isCapturePhaseA() ) {
				results.add(d);
			}
		}
		if ( currSample.supportsPhase(AcPhase.PhaseB) && (isCapturePhaseB()) ) {
			SDMDatum d = new SDMDatum(currSample,
					resolvePlaceholders(getSourceMapping().get(AcPhase.PhaseB)), AcPhase.PhaseB,
					backwards);
			populateExpressionDatumProperties(d, getExpressionConfigs());
			if ( isCapturePhaseB() ) {
				results.add(d);
			}
		}
		if ( currSample.supportsPhase(AcPhase.PhaseC) && (isCapturePhaseC()) ) {
			SDMDatum d = new SDMDatum(currSample,
					resolvePlaceholders(getSourceMapping().get(AcPhase.PhaseC)), AcPhase.PhaseC,
					backwards);
			populateExpressionDatumProperties(d, getExpressionConfigs());
			if ( isCapturePhaseC() ) {
				results.add(d);
			}
		}
		return results;
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

	private String getInfoMessage() {
		String msg = null;
		try {
			msg = getDeviceInfoMessage();
		} catch ( RuntimeException e ) {
			log.debug("Error reading info: {}", e.getMessage());
		}
		return (msg == null ? "N/A" : msg);
	}

	private String getSampleMessage(SDMData data) {
		if ( data.getDataTimestamp() == null ) {
			return "N/A";
		}
		StringBuilder buf = new StringBuilder();
		buf.append("W = ").append(data.getActivePower());
		buf.append(", VAR = ").append(data.getReactivePower());
		buf.append(", Wh rec = ").append(data.getActiveEnergyReceived());
		buf.append(", Wh del = ").append(data.getActiveEnergyDelivered());
		buf.append("; sampled at ").append(formatForLocalDisplay(data.getDataTimestamp()));
		return buf.toString();
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.deson.sdm";
	}

	@Override
	public String getDisplayName() {
		return "Deson SDM Series Meter";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(12);
		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(), true));
		results.add(new BasicTitleSettingSpecifier("sample", getSampleMessage(getSample()), true));

		results.addAll(getIdentifiableSettingSpecifiers());
		results.addAll(getModbusNetworkSettingSpecifiers());

		SDMDatumDataSource defaults = new SDMDatumDataSource();

		// device type radio group
		BasicRadioGroupSettingSpecifier deviceTypeSpec = new BasicRadioGroupSettingSpecifier(
				"deviceTypeValue", defaults.getDeviceTypeValue());
		Map<String, String> deviceTypeValues = new LinkedHashMap<>(3);
		for ( SDMDeviceType model : SDMDeviceType.values() ) {
			deviceTypeValues.put(model.toString(), model.toString());
		}
		deviceTypeSpec.setValueTitles(deviceTypeValues);
		results.add(deviceTypeSpec);

		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(defaults.getSampleCacheMs())));
		results.add(new BasicTextFieldSettingSpecifier("sourceMappingValue",
				defaults.getSourceMappingValue()));
		results.add(new BasicToggleSettingSpecifier("backwards", Boolean.valueOf(defaults.backwards)));

		Iterable<ExpressionService> exprServices = (getExpressionServices() != null
				? getExpressionServices().services()
				: null);
		if ( exprServices != null ) {
			ExpressionConfig[] exprConfs = getExpressionConfigs();
			List<ExpressionConfig> exprConfsList = (exprConfs != null ? Arrays.asList(exprConfs)
					: Collections.<ExpressionConfig> emptyList());
			results.add(SettingUtils.dynamicListSettingSpecifier("expressionConfigs", exprConfsList,
					new SettingUtils.KeyedListCallback<ExpressionConfig>() {

						@Override
						public Collection<SettingSpecifier> mapListSettingKey(ExpressionConfig value,
								int index, String key) {
							BasicGroupSettingSpecifier configGroup = new BasicGroupSettingSpecifier(
									ExpressionConfig.settings(DatumDataSourceSupport.class, key + ".",
											exprServices));
							return Collections.<SettingSpecifier> singletonList(configGroup);
						}
					}));
		}

		return results;
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
	 * Get the configured mapping from AC phase constants to source ID values.
	 *
	 * @return The source mapping.
	 */
	public Map<AcPhase, String> getSourceMapping() {
		return sourceMapping;
	}

	/**
	 * Configure a mapping from AC phase constants to source ID values.
	 *
	 * @param sourceMapping
	 *        The source mappinng to set.
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
		Map<AcPhase, String> kindMap = new EnumMap<AcPhase, String>(AcPhase.class);
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
	 * Get the configured device type.
	 *
	 * @return the device type
	 */
	public SDMDeviceType getDeviceType() {
		return getSample().getDeviceType();
	}

	/**
	 * Set the type of device to use. If this value changes, any cached sample
	 * data will be cleared.
	 *
	 * @param deviceType
	 *        The type of device to use.
	 */
	public void setDeviceType(final SDMDeviceType deviceType) {
		if ( deviceType == null ) {
			throw new IllegalArgumentException("The deviceType cannot be null.");
		}
		getSample().setDeviceType(deviceType);
	}

	/**
	 * Get the device type, as a string.
	 *
	 * @return The device type, as a string.
	 */
	public String getDeviceTypeValue() {
		final SDMDeviceType type = getDeviceType();
		return (type == null ? "" : type.toString());
	}

	/**
	 * Set the device type, as a string.
	 *
	 * @param type
	 *        The {@link SDMDeviceType} string value to set.
	 */
	public void setDeviceTypeValue(String type) {
		try {
			setDeviceType(SDMDeviceType.valueOf(type));
		} catch ( IllegalArgumentException e ) {
			// not supported type
		}
	}

	/**
	 * Get the backwards setting.
	 *
	 * @return {@literal true} if should interpret the meter data as "backwards"
	 */
	public boolean isBackwards() {
		return backwards;
	}

	/**
	 * Set the backwards setting.
	 *
	 * @param value
	 *        {@literal true} if should interpret the meter data as "backwards"
	 *        in terms of the direction of current
	 */
	public void setBackwards(boolean value) {
		this.backwards = value;
	}
}
