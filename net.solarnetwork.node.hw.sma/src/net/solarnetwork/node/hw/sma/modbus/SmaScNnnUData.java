/* ==================================================================
 * SmaScNnnUData.java - 14/09/2020 2:50:18 PM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sma.modbus;

import static net.solarnetwork.domain.datum.DatumSamplesType.Accumulating;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.MutableDatumSamplesOperations;
import net.solarnetwork.node.domain.datum.AcEnergyDatum;
import net.solarnetwork.node.hw.sma.domain.SmaCodedValue;
import net.solarnetwork.node.hw.sma.domain.SmaCommonStatusCode;
import net.solarnetwork.node.hw.sma.domain.SmaDeviceKind;
import net.solarnetwork.node.hw.sma.domain.SmaScNnnUDataAccessor;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * {@link SmaDeviceData} for Sunny Central nnnU devices.
 * 
 * @author matt
 * @version 2.0
 */
public class SmaScNnnUData extends SmaCommonDeviceData implements SmaScNnnUDataAccessor {

	/**
	 * Constructor.
	 * 
	 * @param deviceKind
	 *        the device kind
	 */
	public SmaScNnnUData(SmaDeviceKind deviceKind) {
		super(deviceKind);
	}

	/**
	 * Constructor.
	 * 
	 * @param deviceKind
	 *        the device kind
	 * @param data
	 *        some initial data to use
	 * @param addr
	 *        the starting Modbus register address of {@code data}
	 */
	public SmaScNnnUData(SmaDeviceKind deviceKind, short[] data, int addr) {
		super(deviceKind, data, addr);
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the meter data to copy
	 * @param deviceKind
	 *        the device kind
	 */
	public SmaScNnnUData(ModbusData other, SmaDeviceKind deviceKind) {
		super(other, deviceKind);
	}

	@Override
	public SmaScNnnUData copy() {
		return new SmaScNnnUData(this, getDeviceKind());
	}

	@Override
	public void populateDatumSamples(MutableDatumSamplesOperations samples, Map<String, ?> parameters) {
		super.populateDatumSamples(samples, parameters);

		samples.putSampleValue(Instantaneous, "gridReconnectTime", getGridReconnectTime());

		SmaCodedValue state = getOperatingState();
		samples.putSampleValue(Status, Datum.OP_STATES, codedValueCode(state));

		state = getRecommendedAction();
		samples.putSampleValue(Status, "recommendedAction", codedValueCode(state));

		state = getGridContactorStatus();
		samples.putSampleValue(Status, "gridContactorStatus", codedValueCode(state));

		state = getError();
		samples.putSampleValue(Status, "error", codedValueCode(state));

		samples.putSampleValue(Status, "smaError", getSmaError());

		state = getDcSwitchStatus();
		samples.putSampleValue(Status, "dcSwitch", codedValueCode(state));

		state = getAcSwitchStatus();
		samples.putSampleValue(Status, "acSwitch", codedValueCode(state));

		state = getAcSwitchDisconnectorStatus();
		samples.putSampleValue(Status, "acSwitchDisconnector", codedValueCode(state));

		samples.putSampleValue(Instantaneous, AcPhase.PhaseA.withKey(AcEnergyDatum.CURRENT_KEY),
				getGridCurrentLine1());
		samples.putSampleValue(Instantaneous, AcPhase.PhaseB.withKey(AcEnergyDatum.CURRENT_KEY),
				getGridCurrentLine2());
		samples.putSampleValue(Instantaneous, AcPhase.PhaseC.withKey(AcEnergyDatum.CURRENT_KEY),
				getGridCurrentLine3());

		state = getActivePowerLimitStatus();
		samples.putSampleValue(Status, "activePowerLimitStatus", codedValueCode(state));

		samples.putSampleValue(Instantaneous, "activePowerLimit", getActivePowerTarget());

		samples.putSampleValue(Instantaneous, AcEnergyDatum.VOLTAGE_KEY, getVoltage());

		samples.putSampleValue(Accumulating, "fanCabinet2OpTime", getCabinetFan2OperatingTime());
		samples.putSampleValue(Accumulating, "fanHeatSinkOpTime", getHeatSinkFanOperatingTime());

		DeviceOperatingState dos = getDeviceOperatingState();
		if ( dos != null ) {
			samples.putSampleValue(Status, Datum.OP_STATE, dos.getCode());
		}
	}

	@Override
	public final void readInformationData(final ModbusConnection conn) throws IOException {
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				SmaScNnnURegister.INFO_REGISTER_ADDRESS_SET, MAX_RESULTS);
	}

