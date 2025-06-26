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

import static java.util.Collections.singleton;
import static net.solarnetwork.test.EasyMockUtils.assertWith;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.dao.BasicBatchResult;
import net.solarnetwork.dao.BatchableDao.BatchCallback;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.service.IdentityService;
import net.solarnetwork.node.setup.UserAuthenticationInfo;
import net.solarnetwork.node.setup.security.SettingsUserService;
import net.solarnetwork.node.setup.security.UserEntity;
import net.solarnetwork.test.Assertion;

/**
 * Test cases for the {@link SettingsUserService}.
 *
 * @author matt
 * @version 1.1
 */
public class SettingsUserServiceTests {

	private SettingDao settingDao;
	private IdentityService identityService;
	private PasswordEncoder passwordEncoder;

	private SettingsUserService service;

	@Before
	public void setup() throws IOException {
		settingDao = EasyMock.createMock(SettingDao.class);
		identityService = EasyMock.createMock(IdentityService.class);
		passwordEncoder = new BCryptPasswordEncoder();
		service = new SettingsUserService(settingDao, identityService, passwordEncoder);
		Path usersDbPath = Paths.get(service.getUsersFilePath());
		if ( Files.exists(usersDbPath) ) {
			Files.deleteIfExists(usersDbPath);
		}
	}

	@After
	public void teardown() {
		EasyMock.verify(settingDao, identityService);
	}

	private void replayAll() {
		EasyMock.replay(settingDao, identityService);
	}

	private void assertUserEntityMatches(String msg, UserEntity actual, UserEntity expected) {
		assertThat(msg + " username", actual.getUsername(), is(equalTo(expected.getUsername())));
		assertThat(msg + " username", actual.getPassword(), is(equalTo(expected.getPassword())));
		assertThat(msg + " roles", actual.getRoles(), is(equalTo(expected.getRoles())));
	}

	@Test
	public void authInfo() throws IOException {
		// GIVEN
		final String username = "foo";
		final String hashedPassword = "$2a$10$bmJyEhL/EUQWubIpssV.L.bWk354wJ1qCdnMbGW1DFwRiuo.nY0Me";
		final long now = System.currentTimeMillis();
		final UserEntity user = new UserEntity(now, now + 1, username, hashedPassword,
				singleton("ROLE_USER"));
		Path usersDbPath = Paths.get(service.getUsersFilePath());
		if ( !Files.isDirectory(usersDbPath.getParent()) ) {
			Files.createDirectories(usersDbPath.getParent());
		}
		JsonUtils.newObjectMapper().writeValue(usersDbPath.toFile(), new UserEntity[] { user });

		// WHEN
		replayAll();
		UserAuthenticationInfo info = service.authenticationInfo(username);

		// THEN
		assertThat("Info returned", info, is(notNullValue()));
		assertThat("Info alg matches", info.getHashAlgorithm(), is(equalTo("bcrypt")));
		assertThat("Info salt param populated", info.getHashParameters(),
				hasEntry("salt", "$2a$10$bmJyEhL/EUQWubIpssV.L."));
	}

	@Test
	public void authInfo_settingUser() throws IOException {
		// GIVEN
		final String username = "foo";
		final String hashedPassword = "$2a$10$bmJyEhL/EUQWubIpssV.L.bWk354wJ1qCdnMbGW1DFwRiuo.nY0Me";
		expect(settingDao.getSetting(username, SettingsUserService.SETTING_TYPE_USER))
				.andReturn(hashedPassword).times(2);
		expect(settingDao.getSetting(username, SettingsUserService.SETTING_TYPE_ROLE))
				.andReturn("ROLE_USER");

		// migrate setting user to users db
		expect(settingDao.deleteSetting(username, SettingsUserService.SETTING_TYPE_USER))
				.andReturn(true);
		expect(settingDao.deleteSetting(username, SettingsUserService.SETTING_TYPE_ROLE))
				.andReturn(true);

		// WHEN
		replayAll();
		UserAuthenticationInfo info = service.authenticationInfo(username);

		// THEN
		assertThat("Info returned", info, is(notNullValue()));
		assertThat("Info alg matches", info.getHashAlgorithm(), is(equalTo("bcrypt")));
		assertThat("Info salt param populated", info.getHashParameters(),
				hasEntry("salt", "$2a$10$bmJyEhL/EUQWubIpssV.L."));

		Path usersDbPath = Paths.get(service.getUsersFilePath());
		assertThat("Setting user migrated to users file", Files.exists(usersDbPath), is(true));
		UserEntity[] users = JsonUtils.newObjectMapper().readValue(usersDbPath.toFile(),
				UserEntity[].class);
		assertThat("Users file has one user", users, arrayWithSize(1));
		assertUserEntityMatches("Migrated user", users[0],
				new UserEntity(0, 0, username, hashedPassword, singleton("ROLE_USER")));
	}

