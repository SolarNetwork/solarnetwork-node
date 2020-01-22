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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.hw.deson.meter.SDMData;
import net.solarnetwork.node.hw.deson.meter.SDMDeviceType;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusDataDatumDataSourceSupport;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicRadioGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.StringUtils;

/**
 * {@link DatumDataSource} implementation for {@link GeneralNodeACEnergyDatum}
 * with the SDM series watt meter.
 * 
 * @author matt
 * @version 2.0
 */
public class SDMDatumDataSource extends ModbusDataDatumDataSourceSupport<SDMData>
		implements DatumDataSource<GeneralNodeACEnergyDatum>,
		MultiDatumDataSource<GeneralNodeACEnergyDatum>, SettingSpecifierProvider {

	/** The default source ID applied for the total reading values. */
	public static final String MAIN_SOURCE_ID = "Main";

	// a mapping of AC phase to source ID
	private Map<ACPhase, String> sourceMapping = getDefaulSourceMapping();

	// the "installed backwards" setting
	private boolean backwards = false;

	/**
	 * Get a default {@code sourceMapping} value. This maps only the
	 * {@code Total} phase to the value {@code Main}.
	 * 
	 * @return mapping
	 */
	public static Map<ACPhase, String> getDefaulSourceMapping() {
		Map<ACPhase, String> result = new EnumMap<ACPhase, String>(ACPhase.class);
		result.put(ACPhase.Total, MAIN_SOURCE_ID);
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
	protected void refreshDeviceInfo(ModbusConnection connection, SDMData sample) {
		sample.readConfigurationData(connection);
	}

	@Override
	protected void refreshDeviceData(ModbusConnection connection, SDMData sample) {
		sample.readMeterData(connection);
	}

	@Override
	public Class<? extends GeneralNodeACEnergyDatum> getDatumType() {
		return SDMDatum.class;
	}

	@Override
	public GeneralNodeACEnergyDatum readCurrentDatum() {
		final long start = System.currentTimeMillis();
		final String sourceId = getSourceMapping().get(ACPhase.Total);
		try {
			final SDMData currSample = getCurrentSample();
			if ( currSample == null ) {
				return null;
			}
			SDMDatum d = new SDMDatum(currSample, ACPhase.Total, backwards);
			d.setSourceId(sourceId);
			if ( currSample.getDataTimestamp() >= start ) {
				// we read from the device
				postDatumCapturedEvent(d);
			}
			return d;
		} catch ( IOException e ) {
			log.error("Communication problem reading source {} from PM3200 device {}: {}", sourceId,
					modbusDeviceName(), e.getMessage());
			return null;
		}
	}

	@Override
	public Class<? extends GeneralNodeACEnergyDatum> getMultiDatumType() {
		return SDMDatum.class;
	}

	@Override
	public Collection<GeneralNodeACEnergyDatum> readMultipleDatum() {
		final long start = System.currentTimeMillis();
		final List<GeneralNodeACEnergyDatum> results = new ArrayList<GeneralNodeACEnergyDatum>(4);
		final SDMData currSample;
		try {
			currSample = getCurrentSample();
		} catch ( IOException e ) {
			log.error("Communication problem readiong from SDM device: {}", e.getMessage());
			return results;
		}
		if ( currSample == null ) {
			return results;
		}
		final boolean postCapturedEvent = (currSample.getDataTimestamp() >= start);
		if ( isCaptureTotal() || postCapturedEvent ) {
			SDMDatum d = new SDMDatum(currSample, ACPhase.Total, backwards);
			d.setSourceId(getSourceMapping().get(ACPhase.Total));
			if ( postCapturedEvent ) {
				// we read from the meter
				postDatumCapturedEvent(d);
			}
			if ( isCaptureTotal() ) {
				results.add(d);
			}
		}
		if ( currSample.supportsPhase(ACPhase.PhaseA) && (isCapturePhaseA() || postCapturedEvent) ) {
			SDMDatum d = new SDMDatum(currSample, ACPhase.PhaseA, backwards);
			d.setSourceId(getSourceMapping().get(ACPhase.PhaseA));
			if ( postCapturedEvent ) {
				// we read from the meter
				postDatumCapturedEvent(d);
			}
			if ( isCapturePhaseA() ) {
				results.add(d);
			}
		}
		if ( currSample.supportsPhase(ACPhase.PhaseB) && (isCapturePhaseB() || postCapturedEvent) ) {
			SDMDatum d = new SDMDatum(currSample, ACPhase.PhaseB, backwards);
			d.setSourceId(getSourceMapping().get(ACPhase.PhaseB));
			if ( postCapturedEvent ) {
				// we read from the meter
				postDatumCapturedEvent(d);
			}
			if ( isCapturePhaseB() ) {
				results.add(d);
			}
		}
		if ( currSample.supportsPhase(ACPhase.PhaseC) && (isCapturePhaseC() || postCapturedEvent) ) {
			SDMDatum d = new SDMDatum(currSample, ACPhase.PhaseC, backwards);
			d.setSourceId(getSourceMapping().get(ACPhase.PhaseC));
			if ( postCapturedEvent ) {
				// we read from the meter
				postDatumCapturedEvent(d);
			}
			if ( isCapturePhaseC() ) {
				results.add(d);
			}
		}
		return results;
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
		if ( data.getDataTimestamp() < 1 ) {
			return "N/A";
		}
		StringBuilder buf = new StringBuilder();
		buf.append("W = ").append(data.getActivePower());
		buf.append(", VAR = ").append(data.getReactivePower());
		buf.append(", Wh rec = ").append(data.getActiveEnergyReceived());
		buf.append(", Wh del = ").append(data.getActiveEnergyDelivered());
		buf.append("; sampled at ")
				.append(DateTimeFormat.forStyle("LS").print(new DateTime(data.getDataTimestamp())));
		return buf.toString();
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
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

		return results;
	}

	/**
	 * Test if the {@code Total} phase should be captured.
	 * 
	 * @return <em>true</em> if the {@code sourceMapping} contains a
	 *         {@code Total} key
	 */
	public boolean isCaptureTotal() {
		return (sourceMapping != null && sourceMapping.containsKey(ACPhase.Total));
	}

	/**
	 * Test if the {@code PhaseA} phase should be captured.
	 * 
	 * @return <em>true</em> if the {@code sourceMapping} contains a
	 *         {@code PhaseA} key
	 */
	public boolean isCapturePhaseA() {
		return (sourceMapping != null && sourceMapping.containsKey(ACPhase.PhaseA));
	}

	/**
	 * Test if the {@code PhaseB} phase should be captured.
	 * 
	 * @return <em>true</em> if the {@code sourceMapping} contains a
	 *         {@code PhaseB} key
	 */
	public boolean isCapturePhaseB() {
		return (sourceMapping != null && sourceMapping.containsKey(ACPhase.PhaseB));
	}

	/**
	 * Test if the {@code PhaseC} phase should be captured.
	 * 
	 * @return <em>true</em> if the {@code sourceMapping} contains a
	 *         {@code PhaseC} key
	 */
	public boolean isCapturePhaseC() {
		return (sourceMapping != null && sourceMapping.containsKey(ACPhase.PhaseC));
	}

	/**
	 * Get the configured mapping from AC phase constants to source ID values.
	 * 
	 * @return The source mapping.
	 */
	public Map<ACPhase, String> getSourceMapping() {
		return sourceMapping;
	}

	/**
	 * Configure a mapping from AC phase constants to source ID values.
	 * 
	 * @param sourceMapping
	 *        The source mappinng to set.
	 */
	public void setSourceMapping(Map<ACPhase, String> sourceMapping) {
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
		Map<ACPhase, String> kindMap = new EnumMap<ACPhase, String>(ACPhase.class);
		if ( m != null )
			for ( Map.Entry<String, String> me : m.entrySet() ) {
				String k = me.getKey();
				ACPhase mk;
				try {
					mk = ACPhase.valueOf(k);
				} catch ( RuntimeException e ) {
					log.info("'{}' is not a valid ACPhase value, ignoring.", k);
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
	 * @return
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
	 * @param backwards
	 *        {@literal true} if should interpret the meter data as "backwards"
	 *        in terms of the direction of current
	 */
	public void setBackwards(boolean value) {
		this.backwards = value;
	}
}
