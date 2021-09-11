/* ==================================================================
 * PM3200Data.java - Mar 30, 2014 1:44:23 PM
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

package net.solarnetwork.node.hw.schneider.meter;

import static net.solarnetwork.util.CollectionUtils.coveringIntRanges;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import net.solarnetwork.domain.datum.AcPhase;
import net.solarnetwork.node.domain.AcEnergyDataAccessor;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.util.IntRange;

/**
 * Encapsulates raw Modbus register data from the PM3200 meters.
 * 
 * @author matt
 * @version 3.0
 */
public class PM3200Data extends ModbusData implements PM3200DataAccessor {

	private static final int MAX_RESULTS = 64;

	/**
	 * Constructor.
	 */
	public PM3200Data() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the meter data to copy
	 */
	public PM3200Data(ModbusData other) {
		super(other);
	}

	@Override
	public PM3200Data copy() {
		return new PM3200Data(this);
	}

	@Override
	public Map<String, Object> getDeviceInfo() {
		PM3200DataAccessor data = copy();
		Map<String, Object> result = new LinkedHashMap<>(4);
		String manufacturer = data.getManufacturer();
		if ( manufacturer != null ) {
			result.put(INFO_KEY_DEVICE_MANUFACTURER, manufacturer);
		}
		String model = data.getModel();
		if ( model != null ) {
			String version = data.getFirmwareRevision();
			if ( version != null ) {
				result.put(INFO_KEY_DEVICE_MODEL, String.format("%s (version %s)", model, version));
			} else {
				result.put(INFO_KEY_DEVICE_MODEL, model.toString());
			}
		}
		Long sn = data.getSerialNumber();
		if ( sn != null ) {
			result.put(INFO_KEY_DEVICE_SERIAL_NUMBER, sn);
		}
		LocalDateTime date = data.getManufactureDate();
		if ( date != null ) {
			result.put(INFO_KEY_DEVICE_MANUFACTURE_DATE, date.toLocalDate());
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
		performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) throws IOException {
				// we actually read ALL registers here, so our snapshot timestamp includes everything
				updateData(conn, m,
						coveringIntRanges(PM3200Register.getRegisterAddressSet(), MAX_RESULTS));
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
						coveringIntRanges(PM3200Register.getMeterRegisterAddressSet(), MAX_RESULTS));
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
	 * Get an effective voltage value in V.
	 * 
	 * @param addr
	 *        the register address to read
	 * @return the value interpreted as a voltage value
	 */
	public Float getVoltage(final int addr) {
		return getFloat32(addr);
	}

	/**
	 * Get an effective current value in A.
	 * 
	 * @param addr
	 *        the register address to read
	 * @return the value interpreted as a current value
	 */
	public Float getCurrent(int addr) {
		return getFloat32(addr);
	}

	/**
	 * Get an effective frequency value in Hz.
	 * 
	 * @param addr
	 *        the register address to read
	 * @return the value interpreted as a frequency value
	 */
	public Float getFrequency(int addr) {
		return getFloat32(addr);
	}

	/**
	 * Get an effective temperature value in C.
	 * 
	 * @param addr
	 *        the register address to read
	 * @return the value interpreted as a temperature value
	 */
	public Float getTemperature(int addr) {
		return getFloat32(addr);
	}

	/**
	 * Get an effective power factor value.
	 * 
	 * @param addr
	 *        the register address to read
	 * @return the value interpreted as a power factor value
	 */
	public Float getPowerFactor(int addr) {
		return getFloat32(addr);
	}

	/**
	 * Get the effective total power factor, in terms of cos(phi). The result
	 * range is from -1 to 1.
	 * 
	 * @return the effective power factor
	 */
	public Float getEffectiveTotalPowerFactor() {
		Float tangentPhi = getFloat32(PM3200Register.MeterReactivePowerFactorTotal.getAddress());
		if ( tangentPhi == null ) {
			return null;
		}
		float result = (float) (1.0
				/ Math.sqrt(1 + tangentPhi.doubleValue() * tangentPhi.doubleValue()));
		if ( tangentPhi.floatValue() < 0 ) {
			result = -result;
		}
		return result;
	}

	/**
	 * Get an effective power value in W (active), Var (reactive) or VA
	 * (apparent).
	 * 
	 * @param addr
	 *        the register address to read
	 * @return the value interpreted as a power value
	 */
	public Integer getPower(int addr) {
		Float kiloValue = getFloat32(addr);
		if ( kiloValue == null ) {
			return null;
		}
		return Integer.valueOf((int) Math.ceil(kiloValue.doubleValue() * 1000.0));
	}

	/**
	 * Get an effective energy value in Wh (real), Varh (reactive).
	 * 
	 * @param addr
	 *        the register address to read
	 * @return the value interpreted as an energy value
	 */
	public Long getEnergy(int addr) {
		return getInt64(addr);
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
	public PM3200DataAccessor dataAccessorForPhase(AcPhase phase) {
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

	public Integer getPowerValue(PM3200Register reg) {
		Number n = getNumber(reg);
		return (n != null ? Math.round(n.floatValue() * 1000.0f) : null);
	}

	private Float getCurrentValue(PM3200Register reg) {
		Number n = getNumber(reg);
		return (n != null ? n.floatValue() : null);
	}

	private Float getVoltageValue(PM3200Register reg) {
		Number n = getNumber(reg);
		return (n != null ? n.floatValue() : null);
	}

	private Long getEnergyValue(PM3200Register reg) {
		Number n = getNumber(reg);
		return (n != null ? n.longValue() : null);
	}

	@Override
	public Long getSerialNumber() {
		Number n = getNumber(PM3200Register.InfoSerialNumber);
		return (n != null ? n.longValue() : null);
	}

	@Override
	public String getFirmwareRevision() {
		Number n = getNumber(PM3200Register.InfoFirmwareRevision);
		if ( n == null ) {
			return null;
		}
		String s = n.toString();
		if ( s.length() < 3 ) {
			return null;
		}
		return String.format("%c.%c.%s", s.charAt(0), s.charAt(1), s.substring(2));
	}

	@Override
	public String getModel() {
		return getUtf8String(PM3200Register.InfoModel, true);
	}

	@Override
	public String getName() {
		return getUtf8String(PM3200Register.InfoName, true);
	}

	@Override
	public String getManufacturer() {
		return getUtf8String(PM3200Register.InfoManufacturer, true);
	}

	@Override
	public LocalDateTime getManufactureDate() {
		PM3200Register r = PM3200Register.InfoManufactureDate;
		short[] data = new short[r.getWordLength()];
		slice(data, 0, r.getAddress(), r.getWordLength());
		return DataUtils.parseDateTime(data);
	}

	@Override
	public PowerSystem getPowerSystem() {
		Number n = getNumber(PM3200Register.ConfigPowerSystem);
		PowerSystem m = null;
		if ( n != null ) {
			try {
				m = PowerSystem.forCode(n.intValue());
			} catch ( IllegalArgumentException e ) {
				// ignore
			}
		}
		return m;
	}

	@Override
	public Integer getPhaseCount() {
		return getUnsignedInt16(PM3200Register.ConfigNumPhases.getAddress());
	}

	@Override
	public Integer getWireCount() {
		return getUnsignedInt16(PM3200Register.ConfigNumWires.getAddress());
	}

	@Override
	public Float getFrequency() {
		Float v = getFloat32(PM3200Register.MeterFrequency.getAddress());
		return (v != null ? v.floatValue() : null);
	}

	@Override
	public Float getPowerFactor() {
		Number v = getNumber(PM3200Register.MeterPowerFactorTotal);
		return (v != null ? v.floatValue() : null);
	}

	@Override
	public Integer getActivePower() {
		return getPowerValue(PM3200Register.MeterActivePowerTotal);
	}

	@Override
	public Integer getApparentPower() {
		return getPowerValue(PM3200Register.MeterApparentPowerTotal);
	}

	@Override
	public Integer getReactivePower() {
		return getPowerValue(PM3200Register.MeterReactivePowerTotal);
	}

	@Override
	public Float getCurrent() {
		return getCurrentValue(PM3200Register.MeterCurrentAverage);
	}

	@Override
	public Float getNeutralCurrent() {
		return getCurrentValue(PM3200Register.MeterCurrentNeutral);
	}

	@Override
	public Float getVoltage() {
		return getVoltageValue(PM3200Register.MeterVoltageLineNeutralAverage);
	}

	@Override
	public Float getLineVoltage() {
		return getVoltageValue(PM3200Register.MeterVoltageLineLineAverage);
	}

	@Override
	public Long getActiveEnergyDelivered() {
		return getEnergyValue(PM3200Register.MeterActiveEnergyDelivered);
	}

	@Override
	public Long getActiveEnergyReceived() {
		return getEnergyValue(PM3200Register.MeterActiveEnergyReceived);
	}

	@Override
	public Long getReactiveEnergyDelivered() {
		return getEnergyValue(PM3200Register.MeterReactiveEnergyDelivered);
	}

	@Override
	public Long getReactiveEnergyReceived() {
		return getEnergyValue(PM3200Register.MeterReactiveEnergyReceived);
	}

	@Override
	public Long getApparentEnergyDelivered() {
		return getEnergyValue(PM3200Register.MeterApparentEnergyDelivered);
	}

	@Override
	public Long getApparentEnergyReceived() {
		return getEnergyValue(PM3200Register.MeterApparentEnergyReceived);
	}

	private class PhaseMeterDataAccessor implements PM3200DataAccessor {

		private final AcPhase phase;

		private PhaseMeterDataAccessor(AcPhase phase) {
			super();
			this.phase = phase;
		}

		@Override
		public Map<String, Object> getDeviceInfo() {
			return PM3200Data.this.getDeviceInfo();
		}

		@Override
		public Long getSerialNumber() {
			return PM3200Data.this.getSerialNumber();
		}

		@Override
		public String getModel() {
			return PM3200Data.this.getModel();
		}

		@Override
		public String getName() {
			return PM3200Data.this.getName();
		}

		@Override
		public LocalDateTime getManufactureDate() {
			return PM3200Data.this.getManufactureDate();
		}

		@Override
		public String getManufacturer() {
			return PM3200Data.this.getManufacturer();
		}

		@Override
		public Integer getPhaseCount() {
			return PM3200Data.this.getPhaseCount();
		}

		@Override
		public Integer getWireCount() {
			return PM3200Data.this.getWireCount();
		}

		@Override
		public String getFirmwareRevision() {
			return PM3200Data.this.getFirmwareRevision();
		}

		@Override
		public PowerSystem getPowerSystem() {
			return PM3200Data.this.getPowerSystem();
		}

		@Override
		public long getDataTimestamp() {
			return PM3200Data.this.getDataTimestamp();
		}

		@Override
		public AcEnergyDataAccessor accessorForPhase(AcPhase phase) {
			return PM3200Data.this.accessorForPhase(phase);
		}

		@Override
		public AcEnergyDataAccessor reversed() {
			return new ReversedMeterDataAccessor(this);
		}

		@Override
		public Float getFrequency() {
			return PM3200Data.this.getFrequency();
		}

		@Override
		public Float getCurrent() {
			Float n = null;
			switch (phase) {
				case PhaseA:
					n = getCurrentValue(PM3200Register.MeterCurrentPhaseA);
					break;

				case PhaseB:
					n = getCurrentValue(PM3200Register.MeterCurrentPhaseB);
					break;

				case PhaseC:
					n = getCurrentValue(PM3200Register.MeterCurrentPhaseC);
					break;

				default:
					n = PM3200Data.this.getCurrent();
			}
			return n;
		}

		@Override
		public Float getNeutralCurrent() {
			return PM3200Data.this.getNeutralCurrent();
		}

		@Override
		public Float getVoltage() {
			Float n = null;
			switch (phase) {
				case PhaseA:
					n = getVoltageValue(PM3200Register.MeterVoltageLineNeutralPhaseA);
					break;

				case PhaseB:
					n = getVoltageValue(PM3200Register.MeterVoltageLineNeutralPhaseB);
					break;

				case PhaseC:
					n = getVoltageValue(PM3200Register.MeterVoltageLineNeutralPhaseC);
					break;

				default:
					n = PM3200Data.this.getVoltage();
			}
			return n;
		}

		@Override
		public Float getLineVoltage() {
			Float n = null;
			switch (phase) {
				case PhaseA:
					n = getVoltageValue(PM3200Register.MeterVoltageLineLinePhaseAPhaseB);
					break;

				case PhaseB:
					n = getVoltageValue(PM3200Register.MeterVoltageLineLinePhaseBPhaseC);
					break;

				case PhaseC:
					n = getVoltageValue(PM3200Register.MeterVoltageLineLinePhaseCPhaseA);
					break;

				default:
					n = PM3200Data.this.getLineVoltage();
			}
			return n;
		}

		@Override
		public Float getPowerFactor() {
			return PM3200Data.this.getPowerFactor();
		}

		@Override
		public Integer getActivePower() {
			Integer n = null;
			switch (phase) {
				case PhaseA:
					n = getPowerValue(PM3200Register.MeterActivePowerPhaseA);
					break;

				case PhaseB:
					n = getPowerValue(PM3200Register.MeterActivePowerPhaseB);
					break;

				case PhaseC:
					n = getPowerValue(PM3200Register.MeterActivePowerPhaseC);
					break;

				default:
					n = PM3200Data.this.getActivePower();
			}
			return n;
		}

		@Override
		public Integer getApparentPower() {
			return PM3200Data.this.getApparentPower();
		}

		@Override
		public Integer getReactivePower() {
			return PM3200Data.this.getReactivePower();
		}

		@Override
		public Long getActiveEnergyDelivered() {
			Long n = null;
			switch (phase) {
				case PhaseA:
					n = getEnergyValue(PM3200Register.MeterActiveEnergyDeliveredPhaseA);
					break;
				case PhaseB:
					n = getEnergyValue(PM3200Register.MeterActiveEnergyDeliveredPhaseB);
					break;
				case PhaseC:
					n = getEnergyValue(PM3200Register.MeterActiveEnergyDeliveredPhaseC);
					break;

				default:
					n = PM3200Data.this.getActiveEnergyDelivered();

			}
			return n;
		}

		@Override
		public Long getActiveEnergyReceived() {
			return PM3200Data.this.getActiveEnergyReceived();
		}

		@Override
		public Long getReactiveEnergyDelivered() {
			Long n = null;
			switch (phase) {
				case PhaseA:
					n = getEnergyValue(PM3200Register.MeterReactiveEnergyDeliveredPhaseA);
					break;
				case PhaseB:
					n = getEnergyValue(PM3200Register.MeterReactiveEnergyDeliveredPhaseB);
					break;
				case PhaseC:
					n = getEnergyValue(PM3200Register.MeterReactiveEnergyDeliveredPhaseC);
					break;

				default:
					n = PM3200Data.this.getReactiveEnergyDelivered();

			}
			return n;
		}

		@Override
		public Long getReactiveEnergyReceived() {
			return PM3200Data.this.getReactiveEnergyReceived();
		}

		@Override
		public Long getApparentEnergyDelivered() {
			Long n = null;
			switch (phase) {
				case PhaseA:
					n = getEnergyValue(PM3200Register.MeterApparentEnergyDeliveredPhaseA);
					break;
				case PhaseB:
					n = getEnergyValue(PM3200Register.MeterApparentEnergyDeliveredPhaseB);
					break;
				case PhaseC:
					n = getEnergyValue(PM3200Register.MeterApparentEnergyDeliveredPhaseC);
					break;

				default:
					n = PM3200Data.this.getApparentEnergyDelivered();

			}
			return n;
		}

		@Override
		public Long getApparentEnergyReceived() {
			return PM3200Data.this.getApparentEnergyReceived();
		}

	}

	private static class ReversedMeterDataAccessor implements PM3200DataAccessor {

		private final PM3200DataAccessor delegate;

		private ReversedMeterDataAccessor(PM3200DataAccessor delegate) {
			super();
			this.delegate = delegate;
		}

		@Override
		public Map<String, Object> getDeviceInfo() {
			return delegate.getDeviceInfo();
		}

		@Override
		public AcEnergyDataAccessor accessorForPhase(AcPhase phase) {
			return new ReversedMeterDataAccessor((PM3200DataAccessor) delegate.accessorForPhase(phase));
		}

		@Override
		public Long getSerialNumber() {
			return delegate.getSerialNumber();
		}

		@Override
		public String getModel() {
			return delegate.getModel();
		}

		@Override
		public String getName() {
			return delegate.getName();
		}

		@Override
		public LocalDateTime getManufactureDate() {
			return delegate.getManufactureDate();
		}

		@Override
		public String getManufacturer() {
			return delegate.getManufacturer();
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
		public PowerSystem getPowerSystem() {
			return delegate.getPowerSystem();
		}

		@Override
		public String getFirmwareRevision() {
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
