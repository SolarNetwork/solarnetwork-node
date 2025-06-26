/* ==================================================================
 * ModbusFunctionTests.java - 24/01/2020 3:28:29 pm
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

package net.solarnetwork.node.io.modbus.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import org.junit.Test;
import net.solarnetwork.node.io.modbus.ModbusFunction;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusWriteFunction;

/**
 * Test cases for the {@link ModbusFunction} interface.
 * 
 * @author matt
 * @version 1.0
 */
public class ModbusFunctionTests {

	@Test
	public void functionForCode_read() {
		for ( ModbusReadFunction f : ModbusReadFunction.values() ) {
			assertThat("Code returned", ModbusFunction.functionForCode(f.getCode()), equalTo(f));
		}
	}

	@Test
	public void functionForCode_write() {
		for ( ModbusWriteFunction f : ModbusWriteFunction.values() ) {
			assertThat("Code returned", ModbusFunction.functionForCode(f.getCode()), equalTo(f));
		}
	}

}
