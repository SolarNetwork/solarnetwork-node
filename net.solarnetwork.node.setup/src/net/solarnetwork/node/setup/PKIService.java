/* ==================================================================
 * PKIService.java - Dec 6, 2012 4:20:20 PM
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

package net.solarnetwork.node.setup;

import java.security.cert.X509Certificate;
import net.solarnetwork.support.CertificateException;

/**
 * API for managing the node's certificate infrastructure.
 * 
 * @author matt
 * @version 1.0
 */
public interface PKIService {

	/**
	 * Save the trusted CA certificate.
	 * 
	 * <p>
	 * The node maintains a root CA certificate for the SolarNet network it is
	 * associated with.
	 * </p>
	 * 
	 * @param cert
	 *        the certificate
	 * @throws CertificateException
	 *         if any certificate related error occurs
	 */
	void saveCACertificate(X509Certificate cert) throws net.solarnetwork.support.CertificateException;

	/**
	 * Get the configured CA certificate.
	 * 
	 * @return the CA certificate, or <em>null</em> if not available
	 * @throws CertificateException
	 *         if any certificate related error occurs
	 */
	X509Certificate getCACertificate() throws net.solarnetwork.support.CertificateException;

	/**
	 * Get the configured node certificate.
	 * 
	 * @return the node certificate, or <em>null</em> if not available
	 * @throws CertificateException
	 *         if any certificate related error occurs
	 */
	X509Certificate getNodeCertificate() throws CertificateException;

	/**
	 * Check if the node's certificate is valid.
	 * 
	 * <p>
	 * The certificate is considered valid if it is signed by the given
	 * authority and its chain can be verified and it has not expired.
	 * </p>
	 * 
	 * @param issuerDN
	 *        the expected issuer subject DN
	 * 
	 * @return boolean <em>true</em> if considered valid
	 * @throws CertificateException
	 *         if any certificate related error occurs
	 */
	boolean isNodeCertificateValid(String issuerDN) throws CertificateException;

	/**
	 * Generate a new public and private key pair, and a new self-signed
	 * certificate.
	 * 
	 * @param dn
	 *        the certificate subject DN
	 * @return the Certificate
	 * @throws CertificateException
	 *         if any certificate related error occurs
	 */
	X509Certificate generateNodeSelfSignedCertificate(String dn) throws CertificateException;

	/**
	 * Generate a PKCS#10 certificate signing request (CSR) for the node's
	 * certificate.
	 * 
	 * @return the PEM-encoded CSR
	 * @throws CertificateException
	 *         if any certificate related error occurs
	 */
	public String generateNodePKCS10CertificateRequestString() throws CertificateException;

	/**
	 * Save a signed node certificate.
	 * 
	 * <p>
	 * The issuer of the certificate must match the subject of the configured CA
	 * certificate, and the certificate's subject must match the existing node
	 * certificate's subject.
	 * </p>
	 * 
	 * @param certificateChain
	 *        the PKCS#7 signed certificate chain
	 * @throws CertificateException
	 *         if any certificate related error occurs
	 */
	public void saveNodeSignedCertificate(String certificateChain)
			throws net.solarnetwork.support.CertificateException;

}
