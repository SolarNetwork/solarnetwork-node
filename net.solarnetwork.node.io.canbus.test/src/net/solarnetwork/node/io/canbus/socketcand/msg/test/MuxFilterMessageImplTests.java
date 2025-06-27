/* ==================================================================
 * MuxFilterMessageImplTests.java - 23/09/2019 4:20:49 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.canbus.socketcand.msg.test;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.List;
import org.junit.Test;
import net.solarnetwork.node.io.canbus.socketcand.msg.MuxFilterMessageImpl;

/**
 * Test cases for the {@link MuxFilterMessageImpl} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MuxFilterMessageImplTests {

	@Test
	public void construct_list_oneFilter() {
		// GIVEN
		List<String> arguments = asList(
				"1 500000 123 2 FF 00 00 00 00 00 00 00 33 FF FF FF FF FF FF FF".split(" "));

		// WHEN
		MuxFilterMessageImpl m = new MuxFilterMessageImpl(arguments);

		// THEN
		assertThat("Address", m.getAddress(), equalTo(0x123));
		assertThat("Seconds", m.getSeconds(), equalTo(1));
		assertThat("Microseconds", m.getMicroseconds(), equalTo(500000));
		assertThat("Fractional seconds", m.getFractionalSeconds(), equalTo(new BigDecimal("1.500000")));
		assertThat("Multiplex identifier bitmask", m.getMultiplexIdentifierBitmask(),
				equalTo(0xFF00000000000000L));
		assertThat("Multiplex filters",
				stream(m.getMultiplexDataFilters().spliterator(), false).collect(toList()),
				contains(0x33FFFFFFFFFFFFFFL));
	}

	@Test
	public void construct_list_multiFilter() {
		// GIVEN
		List<String> arguments = asList(
				"1 500000 123 2 FF 00 00 00 00 00 00 00 33 FF FF FF FF FF FF FF 56 FF 00 00 00 00 FF FF 44 FF FF FF FF 00 00 FF ED 00 00 00 00 00 FF FF"
						.split(" "));

		// WHEN
		MuxFilterMessageImpl m = new MuxFilterMessageImpl(arguments);

		// THEN
		assertThat("Address", m.getAddress(), equalTo(0x123));
		assertThat("Seconds", m.getSeconds(), equalTo(1));
		assertThat("Microseconds", m.getMicroseconds(), equalTo(500000));
		assertThat("Fractional seconds", m.getFractionalSeconds(), equalTo(new BigDecimal("1.500000")));
		assertThat("Multiplex identifier bitmask", m.getMultiplexIdentifierBitmask(),
				equalTo(0xFF00000000000000L));
		assertThat("Multiplex filters",
				stream(m.getMultiplexDataFilters().spliterator(), false).collect(toList()),
				contains(0x33FFFFFFFFFFFFFFL, 0x56FF00000000FFFFL, 0x44FFFFFFFF0000FFL,
						0xED0000000000FFFFL));
	}

	@Test
	public void write_oneFilter() throws IOException {
		// GIVEN
		MuxFilterMessageImpl m = new MuxFilterMessageImpl(0x123, false, 1, 500000, 0xFF00000000000000L,
				asList(0x33FFFFFFFFFFFFFFL));

		// WHEN
		StringWriter out = new StringWriter();
		m.write(out);

		// THEN
		assertThat("Address", m.getAddress(), equalTo(0x123));
		assertThat("Seconds", m.getSeconds(), equalTo(1));
		assertThat("Microseconds", m.getMicroseconds(), equalTo(500000));
		assertThat("Fractional seconds", m.getFractionalSeconds(), equalTo(new BigDecimal("1.500000")));
		assertThat("Multiplex identifier bitmask", m.getMultiplexIdentifierBitmask(),
				equalTo(0xFF00000000000000L));
		assertThat("Multiplex filters",
				stream(m.getMultiplexDataFilters().spliterator(), false).collect(toList()),
				contains(0x33FFFFFFFFFFFFFFL));
		assertThat("Written message", out.toString(),
				equalTo("< muxfilter 1 500000 123 2 FF 00 00 00 00 00 00 00 33 FF FF FF FF FF FF FF >"));
	}

	@Test
	public void write_multiFilter() throws IOException {
		// GIVEN
		MuxFilterMessageImpl m = new MuxFilterMessageImpl(0x123, false, 1, 500000, 0xFF00000000000000L,
				asList(0x33FFFFFFFFFFFFFFL, 0x56FF00000000FFFFL, 0x44FFFFFFFF0000FFL,
						0xED0000000000FFFFL));

		// WHEN
		StringWriter out = new StringWriter();
		m.write(out);

		// THEN
		assertThat("Address", m.getAddress(), equalTo(0x123));
		assertThat("Seconds", m.getSeconds(), equalTo(1));
		assertThat("Microseconds", m.getMicroseconds(), equalTo(500000));
		assertThat("Fractional seconds", m.getFractionalSeconds(), equalTo(new BigDecimal("1.500000")));
		assertThat("Multiplex identifier bitmask", m.getMultiplexIdentifierBitmask(),
				equalTo(0xFF00000000000000L));
		assertThat("Multiplex filters",
				stream(m.getMultiplexDataFilters().spliterator(), false).collect(toList()),
				contains(0x33FFFFFFFFFFFFFFL, 0x56FF00000000FFFFL, 0x44FFFFFFFF0000FFL,
						0xED0000000000FFFFL));
		assertThat("Written message", out.toString(), equalTo(
				"< muxfilter 1 500000 123 5 FF 00 00 00 00 00 00 00 33 FF FF FF FF FF FF FF 56 FF 00 00 00 00 FF FF 44 FF FF FF FF 00 00 FF ED 00 00 00 00 00 FF FF >"));
	}
}
