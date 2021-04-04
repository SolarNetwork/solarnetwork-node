/* ==================================================================
 * AE500NxData.java - 22/04/2020 2:06:34 pm
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

package net.solarnetwork.node.hw.ae.inverter.nx;

import static net.solarnetwork.domain.Bitmaskable.setForBitmask;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.node.domain.ACEnergyDataAccessor;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;
import net.solarnetwork.node.io.modbus.ModbusWordOrder;
import net.solarnetwork.util.NumberUtils;

/**
 * Data object for the AE 500NX series inverter.
 * 
 * @author matt
 * @version 1.1
 * @since 2.1
 */
public class AE500NxData extends ModbusData implements AE500NxDataAccessor {

	private static final int MAX_RESULTS = 64;

	/**
	 * Constructor.
	 */
	public AE500NxData() {
		super();
		setWordOrder(ModbusWordOrder.LeastToMostSignificant);
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the meter data to copy
	 */
	public AE500NxData(ModbusData other) {
		super(other);
	}

	@Override
	public ModbusData copy() {
		return new AE500NxData(this);
	}

	/**
	 * Get a snapshot copy of the data.
	 * 
	 * @return a copy of the data
	 * @see #copy()
	 */
	public AE500NxData getSnapshot() {
		return (AE500NxData) copy();
	}

	@Override
	public Map<String, Object> getDeviceInfo() {
		AE500NxDataAccessor data = getSnapshot();
		Map<String, Object> result = new LinkedHashMap<>(4);
		String firmwareVersion = data.getFirmwareVersion();
		if ( firmwareVersion != null ) {
			result.put(INFO_KEY_DEVICE_MODEL, firmwareVersion);
		}
		String s = data.getSerialNumber();
		if ( s != null ) {
			result.put(INFO_KEY_DEVICE_SERIAL_NUMBER, s);
		}
		return result;
	}

	/**
	 * Read the configuration and information registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public final void readConfigurationData(final ModbusConnection conn) throws IOException {
		// we actually read ALL registers here, so our snapshot timestamp includes everything
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				AE500NxRegister.getRegisterAddressSet(), MAX_RESULTS);
	}

	/**
	 * Read the inverter registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public final void readInverterData(final ModbusConnection conn) throws IOException {
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				AE500NxRegister.getInverterRegisterAddressSet(), MAX_RESULTS);
	}

	/**
	 * Read the status registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public final void readStatusData(final ModbusConnection conn) throws IOException {
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				AE500NxRegister.getStatusRegisterAddressSet(), MAX_RESULTS);
	}

	private Integer getKiloValueAsInteger(ModbusReference ref) {
		Number n = getNumber(ref);
		BigDecimal d = NumberUtils.scaled(n, 3);
		if ( d == null ) {
			return null;
		}
		return d.intValue();

	}

	private Long getKiloValueAsLong(ModbusReference ref) {
		Number n = getNumber(ref);
		BigDecimal d = NumberUtils.scaled(n, 3);
		if ( d == null ) {
			return null;
		}
		return d.longValue();

	}

	@Override
	public ACEnergyDataAccessor accessorForPhase(ACPhase phase) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ACEnergyDataAccessor reversed() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Float getFrequency() {
		Number n = getNumber(AE500NxRegister.InverterFrequency);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Float getCurrent() {
		Number n = getNumber(AE500NxRegister.InverterCurrentCommonMode);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Float getNeutralCurrent() {
		Number n = getNumber(AE500NxRegister.InverterCurrentGround);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Float getVoltage() {
		Number n = getNumber(AE500NxRegister.InverterVoltageCommonMode);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Float getLineVoltage() {
		return null;
	}

	@Override
	public Float getPowerFactor() {
		return null;
	}

	@Override
	public Integer getActivePower() {
		return getKiloValueAsInteger(AE500NxRegister.InverterActivePowerTotal);
	}

	@Override
	public Long getActiveEnergyDelivered() {
		return getKiloValueAsLong(AE500NxRegister.InverterActiveEnergyDelivered);
	}

	@Override
	public Long getActiveEnergyReceived() {
		return null;
	}

	@Override
	public Integer getApparentPower() {
		return null;
	}

	@Override
	public Long getApparentEnergyDelivered() {
		return null;
	}

	@Override
	public Long getApparentEnergyReceived() {
		return null;
	}

	@Override
	public Integer getReactivePower() {
		return getKiloValueAsInteger(AE500NxRegister.InverterReactivePowerTotal);
	}

	@Override
	public Long getReactiveEnergyDelivered() {
		return null;
	}

	@Override
	public Long getReactiveEnergyReceived() {
		return null;
	}

	@Override
	public Float getDCVoltage() {
		Number n = getNumber(AE500NxRegister.InverterPvVoltage);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Integer getDCPower() {
		Number n = getNumber(AE500NxRegister.InverterPvCurrent);
		Float v = getDCVoltage();
		if ( n != null && v != null ) {
			return (int) (n.floatValue() * v.floatValue());
		}
		return null;
	}

	@Override
	public String getSerialNumber() {
		return getAsciiString(AE500NxRegister.InfoSerialNumber, true);
	}

	@Override
	public String getFirmwareVersion() {
		String s = getAsciiString(AE500NxRegister.InfoSoftwareVersions, true);
		if ( s != null ) {
			// remove internal whitespace
			s = s.replaceAll("\\s+(?=;)", "");
		}
		return s;
	}

	@Override
	public Set<AE500NxSystemStatus> getSystemStatus() {
		Number n = getNumber(AE500NxRegister.StatusSystemStatus);
		return (n != null ? setForBitmask(n.intValue(), AE500NxSystemStatus.class) : null);
	}

	@Override
	public Set<AE500NxSystemLimit> getSystemLimits() {
		Number n = getNumber(AE500NxRegister.StatusSystemLimits);
		return (n != null ? setForBitmask(n.intValue(), AE500NxSystemLimit.class) : null);
	}

	@Override
	public SortedSet<AE500NxFault> getFaults() {
		Number f0 = getNumber(AE500NxRegister.StatusFaults1);
		Number f1 = getNumber(AE500NxRegister.StatusFaults2);
		Number f2 = getNumber(AE500NxRegister.StatusFaults3);
		return AE500NxUtils.faultSet(f0 != null ? f0.intValue() : 0, f1 != null ? f1.intValue() : 0,
				f2 != null ? f2.intValue() : 0);
	}

	@Override
	public SortedSet<AE500NxWarning> getWarnings() {
		Number f0 = getNumber(AE500NxRegister.StatusWarnings1);
		return AE500NxUtils.warningSet(f0 != null ? f0.intValue() : 0);
	}

	@Override
	public DeviceOperatingState getDeviceOperatingState() {
		Set<AE500NxSystemStatus> status = getSystemStatus();
		if ( status.contains(AE500NxSystemStatus.Startup) ) {
			return DeviceOperatingState.Starting;
		} else if ( status.contains(AE500NxSystemStatus.Sleep) ) {
			return DeviceOperatingState.Standby;
		} else if ( status.contains(AE500NxSystemStatus.Fault)
				|| status.contains(AE500NxSystemStatus.BadMov) ) {
			return DeviceOperatingState.Fault;
		} else if ( !status.contains(AE500NxSystemStatus.Enabled) ) {
			return DeviceOperatingState.Disabled;
		} else if ( !status.contains(AE500NxSystemStatus.Power) ) {
			return DeviceOperatingState.Shutdown;
		}
		return DeviceOperatingState.Normal;
	}

}
