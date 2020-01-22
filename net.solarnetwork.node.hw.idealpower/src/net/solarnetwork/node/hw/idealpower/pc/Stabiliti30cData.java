/* ==================================================================
 * Stabiliti30cData.java - 29/08/2019 10:38:26 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.idealpower.pc;

import static net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cUtils.ABORT2_SEVERITY;
import static net.solarnetwork.node.hw.idealpower.pc.Stabiliti30cUtils.SORT_BY_FAULT_SEVERITY;
import static net.solarnetwork.node.io.modbus.ModbusWriteFunction.WriteHoldingRegister;
import static net.solarnetwork.util.NumberUtils.scaled;
import static net.solarnetwork.util.StringUtils.commaDelimitedStringFromCollection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import net.solarnetwork.domain.Bitmaskable;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;
import net.solarnetwork.util.IntRangeSet;

/**
 * Implementation for Stabiliti 30C series power control system data.
 * 
 * @author matt
 * @version 2.0
 */
public class Stabiliti30cData extends ModbusData implements Stabiliti30cDataAccessor {

	private static final int MAX_RESULTS = 64;
	private static final IntRangeSet REG_ADDR_SET = Stabiliti30cRegister.getRegisterAddressSet();
	private static final IntRangeSet PWR_CRTL_ADDR_SET = Stabiliti30cRegister
			.getPowerControlRegisterAddressSet();
	private static final IntRangeSet CRTL_REG_SET = Stabiliti30cRegister.getControlRegisterAddressSet();

	/**
	 * Default constructor.
	 */
	public Stabiliti30cData() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the data to copy
	 */
	public Stabiliti30cData(ModbusData other) {
		super(other);
	}

	/**
	 * Get a snapshot copy of this data.
	 * 
	 * @return the copy of data
	 */
	public Stabiliti30cData getSnapshot() {
		return new Stabiliti30cData(this);
	}

	@Override
	public ModbusData copy() {
		return getSnapshot();
	}

