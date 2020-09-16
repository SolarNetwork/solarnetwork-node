/* ==================================================================
 * SmaScStringMonitorUsData.java - 14/09/2020 11:31:07 AM
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
import static net.solarnetwork.domain.GeneralDatumSamplesType.Instantaneous;
import static net.solarnetwork.domain.GeneralDatumSamplesType.Status;
import java.io.IOException;
import java.util.Map;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.domain.MutableGeneralDatumSamplesOperations;
import net.solarnetwork.node.domain.ACEnergyDatum;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.hw.sma.domain.SmaCodedValue;
import net.solarnetwork.node.hw.sma.domain.SmaCommonStatusCode;
import net.solarnetwork.node.hw.sma.domain.SmaDeviceType;
import net.solarnetwork.node.hw.sma.domain.SmaScStringMonitorUsDataAccessor;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * {@link SmaDeviceData} for Sunny Central String Monitor US devices.
 * 
 * @author matt
 * @version 1.0
 */
public class SmaScStringMonitorUsData extends SmaDeviceData implements SmaScStringMonitorUsDataAccessor {

	/**
	 * Constructor.
	 */
	public SmaScStringMonitorUsData() {
		super(SmaDeviceType.SunnyCentralStringMonitorUS);
	}

	/**
	 * Constructor.
	 * 
	 * @param data
	 *        some initial data to use
	 * @param addr
	 *        the starting Modbus register address of {@code data}
	 */
	public SmaScStringMonitorUsData(short[] data, int addr) {
		super(SmaDeviceType.SunnyCentralStringMonitorUS, data, addr);
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the meter data to copy
	 */
	public SmaScStringMonitorUsData(ModbusData other) {
		super(other, SmaDeviceType.SunnyCentralStringMonitorUS);
	}

	@Override
	public SmaScStringMonitorUsData copy() {
		return new SmaScStringMonitorUsData(this);
	}

	@Override
	public void populateDatumSamples(MutableGeneralDatumSamplesOperations samples,
			Map<String, ?> parameters) {
		final String currentFmt = ACEnergyDatum.CURRENT_KEY + "_%d";
		samples.putSampleValue(Instantaneous, format(currentFmt, 1), getCurrentString1());
		samples.putSampleValue(Instantaneous, format(currentFmt, 2), getCurrentString2());
		samples.putSampleValue(Instantaneous, format(currentFmt, 3), getCurrentString3());
		samples.putSampleValue(Instantaneous, format(currentFmt, 4), getCurrentString4());
		samples.putSampleValue(Instantaneous, format(currentFmt, 5), getCurrentString5());
		samples.putSampleValue(Instantaneous, format(currentFmt, 6), getCurrentString6());
		samples.putSampleValue(Instantaneous, format(currentFmt, 7), getCurrentString7());
		samples.putSampleValue(Instantaneous, format(currentFmt, 8), getCurrentString8());

		samples.putSampleValue(Status, "smu_id", getStringMonitoringUnitId());

		SmaCodedValue state = getOperatingState();
		samples.putSampleValue(Status, Datum.OP_STATES, codedValueCode(state));

		DeviceOperatingState dos = getDeviceOperatingState();
		if ( dos != null ) {
			samples.putSampleValue(Status, Datum.OP_STATE, dos.getCode());
		}
	}

	@Override
	public String getDataDescription() {
		StringBuilder buf = new StringBuilder();
		buf.append("SMU ID = ").append(getStringMonitoringUnitId());
		buf.append("; I1 = ").append(getCurrentString1());
		buf.append("; I2 = ").append(getCurrentString2());
		buf.append("; I3 = ").append(getCurrentString3());
		buf.append("; I4 = ").append(getCurrentString4());
		buf.append("; I5 = ").append(getCurrentString5());
		buf.append("; I6 = ").append(getCurrentString6());
		buf.append("; I7 = ").append(getCurrentString7());
		buf.append("; I8 = ").append(getCurrentString8());
		return buf.toString();
	}

	@Override
	public final void readInformationData(final ModbusConnection conn) throws IOException {
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				SmaScStringMonitorUsRegister.INFO_REGISTER_ADDRESS_SET, MAX_RESULTS);
	}

	@Override
	public final void readDeviceData(final ModbusConnection conn) throws IOException {
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				SmaScStringMonitorUsRegister.DATA_REGISTER_ADDRESS_SET, MAX_RESULTS);
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
					return DeviceOperatingState.Unknown;
			}
		}
		return null;
	}

	@Override
	public SmaCodedValue getOperatingState() {
		return getStatusCode(SmaScStringMonitorUsRegister.OperatingState, null,
				SmaCommonStatusCode.NotSet);
	}

	@Override
	public Long getStringMonitoringUnitId() {
		Number n = filterNotNumber(getNumber(SmaScStringMonitorUsRegister.SmuId),
				SmaScStringMonitorUsRegister.SmuId);
		return (n instanceof Long ? (Long) n : n != null ? n.longValue() : null);
	}

	@Override
	public Float getCurrentString1() {
		return getCurrentValue(SmaScStringMonitorUsRegister.CurrentString1);
	}

	@Override
	public Float getCurrentString2() {
		return getCurrentValue(SmaScStringMonitorUsRegister.CurrentString2);
	}

	@Override
	public Float getCurrentString3() {
		return getCurrentValue(SmaScStringMonitorUsRegister.CurrentString3);
	}

	@Override
	public Float getCurrentString4() {
		return getCurrentValue(SmaScStringMonitorUsRegister.CurrentString4);
	}

	@Override
	public Float getCurrentString5() {
		return getCurrentValue(SmaScStringMonitorUsRegister.CurrentString5);
	}

	@Override
	public Float getCurrentString6() {
		return getCurrentValue(SmaScStringMonitorUsRegister.CurrentString6);
	}

	@Override
	public Float getCurrentString7() {
		return getCurrentValue(SmaScStringMonitorUsRegister.CurrentString7);
	}

	@Override
	public Float getCurrentString8() {
		return getCurrentValue(SmaScStringMonitorUsRegister.CurrentString8);
	}

}
