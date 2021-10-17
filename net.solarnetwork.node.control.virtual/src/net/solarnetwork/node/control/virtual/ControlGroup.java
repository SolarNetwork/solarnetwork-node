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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.BasicNodeControlInfo;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.domain.datum.SimpleNodeControlInfoDatum;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.OptionalServiceCollection.OptionalFilterableServiceCollection;
import net.solarnetwork.service.support.BasicIdentifiable;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.NodeControlUtils;

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
 * @author matt
 * @version 2.0
 */
public class ControlGroup extends BasicIdentifiable
		implements SettingSpecifierProvider, NodeControlProvider, InstructionHandler, EventHandler {

	/** The default value for the {@code controlPropertyType} property. */
	public static final NodeControlPropertyType DEFAULT_CONTROL_PROPERTY_TYPE = NodeControlPropertyType.Boolean;

	private final OptionalFilterableServiceCollection<NodeControlProvider> controls;
	private final OptionalService<InstructionExecutionService> instructionService;
	private final OptionalService<EventAdmin> eventAdmin;
	private String controlId;
	private NodeControlPropertyType controlPropertyType = DEFAULT_CONTROL_PROPERTY_TYPE;

	private final AtomicReference<String> controlValue = new AtomicReference<>();
	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param controls
	 *        the controls to group into a single virtual control
	 * @param instructionService
	 *        the instruction service
	 * @param eventAdmin
	 *        the event admin
	 */
	public ControlGroup(OptionalFilterableServiceCollection<NodeControlProvider> controls,
			OptionalService<InstructionExecutionService> instructionService,
			OptionalService<EventAdmin> eventAdmin) {
		super();
		this.controls = controls;
		this.instructionService = instructionService;
		this.eventAdmin = eventAdmin;
		setDisplayName("Virtual Control Group");
	}

	@Override
	public boolean handlesTopic(String topic) {
		return TOPIC_SET_CONTROL_PARAMETER.equals(topic);
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
		String controlId = getControlId();
		if ( instruction == null || !TOPIC_SET_CONTROL_PARAMETER.equals(instruction.getTopic())
				|| controlId == null ) {
			return null;
		}
		String paramValue = NodeControlUtils.controlValue(controlPropertyType,
				instruction.getParameterValue(controlId));
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
		SimpleNodeControlInfoDatum result = createDatum(id, type, value);
		postControlEvent(result, NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CAPTURED);
		return result;
	}

	private static SimpleNodeControlInfoDatum createDatum(String controlId, NodeControlPropertyType type,
			Object value) {
		// @formatter:off
		BasicNodeControlInfo info = BasicNodeControlInfo.builder()
				.withControlId(controlId)
				.withReadonly(false)
				.withType(type)
				.withValue(NodeControlUtils.controlValue(type, value))
				.build();
		// formatter:on
		return new SimpleNodeControlInfoDatum(info, Instant.now());
	}

	private void postControlEvent(SimpleNodeControlInfoDatum info, String topic) {
		final EventAdmin admin = (eventAdmin != null ? eventAdmin.service() : null);
		if ( admin == null ) {
			return;
		}
		Event event = DatumEvents.datumEvent(topic, info);
		log.debug("Posting [{}] event with {}", topic, info);
		admin.postEvent(event);
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
		Object datum = event.getProperty(DatumEvents.DATUM_PROPERTY);
		if ( !(datum instanceof NodeControlInfo
				&& controlId.equals(((NodeControlInfo) datum).getControlId())) ) {
			return;
		}
		// TODO: verify value is the expected group value; if not change it?
	}

	// Settings

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.control.virtual.ControlGroup";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		final List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(8);

		results.add(new BasicTitleSettingSpecifier("groupValue", controlValue.get(), true));
		results.add(new BasicTitleSettingSpecifier("controlValues", controlValuesDescription(), true));

		results.addAll(basicIdentifiableSettings());
		results.add(new BasicTextFieldSettingSpecifier("controlId", ""));

		// drop-down menu for controlPropertyType
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				"controlPropertyTypeKey", String.valueOf(DEFAULT_CONTROL_PROPERTY_TYPE.getKey()));
		Map<String, String> propTypeTitles = new LinkedHashMap<String, String>(3);
		for ( NodeControlPropertyType e : NodeControlPropertyType.values() ) {
			propTypeTitles.put(String.valueOf(e.getKey()), e.toString());
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		results.add(propTypeSpec);

		results.add(new BasicTextFieldSettingSpecifier("controlsGroupUidFilter", ""));

		return results;
	}

	private String controlValuesDescription() {
		StringBuilder buf = new StringBuilder();
		for ( NodeControlProvider control : controls.services() ) {
			for ( String controlId : control.getAvailableControlIds() ) {
				NodeControlInfo info = control.getCurrentControlInfo(controlId);
				if ( info != null ) {
					if ( buf.length() > 0 ) {
						buf.append(", ");
					}
					buf.append(controlId).append(" = ").append(info.getValue());
				}
			}
		}
		if ( buf.length() > 0 ) {
			return buf.toString();
		}
		String msg = "No controls available.";
		MessageSource ms = getMessageSource();
		if ( ms != null ) {
			msg = ms.getMessage("controls.empty", null, msg, Locale.getDefault());
		}
		return msg;
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
		if ( controlPropertyType == null ) {
			controlPropertyType = DEFAULT_CONTROL_PROPERTY_TYPE;
		}
		this.controlPropertyType = controlPropertyType;
	}

	/**
	 * Get the control property type key.
	 * 
	 * <p>
	 * This returns the configured {@link #getControlPropertyType()}
	 * {@link NodeControlPropertyType#getKey()} value as a string.
	 * </p>
	 * 
	 * @return the property type key
	 */
	public String getControlPropertyTypeKey() {
		NodeControlPropertyType type = getControlPropertyType();
		if ( type == null ) {
			return null;
		}
		return Character.toString(type.getKey());
	}

	/**
	 * Set the datum property type via a key value.
	 * 
	 * <p>
	 * This uses the first character of {@code key} as a
	 * {@link NodeControlPropertyType} key value to call
	 * {@link #setControlPropertyType(NodeControlPropertyType)}. If {@code key}
	 * is not recognized, then {@link #DEFAULT_CONTROL_PROPERTY_TYPE} will be
	 * set instead.
	 * </p>
	 * 
	 * @param key
	 *        the property type key to set
	 */
	public void setControlPropertyTypeKey(String key) {
		if ( key == null || key.length() < 1 ) {
			return;
		}
		NodeControlPropertyType type;
		try {
			type = NodeControlPropertyType.forKey(key.charAt(0));
		} catch ( IllegalArgumentException e ) {
			type = DEFAULT_CONTROL_PROPERTY_TYPE;
		}
		setControlPropertyType(type);
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
	 * Get the controls to manage.
	 * 
	 * @return the controls to manage
	 */
	public OptionalFilterableServiceCollection<NodeControlProvider> getControls() {
		return controls;
	}

	/**
	 * Get the instruction execution service.
	 * 
	 * @return the service
	 */
	public OptionalService<InstructionExecutionService> getInstructionService() {
		return instructionService;
	}
	
	/**
	 * Set the controls {@literal groupUid} property filter value.
	 * @param groupUid the group UID to filter on
	 */
	public void setControlsGroupUidFilter(String groupUid) {
		controls.setPropertyFilter("groupUid", groupUid);
	}

}
