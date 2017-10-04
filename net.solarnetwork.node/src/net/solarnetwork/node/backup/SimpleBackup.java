/* ==================================================================
 * SimpleBackup.java - Mar 27, 2013 2:46:36 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

/**
 * Simple implementation of {@link Backup}.
 * 
 * @author matt
 * @version 1.1
 */
public class SimpleBackup implements Backup {

	private final Date date;
	private final String key;
	private final Long size;
	private final boolean complete;
	private final Long nodeId;

	/**
	 * Construct with values.
	 * 
	 * @param date
	 *        the date
	 * @param key
	 *        the key
	 * @param size
	 *        the size
	 * @param complete
	 *        the complete flag
	 */
	public SimpleBackup(Date date, String key, Long size, boolean complete) {
		this(null, date, key, size, complete);
	}

	/**
	 * Construct with values.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param date
	 *        the date
	 * @param key
	 *        the key
	 * @param size
	 *        the size
	 * @param complete
	 *        the complete flag
	 */
	public SimpleBackup(Long nodeId, Date date, String key, Long size, boolean complete) {
		super();
		this.nodeId = nodeId;
		this.date = date;
		this.key = key;
		this.size = size;
		this.complete = complete;
	}

	@Override
	public Long getNodeId() {
		return nodeId;
	}

	@Override
	public Date getDate() {
		return date;
	}

	@Override
	public String getKey() {
		return key;
	}

	@Override
	public Long getSize() {
		return size;
	}

	@Override
	public boolean isComplete() {
		return complete;
	}

}
