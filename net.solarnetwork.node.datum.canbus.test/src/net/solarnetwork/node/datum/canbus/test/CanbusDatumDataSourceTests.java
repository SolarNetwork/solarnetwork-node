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

import static net.solarnetwork.domain.datum.DatumSamplesType.Accumulating;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import static net.solarnetwork.util.ByteUtils.decodeHexString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.common.expr.spel.SpelExpressionService;
import net.solarnetwork.domain.BitDataType;
import net.solarnetwork.domain.ByteOrdering;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.external.indriya.IndriyaMeasurementServiceProvider;
import net.solarnetwork.node.datum.canbus.CanbusDatumDataSource;
import net.solarnetwork.node.datum.canbus.CanbusMessageConfig;
import net.solarnetwork.node.datum.canbus.CanbusPropertyConfig;
import net.solarnetwork.node.datum.canbus.ExpressionConfig;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.io.canbus.socketcand.msg.FrameMessageImpl;
import net.solarnetwork.node.io.canbus.support.MeasurementHelper;
import net.solarnetwork.node.service.DatumMetadataService;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.service.OptionalServiceCollection;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.service.StaticOptionalServiceCollection;
import systems.uom.ucum.spi.UCUMServiceProvider;
import tech.units.indriya.spi.DefaultServiceProvider;

/**
 * Test cases for the {@link CanbusDatumDataSource} class.
 * 
 * @author matt
 * @version 2.0
 */
public class CanbusDatumDataSourceTests {

	private static final String TEST_SOURCE = "/test/source";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private DatumQueue datumQueue;
	private DatumMetadataService datumMetadataService;
	private CanbusDatumDataSource dataSource;

	@Before
	public void setup() {
		datumQueue = EasyMock.createMock(DatumQueue.class);
		datumMetadataService = EasyMock.createMock(DatumMetadataService.class);

		dataSource = new CanbusDatumDataSource();
		dataSource.setDatumQueue(new StaticOptionalService<>(datumQueue));
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
		EasyMock.verify(datumQueue, datumMetadataService);
	}

	private void replayAll() {
		EasyMock.replay(datumQueue, datumMetadataService);
	}

	@Test
	public void generateMetadata() {
		// GIVEN
		CanbusMessageConfig message = new CanbusMessageConfig();
		CanbusPropertyConfig prop1 = new CanbusPropertyConfig("watts", DatumSamplesType.Instantaneous,
				0);
		prop1.setLocalizedNames(new KeyValuePair[] { new KeyValuePair("en", "Foo Bar"),
				new KeyValuePair("zh-Hans", "Foo酒吧") });
		prop1.setUnit("kW");
		message.setPropConfigs(new CanbusPropertyConfig[] { prop1 });

		dataSource.setSourceId(TEST_SOURCE);
		dataSource.setMsgConfigs(new CanbusMessageConfig[] { message });

		Capture<GeneralDatumMetadata> metaCaptor = Capture.newInstance();
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

	private void assertDatumCapturedEvent(NodeDatum datum, long minDate, String sourceId,
			DatumSamplesOperations expectedData) {
		assertThat("Event generated", datum, notNullValue());
		log.debug("Got datum captured event: {}", datum);
		assertThat("Event source ID", datum.getSourceId(), is(sourceId));
		assertThat("Event creation date at least", datum.getTimestamp().toEpochMilli(),
				greaterThanOrEqualTo(minDate));
		DatumSamplesOperations expectedOps = datum.asSampleOperations();
		DatumSamplesOperations datumOps = datum.asSampleOperations();
		for ( DatumSamplesType type : EnumSet.of(Instantaneous, Accumulating, Status) ) {
			Map<String, ?> data = expectedOps.getSampleData(type);
			if ( data == null ) {
				continue;
			}
			for ( String propName : data.keySet() ) {
				assertThat(String.format("Event datum property %s value", propName),
						datumOps.getSampleBigDecimal(type, propName),
						equalTo(expectedOps.getSampleBigDecimal(type, propName)));
			}
		}
	}

	@Test
	public void frameReceived_basic() {
		// GIVEN
		CanbusMessageConfig message = new CanbusMessageConfig(1, ByteOrdering.BigEndian);
		message.addPropConfig(new CanbusPropertyConfig("watts", DatumSamplesType.Instantaneous, 0,
				BitDataType.UInt8, 8, "W", null));

		dataSource.setSourceId(TEST_SOURCE);
		dataSource.setMsgConfigs(new CanbusMessageConfig[] { message });

		Capture<NodeDatum> eventCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(eventCaptor), eq(false))).andReturn(true);

