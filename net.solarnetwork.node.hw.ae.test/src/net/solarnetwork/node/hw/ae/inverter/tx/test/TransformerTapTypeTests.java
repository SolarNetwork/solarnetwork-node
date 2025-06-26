/* ==================================================================
 * TransformerTapTypeTests.java - 27/07/2018 4:29:06 PM
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

package net.solarnetwork.node.hw.ae.inverter.tx.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Test;
import net.solarnetwork.node.hw.ae.inverter.tx.TransformerTapType;

/**
 * Test cases for the {@link TransformerTapType} class.
 * 
 * @author matt
 * @version 1.0
 */
public class TransformerTapTypeTests {

	@Test
	public void forCode() {
		assertThat("265 volts", TransformerTapType.forCode(8), equalTo(TransformerTapType.V_265));
		assertThat("295 volts", TransformerTapType.forCode(0), equalTo(TransformerTapType.V_295));
	}

	@Test
	public void forRegisterValue() {
		assertThat("265 volts", TransformerTapType.forRegisterValue(0xFF),
				equalTo(TransformerTapType.V_265));
		assertThat("295 volts", TransformerTapType.forRegisterValue(0xF1),
				equalTo(TransformerTapType.V_295));
	}
}
