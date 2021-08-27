/* ==================================================================
 * PowerGatePlusDatumDataSource.java - 11/11/2019 10:35:44 am
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

package net.solarnetwork.node.datum.satcon.powergate;

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
import net.solarnetwork.node.hw.satcon.PowerGateInverterDataAccessor;
import net.solarnetwork.node.hw.satcon.PowerGatePlusData;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.support.ModbusDataDatumDataSourceSupport;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;

/**
 * {@link DatumDataSource} implementation for {@link GeneralNodePVEnergyDatum}
 * with the PowerGate Plus series inverter.
 * 
 * @author matt
 * @version 1.3
 */
public class PowerGatePlusDatumDataSource extends ModbusDataDatumDataSourceSupport<PowerGatePlusData>
		implements DatumDataSource<GeneralNodePVEnergyDatum>,
		MultiDatumDataSource<GeneralNodePVEnergyDatum>, SettingSpecifierProvider {

	private String sourceId = "PowerGate Plus";

	/**
	 * Default constructor.
	 */
	public PowerGatePlusDatumDataSource() {
		this(new PowerGatePlusData());
	}

	/**
	 * Construct with a specific sample data instance.
	 * 
	 * @param sample
	 *        the sample data to use
	 */
	public PowerGatePlusDatumDataSource(PowerGatePlusData sample) {
		super(sample);
	}

	@Override
	protected void refreshDeviceInfo(ModbusConnection connection, PowerGatePlusData sample)
			throws IOException {
		sample.readConfigurationData(connection);
	}

	@Override
	protected void refreshDeviceData(ModbusConnection connection, PowerGatePlusData sample)
			throws IOException {
		sample.readInverterData(connection);
		sample.readControlData(connection);
	}

	@Override
	public Class<? extends GeneralNodePVEnergyDatum> getDatumType() {
		return PowerGateDatum.class;
	}

	@Override
	public GeneralNodePVEnergyDatum readCurrentDatum() {
		try {
			final PowerGatePlusData currSample = getCurrentSample();
			if ( currSample == null ) {
				return null;
			}
			PowerGateDatum d = new PowerGateDatum(currSample);
			d.setSourceId(resolvePlaceholders(sourceId));
			return d;
		} catch ( IOException e ) {
			log.error("Communication problem reading source {} from PowerGate Plus device {}: {}",
					this.sourceId, modbusDeviceName(), e.getMessage());
			return null;
		}
	}

	@Override
	public Class<? extends GeneralNodePVEnergyDatum> getMultiDatumType() {
		return PowerGateDatum.class;
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
		return "net.solarnetwork.node.datum.satcon.powergateplus";
	}

	@Override
	public String getDisplayName() {
		return "Satcon PowerGate Plus Inverter";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(12);
		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(), true));
		results.add(new BasicTitleSettingSpecifier("sample", getSampleMessage(getSample()), true));

		results.addAll(getIdentifiableSettingSpecifiers());
		results.addAll(getModbusNetworkSettingSpecifiers());

		PowerGatePlusDatumDataSource defaults = new PowerGatePlusDatumDataSource();
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

	private String getSampleMessage(PowerGateInverterDataAccessor data) {
		if ( data.getDataTimestamp() < 1 ) {
			return "N/A";
		}
		StringBuilder buf = new StringBuilder();
		buf.append("state = ").append(data.getOperatingState());
		buf.append(", Hz = ").append(data.getFrequency());
		buf.append(", PV V = ").append(data.getDCVoltage());
		buf.append(", PV W = ").append(data.getDCPower());
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
	 * @param sourceId
	 *        the source ID to use; defaults to {@literal modbus}
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

}
