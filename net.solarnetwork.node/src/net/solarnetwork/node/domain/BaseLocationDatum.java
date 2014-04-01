/* ==================================================================
 * BaseLocationDatum.java - Nov 18, 2013 3:27:31 PM
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

package net.solarnetwork.node.domain;


/**
 * Base object for location datum, such as Price or Weather.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class BaseLocationDatum extends BaseDatum {

	private Long locationId;

	/**
	 * Default constructor.
	 */
	public BaseLocationDatum() {
		super();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getCreated() == null) ? 0 : getCreated().hashCode());
		result = prime * result + ((locationId == null) ? 0 : locationId.hashCode());
		return result;
	}

	/**
	 * Compare for equality.
	 * 
	 * <p>
	 * This method compares the {@code created} and {@code locationId} values
	 * for equality.
	 * </p>
	 * 
	 * @return <em>true</em> if the objects are equal
	 */
	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		BaseLocationDatum other = (BaseLocationDatum) obj;
		if ( getCreated() == null ) {
			if ( other.getCreated() != null ) {
				return false;
			}
		} else if ( !getCreated().equals(other.getCreated()) ) {
			return false;
		}
		if ( locationId == null ) {
			if ( other.locationId != null ) {
				return false;
			}
		} else if ( !locationId.equals(other.locationId) ) {
			return false;
		}
		return true;
	}

	public Long getLocationId() {
		return locationId;
	}

	public void setLocationId(Long locationId) {
		this.locationId = locationId;
	}

}
