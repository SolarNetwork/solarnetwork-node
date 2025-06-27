/* ==================================================================
 * SerialUtilsTests.java - 26/01/2020 7:55:55 am
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

package net.solarnetwork.node.io.serial.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import org.junit.Test;
import net.solarnetwork.node.io.serial.SerialUtils;

/**
 * Test cases for the {@link SerialUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SerialUtilsTests {

	@Test
	public void parseInt64() {
		Long result = SerialUtils.parseInt64(0x1010, 0x2020, 0x3030, 0x4040);
		assertThat("Result value", result, equalTo(0x1010202030304040L));
	}

}
