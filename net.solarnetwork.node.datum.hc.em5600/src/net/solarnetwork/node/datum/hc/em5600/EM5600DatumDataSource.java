/* ==================================================================
 * EM5600DatumDataSource.java - Mar 26, 2014 10:13:12 AM
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

package net.solarnetwork.node.datum.hc.em5600;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.domain.ACEnergyDatum;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.domain.EnergyDatum;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.hw.hc.EM5600Data;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusDataDatumDataSourceSupport;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.StringUtils;

/**
 * {@link DatumDataSource} implementation for {@link ACEnergyDatum} with the
 * EM5600 series watt meter.
 * 
 * @author matt
 * @version 2.0
 */
public class EM5600DatumDataSource extends ModbusDataDatumDataSourceSupport<EM5600Data>
		implements DatumDataSource<ACEnergyDatum>, MultiDatumDataSource<ACEnergyDatum>,
		SettingSpecifierProvider {

	/** The default source ID. */
	public static final String MAIN_SOURCE_ID = "Main";

	private Map<ACPhase, String> sourceMapping = getDefaulSourceMapping();
	private boolean backwards = false;
	private boolean tagConsumption = true;

	/**
	 * Get a default {@code sourceMapping} value. This maps only the {@code 0}
	 * source to the value {@code Main}.
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
	public EM5600DatumDataSource() {
		this(new EM5600Data());
	}

	/**
	 * Construct with a specific sample data instance.
	 * 
	 * @param sample
	 *        the sample data to use
	 */
	public EM5600DatumDataSource(EM5600Data sample) {
		super(sample);
	}

	@Override
	protected void refreshDeviceInfo(ModbusConnection connection, EM5600Data sample) {
		sample.readConfigurationData(connection);
	}

	@Override
	protected void refreshDeviceData(ModbusConnection connection, EM5600Data sample) {
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

	private String getSampleMessage(EM5600Data data) {
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

	@Override
	public Class<? extends GeneralNodeACEnergyDatum> getDatumType() {
		return EM5600Datum.class;
	}

	@Override
	public GeneralNodeACEnergyDatum readCurrentDatum() {
		final long start = System.currentTimeMillis();
		final String sourceId = getSourceMapping().get(ACPhase.Total);
		try {
			final EM5600Data currSample = getCurrentSample();
			if ( currSample == null ) {
				return null;
			}
			EM5600Datum d = new EM5600Datum(currSample, ACPhase.Total, backwards);
			d.setSourceId(sourceId);
			if ( currSample.getDataTimestamp() >= start ) {
				// we read from the device
				postDatumCapturedEvent(d);
			}
			return d;
		} catch ( IOException e ) {
			log.error("Communication problem reading source {} from EM5600 device {}: {}", sourceId,
					modbusDeviceName(), e.getMessage());
			return null;
		}
	}

	@Override
	public Class<? extends GeneralNodeACEnergyDatum> getMultiDatumType() {
		return EM5600Datum.class;
	}

	@Override
	public Collection<ACEnergyDatum> readMultipleDatum() {
		final long start = System.currentTimeMillis();
		final List<ACEnergyDatum> results = new ArrayList<ACEnergyDatum>(4);
		final EM5600Data currSample;
		try {
			currSample = getCurrentSample();
		} catch ( IOException e ) {
			log.error("Communication problem readiong from PM3200 device: {}", e.getMessage());
			return results;
		}
		if ( currSample == null ) {
			return results;
		}
		final List<EM5600Datum> capturedResults = new ArrayList<EM5600Datum>(4);
		final boolean postCapturedEvent = (currSample.getDataTimestamp() >= start);
		if ( isCaptureTotal() || postCapturedEvent ) {
			EM5600Datum d = new EM5600Datum(currSample, ACPhase.Total, backwards);
			d.setSourceId(getSourceMapping().get(ACPhase.Total));
			if ( postCapturedEvent ) {
				// we read from the meter
				postDatumCapturedEvent(d);
			}
			if ( isCaptureTotal() ) {
				results.add(d);
				if ( postCapturedEvent ) {
					capturedResults.add(d);
				}
			}
		}
		if ( isCapturePhaseA() || postCapturedEvent ) {
			EM5600Datum d = new EM5600Datum(currSample, ACPhase.PhaseA, backwards);
			d.setSourceId(getSourceMapping().get(ACPhase.PhaseA));
			if ( postCapturedEvent ) {
				// we read from the meter
				postDatumCapturedEvent(d);
			}
			if ( isCapturePhaseA() ) {
				results.add(d);
				if ( postCapturedEvent ) {
					capturedResults.add(d);
				}
			}
		}
		if ( isCapturePhaseB() || postCapturedEvent ) {
			EM5600Datum d = new EM5600Datum(currSample, ACPhase.PhaseB, backwards);
			d.setSourceId(getSourceMapping().get(ACPhase.PhaseB));
			if ( postCapturedEvent ) {
				// we read from the meter
				postDatumCapturedEvent(d);
			}
			if ( isCapturePhaseB() ) {
				results.add(d);
				if ( postCapturedEvent ) {
					capturedResults.add(d);
				}
			}
		}
		if ( isCapturePhaseC() || postCapturedEvent ) {
			EM5600Datum d = new EM5600Datum(currSample, ACPhase.PhaseC, backwards);
			d.setSourceId(getSourceMapping().get(ACPhase.PhaseC));
			if ( postCapturedEvent ) {
				// we read from the meter
				postDatumCapturedEvent(d);
			}
			if ( isCapturePhaseC() ) {
				results.add(d);
				if ( postCapturedEvent ) {
					capturedResults.add(d);
				}
			}
		}

		for ( EM5600Datum d : capturedResults ) {
			addEnergyDatumSourceMetadata(d);
		}

		return results;
	}

	private void addEnergyDatumSourceMetadata(EM5600Datum d) {
		// associate consumption/generation tags with this source
		GeneralDatumMetadata sourceMeta = new GeneralDatumMetadata();
		if ( isTagConsumption() ) {
			sourceMeta.addTag(EnergyDatum.TAG_CONSUMPTION);
		} else {
			sourceMeta.addTag(EnergyDatum.TAG_GENERATION);
		}
		addSourceMetadata(d.getSourceId(), sourceMeta);
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.datum.hc.em5600";
	}

	@Override
	public String getDisplayName() {
		return "EM5600 Series Meter";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(12);
		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(), true));
		results.add(new BasicTitleSettingSpecifier("sample", getSampleMessage(getSample()), true));

		results.addAll(getIdentifiableSettingSpecifiers());
		results.addAll(getModbusNetworkSettingSpecifiers());

		EM5600DatumDataSource defaults = new EM5600DatumDataSource();
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(defaults.getSampleCacheMs())));
		results.add(new BasicTextFieldSettingSpecifier("sourceMappingValue",
				defaults.getSourceMappingValue()));
		results.add(new BasicToggleSettingSpecifier("tagConsumption",
				Boolean.valueOf(defaults.isTagConsumption())));
		return results;
	}

	/**
	 * Get the consumption/generation tag setting.
	 * 
	 * @return {@literal true} to tag as consumption, {@literal false} for
	 *         generation
	 */
	public boolean isTagConsumption() {
		return tagConsumption;
	}

	/**
	 * Configure the consumption/generation tag setting.
	 * 
	 * <p>
	 * When {@literal true} then tag the configured source with
	 * {@link EnergyDatum#TAG_CONSUMPTION}. When {@literal false} then tag the
	 * configured source with {@link EnergyDatum#TAG_GENERATION}. Requires the
	 * {@link #getDatumMetadataService()} to be available.
	 * </p>
	 * 
	 * @param tagConsumption
	 *        {@literal true} to tag as consumption, {@literal false} for
	 *        generation
	 */
	public void setTagConsumption(boolean tagConsumption) {
		this.tagConsumption = tagConsumption;
	}

	public Map<ACPhase, String> getSourceMapping() {
		return sourceMapping;
	}

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
		Map<ACPhase, String> kindMap = new EnumMap<>(ACPhase.class);
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
	 * Get a source ID value for a given measurement kind.
	 * 
	 * @param kind
	 *        the measurement kind
	 * @return the source ID value, or <em>null</em> if not available
	 */
	public String getSourceIdForACPhase(ACPhase kind) {
		return (sourceMapping == null ? null : sourceMapping.get(kind));
	}

	public boolean isCaptureTotal() {
		return (sourceMapping != null && sourceMapping.containsKey(ACPhase.Total));
	}

	public boolean isCapturePhaseA() {
		return (sourceMapping != null && sourceMapping.containsKey(ACPhase.PhaseA));
	}

	public boolean isCapturePhaseB() {
		return (sourceMapping != null && sourceMapping.containsKey(ACPhase.PhaseB));
	}

	public boolean isCapturePhaseC() {
		return (sourceMapping != null && sourceMapping.containsKey(ACPhase.PhaseC));
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
