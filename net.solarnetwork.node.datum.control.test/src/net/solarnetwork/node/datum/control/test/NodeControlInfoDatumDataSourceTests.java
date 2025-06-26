/* ==================================================================
 * SimpleNodeControlInfoDatumDataSourceTests.java - 10/04/2021 8:27:12 AM
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

package net.solarnetwork.node.datum.control.test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import net.solarnetwork.domain.BasicNodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.datum.control.ControlEventMode;
import net.solarnetwork.node.datum.control.NodeControlInfoDatumDataSource;
import net.solarnetwork.node.datum.control.QueuePersistMode;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleNodeControlInfoDatum;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link NodeControlInfoDatumDataSource} class.
 * 
 * @author matt
 * @version 2.0
 */
public class NodeControlInfoDatumDataSourceTests {

	private DatumQueue datumQueue;
	private NodeControlInfoDatumDataSource dataSource;
	private NodeControlProvider provider;

	@Before
	public void setup() {
		datumQueue = EasyMock.createMock(DatumQueue.class);
		provider = EasyMock.createMock(NodeControlProvider.class);
		dataSource = new NodeControlInfoDatumDataSource(new StaticOptionalService<>(datumQueue),
				singletonList(provider));
		dataSource.setEventMode(ControlEventMode.CaptureAndChange);
		dataSource.setPersistMode(QueuePersistMode.Poll);
	}

	private void replayAll(Object... other) {
		EasyMock.replay(datumQueue, provider);
		if ( other != null ) {
			EasyMock.replay(other);
		}
	}

	@After
	public void teardown() {
		EasyMock.verify(datumQueue, provider);
	}

	private BasicNodeControlInfo.Builder createControlInfoBuilder() {
		// @formatter:off
		return BasicNodeControlInfo.builder()
				.withControlId("test.source")
				.withType(NodeControlPropertyType.Integer)
				.withValue("1")
				.withReadonly(false);
		// @formatter:on
	}

	private SimpleNodeControlInfoDatum createControlInfo(BasicNodeControlInfo.Builder info) {
		return new SimpleNodeControlInfoDatum(info.build(), Instant.now());
	}

	private Event createDatumEvent(String topic, SimpleNodeControlInfoDatum info) {
		return DatumEvents.datumEvent(topic, info);
	}

	private SimpleNodeControlInfoDatum createControlInfo() {
		return createControlInfo(createControlInfoBuilder());
	}

