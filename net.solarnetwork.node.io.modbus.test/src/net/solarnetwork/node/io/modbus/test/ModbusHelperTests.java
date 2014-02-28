/* ==================================================================
 * ModbusHelperTests.java - 1/03/2014 7:28:50 AM
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

package net.solarnetwork.node.io.modbus.test;

import net.solarnetwork.node.io.modbus.ModbusHelper;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for the {@link ModbusHelper} class.
 * 
 * @author matt
 * @version 1.0
 */
public class ModbusHelperTests {

	@Test
	public void testParseFloat32() {
		Float result = ModbusHelper.parseFloat32(new Integer[] { 0x403F, 0xA7F6 });
		Assert.assertNotNull(result);
		Assert.assertEquals("Float value", 2.994626, result.doubleValue(), 0.000001);
	}

	@Test
	public void testParseFloat32NaN() {
		Float result = ModbusHelper.parseFloat32(new Integer[] { 0xFFC0, 0x0000 });
		Assert.assertNull("Float value is NaN", result);
	}

}
