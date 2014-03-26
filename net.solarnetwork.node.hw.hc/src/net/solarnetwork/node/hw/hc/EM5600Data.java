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

import java.util.Arrays;
import net.solarnetwork.node.io.modbus.ModbusHelper;
import net.wimpi.modbus.net.SerialConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates raw Modbus register data from the EM5600 meters.
 * 
 * @author matt
 * @version 1.0
 */
public class EM5600Data {

	// meter info
	public static final Integer ADDR_SYSTEM_METER_MODEL = 0x0;
	public static final Integer ADDR_SYSTEM_METER_HARDWARE_VERSION = 0x2; // length 2 ASCII characters
	public static final Integer ADDR_SYSTEM_METER_SERIAL_NUMBER = 0x10; // length 4 ASCII characters
	public static final Integer ADDR_SYSTEM_METER_MANUFACTURE_DATE = 0x18; // length 2 F10 encoding

	// current
	public static final Integer ADDR_DATA_I1 = 0x130;
	public static final Integer ADDR_DATA_I2 = 0x131;
	public static final Integer ADDR_DATA_I3 = 0x132;
	public static final Integer ADDR_DATA_I_AVERAGE = 0x133;

	// voltage
	public static final Integer ADDR_DATA_V_L1_NEUTRAL = 0x136;
	public static final Integer ADDR_DATA_V_L2_NEUTRAL = 0x137;
	public static final Integer ADDR_DATA_V_L3_NEUTRAL = 0x138;
	public static final Integer ADDR_DATA_V_NEUTRAL_AVERAGE = 0x139;
	public static final Integer ADDR_DATA_V_L1_L2 = 0x13B;
	public static final Integer ADDR_DATA_V_L2_L3 = 0x13C;
	public static final Integer ADDR_DATA_V_L3_L1 = 0x13D;
	public static final Integer ADDR_DATA_V_L_L_AVERAGE = 0x13E;

	// power
	public static final Integer ADDR_DATA_ACTIVE_POWER_TOTAL = 0x140;
	public static final Integer ADDR_DATA_REACTIVE_POWER_TOTAL = 0x141;
	public static final Integer ADDR_DATA_APPARENT_POWER_TOTAL = 0x142;
	public static final Integer ADDR_DATA_POWER_FACTOR_TOTAL = 0x143;
	public static final Integer ADDR_DATA_FREQUENCY = 0x144;
	public static final Integer ADDR_DATA_ACTIVE_POWER_P1 = 0x145;
	public static final Integer ADDR_DATA_REACTIVE_POWER_P1 = 0x146;
	public static final Integer ADDR_DATA_APPARENT_POWER_P1 = 0x147;
	public static final Integer ADDR_DATA_POWER_FACTOR_P1 = 0x148;
	public static final Integer ADDR_DATA_ACTIVE_POWER_P2 = 0x149;
	public static final Integer ADDR_DATA_REACTIVE_POWER_P2 = 0x14A;
	public static final Integer ADDR_DATA_APPARENT_POWER_P2 = 0x14B;
	public static final Integer ADDR_DATA_POWER_FACTOR_P2 = 0x14C;
	public static final Integer ADDR_DATA_ACTIVE_POWER_P3 = 0x14D;
	public static final Integer ADDR_DATA_REACTIVE_POWER_P3 = 0x14E;
	public static final Integer ADDR_DATA_APPARENT_POWER_P3 = 0x14F;
	public static final Integer ADDR_DATA_POWER_FACTOR_P3 = 0x150;
	public static final Integer ADDR_DATA_PHASE_ROTATION = 0x151;

	// energy
	public static final Integer ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT = 0x160; // length 2
	public static final Integer ADDR_DATA_TOTAL_ACTIVE_ENERGY_EXPORT = 0x162;
	public static final Integer ADDR_DATA_TOTAL_REACTIVE_ENERGY_IMPORT = 0x164;
	public static final Integer ADDR_DATA_TOTAL_REACTIVE_ENERGY_EXPORT = 0x166;

	// units
	public static final Integer ADDR_DATA_ENERGY_UNIT = 0x17E;
	public static final Integer ADDR_DATA_PT_RATIO = 0x200A;
	public static final Integer ADDR_DATA_CT_RATIO = 0x200B;

	private final int[] inputRegisters;
	private int ptRatio = 1;
	private int ctRatio = 1;
	private int energyUnit = 1;
	private UnitFactor unitFactor = UnitFactor.EM5610;
	private long dataTimestamp = 0;

	private final static Logger LOG = LoggerFactory.getLogger(EM5600Data.class);

	public EM5600Data() {
		super();
		inputRegisters = new int[ADDR_DATA_ENERGY_UNIT - ADDR_DATA_I1 + 1];
		Arrays.fill(inputRegisters, 0);
	}

