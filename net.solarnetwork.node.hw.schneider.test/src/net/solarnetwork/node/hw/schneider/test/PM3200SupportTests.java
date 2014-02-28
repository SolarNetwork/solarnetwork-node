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

import java.io.IOException;
import net.solarnetwork.node.hw.schneider.meter.PM3200Support;
import net.solarnetwork.node.io.modbus.ModbusConnectionCallback;
import net.solarnetwork.node.io.modbus.ModbusSerialConnectionFactory;
import net.solarnetwork.node.test.AbstractNodeTest;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.StaticOptionalService;
import net.wimpi.modbus.net.SerialConnection;
import org.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;

/**
 * Test cases for the {@link PM3200Support} class.
 * 
 * @author matt
 * @version 1.0
 */
@ContextConfiguration
public class PM3200SupportTests extends AbstractNodeTest {

	@Autowired
	private net.solarnetwork.node.io.modbus.JamodModbusSerialConnectionFactory connectionFactory;

	@Value("${meter.unitId}")
	private Integer unitId;

	@Value("${meter.name}")
	private String meterName;

	@Value("${meter.model}")
	private String meterModel;

	@Value("${meter.manufacturer}")
	private String meterManufacturer;

	@Value("${meter.serialNumber}")
	private Long meterSerialNumber;

	@Value("${meter.manufactureDate}")
	private Long meterManufactureDate;

	private OptionalService<ModbusSerialConnectionFactory> connectionFactoryService;
	private PM3200Support support;

	@Before
	public void setup() {
		connectionFactoryService = new StaticOptionalService<ModbusSerialConnectionFactory>(
				connectionFactory);
		support = new PM3200Support();
		support.setConnectionFactory(connectionFactoryService);
		support.setUnitId(unitId);
	}

	@Test
	public void testReadMeterName() {
		String result = connectionFactory.execute(new ModbusConnectionCallback<String>() {

			@Override
			public String doInConnection(SerialConnection conn) throws IOException {
				return support.getMeterName(conn);
			}
		});
		Assert.assertEquals("Meter name", meterName, result);
	}

	@Test
	public void testReadMeterModel() {
		String result = connectionFactory.execute(new ModbusConnectionCallback<String>() {

			@Override
			public String doInConnection(SerialConnection conn) throws IOException {
				return support.getMeterModel(conn);
			}
		});
		Assert.assertEquals("Meter model", meterModel, result);
	}

	@Test
	public void testReadMeterManufacturer() {
		String result = connectionFactory.execute(new ModbusConnectionCallback<String>() {

			@Override
			public String doInConnection(SerialConnection conn) throws IOException {
				return support.getMeterManufacturer(conn);
			}
		});
		Assert.assertEquals("Meter manufacturer", meterManufacturer, result);
	}

	@Test
	public void testReadMeterSerialNumber() {
		Long result = connectionFactory.execute(new ModbusConnectionCallback<Long>() {

			@Override
			public Long doInConnection(SerialConnection conn) throws IOException {
				return support.getMeterSerialNumber(conn);
			}
		});
		Assert.assertEquals("Meter serial number", meterSerialNumber, result);
	}

	@Test
	public void testReadMeterManufactureDate() {
		LocalDateTime result = connectionFactory.execute(new ModbusConnectionCallback<LocalDateTime>() {

			@Override
			public LocalDateTime doInConnection(SerialConnection conn) throws IOException {
				return support.getMeterManufactureDate(conn);
			}
		});
		LocalDateTime expected = new LocalDateTime(meterManufactureDate);
		Assert.assertEquals("Meter manufacture date", expected, result);
	}

}
