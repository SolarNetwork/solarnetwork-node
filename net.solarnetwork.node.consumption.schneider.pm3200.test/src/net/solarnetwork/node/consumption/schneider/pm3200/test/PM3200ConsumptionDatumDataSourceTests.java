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
import java.io.IOException;
import net.solarnetwork.node.consumption.schneider.pm3200.PM3200ConsumptionDatumDataSource;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.hw.schneider.meter.PM3200Data;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.test.AbstractNodeTest;
import net.solarnetwork.util.StaticOptionalService;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the {@link PM3200ConsumptionDatumDataSource} class.
 * 
 * @author matt
 * @version 1.0
 */
public class PM3200ConsumptionDatumDataSourceTests extends AbstractNodeTest {

	private final int UNIT_ID = 1;

	private ModbusNetwork modbus;
	private ModbusConnection conn;
	private PM3200ConsumptionDatumDataSource service;

	@Before
	public void setup() {
		service = new PM3200ConsumptionDatumDataSource();
		service.setUnitId(UNIT_ID);
		modbus = EasyMock.createMock(ModbusNetwork.class);
		conn = EasyMock.createMock(ModbusConnection.class);
		service.setModbusNetwork(new StaticOptionalService<ModbusNetwork>(modbus));
	}

	@SuppressWarnings("unchecked")
	private <T> ModbusConnectionAction<T> anyAction(Class<T> type) {
		return EasyMock.anyObject(ModbusConnectionAction.class);
	}

	// 2099
	private final int[] TEST_DATA_SET_1 = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };

	// 3019
	private final int[] TEST_DATA_SET_2 = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
			16, 17, 18 };

	// 3053
	private final int[] TEST_DATA_SET_3 = new int[] { 1, 2, 3, 4, 5, 6,
			((Float.floatToIntBits(.12f) >> 16) & 0xFFFF), (Float.floatToIntBits(.12f) & 0xFFFF), 9, 10,
			11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32 };

	// 3107
	private final int[] TEST_DATA_SET_4 = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
			16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26 };

	// 3203
	private final int[] TEST_DATA_SET_5 = new int[] { (int) ((2323L >> 48) & 0xFFFF),
			(int) ((2323L >> 32) & 0xFFFF), (int) ((2323L >> 16) & 0xFFFF),
			(int) ((2323L >> 0) & 0xFFFF), 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
			21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38 };

	@Test
	public void testReadConsumptionDatumMain() throws IOException {
		expect(modbus.performAction(anyAction(PM3200Data.class), EasyMock.eq(UNIT_ID))).andDelegateTo(
				new AbstractModbusNetwork() {

					@Override
					public <T> T performAction(ModbusConnectionAction<T> action, int unitId)
							throws IOException {
						return action.doWithConnection(conn);
					}

				});

		expect(conn.readInts(2999, 12)).andReturn(TEST_DATA_SET_1);
		expect(conn.readInts(3019, 18)).andReturn(TEST_DATA_SET_2);
		expect(conn.readInts(3053, 32)).andReturn(TEST_DATA_SET_3);
		expect(conn.readInts(3107, 26)).andReturn(TEST_DATA_SET_4);
		expect(conn.readInts(3203, 38)).andReturn(TEST_DATA_SET_5);

		replay(modbus, conn);

		GeneralNodeACEnergyDatum result = service.readCurrentDatum();
		log.debug("Read GeneralNodeACEnergyDatum: {}", result);

		verify(modbus, conn);

		Assert.assertNotNull("GeneralNodeACEnergyDatum", result);
		Assert.assertEquals("Source ID", PM3200ConsumptionDatumDataSource.MAIN_SOURCE_ID,
				result.getSourceId());
		Assert.assertEquals("Watts", Integer.valueOf(120), result.getWatts());
		Assert.assertEquals("Total energy", Long.valueOf(2323), result.getWattHourReading());
	}
}
