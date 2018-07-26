
package net.solarnetwork.node.weather.ibm.wc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

public class WCHourlyDatumDataSource extends WCSupport<WCHourlyDatum> {

	private static final String[] DEFAULT_MENU = new String[] { "3day", "5day", "7day", "10day",
			"15day" };

	@Override
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

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.weather.ibm.wc.hour";
	}

	@Override
	public String getDisplayName() {
		return "IBM Weather Channel Hourly Weather";
	}

	@Override
	public Class<? extends WCHourlyDatum> getMultiDatumType() {
		return WCHourlyDatum.class;
	}

	@Override
	public Collection<WCHourlyDatum> readMultipleDatum() {
		// TODO Auto-generated method stub
		return this.getClient().readHourlyForecast(this.getLocationIdentifier(), this.getApiKey(),
				this.getDatumPeriod());
	}

	@Override
	public Class<? extends WCHourlyDatum> getDatumType() {
		return WCHourlyDatum.class;
	}

	@Override
	public WCHourlyDatum readCurrentDatum() {
		// TODO Auto-generated method stub
		return null;
	}
}
