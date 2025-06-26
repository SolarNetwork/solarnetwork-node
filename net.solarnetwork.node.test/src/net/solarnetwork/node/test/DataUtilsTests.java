/* ==================================================================
 * DataUtilsTests.java - 4/08/2018 9:41:13 AM
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

package net.solarnetwork.node.test;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import org.junit.Test;

/**
 * Test cases for the {@link DataUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DataUtilsTests {

	private static Integer[] integerArray(int[] data) {
		Integer[] a = new Integer[data.length];
		for ( int i = 0; i < data.length; i++ ) {
			a[i] = data[i];
		}
		return a;
	}

	@Test
	public void parseModbusHexRegisterLinesEmptyString() throws IOException {
		final String data = "";
		int[] result = DataUtils.parseModbusHexRegisterLines(new BufferedReader(new StringReader(data)));
		assertThat("Parsed integers", integerArray(result), arrayWithSize(0));
	}

	@Test
	public void parseModbusHexRegisterLines() throws IOException {
		final String data = "[0]:    0x4031\n[1]:    0x0632\n[2]:    0x01F0\n";
		int[] result = DataUtils.parseModbusHexRegisterLines(new BufferedReader(new StringReader(data)));
		assertThat("Parsed integers", integerArray(result), arrayContaining(0x4031, 0x0632, 0x01F0));
	}

	@Test
	public void parseModbusHexRegisterLinesWithoutTerminatingNewline() throws IOException {
		final String data = "[0]:    0x4031\n[1]:    0x0632\n[2]:    0x01F0";
		int[] result = DataUtils.parseModbusHexRegisterLines(new BufferedReader(new StringReader(data)));
		assertThat("Parsed integers", integerArray(result), arrayContaining(0x4031, 0x0632, 0x01F0));
	}

	@Test
	public void parseModbusHexRegisterLinesWithComments() throws IOException {
		final String data = "# skip 1\n[0]:    0x4031\n[1]:    0x0632\n[2]:    0x01F0\n#skip 2\n";
		int[] result = DataUtils.parseModbusHexRegisterLines(new BufferedReader(new StringReader(data)));
		assertThat("Parsed integers", integerArray(result), arrayContaining(0x4031, 0x0632, 0x01F0));
	}

	@Test
	public void parseModbusHexRegisterLinesWithBlanks() throws IOException {
		final String data = "\n\n\n[0]:    0x4031\n\n[1]:    0x0632\n[2]:    0x01F0\n\n";
		int[] result = DataUtils.parseModbusHexRegisterLines(new BufferedReader(new StringReader(data)));
		assertThat("Parsed integers", integerArray(result), arrayContaining(0x4031, 0x0632, 0x01F0));
	}

	@Test
	public void parseModbusHexMappingLines() throws IOException {
		final String data = "[0]:    0x4031\n[1]:    0x0632\n[2]:    0x01F0\n";
		Map<Integer, Integer> result = DataUtils
				.parseModbusHexRegisterMappingLines(new BufferedReader(new StringReader(data)));
		assertThat("Parsed mapping size", result.keySet(), hasSize(3));
		assertThat("Parsed mapping", result,
				allOf(hasEntry(0, 0x4031), hasEntry(1, 0x0632), hasEntry(2, 0x01F0)));
	}

}
