/* ==================================================================
 * MeasurementHelperTests.java - 15/09/2019 10:08:41 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.canbus.support.test;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.measure.IncommensurableException;
import javax.measure.MetricPrefix;
import javax.measure.Quantity;
import javax.measure.UnconvertibleException;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.format.UnitFormat;
import javax.measure.quantity.ElectricResistance;
import javax.measure.quantity.Frequency;
import javax.measure.quantity.Power;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.external.indriya.IndriyaMeasurementServiceProvider;
import net.solarnetwork.javax.measure.MeasurementServiceProvider;
import net.solarnetwork.node.io.canbus.kcd.NetworkDefinitionType;
import net.solarnetwork.node.io.canbus.support.JaxbSnKcdParser;
import net.solarnetwork.node.io.canbus.support.MeasurementHelper;
import net.solarnetwork.service.OptionalServiceCollection;
import net.solarnetwork.service.StaticOptionalServiceCollection;
import systems.uom.ucum.spi.UCUMServiceProvider;
import tech.units.indriya.ComparableUnit;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.spi.DefaultServiceProvider;
import tech.units.indriya.unit.Units;

/**
 * Test cases for the {@link MeasurementHelper} class.
 * 
 * @author matt
 * @version 2.0
 */
public class MeasurementHelperTests {

	private static final Logger log = LoggerFactory.getLogger(MeasurementHelperTests.class);

	private OptionalServiceCollection<MeasurementServiceProvider> measurementProviders;
	private MeasurementHelper helper;

	@Before
	public void setup() {
		measurementProviders = new StaticOptionalServiceCollection<>(
				asList(new IndriyaMeasurementServiceProvider(new UCUMServiceProvider()),
						new IndriyaMeasurementServiceProvider(new DefaultServiceProvider())));
		helper = new MeasurementHelper(measurementProviders);
	}

