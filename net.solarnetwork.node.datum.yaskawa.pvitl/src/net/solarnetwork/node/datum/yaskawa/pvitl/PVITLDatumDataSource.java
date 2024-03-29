/* ==================================================================
 * PVITLDatumDataSource.java - 21/09/2018 2:24:12 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.yaskawa.pvitl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.solarnetwork.node.domain.datum.AcDcEnergyDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.hw.yaskawa.mb.inverter.PVITLData;
import net.solarnetwork.node.hw.yaskawa.mb.inverter.PVITLDataAccessor;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.io.modbus.support.ModbusDataDatumDataSourceSupport;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;

/**
 * {@link DatumDataSource} implementation for {@link AcDcEnergyDatum} with the
 * PVI-XXTL series inverter.
 * 
 * @author matt
 * @version 2.2
 */
public class PVITLDatumDataSource extends ModbusDataDatumDataSourceSupport<PVITLData>
		implements DatumDataSource, MultiDatumDataSource, SettingSpecifierProvider {

	/**
	 * The {@code sampleCacheMs} property default value.
	 * 
	 * @since 2.2
	 */
	public static final long DEFAULT_SAMPLE_CACHE_MS = 5000L;

	private String sourceId;
	private boolean includePhaseMeasurements = false;

	/**
	 * Default constructor.
	 */
	public PVITLDatumDataSource() {
		this(new PVITLData());
	}

	/**
	 * Construct with a specific sample data instance.
	 * 
	 * @param sample
	 *        the sample data to use
	 */
	public PVITLDatumDataSource(PVITLData sample) {
		super(sample);
		setSampleCacheMs(DEFAULT_SAMPLE_CACHE_MS);
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		boolean notEmpty = false;
		if ( sourceId != null ) {
			buf.append("sourceId=").append(sourceId);
			notEmpty = true;
		}
		ModbusNetwork net = OptionalService.service(getModbusNetwork());
		if ( net != null ) {
			if ( notEmpty ) {
				buf.append(',');
			} else {
				notEmpty = true;
			}
			buf.append("network=").append(net);
		}
		if ( getUid() != null ) {
			if ( notEmpty ) {
				buf.append(',');
			} else {
				notEmpty = true;
			}
			buf.append("uid=").append(getUid());
		}
		buf.insert(0, "PVITLDatumDataSource{");
		buf.append('}');
		return buf.toString();
	}

	@Override
	public String deviceInfoSourceId() {
		return resolvePlaceholders(sourceId);
	}

	@Override
	protected void refreshDeviceInfo(ModbusConnection connection, PVITLData sample) throws IOException {
		sample.readConfigurationData(connection);
	}

	@Override
	protected void refreshDeviceData(ModbusConnection connection, PVITLData sample) throws IOException {
		sample.readInverterData(connection);
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return PVITLDatum.class;
	}

	@Override
	public PVITLDatum readCurrentDatum() {
		final String sourceId = resolvePlaceholders(this.sourceId);
		try {
			final PVITLData currSample = getCurrentSample();
			if ( currSample == null ) {
				return null;
			}
			PVITLDatum d = new PVITLDatum(currSample, sourceId);
			if ( this.includePhaseMeasurements ) {
				d.populatePhaseMeasurementProperties(currSample);
			}
			return d;
		} catch ( IOException e ) {
			log.error("Communication problem reading source {} from PVI-TL device {}: {}", sourceId,
					modbusDeviceName(), e.getMessage());
			return null;
		}
	}

	@Override
	public Class<? extends NodeDatum> getMultiDatumType() {
		return AcDcEnergyDatum.class;
	}

	@Override
	public Collection<NodeDatum> readMultipleDatum() {
		PVITLDatum datum = readCurrentDatum();
		if ( datum != null ) {
			return Collections.singletonList(datum);
		}
		return Collections.emptyList();
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.yaskawa.pvitl";
	}

	@Override
	public String getDisplayName() {
		return "PVI-TL Inverter";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(12);
		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(), true));
		results.add(new BasicTitleSettingSpecifier("sample", getSampleMessage(getSample()), true));

		results.addAll(getIdentifiableSettingSpecifiers());
		results.addAll(getModbusNetworkSettingSpecifiers());

		results.add(new BasicTextFieldSettingSpecifier("sourceId", null));
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(DEFAULT_SAMPLE_CACHE_MS)));
		results.add(new BasicToggleSettingSpecifier("includePhaseMeasurements", false));

		results.addAll(getDeviceInfoMetadataSettingSpecifiers());

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

	private String getSampleMessage(PVITLDataAccessor data) {
		if ( data.getDataTimestamp() == null ) {
			return "N/A";
		}
		StringBuilder buf = new StringBuilder();
		buf.append("State = ").append(data.getOperatingState());
		buf.append(", Hz = ").append(data.getFrequency());
		buf.append(", PV1 V = ").append(data.getPv1Voltage());
		buf.append(", PV2 V = ").append(data.getPv2Voltage());
		buf.append(", W = ").append(data.getActivePower());
		buf.append(", Wh today = ").append(data.getActiveEnergyDeliveredToday());
		buf.append(", Wh total = ").append(data.getActiveEnergyDelivered());
		buf.append("; sampled at ").append(data.getDataTimestamp());
		return buf.toString();
	}

	/**
	 * Get the source ID.
	 * 
	 * @return the source ID
	 * @since 2.1
	 */
	public String getSourceId() {
		return sourceId;
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
	 * Get the inclusion toggle of phase measurement properties in collected
	 * datum.
	 * 
	 * @return {@literal true} to collect phase measurements
	 * @since 2.2
	 */
	public boolean isIncludePhaseMeasurements() {
		return includePhaseMeasurements;
	}

	/**
	 * Toggle the inclusion of phase measurement properties in collected datum.
	 * 
	 * @param includePhaseMeasurements
	 *        {@literal true} to collect phase measurements
	 * @since 2.2
	 */
	public void setIncludePhaseMeasurements(boolean includePhaseMeasurements) {
		this.includePhaseMeasurements = includePhaseMeasurements;
	}

}
