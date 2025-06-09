/* ==================================================================
 * MockNodeControlProvider.java - Oct 1, 2011 9:13:57 PM
 *
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.mock;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import net.solarnetwork.domain.BasicNodeControlInfo;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.domain.datum.SimpleNodeControlInfoDatum;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.StringUtils;

/**
 * Mock implementation of {@link NodeControlProvider} combined with
 * {@link InstructionHandler}.
 *
 * <p>
 * This mock service implements both {@link NodeControlProvider} and
 * {@link InstructionHandler} to demonstrate a common pattern of control
 * providers implementing mutable controls. The control values can be changed
 * via a {@link InstructionHandler#TOPIC_SET_CONTROL_PARAMETER} instruction sent
 * to the node.
 * </p>
 *
 * @author matt
 * @version 3.1
 */
public class MockNodeControlProvider extends BaseIdentifiable implements NodeControlProvider,
		InstructionHandler, SettingSpecifierProvider, SettingsChangeObserver, ServiceLifecycleObserver {

	/** The {@code controlType} property default value. */
	public static final NodeControlPropertyType DEFAULT_CONTROL_TYPE = NodeControlPropertyType.Boolean;

	/** The {@code initialControlValue} property default value. */
	public static final String DEFAULT_INITIAL_CONTROL_VALUE = "false";

	private OptionalService<EventAdmin> eventAdmin;
	private NodeControlPropertyType controlType = DEFAULT_CONTROL_TYPE;
	private String initialControlValue = DEFAULT_INITIAL_CONTROL_VALUE;
	private String controlId;

	private SimpleNodeControlInfoDatum controlValue;

	/**
	 * Default constructor.
	 */
	public MockNodeControlProvider() {
		super();
		setDisplayName("Mock Control");
		setGroupUid("Mock");
	}

	@Override
	public void serviceDidStartup() {
		if ( controlValue == null ) {
			configurationChanged(null);
		}
	}

	@Override
	public void serviceDidShutdown() {
		// nothing to do
	}

	@Override
	public void configurationChanged(Map<String, Object> properties) {
		setControlValue(initialControlValue);
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.control.mock";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(8);
		result.addAll(baseIdentifiableSettings(""));
		result.add(new BasicTextFieldSettingSpecifier("controlId", null));

		// drop-down menu for control type
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				"controlTypeValue", String.valueOf(DEFAULT_CONTROL_TYPE.getKey()));
		Map<String, String> propTypeTitles = new LinkedHashMap<>(8);
		for ( NodeControlPropertyType e : NodeControlPropertyType.values() ) {
			propTypeTitles.put(String.valueOf(e.getKey()), e.toString());
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		result.add(propTypeSpec);

		result.add(new BasicTextFieldSettingSpecifier("initialControlValue",
				DEFAULT_INITIAL_CONTROL_VALUE));

		return result;
	}

	@Override
	public List<String> getAvailableControlIds() {
		final String controlId = this.controlId;
		return Collections.singletonList(controlId);
	}

	@Override
	public NodeControlInfo getCurrentControlInfo(String controlId) {
		SimpleNodeControlInfoDatum info = this.controlValue;
		if ( info != null ) {
			postControlEvent(info, NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CAPTURED);
		}
		return info;
	}

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SET_CONTROL_PARAMETER.equals(topic);
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
		if ( instruction == null || !handlesTopic(instruction.getTopic()) ) {
			return null;
		}
		// look for a parameter name that matches a control ID
		final String controlId = getControlId();
		if ( controlId == null || controlId.isEmpty() ) {
			return null;
		}
		InstructionState result = null;
		for ( String paramName : instruction.getParameterNames() ) {
			if ( controlId.equals(paramName) ) {
				String newValue = instruction.getParameterValue(controlId);
				if ( updateSimpleNodeControlInfoDatumValue(newValue) ) {
					result = InstructionState.Completed;
				} else {
					result = InstructionState.Declined;
				}
			}
		}
		return (result != null ? InstructionUtils.createStatus(instruction, result) : null);
	}

	private synchronized boolean updateSimpleNodeControlInfoDatumValue(String value) {
		if ( value == null || value.isEmpty() ) {
			return false;
		}
		final SimpleNodeControlInfoDatum curr = this.controlValue;
		final NodeControlPropertyType type = this.controlType;
		String finalValue;
		switch (type) {
			case Boolean:
				finalValue = String.valueOf(StringUtils.parseBoolean(value));
				break;

			case Float:
			case Percent:
				try {
					finalValue = new BigDecimal(value).toPlainString();
				} catch ( NumberFormatException e ) {
					return false;
				}
				break;

			case Integer:
				try {
					finalValue = String.valueOf(Integer.parseInt(value));
				} catch ( NumberFormatException e ) {
					return false;
				}
				break;

			default:
				finalValue = value;
				break;
		}
		if ( curr != null && finalValue.equalsIgnoreCase(curr.getValue()) ) {
			return true;
		}

		SimpleNodeControlInfoDatum newValue = newSimpleNodeControlInfoDatum(controlId, null, controlType,
				false, null, finalValue.toString());
		postControlEvent(newValue, NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED);
		this.controlValue = newValue;
		return true;
	}

	private void postControlEvent(SimpleNodeControlInfoDatum info, String topic) {
		final EventAdmin admin = OptionalService.service(eventAdmin);
		if ( admin == null ) {
			return;
		}
		Event event = DatumEvents.datumEvent(topic, info);
		admin.postEvent(event);
	}

	private SimpleNodeControlInfoDatum newSimpleNodeControlInfoDatum(String controlId,
			String propertyName, NodeControlPropertyType type, boolean readonly, String unit,
			String value) {
		// @formatter:off
		NodeControlInfo info = BasicNodeControlInfo.builder()
				.withControlId(resolvePlaceholders(controlId))
				.withType(type)
				.withPropertyName(propertyName)
				.withReadonly(readonly)
				.withUnit(unit)
				.withValue(value)
				.build();
		// @formatter:on
		return new SimpleNodeControlInfoDatum(info, Instant.now());
	}

	/**
	 * Get the current control value.
	 *
	 * @return the value
	 */
	public final String getControlValue() {
		final SimpleNodeControlInfoDatum d = this.controlValue;
		return (d != null ? d.getValue() : null);
	}

	/**
	 * Set the current control value.
	 *
	 * @param controlValue
	 *        the value to set
	 */
	public final void setControlValue(String controlValue) {
		updateSimpleNodeControlInfoDatumValue(controlValue);
	}

	/**
	 * Get the event admin service.
	 *
	 * @return the service
	 */
	public OptionalService<EventAdmin> getEventAdmin() {
		return eventAdmin;
	}

	/**
	 * Set the event admin sevice.
	 *
	 * @param eventAdmin
	 *        the service to set
	 */
	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	/**
	 * Get the control type.
	 *
	 * @return the type
	 */
	public final NodeControlPropertyType getControlType() {
		return controlType;
	}

	/**
	 * Set the control type.
	 *
	 * @param controlType
	 *        the type to set
	 */
	public final void setControlType(NodeControlPropertyType controlType) {
		this.controlType = controlType;
	}

	/**
	 * Get the control type as a string value.
	 *
	 * @return the type
	 */
	public final String getControlTypeValue() {
		return controlType.name();
	}

	/**
	 * Set the control type as a string value.
	 *
	 * @param value
	 *        the type to set; either a key value or name
	 */
	public final void setControlTypeValue(String value) {
		if ( value == null || value.isEmpty() ) {
			this.controlType = DEFAULT_CONTROL_TYPE;
		} else {
			try {
				this.controlType = NodeControlPropertyType.forKey(value.charAt(0));
			} catch ( IllegalArgumentException e ) {
				try {
					this.controlType = NodeControlPropertyType.valueOf(value);
				} catch ( Exception e2 ) {
					this.controlType = DEFAULT_CONTROL_TYPE;
				}
			}
		}
	}

	/**
	 * Get the control ID.
	 *
	 * @return the control ID
	 */
	public final String getControlId() {
		return controlId;
	}

	/**
	 * Set the control ID.
	 *
	 * @param controlId
	 *        the control ID to set
	 */
	public final void setControlId(String controlId) {
		this.controlId = controlId;
	}

	/**
	 * Get the initial control value.
	 *
	 * @return the initial value
	 */
	public final String getInitialControlValue() {
		return initialControlValue;
	}

	/**
	 * Set the initial control value.
	 *
	 * @param initialControlValue
	 *        the initialControlValue to set
	 */
	public final void setInitialControlValue(String initialControlValue) {
		this.initialControlValue = initialControlValue;
	}

}
