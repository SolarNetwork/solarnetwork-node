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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.domain.GeneralDayDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * The WCDayDatumDataSource class provides GUI settings and an entry point for
 * retrieving daily datum.
 * 
 * @author matt frost
 *
 */
public class WCDayDatumDataSource extends WCSupport<GeneralDayDatum>
		implements DatumDataSource<GeneralDayDatum>, MultiDatumDataSource<GeneralDayDatum>,
		SettingSpecifierProvider {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private static final String[] DEFAULT_MENU = new String[] { "3day", "5day", "7day", "10day",
			"15day" };

	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<SettingSpecifier>(1);
		result.add(new BasicTextFieldSettingSpecifier("uid", null));
		result.add(new BasicTextFieldSettingSpecifier("apiKey", null));
		BasicMultiValueSettingSpecifier menuSpec = new BasicMultiValueSettingSpecifier("datumPeriod",
				"7day");
		Map<String, String> menuValues = new LinkedHashMap<String, String>(DEFAULT_MENU.length);
		for ( String s : DEFAULT_MENU ) {
			menuValues.put(s, s);
		}
		menuSpec.setValueTitles(menuValues);
		result.add(menuSpec);
		result.add(new BasicTextFieldSettingSpecifier("locationIdentifier", null));
		return result;
	}

	public String getSettingUID() {
		return "net.solarnetwork.node.weather.ibm.wc.day";
	}

	public String getDisplayName() {
		return "IBM daily weather information";
	}

	@Override
	public Class<? extends GeneralDayDatum> getDatumType() {
		return GeneralDayDatum.class;
	}

	@Override
	public GeneralDayDatum readCurrentDatum() {

		return null;
	}

	@Override
	public Class<? extends GeneralDayDatum> getMultiDatumType() {
		return GeneralDayDatum.class;
	}

	@Override
	public Collection<GeneralDayDatum> readMultipleDatum() {

		if ( this.getLocationIdentifier() != null && this.getApiKey() != null
				&& this.getDatumPeriod() != null ) {
			return this.getClient().readDailyForecast(this.getLocationIdentifier(), this.getApiKey(),
					DailyDatumPeriod.forPeriod(this.getDatumPeriod()));
		}
		log.error("Unable to retrieve datum because of incorrect configuration");

		return null;
	}

}
