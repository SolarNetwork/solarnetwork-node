/* ==================================================================
 * PricedDatum.java - Aug 26, 2014 8:34:45 PM
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

/**
 * Standardized API for datum associated with a price to implement.
 * 
 * @author matt
 * @version 1.1
 */
public interface PricedDatum {

	/**
	 * A {@link net.solarnetwork.domain.GeneralNodeDatumSamples} status sample
	 * key for {@link #getPriceLocationId()} values.
	 */
	public static final String PRICE_LOCATION_KEY = "priceLocationId";

	/**
	 * A {@link net.solarnetwork.domain.GeneralNodeDatumSamples} status sample
	 * key for {@link #getPriceSourceId()} values.
	 * 
	 * @since 1.1
	 */
	public static final String PRICE_SOURCE_KEY = "priceSourceId";

	/**
	 * Get the location ID associated with this datum.
	 * 
	 * @return the price location ID
	 */
	public Long getPriceLocationId();

	/**
	 * Get the location source ID associated with this datum.
	 * 
	 * @return the price source ID
	 * @since 1.1
	 */
	public String getPriceSourceId();

}
