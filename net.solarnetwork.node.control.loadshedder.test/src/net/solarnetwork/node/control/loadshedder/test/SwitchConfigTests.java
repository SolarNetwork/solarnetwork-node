/* ==================================================================
 * SwitchConfigTests.java - 27/06/2015 12:10:21 pm
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.loadshedder.test;

import java.util.Calendar;
import org.junit.Assert;
import org.junit.Test;
import net.solarnetwork.node.control.loadshedder.LoadShedControlConfig;

/**
 * Test cases for the {@link LoadShedControlConfig} class.
 * 
 * @author matt
 * @version 2.0
 */
public class SwitchConfigTests {

	@Test
	public void timeWindowBetweenNone() {
		LoadShedControlConfig conf = new LoadShedControlConfig();
		Assert.assertTrue("Any time", conf.fallsWithinTimeWindow(System.currentTimeMillis()));
	}

	@Test
	public void timeWindowBetween() {
		Calendar c = Calendar.getInstance();
		c.set(2010, 1, 1, 12, 0);
		LoadShedControlConfig conf = new LoadShedControlConfig();
		conf.setTimeWindowStart("8:00");
		conf.setTimeWindowEnd("20:00");
		Assert.assertTrue("Around noon", conf.fallsWithinTimeWindow(c.getTimeInMillis()));

		c.set(Calendar.HOUR_OF_DAY, 7);
		Assert.assertFalse("Before 8am", conf.fallsWithinTimeWindow(c.getTimeInMillis()));

		c.set(Calendar.HOUR_OF_DAY, 21);
		Assert.assertFalse("After 8pm", conf.fallsWithinTimeWindow(c.getTimeInMillis()));
	}

	@Test
	public void timeWindowBefore() {
		Calendar c = Calendar.getInstance();
		c.set(2010, 1, 1, 12, 0);
		LoadShedControlConfig conf = new LoadShedControlConfig();
		conf.setTimeWindowEnd("20:00");

		c.set(Calendar.HOUR_OF_DAY, 7);
		Assert.assertTrue("Before 8pm", conf.fallsWithinTimeWindow(c.getTimeInMillis()));

		c.set(Calendar.HOUR_OF_DAY, 21);
		Assert.assertFalse("After 8pm", conf.fallsWithinTimeWindow(c.getTimeInMillis()));
	}

	@Test
	public void timeWindowAfter() {
		Calendar c = Calendar.getInstance();
		c.set(2010, 1, 1, 12, 0);
		LoadShedControlConfig conf = new LoadShedControlConfig();
		conf.setTimeWindowStart("8:00");

		c.set(Calendar.HOUR_OF_DAY, 7);
		Assert.assertFalse("Before 8am", conf.fallsWithinTimeWindow(c.getTimeInMillis()));

		c.set(Calendar.HOUR_OF_DAY, 21);
		Assert.assertTrue("After 8am", conf.fallsWithinTimeWindow(c.getTimeInMillis()));
	}
}
