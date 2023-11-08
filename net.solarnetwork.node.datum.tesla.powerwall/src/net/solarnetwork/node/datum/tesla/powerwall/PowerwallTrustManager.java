/* ==================================================================
 * PowerwallTrustManager.java - 9/11/2023 6:34:28 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.tesla.powerwall;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.security.auth.x500.X500Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@code TrustManager} that works with Powerwall's
 * self-signed certificates.
 * 
 * @author matt
 * @version 1.0
 */
public class PowerwallTrustManager extends X509ExtendedTrustManager {

	private static final Logger log = LoggerFactory.getLogger(PowerwallTrustManager.class);

	private static final X509Certificate[] EMPTY_CERT_ARRAY = new X509Certificate[0];

	private final String hostName;

	/**
	 * Constructor.
	 * 
	 * @param hostName
	 *        the host name that is required
	 */
	public PowerwallTrustManager(String hostName) {
		super();
		this.hostName = requireNonNullArgument(hostName, "hostName").split(":", 2)[0].toLowerCase();
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return EMPTY_CERT_ARRAY;
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
			throws CertificateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
			throws CertificateException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		verifyChain(chain);
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
			throws CertificateException {
		verifyChain(chain);
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)
			throws CertificateException {
		verifyChain(chain);
	}

	private void verifyChain(X509Certificate[] chain) throws CertificateException {
		final Set<String> names = new LinkedHashSet<>(8);
		for ( X509Certificate c : chain ) {
			String dn = c.getSubjectX500Principal().getName(X500Principal.CANONICAL);
			log.debug("Validating server trust for certificate [{}]", dn);
			Collection<List<?>> alts = c.getSubjectAlternativeNames();
			if ( alts != null ) {
				for ( List<?> alt : alts ) {
					// list has Integer, X where X is String for DNSName type
					final int type = (Integer) alt.get(0);
					if ( type == 2 ) { // 2 === DNSName
						names.add(((String) alt.get(1)).toLowerCase());
					}
				}
			}
			break; // only take first certificate
		}
		if ( !names.contains(hostName) ) {
			throw new CertificateException("Host certificate does not include name [" + hostName + "]");
		}
	}

}
