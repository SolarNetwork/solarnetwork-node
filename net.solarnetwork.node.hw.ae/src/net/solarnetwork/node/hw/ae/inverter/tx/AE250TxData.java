/* ==================================================================
 * AE250TxData.java - 27/07/2018 2:13:03 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.ae.inverter.tx;

import static net.solarnetwork.domain.Bitmaskable.setForBitmask;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import net.solarnetwork.domain.Bitmaskable;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.node.domain.ACEnergyDataAccessor;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;
import net.solarnetwork.util.NumberUtils;

/**
 * Data object for the AE 250TX series inverter.
 * 
 * @author matt
 * @version 1.4
 */
public class AE250TxData extends ModbusData implements AE250TxDataAccessor {

	private static final int MAX_RESULTS = 64;

	/**
	 * Constructor.
	 */
	public AE250TxData() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the meter data to copy
	 */
	public AE250TxData(ModbusData other) {
		super(other);
	}

	@Override
	public ModbusData copy() {
		return new AE250TxData(this);
	}

	/**
	 * Get a snapshot copy of the data.
	 * 
	 * @return a copy of the data
	 * @see #copy()
	 */
	public AE250TxData getSnapshot() {
		return (AE250TxData) copy();
	}

	@Override
	public Map<String, Object> getDeviceInfo() {
		AE250TxDataAccessor data = getSnapshot();
		Map<String, Object> result = new LinkedHashMap<>(4);
		AEInverterType type = data.getInverterType();
		if ( type != null ) {
			String firmwareVersion = data.getFirmwareRevision();
			if ( firmwareVersion != null ) {
				result.put(INFO_KEY_DEVICE_MODEL,
						String.format("%s (firmware %s)", type.getDescription(), firmwareVersion));
			} else {
				result.put(INFO_KEY_DEVICE_MODEL, type.getDescription());
			}
		}
		AEInverterConfiguration config = data.getInverterConfiguration();
		if ( config != null ) {
			result.put("Configuration", String.format("%s; %s; tap = %s; meter installed = %s",
					config.getVoltageType().getDescription(), config.getWiringType().getDescription(),
					config.getTapType().getDescription(), config.isMeterInstalled() ? "yes" : "no"));
		}
		String s = data.getSerialNumber();
		if ( s != null && !s.isEmpty() ) {
			result.put(INFO_KEY_DEVICE_SERIAL_NUMBER, s);
		}
		s = data.getIdNumber();
		if ( s != null && !s.isEmpty() ) {
			result.put("ID Number", s);
		}
		Integer rating = data.getInverterRatedPower();
		if ( rating != null ) {
			result.put("Rated Power", rating);
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
				AE250TxRegister.getRegisterAddressSet(), MAX_RESULTS);
	}

