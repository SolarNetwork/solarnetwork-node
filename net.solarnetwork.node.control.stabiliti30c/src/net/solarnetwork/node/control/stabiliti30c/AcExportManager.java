/* ==================================================================
 * AcExportManager.java - 2/09/2019 8:57:12 am
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

package net.solarnetwork.node.control.stabiliti30c;

import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.DateUtils.formatForLocalDisplay;
import java.io.IOException;
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
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.domain.datum.SimpleNodeControlInfoDatum;
import net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cAcControlMethod;
import net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cControlAccessor;
import net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cData;
import net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cDataAccessor;
import net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cDcControlMethod;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.io.modbus.support.ModbusDataDeviceSupport;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.NodeControlProvider;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.StringUtils;

/**
 * Control component that can respond to
 * {@link InstructionHandler#TOPIC_SHED_LOAD} instructions for the purposes of
 * managing AC power export.
 * 
 * @author matt
 * @version 2.0
 */
public class AcExportManager extends ModbusDataDeviceSupport<Stabiliti30cData>
		implements InstructionHandler, NodeControlProvider, SettingSpecifierProvider {

	private OptionalService<EventAdmin> eventAdmin;
	private String controlId;

	/**
	 * Constructor.
	 */
	public AcExportManager() {
		super(new Stabiliti30cData());
	}

	/**
	 * Call once after properties are configured to initialize.
	 */
	public void startup() {
		try {
			getCurrentSample();
		} catch ( Exception e ) {
			log.warn("Unable to initialize AC export power settings from Stabiliti {}: {}",
					modbusDeviceName(), e.getMessage());
		}
	}

	/**
	 * Call once before disposing of component.
	 */
	public void resetDeviceSettings() {
		try {
			log.info("Resetting AC export power settings on Stabiliti {}", modbusDeviceName());
			performAction(new ModbusConnectionAction<Void>() {

				@Override
				public Void doWithConnection(ModbusConnection connection) throws IOException {
					final Stabiliti30cData sample = getSample();
					sample.performUpdates(new ModbusDataUpdateAction() {

						@Override
						public boolean updateModbusData(MutableModbusData m) throws IOException {
							resetAcExportSettings(connection, sample, m);
							return false;
						}
					});
					return null;
				}
			});
		} catch ( Exception e ) {
			log.warn("Unable to reset AC export power settings on Stabiliti {}: {}", modbusDeviceName(),
					e.getMessage());
		}
	}

	@Override
	protected void readDeviceInfoFirstTime(ModbusConnection connection, Stabiliti30cData sample)
			throws IOException {
		// when reading data for the very first time, make sure Stabiliti is setup in a known "good" state
		sample.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) throws IOException {
				log.info("Configuring initial AC export power settings on Stabiliti {}",
						modbusDeviceName());
				resetAcExportSettings(connection, sample, m);
				return false;
			}
		});
		super.readDeviceInfoFirstTime(connection, sample);
	}

	@Override
	protected void refreshDeviceInfo(ModbusConnection connection, Stabiliti30cData sample)
			throws IOException {
		sample.readConfigurationData(connection);
	}

	@Override
	protected void refreshDeviceData(ModbusConnection connection, Stabiliti30cData sample)
			throws IOException {
		sample.readControlData(connection);
		sample.readPowerControlData(connection);
	}

	private static SimpleNodeControlInfoDatum datumForSample(Stabiliti30cDataAccessor sample,
			String controlId) {
		final Integer value = (sample.getP1ActivePowerSetpoint() != null
				? sample.getP1ActivePowerSetpoint()
				: 0);
		NodeControlInfo info = BasicNodeControlInfo.builder().withControlId(controlId)
				.withReadonly(false).withType(NodeControlPropertyType.Integer)
				.withValue(value.toString()).build();
		return new SimpleNodeControlInfoDatum(info, sample.getDataTimestamp());
	}

	/* === NodeControlProvider === */

	@Override
	public List<String> getAvailableControlIds() {
		final String controlId = getControlId();
		return (controlId == null ? Collections.emptyList() : Collections.singletonList(controlId));
	}

	@Override
	public NodeControlInfo getCurrentControlInfo(String controlId) {
		final String myControlId = getControlId();
		if ( myControlId == null || !myControlId.equals(controlId) ) {
			return null;
		}
		return readCurrentDatum();
	}

	private SimpleNodeControlInfoDatum readCurrentDatum() {
		final Instant start = Instant.now();
		final String controlId = getControlId();
		try {
			final Stabiliti30cData currSample = getCurrentSample();
			if ( currSample == null ) {
				return null;
			}
			SimpleNodeControlInfoDatum d = datumForSample(currSample, resolvePlaceholders(controlId));
			if ( !currSample.getDataTimestamp().isBefore(start) ) {
				// we read from the device
				postControlInfoCapturedEvent(d);
			}
			return d;
		} catch ( IOException e ) {
			log.error("Communication problem reading source {} from Stabiliti {}: {}", controlId,
					modbusDeviceName(), e.getMessage());
			return null;
		}
	}

	private void postControlInfoCapturedEvent(SimpleNodeControlInfoDatum info) {
		postControlEvent(info, EVENT_TOPIC_CONTROL_INFO_CAPTURED);
	}

	private void postControlEvent(SimpleNodeControlInfoDatum info, String topic) {
		final EventAdmin admin = service(eventAdmin);
		if ( admin == null ) {
			return;
		}
		Event event = DatumEvents.datumEvent(topic, info);
		admin.postEvent(event);
	}

	/* === InstructionHandler === */

	@Override
	public boolean handlesTopic(String topic) {
		return TOPIC_SHED_LOAD.equals(topic) || TOPIC_SET_CONTROL_PARAMETER.equals(topic);
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
		String controlId = getControlId();
		if ( instruction == null || controlId == null ) {
			return null;
		}
		String topic = instruction.getTopic();
		Integer targetPower = null;
		if ( TOPIC_SHED_LOAD.equals(topic) || TOPIC_SET_CONTROL_PARAMETER.equals(topic) ) {
			try {
				targetPower = Integer.valueOf(instruction.getParameterValue(controlId));
			} catch ( NumberFormatException | NullPointerException e ) {
				log.warn("ShedLoad target missing or not a number: {}", e.getMessage());
				return null;
			}
		} else {
			return null;
		}
		if ( targetPower.intValue() == 0 ) {
			resetDeviceSettings();
		} else {
			adjustAcExportPower(targetPower);
		}
		return InstructionUtils.createStatus(instruction, InstructionState.Completed);
	}

	private synchronized void adjustAcExportPower(final Integer targetPower) {
		final ModbusNetwork modbus = modbusNetwork();
		final int unitId = getUnitId();
		if ( modbus == null ) {
			log.warn("No ModbusNetwork available for AC export management task for Stabiliti {}",
					unitId);
			return;
		}
		final SimpleNodeControlInfoDatum datum = readCurrentDatum();
		final Integer currSetpointValueInteger = datum.asSampleOperations().getSampleInteger(
				DatumSamplesType.Instantaneous,
				SimpleNodeControlInfoDatum.DEFAULT_INSTANT_PROPERTY_NAME);
		final int currSetpointValue = (currSetpointValueInteger != null
				? currSetpointValueInteger.intValue()
				: 0);
		final int desiredSetpointValue = (targetPower != null ? targetPower.intValue() : 0);
		if ( currSetpointValue == desiredSetpointValue ) {
			log.info("Stabiliti {} AC export power setpoint already configured as {}, nothing to do",
					modbusDeviceName(), currSetpointValue);
		}
		log.info("Setting AC export power setpoint to {}W on Stabiliti {}", targetPower,
				modbusDeviceName());
		try {
			modbus.performAction(unitId, new ModbusConnectionAction<Void>() {

				@Override
				public Void doWithConnection(ModbusConnection connection) throws IOException {
					final Stabiliti30cData sample = getSample();
					sample.performUpdates(new ModbusDataUpdateAction() {

						@Override
						public boolean updateModbusData(MutableModbusData m) throws IOException {
							setAcExportSettings(connection, sample, m, targetPower, true);
							return false;
						}

					});
					return null;
				}
			});
		} catch ( Exception e ) {
			log.error("Exception adjusting AC export power to {} for Stabiliti {}: {}", targetPower,
					modbusDeviceName(), e.getMessage(), e);
		}

	}

	private synchronized void setAcExportSettings(ModbusConnection connection, Stabiliti30cData sample,
			MutableModbusData m, Integer targetPower, boolean manualModeEnabled) throws IOException {
		if ( manualModeEnabled != sample.isManualModeEnabled() ) {
			// first set control methods to safe starting values...
			setAcExportSettings(connection, sample, m, Stabiliti30cAcControlMethod.Idle,
					Stabiliti30cDcControlMethod.Idle, Stabiliti30cDcControlMethod.Idle, null, null,
					true);
		}

		// then set control methods to desired values
		setAcExportSettings(connection, sample, m, Stabiliti30cAcControlMethod.GridPower,
				Stabiliti30cDcControlMethod.Net, Stabiliti30cDcControlMethod.Mppt, targetPower,
				manualModeEnabled, false);
	}

	private synchronized void resetAcExportSettings(ModbusConnection connection, Stabiliti30cData sample,
			MutableModbusData m) throws IOException {
		setAcExportSettings(connection, sample, m, Stabiliti30cAcControlMethod.Idle,
				Stabiliti30cDcControlMethod.Idle, Stabiliti30cDcControlMethod.Idle, null, null, true);
		setAcExportSettings(connection, sample, m, Stabiliti30cAcControlMethod.Net,
				Stabiliti30cDcControlMethod.Idle, Stabiliti30cDcControlMethod.Mppt, 0, false, true);
	}

	private synchronized void setAcExportSettings(ModbusConnection connection, Stabiliti30cData sample,
			MutableModbusData m, Stabiliti30cAcControlMethod p1Method,
			Stabiliti30cDcControlMethod p2Method, Stabiliti30cDcControlMethod p3Method,
			Integer targetPower, Boolean manualModeEnabled, boolean force) throws IOException {
		Stabiliti30cControlAccessor acc = sample.controlAccessor(connection);

		if ( force || sample.getP1ControlMethod() != p1Method ) {
			acc.setP1ControlMethod(p1Method);
		}
		if ( force || sample.getP2ControlMethod() != p2Method ) {
			acc.setP2ControlMethod(p2Method);
		}
		if ( force || sample.getP3ControlMethod() != p3Method ) {
			acc.setP3ControlMethod(p3Method);
		}

		// 
		if ( targetPower != null && (force
				|| (targetPower != null && !targetPower.equals(sample.getP1ActivePowerSetpoint()))) ) {
			acc.setP1ActivePowerSetpoint(targetPower);
		}

		if ( manualModeEnabled != null
				&& (force || manualModeEnabled != sample.isManualModeEnabled()) ) {
			acc.setManualModeEnabled(manualModeEnabled);
		}
	}

	/* === SettingSpecifierProvider === */

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.control.stabiliti30c.AcExportManager";
	}

	@Override
	public String getDisplayName() {
		return "Stabiliti AC Load Manager";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(12);
		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(), true));
		results.add(new BasicTitleSettingSpecifier("sample", getSampleMessage(getSample()), true));

		results.addAll(baseIdentifiableSettings(""));
		results.addAll(modbusNetworkSettings(""));
		results.addAll(modbusDeviceNetworkSettings(""));

		results.add(new BasicTextFieldSettingSpecifier("controlId", ""));

		return results;
	}

	private String getInfoMessage() {
		String msg = null;
		try {
			msg = getDeviceInfoMessage();
		} catch ( RuntimeException e ) {
			log.debug("Error reading info: {}", e.getMessage());
		}
		return (msg == null ? "N/A" : msg);
	}

	private String getSampleMessage(Stabiliti30cDataAccessor sample) {
		if ( sample.getDataTimestamp() == null || controlId == null ) {
			return "N/A";
		}

		Map<String, Object> data = new LinkedHashMap<>(2);
		data.put("P1 Active Power", sample.getP1ActivePower());
		data.put("P1 Active Power Setpoint", sample.getP1ActivePowerSetpoint());

		if ( data.isEmpty() ) {
			return "N/A";
		}

		StringBuilder buf = new StringBuilder();
		buf.append(StringUtils.delimitedStringFromMap(data));
		buf.append("; sampled at ").append(formatForLocalDisplay(sample.getDataTimestamp()));
		return buf.toString();
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
