/* ==================================================================
 * SettingsServiceTests.java - 17/09/2019 7:16:06 am
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

package net.solarnetwork.node.settings.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.junit.Test;
import net.solarnetwork.node.Constants;
import net.solarnetwork.node.settings.SettingsService;

/**
 * Test cases for the {@link SettingsService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SettingsServiceTests {

	@Test
	public void resourcePathDefault() {
		System.clearProperty(SettingsService.SYSTEM_PROP_SETTING_RESOURCE_DIR);
		Path p = SettingsService.settingResourceDirectory();
		assertThat("Default resource path", p, equalTo(
				Paths.get(Constants.solarNodeHome(), SettingsService.DEFAULT_SETTING_RESOURCE_DIR)));
	}

	@Test
	public void resourcePathCustomAbsolute() {
		String tmpDir = "/tmp/" + UUID.randomUUID().toString();
		System.setProperty(SettingsService.SYSTEM_PROP_SETTING_RESOURCE_DIR, tmpDir);
		Path p = SettingsService.settingResourceDirectory();
		assertThat("Absolute resource path", p, equalTo(Paths.get(tmpDir)));
	}

	@Test
	public void resourcePathCustomRelative() {
		String tmpDir = UUID.randomUUID().toString() + "/foo";
		System.setProperty(SettingsService.SYSTEM_PROP_SETTING_RESOURCE_DIR, tmpDir);
		Path p = SettingsService.settingResourceDirectory();
		assertThat("Relative resource path", p, equalTo(Paths.get(Constants.solarNodeHome(), tmpDir)));
	}

}
