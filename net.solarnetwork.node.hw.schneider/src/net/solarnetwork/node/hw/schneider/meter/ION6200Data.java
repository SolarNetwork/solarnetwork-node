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

import java.math.BigDecimal;
import bak.pcj.set.IntRange;
import bak.pcj.set.IntRangeSet;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusWordOrder;

/**
 * Data object for the ION6200 series meter.
 * 
 * @author matt
 * @version 1.0
 * @since 2.4
 */
public class ION6200Data extends ModbusData implements ION6200DataAccessor {

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
	 * @boolean megawatt {@literal true} if this data is from the Megawatt
	 *          version of the 6200 meter
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
	 */
	public final void readConfigurationData(final ModbusConnection conn) {
		performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				// we actually read ALL registers here, so our snapshot timestamp includes everything
				updateData(conn, m, ION6200Register.getRegisterAddressSet());
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
				updateData(conn, m, ION6200Register.getMeterRegisterAddressSet());
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
	public ION6200DataAccessor dataAccessorForPhase(ACPhase phase) {
		if ( phase == ACPhase.Total ) {
			return this;
		}
		// TODO
		throw new UnsupportedOperationException("Phase measurements not supported yet.");
	}

	@Override
	public Long getSerialNumber() {
		return getInt32(ION6200Register.InfoSerialNumber.getAddress());
	}

	@Override
	public Integer getFirmwareRevision() {
		return getInt16(ION6200Register.InfoFirmwareVersion.getAddress());
	}

	@Override
	public Integer getDeviceType() {
		return getInt16(ION6200Register.InfoDeviceType.getAddress());
	}

	@Override
	public ION6200VoltsMode getVoltsMode() {
		Integer v = getInt16(ION6200Register.ConfigVoltsMode.getAddress());
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
		Short v = getSignedInt16(ION6200Register.MeterFrequency.getAddress());
		return (v != null ? v.floatValue() / 100.0f : null);
	}

	@Override
	public Float getPowerFactor() {
		Number v = getNumber(ION6200Register.MeterPowerFactorTotal);
		return (v != null ? v.floatValue() / 100.0f : null);
	}

	private BigDecimal getProgrammableScale(ION6200Register reg) {
		Integer v = getInt16(reg.getAddress());
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

	private Long getEnergyValue(ION6200Register reg) {
		Long v = getInt32(reg.getAddress());
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

}
