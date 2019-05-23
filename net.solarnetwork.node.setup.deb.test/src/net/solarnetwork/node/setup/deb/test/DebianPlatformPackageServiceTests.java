/* ==================================================================
 * DebianPlatformPackageServiceTests.java - 23/05/2019 3:04:35 pm
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

package net.solarnetwork.node.setup.deb.test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import net.solarnetwork.node.setup.deb.DebianPlatformPackageService;

/**
 * Test cases for the {@link DebianPlatformPackageService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DebianPlatformPackageServiceTests {

	@Test
	public void handlesDotDeb() {
		DebianPlatformPackageService s = new DebianPlatformPackageService();
		assertThat(".deb extension handled", s.handlesPackage("some thing ending in.deb"),
				equalTo(true));
	}

	@Test
	public void ignoresNotDotDeb() {
		DebianPlatformPackageService s = new DebianPlatformPackageService();
		assertThat("Not .deb extension ignored", s.handlesPackage("some thing not ending in dot deb"),
				equalTo(false));
	}

}
