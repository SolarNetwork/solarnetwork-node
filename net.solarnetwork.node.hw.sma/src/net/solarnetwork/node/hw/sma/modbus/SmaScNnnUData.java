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

import static net.solarnetwork.domain.GeneralDatumSamplesType.Accumulating;
import static net.solarnetwork.domain.GeneralDatumSamplesType.Instantaneous;
import static net.solarnetwork.domain.GeneralDatumSamplesType.Status;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.domain.MutableGeneralDatumSamplesOperations;
import net.solarnetwork.node.domain.ACEnergyDatum;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.domain.Datum;
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
 * @version 1.0
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
	public void populateDatumSamples(MutableGeneralDatumSamplesOperations samples,
			Map<String, ?> parameters) {
		super.populateDatumSamples(samples, parameters);

		samples.putSampleValue(Instantaneous, "gridReconnectTime", getGridReconnectTime());

		SmaCommonStatusCode state = getOperatingState();
		if ( state != null ) {
			samples.putSampleValue(Status, Datum.OP_STATES, state.getCode());
		}

		state = getRecommendedAction();
		if ( state != null ) {
			samples.putSampleValue(Status, "recommendedAction", state.getCode());
		}

		state = getGridContactorStatus();
		if ( state != null ) {
			samples.putSampleValue(Status, "gridContactorStatus", state.getCode());
		}

		state = getError();
		if ( state != null ) {
			samples.putSampleValue(Status, "error", state.getCode());
		}

		samples.putSampleValue(Status, "smaError", getSmaError());

		state = getDcSwitchStatus();
		if ( state != null ) {
			samples.putSampleValue(Status, "dcSwitch", state.getCode());
		}

		state = getAcSwitchStatus();
		if ( state != null ) {
			samples.putSampleValue(Status, "acSwitch", state.getCode());
		}

		state = getAcSwitchDisconnectorStatus();
		if ( state != null ) {
			samples.putSampleValue(Status, "acSwitchDisconnector", state.getCode());
		}

		samples.putSampleValue(Instantaneous, ACPhase.PhaseA.withKey(ACEnergyDatum.CURRENT_KEY),
				getGridCurrentLine1());
		samples.putSampleValue(Instantaneous, ACPhase.PhaseB.withKey(ACEnergyDatum.CURRENT_KEY),
				getGridCurrentLine2());
		samples.putSampleValue(Instantaneous, ACPhase.PhaseC.withKey(ACEnergyDatum.CURRENT_KEY),
				getGridCurrentLine3());

		state = getActivePowerLimitStatus();
		if ( state != null ) {
			samples.putSampleValue(Status, "activePowerLimitStatus", state.getCode());
		}

		samples.putSampleValue(Instantaneous, "activePowerLimit", getActivePowerTarget());

		samples.putSampleValue(Instantaneous, ACEnergyDatum.VOLTAGE_KEY, getVoltage());

		samples.putSampleValue(Accumulating, "fanCabinet2OpTime", getCabinetFan2OperatingTime());
		samples.putSampleValue(Accumulating, "fanHeatSinkOpTime", getHeatSinkFanOperatingTime());

		DeviceOperatingState dos = getDeviceOperatingState();
		if ( dos != null ) {
			samples.putSampleValue(Status, Datum.OP_STATE, dos.getCode());
		}
	}

	/**
	 * Read the informational registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 */
	@Override
	public final void readInformationData(final ModbusConnection conn) {
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				SmaScNnnURegister.INFO_REGISTER_ADDRESS_SET, MAX_RESULTS);
	}

	/**
	 * Read the registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 */
	@Override
	public final void readDeviceData(final ModbusConnection conn) {
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				SmaScNnnURegister.DATA_REGISTER_ADDRESS_SET, MAX_RESULTS);
	}

	@Override
	public DeviceOperatingState getDeviceOperatingState() {
		SmaCommonStatusCode c = getOperatingState();
		if ( c != null ) {
			switch (c) {
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
	public SmaCommonStatusCode getOperatingState() {
		return getStatusCode(SmaScNnnURegister.OperatingState);
	}

	@Override
	public Long getGridReconnectTime() {
		Number n = filterNotNumber(getNumber(SmaScNnnURegister.GridConnectTimeRemaining),
				SmaScNnnURegister.GridConnectTimeRemaining);
		return (n instanceof Long ? (Long) n : n != null ? n.longValue() : null);
	}

	@Override
	public SmaCommonStatusCode getRecommendedAction() {
		return getStatusCode(SmaScNnnURegister.RecommendedAction, null, SmaCommonStatusCode.Invalid);
	}

	@Override
	public SmaCommonStatusCode getGridContactorStatus() {
		return getStatusCode(SmaScNnnURegister.GridContactorStatus);
	}

	@Override
	public SmaCommonStatusCode getError() {
		return getStatusCode(SmaScNnnURegister.ErrorState, null, SmaCommonStatusCode.NotSet);
	}

	@Override
	public Long getSmaError() {
		Number n = filterNotNumber(getNumber(SmaScNnnURegister.SmaErrorId),
				SmaScNnnURegister.SmaErrorId);
		return (n instanceof Long ? (Long) n : n != null ? n.longValue() : null);
	}

	@Override
	public SmaCommonStatusCode getDcSwitchStatus() {
		return getStatusCode(SmaScNnnURegister.DcSwitchState);
	}

	@Override
	public SmaCommonStatusCode getAcSwitchStatus() {
		return getStatusCode(SmaScNnnURegister.AcSwitchState);
	}

	@Override
	public SmaCommonStatusCode getAcSwitchDisconnectorStatus() {
		return getStatusCode(SmaScNnnURegister.AcSwitchDisconnectState);
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
	public SmaCommonStatusCode getActivePowerLimitStatus() {
		return getStatusCode(SmaScNnnURegister.ActivePowerLimitationOperatingMode);
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
