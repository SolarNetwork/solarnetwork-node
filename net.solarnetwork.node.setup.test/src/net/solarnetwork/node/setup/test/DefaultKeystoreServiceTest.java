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
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import net.solarnetwork.node.SetupSettings;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.setup.impl.DefaultKeystoreService;
import net.solarnetwork.pki.bc.BCCertificateService;
import net.solarnetwork.support.CertificateException;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX500NameUtil;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
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
		CA_CERT = generateNewCACert(CA_KEY_PAIR.getPublic(), TEST_CA_DN, null, CA_KEY_PAIR.getPrivate());

		CA_SUB_KEY_PAIR = keyGen.generateKeyPair();
		CA_SUB_CERT = generateNewCACert(CA_SUB_KEY_PAIR.getPublic(), TEST_CA_SUB_DN, CA_CERT,
				CA_KEY_PAIR.getPrivate());
	}

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
		log.debug("Saving CA Cert: {}", CA_CERT);
		replay(settingDao);
		service.saveCACertificate(CA_CERT);
		verify(settingDao);
	}

	@Test
	public void generateCSR() throws Exception {
		expect(settingDao.getSetting(SetupSettings.KEY_CONFIRMATION_CODE, SetupSettings.SETUP_TYPE_KEY))
				.andReturn(TEST_CONF_VALUE).times(5);
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
		expect(settingDao.getSetting(SetupSettings.KEY_CONFIRMATION_CODE, SetupSettings.SETUP_TYPE_KEY))
				.andReturn(TEST_CONF_VALUE).times(7);
		replay(settingDao);
		service.saveCACertificate(CA_CERT);
		service.generateNodeSelfSignedCertificate(TEST_DN);
		String csr = service.generateNodePKCS10CertificateRequestString();

		PemReader pemReader = new PemReader(new StringReader(csr));
		try {
			PemObject pem = pemReader.readPemObject();
			PKCS10CertificationRequest req = new PKCS10CertificationRequest(pem.getContent());
			X509Certificate signedCert = sign(req, CA_CERT, CA_KEY_PAIR.getPrivate());
			String signedPem = getPKCS7Encoding(new X509Certificate[] { signedCert });
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
		expect(settingDao.getSetting(SetupSettings.KEY_CONFIRMATION_CODE, SetupSettings.SETUP_TYPE_KEY))
				.andReturn(TEST_CONF_VALUE).times(7);
		replay(settingDao);
		service.saveCACertificate(CA_CERT);
		service.generateNodeSelfSignedCertificate(TEST_DN);
		String csr = service.generateNodePKCS10CertificateRequestString();

		PemReader pemReader = new PemReader(new StringReader(csr));
		try {
			PemObject pem = pemReader.readPemObject();
			PKCS10CertificationRequest req = new PKCS10CertificationRequest(pem.getContent());
			X509Certificate signedCert = sign(req, CA_SUB_CERT, CA_SUB_KEY_PAIR.getPrivate());
			String signedPem = getPKCS7Encoding(new X509Certificate[] { signedCert, CA_SUB_CERT, CA_CERT });
			service.saveNodeSignedCertificate(signedPem);

			log.debug("Saved signed node certificate:\n{}", signedPem);

			verify(settingDao);
			assertNotNull(csr);
		} finally {
			pemReader.close();
		}
	}

	private String getPKCS7Encoding(X509Certificate[] chain) throws IOException,
			java.security.cert.CertificateException {
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		List<X509Certificate> chainList = Arrays.asList(chain);
		log.debug("Cert chain:\n{}", chainList);
		CertPath path = cf.generateCertPath(chainList);
		StringWriter out = new StringWriter();
		PemWriter writer = new PemWriter(out);
		PemObject pemObj = new PemObject("CERTIFICATE CHAIN", path.getEncoded("PKCS7"));
		writer.writeObject(pemObj);
		writer.flush();
		writer.close();
		out.close();
		String result = out.toString();
		log.debug("Generated cert chain:\n{}", result);
		return result;

	}

	private static final AtomicLong serialCounter = new AtomicLong(0);

	private static BigInteger getNextSerialNumber() {
		return new BigInteger(Long.valueOf(serialCounter.incrementAndGet()).toString());
	}

	private static X509Certificate generateNewCACert(PublicKey publicKey, String subject,
			X509Certificate issuer, PrivateKey issuerKey) throws Exception {
		final X500Name issuerDn = (issuer == null ? new X500Name(subject) : JcaX500NameUtil
				.getSubject(issuer));
		final X500Name subjectDn = new X500Name(subject);
		final BigInteger serial = getNextSerialNumber();
		final Date notBefore = new Date();
		final Date notAfter = new Date(System.currentTimeMillis() + 1000L * 60L * 60L);
		JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(issuerDn, serial,
				notBefore, notAfter, subjectDn, publicKey);

		// add "CA" extension
		BasicConstraints basicConstraints;
		if ( issuer == null ) {
			basicConstraints = new BasicConstraints(true);
		} else {
			int issuerPathLength = issuer.getBasicConstraints();
			basicConstraints = new BasicConstraints(issuerPathLength - 1);
		}
		builder.addExtension(X509Extension.basicConstraints, true, basicConstraints);

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
		ContentSigner signer = signerBuilder.build(issuerKey);

		X509CertificateHolder holder = builder.build(signer);
		JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
		return converter.getCertificate(holder);
	}

	private static X509Certificate sign(PKCS10CertificationRequest csr, X509Certificate issuer,
			PrivateKey issuerPrivateKey) throws InvalidKeyException, NoSuchAlgorithmException,
			NoSuchProviderException, SignatureException, IOException, OperatorCreationException,
			CertificateException, java.security.cert.CertificateException {

		final BigInteger serial = getNextSerialNumber();
		final Date notBefore = new Date();
		final Date notAfter = new Date(System.currentTimeMillis() + 24L * 60L * 60L * 1000L);

		X500Name issuerName = JcaX500NameUtil.getSubject(issuer);
		X509v3CertificateBuilder myCertificateGenerator = new X509v3CertificateBuilder(issuerName,
				serial, notBefore, notAfter, csr.getSubject(), csr.getSubjectPublicKeyInfo());

		JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder("SHA256WithRSA");
		ContentSigner signer = signerBuilder.build(issuerPrivateKey);
		X509CertificateHolder holder = myCertificateGenerator.build(signer);

		JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
		return converter.getCertificate(holder);
	}

}
