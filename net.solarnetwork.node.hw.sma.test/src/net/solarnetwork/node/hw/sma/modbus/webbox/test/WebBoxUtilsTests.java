/* ==================================================================
 * WebBoxUtilsTests.java - 11/09/2020 2:49:16 PM
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

package net.solarnetwork.node.hw.sma.modbus.webbox.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.Test;
import net.solarnetwork.node.hw.sma.modbus.webbox.WebBoxDeviceReference;
import net.solarnetwork.node.hw.sma.modbus.webbox.WebBoxRegister;
import net.solarnetwork.node.hw.sma.modbus.webbox.WebBoxUtils;
import net.solarnetwork.node.hw.sma.test.TestUtils;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;

/**
 * Test cases for the {@link WebBoxUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class WebBoxUtilsTests {

	@Test
	public void parseDeviceReference_ModbusData() throws IOException {
		ModbusData d = TestUtils.testData(getClass(), "data-01.txt");
		WebBoxDeviceReference ref = WebBoxUtils.parseDeviceReference(d,
				WebBoxRegister.DEVICE_UNIT_IDS_STARTING_ADDRESS);
		assertThat("Reference device ID parsed", ref.getDeviceId(), equalTo(0x009B));
		assertThat("Reference unit ID parsed", ref.getUnitId(), equalTo(0x0003));
		assertThat("Reference serial number parsed", ref.getSerialNumber(), equalTo(0x0ACB5EC7L));
	}

	@Test
	public void readAvailableDevices() throws IOException {
		ModbusConnection conn = TestUtils.testDataConnection(getClass(), "data-01.txt");
		ModbusData d = new ModbusData();
		List<WebBoxDeviceReference> result = new ArrayList<>(8);
		d.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) throws IOException {
				result.addAll(WebBoxUtils.readAvailableDevices(conn, m));
				return false;
			}
		});
		assertThat("References parsed", result, Matchers.hasSize(6));
		WebBoxDeviceReference ref = result.get(0);
		assertThat("Reference 1 device ID parsed", ref.getDeviceId(), equalTo(0x009B));
		assertThat("Reference 1 unit ID parsed", ref.getUnitId(), equalTo(0x0003));
		assertThat("Reference 1 serial number parsed", ref.getSerialNumber(), equalTo(0x0ACB5EC7L));

		ref = result.get(5);
		assertThat("Reference 6 device ID parsed", ref.getDeviceId(), equalTo(0x00BE));
		assertThat("Reference 6 unit ID parsed", ref.getUnitId(), equalTo(0x0008));
		assertThat("Reference 6 serial number parsed", ref.getSerialNumber(), equalTo(0x082B5431L));
	}

	@Test
	public void readAvailableDevices_full() throws IOException {
		ModbusConnection conn = TestUtils.testDataConnection(getClass(), "data-02.txt");
		ModbusData d = new ModbusData();
		List<WebBoxDeviceReference> result = new ArrayList<>(8);
		d.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) throws IOException {
				result.addAll(WebBoxUtils.readAvailableDevices(conn, m));
				return false;
			}
		});
		assertThat("References parsed", result, Matchers.hasSize(245));
		WebBoxDeviceReference ref = result.get(0);
		assertThat("Reference 1 device ID parsed", ref.getDeviceId(), equalTo(0x009B));
		assertThat("Reference 1 unit ID parsed", ref.getUnitId(), equalTo(0x0003));
		assertThat("Reference 1 serial number parsed", ref.getSerialNumber(), equalTo(0x0ACB5EC7L));

		ref = result.get(1);
		assertThat("Reference 2 device ID parsed", ref.getDeviceId(), equalTo(0));
		assertThat("Reference 2 unit ID parsed", ref.getUnitId(), equalTo(0));
		assertThat("Reference 2 serial number parsed", ref.getSerialNumber(), equalTo(0L));

		ref = result.get(244);
		assertThat("Reference 245 device ID parsed", ref.getDeviceId(), equalTo(0x00D7));
		assertThat("Reference 245 unit ID parsed", ref.getUnitId(), equalTo(0x00F5));
		assertThat("Reference 245 serial number parsed", ref.getSerialNumber(), equalTo(0xABCD1234L));
	}
}
