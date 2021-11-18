/* ==================================================================
 * WebBoxDataDeviceTests.java - 17/11/2021 6:59:07 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

import static net.solarnetwork.node.hw.sma.modbus.SmaCommonDeviceRegister.SerialNumber;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Map.Entry;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.hw.sma.domain.SmaDeviceDataAccessor;
import net.solarnetwork.node.hw.sma.domain.SmaDeviceType;
import net.solarnetwork.node.hw.sma.modbus.SmaScStringMonitorControllerData;
import net.solarnetwork.node.hw.sma.modbus.webbox.WebBoxDataDevice;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.io.modbus.support.StaticDataMapReadonlyModbusConnection;
import net.solarnetwork.node.test.DataUtils;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.util.IntShortMap;

/**
 * Test cases for the {@link WebBoxDataDevice} class.
 * 
 * @author matt
 * @version 1.0
 */
public class WebBoxDataDeviceTests {

	private static final Logger log = LoggerFactory.getLogger(WebBoxDataDeviceTests.class);

	public static final int TEST_UNIT_ID = 1;

	private ModbusNetwork modbusNetwork;
	private OptionalService<ModbusNetwork> optModbusNetwork;

	@Before
	public void setup() {
		modbusNetwork = EasyMock.createMock(ModbusNetwork.class);
		optModbusNetwork = new StaticOptionalService<>(modbusNetwork);
	}

	@After
	public void teardown() {
		EasyMock.verify(modbusNetwork);
	}

	private void replayAll() {
		EasyMock.replay(modbusNetwork);
	}

	private static IntShortMap parseTestData(String resource) {
		IntShortMap result = new IntShortMap();
		try {
			Map<Integer, Integer> m = DataUtils.parseModbusHexRegisterMappingLines(new BufferedReader(
					new InputStreamReader(WebBoxDataDeviceTests.class.getResourceAsStream(resource))));
			for ( Entry<Integer, Integer> e : m.entrySet() ) {
				result.putValue(e.getKey(), e.getValue());
			}
		} catch ( IOException e ) {
			log.error("Error reading modbus data resource [{}]", resource, e);
		}
		return result;
	}

	@Test
	public void refreshData_cached() throws IOException {
		// GIVEN
		final ModbusConnection conn = new StaticDataMapReadonlyModbusConnection(
				parseTestData("device-data-01.txt"));

		Capture<ModbusConnectionAction<ModbusData>> connActionCapture = new Capture<>();
		expect(modbusNetwork.performAction(eq(TEST_UNIT_ID), capture(connActionCapture)))
				.andAnswer(new IAnswer<ModbusData>() {

					@Override
					public ModbusData answer() throws Throwable {
						ModbusConnectionAction<ModbusData> action = connActionCapture.getValue();
						return action.doWithConnection(conn);
					}
				});

		short[] serialNumberData = new short[] { (short) 1, (short) 2, (short) 3 };
		WebBoxDataDevice<SmaScStringMonitorControllerData> device = new WebBoxDataDevice<>(
				optModbusNetwork, 1, SmaDeviceType.SunnyCentralStringMonitorUS,
				new SmaScStringMonitorControllerData(serialNumberData, SerialNumber.getAddress()));

		// WHEN
		replayAll();
		Instant now = Instant.now();
		SmaDeviceDataAccessor result = device.refreshData(0);
		SmaDeviceDataAccessor result2 = device.refreshData(Long.MAX_VALUE);

		// THEN
		assertThat("Result provided", result, is(notNullValue()));
		assertThat("Data timestamp close to read date",
				now.until(result.getDataTimestamp(), ChronoUnit.MILLIS), is(lessThanOrEqualTo(100L)));
		assertThat("Cached data returned on 2nd call", result2.getDataTimestamp(),
				is(equalTo(result.getDataTimestamp())));
	}

	@Test
	public void refreshData_refreshedFromCache() throws IOException, InterruptedException {
		// GIVEN
		final ModbusConnection conn = new StaticDataMapReadonlyModbusConnection(
				parseTestData("device-data-01.txt"));

		Capture<ModbusConnectionAction<ModbusData>> connActionCapture = new Capture<>(CaptureType.ALL);
		expect(modbusNetwork.performAction(eq(TEST_UNIT_ID), capture(connActionCapture)))
				.andAnswer(new IAnswer<ModbusData>() {

					@Override
					public ModbusData answer() throws Throwable {
						ModbusConnectionAction<ModbusData> action = connActionCapture.getValues()
								.get(connActionCapture.getValues().size() - 1);
						return action.doWithConnection(conn);
					}
				}).times(2);

		short[] serialNumberData = new short[] { (short) 1, (short) 2, (short) 3 };
		WebBoxDataDevice<SmaScStringMonitorControllerData> device = new WebBoxDataDevice<>(
				optModbusNetwork, 1, SmaDeviceType.SunnyCentralStringMonitorUS,
				new SmaScStringMonitorControllerData(serialNumberData, SerialNumber.getAddress()));

		// WHEN
		replayAll();
		Instant now = Instant.now();
		SmaDeviceDataAccessor result = device.refreshData(0L);
		SmaDeviceDataAccessor result2 = device.refreshData(Long.MAX_VALUE);
		Thread.sleep(100);
		Instant now2 = Instant.now();
		SmaDeviceDataAccessor result3 = device.refreshData(99L);

		// THEN
		assertThat("Result provided", result, is(notNullValue()));
		assertThat("Data timestamp close to read date",
				now.until(result.getDataTimestamp(), ChronoUnit.MILLIS), is(lessThanOrEqualTo(80L)));
		assertThat("Cached data returned on 2nd call", result2.getDataTimestamp(),
				is(equalTo(result.getDataTimestamp())));
		assertThat("Refreshed data 3rd call date close to 3rd read date",
				now2.until(result3.getDataTimestamp(), ChronoUnit.MILLIS), is(lessThanOrEqualTo(80L)));
		assertThat("3rd call date after 1st call date",
				result.getDataTimestamp().until(result3.getDataTimestamp(), ChronoUnit.MILLIS),
				is(greaterThan(0L)));
	}

}
