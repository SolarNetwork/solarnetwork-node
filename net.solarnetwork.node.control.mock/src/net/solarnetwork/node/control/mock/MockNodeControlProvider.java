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

import java.time.Instant;
import java.util.ArrayList;
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
 * <p>
 * The {@link InstructionHandler#TOPIC_SHED_LOAD} instruction is also handled:
 * if the parameter value (representing watts of power to shed) is greater than
 * {@literal 0} then the control value is set to {@literal true} and otherwise
 * {@literal false}.
 * </p>
 * 
 * @author matt
 * @version 2.0
 */
public class MockNodeControlProvider extends BaseIdentifiable
		implements NodeControlProvider, InstructionHandler {

	private String[] booleanControlIds = new String[] { "/mock/switch/1", "/mock/switch/2", };

	private OptionalService<EventAdmin> eventAdmin;
	private List<String> controlIds = null;
	private final Map<String, SimpleNodeControlInfoDatum> controlValues = new LinkedHashMap<>();

	/**
	 * Default constructor.
	 */
	public MockNodeControlProvider() {
		super();
		setDisplayName("Mock Switch");
		setUid("Mock Switch");
		setGroupUid("Mock");
		configureControlIds();
	}

	private void configureControlIds() {
		List<String> ids = new ArrayList<String>(
				(booleanControlIds == null ? 0 : booleanControlIds.length));
		if ( booleanControlIds != null ) {
			for ( String id : booleanControlIds ) {
				ids.add(id);
			}
		}
		controlIds = ids;
	}

	@Override
	public List<String> getAvailableControlIds() {
		return controlIds;
	}

	@Override
	public NodeControlInfo getCurrentControlInfo(String controlId) {
		SimpleNodeControlInfoDatum info = getSimpleNodeControlInfoDatum(controlId);
		postControlEvent(info, null, NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CAPTURED);
		return info;
	}

	private SimpleNodeControlInfoDatum getSimpleNodeControlInfoDatum(String controlId) {
		SimpleNodeControlInfoDatum info = null;
		synchronized ( controlValues ) {
			info = controlValues.get(controlId);
			if ( info == null ) {
				// create new info value
				if ( booleanControlIds != null ) {
					for ( int i = 0; i < booleanControlIds.length; i++ ) {
						String id = booleanControlIds[i];
						if ( id.equals(controlId) ) {
							info = newSimpleNodeControlInfoDatum(controlId,
									(i == 0 ? null : "Boolean Mock " + i),
									NodeControlPropertyType.Boolean, false, null);
							controlValues.put(controlId, info);
							break;
						}
					}
				}
			}
		}
		return info;
	}

	@Override
	public boolean handlesTopic(String topic) {
		return (InstructionHandler.TOPIC_SET_CONTROL_PARAMETER.equals(topic)
				|| InstructionHandler.TOPIC_SHED_LOAD.equals(topic));
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
		// look for a parameter name that matches a control ID
		InstructionState result = null;
		for ( String controlId : instruction.getParameterNames() ) {
			synchronized ( controlValues ) {
				SimpleNodeControlInfoDatum info = getSimpleNodeControlInfoDatum(controlId);
				if ( info != null ) {
					String newValue = instruction.getParameterValue(controlId);
					if ( TOPIC_SHED_LOAD.equals(instruction.getTopic()) ) {
						// treat value as number, with < 1 as FALSE
						try {
							int watts = Integer.parseInt(newValue);
							if ( watts < 1 ) {
								newValue = Boolean.FALSE.toString();
							} else {
								newValue = Boolean.TRUE.toString();
							}
						} catch ( NumberFormatException e ) {
							return InstructionUtils.createStatus(instruction, InstructionState.Declined);
						}
					}
					if ( updateSimpleNodeControlInfoDatumValue(info, newValue) ) {
						result = InstructionState.Completed;
					} else {
						result = InstructionState.Declined;
					}
				}
			}
		}
		return (result != null ? InstructionUtils.createStatus(instruction, result) : null);
	}

	private boolean updateSimpleNodeControlInfoDatumValue(SimpleNodeControlInfoDatum datum,
			String value) {
		if ( datum.getType() == NodeControlPropertyType.Boolean ) {
			if ( value != null ) {
				value = value.toLowerCase();
			}
			String oldValue = datum.getValue();
			String newValue = ("false".equalsIgnoreCase(value) || "0".equals(value)
					|| "no".equalsIgnoreCase(value) ? Boolean.FALSE.toString()
							: Boolean.TRUE.toString());
			if ( !newValue.equals(oldValue) ) {
				SimpleNodeControlInfoDatum newDatum = newSimpleNodeControlInfoDatum(datum.getControlId(),
						datum.getPropertyName(), datum.getType(), datum.getReadonly(), datum.getUnit(),
						newValue);
				controlValues.put(newDatum.getControlId(), newDatum);
				postControlEvent(newDatum, oldValue,
						NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED);
			}
			return true;
		}
		return false;
	}

	private void postControlEvent(SimpleNodeControlInfoDatum info, String oldValue, String topic) {
		final EventAdmin admin = OptionalService.service(eventAdmin);
		if ( admin == null ) {
			return;
		}
		Event event = DatumEvents.datumEvent(topic, info);
		admin.postEvent(event);
	}

	private SimpleNodeControlInfoDatum newSimpleNodeControlInfoDatum(String controlId,
			String propertyName, NodeControlPropertyType type, boolean readonly, String unit) {
		// generate initial value
		String value = null;
		switch (type) {
			case Boolean:
				if ( Math.random() < 0.5 ) {
					value = Boolean.FALSE.toString();
				} else {
					value = Boolean.TRUE.toString();
				}
				break;

			default:
				value = "?";
		}
		return newSimpleNodeControlInfoDatum(controlId, propertyName, type, readonly, unit, value);
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

	public String[] getBooleanControlIds() {
		return booleanControlIds;
	}

	public void setBooleanControlIds(String[] booleanControlIds) {
		this.booleanControlIds = booleanControlIds;
		configureControlIds();
	}

	public OptionalService<EventAdmin> getEventAdmin() {
		return eventAdmin;
	}

	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

}
