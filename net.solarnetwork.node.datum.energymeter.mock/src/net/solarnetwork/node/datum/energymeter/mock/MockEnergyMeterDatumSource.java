package net.solarnetwork.node.datum.energymeter.mock;

import java.util.Date;
import java.util.List;

import org.springframework.context.MessageSource;

import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.support.DatumDataSourceSupport;

/**
 * Mock plugin to be the source of values for a GeneralNodeACEnergyDatum, the
 * datum will contain random values based around a settable center deviating by
 * a settable deviation.
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
	private String voltagerms = "230";
	private String frequency = "50";
	private String resistance = "10";
	private String inductance = "10";

	private MessageSource messageSource;

	private Integer apparantPow;
	private Float phaseVolt;
	private Float phaseCurr;
	private Integer watt;
	private Integer realPow;
	private Integer reacPow;
	private Float powfac;

	@Override
	public Class<? extends GeneralNodeACEnergyDatum> getDatumType() {
		return GeneralNodeACEnergyDatum.class;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.solarnetwork.node.DatumDataSource#readCurrentDatum()
	 * 
	 * Returns a {@link GeneralNodeACEnergyDatum} filled with mock data. The
	 * method uses reflection to find setters in {@link
	 * GeneralNodeACEnergyDatum} and fill them with a number equal to the
	 * baseReading +- a random number that goes upto deviation.
	 * 
	 * @return A {@link GeneralNodeACEnergyDatum}
	 * 
	 */
	@Override
	public GeneralNodeACEnergyDatum readCurrentDatum() {

		GeneralNodeACEnergyDatum datum = new GeneralNodeACEnergyDatum();
		datum.setCreated(new Date());
		datum.setSourceId(sourceId);

		datum.setFrequency(Float.parseFloat(this.frequency));
		datum.setVoltage(Float.parseFloat(this.voltagerms));
		calcVariables();
		datum.setReactivePower(this.reacPow);
		datum.setApparentPower(this.apparantPow);
		datum.setPhaseVoltage(this.phaseVolt);
		datum.setCurrent(this.phaseCurr);
		datum.setWatts(this.watt);
		datum.setRealPower(this.realPow);
		datum.setPowerFactor(this.powfac);
		return datum;
	}

	/**
	 * Calculates the values to feed the datum
	 */
	private void calcVariables() {
		double vrms = Double.parseDouble(voltagerms);

		// convention to use capital L for inductance reading in microhenry
		double L = Double.parseDouble(inductance) / 1000000;

		double f = Double.parseDouble(frequency);

		// convention to use capital R for resistance
		double R = Double.parseDouble(resistance);

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

	// Method get used by the settings page
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	// Method get used by the settings page
	public void setVoltage(String voltage) {
		try {
			Float reading = Float.parseFloat(voltage);
			this.voltagerms = reading.toString();
		} catch (NumberFormatException e) {
			// what was entered was not valid keep current value
		}

	}

	// Method get used by the settings page
	public void setResistance(String resistance) {
		try {
			Double reading = Double.parseDouble(resistance);
			this.resistance = reading.toString();
		} catch (NumberFormatException e) {
			// what was entered was not valid keep current value
		}

	}

	// Method get used by the settings page
	public void setInductance(String inductance) {
		try {
			Double reading = Double.parseDouble(inductance);
			this.inductance = reading.toString();
		} catch (NumberFormatException e) {
			// what was entered was not valid keep current value
		}

	}

	// Method get used by the settings page
	public void setFrequency(String frequency) {
		try {
			Float reading = Float.parseFloat(frequency);
			this.frequency = reading.toString();
		} catch (NumberFormatException e) {
			// what was entered was not valid keep current value
		}

	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.datum.energymeter.mock";
	}

	@Override
	public String getDisplayName() {
		return "Mock Meter";
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	// Puts the user configurable settings on the settings page
	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		MockEnergyMeterDatumSource defaults = new MockEnergyMeterDatumSource();
		List<SettingSpecifier> results = getIdentifiableSettingSpecifiers();

		// user enters text
		results.add(new BasicTextFieldSettingSpecifier("sourceId", defaults.sourceId));
		results.add(new BasicTextFieldSettingSpecifier("voltage", defaults.voltagerms));
		results.add(new BasicTextFieldSettingSpecifier("frequency", defaults.frequency));
		results.add(new BasicTextFieldSettingSpecifier("resistance", defaults.resistance));
		results.add(new BasicTextFieldSettingSpecifier("inductance", defaults.inductance));
		return results;
	}
}
