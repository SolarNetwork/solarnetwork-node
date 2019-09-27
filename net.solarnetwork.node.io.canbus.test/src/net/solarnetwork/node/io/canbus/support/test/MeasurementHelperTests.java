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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.measure.IncommensurableException;
import javax.measure.Quantity;
import javax.measure.UnconvertibleException;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.format.UnitFormat;
import javax.measure.quantity.ElectricResistance;
import javax.measure.quantity.Frequency;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.kayak.core.description.BusDescription;
import com.github.kayak.core.description.Document;
import com.github.kayak.core.description.MessageDescription;
import com.github.kayak.core.description.SignalDescription;
import net.solarnetwork.external.indriya.IndriyaMeasurementServiceProvider;
import net.solarnetwork.javax.measure.MeasurementServiceProvider;
import net.solarnetwork.node.io.canbus.support.KcdLoader;
import net.solarnetwork.node.io.canbus.support.MeasurementHelper;
import net.solarnetwork.util.OptionalServiceCollection;
import net.solarnetwork.util.StaticOptionalServiceCollection;
import si.uom.SIServiceProvider;
import systems.uom.common.internal.CommonServiceProvider;
import systems.uom.ucum.internal.UCUMServiceProvider;
import systems.uom.unicode.internal.UnicodeServiceProvider;
import tech.units.indriya.ComparableUnit;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.spi.DefaultServiceProvider;
import tech.units.indriya.unit.Units;

/**
 * Test cases for the {@link MeasurementHelper} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MeasurementHelperTests {

	private static final Logger log = LoggerFactory.getLogger(MeasurementHelperTests.class);

	private OptionalServiceCollection<MeasurementServiceProvider> measurementProviders;
	private MeasurementHelper helper;

	@Before
	public void setup() {
		measurementProviders = new StaticOptionalServiceCollection<>(
				asList(new IndriyaMeasurementServiceProvider(new UCUMServiceProvider()),
						new IndriyaMeasurementServiceProvider(new CommonServiceProvider()),
						new IndriyaMeasurementServiceProvider(new UnicodeServiceProvider()),
						new IndriyaMeasurementServiceProvider(new SIServiceProvider()),
						new IndriyaMeasurementServiceProvider(new DefaultServiceProvider())));
		helper = new MeasurementHelper(measurementProviders);
	}

	private Document testDocument(String name) {
		try {
			try (InputStream in = getClass().getResourceAsStream(name)) {
				return new KcdLoader().parse(in, "test.kcd");
			}
		} catch ( IOException e ) {
			throw new RuntimeException();
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
		assertThat("Unit ", unit.getSystemUnit(), equalTo(Units.WATT.multiply(Units.SECOND)));
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

		// the following returns the DefaultFormatService class
		UnitFormat uf = sp.getFormatService().getUnitFormat();
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
	public void unitValue_SignalDescription_Motor_OutsideTemp() {
		Document d = testDocument("kcd-test-01.xml");
		BusDescription motor = d.getBusDescriptions().stream().filter(b -> "Motor".equals(b.getName()))
				.findFirst().get();
		MessageDescription msg = motor.getMessages().values().stream()
				.filter(m -> "ABS".equals(m.getName())).findFirst().get();
		SignalDescription sig = msg.getSignals().stream().filter(s -> "OutsideTemp".equals(s.getName()))
				.findFirst().get();
		String sigUnit = sig.getUnit();
		Unit<?> unit = helper.unitValue(sigUnit);
		assertThat("Unit is degrees celsius", unit, equalTo(Units.CELSIUS));
	}

	@Test
	public void quantityValue_basic() {
		Quantity<?> q = helper.quantityValue(40.1, "Cel", null, null);
		assertThat("Quantity is expected", q, equalTo(Quantities.getQuantity(40.1, Units.CELSIUS)));
	}

	@Test
	public void unitValue_allFromTest2() {
		Document d = testDocument("kcd-test-02.xml");
		Set<String> seenUnits = new HashSet<>(8);
		d.getBusDescriptions().stream().flatMap(b -> b.getMessages().values().stream())
				.flatMap(m -> m.getSignals().stream())
				.filter(s -> s.getUnit() != null && !s.getUnit().isEmpty()).forEachOrdered(s -> {

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

}
