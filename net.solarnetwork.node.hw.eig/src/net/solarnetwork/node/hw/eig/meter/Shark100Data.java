/* ==================================================================
 * Shark100Data.java - 26/07/2018 2:53:25 PM
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

package net.solarnetwork.node.hw.eig.meter;

import java.util.LinkedHashMap;
import java.util.Map;
import net.solarnetwork.node.domain.ACEnergyDataAccessor;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Data object for the Shark 100 series meter.
 * 
 * @author matt
 * @version 1.0
 */
public class Shark100Data extends ModbusData implements Shark100DataAccessor {

	private static final int MAX_RESULTS = 64;

	/**
	 * Constructor.
	 */
	public Shark100Data() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the meter data to copy
	 */
	public Shark100Data(ModbusData other) {
		super(other);
	}

	@Override
	public ModbusData copy() {
		return new Shark100Data(this);
	}

	/**
	 * Get a snapshot copy of the data.
	 * 
	 * @return a copy of the data
	 * @see Shark100Data#copy()
	 */
	public Shark100Data getSnapshot() {
		return (Shark100Data) copy();
	}

	@Override
	public Map<String, Object> getDeviceInfo() {
		Shark100DataAccessor data = getSnapshot();
		Map<String, Object> result = new LinkedHashMap<>(4);
		String name = data.getName();
		if ( name != null ) {
			String firmwareVersion = data.getFirmwareRevision();
			if ( firmwareVersion != null ) {
				result.put(INFO_KEY_DEVICE_MODEL,
						String.format("%s (firmware %s)", name, firmwareVersion));
			} else {
				result.put(INFO_KEY_DEVICE_MODEL, name);
			}
		}
		SharkPowerSystem wiringMode = data.getPowerSystem();
		if ( wiringMode != null ) {
			result.put("Wiring Mode", wiringMode.getDescription());
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
	 */
	public final void readConfigurationData(final ModbusConnection conn) {
		// we actually read ALL registers here, so our snapshot timestamp includes everything
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				Shark100Register.getRegisterAddressSet(), MAX_RESULTS);
	}

	/**
	 * Read the meter registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 */
	public final void readMeterData(final ModbusConnection conn) {
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				Shark100Register.getMeterRegisterAddressSet(), MAX_RESULTS);
	}

	/**
	 * Get an accessor for a specific phase.
	 * 
	 * <p>
	 * This class implements {@link Shark100DataAccessor} for the {@code Total}
	 * phase. Call this method to get an accessor for a different phase.
	 * </p>
	 * 
	 * @param phase
	 *        the phase to get an accessor for
	 * @return the accessor
	 */
	public Shark100DataAccessor dataAccessorForPhase(ACPhase phase) {
		if ( phase == ACPhase.Total ) {
			return this;
		}
		return new PhaseMeterDataAccessor(phase);
	}

	/**
	 * Get an accessor that reverses the current direction of the data, turning
	 * received into delivered and vice versa.
	 * 
	 * @return the accessor
	 */
	public Shark100DataAccessor reversedDataAccessor() {
		return new ReversedMeterDataAccessor(this);
	}

	@Override
	public ACEnergyDataAccessor accessorForPhase(ACPhase phase) {
		return dataAccessorForPhase(phase);
	}

	@Override
	public ACEnergyDataAccessor reversed() {
		return reversedDataAccessor();
	}

	/**
	 * Get a scaled energy value for a specific register.
	 * 
	 * <p>
	 * Note that the returned value will always be posative, even when the meter
	 * reports the values as negative based on it's directional configuration.
	 * </p>
	 * 
	 * @param reg
	 *        the register to get the energy reading for
	 * @return the value, or {@literal null} if not available
	 */
	private Long getEnergyValue(Shark100Register reg) {
		Number n = getNumber(reg);
		if ( n == null ) {
			return null;
		}
		SharkPowerEnergyFormat pef = getPowerEnergyFormat();
		if ( pef != null ) {
			n = pef.energyValue(n);
		}
		return (n != null ? Math.abs(n.longValue()) : null);
	}

	@Override
	public SharkPowerEnergyFormat getPowerEnergyFormat() {
		Number n = getNumber(Shark100Register.ConfigPowerEnergyFormats);
		return (n != null ? SharkPowerEnergyFormat.forRegisterValue(n.intValue()) : null);
	}

	@Override
	public String getName() {
		return getAsciiString(Shark100Register.InfoMeterName, true);
	}

	@Override
	public String getSerialNumber() {
		return getAsciiString(Shark100Register.InfoSerialNumber, true);
	}

	@Override
	public String getFirmwareRevision() {
		return getAsciiString(Shark100Register.InfoFirmwareVersion, true);
	}

