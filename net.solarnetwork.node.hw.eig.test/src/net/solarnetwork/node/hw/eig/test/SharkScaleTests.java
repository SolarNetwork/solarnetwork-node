/* ==================================================================
 * SharkScaleTests.java - 26/07/2018 1:35:35 PM
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
import net.solarnetwork.node.hw.eig.meter.SharkScale;

/**
 * Test cases for the {@link SharkScale} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SharkScaleTests {

	@Test
	public void forCode() {
		assertThat("Unit", SharkScale.forCode(0), equalTo(SharkScale.Unit));
		assertThat("Kilo", SharkScale.forCode(3), equalTo(SharkScale.Kilo));
		assertThat("Mega", SharkScale.forCode(6), equalTo(SharkScale.Mega));
		assertThat("Auto", SharkScale.forCode(8), equalTo(SharkScale.Auto));
	}

	@Test(expected = IllegalArgumentException.class)
	public void forCodeIllegal() {
		SharkScale.forCode(123);
	}

	@Test
	public void codes() {
		assertThat("Unit", SharkScale.Unit.getCode(), equalTo(0));
		assertThat("Kilo", SharkScale.Kilo.getCode(), equalTo(3));
		assertThat("Mega", SharkScale.Mega.getCode(), equalTo(6));
		assertThat("Auto", SharkScale.Auto.getCode(), equalTo(8));
	}

	@Test
	public void forEnergyRegisterValue() {
		assertThat("Unit", SharkScale.forEnergyRegisterValue(0x00), equalTo(SharkScale.Unit));
		assertThat("Kilo", SharkScale.forEnergyRegisterValue(0x30), equalTo(SharkScale.Kilo));
		assertThat("Mega", SharkScale.forEnergyRegisterValue(0x60), equalTo(SharkScale.Mega));
	}

	@Test(expected = IllegalArgumentException.class)
	public void forEnergyRegisterValueIllegal() {
		SharkScale.forEnergyRegisterValue(0x10);
	}

	@Test
	public void forPowerRegisterValue() {
		assertThat("Unit", SharkScale.forPowerRegisterValue(0x0000), equalTo(SharkScale.Unit));
		assertThat("Kilo", SharkScale.forPowerRegisterValue(0x3000), equalTo(SharkScale.Kilo));
		assertThat("Mega", SharkScale.forPowerRegisterValue(0x6000), equalTo(SharkScale.Mega));
		assertThat("Auto", SharkScale.forPowerRegisterValue(0x8000), equalTo(SharkScale.Auto));
	}

	@Test(expected = IllegalArgumentException.class)
	public void forPowerRegisterValueIllegal() {
		SharkScale.forPowerRegisterValue(0x9000);
	}

	@Test
	public void scaleFactors() {
		assertThat("Unit", SharkScale.Unit.getScaleFactor(), equalTo(1));
		assertThat("Kilo", SharkScale.Kilo.getScaleFactor(), equalTo(1000));
		assertThat("Mega", SharkScale.Mega.getScaleFactor(), equalTo(1000000));
		assertThat("Auto", SharkScale.Auto.getScaleFactor(), equalTo(1));
	}

}
