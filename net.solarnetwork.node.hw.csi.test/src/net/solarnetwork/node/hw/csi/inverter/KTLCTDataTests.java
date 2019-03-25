/* ==================================================================
 * KTLCTDataTests.java - 23/11/2017 5:10:24 pm
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.csi.inverter;

import static java.util.Arrays.copyOfRange;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.test.DataUtils;

/**
 * Unit tests for the {@link KTLCTData} class.
 * 
 * @author Max Duncan
 * @version 1.0
 */
public class KTLCTDataTests {

	private static final int[] TEST_DATA = parseTestData("data-01.txt");

	private static final Logger log = LoggerFactory.getLogger(KTLCTDataTests.class);

	private static int[] parseTestData(String resource) {
		try {
			return DataUtils.parseModbusHexRegisterLines(new BufferedReader(
					new InputStreamReader(KTLCTDataTests.class.getResourceAsStream(resource))));
		} catch ( IOException e ) {
			log.error("Error reading modbus data resource [{}]", resource, e);
			return new int[0];
		}
	}

	private final KTLCTData data = new KTLCTData();

	@Before
	public void setup() {
		data.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(TEST_DATA, 0);
				return true;
			}
		});
	}

	@Test
	public void readConfigurationData() {
		// given
		ModbusConnection conn = EasyMock.createMock(ModbusConnection.class);
		KTLCTData data = new KTLCTData();

		expect(conn.readUnsignedShorts(ModbusReadFunction.ReadInputRegister, 0, 48))
				.andReturn(copyOfRange(TEST_DATA, 0, 48));

		// when
		EasyMock.replay(conn);
		data.readConfigurationData(conn);

		// then

		EasyMock.verify(conn);
	}

	@Test
	public void getDeviceModel() {
		assertThat("Inveter type", data.getInverterType(), equalTo(KTLCTInverterType.CSI_50KTL_CT));
	}

	@Test
	public void getModelName() {
		assertThat("Model name", data.getModelName(), equalTo("PVI36TL-480"));
	}

	@Test
	public void getSerialNumber() {
		assertThat("Serial number", data.getSerialNumber(), equalTo("282791139098658"));
	}

	@Test
	public void getActivePower() {
		assertThat("Active power", data.getActivePower(), equalTo(15600));
	}

	@Test
	public void getApparentPower() {
		assertThat("Apparent power", data.getApparentPower(), equalTo(15700));
	}

	@Test
	public void getVoltage() {
		assertThat("Voltage", data.getVoltage(), equalTo(490.7f));
	}

	@Test
	public void getLineVoltage() {
		assertThat("Voltage", data.getLineVoltage(), equalTo(490.7f));
	}

	@Test
	public void getWorkMode() {
		assertThat("Work mode", data.getWorkMode(), equalTo(KTLCTInverterWorkMode.Running));
	}

}
