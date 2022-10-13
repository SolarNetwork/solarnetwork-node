/* ==================================================================
 * OwmDayDatumDataSource.java - 14/09/2018 4:58:36 PM
 * 
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
 * ==================================================================
 */

package net.solarnetwork.node.weather.owm;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import net.solarnetwork.node.domain.datum.DayDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.settings.SettingSpecifierProvider;

/**
 * OWM implementation of a {@link DayDatum} {@link DatumDataSource}.
 * 
 * @author matt
 * @version 2.0
 */
public class OwmDayDatumDataSource extends ConfigurableOwmClientService
		implements SettingSpecifierProvider, DatumDataSource, MultiDatumDataSource {

	/**
	 * Constructor.
	 */
	public OwmDayDatumDataSource() {
		super();
		setDisplayName("OWM day information");
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return DayDatum.class;
	}

	@Override
	public NodeDatum readCurrentDatum() {
		// first see if we have cached data
		NodeDatum result = datumCache.get(LAST_DATUM_CACHE_KEY);
		if ( result != null && result.getTimestamp() != null ) {
			ZoneId zone = ZoneId.of(getTimeZoneId());
			LocalDate today = LocalDate.now(zone);
			LocalDate datumDay = result.getTimestamp().atZone(zone).toLocalDate();
			if ( today.compareTo(datumDay) == 0 ) {
				// cached data is for same date, so return that
				return result;
			}

			// invalid cached data, remove now
			datumCache.remove(LAST_DATUM_CACHE_KEY);
		}

		result = getClient().getCurrentDay(getLocationIdentifier(), getTimeZoneId());

		if ( result != null ) {
			datumCache.put(LAST_DATUM_CACHE_KEY, result);
		}

		return result;
	}

	@Override
	public Class<? extends NodeDatum> getMultiDatumType() {
		return DayDatum.class;
	}

	@Override
	public Collection<NodeDatum> readMultipleDatum() {
		return Collections.singleton(readCurrentDatum());
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.weather.owm.day";
	}

}
