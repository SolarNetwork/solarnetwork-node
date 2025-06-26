/* ==================================================================
 * StaticDataMapModbusConnectionTests.java - 24/01/2020 9:52:40 am
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

package net.solarnetwork.node.io.modbus.support.test;

import static net.solarnetwork.node.io.modbus.ModbusDataUtils.shortArray;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import java.io.IOException;
import org.junit.Test;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusWriteFunction;
import net.solarnetwork.node.io.modbus.support.StaticDataMapModbusConnection;
import net.solarnetwork.util.IntShortMap;

/**
 * Test cases for the {@link StaticDataMapModbusConnection} class.
 * 
 * @author matt
 * @version 1.0
 */
public class StaticDataMapModbusConnectionTests {

	@Test
	public void writeData() throws IOException {
		IntShortMap data = new IntShortMap();
		int[] raw = new int[] { 0x1234, 0x4321, 0xFFFF, 0x1000 };
		try (ModbusConnection conn = new StaticDataMapModbusConnection(data)) {
			conn.writeWords(ModbusWriteFunction.WriteHoldingRegister, 10, shortArray(raw));
			assertThat("Stored data size", data.keySet(), hasSize(raw.length));
			for ( int i = 0; i < raw.length; i++ ) {
				assertThat("Stored data " + (10 + i), data, hasEntry(10 + i, (short) raw[i]));
			}
		}
	}

}
