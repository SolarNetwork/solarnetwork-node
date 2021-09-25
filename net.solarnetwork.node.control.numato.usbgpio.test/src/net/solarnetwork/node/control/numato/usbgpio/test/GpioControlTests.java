/* ==================================================================
 * GpioControlTests.java - 25/09/2021 8:40:55 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.numato.usbgpio.test;

import static java.lang.Boolean.TRUE;
import static net.solarnetwork.domain.NodeControlPropertyType.Boolean;
import static net.solarnetwork.domain.NodeControlPropertyType.Float;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import static net.solarnetwork.test.EasyMockUtils.assertWith;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.BitSet;
import java.util.List;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.node.control.numato.usbgpio.GpioControl;
import net.solarnetwork.node.control.numato.usbgpio.GpioPropertyConfig;
import net.solarnetwork.node.control.numato.usbgpio.GpioService;
import net.solarnetwork.node.control.numato.usbgpio.GpioType;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.io.serial.SerialConnectionAction;
import net.solarnetwork.node.io.serial.SerialNetwork;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.test.Assertion;

/**
 * Test cases for the {@link GpioControl} class.
 * 
 * @author matt
 * @version 1.0
 */
public class GpioControlTests {

	private SerialNetwork network;
	private SerialConnection conn;
	private GpioService gpio;
	private GpioControl control;

	@Before
	public void setup() {
		network = EasyMock.createMock(SerialNetwork.class);
		conn = EasyMock.createMock(SerialConnection.class);
		gpio = EasyMock.createMock(GpioService.class);
		control = new GpioControl(new StaticOptionalService<>(network));
		control.setServiceProvider(conn -> gpio);
	}

	@After
	public void teardown() {
		EasyMock.verify(network, conn, gpio);
	}

	private void replayAll() {
		EasyMock.replay(network, conn, gpio);
	}

	private <T> void doConnAction() throws IOException {
		Object[] actionResult = new Object[1];
		expect(network.performAction(assertWith(new Assertion<SerialConnectionAction<T>>() {

			@Override
			public void check(SerialConnectionAction<T> argument) throws Throwable {
				actionResult[0] = argument.doWithConnection(conn);
			}

		}))).andAnswer(new IAnswer<T>() {

			@SuppressWarnings("unchecked")
			@Override
			public T answer() throws Throwable {
				return (T) actionResult[0];
			}
		});
	}

	@Test
	public void availableControlIds_multi() {
		// GIVEN
		GpioPropertyConfig cfg1 = new GpioPropertyConfig();
		cfg1.setAddress(0);
		cfg1.setControlId("/foo/bar/1");
		cfg1.setPropertyKey("p1");

		GpioPropertyConfig cfg2 = new GpioPropertyConfig();
		cfg2.setAddress(1);
		cfg2.setControlId("/foo/bar/2");
		cfg2.setPropertyKey("p2");

		control.setPropConfigs(new GpioPropertyConfig[] { cfg1, cfg2 });

		// WHEN
		replayAll();
		List<String> result = control.getAvailableControlIds();

		// THEN
		assertThat("Control IDs returned", result, contains(cfg1.getControlId(), cfg2.getControlId()));
	}

	@Test
	public void availableControlIds_ignoreInvalid() {
		// GIVEN
		GpioPropertyConfig cfg1 = new GpioPropertyConfig();
		cfg1.setAddress(0);
		cfg1.setControlId("/foo/bar/1");
		cfg1.setPropertyKey("p1");

		GpioPropertyConfig cfg2 = new GpioPropertyConfig(); // no control ID
		cfg2.setAddress(1);
		cfg2.setPropertyKey("p2");

		control.setPropConfigs(new GpioPropertyConfig[] { cfg1, cfg2 });

		// WHEN
		replayAll();
		List<String> result = control.getAvailableControlIds();

		// THEN
		assertThat("Control IDs returned", result, contains(cfg1.getControlId()));
	}

