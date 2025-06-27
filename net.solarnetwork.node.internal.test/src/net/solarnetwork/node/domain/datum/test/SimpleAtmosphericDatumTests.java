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

package net.solarnetwork.node.domain.datum.test;

import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.Test;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.AtmosphericDatum;
import net.solarnetwork.node.domain.datum.SimpleAtmosphericDatum;

/**
 * Test cases for the {@link SimpleAtmosphericDatum} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleAtmosphericDatumTests {

	@Test
	public void irradianceSetter() {
		// given
		BigDecimal v = new BigDecimal("1.234");

		// when
		SimpleAtmosphericDatum d = new SimpleAtmosphericDatum("foo", Instant.now(), new DatumSamples());
		d.setIrradiance(v);

		// then
		assertThat("Irradiance set as instantaneous property", d.asSampleOperations()
				.getSampleBigDecimal(Instantaneous, AtmosphericDatum.IRRADIANCE_KEY), equalTo(v));
	}

	@Test
	public void irradianceGetter() {
		// given
		BigDecimal v = new BigDecimal("1.234");
		SimpleAtmosphericDatum d = new SimpleAtmosphericDatum("foo", Instant.now(), new DatumSamples());
		d.asMutableSampleOperations().putSampleValue(Instantaneous, AtmosphericDatum.IRRADIANCE_KEY, v);

		// when
		BigDecimal result = d.getIrradiance();

		// then
		assertThat("Irradiance get from instantaneous property", result, equalTo(v));
	}
}
