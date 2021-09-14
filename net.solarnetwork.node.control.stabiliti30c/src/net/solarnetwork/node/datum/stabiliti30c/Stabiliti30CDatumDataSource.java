/* ==================================================================
 * Stabiliti30CDatumDataSource.java - 10/09/2019 10:51:13 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.stabiliti30c;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import static net.solarnetwork.util.DateUtils.formatForLocalDisplay;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.AcDcEnergyDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleAcDcEnergyDatum;
import net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cData;
import net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cDataAccessor;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.support.ModbusDataDatumDataSourceSupport;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;

/**
 * Datum data source for Stabiliti 30C devices.
 * 
 * <p>
 * This data source exposes 3 datum sources, one for each of the 3 ports on the
 * Stabiliti 30C.
 * </p>
 * 
 * @author matt
 * @version 2.0
 */
public class Stabiliti30CDatumDataSource extends ModbusDataDatumDataSourceSupport<Stabiliti30cData>
		implements MultiDatumDataSource, SettingSpecifierProvider {

	private String p1SourceId;
	private String p2SourceId;
	private String p3SourceId;

	/**
	 * Default constructor.
	 */
	public Stabiliti30CDatumDataSource() {
		this(new Stabiliti30cData());
	}

	/**
	 * Construct with a specific sample data instance.
	 * 
	 * @param sample
	 *        the sample data to use
	 */
	public Stabiliti30CDatumDataSource(Stabiliti30cData sample) {
		super(sample);
	}

	@Override
	protected void refreshDeviceInfo(ModbusConnection connection, Stabiliti30cData sample)
			throws IOException {
		sample.readConfigurationData(connection);
	}

	@Override
	protected void refreshDeviceData(ModbusConnection connection, Stabiliti30cData sample)
			throws IOException {
		sample.readPowerControlData(connection);
	}

	@Override
	public Class<? extends NodeDatum> getMultiDatumType() {
		return AcDcEnergyDatum.class;
	}

	@Override
	public Collection<NodeDatum> readMultipleDatum() {
		try {
			final Stabiliti30cDataAccessor sample = getCurrentSample();
			if ( sample == null ) {
				return null;
			}
			List<NodeDatum> results = new ArrayList<>(3);
			final String s1 = getP1SourceId();
			if ( s1 != null && !s1.isEmpty() ) {
				results.add(createP1Datum(sample, s1));
			}
			final String s2 = getP2SourceId();
			if ( s2 != null && !s2.isEmpty() ) {
				results.add(createP2Datum(sample, s2));
			}
			final String s3 = getP3SourceId();
			if ( s3 != null && !s3.isEmpty() ) {
				results.add(createP3Datum(sample, s3));
			}
			return results;
		} catch ( IOException e ) {
			log.error("Communication problem reading sources {} from Stabiliti 30C device {}: {}",
					asList(p1SourceId, p2SourceId, p3SourceId).stream().filter(s -> s != null)
							.collect(toList()),
					modbusDeviceName(), e.getMessage());
			return null;
		}
	}

	private AcDcEnergyDatum createP1Datum(Stabiliti30cDataAccessor sample, String sourceId) {
		SimpleAcDcEnergyDatum d = new SimpleAcDcEnergyDatum(resolvePlaceholders(sourceId),
				sample.getDataTimestamp(), new DatumSamples());
		d.setWatts(sample.getP1ActivePower());

		if ( sample.getP1ControlMethod() != null ) {
			d.asMutableSampleOperations().putSampleValue(Status, "controlMethod",
					sample.getP1ControlMethod().toString());
		}
		d.asMutableSampleOperations().putSampleValue(Status, "powerSetpoint",
				sample.getP1ActivePowerSetpoint());
		d.asMutableSampleOperations().putSampleValue(Status, "currentLimit", sample.getP1CurrentLimit());
		d.asMutableSampleOperations().putSampleValue(Status, "frequencySetpoint",
				sample.getP1FrequencySetpoint());
		if ( sample.getP1PortType() != null ) {
			d.asMutableSampleOperations().putSampleValue(Status, "portType",
					sample.getP1PortType().toString());
		}
		d.asMutableSampleOperations().putSampleValue(Status, "voltageSetpoint",
				sample.getP1VoltageSetpoint());
		return d;
	}

	private AcDcEnergyDatum createP2Datum(Stabiliti30cDataAccessor sample, String sourceId) {
		SimpleAcDcEnergyDatum d = new SimpleAcDcEnergyDatum(resolvePlaceholders(sourceId),
				sample.getDataTimestamp(), new DatumSamples());
		d.setCurrent(sample.getP2Current());
		d.setWatts(sample.getP2Power());
		d.setVoltage(sample.getP2Voltage());

		if ( sample.getP2ControlMethod() != null ) {
			d.asMutableSampleOperations().putSampleValue(Status, "controlMethod",
					sample.getP2ControlMethod().toString());
		}
		d.asMutableSampleOperations().putSampleValue(Status, "currentLimit", sample.getP2CurrentLimit());
		d.asMutableSampleOperations().putSampleValue(Status, "currentSetpoint",
				sample.getP2CurrentSetpoint());
		d.asMutableSampleOperations().putSampleValue(Status, "exportPowerLimit",
				sample.getP2ExportPowerLimit());
		d.asMutableSampleOperations().putSampleValue(Status, "importPowerLimit",
				sample.getP2ImportPowerLimit());
		d.asMutableSampleOperations().putSampleValue(Status, "powerSetpoint",
				sample.getP2PowerSetpoint());
		d.asMutableSampleOperations().putSampleValue(Status, "voltageMinimumLimit",
				sample.getP2VoltageMinimumLimit());
		d.asMutableSampleOperations().putSampleValue(Status, "voltageMaximumLimit",
				sample.getP2VoltageMaximumLimit());

		return d;
	}

	private AcDcEnergyDatum createP3Datum(Stabiliti30cDataAccessor sample, String sourceId) {
		SimpleAcDcEnergyDatum d = new SimpleAcDcEnergyDatum(resolvePlaceholders(sourceId),
				sample.getDataTimestamp(), new DatumSamples());
		d.setCurrent(sample.getP3Current());
		d.setWatts(sample.getP3Power());
		d.setVoltage(sample.getP3Voltage());

		if ( sample.getP3ControlMethod() != null ) {
			d.asMutableSampleOperations().putSampleValue(Status, "controlMethod",
					sample.getP3ControlMethod().toString());
		}
		d.asMutableSampleOperations().putSampleValue(Status, "currentLimit", sample.getP3CurrentLimit());
		d.asMutableSampleOperations().putSampleValue(Status, "currentLimit", sample.getP3CurrentLimit());
		d.asMutableSampleOperations().putSampleValue(Status, "importPowerLimit",
				sample.getP3ImportPowerLimit());
		d.asMutableSampleOperations().putSampleValue(Status, "mpptStartTimeOffsetSetpoint",
				sample.getP3MpptStartTimeOffsetSetpoint());
		d.asMutableSampleOperations().putSampleValue(Status, "mpptStopTimeOffsetSetpoint",
				sample.getP3MpptStopTimeOffsetSetpoint());
		d.asMutableSampleOperations().putSampleValue(Status, "mpptVoltageMinimumSetpoint",
				sample.getP3MpptVoltageMinimumSetpoint());
		d.asMutableSampleOperations().putSampleValue(Status, "voltageMinimum",
				sample.getP3VoltageMinimum());
		d.asMutableSampleOperations().putSampleValue(Status, "voltageMaximum",
				sample.getP3VoltageMaximum());

		return d;
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.control.stabiliti30c.DatumDataSource";
	}

	@Override
	public String getDisplayName() {
		return "Stabiliti 30C Power Control";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(12);
		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(), true));
		results.add(new BasicTitleSettingSpecifier("sample", getSampleMessage(getSample()), true));

		results.addAll(getIdentifiableSettingSpecifiers());
		results.addAll(getModbusNetworkSettingSpecifiers());

		Stabiliti30CDatumDataSource defaults = new Stabiliti30CDatumDataSource();
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(defaults.getSampleCacheMs())));
		results.add(new BasicTextFieldSettingSpecifier("p1SourceId", ""));
		results.add(new BasicTextFieldSettingSpecifier("p2SourceId", ""));
		results.add(new BasicTextFieldSettingSpecifier("p3SourceId", ""));

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

	private String getSampleMessage(Stabiliti30cDataAccessor data) {
		if ( data.getDataTimestamp() == null ) {
			return "N/A";
		}
		StringBuilder buf = new StringBuilder();
		buf.append("P1 = ").append(
				data.getP1ControlMethod() != null ? data.getP1ControlMethod().getDescription() : "N/A");
		buf.append(", P1 AC W = ").append(data.getP1ActivePower());
		buf.append(", P2 = ").append(
				data.getP2ControlMethod() != null ? data.getP2ControlMethod().getDescription() : "N/A");
		buf.append(", P2 DC W = ").append(data.getP2Power());
		buf.append(", P3 = ").append(
				data.getP3ControlMethod() != null ? data.getP3ControlMethod().getDescription() : "N/A");
		buf.append(", P3 DC W = ").append(data.getP3Power());
		buf.append("; sampled at ").append(formatForLocalDisplay(data.getDataTimestamp()));
		return buf.toString();
	}

	/**
	 * Get the P1 (AC, grid) source ID.
	 * 
	 * @return the P1 source ID
	 */
	public String getP1SourceId() {
		return p1SourceId;
	}

	/**
	 * Set the P1 (AC, grid) source ID.
	 * 
	 * @param p1SourceId
	 *        the source ID to use
	 */
	public void setP1SourceId(String p1SourceId) {
		this.p1SourceId = p1SourceId;
	}

	/**
	 * Get the P2 (DC, battery) source ID.
	 * 
	 * @return the P2 source ID
	 */
	public String getP2SourceId() {
		return p2SourceId;
	}

	/**
	 * Set the P2 (DC, battery) source ID.
	 * 
	 * @param p2SourceId
	 *        the source ID to use
	 */
	public void setP2SourceId(String p2SourceId) {
		this.p2SourceId = p2SourceId;
	}

	/**
	 * Get the P3 (DC, PV) source ID.
	 * 
	 * @return the P3 source ID
	 */
	public String getP3SourceId() {
		return p3SourceId;
	}

	/**
	 * Set the P3 (DC, PV) source ID.
	 * 
	 * @param p3SourceId
	 *        the source ID to use
	 */
	public void setP3SourceId(String p3SourceId) {
		this.p3SourceId = p3SourceId;
	}

}
