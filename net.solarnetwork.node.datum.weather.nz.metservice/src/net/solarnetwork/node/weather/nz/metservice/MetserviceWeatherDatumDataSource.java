/* ==================================================================
 * MetserviceWeatherDatumDataSource.java - Oct 18, 2011 4:58:27 PM
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
import net.solarnetwork.node.domain.datum.AtmosphericDatum;
import net.solarnetwork.node.domain.datum.DayDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.settings.SettingSpecifierProvider;

/**
 * MetService implementation of a {@link AtmosphericDatum}
 * {@link DatumDataSource}.
 *
 * <p>
 * This implementation reads public data available on the MetService website to
 * collect weather information.
 * </p>
 *
 * @author matt
 * @version 3.0
 */
public class MetserviceWeatherDatumDataSource extends MetserviceSupport
		implements DatumDataSource, MultiDatumDataSource, SettingSpecifierProvider {

	/**
	 * Constructor.
	 */
	public MetserviceWeatherDatumDataSource() {
		super();
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return AtmosphericDatum.class;
	}

	@Override
	public NodeDatum readCurrentDatum() {
		Collection<NodeDatum> results = getClient()
				.readCurrentLocalObservations(getLocationIdentifier());
		AtmosphericDatum result = null;
		if ( results != null ) {
			for ( NodeDatum datum : results ) {
				if ( datum instanceof AtmosphericDatum ) {
					result = (AtmosphericDatum) datum;
					break;
				}
			}
		}

		if ( result != null && result.getSkyConditions() == null ) {
			LocalDate resultDay = result.getTimestamp().atZone(ZoneId.systemDefault()).toLocalDate();
			Collection<DayDatum> forecast = getClient().readLocalForecast(getLocationIdentifier());
			if ( forecast != null ) {
				for ( DayDatum day : forecast ) {
					LocalDate dayDay = day.getTimestamp().atZone(ZoneId.systemDefault()).toLocalDate();
					if ( dayDay.compareTo(resultDay) == 0 ) {
						result.setSkyConditions(day.getSkyConditions());
						break;
					}
				}
			}
		}

		return result;
	}

	@Override
	public Class<? extends NodeDatum> getMultiDatumType() {
		return AtmosphericDatum.class;
	}

	@Override
	public Collection<NodeDatum> readMultipleDatum() {
		List<NodeDatum> result = new ArrayList<>(10);
		AtmosphericDatum now = (AtmosphericDatum) readCurrentDatum();
		if ( now != null ) {
			result.add(now);
			Collection<AtmosphericDatum> forecast = getClient()
					.readHourlyForecast(getLocationIdentifier());
			if ( forecast != null ) {
				for ( AtmosphericDatum hour : forecast ) {
					if ( hour.getTimestamp().isAfter(now.getTimestamp()) ) {
						result.add(hour);
					}
				}
			}
		}
		return result;
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.weather.nz.metservice.weather";
	}

	@Override
	public String getDisplayName() {
		return "New Zealand Metservice weather information";
	}

}
