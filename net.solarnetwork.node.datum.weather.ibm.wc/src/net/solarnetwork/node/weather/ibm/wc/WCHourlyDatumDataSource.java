/* ===================================================================
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
 * ===================================================================
 */

package net.solarnetwork.node.weather.ibm.wc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.node.domain.datum.AtmosphericDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * The WCHourlyDatumDataSource class provides GUI settings and an entry point
 * for retrieving hourly datum.
 * 
 * @author matt frost
 * @version 2.0
 */
public class WCHourlyDatumDataSource extends WCSupport
		implements SettingSpecifierProvider, DatumDataSource, MultiDatumDataSource {

	public WCHourlyDatumDataSource() {
		super();
		setDatumPeriod(HourlyDatumPeriod.TWODAY.getPeriod());
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<SettingSpecifier>(1);
		result.add(new BasicTextFieldSettingSpecifier("uid", null));
		result.add(new BasicTextFieldSettingSpecifier("apiKey", null));
		BasicMultiValueSettingSpecifier menuSpec = new BasicMultiValueSettingSpecifier("datumPeriod",
				HourlyDatumPeriod.TWODAY.getPeriod());
		Map<String, String> menuValues = new LinkedHashMap<String, String>(
				HourlyDatumPeriod.values().length);
		for ( HourlyDatumPeriod p : HourlyDatumPeriod.values() ) {
			menuValues.put(p.getPeriod(), p.getPeriod());
		}
		menuSpec.setValueTitles(menuValues);
		result.add(menuSpec);
		result.add(new BasicTextFieldSettingSpecifier("locationIdentifier", null));
		return result;
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.weather.ibm.wc.hour";
	}

	@Override
	public String getDisplayName() {
		return "IBM Weather Channel Hourly Weather";
	}

	@Override
	public Class<? extends NodeDatum> getDatumType() {
		return AtmosphericDatum.class;
	}

	@Override
	public NodeDatum readCurrentDatum() {
		Collection<NodeDatum> results = readMultipleDatum();
		return (results.isEmpty() ? null : results.iterator().next());
	}

	@Override
	public Class<? extends NodeDatum> getMultiDatumType() {
		return AtmosphericDatum.class;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Collection<NodeDatum> readMultipleDatum() {
		return (Collection) this.getClient().readHourlyForecast(this.getLocationIdentifier(),
				this.getApiKey(), HourlyDatumPeriod.forPeriod(this.getDatumPeriod()));
	}

}
