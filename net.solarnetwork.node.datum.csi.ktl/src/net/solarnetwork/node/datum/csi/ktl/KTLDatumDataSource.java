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

import static net.solarnetwork.util.DateUtils.formatForLocalDisplay;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.node.domain.datum.AcDcEnergyDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.hw.csi.inverter.KTLCTData;
import net.solarnetwork.node.hw.csi.inverter.KTLCTDataAccessor;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.support.ModbusDataDatumDataSourceSupport;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;

/**
 * {@link DatumDataSource} implementation for {@link AcDcEnergyDatum} with the
 * CSI KTL inverter.
 *
 * @author matt
 * @author maxieduncan
 * @version 2.3
 */
public class KTLDatumDataSource extends ModbusDataDatumDataSourceSupport<KTLCTData>
		implements DatumDataSource, MultiDatumDataSource, SettingSpecifierProvider, InstructionHandler {

	/**
	 * The {@code sampleCacheMs} property default value.
	 *
	 * @since 2.2
	 */
	public static final long DEFAULT_SAMPLE_CACHE_MS = 5000L;

	private String sourceId;
	private boolean includePhaseMeasurements = false;

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
		setSampleCacheMs(DEFAULT_SAMPLE_CACHE_MS);
	}

	@Override
	public String deviceInfoSourceId() {
		return resolvePlaceholders(sourceId);
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
	public Class<? extends NodeDatum> getDatumType() {
		return AcDcEnergyDatum.class;
	}

	@Override
	public AcDcEnergyDatum readCurrentDatum() {
		final String sourceId = resolvePlaceholders(this.sourceId);
		try {
			final KTLCTData currSample = getCurrentSample();
			if ( currSample == null ) {
				return null;
			}
			KTLDatum d = new KTLDatum(currSample, sourceId);
			if ( this.includePhaseMeasurements ) {
				d.populatePhaseMeasurementProperties(currSample);
			}
			return d;
		} catch ( IOException e ) {
			log.error("Communication problem reading source {} from KTL device {}: {}", sourceId,
					modbusDeviceName(), e.getMessage());
			return null;
		}
	}

	@Override
	public Collection<String> publishedSourceIds() {
		final String sourceId = resolvePlaceholders(getSourceId());
		return (sourceId == null || sourceId.isEmpty() ? Collections.emptySet()
				: Collections.singleton(sourceId));
	}

	@Override
	public Class<? extends NodeDatum> getMultiDatumType() {
		return AcDcEnergyDatum.class;
	}

	@Override
	public Collection<NodeDatum> readMultipleDatum() {
		AcDcEnergyDatum datum = readCurrentDatum();
		if ( datum != null ) {
			return Collections.singletonList(datum);
		}
		return Collections.emptyList();
	}

	// InstructionHandler

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SET_OPERATING_STATE.equals(topic);
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
		final String topic = (instruction != null ? instruction.getTopic() : null);
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
				return InstructionUtils.createStatus(instruction, InstructionState.Completed);
			} catch ( Exception e ) {
				log.warn("Error processing {} instruction on inverter {}", topic, sourceId, e);
				Map<String, Object> resultParams = new LinkedHashMap<>();
				resultParams.put(InstructionStatus.ERROR_CODE_RESULT_PARAM, "KTL.001");
				resultParams.put(InstructionStatus.MESSAGE_RESULT_PARAM, e.toString());
				return InstructionUtils.createStatus(instruction, InstructionState.Declined,
						resultParams);
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
	public String getSettingUid() {
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

		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(DEFAULT_SAMPLE_CACHE_MS)));
		results.add(new BasicTextFieldSettingSpecifier("sourceId", null));
		results.add(new BasicToggleSettingSpecifier("includePhaseMeasurements", false));

		results.addAll(getDeviceInfoMetadataSettingSpecifiers());

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
		if ( data.getDataTimestamp() == null ) {
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
		buf.append("; sampled at ").append(formatForLocalDisplay(data.getDataTimestamp()));
		return buf.toString();
	}

	/**
	 * Get the source ID.
	 *
	 * @return the source ID
	 * @since 2.2
	 */
	public String getSourceId() {
		return sourceId;
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

	/**
	 * Get the inclusion toggle of phase measurement properties in collected
	 * datum.
	 *
	 * @return {@literal true} to collect phase measurements
	 * @since 2.2
	 */
	public boolean isIncludePhaseMeasurements() {
		return includePhaseMeasurements;
	}

	/**
	 * Toggle the inclusion of phase measurement properties in collected datum.
	 *
	 * @param includePhaseMeasurements
	 *        {@literal true} to collect phase measurements
	 * @since 2.1
	 */
	public void setIncludePhaseMeasurements(boolean includePhaseMeasurements) {
		this.includePhaseMeasurements = includePhaseMeasurements;
	}

}
