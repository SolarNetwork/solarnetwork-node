/* ==================================================================
 * LoginKey.java - 30/10/2019 6:35:20 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.web.support;

/**
 * Random data and associated metadata for a nonce-based authentication.
 * 
 * @author matt
 * @version 1.0
 * @since 1.41
 */
public class LoginKey {

	private final String iv;
	private final String key;

	/**
	 * Constructor.
	 * 
	 * @param iv
	 *        encryption initialization vector, Base64 encoded
	 * @param key
	 *        encryption key, Base64 encoded
	 */
	public LoginKey(String iv, String key) {
		super();
		this.iv = iv;
		this.key = key;
	}

	/**
	 * Get the encryption initialization vector, Base64 encoded.
	 * 
	 * @return the encryption initialization vector, Base64 encoded
	 */
	public String getIv() {
		return iv;
	}

	/**
	 * Get the encryption key, Base64 encoded.
	 * 
	 * @return the encryption key, Base64 encoded
	 */
	public String getKey() {
		return key;
	}

}
