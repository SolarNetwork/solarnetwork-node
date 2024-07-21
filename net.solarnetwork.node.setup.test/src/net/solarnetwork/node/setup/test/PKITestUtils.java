/* ==================================================================
 * PKITestHelper.java - 18/07/2016 3:52:59 PM
 *
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertPath;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
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
import org.bouncycastle.util.io.pem.PemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.service.CertificateException;

/**
 * Helper class for PKI related tasks within tests.
 *
 * @author matt
 * @version 1.1
 */
public class PKITestUtils {

	private static final Logger LOG = LoggerFactory.getLogger(PKITestUtils.class);

	public static String getPKCS7Encoding(X509Certificate[] chain)
			throws IOException, java.security.cert.CertificateException {
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		List<X509Certificate> chainList = Arrays.asList(chain);
		LOG.debug("Cert chain:\n{}", chainList);
		CertPath path = cf.generateCertPath(chainList);
		StringWriter out = new StringWriter();
		PemWriter writer = new PemWriter(out);
		PemObject pemObj = new PemObject("CERTIFICATE CHAIN", path.getEncoded("PKCS7"));
		writer.writeObject(pemObj);
		writer.flush();
		writer.close();
		out.close();
		String result = out.toString();
		LOG.debug("Generated cert chain:\n{}", result);
		return result;

	}

	private static final AtomicLong serialCounter = new AtomicLong(0);

	public static BigInteger getNextSerialNumber() {
		return new BigInteger(Long.valueOf(serialCounter.incrementAndGet()).toString());
	}

	public static X509Certificate generateNewCACert(PublicKey publicKey, String subject,
			X509Certificate issuer, PrivateKey issuerKey, String caDN) throws Exception {
		final X500Name issuerDn = (issuer == null ? new X500Name(subject)
				: JcaX500NameUtil.getSubject(issuer));
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
		builder.addExtension(Extension.basicConstraints, true, basicConstraints);

		// add subjectKeyIdentifier
		JcaX509ExtensionUtils utils = new JcaX509ExtensionUtils();
		SubjectKeyIdentifier ski = utils.createSubjectKeyIdentifier(publicKey);
		builder.addExtension(Extension.subjectKeyIdentifier, false, ski);

		// add authorityKeyIdentifier
		GeneralNames issuerName = new GeneralNames(new GeneralName(GeneralName.directoryName, caDN));
		AuthorityKeyIdentifier aki = utils.createAuthorityKeyIdentifier(publicKey);
		aki = new AuthorityKeyIdentifier(aki.getKeyIdentifier(), issuerName, serial);
		builder.addExtension(Extension.authorityKeyIdentifier, false, aki);

		// add keyUsage
		X509KeyUsage keyUsage = new X509KeyUsage(X509KeyUsage.cRLSign | X509KeyUsage.digitalSignature
				| X509KeyUsage.keyCertSign | X509KeyUsage.nonRepudiation);
		builder.addExtension(Extension.keyUsage, true, keyUsage);

		JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder("SHA256WithRSA");
		ContentSigner signer = signerBuilder.build(issuerKey);

		X509CertificateHolder holder = builder.build(signer);
		JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
		return converter.getCertificate(holder);
	}

	public static X509Certificate sign(PKCS10CertificationRequest csr, X509Certificate issuer,
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