	@Test
	public void init_basic() throws Exception {
		// GIVEN
		GpioPropertyConfig cfg1 = new GpioPropertyConfig();
		cfg1.setGpioType(GpioType.Digital);
		cfg1.setAddress(0);
		cfg1.setControlId("/foo/bar/1");
		cfg1.setPropertyKey("p1");
		cfg1.setPropertyType(Instantaneous);
		control.setPropConfigs(new GpioPropertyConfig[] { cfg1 });

		final BitSet dirs = new BitSet();
		dirs.set(cfg1.getAddress());
		gpio.configureIoDirection(dirs);

		doConnAction();

		// WHEN
		replayAll();
		control.startup();

		// THEN
	}

	@Test
	public void init_multiInputs() throws Exception {
		// GIVEN
		GpioPropertyConfig cfg1 = GpioPropertyConfig.of("1", 0);
		GpioPropertyConfig cfg2 = GpioPropertyConfig.of("2", 4);
		GpioPropertyConfig cfg3 = GpioPropertyConfig.of("3", 7);
		control.setPropConfigs(new GpioPropertyConfig[] { cfg1, cfg2, cfg3 });

		final BitSet dirs = new BitSet();
		dirs.set(cfg1.getAddress());
		dirs.set(cfg2.getAddress());
		dirs.set(cfg3.getAddress());
		gpio.configureIoDirection(dirs);

		doConnAction();

		// WHEN
		replayAll();
		control.startup();

		// THEN
	}

	@Test
	public void readControl_digital_basic() throws Exception {
		// GIVEN
		GpioPropertyConfig cfg1 = new GpioPropertyConfig();
		cfg1.setGpioType(GpioType.Digital);
		cfg1.setAddress(0);
		cfg1.setControlId("/foo/bar/1");
		cfg1.setPropertyKey("p1");
		cfg1.setPropertyType(Instantaneous);

		final BitSet dirs = new BitSet();
		dirs.set(cfg1.getAddress());
		gpio.configureIoDirection(dirs);
		expect(gpio.read(cfg1.getAddress())).andReturn(true);

		doConnAction();
		control.setPropConfigs(new GpioPropertyConfig[] { cfg1 });

		// WHEN
		replayAll();
		NodeControlInfo info = control.getCurrentControlInfo(cfg1.getControlId());

		// THEN
		assertThat("Control info returned", info, is(notNullValue()));
		assertThat("Info is a datum", info, is(instanceOf(NodeDatum.class)));
		assertThat("Info control ID set", info.getControlId(), is(cfg1.getControlId()));
		assertThat("Info control type is boolean for digital", info.getType(), is(Boolean));
		assertThat("Info control value is boolean", info.getValue(), is(TRUE.toString()));
		assertThat("Control property provided", info.getPropertyName(), is(cfg1.getPropertyKey()));
		DatumSamplesOperations d = ((NodeDatum) info).asSampleOperations();
		assertThat("Datum prop set", d.getSampleInteger(Instantaneous, cfg1.getPropertyKey()), is(1));
		assertThat("Datum status prop not set", d.getSampleInteger(Status, cfg1.getPropertyKey()),
				is(nullValue()));
	}

	@Test
	public void readControl_digital_customPropertyClassification() throws Exception {
		// GIVEN
		GpioPropertyConfig cfg1 = new GpioPropertyConfig();
		cfg1.setGpioType(GpioType.Digital);
		cfg1.setAddress(0);
		cfg1.setControlId("/foo/bar/1");
		cfg1.setPropertyKey("i1");
		cfg1.setPropertyType(Instantaneous);

		final BitSet dirs = new BitSet();
		dirs.set(cfg1.getAddress());
		gpio.configureIoDirection(dirs);

		expect(gpio.read(cfg1.getAddress())).andReturn(true);

		doConnAction();
		control.setPropConfigs(new GpioPropertyConfig[] { cfg1 });

		// WHEN
		replayAll();
		NodeControlInfo info = control.getCurrentControlInfo(cfg1.getControlId());

		// THEN
		assertThat("Info is a datum", info, is(instanceOf(NodeDatum.class)));
		assertThat("Info control ID set", info.getControlId(), is(cfg1.getControlId()));
		assertThat("Info control type is boolean for digital", info.getType(), is(Boolean));
		assertThat("Info control value is boolean", info.getValue(), is(TRUE.toString()));
		assertThat("Control property provided", info.getPropertyName(), is(cfg1.getPropertyKey()));
		DatumSamplesOperations d = ((NodeDatum) info).asSampleOperations();
		assertThat("Datum prop set", d.getSampleInteger(Instantaneous, cfg1.getPropertyKey()), is(1));
	}

