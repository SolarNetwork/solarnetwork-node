/* ==================================================================
 * OverlayCloudService.java - 5/07/2022 7:40:37 am
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

import java.util.List;

/**
 * Service API for Overlay Cloud.
 * 
 * @author matt
 * @version 1.0
 */
public interface OverlayCloudService {

	/**
	 * Get all available grids.
	 * 
	 * @return the available grids
	 */
	List<Grid> getGrids();

	/**
	 * Get the latest available feed data.
	 * 
	 * @param gridId
	 *        the ID of the grid
	 * @param feedId
	 *        the ID of the feed
	 * @return the feed data
	 */
	FeedData getFeedLatest(Long gridId, Long feedId);

}
