/* ==================================================================
 * ControlGroup.java - 30/07/2019 3:07:22 pm
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

package net.solarnetwork.node.control.virtual;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.NodeControlProvider;
import net.solarnetwork.node.domain.NodeControlInfoDatum;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.support.BaseIdentifiable;
import net.solarnetwork.util.NodeControlUtils;
import net.solarnetwork.util.OptionalService;

/**
 * A virtual control that operates on a set of other controls, making a group of
 * controls act as one.
 * 
 * <p>
 * This can be useful in situations where a set of similar hardware devices need
 * to be controlled together, such as a set of switches that need to be toggled
 * together as if they were a single switch.
 * </p>
 * 
 * @author Matt Magoffin
 * @version 1.0
 */
public class ControlGroup extends BaseIdentifiable
		implements SettingSpecifierProvider, NodeControlProvider, InstructionHandler, EventHandler {

	public static final NodeControlPropertyType DEFAULT_CONTROL_PROPERTY_TYPE = NodeControlPropertyType.Boolean;

	private String controlId;
	private NodeControlPropertyType controlPropertyType = DEFAULT_CONTROL_PROPERTY_TYPE;
	private OptionalService<EventAdmin> eventAdmin;

	private final AtomicReference<String> controlValue = new AtomicReference<>();
	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public boolean handlesTopic(String topic) {
		return TOPIC_SET_CONTROL_PARAMETER.equals(topic);
	}

	@Override
	public InstructionState processInstruction(Instruction instruction) {
		String controlId = getControlId();
		if ( instruction == null || !TOPIC_SET_CONTROL_PARAMETER.equals(instruction.getTopic())
				|| controlId == null ) {
			return null;
		}
		String paramValue = instruction.getParameterValue(controlId);
		if ( paramValue == null ) {
			return null;
		}
		return null;
	}

	@Override
	public List<String> getAvailableControlIds() {
		final String controlId = getControlId();
		return controlId == null ? Collections.emptyList() : Collections.singletonList(controlId);
	}

	@Override
	public NodeControlInfo getCurrentControlInfo(String controlId) {
		final String id = getControlId();
		final NodeControlPropertyType type = getControlPropertyType();
		final String value = controlValue.get();
		if ( id == null || type == null || value == null || !id.equals(controlId) ) {
			return null;
		}
		NodeControlInfoDatum result = createDatum(id, type, value);
		postControlEvent(result, NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CAPTURED);
		return result;
	}

	private static NodeControlInfoDatum createDatum(String controlId, NodeControlPropertyType type,
			Object value) {
		NodeControlInfoDatum d = new NodeControlInfoDatum();
		d.setCreated(new Date());
		d.setReadonly(false);
		d.setSourceId(controlId);
		d.setType(type);

		String controlValue = NodeControlUtils.controlValue(type, value);
		d.setValue(controlValue);
		return d;
	}

	private void postControlEvent(NodeControlInfoDatum info, String topic) {
		final EventAdmin admin = (eventAdmin != null ? eventAdmin.service() : null);
		if ( admin == null ) {
			return;
		}
		Map<String, ?> props = info.asSimpleMap();
		log.debug("Posting [{}] event with {}", topic, props);
		admin.postEvent(new Event(topic, props));
	}

	// EventHandler

	@Override
	public void handleEvent(Event event) {
		final String topic = (event != null ? event.getTopic() : null);
		if ( !EVENT_TOPIC_CONTROL_INFO_CHANGED.equals(topic) ) {
			return;
		}
		final String controlId = getControlId();
		if ( controlId == null ) {
			return;
		}
		Object eventControlId = event.getProperty("controlId");
		if ( !controlId.equals(eventControlId) ) {
			return;
		}
		// TODO: verify value is the expected group value; if not change it?
	}

	// Settings

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.control.virtual.ControlGroup";
	}

	@Override
	public String getDisplayName() {
		return "Virtual Control Group";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Get the control ID.
	 * 
	 * @return the control ID
	 */
	public String getControlId() {
		return controlId;
	}

	/**
	 * Set the control ID.
	 * 
	 * @param controlId
	 *        the controlId to set
	 */
	public void setControlId(String controlId) {
		this.controlId = controlId;
	}

	/**
	 * Get the control property type.
	 * 
	 * @return the controlPropertyType
	 */
	public NodeControlPropertyType getControlPropertyType() {
		return controlPropertyType;
	}

	/**
	 * Set the control property type.
	 * 
	 * @param controlPropertyType
	 *        the controlPropertyType to set
	 */
	public void setControlPropertyType(NodeControlPropertyType controlPropertyType) {
		this.controlPropertyType = controlPropertyType;
	}

	/**
	 * Get the event admin.
	 * 
	 * @return the event admin
	 */
	public OptionalService<EventAdmin> getEventAdmin() {
		return eventAdmin;
	}

	/**
	 * Set the event admin.
	 * 
	 * @param eventAdmin
	 *        the event admin to use
	 */
	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

}
