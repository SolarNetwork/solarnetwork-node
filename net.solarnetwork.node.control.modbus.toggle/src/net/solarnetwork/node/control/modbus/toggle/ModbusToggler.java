/* ==================================================================
 * ModbusToggler.java - Jul 15, 2013 7:48:20 AM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.modbus.toggle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.NodeControlProvider;
import net.solarnetwork.node.io.modbus.ModbusHelper;
import net.solarnetwork.node.io.modbus.ModbusHelper.ModbusConnectionCallback;
import net.solarnetwork.node.io.modbus.ModbusSerialConnectionFactory;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.support.NodeControlInfoDatum;
import net.solarnetwork.util.DynamicServiceTracker;
import net.wimpi.modbus.net.SerialConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Control a Modbus "coil" type register to turn a switch on or off.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>address</dt>
 * <dd>The Modbus address of the coil-type register to use.</dd>
 * <dt>unitId</dt>
 * <dd>The Modbus unit ID to use.</dd>
 * <dt>controlId</dt>
 * <dd>The {@link NodeControlProvider} UID to use.</dd>
 * <dt>connectionFactory</dt>
 * <dd>The {@link ModbusSerialConnectionFactory} to use.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class ModbusToggler implements SettingSpecifierProvider, NodeControlProvider, InstructionHandler {

	private static MessageSource MESSAGE_SOURCE;

	private Integer address = 0x4008;

	private Integer unitId = 1;
	private String controlId = "/switch/1";

	private DynamicServiceTracker<ModbusSerialConnectionFactory> connectionFactory;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Get the values of the discreet values, as a Boolen.
	 * 
	 * @return Boolean for the switch status
	 */
	private synchronized Boolean currentValue() {
		BitSet result = ModbusHelper.readDiscreetValues(connectionFactory, new Integer[] { address }, 1,
				this.unitId);
		if ( log.isInfoEnabled() ) {
			log.info("Read {} value: {}", controlId, result.get(0));
		}
		return result.get(0);
	}

	private synchronized Boolean setValue(Boolean desiredValue) {
		final BitSet bits = new BitSet(1);
		bits.set(0, desiredValue);
		log.info("Setting {} value to {}", controlId, desiredValue);
		final Integer[] addresses = new Integer[] { address };
		return ModbusHelper.execute(connectionFactory, new ModbusConnectionCallback<Boolean>() {

			@Override
			public Boolean doInConnection(SerialConnection conn) throws IOException {
				return ModbusHelper.writeDiscreetValues(conn, addresses, bits, unitId);
			}

		});
	}

	// NodeControlProvider

	@Override
	public List<String> getAvailableControlIds() {
		return Collections.singletonList(controlId);
	}

	@Override
	public NodeControlInfo getCurrentControlInfo(String controlId) {
		// read the control's current status
		log.debug("Reading {} status", controlId);
		NodeControlInfoDatum result = null;
		try {
			Boolean value = currentValue();
			result = newNodeControlInfoDatum(controlId, value);
		} catch ( RuntimeException e ) {
			log.error("Error reading {} status: {}", controlId, e.getMessage());
		}
		return result;
	}

	private NodeControlInfoDatum newNodeControlInfoDatum(String controlId, Boolean status) {
		NodeControlInfoDatum info = new NodeControlInfoDatum();
		info.setCreated(new Date());
		info.setSourceId(controlId);
		info.setType(NodeControlPropertyType.Boolean);
		info.setReadonly(false);
		info.setValue(status.toString());
		return info;
	}

	// InstructionHandler

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SET_CONTROL_PARAMETER.equals(topic);
	}

	@Override
	public InstructionState processInstruction(Instruction instruction) {
		// look for a parameter name that matches a control ID
		InstructionState result = null;
		log.debug("Inspecting instruction {} against control {}", instruction.getId(), controlId);
		for ( String paramName : instruction.getParameterNames() ) {
			log.trace("Got instruction parameter {}", paramName);
			if ( controlId.equals(paramName) ) {
				// treat parameter value as a boolean String
				String str = instruction.getParameterValue(controlId);
				Boolean desiredValue = Boolean.parseBoolean(str);
				if ( setValue(desiredValue) ) {
					result = InstructionState.Completed;
				} else {
					result = InstructionState.Declined;
				}
			}
		}
		return result;
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.control.modbus.toggle";
	}

	@Override
	public String getDisplayName() {
		return "Modbus Modem Resetter";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		ModbusToggler defaults = new ModbusToggler();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);

		// get current value
		BasicTitleSettingSpecifier status = new BasicTitleSettingSpecifier("status", "N/A", true);
		try {
			Boolean val = currentValue();
			status.setDefaultValue(val.toString());
		} catch ( RuntimeException e ) {
			log.debug("Error reading {} status: {}", controlId, e.getMessage());
		}
		results.add(status);

		results.add(new BasicTextFieldSettingSpecifier("connectionFactory.propertyFilters['UID']",
				"/dev/ttyUSB0"));
		results.add(new BasicTextFieldSettingSpecifier("unitId", defaults.unitId.toString()));
		results.add(new BasicTextFieldSettingSpecifier("controlId", defaults.controlId.toString()));
		results.add(new BasicTextFieldSettingSpecifier("address", defaults.address.toString()));

		return results;
	}

	@Override
	public MessageSource getMessageSource() {
		if ( MESSAGE_SOURCE == null ) {
			ResourceBundleMessageSource source = new ResourceBundleMessageSource();
			source.setBundleClassLoader(getClass().getClassLoader());
			source.setBasename(getClass().getName());
			MESSAGE_SOURCE = source;
		}
		return MESSAGE_SOURCE;
	}

	public Integer getAddress() {
		return address;
	}

	public void setAddress(Integer address) {
		this.address = address;
	}

	public Integer getUnitId() {
		return unitId;
	}

	public void setUnitId(Integer unitId) {
		this.unitId = unitId;
	}

	public String getControlId() {
		return controlId;
	}

	public void setControlId(String controlId) {
		this.controlId = controlId;
	}

	public DynamicServiceTracker<ModbusSerialConnectionFactory> getConnectionFactory() {
		return connectionFactory;
	}

	public void setConnectionFactory(
			DynamicServiceTracker<ModbusSerialConnectionFactory> connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

}
