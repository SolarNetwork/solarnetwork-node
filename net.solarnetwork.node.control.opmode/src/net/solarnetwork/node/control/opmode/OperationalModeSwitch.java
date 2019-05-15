/* ==================================================================
 * OperationalModeSwitch.java - 15/05/2019 3:39:20 pm
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

package net.solarnetwork.node.control.opmode;

import static java.util.Collections.singleton;
import static net.solarnetwork.node.OperationalModesService.EVENT_PARAM_ACTIVE_OPERATIONAL_MODES;
import static net.solarnetwork.node.OperationalModesService.EVENT_TOPIC_OPERATIONAL_MODES_CHANGED;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.NodeControlProvider;
import net.solarnetwork.node.OperationalModesService;
import net.solarnetwork.node.domain.NodeControlInfoDatum;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.support.BaseIdentifiable;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.StringUtils;

/**
 * Control that acts like a binary switch for toggling an operational mode
 * on/off.
 * 
 * <p>
 * This control works by configuring an operational mode
 * ({@link #setMode(String)} and control ID ({@link #setControlId(String)}. When
 * a {@link InstructionHandler#TOPIC_SET_CONTROL_PARAMETER} instruction is
 * received that matches the configured control ID, the configured operational
 * mode will be enabled or disabled based on the boolean value of the received
 * instruction parameter.
 * </p>
 * 
 * <p>
 * The state of the control is tied to the state of the configured operational
 * mode. If the operational mode is enabled/disabled the control state will be
 * updated to match.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class OperationalModeSwitch extends BaseIdentifiable
		implements EventHandler, InstructionHandler, NodeControlProvider, SettingSpecifierProvider {

	private String mode;
	private String controlId;
	private OptionalService<EventAdmin> eventAdmin;

	private final OptionalService<OperationalModesService> opModesService;
	private final AtomicBoolean active = new AtomicBoolean(false);

	/**
	 * Constructor.
	 * 
	 * @param opModesService
	 *        the operational modes service to use
	 */
	public OperationalModeSwitch(OptionalService<OperationalModesService> opModesService) {
		super();
		this.opModesService = opModesService;
	}

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public boolean handlesTopic(String topic) {
		return TOPIC_SET_CONTROL_PARAMETER.equals(topic);
	}

	@Override
	public InstructionState processInstruction(Instruction instruction) {
		String mode = getMode();
		String controlId = getControlId();
		if ( instruction == null || !TOPIC_SET_CONTROL_PARAMETER.equals(instruction.getTopic())
				|| mode == null || controlId == null ) {
			return null;
		}
		String paramValue = instruction.getParameterValue(controlId);
		if ( paramValue == null ) {
			return null;
		}
		boolean active = StringUtils.parseBoolean(paramValue);
		OperationalModesService service = opModesService();
		if ( service == null ) {
			log.warn(
					"OperationalModesService not available; cannot manage [{}] mode enabled for control [{}] to [{}]",
					mode, controlId, active);
			return InstructionState.Declined;
		}
		if ( active ) {
			service.enableOperationalModes(singleton(mode));
		} else {
			service.disableOperationalModes(singleton(mode));
		}
		log.info("Operational mode [{}] {} for control [{}]", mode, active ? "enabled" : "disabled",
				controlId);
		return InstructionState.Completed;
	}

	// NodeControlProvider

	@Override
	public List<String> getAvailableControlIds() {
		return (controlId == null ? Collections.emptyList() : Collections.singletonList(controlId));
	}

	@Override
	public NodeControlInfo getCurrentControlInfo(String controlId) {
		if ( this.controlId == null || !this.controlId.equals(controlId) ) {
			return null;
		}
		NodeControlInfoDatum d = createDatum();
		postControlEvent(d, EVENT_TOPIC_CONTROL_INFO_CAPTURED);
		return d;
	}

	private NodeControlInfoDatum createDatum() {
		NodeControlInfoDatum d = new NodeControlInfoDatum();
		d.setCreated(new Date());
		d.setReadonly(false);
		d.setSourceId(getControlId());
		d.setType(NodeControlPropertyType.Boolean);
		d.setValue(String.valueOf(isModeActive()));
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
		String topic = (event != null ? event.getTopic() : null);
		if ( !EVENT_TOPIC_OPERATIONAL_MODES_CHANGED.equals(topic) ) {
			return;
		}
		String mode = getMode();
		@SuppressWarnings("unchecked")
		Set<String> enabledModes = (Set<String>) event.getProperty(EVENT_PARAM_ACTIVE_OPERATIONAL_MODES);
		boolean enabled = enabledModes != null && enabledModes.contains(mode);
		if ( enabled != active.getAndSet(enabled) ) {
			// notify of changed control value
			NodeControlInfoDatum d = createDatum();
			d.setValue(String.valueOf(enabled));
			postControlEvent(d, EVENT_TOPIC_CONTROL_INFO_CHANGED);
		}
	}

	// Settings

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.control.opmode.switch";
	}

	@Override
	public String getDisplayName() {
		return "Operational State Manager";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(8);

		results.add(
				new BasicTitleSettingSpecifier("info", isModeActive() ? "Active" : "Inactive", true));

		results.addAll(baseIdentifiableSettings(null));
		results.add(new BasicTextFieldSettingSpecifier("mode", ""));
		results.add(new BasicTextFieldSettingSpecifier("controlId", ""));

		return results;
	}

	// Accessors

	private OperationalModesService opModesService() {
		return (opModesService != null ? opModesService.service() : null);
	}

	/**
	 * Get the active state.
	 * 
	 * @return {@literal true} if the configured mode is considered as enabled,
	 *         {@literal false} otherwise
	 */
	public boolean isModeActive() {
		String mode = getMode();
		if ( mode == null ) {
			return false;
		}
		OperationalModesService service = opModesService();
		return (service != null ? service.isOperationalModeActive(mode) : false);
	}

	/**
	 * Get the operational mode to listen for.
	 * 
	 * @return the operational mode
	 */
	public String getMode() {
		return mode;
	}

	/**
	 * Set the operational mode to listen for.
	 * 
	 * @param mode
	 *        the operational mode
	 */
	public void setMode(String mode) {
		this.mode = mode;
	}

	/**
	 * Get the control ID to use.
	 * 
	 * @return the control ID
	 */
	public String getControlId() {
		return controlId;
	}

	/**
	 * Set the control ID to use.
	 * 
	 * @param controlId
	 *        the control ID
	 */
	public void setControlId(String controlId) {
		this.controlId = controlId;
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
