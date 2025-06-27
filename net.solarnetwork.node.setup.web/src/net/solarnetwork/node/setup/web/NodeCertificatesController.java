/* ==================================================================
 * NodeCertificatesController.java - Dec 7, 2012 7:16:19 AM
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

package net.solarnetwork.node.setup.web;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import net.solarnetwork.node.service.PKIService;
import net.solarnetwork.node.setup.web.support.ServiceAwareController;

/**
 * Controller for node certificate management.
 *
 * @author matt
 * @version 2.3
 */
@ServiceAwareController
@RequestMapping("/a/certs")
public class NodeCertificatesController extends BaseSetupController {

	private final PKIService pkiService;

	/**
	 * Certificate information.
	 */
	public static final class CertificateInfo {

		private final X509Certificate cert;

		private CertificateInfo(X509Certificate cert) {
			super();
			this.cert = cert;
		}

		/**
		 * Get the issuer DN.
		 *
		 * @return the issuer DN
		 * @see java.security.cert.X509Certificate#getIssuerDN()
		 */
		@SuppressWarnings("deprecation")
		public String getIssuerDN() {
			return cert.getIssuerDN().getName();
		}

		/**
		 * Get the subject DN.
		 *
		 * @return the subject DN
		 * @see java.security.cert.X509Certificate#getSubjectDN()
		 */
		@SuppressWarnings("deprecation")
		public String getSubjectDN() {
			return cert.getSubjectDN().getName();
		}

		/**
		 * Get the valid start date.
		 *
		 * @return the start date
		 * @see java.security.cert.X509Certificate#getNotBefore()
		 */
		public Date getNotBefore() {
			return cert.getNotBefore();
		}

		/**
		 * Get the valid end date.
		 *
		 * @return the end date
		 * @see java.security.cert.X509Certificate#getNotAfter()
		 */
		public Date getNotAfter() {
			return cert.getNotAfter();
		}

	}

	/**
	 * Constructor.
	 *
	 * @param pkiService
	 *        the PKI service
	 */
	public NodeCertificatesController(PKIService pkiService) {
		super();
		this.pkiService = requireNonNullArgument(pkiService, "pkiService");
	}

	/**
	 * View the main certs page.
	 *
	 * @param model
	 *        the view model
	 * @return the view name
	 */
	@SuppressWarnings("deprecation")
	@RequestMapping
	public String home(Model model) {
		X509Certificate nodeCert = pkiService.getNodeCertificate();
		final Date now = new Date();
		final boolean expired = (nodeCert != null && now.after(nodeCert.getNotAfter()));
		final boolean valid = (nodeCert != null
				&& (!nodeCert.getIssuerDN().equals(nodeCert.getSubjectDN())
						&& !now.before(nodeCert.getNotBefore()) && !expired));
		model.addAttribute("nodeCert", new CertificateInfo(nodeCert));
		if ( nodeCert != null ) {
			model.addAttribute("nodeCertSerialNumber", "0x" + nodeCert.getSerialNumber().toString(16));
		}
		model.addAttribute("nodeCertExpired", expired);
		model.addAttribute("nodeCertValid", valid);
		return "certs";
	}

	/**
	 * Return a node's CSR based on its current certificate.
	 *
	 * @return a map with the PEM encoded certificate on key {@code csr}
	 */
	@RequestMapping(value = "/nodeCSR", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> nodeCSR() {
		String csr = pkiService.generateNodePKCS10CertificateRequestString();
		Map<String, Object> result = new HashMap<String, Object>(1);
		result.put("csr", csr);
		return result;
	}

	/**
	 * Return a node's current certificate.
	 *
	 * @param download
	 *        {@literal true} to add a {@literal Content-Disposition} response
	 *        header
	 * @param asChain
	 *        {@literal true} to download the entire chain
	 * @return a map with the PEM encoded certificate on key {@code cert} if
	 *         {@code download} is not {@literal true}, otherwise the content is
	 *         returned as a file attachment
	 */
	@RequestMapping(value = "/nodeCert", method = RequestMethod.GET)
	@ResponseBody
	public Object viewNodeCert(
			@RequestParam(value = "download", required = false) final Boolean download,
			@RequestParam(value = "chain", required = false) final Boolean asChain) {
		final String cert = (Boolean.TRUE.equals(asChain)
				? pkiService.generateNodePKCS7CertificateChainString()
				: pkiService.generateNodePKCS7CertificateString());

		if ( !Boolean.TRUE.equals(download) ) {
			Map<String, Object> result = new HashMap<String, Object>(1);
			result.put("cert", cert);
			return result;
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentLength(cert.length());
		headers.setContentType(MediaType.parseMediaType("application/x-pem-file"));
		headers.setLastModified(System.currentTimeMillis());
		headers.setCacheControl("no-cache");

		headers.set("Content-Disposition",
				"attachment; filename=solarnode-" + getIdentityService().getNodeId() + ".pem");

		return new ResponseEntity<String>(cert, headers, HttpStatus.OK);
	}

	/**
	 * Import a certificate reply (signed certificate chain).
	 *
	 * <p>
	 * Either a file or raw text can be imported.
	 * </p>
	 *
	 * @param file
	 *        the certificate file to import
	 * @param text
	 *        the certificate text to import
	 * @return the destination view
	 * @throws IOException
	 *         if an IO error occurs
	 */
	@RequestMapping(value = "/import", method = RequestMethod.POST)
	public String importCertificate(@RequestParam(value = "file", required = false) MultipartFile file,
			@RequestParam(value = "text", required = false) String text) throws IOException {
		String pem = text;
		if ( file != null && !file.isEmpty() ) {
			pem = FileCopyUtils.copyToString(new InputStreamReader(file.getInputStream(), "UTF-8"));
		}
		pkiService.saveNodeSignedCertificate(pem);
		return "redirect:/a/certs";
	}

	/**
	 * Renew the certificate.
	 *
	 * @param password
	 *        the password of the certificate keystore
	 * @return the result
	 */
	@RequestMapping(value = "/renew", method = RequestMethod.POST)
	@ResponseBody
	public Object renewCertificate(@RequestParam("password") String password) {
		Map<String, Object> result = new HashMap<String, Object>(1);
		boolean success = false;
		try {
			getSetupBiz().renewNetworkCertificate(password);
			success = true;
		} catch ( RuntimeException e ) {
			result.put("message", e.getMessage());
		}
		result.put("success", success);
		return result;
	}
}
