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
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.InputStream;
import javax.measure.Unit;
import org.junit.Before;
import org.junit.Test;
import com.github.kayak.core.description.BusDescription;
import com.github.kayak.core.description.Document;
import com.github.kayak.core.description.MessageDescription;
import com.github.kayak.core.description.SignalDescription;
import net.solarnetwork.javax.measure.DelegateMeasurementServiceProvider;
import net.solarnetwork.javax.measure.MeasurementServiceProvider;
import net.solarnetwork.node.io.canbus.support.KcdLoader;
import net.solarnetwork.node.io.canbus.support.MeasurementHelper;
import net.solarnetwork.util.OptionalServiceCollection;
import net.solarnetwork.util.StaticOptionalServiceCollection;
import si.uom.SIServiceProvider;
import systems.uom.common.internal.CommonServiceProvider;
import systems.uom.ucum.internal.UCUMServiceProvider;
import systems.uom.unicode.internal.UnicodeServiceProvider;
import tech.units.indriya.spi.DefaultServiceProvider;
import tech.units.indriya.unit.Units;

/**
 * Test cases for the {@link MeasurementHelper} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MeasurementHelperTests {

	private OptionalServiceCollection<MeasurementServiceProvider> measurementProviders;
	private MeasurementHelper helper;

	@Before
	public void setup() {
		measurementProviders = new StaticOptionalServiceCollection<>(
				asList(new DelegateMeasurementServiceProvider(new UCUMServiceProvider()),
						new DelegateMeasurementServiceProvider(new CommonServiceProvider()),
						new DelegateMeasurementServiceProvider(new UnicodeServiceProvider()),
						new DelegateMeasurementServiceProvider(new SIServiceProvider()),
						new DelegateMeasurementServiceProvider(new DefaultServiceProvider())));
		helper = new MeasurementHelper(measurementProviders);
	}

	private Document testDocument() {
		try {
			try (InputStream in = getClass().getResourceAsStream("kcd-test-01.xml")) {
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
	}

	@Test
	public void unitValue_SignalDescription_Motor_OutsideTemp() {
		Document d = testDocument();
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

}
