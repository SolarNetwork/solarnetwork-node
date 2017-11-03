/* ==================================================================
 * DefaultSetupServiceTest.java - Dec 6, 2012 4:56:47 PM
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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import javax.security.auth.x500.X500Principal;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.jcajce.JcaX500NameUtil;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.support.BasicInstruction;
import net.solarnetwork.node.reactor.support.BasicInstructionStatus;
import net.solarnetwork.node.setup.impl.DefaultKeystoreService;
import net.solarnetwork.node.setup.impl.DefaultSetupService;
import net.solarnetwork.node.setup.impl.SetupIdentityDao;
import net.solarnetwork.node.setup.impl.SetupIdentityInfo;
import net.solarnetwork.pki.bc.BCCertificateService;
import net.solarnetwork.support.CertificateException;

/**
 * Test cases for the {@link DefaultSetupService} class.
 * 
 * @author matt
 * @version 1.1
 */
public class DefaultSetupServiceTest {

	private static final String TEST_CONF_VALUE = "password";
	private static final String TEST_PW_VALUE = "test.password";
	private static final String TEST_DN = "UID=1, OU=Development, O=SolarNetwork";
	private static final String TEST_CA_DN = "CN=Developer CA, OU=SolarNetwork Developer Network, O=SolarNetwork Domain";

	private static final String TEST_SOLARIN_HOST = "localhost";

	private static final String KEYSTORE_PATH = "conf/test.jks";

	private static KeyPair CA_KEY_PAIR;
	private static X509Certificate CA_CERT;

	private SetupIdentityDao setupIdentityDao;
	private BCCertificateService certService;
	private DefaultKeystoreService keystoreService;
	private Server httpServer;

	private DefaultSetupService service;

	private final Logger log = LoggerFactory.getLogger(getClass());

