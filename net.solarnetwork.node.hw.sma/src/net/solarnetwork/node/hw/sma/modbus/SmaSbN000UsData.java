/* ==================================================================
 * SmaSbN000UsData.java - 17/09/2020 10:14:16 AM
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
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Map;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.domain.MutableGeneralDatumSamplesOperations;
import net.solarnetwork.node.domain.ACEnergyDatum;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.EnergyDatum;
import net.solarnetwork.node.domain.PVEnergyDatum;
import net.solarnetwork.node.hw.sma.domain.GenericSmaCodedValue;
import net.solarnetwork.node.hw.sma.domain.SmaCodedValue;
import net.solarnetwork.node.hw.sma.domain.SmaCommonStatusCode;
import net.solarnetwork.node.hw.sma.domain.SmaDeviceKind;
import net.solarnetwork.node.hw.sma.domain.SmaSbN000UsDataAccessor;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.util.NumberUtils;

/**
 * {@link SmaDeviceData} for Sunny Boy n000 US devices.
 * 
 * @author matt
 * @version 1.0
 */
public class SmaSbN000UsData extends SmaDeviceData implements SmaSbN000UsDataAccessor {

	/**
	 * Constructor.
	 * 
	 * @param deviceKind
	 *        the device kind
	 */
	public SmaSbN000UsData(SmaDeviceKind deviceKind) {
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
	public SmaSbN000UsData(SmaDeviceKind deviceKind, short[] data, int addr) {
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
	public SmaSbN000UsData(ModbusData other, SmaDeviceKind deviceKind) {
		super(other, deviceKind);
	}

	@Override
	public SmaSbN000UsData copy() {
		return new SmaSbN000UsData(this, getDeviceKind());
	}

	@Override
	public void populateDatumSamples(MutableGeneralDatumSamplesOperations samples,
			Map<String, ?> parameters) {
		samples.putSampleValue(Status, "powerLimit", getActivePowerMaximum());
		samples.putSampleValue(Status, "powerMax", getActivePowerPermanentLimit());
		samples.putSampleValue(Status, "error", codedValueCode(getError()));
		samples.putSampleValue(Status, "backupState", codedValueCode(getBackupMode()));
		samples.putSampleValue(Status, "gridType", codedValueCode(getGridType()));
		samples.putSampleValue(Status, "powerBalanceMode",
				codedValueCode(getPowerBalancerOperatingMode()));
		samples.putSampleValue(Status, Datum.OP_STATES, codedValueCode(getOperatingState()));

		samples.putSampleValue(Accumulating, EnergyDatum.WATT_HOUR_READING_KEY,
				getActiveEnergyExported());
		samples.putSampleValue(Accumulating, "opTime", getOperatingTime());
		samples.putSampleValue(Accumulating, "feedInTime", getFeedInTime());

		samples.putSampleValue(Instantaneous, PVEnergyDatum.DC_VOLTAGE_KEY, getDcVoltage());
		samples.putSampleValue(Instantaneous, PVEnergyDatum.DC_POWER_KEY, getDcPower());
		samples.putSampleValue(Instantaneous, EnergyDatum.WATTS_KEY, getActivePower());
		samples.putSampleValue(Instantaneous, ACPhase.PhaseA.withKey(ACEnergyDatum.VOLTAGE_KEY),
				getVoltageLine1Neutral());
		samples.putSampleValue(Instantaneous, ACPhase.PhaseB.withKey(ACEnergyDatum.VOLTAGE_KEY),
				getVoltageLine2Neutral());
		samples.putSampleValue(Instantaneous, ACEnergyDatum.CURRENT_KEY, getCurrent());
		samples.putSampleValue(Instantaneous, ACEnergyDatum.FREQUENCY_KEY, getFrequency());
	}

	@Override
	public String getDataDescription() {
		StringBuilder buf = new StringBuilder();
		buf.append(getDeviceOperatingState());
		buf.append("; W = ").append(getActivePower());
		buf.append("; Wh = ").append(getActiveEnergyExported());
		buf.append("; dcVoltage = ").append(getDcVoltage());
		buf.append("; dcCurrent = ").append(getDcCurrent());
		return buf.toString();
	}

	@Override
	public final void readInformationData(final ModbusConnection conn) throws IOException {
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				SmaSbN000UsRegister.INFO_REGISTER_ADDRESS_SET, MAX_RESULTS);
	}

	@Override
	public final void readDeviceData(final ModbusConnection conn) throws IOException {
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				SmaSbN000UsRegister.DATA_REGISTER_ADDRESS_SET, MAX_RESULTS);
	}

