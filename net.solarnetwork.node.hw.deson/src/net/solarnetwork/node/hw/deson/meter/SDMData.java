/* ==================================================================
 * SDMData.java - 25/01/2016 5:52:26 pm
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.deson.meter;

import static net.solarnetwork.util.CollectionUtils.coveringIntRanges;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.node.domain.ACEnergyDataAccessor;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusDataUtils;
import net.solarnetwork.node.io.modbus.ModbusDeviceSupport;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;
import net.solarnetwork.util.IntRange;
import net.solarnetwork.util.IntShortMap;
import net.solarnetwork.util.NumberUtils;

/**
 * Encapsulates raw Modbus register data from the SDM meters.
 * 
 * @author matt
 * @version 2.0
 */
public class SDMData extends ModbusData implements SDMDataAccessor {

	/** An information key for the wiring type. */
	public static final String INFO_KEY_DEVICE_WIRING_TYPE = "Wiring Type";

	/** The maximum number of Modbus registers to read at once. */
	protected static final int MAX_RESULTS = 64;

	private final IntShortMap controlRegisters;
	private long controlDataTimestamp = 0;
	private SDMDeviceType deviceType;

	/**
	 * Default constructor.
	 */
	public SDMData() {
		super();
		this.controlRegisters = new IntShortMap(8);
		this.deviceType = SDMDeviceType.SDM120;
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the object to copy
	 */
	public SDMData(SDMData other) {
		super(other);
		synchronized ( other.controlRegisters ) {
			this.controlRegisters = (IntShortMap) other.controlRegisters.clone();
			this.controlDataTimestamp = other.controlDataTimestamp;
			this.deviceType = other.deviceType;
		}
	}

	@Override
	public SDMData copy() {
		return new SDMData(this);
	}

	/**
	 * Get a brief operational status message.
	 * 
	 * @return the message
	 */
	public String getOperationStatusMessage() {
		StringBuilder buf = new StringBuilder();
		buf.append("W = ").append(getActivePower());
		buf.append(", VA = ").append(getApparentPower());
		buf.append(", Wh = ").append(getActiveEnergyDelivered());
		buf.append(", PF = ").append(getPowerFactor());
		return buf.toString();
	}

	@Override
	public final long getControlDataTimestamp() {
		return controlDataTimestamp;
	}

	/**
	 * Perform a set of updates to saved control register data.
	 * 
	 * @param action
	 *        the callback to perform the updates on
	 * @return this object to allow method chaining
	 * @since 2.0
	 */
	public final SDMData performControlUpdates(ModbusDataUpdateAction action) {
		synchronized ( controlRegisters ) {
			final long now = System.currentTimeMillis();
			if ( action.updateModbusData(new MutableModbusDataView(controlRegisters, getWordOrder())) ) {
				controlDataTimestamp = now;
			}
		}
		return this;
	}

	/**
	 * Force the data timestamp to be expired.
	 * 
	 * <p>
	 * Calling this method will reset the {@code dataTimestamp} to zero,
	 * effectively expiring the data.
	 * </p>
	 * 
	 * @return this object to allow method chaining
	 * @since 2.0
	 */
	public final ModbusData expireControl() {
		synchronized ( controlRegisters ) {
			controlDataTimestamp = 0;
		}
		return this;
	}

	private final Float getControlFloat32(final int addr) {
		return ModbusDataUtils.parseFloat32(controlRegisters.getValue(addr),
				controlRegisters.getValue(addr + 1));
	}

	private Float getVoltageValue(final ModbusReference ref) {
		return getFloat32(ref.getAddress());
	}

	private Float getCurrentValue(final ModbusReference ref) {
		return getFloat32(ref.getAddress());
	}

	private Float getFrequencyValue(final ModbusReference ref) {
		return getFloat32(ref.getAddress());
	}

	private Float getPowerFactor(final ModbusReference ref) {
		return getFloat32(ref.getAddress());
	}

	private Integer getPowerValue(final ModbusReference ref) {
		Float value = getFloat32(ref.getAddress());
		if ( value == null ) {
			return null;
		}
		return Integer.valueOf((int) (Math.round(value.doubleValue())));
	}

	private Long getEnergyValue(final ModbusReference ref) {
		Float value = getFloat32(ref.getAddress());
		if ( value == null ) {
			return null;
		}
		return NumberUtils.maximumDecimalScale(NumberUtils.scaled(value, 3), 0).longValue();
	}

	@Override
	public SDMDeviceType getDeviceType() {
		return deviceType;
	}

	/**
	 * Set the device type.
	 * 
	 * @param deviceType
	 *        the device type
	 */
	public void setDeviceType(SDMDeviceType deviceType) {
		synchronized ( controlRegisters ) {
			this.deviceType = deviceType;
		}
	}

	@Override
	public SDMWiringMode getWiringMode() {
		if ( deviceType == SDMDeviceType.SDM630 ) {
			final Float wiringType = getControlFloat32(
					SDM630Register.ControlConfigWiringMode.getAddress());
			if ( wiringType == null ) {
				return null;
			}
			final int type = wiringType.intValue();
			try {
				return SDMWiringMode.valueOf(type);
			} catch ( IllegalArgumentException e ) {
				return null;
			}
		} else {
			return SDMWiringMode.OnePhaseTwoWire;
		}
	}

	private String getWiringType() {
		SDMWiringMode mode = getWiringMode();
		if ( mode == null ) {
			return "N/A";
		}
		return mode.getDescription();
	}

	public String getSerialNumber() {
		final Float serialNumber;
		if ( deviceType == SDMDeviceType.SDM630 ) {
			serialNumber = getControlFloat32(SDM630Register.ControlInfoSerialNumber.getAddress());
		} else {
			serialNumber = null;
		}
		return (serialNumber == null ? "N/A" : serialNumber.toString());
	}

	@Override
	public Map<String, Object> getDeviceInfo() {
		Map<String, Object> result = new LinkedHashMap<String, Object>(4);
		result.put(ModbusDeviceSupport.INFO_KEY_DEVICE_MODEL, deviceType.getDescription());
		result.put(INFO_KEY_DEVICE_WIRING_TYPE, getWiringType());
		if ( deviceType == SDMDeviceType.SDM630 ) {
			result.put(ModbusDeviceSupport.INFO_KEY_DEVICE_SERIAL_NUMBER, getSerialNumber());
		}
		return result;
	}

	@Override
	public boolean supportsPhase(ACPhase phase) {
		if ( phase == ACPhase.Total ) {
			return true;
		}
		SDMWiringMode wiringMode = getWiringMode();
		if ( wiringMode == null ) {
			return false;
		}
		if ( wiringMode == SDMWiringMode.OnePhaseTwoWire ) {
			// only the Total phase is supported for 1P2
			return false;
		}
		return true;
	}

	/**
	 * Read the configuration and information registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 */
	public final void readConfigurationData(final ModbusConnection conn) {
		// we actually read ALL registers here, so our snapshot timestamp includes everything
		readMeterData(conn);
		if ( deviceType == SDMDeviceType.SDM630 ) {
			final List<IntRange> ranges = coveringIntRanges(
					SDM630Register.getControlRegisterAddressSet(), MAX_RESULTS);
			performControlUpdates(new ModbusDataUpdateAction() {

				@Override
				public boolean updateModbusData(MutableModbusData m) {
					refreshData(conn, ModbusReadFunction.ReadHoldingRegister, ranges, m);
					return true;
				}
			});
		}
	}

	/**
	 * Read the meter registers from the device.
	 * 
	 * @param conn
	 *        the connection
	 */
	public final void readMeterData(final ModbusConnection conn) {
		refreshData(conn, ModbusReadFunction.ReadInputRegister,
				SDM630Register.getMeterRegisterAddressSet(), MAX_RESULTS);
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
	public Float getFrequency() {
		switch (deviceType) {
			case SDM630:
				return getFrequencyValue(SDM630Register.MeterFrequency);

			default:
				return getFrequencyValue(SDM120Register.MeterFrequency);
		}
	}

	@Override
	public Float getCurrent() {
		switch (deviceType) {
			case SDM630:
				if ( getWiringMode() == SDMWiringMode.OnePhaseTwoWire ) {
					return getCurrentValue(SDM630Register.MeterCurrentPhaseA);
				}
				return getCurrentValue(SDM630Register.MeterCurrentAverage);

			default:
				return getCurrentValue(SDM120Register.MeterCurrentAverage);
		}
	}

	@Override
	public Float getNeutralCurrent() {
		switch (deviceType) {
			case SDM630:
				return getCurrentValue(SDM630Register.MeterCurrentNeutral);

			default:
				return null;
		}
	}

	@Override
	public Float getVoltage() {
		switch (deviceType) {
			case SDM630:
				if ( getWiringMode() == SDMWiringMode.OnePhaseTwoWire ) {
					return getVoltageValue(SDM630Register.MeterVoltageLineNeutralPhaseA);
				}
				return getVoltageValue(SDM630Register.MeterVoltageLineNeutralAverage);

			default:
				return getVoltageValue(SDM120Register.MeterVoltageLineNeutralAverage);
		}
	}

	@Override
	public Float getLineVoltage() {
		switch (deviceType) {
			case SDM630:
				return getVoltageValue(SDM630Register.MeterVoltageLineLineAverage);

			default:
				return null;
		}
	}

	@Override
	public Float getPowerFactor() {
		switch (deviceType) {
			case SDM630:
				return getPowerFactor(SDM630Register.MeterPowerFactorTotal);

			default:
				return getPowerFactor(SDM120Register.MeterPowerFactorTotal);
		}
	}

	@Override
	public Integer getActivePower() {
		switch (deviceType) {
			case SDM630:
				return getPowerValue(SDM630Register.MeterActivePowerTotal);

			default:
				return getPowerValue(SDM120Register.MeterActivePowerTotal);
		}
	}

	@Override
	public Long getActiveEnergyDelivered() {
		switch (deviceType) {
			case SDM630:
				return getEnergyValue(SDM630Register.MeterActiveEnergyDelivered);

			default:
				return getEnergyValue(SDM120Register.MeterActiveEnergyDelivered);
		}
	}

	@Override
	public Long getActiveEnergyReceived() {
		switch (deviceType) {
			case SDM630:
				return getEnergyValue(SDM630Register.MeterActiveEnergyReceived);

			default:
				return getEnergyValue(SDM120Register.MeterActiveEnergyReceived);
		}
	}

	@Override
	public Integer getApparentPower() {
		switch (deviceType) {
			case SDM630:
				return getPowerValue(SDM630Register.MeterApparentPowerTotal);

			default:
				return getPowerValue(SDM120Register.MeterApparentPowerTotal);
		}
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
		switch (deviceType) {
			case SDM630:
				return getPowerValue(SDM630Register.MeterReactivePowerTotal);

			default:
				return getPowerValue(SDM120Register.MeterReactivePowerTotal);
		}
	}

	@Override
	public Long getReactiveEnergyDelivered() {
		switch (deviceType) {
			case SDM630:
				return getEnergyValue(SDM630Register.MeterReactiveEnergyDelivered);

			default:
				return getEnergyValue(SDM120Register.MeterReactiveEnergyDelivered);
		}
	}

	@Override
	public Long getReactiveEnergyReceived() {
		switch (deviceType) {
			case SDM630:
				return getEnergyValue(SDM630Register.MeterReactiveEnergyReceived);

			default:
				return getEnergyValue(SDM120Register.MeterReactiveEnergyReceived);
		}
	}

	private class PhaseMeterDataAccessor implements SDMDataAccessor {

		private final ACPhase phase;

		private PhaseMeterDataAccessor(ACPhase phase) {
			super();
			this.phase = phase;
		}

		@Override
		public SDMDeviceType getDeviceType() {
			return SDMData.this.getDeviceType();
		}

		@Override
		public Map<String, Object> getDeviceInfo() {
			return SDMData.this.getDeviceInfo();
		}

		@Override
		public long getDataTimestamp() {
			return SDMData.this.getDataTimestamp();
		}

		@Override
		public ACEnergyDataAccessor accessorForPhase(ACPhase phase) {
			return SDMData.this.accessorForPhase(phase);
		}

		@Override
		public ACEnergyDataAccessor reversed() {
			return new ReversedMeterDataAccessor(this);
		}

		@Override
		public boolean supportsPhase(ACPhase phase) {
			return SDMData.this.supportsPhase(phase);
		}

		@Override
		public SDMWiringMode getWiringMode() {
			return SDMData.this.getWiringMode();
		}

		@Override
		public long getControlDataTimestamp() {
			return SDMData.this.getControlDataTimestamp();
		}

		@Override
		public Float getFrequency() {
			return SDMData.this.getFrequency();
		}

		@Override
		public Float getCurrent() {
			Float n = null;
			switch (phase) {
				case PhaseA:
					n = SDMData.this.getCurrentValue(SDM630Register.MeterCurrentPhaseA);
					break;

				case PhaseB:
					n = SDMData.this.getCurrentValue(SDM630Register.MeterCurrentPhaseB);
					break;

				case PhaseC:
					n = SDMData.this.getCurrentValue(SDM630Register.MeterCurrentPhaseC);
					break;

				default:
					n = SDMData.this.getCurrent();
			}
			return n;
		}

		@Override
		public Float getNeutralCurrent() {
			return SDMData.this.getNeutralCurrent();
		}

		@Override
		public Float getVoltage() {
			Float n = null;
			switch (phase) {
				case PhaseA:
					n = SDMData.this.getVoltageValue(SDM630Register.MeterVoltageLineNeutralPhaseA);
					break;

				case PhaseB:
					n = SDMData.this.getVoltageValue(SDM630Register.MeterVoltageLineNeutralPhaseB);
					break;

				case PhaseC:
					n = SDMData.this.getVoltageValue(SDM630Register.MeterVoltageLineNeutralPhaseC);
					break;

				default:
					n = SDMData.this.getVoltage();
			}
			return n;
		}

		@Override
		public Float getLineVoltage() {
			Float n = null;
			switch (phase) {
				case PhaseA:
					n = SDMData.this.getVoltageValue(SDM630Register.MeterVoltageLineLinePhaseAPhaseB);
					break;

				case PhaseB:
					n = SDMData.this.getVoltageValue(SDM630Register.MeterVoltageLineLinePhaseBPhaseC);
					break;

				case PhaseC:
					n = SDMData.this.getVoltageValue(SDM630Register.MeterVoltageLineLinePhaseCPhaseA);
					break;

				default:
					n = SDMData.this.getLineVoltage();
			}
			return n;
		}

		@Override
		public Float getPowerFactor() {
			Float n = null;
			switch (phase) {
				case PhaseA:
					n = SDMData.this.getPowerFactor(SDM630Register.MeterPowerFactorPhaseA);
					break;

				case PhaseB:
					n = SDMData.this.getPowerFactor(SDM630Register.MeterPowerFactorPhaseB);
					break;

				case PhaseC:
					n = SDMData.this.getPowerFactor(SDM630Register.MeterPowerFactorPhaseC);
					break;

				default:
					n = SDMData.this.getPowerFactor();
			}
			return n;
		}

		@Override
		public Integer getActivePower() {
			Integer n = null;
			switch (phase) {
				case PhaseA:
					n = SDMData.this.getPowerValue(SDM630Register.MeterActivePowerPhaseA);
					break;

				case PhaseB:
					n = SDMData.this.getPowerValue(SDM630Register.MeterActivePowerPhaseB);
					break;

				case PhaseC:
					n = SDMData.this.getPowerValue(SDM630Register.MeterActivePowerPhaseC);
					break;

				default:
					n = SDMData.this.getActivePower();
			}
			return n;
		}

		@Override
		public Integer getApparentPower() {
			Integer n = null;
			switch (phase) {
				case PhaseA:
					n = SDMData.this.getPowerValue(SDM630Register.MeterApparentPowerPhaseA);
					break;

				case PhaseB:
					n = SDMData.this.getPowerValue(SDM630Register.MeterApparentPowerPhaseB);
					break;

				case PhaseC:
					n = SDMData.this.getPowerValue(SDM630Register.MeterApparentPowerPhaseC);
					break;

				default:
					n = SDMData.this.getApparentPower();
			}
			return n;
		}

		@Override
		public Integer getReactivePower() {
			Integer n = null;
			switch (phase) {
				case PhaseA:
					n = SDMData.this.getPowerValue(SDM630Register.MeterReactivePowerPhaseA);
					break;

				case PhaseB:
					n = SDMData.this.getPowerValue(SDM630Register.MeterReactivePowerPhaseB);
					break;

				case PhaseC:
					n = SDMData.this.getPowerValue(SDM630Register.MeterReactivePowerPhaseC);
					break;

				default:
					n = SDMData.this.getReactivePower();
			}
			return n;
		}

		@Override
		public Long getActiveEnergyDelivered() {
			switch (phase) {
				case Total:
					return SDMData.this.getActiveEnergyDelivered();

				default:
					return null;
			}
		}

		@Override
		public Long getActiveEnergyReceived() {
			switch (phase) {
				case Total:
					return SDMData.this.getActiveEnergyReceived();

				default:
					return null;
			}
		}

		@Override
		public Long getReactiveEnergyDelivered() {
			switch (phase) {
				case Total:
					return SDMData.this.getReactiveEnergyDelivered();

				default:
					return null;
			}
		}

		@Override
		public Long getReactiveEnergyReceived() {
			switch (phase) {
				case Total:
					return SDMData.this.getReactiveEnergyReceived();

				default:
					return null;
			}
		}

		@Override
		public Long getApparentEnergyDelivered() {
			return null;
		}

		@Override
		public Long getApparentEnergyReceived() {
			return null;
		}

	}

	private static class ReversedMeterDataAccessor implements SDMDataAccessor {

		private final SDMDataAccessor delegate;

		private ReversedMeterDataAccessor(SDMDataAccessor delegate) {
			super();
			this.delegate = delegate;
		}

		@Override
		public boolean supportsPhase(ACPhase phase) {
			return delegate.supportsPhase(phase);
		}

		@Override
		public SDMDeviceType getDeviceType() {
			return delegate.getDeviceType();
		}

		@Override
		public SDMWiringMode getWiringMode() {
			return delegate.getWiringMode();
		}

		@Override
		public long getControlDataTimestamp() {
			return delegate.getControlDataTimestamp();
		}

		@Override
		public Map<String, Object> getDeviceInfo() {
			return delegate.getDeviceInfo();
		}

		@Override
		public ACEnergyDataAccessor accessorForPhase(ACPhase phase) {
			return new ReversedMeterDataAccessor((SDMDataAccessor) delegate.accessorForPhase(phase));
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
