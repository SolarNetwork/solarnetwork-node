/* ==================================================================
 * PriceMapAccessor.java - 10/08/2019 11:46:01 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.esi.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Locale;
import net.solarnetwork.service.Identifiable;

/**
 * API for accessing PriceMap information.
 * 
 * @author matt
 * @version 2.0
 */
public interface PriceMapAccessor extends Identifiable {

	/**
	 * Get a status message.
	 * 
	 * @param locale
	 *        the desired locale of the message
	 * @return the message
	 */
	String getStatusMessage(Locale locale);

	/**
	 * Calculate the theoretical cost represented by this price map as the
	 * apparent power multiplied by the duration (in hours) multiplied by the
	 * apparent energy price.
	 * 
	 * @return the apparent energy cost, in the configured currency units per
	 *         volt-amp-hours (VAh)
	 */
	BigDecimal calculatedApparentEnergyCost();

	/**
	 * Get the fractional hours represented by the configured duration.
	 * 
	 * @return the duration, as fractional hours
	 */
	double durationHours();

	/**
	 * Get a brief informational string out of the main aspects of this price
	 * map.
	 * 
	 * @param locale
	 *        the locale
	 * @return the string
	 */
	String toInfoString(Locale locale);

	/**
	 * Get the info string in the default locale.
	 * 
	 * @return the info string
	 * @see #toInfoString(Locale)
	 */
	String getInfo();

	/**
	 * Get the power components.
	 * 
	 * @return the power components
	 */
	PowerComponents getPowerComponents();

	/**
	 * Get the duration of time for this price map.
	 * 
	 * @return the duration
	 */
	Duration getDuration();

	/**
	 * Get the duration, in milliseconds
	 * 
	 * @return the duration, in milliseconds
	 */
	long getDurationMillis();

	/**
	 * Get the response time range.
	 * 
	 * @return the response time range
	 */
	DurationRange getResponseTime();

	/**
	 * Get the price components.
	 * 
	 * @return the price components
	 */
	PriceComponents getPriceComponents();

}
