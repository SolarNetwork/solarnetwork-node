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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.io.canbus.socketcand.msg.FrameMessageImpl;
import net.solarnetwork.node.io.canbus.support.MeasurementHelper;
import net.solarnetwork.util.StaticOptionalService;
import net.solarnetwork.util.StaticOptionalServiceCollection;
import systems.uom.ucum.internal.UCUMServiceProvider;

/**
 * Test cases for the {@link CanbusDatumDataSource} class.
 * 
 * @author matt
 * @version 1.0
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
				Arrays.asList(new IndriyaMeasurementServiceProvider(new UCUMServiceProvider())))));
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
		Set<String> expectedKeys = new HashSet<>(5 + expectedData.size());
		expectedKeys.add("event.topics");
		expectedKeys.add("_DatumType");
		expectedKeys.add("_DatumTypes");
		expectedKeys.add("created");
		expectedKeys.add("sourceId");
		expectedKeys.addAll(expectedData.keySet());
		assertThat("Event property keys", evtProps.keySet(), equalTo(expectedKeys));
		assertThat("Event creation date at least", (Long) evtProps.get("created"),
				greaterThanOrEqualTo(minDate));
		for ( Map.Entry<String, Object> me : expectedData.entrySet() ) {
			assertThat("Event property " + me.getKey() + " value", evtProps.get(me.getKey()),
					equalTo(me.getValue()));
		}
	}

	@Test
	public void frameReceived_basic() {
		// GIVEN
		CanbusMessageConfig message = new CanbusMessageConfig(1, ByteOrdering.BigEndian);
		message.addPropConfig(new CanbusPropertyConfig("watts", GeneralDatumSamplesType.Instantaneous, 0,
				BitDataType.UInt8, 8, "W"));

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
		Map<String, Object> expectedData = Collections.singletonMap("watts", new BigDecimal("17"));
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
				GeneralDatumSamplesType.Instantaneous, 0, BitDataType.UInt8, 8, "W");
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
				BitDataType.UInt8, 8, "kW"));

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
}
