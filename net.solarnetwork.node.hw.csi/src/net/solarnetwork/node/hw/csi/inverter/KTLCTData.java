/* ==================================================================
 * KTLCTData.java - 22 Nov 2017 12:28:46
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.csi.inverter;

import static net.solarnetwork.util.StringUtils.commaDelimitedStringFromCollection;
import java.io.IOException;
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
import net.solarnetwork.node.io.modbus.ModbusWriteFunction;
import net.solarnetwork.util.NumberUtils;

/**
 * Implementation for accessing SI-60KTL-CT data.
 * 
 * @author maxieduncan
 * @author matt
 * @version 2.1
 */
public class KTLCTData extends ModbusData implements KTLCTDataAccessor {

	/**
	 * The value of the {@link KTLCTRegister#ControlDevicePowerSwitch} register
	 * representing <i>on</i>.
	 * 
	 * @since 1.4
	 */
	public static final int POWER_SWITCH_ON = 0xAAAA;

	/**
	 * The value of the {@link KTLCTRegister#ControlDevicePowerSwitch} register
	 * representing <i>off</i>.
	 * 
	 * @since 1.4
	 */
	public static final int POWER_SWITCH_OFF = 0x5555;

	private static final int MAX_RESULTS = 64;

	/**
	 * Default constructor.
	 */
	public KTLCTData() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the data to copy
	 */
	public KTLCTData(ModbusData other) {
		super(other);
	}

	/**
	 * Get a snapshot (copy).
	 * 
	 * @return the copy
	 */
	public KTLCTData getSnapshot() {
		return new KTLCTData(this);
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
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public final void readConfigurationData(final ModbusConnection conn) throws IOException {
		// we actually read ALL registers here, so our snapshot timestamp includes everything
		refreshData(conn, ModbusReadFunction.ReadInputRegister, KTLCTRegister.getRegisterAddressSet(),
				MAX_RESULTS);
		readControlData(conn);
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
		refreshData(conn, ModbusReadFunction.ReadInputRegister,
				KTLCTRegister.getInverterRegisterAddressSet(), MAX_RESULTS);
	}

	/**
	 * Read the control registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 * @throws IOException
	 *         if any communication error occurs
	 * @since 1.4
	 */
	public final void readControlData(final ModbusConnection conn) throws IOException {
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				KTLCTRegister.getControlRegisterAddressSet(), MAX_RESULTS);
	}

	@Override
	public Map<String, Object> getDeviceInfo() {
		KTLCTDataAccessor data = getSnapshot();
		Map<String, Object> result = new LinkedHashMap<>(4);
		String model = data.getModelName();
		if ( model != null ) {
			KTLCTInverterType type = data.getInverterType();
			if ( type != null ) {
				result.put(DataAccessor.INFO_KEY_DEVICE_MODEL,
						String.format("%s (%s)", model, type.getDescription()));
			} else {
				result.put(DataAccessor.INFO_KEY_DEVICE_MODEL, model);
			}
		}
		KTLCTFirmwareVersion version = data.getFirmwareVersion();
		if ( version != null ) {
			result.put(DataAccessor.INFO_KEY_DEVICE_VERSION, String.format("DSP = %d, MCU = %d",
					version.getDspVersion(), version.getMcuVersion()));
		}
		String s = data.getSerialNumber();
		if ( s != null ) {
			result.put(DataAccessor.INFO_KEY_DEVICE_SERIAL_NUMBER, s);
		}
		Set<KTLCTWarn> warns = data.getWarnings();
		if ( warns != null && !warns.isEmpty() ) {
			result.put("Warnings", commaDelimitedStringFromCollection(warns));
		}
		Set<KTLCTPermanentFault> permFaults = data.getPermanentFaults();
		if ( permFaults != null && !permFaults.isEmpty() ) {
			result.put("Permanent Faults", commaDelimitedStringFromCollection(permFaults));
		}
		Set<KTLCTFault0> faults0 = data.getFaults0();
		if ( faults0 != null && !faults0.isEmpty() ) {
			result.put("Faults 0", commaDelimitedStringFromCollection(faults0));
		}
		Set<KTLCTFault1> faults1 = data.getFaults1();
		if ( faults1 != null && !faults1.isEmpty() ) {
			result.put("Faults 1", commaDelimitedStringFromCollection(faults1));
		}
		Set<KTLCTFault2> faults2 = data.getFaults2();
		if ( faults2 != null && !faults2.isEmpty() ) {
			result.put("Faults 2", commaDelimitedStringFromCollection(faults2));
		}
		return result;
	}

