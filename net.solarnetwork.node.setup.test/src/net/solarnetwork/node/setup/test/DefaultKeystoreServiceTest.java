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
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import net.solarnetwork.node.SetupSettings;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.setup.impl.DefaultKeystoreService;
import net.solarnetwork.pki.bc.BCCertificateService;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
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
	private static final String TEST_CA_DN = "CN=SolarNetwork Unit Test, OU=Unit Test, O=SolarNetwork Domain";

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
		final boolean result = service.isNodeCertificateValid(TEST_DN);
		verify(settingDao);
		assertFalse("Node certificate should not be valid", result);
	}

	@Test
	public void checkForCertificateFileMissing() {
		expect(settingDao.getSetting(SetupSettings.KEY_CONFIRMATION_CODE, SetupSettings.SETUP_TYPE_KEY))
				.andReturn(TEST_CONF_VALUE);
		replay(settingDao);
		final boolean result = service.isNodeCertificateValid(TEST_DN);
		verify(settingDao);
		assertFalse("Node certificate should not be valid", result);
	}

	@Test
	public void generateNodeSelfSignedCertificate() {
		expect(settingDao.getSetting(SetupSettings.KEY_CONFIRMATION_CODE, SetupSettings.SETUP_TYPE_KEY))
				.andReturn(TEST_CONF_VALUE).times(2);
		replay(settingDao);
		final Certificate result = service.generateNodeSelfSignedCertificate(TEST_DN);
		log.debug("Got self-signed cert: {}", result);
		verify(settingDao);
		assertNotNull("Node certificate should exist", result);
	}

	@Test
	public void validateNodeSelfSignedCertificate() {
		expect(settingDao.getSetting(SetupSettings.KEY_CONFIRMATION_CODE, SetupSettings.SETUP_TYPE_KEY))
				.andReturn(TEST_CONF_VALUE).times(4);
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
		expect(settingDao.getSetting(SetupSettings.KEY_CONFIRMATION_CODE, SetupSettings.SETUP_TYPE_KEY))
				.andReturn(TEST_CONF_VALUE).times(2);
		X509Certificate caCert = generateNewCACert();
		log.debug("Generated CA Cert: {}", caCert);
		replay(settingDao);
		service.saveTrustedCertificate(caCert, service.getCaAlias());
		verify(settingDao);
	}

	private static final AtomicLong serialCounter = new AtomicLong(0);

	private X509Certificate generateNewCACert() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048, new SecureRandom());
		final KeyPair keypair = keyGen.generateKeyPair();
		final PublicKey publicKey = keypair.getPublic();
		final PrivateKey privateKey = keypair.getPrivate();

		X500Name caDn = new X500Name(TEST_CA_DN);
		final BigInteger serial = new BigInteger(Long.valueOf(serialCounter.incrementAndGet())
				.toString());
		final Date notBefore = new Date();
		final Date notAfter = new Date(System.currentTimeMillis() + 1000L * 60L * 60L);
		JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(caDn, serial, notBefore,
				notAfter, caDn, publicKey);

		// add "CA" extension
		builder.addExtension(X509Extension.basicConstraints, true, new BasicConstraints(true));

		// add subjectKeyIdentifier
		JcaX509ExtensionUtils utils = new JcaX509ExtensionUtils();
		SubjectKeyIdentifier ski = utils.createSubjectKeyIdentifier(publicKey);
		builder.addExtension(X509Extension.subjectKeyIdentifier, false, ski);

		// add authorityKeyIdentifier
		GeneralNames issuerName = new GeneralNames(
				new GeneralName(GeneralName.directoryName, TEST_CA_DN));
		AuthorityKeyIdentifier aki = utils.createAuthorityKeyIdentifier(publicKey);
		aki = new AuthorityKeyIdentifier(aki.getKeyIdentifier(), issuerName, serial);
		builder.addExtension(X509Extension.authorityKeyIdentifier, false, aki);

		// add keyUsage
		X509KeyUsage keyUsage = new X509KeyUsage(X509KeyUsage.cRLSign | X509KeyUsage.digitalSignature
				| X509KeyUsage.keyCertSign | X509KeyUsage.nonRepudiation);
		builder.addExtension(X509Extension.keyUsage, true, keyUsage);

		JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder("SHA256WithRSA");
		ContentSigner signer = signerBuilder.build(privateKey);

		X509CertificateHolder holder = builder.build(signer);
		JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
		return converter.getCertificate(holder);
	}
}
