/* ===================================================================
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.weather.ibm.wc;

import java.util.Collection;
import net.solarnetwork.node.domain.datum.AtmosphericDatum;
import net.solarnetwork.node.domain.datum.DayDatum;

/**
 * This is an API for accessing and storing data from the IBM Weather Company
 * APIs
 * 
 * @author matt frost
 * @version 2.0
 */
public interface WCClient {

	/**
	 * @param locationIdentifier
	 *        the WC location for which the forecast is retrieved
	 * @param apiKey
	 *        the WC api key for connecting to IBM Weather
	 * @param datumPeriod
	 *        the period over which the datum can be retrieved from
	 * @return the results, never {@literal null}
	 */
	Collection<DayDatum> readDailyForecast(String locationIdentifier, String apiKey,
			DailyDatumPeriod datumPeriod);

	/**
	 * @param locationIdentifier
	 *        the WC location for which the forecast is retrieved
	 * @param apiKey
	 *        the WC api key for connecting to IBM Weather
	 * @param datumPeriod
	 *        the period over which the datum can be retrieved from
	 * @return the results, never {@literal null}
	 */
	Collection<AtmosphericDatum> readHourlyForecast(String locationIdentifier, String apiKey,
			HourlyDatumPeriod datumPeriod);

}
