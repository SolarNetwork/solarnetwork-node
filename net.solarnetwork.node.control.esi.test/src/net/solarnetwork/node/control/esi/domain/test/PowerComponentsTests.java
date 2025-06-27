/* ==================================================================
 * PowerComponentsTests.java - 9/08/2019 2:38:17 pm
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

package net.solarnetwork.node.control.esi.domain.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import java.util.Map;
import org.junit.Test;
import net.solarnetwork.node.control.esi.domain.PowerComponents;

/**
 * Test cases for the {@link PowerComponents} class.
 * 
 * @author matt
 * @version 1.0
 */
public class PowerComponentsTests {

	@Test
	public void apparentPowerNoPowerValues() {
		PowerComponents p = new PowerComponents();
		assertThat("Apparent power computes to zero", p.derivedApparentPower(), equalTo(0.0));
	}

	@Test
	public void apparentPower() {
		PowerComponents p = new PowerComponents(12345L, 23456L);
		double expected = 26506.281538533465; // Math.sqrt(12345L * 12345L + 23456L * 23456L);
		assertThat("Apparent power computes to zero", p.derivedApparentPower(),
				closeTo(expected, 0.00001));
	}

	@Test
	public void asMap() {
		// given
		Long realPower = 1L;
		Long reactivePower = 2L;
		PowerComponents pc = new PowerComponents(realPower, reactivePower);

		// when
		Map<String, Object> m = pc.asMap();

		// then
		assertThat("Map size", m.keySet(), hasSize(2));
		assertThat("Map value", m,
				allOf(hasEntry("realPower", realPower), hasEntry("reactivePower", reactivePower)));
	}

}
