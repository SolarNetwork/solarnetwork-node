/* ==================================================================
 * PowerGatePlusRegisterTests.java - 8/11/2019 9:50:42 am
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

package net.solarnetwork.node.hw.satcon.test;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import bak.pcj.set.IntRangeSet;
import net.solarnetwork.node.hw.satcon.PowerGatePlusRegister;
import net.solarnetwork.node.io.modbus.IntRangeSetUtils;

/**
 * Test cases for the {@link PowerGatePlusRegister} class.
 * 
 * @author matt
 * @version 1.0
 */
public class PowerGatePlusRegisterTests {

	@Test
	public void configRegisterSet() {
		IntRangeSet set = PowerGatePlusRegister.getConfigRegisterAddressSet();
		IntRangeSet norm = IntRangeSetUtils.combineToReduceSize(set, 64);
		assertThat("Set available", norm, notNullValue());
	}

	@Test
	public void inverterRegisterSet() {
		IntRangeSet set = PowerGatePlusRegister.getInverterRegisterAddressSet();
		IntRangeSet norm = IntRangeSetUtils.combineToReduceSize(set, 64);
		assertThat("Set available", norm, notNullValue());
	}

	@Test
	public void controlRegisterSet() {
		IntRangeSet set = PowerGatePlusRegister.getControlRegisterAddressSet();
		IntRangeSet norm = IntRangeSetUtils.combineToReduceSize(set, 64);
		assertThat("Set available", norm, notNullValue());
	}

}
