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

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.NodeControlProvider;
import net.solarnetwork.node.domain.NodeControlInfoDatum;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.util.ClassUtils;
import net.solarnetwork.util.OptionalService;

/**
 * Mock implementation of {@link NodeControlProvider} combined with
 * {@link InstructionHandler}.
 * 
 * This mock service implements both {@link NodeControlProvider} and
 * {@link InstructionHandler} to demonstrate a common pattern of control
 * providers implementing mutable controls. The control values can be changed
 * via a {@link InstructionHandler#TOPIC_SET_CONTROL_PARAMETER} instruction sent
 * to the node.
 * 
 * The {@link InstructionHandler#TOPIC_SHED_LOAD} instruction is also handled:
 * if the parameter value (representing watts of power to shed) is greater than
 * <code>0</code> then the control value is set to <b>TRUE</b> and otherwise
 * <b>FALSE</b>.
 * 
 * @author matt
 * @version 1.3
 */
public class MockNodeControlProvider implements NodeControlProvider, InstructionHandler {

	private String[] booleanControlIds = new String[] { "/mock/switch/1", "/mock/switch/2", };

	private OptionalService<EventAdmin> eventAdmin;
	private List<String> controlIds = null;
	private final Map<String, NodeControlInfoDatum> controlValues = new LinkedHashMap<String, NodeControlInfoDatum>();

	/**
	 * Default constructor.
	 */
	public MockNodeControlProvider() {
		super();
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
	public String getUID() {
		return "Mock Switch";
	}

	@Override
	public String getGroupUID() {
		return "Mock";
	}

	@Override
	public List<String> getAvailableControlIds() {
		return controlIds;
	}

	@Override
	public NodeControlInfo getCurrentControlInfo(String controlId) {
		NodeControlInfo info = getNodeControlInfoDatum(controlId);
		postControlEvent(info, null, NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CAPTURED);
		return info;
	}

	private NodeControlInfoDatum getNodeControlInfoDatum(String controlId) {
		NodeControlInfoDatum info = null;
		synchronized ( controlValues ) {
			info = controlValues.get(controlId);
			if ( info == null ) {
				// create new info value
				if ( booleanControlIds != null ) {
					for ( int i = 0; i < booleanControlIds.length; i++ ) {
						String id = booleanControlIds[i];
						if ( id.equals(controlId) ) {
							info = newNodeControlInfoDatum(controlId,
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
	public InstructionState processInstruction(Instruction instruction) {
		// look for a parameter name that matches a control ID
		InstructionState result = null;
		for ( String controlId : instruction.getParameterNames() ) {
			synchronized ( controlValues ) {
				NodeControlInfoDatum info = getNodeControlInfoDatum(controlId);
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
							return InstructionState.Declined;
						}
					}
					if ( updateNodeControlInfoDatumValue(info, newValue) ) {
						result = InstructionState.Completed;
					} else {
						result = InstructionState.Declined;
					}
				}
			}
		}
		return result;
	}

	private boolean updateNodeControlInfoDatumValue(NodeControlInfoDatum datum, String value) {
		if ( datum.getType() == NodeControlPropertyType.Boolean ) {
			if ( value != null ) {
				value = value.toLowerCase();
			}
			String oldValue = datum.getValue();
			String newValue = ("false".equalsIgnoreCase(value) || "0".equals(value)
					|| "no".equalsIgnoreCase(value) ? Boolean.FALSE.toString()
							: Boolean.TRUE.toString());
			if ( !newValue.equals(oldValue) ) {
				datum.setValue(newValue);
				postControlEvent(datum, oldValue, NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED);
			}
			return true;
		}
		return false;
	}

	private void postControlEvent(NodeControlInfo info, String oldValue, String topic) {
		final EventAdmin admin = (eventAdmin != null ? eventAdmin.service() : null);
		if ( admin == null ) {
			return;
		}
		Map<String, Object> props = ClassUtils.getSimpleBeanProperties(info, null);
		if ( oldValue != null ) {
			props.put("oldValue", oldValue);
		}
		admin.postEvent(new Event(topic, props));
	}

	private NodeControlInfoDatum newNodeControlInfoDatum(String controlId, String propertyName,
			NodeControlPropertyType type, boolean readonly, String unit) {
		NodeControlInfoDatum info = new NodeControlInfoDatum();
		info.setCreated(new Date());
		info.setSourceId(controlId);
		info.setPropertyName(propertyName);
		info.setType(type);
		info.setReadonly(readonly);
		info.setUnit(unit);

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

		info.setValue(value);
		return info;
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
