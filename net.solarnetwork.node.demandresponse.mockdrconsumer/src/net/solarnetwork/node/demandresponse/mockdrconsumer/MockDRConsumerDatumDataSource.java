package net.solarnetwork.node.demandresponse.mockdrconsumer;

import java.util.Date;
import java.util.List;

import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.support.DatumDataSourceSupport;

public class MockDRConsumerDatumDataSource extends DatumDataSourceSupport
		implements SettingSpecifierProvider, DatumDataSource<GeneralNodeACEnergyDatum> {

	private Integer minwatts = 0;
	private Integer maxwatts = 10;
	private Integer energycost = 1;
	private Integer watts = 0;
	private String drsource = "";

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.demandresponse.mockdrconsumer";
	}

	@Override
	public String getDisplayName() {
		return "Mock DR Device";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		MockDRConsumerDatumDataSource defaults = new MockDRConsumerDatumDataSource();
		List<SettingSpecifier> results = super.getIdentifiableSettingSpecifiers();

		// user enters text
		results.add(new BasicTextFieldSettingSpecifier("minwatts", defaults.minwatts.toString()));
		results.add(new BasicTextFieldSettingSpecifier("maxwatts", defaults.maxwatts.toString()));
		results.add(new BasicTextFieldSettingSpecifier("energycost", defaults.energycost.toString()));
		results.add(new BasicTextFieldSettingSpecifier("drsource", defaults.drsource));
		return results;
	}

	public Integer getMinwatts() {
		return minwatts;
	}

	public void setMinwatts(Integer minwatts) {
		if (watts < minwatts) {
			watts = minwatts;
		}
		this.minwatts = minwatts;
	}

	public Integer getMaxwatts() {
		return maxwatts;
	}

	public void setMaxwatts(Integer maxwatts) {
		if (watts > maxwatts) {
			watts = maxwatts;
		}
		this.maxwatts = maxwatts;
	}

	public Integer getEnergycost() {
		return energycost;
	}

	public void setEnergycost(Integer energycost) {
		this.energycost = energycost;
	}

	// This is the sourceID of the DRAnouncer to which the device will follow
	public String getDrsource() {
		return drsource;
	}

	public void setDrsource(String drsource) {
		this.drsource = drsource;
	}

	// protected as this is not set via the settings page but instead via demand
	// response
	protected void setWatts(Integer watts) {
		this.watts = watts;
	}

	public Integer getWatts() {
		return watts;
	}

	@Override
	public Class<? extends GeneralNodeACEnergyDatum> getDatumType() {
		return GeneralNodeACEnergyDatum.class;
	}

	@Override
	public GeneralNodeACEnergyDatum readCurrentDatum() {
		GeneralNodeACEnergyDatum datum = new GeneralNodeACEnergyDatum();
		datum.setCreated(new Date());
		datum.setSourceId(getUID());
		datum.setWatts(getWatts());
		return datum;
	}

}
