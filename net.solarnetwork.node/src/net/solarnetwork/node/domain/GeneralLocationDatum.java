/* ==================================================================
 * GeneralLocationDatum.java - Oct 20, 2014 12:06:00 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.domain;

import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.domain.GeneralLocationDatumSamples;

/**
 * General location datum.
 * 
 * @author matt
 * @version 1.1
 */
public class GeneralLocationDatum extends GeneralNodeDatum {

	private Long locationId;

	/**
	 * Default constructor.
	 */
	public GeneralLocationDatum() {
		super();
		setSourceId(null);
	}

	@Override
	protected GeneralDatumSamples newSamplesInstance() {
		return new GeneralLocationDatumSamples();
	}

	public Long getLocationId() {
		return locationId;
	}

	public void setLocationId(Long locationId) {
		this.locationId = locationId;
	}

}
