/* ==================================================================
 * WeatherUndergroundDayDatumDataSource.java - Apr 9, 2017 4:28:03 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.weather.wu;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.domain.DayDatum;
import net.solarnetwork.node.domain.GeneralDayDatum;
import net.solarnetwork.node.settings.SettingSpecifierProvider;

/**
 * Weather Underground implementation of a {@link DayDatum}
 * {@link DatumDataSource}.
 * 
 * @author matt
 * @version 1.0
 */
public class WeatherUndergroundDayDatumDataSource
		extends ConfigurableWeatherUndergroundClientService<DayDatum>
		implements SettingSpecifierProvider, DatumDataSource<DayDatum>, MultiDatumDataSource<DayDatum> {

	@Override
	public Class<? extends DayDatum> getMultiDatumType() {
		return GeneralDayDatum.class;
	}

	@Override
	public Class<? extends DayDatum> getDatumType() {
		return GeneralDayDatum.class;
	}

	@Override
	public DayDatum readCurrentDatum() {
		// first see if we have cached data
		DayDatum result = datumCache.get(LAST_DATUM_CACHE_KEY);
		if ( result != null && result.getCreated() != null ) {
			Calendar now = Calendar.getInstance();
			Calendar datumCal = Calendar.getInstance();
			datumCal.setTime(result.getCreated());
			if ( now.get(Calendar.YEAR) == datumCal.get(Calendar.YEAR)
					&& now.get(Calendar.DAY_OF_YEAR) == datumCal.get(Calendar.DAY_OF_YEAR) ) {
				// cached data is for same date, so return that
				return result;
			}

			// invalid cached data, remove now
			datumCache.remove(LAST_DATUM_CACHE_KEY);
		}

		result = getClient().getCurrentDay(getLocationIdentifier());

		if ( result != null ) {
			datumCache.put(LAST_DATUM_CACHE_KEY, result);
		}

		return result;
	}

	@Override
	public Collection<DayDatum> readMultipleDatum() {
		List<DayDatum> result = new ArrayList<DayDatum>(10);
		DayDatum today = readCurrentDatum();
		if ( today != null ) {
			result.add(today);
		}
		Collection<DayDatum> forecast = getClient().getTenDayForecast(getLocationIdentifier());
		if ( forecast != null ) {
			for ( DayDatum day : forecast ) {
				if ( day.getCreated() != null && today.getCreated() != null
						&& day.getCreated().equals(today.getCreated()) ) {
					// skip today
					continue;
				}
				result.add(day);
			}
		}
		return result;
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.weather.wu.day";
	}

	@Override
	public String getDisplayName() {
		return "Weather Underground day information";
	}

}
