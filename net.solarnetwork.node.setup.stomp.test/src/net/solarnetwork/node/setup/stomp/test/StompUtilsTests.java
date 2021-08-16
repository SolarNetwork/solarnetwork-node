/* ==================================================================
 * StompUtilsTests.java - 16/08/2021 7:56:04 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.stomp.test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import net.solarnetwork.node.setup.stomp.StompUtils;

/**
 * Test cases for the {@link StompUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class StompUtilsTests {

	@Test
	public void encodeHeader_plain() {
		String v = StompUtils.encodeStompHeaderValue("nothing to escape");
		assertThat("No change", v, is("nothing to escape"));
	}

	@Test
	public void encodeHeader_carriagereturn() {
		String v = StompUtils.encodeStompHeaderValue("a\rb");
		assertThat("Carriage return escaped", v, is("a\\rb"));
	}

	@Test
	public void encodeHeader_linefeed() {
		String v = StompUtils.encodeStompHeaderValue("a\nb");
		assertThat("Linefeed escaped", v, is("a\\nb"));
	}

	@Test
	public void encodeHeader_colon() {
		String v = StompUtils.encodeStompHeaderValue("a:b");
		assertThat("Colon escaped", v, is("a\\cb"));
	}

	@Test
	public void encodeHeader_backslash() {
		String v = StompUtils.encodeStompHeaderValue("a\\b");
		assertThat("Backslash escaped", v, is("a\\\\b"));
	}

	@Test
	public void decodeHeader_plain() {
		String v = StompUtils.decodeStompHeaderValue("nothing to escape");
		assertThat("No change", v, is("nothing to escape"));
	}

	@Test
	public void decodeHeader_carriagereturn() {
		String v = StompUtils.decodeStompHeaderValue("a\\rb");
		assertThat("Carriage return decoded", v, is("a\rb"));
	}

	@Test
	public void decodeHeader_linefeed() {
		String v = StompUtils.decodeStompHeaderValue("a\\nb");
		assertThat("Linefeed decoded", v, is("a\nb"));
	}

	@Test
	public void decodeHeader_colon() {
		String v = StompUtils.decodeStompHeaderValue("a\\cb");
		assertThat("Colon decoded", v, is("a:b"));
	}

	@Test
	public void decodeHeader_backslash() {
		String v = StompUtils.decodeStompHeaderValue("a\\\\b");
		assertThat("Backslash decoded", v, is("a\\b"));
	}

}
