
package net.solarnetwork.node.weather.ibm.wc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.solarnetwork.node.domain.GeneralDayDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

public class WCDayDatumDataSource extends WCSupport<GeneralDayDatum> {

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<SettingSpecifier>(1);
		result.add(new BasicTextFieldSettingSpecifier("uid", null));
		result.add(new BasicTextFieldSettingSpecifier("apiKey", null));
		result.add(new BasicTextFieldSettingSpecifier("datumPeriod", "7day"));
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
				this.getDatumPeriod());
	}

}
