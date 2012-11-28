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

import net.solarnetwork.domain.NetworkAssociationDetails;

/**
 * API for node setup support.
 * 
 * @author matt
 * @version $Id$
 */
public interface SetupService {

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
	 * Associate this node with a SolarNet central service.
	 * 
	 * @param details
	 *        the host details to associate with
	 * @throws SetupException
	 *         thrown if an error is encountered confirming the server
	 *         association
	 */
	void acceptSolarNetHost(NetworkAssociationDetails details) throws SetupException;

	/**
	 * Should use the host settings in the supplied <code>details</code> to
	 * retrieve the server identity and terms of service and store them in
	 * <code>details</code>.
	 * 
	 * @param details
	 *        Contains the host details and is where the identity key and TOS
	 *        will be stored.
	 */
	void populateServerIdentity(NetworkAssociationDetails details);
}