	@Test
	public void authInfo_legacyUser() throws IOException {
		// GIVEN
		final String username = "1";

		expect(settingDao.getSetting(username, SettingsUserService.SETTING_TYPE_USER)).andReturn(null)
				.times(2);

		BasicBatchResult queryResult = new BasicBatchResult(0);
		expect(settingDao.batchProcess(anyObject(), anyObject())).andReturn(queryResult);

		// assume no node ID available either, if no user available
		expect(identityService.getNodeId()).andReturn(1L);

		// WHEN
		replayAll();
		UserAuthenticationInfo info = service.authenticationInfo(username);

		// THEN
		assertThat("Info returned", info, is(notNullValue()));
		assertThat("Info alg matches", info.getHashAlgorithm(), is(equalTo("bcrypt")));
		assertThat("Info salt param populated", info.getHashParameters(),
				hasEntry(equalTo("salt"), notNullValue()));
		Path usersDbPath = Paths.get(service.getUsersFilePath());
		assertThat("Legacy user NOT migrated to users file", Files.exists(usersDbPath), is(false));
	}

	@Test
	public void authInfo_userNotFound_someUserExists() {
		// GIVEN
		final String username = "foo";
		expect(settingDao.getSetting(username, SettingsUserService.SETTING_TYPE_USER)).andReturn(null);

		BasicBatchResult queryResult = new BasicBatchResult(1);
		expect(settingDao.batchProcess(assertWith(new Assertion<BatchCallback<Setting>>() {

			@Override
			public void check(BatchCallback<Setting> cb) throws Throwable {
				Setting s = new Setting("otheruser", SettingsUserService.SETTING_TYPE_USER, "secret",
						null);
				cb.handle(s);
				s = new Setting("otheruser", SettingsUserService.SETTING_TYPE_ROLE,
						SettingsUserService.GRANTED_AUTH_USER, null);
				cb.handle(s);
			}
		}), anyObject())).andReturn(queryResult);

		// WHEN
		replayAll();
		UserAuthenticationInfo info = service.authenticationInfo(username);

		// THEN
		assertThat("Null info returned when no username found", info, is(nullValue()));
	}

	@Test
	public void authInfo_userNotFound_noUserExists_noNodeId() {
		// GIVEN
		final String username = "foo";
		expect(settingDao.getSetting(username, SettingsUserService.SETTING_TYPE_USER)).andReturn(null);

		BasicBatchResult queryResult = new BasicBatchResult(0);
		expect(settingDao.batchProcess(anyObject(), anyObject())).andReturn(queryResult);

		// assume no node ID available either, if no user available
		expect(identityService.getNodeId()).andReturn(null);

		// WHEN
		replayAll();
		UserAuthenticationInfo info = service.authenticationInfo(username);

		// THEN
		assertThat("Null info returned when no username found", info, is(nullValue()));
	}

	@Test
	public void authInfo_userNotFound_noUserExists_withNodeId() {
		// GIVEN
		final String username = "foo";
		expect(settingDao.getSetting(username, SettingsUserService.SETTING_TYPE_USER)).andReturn(null);

		BasicBatchResult queryResult = new BasicBatchResult(0);
		expect(settingDao.batchProcess(anyObject(), anyObject())).andReturn(queryResult);

		// assume no node ID available either, if no user available
		expect(identityService.getNodeId()).andReturn(1L);

		// WHEN
		replayAll();
		UserAuthenticationInfo info = service.authenticationInfo(username);

		// THEN
		assertThat("Null info returned when no username found", info, is(nullValue()));
	}

}