	@Override
	public final void readDeviceData(final ModbusConnection conn) throws IOException {
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				SmaScNnnURegister.DATA_REGISTER_ADDRESS_SET, MAX_RESULTS);
	}

	@Override
	public DeviceOperatingState getDeviceOperatingState() {
		SmaCodedValue c = getOperatingState();
		if ( c instanceof SmaCommonStatusCode ) {
			switch ((SmaCommonStatusCode) c) {
				case Operation:
					return DeviceOperatingState.Normal;

				case Warning:
				case Error:
					return DeviceOperatingState.Fault;

				case Disruption:
					return DeviceOperatingState.Recovery;

				default:
					// nothing to do
			}
		}
		return DeviceOperatingState.Unknown;
	}

	@Override
	public SmaCodedValue getOperatingState() {
		return getStatusCode(SmaScNnnURegister.OperatingState, null);
	}

	@Override
	public Long getGridReconnectTime() {
		Number n = filterNotNumber(getNumber(SmaScNnnURegister.GridConnectTimeRemaining),
				SmaScNnnURegister.GridConnectTimeRemaining);
		return (n instanceof Long ? (Long) n : n != null ? n.longValue() : null);
	}

	@Override
	public SmaCodedValue getRecommendedAction() {
		return getStatusCode(SmaScNnnURegister.RecommendedAction, null, SmaCommonStatusCode.Invalid);
	}

	@Override
	public SmaCodedValue getGridContactorStatus() {
		return getStatusCode(SmaScNnnURegister.GridContactorStatus, null);
	}

	@Override
	public SmaCodedValue getError() {
		return getStatusCode(SmaScNnnURegister.ErrorState, null, SmaCommonStatusCode.NotSet);
	}

	@Override
	public Long getSmaError() {
		Number n = filterNotNumber(getNumber(SmaScNnnURegister.SmaErrorId),
				SmaScNnnURegister.SmaErrorId);
		return (n instanceof Long ? (Long) n : n != null ? n.longValue() : null);
	}

	@Override
	public SmaCodedValue getDcSwitchStatus() {
		return getStatusCode(SmaScNnnURegister.DcSwitchState, null);
	}

	@Override
	public SmaCodedValue getAcSwitchStatus() {
		return getStatusCode(SmaScNnnURegister.AcSwitchState, null);
	}

	@Override
	public SmaCodedValue getAcSwitchDisconnectorStatus() {
		return getStatusCode(SmaScNnnURegister.AcSwitchDisconnectState, null);
	}

	@Override
	public Float getGridCurrentLine1() {
		return getCurrentValue(SmaScNnnURegister.GridCurrentLine1);
	}

	@Override
	public Float getGridCurrentLine2() {
		return getCurrentValue(SmaScNnnURegister.GridCurrentLine2);
	}

	@Override
	public Float getGridCurrentLine3() {
		return getCurrentValue(SmaScNnnURegister.GridCurrentLine3);
	}

	@Override
	public SmaCodedValue getActivePowerLimitStatus() {
		return getStatusCode(SmaScNnnURegister.ActivePowerLimitationOperatingMode, null);
	}

	@Override
	public Integer getActivePowerTargetPercent() {
		Number n = filterNotNumber(getNumber(SmaScNnnURegister.ActivePowerTargetPercent),
				SmaScNnnURegister.ActivePowerTargetPercent);
		return (n instanceof Integer ? (Integer) n : n != null ? n.intValue() : null);
	}

	@Override
	public Float getVoltage() {
		BigDecimal d = getFixedScaleValue(SmaScNnnURegister.AcVoltageTotal, 2);
		return (d != null ? d.floatValue() : null);
	}

	@Override
	public BigInteger getCabinetFan2OperatingTime() {
		Number n = getNumber(SmaScNnnURegister.Fan2OperatingTime);
		return (n instanceof BigInteger ? (BigInteger) n
				: n != null ? new BigInteger(n.toString()) : null);
	}

	@Override
	public BigInteger getHeatSinkFanOperatingTime() {
		Number n = getNumber(SmaScNnnURegister.HeatSinkFanOperatingTime);
		return (n instanceof BigInteger ? (BigInteger) n
				: n != null ? new BigInteger(n.toString()) : null);
	}

	@Override
	public BigDecimal getTemperatureCabinet2() {
		return getTemperatureValue(SmaScNnnURegister.InteriorTemperature2);
	}

	@Override
	public BigDecimal getTemperatureTransformer() {
		return getTemperatureValue(SmaScNnnURegister.TransformerTemperature);
	}

}
