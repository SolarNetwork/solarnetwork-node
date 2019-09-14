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
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.domain.GeneralNodePVEnergyDatum;
import net.solarnetwork.node.hw.yaskawa.mb.inverter.PVITLData;
import net.solarnetwork.node.hw.yaskawa.mb.inverter.PVITLDataAccessor;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusDataDatumDataSourceSupport;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;

/**
 * {@link DatumDataSource} implementation for {@link GeneralNodePVEnergyDatum}
 * with the PVI-XXTL series inverter.
 * 
 * @author matt
 * @version 1.1
 */
public class PVITLDatumDataSource extends ModbusDataDatumDataSourceSupport<PVITLData>
		implements DatumDataSource<GeneralNodePVEnergyDatum>,
		MultiDatumDataSource<GeneralNodePVEnergyDatum>, SettingSpecifierProvider {

	private String sourceId = "PVI-XXTL";

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
	}

	@Override
	protected void refreshDeviceInfo(ModbusConnection connection, PVITLData sample) {
		sample.readConfigurationData(connection);
	}

	@Override
	protected void refreshDeviceData(ModbusConnection connection, PVITLData sample) {
		sample.readInverterData(connection);
	}

	@Override
	public Class<? extends GeneralNodePVEnergyDatum> getDatumType() {
		return PVITLDatum.class;
	}

	@Override
	public GeneralNodePVEnergyDatum readCurrentDatum() {
		final long start = System.currentTimeMillis();
		try {
			final PVITLData currSample = getCurrentSample();
			if ( currSample == null ) {
				return null;
			}
			PVITLDatum d = new PVITLDatum(currSample);
			d.setSourceId(this.sourceId);
			if ( currSample.getDataTimestamp() >= start ) {
				// we read from the device
				postDatumCapturedEvent(d);
			}
			return d;
		} catch ( IOException e ) {
			log.error("Communication problem reading source {} from PVI-TL device {}: {}", this.sourceId,
					modbusDeviceName(), e.getMessage());
			return null;
		}
	}

	@Override
	public Class<? extends GeneralNodePVEnergyDatum> getMultiDatumType() {
		return PVITLDatum.class;
	}

	@Override
	public Collection<GeneralNodePVEnergyDatum> readMultipleDatum() {
		GeneralNodePVEnergyDatum datum = readCurrentDatum();
		if ( datum != null ) {
			return Collections.singletonList(datum);
		}
		return Collections.emptyList();
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
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

		PVITLDatumDataSource defaults = new PVITLDatumDataSource();
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(defaults.getSampleCacheMs())));
		results.add(new BasicTextFieldSettingSpecifier("sourceId", defaults.sourceId));

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
		if ( data.getDataTimestamp() < 1 ) {
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
		buf.append("; sampled at ")
				.append(DateTimeFormat.forStyle("LS").print(new DateTime(data.getDataTimestamp())));
		return buf.toString();
	}

	/**
	 * Set the source ID to use for returned datum.
	 * 
	 * @param soruceId
	 *        the source ID to use; defaults to {@literal modbus}
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}
}
