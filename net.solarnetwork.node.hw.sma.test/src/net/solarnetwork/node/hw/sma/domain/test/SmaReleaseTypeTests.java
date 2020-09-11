/* ==================================================================
 * SmaReleaseTypeTests.java - 11/09/2020 9:24:31 AM
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

package net.solarnetwork.node.hw.sma.domain.test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import net.solarnetwork.node.hw.sma.domain.SmaReleaseType;

/**
 * Test cases for the {@link SmaReleaseType} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SmaReleaseTypeTests {

	@Test
	public void docExample() {
		SmaReleaseType t = SmaReleaseType.forCode(0x4);
		assertThat("Release decoded", t, equalTo(SmaReleaseType.Release));
	}

	@Test
	public void noSpecial() {
		SmaReleaseType t = SmaReleaseType.forCode(0x6);
		assertThat("None decoded", t, equalTo(SmaReleaseType.None));
	}

}
