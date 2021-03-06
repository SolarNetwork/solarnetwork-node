/* ==================================================================
 * CanbusDatumDataSourceTests.java - 8/10/2019 10:38:59 am
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

package net.solarnetwork.node.datum.canbus.test;

import static java.util.Collections.singletonMap;
import static net.solarnetwork.domain.GeneralDatumSamplesType.Instantaneous;
import static net.solarnetwork.util.ByteUtils.decodeHexString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.common.expr.spel.SpelExpressionService;
import net.solarnetwork.domain.BitDataType;
import net.solarnetwork.domain.ByteOrdering;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.external.indriya.IndriyaMeasurementServiceProvider;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.DatumMetadataService;
import net.solarnetwork.node.datum.canbus.CanbusDatumDataSource;
import net.solarnetwork.node.datum.canbus.CanbusMessageConfig;
import net.solarnetwork.node.datum.canbus.CanbusPropertyConfig;
import net.solarnetwork.node.datum.canbus.ExpressionConfig;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.GeneralDatum;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.io.canbus.socketcand.msg.FrameMessageImpl;
import net.solarnetwork.node.io.canbus.support.MeasurementHelper;
import net.solarnetwork.support.ExpressionService;
import net.solarnetwork.util.OptionalServiceCollection;
import net.solarnetwork.util.StaticOptionalService;
import net.solarnetwork.util.StaticOptionalServiceCollection;
import systems.uom.ucum.spi.UCUMServiceProvider;
import tech.units.indriya.spi.DefaultServiceProvider;

/**
 * Test cases for the {@link CanbusDatumDataSource} class.
 * 
 * @author matt
 * @version 1.2
 */
public class CanbusDatumDataSourceTests {

	private static final String TEST_SOURCE = "/test/source";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private EventAdmin eventAdmin;
	private DatumMetadataService datumMetadataService;
	private CanbusDatumDataSource dataSource;

	@Before
	public void setup() {
		eventAdmin = EasyMock.createMock(EventAdmin.class);
		datumMetadataService = EasyMock.createMock(DatumMetadataService.class);

		dataSource = new CanbusDatumDataSource();
		dataSource.setEventAdmin(new StaticOptionalService<EventAdmin>(eventAdmin));
		dataSource.setDatumMetadataService(new StaticOptionalService<>(datumMetadataService));
		dataSource.setMeasurementHelper(new MeasurementHelper(new StaticOptionalServiceCollection<>(
				Arrays.asList(new IndriyaMeasurementServiceProvider(new UCUMServiceProvider()),
						new IndriyaMeasurementServiceProvider(new DefaultServiceProvider())))));
		dataSource.setExpressionServices(spelExpressionServices());
	}

	private OptionalServiceCollection<ExpressionService> spelExpressionServices() {
		return new StaticOptionalServiceCollection<>(
				Collections.singletonList(new SpelExpressionService()));
	}

	@After
	public void teardown() {
		EasyMock.verify(eventAdmin, datumMetadataService);
	}

	private void replayAll() {
		EasyMock.replay(eventAdmin, datumMetadataService);
	}

	@Test
	public void generateMetadata() {
		// GIVEN
		CanbusMessageConfig message = new CanbusMessageConfig();
		CanbusPropertyConfig prop1 = new CanbusPropertyConfig("watts",
				GeneralDatumSamplesType.Instantaneous, 0);
		prop1.setLocalizedNames(new KeyValuePair[] { new KeyValuePair("en", "Foo Bar"),
				new KeyValuePair("zh-Hans", "Foo酒吧") });
		prop1.setUnit("kW");
		message.setPropConfigs(new CanbusPropertyConfig[] { prop1 });

		dataSource.setSourceId(TEST_SOURCE);
		dataSource.setMsgConfigs(new CanbusMessageConfig[] { message });

		Capture<GeneralDatumMetadata> metaCaptor = new Capture<>();
		datumMetadataService.addSourceMetadata(eq(TEST_SOURCE), capture(metaCaptor));

		// WHEN
		replayAll();
		dataSource.configurationChanged(null);

		// THEN
		GeneralDatumMetadata meta = metaCaptor.getValue();
		assertThat("Metadata added", meta, notNullValue());
		assertThat("Property metadata keys", meta.getPm().keySet(), containsInAnyOrder("watts"));

		assertThat("watts property metadata values", meta.getPm().get("watts").keySet(),
				containsInAnyOrder("name", "unit", "sourceUnit"));
		assertThat("watts names", meta.getPm().get("watts"),
				hasEntry("name", prop1.getLocalizedNamesMap()));
		assertThat("watts unit", meta.getPm().get("watts"), hasEntry("unit", "W"));
		assertThat("watts unit", meta.getPm().get("watts"), hasEntry("sourceUnit", "kW"));
	}

