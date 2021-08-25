/* ==================================================================
 * UserAuthenticationInfo.java - 12/08/2021 4:29:54 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

import java.util.Collections;
import java.util.Map;

/**
 * Details about user authentication, to support API authentication methods.
 * 
 * @author matt
 * @version 1.0
 * @since 1.89
 */
public class UserAuthenticationInfo {

	private final String hashAlgorithm;
	private final Map<String, ?> hashParameters;

	/**
	 * Constructor.
	 * 
	 * @param hashAlgorithm
	 *        the hashing algorithm used for passwords
	 * @param hashParameters
	 *        optional hash parameters, algorithm-specific
	 * @throws IllegalArgumentException
	 *         if {@code hashAlgorithm} is {@literal null}
	 */
	public UserAuthenticationInfo(String hashAlgorithm, Map<String, ?> hashParameters) {
		super();
		if ( hashAlgorithm == null ) {
			throw new IllegalArgumentException("The hashAlgorithm argument must not be null.");
		}
		this.hashAlgorithm = hashAlgorithm;
		this.hashParameters = (hashParameters != null ? hashParameters : Collections.emptyMap());
	}

	/**
	 * Get the hash algorithm name.
	 * 
	 * @return the hash algorithm name, never {@literal null}
	 */
	public String getHashAlgorithm() {
		return hashAlgorithm;
	}

	/**
	 * Get the optional hash algorithm parameters.
	 * 
	 * @return the hash parameters, never {@literal null}
	 */
	public Map<String, ?> getHashParameters() {
		return hashParameters;
	}

}
