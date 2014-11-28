/* ==================================================================
 * ModbusPCMController.java - Jul 10, 2013 7:14:40 AM
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

package net.solarnetwork.node.control.sma.pcm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.List;
import java.util.Map;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.NodeControlProvider;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.NodeControlInfoDatum;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusDeviceSupport;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.util.ClassUtils;
import net.solarnetwork.util.OptionalService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.springframework.context.MessageSource;

/**
 * Toggle four Modbus "coil" type addresses to control the SMA Power Control
 * Module.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>d1Address</dt>
 * <dd>The Modbus address for the PCM D1 input.</dd>
 * <dt>d2Address</dt>
 * <dd>The Modbus address for the PCM D2 input.</dd>
 * <dt>d3Address</dt>
 * <dd>The Modbus address for the PCM D3 input.</dd>
 * <dt>d4Address</dt>
 * <dd>The Modbus address for the PCM D4 input.</dd>
 * 
 * <dt>eventAdmin</dt>
 * <dd>An {@link EventAdmin} to publish events with.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.4
 */
public class ModbusPCMController extends ModbusDeviceSupport implements SettingSpecifierProvider,
		NodeControlProvider, InstructionHandler {

	/**
	 * The suffix added to the configured control ID to handle percent-based PCM
	 * values.
	 * 
	 * @since 1.3
	 */
	public static final String PERCENT_CONTROL_ID_SUFFIX = "?percent";

	private Integer d1Address = 0x4000;
	private Integer d2Address = 0x4002;
	private Integer d3Address = 0x4004;
	private Integer d4Address = 0x4006;

	private String controlId = "/power/pcm/1";
	private String groupUID;
	private int sampleCacheSeconds = 1;

	private OptionalService<EventAdmin> eventAdmin;
	private MessageSource messageSource;

	private final long sampleCaptureDate = 0;
	private BitSet cachedSample = null;

	private boolean isCachedSampleExpired() {
		final long lastReadDiff = System.currentTimeMillis() - sampleCaptureDate;
		if ( lastReadDiff > (sampleCacheSeconds * 1000) ) {
			return true;
		}
		return false;
	}

	@Override
	protected Map<String, Object> readDeviceInfo(ModbusConnection conn) {
		return null;
	}

	/**
	 * Get the values of the D1 - D4 discreet values, as a BitSet.
	 * 
	 * @return BitSet, with index 0 representing D1 and index 1 representing D2,
	 *         etc.
	 */
	private synchronized BitSet currentDiscreetValue() throws IOException {
		BitSet result;
		if ( isCachedSampleExpired() ) {
			result = performAction(new ModbusConnectionAction<BitSet>() {

				@Override
				public BitSet doWithConnection(ModbusConnection conn) throws IOException {
					return conn.readDiscreetValues(new Integer[] { d1Address, d2Address, d3Address,
							d4Address }, 1);
				}
			});
			log.debug("Read discreet PCM values: {}", result);
			Integer status = integerValueForBitSet(result);
			postControlCapturedEvent(newNodeControlInfoDatum(getPercentControlId(), status, true));
			cachedSample = result;
		} else {
			result = cachedSample;
		}
		return result;
	}

	/**
	 * Get the status value of the PCM, as an Integer.
	 * 
	 * <p>
	 * This returns the overall vale of the PCM, as an integer between 0 and 15.
	 * A value of 0 represent a 0% output setting, while 15 represents 100%.
	 * </p>
	 * 
	 * @return an integer between 0 and 15
	 */
	private Integer integerValueForBitSet(BitSet bits) {
		return ((bits.get(0) ? 1 : 0) | ((bits.get(1) ? 1 : 0) << 1) | ((bits.get(2) ? 1 : 0) << 2) | ((bits
				.get(3) ? 1 : 0) << 3));
	}

	private static final int PCM_LEVEL_0 = 0;
	private static final int PCM_LEVEL_1 = 5;
	private static final int PCM_LEVEL_2 = 10;
	private static final int PCM_LEVEL_3 = 16;
	private static final int PCM_LEVEL_4 = 23;
	private static final int PCM_LEVEL_5 = 30;
	private static final int PCM_LEVEL_6 = 36;
	private static final int PCM_LEVEL_7 = 42;
	private static final int PCM_LEVEL_8 = 50;
	private static final int PCM_LEVEL_9 = 57;
	private static final int PCM_LEVEL_10 = 65;
	private static final int PCM_LEVEL_11 = 72;
	private static final int PCM_LEVEL_12 = 80;
	private static final int PCM_LEVEL_13 = 86;
	private static final int PCM_LEVEL_14 = 93;
	private static final int PCM_LEVEL_15 = 100;

	/**
	 * Get the approximate power output setting, from 0 to 100.
	 * 
	 * <p>
	 * These values are described in the SMA documentation, it is not a direct
	 * percentage value derived from the value itself.
	 * </p>
	 */
	private Integer percentValueForIntegerValue(Integer val) {
		switch (val) {
			case 1:
				return PCM_LEVEL_1;
			case 2:
				return PCM_LEVEL_2;
			case 3:
				return PCM_LEVEL_3;
			case 4:
				return PCM_LEVEL_4;
			case 5:
				return PCM_LEVEL_5;
			case 6:
				return PCM_LEVEL_6;
			case 7:
				return PCM_LEVEL_7;
			case 8:
				return PCM_LEVEL_8;
			case 9:
				return PCM_LEVEL_9;
			case 10:
				return PCM_LEVEL_10;
			case 11:
				return PCM_LEVEL_11;
			case 12:
				return PCM_LEVEL_12;
			case 13:
				return PCM_LEVEL_13;
			case 14:
				return PCM_LEVEL_14;
			default:
				return (val < 1 ? PCM_LEVEL_0 : PCM_LEVEL_15);
		}
	}

	/**
	 * Get the appropriate power output value, from 0 to 15, from an integer
	 * percentage (0-100). Note that the value is floored, such that the PCM
	 * value can never be larger than the percentage value passed in.
	 * 
	 * @param percent
	 *        an integer percentage from 0-100
	 * @return a PCM output value from 0-15
	 */
	private Integer pcmValueForPercentValue(Integer percent) {
		final int p = (percent == null ? 0 : percent.intValue());
		if ( p < PCM_LEVEL_1 ) {
			return 0;
		}
		if ( p < PCM_LEVEL_2 ) {
			return 1;
		}
		if ( p < PCM_LEVEL_3 ) {
			return 2;
		}
		if ( p < PCM_LEVEL_4 ) {
			return 3;
		}
		if ( p < PCM_LEVEL_5 ) {
			return 4;
		}
		if ( p < PCM_LEVEL_6 ) {
			return 5;
		}
		if ( p < PCM_LEVEL_7 ) {
			return 6;
		}
		if ( p < PCM_LEVEL_8 ) {
			return 7;
		}
		if ( p < PCM_LEVEL_9 ) {
			return 8;
		}
		if ( p < PCM_LEVEL_10 ) {
			return 9;
		}
		if ( p < PCM_LEVEL_11 ) {
			return 10;
		}
		if ( p < PCM_LEVEL_12 ) {
			return 11;
		}
		if ( p < PCM_LEVEL_13 ) {
			return 12;
		}
		if ( p < PCM_LEVEL_14 ) {
			return 13;
		}
		if ( p < PCM_LEVEL_15 ) {
			return 14;
		}
		// all systems go!
		return 15;
	}

	private synchronized boolean setPCMStatus(Integer desiredValue) {
		final BitSet bits = new BitSet(4);
		final int v = desiredValue;
		for ( int i = 0; i < 4; i++ ) {
			bits.set(i, ((v >> i) & 1) == 1);
		}
		log.info("Setting PCM status to {} ({}%)", desiredValue,
				percentValueForIntegerValue(desiredValue));
		final Integer[] addresses = new Integer[] { d1Address, d2Address, d3Address, d4Address };
		try {
			return performAction(new ModbusConnectionAction<Boolean>() {

				@Override
				public Boolean doWithConnection(ModbusConnection conn) throws IOException {
					return conn.writeDiscreetValues(addresses, bits);
				}
			});
		} catch ( IOException e ) {
			log.error("Error communicating with PCM: {}", e.getMessage());
		}
		return false;
	}

	// NodeControlProvider

	private String getPercentControlId() {
		return controlId + PERCENT_CONTROL_ID_SUFFIX;
	}

	@Override
	public String getUID() {
		return getControlId();
	}

	@Override
	public List<String> getAvailableControlIds() {
		return Arrays.asList(controlId, getPercentControlId());
	}

	@Override
	public NodeControlInfo getCurrentControlInfo(String controlId) {
		// read the control's current status
		log.debug("Reading PCM {} status", controlId);
		NodeControlInfoDatum result = null;
		try {
			Integer value = integerValueForBitSet(currentDiscreetValue());
			result = newNodeControlInfoDatum(controlId, value,
					controlId.endsWith(PERCENT_CONTROL_ID_SUFFIX));
		} catch ( Exception e ) {
			log.error("Error reading PCM {} status: {}", controlId, e.getMessage());
		}
		return result;
	}

	private NodeControlInfoDatum newNodeControlInfoDatum(String controlId, Integer status,
			boolean asPercent) {
		NodeControlInfoDatum info = new NodeControlInfoDatum();
		info.setCreated(new Date());
		info.setSourceId(controlId);
		info.setType(NodeControlPropertyType.Integer);
		info.setReadonly(false);
		if ( asPercent ) {
			info.setValue(percentValueForIntegerValue(status).toString());
		} else {
			info.setValue(status.toString());
		}
		return info;
	}

	// InstructionHandler

	@Override
	public boolean handlesTopic(String topic) {
		return (InstructionHandler.TOPIC_SET_CONTROL_PARAMETER.equals(topic) || InstructionHandler.TOPIC_DEMAND_BALANCE
				.equals(topic));
	}

	@Override
	public InstructionState processInstruction(Instruction instruction) {
		// look for a parameter name that matches a control ID
		InstructionState result = null;
		log.debug("Inspecting instruction {} against control {}", instruction.getTopic(), controlId);
		final String percentControlId = getPercentControlId();
		for ( String paramName : instruction.getParameterNames() ) {
			log.trace("Got instruction parameter {}", paramName);
			if ( controlId.equals(paramName) || percentControlId.equals(paramName) ) {
				String str = instruction.getParameterValue(paramName);
				// by default, treat parameter value as a decimal integer, value between 0-15
				Integer desiredValue = Integer.parseInt(str);
				if ( paramName.equals(percentControlId)
						|| InstructionHandler.TOPIC_DEMAND_BALANCE.equals(instruction.getTopic()) ) {
					// treat as a percentage integer 0-100, translate to 0-15
					Integer val = pcmValueForPercentValue(desiredValue);
					log.info("Percent output request to {}%; PCM output to be capped at {} ({}%)",
							desiredValue, val, percentValueForIntegerValue(val));
					desiredValue = val;
				}
				if ( setPCMStatus(desiredValue) ) {
					result = InstructionState.Completed;
				} else {
					result = InstructionState.Declined;
				}
			}
		}
		return result;
	}

	/**
	 * Post a {@link NodeControlProvider#EVENT_TOPIC_CONTROL_INFO_CAPTURED}
	 * {@link Event}.
	 * 
	 * <p>
	 * This method calls {@link #createControlCapturedEvent(NodeControlInfo)} to
	 * create the actual Event, which may be overridden by extending classes.
	 * </p>
	 * 
	 * @param info
	 *        the {@link NodeControlInfo} to post the event for
	 * @since 1.2
	 */
	protected final void postControlCapturedEvent(final NodeControlInfo info) {
		EventAdmin ea = (eventAdmin == null ? null : eventAdmin.service());
		if ( ea == null || info == null ) {
			return;
		}
		Event event = createControlCapturedEvent(info);
		ea.postEvent(event);
	}

	/**
	 * Create a new
	 * {@link NodeControlProvider#EVENT_TOPIC_CONTROL_INFO_CAPTURED}
	 * {@link Event} object out of a {@link Datum}.
	 * 
	 * <p>
	 * This method will populate all simple properties of the given
	 * {@link Datum} into the event properties, along with the
	 * {@link DatumDataSource#EVENT_DATUM_CAPTURED_DATUM_TYPE}.
	 * 
	 * @param info
	 *        the info to create the event for
	 * @return the new Event instance
	 * @since 1.2
	 */
	protected Event createControlCapturedEvent(final NodeControlInfo info) {
		Map<String, Object> props = ClassUtils.getSimpleBeanProperties(info, null);
		log.debug("Created {} event with props {}",
				NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CAPTURED, props);
		return new Event(NodeControlProvider.EVENT_TOPIC_CONTROL_INFO_CAPTURED, props);
	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.control.sma.pcm";
	}

	@Override
	public String getDisplayName() {
		return "SMA Power Control Module";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		ModbusPCMController defaults = new ModbusPCMController();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);

		// get current value
		BasicTitleSettingSpecifier status = new BasicTitleSettingSpecifier("status", "N/A", true);
		try {
			BitSet bits = currentDiscreetValue();
			Integer val = integerValueForBitSet(bits);
			String binValue = Integer.toBinaryString(val);
			String padding = "";
			if ( binValue.length() < 4 ) {
				padding = String.format("%0" + (4 - binValue.length()) + "d", 0);
			}
			status.setDefaultValue(String.format("%s%s - %d%%", padding, binValue,
					percentValueForIntegerValue(val)));
		} catch ( Exception e ) {
			log.debug("Error reading PCM status: {}", e.getMessage());
		}
		results.add(status);

		results.add(new BasicTextFieldSettingSpecifier("controlId", defaults.controlId));
		results.add(new BasicTextFieldSettingSpecifier("groupUID", defaults.groupUID));
		results.add(new BasicTextFieldSettingSpecifier("modbusNetwork.propertyFilters['UID']",
				"Serial Port"));
		results.add(new BasicTextFieldSettingSpecifier("unitId", String.valueOf(defaults.getUnitId())));
		results.add(new BasicTextFieldSettingSpecifier("d1Address", defaults.d1Address.toString()));
		results.add(new BasicTextFieldSettingSpecifier("d2Address", defaults.d2Address.toString()));
		results.add(new BasicTextFieldSettingSpecifier("d3Address", defaults.d3Address.toString()));
		results.add(new BasicTextFieldSettingSpecifier("d4Address", defaults.d4Address.toString()));

		results.add(new BasicTextFieldSettingSpecifier("sampleCacheSeconds", String.valueOf(defaults
				.getSampleCacheSeconds())));

		return results;
	}

	@Override
	public MessageSource getMessageSource() {
		return messageSource;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	public Integer getD1Address() {
		return d1Address;
	}

	public void setD1Address(Integer d1Address) {
		this.d1Address = d1Address;
	}

	public Integer getD2Address() {
		return d2Address;
	}

	public void setD2Address(Integer d2Address) {
		this.d2Address = d2Address;
	}

	public Integer getD3Address() {
		return d3Address;
	}

	public void setD3Address(Integer d3Address) {
		this.d3Address = d3Address;
	}

	public Integer getD4Address() {
		return d4Address;
	}

	public void setD4Address(Integer d4Address) {
		this.d4Address = d4Address;
	}

	public String getControlId() {
		return controlId;
	}

	public void setControlId(String controlId) {
		this.controlId = controlId;
	}

	@Override
	public String getGroupUID() {
		return groupUID;
	}

	@Override
	public void setGroupUID(String groupUID) {
		this.groupUID = groupUID;
	}

	public OptionalService<EventAdmin> getEventAdmin() {
		return eventAdmin;
	}

	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	public int getSampleCacheSeconds() {
		return sampleCacheSeconds;
	}

	public void setSampleCacheSeconds(int sampleCacheSeconds) {
		this.sampleCacheSeconds = sampleCacheSeconds;
	}

}