	private Map<String, Object> eventProps(Event evt) {
		Map<String, Object> m = new HashMap<>();
		for ( String n : evt.getPropertyNames() ) {
			m.put(n, evt.getProperty(n));
		}
		return m;
	}

	private void assertDatumCapturedEvent(Event event, long minDate, String sourceId,
			Map<String, Object> expectedData) {
		assertThat("Event generated", event, notNullValue());
		assertThat("Event topic", event.getTopic(), equalTo(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED));
		Map<String, Object> evtProps = eventProps(event);
		log.debug("Got datum captured event: {}", evtProps);
		assertThat("Event property keys", evtProps.keySet(), containsInAnyOrder(Datum.DATUM_PROPERTY,
				Datum.DATUM_TYPE_PROPERTY, Datum.DATUM_TYPES_PROPERTY, "event.topics"));
		Object o = evtProps.get(Datum.DATUM_PROPERTY);
		assertThat("Event Datum is GeneralDatum", o, instanceOf(GeneralDatum.class));
		GeneralDatum datum = (GeneralDatum) o;
		assertThat("Event creation date at least", datum.getCreated().getTime(),
				greaterThanOrEqualTo(minDate));
		Map<String, ?> datumProps = datum.getSampleData();
		for ( Map.Entry<String, Object> me : expectedData.entrySet() ) {
			if ( me.getValue() != null ) {
				assertThat("Event datum property " + me.getKey() + " value", datumProps.get(me.getKey()),
						equalTo(me.getValue()));
			}
		}
	}

	@Test
	public void frameReceived_basic() {
		// GIVEN
		CanbusMessageConfig message = new CanbusMessageConfig(1, ByteOrdering.BigEndian);
		message.addPropConfig(new CanbusPropertyConfig("watts", GeneralDatumSamplesType.Instantaneous, 0,
				BitDataType.UInt8, 8, "W", null));

		dataSource.setSourceId(TEST_SOURCE);
		dataSource.setMsgConfigs(new CanbusMessageConfig[] { message });

		Capture<Event> eventCaptor = new Capture<>();
		eventAdmin.postEvent(capture(eventCaptor));

		// WHEN
		replayAll();
		long start = System.currentTimeMillis();
		FrameMessageImpl f = new FrameMessageImpl(1, false, 1, 2, new byte[] { (byte) 0x11 });
		dataSource.canbusFrameReceived(f);
		GeneralNodeDatum d = dataSource.readCurrentDatum();

		// THEN
		Event evt = eventCaptor.getValue();
		assertThat("Event generated", evt, notNullValue());
		assertThat("Event topic", evt.getTopic(), equalTo(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED));
		Map<String, Object> expectedData = Collections.singletonMap("watts", 17);
		assertDatumCapturedEvent(evt, start, TEST_SOURCE, expectedData);

		assertThat("Datum captured", d, notNullValue());
		assertThat("Datum watts instantaneous value", d.getInstantaneousSampleBigDecimal("watts"),
				equalTo(new BigDecimal("17")));
	}

