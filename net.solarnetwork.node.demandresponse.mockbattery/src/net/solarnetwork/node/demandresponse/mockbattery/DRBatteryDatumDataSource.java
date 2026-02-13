package net.solarnetwork.node.demandresponse.mockbattery;

import java.util.Date;
import java.util.List;

import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.domain.GeneralNodeEnergyStorageDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.support.DatumDataSourceSupport;

/**
 * DatumDataSource for the Demand response battery. When datums are read
 * calculates the current battery charge.
 * 
 * @author robert
 *
 */
public class DRBatteryDatumDataSource extends DatumDataSourceSupport
		implements SettingSpecifierProvider, DatumDataSource<GeneralNodeEnergyStorageDatum> {

	// Default values
	private Double maxCharge = 10.0;
	private Double charge = 10.0;
	private Integer cost = 1000;
	private Integer cycles = 10000;
	private String drEngineName = "DREngine";
	private Double maxDraw = 1.0;
	private Double maxChargeDraw = 1.0;

	// The settings the user configures will be applied to this mockbattery
	private MockBattery mockbattery;

	@Override
	public Class<? extends GeneralNodeEnergyStorageDatum> getDatumType() {
		return GeneralNodeEnergyStorageDatum.class;
	}

	@Override
	public GeneralNodeEnergyStorageDatum readCurrentDatum() {

		// create new datum
		GeneralNodeEnergyStorageDatum datum = new GeneralNodeEnergyStorageDatum();
		datum.setCreated(new Date());
		datum.setSourceId(getUID());

		// populate the datum with values from the battery
		MockBattery mb = getMockBattery();
		datum.setAvailableEnergy((long) mb.readCharge());
		datum.setAvailableEnergyPercentage(mb.capacityFraction());

		// Status indication on the datum
		if (mb.readDraw() == 0) {
			datum.putStatusSampleValue("Mode", "Idle");
		} else if (mb.readDraw() > 0) {
			datum.putStatusSampleValue("Mode", "Discharging");
		} else {
			datum.putStatusSampleValue("Mode", "Charging");
		}

		return datum;

	}

	public String getDREngineName() {
		return drEngineName;
	}

	public void setDrEngineName(String drEngineName) {
		this.drEngineName = drEngineName;
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.demandresponse.mockbattery";
	}

	@Override
	public String getDisplayName() {
		return "Mock DR Battery";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = super.getIdentifiableSettingSpecifiers();
		DRBatteryDatumDataSource defaults = new DRBatteryDatumDataSource();
		results.add(new BasicTextFieldSettingSpecifier("batteryMaxCharge", defaults.maxCharge.toString()));
		results.add(new BasicTextFieldSettingSpecifier("maxDraw", defaults.maxDraw.toString()));
		results.add(new BasicTextFieldSettingSpecifier("maxChargeDraw", defaults.maxChargeDraw.toString()));
		results.add(new BasicTextFieldSettingSpecifier("batteryCharge", defaults.charge.toString()));
		results.add(new BasicTextFieldSettingSpecifier("batteryCost", defaults.cost.toString()));
		results.add(new BasicTextFieldSettingSpecifier("batteryCycles", defaults.cycles.toString()));

		// Display the calculated energycost on the screen
		// Limitation is you need to refresh the settings page for it to update
		results.add(new BasicTitleSettingSpecifier("energyCost", calcCost(), true));

		// SourceID of the DREngine to accept demand response from
		results.add(new BasicTextFieldSettingSpecifier("drEngineName", defaults.drEngineName));
		return results;
	}

	// configured in OSGI
	public void setMockBattery(MockBattery mockbattery) {
		this.mockbattery = mockbattery;
	}

	public MockBattery getMockBattery() {
		return this.mockbattery;
	}

	public void setBatteryMaxCharge(Double charge) {
		if (charge != null) {
			mockbattery.setMax(charge);
		}
		this.maxCharge = charge;
	}

	public Double getBatteryMaxCharge() {
		return maxCharge;
	}

	public Double getMaxDraw() {
		return maxDraw;
	}

	public void setMaxDraw(Double maxDraw) {
		this.maxDraw = maxDraw;
		// the mock battery does not have a concept of max draw so this method
		// needs to handle the logic
		if (mockbattery.readDraw() > maxDraw) {
			mockbattery.setDraw(maxDraw);
		}
	}

	public Double getMaxChargeDraw() {
		return maxChargeDraw;
	}

	public void setMaxChargeDraw(Double maxChargeDraw) {
		this.maxChargeDraw = maxChargeDraw;
	}

	public void setBatteryCharge(Double charge) {
		if (charge != null) {
			mockbattery.setCharge(charge);
		}
		this.charge = charge;
	}

	// note this returns the charge value the user set in settings page not the
	// current charge
	// of the battery
	public Double getBatteryCharge() {
		return this.charge;
	}

	public Integer getBatteryCost() {
		return cost;
	}

	public void setBatteryCost(Integer cost) {
		this.cost = cost;
	}

	public Integer getBatteryCycles() {
		return cycles;
	}

	public void setBatteryCycles(Integer cycles) {
		this.cycles = cycles;
	}

	// Used for showing the depreciation cost on the settings page,
	private String calcCost() {
		return new Double(
				(getBatteryCost().doubleValue() / (getBatteryCycles().doubleValue() * getBatteryMaxCharge() * 2.0)))
						.toString();
	}

}
