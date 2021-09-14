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

import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.time.Instant;
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

	@Before
	public void setup() {
		datumQueue = EasyMock.createMock(DatumQueue.class);
		dataSource = new NodeControlInfoDatumDataSource(new StaticOptionalService<>(datumQueue));
		dataSource.setEventMode(ControlEventMode.CaptureAndChange);
	}

	private void replayAll() {
		EasyMock.replay(datumQueue);
	}

	@After
	public void teardown() {
		EasyMock.verify(datumQueue);
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
		Capture<NodeDatum> datumCaptor = new Capture<>();
		expect(datumQueue.offer(capture(datumCaptor))).andReturn(true);

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
		Capture<NodeDatum> datumCaptor = new Capture<>();
		expect(datumQueue.offer(capture(datumCaptor))).andReturn(true);

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
		Capture<NodeDatum> datumCaptor = new Capture<>();
		expect(datumQueue.offer(capture(datumCaptor))).andReturn(true);

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
		Capture<NodeDatum> datumCaptor = new Capture<>();
		expect(datumQueue.offer(capture(datumCaptor))).andReturn(true);

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
}
