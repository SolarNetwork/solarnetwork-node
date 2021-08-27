/* ==================================================================
 * KTLDatumDataSource.java - 23/11/2017 3:06:48 pm
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.csi.ktl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.MultiDatumDataSource;
import net.solarnetwork.node.domain.GeneralNodeACEnergyDatum;
import net.solarnetwork.node.domain.GeneralNodePVEnergyDatum;
import net.solarnetwork.node.hw.csi.inverter.KTLCTData;
import net.solarnetwork.node.hw.csi.inverter.KTLCTDataAccessor;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.support.ModbusDataDatumDataSourceSupport;
import net.solarnetwork.node.reactor.FeedbackInstructionHandler;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionStatus.InstructionState;
import net.solarnetwork.node.reactor.support.BasicInstructionStatus;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;

/**
 * {@link DatumDataSource} implementation for {@link GeneralNodeACEnergyDatum}
 * with the CSI KTL inverter.
 * 
 * @author matt
 * @author maxieduncan
 * @version 1.4
 */
public class KTLDatumDataSource extends ModbusDataDatumDataSourceSupport<KTLCTData> implements
		DatumDataSource<GeneralNodePVEnergyDatum>, MultiDatumDataSource<GeneralNodePVEnergyDatum>,
		SettingSpecifierProvider, FeedbackInstructionHandler {

	private String sourceId = "CSI";

	/**
	 * Default constructor.
	 */
	public KTLDatumDataSource() {
		this(new KTLCTData());
	}

	/**
	 * Construct with a specific sample data instance.
	 * 
	 * @param sample
	 *        the sample data to use
	 */
	public KTLDatumDataSource(KTLCTData sample) {
		super(sample);
	}

	@Override
	protected void refreshDeviceInfo(ModbusConnection connection, KTLCTData sample) throws IOException {
		sample.readConfigurationData(connection);
	}

	@Override
	protected void refreshDeviceData(ModbusConnection connection, KTLCTData sample) throws IOException {
		sample.readInverterData(connection);
		sample.readControlData(connection);
	}

	@Override
	public Class<? extends GeneralNodePVEnergyDatum> getDatumType() {
		return KTLDatum.class;
	}

	@Override
	public GeneralNodePVEnergyDatum readCurrentDatum() {
		try {
			final KTLCTData currSample = getCurrentSample();
			if ( currSample == null ) {
				return null;
			}
			KTLDatum d = new KTLDatum(currSample);
			d.setSourceId(resolvePlaceholders(sourceId));
			return d;
		} catch ( IOException e ) {
			log.error("Communication problem reading source {} from KTL device {}: {}", this.sourceId,
					modbusDeviceName(), e.getMessage());
			return null;
		}
	}

	@Override
	public Class<? extends GeneralNodePVEnergyDatum> getMultiDatumType() {
		return KTLDatum.class;
	}

	@Override
	public Collection<GeneralNodePVEnergyDatum> readMultipleDatum() {
		GeneralNodePVEnergyDatum datum = readCurrentDatum();
		if ( datum != null ) {
			return Collections.singletonList(datum);
		}
		return Collections.emptyList();
	}

	// FeedbackInstructionHandler

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SET_OPERATING_STATE.equals(topic);
	}

	@Override
	public InstructionState processInstruction(Instruction instruction) {
		InstructionStatus status = processInstructionWithFeedback(instruction);
		return (status != null ? status.getInstructionState() : InstructionState.Declined);
	}

	@Override
	public InstructionStatus processInstructionWithFeedback(Instruction instruction) {
		final String topic = (instruction != null ? instruction.getTopic() : null);
		final InstructionStatus status = (instruction != null ? instruction.getStatus() : null);
		final String sourceId = this.sourceId;
		if ( InstructionHandler.TOPIC_SET_OPERATING_STATE.equals(topic) && sourceId != null ) {
			String paramVal = instruction.getParameterValue(this.sourceId);
			if ( paramVal == null ) {
				// not intended for me
				return null;
			}

			try {
				DeviceOperatingState desiredState = null;
				try {
					desiredState = DeviceOperatingState.forCode(Integer.parseInt(paramVal));
				} catch ( NumberFormatException e ) {
					desiredState = DeviceOperatingState.valueOf(paramVal);
				}
				log.info("Processing {} instruction on inverter {} to set operating state to {}", topic,
						this.sourceId, desiredState);
				setDeviceOperatingState(desiredState);
				return (status != null ? status.newCopyWithState(InstructionState.Completed)
						: new BasicInstructionStatus(instruction.getId(), InstructionState.Completed,
								new Date()));
			} catch ( Exception e ) {
				log.warn("Error processing {} instruction on inverter {}", topic, sourceId, e);
				Map<String, Object> resultParams = new LinkedHashMap<>();
				resultParams.put(InstructionStatus.ERROR_CODE_RESULT_PARAM, "KTL.001");
				resultParams.put(InstructionStatus.MESSAGE_RESULT_PARAM, e.toString());
				return (status != null ? status.newCopyWithState(InstructionState.Declined, resultParams)
						: new BasicInstructionStatus(instruction.getId(), InstructionState.Declined,
								new Date(), null, resultParams));
			}
		}
		return null;
	}

	private void setDeviceOperatingState(final DeviceOperatingState desiredState) throws IOException {
		performAction(new ModbusConnectionAction<Void>() {

			@Override
			public Void doWithConnection(ModbusConnection conn) throws IOException {
				KTLCTData sample = getSample();
				// re-read the control data to make sure we have the current state of the inverter
				sample.readControlData(conn);

				// now request the state to our desired state
				sample.setDeviceOperatingState(conn, desiredState);
				return null;
			}
		});

	}

	// SettingSpecifierProvider

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.datum.csi.ktl";
	}

	@Override
	public String getDisplayName() {
		return "CSI KTL Inverter";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(12);
		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(), true));
		results.add(new BasicTitleSettingSpecifier("sample", getSampleMessage(getSample()), true));

		results.addAll(getIdentifiableSettingSpecifiers());
		results.addAll(getModbusNetworkSettingSpecifiers());

		KTLDatumDataSource defaults = new KTLDatumDataSource();
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(defaults.getSampleCacheMs())));
		results.add(new BasicTextFieldSettingSpecifier("sourceId", defaults.sourceId));

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

	private String getSampleMessage(KTLCTDataAccessor data) {
		if ( data.getDataTimestamp() < 1 ) {
			return "N/A";
		}
		StringBuilder buf = new StringBuilder();
		buf.append("mode = ").append(data.getWorkMode());
		buf.append(", Hz = ").append(data.getFrequency());
		buf.append(", PV1 V = ").append(data.getPv1Voltage());
		buf.append(", PV2 V = ").append(data.getPv2Voltage());
		buf.append(", PV3 V = ").append(data.getPv3Voltage());
		buf.append(", W = ").append(data.getActivePower());
		buf.append(", Wh today = ").append(data.getActiveEnergyDeliveredToday());
		buf.append(", Wh total = ").append(data.getActiveEnergyDelivered());
		buf.append("; sampled at ")
				.append(DateTimeFormat.forStyle("LS").print(new DateTime(data.getDataTimestamp())));
		return buf.toString();
	}

	/**
	 * Set the source ID to use for returned datum.
	 * 
	 * @param sourceId
	 *        the source ID to use; defaults to {@literal modbus}
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

}
