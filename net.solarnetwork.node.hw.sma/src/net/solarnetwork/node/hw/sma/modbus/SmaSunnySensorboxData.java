/* ==================================================================
 * SmaSunnySensorboxData.java - 15/09/2020 7:02:16 AM
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
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.domain.MutableGeneralDatumSamplesOperations;
import net.solarnetwork.node.domain.AtmosphericDatum;
import net.solarnetwork.node.hw.sma.domain.SmaDeviceType;
import net.solarnetwork.node.hw.sma.domain.SmaSunnySensorboxDataAccessor;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.util.NumberUtils;

/**
 * {@link SmaDeviceData} for Sunny Sensorbox devices.
 * 
 * @author matt
 * @version 1.0
 */
public class SmaSunnySensorboxData extends SmaDeviceData implements SmaSunnySensorboxDataAccessor {

	/**
	 * Constructor.
	 */
	public SmaSunnySensorboxData() {
		super(SmaDeviceType.SunnySensorbox);
	}

	/**
	 * Constructor.
	 * 
	 * @param data
	 *        some initial data to use
	 * @param addr
	 *        the starting Modbus register address of {@code data}
	 */
	public SmaSunnySensorboxData(short[] data, int addr) {
		super(SmaDeviceType.SunnySensorbox, data, addr);
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the meter data to copy
	 */
	public SmaSunnySensorboxData(ModbusData other) {
		super(other, SmaDeviceType.SunnySensorbox);
	}

	@Override
	public SmaSunnySensorboxData copy() {
		return new SmaSunnySensorboxData(this);
	}

	@Override
	public void populateDatumSamples(MutableGeneralDatumSamplesOperations samples,
			Map<String, ?> parameters) {
		samples.putSampleValue(Instantaneous, AtmosphericDatum.IRRADIANCE_KEY, getIrradiance());
		samples.putSampleValue(Instantaneous, AtmosphericDatum.IRRADIANCE_KEY + "_ex",
				getExternalIrradiance());
		samples.putSampleValue(Instantaneous, AtmosphericDatum.TEMPERATURE_KEY, getTemperature());
		samples.putSampleValue(Instantaneous, AtmosphericDatum.TEMPERATURE_KEY + "_module",
				getModuleTemperature());
		samples.putSampleValue(Instantaneous, AtmosphericDatum.WIND_SPEED_KEY, getWindSpeed());

		samples.putSampleValue(Accumulating, "opTime", getOperatingTime());
	}

	@Override
	public String getDataDescription() {
		StringBuilder buf = new StringBuilder();
		buf.append("irradiance = ").append(getIrradiance());
		buf.append("; temp = ").append(getTemperature());
		buf.append("; moduleTemp = ").append(getModuleTemperature());
		buf.append("; windSpeed = ").append(getWindSpeed());
		return buf.toString();
	}

	@Override
	public final void readInformationData(final ModbusConnection conn) throws IOException {
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				SmaSunnySensorboxRegister.INFO_REGISTER_ADDRESS_SET, MAX_RESULTS);
	}

	@Override
	public final void readDeviceData(final ModbusConnection conn) throws IOException {
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				SmaSunnySensorboxRegister.DATA_REGISTER_ADDRESS_SET, MAX_RESULTS);
	}

	@Override
	public DeviceOperatingState getDeviceOperatingState() {
		return null;
	}

	@Override
	public Long getDeviceClass() {
		Number n = filterNotNumber(getNumber(SmaSunnySensorboxRegister.MainModel),
				SmaSunnySensorboxRegister.MainModel);
		return (n instanceof Long ? (Long) n : n != null ? n.longValue() : null);
	}

	@Override
	public BigInteger getOperatingTime() {
		Number n = filterNotNumber(getNumber(SmaSunnySensorboxRegister.OperatingTime),
				SmaSunnySensorboxRegister.OperatingTime);
		return (n instanceof BigInteger ? (BigInteger) n
				: n != null ? new BigInteger(n.toString()) : null);
	}

	@Override
	public BigDecimal getTemperature() {
		return getTemperatureValue(SmaSunnySensorboxRegister.AmbientTemperature);
	}

	@Override
	public BigDecimal getIrradiance() {
		Number n = filterNotNumber(getNumber(SmaSunnySensorboxRegister.Irradiance),
				SmaSunnySensorboxRegister.Irradiance);
		return NumberUtils.bigDecimalForNumber(n);
	}

	@Override
	public BigDecimal getWindSpeed() {
		return getFixedScaleValue(SmaSunnySensorboxRegister.WindSpeed, 1);
	}

	@Override
	public BigDecimal getModuleTemperature() {
		return getTemperatureValue(SmaSunnySensorboxRegister.ModuleTemperature);
	}

	@Override
	public BigDecimal getExternalIrradiance() {
		Number n = filterNotNumber(getNumber(SmaSunnySensorboxRegister.ExternalIrradiance),
				SmaSunnySensorboxRegister.ExternalIrradiance);
		return NumberUtils.bigDecimalForNumber(n);
	}

}
