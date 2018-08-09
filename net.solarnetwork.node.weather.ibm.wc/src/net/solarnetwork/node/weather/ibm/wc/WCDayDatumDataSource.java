
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
