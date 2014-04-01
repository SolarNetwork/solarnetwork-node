/* ===================================================================
 * PriceDatum.java
 * 
 * Created Dec 3, 2009 3:46:38 PM
 * 
 * Copyright 2007-2009 SolarNetwork.net Dev Team
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
 * ===================================================================
 */

package net.solarnetwork.node.price;

import java.util.Date;
import net.solarnetwork.node.domain.BaseLocationDatum;
import net.solarnetwork.node.domain.Datum;

/**
 * Domain object for energy price related data.
 * 
 * @author matt
 * @version 1.2
 */
public class PriceDatum extends BaseLocationDatum implements Datum {

	private Double price = null; // the price

	/**
	 * Default constructor.
	 */
	public PriceDatum() {
		super();
	}

	/**
	 * Construct with source and price value.
	 * 
	 * @param sourceId
	 *        the source ID
	 * @param price
	 *        the price
	 * @param locationId
	 *        the location ID
	 */
	public PriceDatum(String sourceId, double price, Long locationId) {
		this();
		setCreated(new Date());
		setSourceId(sourceId);
		setPrice(price);
		setLocationId(locationId);
	}

	@Override
	public String toString() {
		return "PriceDatum{locationId=" + getLocationId() + ",sourceId=" + getSourceId() + ",price="
				+ this.price + '}';
	}

	public Double getPrice() {
		return price;
	}

	public void setPrice(Double price) {
		this.price = price;
	}

}
