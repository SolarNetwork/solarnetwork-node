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

import static net.solarnetwork.node.setup.impl.DefaultKeystoreService.KEY_PASSWORD;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.StringReader;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.SetupSettings;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.setup.impl.DefaultKeystoreService;
import net.solarnetwork.pki.bc.BCCertificateService;

/**
 * Test cases for the {@link DefaultKeystoreService} class.
 * 
 * @author matt
 * @version 1.1
 */
public class DefaultKeystoreServiceTest {

	private static final String TEST_CONF_VALUE = "password";
	private static final String TEST_DN = "UID=1, OU=Development, O=SolarNetwork";
	private static final String TEST_CA_DN = "CN=Developer CA, OU=SolarNetwork Developer Network, O=SolarNetwork Domain";
	private static final String TEST_CA_SUB_DN = "CN=Unit Test CA, OU=SolarNetwork Developer Network, O=SolarNetwork Domain";

	private static KeyPair CA_KEY_PAIR;
	private static X509Certificate CA_CERT;

	private static KeyPair CA_SUB_KEY_PAIR;
	private static X509Certificate CA_SUB_CERT;

	private SettingDao settingDao;
	private BCCertificateService certService;

	private DefaultKeystoreService service;

	private final Logger log = LoggerFactory.getLogger(getClass());

