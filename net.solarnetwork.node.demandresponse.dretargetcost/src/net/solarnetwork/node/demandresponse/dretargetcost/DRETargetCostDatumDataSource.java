package net.solarnetwork.node.demandresponse.dretargetcost;

import java.util.List;

import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.domain.GeneralNodeEnergyStorageDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.support.DatumDataSourceSupport;

public class DRETargetCostDatumDataSource extends DatumDataSourceSupport
		implements SettingSpecifierProvider, DatumDataSource<GeneralNodeEnergyStorageDatum> {
	private Integer energyCost = 10;

	// we will use this value as a means to calebrate the DR a lower value means
	// more likly to turn things off. The goal is to get the cost of powering
	// devices as close to this value without going over.
	private Integer drtargetCost = 10;

	private DRETargetCost drengine;

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.demandresponse.dretargetcost";
	}

	@Override
	public String getDisplayName() {
		return "DR Engine";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = getIdentifiableSettingSpecifiers();
		DRETargetCostDatumDataSource defaults = new DRETargetCostDatumDataSource();

		// cost per watt hour for grid energy
		results.add(new BasicTextFieldSettingSpecifier("energyCost", defaults.energyCost.toString()));

		// the target cost per hour. This strategy tries to preform a demand
		// response to get closest without going over
		results.add(new BasicTextFieldSettingSpecifier("drtargetCost", defaults.drtargetCost.toString()));

		return results;
	}

	public Integer getEnergyCost() {
		return energyCost;
	}

	public void setEnergyCost(Integer energyCost) {
		this.energyCost = energyCost;

	}

	public Integer getDrtargetCost() {
		return drtargetCost;
	}

	public void setDrtargetCost(Integer drtargetCost) {
		this.drtargetCost = drtargetCost;

	}

	public DRETargetCost getDRInstance() {
		return drengine;
	}

	public void setDRInstance(DRETargetCost linkedInstance) {
		this.drengine = linkedInstance;
		linkedInstance.setSettings(this);
	}

	@Override
	public Class<? extends GeneralNodeEnergyStorageDatum> getDatumType() {
		return GeneralNodeEnergyStorageDatum.class;
	}

	@Override
	public GeneralNodeEnergyStorageDatum readCurrentDatum() {
		try {
			drengine.drupdate();
		} catch (RuntimeException e) {
			// the try catch is only for debugging I have noticed that
			e.printStackTrace();
		}

		// the datum will contain num devices as well as watts cost?
		GeneralNodeEnergyStorageDatum datum = new GeneralNodeEnergyStorageDatum();
		datum.putInstantaneousSampleValue("Num Devices", drengine.getNumdrdevices());
		datum.setSourceId(getUID());
		return datum;
	}

}
