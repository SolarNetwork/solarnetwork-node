/* ==================================================================
 * OwmWeatherDatumDataSource.java - 17/09/2018 8:12:59 PM
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.domain.AtmosphericDatum;
import net.solarnetwork.node.domain.GeneralAtmosphericDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicMultiValueSettingSpecifier;

/**
 * OpenWeatherMap implementation of a {@link AtmosphericDatum}
 * {@link DatumDataSource}.
 * 
 * @author matt
 * @version 1.1
 */
public class OwmWeatherDatumDataSource extends ConfigurableOwmClientService<AtmosphericDatum>
		implements SettingSpecifierProvider, DatumDataSource<AtmosphericDatum>,
		MultiDatumDataSource<AtmosphericDatum> {

	private DataCollectionMode dataCollectionMode = DataCollectionMode.Mixed;

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.weather.owm.weather";
	}

	@Override
	public String getDisplayName() {
		return "OpenWeatherMap weather information";
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
		if ( dataCollectionMode == DataCollectionMode.Forecast ) {
			// only forecast desired
			return null;
		}
		return getClient().getCurrentConditions(getLocationIdentifier());
	}

	@Override
	public Collection<AtmosphericDatum> readMultipleDatum() {
		AtmosphericDatum curr = readCurrentDatum();
		Collection<AtmosphericDatum> forecast = null;
		if ( dataCollectionMode != DataCollectionMode.Observation ) {
			forecast = getClient().getHourlyForecast(getLocationIdentifier());
		}
		List<AtmosphericDatum> results = new ArrayList<AtmosphericDatum>(
				(forecast != null ? forecast.size() : 0) + 1);
		if ( curr != null ) {
			results.add(curr);
		}
		if ( forecast != null ) {
			results.addAll(forecast);
		}
		return results;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = super.getSettingSpecifiers();

		// drop-down menu for data collection mode
		BasicMultiValueSettingSpecifier modeSpec = new BasicMultiValueSettingSpecifier(
				"dataCollectionModeKey", String.valueOf(DataCollectionMode.Mixed.getKey()));
		Map<String, String> modeTitles = new LinkedHashMap<String, String>(2);
		for ( DataCollectionMode e : DataCollectionMode.values() ) {
			modeTitles.put(String.valueOf(e.getKey()), e.toDisplayString());
		}
		modeSpec.setValueTitles(modeTitles);
		results.add(modeSpec);

		return results;
	}

	/**
	 * Set the data collection mode.
	 * 
	 * @param dataCollectionMode
	 *        the data collection mode
	 * @since 1.1
	 */
	public void setDataCollectionMode(DataCollectionMode dataCollectionMode) {
		this.dataCollectionMode = dataCollectionMode;
	}

	/**
	 * Set the data collection mode as a key value.
	 * 
	 * @param key
	 *        the {@link DataCollectionMode#getKey()} value to set
	 * @since 1.1
	 */
	public void setDataCollectionModeKey(int key) {
		DataCollectionMode mode = DataCollectionMode.Mixed;
		try {
			mode = DataCollectionMode.forKey(key);
		} catch ( IllegalArgumentException e ) {
			// ignore and fall back to Mixed
		}
		setDataCollectionMode(mode);
	}

}
