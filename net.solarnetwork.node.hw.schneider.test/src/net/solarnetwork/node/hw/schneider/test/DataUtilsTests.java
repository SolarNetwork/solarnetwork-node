/* ==================================================================
 * DataUtilsTests.java - 17/05/2018 3:49:59 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import net.solarnetwork.node.hw.schneider.meter.DataUtils;

/**
 * Test cases for the {@link DataUtils} class.
 * 
 * @author matt
 * @version 3.0
 */
public class DataUtilsTests {

	@Test
	public void testReadMeterManufactureDate() {
		LocalDateTime result = DataUtils
				.parseDateTime(new short[] { 14, ((7 << 8) | (5 << 4) | 31), ((12 << 8) | 27), 30599 });
		assertThat("Date", result, equalTo(
				LocalDateTime.of(2014, 7, 31, 12, 27, 30, (int) TimeUnit.MILLISECONDS.toNanos(599))));
	}

}
