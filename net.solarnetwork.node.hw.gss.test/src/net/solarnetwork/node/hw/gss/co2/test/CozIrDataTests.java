/* ==================================================================
 * CozIrDataTests.java - 27/08/2020 4:30:57 PM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.gss.co2.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import java.math.BigDecimal;
import org.junit.Test;
import net.solarnetwork.node.hw.gss.co2.CozIrData;

/**
 * Test cases for the {@link CozIrData} class.
 * 
 * @author matt
 * @version 1.0
 */
public class CozIrDataTests {

	@Test
	public void create_factory() {
		CozIrData d = CozIrData.forRawValue(65, 60, 10, 345, 1195);
		assertThat("CO2 scaled", d.getCo2(), equalTo(new BigDecimal("650")));
		assertThat("CO2 unfiltered scaled", d.getCo2Unfiltered(), equalTo(new BigDecimal("600")));
		assertThat("Humdity scaled", d.getHumidity(), equalTo(new BigDecimal("34.5")));
		assertThat("Temperature scaled", d.getTemperature(), equalTo(new BigDecimal("19.5")));
	}

}
