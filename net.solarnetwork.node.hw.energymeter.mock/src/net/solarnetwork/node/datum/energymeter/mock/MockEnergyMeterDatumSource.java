package net.solarnetwork.node.datum.energymeter.mock;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

import org.springframework.context.MessageSource;

import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;
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

		Class<GeneralNodeACEnergyDatum> gacdatum = GeneralNodeACEnergyDatum.class;

		Method[] gacdMethods = gacdatum.getMethods();
		for (Method m : gacdMethods) {

			// check if method is setter
			if (m.getName().toLowerCase().startsWith("set")) {
				Class<?>[] args = m.getParameterTypes();

				// only 1 thing to set
				if (args.length == 1) {
					Class<?> arg = args[0];

					// check we have a number
					if (Number.class.isAssignableFrom(arg)) {
						Number result;
						if (deviate) {
							result = (Math.cos(Math.random() * Math.PI) * Double.parseDouble(deviation)
									+ Double.parseDouble(baseReading));
						} else {
							result = Double.parseDouble(baseReading);
						}

						// get the number in the correct format
						Object setterarg = null;
						if (arg.equals(Double.class)) {
							setterarg = result;
						} else if (arg.equals(Float.class)) {
							setterarg = result.floatValue();
						} else if (arg.equals(Integer.class)) {
							setterarg = result.intValue();
						}

						// if the setter is of the type we support invoke it
						if (setterarg != null) {
							try {
								m.invoke(datum, setterarg);

								// These exceptions should not get thrown due to
								// all of my checks but if they do
								// thats a mistake on my part, print the
								// stacktrace.
							} catch (IllegalAccessException e) {
								e.printStackTrace();
							} catch (IllegalArgumentException e) {
								e.printStackTrace();
							} catch (InvocationTargetException e) {
								e.printStackTrace();
							}
						}

					}
				}
			}
		}

		return datum;
	}

	// default values
	private String sourceId = "Mock Energy Meter";

	private String baseReading = "1000";

	private String deviation = "200";

	private Boolean deviate = true;

	// Method get used by the settings page
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	// Method get used by the settings page
	public void setBaseReading(String baseReading) {
		try {
			Double reading = Double.parseDouble(baseReading);
			this.baseReading = reading.toString();
		} catch (NumberFormatException e) {
			// what was entered was not valid keep current value
		}
	}

	// Method get used by the settings page
	public void setDeviation(String deviation) {
		try {
			Double reading = Double.parseDouble(deviation);
			this.deviation = reading.toString();
		} catch (NumberFormatException e) {
			// what was entered was not valid keep current value
		}

	}

	// Method get used by the settings page
	public void setDeviate(String deviate) {
		this.deviate = Boolean.parseBoolean(deviate);
	}

	private MessageSource messageSource;

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

	// Puts the user configerable settings on the settings page
	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		MockEnergyMeterDatumSource defaults = new MockEnergyMeterDatumSource();
		List<SettingSpecifier> results = getIdentifiableSettingSpecifiers();

		// user enters text
		results.add(new BasicTextFieldSettingSpecifier("sourceId", defaults.sourceId));
		results.add(new BasicTextFieldSettingSpecifier("baseReading", defaults.baseReading));
		results.add(new BasicTextFieldSettingSpecifier("deviation", defaults.deviation));

		// toggleable button
		results.add(new BasicToggleSettingSpecifier("deviate", defaults.deviate));
		return results;
	}
}