	private Float getCentiValueAsFloat(ModbusReference ref) {
		Number n = getNumber(ref);
		if ( n == null ) {
			return null;
		}
		return n.floatValue() / 10;
	}

	private Integer getHectoValueAsInteger(ModbusReference ref) {
		Number n = getNumber(ref);
		if ( n == null ) {
			return null;
		}
		return n.intValue() * 100;

	}

	@Override
	public Integer getActivePower() {
		return getHectoValueAsInteger(KTLCTRegister.InverterActivePowerTotal);
	}

	@Override
	public Integer getApparentPower() {
		return getHectoValueAsInteger(KTLCTRegister.InverterApparentPowerTotal);
	}

	@Override
	public Float getFrequency() {
		return getCentiValueAsFloat(KTLCTRegister.InverterFrequency);
	}

	@Override
	public Float getPv1Voltage() {
		return getCentiValueAsFloat(KTLCTRegister.InverterPv1Voltage);
	}

	@Override
	public Float getPv1Current() {
		return getCentiValueAsFloat(KTLCTRegister.InverterPv1Current);
	}

	@Override
	public Float getPv2Voltage() {
		return getCentiValueAsFloat(KTLCTRegister.InverterPv2Voltage);
	}

	@Override
	public Float getPv2Current() {
		return getCentiValueAsFloat(KTLCTRegister.InverterPv2Current);
	}

	@Override
	public Float getPv3Voltage() {
		return getCentiValueAsFloat(KTLCTRegister.InverterPv3Voltage);
	}