		// WHEN
		replayAll();
		long start = System.currentTimeMillis();
		FrameMessageImpl f = new FrameMessageImpl(1, false, 1, 2, new byte[] { (byte) 0x11 });
		dataSource.canbusFrameReceived(f);
		NodeDatum d = dataSource.readCurrentDatum();

		// THEN
		DatumSamples expectedData = new DatumSamples();
		expectedData.putInstantaneousSampleValue("watts", 17);
		assertDatumCapturedEvent(eventCaptor.getValue(), start, TEST_SOURCE, expectedData);

		assertThat("Datum captured", d, notNullValue());
		assertThat("Datum watts instantaneous value",
				d.asSampleOperations().getSampleBigDecimal(Instantaneous, "watts"),
				equalTo(new BigDecimal("17")));
	}

	@Test
	public void frameReceived_transformed() {
		// GIVEN
		CanbusMessageConfig message = new CanbusMessageConfig(1, ByteOrdering.BigEndian);
		CanbusPropertyConfig prop = new CanbusPropertyConfig("watts", DatumSamplesType.Instantaneous, 0,
				BitDataType.UInt8, 8, "W", null);
		prop.setSlope(new BigDecimal("0.1"));
		prop.setIntercept(new BigDecimal("33"));
		message.addPropConfig(prop);

		dataSource.setSourceId(TEST_SOURCE);
		dataSource.setMsgConfigs(new CanbusMessageConfig[] { message });

		Capture<NodeDatum> eventCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(eventCaptor), eq(false))).andReturn(true);

		// WHEN
		replayAll();
		long start = System.currentTimeMillis();
		FrameMessageImpl f = new FrameMessageImpl(1, false, 1, 2, new byte[] { (byte) 0x11 });
		dataSource.canbusFrameReceived(f);
		NodeDatum d = dataSource.readCurrentDatum();

		// THEN
		DatumSamples expectedData = new DatumSamples();
		expectedData.putInstantaneousSampleValue("watts", new BigDecimal("34.7"));
		assertDatumCapturedEvent(eventCaptor.getValue(), start, TEST_SOURCE, expectedData);

		assertThat("Datum captured", d, notNullValue());
		assertThat("Datum watts instantaneous value",
				d.asSampleOperations().getSampleBigDecimal(Instantaneous, "watts"),
				equalTo(new BigDecimal("34.7")));
	}

	@Test
	public void frameReceived_kilo() {
		// GIVEN
		CanbusMessageConfig message = new CanbusMessageConfig(1, ByteOrdering.BigEndian);
		// the raw data is in kW, which should be automagically parsed into W
		message.addPropConfig(new CanbusPropertyConfig("watts", DatumSamplesType.Instantaneous, 0,
				BitDataType.UInt8, 8, "kW", null));

		dataSource.setSourceId(TEST_SOURCE);
		dataSource.setMsgConfigs(new CanbusMessageConfig[] { message });

		Capture<NodeDatum> eventCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(eventCaptor), eq(false))).andReturn(true);

		// WHEN
		replayAll();
		long start = System.currentTimeMillis();
		FrameMessageImpl f = new FrameMessageImpl(1, false, 1, 2, new byte[] { (byte) 0x11 });
		dataSource.canbusFrameReceived(f);
		NodeDatum d = dataSource.readCurrentDatum();

		// THEN
		DatumSamples expectedData = new DatumSamples();
		expectedData.putInstantaneousSampleValue("watts", 17000);
		assertDatumCapturedEvent(eventCaptor.getValue(), start, TEST_SOURCE, expectedData);

		assertThat("Datum captured", d, notNullValue());
		assertThat("Datum watts instantaneous value",
				d.asSampleOperations().getSampleBigDecimal(Instantaneous, "watts"),
				equalTo(new BigDecimal("17000")));
	}

	@Test
	public void frameReceived_customNormalization_identity() {
		// GIVEN
		CanbusMessageConfig message = new CanbusMessageConfig(1, ByteOrdering.BigEndian);
		message.addPropConfig(new CanbusPropertyConfig("torque", DatumSamplesType.Instantaneous, 0,
				BitDataType.UInt8, 8, "N.m", "N.m"));

		dataSource.setSourceId(TEST_SOURCE);
		dataSource.setMsgConfigs(new CanbusMessageConfig[] { message });

		Capture<NodeDatum> eventCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(eventCaptor), eq(false))).andReturn(true);

		// WHEN
		replayAll();
		long start = System.currentTimeMillis();
		FrameMessageImpl f = new FrameMessageImpl(1, false, 1, 2, new byte[] { (byte) 0x11 });
		dataSource.canbusFrameReceived(f);
		NodeDatum d = dataSource.readCurrentDatum();

		// THEN
		DatumSamples expectedData = new DatumSamples();
		expectedData.putInstantaneousSampleValue("torque", 17);
		assertDatumCapturedEvent(eventCaptor.getValue(), start, TEST_SOURCE, expectedData);

		assertThat("Datum captured", d, notNullValue());
		assertThat("Datum distance instantaneous value",
				d.asSampleOperations().getSampleBigDecimal(Instantaneous, "torque"),
				equalTo(new BigDecimal("17")));
	}

	@Test
	public void frameReceived_customNormalization_converted() {
		// GIVEN
		CanbusMessageConfig message = new CanbusMessageConfig(1, ByteOrdering.BigEndian);
		message.addPropConfig(new CanbusPropertyConfig("distance", DatumSamplesType.Instantaneous, 0,
				BitDataType.UInt8, 8, "m", "km"));

		dataSource.setSourceId(TEST_SOURCE);
		dataSource.setMsgConfigs(new CanbusMessageConfig[] { message });

		Capture<NodeDatum> eventCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(eventCaptor), eq(false))).andReturn(true);

		// WHEN
		replayAll();
		long start = System.currentTimeMillis();
		FrameMessageImpl f = new FrameMessageImpl(1, false, 1, 2, new byte[] { (byte) 0x11 });
		dataSource.canbusFrameReceived(f);
		NodeDatum d = dataSource.readCurrentDatum();

		// THEN
		DatumSamples expectedData = new DatumSamples();
		expectedData.putInstantaneousSampleValue("distance", new BigDecimal("0.017"));
		assertDatumCapturedEvent(eventCaptor.getValue(), start, TEST_SOURCE, expectedData);

		assertThat("Datum captured", d, notNullValue());
		assertThat("Datum distance instantaneous value",
				d.asSampleOperations().getSampleBigDecimal(Instantaneous, "distance"),
				equalTo(new BigDecimal("0.017")));
	}

	@Test
	public void frameReceived_subFrame() {
		// GIVEN
		CanbusMessageConfig message = new CanbusMessageConfig(1, ByteOrdering.BigEndian);
		message.addPropConfig(new CanbusPropertyConfig("watts", DatumSamplesType.Instantaneous, 32,
				BitDataType.UInt32, 32, "W", null));

		dataSource.setSourceId(TEST_SOURCE);
		dataSource.setMsgConfigs(new CanbusMessageConfig[] { message });

		Capture<NodeDatum> eventCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(eventCaptor), eq(false))).andReturn(true);

		// WHEN
		replayAll();
		long start = System.currentTimeMillis();
		FrameMessageImpl f = new FrameMessageImpl(1, false, 1, 2, decodeHexString("00000000002B5F1E"));
		dataSource.canbusFrameReceived(f);
		NodeDatum d = dataSource.readCurrentDatum();

		// THEN
		DatumSamples expectedData = new DatumSamples();
		expectedData.putInstantaneousSampleValue("watts", 2842398);
		assertDatumCapturedEvent(eventCaptor.getValue(), start, TEST_SOURCE, expectedData);

		assertThat("Datum captured", d, notNullValue());
		assertThat("Datum watts instantaneous value",
				d.asSampleOperations().getSampleBigDecimal(Instantaneous, "watts"),
				equalTo(new BigDecimal("2842398")));
	}

	@Test
	public void frameReceived_valueLabel() {
		// GIVEN
		CanbusMessageConfig message = new CanbusMessageConfig(1, ByteOrdering.BigEndian);
		CanbusPropertyConfig propConfig = new CanbusPropertyConfig("watts",
				DatumSamplesType.Instantaneous, 0, BitDataType.UInt8, 8, "W", null);
		propConfig.putValueLabel("1", "One");
		propConfig.putValueLabel("2", "Two");
		propConfig.putValueLabel("3", "Three");
		message.addPropConfig(propConfig);

		dataSource.setSourceId(TEST_SOURCE);
		dataSource.setMsgConfigs(new CanbusMessageConfig[] { message });

		Capture<NodeDatum> eventCaptor = Capture.newInstance(CaptureType.ALL);
		expect(datumQueue.offer(capture(eventCaptor), eq(false))).andReturn(true).times(3);

		// WHEN
		replayAll();
		List<NodeDatum> datums = new ArrayList<>(3);
		long start = System.currentTimeMillis();
		dataSource.canbusFrameReceived(new FrameMessageImpl(1, false, 1, 2, new byte[] { (byte) 0x2 }));
		datums.add(dataSource.readCurrentDatum());
		dataSource.canbusFrameReceived(new FrameMessageImpl(1, false, 3, 4, new byte[] { (byte) 0x3 }));
		datums.add(dataSource.readCurrentDatum());
		dataSource.canbusFrameReceived(new FrameMessageImpl(1, false, 5, 6, new byte[] { (byte) 0x4 }));
		datums.add(dataSource.readCurrentDatum());

		// THEN
		List<NodeDatum> evts = eventCaptor.getValues();
		assertThat("Events generated", evts, hasSize(3));
		assertThat("Datum generated", datums, hasSize(3));
		for ( int i = 0; i < 3; i++ ) {
			NodeDatum evt = evts.get(i);

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

			DatumSamples expectedData = new DatumSamples();
			expectedData.putInstantaneousSampleValue("watts", (i + 2));
			if ( expectedLabel != null ) {
				expectedData.putStatusSampleValue("wattsLabel", expectedLabel);
			}
			assertDatumCapturedEvent(evt, start, TEST_SOURCE, expectedData);

			NodeDatum d = datums.get(i);
			assertThat("Datum captured", d, notNullValue());
			assertThat("Datum watts instantaneous value",
					d.asSampleOperations().getSampleBigDecimal(Instantaneous, "watts"),
					equalTo(new BigDecimal(i + 2)));
			if ( expectedLabel != null ) {
				assertThat("Datum watts label value for " + (i + 2),
						d.asSampleOperations().getSampleString(Status, "wattsLabel"),
						equalTo(expectedLabel));
			} else {
				assertThat("No label matched value " + (i + 2),
						d.asSampleOperations().getSampleString(Status, "wattsLabel"), nullValue());
			}
		}
	}

	@Test
	public void frameReceived_withExpressions() {
		// GIVEN
		CanbusMessageConfig message = new CanbusMessageConfig(1, ByteOrdering.BigEndian);
		message.addPropConfig(new CanbusPropertyConfig("watts", DatumSamplesType.Instantaneous, 0,
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

		Capture<NodeDatum> eventCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(eventCaptor), eq(false))).andReturn(true);

		// WHEN
		replayAll();
		long start = System.currentTimeMillis();
		FrameMessageImpl f = new FrameMessageImpl(1, false, 1, 2, new byte[] { (byte) 0x11 });
		dataSource.canbusFrameReceived(f);
		NodeDatum d = dataSource.readCurrentDatum();

		// THEN
		DatumSamples expectedData = new DatumSamples();
		expectedData.putInstantaneousSampleValue("watts", 17);
		expectedData.putInstantaneousSampleValue("prop-val", 34);
		expectedData.putInstantaneousSampleValue("prop-multiply", 51);
		assertDatumCapturedEvent(eventCaptor.getValue(), start, TEST_SOURCE, expectedData);

		assertThat("Datum captured", d, notNullValue());
		assertThat("Datum watts instantaneous value",
				d.asSampleOperations().getSampleBigDecimal(Instantaneous, "watts"),
				equalTo(new BigDecimal("17")));
		assertThat("Datum prop-val instantaneous value",
				d.asSampleOperations().getSampleBigDecimal(Instantaneous, "prop-val"),
				equalTo(new BigDecimal("34")));
		assertThat("Datum props-multiply instantaneous value",
				d.asSampleOperations().getSampleBigDecimal(Instantaneous, "prop-multiply"),
				equalTo(new BigDecimal("51")));
	}

	@Test
	public void frameReceived_withExpressions_literalPropertyVariable() {
		// GIVEN
		CanbusMessageConfig message = new CanbusMessageConfig(1, ByteOrdering.BigEndian);
		message.addPropConfig(new CanbusPropertyConfig("watts", DatumSamplesType.Instantaneous, 0,
				BitDataType.UInt8, 8, "W", null));

		dataSource.setSourceId(TEST_SOURCE);
		dataSource.setMsgConfigs(new CanbusMessageConfig[] { message });

		ExpressionConfig[] exprConfigs = new ExpressionConfig[] {
				new ExpressionConfig("prop-val", Instantaneous, "has('foo') and foo > 0 ? 1 : -1",
						SpelExpressionService.class.getName()),
				new ExpressionConfig("prop-multiply", Instantaneous, "watts * 3",
						SpelExpressionService.class.getName()), };
		dataSource.setExpressionConfigs(exprConfigs);

		Capture<NodeDatum> eventCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(eventCaptor), eq(false))).andReturn(true);

		// WHEN
		replayAll();
		long start = System.currentTimeMillis();
		FrameMessageImpl f = new FrameMessageImpl(1, false, 1, 2, new byte[] { (byte) 0x11 });
		dataSource.canbusFrameReceived(f);
		NodeDatum d = dataSource.readCurrentDatum();

		// THEN
		DatumSamples expectedData = new DatumSamples();
		expectedData.putInstantaneousSampleValue("watts", 17);
		expectedData.putInstantaneousSampleValue("prop-val", -1);
		expectedData.putInstantaneousSampleValue("prop-multiply", 51);
		assertDatumCapturedEvent(eventCaptor.getValue(), start, TEST_SOURCE, expectedData);

		assertThat("Datum captured", d, notNullValue());
		assertThat("Datum watts instantaneous value",
				d.asSampleOperations().getSampleBigDecimal(Instantaneous, "watts"),
				equalTo(new BigDecimal("17")));
		assertThat("Datum props-val instantaneous value",
				d.asSampleOperations().getSampleBigDecimal(Instantaneous, "prop-val"),
				equalTo(new BigDecimal("-1")));
		assertThat("Datum props-multiply instantaneous value",
				d.asSampleOperations().getSampleBigDecimal(Instantaneous, "prop-multiply"),
				equalTo(new BigDecimal("51")));
	}

}
