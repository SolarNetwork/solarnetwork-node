/* ==================================================================
 * SmaCommonDeviceData.java - 15/09/2020 11:33:10 AM
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
import net.solarnetwork.node.domain.DataAccessor;
import net.solarnetwork.node.domain.EnergyDatum;
import net.solarnetwork.node.domain.PVEnergyDatum;
import net.solarnetwork.node.hw.sma.domain.SmaDeviceCommonDataAccessor;
import net.solarnetwork.node.hw.sma.domain.SmaDeviceKind;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.util.NumberUtils;

/**
 * {@link DataAccessor} for SMA common devices.
 * 
 * @author matt
 * @version 1.0
 */
public class SmaCommonDeviceData extends SmaDeviceData implements SmaDeviceCommonDataAccessor {

	private final SmaDeviceKind deviceKind;

	/**
	 * Constructor.
	 * 
	 * @param deviceKind
	 *        the device kind
	 */
	public SmaCommonDeviceData(SmaDeviceKind deviceKind) {
		super();
		this.deviceKind = deviceKind;
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
	public SmaCommonDeviceData(SmaDeviceKind deviceKind, short[] data, int addr) {
		super(data, addr);
		this.deviceKind = deviceKind;
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the data to copy
	 * @param deviceKind
	 *        the device kind
	 */
	public SmaCommonDeviceData(ModbusData other, SmaDeviceKind deviceKind) {
		super(other);
		this.deviceKind = deviceKind;
	}

	@Override
	public SmaScNnnUData copy() {
		return new SmaScNnnUData(this, deviceKind);
	}

	@Override
	public SmaDeviceKind getDeviceKind() {
		return deviceKind;
	}

	@Override
	public DeviceOperatingState getDeviceOperatingState() {
		return null;
	}

	@Override
	public void populateDatumSamples(MutableGeneralDatumSamplesOperations samples,
			Map<String, ?> parameters) {
		samples.putSampleValue(Status, "eventId", getEventId());
		samples.putSampleValue(Status, "powerLimit", getActivePowerMaximum());
		samples.putSampleValue(Status, "powerMax", getActivePowerPermanentLimit());

		samples.putSampleValue(Accumulating, EnergyDatum.WATT_HOUR_READING_KEY,
				getActiveEnergyExported());
		samples.putSampleValue(Accumulating, "opTime", getOperatingTime());
		samples.putSampleValue(Accumulating, "feedInTime", getFeedInTime());

		samples.putSampleValue(Instantaneous, PVEnergyDatum.DC_VOLTAGE_KEY, getDcVoltage());
		samples.putSampleValue(Instantaneous, PVEnergyDatum.DC_POWER_KEY, getDcPower());
		samples.putSampleValue(Instantaneous, EnergyDatum.WATTS_KEY, getActivePower());
		samples.putSampleValue(Instantaneous, ACPhase.PhaseA.withLineKey(ACEnergyDatum.VOLTAGE_KEY),
				getLineVoltageLine1Line2());
		samples.putSampleValue(Instantaneous, ACPhase.PhaseB.withLineKey(ACEnergyDatum.VOLTAGE_KEY),
				getLineVoltageLine2Line3());
		samples.putSampleValue(Instantaneous, ACPhase.PhaseC.withLineKey(ACEnergyDatum.VOLTAGE_KEY),
				getLineVoltageLine3Line1());
		samples.putSampleValue(Instantaneous, ACEnergyDatum.CURRENT_KEY, getCurrent());
		samples.putSampleValue(Instantaneous, ACEnergyDatum.FREQUENCY_KEY, getFrequency());
		samples.putSampleValue(Instantaneous, ACEnergyDatum.REACTIVE_POWER_KEY, getReactivePower());
		samples.putSampleValue(Instantaneous, ACEnergyDatum.APPARENT_POWER_KEY, getApparentPower());
		samples.putSampleValue(Instantaneous, "activePowerTarget", getActivePowerTarget());
		samples.putSampleValue(Instantaneous, "tempHeatSink", getHeatSinkTemperature());
		samples.putSampleValue(Instantaneous, "tempCabinet", getCabinetTemperature());
		samples.putSampleValue(Instantaneous, "tempAmbient", getExternalTemperature());
	}

	@Override
	public void readInformationData(ModbusConnection conn) throws IOException {
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				SmaCommonDeviceRegister.INFO_REGISTER_ADDRESS_SET, MAX_RESULTS);
	}

	@Override
	public void readDeviceData(ModbusConnection conn) throws IOException {
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				SmaCommonDeviceRegister.DATA_REGISTER_ADDRESS_SET, MAX_RESULTS);
	}

