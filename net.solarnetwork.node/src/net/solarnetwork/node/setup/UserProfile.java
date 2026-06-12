/* ==================================================================
 * UserProfile.java - 27/07/2016 1:31:33 PM
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

package net.solarnetwork.node.setup;

import org.jspecify.annotations.Nullable;

/**
 * Command object for a user profile.
 *
 * @author matt
 * @version 1.0
 * @since 1.48
 */
public class UserProfile {

	private @Nullable String username;
	private @Nullable String oldPassword;
	private @Nullable String password;
	private @Nullable String passwordAgain;

	/**
	 * Default constructor.
	 */
	public UserProfile() {
		super();
	}

	/**
	 * Get the username.
	 *
	 * @return the username
	 */
	public @Nullable String getUsername() {
		return username;
	}

	/**
	 * Set the username.
	 *
	 * @param username
	 *        the username to set
	 */
	public void setUsername(@Nullable String username) {
		this.username = username;
	}

	/**
	 * Get the old password.
	 *
	 * @return the old password
	 */
	public @Nullable String getOldPassword() {
		return oldPassword;
	}

	/**
	 * Set the old password.
	 *
	 * @param oldPassword
	 *        the old password to set
	 */
	public void setOldPassword(@Nullable String oldPassword) {
		this.oldPassword = oldPassword;
	}

	/**
	 * Get the password.
	 *
	 * @return the password
	 */
	public @Nullable String getPassword() {
		return password;
	}

	/**
	 * Set the password.
	 *
	 * @param password
	 *        the password to set
	 */
	public void setPassword(@Nullable String password) {
		this.password = password;
	}

	/**
	 * Get the password again.
	 *
	 * @return the password again
	 */
	public @Nullable String getPasswordAgain() {
		return passwordAgain;
	}

	/**
	 * Set the password again.
	 *
	 * @param passwordAgain
	 *        the password again
	 */
	public void setPasswordAgain(@Nullable String passwordAgain) {
		this.passwordAgain = passwordAgain;
	}

}
