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
import net.solarnetwork.node.hw.yaskawa.mb.inverter.PVITLData;
import net.solarnetwork.node.hw.yaskawa.mb.inverter.PVITLInverterState;
import net.solarnetwork.node.hw.yaskawa.mb.inverter.PVITLInverterType;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;
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
		PVITLData data = new PVITLData();

		expect(conn.readUnsignedShorts(ModbusReadFunction.ReadInputRegister, 5, 43))
				.andReturn(copyOfRange(TEST_DATA, 5, 43));

		// when
		EasyMock.replay(conn);
		data.readConfigurationData(conn);

		// then
		EasyMock.verify(conn);
	}

	@Test
	public void readInverterData() {
		// given
		ModbusConnection conn = EasyMock.createMock(ModbusConnection.class);
		PVITLData data = new PVITLData();

		expect(conn.readUnsignedShorts(ModbusReadFunction.ReadInputRegister, 0x16, (0x2F - 0x16) + 1))
				.andReturn(copyOfRange(TEST_DATA, 0x16, (0x2F - 0x16) + 1));

		// when
		EasyMock.replay(conn);
		data.readInverterData(conn);

		// then
		EasyMock.verify(conn);
	}

	@Test
	public void getDeviceModel() {
		assertThat("Inveter type", data.getInverterType(), equalTo(PVITLInverterType.PVI_14TL));
	}

	@Test
	public void getModelName() {
		assertThat("Model name", data.getModelName(), equalTo("PVI14TL-208"));
	}

	@Test
	public void getSerialNumber() {
		assertThat("Serial number", data.getSerialNumber(), equalTo("11491338014"));
	}

	@Test
	public void getDspFirmwareVersion() {
		assertThat("DSP version", data.getDspFirmwareVersion(), equalTo("1.93"));
	}

	@Test
	public void getLcdFirmwareVersion() {
		assertThat("LCD version", data.getLcdFirmwareVersion(), equalTo("0.06"));
	}

	@Test
	public void getModuleTemperature() {
		assertThat("Module temperature", data.getModuleTemperature(), equalTo(40.1f));
	}

	@Test
	public void getInternalTemperature() {
		assertThat("Internal temperature", data.getInternalTemperature(), equalTo(31.6f));
	}

	@Test
	public void foo() {
		assertThat("Operating state", data.getOperatingState(), equalTo(PVITLInverterState.Running));
	}

	@Test
	public void getPv1Voltage() {
		assertThat("PV 1 voltage", data.getPv1Voltage(), equalTo(367.9f));
	}

	@Test
	public void getPv1Power() {
		assertThat("PV 1 power", data.getPv1Power(), equalTo(0));
	}

	@Test
	public void getPv2Voltage() {
		assertThat("PV 2 voltage", data.getPv2Voltage(), equalTo(370.7f));
	}

	@Test
	public void getPv2Power() {
		assertThat("PV 2 power", data.getPv2Power(), equalTo(3188));
	}

	@Test
	public void getActivePower() {
		assertThat("Active power", data.getActivePower(), equalTo(3100));
	}

	@Test
	public void getApparentPower() {
		assertThat("Apparent power", data.getApparentPower(), equalTo(32000));
	}

	@Test
	public void getActiveEnergyDelivered() {
		assertThat("Active energy delivered", data.getActiveEnergyDelivered(), equalTo(106394000L));
	}

	@Test
	public void getActiveEnergyDeliveredToday() {
		assertThat("Active energy delivered today", data.getActiveEnergyDeliveredToday(),
				equalTo(34700L));
	}

	@Test
	public void getFrequency() {
		assertThat("Frequency", data.getFrequency(), equalTo(59.9f));
	}

	@Test
	public void getDCVoltage() {
		assertThat("DC voltage", data.getDCVoltage(), equalTo(369.3f));
	}

	@Test
	public void getDCPower() {
		assertThat("DC power", data.getDCPower(), equalTo(3188));
	}

}
