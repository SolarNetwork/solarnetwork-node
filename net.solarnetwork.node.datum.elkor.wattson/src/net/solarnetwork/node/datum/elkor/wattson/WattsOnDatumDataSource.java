/* ==================================================================
 * WattsOnDatumDataSource.java - 14/08/2020 11:49:58 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.elkor.wattson;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.hw.elkor.upt.WattsOnData;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.support.ModbusDataDatumDataSourceSupport;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;

/**
 * {@link DatumDataSource} for Elkor WattsOn meters.
 * 
 * @author matt
 * @version 1.1
 */
public class WattsOnDatumDataSource extends ModbusDataDatumDataSourceSupport<WattsOnData>
		implements DatumDataSource<GeneralNodeACEnergyDatum>,
		MultiDatumDataSource<GeneralNodeACEnergyDatum>, SettingSpecifierProvider {

	private String sourceId;
	private boolean backwards = false;
	private boolean includePhaseMeasurements = false;

	/**
	 * Constructor.
	 */
	public WattsOnDatumDataSource() {
		this(new WattsOnData());
	}

	/**
	 * Construct with a sample instance.
	 * 
	 * @param sample
	 *        the sample to use
	 */
	public WattsOnDatumDataSource(WattsOnData sample) {
		super(sample);
		sourceId = "WattsOn";
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.datum.elkor.wattson";
	}

	@Override
	public String getDisplayName() {
		return "WattsOn";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(12);
		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(), true));
		results.add(new BasicTitleSettingSpecifier("sample", getSampleMessage(getSample()), true));

		results.addAll(getIdentifiableSettingSpecifiers());
		results.addAll(getModbusNetworkSettingSpecifiers());

		WattsOnDatumDataSource defaults = new WattsOnDatumDataSource();
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(defaults.getSampleCacheMs())));
		results.add(new BasicTextFieldSettingSpecifier("sourceId", defaults.sourceId));
		results.add(new BasicToggleSettingSpecifier("backwards", defaults.backwards));
		results.add(new BasicToggleSettingSpecifier("includePhaseMeasurements",
				defaults.includePhaseMeasurements));

		return results;
	}

	@Override
	public Class<? extends GeneralNodeACEnergyDatum> getDatumType() {
		return GeneralNodeACEnergyDatum.class;
	}

	@Override
	public Class<? extends GeneralNodeACEnergyDatum> getMultiDatumType() {
		return GeneralNodeACEnergyDatum.class;
	}

	@Override
	public GeneralNodeACEnergyDatum readCurrentDatum() {
		final long start = System.currentTimeMillis();
		try {
			final WattsOnData currSample = getCurrentSample();
			if ( currSample == null ) {
				return null;
			}
			WattsOnDatum d = new WattsOnDatum(currSample, ACPhase.Total, this.backwards);
			if ( this.includePhaseMeasurements ) {
				d.populatePhaseMeasurementProperties(currSample);
			}
			d.setSourceId(resolvePlaceholders(sourceId));
			if ( currSample.getDataTimestamp() >= start ) {
				// we read from the device
				postDatumCapturedEvent(d);
			}
			return d;
		} catch ( IOException e ) {
			log.error("Communication problem reading source {} from WattsOn device {}: {}",
					this.sourceId, modbusDeviceName(), e.getMessage());
			return null;
		}
	}

	@Override
	public Collection<GeneralNodeACEnergyDatum> readMultipleDatum() {
		GeneralNodeACEnergyDatum datum = readCurrentDatum();
		// TODO: support phases
		if ( datum != null ) {
			return Collections.singletonList(datum);
		}
		return Collections.emptyList();
	}

	@Override
	protected void refreshDeviceInfo(ModbusConnection connection, WattsOnData sample) {
		sample.readConfigurationData(connection);
	}

	@Override
	protected void refreshDeviceData(ModbusConnection connection, WattsOnData sample) {
		sample.readDeviceData(connection);
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

	private String getSampleMessage(WattsOnData data) {
		if ( data.getDataTimestamp() < 1 ) {
			return "N/A";
		}
		StringBuilder buf = new StringBuilder();
		buf.append("W = ").append(data.getActivePower());
		buf.append(", VAR = ").append(data.getReactivePower());
		buf.append(", Wh rec = ").append(data.getActiveEnergyReceived());
		buf.append(", Wh del = ").append(data.getActiveEnergyDelivered());
		buf.append("; sampled at ").append(
				DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT).format(
						Instant.ofEpochMilli(data.getDataTimestamp()).atZone(ZoneId.systemDefault())));
		return buf.toString();
	}

	/**
	 * Set the source ID to use for returned datum.
	 * 
	 * @param sourceId
	 *        the source ID to use; defaults to {@literal modbus}
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Toggle the "backwards" current direction flag.
	 * 
	 * @param backwards
	 *        {@literal true} to swap energy delivered and received values
	 */
	public void setBackwards(boolean backwards) {
		this.backwards = backwards;
	}

	/**
	 * Toggle the inclusion of phase measurement properties in collected datum.
	 * 
	 * @param includePhaseMeasurements
	 *        {@literal true} to collect phase measurements
	 */
	public void setIncludePhaseMeasurements(boolean includePhaseMeasurements) {
		this.includePhaseMeasurements = includePhaseMeasurements;
	}

}
