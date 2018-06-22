/* ==================================================================
 * GeneralAtmosphericDatumTests.java - 22/06/2018 4:46:06 PM
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

package net.solarnetwork.node.domain.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import org.junit.Test;
import net.solarnetwork.node.domain.AtmosphericDatum;
import net.solarnetwork.node.domain.GeneralAtmosphericDatum;

/**
 * Test cases for the {@link GeneralAtmosphericDatum} class.
 * 
 * @author matt
 * @version 1.0
 */
public class GeneralAtmosphericDatumTests {

	@Test
	public void irradianceSetter() {
		// given
		BigDecimal v = new BigDecimal("1.234");

		// when
		GeneralAtmosphericDatum d = new GeneralAtmosphericDatum();
		d.setIrradiance(v);

		// then
		assertThat("Irradiance set as instantaneous property",
				d.getInstantaneousSampleBigDecimal(AtmosphericDatum.IRRADIANCE_KEY), equalTo(v));
	}

	@Test
	public void irradianceGetter() {
		// given
		BigDecimal v = new BigDecimal("1.234");
		GeneralAtmosphericDatum d = new GeneralAtmosphericDatum();
		d.putInstantaneousSampleValue(AtmosphericDatum.IRRADIANCE_KEY, v);

		// when
		BigDecimal result = d.getIrradiance();

		// then
		assertThat("Irradiance get from instantaneous property", result, equalTo(v));
	}
}
