/* ==================================================================
 * SmaScStringMonitorControllerData.java - 15/09/2020 10:23:22 AM
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

import static java.lang.String.format;
import static net.solarnetwork.domain.GeneralDatumSamplesType.Accumulating;
import static net.solarnetwork.domain.GeneralDatumSamplesType.Instantaneous;
import static net.solarnetwork.domain.GeneralDatumSamplesType.Status;
import java.math.BigInteger;
import java.util.Map;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.domain.MutableGeneralDatumSamplesOperations;
import net.solarnetwork.node.domain.ACEnergyDatum;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.hw.sma.domain.SmaCommonStatusCode;
import net.solarnetwork.node.hw.sma.domain.SmaDeviceKind;
import net.solarnetwork.node.hw.sma.domain.SmaDeviceType;
import net.solarnetwork.node.hw.sma.domain.SmaScStringMonitorControllerDataAccessor;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * FIXME
 * 
 * <p>
 * TODO
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class SmaScStringMonitorControllerData extends SmaDeviceData
		implements SmaScStringMonitorControllerDataAccessor {

	/**
	 * Constructor.
	 */
	public SmaScStringMonitorControllerData() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param data
	 *        some initial data to use
	 * @param addr
	 *        the starting Modbus register address of {@code data}
	 */
	public SmaScStringMonitorControllerData(short[] data, int addr) {
		super(data, addr);
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the meter data to copy
	 */
	public SmaScStringMonitorControllerData(ModbusData other) {
		super(other);
	}

	@Override
	public SmaScStringMonitorControllerData copy() {
		return new SmaScStringMonitorControllerData(this);
	}

	@Override
	public void populateDatumSamples(MutableGeneralDatumSamplesOperations samples,
			Map<String, ?> parameters) {
		final String currentFmt = ACEnergyDatum.CURRENT_KEY + "_%d";
		samples.putSampleValue(Instantaneous, format(currentFmt, 1), getCurrentGroup1());
		samples.putSampleValue(Instantaneous, format(currentFmt, 2), getCurrentGroup2());
		samples.putSampleValue(Instantaneous, format(currentFmt, 3), getCurrentGroup3());
		samples.putSampleValue(Instantaneous, format(currentFmt, 4), getCurrentGroup4());
		samples.putSampleValue(Instantaneous, format(currentFmt, 5), getCurrentGroup5());
		samples.putSampleValue(Instantaneous, format(currentFmt, 6), getCurrentGroup6());

		samples.putSampleValue(Accumulating, "opTime", getOperatingTime());

		SmaCommonStatusCode state = getError();
		if ( state != null ) {
			samples.putSampleValue(Status, "error", state.getCode());
		}

		Long code = getSmuWarningCode();
		if ( code != null ) {
			samples.putSampleValue(Status, "ssmuWarn", code);
		}

		state = getOperatingState();
		if ( state != null ) {
			samples.putSampleValue(Status, Datum.OP_STATES, state.getCode());
		}

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
				SmaScStringMonitorControllerRegister.INFO_REGISTER_ADDRESS_SET, MAX_RESULTS);
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
				SmaScStringMonitorControllerRegister.DATA_REGISTER_ADDRESS_SET, MAX_RESULTS);
	}

	@Override
	public SmaDeviceKind getDeviceKind() {
		return SmaDeviceType.SunnyCentralStringMonitor;
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
		Number n = getNumber(SmaScStringMonitorUsRegister.OperatingState);
		if ( n == null ) {
			return SmaCommonStatusCode.Unknown;
		}
		return SmaCommonStatusCode.forCode(n.intValue());
	}

	@Override
	public Long getEventId() {
		Number n = getNumber(SmaScStringMonitorControllerRegister.Event);
		return (n instanceof Long ? (Long) n : n != null ? n.longValue() : null);
	}

	@Override
	public SmaCommonStatusCode getError() {
		return getStatusCode(SmaScStringMonitorControllerRegister.ErrorState);
	}

	@Override
	public BigInteger getOperatingTime() {
		Number n = getNumber(SmaSunnySensorboxRegister.OperatingTime);
		return (n instanceof BigInteger ? (BigInteger) n
				: n != null ? new BigInteger(n.toString()) : null);
	}

	@Override
	public Float getCurrentGroup1() {
		return getCurrentValue(SmaScStringMonitorControllerRegister.CurrentGroup1);
	}

	@Override
	public Float getCurrentGroup2() {
		return getCurrentValue(SmaScStringMonitorControllerRegister.CurrentGroup2);
	}

	@Override
	public Float getCurrentGroup3() {
		return getCurrentValue(SmaScStringMonitorControllerRegister.CurrentGroup3);
	}

	@Override
	public Float getCurrentGroup4() {
		return getCurrentValue(SmaScStringMonitorControllerRegister.CurrentGroup4);
	}

	@Override
	public Float getCurrentGroup5() {
		return getCurrentValue(SmaScStringMonitorControllerRegister.CurrentGroup5);
	}

	@Override
	public Float getCurrentGroup6() {
		return getCurrentValue(SmaScStringMonitorControllerRegister.CurrentGroup6);
	}

	@Override
	public Long getSmuWarningCode() {
		Number n = getNumber(SmaScStringMonitorControllerRegister.SmuWarning);
		return (n instanceof Long ? (Long) n : n != null ? n.longValue() : null);
	}

}
