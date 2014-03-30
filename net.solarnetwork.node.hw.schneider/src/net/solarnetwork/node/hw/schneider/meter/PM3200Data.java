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

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import java.util.Arrays;
import net.solarnetwork.node.io.modbus.ModbusHelper;
import net.wimpi.modbus.net.SerialConnection;

/**
 * Encapsulates raw Modbus register data from the PM3200 meters.
 * 
 * @author matt
 * @version 1.0
 */
public class PM3200Data {

	// current (Float32)
	public static final int ADDR_DATA_I1 = 2999;
	public static final int ADDR_DATA_I2 = 3001;
	public static final int ADDR_DATA_I3 = 3003;
	public static final int ADDR_DATA_I_NEUTRAL = 3005;
	public static final int ADDR_DATA_I_AVERAGE = 3009;

	// voltage
	public static final int ADDR_DATA_V_L1_L2 = 3019;
	public static final int ADDR_DATA_V_L2_L3 = 3021;
	public static final int ADDR_DATA_V_L3_L1 = 3023;
	public static final int ADDR_DATA_V_L_L_AVERAGE = 3025;
	public static final int ADDR_DATA_V_L1_NEUTRAL = 3027;
	public static final int ADDR_DATA_V_L2_NEUTRAL = 3029;
	public static final int ADDR_DATA_V_L3_NEUTRAL = 3031;
	public static final int ADDR_DATA_V_NEUTRAL_AVERAGE = 3035;

	// power (Float32)
	public static final int ADDR_DATA_ACTIVE_POWER_P1 = 3053;
	public static final int ADDR_DATA_ACTIVE_POWER_P2 = 3055;
	public static final int ADDR_DATA_ACTIVE_POWER_P3 = 3057;
	public static final int ADDR_DATA_ACTIVE_POWER_TOTAL = 3059;
	public static final int ADDR_DATA_REACTIVE_POWER_P1 = 3061;
	public static final int ADDR_DATA_REACTIVE_POWER_P2 = 3063;
	public static final int ADDR_DATA_REACTIVE_POWER_P3 = 3065;
	public static final int ADDR_DATA_REACTIVE_POWER_TOTAL = 3067;
	public static final int ADDR_DATA_APPARENT_POWER_P1 = 3069;
	public static final int ADDR_DATA_APPARENT_POWER_P2 = 3071;
	public static final int ADDR_DATA_APPARENT_POWER_P3 = 3073;
	public static final int ADDR_DATA_APPARENT_POWER_TOTAL = 3075;

	// power factor (Float32)
	public static final int ADDR_DATA_POWER_FACTOR_P1 = 3077;
	public static final int ADDR_DATA_POWER_FACTOR_P2 = 3079;
	public static final int ADDR_DATA_POWER_FACTOR_P3 = 3081;
	public static final int ADDR_DATA_POWER_FACTOR_TOTAL = 3083;

	// tangent phi, frequency, temp (Float32)
	public static final int ADDR_DATA_REACTIVE_FACTOR_TOTAL = 3107;
	public static final int ADDR_DATA_FREQUENCY = 3109;
	public static final int ADDR_DATA_TEMP = 3131;

	// total energy (Int64)
	public static final int ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT = 3203;
	public static final int ADDR_DATA_TOTAL_ACTIVE_ENERGY_EXPORT = 3207;
	public static final int ADDR_DATA_TOTAL_REACTIVE_ENERGY_IMPORT = 3219;
	public static final int ADDR_DATA_TOTAL_REACTIVE_ENERGY_EXPORT = 3223;
	public static final int ADDR_DATA_TOTAL_APPARENT_ENERGY_IMPORT = 3235;
	public static final int ADDR_DATA_TOTAL_APPARENT_ENERGY_EXPORT = 3239;

	private final TIntIntMap dataRegisters;
	private long dataTimestamp = 0;

