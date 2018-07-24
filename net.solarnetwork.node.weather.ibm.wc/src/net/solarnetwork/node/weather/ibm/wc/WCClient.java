
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
	 * Read the daily forecast data for a location
	 * 
	 * @param locationIdentifier
	 *        The location for which the forecast is retrieved
	 * @return A collection of {@link GeneralDayDatum} that will be utilized.
	 */
	Collection<GeneralDayDatum> readDailyForecast(String locationIdentifier, String apiKey,
			String datumPeriod);

	/**
	 * Read the hourly forecast for a location
	 * 
	 * @param locationIdentifier
	 *        The location for which the forecast is retrieved
	 * @return A collection of {@link GeneralDayDatum} that will be utilized.
	 */
	Collection<WCHourlyDatum> readHourlyForecast(String locationIdentifier, String apiKey,
			String datumPeriod);

}