	/**
	 * Read the configuration and information registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 */
	public final void readConfigurationData(final ModbusConnection conn) {
		// we actually read ALL registers here, so our snapshot timestamp includes everything
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister, REG_ADDR_SET, MAX_RESULTS);
		readControlData(conn);
	}

	/**
	 * Read the power control registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 */
	public final void readPowerControlData(final ModbusConnection conn) {
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister, PWR_CRTL_ADDR_SET, MAX_RESULTS);
	}

	/**
	 * Read the control registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 */
	public final void readControlData(final ModbusConnection conn) {
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister, CRTL_REG_SET, MAX_RESULTS);
	}

	@Override
	public Map<String, Object> getDeviceInfo() {
		Stabiliti30cDataAccessor data = getSnapshot();
		Map<String, Object> result = new LinkedHashMap<>(8);
		String fwVersion = data.getFirmwareVersion();
		if ( fwVersion != null ) {
			result.put("Firmware Version", fwVersion);
		}
		String commVersion = data.getCommunicationsVersion();
		if ( commVersion != null ) {
			result.put("Comms Version", commVersion);
		}
		String s = data.getSerialNumber();
		if ( s != null ) {
			result.put(INFO_KEY_DEVICE_SERIAL_NUMBER, s);
		}
		SortedSet<Stabiliti30cFault> faults = data.getFaults();
		if ( faults != null && !faults.isEmpty() ) {
			SortedSet<Stabiliti30cFault> faultsBySeverity = new TreeSet<>(SORT_BY_FAULT_SEVERITY);
			faultsBySeverity.addAll(faults);
			SortedSet<Stabiliti30cFault> severeFaults = faultsBySeverity.tailSet(ABORT2_SEVERITY);
			result.put("Abort2+ faults", commaDelimitedStringFromCollection(severeFaults));
		}
		return result;
	}

	private Float getValueAsFloat(ModbusReference ref) {
		Number n = getNumber(ref);
		return (n != null ? n.floatValue() : null);
	}

	private Integer getValueAsInteger(ModbusReference ref) {
		Number n = getNumber(ref);
		return (n != null ? n.intValue() : null);
	}

	private int asDeciValue(Number value) {
		return scaled(value, 1).intValue();
	}

	private Float getDeciValueAsFloat(ModbusReference ref) {
		Number n = scaled(getNumber(ref), -1);
		if ( n == null ) {
			return null;
		}
		return n.floatValue();
	}

	private int asDecaValue(Number value) {
		return scaled(value, -1).intValue();
	}

	private Integer getDecaValueAsInteger(ModbusReference ref) {
		Number n = scaled(getNumber(ref), 1);
		if ( n == null ) {
			return null;
		}
		return n.intValue();
	}

	private int asMilliValue(Number value) {
		return scaled(value, 3).intValue();
	}

	private Float getMilliValueAsFloat(ModbusReference ref) {
		Number n = scaled(getNumber(ref), -3);
		if ( n == null ) {
			return null;
		}
		return n.floatValue();
	}

	@Override
	public Stabiliti30cAcPortType getP1PortType() {
		Number n = getNumber(Stabiliti30cRegister.ConfigP1PortType);
		if ( n != null ) {
			try {
				return Stabiliti30cAcPortType.forCode(n.intValue());
			} catch ( IllegalArgumentException e ) {
				// ignore
			}
		}
		return null;
	}

	@Override
	public Integer getP1ActivePower() {
		return getDecaValueAsInteger(Stabiliti30cRegister.PowerControlP1RealPower);
	}

	@Override
	public Float getP2Voltage() {
		return getValueAsFloat(Stabiliti30cRegister.PowerControlP2Voltage);
	}

	@Override
	public Stabiliti30cAcControlMethod getP1ControlMethod() {
		Number n = getNumber(Stabiliti30cRegister.ControlP1ControlMethod);
		if ( n != null ) {
			try {
				return Stabiliti30cAcControlMethod.forCode(n.intValue());
			} catch ( IllegalArgumentException e ) {
				// ignore
			}
		}
		return null;
	}

	@Override
	public Integer getP1ActivePowerSetpoint() {
		return getDecaValueAsInteger(Stabiliti30cRegister.ControlP1RealPowerSetpoint);
	}

	@Override
	public Integer getP1VoltageSetpoint() {
		return getValueAsInteger(Stabiliti30cRegister.ControlP1VoltageSetpoint);
	}

	@Override
	public Float getP1FrequencySetpoint() {
		return getMilliValueAsFloat(Stabiliti30cRegister.ControlP1FrequencySetpoint);
	}

	@Override
	public Float getP1CurrentLimit() {
		return getDeciValueAsFloat(Stabiliti30cRegister.ControlP1CurrentLimit);
	}

	@Override
	public Integer getP2Power() {
		return getDecaValueAsInteger(Stabiliti30cRegister.PowerControlP2Power);
	}

	@Override
	public Float getP2Current() {
		return getDeciValueAsFloat(Stabiliti30cRegister.PowerControlP2Current);
	}

	@Override
	public Stabiliti30cDcControlMethod getP2ControlMethod() {
		Number n = getNumber(Stabiliti30cRegister.ControlP2ControlMethod);
		if ( n != null ) {
			try {
				return Stabiliti30cDcControlMethod.forCode(n.intValue());
			} catch ( IllegalArgumentException e ) {
				// ignore
			}
		}
		return null;
	}

	@Override
	public Float getP2CurrentSetpoint() {
		return getDeciValueAsFloat(Stabiliti30cRegister.ControlP2CurrentSetpoint);
	}

	@Override
	public Integer getP2PowerSetpoint() {
		return getDecaValueAsInteger(Stabiliti30cRegister.ControlP2PowerSetpoint);
	}

	@Override
	public Integer getP2VoltageMaximumLimit() {
		return getValueAsInteger(Stabiliti30cRegister.ControlP2VoltageMax);
	}

	@Override
	public Integer getP2VoltageMinimumLimit() {
		return getValueAsInteger(Stabiliti30cRegister.ControlP2VoltageMin);
	}

	@Override
	public Integer getP2ImportPowerLimit() {
		return getDecaValueAsInteger(Stabiliti30cRegister.ControlP2ImportPowerLimit);
	}

	@Override
	public Integer getP2ExportPowerLimit() {
		return getDecaValueAsInteger(Stabiliti30cRegister.ControlP2ExportPowerLimit);
	}

	@Override
	public Float getP2CurrentLimit() {
		return getDeciValueAsFloat(Stabiliti30cRegister.ControlP2CurrentLimit);
	}

	@Override
	public Float getP3Voltage() {
		Number n = getNumber(Stabiliti30cRegister.PowerControlP3Voltage);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Integer getP3Power() {
		return getDecaValueAsInteger(Stabiliti30cRegister.PowerControlP3Power);
	}

	@Override
	public Float getP3Current() {
		return getDeciValueAsFloat(Stabiliti30cRegister.PowerControlP3Current);
	}

	@Override
	public Stabiliti30cDcControlMethod getP3ControlMethod() {
		Number n = getNumber(Stabiliti30cRegister.ControlP3ControlMethod);
		if ( n != null ) {
			try {
				return Stabiliti30cDcControlMethod.forCode(n.intValue());
			} catch ( IllegalArgumentException e ) {
				// ignore
			}
		}
		return null;
	}

	@Override
	public Integer getP3MpptStartTimeOffsetSetpoint() {
		return getValueAsInteger(Stabiliti30cRegister.ControlP3MpptStart);
	}

	@Override
	public Integer getP3MpptStopTimeOffsetSetpoint() {
		return getValueAsInteger(Stabiliti30cRegister.ControlP3MpptStop);
	}

	@Override
	public Integer getP3MpptVoltageMinimumSetpoint() {
		return getValueAsInteger(Stabiliti30cRegister.ControlP3MpptVoltageMin);
	}

	@Override
	public Integer getP3VoltageMaximum() {
		return getValueAsInteger(Stabiliti30cRegister.ControlP3VoltageMax);
	}

	@Override
	public Integer getP3VoltageMinimum() {
		return getValueAsInteger(Stabiliti30cRegister.ControlP3VoltageMin);
	}

	@Override
	public Integer getP3ImportPowerLimit() {
		return getDecaValueAsInteger(Stabiliti30cRegister.ControlP3ImportPowerLimit);
	}

	@Override
	public Float getP3CurrentLimit() {
		return getDeciValueAsFloat(Stabiliti30cRegister.ControlP3CurrentLimit);
	}

	@Override
	public String getSerialNumber() {
		return getAsciiString(Stabiliti30cRegister.InfoSerialNumber, true);
	}

	@Override
	public String getFirmwareVersion() {
		Number major = getNumber(Stabiliti30cRegister.InfoFirmwareVersion);
		Number minor = getNumber(Stabiliti30cRegister.InfoBuildVersion);
		return (major != null && minor != null ? major.toString() + "." + minor.toString() : null);
	}

	@Override
	public String getCommunicationsVersion() {
		Number major = getNumber(Stabiliti30cRegister.InfoCommsVersion);
		Number minor = getNumber(Stabiliti30cRegister.InfoCommsBuildVersion);
		return (major != null && minor != null ? major.toString() + "." + minor.toString() : null);
	}

	@Override
	public boolean isManualModeEnabled() {
		Number n = getNumber(Stabiliti30cRegister.ControlManualModeStart);
		return (n != null && n.intValue() > 0);
	}

	@Override
	public Integer getWatchdogTimeout() {
		return getValueAsInteger(Stabiliti30cRegister.ControlWatchdogSeconds);
	}

	@Override
	public DeviceOperatingState getDeviceOperatingState() {
		Set<Stabiliti30cSystemInfo> infos = getSystemInfo();
		if ( infos != null ) {
			if ( infos.contains(Stabiliti30cSystemInfo.Shutdown) ) {
				return DeviceOperatingState.Shutdown;
			}
			for ( Stabiliti30cSystemInfo info : infos ) {
				switch (info) {
					case ArcFault:
					case GfdiFault:
					case GridContactorFault:
					case ImiFault:
					case SelfTestFault:
					case SystemFault:
						return DeviceOperatingState.Fault;

					default:
						// nothing
				}
			}
			if ( infos.contains(Stabiliti30cSystemInfo.Reconnecting) ) {
				return DeviceOperatingState.Recovery;
			}
		}
		return DeviceOperatingState.Normal;
	}

	@Override
	public Set<Stabiliti30cSystemInfo> getSystemInfo() {
		Number n = getNumber(Stabiliti30cRegister.StatusInfo);
		return (n != null ? Bitmaskable.setForBitmask(n.intValue(), Stabiliti30cSystemInfo.class)
				: null);
	}

	@Override
	public Stabiliti30cOperatingMode getOperatingMode() {
		Number n = getNumber(Stabiliti30cRegister.StatusOperatingMode);
		return (n != null ? Stabiliti30cOperatingMode.forCode(n.intValue()) : null);
	}

	@Override
	public Set<Stabiliti30cSystemStatus> getSystemStatus() {
		Number n = getNumber(Stabiliti30cRegister.StatusSystem);
		return (n != null ? Bitmaskable.setForBitmask(n.intValue(), Stabiliti30cSystemStatus.class)
				: null);
	}

	@Override
	public SortedSet<Stabiliti30cFault> getFaults() {
		Number f0 = getNumber(Stabiliti30cRegister.StatusFaultActive0);
		Number f1 = getNumber(Stabiliti30cRegister.StatusFaultActive1);
		Number f2 = getNumber(Stabiliti30cRegister.StatusFaultActive2);
		Number f3 = getNumber(Stabiliti30cRegister.StatusFaultActive3);
		return Stabiliti30cUtils.faultSet(f0 != null ? f0.intValue() : 0, f1 != null ? f1.intValue() : 0,
				f2 != null ? f2.intValue() : 0, f3 != null ? f3.intValue() : 0);
	}

	/**
	 * Get a control accessor that does not update local state.
	 * 
	 * @param conn
	 *        the Modbus connection to use
	 * @return the accessor
	 */
	public Stabiliti30cControlAccessor controlAccessor(ModbusConnection conn) {
		return new ControlAccessor(conn, null);
	}

	/**
	 * Get a control accessor.
	 * 
	 * <p>
	 * This accessor is designed so that several updates can be issued in one
	 * overall transaction and then discarded.
	 * </p>
	 * 
	 * @param conn
	 *        the Modbus connection to use
	 * @param state
	 *        the mutable modbus data state to update
	 * @return the accessor
	 */
	public Stabiliti30cControlAccessor controlAccessor(ModbusConnection conn, MutableModbusData state) {
		return new ControlAccessor(conn, state);
	}

	private class ControlAccessor implements Stabiliti30cControlAccessor {

		private final ModbusConnection conn;
		private final MutableModbusData state;

		private ControlAccessor(ModbusConnection conn, MutableModbusData state) {
			super();
			this.conn = conn;
			this.state = state;
		}

		private void update(final ModbusReference register, final int data) {
			update(register, new int[] { data });
		}

		private void update(final ModbusReference register, final int[] data) {
			final int addr = register.getAddress();
			conn.writeUnsignedShorts(WriteHoldingRegister, addr, data);
			if ( state != null ) {
				state.saveDataArray(data, addr);
			}
		}

		@Override
		public void setManualModeEnabled(boolean enabled) {
			if ( enabled ) {
				update(Stabiliti30cRegister.ControlUserStart, 1);
				if ( state != null ) {
					state.saveDataArray(new int[0], Stabiliti30cRegister.ControlUserStop.getAddress());
				}
			} else {
				update(Stabiliti30cRegister.ControlUserStop, 1);
				if ( state != null ) {
					state.saveDataArray(new int[0], Stabiliti30cRegister.ControlUserStart.getAddress());
				}
			}
		}

		@Override
		public void setWatchdogTimeout(int seconds) {
			update(Stabiliti30cRegister.ControlWatchdogSeconds, seconds);
		}

		@Override
		public void setP1ControlMethod(Stabiliti30cAcControlMethod method) {
			update(Stabiliti30cRegister.ControlP1ControlMethod, method.getCode());
		}

		@Override
		public void setP1ActivePowerSetpoint(Integer power) {
			update(Stabiliti30cRegister.ControlP1RealPowerSetpoint, asDecaValue(power));
		}

		@Override
		public void setP1VoltageSetpoint(Integer voltage) {
			update(Stabiliti30cRegister.ControlP1VoltageSetpoint, voltage);
		}

		@Override
		public void setP1FrequencySetpoint(Float frequency) {
			update(Stabiliti30cRegister.ControlP1FrequencySetpoint, asMilliValue(frequency));
		}

		@Override
		public void setP1CurrentLimit(Float current) {
			update(Stabiliti30cRegister.ControlP1CurrentLimit, asDeciValue(current));
		}

		@Override
		public void setP2ControlMethod(Stabiliti30cDcControlMethod method) {
			update(Stabiliti30cRegister.ControlP2ControlMethod, method.getCode());
		}

		@Override
		public void setP2CurrentSetpoint(Float current) {
			update(Stabiliti30cRegister.ControlP2CurrentSetpoint, asDeciValue(current));
		}

		@Override
		public void setP2PowerSetpoint(Integer power) {
			update(Stabiliti30cRegister.ControlP2PowerSetpoint, asDecaValue(power));
		}

		@Override
		public void setP2VoltageMaximumLimit(Integer voltage) {
			update(Stabiliti30cRegister.ControlP2VoltageMax, voltage);
		}

		@Override
		public void setP2VoltageMinimumLimit(Integer voltage) {
			update(Stabiliti30cRegister.ControlP2VoltageMin, voltage);
		}

		@Override
		public void setP2ImportPowerLimit(Integer power) {
			update(Stabiliti30cRegister.ControlP2ImportPowerLimit, asDecaValue(power));
		}

		@Override
		public void setP2ExportPowerLimit(Integer power) {
			update(Stabiliti30cRegister.ControlP2ExportPowerLimit, asDecaValue(power));
		}

		@Override
		public void setP2CurrentLimit(Float current) {
			update(Stabiliti30cRegister.ControlP2CurrentLimit, asDeciValue(current));
		}

		@Override
		public void setP3ControlMethod(Stabiliti30cDcControlMethod method) {
			update(Stabiliti30cRegister.ControlP3ControlMethod, method.getCode());
		}

		@Override
		public void setP3MpptStartTimeOffsetSetpoint(Integer offset) {
			update(Stabiliti30cRegister.ControlP3MpptStart, offset);
		}

		@Override
		public void setP3MpptStopTimeOffsetSetpoint(Integer offset) {
			update(Stabiliti30cRegister.ControlP3MpptStop, offset);
		}

		@Override
		public void setP3MpptVoltageMinimumSetpoint(Integer voltage) {
			update(Stabiliti30cRegister.ControlP3MpptVoltageMin, voltage);
		}

		@Override
		public void setP3VoltageMaximum(Integer voltage) {
			update(Stabiliti30cRegister.ControlP3VoltageMax, voltage);
		}

		@Override
		public void setP3VoltageMinimum(Integer voltage) {
			update(Stabiliti30cRegister.ControlP3VoltageMin, voltage);
		}

		@Override
		public void setP3ImportPowerLimit(Integer power) {
			update(Stabiliti30cRegister.ControlP3ImportPowerLimit, asDecaValue(power));
		}

		@Override
		public void setP3CurrentLimit(Float current) {
			update(Stabiliti30cRegister.ControlP3CurrentLimit, asDeciValue(current));
		}

	}

}
