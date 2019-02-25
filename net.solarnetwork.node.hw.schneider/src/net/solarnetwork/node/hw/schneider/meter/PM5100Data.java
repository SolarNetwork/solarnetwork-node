/* ==================================================================
 * PM5100Data.java - 17/05/2018 3:13:25 PM
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

package net.solarnetwork.node.hw.schneider.meter;

import static net.solarnetwork.node.io.modbus.IntRangeSetUtils.combineToReduceSize;
import java.util.Map;
import bak.pcj.set.IntRange;
import bak.pcj.set.IntRangeSet;
import net.solarnetwork.node.domain.ACEnergyDataAccessor;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Data object for the PM5100 series meter.
 * 
 * @author matt
 * @version 1.1
 * @since 2.4
 */
public class PM5100Data extends ModbusData implements PM5100DataAccessor {

	private static final int MAX_RESULTS = 64;

	/**
	 * Constructor.
	 */
	public PM5100Data() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the meter data to copy
	 */
	public PM5100Data(ModbusData other) {
		super(other);
	}

	@Override
	public ModbusData copy() {
		return new PM5100Data(this);
	}

	/**
	 * Get a snapshot copy of the data.
	 * 
	 * @return a copy of the data
	 * @see ION6200Data#copy()
	 */
	public PM5100Data getSnapshot() {
		return (PM5100Data) copy();
	}

