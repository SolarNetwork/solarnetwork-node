
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
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

public class WCHourlyDatumDataSource extends WCSupport<WCHourlyDatum> implements
		SettingSpecifierProvider, DatumDataSource<WCHourlyDatum>, MultiDatumDataSource<WCHourlyDatum> {

	private static final String[] DEFAULT_MENU = new String[] { "6hour", "12hour", "1day", "2day",
			"3day", "10day", "15day" };

	private final Logger log = LoggerFactory.getLogger(getClass());

	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<SettingSpecifier>(1);
		result.add(new BasicTextFieldSettingSpecifier("uid", null));
		result.add(new BasicTextFieldSettingSpecifier("apiKey", null));
		BasicMultiValueSettingSpecifier menuSpec = new BasicMultiValueSettingSpecifier("datumPeriod",
				"2day");
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
		return "net.solarnetwork.node.weather.ibm.wc.hour";
	}

	public String getDisplayName() {
		return "IBM Weather Channel Hourly Weather";
	}

	@Override
	public Class<? extends WCHourlyDatum> getMultiDatumType() {
		return WCHourlyDatum.class;
	}

	@Override
	public Collection<WCHourlyDatum> readMultipleDatum() {
		if ( this.getLocationIdentifier() != null && this.getApiKey() != null
				&& this.getDatumPeriod() != null ) {
			return this.getClient().readHourlyForecast(this.getLocationIdentifier(), this.getApiKey(),
					HourlyDatumPeriod.forPeriod(this.getDatumPeriod()));
		}
		log.error("Unable to retrieve datum because of incorrect configuration");
		return null;
	}

	@Override
	public Class<? extends WCHourlyDatum> getDatumType() {
		return WCHourlyDatum.class;
	}

	@Override
	public WCHourlyDatum readCurrentDatum() {
		return null;
	}
}
