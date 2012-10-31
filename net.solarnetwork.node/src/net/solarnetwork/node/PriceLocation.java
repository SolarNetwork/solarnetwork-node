/* ==================================================================
 * PriceLocation.java - Feb 19, 2011 2:29:20 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.node;

/**
 * Information about a specific price location.
 * 
 * @author matt
 * @version $Revision$
 */
public class PriceLocation {

	private Long locationId;
	private String currency;
	private String unit;
	
	@Override
	public String toString() {
		return "PriceDatum{locationId=" +this.locationId 
			+",currency=" +this.currency
			+",unit=" +this.unit
			+'}';
	}

	/**
	 * @return the locationId
	 */
	public Long getLocationId() {
		return locationId;
	}
	
	/**
	 * @param locationId the locationId to set
	 */
	public void setLocationId(Long locationId) {
		this.locationId = locationId;
	}
	
	/**
	 * @return the currency
	 */
	public String getCurrency() {
		return currency;
	}
	
	/**
	 * @param currency the currency to set
	 */
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	
	/**
	 * @return the unit
	 */
	public String getUnit() {
		return unit;
	}
	
	/**
	 * @param unit the unit to set
	 */
	public void setUnit(String unit) {
		this.unit = unit;
	}
	
}
