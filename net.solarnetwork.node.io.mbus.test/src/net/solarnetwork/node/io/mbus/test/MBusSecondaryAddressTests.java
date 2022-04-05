/* ==================================================================
 * MBusSecondaryAddressTests.java - 6/04/2022 10:18:40 AM
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.mbus.test;

import static org.apache.commons.codec.binary.Hex.decodeHex;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.apache.commons.codec.DecoderException;
import org.junit.Test;
import net.solarnetwork.node.io.mbus.MBusSecondaryAddress;

/**
 * Test cases for the {@link MBusSecondaryAddress} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MBusSecondaryAddressTests {

	@Test
	public void isValid_invalid() {
		// GIVEN
		MBusSecondaryAddress addr = new MBusSecondaryAddress("0000000000000000");

		// THEN
		assertThat("Invalid address", addr.isValid(), is(false));
	}

	@Test
	public void isValid_valid() {
		// GIVEN
		MBusSecondaryAddress addr = new MBusSecondaryAddress("1234567890ABCDEF");

		// THEN
		assertThat("Invalid address", addr.isValid(), is(true));
	}

	@Test
	public void equals_identity() throws DecoderException {
		// GIVEN
		final byte[] data = decodeHex("1234567890ABCDEF");
		MBusSecondaryAddress addr = new MBusSecondaryAddress(data);

		// THEN
		assertThat("Addresses equal for same byte array instance",
				addr.equals(new MBusSecondaryAddress(data)), is(true));
	}

	@Test
	public void equals_content() throws DecoderException {
		MBusSecondaryAddress addr = new MBusSecondaryAddress(decodeHex("1234567890ABCDEF"));

		// THEN
		assertThat("Addresses equal for same byte array instance",
				addr.equals(new MBusSecondaryAddress(decodeHex("1234567890ABCDEF"))), is(true));
	}

	@Test
	public void notEquals() throws DecoderException {
		MBusSecondaryAddress addr = new MBusSecondaryAddress(decodeHex("1234567890ABCDEF"));

		// THEN
		assertThat("Addresses equal for same byte array instance",
				addr.equals(new MBusSecondaryAddress(decodeHex("01234567890ABCDE"))), is(false));
	}

}
