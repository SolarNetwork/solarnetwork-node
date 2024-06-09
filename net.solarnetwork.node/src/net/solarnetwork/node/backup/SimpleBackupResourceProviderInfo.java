/* ==================================================================
 * SimpleBackupResourceInfo.java - 2/11/2016 1:29:40 PM
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
 * Basic implementation of {@link BackupResourceProviderInfo}.
 *
 * @author matt
 * @version 1.1
 * @since 1.46
 */
public class SimpleBackupResourceProviderInfo implements BackupResourceProviderInfo {

	private final String providerKey;
	private final String name;
	private final String description;
	private final boolean defaultShouldRestore;

	/**
	 * Construct with values.
	 *
	 * <p>
	 * The {@code defaultShouldRestore} property will be set to {@literal true}.
	 * </p>
	 *
	 * @param providerKey
	 *        The provider key.
	 * @param name
	 *        The name.
	 * @param description
	 *        The description.
	 */
	public SimpleBackupResourceProviderInfo(String providerKey, String name, String description) {
		this(providerKey, name, description, true);
	}

	/**
	 * Construct with values.
	 *
	 * @param providerKey
	 *        The provider key.
	 * @param name
	 *        The name.
	 * @param description
	 *        The description.
	 * @param defaultShouldRestore
	 *        the "default should restore" flag
	 * @since 1.1
	 */
	public SimpleBackupResourceProviderInfo(String providerKey, String name, String description,
			boolean defaultShouldRestore) {
		super();
		this.providerKey = providerKey;
		this.name = name;
		this.description = description;
		this.defaultShouldRestore = defaultShouldRestore;
	}

	@Override
	public String getProviderKey() {
		return providerKey;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public boolean isDefaultShouldRestore() {
		return defaultShouldRestore;
	}

}