	@Override
	public SharkPowerSystem getPowerSystem() {
		Number n = getNumber(Shark100Register.ConfigPtMultiplierAndPowerSystem);
		SharkPowerSystem m = null;
		if ( n != null ) {
			try {
				m = SharkPowerSystem.forRegisterValue(n.intValue());
			} catch ( IllegalArgumentException e ) {
				// ignore
			}
		}
		return m;
	}

	@Override
	public Float getFrequency() {
		Number v = getNumber(Shark100Register.MeterFrequency);
		return (v != null ? v.floatValue() : null);
	}

	@Override
	public Float getPowerFactor() {
		Number v = getNumber(Shark100Register.MeterPowerFactorTotal);
		return (v != null ? v.floatValue() : null);
	}

	@Override
	public Integer getActivePower() {
		Number v = getNumber(Shark100Register.MeterActivePowerTotal);
		return (v != null ? v.intValue() : null);
	}

	@Override
	public Integer getApparentPower() {
		Number v = getNumber(Shark100Register.MeterApparentPowerTotal);
		return (v != null ? v.intValue() : null);
	}

	@Override
	public Integer getReactivePower() {
		Number v = getNumber(Shark100Register.MeterReactivePowerTotal);
		return (v != null ? v.intValue() : null);
	}

	@Override
	public Float getCurrent() {
		Number a = getNumber(Shark100Register.MeterCurrentPhaseA);
		Number b = getNumber(Shark100Register.MeterCurrentPhaseB);
		Number c = getNumber(Shark100Register.MeterCurrentPhaseC);
		return (a != null && b != null && c != null ? a.floatValue() + b.floatValue() + c.floatValue()
				: null);
	}

	@Override
	public Float getVoltage() {
		Number a = getNumber(Shark100Register.MeterVoltageLineNeutralPhaseA);
		Number b = getNumber(Shark100Register.MeterVoltageLineNeutralPhaseB);
		Number c = getNumber(Shark100Register.MeterVoltageLineNeutralPhaseC);
		return (a != null && b != null && c != null
				? (a.floatValue() + b.floatValue() + c.floatValue()) / 3.0f
				: null);
	}

	@Override
	public Long getActiveEnergyDelivered() {
		return getEnergyValue(Shark100Register.MeterActiveEnergyDelivered);
	}

	@Override
	public Long getActiveEnergyReceived() {
		return getEnergyValue(Shark100Register.MeterActiveEnergyReceived);
	}

	@Override
	public Long getReactiveEnergyDelivered() {
		return getEnergyValue(Shark100Register.MeterReactiveEnergyDelivered);
	}

	@Override
	public Long getReactiveEnergyReceived() {
		return getEnergyValue(Shark100Register.MeterReactiveEnergyReceived);
	}

	@Override
	public Long getApparentEnergyDelivered() {
		return null;
	}

	@Override
	public Long getApparentEnergyReceived() {
		return null;
	}

	private class PhaseMeterDataAccessor implements Shark100DataAccessor {

		private final ACPhase phase;

		private PhaseMeterDataAccessor(ACPhase phase) {
			super();
			this.phase = phase;
		}

		@Override
		public Map<String, Object> getDeviceInfo() {
			return Shark100Data.this.getDeviceInfo();
		}

		@Override
		public String getName() {
			return Shark100Data.this.getName();
		}

		@Override
		public String getSerialNumber() {
			return Shark100Data.this.getSerialNumber();
		}

		@Override
		public String getFirmwareRevision() {
			return Shark100Data.this.getFirmwareRevision();
		}

		@Override
		public SharkPowerSystem getPowerSystem() {
			return Shark100Data.this.getPowerSystem();
		}

		@Override
		public SharkPowerEnergyFormat getPowerEnergyFormat() {
			return Shark100Data.this.getPowerEnergyFormat();
		}

		@Override
		public long getDataTimestamp() {
			return Shark100Data.this.getDataTimestamp();
		}

		@Override
		public ACEnergyDataAccessor accessorForPhase(ACPhase phase) {
			return Shark100Data.this.accessorForPhase(phase);
		}

		@Override
		public ACEnergyDataAccessor reversed() {
			return Shark100Data.this.reversed();
		}

		@Override
		public Float getFrequency() {
			return Shark100Data.this.getFrequency();
		}

		@Override
		public Float getCurrent() {
			Number n = null;
			switch (phase) {
				case PhaseA:
					n = getNumber(Shark100Register.MeterCurrentPhaseA);
					break;

				case PhaseB:
					n = getNumber(Shark100Register.MeterCurrentPhaseB);
					break;

				case PhaseC:
					n = getNumber(Shark100Register.MeterCurrentPhaseC);
					break;

				default:
					return Shark100Data.this.getCurrent();
			}
			return (n != null ? n.floatValue() : null);
		}