	@Override
	public Float getPv3Current() {
		return getCentiValueAsFloat(KTLCTRegister.InverterPv3Current);
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
	public Float getCurrent() {
		Number a = getNumber(KTLCTRegister.InverterCurrentPhaseA);
		Number b = getNumber(KTLCTRegister.InverterCurrentPhaseB);
		Number c = getNumber(KTLCTRegister.InverterCurrentPhaseC);
		return (a != null && b != null && c != null
				? (a.floatValue() + b.floatValue() + c.floatValue()) / 10.0f
				: null);
	}

	@Override
	public Float getNeutralCurrent() {
		return null;
	}

	@Override
	public Float getVoltage() {
		Number a = getNumber(KTLCTRegister.InverterVoltagePhaseA);
		Number b = getNumber(KTLCTRegister.InverterVoltagePhaseB);
		Number c = getNumber(KTLCTRegister.InverterVoltagePhaseC);
		return (a != null && b != null && c != null
				? (a.floatValue() + b.floatValue() + c.floatValue()) / 30.0f
				: null);
	}

	@Override
	public Float getLineVoltage() {
		Number a = getNumber(KTLCTRegister.InverterVoltageLineLinePhaseAPhaseB);
		Number b = getNumber(KTLCTRegister.InverterVoltageLineLinePhaseBPhaseC);
		Number c = getNumber(KTLCTRegister.InverterVoltageLineLinePhaseCPhaseA);
		return (a != null && b != null && c != null
				? (a.floatValue() + b.floatValue() + c.floatValue()) / 30.0f
				: null);
	}

	@Override
	public Float getPowerFactor() {
		Number n = getNumber(KTLCTRegister.InverterPowerFactor);
		return (n != null ? n.floatValue() / 1000.0f : null);
	}

	@Override
	public Long getActiveEnergyDelivered() {
		Number n = getNumber(KTLCTRegister.InverterActiveEnergyDelivered);
		return (n != null ? n.longValue() * 1000L : null);
	}

	@Override
	public Long getActiveEnergyDeliveredToday() {
		Number n = getNumber(KTLCTRegister.InverterActiveEnergyDeliveredToday);
		return (n != null ? n.longValue() * 100L : null);
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
	public Integer getReactivePower() {
		return getHectoValueAsInteger(KTLCTRegister.InverterReactivePowerTotal);
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
	public Float getDcCurrent() {
		float total = 0;
		Float f = getPv1Current();
		if ( f != null ) {
			total += f.floatValue();
		}
		Float f2 = getPv1Current();
		if ( f2 != null ) {
			total += f2.floatValue();
		}
		Float f3 = getPv1Current();
		if ( f3 != null ) {
			total += f3.floatValue();
		}
		return total;
	}

	@Override
	public Float getDcVoltage() {
		int count = 0;
		float total = 0;
		Float f = getPv1Voltage();
		if ( f != null ) {
			total += f.floatValue();
			count++;
		}
		Float f2 = getPv2Voltage();
		if ( f2 != null ) {
			total += f2.floatValue();
			count++;
		}
		Float f3 = getPv3Voltage();
		if ( f3 != null ) {
			total += f3.floatValue();
			count++;
		}
		return (count > 0 ? total / count : null);
	}

	@Override
	public Integer getDcPower() {
		return Math.round(getPv1Voltage() * getPv1Current());
	}

	@Override
	public KTLCTInverterType getInverterType() {
		Number n = getNumber(KTLCTRegister.InfoInverterModel);
		if ( n == null ) {
			return null;
		}
		try {
			return KTLCTInverterType.forCode(n.intValue());
		} catch ( IllegalArgumentException e ) {
			return null;
		}
	}

	@Override
	public KTLCTInverterWorkMode getWorkMode() {
		Number n = getNumber(KTLCTRegister.StatusMode);
		if ( n == null ) {
			return null;
		}
		try {
			return KTLCTInverterWorkMode.forCode(n.intValue());
		} catch ( IllegalArgumentException e ) {
			return null;
		}
	}

	private String getNullTerminatedString(String s) {
		if ( s == null || s.isEmpty() ) {
			return s;
		}
		char[] c = s.toCharArray();
		for ( int i = 0; i < c.length; i++ ) {
			if ( c[i] == 0 ) {
				return s.substring(0, i);
			}
		}
		return s;
	}

	@Override
	public String getModelName() {
		return getNullTerminatedString(getAsciiString(KTLCTRegister.InfoInverterModelName, true));
	}

	@Override
	public String getSerialNumber() {
		Number n = getNumber(KTLCTRegister.InfoSerialNumber);
		return (n != null ? Long.toString(n.longValue(), 16) : null);
	}

	@Override
	public KTLCTFirmwareVersion getFirmwareVersion() {
		Number n = getNumber(KTLCTRegister.InfoFirmwareVersion);
		return (n != null ? KTLCTFirmwareVersion.forCode(n.intValue()) : null);
	}

	@Override
	public Set<KTLCTWarn> getWarnings() {
		Number n = getNumber(KTLCTRegister.StatusWarn);
		return (n != null ? Bitmaskable.setForBitmask(n.intValue(), KTLCTWarn.class) : null);
	}

	@Override
	public Set<KTLCTFault0> getFaults0() {
		Number n = getNumber(KTLCTRegister.StatusFault0);
		return (n != null ? Bitmaskable.setForBitmask(n.intValue(), KTLCTFault0.class) : null);
	}

	@Override
	public Set<KTLCTFault1> getFaults1() {
		Number n = getNumber(KTLCTRegister.StatusFault1);
		return (n != null ? Bitmaskable.setForBitmask(n.intValue(), KTLCTFault1.class) : null);
	}

	@Override
	public Set<KTLCTFault2> getFaults2() {
		Number n = getNumber(KTLCTRegister.StatusFault2);
		return (n != null ? Bitmaskable.setForBitmask(n.intValue(), KTLCTFault2.class) : null);
	}

	@Override
	public Set<KTLCTPermanentFault> getPermanentFaults() {
		Number n = getNumber(KTLCTRegister.StatusPermanentFault);
		return (n != null ? Bitmaskable.setForBitmask(n.intValue(), KTLCTPermanentFault.class) : null);
	}

	@Override
	public SortedSet<KTLCTFault> getFaults() {
		SortedSet<KTLCTFault> result = new TreeSet<>(GroupedBitmaskable.SORT_BY_OVERALL_INDEX);
		Set<KTLCTFault0> faults0 = getFaults0();
		if ( faults0 != null ) {
			result.addAll(faults0);
		}
		Set<KTLCTFault1> faults1 = getFaults1();
		if ( faults1 != null ) {
			result.addAll(faults1);
		}
		Set<KTLCTFault2> faults2 = getFaults2();
		if ( faults2 != null ) {
			result.addAll(faults2);
		}
		return result;
	}

	@Override
	public Float getModuleTemperature() {
		return getCentiValueAsFloat(KTLCTRegister.InverterModuleTemperature);
	}

	@Override
	public Float getInternalTemperature() {
		return getCentiValueAsFloat(KTLCTRegister.InverterInternalTemperature);
	}

	@Override
	public Float getTransformerTemperature() {
		return getCentiValueAsFloat(KTLCTRegister.InverterTransformerTemperature);
	}

	@Override
	public DeviceOperatingState getDeviceOperatingState() {
		Number n = getNumber(KTLCTRegister.ControlDevicePowerSwitch);
		if ( n != null && n.intValue() == POWER_SWITCH_OFF ) {
			return DeviceOperatingState.Shutdown;
		}
		KTLCTInverterWorkMode mode = getWorkMode();
		return (mode != null ? mode.asDeviceOperatingState() : DeviceOperatingState.Unknown);
	}

	/**
	 * Set the device operating state.
	 * 
	 * <p>
	 * This modifies the {@link KTLCTRegister#ControlDevicePowerSwitch}
	 * register. This method supports setting the state to either
	 * {@link DeviceOperatingState#Shutdown} or
	 * {@link DeviceOperatingState#Normal}. This instance's sample data will
	 * also be updated to reflect the value set on the device, if successful.
	 * </p>
	 * 
	 * @param conn
	 *        the modbus connection to use
	 * @param state
	 *        the desired state
	 * @throws IOException
	 *         if any communication error occurs
	 * @since 1.4
	 */
	public void setDeviceOperatingState(ModbusConnection conn, DeviceOperatingState state)
			throws IOException {
		final DeviceOperatingState currState = getDeviceOperatingState();
		final int powerSwitchAddr = KTLCTRegister.ControlDevicePowerSwitch.getAddress();
		final Integer update;
		if ( state == DeviceOperatingState.Shutdown && currState != DeviceOperatingState.Shutdown ) {
			// turn off
			update = KTLCTData.POWER_SWITCH_OFF;

		} else if ( state != DeviceOperatingState.Shutdown && (currState == DeviceOperatingState.Shutdown
				|| currState == DeviceOperatingState.Unknown) ) {
			// turn on
			update = KTLCTData.POWER_SWITCH_ON;
		} else {
			update = null;
		}
		if ( update != null ) {
			conn.writeWords(ModbusWriteFunction.WriteHoldingRegister, powerSwitchAddr,
					new int[] { update });
			performUpdates(new ModbusDataUpdateAction() {

				@Override
				public boolean updateModbusData(MutableModbusData m) {
					m.saveDataArray(new int[] { update }, powerSwitchAddr);
					return true;
				}
			});
		}
	}

	@Override
	public Float getOutputPowerLimitPercent() {
		return getCentiValueAsFloat(KTLCTRegister.ControlDevicePowerLimit);
	}

	/**
	 * Set the device output power limit.
	 * 
	 * <p>
	 * This modifies the {@link KTLCTRegister#ControlDevicePowerLimit} register.
	 * This instance's sample data will also be updated to reflect the value set
	 * on the device, if successful.
	 * </p>
	 * 
	 * @param conn
	 *        the modbus connection to use
	 * @param percent
	 *        the desired output power limit, as a percentage from 0 - 1
	 * @throws IOException
	 *         if any communication error occurs
	 * @since 1.4
	 */
	public void setOutputPowerLimitPercent(ModbusConnection conn, Float percent) throws IOException {
		final int update = (percent != null ? (int) (percent.floatValue() * 1000) : 1000);
		final int powerLimitAddr = KTLCTRegister.ControlDevicePowerLimit.getAddress();
		conn.writeWords(ModbusWriteFunction.WriteHoldingRegister, powerLimitAddr, new int[] { update });
		performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(new int[] { update }, powerLimitAddr);
				return true;
			}
		});
	}

