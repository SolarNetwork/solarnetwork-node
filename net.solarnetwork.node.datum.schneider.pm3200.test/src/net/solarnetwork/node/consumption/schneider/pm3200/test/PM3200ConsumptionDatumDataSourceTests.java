/* ==================================================================
 * PM3200ConsumptionDatumDataSourceTests.java - 1/03/2014 12:24:49 PM
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

package net.solarnetwork.node.consumption.schneider.pm3200.test;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Map;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.datum.schneider.pm3200.PM3200DatumDataSource;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.hw.schneider.meter.PM3200Data;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.io.modbus.support.StaticDataMapReadonlyModbusConnection;
import net.solarnetwork.node.test.AbstractNodeTest;
import net.solarnetwork.node.test.DataUtils;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link PM3200DatumDataSource} class.
 * 
 * @author matt
 * @version 1.1
 */
public class PM3200ConsumptionDatumDataSourceTests extends AbstractNodeTest {

	private final int UNIT_ID = 1;

	private ModbusNetwork modbus;
	private PM3200DatumDataSource service;

	@Before
	public void setup() {
		service = new PM3200DatumDataSource();
		service.setUnitId(UNIT_ID);
		modbus = EasyMock.createMock(ModbusNetwork.class);
		service.setModbusNetwork(new StaticOptionalService<ModbusNetwork>(modbus));
	}

	@SuppressWarnings("unchecked")
	private <T> ModbusConnectionAction<T> anyAction(Class<T> type) {
		return EasyMock.anyObject(ModbusConnectionAction.class);
	}

	private static final Logger log = LoggerFactory
			.getLogger(PM3200ConsumptionDatumDataSourceTests.class);

	private static Map<Integer, Integer> parseTestData(String resource) {
		try {
			return DataUtils.parseModbusHexRegisterMappingLines(new BufferedReader(new InputStreamReader(
					PM3200ConsumptionDatumDataSourceTests.class.getResourceAsStream(resource))));
		} catch ( IOException e ) {
			log.error("Error reading modbus data resource [{}]", resource, e);
			return Collections.emptyMap();
		}
	}

	@Test
	public void testReadConsumptionDatumMain() throws IOException {
		final ModbusConnection conn = new StaticDataMapReadonlyModbusConnection(
				parseTestData("test-data-01.txt"));
		expect(modbus.performAction(anyAction(PM3200Data.class), EasyMock.eq(UNIT_ID)))
				.andDelegateTo(new AbstractModbusNetwork() {

					@Override
					public <T> T performAction(ModbusConnectionAction<T> action, int unitId)
							throws IOException {
						return action.doWithConnection(conn);
					}

				});

		replay(modbus);

		GeneralNodeACEnergyDatum result = service.readCurrentDatum();
		log.debug("Read GeneralNodeACEnergyDatum: {}", result);

		verify(modbus);

		Assert.assertNotNull("GeneralNodeACEnergyDatum", result);
		Assert.assertEquals("Source ID", PM3200DatumDataSource.MAIN_SOURCE_ID, result.getSourceId());
		Assert.assertEquals("Watts", Integer.valueOf(120), result.getWatts());
		Assert.assertEquals("Total energy", Long.valueOf(2323), result.getWattHourReading());
		Assert.assertEquals("Total energy export", Long.valueOf(1212),
				result.getReverseWattHourReading());
	}
}
