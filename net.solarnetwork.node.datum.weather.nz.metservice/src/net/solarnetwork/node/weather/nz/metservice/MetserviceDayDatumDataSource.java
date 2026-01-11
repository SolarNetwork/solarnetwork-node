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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.solarnetwork.node.domain.datum.DayDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.settings.SettingSpecifierProvider;

/**
 * MetService implementation of a {@link DayDatum} {@link DatumDataSource}.
 *
 * <p>
 * This implementation reads public data available on the MetService website to
 * collect day information (sunrise, sunset, etc.).
 * </p>
 *
 * @author matt
 * @version 3.0
 */
public class MetserviceDayDatumDataSource extends MetserviceSupport
		implements DatumDataSource, MultiDatumDataSource, SettingSpecifierProvider {

	/**
	 * Constructor.
	 */
	public MetserviceDayDatumDataSource() {
		super();
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return DayDatum.class;
	}

	@Override
	public NodeDatum readCurrentDatum() {

		// first see if we have cached data
		NodeDatum result = getDatumCache().get(LAST_DATUM_CACHE_KEY);
		if ( result != null ) {
			LocalDate today = LocalDate.now();
			LocalDate datumDay = result.getTimestamp().atZone(ZoneId.systemDefault()).toLocalDate();
			if ( today.compareTo(datumDay) == 0 ) {
				// cached data is for same date, so return that
				return result;
			}

			// invalid cached data, remove now
			getDatumCache().remove(LAST_DATUM_CACHE_KEY);
		}

		DayDatum dayResult = getClient().readCurrentRiseSet(getLocationIdentifier());

		Collection<NodeDatum> observations = getClient()
				.readCurrentLocalObservations(getLocationIdentifier());
		for ( NodeDatum observation : observations ) {
			if ( observation instanceof DayDatum ) {
				DayDatum day = (DayDatum) observation;
				dayResult.setTemperatureMinimum(day.getTemperatureMinimum());
				dayResult.setTemperatureMaximum(day.getTemperatureMaximum());
			}
		}
		getDatumCache().put(LAST_DATUM_CACHE_KEY, dayResult);

		return dayResult;
	}

	@Override
	public Class<? extends NodeDatum> getMultiDatumType() {
		return DayDatum.class;
	}

	@Override
	public Collection<NodeDatum> readMultipleDatum() {
		List<NodeDatum> result = new ArrayList<>(10);
		DayDatum today = (DayDatum) readCurrentDatum();
		if ( today != null ) {
			result.add(today);
		}
		Collection<DayDatum> forecast = getClient().readLocalForecast(getLocationIdentifier());
		if ( forecast != null ) {
			for ( DayDatum day : forecast ) {
				if ( day.getTimestamp().equals(today.getTimestamp()) ) {
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
	public String getSettingUid() {
		return "net.solarnetwork.node.weather.nz.metservice.day";
	}

	@Override
	public String getDisplayName() {
		return "New Zealand Metservice day information";
	}

}
