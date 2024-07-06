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

import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;
import net.solarnetwork.domain.BasicDeviceInfo;
import net.solarnetwork.domain.DeviceInfo;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.AcEnergyDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleAcEnergyDatum;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;

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
 * @version 1.6
 */
public class MockEnergyMeterDatumSource extends DatumDataSourceSupport
		implements DatumDataSource, SettingSpecifierProvider {

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

	private final AtomicReference<AcEnergyDatum> lastsample = new AtomicReference<>();

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
	public Class<? extends NodeDatum> getDatumType() {
		return AcEnergyDatum.class;
	}

	/***
	 * Returns an {@link AcEnergyDatum} the data in the datum is the state of
	 * the simulated circuit.
	 *
	 * @see net.solarnetwork.node.service.DatumDataSource#readCurrentDatum()
	 * @return an {@link AcEnergyDatum}
	 */
	@Override
	public NodeDatum readCurrentDatum() {
		AcEnergyDatum prev = this.lastsample.get();
		AcEnergyDatum datum = new SimpleAcEnergyDatum(resolvePlaceholders(sourceId), Instant.now(),
				new DatumSamples());

		// the values for most datum variables are calculated here
		calcVariables(datum);

		calcWattHours(prev, datum);

		this.lastsample.compareAndSet(prev, datum);

		NodeDatum result = applyDatumFilter(datum, null);

		return result;
	}

	@Override
	public String deviceInfoSourceId() {
		return resolvePlaceholders(sourceId);
	}

	@Override
	public DeviceInfo deviceInfo() {
		// @formatter:off
		return BasicDeviceInfo.builder()
				.withName("Mock Energy Meter")
				.withManufacturer("SolarNetwork")
				.withModelName("Simutron")
				.withSerialNumber("ABCDEF123")
				.withVersion("1.4")
				.withManufactureDate(LocalDate.of(2021, 7, 9))
				.withDeviceAddress("localhost").build();
		// @formatter:on
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
	private void calcVariables(AcEnergyDatum datum) {
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
		datum.asMutableSampleOperations().putSampleValue(Instantaneous, AcEnergyDatum.CURRENT_KEY,
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
		datum.asMutableSampleOperations().putSampleValue(Instantaneous, AcEnergyDatum.POWER_FACTOR_KEY,
				BigDecimal.valueOf(Math.cos(phaseAngle)).setScale(8, RoundingMode.HALF_UP));
	}

	private void calcWattHours(AcEnergyDatum prev, AcEnergyDatum datum) {
		if ( prev == null ) {
			datum.setWattHourReading(meterStartValue());
		} else {
			double diffHours = prev.getTimestamp().until(datum.getTimestamp(), ChronoUnit.MILLIS)
					/ (double) (1000 * 60 * 60);
			long wh = (long) (datum.getRealPower() * diffHours);
			long newWh = prev.getWattHourReading() + wh;
			datum.setWattHourReading(newWh);
		}
	}

	public String getSourceId() {
		return sourceId;
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
	 *        the random number generator
	 */
	public void setRNG(Random rng) {
		this.rng = rng;
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.energymeter.mock";
	}

	@Override
	public String getDisplayName() {
		return "Mock Meter";
	}

	@Override
	public Collection<String> publishedSourceIds() {
		final String sourceId = getSourceId();
		return (sourceId == null || sourceId.isEmpty() ? Collections.emptyList()
				: Collections.singleton(sourceId));
	}

	// Puts the user configurable settings on the settings page
	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		MockEnergyMeterDatumSource defaults = new MockEnergyMeterDatumSource();
		List<SettingSpecifier> results = getIdentifiableSettingSpecifiers();
		results.addAll(getDeviceInfoMetadataSettingSpecifiers());

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
