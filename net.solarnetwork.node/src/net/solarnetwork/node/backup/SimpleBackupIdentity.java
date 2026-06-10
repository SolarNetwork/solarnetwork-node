/* ==================================================================
 * SimpleBackupIdentity.java - 12/10/2017 5:14:43 PM
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

package net.solarnetwork.node.backup;

import java.util.Date;
import org.jspecify.annotations.Nullable;

/**
 * Basic implementation of {@link BackupIdentity}.
 *
 * @author matt
 * @version 1.0
 */
public class SimpleBackupIdentity implements BackupIdentity {

	private final String key;
	private final Date date;
	private final @Nullable Long nodeId;
	private final @Nullable String qualifier;

	/**
	 * Constructor.
	 *
	 * @param key
	 *        the key
	 * @param date
	 *        the date
	 * @param nodeId
	 *        the node ID
	 * @param qualifier
	 *        the qualifier
	 */
	public SimpleBackupIdentity(String key, Date date, @Nullable Long nodeId,
			@Nullable String qualifier) {
		super();
		this.key = key;
		this.date = date;
		this.nodeId = nodeId;
		this.qualifier = qualifier;
	}

	@Override
	public String getKey() {
		return key;
	}

	@Override
	public Date getDate() {
		return date;
	}

	@Override
	public @Nullable Long getNodeId() {
		return nodeId;
	}

	@Override
	public @Nullable String getQualifier() {
		return qualifier;
	}

}
