/* ==================================================================
 * DefaultTransientSettingDaoTests.java - 11/04/2022 6:47:17 AM
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.dao.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.util.concurrent.ConcurrentMap;
import org.junit.Test;
import net.solarnetwork.node.dao.DefaultTransientSettingDao;
import net.solarnetwork.node.dao.TransientSettingDao;

/**
 * Test cases for the {@link DefaultTransientSettingDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DefaultTransientSettingDaoTests {

	@Test
	public void settings_firstTime() {
		// GIVEN
		TransientSettingDao dao = new DefaultTransientSettingDao();

		// WHEN
		ConcurrentMap<String, Object> settings = dao.settings("foo");

		// THEN
		assertThat("Settings map created", settings, is(notNullValue()));
	}

	@Test
	public void settings_secondTime() {
		// GIVEN
		TransientSettingDao dao = new DefaultTransientSettingDao();

		// WHEN
		ConcurrentMap<String, Object> settings = dao.settings("foo");
		ConcurrentMap<String, Object> settings2 = dao.settings("foo");

		// THEN
		assertThat("Settings map returns same instance for same key", settings2,
				is(sameInstance(settings)));
	}

	@Test
	public void settings_store() {
		// GIVEN
		TransientSettingDao dao = new DefaultTransientSettingDao();
		ConcurrentMap<String, Object> settings = dao.settings("foo");

		// WHEN
		settings.put("foo", "bar");

		// THEN
		assertThat("Settings map returns same instance for same key", settings, hasEntry("foo", "bar"));
	}

}
