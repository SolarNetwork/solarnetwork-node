/* ==================================================================
 * DefaultSetupIdentityDaoTests.java - 3/11/2017 11:34:09 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.test;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.UUID;
import org.easymock.EasyMock;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.setup.SetupSettings;
import net.solarnetwork.node.setup.impl.DefaultSetupIdentityDao;
import net.solarnetwork.node.setup.impl.SetupIdentityInfo;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link DefaultSetupIdentityDao} class.
 * 
 * @author matt
 * @version 1.1
 */
public class DefaultSetupIdentityDaoTests {

	private static final Long TEST_NODE_ID = 123L;
	private static final String TEST_HOST_NAME = "solarnet.example.com";
	private static final Integer TEST_HOST_PORT = 12345;
	private static final String TEST_CONF_VALUE = "password";
	private static final String TEST_PW_VALUE = "test.password";

	private SettingDao settingDao;
	private ObjectMapper objectMapper;
	private File dataFile;
	private DefaultSetupIdentityDao dao;

	@Before
	public void setup() {
		settingDao = EasyMock.createMock(SettingDao.class);
		dataFile = new File(System.getProperty("java.io.tmpdir"),
				UUID.randomUUID().toString() + ".json");
		objectMapper = new ObjectMapper().setSerializationInclusion(Include.NON_NULL);
		dao = new DefaultSetupIdentityDao(objectMapper);
		dao.setSettingDao(new StaticOptionalService<SettingDao>(settingDao));
		dao.setDataFilePath(dataFile.getAbsolutePath());
	}

	private void replayAll() {
		EasyMock.replay(settingDao);
	}

	@After
	public void teardown() {
		EasyMock.verify(settingDao);
	}

	private void expectLoadLegacySettings(Long nodeId, String keyStorePassword) {
		expect(settingDao.getSetting(DefaultSetupIdentityDao.KEY_NODE_ID, SetupSettings.SETUP_TYPE_KEY))
				.andReturn(nodeId != null ? nodeId.toString() : null);
		expect(settingDao.getSetting(DefaultSetupIdentityDao.KEY_CONFIRMATION_CODE,
				SetupSettings.SETUP_TYPE_KEY)).andReturn(TEST_CONF_VALUE);
		expect(settingDao.getSetting(DefaultSetupIdentityDao.KEY_SOLARNETWORK_HOST_NAME,
				SetupSettings.SETUP_TYPE_KEY)).andReturn(TEST_HOST_NAME);
		expect(settingDao.getSetting(DefaultSetupIdentityDao.KEY_SOLARNETWORK_HOST_PORT,
				SetupSettings.SETUP_TYPE_KEY)).andReturn(TEST_HOST_PORT.toString());
		expect(settingDao.getSetting(DefaultSetupIdentityDao.KEY_SOLARNETWORK_FORCE_TLS,
				SetupSettings.SETUP_TYPE_KEY)).andReturn(Boolean.TRUE.toString());
		expect(settingDao.getSetting(DefaultSetupIdentityDao.KEY_KEY_STORE_PASSWORD,
				SetupSettings.SETUP_TYPE_KEY)).andReturn(keyStorePassword);
	}

	@Test
	public void getIdentityNoLegacyDaoAvailable() {
		dao.setSettingDao(null);
		replayAll();
		SetupIdentityInfo info = dao.getSetupIdentityInfo();
		assertThat(info, Matchers.sameInstance(SetupIdentityInfo.UNKNOWN_IDENTITY));
	}

	@Test
	public void getIdentityNothingAvailable() {
		expectLoadLegacySettings(null, null);
		replayAll();
		SetupIdentityInfo info = dao.getSetupIdentityInfo();
		assertThat(info, Matchers.sameInstance(SetupIdentityInfo.UNKNOWN_IDENTITY));
	}

	private void verifyInfo(String msgPrefix, SetupIdentityInfo info) {
		assertThat(msgPrefix + " node ID", info.getNodeId(), equalTo(TEST_NODE_ID));
		assertThat(msgPrefix + " conf code", info.getConfirmationCode(), equalTo(TEST_CONF_VALUE));
		assertThat(msgPrefix + " host name", info.getSolarNetHostName(), equalTo(TEST_HOST_NAME));
		assertThat(msgPrefix + " host port", info.getSolarNetHostPort(), equalTo(TEST_HOST_PORT));
		assertThat(msgPrefix + " host TLS", info.isSolarNetForceTls(), equalTo(true));
		assertThat(msgPrefix + " key store pw", info.getKeyStorePassword(), equalTo(TEST_PW_VALUE));
	}

	private void verifySavedDataFile() {
		assertThat("Data file exists", dataFile.exists(), equalTo(true));
		try {
			Set<PosixFilePermission> perms = Files.getPosixFilePermissions(dataFile.toPath());
			assertThat("File permissions", perms,
					containsInAnyOrder(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
			SetupIdentityInfo info = objectMapper.readValue(dataFile, SetupIdentityInfo.class);
			verifyInfo("Persisted", info);
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void getIdentityLegacyAvailable() {
		expectLoadLegacySettings(TEST_NODE_ID, TEST_PW_VALUE);
		replayAll();
		SetupIdentityInfo info = dao.getSetupIdentityInfo();
		verifyInfo("Returned", info);
		verifySavedDataFile();
	}

	@Test
	public void getIdentityLoadFromFile() throws IOException {
		SetupIdentityInfo info = new SetupIdentityInfo((long) (Math.random() * 10000), "foobar",
				"localhost", 54321, false, "test.pw");
		objectMapper.writeValue(dataFile, info);
		replayAll();
		SetupIdentityInfo result = dao.getSetupIdentityInfo();
		assertThat(result, equalTo(info));
	}

	@Test
	public void updateIdentity() throws IOException {
		SetupIdentityInfo info = new SetupIdentityInfo((long) (Math.random() * 10000), "foobar",
				"localhost", 54321, false, "test.pw");
		objectMapper.writeValue(dataFile, info);
		replayAll();
		dao.saveSetupIdentityInfo(new SetupIdentityInfo(TEST_NODE_ID, TEST_CONF_VALUE, TEST_HOST_NAME,
				TEST_HOST_PORT, true, TEST_PW_VALUE));
		SetupIdentityInfo result = dao.getSetupIdentityInfo();
		verifyInfo("Returned", result);
		verifySavedDataFile();
	}

}
