/* ==================================================================
 * DTSConsumptionDatumDataSource.java - Mar 28, 2014 6:58:24 AM
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

package net.solarnetwork.node.datum.carrel.dts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.domain.ACEnergyDatum;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusDataUtils;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.support.ModbusDeviceDatumDataSourceSupport;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * {@link DatumDataSource} implementation for {@link ACEnergyDatum} with the DTS
 * series watt meter.
 * 
 * <p>
 * The DTS series watt-hour meter supports the following serial port
 * configuration:
 * </p>
 * 
 * <ul>
 * <li><b>Baud</b> - 9600</li>
 * <li><b>Mode</b> - RTU</li>
 * <li><b>Data bits</b> - 8</li>
 * <li><b>Parity</b> - None</li>
 * <li><b>Stop bit</b> - 1</li>
 * </ul>
 * 
 * @author matt
 * @version 2.0
 */
public class DTSConsumptionDatumDataSource extends ModbusDeviceDatumDataSourceSupport
		implements DatumDataSource<ACEnergyDatum>, SettingSpecifierProvider {

	public static final int ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT = 40001;

	private String sourceId = "Main";

	@Override
	protected Map<String, Object> readDeviceInfo(ModbusConnection conn) {
		return null;
	}

	@Override
	public Class<? extends ACEnergyDatum> getDatumType() {
		return GeneralNodeACEnergyDatum.class;
	}

	@Override
	public ACEnergyDatum readCurrentDatum() {
		try {
			GeneralNodeACEnergyDatum datum = performAction(
					new ModbusConnectionAction<GeneralNodeACEnergyDatum>() {

						@Override
						public GeneralNodeACEnergyDatum doWithConnection(ModbusConnection conn)
								throws IOException {
							GeneralNodeACEnergyDatum d = new GeneralNodeACEnergyDatum();
							short[] data = conn.readWords(ModbusReadFunction.ReadHoldingRegister,
									ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT, 2);
							Integer hectoWh = ModbusDataUtils.parseInt32(data[0], data[1]);
							if ( hectoWh != null ) {
								d.setWattHourReading(hectoWh * 100L);
							}
							d.setSourceId(sourceId);
							return (d.getWattHourReading() != null ? d : null);
						}
					});
			postDatumCapturedEvent(datum);
			return datum;
		} catch ( IOException e ) {
			log.error("Error communicating with meter: {}", e.getMessage());
			throw new RuntimeException(e);
		}
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.datum.carrel.dts";
	}

	@Override
	public String getDisplayName() {
		return "DTS Series Meter";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		DTSConsumptionDatumDataSource defaults = new DTSConsumptionDatumDataSource();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(10);
		results.addAll(getIdentifiableSettingSpecifiers());
		results.add(new BasicTextFieldSettingSpecifier("sourceId", defaults.getSourceId()));
		results.add(new BasicTextFieldSettingSpecifier("modbusNetwork.propertyFilters['UID']",
				"Serial Port"));
		results.add(new BasicTextFieldSettingSpecifier("unitId", String.valueOf(defaults.getUnitId())));

		return results;
	}

	/**
	 * Get the configured source ID.
	 * 
	 * @return the source ID
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the {@code sourceId} to assign to captured datum.
	 * 
	 * @param sourceId
	 *        the source ID to use
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

}