	@Test
	public void frameReceived_transformed() {
		// GIVEN
		CanbusMessageConfig message = new CanbusMessageConfig(1, ByteOrdering.BigEndian);
		CanbusPropertyConfig prop = new CanbusPropertyConfig("watts",
				GeneralDatumSamplesType.Instantaneous, 0, BitDataType.UInt8, 8, "W", null);
		prop.setSlope(new BigDecimal("0.1"));
		prop.setIntercept(new BigDecimal("33"));
		message.addPropConfig(prop);

		dataSource.setSourceId(TEST_SOURCE);
		dataSource.setMsgConfigs(new CanbusMessageConfig[] { message });

		Capture<Event> eventCaptor = new Capture<>();
		eventAdmin.postEvent(capture(eventCaptor));

		// WHEN
		replayAll();
		long start = System.currentTimeMillis();
		FrameMessageImpl f = new FrameMessageImpl(1, false, 1, 2, new byte[] { (byte) 0x11 });
		dataSource.canbusFrameReceived(f);
		GeneralNodeDatum d = dataSource.readCurrentDatum();

		// THEN
		Event evt = eventCaptor.getValue();
		assertThat("Event generated", evt, notNullValue());
		assertThat("Event topic", evt.getTopic(), equalTo(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED));
		Map<String, Object> expectedData = Collections.singletonMap("watts", new BigDecimal("34.7"));
		assertDatumCapturedEvent(evt, start, TEST_SOURCE, expectedData);

		assertThat("Datum captured", d, notNullValue());
		assertThat("Datum watts instantaneous value", d.getInstantaneousSampleBigDecimal("watts"),
				equalTo(new BigDecimal("34.7")));
	}

	@Test
	public void frameReceived_kilo() {
		// GIVEN
		CanbusMessageConfig message = new CanbusMessageConfig(1, ByteOrdering.BigEndian);
		// the raw data is in kW, which should be automagically parsed into W
		message.addPropConfig(new CanbusPropertyConfig("watts", GeneralDatumSamplesType.Instantaneous, 0,
				BitDataType.UInt8, 8, "kW", null));

		dataSource.setSourceId(TEST_SOURCE);
		dataSource.setMsgConfigs(new CanbusMessageConfig[] { message });

		Capture<Event> eventCaptor = new Capture<>();
		eventAdmin.postEvent(capture(eventCaptor));

		// WHEN
		replayAll();
		long start = System.currentTimeMillis();
		FrameMessageImpl f = new FrameMessageImpl(1, false, 1, 2, new byte[] { (byte) 0x11 });
		dataSource.canbusFrameReceived(f);
		GeneralNodeDatum d = dataSource.readCurrentDatum();

		// THEN
		Event evt = eventCaptor.getValue();
		assertThat("Event generated", evt, notNullValue());
		assertThat("Event topic", evt.getTopic(), equalTo(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED));
		Map<String, Object> expectedData = Collections.singletonMap("watts", 17000);
		assertDatumCapturedEvent(evt, start, TEST_SOURCE, expectedData);

		assertThat("Datum captured", d, notNullValue());
		assertThat("Datum watts instantaneous value", d.getInstantaneousSampleBigDecimal("watts"),
				equalTo(new BigDecimal("17000")));
	}

	@Test
	public void frameReceived_customNormalization_identity() {
		// GIVEN
		CanbusMessageConfig message = new CanbusMessageConfig(1, ByteOrdering.BigEndian);
		message.addPropConfig(new CanbusPropertyConfig("torque", GeneralDatumSamplesType.Instantaneous,
				0, BitDataType.UInt8, 8, "N.m", "N.m"));

		dataSource.setSourceId(TEST_SOURCE);
		dataSource.setMsgConfigs(new CanbusMessageConfig[] { message });

		Capture<Event> eventCaptor = new Capture<>();
		eventAdmin.postEvent(capture(eventCaptor));

		// WHEN
		replayAll();
		long start = System.currentTimeMillis();
		FrameMessageImpl f = new FrameMessageImpl(1, false, 1, 2, new byte[] { (byte) 0x11 });
		dataSource.canbusFrameReceived(f);
		GeneralNodeDatum d = dataSource.readCurrentDatum();

		// THEN
		Event evt = eventCaptor.getValue();
		assertThat("Event generated", evt, notNullValue());
		assertThat("Event topic", evt.getTopic(), equalTo(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED));
		Map<String, Object> expectedData = singletonMap("torque", 17);
		assertDatumCapturedEvent(evt, start, TEST_SOURCE, expectedData);

		assertThat("Datum captured", d, notNullValue());
		assertThat("Datum distance instantaneous value", d.getInstantaneousSampleBigDecimal("torque"),
				equalTo(new BigDecimal("17")));
	}

