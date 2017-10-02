/* ==================================================================
 * SetupService.java - Jun 1, 2010 2:17:28 PM
 * 
 * Copyright 2007-2010 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node.setup;

import net.solarnetwork.domain.NetworkAssociation;
import net.solarnetwork.domain.NetworkAssociationDetails;
import net.solarnetwork.domain.NetworkCertificate;
import net.solarnetwork.domain.NetworkIdentity;
import net.solarnetwork.node.domain.NodeAppConfiguration;

/**
 * API for node setup support.
 * 
 * @author matt
 * @version 1.3
 */
public interface SetupService {

	/** Topic for when a network association has been accepted. */
	public static final String TOPIC_NETWORK_ASSOCIATION_ACCEPTED = "net/solarnetwork/node/setup/NETWORK_ASSOCIATION_ACCEPTED";

	/**
	 * Decode a SolarNet verification code to determine the service that the
	 * node should register itself with.
	 * 
	 * @param verificationCode
	 *        The verification code supplied by SolarNet to decode.
	 * @return details for the given SolarNet host
	 * @throws InvalidVerificationCodeException
	 *         thrown if an error is encountered decoding the verification code.
	 */
	NetworkAssociationDetails decodeVerificationCode(String verificationCode)
			throws InvalidVerificationCodeException;

	/**
	 * Use the {@link NetworkIdentity} settings in the supplied
	 * <code>details</code> to retrieve the server identity, terms of service,
	 * security phrase, etc.
	 * 
	 * @param details
	 *        Contains the host details to determine where we retrieve the
	 *        association from
	 * @return the NetworkAssociation
	 */
	NetworkAssociation retrieveNetworkAssociation(NetworkAssociationDetails details);

	/**
	 * Associate this node with a SolarNet central service, using details
	 * previously obtained via {@link #decodeVerificationCode(String)}.
	 * 
	 * @param details
	 *        the host details to associate with
	 * @return the resulting NetworkCertificate
	 * @throws SetupException
	 *         thrown if an error is encountered confirming the server
	 *         association
	 */
	NetworkCertificate acceptNetworkAssociation(NetworkAssociationDetails details) throws SetupException;

	/**
	 * Renew the node's active certificate. The node must already be associated
	 * before this method will work. The renewal will be processed in the
	 * future.
	 * 
	 * @param password
	 *        A password to encrypt the keystore with when passing to SolarNet.
	 * @throws SetupException
	 *         if an error is encountered renewing the certificate
	 * @since 1.2
	 */
	void renewNetworkCertificate(String password) throws SetupException;

	/**
	 * Get the application configuration.
	 * 
	 * <p>
	 * The application configuration includes network service URLs returned by
	 * the SolarUser and SolarQuery applications.
	 * </p>
	 * 
	 * @return the app configuration, never {@code null}
	 * @since 1.3
	 */
	NodeAppConfiguration getAppConfiguration();

}
