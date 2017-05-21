/* ==================================================================
 * YrDayDatumDataSource.java - 21/05/2017 4:39:02 PM
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

package net.solarnetwork.node.weather.yr;

import java.util.Collection;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.domain.DayDatum;
import net.solarnetwork.node.domain.GeneralDayDatum;
import net.solarnetwork.node.settings.SettingSpecifierProvider;

/**
 * Yr implementation of a {@link DayDatum} {@link DatumDataSource}.
 * 
 * @author matt
 * @version 1.0
 */
public class YrDayDatumDataSource extends ConfigurableYrClientService<DayDatum>
		implements SettingSpecifierProvider, DatumDataSource<DayDatum>, MultiDatumDataSource<DayDatum> {

	@Override
	public Class<? extends DayDatum> getDatumType() {
		return GeneralDayDatum.class;
	}

	@Override
	public DayDatum readCurrentDatum() {
		// TODO: not supported yet
		return null;
	}

	@Override
	public Class<? extends DayDatum> getMultiDatumType() {
		return GeneralDayDatum.class;
	}

	@Override
	public Collection<DayDatum> readMultipleDatum() {
		return getClient().getTenDayForecast(getLocationIdentifier());
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.weather.yr.day";
	}

	@Override
	public String getDisplayName() {
		return "Yr day information";
	}

}
