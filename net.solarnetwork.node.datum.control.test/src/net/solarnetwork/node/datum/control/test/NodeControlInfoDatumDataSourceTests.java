/* ==================================================================
 * NodeControlInfoDatumDataSourceTests.java - 10/04/2021 8:27:12 AM
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

import static org.easymock.EasyMock.capture;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.Date;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.NodeControlProvider;
import net.solarnetwork.node.dao.DatumDao;
import net.solarnetwork.node.datum.control.ControlEventMode;
import net.solarnetwork.node.datum.control.NodeControlInfoDatumDataSource;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.domain.NodeControlInfoDatum;
import net.solarnetwork.node.support.DatumEvents;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link NodeControlInfoDatumDataSource} class.
 * 
 * @author matt
 * @version 1.0
 */
public class NodeControlInfoDatumDataSourceTests {

	private DatumDao<GeneralNodeDatum> datumDao;
	private NodeControlInfoDatumDataSource dataSource;

	@SuppressWarnings("unchecked")
	@Before
	public void setup() {
		datumDao = EasyMock.createMock(DatumDao.class);
		dataSource = new NodeControlInfoDatumDataSource();
		dataSource.setDatumDao(new StaticOptionalService<>(datumDao));
		dataSource.setEventMode(ControlEventMode.CaptureAndChange);
	}

	private void replayAll() {
		EasyMock.replay(datumDao);
	}

	@After
	public void teardown() {
		EasyMock.verify(datumDao);
	}

	private NodeControlInfoDatum createControlInfo() {
		NodeControlInfoDatum info = new NodeControlInfoDatum();
		info.setSourceId("test.source");
		info.setCreated(new Date());
		info.setType(NodeControlPropertyType.Integer);
		info.setValue("1");
		info.setReadonly(false);
		return info;
	}

	private Event createDatumEvent(String topic, NodeControlInfoDatum info) {
		return DatumEvents.datumEvent(topic, info);
	}

	@Test
	public void handleChanged() {
		// GIVEN
		Capture<GeneralNodeDatum> datumCaptor = new Capture<>();
		datumDao.storeDatum(capture(datumCaptor));

		// WHEN
		replayAll();
		NodeControlInfoDatum info = createControlInfo();
		Event event = createDatumEvent(NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED, info);
		dataSource.handleEvent(event);

		// THEN
		GeneralNodeDatum d = datumCaptor.getValue();
		assertThat("Datum persisted", d, notNullValue());
		assertThat("Created date near event date",
				d.getCreated().getTime() - info.getCreated().getTime(),
				allOf(greaterThanOrEqualTo(0L), lessThanOrEqualTo(100L)));
		assertThat("Control ID -> source ID", d.getSourceId(), equalTo(info.getControlId()));
		assertThat("Integer value as instantaneous property v", d.getInstantaneousSampleInteger("v"),
				equalTo(1));
		assertThat("Integer value as status property val", d.getStatusSampleInteger("val"), equalTo(1));
	}

	@Test
	public void handleChanged_explicitPropertyName() {
		// GIVEN
		Capture<GeneralNodeDatum> datumCaptor = new Capture<>();
		datumDao.storeDatum(capture(datumCaptor));

		// WHEN
		replayAll();
		NodeControlInfoDatum info = createControlInfo();
		info.setPropertyName("foo");
		Event event = createDatumEvent(NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED, info);
		dataSource.handleEvent(event);

		// THEN
		GeneralNodeDatum d = datumCaptor.getValue();
		assertThat("Datum persisted", d, notNullValue());
		assertThat("Created date near event date",
				d.getCreated().getTime() - info.getCreated().getTime(),
				allOf(greaterThanOrEqualTo(0L), lessThanOrEqualTo(100L)));
		assertThat("Control ID -> source ID", d.getSourceId(), equalTo(info.getControlId()));
		assertThat("Integer value as explicit status property foo", d.getStatusSampleInteger("foo"),
				equalTo(1));
	}

	@Test
	public void handleChanged_controlIdFilter_match() {
		// GIVEN
		dataSource.setControlIdRegexValue("^test");
		Capture<GeneralNodeDatum> datumCaptor = new Capture<>();
		datumDao.storeDatum(capture(datumCaptor));

		// WHEN
		replayAll();
		NodeControlInfoDatum info = createControlInfo();
		Event event = createDatumEvent(NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED, info);
		dataSource.handleEvent(event);

		// THEN
		GeneralNodeDatum d = datumCaptor.getValue();
		assertThat("Datum persisted", d, notNullValue());
		assertThat("Created date near event date",
				d.getCreated().getTime() - info.getCreated().getTime(),
				allOf(greaterThanOrEqualTo(0L), lessThanOrEqualTo(100L)));
		assertThat("Control ID -> source ID", d.getSourceId(), equalTo(info.getControlId()));
		assertThat("Integer value as instantaneous property v", d.getInstantaneousSampleInteger("v"),
				equalTo(1));
		assertThat("Integer value as status property val", d.getStatusSampleInteger("val"), equalTo(1));
	}

	@Test
	public void handleChanged_controlIdFilter_filtered() {
		// GIVEN
		dataSource.setControlIdRegexValue("^not");

		// WHEN
		replayAll();
		NodeControlInfoDatum info = createControlInfo();
		Event event = createDatumEvent(NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED, info);
		dataSource.handleEvent(event);

		// THEN
	}

	@Test
	public void handleCaptured() {
		// GIVEN
		Capture<GeneralNodeDatum> datumCaptor = new Capture<>();
		datumDao.storeDatum(capture(datumCaptor));

		// WHEN
		replayAll();
		NodeControlInfoDatum info = createControlInfo();
		Event event = createDatumEvent(NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CAPTURED, info);
		dataSource.handleEvent(event);

		// THEN
		GeneralNodeDatum d = datumCaptor.getValue();
		assertThat("Datum persisted", d, notNullValue());
		assertThat("Created date near event date",
				d.getCreated().getTime() - info.getCreated().getTime(),
				allOf(greaterThanOrEqualTo(0L), lessThanOrEqualTo(100L)));
		assertThat("Control ID -> source ID", d.getSourceId(), equalTo(info.getControlId()));
		assertThat("Integer value as instantaneous property v", d.getInstantaneousSampleInteger("v"),
				equalTo(1));
		assertThat("Integer value as status property val", d.getStatusSampleInteger("val"), equalTo(1));
	}

	@Test
	public void handleCapture_eventMode_filtered() {
		// GIVEN
		dataSource.setEventMode(ControlEventMode.Change);

		// WHEN
		replayAll();
		NodeControlInfoDatum info = createControlInfo();
		Event event = createDatumEvent(NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CAPTURED, info);
		dataSource.handleEvent(event);

		// THEN
	}

	@Test
	public void handleUnsuportedEvent_filtered() {
		// GIVEN

		// WHEN
		replayAll();
		NodeControlInfoDatum info = createControlInfo();
		Event event = createDatumEvent("rando/event", info);
		dataSource.handleEvent(event);

		// THEN
	}
}
