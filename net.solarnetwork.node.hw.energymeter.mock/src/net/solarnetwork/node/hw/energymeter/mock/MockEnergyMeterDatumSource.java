package net.solarnetwork.node.hw.energymeter.mock;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.context.MessageSource;

import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;

public class MockEnergyMeterDatumSource implements DatumDataSource<GeneralNodeACEnergyDatum>, SettingSpecifierProvider {

	@Override
	public String getUID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getGroupUID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<? extends GeneralNodeACEnergyDatum> getDatumType() {
		// TODO Auto-generated method stub
		return GeneralNodeACEnergyDatum.class;
	}

	@Override
	public GeneralNodeACEnergyDatum readCurrentDatum() {

		// we'll increment our Wh reading by a random amount between 0-15, with
		// the assumption we will read samples once per minute
		long wattHours = wattHourReading.addAndGet(Math.round(Math.random() * 15.0));

		GeneralNodeACEnergyDatum datum = new GeneralNodeACEnergyDatum();
		datum.setCreated(new Date());
		datum.setWattHourReading(wattHours);
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
							} catch (IllegalAccessException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IllegalArgumentException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (InvocationTargetException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}

					}
				}
			}
		}

		return datum;
	}

	private final AtomicLong wattHourReading = new AtomicLong(0);

	private String sourceId = "Deson Mock 2";

	private String baseReading = "1000";

	private String deviation = "200";

	private Boolean deviate = true;

	// reflected method
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	// reflected method
	public void setBaseReading(String baseReading) {
		try {
			Double reading = Double.parseDouble(baseReading);
			this.baseReading = reading.toString();
		} catch (NumberFormatException e) {
			// what was entered was not valid keep current value
		}

	}

	// reflected method
	public void setDeviation(String deviation) {
		try {
			Double reading = Double.parseDouble(deviation);
			this.deviation = reading.toString();
		} catch (NumberFormatException e) {
			// what was entered was not valid keep current value
		}

	}

	// reflected method
	public void setDeviate(String deviate) {
		this.deviate = Boolean.parseBoolean(deviate);
	}

	private MessageSource messageSource;

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.hw.energymeter.mock";
	}

	@Override
	public String getDisplayName() {
		return "Foobar Power";
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		MockEnergyMeterDatumSource defaults = new MockEnergyMeterDatumSource();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(1);
		results.add(new BasicTextFieldSettingSpecifier("sourceId", defaults.sourceId));
		results.add(new BasicTextFieldSettingSpecifier("baseReading", defaults.baseReading));
		results.add(new BasicTextFieldSettingSpecifier("deviation", defaults.deviation));
		results.add(new BasicToggleSettingSpecifier("deviate", defaults.deviate));
		return results;
	}
}