	/**
	 * Read the inverter and status registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public final void readInverterData(final ModbusConnection conn) throws IOException {
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				AE250TxRegister.getDataRegisterAddressSet(), MAX_RESULTS);
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
				AE250TxRegister.getStatusRegisterAddressSet(), MAX_RESULTS);
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
	public String getIdNumber() {
		return getAsciiString(AE250TxRegister.InfoInverterIdNumber, true);
	}

	@Override
	public AEInverterType getInverterType() {
		return AEInverterType.forInverterId(getIdNumber());
	}

	@Override
	public String getSerialNumber() {
		return getAsciiString(AE250TxRegister.InfoSerialNumber, true);
	}

	@Override
	public String getFirmwareRevision() {
		return getAsciiString(AE250TxRegister.InfoFirmwareVersion, true);
	}

	@Override
	public Integer getMapVersion() {
		Number n = getNumber(AE250TxRegister.InfoMapVersion);
		return (n != null ? n.intValue() : null);
	}

	@Override
	public AEInverterConfiguration getInverterConfiguration() {
		Number n = getNumber(AE250TxRegister.InfoInverterConfiguration);
		return (n != null ? AEInverterConfiguration.forRegisterValue(n.intValue()) : null);
	}

	@Override
	public Integer getInverterRatedPower() {
		return getKiloValueAsInteger(AE250TxRegister.InfoRatedPower);
	}

	@Override
	public Set<AE250TxSystemStatus> getSystemStatus() {
		Number n = getNumber(AE250TxRegister.StatusOperatingState);
		return (n != null ? setForBitmask(n.intValue(), AE250TxSystemStatus.class) : null);
	}

	@Override
	public SortedSet<AE250TxFault> getFaults() {
		SortedSet<AE250TxFault> result = new TreeSet<>(Bitmaskable.SORT_BY_TYPE);
		Number n = getNumber(AE250TxRegister.StatusMainFault);
		if ( n != null ) {
			result.addAll(setForBitmask(n.intValue(), AE250TxMainFault.class));
		}
		n = getNumber(AE250TxRegister.StatusDriveFault);
		if ( n != null ) {
			result.addAll(setForBitmask(n.intValue(), AE250TxDriveFault.class));
		}
		n = getNumber(AE250TxRegister.StatusVoltageFault);
		if ( n != null ) {
			result.addAll(setForBitmask(n.intValue(), AE250TxVoltageFault.class));
		}
		n = getNumber(AE250TxRegister.StatusGridFault);
		if ( n != null ) {
			result.addAll(setForBitmask(n.intValue(), AE250TxGridFault.class));
		}
		n = getNumber(AE250TxRegister.StatusTemperatureFault);
		if ( n != null ) {
			result.addAll(setForBitmask(n.intValue(), AE250TxTemperatureFault.class));
		}
		n = getNumber(AE250TxRegister.StatusSystemFault);
		if ( n != null ) {
			result.addAll(setForBitmask(n.intValue(), AE250TxSystemFault.class));
		}
		return result;
	}

	@Override
	public SortedSet<AE250TxWarning> getWarnings() {
		SortedSet<AE250TxWarning> result = new TreeSet<>(Bitmaskable.SORT_BY_TYPE);
		Number n = getNumber(AE250TxRegister.StatusSystemWarnings);
		if ( n != null ) {
			result.addAll(setForBitmask(n.intValue(), AE250TxSystemWarning.class));
		}
		n = getNumber(AE250TxRegister.StatusPvMonitoringStatus);
		if ( n != null ) {
			result.addAll(setForBitmask(n.intValue(), AE250TxPvmStatus.class));
		}
		return result;
	}

	@Override
	public DeviceOperatingState getDeviceOperatingState() {
		Set<AE250TxSystemStatus> status = getSystemStatus();
		if ( status.contains(AE250TxSystemStatus.Initialization)
				|| status.contains(AE250TxSystemStatus.StartupDelay)
				|| status.contains(AE250TxSystemStatus.Latching)
				|| status.contains(AE250TxSystemStatus.AcPrecharge)
				|| status.contains(AE250TxSystemStatus.DcPrecharge) ) {
			return DeviceOperatingState.Starting;
		} else if ( status.contains(AE250TxSystemStatus.Sleep) ) {
			return DeviceOperatingState.Standby;
		} else if ( status.contains(AE250TxSystemStatus.Fault) ) {
			return DeviceOperatingState.Fault;
		} else if ( status.contains(AE250TxSystemStatus.Disabled) ) {
			return DeviceOperatingState.Disabled;
		} else if ( status.contains(AE250TxSystemStatus.Idle) ) {
			return DeviceOperatingState.Shutdown;
		} else if ( status.contains(AE250TxSystemStatus.CoolDown) ) {
			return DeviceOperatingState.Recovery;
		}
		return DeviceOperatingState.Normal;
	}

	@Override
	public Float getFrequency() {
		Number n = getNumber(AE250TxRegister.InverterFrequency);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Float getCurrent() {
		Number a = getNumber(AE250TxRegister.InverterCurrentPhaseA);
		Number b = getNumber(AE250TxRegister.InverterCurrentPhaseB);
		Number c = getNumber(AE250TxRegister.InverterCurrentPhaseC);
		return (a != null && b != null && c != null ? a.floatValue() + b.floatValue() + c.floatValue()
				: null);
	}

	@Override
	public Float getNeutralCurrent() {
		return null;
	}

	@Override
	public Float getVoltage() {
		Number a = getNumber(AE250TxRegister.InverterVoltageLineNeutralPhaseA);
		Number b = getNumber(AE250TxRegister.InverterVoltageLineNeutralPhaseB);
		Number c = getNumber(AE250TxRegister.InverterVoltageLineNeutralPhaseC);
		return (a != null && b != null && c != null
				? (a.floatValue() + b.floatValue() + c.floatValue()) / 3.0f
				: null);
	}

	@Override
	public Float getLineVoltage() {
		return null;
	}

	@Override
	public Integer getActivePower() {
		return getKiloValueAsInteger(AE250TxRegister.InverterActivePowerTotal);
	}

	@Override
	public Long getActiveEnergyDelivered() {
		return getKiloValueAsLong(AE250TxRegister.InverterActiveEnergyDelivered);
	}

	@Override
	public Float getDCVoltage() {
		Number n = getNumber(AE250TxRegister.InverterDcVoltage);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Integer getDCPower() {
		return getKiloValueAsInteger(AE250TxRegister.InverterDcPower);
	}

	@Override
	public Float getPowerFactor() {
		return null;
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
		return null;
	}

	@Override
	public Long getReactiveEnergyDelivered() {
		return null;
	}

	@Override
	public Long getReactiveEnergyReceived() {
		return null;
	}

}
