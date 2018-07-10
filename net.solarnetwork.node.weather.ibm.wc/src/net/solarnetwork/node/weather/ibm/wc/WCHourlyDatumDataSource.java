package net.solarnetwork.node.weather.ibm.wc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

public class WCHourlyDatumDataSource extends WCSupport<WCHourlyDatum>{
	
	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<SettingSpecifier>(1);
		result.add(new BasicTextFieldSettingSpecifier("uid", null));
		result.add(new BasicTextFieldSettingSpecifier("apiKey", null));
		result.add(new BasicTextFieldSettingSpecifier("datumPeriod", "2day"));
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
		return this.getClient().readHourlyForecast(this.getLocationIdentifier(), this.getApiKey(), this.getDatumPeriod());
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
