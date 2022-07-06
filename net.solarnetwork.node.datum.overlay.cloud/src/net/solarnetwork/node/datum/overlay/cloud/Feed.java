/* ==================================================================
 * Feed.java - 6/07/2022 5:13:27 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.overlay.cloud;

/**
 * A feed entity, within a grid.
 * 
 * @author matt
 * @version 1.0
 */
public class Feed extends BaseEntity {

	/**
	 * Get the feed ID.
	 * 
	 * @return the feed ID
	 */
	public Long getFeedId() {
		return getId();
	}

	/**
	 * Set the feed ID.
	 * 
	 * @param feedId
	 *        the feed ID
	 */
	public void setFeedId(Long feedId) {
		setId(feedId);
	}

}