		@Override
		public Float getVoltage() {
			Number n = null;
			switch (phase) {
				case PhaseA:
					n = getNumber(Shark100Register.MeterVoltageLineNeutralPhaseA);
					break;

				case PhaseB:
					n = getNumber(Shark100Register.MeterVoltageLineNeutralPhaseB);
					break;

				case PhaseC:
					n = getNumber(Shark100Register.MeterVoltageLineNeutralPhaseC);
					break;

				default:
					return Shark100Data.this.getVoltage();
			}
			return (n != null ? n.floatValue() : null);
		}

		@Override
		public Float getPowerFactor() {
			return Shark100Data.this.getPowerFactor();
		}

		@Override
		public Integer getActivePower() {
			return Shark100Data.this.getActivePower();
		}

		@Override
		public Integer getApparentPower() {
			return Shark100Data.this.getApparentPower();
		}

		@Override
		public Integer getReactivePower() {
			return Shark100Data.this.getReactivePower();
		}

		@Override
		public Long getActiveEnergyDelivered() {
			return Shark100Data.this.getActiveEnergyDelivered();
		}

		@Override
		public Long getActiveEnergyReceived() {
			return Shark100Data.this.getActiveEnergyReceived();
		}

		@Override
		public Long getReactiveEnergyDelivered() {
			return Shark100Data.this.getReactiveEnergyDelivered();
		}

		@Override
		public Long getReactiveEnergyReceived() {
			return Shark100Data.this.getReactiveEnergyReceived();
		}

		@Override
		public Long getApparentEnergyDelivered() {
			return Shark100Data.this.getApparentEnergyDelivered();
		}

		@Override
		public Long getApparentEnergyReceived() {
			return Shark100Data.this.getActiveEnergyReceived();
		}

	}

	private static class ReversedMeterDataAccessor implements Shark100DataAccessor {

		private final Shark100DataAccessor delegate;

		private ReversedMeterDataAccessor(Shark100DataAccessor delegate) {
			super();
			this.delegate = delegate;
		}

		@Override
		public Map<String, Object> getDeviceInfo() {
			return delegate.getDeviceInfo();
		}

		@Override
		public String getName() {
			return delegate.getName();
		}

		@Override
		public ACEnergyDataAccessor accessorForPhase(ACPhase phase) {
			return delegate.accessorForPhase(phase);
		}

		@Override
		public String getSerialNumber() {
			return delegate.getSerialNumber();
		}

		@Override
		public String getFirmwareRevision() {
			return delegate.getFirmwareRevision();
		}

		@Override
		public ACEnergyDataAccessor reversed() {
			return delegate;
		}

		@Override
		public SharkPowerSystem getPowerSystem() {
			return delegate.getPowerSystem();
		}

		@Override
		public long getDataTimestamp() {
			return delegate.getDataTimestamp();
		}

		@Override
		public SharkPowerEnergyFormat getPowerEnergyFormat() {
			return delegate.getPowerEnergyFormat();
		}

		@Override
		public Float getFrequency() {
			return delegate.getFrequency();
		}

		@Override
		public Float getCurrent() {
			return delegate.getCurrent();
		}

		@Override
		public Float getVoltage() {
			return delegate.getVoltage();
		}

		@Override
		public Float getPowerFactor() {
			return delegate.getPowerFactor();
		}

		@Override
		public Integer getActivePower() {
			Integer n = delegate.getActivePower();
			return (n != null ? n * -1 : null);
		}

		@Override
		public Integer getApparentPower() {
			return delegate.getApparentPower();
		}

		@Override
		public Integer getReactivePower() {
			Integer n = delegate.getReactivePower();
			return (n != null ? n * -1 : null);
		}

		@Override
		public Long getActiveEnergyDelivered() {
			return delegate.getActiveEnergyReceived();
		}

		@Override
		public Long getActiveEnergyReceived() {
			return delegate.getActiveEnergyDelivered();
		}

		@Override
		public Long getReactiveEnergyDelivered() {
			return delegate.getReactiveEnergyReceived();
		}

		@Override
		public Long getReactiveEnergyReceived() {
			return delegate.getReactiveEnergyDelivered();
		}

		@Override
		public Long getApparentEnergyDelivered() {
			return delegate.getApparentEnergyReceived();
		}

		@Override
		public Long getApparentEnergyReceived() {
			return delegate.getApparentEnergyDelivered();
		}

	}
}
