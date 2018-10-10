/* ==================================================================
 * PVITLDataTests.java - 21/09/2018 2:35:49 PM
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

package net.solarnetwork.node.hw.yaskawa.mb.test;

import static java.util.Arrays.copyOfRange;
import static org.easymock.EasyMock.expect;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.hw.yaskawa.mb.inverter.PVITLData;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.test.DataUtils;

/**
 * Test cases for the {@link PVITLData} class.
 * 
 * @author matt
 * @version 1.0
 */
public class PVITLDataTests {

	private static final int[] TEST_DATA = parseTestData("data-01.txt");

	private static final Logger log = LoggerFactory.getLogger(PVITLDataTests.class);

	private static int[] parseTestData(String resource) {
		try {
			return DataUtils.parseModbusHexRegisterLines(new BufferedReader(
					new InputStreamReader(PVITLDataTests.class.getResourceAsStream(resource))));
		} catch ( IOException e ) {
			log.error("Error reading modbus data resource [{}]", resource, e);
			return new int[0];
		}
	}

	private final PVITLData data = new PVITLData();

	@Before
	public void setup() {
		/*-
		data.performUpdates(new ModbusDataUpdateAction() {
		
			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(TEST_DATA, 0);
				return true;
			}
		});
		*/
	}

	@Test
	public void readConfigurationData() {
		// given
		ModbusConnection conn = EasyMock.createMock(ModbusConnection.class);
		PVITLData data = new PVITLData();

		expect(conn.readUnsignedShorts(ModbusReadFunction.ReadInputRegister, 5, 43))
				.andReturn(copyOfRange(TEST_DATA, 5, 43));

		// when
		EasyMock.replay(conn);
		data.readConfigurationData(conn);

		// then

		EasyMock.verify(conn);
	}
}
