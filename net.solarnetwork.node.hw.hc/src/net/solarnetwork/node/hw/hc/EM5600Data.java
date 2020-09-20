/* ==================================================================
 * EM5600Data.java - Mar 26, 2014 4:13:48 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.hc;

import static net.solarnetwork.util.NumberUtils.bigDecimalForNumber;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.joda.time.LocalDateTime;
import net.solarnetwork.node.domain.ACEnergyDataAccessor;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.util.NumberUtils;

/**
 * Encapsulates raw Modbus register data from the EM5600 meters.
 * 
 * @author matt
 * @version 2.1
 */
public class EM5600Data extends ModbusData implements EM5600DataAccessor {

	private static final int MAX_RESULTS = 64;

	private UnitFactor unitFactor;

	/**
	 * Default constructor.
	 */
	public EM5600Data() {
		super();
		this.unitFactor = UnitFactor.EM5610;
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the object to copy
	 */
	public EM5600Data(EM5600Data other) {
		super(other);
		unitFactor = other.unitFactor;
	}

	@Override
	public EM5600Data copy() {
		return new EM5600Data(this);
	}

	@Override
	public Map<String, Object> getDeviceInfo() {
		EM5600DataAccessor data = copy();
		Map<String, Object> result = new LinkedHashMap<>(4);
		Integer model = data.getModel();
		if ( model != null ) {
			String version = data.getHardwareRevision();
			if ( version != null ) {
				result.put(INFO_KEY_DEVICE_MODEL, String.format("%s (version %s)", model, version));
			} else {
				result.put(INFO_KEY_DEVICE_MODEL, model.toString());
			}
		}
		String sn = data.getSerialNumber();
		if ( sn != null ) {
			result.put(INFO_KEY_DEVICE_SERIAL_NUMBER, sn);
		}
		LocalDateTime date = data.getManufactureDate();
		if ( date != null ) {
			result.put(INFO_KEY_DEVICE_MANUFACTURE_DATE, date.toLocalDate());
		}
		return result;
	}

	@Override
	public ACEnergyDataAccessor accessorForPhase(ACPhase phase) {
		if ( phase == ACPhase.Total ) {
			return this;
		}
		return new PhaseMeterDataAccessor(phase);
	}

	@Override
	public ACEnergyDataAccessor reversed() {
		return new ReversedMeterDataAccessor(this);
	}

	@Override
	public String getSerialNumber() {
		return getAsciiString(EM5600Register.InfoSerialNumber, true);
	}

	@Override
	public String getHardwareRevision() {
		return getAsciiString(EM5600Register.InfoHardwareVersion, true);
	}

	@Override
	public Integer getModel() {
		Number n = getNumber(EM5600Register.InfoModel);
		return (n != null ? n.intValue() : null);
	}

	@Override
	public LocalDateTime getManufactureDate() {
		return getDateValue(EM5600Register.InfoManufactureDate);
	}

	@Override
	public Float getFrequency() {
		return getFrequencyValue(EM5600Register.MeterFrequency);
	}

	@Override
	public Float getCurrent() {
		return getCurrentValue(EM5600Register.MeterCurrentAverage);
	}

	@Override
	public Float getNeutralCurrent() {
		return null;
	}

	@Override
	public Float getVoltage() {
		return getVoltageValue(EM5600Register.MeterVoltageLineNeutralAverage);
	}

	@Override
	public Float getLineVoltage() {
		return getVoltageValue(EM5600Register.MeterVoltageLineLineAverage);
	}

	@Override
	public Float getPowerFactor() {
		return getPowerFactorValue(EM5600Register.MeterPowerFactorTotal);
	}

	@Override
	public Integer getActivePower() {
		return getPowerValue(EM5600Register.MeterActivePowerTotal);
	}

	@Override
	public Long getActiveEnergyDelivered() {
		return getEnergyValue(EM5600Register.MeterActiveEnergyDelivered);
	}

	@Override
	public Long getActiveEnergyReceived() {
		return getEnergyValue(EM5600Register.MeterActiveEnergyReceived);
	}

	@Override
	public Integer getApparentPower() {
		return getPowerValue(EM5600Register.MeterApparentPowerTotal);
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
		return getPowerValue(EM5600Register.MeterReactivePowerTotal);
	}

	@Override
	public Long getReactiveEnergyDelivered() {
		return getEnergyValue(EM5600Register.MeterReactiveEnergyDelivered);
	}

	@Override
	public Long getReactiveEnergyReceived() {
		return getEnergyValue(EM5600Register.MeterReactiveEnergyReceived);
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
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister, EM5600Register.getRegisterAddressSet(),
				MAX_RESULTS);
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
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				EM5600Register.getMeterRegisterAddressSet(), MAX_RESULTS);
	}

	/*- TODO: remove
	 * Read the PT ratio, and CT ratio values from the meter. If the
	 * {@code unitFactor} is set to {@link UnitFactor#EM5610} then this method
	 * will not actually query the meter, as the values are fixed for that
	 * meter.
	 * 
	 * @param conn
	 *        the Modbus connection
	 *
	public void readEnergyRatios(final ModbusConnection conn) {
		refreshData(conn, ModbusReadFunction.ReadHoldingRegister,
				EM5600Register.getConfigRegisterAddressSet(), 64);
		int[] transformerRatios = conn.readInts(ADDR_DATA_PT_RATIO, 2);
		if ( transformerRatios != null && transformerRatios.length > 1 ) {
			int ptr = transformerRatios[0];
			ptRatio = (ptr < 1 ? 1 : ptr / 10);
			int ctr = transformerRatios[1];
			ctRatio = (ctr < 1 ? 1 : ctr / 10);
		}
	}
	*/

	/*- TODO: remove
	 * Set the raw Modbus current, voltage, and power register data. This
	 * corresponds to the register range 0x130 - 0x151.
	 * 
	 * @param current
	 *        the data
	 *
	public void setCurrentVoltagePower(int[] data) {
		if ( data == null ) {
			return;
		}
		System.arraycopy(data, 0, inputRegisters, (ADDR_DATA_I1 - ADDR_INPUT_REG_START),
				Math.min(data.length, (ADDR_DATA_PHASE_ROTATION - ADDR_DATA_I1 + 1)));
	}
	
	/**
	 * Set the raw Modbus energy register data. This corresponds to the register
	 * range 0x160 - 0x17E.
	 * 
	 * @param power
	 *        the data
	 *
	public void setEnergy(int[] energy) {
		if ( energy == null ) {
			return;
		}
		System.arraycopy(energy, 0, inputRegisters,
				(ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT - ADDR_INPUT_REG_START), Math.min(energy.length,
						(ADDR_DATA_ENERGY_UNIT - ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT + 1)));
	}
	*/

	@Override
	public UnitFactor getUnitFactor() {
		return unitFactor;
	}

	/**
	 * Set the {@link UnitFactor} to use for calculating effective values. This
	 * defaults to {@link UnitFactor#EM5610}.
	 * 
	 * @param unitFactor
	 *        the unit factor to set
	 */
	public void setUnitFactor(UnitFactor unitFactor) {
		assert unitFactor != null;
		this.unitFactor = unitFactor;
	}

	/**
	 * Get the effective CT ratio. This will return {@code 1} unless the
	 * {@code unitFactor} is set to {@code EM5630_5A}, in which case it will
	 * return {@link #getCtRatio()} which has presumably be set by reading from
	 * the meter via {@link #readConfigurationData(ModbusConnection)}.
	 * 
	 * @return effective CT ratio
	 */
	public BigDecimal getEffectiveCtRatio() {
		return (unitFactor == UnitFactor.EM5630_5A ? getCtRatio() : BigDecimal.ONE);
	}

	/**
	 * Get the effective PT ratio. This will return {@code 1} unless the
	 * {@code unitFactor} is set to {@code EM5630_5A}, in which case it will
	 * return {@link #getPtRatio()} which has presumably be set by reading from
	 * the meter via {@link #readConfigurationData(ModbusConnection)}.
	 * 
	 * @return effective PT ratio
	 */
	public BigDecimal getEffectivePtRatio() {
		return (unitFactor == UnitFactor.EM5630_5A ? getPtRatio() : BigDecimal.ONE);
	}

	/**
	 * Get an effective voltage value in V.
	 * 
	 * @param addr
	 *        the register address to read
	 * @return the value interpreted as a voltage value
	 */
	private Float getVoltageValue(EM5600Register ref) {
		Number n = getNumber(ref);
		if ( n != null ) {
			n = bigDecimalForNumber(n).multiply(getEffectivePtRatio()).multiply(unitFactor.getU());
		}
		return (n != null ? n.floatValue() : null);
	}

	/**
	 * Get an effective current value in A.
	 * 
	 * @param addr
	 *        the register address to read
	 * @return the value interpreted as a current value
	 */
	private Float getCurrentValue(EM5600Register ref) {
		Number n = getNumber(ref);
		if ( n != null ) {
			n = bigDecimalForNumber(n).multiply(getEffectiveCtRatio()).multiply(unitFactor.getA());
		}
		return (n != null ? n.floatValue() : null);
	}

	/**
	 * Get an effective frequency value in Hz.
	 * 
	 * @param addr
	 *        the register address to read
	 * @return the value interpreted as a frequency value
	 */
	private Float getFrequencyValue(EM5600Register ref) {
		Number n = getNumber(ref);
		if ( n != null ) {
			n = NumberUtils.scaled(bigDecimalForNumber(n).multiply(new BigDecimal("2")), -3);
		}
		return (n != null ? n.floatValue() : null);
	}

	/**
	 * Get an effective power factor value (cosine of the phase angle).
	 * 
	 * @param addr
	 *        the register address to read
	 * @return the value interpreted as a power factor value
	 */
	private Float getPowerFactorValue(EM5600Register ref) {
		Number n = getNumber(ref);
		if ( n != null ) {
			n = NumberUtils.scaled(n, -4);
		}
		return (n != null ? n.floatValue() : null);
	}

	/**
	 * Get an effective power value in W (active), Var (reactive) or VA
	 * (apparent).
	 * 
	 * @param addr
	 *        the register address to read
	 * @return the value interpreted as a power value
	 */
	private Integer getPowerValue(EM5600Register ref) {
		Number n = getNumber(ref);
		if ( n != null ) {
			n = NumberUtils.scaled(bigDecimalForNumber(n).multiply(getEffectivePtRatio())
					.multiply(getEffectiveCtRatio()).multiply(unitFactor.getP()), 0);
		}
		return (n != null ? n.intValue() : null);
	}

	/**
	 * Get an effective energy value in Wh (real), Varh (reactive).
	 * 
	 * @param addr
	 *        the register address to read
	 * @return the value interpreted as an energy value
	 */
	private Long getEnergyValue(EM5600Register ref) {
		Number n = getNumber(ref);
		if ( n != null ) {
			n = NumberUtils.scaled(bigDecimalForNumber(n), getEnergyUnit().getScaleFactor());
		}
		return (n != null ? n.longValue() : null);
	}

	private LocalDateTime getDateValue(EM5600Register ref) {
		Number n = getNumber(ref);
		LocalDateTime result = null;
		if ( n != null ) {
			final long l = n.longValue();
			int day = (int) ((l >> 24) & 0x1F); // 1 - 31
			int year = 2000 + (int) ((l >> 8) & 0xFF); // 0 - 255
			int month = ((int) l & 0xF); //1-12
			result = new LocalDateTime(year, month, day, 0, 0, 0, 0);
		}
		return result;
	}

	@Override
	public BigDecimal getCtRatio() {
		Number n = getNumber(EM5600Register.ConfigCtRatio);
		return (n != null && n.intValue() > 0 ? NumberUtils.scaled(n, -1) : BigDecimal.ONE);
	}

	@Override
	public BigDecimal getPtRatio() {
		Number n = getNumber(EM5600Register.ConfigPtRatio);
		return (n != null && n.intValue() > 0 ? NumberUtils.scaled(n, -1) : BigDecimal.ONE);
	}

	@Override
	public EnergyUnit getEnergyUnit() {
		Number n = getNumber(EM5600Register.ConfigEnergyUnit);
		EnergyUnit u;
		if ( n != null ) {
			u = EnergyUnit.energyUnitForValue(n.intValue());
		} else {
			u = EnergyUnit.WattHour;
		}
		return u;
	}

	private class PhaseMeterDataAccessor implements EM5600DataAccessor {

		private final ACPhase phase;

		private PhaseMeterDataAccessor(ACPhase phase) {
			super();
			this.phase = phase;
		}

		@Override
		public Map<String, Object> getDeviceInfo() {
			return EM5600Data.this.getDeviceInfo();
		}

		@Override
		public String getSerialNumber() {
			return EM5600Data.this.getSerialNumber();
		}

		@Override
		public String getHardwareRevision() {
			return EM5600Data.this.getHardwareRevision();
		}

		@Override
		public Integer getModel() {
			return EM5600Data.this.getModel();
		}

		@Override
		public LocalDateTime getManufactureDate() {
			return EM5600Data.this.getManufactureDate();
		}

		@Override
		public EnergyUnit getEnergyUnit() {
			return EM5600Data.this.getEnergyUnit();
		}

		@Override
		public UnitFactor getUnitFactor() {
			return EM5600Data.this.getUnitFactor();
		}

		@Override
		public BigDecimal getCtRatio() {
			return EM5600Data.this.getCtRatio();
		}

		@Override
		public BigDecimal getPtRatio() {
			return EM5600Data.this.getPtRatio();
		}

		@Override
		public long getDataTimestamp() {
			return EM5600Data.this.getDataTimestamp();
		}

		@Override
		public ACEnergyDataAccessor accessorForPhase(ACPhase phase) {
			return EM5600Data.this.accessorForPhase(phase);
		}

		@Override
		public ACEnergyDataAccessor reversed() {
			return new ReversedMeterDataAccessor(this);
		}

		@Override
		public Float getFrequency() {
			return EM5600Data.this.getFrequency();
		}

		@Override
		public Float getCurrent() {
			Float n = null;
			switch (phase) {
				case PhaseA:
					n = getCurrentValue(EM5600Register.MeterCurrentPhaseA);
					break;

				case PhaseB:
					n = getCurrentValue(EM5600Register.MeterCurrentPhaseB);
					break;

				case PhaseC:
					n = getCurrentValue(EM5600Register.MeterCurrentPhaseC);
					break;

				default:
					n = EM5600Data.this.getCurrent();
			}
			return n;
		}

		@Override
		public Float getNeutralCurrent() {
			return EM5600Data.this.getNeutralCurrent();
		}

		@Override
		public Float getVoltage() {
			Float n = null;
			switch (phase) {
				case PhaseA:
					n = getVoltageValue(EM5600Register.MeterVoltageLineNeutralPhaseA);
					break;

				case PhaseB:
					n = getVoltageValue(EM5600Register.MeterVoltageLineNeutralPhaseB);
					break;

				case PhaseC:
					n = getVoltageValue(EM5600Register.MeterVoltageLineNeutralPhaseC);
					break;

				default:
					n = EM5600Data.this.getVoltage();
			}
			return n;
		}

		@Override
		public Float getLineVoltage() {
			Float n = null;
			switch (phase) {
				case PhaseA:
					n = getVoltageValue(EM5600Register.MeterVoltageLineLinePhaseAPhaseB);
					break;

				case PhaseB:
					n = getVoltageValue(EM5600Register.MeterVoltageLineLinePhaseBPhaseC);
					break;

				case PhaseC:
					n = getVoltageValue(EM5600Register.MeterVoltageLineLinePhaseCPhaseA);
					break;

				default:
					n = EM5600Data.this.getLineVoltage();
			}
			return n;
		}

		@Override
		public Float getPowerFactor() {
			return EM5600Data.this.getPowerFactor();
		}

		@Override
		public Integer getActivePower() {
			Integer n = null;
			switch (phase) {
				case PhaseA:
					n = getPowerValue(EM5600Register.MeterActivePowerPhaseA);
					break;

				case PhaseB:
					n = getPowerValue(EM5600Register.MeterActivePowerPhaseB);
					break;

				case PhaseC:
					n = getPowerValue(EM5600Register.MeterActivePowerPhaseC);
					break;

				default:
					n = EM5600Data.this.getActivePower();
			}
			return n;
		}

		@Override
		public Integer getApparentPower() {
			return EM5600Data.this.getApparentPower();
		}

		@Override
		public Integer getReactivePower() {
			return EM5600Data.this.getReactivePower();
		}

		@Override
		public Long getActiveEnergyDelivered() {
			Long n = null;
			if ( phase == ACPhase.Total ) {
				n = EM5600Data.this.getActiveEnergyDelivered();
			}
			return n;
		}

		@Override
		public Long getActiveEnergyReceived() {
			Long n = null;
			if ( phase == ACPhase.Total ) {
				n = EM5600Data.this.getActiveEnergyReceived();
			}
			return n;
		}

		@Override
		public Long getReactiveEnergyDelivered() {
			Long n = null;
			if ( phase == ACPhase.Total ) {
				n = EM5600Data.this.getReactiveEnergyDelivered();
			}
			return n;
		}

		@Override
		public Long getReactiveEnergyReceived() {
			Long n = null;
			if ( phase == ACPhase.Total ) {
				n = EM5600Data.this.getReactiveEnergyReceived();
			}
			return n;
		}

		@Override
		public Long getApparentEnergyDelivered() {
			Long n = null;
			if ( phase == ACPhase.Total ) {
				n = EM5600Data.this.getApparentEnergyDelivered();
			}
			return n;
		}

		@Override
		public Long getApparentEnergyReceived() {
			Long n = null;
			if ( phase == ACPhase.Total ) {
				n = EM5600Data.this.getApparentEnergyReceived();
			}
			return n;
		}

	}

	private static class ReversedMeterDataAccessor implements EM5600DataAccessor {

		private final EM5600DataAccessor delegate;

		private ReversedMeterDataAccessor(EM5600DataAccessor delegate) {
			super();
			this.delegate = delegate;
		}

		@Override
		public Map<String, Object> getDeviceInfo() {
			return delegate.getDeviceInfo();
		}

		@Override
		public ACEnergyDataAccessor accessorForPhase(ACPhase phase) {
			return new ReversedMeterDataAccessor((EM5600DataAccessor) delegate.accessorForPhase(phase));
		}

		@Override
		public ACEnergyDataAccessor reversed() {
			return delegate;
		}

		@Override
		public String getSerialNumber() {
			return delegate.getSerialNumber();
		}

		@Override
		public String getHardwareRevision() {
			return delegate.getHardwareRevision();
		}

		@Override
		public Integer getModel() {
			return delegate.getModel();
		}

		@Override
		public LocalDateTime getManufactureDate() {
			return delegate.getManufactureDate();
		}

		@Override
		public EnergyUnit getEnergyUnit() {
			return delegate.getEnergyUnit();
		}

		@Override
		public UnitFactor getUnitFactor() {
			return delegate.getUnitFactor();
		}

		@Override
		public BigDecimal getCtRatio() {
			return delegate.getCtRatio();
		}

		@Override
		public BigDecimal getPtRatio() {
			return delegate.getPtRatio();
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

	/*- TODO: move to Datum
	/**
	 * Populate measurements into a {@link GeneralNodeACEnergyDatum} instance.
	 * 
	 * @param phase
	 *        the phase to populate data for
	 * @param datum
	 *        the datum to populate data into
	 * @since 1.2
	 *
	public void populateMeasurements(final ACPhase phase, final GeneralNodeACEnergyDatum datum) {
		switch (phase) {
			case Total:
				populateTotalMeasurements(this, datum);
				break;
	
			case PhaseA:
				populatePhaseAMeasurements(this, datum);
				break;
	
			case PhaseB:
				populatePhaseBMeasurements(this, datum);
				break;
	
			case PhaseC:
				populatePhaseCMeasurements(this, datum);
				break;
		}
	}
	
	private static void populateTotalMeasurements(final EM5600Data sample,
			final GeneralNodeACEnergyDatum datum) {
		final boolean backwards = sample.isBackwards();
	
		datum.setEffectivePowerFactor(sample.getPowerFactor(ADDR_DATA_POWER_FACTOR_TOTAL));
		datum.setFrequency(sample.getFrequency(ADDR_DATA_FREQUENCY));
	
		Long whImport = sample.getEnergy(ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT);
		Long whExport = sample.getEnergy(ADDR_DATA_TOTAL_ACTIVE_ENERGY_EXPORT);
	
		if ( backwards ) {
			datum.setWattHourReading(whExport);
			datum.setReverseWattHourReading(whImport);
		} else {
			datum.setWattHourReading(whImport);
			datum.setReverseWattHourReading(whExport);
		}
	
		datum.setApparentPower(sample.getPower(ADDR_DATA_APPARENT_POWER_TOTAL));
		datum.setCurrent(sample.getCurrent(ADDR_DATA_I_AVERAGE));
		datum.setPhaseVoltage(sample.getVoltage(ADDR_DATA_V_L_L_AVERAGE));
		datum.setReactivePower(sample.getPower(ADDR_DATA_REACTIVE_POWER_TOTAL));
		datum.setRealPower(sample.getPower(ADDR_DATA_ACTIVE_POWER_TOTAL));
		datum.setPowerFactor(sample.getPowerFactor(ADDR_DATA_POWER_FACTOR_TOTAL));
		datum.setVoltage(sample.getVoltage(ADDR_DATA_V_NEUTRAL_AVERAGE));
		datum.setWatts((backwards ? -1 : 1) * sample.getPower(ADDR_DATA_ACTIVE_POWER_TOTAL));
	}
	
	private static void populatePhaseAMeasurements(final EM5600Data sample,
			final GeneralNodeACEnergyDatum datum) {
		datum.setApparentPower(sample.getPower(ADDR_DATA_APPARENT_POWER_P1));
		datum.setCurrent(sample.getCurrent(ADDR_DATA_I1));
		datum.setPhaseVoltage(sample.getVoltage(ADDR_DATA_V_L1_L2));
		datum.setReactivePower(sample.getPower(ADDR_DATA_REACTIVE_POWER_P1));
		datum.setRealPower(sample.getPower(ADDR_DATA_ACTIVE_POWER_P1));
		datum.setPowerFactor(sample.getPowerFactor(ADDR_DATA_POWER_FACTOR_P1));
		datum.setVoltage(sample.getVoltage(ADDR_DATA_V_L1_NEUTRAL));
		datum.setWatts((sample.isBackwards() ? -1 : 1) * sample.getPower(ADDR_DATA_ACTIVE_POWER_P1));
	}
	
	private static void populatePhaseBMeasurements(final EM5600Data sample,
			final GeneralNodeACEnergyDatum datum) {
		datum.setApparentPower(sample.getPower(ADDR_DATA_APPARENT_POWER_P2));
		datum.setCurrent(sample.getCurrent(ADDR_DATA_I2));
		datum.setPhaseVoltage(sample.getVoltage(ADDR_DATA_V_L2_L3));
		datum.setReactivePower(sample.getPower(ADDR_DATA_REACTIVE_POWER_P2));
		datum.setRealPower(sample.getPower(ADDR_DATA_ACTIVE_POWER_P2));
		datum.setPowerFactor(sample.getPowerFactor(ADDR_DATA_POWER_FACTOR_P2));
		datum.setVoltage(sample.getVoltage(ADDR_DATA_V_L2_NEUTRAL));
		datum.setWatts((sample.isBackwards() ? -1 : 1) * sample.getPower(ADDR_DATA_ACTIVE_POWER_P2));
	}
	
	private static void populatePhaseCMeasurements(final EM5600Data sample,
			final GeneralNodeACEnergyDatum datum) {
		datum.setApparentPower(sample.getPower(ADDR_DATA_APPARENT_POWER_P3));
		datum.setCurrent(sample.getCurrent(ADDR_DATA_I3));
		datum.setPhaseVoltage(sample.getVoltage(ADDR_DATA_V_L3_L1));
		datum.setReactivePower(sample.getPower(ADDR_DATA_REACTIVE_POWER_P3));
		datum.setRealPower(sample.getPower(ADDR_DATA_ACTIVE_POWER_P3));
		datum.setPowerFactor(sample.getPowerFactor(ADDR_DATA_POWER_FACTOR_P3));
		datum.setVoltage(sample.getVoltage(ADDR_DATA_V_L3_NEUTRAL));
		datum.setWatts((sample.isBackwards() ? -1 : 1) * sample.getPower(ADDR_DATA_ACTIVE_POWER_P3));
	}
	*/
}
