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

package net.solarnetwork.node.consumption.carrel.dts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.consumption.ConsumptionDatum;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusDeviceSupport;
import net.solarnetwork.node.io.modbus.ModbusHelper;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import org.springframework.context.MessageSource;

/**
 * {@link DatumDataSource} implementation for {@link ConsumptionDatum} with the
 * DTS series watt meter.
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
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>sourceId</dt>
 * <dd>The {@code sourceId} to assign to captured {@link ConsumptionDatum}.</dd>
 * </dl>
 * 
 * @author matt
 * @version 2.0
 */
public class DTSConsumptionDatumDataSource extends ModbusDeviceSupport implements
		DatumDataSource<ConsumptionDatum>, SettingSpecifierProvider {

	public static final int ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT = 40001;

	private String sourceId = "Main";
	private MessageSource messageSource;

	@Override
	protected Map<String, Object> readDeviceInfo(ModbusConnection conn) {
		return null;
	}

	@Override
	public Class<? extends ConsumptionDatum> getDatumType() {
		return ConsumptionDatum.class;
	}

	@Override
	public ConsumptionDatum readCurrentDatum() {
		try {
			return performAction(new ModbusConnectionAction<ConsumptionDatum>() {

				@Override
				public ConsumptionDatum doWithConnection(ModbusConnection conn) throws IOException {
					ConsumptionDatum d = new ConsumptionDatum();
					int[] data = conn.readInts(ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT, 2);
					Long hectoWh = ModbusHelper.parseInt32(data, 0);
					if ( hectoWh != null ) {
						d.setWattHourReading(hectoWh * 100L);
					}
					d.setSourceId(sourceId);
					return (d.getWattHourReading() != null ? d : null);
				}
			});
		} catch ( IOException e ) {
			log.error("Error communicating with meter: {}", e.getMessage());
			throw new RuntimeException(e);
		}
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.consumption.carrel.dts";
	}

	@Override
	public String getDisplayName() {
		return "DTS Series Meter";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		DTSConsumptionDatumDataSource defaults = new DTSConsumptionDatumDataSource();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(10);

		results.add(new BasicTextFieldSettingSpecifier("uid", defaults.getUid()));
		results.add(new BasicTextFieldSettingSpecifier("groupUID", defaults.getGroupUID()));
		results.add(new BasicTextFieldSettingSpecifier("sourceId", defaults.getSourceId()));
		results.add(new BasicTextFieldSettingSpecifier("modbusNetwork.propertyFilters['UID']",
				"Serial Port"));
		results.add(new BasicTextFieldSettingSpecifier("unitId", String.valueOf(defaults.getUnitId())));

		return results;
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

}
