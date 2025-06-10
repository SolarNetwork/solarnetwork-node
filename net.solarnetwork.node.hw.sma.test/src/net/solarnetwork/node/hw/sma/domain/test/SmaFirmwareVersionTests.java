/* ==================================================================
 * SmaFirmwareVersionTests.java - 11/09/2020 9:30:03 AM
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import org.junit.Test;
import net.solarnetwork.node.hw.sma.domain.SmaFirmwareVersion;
import net.solarnetwork.node.hw.sma.domain.SmaReleaseType;

/**
 * Test cases for the {@link SmaFirmwareVersion} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SmaFirmwareVersionTests {

	@Test
	public void docExample() {
		SmaFirmwareVersion v = SmaFirmwareVersion.forRegisterValue(0x01050A04);
		assertThat("Major version decoded", v.getMajorVersion(), equalTo(1));
		assertThat("Minor version decoded", v.getMinorVersion(), equalTo(5));
		assertThat("Build version decoded", v.getBuildVersion(), equalTo(10));
		assertThat("Release type decoded", v.getReleaseType(), equalTo(SmaReleaseType.Release));
		assertThat("String value", v.toString(), equalTo("1.5.10.R"));
	}

	@Test
	public void noSpecial() {
		SmaFirmwareVersion v = SmaFirmwareVersion.forRegisterValue(0x0F231C06);
		assertThat("Major version decoded", v.getMajorVersion(), equalTo(15));
		assertThat("Minor version decoded", v.getMinorVersion(), equalTo(35));
		assertThat("Build version decoded", v.getBuildVersion(), equalTo(28));
		assertThat("Release type decoded", v.getReleaseType(), equalTo(SmaReleaseType.None));
		assertThat("String value", v.toString(), equalTo("15.35.28.6"));
	}

}
