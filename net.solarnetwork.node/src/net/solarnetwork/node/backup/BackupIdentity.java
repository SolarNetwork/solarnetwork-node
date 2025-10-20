/* ==================================================================
 * BackupIdentity.java - 12/10/2017 5:11:49 PM
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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.domain.Unique;

/**
 * Identity information about a {@link Backup}.
 *
 * @author matt
 * @version 1.1
 * @since 1.55
 */
@JsonPropertyOrder({ "key", "nodeId", "date", "qualifier" })
public interface BackupIdentity extends Unique<String> {

	/**
	 * Get the backup ID.
	 *
	 * <p>
	 * This implementation returns {@link #getKey()}.
	 * </p>
	 *
	 * @since 1.1
	 */
	@JsonIgnore
	@Override
	default String getId() {
		return getKey();
	}

	/**
	 * Get a unique key for the backup.
	 *
	 * @return the backup key
	 */
	String getKey();

	/**
	 * Get the node ID associated with the backup.
	 *
	 * @return the node ID
	 */
	Long getNodeId();

	/**
	 * Get the date the backup was created.
	 *
	 * @return the backup date
	 */
	Date getDate();

	/**
	 * Get an optional qualifier.
	 *
	 * <p>
	 * A qualifier can be used to provide a backup with a more descriptive name
	 * or tag.
	 * </p>
	 *
	 * @return the qualifier, or {@literal null} if not known
	 */
	String getQualifier();

}
