/* ==================================================================
 * SettingsUserServiceTests.java - 12/08/2021 5:41:51 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.security.test;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import net.solarnetwork.node.IdentityService;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.setup.UserAuthenticationInfo;
import net.solarnetwork.node.setup.security.SettingsUserService;

/**
 * Test cases for the {@link SettingsUserService}.
 * 
 * @author matt
 * @version 1.0
 */
public class SettingsUserServiceTests {

	private SettingDao settingDao;
	private IdentityService identityService;
	private PasswordEncoder passwordEncoder;

	private SettingsUserService service;

	@Before
	public void setup() {
		settingDao = EasyMock.createMock(SettingDao.class);
		identityService = EasyMock.createMock(IdentityService.class);
		passwordEncoder = new BCryptPasswordEncoder();
		service = new SettingsUserService(settingDao, identityService, passwordEncoder);
	}

	@After
	public void teardown() {
		EasyMock.verify(settingDao, identityService);
	}

	private void replayAll() {
		EasyMock.replay(settingDao, identityService);
	}

	@Test
	public void authInfo() {
		// GIVEN
		final String username = "foo";
		final String hashedPassword = "$2a$10$bmJyEhL/EUQWubIpssV.L.bWk354wJ1qCdnMbGW1DFwRiuo.nY0Me";
		expect(settingDao.getSetting(username, SettingsUserService.SETTING_TYPE_USER))
				.andReturn(hashedPassword);
		expect(settingDao.getSetting(username, SettingsUserService.SETTING_TYPE_ROLE))
				.andReturn("ROLE_USER");

		// WHEN
		replayAll();
		UserAuthenticationInfo info = service.authenticationInfo(username);

		// THEN
		assertThat("Info returned", info, is(notNullValue()));
		assertThat("Info alg matches", info.getHashAlgorithm(), is(equalTo("bcrypt")));
		assertThat("Info salt param populated", info.getHashParameters(),
				hasEntry("salt", "$2a$10$bmJyEhL/EUQWubIpssV.L."));
	}

}