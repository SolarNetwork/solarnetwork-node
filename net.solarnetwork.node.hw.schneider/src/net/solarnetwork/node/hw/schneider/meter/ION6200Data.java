/* ==================================================================
 * ION6200Data.java - 14/05/2018 1:17:03 PM
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

import static net.solarnetwork.util.CollectionUtils.coveringIntRanges;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.node.domain.AcEnergyDataAccessor;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusWordOrder;
import net.solarnetwork.util.IntRange;

/**
 * Data object for the ION6200 series meter.
 * 
 * @author matt
 * @version 2.1
 * @since 2.4
 */
public class ION6200Data extends ModbusData implements ION6200DataAccessor {

	private static final int MAX_RESULTS = 64;

	private static final BigDecimal MEGA = new BigDecimal(1000000);
	private static final BigDecimal KILO = new BigDecimal(1000);
	private static final BigDecimal HECTO = new BigDecimal(100);
	private static final BigDecimal DECI = new BigDecimal("0.1");
	private static final BigDecimal CENTI = new BigDecimal("0.01");
	private static final BigDecimal MILLI = new BigDecimal("0.001");

	private boolean megawatt;

	/**
	 * Default constructor.
	 */
	public ION6200Data() {
		this(false);
	}

	/**
	 * Constructor.
	 * 
	 * @param megawatt
	 *        {@literal true} if this data is from the Megawatt version of the
	 *        6200 meter
	 */
	public ION6200Data(boolean megawatt) {
		super();
		setWordOrder(ModbusWordOrder.LeastToMostSignificant);
		this.megawatt = megawatt;
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the modbus data to copy
	 */
	public ION6200Data(ModbusData other) {
		super(other);
		this.megawatt = (other instanceof ION6200Data ? ((ION6200Data) other).megawatt : false);
	}

	@Override
	public ModbusData copy() {
		return new ION6200Data(this);
	}

	/**
	 * Get the megawatt model flag.
	 * 
	 * @return {@code true} if the data is treated as from a Megawatt model
	 */
	public boolean isMegawattModel() {
		return megawatt;
	}

	/**
	 * Set the megawatt model flag.
	 * 
	 * @param megawatt
	 *        {@literal true} to treat the data as from a Megawatt model
	 */
	public void setMegawattModel(boolean megawatt) {
		this.megawatt = megawatt;
	}

	/**
	 * Get a snapshot copy of the data.
	 * 
	 * @return a copy of the data
	 * @see ION6200Data#copy()
	 */
	public ION6200Data getSnapshot() {
		return (ION6200Data) copy();
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
		performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) throws IOException {
				// we actually read ALL registers here, so our snapshot timestamp includes everything
				updateData(conn, m,
						coveringIntRanges(ION6200Register.getRegisterAddressSet(), MAX_RESULTS));
				return true;
			}
		});
	}

	/**
	 * Read the meter registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public final void readMeterData(final ModbusConnection conn) throws IOException {
		performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) throws IOException {
				updateData(conn, m,
						coveringIntRanges(ION6200Register.getMeterRegisterAddressSet(), MAX_RESULTS));
				return true;
			}
		});
	}

	private void updateData(ModbusConnection conn, MutableModbusData m, Collection<IntRange> ranges)
			throws IOException {
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
	 * This class implements {@link ION6200DataAccessor} for the {@code Total}
	 * phase. Call this method to get an accessor for a different phase.
	 * </p>
	 * 
	 * @param phase
	 *        the phase to get an accessor for
	 * @return the accessor
	 */
	public ION6200DataAccessor dataAccessorForPhase(AcPhase phase) {
		if ( phase == AcPhase.Total ) {
			return this;
		}
		return new PhaseMeterDataAccessor(phase);
	}

	@Override
	public AcEnergyDataAccessor accessorForPhase(AcPhase phase) {
		return dataAccessorForPhase(phase);
	}

	@Override
	public AcEnergyDataAccessor reversed() {
		return new ReversedMeterDataAccessor(this);
	}

	@Override
	public Long getSerialNumber() {
		return getUnsignedInt32(ION6200Register.InfoSerialNumber.getAddress());
	}

	@Override
	public Integer getFirmwareRevision() {
		return getUnsignedInt16(ION6200Register.InfoFirmwareVersion.getAddress());
	}

	@Override
	public Integer getDeviceType() {
		return getUnsignedInt16(ION6200Register.InfoDeviceType.getAddress());
	}

	@Override
	public ION6200VoltsMode getVoltsMode() {
		Integer v = getUnsignedInt16(ION6200Register.ConfigVoltsMode.getAddress());
		ION6200VoltsMode m = null;
		if ( v != null ) {
			try {
				m = ION6200VoltsMode.forCode(v);
			} catch ( IllegalArgumentException e ) {
				// ignore
			}
		}
		return m;
	}

	@Override
	public Float getFrequency() {
		Short v = getInt16(ION6200Register.MeterFrequency.getAddress());
		return (v != null ? v.floatValue() / 100.0f : null);
	}

	@Override
	public Float getPowerFactor() {
		Number v = getNumber(ION6200Register.MeterPowerFactorTotal);
		return (v != null ? v.floatValue() / 100.0f : null);
	}

	private BigDecimal getProgrammableScale(ION6200Register reg) {
		Integer v = getUnsignedInt16(reg.getAddress());
		if ( v == null ) {
			return BigDecimal.ONE;
		}
		int pps = v.intValue();
		switch (pps) {
			case 0:
				return MILLI;
			case 1:
				return CENTI;
			case 2:
				return DECI;
			case 4:
				return BigDecimal.TEN;
			case 5:
				return HECTO;
			case 6:
				return KILO;
			default:
				return BigDecimal.ONE;
		}
	}

	private BigDecimal getProgrammableScaleValue(ION6200Register reg, ION6200Register scaleReg) {
		Number v = getNumber(reg);
		if ( v == null ) {
			return null;
		}
		BigDecimal pps = getProgrammableScale(scaleReg);
		BigDecimal d = new BigDecimal(v.toString());
		if ( pps == null || pps.equals(BigDecimal.ONE) || d.compareTo(BigDecimal.ZERO) == 0 ) {
			return d;
		}
		return d.divide(pps);
	}

	public Integer getPowerValue(ION6200Register reg) {
		BigDecimal v = getProgrammableScaleValue(reg, ION6200Register.ConfigProgrammablePowerScale);
		if ( v == null ) {
			return null;
		}
		return (megawatt ? v.multiply(MEGA) : v.multiply(KILO)).intValue();
	}

	@Override
	public Integer getActivePower() {
		return getPowerValue(ION6200Register.MeterActivePowerTotal);
	}

	@Override
	public Integer getApparentPower() {
		return getPowerValue(ION6200Register.MeterApparentPowerTotal);
	}

	@Override
	public Integer getReactivePower() {
		return getPowerValue(ION6200Register.MeterReactivePowerTotal);
	}

	private Float getCurrentValue(ION6200Register reg) {
		BigDecimal v = getProgrammableScaleValue(reg, ION6200Register.ConfigProgrammableCurrentScale);
		if ( v == null ) {
			return null;
		}
		return v.floatValue();
	}

	@Override
	public Float getCurrent() {
		return getCurrentValue(ION6200Register.MeterCurrentAverage);
	}

	private Float getVoltageValue(ION6200Register reg) {
		BigDecimal v = getProgrammableScaleValue(reg, ION6200Register.ConfigProgrammableVoltageScale);
		if ( v == null ) {
			return null;
		}
		return (megawatt ? v.multiply(KILO) : v).floatValue();
	}

	@Override
	public Float getVoltage() {
		return getVoltageValue(ION6200Register.MeterVoltageLineNeutralAverage);
	}

	@Override
	public Float getLineVoltage() {
		return getVoltageValue(ION6200Register.MeterVoltageLineLineAverage);
	}

	private Long getEnergyValue(ION6200Register reg) {
		Long v = getUnsignedInt32(reg.getAddress());
		if ( v == null ) {
			return null;
		}
		BigDecimal d = new BigDecimal(v);
		return (megawatt ? d.multiply(MEGA) : d.multiply(KILO)).longValue();
	}

	@Override
	public Long getActiveEnergyDelivered() {
		return getEnergyValue(ION6200Register.MeterActiveEnergyDelivered);
	}

	@Override
	public Long getActiveEnergyReceived() {
		return getEnergyValue(ION6200Register.MeterActiveEnergyReceived);
	}

	@Override
	public Long getReactiveEnergyDelivered() {
		return getEnergyValue(ION6200Register.MeterReactiveEnergyDelivered);
	}

	@Override
	public Long getReactiveEnergyReceived() {
		return getEnergyValue(ION6200Register.MeterReactiveEnergyReceived);
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

	private class PhaseMeterDataAccessor implements ION6200DataAccessor {

		private final AcPhase phase;

		private PhaseMeterDataAccessor(AcPhase phase) {
			super();
			this.phase = phase;
		}

		@Override
		public Map<String, Object> getDeviceInfo() {
			return ION6200Data.this.getDeviceInfo();
		}

		@Override
		public Long getSerialNumber() {
			return ION6200Data.this.getSerialNumber();
		}

		@Override
		public Integer getFirmwareRevision() {
			return ION6200Data.this.getFirmwareRevision();
		}

		@Override
		public Integer getDeviceType() {
			return ION6200Data.this.getDeviceType();
		}

		@Override
		public ION6200VoltsMode getVoltsMode() {
			return ION6200Data.this.getVoltsMode();
		}

		@Override
		public long getDataTimestamp() {
			return ION6200Data.this.getDataTimestamp();
		}

		@Override
		public AcEnergyDataAccessor accessorForPhase(AcPhase phase) {
			return ION6200Data.this.accessorForPhase(phase);
		}

		@Override
		public AcEnergyDataAccessor reversed() {
			return new ReversedMeterDataAccessor(this);
		}

		@Override
		public Float getFrequency() {
			return ION6200Data.this.getFrequency();
		}

		@Override
		public Float getCurrent() {
			Float n = null;
			switch (phase) {
				case PhaseA:
					n = getCurrentValue(ION6200Register.MeterCurrentPhaseA);
					break;

				case PhaseB:
					n = getCurrentValue(ION6200Register.MeterCurrentPhaseB);
					break;

				case PhaseC:
					n = getCurrentValue(ION6200Register.MeterCurrentPhaseC);
					break;

				default:
					n = ION6200Data.this.getCurrent();
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
					n = getVoltageValue(ION6200Register.MeterVoltageLineNeutralPhaseA);
					break;

				case PhaseB:
					n = getVoltageValue(ION6200Register.MeterVoltageLineNeutralPhaseB);
					break;

				case PhaseC:
					n = getVoltageValue(ION6200Register.MeterVoltageLineNeutralPhaseC);
					break;

				default:
					n = ION6200Data.this.getVoltage();
			}
			return n;
		}

		@Override
		public Float getLineVoltage() {
			Float n = null;
			switch (phase) {
				case PhaseA:
					n = getVoltageValue(ION6200Register.MeterVoltageLineLinePhaseAPhaseB);
					break;

				case PhaseB:
					n = getVoltageValue(ION6200Register.MeterVoltageLineLinePhaseBPhaseC);
					break;

				case PhaseC:
					n = getVoltageValue(ION6200Register.MeterVoltageLineLinePhaseCPhaseA);
					break;

				default:
					n = ION6200Data.this.getLineVoltage();
			}
			return n;
		}

		@Override
		public Float getPowerFactor() {
			return ION6200Data.this.getPowerFactor();
		}

		@Override
		public Integer getActivePower() {
			Integer n = null;
			switch (phase) {
				case PhaseA:
					n = getPowerValue(ION6200Register.MeterActivePowerPhaseA);
					break;

				case PhaseB:
					n = getPowerValue(ION6200Register.MeterActivePowerPhaseB);
					break;

				case PhaseC:
					n = getPowerValue(ION6200Register.MeterActivePowerPhaseC);
					break;

				default:
					n = ION6200Data.this.getActivePower();
			}
			return n;
		}

		@Override
		public Integer getApparentPower() {
			return ION6200Data.this.getApparentPower();
		}

		@Override
		public Integer getReactivePower() {
			return ION6200Data.this.getReactivePower();
		}

		@Override
		public Long getActiveEnergyDelivered() {
			return ION6200Data.this.getActiveEnergyDelivered();
		}

		@Override
		public Long getActiveEnergyReceived() {
			return ION6200Data.this.getActiveEnergyReceived();
		}

		@Override
		public Long getReactiveEnergyDelivered() {
			return ION6200Data.this.getReactiveEnergyDelivered();
		}

		@Override
		public Long getReactiveEnergyReceived() {
			return ION6200Data.this.getReactiveEnergyReceived();
		}

		@Override
		public Long getApparentEnergyDelivered() {
			return ION6200Data.this.getApparentEnergyDelivered();
		}

		@Override
		public Long getApparentEnergyReceived() {
			return ION6200Data.this.getActiveEnergyReceived();
		}

	}

	private static class ReversedMeterDataAccessor implements ION6200DataAccessor {

		private final ION6200DataAccessor delegate;

		private ReversedMeterDataAccessor(ION6200DataAccessor delegate) {
			super();
			this.delegate = delegate;
		}

		@Override
		public Map<String, Object> getDeviceInfo() {
			return delegate.getDeviceInfo();
		}

		@Override
		public AcEnergyDataAccessor accessorForPhase(AcPhase phase) {
			return new ReversedMeterDataAccessor((ION6200DataAccessor) delegate.accessorForPhase(phase));
		}

		@Override
		public Integer getDeviceType() {
			return delegate.getDeviceType();
		}

		@Override
		public ION6200VoltsMode getVoltsMode() {
			return delegate.getVoltsMode();
		}

		@Override
		public Long getSerialNumber() {
			return delegate.getSerialNumber();
		}

		@Override
		public Integer getFirmwareRevision() {
			return delegate.getFirmwareRevision();
		}

		@Override
		public AcEnergyDataAccessor reversed() {
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
