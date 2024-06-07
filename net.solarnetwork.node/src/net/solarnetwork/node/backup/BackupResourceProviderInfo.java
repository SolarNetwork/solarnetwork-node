/* ==================================================================
 * BackupResourceProviderInfo.java - 2/11/2016 1:26:05 PM
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

package net.solarnetwork.node.backup;

/**
 * Metadata for a {@link BackupResourceProvider}.
 *
 * @author matt
 * @version 1.1
 * @since 1.46
 */
public interface BackupResourceProviderInfo {

	/**
	 * Get a unique key for the provider.
	 *
	 * @return The unique provider key.
	 */
	String getProviderKey();

	/**
	 * Get a display-friendly name of the {@link BackupResourceProvider}.
	 *
	 * @return The name.
	 */
	String getName();

	/**
	 * Get a display-friendly description of the {@link BackupResourceProvider}.
	 *
	 * @return A display-friendly description.
	 */
	String getDescription();

	/**
	 * Get the "should restore" default setting.
	 *
	 * <p>
	 * This flag indicates to the backup interface if the resources provided by
	 * this service should be enabled for restore by default.
	 * </p>
	 *
	 * @return {@literal true} if the resources provided by this service should
	 *         be selected for restore by default
	 * @since 1.1
	 */
	default boolean isDefaultShouldRestore() {
		return true;
	}

}
