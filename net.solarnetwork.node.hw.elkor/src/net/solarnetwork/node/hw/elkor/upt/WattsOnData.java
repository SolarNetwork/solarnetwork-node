/* ==================================================================
 * WattsOnData.java - 14/08/2020 9:39:00 AM
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

package net.solarnetwork.node.hw.elkor.upt;

import static net.solarnetwork.util.CollectionUtils.coveringIntRanges;
import static net.solarnetwork.util.NumberUtils.maximumDecimalScale;
import static net.solarnetwork.util.NumberUtils.scaled;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import net.solarnetwork.node.domain.ACEnergyDataAccessor;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;
import net.solarnetwork.util.IntRange;

/**
 * Data object for the WattsOn series meter.
 * 
 * @author matt
 * @version 1.0
 */
public class WattsOnData extends ModbusData implements WattsOnDataAccessor {

	private static final int MAX_RESULTS = 64;

	/**
	 * Default constructor.
	 */
	public WattsOnData() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the modbus data to copy
	 */
	public WattsOnData(ModbusData other) {
		super(other);
	}

	@Override
	public WattsOnData copy() {
		return new WattsOnData(this);
	}

	/**
	 * Get a snapshot copy of the data.
	 * 
	 * @return a copy of the data
	 * @see #copy()
	 */
	public WattsOnData getSnapshot() {
		return copy();
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
						coveringIntRanges(WattsOnRegister.getRegisterAddressSet(), MAX_RESULTS));
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
						coveringIntRanges(WattsOnRegister.getMeterRegisterAddressSet(), MAX_RESULTS));
				return true;
			}
		});
	}

	private void updateData(ModbusConnection conn, MutableModbusData m, Collection<IntRange> ranges) {
		for ( IntRange r : ranges ) {
			short[] data = conn.readWords(ModbusReadFunction.ReadHoldingRegister, r.getMin(),
					r.length());
			m.saveDataArray(data, r.getMin());
		}
	}

	/**
	 * Get an accessor for a specific phase.
	 * 
	 * <p>
	 * This class implements {@link WattsOnDataAccessor} for the {@code Total}
	 * phase. Call this method to get an accessor for a different phase.
	 * </p>
	 * 
	 * @param phase
	 *        the phase to get an accessor for
	 * @return the accessor
	 */
	public WattsOnDataAccessor dataAccessorForPhase(ACPhase phase) {
		if ( phase == ACPhase.Total ) {
			return this;
		}
		return new PhaseMeterDataAccessor(phase);
	}

	private Integer getPowerValue(ModbusReference reg) {
		BigDecimal n = scaled(getNumber(reg), 3);
		return (n != null ? n.intValue() : null);
	}

	private Float getCurrentValue(ModbusReference reg) {
		Number n = maximumDecimalScale(getNumber(reg), 3);
		return (n != null ? n.floatValue() : null);
	}

	private Float getVoltageValue(ModbusReference reg) {
		Number n = maximumDecimalScale(getNumber(reg), 1);
		return (n != null ? n.floatValue() : null);
	}

	private Long getEnergyValue(ModbusReference reg) {
		BigDecimal n = scaled(getNumber(reg), 3);
		return (n != null ? n.longValue() : null);
	}

	private Ratio getRatioValue(ModbusReference primary, ModbusReference secondary) {
		Number p = getNumber(primary);
		Number s = getNumber(secondary);
		return (p != null && s != null ? new Ratio(p.intValue(), s.intValue()) : null);
	}

	@Override
	public ACEnergyDataAccessor accessorForPhase(ACPhase phase) {
		return dataAccessorForPhase(phase);
	}

	@Override
	public ACEnergyDataAccessor reversed() {
		return new ReversedMeterDataAccessor(this);
	}

	@Override
	public Number getSerialNumber() {
		return getNumber(WattsOnRegister.InfoSerialNumber);
	}

	@Override
	public Number getFirmwareRevision() {
		return maximumDecimalScale(getNumber(WattsOnRegister.InfoFirmwareVersion), 1);
	}

	@Override
	public Ratio getPowerTransformerRatio() {
		return getRatioValue(WattsOnRegister.ConfigPtRatioPrimary,
				WattsOnRegister.ConfigPtRatioSecondary);
	}

	@Override
	public Ratio getCurrentTransformerRatio() {
		return getRatioValue(WattsOnRegister.ConfigCtRatioPrimary,
				WattsOnRegister.ConfigCtRatioSecondary);
	}

	@Override
	public Float getFrequency() {
		Number n = maximumDecimalScale(getNumber(WattsOnRegister.MeterFrequency), 1);
		return (n != null ? n.floatValue() : null);
	}

	@Override
	public Float getPowerFactor() {
		return getFloat32(WattsOnRegister.MeterPowerFactorTotal.getAddress());
	}

	@Override
	public Integer getActivePower() {
		return getPowerValue(WattsOnRegister.MeterActivePowerTotal);
	}

	@Override
	public Integer getApparentPower() {
		return getPowerValue(WattsOnRegister.MeterApparentPowerTotal);
	}

	@Override
	public Integer getReactivePower() {
		return getPowerValue(WattsOnRegister.MeterReactivePowerTotal);
	}

	@Override
	public Float getCurrent() {
		return getCurrentValue(WattsOnRegister.MeterCurrentAverage);
	}

	@Override
	public Float getVoltage() {
		return getVoltageValue(WattsOnRegister.MeterVoltageLineNeutralAverage);
	}

	@Override
	public Float getLineVoltage() {
		return getVoltageValue(WattsOnRegister.MeterVoltageLineLineAverage);
	}

	@Override
	public Long getActiveEnergyDelivered() {
		return getEnergyValue(WattsOnRegister.MeterActiveEnergyDelivered);
	}

	@Override
	public Long getActiveEnergyReceived() {
		return getEnergyValue(WattsOnRegister.MeterActiveEnergyReceived);
	}

	@Override
	public Long getReactiveEnergyDelivered() {
		return getEnergyValue(WattsOnRegister.MeterReactiveEnergyDelivered);
	}

	@Override
	public Long getReactiveEnergyReceived() {
		return getEnergyValue(WattsOnRegister.MeterReactiveEnergyReceived);
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
	public Float getNeutralCurrent() {
		return null;
	}

	private class PhaseMeterDataAccessor implements WattsOnDataAccessor {

		private final ACPhase phase;

		private PhaseMeterDataAccessor(ACPhase phase) {
			super();
			this.phase = phase;
		}

		@Override
		public Map<String, Object> getDeviceInfo() {
			return WattsOnData.this.getDeviceInfo();
		}

		@Override
		public Number getSerialNumber() {
			return WattsOnData.this.getSerialNumber();
		}

		@Override
		public Number getFirmwareRevision() {
			return WattsOnData.this.getFirmwareRevision();
		}

		@Override
		public Ratio getPowerTransformerRatio() {
			return WattsOnData.this.getPowerTransformerRatio();
		}

		@Override
		public Ratio getCurrentTransformerRatio() {
			return WattsOnData.this.getCurrentTransformerRatio();
		}

		@Override
		public long getDataTimestamp() {
			return WattsOnData.this.getDataTimestamp();
		}

		@Override
		public ACEnergyDataAccessor accessorForPhase(ACPhase phase) {
			return WattsOnData.this.accessorForPhase(phase);
		}

		@Override
		public ACEnergyDataAccessor reversed() {
			return new ReversedMeterDataAccessor(this);
		}

		@Override
		public Float getFrequency() {
			return WattsOnData.this.getFrequency();
		}

		@Override
		public Float getCurrent() {
			Float n = null;
			switch (phase) {
				case PhaseA:
					n = getCurrentValue(WattsOnRegister.MeterCurrentPhaseA);
					break;

				case PhaseB:
					n = getCurrentValue(WattsOnRegister.MeterCurrentPhaseB);
					break;

				case PhaseC:
					n = getCurrentValue(WattsOnRegister.MeterCurrentPhaseC);
					break;

				default:
					n = WattsOnData.this.getCurrent();
			}
			return n;
		}

		@Override
		public Float getNeutralCurrent() {
			return null;
		}

		@Override
		public Float getVoltage() {
			Float n = null;
			switch (phase) {
				case PhaseA:
					n = getVoltageValue(WattsOnRegister.MeterVoltageLineNeutralPhaseA);
					break;

				case PhaseB:
					n = getVoltageValue(WattsOnRegister.MeterVoltageLineNeutralPhaseB);
					break;

				case PhaseC:
					n = getVoltageValue(WattsOnRegister.MeterVoltageLineNeutralPhaseC);
					break;

				default:
					n = WattsOnData.this.getVoltage();
			}
			return n;
		}

		@Override
		public Float getLineVoltage() {
			Float n = null;
			switch (phase) {
				case PhaseA:
					n = getVoltageValue(WattsOnRegister.MeterVoltageLineLinePhaseAPhaseB);
					break;

				case PhaseB:
					n = getVoltageValue(WattsOnRegister.MeterVoltageLineLinePhaseBPhaseC);
					break;

				case PhaseC:
					n = getVoltageValue(WattsOnRegister.MeterVoltageLineLinePhaseCPhaseA);
					break;

				default:
					n = WattsOnData.this.getLineVoltage();
			}
			return n;
		}

		@Override
		public Float getPowerFactor() {
			return WattsOnData.this.getPowerFactor();
		}

		@Override
		public Integer getActivePower() {
			Integer n = null;
			switch (phase) {
				case PhaseA:
					n = getPowerValue(WattsOnRegister.MeterActivePowerPhaseA);
					break;

				case PhaseB:
					n = getPowerValue(WattsOnRegister.MeterActivePowerPhaseB);
					break;

				case PhaseC:
					n = getPowerValue(WattsOnRegister.MeterActivePowerPhaseC);
					break;

				default:
					n = WattsOnData.this.getActivePower();
			}
			return n;
		}

		@Override
		public Integer getApparentPower() {
			return WattsOnData.this.getApparentPower();
		}

		@Override
		public Integer getReactivePower() {
			return WattsOnData.this.getReactivePower();
		}

		@Override
		public Long getActiveEnergyDelivered() {
			return WattsOnData.this.getActiveEnergyDelivered();
		}

		@Override
		public Long getActiveEnergyReceived() {
			return WattsOnData.this.getActiveEnergyReceived();
		}

		@Override
		public Long getReactiveEnergyDelivered() {
			return WattsOnData.this.getReactiveEnergyDelivered();
		}

		@Override
		public Long getReactiveEnergyReceived() {
			return WattsOnData.this.getReactiveEnergyReceived();
		}

		@Override
		public Long getApparentEnergyDelivered() {
			return WattsOnData.this.getApparentEnergyDelivered();
		}

		@Override
		public Long getApparentEnergyReceived() {
			return WattsOnData.this.getActiveEnergyReceived();
		}

	}

	private static class ReversedMeterDataAccessor implements WattsOnDataAccessor {

		private final WattsOnDataAccessor delegate;

		private ReversedMeterDataAccessor(WattsOnDataAccessor delegate) {
			super();
			this.delegate = delegate;
		}

		@Override
		public Map<String, Object> getDeviceInfo() {
			return delegate.getDeviceInfo();
		}

		@Override
		public ACEnergyDataAccessor accessorForPhase(ACPhase phase) {
			return new ReversedMeterDataAccessor((WattsOnDataAccessor) delegate.accessorForPhase(phase));
		}

		@Override
		public Number getSerialNumber() {
			return delegate.getSerialNumber();
		}

		@Override
		public Number getFirmwareRevision() {
			return delegate.getFirmwareRevision();
		}

		@Override
		public Ratio getPowerTransformerRatio() {
			return delegate.getPowerTransformerRatio();
		}

		@Override
		public Ratio getCurrentTransformerRatio() {
			return delegate.getCurrentTransformerRatio();
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
		public Float getNeutralCurrent() {
			return delegate.getNeutralCurrent();
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