	@BeforeClass
	public static void setupClass() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048, new SecureRandom());
		CA_KEY_PAIR = keyGen.generateKeyPair();
		CA_CERT = PKITestUtils.generateNewCACert(CA_KEY_PAIR.getPublic(), TEST_CA_DN, null,
				CA_KEY_PAIR.getPrivate(), TEST_CA_DN);
	}

	@Before
	public void setup() throws Exception {
		setupIdentityDao = EasyMock.createMock(SetupIdentityDao.class);
		certService = new BCCertificateService();
		keystoreService = new DefaultKeystoreService(setupIdentityDao, certService);
		keystoreService.setKeyStorePath(KEYSTORE_PATH);

		httpServer = new Server(0);
		httpServer.start();

		service = new DefaultSetupService(setupIdentityDao);
		service.setPkiService(keystoreService);
	}

	private int getHttpServerPort() {
		return httpServer.getConnectors()[0].getLocalPort();
	}

	private void replayAll() {
		EasyMock.replay(setupIdentityDao);
	}

	@After
	public void cleanup() throws Exception {
		new File(KEYSTORE_PATH).delete();
		httpServer.stop();
		EasyMock.verify(setupIdentityDao);
	}

	private synchronized KeyStore loadKeyStore() throws Exception {
		File ksFile = new File(KEYSTORE_PATH);
		InputStream in = null;
		String passwd = TEST_PW_VALUE;
		try {
			if ( ksFile.isFile() ) {
				in = new BufferedInputStream(new FileInputStream(ksFile));
			}
			return loadKeyStore(KeyStore.getDefaultType(), in, passwd);
		} catch ( IOException e ) {
			throw new CertificateException("Error opening file " + KEYSTORE_PATH, e);
		}
	}

	private KeyStore loadKeyStore(String type, InputStream in, String password) throws Exception {
		if ( password == null ) {
			password = "";
		}
		KeyStore keyStore = null;
		try {
			keyStore = KeyStore.getInstance(type);
			keyStore.load(in, password.toCharArray());
			return keyStore;
		} finally {
			if ( in != null ) {
				try {
					in.close();
				} catch ( IOException e ) {
					// ignore this one
				}
			}
		}
	}

	@Test
	public void handleRenewCertificateInstruction() throws Exception {
		SetupIdentityInfo info = new SetupIdentityInfo(1L, TEST_CONF_VALUE, "localhost", 80, false,
				TEST_PW_VALUE);
		expect(setupIdentityDao.getSetupIdentityInfo()).andReturn(info).atLeastOnce();
		replayAll();
		keystoreService.saveCACertificate(CA_CERT);
		keystoreService.generateNodeSelfSignedCertificate(TEST_DN);
		String csr = keystoreService.generateNodePKCS10CertificateRequestString();

		X509Certificate originalCert;

		PemReader pemReader = new PemReader(new StringReader(csr));
		try {
			PemObject pem = pemReader.readPemObject();
			PKCS10CertificationRequest req = new PKCS10CertificationRequest(pem.getContent());
			originalCert = PKITestUtils.sign(req, CA_CERT, CA_KEY_PAIR.getPrivate());
			String signedPem = PKITestUtils.getPKCS7Encoding(new X509Certificate[] { originalCert });
			keystoreService.saveNodeSignedCertificate(signedPem);

			log.debug("Saved signed node certificate {}:\n{}", originalCert.getSerialNumber(),
					signedPem);

			assertThat("Generated CSR", csr, notNullValue());
		} finally {
			pemReader.close();
		}

		// now let's renew!
		KeyStore keyStore = loadKeyStore();
		PrivateKey nodeKey = (PrivateKey) keyStore.getKey("node", TEST_PW_VALUE.toCharArray());
		JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder("SHA256WithRSA");
		ContentSigner signer = signerBuilder.build(nodeKey);
		PKCS10CertificationRequestBuilder builder = new PKCS10CertificationRequestBuilder(
				JcaX500NameUtil.getSubject(originalCert),
				SubjectPublicKeyInfo.getInstance(originalCert.getPublicKey().getEncoded()));
		X509Certificate renewedCert = PKITestUtils.sign(builder.build(signer), CA_CERT,
				CA_KEY_PAIR.getPrivate());
		String renewedSignedPem = PKITestUtils.getPKCS7Encoding(new X509Certificate[] { renewedCert });

		BasicInstruction instr = new BasicInstruction(
				DefaultSetupService.INSTRUCTION_TOPIC_RENEW_CERTIFICATE, new Date(), "123", "456",
				new BasicInstructionStatus(456L, InstructionState.Received, new Date()));
		for ( int i = 0; i < renewedSignedPem.length(); i += 256 ) {
			int end = i + (i + 256 < renewedSignedPem.length() ? 256 : renewedSignedPem.length() - i);
			instr.addParameter(DefaultSetupService.INSTRUCTION_PARAM_CERTIFICATE,
					renewedSignedPem.substring(i, end));
		}

		InstructionState state = service.processInstruction(instr);
		assertThat("Instruction state", state, equalTo(InstructionState.Completed));

		X509Certificate nodeCert = keystoreService.getNodeCertificate();
		assertThat("Node cert is now renewed cert", nodeCert, equalTo(renewedCert));
	}

	@Test
	public void renewNetworkCertificate() throws Exception {
		SetupIdentityInfo info = new SetupIdentityInfo(1L, TEST_CONF_VALUE, TEST_SOLARIN_HOST,
				getHttpServerPort(), false, TEST_PW_VALUE);
		expect(setupIdentityDao.getSetupIdentityInfo()).andReturn(info).atLeastOnce();
		replayAll();
		keystoreService.saveCACertificate(CA_CERT);
		keystoreService.generateNodeSelfSignedCertificate(TEST_DN);
		String csr = keystoreService.generateNodePKCS10CertificateRequestString();

		X509Certificate originalCert;

		PemReader pemReader = new PemReader(new StringReader(csr));
		try {
			PemObject pem = pemReader.readPemObject();
			PKCS10CertificationRequest req = new PKCS10CertificationRequest(pem.getContent());
			originalCert = PKITestUtils.sign(req, CA_CERT, CA_KEY_PAIR.getPrivate());
			String signedPem = PKITestUtils.getPKCS7Encoding(new X509Certificate[] { originalCert });
			keystoreService.saveNodeSignedCertificate(signedPem);

			log.debug("Saved signed node certificate {}:\n{}", originalCert.getSerialNumber(),
					signedPem);

			assertThat("Generated CSR", csr, notNullValue());
		} finally {
			pemReader.close();
		}

		// now let's renew!
		AbstractTestHandler handler = new AbstractTestHandler() {

			@Override
			protected boolean handleInternal(String target, HttpServletRequest request,
					HttpServletResponse response, int dispatch) throws Exception {
				assertEquals("POST", request.getMethod());
				assertEquals("/solarin/api/v1/sec/cert/renew", target);
				String password = request.getParameter("password");
				assertEquals("foobar", password);

				String keystoreData = request.getParameter("keystore");
				assertNotNull(keystoreData);
				byte[] data = Base64.decodeBase64(keystoreData);
				KeyStore keyStore = KeyStore.getInstance("pkcs12");
				keyStore.load(new ByteArrayInputStream(data), password.toCharArray());
				Certificate cert = keyStore.getCertificate("node");
				assertNotNull(cert);
				assertTrue(cert instanceof X509Certificate);
				X509Certificate nodeCert = (X509Certificate) cert;
				assertEquals(new X500Principal(TEST_DN), nodeCert.getSubjectX500Principal());
				assertEquals(CA_CERT.getSubjectX500Principal(), nodeCert.getIssuerX500Principal());

				response.setContentType("application/json");
				PrintWriter out = response.getWriter();
				out.write("{\"success\":true}");
				out.flush();
				response.flushBuffer();
				return true;
			}

		};
		httpServer.addHandler(handler);

		service.renewNetworkCertificate("foobar");
	}
}