	@Override
	public DeviceOperatingState getDeviceOperatingState() {
		SmaCodedValue c = getOperatingState();
		if ( c instanceof SmaCommonStatusCode ) {
			switch ((SmaCommonStatusCode) c) {
				case ConstantVoltage:
				case Mpp:
				case Operation:
				case PowerSpecificationViaCurve:
					return DeviceOperatingState.Normal;

				case TemperatureDerating:
				case PowerBalancing:
					return DeviceOperatingState.Override;

				case Starting:
				case Waiting:
					return DeviceOperatingState.Starting;

				case Stop:
					return DeviceOperatingState.Shutdown;

				case Warning:
				case Error:
					return DeviceOperatingState.Fault;

				case MppSearch:
				case Disruption:
					return DeviceOperatingState.Recovery;

				default:
					// nothing to do
			}
		}
		return DeviceOperatingState.Unknown;
	}

	@Override
	public Long getDeviceClass() {
		Number n = filterNotNumber(getNumber(SmaSbN000UsRegister.MainModel),
				SmaSbN000UsRegister.MainModel);
		return (n instanceof Long ? (Long) n : n != null ? n.longValue() : null);
	}

	@Override
	public SmaCodedValue getError() {
		Number n = filterNotNumber(getNumber(SmaSbN000UsRegister.ErrorStatus),
				SmaSbN000UsRegister.ErrorStatus);
		return (n != null ? new GenericSmaCodedValue(n.intValue()) : null);
	}

	@Override
	public Integer getActivePowerMaximum() {
		return getPowerValue(SmaSbN000UsRegister.MaximumActivePower);
	}

	@Override
	public Integer getActivePowerPermanentLimit() {
		return getPowerValue(SmaSbN000UsRegister.MaximumActivePowerLimit);
	}

	@Override
	public SmaCodedValue getBackupMode() {
		return getStatusCode(SmaSbN000UsRegister.BackupMode, null);
	}

	@Override
	public SmaCodedValue getGridType() {
		Number n = filterNotNumber(getNumber(SmaSbN000UsRegister.GridType),
				SmaSbN000UsRegister.GridType);
		return (n != null ? new GenericSmaCodedValue(n.intValue()) : null);
	}

	@Override
	public SmaCodedValue getPowerBalancerOperatingMode() {
		return getStatusCode(SmaSbN000UsRegister.PowerBalanceOperatingMode, null);
	}

	@Override
	public SmaCodedValue getOperatingState() {
		return getStatusCode(SmaSbN000UsRegister.OperatingState, null);
	}

	@Override
	public Long getActiveEnergyExported() {
		return getEnergyValue(SmaSbN000UsRegister.TotalYield);
	}

	@Override
	public BigInteger getOperatingTime() {
		Number n = getNumber(SmaSbN000UsRegister.OperatingTime);
		return (n instanceof BigInteger ? (BigInteger) n
				: n != null ? new BigInteger(n.toString()) : null);
	}

	@Override
	public BigInteger getFeedInTime() {
		Number n = getNumber(SmaSbN000UsRegister.FeedInTime);
		return (n instanceof BigInteger ? (BigInteger) n
				: n != null ? new BigInteger(n.toString()) : null);
	}

	@Override
	public Float getDcCurrent() {
		return getCurrentValue(SmaSbN000UsRegister.DcCurrentInput);
	}

	@Override
	public Float getDcVoltage() {
		return getVoltageValue(SmaSbN000UsRegister.DcVoltageInput);
	}

	public Integer getDcPower() {
		BigDecimal v = NumberUtils.bigDecimalForNumber(
				filterNotNumber(getFixedScaleValue(SmaSbN000UsRegister.DcVoltageInput, 2),
						SmaSbN000UsRegister.DcCurrentInput));
		BigDecimal i = NumberUtils.bigDecimalForNumber(
				filterNotNumber(getFixedScaleValue(SmaSbN000UsRegister.DcCurrentInput, 3),
						SmaSbN000UsRegister.DcCurrentInput));
		if ( v != null && i != null ) {
			return v.multiply(i).setScale(0, RoundingMode.HALF_UP).intValue();
		}
		return null;
	}

	@Override
	public Integer getActivePower() {
		return getPowerValue(SmaSbN000UsRegister.ActivePowerTotal);
	}

	@Override
	public Float getVoltageLine1Neutral() {
		return getVoltageValue(SmaSbN000UsRegister.GridVoltageLine1Neutral);
	}

	@Override
	public Float getVoltageLine2Neutral() {
		return getVoltageValue(SmaSbN000UsRegister.GridVoltageLine2Neutral);
	}

	@Override
	public Float getCurrent() {
		return getCurrentValue(SmaSbN000UsRegister.GridCurrent);
	}

	@Override
	public Float getFrequency() {
		BigDecimal d = getFixedScaleValue(SmaSbN000UsRegister.Frequency, 2);
		return (d != null ? d.floatValue() : null);
	}

}
