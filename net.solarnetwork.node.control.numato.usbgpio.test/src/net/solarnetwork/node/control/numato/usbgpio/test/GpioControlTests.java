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
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import org.easymock.EasyMock;
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
import net.solarnetwork.node.io.serial.SerialNetwork;
import net.solarnetwork.node.service.PlaceholderService;
import net.solarnetwork.service.StaticOptionalService;

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

	private Object[] otherMocks;

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
		if ( otherMocks != null ) {
			EasyMock.verify(otherMocks);
		}
	}

	private void replayAll(Object... other) {
		EasyMock.replay(network, conn, gpio);
		if ( other != null ) {
			EasyMock.replay(other);
			this.otherMocks = other;
		}
	}

	private void doCreateConnection() throws IOException {
		expect(network.createConnection()).andReturn(conn);
		conn.open();
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
	public void availableControlIds_placeholders() {
		// GIVEN
		final PlaceholderService placeholderService = EasyMock.createMock(PlaceholderService.class);
		control.setPlaceholderService(new StaticOptionalService<>(placeholderService));

		GpioPropertyConfig cfg1 = new GpioPropertyConfig();
		cfg1.setAddress(0);
		cfg1.setControlId("/{name}/1");
		cfg1.setPropertyKey("p1");

		control.setPropConfigs(new GpioPropertyConfig[] { cfg1 });

		final String resolvedControlId = "/yes/1";
		expect(placeholderService.resolvePlaceholders(cfg1.getControlId(), null))
				.andReturn(resolvedControlId);

		// WHEN
		replayAll(placeholderService);
		List<String> result = control.getAvailableControlIds();

		// THEN
		assertThat("Control IDs returned", result, contains(resolvedControlId));
	}

	@Test
	public void startup_basic() throws Exception {
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

		doCreateConnection();

		// WHEN
		replayAll();
		control.startup();

		// THEN
	}

	@Test
	public void startup_exception() throws Exception {
		// GIVEN
		GpioPropertyConfig cfg1 = new GpioPropertyConfig();
		cfg1.setGpioType(GpioType.Digital);
		cfg1.setAddress(0);
		cfg1.setControlId("/foo/bar/1");
		cfg1.setPropertyKey("p1");
		cfg1.setPropertyType(Instantaneous);
		control.setPropConfigs(new GpioPropertyConfig[] { cfg1 });

		expect(network.createConnection()).andReturn(conn);
		conn.open();
		expectLastCall().andThrow(new IOException("Ouch"));
		conn.close();

		// WHEN
		replayAll();
		control.startup();

		// THEN
	}

	@Test
	public void startup_exception_thenRead() throws Exception {
		// GIVEN
		GpioPropertyConfig cfg1 = new GpioPropertyConfig();
		cfg1.setGpioType(GpioType.Digital);
		cfg1.setAddress(0);
		cfg1.setControlId("/foo/bar/1");
		cfg1.setPropertyKey("p1");
		cfg1.setPropertyType(Instantaneous);
		control.setPropConfigs(new GpioPropertyConfig[] { cfg1 });

		// open connection at startup
		expect(network.createConnection()).andReturn(conn);
		conn.open();
		expectLastCall().andThrow(new IOException("Ouch"));
		conn.close();

		// re-try open connection in read
		expect(network.createConnection()).andReturn(conn);
		conn.open();

		// set direction first time
		final BitSet dirs = new BitSet();
		dirs.set(cfg1.getAddress());
		gpio.configureIoDirection(dirs);
		expect(gpio.read(cfg1.getAddress())).andReturn(true);

		// WHEN
		replayAll();
		control.startup();

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
	public void shutdown_after_init() throws Exception {
		// GIVEN
		GpioPropertyConfig cfg1 = new GpioPropertyConfig();
		cfg1.setGpioType(GpioType.Digital);
		cfg1.setAddress(0);
		cfg1.setControlId("/foo/bar/1");
		cfg1.setPropertyKey("p1");
		cfg1.setPropertyType(Instantaneous);
		control.setPropConfigs(new GpioPropertyConfig[] { cfg1 });

		// open connection in startup()
		doCreateConnection();

		// set direction in startup()
		final BitSet dirs = new BitSet();
		dirs.set(cfg1.getAddress());
		gpio.configureIoDirection(dirs);

		// close connection in shutdown()
		conn.close();

		// WHEN
		replayAll();
		control.startup();
		control.shutdown();

		// THEN
	}

	@Test
	public void startup_multiInputs() throws Exception {
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

		doCreateConnection();

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

		doCreateConnection();
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
	public void readControl_digital_placeholders() throws Exception {
		// GIVEN
		final PlaceholderService placeholderService = EasyMock.createMock(PlaceholderService.class);
		control.setPlaceholderService(new StaticOptionalService<>(placeholderService));

		GpioPropertyConfig cfg1 = new GpioPropertyConfig();
		cfg1.setGpioType(GpioType.Digital);
		cfg1.setAddress(0);
		cfg1.setControlId("/{name}/1");
		cfg1.setPropertyKey("p1");
		cfg1.setPropertyType(Instantaneous);

		final BitSet dirs = new BitSet();
		dirs.set(cfg1.getAddress());
		gpio.configureIoDirection(dirs);
		expect(gpio.read(cfg1.getAddress())).andReturn(true);

		doCreateConnection();
		control.setPropConfigs(new GpioPropertyConfig[] { cfg1 });

		final String resolvedControlId = "/yes/1";
		expect(placeholderService.resolvePlaceholders(cfg1.getControlId(), null))
				.andReturn(resolvedControlId).anyTimes();

		// WHEN
		replayAll(placeholderService);
		NodeControlInfo info = control.getCurrentControlInfo(resolvedControlId);

		// THEN
		assertThat("Control info returned", info, is(notNullValue()));
		assertThat("Info is a datum", info, is(instanceOf(NodeDatum.class)));
		assertThat("Info control ID set", info.getControlId(), is(resolvedControlId));
		assertThat("Info control type is boolean for digital", info.getType(), is(Boolean));
		assertThat("Info control value is boolean", info.getValue(), is(TRUE.toString()));
		assertThat("Control property provided", info.getPropertyName(), is(cfg1.getPropertyKey()));
		DatumSamplesOperations d = ((NodeDatum) info).asSampleOperations();
		assertThat("Datum prop set", d.getSampleInteger(Instantaneous, cfg1.getPropertyKey()), is(1));
		assertThat("Datum status prop not set", d.getSampleInteger(Status, cfg1.getPropertyKey()),
				is(nullValue()));
	}

	@Test
	public void readControl_digital_2ndTime() throws Exception {
		// GIVEN
		GpioPropertyConfig cfg1 = new GpioPropertyConfig();
		cfg1.setGpioType(GpioType.Digital);
		cfg1.setAddress(0);
		cfg1.setControlId("/foo/bar/1");
		cfg1.setPropertyKey("p1");
		cfg1.setPropertyType(Instantaneous);
		control.setPropConfigs(new GpioPropertyConfig[] { cfg1 });

		// open connection first time
		doCreateConnection();

		// set direction first time
		final BitSet dirs = new BitSet();
		dirs.set(cfg1.getAddress());
		gpio.configureIoDirection(dirs);

		// read first time
		expect(gpio.read(cfg1.getAddress())).andReturn(true);

		// read second time, without re-setting direction
		expect(gpio.read(cfg1.getAddress())).andReturn(true);

		// WHEN
		replayAll();
		NodeControlInfo info1 = control.getCurrentControlInfo(cfg1.getControlId());
		NodeControlInfo info2 = control.getCurrentControlInfo(cfg1.getControlId());

		// THEN
		for ( NodeControlInfo info : Arrays.asList(info1, info2) ) {
			assertThat("Control info returned", info, is(notNullValue()));
			assertThat("Info is a datum", info, is(instanceOf(NodeDatum.class)));
			assertThat("Info control ID set", info.getControlId(), is(cfg1.getControlId()));
			assertThat("Info control type is boolean for digital", info.getType(), is(Boolean));
			assertThat("Info control value is boolean", info.getValue(), is(TRUE.toString()));
			assertThat("Control property provided", info.getPropertyName(), is(cfg1.getPropertyKey()));
			DatumSamplesOperations d = ((NodeDatum) info).asSampleOperations();
			assertThat("Datum prop set", d.getSampleInteger(Instantaneous, cfg1.getPropertyKey()),
					is(1));
			assertThat("Datum status prop not set", d.getSampleInteger(Status, cfg1.getPropertyKey()),
					is(nullValue()));
		}
	}

	@Test
	public void readControl_digital_2ndTimeException_3rdTimeCharm() throws Exception {
		// GIVEN
		GpioPropertyConfig cfg1 = new GpioPropertyConfig();
		cfg1.setGpioType(GpioType.Digital);
		cfg1.setAddress(0);
		cfg1.setControlId("/foo/bar/1");
		cfg1.setPropertyKey("p1");
		cfg1.setPropertyType(Instantaneous);
		control.setPropConfigs(new GpioPropertyConfig[] { cfg1 });

		// open connection first time
		doCreateConnection();

		// set direction first time
		final BitSet dirs = new BitSet();
		dirs.set(cfg1.getAddress());
		gpio.configureIoDirection(dirs);

		// read first time
		expect(gpio.read(cfg1.getAddress())).andReturn(true);

		// read second time, without re-setting direction
		expect(gpio.read(cfg1.getAddress())).andThrow(new IOException("Ouch"));

		// close connection after exception
		conn.close();

		// re-open connection for 3rd read
		doCreateConnection();

		// re-set direction after open
		gpio.configureIoDirection(dirs);

		// read 3rd time OK
		expect(gpio.read(cfg1.getAddress())).andReturn(true);

		// WHEN
		replayAll();
		NodeControlInfo info1 = control.getCurrentControlInfo(cfg1.getControlId());
		NodeControlInfo info2 = control.getCurrentControlInfo(cfg1.getControlId());
		NodeControlInfo info3 = control.getCurrentControlInfo(cfg1.getControlId());

		assertThat("2nd read returned null from IOException", info2, is(nullValue()));

		// THEN
		for ( NodeControlInfo info : Arrays.asList(info1, info3) ) {
			assertThat("Control info returned", info, is(notNullValue()));
			assertThat("Info is a datum", info, is(instanceOf(NodeDatum.class)));
			assertThat("Info control ID set", info.getControlId(), is(cfg1.getControlId()));
			assertThat("Info control type is boolean for digital", info.getType(), is(Boolean));
			assertThat("Info control value is boolean", info.getValue(), is(TRUE.toString()));
			assertThat("Control property provided", info.getPropertyName(), is(cfg1.getPropertyKey()));
			DatumSamplesOperations d = ((NodeDatum) info).asSampleOperations();
			assertThat("Datum prop set", d.getSampleInteger(Instantaneous, cfg1.getPropertyKey()),
					is(1));
			assertThat("Datum status prop not set", d.getSampleInteger(Status, cfg1.getPropertyKey()),
					is(nullValue()));
		}
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

		doCreateConnection();
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

		doCreateConnection();
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

		doCreateConnection();
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

	@Test
	public void changeSerialNetwork_betweenReads() throws Exception {
		// GIVEN
		GpioPropertyConfig cfg1 = new GpioPropertyConfig();
		cfg1.setGpioType(GpioType.Digital);
		cfg1.setAddress(0);
		cfg1.setControlId("/foo/bar/1");
		cfg1.setPropertyKey("p1");
		cfg1.setPropertyType(Instantaneous);
		control.setPropConfigs(new GpioPropertyConfig[] { cfg1 });

		// open connection first time
		doCreateConnection();

		// set direction first time
		final BitSet dirs = new BitSet();
		dirs.set(cfg1.getAddress());
		gpio.configureIoDirection(dirs);

		// read first time
		expect(gpio.read(cfg1.getAddress())).andReturn(true);

		// close first connection
		conn.close();

		// re-create connection
		SerialConnection conn2 = EasyMock.createMock(SerialConnection.class);
		expect(network.createConnection()).andReturn(conn2);
		conn2.open();

		// set direction second time
		gpio.configureIoDirection(dirs);

		// read second time
		expect(gpio.read(cfg1.getAddress())).andReturn(true);

		// WHEN
		replayAll(conn2);
		NodeControlInfo info1 = control.getCurrentControlInfo(cfg1.getControlId());

		// change serial network
		control.setSerialNetworkUid("Other Serial");
		control.configurationChanged(null);

		NodeControlInfo info2 = control.getCurrentControlInfo(cfg1.getControlId());

		// THEN
		for ( NodeControlInfo info : Arrays.asList(info1, info2) ) {
			assertThat("Control info returned", info, is(notNullValue()));
			assertThat("Info is a datum", info, is(instanceOf(NodeDatum.class)));
			assertThat("Info control ID set", info.getControlId(), is(cfg1.getControlId()));
			assertThat("Info control type is boolean for digital", info.getType(), is(Boolean));
			assertThat("Info control value is boolean", info.getValue(), is(TRUE.toString()));
			assertThat("Control property provided", info.getPropertyName(), is(cfg1.getPropertyKey()));
			DatumSamplesOperations d = ((NodeDatum) info).asSampleOperations();
			assertThat("Datum prop set", d.getSampleInteger(Instantaneous, cfg1.getPropertyKey()),
					is(1));
			assertThat("Datum status prop not set", d.getSampleInteger(Status, cfg1.getPropertyKey()),
					is(nullValue()));
		}

		EasyMock.verify(conn2);
	}

	@Test
	public void changeIoDirections_betweenReads() throws Exception {
		// GIVEN
		GpioPropertyConfig cfg1 = new GpioPropertyConfig();
		cfg1.setGpioType(GpioType.Digital);
		cfg1.setAddress(0);
		cfg1.setControlId("/foo/bar/1");
		cfg1.setPropertyKey("p1");
		cfg1.setPropertyType(Instantaneous);
		control.setPropConfigs(new GpioPropertyConfig[] { cfg1 });

		// open connection first time
		doCreateConnection();

		// set direction first time
		final BitSet dirs = new BitSet();
		dirs.set(0);
		gpio.configureIoDirection(dirs);

		// read first time
		expect(gpio.read(0)).andReturn(true);

		// set direction second time
		final BitSet dirs2 = new BitSet();
		dirs2.set(1);
		gpio.configureIoDirection(dirs2);

		// read second time
		expect(gpio.read(1)).andReturn(true);

		// WHEN
		replayAll();
		NodeControlInfo info1 = control.getCurrentControlInfo(cfg1.getControlId());

		// change address of config, which changes direction bitset
		cfg1.setAddress(1);
		control.configurationChanged(null);

		NodeControlInfo info2 = control.getCurrentControlInfo(cfg1.getControlId());

		// THEN
		for ( NodeControlInfo info : Arrays.asList(info1, info2) ) {
			assertThat("Control info returned", info, is(notNullValue()));
			assertThat("Info is a datum", info, is(instanceOf(NodeDatum.class)));
			assertThat("Info control ID set", info.getControlId(), is(cfg1.getControlId()));
			assertThat("Info control type is boolean for digital", info.getType(), is(Boolean));
			assertThat("Info control value is boolean", info.getValue(), is(TRUE.toString()));
			assertThat("Control property provided", info.getPropertyName(), is(cfg1.getPropertyKey()));
			DatumSamplesOperations d = ((NodeDatum) info).asSampleOperations();
			assertThat("Datum prop set", d.getSampleInteger(Instantaneous, cfg1.getPropertyKey()),
					is(1));
			assertThat("Datum status prop not set", d.getSampleInteger(Status, cfg1.getPropertyKey()),
					is(nullValue()));
		}
	}
}