	@Test
	public void handleChanged() {
		// GIVEN
		Capture<NodeDatum> datumCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(datumCaptor), eq(false))).andReturn(true);

		// WHEN
		replayAll();
		SimpleNodeControlInfoDatum info = createControlInfo();
		Event event = createDatumEvent(NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED, info);
		dataSource.handleEvent(event);

		// THEN
		NodeDatum d = datumCaptor.getValue();
		assertThat("Datum persisted", d, notNullValue());
		assertThat("Created date near event date",
				d.getTimestamp().toEpochMilli() - info.getTimestamp().toEpochMilli(),
				allOf(greaterThanOrEqualTo(0L), lessThanOrEqualTo(100L)));
		assertThat("Control ID -> source ID", d.getSourceId(), equalTo(info.getControlId()));
		assertThat("Integer value as instantaneous property v",
				d.asSampleOperations().getSampleInteger(Instantaneous, "v"), equalTo(1));
		assertThat("Integer value as status property val",
				d.asSampleOperations().getSampleInteger(Status, "val"), equalTo(1));
	}

	@Test
	public void handleChanged_persist() {
		// GIVEN
		dataSource.setPersistMode(QueuePersistMode.PollAndEvent);
		Capture<NodeDatum> datumCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(datumCaptor), eq(true))).andReturn(true);

		// WHEN
		replayAll();
		SimpleNodeControlInfoDatum info = createControlInfo();
		Event event = createDatumEvent(NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED, info);
		dataSource.handleEvent(event);

		// THEN
		NodeDatum d = datumCaptor.getValue();
		assertThat("Datum persisted", d, notNullValue());
		assertThat("Created date near event date",
				d.getTimestamp().toEpochMilli() - info.getTimestamp().toEpochMilli(),
				allOf(greaterThanOrEqualTo(0L), lessThanOrEqualTo(100L)));
		assertThat("Control ID -> source ID", d.getSourceId(), equalTo(info.getControlId()));
		assertThat("Integer value as instantaneous property v",
				d.asSampleOperations().getSampleInteger(Instantaneous, "v"), equalTo(1));
		assertThat("Integer value as status property val",
				d.asSampleOperations().getSampleInteger(Status, "val"), equalTo(1));
	}

	@Test
	public void handleChanged_explicitPropertyName() {
		// GIVEN
		Capture<NodeDatum> datumCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(datumCaptor), eq(false))).andReturn(true);

		// WHEN
		replayAll();
		SimpleNodeControlInfoDatum info = createControlInfo(
				createControlInfoBuilder().withPropertyName("foo"));
		Event event = createDatumEvent(NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED, info);
		dataSource.handleEvent(event);

		// THEN
		NodeDatum d = datumCaptor.getValue();
		assertThat("Datum persisted", d, notNullValue());
		assertThat("Created date near event date",
				d.getTimestamp().toEpochMilli() - info.getTimestamp().toEpochMilli(),
				allOf(greaterThanOrEqualTo(0L), lessThanOrEqualTo(100L)));
		assertThat("Control ID -> source ID", d.getSourceId(), equalTo(info.getControlId()));
		assertThat("Integer value as explicit status property foo",
				d.asSampleOperations().getSampleInteger(Status, "foo"), equalTo(1));
	}

	@Test
	public void handleChanged_controlIdFilter_match() {
		// GIVEN
		dataSource.setControlIdRegexValue("^test");
		Capture<NodeDatum> datumCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(datumCaptor), eq(false))).andReturn(true);

		// WHEN
		replayAll();
		SimpleNodeControlInfoDatum info = createControlInfo();
		Event event = createDatumEvent(NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED, info);
		dataSource.handleEvent(event);

		// THEN
		NodeDatum d = datumCaptor.getValue();
		assertThat("Datum persisted", d, notNullValue());
		assertThat("Created date near event date",
				d.getTimestamp().toEpochMilli() - info.getTimestamp().toEpochMilli(),
				allOf(greaterThanOrEqualTo(0L), lessThanOrEqualTo(100L)));
		assertThat("Control ID -> source ID", d.getSourceId(), equalTo(info.getControlId()));
		assertThat("Integer value as instantaneous property v",
				d.asSampleOperations().getSampleInteger(Instantaneous, "v"), equalTo(1));
		assertThat("Integer value as status property val",
				d.asSampleOperations().getSampleInteger(Status, "val"), equalTo(1));
	}

	@Test
	public void handleChanged_controlIdFilter_filtered() {
		// GIVEN
		dataSource.setControlIdRegexValue("^not");

		// WHEN
		replayAll();
		SimpleNodeControlInfoDatum info = createControlInfo();
		Event event = createDatumEvent(NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED, info);
		dataSource.handleEvent(event);

		// THEN
	}

	@Test
	public void handleCaptured() {
		// GIVEN
		Capture<NodeDatum> datumCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(datumCaptor), eq(false))).andReturn(true);

		// WHEN
		replayAll();
		SimpleNodeControlInfoDatum info = createControlInfo();
		Event event = createDatumEvent(NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CAPTURED, info);
		dataSource.handleEvent(event);

		// THEN
		NodeDatum d = datumCaptor.getValue();
		assertThat("Datum persisted", d, notNullValue());
		assertThat("Created date near event date",
				d.getTimestamp().toEpochMilli() - info.getTimestamp().toEpochMilli(),
				allOf(greaterThanOrEqualTo(0L), lessThanOrEqualTo(100L)));
		assertThat("Control ID -> source ID", d.getSourceId(), equalTo(info.getControlId()));
		assertThat("Integer value as instantaneous property v",
				d.asSampleOperations().getSampleInteger(Instantaneous, "v"), equalTo(1));
		assertThat("Integer value as status property val",
				d.asSampleOperations().getSampleInteger(Status, "val"), equalTo(1));
	}

	@Test
	public void handleCapture_eventMode_filtered() {
		// GIVEN
		dataSource.setEventMode(ControlEventMode.Change);

		// WHEN
		replayAll();
		SimpleNodeControlInfoDatum info = createControlInfo();
		Event event = createDatumEvent(NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CAPTURED, info);
		dataSource.handleEvent(event);

		// THEN
	}

	@Test
	public void handleUnsuportedEvent_filtered() {
		// GIVEN

		// WHEN
		replayAll();
		SimpleNodeControlInfoDatum info = createControlInfo();
		Event event = createDatumEvent("rando/event", info);
		dataSource.handleEvent(event);

		// THEN
	}

	@Test
	public void poll() {
		// GIVEN
		final String controlId = UUID.randomUUID().toString();
		final SimpleNodeControlInfoDatum info = createControlInfo(
				createControlInfoBuilder().withControlId(controlId));
		expect(provider.getAvailableControlIds()).andReturn(singletonList(controlId));
		expect(provider.getCurrentControlInfo(controlId)).andReturn(info);

		// WHEN
		replayAll();
		Collection<NodeDatum> result = dataSource.readMultipleDatum();

		// THEN
		assertThat("Result has datum", result, hasSize(1));
		NodeDatum d = result.iterator().next();
		assertThat("Datum that returned from Provider", d, sameInstance(info));
	}

	@Test
	public void poll_idPattern_match() {
		// GIVEN
		dataSource.setControlIdRegexValue("^test");
		final String controlId = "test.source";
		final SimpleNodeControlInfoDatum info = createControlInfo(
				createControlInfoBuilder().withControlId(controlId));
		expect(provider.getAvailableControlIds()).andReturn(singletonList(controlId));
		expect(provider.getCurrentControlInfo(controlId)).andReturn(info);

		// WHEN
		replayAll();
		Collection<NodeDatum> result = dataSource.readMultipleDatum();

		// THEN
		assertThat("Result has datum", result, hasSize(1));
		NodeDatum d = result.iterator().next();
		assertThat("Datum that returned from Provider", d, sameInstance(info));
	}

	@Test
	public void poll_idPattern_nomatch() {
		// GIVEN
		dataSource.setControlIdRegexValue("^nope");
		final String controlId = "test.source";
		expect(provider.getAvailableControlIds()).andReturn(singletonList(controlId));

		// WHEN
		replayAll();
		Collection<NodeDatum> result = dataSource.readMultipleDatum();

		// THEN
		assertThat("Result has datum", result, hasSize(0));
	}

	@Test
	public void poll_idPattern_multimatch() {
		// GIVEN
		NodeControlProvider p2 = EasyMock.createMock(NodeControlProvider.class);
		dataSource = new NodeControlInfoDatumDataSource(dataSource.getDatumQueue(),
				Arrays.asList(provider, p2));
		dataSource.setEventMode(ControlEventMode.CaptureAndChange);
		dataSource.setPersistMode(QueuePersistMode.Poll);
		dataSource.setControlIdRegexValue("^test");

		final String controlId1 = "test.source.1";
		final String controlId2 = "nope.source";
		final String controlId3 = "test.source.2";

		final SimpleNodeControlInfoDatum info1 = createControlInfo(
				createControlInfoBuilder().withControlId(controlId1));
		expect(provider.getAvailableControlIds()).andReturn(singletonList(controlId1));
		expect(provider.getCurrentControlInfo(controlId1)).andReturn(info1);

		final SimpleNodeControlInfoDatum info2 = createControlInfo(
				createControlInfoBuilder().withControlId(controlId3));
		expect(p2.getAvailableControlIds()).andReturn(asList(controlId2, controlId3));
		expect(p2.getCurrentControlInfo(controlId3)).andReturn(info2);

		// WHEN
		replayAll(p2);
		Collection<NodeDatum> result = dataSource.readMultipleDatum();

		// THEN
		assertThat("Result has datum for matching control IDs", result, hasSize(2));
		List<NodeDatum> list = result.stream().collect(Collectors.toList());
		assertThat("Datum that returned from Provider", list.get(0), sameInstance(info1));
		assertThat("Datum that returned from Provider", list.get(1), sameInstance(info2));

		EasyMock.verify(p2);
	}

}
