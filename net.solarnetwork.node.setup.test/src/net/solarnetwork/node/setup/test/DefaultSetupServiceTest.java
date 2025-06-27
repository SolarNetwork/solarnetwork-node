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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import javax.security.auth.x500.X500Principal;
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
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.BasicInstruction;
import net.solarnetwork.node.reactor.BasicInstructionStatus;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.setup.impl.DefaultKeystoreService;
import net.solarnetwork.node.setup.impl.DefaultSetupService;
import net.solarnetwork.node.setup.impl.SetupIdentityDao;
import net.solarnetwork.node.setup.impl.SetupIdentityInfo;
import net.solarnetwork.pki.bc.BCCertificateService;
import net.solarnetwork.service.CertificateException;
import net.solarnetwork.test.http.AbstractHttpServerTests;
import net.solarnetwork.test.http.TestHttpHandler;

/**
 * Test cases for the {@link DefaultSetupService} class.
 *
 * @author matt
 * @version 2.0
 */
public class DefaultSetupServiceTest extends AbstractHttpServerTests {

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

	@Override
	@Before
	public void setup() {
		super.setup();

		setupIdentityDao = EasyMock.createMock(SetupIdentityDao.class);
		certService = new BCCertificateService();
		keystoreService = new DefaultKeystoreService(setupIdentityDao, certService);
		keystoreService.setKeyStorePath(KEYSTORE_PATH);

		service = new DefaultSetupService(setupIdentityDao);
		service.setPkiService(keystoreService);
	}

	private void replayAll() {
		EasyMock.replay(setupIdentityDao);
	}

	@After
	public void cleanup() throws Exception {
		new File(KEYSTORE_PATH).delete();
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

		BasicInstruction instr = new BasicInstruction(123L,
				DefaultSetupService.INSTRUCTION_TOPIC_RENEW_CERTIFICATE, Instant.now(), "456",
				new BasicInstructionStatus(123L, InstructionState.Received, Instant.now()));
		for ( int i = 0; i < renewedSignedPem.length(); i += 256 ) {
			int end = i + (i + 256 < renewedSignedPem.length() ? 256 : renewedSignedPem.length() - i);
			instr.addParameter(DefaultSetupService.INSTRUCTION_PARAM_CERTIFICATE,
					renewedSignedPem.substring(i, end));
		}

		InstructionStatus status = service.processInstruction(instr);
		assertThat("Instruction state", status.getInstructionState(),
				equalTo(InstructionState.Completed));

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
		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(Request request, Response response, Callback callback)
					throws Exception {
				assertThat(request.getMethod(), is(equalTo("POST")));
				assertThat(request.getHttpURI().getPath(),
						is(equalTo("/solarin/api/v1/sec/cert/renew")));

				MultiMap<String> queryParams = UrlEncoded.decodeQuery(getRequestBody(request));

				String password = queryParams.getValue("password");
				assertThat(password, is(equalTo("foobar")));

				String keystoreData = queryParams.getValue("keystore");
				assertThat(password, is(notNullValue()));

				byte[] data = Base64.decodeBase64(keystoreData);
				KeyStore keyStore = KeyStore.getInstance("pkcs12");
				keyStore.load(new ByteArrayInputStream(data), password.toCharArray());
				Certificate cert = keyStore.getCertificate("node");
				assertThat(cert, is(notNullValue()));
				assertThat(cert, is(instanceOf(X509Certificate.class)));

				X509Certificate nodeCert = (X509Certificate) cert;
				assertThat(nodeCert.getSubjectX500Principal(), is(equalTo(new X500Principal(TEST_DN))));
				assertThat(nodeCert.getIssuerX500Principal(),
						is(equalTo(CA_CERT.getSubjectX500Principal())));

				respondWithJson(request, response, """
						{"success":true}""");

				return true;
			}

		};
		addHandler(handler);

		service.renewNetworkCertificate("foobar");
	}

	@Test
	public void readSolarInMqttUrl() {
		// given
		SetupIdentityInfo info = new SetupIdentityInfo(1L, TEST_CONF_VALUE, TEST_SOLARIN_HOST,
				getHttpServerPort(), false, TEST_PW_VALUE);
		expect(setupIdentityDao.getSetupIdentityInfo()).andReturn(info).atLeastOnce();

		TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(Request request, Response response, Callback callback)
					throws Exception {
				assertThat(request.getMethod(), is(equalTo("GET")));
				assertThat(request.getHttpURI().getPath(), is(equalTo("/solarin/identity.do")));

				byte[] xml = FileCopyUtils.copyToByteArray(
						DefaultSetupServiceTest.class.getResourceAsStream("identity-01.xml"));

				respondWithContent(request, response, "text/xml", xml);

				return true;
			}

		};
		addHandler(handler);

		replayAll();

		// when
		String url = service.getSolarInMqttUrl();

		// then
		assertThat("MQTT URL", url, equalTo("mqtts://queue.solarnetwork.net:8883"));
	}
}