	@Test
	public void readControl_analog_basic() throws Exception {
		// GIVEN
		GpioPropertyConfig cfg1 = new GpioPropertyConfig();
		cfg1.setGpioType(GpioType.Analog);
		cfg1.setAddress(1);
		cfg1.setControlId("/analog/1");
		cfg1.setPropertyKey("i1");
		cfg1.setPropertyType(Instantaneous);

		final BitSet dirs = new BitSet();
		dirs.set(cfg1.getAddress());
		gpio.configureIoDirection(dirs);

		final Integer gpioOut = 123;
		expect(gpio.readAnalog(cfg1.getAddress())).andReturn(gpioOut);

		doConnAction();
		control.setPropConfigs(new GpioPropertyConfig[] { cfg1 });

		// WHEN
		replayAll();
		NodeControlInfo info = control.getCurrentControlInfo(cfg1.getControlId());

		// THEN
		assertThat("Info is a datum", info, is(instanceOf(NodeDatum.class)));
		assertThat("Info control ID set", info.getControlId(), is(cfg1.getControlId()));
		assertThat("Info control type is Float for analog", info.getType(), is(Float));
		assertThat("Info control value is number string", info.getValue(), is(gpioOut.toString()));
		assertThat("Control property provided", info.getPropertyName(), is(cfg1.getPropertyKey()));
		DatumSamplesOperations d = ((NodeDatum) info).asSampleOperations();
		assertThat("Datum prop set", d.getSampleInteger(Instantaneous, cfg1.getPropertyKey()),
				is(gpioOut));
	}

	@Test
	public void readControl_analog_transformed() throws Exception {
		// GIVEN
		GpioPropertyConfig cfg1 = new GpioPropertyConfig();
		cfg1.setGpioType(GpioType.Analog);
		cfg1.setAddress(1);
		cfg1.setControlId("/analog/1");
		cfg1.setPropertyKey("i1");
		cfg1.setPropertyType(Instantaneous);
		cfg1.setUnitIntercept(new BigDecimal("-61"));
		cfg1.setUnitSlope(new BigDecimal("12").divide(new BigDecimal("471"), 9, RoundingMode.HALF_UP));

		final BitSet dirs = new BitSet();
		dirs.set(cfg1.getAddress());
		gpio.configureIoDirection(dirs);

		final Integer gpioOut = 532;
		expect(gpio.readAnalog(cfg1.getAddress())).andReturn(gpioOut);

		doConnAction();
		control.setPropConfigs(new GpioPropertyConfig[] { cfg1 });

		// WHEN
		replayAll();
		NodeControlInfo info = control.getCurrentControlInfo(cfg1.getControlId());

		// THEN
		final BigDecimal expected = new BigDecimal("12.00000");
		assertThat("Info is a datum", info, is(instanceOf(NodeDatum.class)));
		assertThat("Info control ID set", info.getControlId(), is(cfg1.getControlId()));
		assertThat("Info control type is Float for analog", info.getType(), is(Float));
		assertThat("Info control value is number string", info.getValue(), is(expected.toString()));
		assertThat("Control property provided", info.getPropertyName(), is(cfg1.getPropertyKey()));
		DatumSamplesOperations d = ((NodeDatum) info).asSampleOperations();
		assertThat("Datum prop set", d.getSampleBigDecimal(Instantaneous, cfg1.getPropertyKey()),
				is(expected));
	}

}
