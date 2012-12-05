/* ==================================================================
 * DefaultKeystoreServiceTest.java - Dec 5, 2012 8:28:59 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import java.io.File;
import java.security.cert.Certificate;
import net.solarnetwork.node.SetupSettings;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.setup.impl.DefaultKeystoreService;
import net.solarnetwork.pki.bc.BCCertificateService;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test cases for the {@link DefaultKeystoreService} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DefaultKeystoreServiceTest {

	private static final String TEST_CONF_VALUE = "password";
	private static final String TEST_DN = "UID=1, O=SolarNetwork";

	private SettingDao settingDao;
	private BCCertificateService certService;

	private DefaultKeystoreService service;

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Before
	public void setup() {
		settingDao = EasyMock.createMock(SettingDao.class);
		certService = new BCCertificateService();
		service = new DefaultKeystoreService();
		service.setSettingDao(settingDao);
		service.setCertificateService(certService);
	}

	@After
	public void cleanup() {
		new File("conf/pki/node.jks").delete();
	}

	@Test
	public void checkForCertificateNoConfKey() {
		expect(settingDao.getSetting(SetupSettings.KEY_CONFIRMATION_CODE, SetupSettings.SETUP_TYPE_KEY))
				.andReturn(null);
		replay(settingDao);
		final boolean result = service.isNodeCertificateValid();
		verify(settingDao);
		assertFalse("Node certificate should not be valid", result);
	}

	@Test
	public void checkForCertificateFileMissing() {
		expect(settingDao.getSetting(SetupSettings.KEY_CONFIRMATION_CODE, SetupSettings.SETUP_TYPE_KEY))
				.andReturn(TEST_CONF_VALUE);
		replay(settingDao);
		final boolean result = service.isNodeCertificateValid();
		verify(settingDao);
		assertFalse("Node certificate should not be valid", result);
	}

	@Test
	public void generateNodeSelfSignedCertificate() {
		expect(settingDao.getSetting(SetupSettings.KEY_CONFIRMATION_CODE, SetupSettings.SETUP_TYPE_KEY))
				.andReturn(TEST_CONF_VALUE).times(2);
		replay(settingDao);
		final Certificate result = service.generateSelfSignedCertificate(TEST_DN);
		log.debug("Got self-signed cert: {}", result);
		verify(settingDao);
		assertNotNull("Node certificate should exist", result);
	}

}
