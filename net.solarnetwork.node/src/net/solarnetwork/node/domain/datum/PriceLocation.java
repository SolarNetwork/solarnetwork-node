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
 */

package net.solarnetwork.node.domain.datum;

/**
 * Information about a specific price location.
 * 
 * @author matt
 * @version 1.1
 */
public class PriceLocation extends SimpleDatumLocation {

	private String currency;
	private String unit;

	/**
	 * Default constructor.
	 */
	public PriceLocation() {
		super();
	}

	@Override
	public String toString() {
		return "PriceDatum{locationId=" + getLocationId() + ",currency=" + this.currency + ",unit="
				+ this.unit + '}';
	}

	/**
	 * Get the currency symbol.
	 * 
	 * @return the currency symbol
	 */
	public String getCurrency() {
		return currency;
	}

	/**
	 * Set the currency symbol.
	 * 
	 * @param currency
	 *        the currency symbol to set
	 */
	public void setCurrency(String currency) {
		this.currency = currency;
	}

	/**
	 * Get the unit.
	 * 
	 * @return the unit
	 */
	public String getUnit() {
		return unit;
	}

	/**
	 * Set the unit.
	 * 
	 * @param unit
	 *        the unit to set
	 */
	public void setUnit(String unit) {
		this.unit = unit;
	}

}
