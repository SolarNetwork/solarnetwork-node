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
import static net.solarnetwork.node.domain.ACEnergyDatum.CURRENT_KEY;
import static net.solarnetwork.node.domain.PVEnergyDatum.VOLTAGE_KEY;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.domain.GeneralNodeEnergyDatum;
import net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cData;
import net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cDataAccessor;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.support.ModbusDataDatumDataSourceSupport;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;

/**
 * Datum data source for Stabiliti 30C devices.
 * 
 * <p>
 * This data source exposes 3 datum sources, one for each of the 3 ports on the
 * Stabiliti 30C.
 * </p>
 * 
 * @author matt
 * @version 1.1
 */
public class Stabiliti30CDatumDataSource extends ModbusDataDatumDataSourceSupport<Stabiliti30cData>
		implements MultiDatumDataSource<GeneralNodeEnergyDatum>, SettingSpecifierProvider {

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
			.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.FULL).withZone(ZoneId.systemDefault());

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
	public Class<? extends GeneralNodeEnergyDatum> getMultiDatumType() {
		return GeneralNodeEnergyDatum.class;
	}

	@Override
	public Collection<GeneralNodeEnergyDatum> readMultipleDatum() {
		try {
			final Stabiliti30cDataAccessor sample = getCurrentSample();
			if ( sample == null ) {
				return null;
			}
			List<GeneralNodeEnergyDatum> results = new ArrayList<>(3);
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

	private GeneralNodeACEnergyDatum createP1Datum(Stabiliti30cDataAccessor sample, String sourceId) {
		GeneralNodeACEnergyDatum d = new GeneralNodeACEnergyDatum();
		d.setSourceId(sourceId);
		d.setCreated(new Date(sample.getDataTimestamp()));

		d.setWatts(sample.getP1ActivePower());

		if ( sample.getP1ControlMethod() != null ) {
			d.putStatusSampleValue("controlMethod", sample.getP1ControlMethod().toString());
		}
		d.putStatusSampleValue("powerSetpoint", sample.getP1ActivePowerSetpoint());
		d.putStatusSampleValue("currentLimit", sample.getP1CurrentLimit());
		d.putStatusSampleValue("frequencySetpoint", sample.getP1FrequencySetpoint());
		if ( sample.getP1PortType() != null ) {
			d.putStatusSampleValue("portType", sample.getP1PortType().toString());
		}
		d.putStatusSampleValue("voltageSetpoint", sample.getP1VoltageSetpoint());
		return d;
	}

	private GeneralNodeEnergyDatum createP2Datum(Stabiliti30cDataAccessor sample, String sourceId) {
		GeneralNodeEnergyDatum d = new GeneralNodeEnergyDatum();
		d.setSourceId(sourceId);
		d.setCreated(new Date(sample.getDataTimestamp()));

		d.putInstantaneousSampleValue(CURRENT_KEY, sample.getP2Current());
		d.setWatts(sample.getP2Power());
		d.putInstantaneousSampleValue(VOLTAGE_KEY, sample.getP2Voltage());

		if ( sample.getP2ControlMethod() != null ) {
			d.putStatusSampleValue("controlMethod", sample.getP2ControlMethod().toString());
		}
		d.putStatusSampleValue("currentLimit", sample.getP2CurrentLimit());
		d.putStatusSampleValue("currentSetpoint", sample.getP2CurrentSetpoint());
		d.putStatusSampleValue("exportPowerLimit", sample.getP2ExportPowerLimit());
		d.putStatusSampleValue("importPowerLimit", sample.getP2ImportPowerLimit());
		d.putStatusSampleValue("powerSetpoint", sample.getP2PowerSetpoint());
		d.putStatusSampleValue("voltageMinimumLimit", sample.getP2VoltageMinimumLimit());
		d.putStatusSampleValue("voltageMaximumLimit", sample.getP2VoltageMaximumLimit());

		return d;
	}

	private GeneralNodeEnergyDatum createP3Datum(Stabiliti30cDataAccessor sample, String sourceId) {
		GeneralNodeEnergyDatum d = new GeneralNodeEnergyDatum();
		d.setSourceId(sourceId);
		d.setCreated(new Date(sample.getDataTimestamp()));

		d.putInstantaneousSampleValue(CURRENT_KEY, sample.getP3Current());
		d.setWatts(sample.getP3Power());
		d.putInstantaneousSampleValue(VOLTAGE_KEY, sample.getP3Voltage());

		if ( sample.getP3ControlMethod() != null ) {
			d.putStatusSampleValue("controlMethod", sample.getP3ControlMethod().toString());
		}
		d.putStatusSampleValue("currentLimit", sample.getP3CurrentLimit());
		d.putStatusSampleValue("currentLimit", sample.getP3CurrentLimit());
		d.putStatusSampleValue("importPowerLimit", sample.getP3ImportPowerLimit());
		d.putStatusSampleValue("mpptStartTimeOffsetSetpoint", sample.getP3MpptStartTimeOffsetSetpoint());
		d.putStatusSampleValue("mpptStopTimeOffsetSetpoint", sample.getP3MpptStopTimeOffsetSetpoint());
		d.putStatusSampleValue("mpptVoltageMinimumSetpoint", sample.getP3MpptVoltageMinimumSetpoint());
		d.putStatusSampleValue("voltageMinimum", sample.getP3VoltageMinimum());
		d.putStatusSampleValue("voltageMaximum", sample.getP3VoltageMaximum());

		return d;
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
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
		if ( data.getDataTimestamp() < 1 ) {
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

		final String ts = DATE_FORMAT.format(Instant.ofEpochMilli(data.getDataTimestamp()));
		buf.append("; sampled at ").append(ts);
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
