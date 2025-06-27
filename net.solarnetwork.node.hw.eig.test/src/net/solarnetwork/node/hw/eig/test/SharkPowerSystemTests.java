/* ==================================================================
 * SharkPowerSystemTests.java - 26/07/2018 2:21:58 PM
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

package net.solarnetwork.node.hw.eig.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Test;
import net.solarnetwork.node.hw.eig.meter.SharkPowerSystem;

/**
 * Tests for the {@link SharkPowerSystem} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SharkPowerSystemTests {

	@Test
	public void forCode() {
		assertThat("ThreeElementWye", SharkPowerSystem.forCode(0),
				equalTo(SharkPowerSystem.ThreeElementWye));
		assertThat("TwoCtDelta", SharkPowerSystem.forCode(1), equalTo(SharkPowerSystem.TwoCtDelta));
		assertThat("TwoFiveElementWye", SharkPowerSystem.forCode(3),
				equalTo(SharkPowerSystem.TwoFiveElementWye));
	}

	@Test(expected = IllegalArgumentException.class)
	public void forCodeIllegal() {
		SharkPowerSystem.forCode(123);
	}

	@Test
	public void codes() {
		assertThat("ThreeElementWye", SharkPowerSystem.ThreeElementWye.getCode(), equalTo(0));
		assertThat("TwoCtDelta", SharkPowerSystem.TwoCtDelta.getCode(), equalTo(1));
		assertThat("TwoFiveElementWye", SharkPowerSystem.TwoFiveElementWye.getCode(), equalTo(3));
	}

	@Test
	public void forRegisterValue() {
		assertThat("ThreeElementWye", SharkPowerSystem.forRegisterValue(0x0),
				equalTo(SharkPowerSystem.ThreeElementWye));
		assertThat("TwoCtDelta", SharkPowerSystem.forRegisterValue(0x1),
				equalTo(SharkPowerSystem.TwoCtDelta));
		assertThat("TwoFiveElementWye", SharkPowerSystem.forRegisterValue(0x3),
				equalTo(SharkPowerSystem.TwoFiveElementWye));
	}

	@Test(expected = IllegalArgumentException.class)
	public void forEnergyRegisterValueIllegal() {
		SharkPowerSystem.forRegisterValue(0x2);
	}

}
