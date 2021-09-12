/* ==================================================================
 * PanasonicBatteryDatumDataSourceTests.java - 17/02/2016 6:20:45 am
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

package net.solarnetwork.node.datum.panasonic.battery.test;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import java.io.IOException;
import java.time.Instant;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.datum.panasonic.battery.PanasonicBatteryDatumDataSource;
import net.solarnetwork.node.domain.datum.EnergyStorageDatum;
import net.solarnetwork.node.hw.panasonic.battery.BatteryAPIClient;
import net.solarnetwork.node.hw.panasonic.battery.BatteryData;

/**
 * Test cases for the {@link PanasonicBatteryDatumDataSource} class.
 * 
 * @author matt
 * @version 2.0
 */
public class PanasonicBatteryDatumDataSourceTests {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final String TEST_EMAIL = "test@localhost";
	private final String TEST_SOURCE_ID = "Test Battery";

	private BatteryAPIClient client;
	private PanasonicBatteryDatumDataSource service;

	@Before
	public void setup() {
		service = new PanasonicBatteryDatumDataSource();
		client = EasyMock.createMock(BatteryAPIClient.class);
		service.setClient(client);
		service.setSourceId(TEST_SOURCE_ID);
		service.setEmail(TEST_EMAIL);
	}

	@Test
	public void testReadDatum() throws IOException {
		final BatteryData data = new BatteryData("test", Instant.now(), "A", 10, 100);
		expect(client.getCurrentBatteryDataForEmail(TEST_EMAIL)).andReturn(data);

		replay(client);

		EnergyStorageDatum result = service.readCurrentDatum();
		log.debug("Read GeneralNodeEnergyStorageDatum: {}", result);

		verify(client);

		Assert.assertNotNull("GeneralNodeEnergyStorageDatum", result);
		Assert.assertEquals("Source ID", TEST_SOURCE_ID, result.getSourceId());
		Assert.assertEquals("Available Wh", Long.valueOf(10), result.getAvailableEnergy());
		Assert.assertEquals("Available percentage", Float.valueOf(0.1f),
				result.getAvailableEnergyPercentage());
	}

}