	/**
	 * Read the configuration and information registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 */
	public final void readConfigurationData(final ModbusConnection conn) {
		performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				// we actually read ALL registers here, so our snapshot timestamp includes everything
				updateData(conn, m,
						combineToReduceSize(PM5100Register.getRegisterAddressSet(), MAX_RESULTS));
				return true;
			}
		});
	}

	/**
	 * Read the meter registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 */
	public final void readMeterData(final ModbusConnection conn) {
		performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				updateData(conn, m,
						combineToReduceSize(PM5100Register.getMeterRegisterAddressSet(), MAX_RESULTS));
				return true;
			}
		});
	}

	private void updateData(ModbusConnection conn, MutableModbusData m, IntRangeSet rangeSet) {
		IntRange[] ranges = rangeSet.ranges();
		for ( IntRange r : ranges ) {
			int[] data = conn.readUnsignedShorts(ModbusReadFunction.ReadHoldingRegister, r.first(),
					r.length());
			m.saveDataArray(data, r.first());
		}
	}

	/**
	 * Get an accessor for a specific phase.
	 * 
	 * <p>
	 * This class implements {@link ION6200DataAccessor} for the {@code Total}
	 * phase. Call this method to get an accessor for a different phase.
	 * </p>
	 * 
	 * @param phase
	 *        the phase to get an accessor for
	 * @return the accessor
	 */
	public PM5100DataAccessor dataAccessorForPhase(ACPhase phase) {
		if ( phase == ACPhase.Total ) {
			return this;
		}
		return new PhaseMeterDataAccessor(phase);
	}

	@Override
	public ACEnergyDataAccessor accessorForPhase(ACPhase phase) {
		return dataAccessorForPhase(phase);
	}

	@Override
	public ACEnergyDataAccessor reversed() {
		return new ReversedMeterDataAccessor(this);
	}

	public Integer getPowerValue(PM5100Register reg) {
		Number n = getNumber(reg);
		return (n != null ? Math.round(n.floatValue() * 1000.0f) : null);
	}

	private Float getCurrentValue(PM5100Register reg) {
		Number n = getNumber(reg);
		return (n != null ? n.floatValue() : null);
	}

	private Float getVoltageValue(PM5100Register reg) {
		Number n = getNumber(reg);
		return (n != null ? n.floatValue() : null);
	}

	private Long getEnergyValue(PM5100Register reg) {
		Number n = getNumber(reg);
		return (n != null ? n.longValue() : null);
	}

	@Override
	public Long getSerialNumber() {
		Number n = getNumber(PM5100Register.InfoSerialNumber);
		return (n != null ? n.longValue() : null);
	}

	@Override
	public String getFirmwareRevision() {
		Number major = getNumber(PM5100Register.InfoFirmwareRevisionMajor);
		Number minor = getNumber(PM5100Register.InfoFirmwareRevisionMinor);
		Number patch = getNumber(PM5100Register.InfoFirmwareRevisionPatch);
		return (major != null && minor != null && patch != null
				? String.format("%d.%d.%d", major, minor, patch)
				: null);
	}

	@Override
	public PM5100Model getModel() {
		Number n = getNumber(PM5100Register.InfoModel);
		PM5100Model m = null;
		if ( n != null ) {
			try {
				m = PM5100Model.forCode(n.intValue());
			} catch ( IllegalArgumentException e ) {
				// ignore
			}
		}
		return m;
	}

	@Override
	public PM5100PowerSystem getPowerSystem() {
		Number n = getNumber(PM5100Register.ConfigPowerSystem);
		PM5100PowerSystem m = null;
		if ( n != null ) {
			try {
				m = PM5100PowerSystem.forCode(n.intValue());
			} catch ( IllegalArgumentException e ) {
				// ignore
			}
		}
		return m;
	}

	@Override
	public Integer getPhaseCount() {
		return getInt16(PM5100Register.ConfigNumPhases.getAddress());
	}

	@Override
	public Integer getWireCount() {
		return getInt16(PM5100Register.ConfigNumWires.getAddress());
	}

	@Override
	public Float getFrequency() {
		Float v = getFloat32(PM5100Register.MeterFrequency.getAddress());
		return (v != null ? v.floatValue() : null);
	}

	@Override
	public Float getPowerFactor() {
		Number v = getNumber(PM5100Register.MeterPowerFactorTotal);
		return (v != null ? v.floatValue() : null);
	}

	@Override
	public Integer getActivePower() {
		return getPowerValue(PM5100Register.MeterActivePowerTotal);
	}

	@Override
	public Integer getApparentPower() {
		return getPowerValue(PM5100Register.MeterApparentPowerTotal);
	}

	@Override
	public Integer getReactivePower() {
		return getPowerValue(PM5100Register.MeterReactivePowerTotal);
	}

	@Override
	public Float getCurrent() {
		return getCurrentValue(PM5100Register.MeterCurrentAverage);
	}

	@Override
	public Float getVoltage() {
		return getVoltageValue(PM5100Register.MeterVoltageLineNeutralAverage);
	}

	@Override
	public Float getLineVoltage() {
		return getVoltageValue(PM5100Register.MeterVoltageLineLineAverage);
	}

	@Override
	public Long getActiveEnergyDelivered() {
		return getEnergyValue(PM5100Register.MeterActiveEnergyDelivered);
	}

	@Override
	public Long getActiveEnergyReceived() {
		return getEnergyValue(PM5100Register.MeterActiveEnergyReceived);
	}

	@Override
	public Long getReactiveEnergyDelivered() {
		return getEnergyValue(PM5100Register.MeterReactiveEnergyDelivered);
	}

	@Override
	public Long getReactiveEnergyReceived() {
		return getEnergyValue(PM5100Register.MeterReactiveEnergyReceived);
	}

	@Override
	public Long getApparentEnergyDelivered() {
		return getEnergyValue(PM5100Register.MeterApparentEnergyDelivered);
	}

	@Override
	public Long getApparentEnergyReceived() {
		return getEnergyValue(PM5100Register.MeterApparentEnergyReceived);
	}

	private class PhaseMeterDataAccessor implements PM5100DataAccessor {

		private final ACPhase phase;

		private PhaseMeterDataAccessor(ACPhase phase) {
			super();
			this.phase = phase;
		}

		@Override
		public Map<String, Object> getDeviceInfo() {
			return PM5100Data.this.getDeviceInfo();
		}

		@Override
		public Long getSerialNumber() {
			return PM5100Data.this.getSerialNumber();
		}

		@Override
		public PM5100Model getModel() {
			return PM5100Data.this.getModel();
		}

		@Override
		public Integer getPhaseCount() {
			return PM5100Data.this.getPhaseCount();
		}

		@Override
		public Integer getWireCount() {
			return PM5100Data.this.getWireCount();
		}

		@Override
		public String getFirmwareRevision() {
			return PM5100Data.this.getFirmwareRevision();
		}

		@Override
		public PM5100PowerSystem getPowerSystem() {
			return PM5100Data.this.getPowerSystem();
		}

		@Override
		public long getDataTimestamp() {
			return PM5100Data.this.getDataTimestamp();
		}

		@Override
		public ACEnergyDataAccessor accessorForPhase(ACPhase phase) {
			return PM5100Data.this.accessorForPhase(phase);
		}

		@Override
		public ACEnergyDataAccessor reversed() {
			return new ReversedMeterDataAccessor(this);
		}

		@Override
		public Float getFrequency() {
			return PM5100Data.this.getFrequency();
		}

		@Override
		public Float getCurrent() {
			Float n = null;
			switch (phase) {
				case PhaseA:
					n = getCurrentValue(PM5100Register.MeterCurrentPhaseA);
					break;

				case PhaseB:
					n = getCurrentValue(PM5100Register.MeterCurrentPhaseB);
					break;

				case PhaseC:
					n = getCurrentValue(PM5100Register.MeterCurrentPhaseC);
					break;

				default:
					n = PM5100Data.this.getCurrent();
			}
			return n;
		}

		@Override
		public Float getVoltage() {
			Float n = null;
			switch (phase) {
				case PhaseA:
					n = getVoltageValue(PM5100Register.MeterVoltageLineNeutralPhaseA);
					break;

				case PhaseB:
					n = getVoltageValue(PM5100Register.MeterVoltageLineNeutralPhaseB);
					break;

				case PhaseC:
					n = getVoltageValue(PM5100Register.MeterVoltageLineNeutralPhaseC);
					break;

				default:
					n = PM5100Data.this.getVoltage();
			}
			return n;
		}

		@Override
		public Float getLineVoltage() {
			Float n = null;
			switch (phase) {
				case PhaseA:
					n = getVoltageValue(PM5100Register.MeterVoltageLineLinePhaseAPhaseB);
					break;

				case PhaseB:
					n = getVoltageValue(PM5100Register.MeterVoltageLineLinePhaseBPhaseC);
					break;

				case PhaseC:
					n = getVoltageValue(PM5100Register.MeterVoltageLineLinePhaseCPhaseA);
					break;

				default:
					n = PM5100Data.this.getLineVoltage();
			}
			return n;
		}

		@Override
		public Float getPowerFactor() {
			return PM5100Data.this.getPowerFactor();
		}

		@Override
		public Integer getActivePower() {
			Integer n = null;
			switch (phase) {
				case PhaseA:
					n = getPowerValue(PM5100Register.MeterActivePowerPhaseA);
					break;

				case PhaseB:
					n = getPowerValue(PM5100Register.MeterActivePowerPhaseB);
					break;

				case PhaseC:
					n = getPowerValue(PM5100Register.MeterActivePowerPhaseC);
					break;

				default:
					n = PM5100Data.this.getActivePower();
			}
			return n;
		}

		@Override
		public Integer getApparentPower() {
			return PM5100Data.this.getApparentPower();
		}

		@Override
		public Integer getReactivePower() {
			return PM5100Data.this.getReactivePower();
		}

		@Override
		public Long getActiveEnergyDelivered() {
			return PM5100Data.this.getActiveEnergyDelivered();
		}

		@Override
		public Long getActiveEnergyReceived() {
			return PM5100Data.this.getActiveEnergyReceived();
		}

		@Override
		public Long getReactiveEnergyDelivered() {
			return PM5100Data.this.getReactiveEnergyDelivered();
		}

		@Override
		public Long getReactiveEnergyReceived() {
			return PM5100Data.this.getReactiveEnergyReceived();
		}

		@Override
		public Long getApparentEnergyDelivered() {
			return PM5100Data.this.getApparentEnergyDelivered();
		}

		@Override
		public Long getApparentEnergyReceived() {
			return PM5100Data.this.getActiveEnergyReceived();
		}

	}

	private static class ReversedMeterDataAccessor implements PM5100DataAccessor {

		private final PM5100DataAccessor delegate;

		private ReversedMeterDataAccessor(PM5100DataAccessor delegate) {
			super();
			this.delegate = delegate;
		}

		@Override
		public Map<String, Object> getDeviceInfo() {
			return delegate.getDeviceInfo();
		}

		@Override
		public ACEnergyDataAccessor accessorForPhase(ACPhase phase) {
			return new ReversedMeterDataAccessor((PM5100DataAccessor) delegate.accessorForPhase(phase));
		}

		@Override
		public Long getSerialNumber() {
			return delegate.getSerialNumber();
		}

		@Override
		public PM5100Model getModel() {
			return delegate.getModel();
		}

		@Override
		public Integer getPhaseCount() {
			return delegate.getPhaseCount();
		}

		@Override
		public Integer getWireCount() {
			return delegate.getWireCount();
		}

		@Override
		public PM5100PowerSystem getPowerSystem() {
			return delegate.getPowerSystem();
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
		public long getDataTimestamp() {
			return delegate.getDataTimestamp();
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
		public Float getLineVoltage() {
			return delegate.getLineVoltage();
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
