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
import static net.solarnetwork.domain.CodedValue.forCodeValue;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.domain.Bitmaskable;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.domain.GroupedBitmaskable;
import net.solarnetwork.node.domain.AcEnergyDataAccessor;
import net.solarnetwork.node.domain.DataAccessor;
import net.solarnetwork.node.hw.sunspec.ModelEvent;
import net.solarnetwork.node.hw.sunspec.inverter.InverterModelEvent;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;
import net.solarnetwork.util.NumberUtils;

/**
 * Data object for the AE 250TX series inverter.
 * 
 * @author matt
 * @version 2.2
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
		AEInverterType type = null;
		try {
			type = data.getInverterType();
		} catch ( IllegalArgumentException e ) {
			// ignore and continue
		}
		if ( type != null ) {
			String firmwareVersion = data.getFirmwareRevision();
			if ( firmwareVersion != null ) {
				result.put(DataAccessor.INFO_KEY_DEVICE_MODEL,
						String.format("%s (firmware %s)", type.getDescription(), firmwareVersion));
			} else {
				result.put(DataAccessor.INFO_KEY_DEVICE_MODEL, type.getDescription());
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
			result.put(DataAccessor.INFO_KEY_DEVICE_SERIAL_NUMBER, s);
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
	public AcEnergyDataAccessor accessorForPhase(AcPhase phase) {
		if ( phase == AcPhase.Total ) {
			return this;
		}
		return new PhaseDataAccessor(phase);
	}

	@Override
	public AcEnergyDataAccessor reversed() {
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
	public AE250TxSystemStatus getSystemStatus() {
		Number n = getNumber(AE250TxRegister.StatusOperatingState);
		return (n != null
				? forCodeValue(n.intValue(), AE250TxSystemStatus.class, AE250TxSystemStatus.Sleep)
				: null);
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
		return result;
	}

	@Override
	public DeviceOperatingState getDeviceOperatingState() {
		final AE250TxSystemStatus status = getSystemStatus();
		switch (status) {
			case Initialization:
			case StartupDelay:
			case AcPrecharge:
			case DcPrecharge:
				return DeviceOperatingState.Starting;

			case Sleep:
				return DeviceOperatingState.Shutdown;

			case Fault:
			case LatchingFault:
				return DeviceOperatingState.Fault;

			case Disabled:
				return DeviceOperatingState.Disabled;

			case Idle:
				return DeviceOperatingState.Standby;

			case CoolDown:
				return DeviceOperatingState.Recovery;

			default:
				return DeviceOperatingState.Normal;
		}
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
	public Float getPvVoltage() {
		Number n = getNumber(AE250TxRegister.InverterPvVoltage);
		return (n != null ? n.floatValue() : null);
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
	public Float getDcCurrent() {
		Number n = getNumber(AE250TxRegister.InverterDcCurrent);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Float getDcVoltage() {
		Number n = getNumber(AE250TxRegister.InverterDcVoltage);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Integer getDcPower() {
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

	@Override
	public Set<ModelEvent> getEvents() {
		SortedSet<AE250TxFault> faults = getFaults();
		Set<ModelEvent> events = new LinkedHashSet<>(16);
		if ( !faults.isEmpty() ) {
			if ( faults.contains(AE250TxVoltageFault.DcVoltageHigh) ) {
				events.add(InverterModelEvent.DcOverVoltage);
			}
			if ( faults.contains(AE250TxGridFault.AcFastUnderVoltA)
					|| faults.contains(AE250TxGridFault.AcFastUnderVoltB)
					|| faults.contains(AE250TxGridFault.AcFastUnderVoltC)
					|| faults.contains(AE250TxGridFault.AcSlowUnderVoltA)
					|| faults.contains(AE250TxGridFault.AcSlowUnderVoltB)
					|| faults.contains(AE250TxGridFault.AcSlowUnderVoltC) ) {
				events.add(InverterModelEvent.AcUnderVoltage);
			}
			if ( faults.contains(AE250TxGridFault.AcFastOverVoltA)
					|| faults.contains(AE250TxGridFault.AcFastOverVoltB)
					|| faults.contains(AE250TxGridFault.AcFastOverVoltC)
					|| faults.contains(AE250TxGridFault.AcSlowOverVoltA)
					|| faults.contains(AE250TxGridFault.AcSlowOverVoltB)
					|| faults.contains(AE250TxGridFault.AcSlowOverVoltC) ) {
				events.add(InverterModelEvent.AcOverVoltage);
			}
			if ( faults.contains(AE250TxGridFault.AcUnderFreq) ) {
				events.add(InverterModelEvent.UnderFrequency);
			}
			if ( faults.contains(AE250TxGridFault.AcOverFreq) ) {
				events.add(InverterModelEvent.OverFrequency);
			}
			if ( faults.contains(AE250TxTemperatureFault.HeatsinkTempA1)
					|| faults.contains(AE250TxTemperatureFault.HeatsinkTempA2)
					|| faults.contains(AE250TxTemperatureFault.HeatsinkTempB1)
					|| faults.contains(AE250TxTemperatureFault.HeatsinkTempB2)
					|| faults.contains(AE250TxTemperatureFault.HeatsinkTempC1)
					|| faults.contains(AE250TxTemperatureFault.HeatsinkTempC2)
					|| faults.contains(AE250TxTemperatureFault.BoardTempHigh)
					|| faults.contains(AE250TxTemperatureFault.MagTempHigh)
					|| faults.contains(AE250TxTemperatureFault.IpmTempHigh)
					|| faults.contains(AE250TxTemperatureFault.InductorTempHigh) ) {
				events.add(InverterModelEvent.OverTemperature);
			}
			if ( faults.contains(AE250TxTemperatureFault.DriveTempLow)
					|| faults.contains(AE250TxTemperatureFault.AmbientTempLow)
					|| faults.contains(AE250TxTemperatureFault.MagTempLow) ) {
				events.add(InverterModelEvent.UnderTemperature);
			}
			if ( faults.contains(AE250TxSystemFault.Ground) ) {
				events.add(InverterModelEvent.GroundFault);
			}
			if ( faults.contains(AE250TxSystemFault.AcContactor) ) {
				events.add(InverterModelEvent.AcDisconnect);
			}
			if ( faults.contains(AE250TxSystemFault.DcContactor) ) {
				events.add(InverterModelEvent.DcDisconnect);
			}
		}
		return events;
	}

	@Override
	public BitSet getVendorEvents() {
		return GroupedBitmaskable.overallBitmaskValue(getFaults());
	}

	private class PhaseDataAccessor implements AE250TxDataAccessor {

		private final AcPhase phase;

		private PhaseDataAccessor(AcPhase phase) {
			super();
			this.phase = phase;
		}

		@Override
		public Instant getDataTimestamp() {
			return AE250TxData.this.getDataTimestamp();
		}

		@Override
		public AcEnergyDataAccessor accessorForPhase(AcPhase phase) {
			return AE250TxData.this.accessorForPhase(phase);
		}

		@Override
		public Float getFrequency() {
			return AE250TxData.this.getFrequency();
		}

		@Override
		public Float getCurrent() {
			Number n = null;
			switch (phase) {
				case PhaseA:
					n = getNumber(AE250TxRegister.InverterCurrentPhaseA);
					break;

				case PhaseB:
					n = getNumber(AE250TxRegister.InverterCurrentPhaseB);
					break;

				case PhaseC:
					n = getNumber(AE250TxRegister.InverterCurrentPhaseC);
					break;

				default:
					return AE250TxData.this.getCurrent();
			}
			return (n != null ? n.floatValue() : null);
		}

		@Override
		public Float getVoltage() {
			Number n = null;
			switch (phase) {
				case PhaseA:
					n = getNumber(AE250TxRegister.InverterVoltageLineNeutralPhaseA);
					break;

				case PhaseB:
					n = getNumber(AE250TxRegister.InverterVoltageLineNeutralPhaseB);
					break;

				case PhaseC:
					n = getNumber(AE250TxRegister.InverterVoltageLineNeutralPhaseC);
					break;

				default:
					return AE250TxData.this.getVoltage();
			}
			return (n != null ? n.floatValue() : null);
		}

		@Override
		public Float getPowerFactor() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return AE250TxData.this.getPowerFactor();
			}
		}

		@Override
		public Integer getActivePower() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return AE250TxData.this.getActivePower();
			}
		}

		@Override
		public Integer getApparentPower() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return AE250TxData.this.getApparentPower();
			}
		}

		@Override
		public Integer getReactivePower() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return AE250TxData.this.getReactivePower();
			}
		}

		@Override
		public Float getPvVoltage() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return AE250TxData.this.getPvVoltage();
			}
		}

		@Override
		public Float getDcCurrent() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return AE250TxData.this.getDcCurrent();
			}
		}

		@Override
		public Float getDcVoltage() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return AE250TxData.this.getDcVoltage();
			}
		}

		@Override
		public Integer getDcPower() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return AE250TxData.this.getDcPower();
			}
		}

		@Override
		public Float getLineVoltage() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return AE250TxData.this.getLineVoltage();
			}
		}

		@Override
		public Long getActiveEnergyDelivered() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return AE250TxData.this.getActiveEnergyDelivered();
			}
		}

		@Override
		public Long getActiveEnergyReceived() {
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
		public Long getReactiveEnergyDelivered() {
			return null;
		}

		@Override
		public Long getReactiveEnergyReceived() {
			return null;
		}

		@Override
		public Map<String, Object> getDeviceInfo() {
			return AE250TxData.this.getDeviceInfo();
		}

		@Override
		public AcEnergyDataAccessor reversed() {
			return AE250TxData.this.reversed();
		}

		@Override
		public String getSerialNumber() {
			return AE250TxData.this.getSerialNumber();
		}

		@Override
		public DeviceOperatingState getDeviceOperatingState() {
			return AE250TxData.this.getDeviceOperatingState();
		}

		@Override
		public Float getNeutralCurrent() {
			switch (phase) {
				case PhaseA:
				case PhaseB:
				case PhaseC:
					return null;

				default:
					return AE250TxData.this.getNeutralCurrent();
			}
		}

		@Override
		public AEInverterType getInverterType() {
			return AE250TxData.this.getInverterType();
		}

		@Override
		public String getIdNumber() {
			return AE250TxData.this.getIdNumber();
		}

		@Override
		public String getFirmwareRevision() {
			return AE250TxData.this.getFirmwareRevision();
		}

		@Override
		public Integer getMapVersion() {
			return AE250TxData.this.getMapVersion();
		}

		@Override
		public AEInverterConfiguration getInverterConfiguration() {
			return AE250TxData.this.getInverterConfiguration();
		}

		@Override
		public Integer getInverterRatedPower() {
			return AE250TxData.this.getInverterRatedPower();
		}

		@Override
		public AE250TxSystemStatus getSystemStatus() {
			return AE250TxData.this.getSystemStatus();
		}

		@Override
		public SortedSet<AE250TxFault> getFaults() {
			return AE250TxData.this.getFaults();
		}

		@Override
		public SortedSet<AE250TxWarning> getWarnings() {
			return AE250TxData.this.getWarnings();
		}

		@Override
		public Set<ModelEvent> getEvents() {
			return AE250TxData.this.getEvents();
		}

		@Override
		public BitSet getVendorEvents() {
			return AE250TxData.this.getVendorEvents();
		}

	}

}
