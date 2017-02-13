/* ==================================================================
 * UserService.java - 13/02/2017 3:40:06 PM
 * 
 * Copyright 2007-2017 SolarNetwork.net Dev Team
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

/**
 * API for managing users and roles on the SolarNode system.
 * 
 * @author matt
 * @version 1.0
 * @since 1.48
 */
public interface UserService {

	/**
	 * Test if any user exists.
	 * 
	 * @return <em>true</em> if some user exists
	 */
	public boolean someUserExists();

	/**
	 * Update the active user's password.
	 * 
	 * @param existingPassword
	 *        The existing password.
	 * @param newPassword
	 *        The new password to set.
	 * @param newPasswordAgain
	 *        The new password, repeated.
	 * @throws IllegalArgumentException
	 *         if the {@code newPassword} and {@code newPasswordAgain} values do
	 *         not match, or are <em>null</em>
	 */
	public void changePassword(final String existingPassword, final String newPassword,
			final String newPasswordAgain);

	/**
	 * Update the active user's username.
	 * 
	 * @param newUsername
	 *        The new username to set.
	 * @param newUsernameAgain
	 *        The new username, repeated.
	 * @throws IllegalArgumentException
	 *         if the {@code newUsername} and {@code newUsernameAgain} values do
	 *         not match, or are <em>null</em>
	 */
	public void changeUsername(String newUsername, String newUsernameAgain);

	/**
	 * Store a user profile into settings.
	 * 
	 * @param profile
	 *        The profile to store.
	 * @throws IllegalArgumentException
	 *         if {@code username} is <em>null</em>, or if the {@code password}
	 *         and {@code passwordAgain} values do not match or are
	 *         <em>null</em>
	 */
	public void storeUserProfile(UserProfile profile);

}
