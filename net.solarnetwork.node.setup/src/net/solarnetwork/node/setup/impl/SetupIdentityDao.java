/* ==================================================================
 * SetupIdentityDao.java - 3/11/2017 6:51:23 AM
 *
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.impl;

/**
 * API for accessing the singleton {@link SetupIdentityInfo}.
 *
 * @author matt
 * @version 1.0
 */
public interface SetupIdentityDao {

	/**
	 * Get the current identity.
	 *
	 * @return the current identity, never {@literal null}
	 */
	SetupIdentityInfo getSetupIdentityInfo();

	/**
	 * Save identity information.
	 *
	 * @param info
	 *        the information to save
	 */
	void saveSetupIdentityInfo(SetupIdentityInfo info);

}