	@BeforeClass
	public static void setupClass() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048, new SecureRandom());
		CA_KEY_PAIR = keyGen.generateKeyPair();
		CA_CERT = PKITestUtils.generateNewCACert(CA_KEY_PAIR.getPublic(), TEST_CA_DN, null,
				CA_KEY_PAIR.getPrivate(), TEST_CA_DN);

		CA_SUB_KEY_PAIR = keyGen.generateKeyPair();
		CA_SUB_CERT = PKITestUtils.generateNewCACert(CA_SUB_KEY_PAIR.getPublic(), TEST_CA_SUB_DN,
				CA_CERT, CA_KEY_PAIR.getPrivate(), TEST_CA_DN);
	}

	@Before
	public void setup() {
		settingDao = EasyMock.createMock(SettingDao.class);
		certService = new BCCertificateService();
		service = new DefaultKeystoreService();
		service.setSettingDao(settingDao);
		service.setCertificateService(certService);
		service.setKeyStorePath("conf/test.jks");
	}

	@After
	public void cleanup() {
		new File("conf/test.jks").delete();
	}

	@Test
	public void checkForCertificateNoConfKey() {
		expect(settingDao.getSetting(KEY_PASSWORD, SetupSettings.SETUP_TYPE_KEY)).andReturn(null);
		settingDao.storeSetting(eq(KEY_PASSWORD), eq(SetupSettings.SETUP_TYPE_KEY),
				anyObject(String.class));
		replay(settingDao);
		final boolean result = service.isNodeCertificateValid(TEST_DN);
		verify(settingDao);
		assertFalse("Node certificate should not be valid", result);
	}

	@Test
	public void checkForCertificateFileMissing() {
		expect(settingDao.getSetting(KEY_PASSWORD, SetupSettings.SETUP_TYPE_KEY))
				.andReturn(TEST_CONF_VALUE);
		replay(settingDao);
		final boolean result = service.isNodeCertificateValid(TEST_DN);
		verify(settingDao);
		assertFalse("Node certificate should not be valid", result);
	}

	@Test
	public void generateNodeSelfSignedCertificate() {
		expect(settingDao.getSetting(KEY_PASSWORD, SetupSettings.SETUP_TYPE_KEY))
				.andReturn(TEST_CONF_VALUE).times(3);
		replay(settingDao);
		final Certificate result = service.generateNodeSelfSignedCertificate(TEST_DN);
		log.debug("Got self-signed cert: {}", result);
		verify(settingDao);
		assertNotNull("Node certificate should exist", result);
	}

	@Test
	public void generateNodeSelfSignedCertificateExistingKeyStoreBadPassword() {
		generateNodeSelfSignedCertificate();
		EasyMock.reset(settingDao);
		expect(settingDao.getSetting(KEY_PASSWORD, SetupSettings.SETUP_TYPE_KEY))
				.andReturn("this.is.not.the.password");
		expect(settingDao.deleteSetting(KEY_PASSWORD, SetupSettings.SETUP_TYPE_KEY)).andReturn(true);
		expect(settingDao.getSetting(KEY_PASSWORD, SetupSettings.SETUP_TYPE_KEY))
				.andReturn(TEST_CONF_VALUE).times(3);
		replay(settingDao);
		final Certificate result = service.generateNodeSelfSignedCertificate(TEST_DN);
		log.debug("Got self-signed cert: {}", result);
		verify(settingDao);
		assertNotNull("Node certificate should exist", result);
	}

	@Test
	public void validateNodeSelfSignedCertificate() {
		expect(settingDao.getSetting(KEY_PASSWORD, SetupSettings.SETUP_TYPE_KEY))
				.andReturn(TEST_CONF_VALUE).times(5);
		replay(settingDao);
		final Certificate result = service.generateNodeSelfSignedCertificate(TEST_DN);
		final boolean valid = service.isNodeCertificateValid(TEST_DN);
		final boolean certified = service.isNodeCertificateValid(TEST_CA_DN);
		verify(settingDao);
		assertNotNull("Node certificate should exist", result);
		assertTrue("Node certificate is self-signed and should be considered valid", valid);
		assertFalse("Node certificate is self-signed and should not be considered certified", certified);
	}

	@Test
	public void saveTrustedCert() throws Exception {
		expect(settingDao.getSetting(KEY_PASSWORD, SetupSettings.SETUP_TYPE_KEY))
				.andReturn(TEST_CONF_VALUE).times(2);
		log.debug("Saving CA Cert: {}", CA_CERT);
		replay(settingDao);
		service.saveCACertificate(CA_CERT);
		verify(settingDao);
	}

	@Test
	public void generateCSR() throws Exception {
		expect(settingDao.getSetting(KEY_PASSWORD, SetupSettings.SETUP_TYPE_KEY))
				.andReturn(TEST_CONF_VALUE).times(7);
		replay(settingDao);
		service.saveCACertificate(CA_CERT);
		service.generateNodeSelfSignedCertificate(TEST_DN);
		String csr = service.generateNodePKCS10CertificateRequestString();
		verify(settingDao);
		assertNotNull(csr);
		log.debug("Got CSR:\n{}", csr);
	}

	@Test
	public void saveCASignedCert() throws Exception {
		expect(settingDao.getSetting(KEY_PASSWORD, SetupSettings.SETUP_TYPE_KEY))
				.andReturn(TEST_CONF_VALUE).times(11);
		replay(settingDao);
		service.saveCACertificate(CA_CERT);
		service.generateNodeSelfSignedCertificate(TEST_DN);
		String csr = service.generateNodePKCS10CertificateRequestString();

		PemReader pemReader = new PemReader(new StringReader(csr));
		try {
			PemObject pem = pemReader.readPemObject();
			PKCS10CertificationRequest req = new PKCS10CertificationRequest(pem.getContent());
			X509Certificate signedCert = PKITestUtils.sign(req, CA_CERT, CA_KEY_PAIR.getPrivate());
			String signedPem = PKITestUtils.getPKCS7Encoding(new X509Certificate[] { signedCert });
			service.saveNodeSignedCertificate(signedPem);

			log.debug("Saved signed node certificate:\n{}", signedPem);

			verify(settingDao);
			assertNotNull(csr);
		} finally {
			pemReader.close();
		}
	}

	@Test
	public void saveCASubSignedCert() throws Exception {
		expect(settingDao.getSetting(KEY_PASSWORD, SetupSettings.SETUP_TYPE_KEY))
				.andReturn(TEST_CONF_VALUE).times(11);
		replay(settingDao);
		service.saveCACertificate(CA_CERT);
		service.generateNodeSelfSignedCertificate(TEST_DN);
		String csr = service.generateNodePKCS10CertificateRequestString();

		PemReader pemReader = new PemReader(new StringReader(csr));
		try {
			PemObject pem = pemReader.readPemObject();
			PKCS10CertificationRequest req = new PKCS10CertificationRequest(pem.getContent());
			X509Certificate signedCert = PKITestUtils.sign(req, CA_SUB_CERT,
					CA_SUB_KEY_PAIR.getPrivate());
			String signedPem = PKITestUtils
					.getPKCS7Encoding(new X509Certificate[] { signedCert, CA_SUB_CERT, CA_CERT });
			service.saveNodeSignedCertificate(signedPem);

			log.debug("Saved signed node certificate:\n{}", signedPem);

			verify(settingDao);
			assertNotNull(csr);
		} finally {
			pemReader.close();
		}
	}

}