	@Test
	public void frameReceived_customNormalization_converted() {
		// GIVEN
		CanbusMessageConfig message = new CanbusMessageConfig(1, ByteOrdering.BigEndian);
		message.addPropConfig(new CanbusPropertyConfig("distance", GeneralDatumSamplesType.Instantaneous,
				0, BitDataType.UInt8, 8, "m", "km"));

		dataSource.setSourceId(TEST_SOURCE);
		dataSource.setMsgConfigs(new CanbusMessageConfig[] { message });

		Capture<Event> eventCaptor = new Capture<>();
		eventAdmin.postEvent(capture(eventCaptor));

		// WHEN
		replayAll();
		long start = System.currentTimeMillis();
		FrameMessageImpl f = new FrameMessageImpl(1, false, 1, 2, new byte[] { (byte) 0x11 });
		dataSource.canbusFrameReceived(f);
		GeneralNodeDatum d = dataSource.readCurrentDatum();

		// THEN
		Event evt = eventCaptor.getValue();
		assertThat("Event generated", evt, notNullValue());
		assertThat("Event topic", evt.getTopic(), equalTo(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED));
		assertDatumCapturedEvent(evt, start, TEST_SOURCE, singletonMap("distance", null));
		Map<String, ?> eventDatumProps = ((Datum) evt.getProperty(Datum.DATUM_PROPERTY)).getSampleData();
		assertThat("Event distance", ((Number) eventDatumProps.get("distance")).toString(),
				equalTo("0.017"));

		assertThat("Datum captured", d, notNullValue());
		assertThat("Datum distance instantaneous value", d.getInstantaneousSampleBigDecimal("distance"),
				equalTo(new BigDecimal("0.017")));
	}

	private static final Pattern DEBUG_LOG_PAT = Pattern.compile(
			"# \\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z\n\\(\\d+\\.\\d{6}\\) can0 (\\d+)#([0-9A-F]*)");

	@Test
	public void frameReceived_debug() throws IOException {
		// GIVEN
		Path tmpFile = Files.createTempFile("canbus-debug-out-test-", ".log");
		dataSource.setSourceId(TEST_SOURCE);
		dataSource.setBusName("can0");
		dataSource.setDebug(true);
		dataSource.setDebugLogPath(tmpFile.toAbsolutePath().toString());

		// WHEN
		replayAll();
		FrameMessageImpl f = new FrameMessageImpl(1, false, 1, 2,
				new byte[] { (byte) 0x11, (byte) 0x00, (byte) 0xFD });
		dataSource.canbusFrameReceived(f);
		dataSource.serviceDidShutdown();

		// THEN
		String logData = FileCopyUtils.copyToString(Files.newBufferedReader(tmpFile));
		assertThat("Log data captured one line", logData, not(isEmptyOrNullString()));
		Matcher m = DEBUG_LOG_PAT.matcher(logData.trim());
		assertThat("Log line formatted with comment and timestamp, address, hex data", m.matches(),
				equalTo(true));
		assertThat("Log line address", m.group(1), equalTo("1"));
		assertThat("Log line hex data", m.group(2), equalTo("1100FD"));

		Files.deleteIfExists(tmpFile);
	}

	@Test
	public void frameReceived_subFrame() {
		// GIVEN
		CanbusMessageConfig message = new CanbusMessageConfig(1, ByteOrdering.BigEndian);
		message.addPropConfig(new CanbusPropertyConfig("watts", GeneralDatumSamplesType.Instantaneous,
				32, BitDataType.UInt32, 32, "W", null));

		dataSource.setSourceId(TEST_SOURCE);
		dataSource.setMsgConfigs(new CanbusMessageConfig[] { message });

		Capture<Event> eventCaptor = new Capture<>();
		eventAdmin.postEvent(capture(eventCaptor));

		// WHEN
		replayAll();
		long start = System.currentTimeMillis();
		FrameMessageImpl f = new FrameMessageImpl(1, false, 1, 2, decodeHexString("00000000002B5F1E"));
		dataSource.canbusFrameReceived(f);
		GeneralNodeDatum d = dataSource.readCurrentDatum();

		// THEN
		Event evt = eventCaptor.getValue();
		assertThat("Event generated", evt, notNullValue());
		assertThat("Event topic", evt.getTopic(), equalTo(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED));
		Map<String, Object> expectedData = Collections.singletonMap("watts", 2842398);
		assertDatumCapturedEvent(evt, start, TEST_SOURCE, expectedData);

		assertThat("Datum captured", d, notNullValue());
		assertThat("Datum watts instantaneous value", d.getInstantaneousSampleBigDecimal("watts"),
				equalTo(new BigDecimal("2842398")));
	}

