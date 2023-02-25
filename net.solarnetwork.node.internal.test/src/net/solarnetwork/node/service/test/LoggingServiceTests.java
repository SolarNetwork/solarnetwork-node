/* ==================================================================
 * LoggingServiceTests.java - 24/02/2023 2:20:41 pm
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

package net.solarnetwork.node.service.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import org.junit.Test;
import net.solarnetwork.node.service.LoggingService;

/**
 * Test cases for the {@link LoggingService} API.
 * 
 * @author matt
 * @version 1.0
 */
public class LoggingServiceTests {

	@Test
	public void parseLevelNamesCaseInsensitively() {
		for ( LoggingService.Level level : LoggingService.Level.values() ) {
			assertThat("Upper-case level parsed",
					LoggingService.Level.forName(level.name().toUpperCase()), is(equalTo(level)));
			assertThat("Lower-case level parsed",
					LoggingService.Level.forName(level.name().toLowerCase()), is(equalTo(level)));
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void parseNull() {
		LoggingService.Level.forName(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void parseEmpty() {
		LoggingService.Level.forName("");
	}

	@Test(expected = IllegalArgumentException.class)
	public void parseInvalid() {
		LoggingService.Level.forName("Not a level");
	}

}
