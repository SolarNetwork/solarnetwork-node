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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
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
import net.solarnetwork.domain.AcPhase;
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
	public void setup() {
		setupData(TEST_DATA);
	}

	private void setupData(Map<Integer, Integer> dataMap) {
		try {
			data.performUpdates(new ModbusDataUpdateAction() {

				@Override
				public boolean updateModbusData(MutableModbusData m) {
					m.saveDataMap(dataMap);
					return true;
				}
			});
		} catch ( IOException e ) {
			throw new RuntimeException("Error loading data " + dataMap, e);
		}
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
		expect(conn.readWords(ModbusReadFunction.ReadInputRegister, 33027, 15))
				.andReturn(mapSlice(TEST_DATA, 33027, 15));

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
	public void reactivePower() {
		assertThat("Reactive power", data.getReactivePower(), equalTo(800));
	}

	@Test
	public void reactivePower_phased() {
		assertThat("Reactive power A", data.accessorForPhase(AcPhase.PhaseA).getReactivePower(),
				equalTo(0));
		assertThat("Reactive power B", data.accessorForPhase(AcPhase.PhaseB).getReactivePower(),
				equalTo(594800));
		assertThat("Reactive power C", data.accessorForPhase(AcPhase.PhaseC).getReactivePower(),
				equalTo(900));
	}

	@Test
	public void apparentPower() {
		assertThat("Apparent power", data.getApparentPower(), equalTo(15700));
	}

	@Test
	public void current() {
		assertThat("Current", data.getCurrent(), equalTo(42.6f));
	}

	@Test
	public void current_phased() {
		assertThat("Current A", data.accessorForPhase(AcPhase.PhaseA).getCurrent(), equalTo(14.2f));
		assertThat("Current B", data.accessorForPhase(AcPhase.PhaseB).getCurrent(), equalTo(14.5f));
		assertThat("Current C", data.accessorForPhase(AcPhase.PhaseC).getCurrent(), equalTo(13.9f));
	}

	@Test
	public void voltage() {
		assertThat("Voltage", data.getVoltage(), equalTo(100.86667f));
	}

	@Test
	public void lineVoltage() {
		assertThat("Line voltage", data.getLineVoltage(), equalTo(490.7f));
	}

	@Test
	public void lineVoltage_phased() {
		assertThat("Line voltage A", data.accessorForPhase(AcPhase.PhaseA).getLineVoltage(),
				equalTo(490.6f));
		assertThat("Line voltage B", data.accessorForPhase(AcPhase.PhaseB).getLineVoltage(),
				equalTo(490.0f));
		assertThat("Line voltage C", data.accessorForPhase(AcPhase.PhaseC).getLineVoltage(),
				equalTo(491.5f));
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

	@Test
	public void readPermananentFault() throws IOException {
		// GIVEN
		setupData(parseTestData("data-02.txt"));

		// THEN
		assertThat(data.getWorkMode(), equalTo(KTLCTInverterWorkMode.Fault));
		assertThat(data.getPermanentFaults(), containsInAnyOrder(KTLCTPermanentFault.Fault0110));
	}
}
