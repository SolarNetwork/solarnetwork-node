/* ==================================================================
 * PrecipitationTypeTests.java - 10/07/2023 8:33:00 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sunspec.environmental.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.environmental.PrecipitationType;

/**
 * Test cases for the {@link PrecipitationType} class.
 * 
 * @author matt
 * @version 1.0
 */
public class PrecipitationTypeTests {

	@Test
	public void description() {
		String msg = PrecipitationType.HeavyRain.getDescription();
		assertThat(msg, is(equalTo("Rain, heavy")));
	}

}
