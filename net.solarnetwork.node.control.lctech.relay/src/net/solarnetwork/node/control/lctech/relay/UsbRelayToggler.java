/* ==================================================================
 * UsbRelayToggler.java - 18/06/2019 10:18:12 am
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

package net.solarnetwork.node.control.lctech.relay;

import static net.solarnetwork.node.hw.lctech.relay.UsbRelayUtils.DEFAULT_IDENTITY;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.osgi.service.event.Event;
import net.solarnetwork.domain.BasicNodeControlInfo;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.domain.datum.SimpleNodeControlInfoDatum;
import net.solarnetwork.node.hw.lctech.relay.UsbRelayUtils;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.io.serial.SerialConnectionAction;
import net.solarnetwork.node.io.serial.support.SerialDeviceSupport;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.StringUtils;

/**
 * Control a relay state: open or closed.
 * 
 * @author matt
 * @version 3.0
 */
public class UsbRelayToggler extends SerialDeviceSupport
		implements SettingSpecifierProvider, NodeControlProvider, InstructionHandler {

	/** The default value for the {@code address} property. */
	public static final int DEFAULT_ADDRESS = 0x01;

	/** The default value for the {@code controlId} property. */
	public static final String DEFAULT_CONTROL_ID = "/relay/1";

	private int identity = DEFAULT_IDENTITY;
	private int address = DEFAULT_ADDRESS;
	private String controlId = DEFAULT_CONTROL_ID;

	private final AtomicBoolean state = new AtomicBoolean();

	// InstructionHandler

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SET_CONTROL_PARAMETER.equals(topic);
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
		if ( !InstructionHandler.TOPIC_SET_CONTROL_PARAMETER.equals(instruction.getTopic()) ) {
			return null;
		}
		// look for a parameter name that matches a control ID
		InstructionState result = null;
		log.debug("Inspecting instruction {} against control {}", instruction.getId(), controlId);
		for ( String paramName : instruction.getParameterNames() ) {
			log.trace("Got instruction parameter {}", paramName);
			if ( controlId.equals(paramName) ) {
				// treat parameter value as a boolean String (1, true, t, yes, y)
				String str = instruction.getParameterValue(controlId);
				Boolean desiredValue = StringUtils.parseBoolean(str);
				boolean success = false;
				try {
					success = setValue(desiredValue);
				} catch ( Exception e ) {
					log.warn("Error handling instruction {} on control {}: {}", instruction.getTopic(),
							controlId, e.getMessage());
				}
				if ( success ) {
					postControlEvent(newSimpleNodeControlInfoDatum(controlId, desiredValue),
							NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED);
					result = InstructionState.Completed;
				} else {
					result = InstructionState.Declined;
				}
			}
		}
		return (result != null ? InstructionUtils.createStatus(instruction, result) : null);
	}

	@Override
	public List<String> getAvailableControlIds() {
		return Collections.singletonList(controlId);
	}

	@Override
	public NodeControlInfo getCurrentControlInfo(String controlId) {
		return newSimpleNodeControlInfoDatum(controlId, state.get());
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

	@Override
	protected Map<String, Object> readDeviceInfo(SerialConnection conn) throws IOException {
		return Collections.emptyMap();
	}

	/**
	 * Set the relay state to open or closed.
	 * 
	 * @param desiredValue
	 *        {@literal true} for open, {@literal false} for closed
	 * @return {@literal true} if the write succeeded
	 * @throws IOException
	 *         if an IO error occurs
	 */
	private synchronized boolean setValue(final boolean desiredValue) throws IOException {
		log.info("Setting {} value to {}", controlId, desiredValue);

		Boolean result = performAction(new SerialConnectionAction<Boolean>() {

			@Override
			public Boolean doWithConnection(SerialConnection conn) throws IOException {
				UsbRelayUtils.setRelayState(conn, identity, address, desiredValue);
				return true;
			}
		});

		if ( result == null ) {
			log.warn("Unable to control [{}]: is serial port available?", controlId);
		} else if ( result.booleanValue() ) {
			state.set(desiredValue);
		}
		return result;
	}

	private void postControlEvent(SimpleNodeControlInfoDatum info, String topic) {
		Event event = DatumEvents.datumEvent(topic, info);
		postEvent(event);
	}

	// SettingSpecifierProvider

	@Override
	public String getUid() {
		return controlId;
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.control.lctech.relay.usb.toggle";
	}

	@Override
	public String getDisplayName() {
		return "LC Tech USB Relay Toggler";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(8);

		results.add(new BasicTextFieldSettingSpecifier("controlId", DEFAULT_CONTROL_ID));
		results.add(new BasicTextFieldSettingSpecifier("groupUid", ""));
		results.add(new BasicTextFieldSettingSpecifier("serialNetwork.propertyFilters['uid']",
				"Serial Port"));
		results.add(new BasicTextFieldSettingSpecifier("identity", String.valueOf(DEFAULT_IDENTITY)));
		results.add(new BasicTextFieldSettingSpecifier("address", String.valueOf(DEFAULT_ADDRESS)));

		return results;
	}

	/**
	 * Set the relay identity to use.
	 * 
	 * @param identity
	 *        the identity; defaults to {@link UsbRelayUtils#DEFAULT_IDENTITY}
	 */
	public void setIdentity(int identity) {
		this.identity = identity;
	}

	/**
	 * Set the relay address.
	 * 
	 * @param address
	 *        the address, starting from {@literal 1}; defaults to
	 *        {@link #DEFAULT_ADDRESS}
	 */
	public void setAddress(int address) {
		this.address = address;
	}

	/**
	 * Set the control ID.
	 * 
	 * @param controlId
	 *        the control ID to use; defaults to
	 *        {@link UsbRelayToggler#DEFAULT_CONTROL_ID}
	 * @throws IllegalArgumentException
	 *         if {@code controlId} is {@literal null}
	 */
	public void setControlId(String controlId) {
		if ( controlId == null ) {
			throw new IllegalArgumentException("Control ID must not be null.");
		}
		this.controlId = controlId;
	}

}
