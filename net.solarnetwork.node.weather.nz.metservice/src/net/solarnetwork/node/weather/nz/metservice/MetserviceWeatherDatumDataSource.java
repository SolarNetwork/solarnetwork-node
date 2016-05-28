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

import java.util.Collection;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.domain.GeneralAtmosphericDatum;
import net.solarnetwork.node.domain.GeneralDayDatum;
import net.solarnetwork.node.domain.GeneralLocationDatum;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import org.joda.time.LocalDate;

/**
 * MetService implementation of a {@link GeneralAtmosphericDatum}
 * {@link DatumDataSource}.
 * 
 * <p>
 * This implementation reads public data available on the MetService website to
 * collect weather information.
 * </p>
 * 
 * @author matt
 * @version 2.0
 */
public class MetserviceWeatherDatumDataSource extends MetserviceSupport<GeneralAtmosphericDatum>
		implements DatumDataSource<GeneralLocationDatum>, SettingSpecifierProvider {

	@Override
	public Class<? extends GeneralLocationDatum> getDatumType() {
		return GeneralAtmosphericDatum.class;
	}

	@Override
	public GeneralLocationDatum readCurrentDatum() {
		Collection<GeneralLocationDatum> results = getClient().readCurrentLocalObservations(
				getLocationKey());
		GeneralAtmosphericDatum result = null;
		if ( results != null ) {
			for ( GeneralLocationDatum datum : results ) {
				if ( datum instanceof GeneralAtmosphericDatum ) {
					result = (GeneralAtmosphericDatum) datum;
					break;
				}
			}
		}

		if ( result != null && result.getSkyConditions() == null ) {
			Collection<GeneralDayDatum> forecast = getClient().readLocalForecast(getLocationKey());
			if ( forecast != null ) {
				LocalDate resultDate = new LocalDate(result.getCreated());
				for ( GeneralDayDatum day : forecast ) {
					LocalDate dayDate = new LocalDate(day.getCreated());
					if ( dayDate.getYear() == resultDate.getYear()
							&& dayDate.getDayOfYear() == resultDate.getDayOfYear() ) {
						result.setSkyConditions(day.getSkyConditions());
						break;
					}
				}
			}
		}

		return result;
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.weather.nz.metservice.weather";
	}

	@Override
	public String getDisplayName() {
		return "New Zealand Metservice weather information";
	}

	/**
	 * This method is here to preserve backwards compatibility with settings
	 * only.
	 * 
	 * @param localObsContainerKey
	 *        The container key.
	 * @deprecated No longer used.
	 */
	@Deprecated
	public void setLocalObsContainerKey(String localObsContainerKey) {
		// nothing here
	}

}
