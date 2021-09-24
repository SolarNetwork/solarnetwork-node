/* ==================================================================
 * LATAController.java - Oct 25, 2011 8:16:16 PM
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

package net.solarnetwork.node.control.jf2.lata;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.osgi.service.event.Event;
import net.solarnetwork.domain.BasicNodeControlInfo;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.control.jf2.lata.command.AddressableCommand;
import net.solarnetwork.node.control.jf2.lata.command.Command;
import net.solarnetwork.node.control.jf2.lata.command.CommandInterface;
import net.solarnetwork.node.control.jf2.lata.command.CommandValidationException;
import net.solarnetwork.node.control.jf2.lata.command.ToggleMode;
import net.solarnetwork.node.domain.datum.SimpleNodeControlInfoDatum;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.io.serial.SerialNetwork;
import net.solarnetwork.node.io.serial.support.SerialDeviceSupport;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.service.OptionalService.OptionalFilterableService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;

/**
 * Implementation of both {@link NodeControlProvider} and
 * {@link InstructionHandler} for the JF2 LATA switch.
 * 
 * <p>
 * This class allows the LATA switch to both report the on/off status of each
 * configured address, and for those addresses to be toggled on/off.
 * </p>
 * 
 * @author matt
 * @version 3.0
 */
public class LATAController extends SerialDeviceSupport
		implements NodeControlProvider, InstructionHandler, SettingSpecifierProvider {

	/** The default value for the {@code controlIdMappingValue} property. */
	public static final String DEFAULT_CONTROL_ID_MAPPING = "/power/switch/1 = 100000BD, /power/switch/2 = 100000FD";

	/** The {@code serialNetworkUid} property default value. */
	public static final String DEFAULT_SERIAL_NETWORK_UID = "Serial Port";

	private Map<String, String> controlIdMapping = new HashMap<String, String>();

	private static final Pattern SWITCH_STATUS_RESULT_PATTERN = Pattern
			.compile("^T(\\w{8})2\\d{2}(\\w*)");

	/**
	 * Default constructor.
	 */
	public LATAController(OptionalFilterableService<SerialNetwork> serialNetwork) {
		super();
		setDisplayName("LATA Controller");
		setControlIdMappingValue(DEFAULT_CONTROL_ID_MAPPING);
		setSerialNetworkUid(DEFAULT_SERIAL_NETWORK_UID);
	}

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SET_CONTROL_PARAMETER.equals(topic);
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
		// look for a parameter name that matches a control ID
		InstructionState result = null;
		log.debug("Inspecting instruction {} against controls {}", instruction.getId(),
				controlIdMapping.keySet());
		for ( String controlId : instruction.getParameterNames() ) {
			log.trace("Got instruction parameter {}", controlId);
			if ( controlIdMapping.containsKey(controlId) ) {
				// treat parameter value as boolean
				String value = instruction.getParameterValue(controlId);
				if ( value != null ) {
					value = value.toLowerCase();
				}
				boolean newStatus = true;
				if ( "false".equals(value) || "0".equals(value) ) {
					newStatus = false;
				}
				result = InstructionState.Declined;
				try {
					if ( setSwitchStatus(controlId, newStatus) ) {
						result = InstructionState.Completed;
					}
				} catch ( IOException e ) {
					log.warn("Serial communications error: {}", e.getMessage());
				}
			}
		}
		return (result != null ? InstructionUtils.createStatus(instruction, result) : null);
	}

	private synchronized boolean setSwitchStatus(String controlId, boolean newStatus)
			throws IOException {
		String address = controlIdMapping.get(controlId);
		log.debug("Setting switch {} status at address {}", controlId, address);
		if ( address == null ) {
			log.warn("Configuration error: address not available for control ID [{}]", controlId);
			return false;
		}
		CommandInterface cmd;
		try {
			cmd = new AddressableCommand(address, newStatus ? Command.SwitchOn : Command.SwitchOff);
		} catch ( CommandValidationException e ) {
			log.error("Bad address [{}] configured for control ID {}: {}",
					new Object[] { address, controlId, e.getMessage() });
			return false;
		}
		performAction(new LATABusConverser(cmd));
		log.trace("Set status to {} for control {}, address {}",
				new Object[] { newStatus, controlId, address });
		postControlEvent(newSimpleNodeControlInfoDatum(controlId, newStatus),
				NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED);
		return true;
	}

	@Override
	public List<String> getAvailableControlIds() {
		if ( controlIdMapping == null ) {
			return Collections.emptyList();
		}
		return new ArrayList<String>(controlIdMapping.keySet());
	}

	@Override
	protected Map<String, Object> readDeviceInfo(SerialConnection conn) throws IOException {
		String version = new GetVersionAction(false).doWithConnection(conn);
		if ( version != null ) {
			return Collections.singletonMap(INFO_KEY_DEVICE_MODEL, (Object) version);
		}
		return Collections.emptyMap();
	}

	@Override
	public synchronized NodeControlInfo getCurrentControlInfo(String controlId) {
		// read the control's current status
		String address = controlIdMapping.get(controlId);
		log.debug("Reading switch {} status from address {}", controlId, address);
		if ( address == null ) {
			log.warn("Configuration error: address not available for control ID [{}]", controlId);
			return null;
		}
		CommandInterface cmd;
		try {
			cmd = new AddressableCommand(address, Command.SwitchStatus);
		} catch ( CommandValidationException e ) {
			log.error("Bad address [{}] configured for control ID {}: {}",
					new Object[] { address, controlId, e.getMessage() });
			return null;
		}

		try {
			log.trace("Executing command {} for control {}", cmd.getData(), controlId);
			String result = performAction(new LATABusConverser(cmd));
			if ( result == null ) {
				log.info("Status unavailable for control {}, address {}", controlId, address);
				return null;
			}
			log.trace("Got status result [{}] for control {}, address {}",
					new Object[] { result, controlId, address });
			Matcher m = SWITCH_STATUS_RESULT_PATTERN.matcher(result);
			if ( m.find() ) {
				String resultAddress = m.group(1);
				if ( !resultAddress.equals(address) ) {
					log.debug(
							"Address returned in command {} does not match expected address {}, ignoring",
							resultAddress, address);
				} else {
					String status = m.group(2);
					Boolean switchOn = ToggleMode.ON.hexString().equals(status);
					log.trace("Address {} status is {}", address, switchOn);
					SimpleNodeControlInfoDatum info = newSimpleNodeControlInfoDatum(controlId, switchOn);
					postControlEvent(info, NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CAPTURED);
					return info;

				}
			}
			log.info("Invalid status result [{}], ignoring", result);
		} catch ( IOException e ) {
			log.warn("Serial communications error: {}", e.getMessage());
		}
		return null;
	}

	private SimpleNodeControlInfoDatum newSimpleNodeControlInfoDatum(String controlId, boolean status) {
		// @formatter:off
		NodeControlInfo info = BasicNodeControlInfo.builder()
				.withControlId(resolvePlaceholders(controlId))
				.withType(NodeControlPropertyType.Boolean)
				.withReadonly(false)
				.withValue(String.valueOf(status))
				.build();
		// @formatter:on
		return new SimpleNodeControlInfoDatum(info, Instant.now());
	}

	private void postControlEvent(SimpleNodeControlInfoDatum info, String topic) {
		Event event = DatumEvents.datumEvent(topic, info);
		postEvent(event);
	}

	/**
	 * Set the {@code controlIdMapping} property using a string value.
	 * 
	 * <p>
	 * The passed in value must be a comma-delimited list of key/value pairs,
	 * each pair separated by an equal sign. For example:
	 * {@code 1 = one, 2 = two} would define two keys and their associated
	 * values.
	 * </p>
	 * 
	 * @param value
	 *        the value string
	 */
	public void setControlIdMappingValue(String value) {
		String[] keyValues = value.split("\\s*,\\s*");
		Map<String, String> map = new LinkedHashMap<String, String>(3);
		for ( String pair : keyValues ) {
			String[] kv = pair.split("\\s*=\\s*");
			if ( kv.length == 2 ) {
				map.put(kv[0], kv[1]);
			}
		}
		setControlIdMapping(map);
	}

	/**
	 * Get the {@code controlIdMapping} map as a string value.
	 * 
	 * @return the string value
	 */
	public String getControlIdMappingValue() {
		Map<String, String> mapping = getControlIdMapping();
		if ( mapping == null || mapping.size() < 1 ) {
			return "";
		}
		StringBuilder buf = new StringBuilder();
		for ( Map.Entry<String, String> me : mapping.entrySet() ) {
			if ( buf.length() > 0 ) {
				buf.append(", ");
			}
			buf.append(me.getKey()).append(" = ").append(me.getValue());
		}
		return buf.toString();
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.control.jf2.lata";
	}

	@Override
	public String getDisplayName() {
		return "JF2 LATA switch control";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return getDefaultSettingSpecifiers();
	}

	public List<SettingSpecifier> getDefaultSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);
		results.add(new BasicTitleSettingSpecifier("info", getDeviceInfoMessage(), true));
		results.addAll(baseIdentifiableSettings(""));
		results.addAll(serialNetworkSettings("", DEFAULT_SERIAL_NETWORK_UID));
		results.add(
				new BasicTextFieldSettingSpecifier("controlIdMappingValue", DEFAULT_CONTROL_ID_MAPPING));
		return results;
	}

	/**
	 * Get the control ID mapping.
	 * 
	 * @return the mapping of control IDs to LATA switch addresses
	 */
	public Map<String, String> getControlIdMapping() {
		return controlIdMapping;
	}

	/**
	 * A mapping of NodeControlInfo {@code controlId} value keys to associated
	 * LATA switch addresses, as hex string values. This can also be configured
	 * via the {@link #setControlIdMappingValue(String)} method, for easy
	 * configuration via a property placeholder.
	 * 
	 * @param controlIdMapping
	 *        the control ID mapping
	 */
	public void setControlIdMapping(Map<String, String> controlIdMapping) {
		this.controlIdMapping = controlIdMapping;
	}

}
