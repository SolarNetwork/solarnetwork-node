/* ==================================================================
 * KTLDatumDataSourceTests.java - 8/07/2022 8:47:03 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.csi.ktl.test;

import static net.solarnetwork.node.datum.csi.ktl.test.TestDataUtils.parseDataDataRegisters;
import static net.solarnetwork.node.datum.csi.ktl.test.TestDataUtils.toShortArray;
import static net.solarnetwork.node.io.modbus.ModbusReadFunction.ReadHoldingRegister;
import static net.solarnetwork.node.io.modbus.ModbusReadFunction.ReadInputRegister;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.datum.csi.ktl.KTLDatumDataSource;
import net.solarnetwork.node.domain.datum.AcDcEnergyDatum;
import net.solarnetwork.node.hw.csi.inverter.KTLCTData;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link KTLDatumDataSource} class.
 * 
 * @author matt
 * @version 1.0
 */
public class KTLDatumDataSourceTests {

	private static final String TEST_SOURCE_ID = UUID.randomUUID().toString();

	private ModbusNetwork modbusNetwork;
	private ModbusConnection modbusConnection;
	private KTLDatumDataSource dataSource;
	private KTLCTData data;

	@Before
	public void setup() {
		modbusNetwork = EasyMock.createMock(ModbusNetwork.class);
		modbusConnection = EasyMock.createMock(ModbusConnection.class);
		data = new KTLCTData();
		dataSource = new KTLDatumDataSource(data);
		dataSource.setSourceId(TEST_SOURCE_ID);
		dataSource.setModbusNetwork(new StaticOptionalService<ModbusNetwork>(modbusNetwork));
	}

	@After
	public void teardown() {
		EasyMock.verify(modbusNetwork, modbusConnection);
	}

	private void replayAll() {
		EasyMock.replay(modbusNetwork, modbusConnection);
	}

	private void expectMockModbusConnectionAction() throws IOException {
		Capture<ModbusConnectionAction<Object>> connActionCapture = Capture.newInstance();
		expect(modbusNetwork.performAction(eq(1), capture(connActionCapture)))
				.andAnswer(new IAnswer<Object>() {

					@Override
					public Object answer() throws Throwable {
						ModbusConnectionAction<?> action = connActionCapture.getValue();
						return action.doWithConnection(modbusConnection);
					}
				});
	}

	@Test
	public void collectDatum() throws IOException {
		// GIVEN
		expectMockModbusConnectionAction();

		// first have to read all of config + data for first time

		final Map<Integer, Integer> data1 = parseDataDataRegisters(getClass(), "test-data-01-A.txt");
		expect(modbusConnection.readWords(ReadInputRegister, 0, 59)).andReturn(toShortArray(data1));

		final Map<Integer, Integer> data2 = parseDataDataRegisters(getClass(), "test-data-01-B.txt");
		expect(modbusConnection.readWords(ReadHoldingRegister, 4096, 2)).andReturn(toShortArray(data2));

		final Map<Integer, Integer> data3 = parseDataDataRegisters(getClass(), "test-data-01-D.txt");
		expect(modbusConnection.readWords(ReadInputRegister, 33027, 15)).andReturn(toShortArray(data3));

		// then re-read data

		final Map<Integer, Integer> data4 = parseDataDataRegisters(getClass(), "test-data-01-C.txt");
		expect(modbusConnection.readWords(ReadInputRegister, 22, 37)).andReturn(toShortArray(data4));

		expect(modbusConnection.readWords(ReadHoldingRegister, 4096, 2)).andReturn(toShortArray(data2));

		expect(modbusConnection.readWords(ReadInputRegister, 33027, 15)).andReturn(toShortArray(data3));

		// WHEN
		replayAll();
		long start = System.currentTimeMillis();
		AcDcEnergyDatum d = dataSource.readCurrentDatum();

		// THEN
		assertThat("Datum returned", d, notNullValue());
		assertThat("Datum source ID", d.getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Datum created now", d.getTimestamp().toEpochMilli(), greaterThanOrEqualTo(start));
		/*- TODO
		assertThat("Datum frequency", d.asSampleOperations().getSampleFloat(Instantaneous, "frequency"),
				equalTo(50.05f));
		assertThat("Datum voltage", d.asSampleOperations().getSampleFloat(Instantaneous, "voltage"),
				equalTo(248.13335f));
		assertThat("Datum current", d.asSampleOperations().getSampleFloat(Instantaneous, "current"),
				equalTo(0.7f));
		assertThat("Datum power factor",
				d.asSampleOperations().getSampleFloat(Instantaneous, "powerFactor"), equalTo(1.0f));
		assertThat("Datum apparent power",
				d.asSampleOperations().getSampleInteger(Instantaneous, "apparentPower"), equalTo(70));
		assertThat("Datum reactive power",
				d.asSampleOperations().getSampleInteger(Instantaneous, "reactivePower"), equalTo(0));
		assertThat("Datum DC voltage", d.asSampleOperations().getSampleFloat(Instantaneous, "dcVoltage"),
				equalTo(406.9f));
		assertThat("Datum DC power", d.asSampleOperations().getSampleInteger(Instantaneous, "dcPower"),
				equalTo(61));
		assertThat("Datum power", d.asSampleOperations().getSampleInteger(Instantaneous, "watts"),
				equalTo(70));
		assertThat("Datum energy", d.asSampleOperations().getSampleLong(Accumulating, "wattHours"),
				equalTo(11937020L));
		assertThat("Datum status", d.asSampleOperations().getSampleString(Status, "phase"),
				equalTo("Total"));
		assertThat("Datum opState", d.asSampleOperations().getSampleInteger(Status, "opState"),
				equalTo(DeviceOperatingState.Normal.getCode()));
		assertThat("Datum eve ts", d.asSampleOperations().getSampleInteger(Status, "events"),
				equalTo(0));
		*/
	}

}
