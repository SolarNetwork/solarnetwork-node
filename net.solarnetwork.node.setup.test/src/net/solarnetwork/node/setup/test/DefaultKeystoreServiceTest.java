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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringReader;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.setup.impl.DefaultKeystoreService;
import net.solarnetwork.node.setup.impl.SetupIdentityDao;
import net.solarnetwork.node.setup.impl.SetupIdentityInfo;
import net.solarnetwork.pki.bc.BCCertificateService;

/**
 * Test cases for the {@link DefaultKeystoreService} class.
 * 
 * @author matt
 * @version 1.3
 */
public class DefaultKeystoreServiceTest {

	private static final String TEST_CONF_VALUE = "password";
	private static final String TEST_PW_VALUE = "test.password";
	private static final String TEST_DN = "UID=1, OU=Development, O=SolarNetwork";
	private static final String TEST_CA_DN = "CN=Developer CA, OU=SolarNetwork Developer Network, O=SolarNetwork Domain";
	private static final String TEST_CA_SUB_DN = "CN=Unit Test CA, OU=SolarNetwork Developer Network, O=SolarNetwork Domain";

	private static KeyPair CA_KEY_PAIR;
	private static X509Certificate CA_CERT;

	private static KeyPair CA_SUB_KEY_PAIR;
	private static X509Certificate CA_SUB_CERT;

