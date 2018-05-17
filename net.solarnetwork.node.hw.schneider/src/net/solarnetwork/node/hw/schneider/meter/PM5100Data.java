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

import bak.pcj.set.IntRange;
import bak.pcj.set.IntRangeSet;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Data object for the PM5100 series meter.
 * 
 * @author matt
 * @version 1.0
 * @since 2.4
 */
public class PM5100Data extends ModbusData implements PM5100DataAccessor {

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
				updateData(conn, m, PM5100Register.getRegisterAddressSet());
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
				updateData(conn, m, PM5100Register.getMeterRegisterAddressSet());
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
		// TODO
		throw new UnsupportedOperationException("Phase measurements not supported yet.");
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

}