	/**
	 * Read data from the meter and store it internally. If data is populated
	 * successfully, the {@link dataTimestamp} will be updated to the current
	 * system time.
	 * 
	 * @param conn
	 *        the Modbus connection
	 * @param unitId
	 *        the Modbus unit ID to query
	 */
	public void readMeterData(final SerialConnection conn, final int unitId) {
		int[] current = ModbusHelper.readInts(conn, ADDR_DATA_I1,
				(ADDR_DATA_V_L1_NEUTRAL - ADDR_DATA_I1), unitId);
		setCurrent(current);
		int[] voltage = ModbusHelper.readInts(conn, ADDR_DATA_V_L1_NEUTRAL,
				(ADDR_DATA_ACTIVE_POWER_TOTAL - ADDR_DATA_V_L1_NEUTRAL), unitId);
		setVoltage(voltage);
		int[] power = ModbusHelper.readInts(conn, ADDR_DATA_ACTIVE_POWER_TOTAL,
				(ADDR_DATA_PHASE_ROTATION - ADDR_DATA_ACTIVE_POWER_TOTAL), unitId);
		setPower(power);
		try {
			readScalingFactors(conn, unitId);
		} catch ( RuntimeException e ) {
			LOG.debug(
					"Exception reading scaling factors ({}). Ignoring and assuming meter does not support them.",
					e.toString());
		}
		dataTimestamp = System.currentTimeMillis();
	}

	public void readScalingFactors(final SerialConnection conn, final int unitId) {
		int[] eUnit = ModbusHelper.readInts(conn, ADDR_DATA_ENERGY_UNIT, 1, unitId);
		if ( eUnit != null && eUnit.length > 0 ) {
			// a value of 0 here means we should treat the energy unit as 1, e.g. 5610
			int eu = eUnit[0];
			energyUnit = (eu < 1 ? 1 : eu);
		}
		int[] transformerRatios = ModbusHelper.readInts(conn, ADDR_DATA_PT_RATIO, 2, unitId);
		if ( transformerRatios != null && transformerRatios.length > 1 ) {
			int ptr = transformerRatios[0];
			ptRatio = (ptr < 1 ? 1 : ptr * 10);
			int ctr = transformerRatios[1];
			ctRatio = (ctr < 1 ? 1 : ctr * 10);
		}
	}

	/**
	 * Get the system time for the last time data was successfully populated via
	 * {@link #readMeterData(SerialConnection, int)}.
	 * 
	 * @return the system time
	 */
	public long getDataTimestamp() {
		return dataTimestamp;
	}

	private void setCurrent(int[] current) {
		if ( current == null ) {
			return;
		}
		System.arraycopy(current, 0, inputRegisters, 0,
				Math.min(current.length, (ADDR_DATA_V_L1_NEUTRAL - ADDR_DATA_I1)));
	}

	private void setVoltage(int[] voltage) {
		if ( voltage == null ) {
			return;
		}
		System.arraycopy(voltage, 0, inputRegisters, (ADDR_DATA_V_L1_NEUTRAL - ADDR_DATA_I1),
				Math.min(voltage.length, (ADDR_DATA_ACTIVE_POWER_TOTAL - ADDR_DATA_V_L1_NEUTRAL)));
	}

	private void setPower(int[] power) {
		if ( power == null ) {
			return;
		}
		System.arraycopy(power, 0, inputRegisters, (ADDR_DATA_ACTIVE_POWER_TOTAL - ADDR_DATA_I1),
				Math.min(power.length, (ADDR_DATA_ENERGY_UNIT - ADDR_DATA_ACTIVE_POWER_TOTAL)));
	}

	/**
	 * Get the value of an input register. This will not return useful data
	 * until after {@link #readMeterData(SerialConnection, int)} has been
	 * called. Use the various {@code ADDR_*} constants to query specific
	 * supported registers.
	 * 
	 * @param addr
	 *        the input register address to get the value of
	 * @return the register value
	 * @throws IllegalArgumentException
	 *         if the provided address is out of range
	 */
	public int getInputRegister(final int addr) {
		if ( addr < ADDR_DATA_I1 || addr > ADDR_DATA_ENERGY_UNIT ) {
			throw new IllegalArgumentException("Address " + addr + " out of range");
		}
		return inputRegisters[addr - ADDR_DATA_I1];
	}

	/**
	 * Get the {@link UnitFactor} to use for calculating effective values.
	 * 
	 * @return the unit factor
	 */
	public UnitFactor getUnitFactor() {
		return unitFactor;
	}

	/**
	 * Set the {@link UnitFactor} to use for calculating effective values. This
	 * defaults to {@link EM5610}.
	 * 
	 * @param unitFactor
	 */
	public void setUnitFactor(UnitFactor unitFactor) {
		this.unitFactor = unitFactor;
	}

	public int getPtRatio() {
		return ptRatio;
	}

	public int getCtRatio() {
		return ctRatio;
	}

	public int getEnergyUnit() {
		return energyUnit;
	}

}
