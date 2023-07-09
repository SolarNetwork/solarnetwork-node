/* ==================================================================
 * ReferencePointModelAccessor.java - 9/07/2023 4:28:44 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sunspec.environmental;

import java.util.List;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;

/**
 * API for accessing reference point model data.
 * 
 * @author matt
 * @version 1.0
 * @since 4.2
 */
public interface ReferencePointModelAccessor extends ModelAccessor {

	/**
	 * Get the list of available reference points.
	 * 
	 * @return the reference points
	 */
	List<ReferencePoint> getReferencePoints();

	/**
	 * Get the first available reference point element.
	 * 
	 * @return the first available reference point, or {@literal null}
	 */
	default ReferencePoint getReferencePoint() {
		List<ReferencePoint> points = getReferencePoints();
		return (points != null && !points.isEmpty() ? points.get(0) : null);
	}

}
