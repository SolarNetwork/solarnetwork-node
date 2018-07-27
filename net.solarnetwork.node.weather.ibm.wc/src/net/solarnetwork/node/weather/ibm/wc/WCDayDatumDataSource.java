
package net.solarnetwork.node.weather.ibm.wc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.node.domain.GeneralDayDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

public class WCDayDatumDataSource extends WCSupport<GeneralDayDatum> {

	private static final String[] DEFAULT_MENU = new String[] { "3day", "5day", "7day", "10day",
			"15day" };

	@Override
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

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.weather.ibm.wc.day";
	}

	@Override
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

		return this.getClient().readDailyForecast(this.getLocationIdentifier(), this.getApiKey(),
				DailyDatumPeriod.getValue(this.getDatumPeriod()));
	}

}
