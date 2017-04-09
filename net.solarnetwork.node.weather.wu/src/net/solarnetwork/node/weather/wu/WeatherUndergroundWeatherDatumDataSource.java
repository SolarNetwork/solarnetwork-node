/* ==================================================================
 * WeatherUndergroundWeatherDatumDataSource.java - Apr 9, 2017 4:49:10 PM
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

import java.util.Collection;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.domain.AtmosphericDatum;
import net.solarnetwork.node.domain.GeneralAtmosphericDatum;
import net.solarnetwork.node.settings.SettingSpecifierProvider;

/**
 * Weather Underground implementation of a {@link AtmosphericDatum}
 * {@link DatumDataSource}.
 * 
 * @author matt
 * @version 1.0
 */
public class WeatherUndergroundWeatherDatumDataSource
		extends ConfigurableWeatherUndergroundClientService<AtmosphericDatum>
		implements DatumDataSource<AtmosphericDatum>, MultiDatumDataSource<AtmosphericDatum>,
		SettingSpecifierProvider {

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.weather.wu.weather";
	}

	@Override
	public String getDisplayName() {
		return "Weather Underground weather information";
	}

	@Override
	public Class<? extends AtmosphericDatum> getDatumType() {
		return GeneralAtmosphericDatum.class;
	}

	@Override
	public Class<? extends AtmosphericDatum> getMultiDatumType() {
		return GeneralAtmosphericDatum.class;
	}

	@Override
	public AtmosphericDatum readCurrentDatum() {
		return getClient().getCurrentConditions(getLocationIdentifier());
	}

	@Override
	public Collection<AtmosphericDatum> readMultipleDatum() {
		return getClient().getHourlyForecast(getLocationIdentifier());
	}

}
