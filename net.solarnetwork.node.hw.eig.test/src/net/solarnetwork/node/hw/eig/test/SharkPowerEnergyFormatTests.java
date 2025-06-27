/* ==================================================================
 * SharkPowerEnergyFormatTests.java - 26/07/2018 1:53:28 PM
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
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import java.math.BigDecimal;
import org.junit.Test;
import net.solarnetwork.node.hw.eig.meter.SharkPowerEnergyFormat;
import net.solarnetwork.node.hw.eig.meter.SharkScale;

/**
 * Test cases for the {@link SharkPowerEnergyFormat} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SharkPowerEnergyFormatTests {

	@Test
	public void getters() {
		SharkPowerEnergyFormat f = new SharkPowerEnergyFormat(SharkScale.Kilo, 1, SharkScale.Mega, 2);
		assertThat("powerScale", f.getPowerScale(), equalTo(SharkScale.Kilo));
		assertThat("numEnergyDigits", f.getNumEnergyDigits(), equalTo(1));
		assertThat("energyScale", f.getEnergyScale(), equalTo(SharkScale.Mega));
		assertThat("energyDigitsAfterDecimal", f.getEnergyDigitsAfterDecimal(), equalTo(2));
	}

	@Test
	public void forRegisterValue() {
		SharkPowerEnergyFormat f = SharkPowerEnergyFormat.forRegisterValue(0x8263);
		assertThat("powerScale", f.getPowerScale(), equalTo(SharkScale.Auto));
		assertThat("numEnergyDigits", f.getNumEnergyDigits(), equalTo(2));
		assertThat("energyScale", f.getEnergyScale(), equalTo(SharkScale.Mega));
		assertThat("energyDigitsAfterDecimal", f.getEnergyDigitsAfterDecimal(), equalTo(3));
	}

	@Test
	public void forRegisterValueCached() {
		final int w = 0x8263;
		SharkPowerEnergyFormat f = SharkPowerEnergyFormat.forRegisterValue(w);
		assertThat("Cached instance", f, sameInstance(SharkPowerEnergyFormat.forRegisterValue(w)));
	}

	@Test
	public void energyValue() {
		SharkPowerEnergyFormat f = SharkPowerEnergyFormat.forRegisterValue(0x3362);
		Number e = f.energyValue(-1411084);
		assertThat("14,110.84 MWh", e, equalTo(new BigDecimal("-14110840000.00")));
	}

}
