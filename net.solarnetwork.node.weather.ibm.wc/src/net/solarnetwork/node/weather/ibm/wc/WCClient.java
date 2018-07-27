
package net.solarnetwork.node.weather.ibm.wc;

import java.util.Collection;
import net.solarnetwork.node.domain.GeneralDayDatum;

/**
 * This is an API for accessing and storing data from the IBM Weather Company
 * APIs
 * 
 * @author matt frost
 *
 */
public interface WCClient {

	/**
	 * @param locationIdentifier
	 *        - The location for which the forecast is retrieved
	 * @param apiKey
	 *        - the api key for connecting to IBM Weather
	 * @param datumPeriod
	 *        - the period over which the datum can be retrieved from
	 * @return
	 */
	Collection<GeneralDayDatum> readDailyForecast(String locationIdentifier, String apiKey,
			DailyDatumPeriod datumPeriod);

	/**
	 * @param locationIdentifier
	 *        - The location for which the forecast is retrieved
	 * @param apiKey
	 *        - the api key for connecting to IBM Weather
	 * @param datumPeriod
	 *        - the period over which the datum can be retrieved from
	 * @return
	 */
	Collection<WCHourlyDatum> readHourlyForecast(String locationIdentifier, String apiKey,
			HourlyDatumPeriod datumPeriod);

}