	@Test
	public void frameReceived_valueLabel() {
		// GIVEN
		CanbusMessageConfig message = new CanbusMessageConfig(1, ByteOrdering.BigEndian);
		CanbusPropertyConfig propConfig = new CanbusPropertyConfig("watts",
				GeneralDatumSamplesType.Instantaneous, 0, BitDataType.UInt8, 8, "W", null);
		propConfig.putValueLabel("1", "One");
		propConfig.putValueLabel("2", "Two");
		propConfig.putValueLabel("3", "Three");
		message.addPropConfig(propConfig);

		dataSource.setSourceId(TEST_SOURCE);
		dataSource.setMsgConfigs(new CanbusMessageConfig[] { message });

		Capture<Event> eventCaptor = new Capture<>(CaptureType.ALL);
		eventAdmin.postEvent(capture(eventCaptor));
		expectLastCall().times(3);

		// WHEN
		replayAll();
		List<GeneralNodeDatum> datums = new ArrayList<>(3);
		long start = System.currentTimeMillis();
		dataSource.canbusFrameReceived(new FrameMessageImpl(1, false, 1, 2, new byte[] { (byte) 0x2 }));
		datums.add(dataSource.readCurrentDatum());
		dataSource.canbusFrameReceived(new FrameMessageImpl(1, false, 3, 4, new byte[] { (byte) 0x3 }));
		datums.add(dataSource.readCurrentDatum());
		dataSource.canbusFrameReceived(new FrameMessageImpl(1, false, 5, 6, new byte[] { (byte) 0x4 }));
		datums.add(dataSource.readCurrentDatum());

		// THEN
		List<Event> evts = eventCaptor.getValues();
		assertThat("Events generated", evts, hasSize(3));
		assertThat("Datum generated", datums, hasSize(3));
		for ( int i = 0; i < 3; i++ ) {
			Event evt = evts.get(i);
			assertThat("Event topic", evt.getTopic(),
					equalTo(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED));

			String expectedLabel = null;
			switch (i) {
				case 0:
					expectedLabel = "Two";
					break;

				case 1:
					expectedLabel = "Three";
					break;

				default:
					// nothing
			}

			Map<String, Object> expectedData = new LinkedHashMap<>(2);
			expectedData.put("watts", (i + 2));
			if ( expectedLabel != null ) {
				expectedData.put("wattsLabel", expectedLabel);
			}
			assertDatumCapturedEvent(evt, start, TEST_SOURCE, expectedData);

			GeneralNodeDatum d = datums.get(i);
			assertThat("Datum captured", d, notNullValue());
			assertThat("Datum watts instantaneous value", d.getInstantaneousSampleBigDecimal("watts"),
					equalTo(new BigDecimal(i + 2)));
			if ( expectedLabel != null ) {
				assertThat("Datum watts label value for " + (i + 2),
						d.getStatusSampleString("wattsLabel"), equalTo(expectedLabel));
			} else {
				assertThat("No label matched value " + (i + 2), d.getStatusSampleString("wattsLabel"),
						nullValue());
			}
		}
	}

