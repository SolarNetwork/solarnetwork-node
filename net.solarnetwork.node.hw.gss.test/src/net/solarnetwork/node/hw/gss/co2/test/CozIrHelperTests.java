/* ==================================================================
 * CozIrHelperTests.java - 28/08/2020 7:49:20 AM
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

package net.solarnetwork.node.hw.gss.co2.test;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static net.solarnetwork.node.hw.gss.co2.CozIrMessageType.LINE_END;
import static net.solarnetwork.util.ByteUtils.ASCII;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.hw.gss.co2.CozIrData;
import net.solarnetwork.node.hw.gss.co2.CozIrHelper;
import net.solarnetwork.node.hw.gss.co2.CozIrMessageType;
import net.solarnetwork.node.hw.gss.co2.FirmwareVersion;
import net.solarnetwork.node.hw.gss.co2.MeasurementType;
import net.solarnetwork.node.io.serial.SerialConnection;

/**
 * Test cases for the CozIrHelper} class.
 * 
 * @author matt
 * @version 1.0
 */
public class CozIrHelperTests {

	private SerialConnection conn;
	private CozIrHelper helper;

	@Before
	public void setup() {
		conn = EasyMock.createMock(SerialConnection.class);
		helper = new CozIrHelper(conn);
	}

	private void replayAll() {
		EasyMock.replay(conn);
	}

	@After
	public void teardown() {
		EasyMock.verify(conn);
	}

	@Test
	public void getFirmwareVersion() throws IOException {
		// GIVEN
		expect(conn.getPortName()).andReturn("/test/port").anyTimes();

		// set to command mode
		conn.writeMessage(aryEq("K 0\r\n".getBytes(ASCII)));
		expect(conn.readMarkedMessage(aryEq(CozIrMessageType.OperationalMode.getMessageStart()),
				aryEq(LINE_END))).andReturn(" K 00000\r\n".getBytes(ASCII));

		// query for firmware version
		conn.writeMessage(aryEq("Y\r\n".getBytes(ASCII)));
		expect(conn.readMarkedMessage(aryEq(CozIrMessageType.FirmwareVersion.getMessageStart()),
				aryEq(LINE_END))).andReturn(" Y,Oct 25 2016,13:24:49,AL22\r\n".getBytes(ASCII));
		expect(conn.readMarkedMessage(aryEq(CozIrMessageType.SerialNumber.getMessageStart()),
				aryEq(LINE_END))).andReturn(" B 412755 00000\r\n".getBytes(ASCII));

		// return to polling mode
		conn.writeMessage(aryEq("K 2\r\n".getBytes(ASCII)));
		expect(conn.readMarkedMessage(aryEq(CozIrMessageType.OperationalMode.getMessageStart()),
				aryEq(LINE_END))).andReturn(" K 00002\r\n".getBytes(ASCII));

		// WHEN
		replayAll();
		FirmwareVersion v = helper.getFirmwareVersion();

		// THEN
		assertThat("Version returned", v, notNullValue());
		assertThat("Version version", v.getVersion(), equalTo("AL22"));
		assertThat("Serial number", v.getDate(),
				equalTo(LocalDateTime.parse("2016-10-25T13:24:49", ISO_LOCAL_DATE_TIME)));
	}

	@Test
	public void setCo2FreshAirLevel() throws IOException {
		// GIVEN
		expect(conn.getPortName()).andReturn("/test/port").anyTimes();

		// set MSB
		conn.writeMessage(aryEq("P 10 7\r\n".getBytes(ASCII)));
		expect(conn.readMarkedMessage(aryEq(CozIrMessageType.CO2CalibrationZeroLevel.getMessageStart()),
				aryEq(LINE_END))).andReturn(" P 00010 00007\r\n".getBytes(ASCII));

		// set LSB
		conn.writeMessage(aryEq("P 11 208\r\n".getBytes(ASCII)));
		expect(conn.readMarkedMessage(aryEq(CozIrMessageType.CO2CalibrationZeroLevel.getMessageStart()),
				aryEq(LINE_END))).andReturn(" P 00011 00208\r\n".getBytes(ASCII));

		// WHEN
		replayAll();
		helper.setCo2FreshAirLevel(2000);
	}

