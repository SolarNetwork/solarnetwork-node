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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.NodeControlProvider;
import net.solarnetwork.node.domain.NodeControlInfoDatum;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusFunction;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusWriteFunction;
import net.solarnetwork.node.io.modbus.support.ModbusDeviceSupport;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.support.DatumEvents;
import net.solarnetwork.util.CachedResult;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.StringUtils;

/**
 * Control a Modbus "coil" or "holding" type digital switch register on and off.
 * 
 * @author matt
 * @version 2.1
 */
public class ModbusToggler extends ModbusDeviceSupport
		implements SettingSpecifierProvider, NodeControlProvider, InstructionHandler, DatumEvents {

	/** The default value for the {@code address} property. */
	public static final int DEFAULT_ADDRESS = 0x4008;

	/** The default value for the {@code controlId} property. */
	public static final String DEFAULT_CONTROL_ID = "/switch/1";

	private final AtomicReference<CachedResult<NodeControlInfoDatum>> cachedSample = new AtomicReference<CachedResult<NodeControlInfoDatum>>(
			null);

	private int address = DEFAULT_ADDRESS;
	private ModbusWriteFunction function = ModbusWriteFunction.WriteCoil;
	private String controlId = DEFAULT_CONTROL_ID;
	private long sampleCacheMs = 5000;
	private OptionalService<EventAdmin> eventAdmin;

	@Override
	protected Map<String, Object> readDeviceInfo(ModbusConnection conn) {
		return null;
	}

	/**
	 * Get the discreet values, as a Boolean.
	 * 
	 * @return Boolean for the switch status, or {@literal null} if not known
	 */
	private NodeControlInfoDatum currentValue() throws IOException {
		CachedResult<NodeControlInfoDatum> result = cachedSample.get();
		if ( result == null || !result.isValid() ) {
			Boolean value = readCurrentValue();
			if ( value != null ) {
				NodeControlInfoDatum data = newNodeControlInfoDatum(this.controlId, value);
				cachedSample.compareAndSet(result, new CachedResult<NodeControlInfoDatum>(data,
						sampleCacheMs, TimeUnit.MILLISECONDS));
				result = cachedSample.get();
			}
		}
		return (result != null ? result.getResult() : null);
	}

	private synchronized Boolean readCurrentValue() throws IOException {
		final ModbusWriteFunction function = getFunction();
		final Integer address = getAddress();
		Boolean result = performAction(new ModbusConnectionAction<Boolean>() {

			@Override
			public Boolean doWithConnection(ModbusConnection conn) throws IOException {
				if ( function == ModbusWriteFunction.WriteCoil
						|| function == ModbusWriteFunction.WriteMultipleCoils ) {
					BitSet bits = conn.readDiscreetValues(address, 1);
					return (bits != null ? bits.get(0) : null);
				}

				// for all other functions, write as unsigned short value with 1 for true, 0 for false
				ModbusFunction readFunction = function.oppositeFunction();
				if ( readFunction != null ) {
					int[] data = conn.readWordsUnsigned(
							ModbusReadFunction.forCode(readFunction.getCode()), address, 1);
					return (data != null && data.length > 0 && data[0] == 1 ? true : false);
				} else {
					log.warn("Unsupported Modbus function for reading value: {}", function);
				}
				return null;
			}
		});
		log.info("Read {} value: {}", controlId, result);
		return result;
	}

	/**
	 * Set the modbus register to a true/false value.
	 * 
	 * @param desiredValue
	 *        the desired value to set
	 * @return {@literal true} if the write succeeded
	 * @throws IOException
	 *         if an IO error occurs
	 */
	private synchronized boolean setValue(final Boolean desiredValue) throws IOException {
		log.info("Setting {} value to {}", controlId, desiredValue);
		final ModbusWriteFunction function = getFunction();
		final Integer address = getAddress();

		boolean result = performAction(new ModbusConnectionAction<Boolean>() {

			@Override
			public Boolean doWithConnection(ModbusConnection conn) throws IOException {
				if ( function == ModbusWriteFunction.WriteCoil
						|| function == ModbusWriteFunction.WriteMultipleCoils ) {
					final BitSet bits = new BitSet(1);
					bits.set(0, desiredValue);
					conn.writeDiscreetValues(new int[] { address }, bits);
					return true;
				}

				// for all other functions, write as unsigned short value with 1 for true, 0 for false
				conn.writeWords(function, address,
						new int[] { Boolean.TRUE.equals(desiredValue) ? 1 : 0 });
				return true;
			}
		});
		if ( result ) {
			NodeControlInfoDatum data = newNodeControlInfoDatum(this.controlId, result);
			cachedSample.set(
					new CachedResult<NodeControlInfoDatum>(data, sampleCacheMs, TimeUnit.MILLISECONDS));
		}
		return result;
	}

	// NodeControlProvider

	@Override
	public List<String> getAvailableControlIds() {
		return Collections.singletonList(controlId);
	}

	@Override
	public String getUID() {
		return getControlId();
	}

	@Override
	public NodeControlInfo getCurrentControlInfo(String controlId) {
		if ( this.controlId == null || !this.controlId.equals(controlId) ) {
			return null;
		}
		// read the control's current status
		log.debug("Reading {} status", controlId);
		NodeControlInfoDatum result = null;
		try {
			result = currentValue();
		} catch ( Exception e ) {
			log.error("Error reading {} status: {}", controlId, e.getMessage());
		}
		if ( result != null ) {
			postControlEvent(result, NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CAPTURED);
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

	private void postControlEvent(NodeControlInfoDatum info, String topic) {
		final EventAdmin admin = (eventAdmin != null ? eventAdmin.service() : null);
		if ( admin == null ) {
			return;
		}
		Event event = DatumEvents.datumEvent(topic, info);
		admin.postEvent(event);
	}

	// InstructionHandler

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SET_CONTROL_PARAMETER.equals(topic);
	}

	@Override
	public InstructionState processInstruction(Instruction instruction) {
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
					postControlEvent(newNodeControlInfoDatum(controlId, desiredValue),
							NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CHANGED);
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
		return "Modbus Switch Toggler";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		ModbusToggler defaults = new ModbusToggler();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);

		// get current value
		BasicTitleSettingSpecifier status = new BasicTitleSettingSpecifier("status", "N/A", true);
		try {
			NodeControlInfoDatum val = currentValue();
			if ( val != null ) {
				status.setDefaultValue(val.getValue());
			}
		} catch ( Exception e ) {
			log.debug("Error reading {} status: {}", controlId, e.getMessage());
		}
		results.add(status);

		results.add(new BasicTextFieldSettingSpecifier("controlId", defaults.getControlId()));
		results.add(new BasicTextFieldSettingSpecifier("groupUID", defaults.getGroupUID()));
		results.add(new BasicTextFieldSettingSpecifier("modbusNetwork.propertyFilters['UID']",
				"Serial Port"));
		results.add(new BasicTextFieldSettingSpecifier("unitId", String.valueOf(defaults.getUnitId())));
		results.add(
				new BasicTextFieldSettingSpecifier("address", String.valueOf(defaults.getAddress())));

		// drop-down menu for function
		BasicMultiValueSettingSpecifier functionSpec = new BasicMultiValueSettingSpecifier(
				"functionCode", defaults.getFunctionCode());
		Map<String, String> functionTitles = new LinkedHashMap<String, String>(4);
		for ( ModbusWriteFunction e : ModbusWriteFunction.values() ) {
			functionTitles.put(e.toString(), e.toDisplayString());
		}
		functionSpec.setValueTitles(functionTitles);
		results.add(functionSpec);

		return results;
	}

	public int getAddress() {
		return address;
	}

	public void setAddress(int address) {
		this.address = address;
	}

	public String getControlId() {
		return controlId;
	}

	public void setControlId(String controlId) {
		this.controlId = controlId;
	}

	public OptionalService<EventAdmin> getEventAdmin() {
		return eventAdmin;
	}

	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	/**
	 * Get the Modbus write function to use.
	 * 
	 * @return the write function
	 * @since 1.3
	 */
	public ModbusWriteFunction getFunction() {
		return function;
	}

	/**
	 * Set the write function to use.
	 * 
	 * @param function
	 *        the function to write to Modbus with
	 * @since 1.3
	 */
	public void setFunction(ModbusWriteFunction function) {
		this.function = function;
	}

	/**
	 * Get the Modbus function code to use as a string.
	 * 
	 * @return the Modbus function code as a string
	 * @since 1.3
	 */
	public String getFunctionCode() {
		return String.valueOf(function.getCode());
	}

	/**
	 * Set the Modbus function to use as a string.
	 * 
	 * @param function
	 *        the Modbus function
	 * @since 1.3
	 */
	public void setFunctionCode(String function) {
		if ( function == null ) {
			return;
		}
		ModbusWriteFunction f = null;
		try {
			try {
				f = ModbusWriteFunction.forCode(Integer.parseInt(function));
			} catch ( NumberFormatException e ) {
				// backwards compatibility hook for enum name
				f = ModbusWriteFunction.valueOf(function);
			}
		} catch ( IllegalArgumentException e ) {
			// ignore
		}
		setFunction(f);
	}

	/**
	 * Get the sample cache maximum age, in milliseconds.
	 * 
	 * @return the cache milliseconds
	 * @since 1.3
	 */
	public long getSampleCacheMs() {
		return sampleCacheMs;
	}

	/**
	 * Set the sample cache maximum age, in milliseconds.
	 * 
	 * @param sampleCacheMs
	 *        the cache milliseconds
	 * @since 1.3
	 */
	public void setSampleCacheMs(long sampleCacheMs) {
		this.sampleCacheMs = sampleCacheMs;
	}
}
