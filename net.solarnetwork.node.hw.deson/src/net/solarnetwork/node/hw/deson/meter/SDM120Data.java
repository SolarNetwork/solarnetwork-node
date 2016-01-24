/* ==================================================================
 * SDM120Data.java - 23/01/2016 5:33:22 pm
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

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import java.util.Arrays;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusHelper;

/**
 * Encapsulates raw Modbus register data from SDM 120 meters.
 * 
 * @author matt
 * @version 1.0
 */
public class SDM120Data {

	// current (Float32)
	public static final int ADDR_DATA_I = 6;

	// voltage (Float32)
	public static final int ADDR_DATA_V_NEUTRAL = 0;

	// power (Float32)
	public static final int ADDR_DATA_ACTIVE_POWER = 12;
	public static final int ADDR_DATA_APPARENT_POWER = 18;
	public static final int ADDR_DATA_REACTIVE_POWER = 24;

	// power factor (Float32)
	public static final int ADDR_DATA_POWER_FACTOR = 30;

	// frequency (Float32)
	public static final int ADDR_DATA_FREQUENCY = 70;

	// total energy (Float32, k)
	public static final int ADDR_DATA_ACTIVE_ENERGY_IMPORT_TOTAL = 72;
	public static final int ADDR_DATA_ACTIVE_ENERGY_EXPORT_TOTAL = 74;
	public static final int ADDR_DATA_REACTIVE_ENERGY_IMPORT_TOTAL = 76;
	public static final int ADDR_DATA_REACTIVE_ENERGY_EXPORT_TOTAL = 78;

	private final TIntIntMap dataRegisters;
	private long dataTimestamp = 0;

	/**
	 * Default constructor.
	 */
	public SDM120Data() {
		super();
		this.dataRegisters = new TIntIntHashMap(64);
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the object to copy
	 */
	public SDM120Data(SDM120Data other) {
		super();
		this.dataRegisters = new TIntIntHashMap(other.dataRegisters);
		this.dataTimestamp = other.dataTimestamp;
	}

	@Override
	public String toString() {
		return "SDM120Data{V=" + getVoltage(ADDR_DATA_V_NEUTRAL) + ",A=" + getCurrent(ADDR_DATA_I)
				+ ",PF=" + getPowerFactor(ADDR_DATA_POWER_FACTOR) + ",Hz="
				+ getFrequency(ADDR_DATA_FREQUENCY) + ",W=" + getPower(ADDR_DATA_ACTIVE_POWER) + ",var="
				+ getPower(ADDR_DATA_REACTIVE_POWER) + ",VA=" + getPower(ADDR_DATA_APPARENT_POWER)
				+ ",Wh-I=" + getEnergy(ADDR_DATA_ACTIVE_ENERGY_IMPORT_TOTAL) + ",varh-I="
				+ getEnergy(ADDR_DATA_REACTIVE_ENERGY_IMPORT_TOTAL) + ",Wh-E="
				+ getEnergy(ADDR_DATA_ACTIVE_ENERGY_EXPORT_TOTAL) + ",varh-E="
				+ getEnergy(ADDR_DATA_REACTIVE_ENERGY_EXPORT_TOTAL) + "}";
	}

	/**
	 * Get a string of data values, useful for debugging. The generated string
	 * will contain a register address followed by two register values per line,
	 * printed as hexidecimal integers, with a prefix and suffix line. The
	 * register addresses will be printed as {@bold 30001-based} values,
	 * to match Deson's documentation. For example:
	 * 
	 * <pre>
	 * SDM120Data{
	 *      30001: 0x4141, 0x727E
	 *      30007: 0xFFC0, 0x0000
	 *      ...
	 *      30345: 0x0000, 0x0000
	 * }
	 * </pre>
	 * 
	 * @return debug string
	 */
	public String dataDebugString() {
		final StringBuilder buf = new StringBuilder("PM3200Data{\n");
		SDM120Data snapshot = new SDM120Data(this);
		int[] keys = snapshot.dataRegisters.keys();
		Arrays.sort(keys);
		boolean odd = true;
		for ( int k : keys ) {
			if ( odd ) {
				buf.append("\t").append(String.format("%5d", k + 30001)).append(": ");
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
	 */
	public synchronized void readMeterData(final ModbusConnection conn) {
		readIntData(conn, ADDR_DATA_V_NEUTRAL, ADDR_DATA_V_NEUTRAL + 80);

		//		// voltage
		//		readIntData(conn, ADDR_DATA_V_L1_L2, ADDR_DATA_V_NEUTRAL_AVERAGE + 1);
		//
		//		// power, power factor
		//		readIntData(conn, ADDR_DATA_ACTIVE_POWER_P1, ADDR_DATA_POWER_FACTOR_TOTAL + 1);
		//
		//		// tangent phi, frequency, temp (Float32)
		//		readIntData(conn, ADDR_DATA_REACTIVE_FACTOR_TOTAL, ADDR_DATA_TEMP + 1);
		//
		//		// total energy (Int64)
		//		readIntData(conn, ADDR_DATA_ACTIVE_ENERGY_IMPORT_TOTAL,
		//				ADDR_DATA_APPARENT_ENERGY_EXPORT_TOTAL + 3);
		//
		//		// total phase energy import (Int64)
		//		readIntData(conn, ADDR_DATA_ACTIVE_ENERGY_IMPORT_P1, ADDR_DATA_APPARENT_ENERGY_IMPORT_P3 + 3);

		dataTimestamp = System.currentTimeMillis();
	}

	private void readIntData(final ModbusConnection conn, final int startAddr, final int endAddr) {
		int[] data = conn.readInts(startAddr, (endAddr - startAddr + 1));
		saveDataArray(data, startAddr);
	}

	/**
	 * Internally store an array of 16-bit integer register data values,
	 * starting at a given address.
	 * 
	 * @param data
	 *        the data array to save
	 * @param addr
	 *        the starting address of the data
	 */
	protected void saveDataArray(final int[] data, int addr) {
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
	 * (apparent). These are rounded from their native floating point
	 * representations.
	 * 
	 * @param addr
	 *        the register address to read
	 * @return the value interpreted as a power value
	 */
	public Integer getPower(int addr) {
		Float value = getFloat32(addr);
		if ( value == null ) {
			return null;
		}
		return Integer.valueOf((int) (Math.round(value.doubleValue())));
	}

	/**
	 * Get an effective energy value in Wh (real), Varh (reactive).
	 * 
	 * @param addr
	 *        the register address to read
	 * @return the value interpreted as an energy value
	 */
	public Long getEnergy(int addr) {
		Float value = getFloat32(addr);
		if ( value == null ) {
			return null;
		}
		return Long.valueOf(Math.round(value.doubleValue() * 1000.0));
	}

	public long getDataTimestamp() {
		return dataTimestamp;
	}

}
