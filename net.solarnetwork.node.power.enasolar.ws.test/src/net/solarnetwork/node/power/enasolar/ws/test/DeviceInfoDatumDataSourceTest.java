/* ==================================================================
 * DeviceInfoDatumDataSourceTest.java - Oct 2, 2011 9:24:34 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.power.enasolar.ws.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import net.solarnetwork.node.power.PowerDatum;
import net.solarnetwork.node.power.enasolar.ws.DeviceInfoDatumDataSource;
import net.solarnetwork.node.test.AbstractNodeTransactionalTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * Test case for the {@link DeviceInfoDatumDataSource} class.
 * 
 * @author matt
 * @version 1.1
 */
@ContextConfiguration
public class DeviceInfoDatumDataSourceTest extends AbstractNodeTransactionalTest {

	@Autowired
	private DeviceInfoDatumDataSource dataSource;

	@Test
	public void parseDatum() {
		PowerDatum datum = dataSource.readCurrentDatum();
		log.debug("Got datum: {}", datum);
		assertEquals(Long.valueOf(57540), datum.getWattHourReading());
		assertNotNull(datum.getAcOutputVolts());
		assertEquals(241.1, datum.getAcOutputVolts().doubleValue(), 0.001);
		assertNotNull(datum.getAcOutputAmps());
		assertEquals(2.604, datum.getAcOutputAmps().doubleValue(), 0.001);
	}
}
