/* ==================================================================
 * MetserviceDayDatumDataSource.java - Oct 18, 2011 2:48:45 PM
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

package net.solarnetwork.node.weather.nz.metservice;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.domain.DayDatum;
import net.solarnetwork.node.domain.GeneralDayDatum;
import net.solarnetwork.node.domain.GeneralLocationDatum;
import net.solarnetwork.node.settings.SettingSpecifierProvider;

/**
 * MetService implementation of a {@link GeneralDayDatum}
 * {@link DatumDataSource}.
 * 
 * <p>
 * This implementation reads public data available on the MetService website to
 * collect day information (sunrise, sunset, etc.).
 * </p>
 * 
 * @author matt
 * @version 2.0
 */
public class MetserviceDayDatumDataSource extends MetserviceSupport<GeneralDayDatum> implements
		DatumDataSource<GeneralLocationDatum>, MultiDatumDataSource<GeneralLocationDatum>,
		SettingSpecifierProvider {

	@Override
	public Class<? extends GeneralLocationDatum> getDatumType() {
		return GeneralDayDatum.class;
	}

	@Override
	public GeneralLocationDatum readCurrentDatum() {

		// first see if we have cached data
		GeneralDayDatum result = getDatumCache().get(LAST_DATUM_CACHE_KEY);
		if ( result != null ) {
			Calendar now = Calendar.getInstance();
			Calendar datumCal = Calendar.getInstance();
			datumCal.setTime(result.getCreated());
			if ( now.get(Calendar.YEAR) == datumCal.get(Calendar.YEAR)
					&& now.get(Calendar.DAY_OF_YEAR) == datumCal.get(Calendar.DAY_OF_YEAR) ) {
				// cached data is for same date, so return that
				return result;
			}

			// invalid cached data, remove now
			getDatumCache().remove(LAST_DATUM_CACHE_KEY);
		}

		result = getClient().readCurrentRiseSet(getLocationIdentifier());

		Collection<GeneralLocationDatum> observations = getClient().readCurrentLocalObservations(
				getLocationIdentifier());
		for ( GeneralLocationDatum observation : observations ) {
			if ( observation instanceof DayDatum ) {
				DayDatum day = (DayDatum) observation;
				result.setTemperatureMinimum(day.getTemperatureMinimum());
				result.setTemperatureMaximum(day.getTemperatureMaximum());
			}
		}
		getDatumCache().put(LAST_DATUM_CACHE_KEY, result);

		return result;
	}

	@Override
	public Class<? extends GeneralLocationDatum> getMultiDatumType() {
		return GeneralDayDatum.class;
	}

	@Override
	public Collection<GeneralLocationDatum> readMultipleDatum() {
		List<GeneralLocationDatum> result = new ArrayList<GeneralLocationDatum>(10);
		GeneralDayDatum today = (GeneralDayDatum) readCurrentDatum();
		if ( today != null ) {
			result.add(today);
		}
		Collection<GeneralDayDatum> forecast = getClient().readLocalForecast(getLocationIdentifier());
		if ( forecast != null ) {
			for ( GeneralDayDatum day : forecast ) {
				if ( day.getCreated().equals(today.getCreated()) ) {
					if ( today.getSkyConditions() == null ) {
						// copy from forecast
						today.setSkyConditions(day.getSkyConditions());
					}
					continue;
				}
				result.add(day);
			}
		}
		return result;
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.weather.nz.metservice.day";
	}

	@Override
	public String getDisplayName() {
		return "New Zealand Metservice day information";
	}

}
