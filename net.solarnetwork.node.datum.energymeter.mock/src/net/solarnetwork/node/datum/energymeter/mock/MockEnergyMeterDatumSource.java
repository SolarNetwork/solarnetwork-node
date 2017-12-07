package net.solarnetwork.node.datum.energymeter.mock;

import java.util.Date;
import java.util.List;
import java.util.Random;

import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.node.support.DatumDataSourceSupport;

/**
 * Mock plugin to be the source of values for a GeneralNodeACEnergyDatum, this
 * mock tries to simulate a AC circuit containing a resister and inductor in
 * series.
 * 
 * <p>
 * This class implements {@link SettingSpecifierProvider} and
 * {@link DatumDataSource}
 * </p>
 * 
 * @author robert
 * @version 1.0
 */

public class MockEnergyMeterDatumSource extends DatumDataSourceSupport
		implements DatumDataSource<GeneralNodeACEnergyDatum>, SettingSpecifierProvider {

	// default values
	private String sourceId = "Mock Energy Meter";
	private Double voltagerms = 230.0;
	private Double frequency = 50.0;
	private Double resistance = 10.0;
	private Double inductance = 10.0;
	private Boolean randomness = false;
	private Double freqDeviation = 0.0;
	private Double voltDeviation = 0.0;

	private double randomVolt;
	private double randomFreq;

	private Integer apparantPow;
	private Float phaseVolt;
	private Float phaseCurr;
	private Integer watt;
	private Integer realPow;
	private Integer reacPow;
	private Float powfac;
	private Long watthours;
	private Long wattmillis;

	private GeneralNodeACEnergyDatum lastsample = null;
	private GeneralNodeACEnergyDatum currentsample = null;

	private Random rng = new Random();

	@Override
	public Class<? extends GeneralNodeACEnergyDatum> getDatumType() {
		return GeneralNodeACEnergyDatum.class;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.solarnetwork.node.DatumDataSource#readCurrentDatum()
	 * 
	 * Returns a {@link GeneralNodeACEnergyDatum} the data in the datum is the
	 * state of the simulated circuit.
	 * 
	 * @return A {@link GeneralNodeACEnergyDatum}
	 * 
	 */
	@Override
	public GeneralNodeACEnergyDatum readCurrentDatum() {

		this.lastsample = this.currentsample;

		GeneralNodeACEnergyDatum datum = new GeneralNodeACEnergyDatum();
		datum.setCreated(new Date());

		this.currentsample = datum;

		// applies randomness to voltage and frequency if randomness is on
		calcRandomness();

		// the values for most datum variables are calculated here
		calcVariables();

		// calculates the watt hours
		calcWattHours();

		datum.setCreated(new Date());
		datum.setSourceId(sourceId);
		datum.setFrequency((float) this.randomFreq);
		datum.setVoltage((float) this.randomVolt);
		datum.setReactivePower(this.reacPow);
		datum.setApparentPower(this.apparantPow);
		datum.setPhaseVoltage(this.phaseVolt);
		datum.setCurrent(this.phaseCurr);
		datum.setWatts(this.watt);
		datum.setRealPower(this.realPow);
		datum.setPowerFactor(this.powfac);
		datum.setWattHourReading(this.watthours);
		return datum;
	}

	private void calcRandomness() {
		this.randomVolt = voltagerms;
		this.randomFreq = frequency;

		// if randomness is off randomVolt and randomFreq will have no deviation
		if (randomness) {

			// add deviation to the supply
			double vd = this.voltDeviation;
			double fd = this.freqDeviation;
			this.randomVolt += vd * Math.cos(Math.PI * rng.nextDouble());
			this.randomFreq += fd * Math.cos(Math.PI * rng.nextDouble());
		}
	}

	/**
	 * Calculates the values to feed the datum
	 */
	private void calcVariables() {
		double vrms = this.randomVolt;

		// convention to use capital L for inductance reading in microhenry
		double L = inductance / 1000000;

		double f = this.randomFreq;

		// convention to use capital R for resistance
		double R = resistance;

		double vmax = Math.sqrt(2) * vrms;

		double phasevoltage = vmax * Math.sin(2 * Math.PI * f * System.currentTimeMillis() / 1000);
		this.phaseVolt = (float) phasevoltage;

		double inductiveReactance = 2 * Math.PI * f * L;
		double impedance = Math.sqrt(Math.pow(R, 2) + Math.pow(inductiveReactance, 2));

		double phasecurrent = phasevoltage / impedance;
		this.phaseCurr = (float) phasecurrent;
		double current = vrms / impedance;

		double reactivePower = Math.pow(current, 2) * inductiveReactance;
		this.reacPow = (int) reactivePower;
		double realPower = Math.pow(current, 2) * R;
		this.realPow = (int) realPower;
		this.apparantPow = (int) (Math.pow(current, 2) * impedance);

		// not sure if correct calculation
		double watts = Math.pow(phasecurrent, 2) * R;
		this.watt = (int) watts;

		double phaseAngle = Math.atan(inductiveReactance / R);
		this.powfac = (float) Math.cos(phaseAngle);

	}

	private void calcWattHours() {
		if (this.lastsample == null) {
			this.wattmillis = 0L;
			this.watthours = 0L;
		} else {
			Long diff = (this.currentsample.getCreated().getTime() - this.lastsample.getCreated().getTime());
			this.wattmillis += this.realPow.longValue() * diff;
			this.watthours = this.wattmillis / 1000 / 60 / 60;
		}

	}

	// Method get used by the settings page
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	// Method get used by the settings page
	public void setVoltage(Double voltage) {
		this.voltagerms = voltage;

	}

	// Method get used by the settings page
	public void setResistance(Double resistance) {
		this.resistance = resistance;

	}

	// Method get used by the settings page
	public void setInductance(Double inductance) {
		this.inductance = inductance;

	}

	// Method get used by the settings page
	public void setFrequency(Double frequency) {
		this.frequency = frequency;

	}

	// Method get used by the settings page
	public void setVoltdev(Double voltdev) {
		this.voltDeviation = voltdev;

	}

	// Method get used by the settings page
	public void setFreqdev(Double freqdev) {
		this.freqDeviation = freqdev;

	}

	public void setRandomness(Boolean random) {
		this.randomness = random;
	}

	/**
	 * Dependency injection of a Random instance to improve controllability if
	 * needed eg unit testing.
	 * 
	 * @param rng
	 */
	public void setRNG(Random rng) {
		this.rng = rng;
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.datum.energymeter.mock";
	}

	@Override
	public String getDisplayName() {
		return "Mock Meter";
	}

	// Puts the user configurable settings on the settings page
	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		MockEnergyMeterDatumSource defaults = new MockEnergyMeterDatumSource();
		List<SettingSpecifier> results = getIdentifiableSettingSpecifiers();

		// user enters text
		results.add(new BasicTextFieldSettingSpecifier("sourceId", defaults.sourceId));
		results.add(new BasicTextFieldSettingSpecifier("voltage", defaults.voltagerms.toString()));
		results.add(new BasicTextFieldSettingSpecifier("frequency", defaults.frequency.toString()));
		results.add(new BasicTextFieldSettingSpecifier("resistance", defaults.resistance.toString()));
		results.add(new BasicTextFieldSettingSpecifier("inductance", defaults.inductance.toString()));
		results.add(new BasicToggleSettingSpecifier("randomness", defaults.randomness));
		results.add(new BasicTextFieldSettingSpecifier("voltdev", defaults.voltDeviation.toString()));
		results.add(new BasicTextFieldSettingSpecifier("freqdev", defaults.freqDeviation.toString()));
		return results;
	}
}
