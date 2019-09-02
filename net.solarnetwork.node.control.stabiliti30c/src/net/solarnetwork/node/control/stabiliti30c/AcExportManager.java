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

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
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
import net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cAcControlMethod;
import net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cControlAccessor;
import net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cData;
import net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cDataAccessor;
import net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cDcControlMethod;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;
import net.solarnetwork.node.io.modbus.ModbusDataDeviceSupport;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.StringUtils;

/**
 * Control component that can respond to
 * {@link InstructionHandler#TOPIC_SHED_LOAD} instructions for the purposes of
 * managing AC power export.
 * 
 * @author matt
 * @version 1.0
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
	public void shutdown() {
		try {
			performAction(new ModbusConnectionAction<Void>() {

				@Override
				public Void doWithConnection(ModbusConnection connection) throws IOException {
					final Stabiliti30cData sample = getSample();
					sample.performUpdates(new ModbusDataUpdateAction() {

						@Override
						public boolean updateModbusData(MutableModbusData m) {
							setAcExportSettings(connection, sample, m, 0, false);
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
	protected void readDeviceInfoFirstTime(ModbusConnection connection, Stabiliti30cData sample) {
		// when reading data for the very first time, make sure Stabiliti is setup in a known "good" state
		sample.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				setAcExportSettings(connection, sample, m, 0, false);
				return false;
			}
		});
		super.readDeviceInfoFirstTime(connection, sample);
	}

	@Override
	protected void refreshDeviceInfo(ModbusConnection connection, Stabiliti30cData sample) {
		sample.readConfigurationData(connection);
	}

	@Override
	protected void refreshDeviceData(ModbusConnection connection, Stabiliti30cData sample) {
		sample.readControlData(connection);
		sample.readPowerControlData(connection);
	}

	private static final class ControlDatum extends NodeControlInfoDatum {

		private final Stabiliti30cData sample;

		private ControlDatum(Stabiliti30cData sample, String controlId) {
			super();
			this.sample = sample;
			setCreated(new Date(sample.getDataTimestamp()));
			setReadonly(false);
			setSourceId(getControlId());
			setType(NodeControlPropertyType.Integer);
			setValue(String.valueOf(getP1ActivePowerSetpoint()));
		}

		private int getP1ActivePowerSetpoint() {
			final Integer value = sample.getP1ActivePowerSetpoint();
			return (value != null ? value.intValue() : 0);
		}
	}

	/* === NodeControlProvider === */

	@Override
	public List<String> getAvailableControlIds() {
		return (controlId == null ? Collections.emptyList() : Collections.singletonList(controlId));
	}

	@Override
	public NodeControlInfo getCurrentControlInfo(String controlId) {
		if ( this.controlId == null || !this.controlId.equals(controlId) ) {
			return null;
		}
		return readCurrentDatum();
	}

	private ControlDatum readCurrentDatum() {
		final long start = System.currentTimeMillis();
		final String controlId = getControlId();
		try {
			final Stabiliti30cData currSample = getCurrentSample();
			if ( currSample == null ) {
				return null;
			}
			ControlDatum d = new ControlDatum(currSample, controlId);
			d.setSourceId(controlId);
			if ( currSample.getDataTimestamp() >= start ) {
				// we read from the device
				postDatumCapturedEvent(d);
			}
			return d;
		} catch ( IOException e ) {
			log.error("Communication problem reading source {} from Stabiliti {}: {}", controlId,
					modbusDeviceName(), e.getMessage());
			return null;
		}
	}

	private void postDatumCapturedEvent(NodeControlInfoDatum info) {
		postControlEvent(info, EVENT_TOPIC_CONTROL_INFO_CAPTURED);
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

	/* === InstructionHandler === */

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SHED_LOAD.equals(topic);
	}

	@Override
	public InstructionState processInstruction(Instruction instruction) {
		String controlId = getControlId();
		if ( instruction == null || controlId == null
				|| !TOPIC_SHED_LOAD.equals(instruction.getTopic()) ) {
			return null;
		}
		Integer targetPower;
		try {
			targetPower = Integer.valueOf(instruction.getParameterValue(controlId));
		} catch ( NumberFormatException | NullPointerException e ) {
			log.warn("ShedLoad target missing or not a number: {}", e.getMessage());
			return null;
		}
		adjustAcExportPower(targetPower);
		return InstructionState.Completed;
	}

	private synchronized void adjustAcExportPower(final Integer targetPower) {
		final ModbusNetwork modbus = modbusNetwork();
		final int unitId = getUnitId();
		if ( modbus == null ) {
			log.warn("No ModbusNetwork available for AC export management task for Stabiliti {}",
					unitId);
			return;
		}
		final ControlDatum datum = readCurrentDatum();
		final int currSetpointValue = datum.getP1ActivePowerSetpoint();
		final int desiredSetpointValue = (targetPower != null ? targetPower.intValue() : 0);
		if ( currSetpointValue == desiredSetpointValue ) {
			log.info("Stabiliti {} AC export power setpoint already configured as {}, nothing to do",
					modbusDeviceName(), currSetpointValue);
		}
		log.info("Setting AC export power setpoint to {}s on Stabiliti {}", targetPower,
				modbusDeviceName());
		try {
			modbus.performAction(new ModbusConnectionAction<Void>() {

				@Override
				public Void doWithConnection(ModbusConnection connection) throws IOException {
					final Stabiliti30cData sample = getSample();
					sample.performUpdates(new ModbusDataUpdateAction() {

						@Override
						public boolean updateModbusData(MutableModbusData m) {
							setAcExportSettings(connection, sample, m, targetPower, true);
							return false;
						}

					});
					return null;
				}
			}, unitId);
		} catch ( Exception e ) {
			log.error("Exception adjusting AC export power to {} for Stabiliti {}: {}", targetPower,
					modbusDeviceName(), e.getMessage(), e);
		}

	}

	private void setAcExportSettings(ModbusConnection connection, Stabiliti30cData sample,
			MutableModbusData m, Integer targetPower, boolean manualModeEnabled) {
		Stabiliti30cControlAccessor acc = sample.controlAccessor(connection);
		if ( sample.getP2ControlMethod() != Stabiliti30cDcControlMethod.Net ) {
			acc.setP2ControlMethod(Stabiliti30cDcControlMethod.Net);
		}
		if ( sample.getP1ControlMethod() != Stabiliti30cAcControlMethod.GridPower ) {
			acc.setP1ControlMethod(Stabiliti30cAcControlMethod.GridPower);
		}
		if ( sample.getP3ControlMethod() != Stabiliti30cDcControlMethod.Mppt ) {
			acc.setP3ControlMethod(Stabiliti30cDcControlMethod.Mppt);
		}
		if ( targetPower != null && !targetPower.equals(sample.getP1ActivePowerSetpoint()) ) {
			acc.setP1ActivePowerSetpoint(targetPower);
		}
		// TODO: this needed? acc.setP3MpptStartTimeOffsetSetpoint(0);
		// TODO: this needed? acc.setP3MpptStopTimeOffsetSetpoint(1440);
		if ( manualModeEnabled != sample.isManualModeEnabled() ) {
			acc.setManualModeEnabled(manualModeEnabled);
		}

	}

	/* === SettingSpecifierProvider === */

	@Override
	public String getSettingUID() {
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

	private final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
			.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.FULL).withZone(ZoneId.systemDefault());

	private String getSampleMessage(Stabiliti30cDataAccessor sample) {
		if ( sample.getDataTimestamp() < 1 || controlId == null ) {
			return "N/A";
		}

		Map<String, Object> data = new LinkedHashMap<>(2);
		data.put("P1 Active Power", sample.getP1ActivePower());
		data.put("P1 Active Power Setpoint", sample.getP1ActivePowerSetpoint());

		if ( data.isEmpty() ) {
			return "N/A";
		}

		final String ts = DATE_FORMAT.format(Instant.ofEpochMilli(sample.getDataTimestamp()));

		StringBuilder buf = new StringBuilder();
		buf.append(StringUtils.delimitedStringFromMap(data));
		buf.append("; sampled at ").append(ts);
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