	/**
	 * Default constructor.
	 */
	public PM3200Data() {
		super();
		this.dataRegisters = new TIntIntHashMap(64);
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the object to copy
	 */
	public PM3200Data(PM3200Data other) {
		super();
		this.dataRegisters = new TIntIntHashMap(other.dataRegisters);
		this.dataTimestamp = other.dataTimestamp;
	}

	@Override
	public String toString() {
		return "PM3200Data{V1=" + getVoltage(ADDR_DATA_V_L1_NEUTRAL) + ",V2="
				+ getVoltage(ADDR_DATA_V_L2_NEUTRAL) + ",V3=" + getVoltage(ADDR_DATA_V_L3_NEUTRAL)
				+ ",A1=" + getCurrent(ADDR_DATA_I1) + ",A2=" + getCurrent(ADDR_DATA_I2) + ",A3="
				+ getCurrent(ADDR_DATA_I3) + ",PF=" + getPowerFactor(ADDR_DATA_REACTIVE_FACTOR_TOTAL)
				+ ",Hz=" + getFrequency(ADDR_DATA_FREQUENCY) + ",W="
				+ getPower(ADDR_DATA_ACTIVE_POWER_TOTAL) + ",var="
				+ getPower(ADDR_DATA_REACTIVE_POWER_TOTAL) + ",VA="
				+ getPower(ADDR_DATA_APPARENT_POWER_TOTAL) + ",Wh-I="
				+ getEnergy(ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT) + ",varh-I="
				+ getEnergy(ADDR_DATA_TOTAL_REACTIVE_ENERGY_IMPORT) + ",Wh-E="
				+ getEnergy(ADDR_DATA_TOTAL_ACTIVE_ENERGY_EXPORT) + ",varh-E="
				+ getEnergy(ADDR_DATA_TOTAL_REACTIVE_ENERGY_EXPORT) + "}";
	}

	/**
	 * Get a string of data values, useful for debugging. The generated string
	 * will contain a register address followed by two register values per line,
	 * printed as hexidecimal integers, with a prefix and suffix line. The
	 * register addresses will be printed as {@bold 1-based} values, to
	 * match Schneider's documentation. For example:
	 * 
	 * <pre>
	 * PM3200Data{
	 *      3000: 0x4141, 0x727E
	 *      3002: 0xFFC0, 0x0000
	 *      ...
	 *      3240: 0x0000, 0x0000
	 * }
	 * </pre>
	 * 
	 * @return debug string
	 */
	public String dataDebugString() {
		final StringBuilder buf = new StringBuilder("PM3200Data{\n");
		PM3200Data snapshot = new PM3200Data(this);
		int[] keys = snapshot.dataRegisters.keys();
		Arrays.sort(keys);
		boolean odd = true;
		for ( int k : keys ) {
			if ( odd ) {
				buf.append("\t").append(String.format("%5d", k + 1)).append(": ");
			}
			buf.append(String.format("0x%04X", snapshot.dataRegisters.get(k)));
			if ( odd ) {
				buf.append(", ");
			} else {
				buf.append("\n");
			}
			odd = !odd;
		}
		buf.append("}");
		return buf.toString();
	}

	/**
	 * Read data from the meter and store it internally. If data is populated
	 * successfully, the {@link dataTimestamp} will be updated to the current
	 * system time. <b>Note</b> this does <b>not</b> call
	 * {@link #readEnergyRatios(SerialConnection, int)}. Those values are not
	 * expected to change much, so those values should be called manually as
	 * needed.
	 * 
	 * @param conn
	 *        the Modbus connection
	 * @param unitId
	 *        the Modbus unit ID to query
	 */
	public synchronized void readMeterData(final SerialConnection conn, final int unitId) {
		// current
		readIntData(conn, unitId, ADDR_DATA_I1, ADDR_DATA_I_AVERAGE + 1);

		// voltage
		readIntData(conn, unitId, ADDR_DATA_V_L1_L2, ADDR_DATA_V_NEUTRAL_AVERAGE + 1);

		// power, power factor
		readIntData(conn, unitId, ADDR_DATA_ACTIVE_POWER_P1, ADDR_DATA_POWER_FACTOR_TOTAL + 1);

		// tangent phi, frequency, temp (Float32)
		readIntData(conn, unitId, ADDR_DATA_REACTIVE_FACTOR_TOTAL, ADDR_DATA_TEMP + 1);

		// total energy
		readIntData(conn, unitId, ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT,
				ADDR_DATA_TOTAL_APPARENT_ENERGY_EXPORT + 1);

		dataTimestamp = System.currentTimeMillis();
	}

	private void readIntData(final SerialConnection conn, final int unitId, final int startAddr,
			final int endAddr) {
		int[] data = ModbusHelper.readInts(conn, startAddr, (endAddr - startAddr + 1), unitId);
		saveDataArray(data, startAddr);
	}

	private void saveDataArray(final int[] data, int addr) {
		if ( data == null || data.length < 1 ) {
			return;
		}
		for ( int v : data ) {
			dataRegisters.put(addr, v);
			addr++;
		}
	}

	private Float getFloat32(final int addr) {
		return ModbusHelper.parseFloat32(dataRegisters.get(addr), dataRegisters.get(addr + 1));
	}

	private Long getInt64(final int addr) {
		return ModbusHelper.parseInt64(dataRegisters.get(addr), dataRegisters.get(addr + 1),
				dataRegisters.get(addr + 2), dataRegisters.get(addr + 3));
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

	public long getDataTimestamp() {
		return dataTimestamp;
	}

}
