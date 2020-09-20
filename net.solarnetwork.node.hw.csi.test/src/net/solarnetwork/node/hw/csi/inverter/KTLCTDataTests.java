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

import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Map;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusWriteFunction;
import net.solarnetwork.node.test.DataUtils;

/**
 * Unit tests for the {@link KTLCTData} class.
 * 
 * @author Max Duncan
 * @version 1.0
 */
public class KTLCTDataTests {

	private static final Map<Integer, Integer> TEST_DATA = parseTestData("data-01.txt");

	private static final Logger log = LoggerFactory.getLogger(KTLCTDataTests.class);

	private static Map<Integer, Integer> parseTestData(String resource) {
		try {
			return DataUtils.parseModbusHexRegisterMappingLines(new BufferedReader(
					new InputStreamReader(KTLCTDataTests.class.getResourceAsStream(resource))));
		} catch ( IOException e ) {
			log.error("Error reading modbus data resource [{}]", resource, e);
			return Collections.emptyMap();
		}
	}

	private static short[] mapSlice(Map<Integer, Integer> data, int start, int len) {
		short[] slice = new short[len];
		for ( int i = start, end = start + len; i < end; i++ ) {
			Integer k = i;
			Integer v = data.get(k);
			slice[i - start] = (v != null ? v.shortValue() : 0);
		}
		return slice;
	}

	private final KTLCTData data = new KTLCTData();

	@Before
	public void setup() throws IOException {
		data.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataMap(TEST_DATA);
				return true;
			}
		});
	}

	@Test
	public void readConfigurationData() throws IOException {
		// given
		ModbusConnection conn = EasyMock.createMock(ModbusConnection.class);
		KTLCTData data = new KTLCTData();

		expect(conn.readWords(ModbusReadFunction.ReadInputRegister, 0, 59))
				.andReturn(mapSlice(TEST_DATA, 0, 59));
		expect(conn.readWords(ModbusReadFunction.ReadHoldingRegister, 4096, 2))
				.andReturn(mapSlice(TEST_DATA, 4096, 2));

		// when
		replay(conn);
		data.readConfigurationData(conn);

		// then

		verify(conn);
	}

	@Test
	public void deviceModel() {
		assertThat("Inveter type", data.getInverterType(), equalTo(KTLCTInverterType.CSI_50KTL_CT));
	}

	@Test
	public void modelName() {
		assertThat("Model name", data.getModelName(), equalTo("PVI36TL-480"));
	}

	@Test
	public void serialNumber() {
		assertThat("Serial number", data.getSerialNumber(), equalTo("1013271644022"));
	}

	@Test
	public void activePower() {
		assertThat("Active power", data.getActivePower(), equalTo(15600));
	}

	@Test
	public void apparentPower() {
		assertThat("Apparent power", data.getApparentPower(), equalTo(15700));
	}

	@Test
	public void voltage() {
		assertThat("Voltage", data.getVoltage(), equalTo(490.7f));
	}

	@Test
	public void lineVoltage() {
		assertThat("Voltage", data.getLineVoltage(), equalTo(490.7f));
	}

	@Test
	public void workMode() {
		assertThat("Work mode", data.getWorkMode(), equalTo(KTLCTInverterWorkMode.Running));
	}

	@Test
	public void deviceOperatingState() {
		assertThat("Power mode", data.getDeviceOperatingState(), equalTo(DeviceOperatingState.Normal));
	}

	@Test
	public void deviceOperatingStateOff() throws IOException {
		data.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(new int[] { KTLCTData.POWER_SWITCH_OFF },
						KTLCTRegister.ControlDevicePowerSwitch.getAddress());
				return true;
			}
		});
		assertThat("Power mode", data.getDeviceOperatingState(), equalTo(DeviceOperatingState.Shutdown));
	}

	@Test
	public void outputPowerLimitPercent() {
		assertThat("Output power limit", data.getOutputPowerLimitPercent(), equalTo(100.0f));
	}

	@Test
	public void outputPowerLimitPercentLimited() throws IOException {
		data.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(new int[] { 0x02C3 },
						KTLCTRegister.ControlDevicePowerLimit.getAddress());
				return true;
			}
		});
		assertThat("Output power limit", data.getOutputPowerLimitPercent(), equalTo(70.7f));
	}

	@Test
	public void updateDeviceOperatingStateOff() throws IOException {
		// given
		ModbusConnection conn = EasyMock.createMock(ModbusConnection.class);
		KTLCTData data = new KTLCTData();

		conn.writeWords(eq(ModbusWriteFunction.WriteHoldingRegister),
				eq(KTLCTRegister.ControlDevicePowerSwitch.getAddress()),
				aryEq(new int[] { KTLCTData.POWER_SWITCH_OFF }));

		// when
		replay(conn);
		data.setDeviceOperatingState(conn, DeviceOperatingState.Shutdown);

		// then

		verify(conn);
	}

	@Test
	public void updateDeviceOperatingStateOn() throws IOException {
		// given
		data.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(new int[] { KTLCTData.POWER_SWITCH_OFF },
						KTLCTRegister.ControlDevicePowerSwitch.getAddress());
				return true;
			}
		});

		ModbusConnection conn = EasyMock.createMock(ModbusConnection.class);
		KTLCTData data = new KTLCTData();

		conn.writeWords(eq(ModbusWriteFunction.WriteHoldingRegister),
				eq(KTLCTRegister.ControlDevicePowerSwitch.getAddress()),
				aryEq(new int[] { KTLCTData.POWER_SWITCH_ON }));

		// when
		replay(conn);
		data.setDeviceOperatingState(conn, DeviceOperatingState.Normal);

		// then

		verify(conn);
	}
}
