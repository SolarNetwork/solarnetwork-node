/* ==================================================================
 * PM3200ConsumptionDatumDataSourceTests.java - 1/03/2014 12:24:49 PM
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

package net.solarnetwork.node.consumption.schneider.pm3200.test;

import java.util.Collection;
import net.solarnetwork.node.consumption.ConsumptionDatum;
import net.solarnetwork.node.consumption.schneider.pm3200.PM3200ConsumptionDatumDataSource;
import net.solarnetwork.node.io.modbus.JamodModbusSerialConnectionFactory;
import net.solarnetwork.node.io.modbus.ModbusSerialConnectionFactory;
import net.solarnetwork.node.test.AbstractNodeTest;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.StaticOptionalService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;

/**
 * Test cases for the {@link PM3200ConsumptionDatumDataSource} class.
 * 
 * @author matt
 * @version 1.0
 */
@ContextConfiguration
public class PM3200ConsumptionDatumDataSourceTests extends AbstractNodeTest {

	@Autowired
	private JamodModbusSerialConnectionFactory connectionFactory;

	@Value("${meter.unitId}")
	private Integer unitId;

	private OptionalService<ModbusSerialConnectionFactory> connectionFactoryService;
	private PM3200ConsumptionDatumDataSource service;

	@Before
	public void setup() {
		connectionFactoryService = new StaticOptionalService<ModbusSerialConnectionFactory>(
				connectionFactory);
		service = new PM3200ConsumptionDatumDataSource();
		service.setConnectionFactory(connectionFactoryService);
		service.setUnitId(unitId);
	}

	@Test
	public void testReadConsumptionDatumMain() {
		ConsumptionDatum result = service.readCurrentDatum();
		log.debug("Read ConsumptionDatum: {}", result);
		Assert.assertNotNull("ConsumptionDatum", result);
		Assert.assertEquals("Source ID", PM3200ConsumptionDatumDataSource.MAIN_SOURCE_ID,
				result.getSourceId());
		Assert.assertNotNull("Current", result.getAmps());
		Assert.assertNotNull("Voltage", result.getVolts());
		Assert.assertNotNull("Total energy", result.getWattHourReading());
	}

	@Test
	public void testReadConsumptionDatumsMain() {
		Collection<ConsumptionDatum> results = service.readMultipleDatum();
		log.debug("Read multi ConsumptionDatum: {}", results);
		Assert.assertNotNull("ConsumptionDatum Collection", results);
		Assert.assertEquals("ConsumptionDatum count", 1, results.size());
		ConsumptionDatum result = results.iterator().next();
		Assert.assertNotNull("ConsumptionDatum", result);
		Assert.assertEquals("Source ID", PM3200ConsumptionDatumDataSource.MAIN_SOURCE_ID,
				result.getSourceId());
		Assert.assertNotNull("Current", result.getAmps());
		Assert.assertNotNull("Voltage", result.getVolts());
		Assert.assertNotNull("Total energy", result.getWattHourReading());
	}
}
