/* ==================================================================
 * MockMeterDataSource.java - 10/06/2015 1:28:07 pm
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.node.datum.energymeter.mock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.domain.ACEnergyDatum;
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
 * @version 1.1
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
	private Double resistanceDeviation = 0.0;
	private Double inductanceDeviation = 0.0;

	private Random rng = new Random();

	private final AtomicReference<GeneralNodeACEnergyDatum> lastsample = new AtomicReference<GeneralNodeACEnergyDatum>();

	/**
	 * Get a mock starting value for our meter based on the current time so the
	 * meter back to zero each time the app restarts.
	 * 
	 * @return a starting meter value
	 */
	private static long meterStartValue() {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		Date now = cal.getTime();
		cal.set(2010, cal.getMinimum(Calendar.MONTH), 1, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return (now.getTime() - cal.getTimeInMillis()) / (1000L * 60);
	}

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
		GeneralNodeACEnergyDatum prev = this.lastsample.get();
		GeneralNodeACEnergyDatum datum = new GeneralNodeACEnergyDatum();
		datum.setCreated(new Date());
		datum.setSourceId(sourceId);

		// the values for most datum variables are calculated here
		calcVariables(datum);

		calcWattHours(prev, datum);

		this.lastsample.compareAndSet(prev, datum);

		postDatumCapturedEvent(datum);

		return datum;
	}

	private double readVoltage() {
		return voltagerms + (randomness ? voltDeviation : 0) * Math.cos(Math.PI * rng.nextDouble());
	}

	private double readFrequency() {
		return frequency + (randomness ? freqDeviation : 0) * Math.cos(Math.PI * rng.nextDouble());
	}

	private double readResistance() {
		return resistance
				+ (randomness ? resistanceDeviation : 0) * Math.cos(Math.PI * rng.nextDouble());
	}

	private double readInductance() {
		return inductance
				+ (randomness ? inductanceDeviation : 0) * Math.cos(Math.PI * rng.nextDouble());
	}

	/**
	 * Calculates the values to feed the datum.
	 * 
	 * @param vrms
	 *        the voltage to use
	 * @param f
	 *        the frequency to use
	 */
	private void calcVariables(GeneralNodeACEnergyDatum datum) {
		double vrms = readVoltage();
		datum.setVoltage((float) vrms);

		double f = readFrequency();
		datum.setFrequency((float) f);

		// convention to use capital L for inductance reading in microhenry
		double L = readInductance() / 1000000;

		// convention to use capital R for resistance
		double R = readResistance();

		double vmax = Math.sqrt(2) * vrms;

		double phasevoltage = vmax * Math.sin(2 * Math.PI * f * System.currentTimeMillis() / 1000);
		datum.setPhaseVoltage((float) phasevoltage);

		double inductiveReactance = 2 * Math.PI * f * L;
		double impedance = Math.sqrt(Math.pow(R, 2) + Math.pow(inductiveReactance, 2));

		double phasecurrent = phasevoltage / impedance;
		datum.setCurrent((float) phasecurrent);
		datum.putInstantaneousSampleValue(ACEnergyDatum.CURRENT_KEY,
				BigDecimal.valueOf(phasecurrent).setScale(6, RoundingMode.HALF_UP));
		double current = vrms / impedance;

		double reactivePower = Math.pow(current, 2) * inductiveReactance;
		datum.setReactivePower((int) reactivePower);
		double realPower = Math.pow(current, 2) * R;
		datum.setRealPower((int) realPower);
		datum.setApparentPower((int) (Math.pow(current, 2) * impedance));

		// not sure if correct calculation
		double watts = Math.pow(phasecurrent, 2) * R;
		datum.setWatts((int) watts);

		double phaseAngle = Math.atan(inductiveReactance / R);
		datum.putInstantaneousSampleValue(ACEnergyDatum.POWER_FACTOR_KEY,
				BigDecimal.valueOf(Math.cos(phaseAngle)).setScale(8, RoundingMode.HALF_UP));
	}

	private void calcWattHours(GeneralNodeACEnergyDatum prev, GeneralNodeACEnergyDatum datum) {
		if ( prev == null ) {
			datum.setWattHourReading(meterStartValue());
		} else {
			double diffHours = ((datum.getCreated().getTime() - prev.getCreated().getTime())
					/ (double) (1000 * 60 * 60));
			long wh = (long) (datum.getRealPower() * diffHours);
			long newWh = prev.getWattHourReading() + wh;
			datum.setWattHourReading(newWh);
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

	public void setResistanceDeviation(Double resistanceDeviation) {
		this.resistanceDeviation = resistanceDeviation;
	}

	public void setInductanceDeviation(Double inductanceDeviation) {
		this.inductanceDeviation = inductanceDeviation;
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
		results.add(new BasicTextFieldSettingSpecifier("resistanceDeviation",
				defaults.resistanceDeviation.toString()));
		results.add(new BasicTextFieldSettingSpecifier("inductanceDeviation",
				defaults.inductanceDeviation.toString()));
		return results;
	}
}
