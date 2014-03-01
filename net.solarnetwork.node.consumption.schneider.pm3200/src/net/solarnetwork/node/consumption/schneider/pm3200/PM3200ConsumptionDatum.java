/* ==================================================================
 * PM3200ConsumptionDatum.java - 1/03/2014 10:12:53 AM
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

package net.solarnetwork.node.consumption.schneider.pm3200;

import net.solarnetwork.node.consumption.ConsumptionDatum;

/**
 * Extension of {@link ConsumptionDatum} with additional properties supported by
 * the PM3200 series meters.
 * 
 * @author matt
 * @version 1.0
 */
public class PM3200ConsumptionDatum extends ConsumptionDatum {

	/**
	 * Test if the data appears valid in this datum.
	 * 
	 * @return <em>true</em> if the data appears to be valid
	 */
	public boolean isValid() {
		return (getVolts() != null && getAmps() != null) || getWattHourReading() != null;
	}

}