	@Override
	public Long getEventId() {
		Number n = filterNotNumber(getNumber(SmaCommonDeviceRegister.Event),
				SmaScNnnURegister.GridConnectTimeRemaining);
		return (n instanceof Long ? (Long) n : n != null ? n.longValue() : null);
	}

	@Override
	public Integer getActivePowerMaximum() {
		return getPowerValue(SmaCommonDeviceRegister.MaximumActivePower);
	}

	@Override
	public Integer getActivePowerPermanentLimit() {
		return getPowerValue(SmaCommonDeviceRegister.MaximumActivePowerLimit);
	}

	@Override
	public Long getActiveEnergyExported() {
		return getEnergyValue(SmaCommonDeviceRegister.TotalYield);
	}

	@Override
	public BigInteger getOperatingTime() {
		Number n = getNumber(SmaCommonDeviceRegister.OperatingTime);
		return (n instanceof BigInteger ? (BigInteger) n
				: n != null ? new BigInteger(n.toString()) : null);
	}

	@Override
	public BigInteger getFeedInTime() {
		Number n = getNumber(SmaCommonDeviceRegister.FeedInTime);
		return (n instanceof BigInteger ? (BigInteger) n
				: n != null ? new BigInteger(n.toString()) : null);
	}

	@Override
	public Float getDcCurrent() {
		return getCurrentValue(SmaCommonDeviceRegister.DcCurrentInput);
	}

	@Override
	public Float getDcVoltage() {
		return getVoltageValue(SmaCommonDeviceRegister.DcVoltageInput);
	}

	@Override
	public Integer getDcPower() {
		return getPowerValue(SmaCommonDeviceRegister.DcPowerInput);
	}

	@Override
	public Integer getActivePower() {
		return getPowerValue(SmaCommonDeviceRegister.ActivePowerTotal);
	}

	@Override
	public Float getLineVoltageLine1Line2() {
		return getVoltageValue(SmaCommonDeviceRegister.GridVoltageLine1Line2);
	}

	@Override
	public Float getLineVoltageLine2Line3() {
		return getVoltageValue(SmaCommonDeviceRegister.GridVoltageLine2Line3);
	}

	@Override
	public Float getLineVoltageLine3Line1() {
		return getVoltageValue(SmaCommonDeviceRegister.GridVoltageLine3Line1);
	}

	@Override
	public Float getVoltage() {
		BigDecimal n1 = NumberUtils
				.bigDecimalForNumber(getNumber(SmaCommonDeviceRegister.GridVoltageLine1Line2));
		BigDecimal n2 = NumberUtils
				.bigDecimalForNumber(getNumber(SmaCommonDeviceRegister.GridVoltageLine2Line3));
		BigDecimal n3 = NumberUtils
				.bigDecimalForNumber(getNumber(SmaCommonDeviceRegister.GridVoltageLine2Line3));
		return n1.add(n2).add(n3).movePointLeft(2).divide(new BigDecimal("3"), RoundingMode.HALF_UP)
				.floatValue();
	}

	@Override
	public Float getCurrent() {
		return getCurrentValue(SmaCommonDeviceRegister.GridCurrent);
	}

	@Override
	public Float getFrequency() {
		BigDecimal d = getFixedScaleValue(SmaCommonDeviceRegister.Frequency, 2);
		return (d != null ? d.floatValue() : null);
	}

	@Override
	public Integer getReactivePower() {
		BigDecimal d = getFixedScaleValue(SmaCommonDeviceRegister.ReactivePower, 2);
		return (d != null ? d.setScale(0, RoundingMode.HALF_UP).intValue() : null);
	}

	@Override
	public Integer getApparentPower() {
		return getPowerValue(SmaCommonDeviceRegister.ApparentPower);
	}

	@Override
	public Integer getActivePowerTarget() {
		return getPowerValue(SmaCommonDeviceRegister.ActivePowerTarget);
	}

	@Override
	public Float getHeatSinkTemperature() {
		BigDecimal d = getTemperatureValue(SmaCommonDeviceRegister.HeatSinkTemperature);
		return (d != null ? d.floatValue() : null);
	}

	@Override
	public Float getCabinetTemperature() {
		BigDecimal d = getTemperatureValue(SmaCommonDeviceRegister.InteriorTemperature);
		return (d != null ? d.floatValue() : null);
	}

	@Override
	public Float getExternalTemperature() {
		BigDecimal d = getTemperatureValue(SmaCommonDeviceRegister.ExternalTemperature1);
		return (d != null ? d.floatValue() : null);
	}

}
