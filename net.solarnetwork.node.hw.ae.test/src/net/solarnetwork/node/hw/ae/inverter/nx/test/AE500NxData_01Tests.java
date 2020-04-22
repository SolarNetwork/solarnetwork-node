/* ==================================================================
 * AE500NxData_01Tests.java - 22/04/2020 3:29:14 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.ae.inverter.nx.test;

import static net.solarnetwork.node.test.DataUtils.parseModbusHexRegisterLines;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import net.solarnetwork.node.hw.ae.inverter.nx.AE500NxData;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.support.StaticDataMapReadonlyModbusConnection;

/**
 * Test cases for reading AE500Nx sample data.
 * 
 * @author matt
 * @version 1.0
 */
public class AE500NxData_01Tests {

	static ModbusConnection conn = null;

	@BeforeClass
	public static void setupStatic() throws IOException {
		final int[] data = parseModbusHexRegisterLines(new BufferedReader(new InputStreamReader(
				AE500NxData_01Tests.class.getResourceAsStream("test-data-500nx-01.txt"))));
		conn = new StaticDataMapReadonlyModbusConnection(data);
	}

	private AE500NxData data;

	@Before
	public void setup() {
		data = new AE500NxData();
		data.readConfigurationData(conn);
	}

	@Test
	public void serialNumber() {
		assertThat("Serial number parsed", data.getSerialNumber(),
				equalTo("M/N 3159500-1000 AD; S/N 781322; F/R AD"));
	}

}
