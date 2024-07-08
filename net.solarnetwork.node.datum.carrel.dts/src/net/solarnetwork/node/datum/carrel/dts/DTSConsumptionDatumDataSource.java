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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.AcEnergyDatum;
import net.solarnetwork.node.domain.datum.SimpleAcDcEnergyDatum;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusDataUtils;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.support.ModbusDeviceDatumDataSourceSupport;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * {@link DatumDataSource} implementation for {@link AcEnergyDatum} with the DTS
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
 * @version 2.2
 */
public class DTSConsumptionDatumDataSource extends ModbusDeviceDatumDataSourceSupport
		implements DatumDataSource, SettingSpecifierProvider {

	/** The modbus register address for active energy import. */
	public static final int ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT = 40001;

	/** The {@code sourceId} default value. */
	public static final String DEFAULT_SOURCE_ID = "DTS Meter";

	private String sourceId;

	/**
	 * Constructor.
	 */
	public DTSConsumptionDatumDataSource() {
		super();
		setDisplayName("DTS Series Meter");
	}

	@Override
	protected Map<String, Object> readDeviceInfo(ModbusConnection conn) {
		return null;
	}

	@Override
	public Class<? extends AcEnergyDatum> getDatumType() {
		return AcEnergyDatum.class;
	}

	@Override
	public AcEnergyDatum readCurrentDatum() {
		try {
			AcEnergyDatum datum = performAction(new ModbusConnectionAction<AcEnergyDatum>() {

				@Override
				public AcEnergyDatum doWithConnection(ModbusConnection conn) throws IOException {
					SimpleAcDcEnergyDatum d = new SimpleAcDcEnergyDatum(resolvePlaceholders(sourceId),
							Instant.now(), new DatumSamples());
					short[] data = conn.readWords(ModbusReadFunction.ReadHoldingRegister,
							ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT, 2);
					Integer hectoWh = ModbusDataUtils.parseInt32(data[0], data[1]);
					if ( hectoWh != null ) {
						d.setWattHourReading(hectoWh * 100L);
					}
					return (d.getWattHourReading() != null ? d : null);
				}
			});
			return datum;
		} catch ( IOException e ) {
			log.error("Error communicating with DTS meter {}: {}", modbusDeviceName(), e.getMessage());
			throw new RuntimeException(e);
		}
	}

	@Override
	public Collection<String> publishedSourceIds() {
		final String sourceId = resolvePlaceholders(getSourceId());
		return (sourceId == null || sourceId.isEmpty() ? Collections.emptySet()
				: Collections.singleton(sourceId));
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.carrel.dts";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(10);
		results.addAll(getIdentifiableSettingSpecifiers());
		results.add(new BasicTextFieldSettingSpecifier("sourceId", null));
		results.add(new BasicTextFieldSettingSpecifier("modbusNetwork.propertyFilters['uid']", null,
				false, "(objectClass=net.solarnetwork.node.io.modbus.ModbusNetwork)"));
		results.add(new BasicTextFieldSettingSpecifier("unitId", String.valueOf(DEFAULT_UNIT_ID)));

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