	private SetupIdentityDao setupIdentityDao;
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
		setupIdentityDao = EasyMock.createMock(SetupIdentityDao.class);
		certService = new BCCertificateService();
		service = new DefaultKeystoreService(setupIdentityDao, certService);
		service.setKeyStorePath("conf/test.jks");
	}

	private void replayAll() {
		replay(setupIdentityDao);
	}

	@After
	public void cleanup() {
		new File("conf/test.jks").delete();
		verify(setupIdentityDao);
	}

	@Test
	public void checkForCertificateNoConfKey() {
		expect(setupIdentityDao.getSetupIdentityInfo()).andReturn(SetupIdentityInfo.UNKNOWN_IDENTITY);
		Capture<SetupIdentityInfo> infoCapture = Capture.newInstance();
		setupIdentityDao.saveSetupIdentityInfo(capture(infoCapture));
		replayAll();
		final boolean result = service.isNodeCertificateValid(TEST_DN);
		assertThat("Node certificate should not be valid", result, equalTo(false));
		assertThat("Identity info updated", infoCapture.hasCaptured(), equalTo(true));
		assertThat("Identity password set", infoCapture.getValue().getKeyStorePassword(),
				notNullValue());
	}

	@Test
	public void checkForCertificateFileMissing() {
		SetupIdentityInfo info = new SetupIdentityInfo(1L, TEST_CONF_VALUE, "localhost", 80, false,
				TEST_PW_VALUE);
		expect(setupIdentityDao.getSetupIdentityInfo()).andReturn(info);
		replayAll();
		final boolean result = service.isNodeCertificateValid(TEST_DN);
		assertThat("Node certificate should not be valid", result, equalTo(false));
	}

	@Test
	public void generateNodeSelfSignedCertificate() {
		// try to load existing identity; find none
		expect(setupIdentityDao.getSetupIdentityInfo()).andReturn(SetupIdentityInfo.UNKNOWN_IDENTITY);

		// generate new password for key store
		final Capture<SetupIdentityInfo> infoCapture = Capture.newInstance();
		setupIdentityDao.saveSetupIdentityInfo(capture(infoCapture));

		// load the identity again, this time returning the captured value
		expect(setupIdentityDao.getSetupIdentityInfo()).andAnswer(new IAnswer<SetupIdentityInfo>() {

			@Override
			public SetupIdentityInfo answer() throws Throwable {
				return infoCapture.getValue();
			}
		}).times(2);

		replayAll();

		final Certificate result = service.generateNodeSelfSignedCertificate(TEST_DN);
		log.debug("Got self-signed cert: {}", result);
		assertThat("Node certificate should exist", result, notNullValue());
		assertThat("Identity info updated", infoCapture.hasCaptured(), equalTo(true));
		assertThat("Identity password set", infoCapture.getValue().getKeyStorePassword(),
				notNullValue());
	}

	@Test
	public void generateNodeSelfSignedCertificateExistingKeyStoreBadPassword() {
		generateNodeSelfSignedCertificate();
		reset(setupIdentityDao);

		// load identity for old node
		SetupIdentityInfo info = new SetupIdentityInfo(1L, TEST_CONF_VALUE, "localhost", 80, false,
				"this.is.not.the.password");
		expect(setupIdentityDao.getSetupIdentityInfo()).andReturn(info).times(2);

		// generate new password for key store
		final Capture<SetupIdentityInfo> infoCapture = Capture.newInstance(CaptureType.LAST);
		setupIdentityDao.saveSetupIdentityInfo(capture(infoCapture));
		EasyMock.expectLastCall().times(2);

		// load the identity again, this time returning the captured value
		expect(setupIdentityDao.getSetupIdentityInfo()).andAnswer(new IAnswer<SetupIdentityInfo>() {

			@Override
			public SetupIdentityInfo answer() throws Throwable {
				return infoCapture.getValue();
			}
		}).atLeastOnce();

		replay(setupIdentityDao);
		final Certificate result = service.generateNodeSelfSignedCertificate(TEST_DN);
		log.debug("Got self-signed cert: {}", result);
		assertThat("Node certificate should exist", result, notNullValue());
		assertThat("Identity info updated", infoCapture.hasCaptured(), equalTo(true));
		assertThat("Identity password set", infoCapture.getValue().getKeyStorePassword(),
				notNullValue());
	}

	@Test
	public void validateNodeSelfSignedCertificate() {
		SetupIdentityInfo info = new SetupIdentityInfo(1L, TEST_CONF_VALUE, "localhost", 80, false,
				TEST_PW_VALUE);
		expect(setupIdentityDao.getSetupIdentityInfo()).andReturn(info).atLeastOnce();
		replayAll();
		final Certificate result = service.generateNodeSelfSignedCertificate(TEST_DN);
		final boolean valid = service.isNodeCertificateValid(TEST_DN);
		final boolean certified = service.isNodeCertificateValid(TEST_CA_DN);
		assertThat("Node certificate should exist", result, notNullValue());
		assertThat("Node certificate is self-signed and should be considered valid", valid,
				equalTo(true));
		assertThat("Node certificate is self-signed and should not be considered certified", certified,
				equalTo(false));
	}

	@Test
	public void saveTrustedCert() throws Exception {
		SetupIdentityInfo info = new SetupIdentityInfo(1L, TEST_CONF_VALUE, "localhost", 80, false,
				TEST_PW_VALUE);
		expect(setupIdentityDao.getSetupIdentityInfo()).andReturn(info).atLeastOnce();
		log.debug("Saving CA Cert: {}", CA_CERT);
		replayAll();
		service.saveCACertificate(CA_CERT);
	}

	@Test
	public void generateCSR() throws Exception {
		SetupIdentityInfo info = new SetupIdentityInfo(1L, TEST_CONF_VALUE, "localhost", 80, false,
				TEST_PW_VALUE);
		expect(setupIdentityDao.getSetupIdentityInfo()).andReturn(info).atLeastOnce();
		replayAll();
		service.generateNodeSelfSignedCertificate(TEST_DN);
		String csr = service.generateNodePKCS10CertificateRequestString();
		assertNotNull(csr);
		log.debug("Got CSR:\n{}", csr);
	}

	@Test
	public void saveCASignedCert() throws Exception {
		SetupIdentityInfo info = new SetupIdentityInfo(1L, TEST_CONF_VALUE, "localhost", 80, false,
				TEST_PW_VALUE);
		expect(setupIdentityDao.getSetupIdentityInfo()).andReturn(info).atLeastOnce();
		replayAll();
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

			assertNotNull(csr);
		} finally {
			pemReader.close();
		}
	}

	@Test
	public void saveCASubSignedCert() throws Exception {
		SetupIdentityInfo info = new SetupIdentityInfo(1L, TEST_CONF_VALUE, "localhost", 80, false,
				TEST_PW_VALUE);
		expect(setupIdentityDao.getSetupIdentityInfo()).andReturn(info).atLeastOnce();
		replayAll();
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
			assertNotNull(csr);
		} finally {
			pemReader.close();
		}
	}

	@Test
	public void generatePKCS12Keystore() throws Exception {
		saveCASignedCert();
		reset(setupIdentityDao);

		SetupIdentityInfo info = new SetupIdentityInfo(1L, TEST_CONF_VALUE, "localhost", 80, false,
				TEST_PW_VALUE);
		expect(setupIdentityDao.getSetupIdentityInfo()).andReturn(info).atLeastOnce();

		replay(setupIdentityDao);

		String keystoreData = service.generatePKCS12KeystoreString("foobar");

		assertNotNull(keystoreData);

		byte[] data = Base64.decodeBase64(keystoreData);

		KeyStore keyStore = KeyStore.getInstance("pkcs12");
		keyStore.load(new ByteArrayInputStream(data), "foobar".toCharArray());
		Certificate cert = keyStore.getCertificate("node");
		assertNotNull(cert);
		assertTrue(cert instanceof X509Certificate);
		X509Certificate nodeCert = (X509Certificate) cert;
		assertEquals(new X500Principal(TEST_DN), nodeCert.getSubjectX500Principal());
		assertEquals(CA_CERT.getSubjectX500Principal(), nodeCert.getIssuerX500Principal());
	}

}
