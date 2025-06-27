/* ==================================================================
 * AEInverterTypeTests.java - 27/07/2018 4:16:46 PM
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
import net.solarnetwork.node.hw.ae.inverter.tx.AEInverterType;

/**
 * Test cases for the {@link AEInverterType} class.
 * 
 * @author matt
 * @version 1.0
 */
public class AEInverterTypeTests {

	@Test
	public void forModelCode() {
		assertThat("Model code", AEInverterType.forModelCode("0272"), equalTo(AEInverterType.PVP30kW));
		assertThat("Model code", AEInverterType.forModelCode("0300"), equalTo(AEInverterType.AE35TX));
		assertThat("Model code", AEInverterType.forModelCode("0304"), equalTo(AEInverterType.AE50TX));
		assertThat("Model code", AEInverterType.forModelCode("0276"), equalTo(AEInverterType.AE75TX));
		assertThat("Model code", AEInverterType.forModelCode("0280"), equalTo(AEInverterType.AE100TX));
		assertThat("Model code", AEInverterType.forModelCode("0312"), equalTo(AEInverterType.AE250TX));

		// following can't be tested because same codes for 250 & 260 models
		//assertThat("Model code", AEInverterType.forModelCode("0312"), equalTo(AEInverterType.AE260TX));

		assertThat("Model code", AEInverterType.forModelCode("0386"), equalTo(AEInverterType.AE500TX));
	}

	@Test(expected = IllegalArgumentException.class)
	public void forModelCodeInvalid() {
		AEInverterType.forModelCode("XXXX");
	}

	@Test
	public void forInverterId() {
		assertThat("Model code", AEInverterType.forInverterId("XX0272XXX"),
				equalTo(AEInverterType.PVP30kW));
	}

	@Test(expected = IllegalArgumentException.class)
	public void forInverterIdInvalid() {
		AEInverterType.forInverterId("XX0000XXX");
	}

}