	@Override
	public Float getEfficiency() {
		Number n = getNumber(KTLCTRegister.InverterEfficiency);
		n = NumberUtils.scaled(n, -4);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Set<ModelEvent> getEvents() {
		SortedSet<KTLCTFault> faults = getFaults();
		Set<ModelEvent> events = new LinkedHashSet<>(16);
		if ( !faults.isEmpty() ) {
			if ( faults.contains(KTLCTFault0.PVVoltageOver) ) {
				events.add(InverterModelEvent.DcOverVoltage);
			}
			if ( faults.contains(KTLCTFault0.GridVoltageOutsideLimit07)
					|| faults.contains(KTLCTFault0.GridVoltageOutsideLimit08)
					|| faults.contains(KTLCTFault1.GridVoltageOutsideLimit09) ) {
				events.add(InverterModelEvent.AcOverVoltage);
			}
			if ( faults.contains(KTLCTFault0.GridFrequencyOutsideLimit) ) {
				events.add(InverterModelEvent.OverFrequency);
			}
			if ( faults.contains(KTLCTFault0.GridVoltageOutsideLimit11) ) {
				events.add(InverterModelEvent.UnderFrequency);
			}
			if ( faults.contains(KTLCTFault0.Protect0020) ) {
				events.add(InverterModelEvent.AcDisconnect);
			}
			if ( faults.contains(KTLCTFault0.TempOver) ) {
				events.add(InverterModelEvent.OverTemperature);
			}
			if ( faults.contains(KTLCTFault1.Protect0110) ) {
				events.add(InverterModelEvent.DcOverVoltage);
			}
		}
		if ( faults.contains(KTLCTFault2.Protect0210) ) {
			events.add(InverterModelEvent.HwTestFailure);
		}
		if ( faults.contains(KTLCTFault2.PV1VoltageOver) || faults.contains(KTLCTFault2.PV2VoltageOver)
				|| faults.contains(KTLCTFault2.PV3VoltageOver) ) {
			events.add(InverterModelEvent.DcOverVoltage);
		}
		if ( faults.contains(KTLCTFault2.EmergencyStp) ) {
			events.add(InverterModelEvent.ManualShutdown);
		}
		return events;
	}

	@Override
	public BitSet getVendorEvents() {
		return GroupedBitmaskable.overallBitmaskValue(getFaults());
	}

	private class PhaseDataAccessor implements KTLCTDataAccessor {

		private final AcPhase phase;

		private PhaseDataAccessor(AcPhase phase) {
			super();
			this.phase = phase;
		}

		@Override
		public Instant getDataTimestamp() {
			return KTLCTData.this.getDataTimestamp();
		}

		@Override
		public AcEnergyDataAccessor accessorForPhase(AcPhase phase) {
			return KTLCTData.this.accessorForPhase(phase);
		}

		@Override
		public Float getFrequency() {
			return KTLCTData.this.getFrequency();
		}

		@Override
		public Float getCurrent() {
			switch (phase) {
				case PhaseA:
					return getCentiValueAsFloat(KTLCTRegister.InverterCurrentPhaseA);

				case PhaseB:
					return getCentiValueAsFloat(KTLCTRegister.InverterCurrentPhaseB);

				case PhaseC:
					return getCentiValueAsFloat(KTLCTRegister.InverterCurrentPhaseC);

				default:
					return KTLCTData.this.getCurrent();
			}
		}

		@Override
		public Float getVoltage() {
			switch (phase) {
				case PhaseA:
					return getCentiValueAsFloat(KTLCTRegister.InverterVoltagePhaseA);

				case PhaseB:
					return getCentiValueAsFloat(KTLCTRegister.InverterVoltagePhaseB);

				case PhaseC:
					return getCentiValueAsFloat(KTLCTRegister.InverterVoltagePhaseC);

				default:
					return KTLCTData.this.getVoltage();
			}
		}

		@Override
		public Float getPowerFactor() {
			Number n;
			switch (phase) {
				case PhaseA:
					n = getNumber(KTLCTRegister.InverterPowerFactorPhaseA);
					return (n != null ? n.floatValue() / 100.0f : null);

				case PhaseB:
					n = getNumber(KTLCTRegister.InverterPowerFactorPhaseB);
					return (n != null ? n.floatValue() / 100.0f : null);

				case PhaseC:
					n = getNumber(KTLCTRegister.InverterPowerFactorPhaseC);
					return (n != null ? n.floatValue() / 100.0f : null);

				default:
					return KTLCTData.this.getPowerFactor();
			}
		}

		@Override
		public Integer getActivePower() {
			switch (phase) {
				case PhaseA:
					return getHectoValueAsInteger(KTLCTRegister.InverterActivePowerPhaseA);

				case PhaseB:
					return getHectoValueAsInteger(KTLCTRegister.InverterActivePowerPhaseB);

				case PhaseC:
					return getHectoValueAsInteger(KTLCTRegister.InverterActivePowerPhaseC);

				default:
					return KTLCTData.this.getActivePower();
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
					return KTLCTData.this.getApparentPower();
			}
		}

		@Override
		public Integer getReactivePower() {
			switch (phase) {
				case PhaseA:
					return getHectoValueAsInteger(KTLCTRegister.InverterReactivePowerPhaseA);

				case PhaseB:
					return getHectoValueAsInteger(KTLCTRegister.InverterReactivePowerPhaseB);

				case PhaseC:
					return getHectoValueAsInteger(KTLCTRegister.InverterReactivePowerPhaseC);

				default:
					return KTLCTData.this.getReactivePower();
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
					return KTLCTData.this.getDcCurrent();
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
					return KTLCTData.this.getDcVoltage();
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
					return KTLCTData.this.getDcPower();
			}
		}

		@Override
		public Float getTransformerTemperature() {
			return KTLCTData.this.getTransformerTemperature();
		}

		@Override
		public Float getNeutralCurrent() {
			return KTLCTData.this.getNeutralCurrent();
		}

		@Override
		public Float getLineVoltage() {
			switch (phase) {
				case PhaseA:
					return getCentiValueAsFloat(KTLCTRegister.InverterVoltageLineLinePhaseAPhaseB);

				case PhaseB:
					return getCentiValueAsFloat(KTLCTRegister.InverterVoltageLineLinePhaseBPhaseC);

				case PhaseC:
					return getCentiValueAsFloat(KTLCTRegister.InverterVoltageLineLinePhaseCPhaseA);

				default:
					return KTLCTData.this.getLineVoltage();
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
					return KTLCTData.this.getActiveEnergyDelivered();
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
			return KTLCTData.this.getDeviceInfo();
		}

		@Override
		public AcEnergyDataAccessor reversed() {
			return KTLCTData.this.reversed();
		}

		@Override
		public KTLCTInverterType getInverterType() {
			return KTLCTData.this.getInverterType();
		}

		@Override
		public KTLCTInverterWorkMode getWorkMode() {
			return KTLCTData.this.getWorkMode();
		}

		@Override
		public Set<KTLCTWarn> getWarnings() {
			return KTLCTData.this.getWarnings();
		}

		@Override
		public SortedSet<KTLCTFault> getFaults() {
			return KTLCTData.this.getFaults();
		}

		@Override
		public Set<KTLCTFault0> getFaults0() {
			return KTLCTData.this.getFaults0();
		}

		@Override
		public Set<KTLCTFault1> getFaults1() {
			return KTLCTData.this.getFaults1();
		}

		@Override
		public Set<KTLCTFault2> getFaults2() {
			return KTLCTData.this.getFaults2();
		}

		@Override
		public Set<KTLCTPermanentFault> getPermanentFaults() {
			return KTLCTData.this.getPermanentFaults();
		}

		@Override
		public String getModelName() {
			return KTLCTData.this.getModelName();
		}

		@Override
		public String getSerialNumber() {
			return KTLCTData.this.getSerialNumber();
		}

		@Override
		public KTLCTFirmwareVersion getFirmwareVersion() {
			return KTLCTData.this.getFirmwareVersion();
		}

		@Override
		public Float getModuleTemperature() {
			return KTLCTData.this.getModuleTemperature();
		}

		@Override
		public Float getInternalTemperature() {
			return KTLCTData.this.getInternalTemperature();
		}

		@Override
		public Long getActiveEnergyDeliveredToday() {
			return KTLCTData.this.getActiveEnergyDeliveredToday();
		}

		@Override
		public Float getPv1Voltage() {
			return KTLCTData.this.getPv1Voltage();
		}

		@Override
		public Float getPv1Current() {
			return KTLCTData.this.getPv1Current();
		}

		@Override
		public Float getPv2Voltage() {
			return KTLCTData.this.getPv2Voltage();
		}

		@Override
		public Float getPv2Current() {
			return KTLCTData.this.getPv2Current();
		}

		@Override
		public Float getPv3Voltage() {
			return KTLCTData.this.getPv3Voltage();
		}

		@Override
		public Float getPv3Current() {
			return KTLCTData.this.getPv3Current();
		}

		@Override
		public DeviceOperatingState getDeviceOperatingState() {
			return KTLCTData.this.getDeviceOperatingState();
		}

		@Override
		public Float getOutputPowerLimitPercent() {
			return KTLCTData.this.getOutputPowerLimitPercent();
		}

		@Override
		public Float getEfficiency() {
			return KTLCTData.this.getEfficiency();
		}

		@Override
		public Set<ModelEvent> getEvents() {
			return KTLCTData.this.getEvents();
		}

		@Override
		public BitSet getVendorEvents() {
			return KTLCTData.this.getVendorEvents();
		}

	}

}
