/* ==================================================================
 * MockRfidEventGenerator.java - 30/07/2016 6:09:01 PM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.rfid.mock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import net.solarnetwork.domain.BasicNodeControlInfo;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.service.support.BasicIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;

/**
 * Mock RFID scanner that exposes a {@link NodeControlProvider} and
 * {@link InstructionHandler} for issuing RFID "message received" events.
 * 
 * @author matt
 * @version 2.0
 */
public class MockRfidEventGenerator extends BasicIdentifiable
		implements InstructionHandler, NodeControlProvider, SettingSpecifierProvider {

	/** A prefix added to {@link #getUID()} to form the control ID used. */
	public static final String CONTROL_ID_PREFIX = "/rfid/";

	/** Topic for when a RFID message has been received. */
	public static final String TOPIC_RFID_MESSAGE_RECEIVED = "net/solarnetwork/node/hw/rfid/MESSAGE_RECEIVED";

	/** Event parameter for the RFID message value. */
	public static final String EVENT_PARAM_MESSAGE = "message";

	/**
	 * Event parameter for the RFID message date, as milliseconds since the
	 * epoch.
	 */
	public static final String EVENT_PARAM_DATE = "date";

	/** Event parameter for the RFID message counter value. */
	public static final String EVENT_PARAM_COUNT = "count";

	/** Event parameter for the configured {@code uid}. */
	public static final String EVENT_PARAM_UID = "uid";

	/** Event parameter for the configured {@code groupUID}. */
	public static final String EVENT_PARAM_GROUP_UID = "groupUID";

	/** The {@code uid} property default value. */
	public static final String DEFAULT_UID = "MockIdTag";

	private EventAdmin eventAdmin;

	private final AtomicInteger count = new AtomicInteger(0);

	/**
	 * Constructor.
	 */
	public MockRfidEventGenerator() {
		super();
		setUid(DEFAULT_UID);
		setDisplayName(getClass().getSimpleName());
	}

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SET_CONTROL_PARAMETER.equals(topic);
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
		// look for a parameter name that matches a control ID
		InstructionState result = null;
		final String expectedControlId = CONTROL_ID_PREFIX + getUid();
		for ( String controlId : instruction.getParameterNames() ) {
			if ( expectedControlId.equals(controlId) ) {
				// doesn't matter what we try to set the control to, all we do is trigger the event here
				postRfidScannedEvent();
				result = InstructionState.Completed;
			}
		}
		return InstructionUtils.createStatus(instruction, result);
	}

	private void postRfidScannedEvent() {
		Map<String, Object> props = new HashMap<>();
		props.put(EVENT_PARAM_COUNT, count.incrementAndGet());
		props.put(EVENT_PARAM_DATE, System.currentTimeMillis());
		props.put(EVENT_PARAM_MESSAGE, getUid());
		props.put(EVENT_PARAM_UID, CONTROL_ID_PREFIX + getUid());
		props.put(EVENT_PARAM_GROUP_UID, getGroupUid());
		Event event = new Event(TOPIC_RFID_MESSAGE_RECEIVED, props);
		EventAdmin ea = eventAdmin;
		if ( ea != null ) {
			ea.postEvent(event);
		}
	}

	@Override
	public List<String> getAvailableControlIds() {
		return Arrays.asList(CONTROL_ID_PREFIX + getUid());
	}

	@Override
	public NodeControlInfo getCurrentControlInfo(String controlId) {
		if ( controlId == null || !controlId.equals(CONTROL_ID_PREFIX + getUid()) ) {
			return null;
		}
		BasicNodeControlInfo info = BasicNodeControlInfo.builder().withControlId(controlId)
				.withType(NodeControlPropertyType.Boolean).withReadonly(false)
				.withValue(Boolean.FALSE.toString()).build();
		return info;
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.hw.rfid.mock";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return basicIdentifiableSettings("", DEFAULT_UID, null);
	}

	/**
	 * Set the event admin.
	 * 
	 * @param eventAdmin
	 *        the service
	 */
	public void setEventAdmin(EventAdmin eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

}