	@Test
	public void calibrateAsFreshAirLevel() throws IOException {
		// GIVEN
		expect(conn.getPortName()).andReturn("/test/port").anyTimes();

		conn.writeMessage(aryEq("G\r\n".getBytes(ASCII)));
		expect(conn.readMarkedMessage(aryEq(CozIrMessageType.CO2CalibrateZeroFreshAir.getMessageStart()),
				aryEq(LINE_END))).andReturn(" G 33390\r\n".getBytes(ASCII));

		// WHEN
		replayAll();
		helper.calibrateAsCo2FreshAirLevel();
	}

	@Test
	public void getAltitudeCompensation() throws IOException {
		// GIVEN
		expect(conn.getPortName()).andReturn("/test/port").anyTimes();

		conn.writeMessage(aryEq("s\r\n".getBytes(ASCII)));
		expect(conn.readMarkedMessage(aryEq(CozIrMessageType.AltitudeCompensationGet.getMessageStart()),
				aryEq(LINE_END))).andReturn("s 08192\r\n".getBytes(ASCII));

		// WHEN
		replayAll();
		int result = helper.getAltitudeCompensation();
		assertThat("Result parsed", result, equalTo(8192));
	}

	@Test
	public void setAltitudeCompensation() throws IOException {
		// GIVEN
		expect(conn.getPortName()).andReturn("/test/port").anyTimes();

		conn.writeMessage(aryEq("S 9876\r\n".getBytes(ASCII)));
		expect(conn.readMarkedMessage(aryEq(CozIrMessageType.AltitudeCompensationSet.getMessageStart()),
				aryEq(LINE_END))).andReturn(" S 09876\r\n".getBytes(ASCII));

		// WHEN
		replayAll();
		helper.setAltitudeCompensation(9876);
	}

	@Test
	public void setMeasurements() throws IOException {
		// GIVEN
		expect(conn.getPortName()).andReturn("/test/port").anyTimes();

		conn.writeMessage(aryEq("M 6\r\n".getBytes(ASCII)));
		expect(conn.readMarkedMessage(aryEq(CozIrMessageType.MeasurementsSet.getMessageStart()),
				aryEq(LINE_END))).andReturn(" M 00006\r\n".getBytes(ASCII));

		// WHEN
		replayAll();
		helper.setMeasurementOutput(
				EnumSet.of(MeasurementType.Co2Filtered, MeasurementType.Co2Unfiltered));
	}

	@Test
	public void getMeasurements() throws IOException {
		// GIVEN
		expect(conn.getPortName()).andReturn("/test/port").anyTimes();

		// read scale factor
		conn.writeMessage(aryEq(".\r\n".getBytes(ASCII)));
		expect(conn.readMarkedMessage(aryEq(CozIrMessageType.CO2ScaleFactor.getMessageStart()),
				aryEq(LINE_END))).andReturn(" . 00010\r\n".getBytes(ASCII));

		// read measurements
		conn.writeMessage(aryEq("Q\r\n".getBytes(ASCII)));
		expect(conn.readMarkedMessage(aryEq(new byte[] { ' ' }), aryEq(LINE_END)))
				.andReturn(" H 00419 T 01216 Z 01592 z 01647\r\n".getBytes(ASCII));

		// WHEN
		replayAll();
		CozIrData data = helper.getMeasurements();
		assertThat("Data returned", data, notNullValue());
		assertThat("CO2 value scaled", data.getCo2(), equalTo(new BigDecimal(15920)));
		assertThat("CO2 unfiltered value scaled", data.getCo2Unfiltered(),
				equalTo(new BigDecimal(16470)));
		assertThat("Humidity value scaled", data.getHumidity(), equalTo(new BigDecimal("41.9")));
		assertThat("Temperature value scaled", data.getTemperature(), equalTo(new BigDecimal("21.6")));
	}
}
