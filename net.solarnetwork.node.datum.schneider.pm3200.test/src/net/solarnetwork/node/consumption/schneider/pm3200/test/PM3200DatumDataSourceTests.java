/* ==================================================================
 * PM3200DatumDataSourceTests.java - 1/03/2014 12:24:49 PM
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Map.Entry;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.datum.schneider.pm3200.PM3200DatumDataSource;
import net.solarnetwork.node.domain.datum.AcEnergyDatum;
import net.solarnetwork.node.hw.schneider.meter.PM3200Data;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.io.modbus.support.StaticDataMapReadonlyModbusConnection;
import net.solarnetwork.node.test.DataUtils;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.util.IntShortMap;

/**
 * Test cases for the {@link PM3200DatumDataSource} class.
 * 
 * @author matt
 */
public class PM3200DatumDataSourceTests {

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

	private static final Logger log = LoggerFactory.getLogger(PM3200DatumDataSourceTests.class);

	private static IntShortMap parseTestData(String resource) {
		IntShortMap result = new IntShortMap();
		try {
			Map<Integer, Integer> m = DataUtils
					.parseModbusHexRegisterMappingLines(new BufferedReader(new InputStreamReader(
							PM3200DatumDataSourceTests.class.getResourceAsStream(resource))));
			for ( Entry<Integer, Integer> e : m.entrySet() ) {
				result.putValue(e.getKey(), e.getValue());
			}
		} catch ( IOException e ) {
			log.error("Error reading modbus data resource [{}]", resource, e);
		}
		return result;
	}

	@Test
	public void testReadConsumptionDatumMain() throws IOException {
		final ModbusConnection conn = new StaticDataMapReadonlyModbusConnection(
				parseTestData("test-data-01.txt"));
		expect(modbus.performAction(EasyMock.eq(UNIT_ID), anyAction(PM3200Data.class)))
				.andDelegateTo(new AbstractModbusNetwork() {

					@Override
					public <T> T performAction(int unitId, ModbusConnectionAction<T> action)
							throws IOException {
						return action.doWithConnection(conn);
					}

				});

		replay(modbus);

		AcEnergyDatum result = service.readCurrentDatum();
		log.debug("Read AcEnergyDatum: {}", result);

		verify(modbus);

		assertThat("Result", result, notNullValue());
		assertThat("Source ID", result.getSourceId(), equalTo(PM3200DatumDataSource.MAIN_SOURCE_ID));
		assertThat("Watts", result.getWatts(), equalTo(120));
		assertThat("Energy reading", result.getWattHourReading(), equalTo(2323L));
		assertThat("Energy reading reverse", result.getReverseWattHourReading(), equalTo(1212L));
	}
}
