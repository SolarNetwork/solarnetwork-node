/* ==================================================================
 * SolcatCriteria.java - 14/10/2022 10:00:04 am
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

package net.solarnetwork.node.datum.solcast;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Set;

/**
 * Solcast common API criteria.
 * 
 * @author matt
 * @version 1.0
 */
public class SolcastCriteria {

	private BigDecimal lat;
	private BigDecimal lon;
	private Set<String> parameters;
	private Duration period;

	/**
	 * Test if the configured criteria is valid.
	 * 
	 * @return {@literal true} if the properties of this criteria are valid for
	 *         submitting to Solcast
	 */
	public boolean isValid() {
		return (lat != null && lon != null);
	}

	/**
	 * Get the GPS latitude to use in Solcast API calls.
	 * 
	 * @return the latitude
	 */
	public BigDecimal getLat() {
		return lat;
	}

	/**
	 * Set the GPS latitude to use in Solcast API calls.
	 * 
	 * @param lat
	 *        the latitude to set
	 */
	public void setLat(BigDecimal lat) {
		this.lat = lat;
	}

	/**
	 * Get the GPS longitude to use in Solcast API calls.
	 * 
	 * @return the longitude
	 */
	public BigDecimal getLon() {
		return lon;
	}

	/**
	 * Set the GPS longitude to use in Solcast API calls.
	 * 
	 * @param lat
	 *        the longitude to set
	 */
	public void setLon(BigDecimal lon) {
		this.lon = lon;
	}

	/**
	 * Get the set of Solcast parameters to collect.
	 * 
	 * @return the parameters
	 */
	public Set<String> getParameters() {
		return parameters;
	}

	/**
	 * Set the set of Solcast parameters to collect.
	 * 
	 * @param parameters
	 *        the parameters to set
	 */
	public void setParameters(Set<String> parameters) {
		this.parameters = parameters;
	}

	/**
	 * Get the desired period.
	 * 
	 * @return the period
	 */
	public Duration getPeriod() {
		return period;
	}

	/**
	 * Set the desired period.
	 * 
	 * @param period
	 *        the period to set
	 */
	public void setPeriod(Duration period) {
		this.period = period;
	}

}
