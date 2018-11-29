/* ==================================================================
 * PM3200SupportTests.java - 28/02/2014 3:13:38 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.schneider.test;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.easymock.EasyMock;
import org.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.hw.schneider.meter.PM3200Support;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.test.AbstractNodeTest;
import net.solarnetwork.util.StaticOptionalService;
import net.solarnetwork.util.StringUtils;

/**
 * Test cases for the {@link PM3200Support} class.
 * 
 * @author matt
 * @version 2.0
 */
public class PM3200SupportTests extends AbstractNodeTest {

	private ModbusNetwork modbus;
	private ModbusConnection conn;
	private PM3200Support support;

	@Before
	public void setup() {
		modbus = EasyMock.createMock(ModbusNetwork.class);
		conn = EasyMock.createMock(ModbusConnection.class);
		support = new PM3200Support();
		support.setModbusNetwork(new StaticOptionalService<ModbusNetwork>(modbus));
		support.setUnitId(UNIT_ID);
	}

	private static final String METER_NAME = "Test Meter";
	private static final String METER_MODEL = "PM32XX_TEST";
	private static final String METER_MANF = "Test Corporation";
	private static final Long METER_SERIAL = 123456L;
	private static final int UNIT_ID = 1;

	@SuppressWarnings("unchecked")
	private <T> ModbusConnectionAction<T> anyAction(Class<T> type) {
		return EasyMock.anyObject(ModbusConnectionAction.class);
	}

	@Test
	public void readMeterName() throws IOException {
		expect(conn.readString(PM3200Support.ADDR_SYSTEM_METER_NAME, 20, true,
				ModbusConnection.UTF8_CHARSET)).andReturn(METER_NAME);

		replay(conn);

		String result = support.getMeterName(conn);

		verify(conn);

		Assert.assertEquals("Meter name", METER_NAME, result);
	}

	@Test
	public void readMeterModel() {
		expect(conn.readString(PM3200Support.ADDR_SYSTEM_METER_NAME, 20, true,
				ModbusConnection.UTF8_CHARSET)).andReturn(METER_MODEL);
		replay(conn);

		String result = support.getMeterName(conn);

		verify(conn);

		Assert.assertEquals("Meter model", METER_MODEL, result);
	}

	@Test
	public void readMeterManufacturer() {
		expect(conn.readString(PM3200Support.ADDR_SYSTEM_METER_MANUFACTURER, 20, true,
				ModbusConnection.UTF8_CHARSET)).andReturn(METER_MANF);
		replay(conn);

		String result = support.getMeterManufacturer(conn);

		verify(conn);

		Assert.assertEquals("Meter manufacturer", METER_MANF, result);
	}

	@Test
	public void readMeterSerialNumber() {
		expect(conn.readValues(PM3200Support.ADDR_SYSTEM_METER_SERIAL_NUMBER, 2))
				.andReturn(new Integer[] { 1, 57920 });

		replay(conn);

		Long result = support.getMeterSerialNumber(conn);

		verify(conn);
		Assert.assertEquals("Meter serial number", METER_SERIAL, result);
	}

	@Test
	public void testReadMeterManufactureDate() {
		expect(conn.readInts(PM3200Support.ADDR_SYSTEM_METER_MANUFACTURE_DATE, 4))
				.andReturn(new int[] { 14, ((7 << 8) | (5 << 4) | 31), ((12 << 8) | 27), 30599 });

		replay(conn);

		LocalDateTime result = support.getMeterManufactureDate(conn);

		verify(conn);

		LocalDateTime expectedDate = new LocalDateTime(2014, 7, 31, 12, 27, 30, 599);
		Assert.assertEquals("Meter manufacture date", expectedDate, result);
	}

	@Test
	public void readMeterInfoMessage() throws IOException {
		expect(modbus.performAction(anyAction(Map.class), EasyMock.eq(UNIT_ID)))
				.andDelegateTo(new AbstractModbusNetwork() {

					@Override
					public <T> T performAction(ModbusConnectionAction<T> action, int unitId)
							throws IOException {
						try {
							conn.open();
							return action.doWithConnection(conn);
						} finally {
							conn.close();
						}
					}

				});

		conn.open();

		expect(conn.readString(PM3200Support.ADDR_SYSTEM_METER_NAME, 20, true,
				ModbusConnection.UTF8_CHARSET)).andReturn(METER_NAME);
		expect(conn.readString(PM3200Support.ADDR_SYSTEM_METER_MODEL, 20, true,
				ModbusConnection.UTF8_CHARSET)).andReturn(METER_MODEL);
		expect(conn.readString(PM3200Support.ADDR_SYSTEM_METER_MANUFACTURER, 20, true,
				ModbusConnection.UTF8_CHARSET)).andReturn(METER_MANF);
		expect(conn.readInts(PM3200Support.ADDR_SYSTEM_METER_MANUFACTURE_DATE, 4))
				.andReturn(new int[] { 14, ((7 << 8) | (5 << 4) | 31), ((12 << 8) | 27), 30599 });
		expect(conn.readValues(PM3200Support.ADDR_SYSTEM_METER_SERIAL_NUMBER, 2))
				.andReturn(new Integer[] { 1, 57920 });

		conn.close();

		replay(modbus, conn);

		String result = support.getDeviceInfoMessage();
		List<Object> data = new ArrayList<Object>(10);
		data.add(METER_NAME);
		data.add(METER_MODEL);
		data.add(METER_MANF);
		LocalDateTime expectedDate = new LocalDateTime(2014, 7, 31, 12, 27, 30, 599);
		data.add(expectedDate.toLocalDate().toString());
		data.add(METER_SERIAL);
		String expected = StringUtils.delimitedStringFromCollection(data, " / ");
		Assert.assertEquals("Meter info message", expected, result);
	}

}