	@Test
	public void frameReceived_withExpressions() {
		// GIVEN
		CanbusMessageConfig message = new CanbusMessageConfig(1, ByteOrdering.BigEndian);
		message.addPropConfig(new CanbusPropertyConfig("watts", GeneralDatumSamplesType.Instantaneous, 0,
				BitDataType.UInt8, 8, "W", null));

		dataSource.setSourceId(TEST_SOURCE);
		dataSource.setMsgConfigs(new CanbusMessageConfig[] { message });

		ExpressionConfig[] exprConfigs = new ExpressionConfig[] {
				new ExpressionConfig("prop-val", Instantaneous,
						"propValue(1,'UInt8','BigEndian',0,8) * 2",
						SpelExpressionService.class.getName()),
				new ExpressionConfig("prop-multiply", Instantaneous, "props['watts'] * 3",
						SpelExpressionService.class.getName()), };
		dataSource.setExpressionConfigs(exprConfigs);

		Capture<Event> eventCaptor = new Capture<>();
		eventAdmin.postEvent(capture(eventCaptor));

		// WHEN
		replayAll();
		long start = System.currentTimeMillis();
		FrameMessageImpl f = new FrameMessageImpl(1, false, 1, 2, new byte[] { (byte) 0x11 });
		dataSource.canbusFrameReceived(f);
		GeneralNodeDatum d = dataSource.readCurrentDatum();

		// THEN
		Event evt = eventCaptor.getValue();
		assertThat("Event generated", evt, notNullValue());
		assertThat("Event topic", evt.getTopic(), equalTo(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED));
		Map<String, Object> expectedData = new HashMap<>(3);
		expectedData.put("watts", 17);
		expectedData.put("prop-val", 34);
		expectedData.put("prop-multiply", 51);
		assertDatumCapturedEvent(evt, start, TEST_SOURCE, expectedData);

		assertThat("Datum captured", d, notNullValue());
		assertThat("Datum watts instantaneous value", d.getInstantaneousSampleBigDecimal("watts"),
				equalTo(new BigDecimal("17")));
		assertThat("Datum prop-val instantaneous value", d.getInstantaneousSampleBigDecimal("prop-val"),
				equalTo(new BigDecimal("34")));
		assertThat("Datum props-multiply instantaneous value",
				d.getInstantaneousSampleBigDecimal("prop-multiply"), equalTo(new BigDecimal("51")));
	}

	@Test
	public void frameReceived_withExpressions_literalPropertyVariable() {
		// GIVEN
		CanbusMessageConfig message = new CanbusMessageConfig(1, ByteOrdering.BigEndian);
		message.addPropConfig(new CanbusPropertyConfig("watts", GeneralDatumSamplesType.Instantaneous, 0,
				BitDataType.UInt8, 8, "W", null));

		dataSource.setSourceId(TEST_SOURCE);
		dataSource.setMsgConfigs(new CanbusMessageConfig[] { message });

		ExpressionConfig[] exprConfigs = new ExpressionConfig[] {
				new ExpressionConfig("prop-val", Instantaneous, "has('foo') and foo > 0 ? 1 : -1",
						SpelExpressionService.class.getName()),
				new ExpressionConfig("prop-multiply", Instantaneous, "watts * 3",
						SpelExpressionService.class.getName()), };
		dataSource.setExpressionConfigs(exprConfigs);

		Capture<Event> eventCaptor = new Capture<>();
		eventAdmin.postEvent(capture(eventCaptor));

		// WHEN
		replayAll();
		long start = System.currentTimeMillis();
		FrameMessageImpl f = new FrameMessageImpl(1, false, 1, 2, new byte[] { (byte) 0x11 });
		dataSource.canbusFrameReceived(f);
		GeneralNodeDatum d = dataSource.readCurrentDatum();

		// THEN
		Event evt = eventCaptor.getValue();
		assertThat("Event generated", evt, notNullValue());
		assertThat("Event topic", evt.getTopic(), equalTo(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED));
		Map<String, Object> expectedData = new HashMap<>(3);
		expectedData.put("watts", 17);
		expectedData.put("prop-val", -1);
		expectedData.put("prop-multiply", 51);
		assertDatumCapturedEvent(evt, start, TEST_SOURCE, expectedData);

		assertThat("Datum captured", d, notNullValue());
		assertThat("Datum watts instantaneous value", d.getInstantaneousSampleBigDecimal("watts"),
				equalTo(new BigDecimal("17")));
		assertThat("Datum props-val instantaneous value", d.getInstantaneousSampleBigDecimal("prop-val"),
				equalTo(new BigDecimal("-1")));
		assertThat("Datum props-multiply instantaneous value",
				d.getInstantaneousSampleBigDecimal("prop-multiply"), equalTo(new BigDecimal("51")));
	}

}