	private NetworkDefinitionType testDocument(String name, boolean validating) {
		try {
			try (InputStream in = getClass().getResourceAsStream(name)) {
				return new JaxbSnKcdParser().parseKcd(in, validating);
			}
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void unitValue_Celsius() {
		Unit<?> unit = helper.unitValue("Celsius");
		assertThat("Unit is degrees celsius", unit, equalTo(Units.CELSIUS));
	}

	@Test
	public void unitValue_Cel() {
		Unit<?> unit = helper.unitValue("Cel");
		assertThat("Unit is degrees celsius", unit, equalTo(Units.CELSIUS));

		Quantity<?> q = Quantities.getQuantity(40.1, unit);
		assertThat("Quantity is expected", q, equalTo(Quantities.getQuantity(40.1, Units.CELSIUS)));
	}

	@Test
	public void unitValue_km() {
		Unit<?> unit = helper.unitValue("km");
		assertThat("System unit is length", unit.getSystemUnit(), equalTo(Units.METRE));
	}

	@Test
	public void unitValue_kWh() {
		Unit<?> unit = helper.unitValue("kW.h");
		assertThat("System unit is W.s", unit.getSystemUnit(),
				equalTo(Units.WATT.multiply(Units.SECOND)));
	}

	@Test
	public void unitValue_W() {
		Unit<?> unit = helper.unitValue("W");
		assertThat("Unit is W", unit.getSystemUnit(), equalTo(Units.WATT));
	}

	@Test
	public void unitValue_kOhm() {
		Unit<?> unit = helper.unitValue("kOhm");
		assertThat("Unit ", unit.getSystemUnit(), equalTo(Units.OHM));

		Unit<ElectricResistance> erUnit = unit.asType(ElectricResistance.class);
		assertThat("Unit is electric resistance", erUnit.getSystemUnit(), equalTo(Units.OHM));

		Map<? extends Unit<?>, Integer> bases = unit.getBaseUnits();
		assertThat("Bases exist", bases, notNullValue());
		assertThat("Bases size", bases.keySet(), hasSize(2));
		assertThat("Base amps", bases, hasEntry(Units.AMPERE, -1));
		assertThat("Base volts", bases, hasEntry(Units.VOLT, 1));
	}

	@Test
	public void unitValue_rpm() throws UnconvertibleException, IncommensurableException {
		Unit<?> unit = helper.unitValue("Hz/60");
		assertThat("Unit base is Hz", unit.isCompatible(Units.HERTZ), equalTo(true));

		// The following does not work because it is parsed into Hz/one*60
		//assertThat("Unit ", unit, equalTo(Units.HERTZ.divide(60)));

		// so as an alternative check, look for identify transform
		UnitConverter conv = unit.getConverterToAny(Units.HERTZ.divide(60));
		assertThat("Unit is RPM", conv.isIdentity(), equalTo(true));
	}

	@Test
	public void unitValue_rpm_2() throws UnconvertibleException, IncommensurableException {
		Unit<?> unit = helper.unitValue("1/min");
		assertThat("Unit ", unit.isCompatible(Units.HERTZ), equalTo(true));

		UnitConverter conv = unit.getConverterToAny(Units.HERTZ.divide(60));
		boolean ident = conv.isIdentity();
		assertThat("Ident", ident, equalTo(true));
	}

	@Test
	public void ucum_rpm() throws UnconvertibleException, IncommensurableException {
		UCUMServiceProvider sp = new UCUMServiceProvider();

		final Unit<Frequency> rpm = Units.HERTZ.divide(60);

		// use explicit DefaultServiceProvider
		UnitFormat uf = new DefaultServiceProvider().getFormatService().getUnitFormat();
		Unit<?> unit = uf.parse("Hz/60");
		assertThat("Unit is RPM", unit, equalTo(rpm));

		// the following returns the UCUMFormatService class
		UnitFormat uf2 = sp.getUnitFormatService().getUnitFormat();
		Unit<?> unit2 = uf2.parse("Hz/60");

		// as an alternative check, look for identify transform
		UnitConverter conv = unit2.getConverterToAny(rpm);
		assertThat("Unit conversion to RPM is identity", conv.isIdentity(), equalTo(true));

		// The following assert does not work because `unit` is parsed into Hz/one*60
		// TODO assertThat("Unit is RPM", unit2, equalTo(Units.HERTZ.divide(60)));
		if ( unit2.isCompatible(rpm) && unit2 instanceof ComparableUnit<?> ) {
			ComparableUnit<Frequency> cu2 = (ComparableUnit<Frequency>) unit2.asType(Frequency.class);
			assertThat("Unit is RPM", cu2.isEquivalentTo(rpm), equalTo(true));
		}
	}

	@Test
	public void quantityValue_basic() {
		Quantity<?> q = helper.quantityValue(40.1, "Cel", null, null);
		assertThat("Quantity is expected", q, equalTo(Quantities.getQuantity(40.1, Units.CELSIUS)));
	}

	@Test
	public void unitValue_allFromTest2() {
		NetworkDefinitionType d = testDocument("kcd-test-02.xml", false);
		Set<String> seenUnits = new HashSet<>(8);
		d.getBus().stream().flatMap(b -> b.getMessage().stream()).flatMap(m -> m.getSignal().stream())
				.map(s -> s.getValue()).filter(s -> s.getUnit() != null && !s.getUnit().isEmpty())
				.forEachOrdered(s -> {

					String sigUnit = s.getUnit();
					if ( seenUnits.contains(sigUnit) ) {
						return;
					}
					seenUnits.add(sigUnit);
					try {
						Unit<?> unit = helper.unitValue(sigUnit);
						log.debug("Parsed signal unit string [{}] into unit: {}", sigUnit, unit);
						assertThat("Unit " + sigUnit + " is parsed", unit, notNullValue());
					} catch ( Exception e ) {
						throw new RuntimeException("Unit " + sigUnit + " failed to parse.", e);
					}
				});
	}

	@Test
	public void normalizedUnitValue_kW() {
		Unit<Power> w = helper.normalizedUnit(MetricPrefix.KILO(Units.WATT));
		assertThat("kW normalized to W", w, equalTo(Units.WATT));
	}

	@Test
	public void normalizedUnitValue_Wh() {
		Unit<?> wh = helper.normalizedUnit(Units.WATT.multiply(Units.HOUR));
		assertThat("Wh normalized to Wh", wh, equalTo(Units.WATT.multiply(Units.HOUR)));
	}

	@Test
	public void normalizedUnitValue_kWh() {
		Unit<?> wh = helper.normalizedUnit(MetricPrefix.KILO(Units.WATT).multiply(Units.HOUR));
		assertThat("kWh normalized to Wh", wh, equalTo(Units.WATT.multiply(Units.HOUR)));
	}

	@Test
	public void unitValue_VA() {
		Unit<?> u = helper.unitValue("VA");
		assertThat("Unit is expected", u.isCompatible(Units.WATT), equalTo(true));
	}

	@Test
	public void unitValue_kVA() {
		Unit<?> u = helper.unitValue("kVA");
		assertThat("Unit is not expected", u, nullValue());
	}

	@Test
	public void normalizedUnitValue_VA() {
		Unit<?> u = helper.unitValue("VA");
		Unit<?> n = helper.normalizedUnit(u);
		assertThat("VA normalized to VA", n.isCompatible(Units.WATT), equalTo(true));
	}

	@Test
	public void unitValue_VAr() {
		Unit<?> u = helper.unitValue("V.A{reactive}");
		assertThat("Unit is expected", u.isCompatible(Units.VOLT.multiply(Units.AMPERE)), equalTo(true));
	}

	@Test
	public void unitValue_var() {
		Unit<?> u = helper.unitValue("var");
		assertThat("Unit is expected", u.isCompatible(Units.WATT), equalTo(true));
	}

	@Test
	public void formatUnit_VA() {
		Unit<?> u = helper.unitValue("VA");
		String va = helper.formatUnit(u);
		assertThat("VA alternate formatted as VA", va, equalTo("VA"));
	}

	@Test
	public void formatUnit_VAr() {
		Unit<?> voltAmpsReactive = helper.unitValue("var");
		String var = helper.formatUnit(voltAmpsReactive);
		assertThat("var normalized to var", var, equalTo("var"));
	}

}
