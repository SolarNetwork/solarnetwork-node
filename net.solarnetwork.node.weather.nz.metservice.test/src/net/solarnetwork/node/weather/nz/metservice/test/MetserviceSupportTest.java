/* ==================================================================
 * MetserviceSupportTest.java - 28/05/2016 8:11:15 am
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

package net.solarnetwork.node.weather.nz.metservice.test;

import java.util.List;
import net.solarnetwork.node.test.AbstractNodeTest;
import net.solarnetwork.node.weather.nz.metservice.MetserviceSupport;
import net.solarnetwork.node.weather.nz.metservice.NewZealandWeatherLocation;
import net.solarnetwork.util.ClassUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for the MetserviceSupport class.
 * 
 * @author matt
 * @version 1.0
 */
public class MetserviceSupportTest extends AbstractNodeTest {

	@Test
	public void loadWeatherLocationCSV() {
		List<NewZealandWeatherLocation> locations = MetserviceSupport.availableWeatherLocations();
		Assert.assertNotNull(locations);
		String[] sortedKeys = ClassUtils
				.getResourceAsString("sorted-metservice-locations.txt", getClass()).trim().split("\n");
		Assert.assertEquals(sortedKeys.length, locations.size());
		for ( int i = 0; i < sortedKeys.length; i++ ) {
			Assert.assertEquals("Key " + i, sortedKeys[i], locations.get(i).getKey());
		}
	}

}
