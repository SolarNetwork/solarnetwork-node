/* ==================================================================
 * BaseSDMData.java - 25/01/2016 5:52:26 pm
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

import java.util.Arrays;
import java.util.Map;
import gnu.trove.map.hash.TIntIntHashMap;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusHelper;

/**
 * Abstract base class for other {@code SDMData} implementations to extend.
 * 
 * The normal use for extending classes is to implement
 * {@link SDMData#readMeterData(ModbusConnection)} by calling
 * {@link #readInputData(ModbusConnection, int, int)} and
 * {@link #readHoldingData(ModbusConnection, int, int)} for ranges of Modbus
 * register data.
 * 
 * @author matt
 * @version 1.1
 */
public abstract class BaseSDMData implements SDMData {

	private final TIntIntHashMap dataRegisters;
	private final TIntIntHashMap controlRegisters;
	private final boolean backwards;
	private long meterDataTimestamp = 0;
	private long controlDataTimestamp = 0;

	/**
	 * Default constructor.
	 */
	public BaseSDMData() {
		super();
		this.dataRegisters = new TIntIntHashMap(64);
		this.controlRegisters = new TIntIntHashMap(8);
		this.backwards = false;
	}

	/**
	 * Construct with backwards setting.
	 * 
	 * @param backwards
	 *        If {@code true} then treat the meter as being installed backwards
	 *        with respect to the current direction. In this case certain
	 *        instantaneous measurements will be negated and certain
	 *        accumulating properties will be switched (like {@code wattHours}
	 *        and {@code wattHoursReverse}) when
	 *        {@link SDMData#populateMeasurements} is called.
	 */
	public BaseSDMData(boolean backwards) {
		super();
		this.dataRegisters = new TIntIntHashMap(64);
		this.controlRegisters = new TIntIntHashMap(8);
		this.backwards = backwards;
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the object to copy
	 */
	public BaseSDMData(BaseSDMData other) {
		this.dataRegisters = new TIntIntHashMap(other.dataRegisters);
		this.controlRegisters = new TIntIntHashMap(other.controlRegisters);
		this.meterDataTimestamp = other.meterDataTimestamp;
		this.controlDataTimestamp = other.controlDataTimestamp;
		this.backwards = other.backwards;
	}

	@Override
	public final synchronized void readMeterData(ModbusConnection conn) {
		if ( readMeterDataInternal(conn) ) {
			this.meterDataTimestamp = System.currentTimeMillis();
		}
	}

	/**
	 * Called by {@link #readMeterData(ModbusConnection)}.
	 * 
	 * @param conn
	 *        The Modbus connection to use.
	 * @return <em>true</em> if the data has been read successfully
	 */
	protected abstract boolean readMeterDataInternal(ModbusConnection conn);

	@Override
	public long getMeterDataTimestamp() {
		return meterDataTimestamp;
	}

	@Override
	public final long getControlDataTimestamp() {
		return controlDataTimestamp;
	}

	@Override
	public final synchronized void readControlData(ModbusConnection conn) {
		if ( readControlDataInternal(conn) ) {
			this.controlDataTimestamp = System.currentTimeMillis();
		}
	}

	/**
	 * Called by {@link #readControlData(ModbusConnection)}.
	 * 
	 * @param conn
	 *        The Modbus connection to use.
	 * @return <em>true</em> if the data has been read successfully
	 */
	protected abstract boolean readControlDataInternal(ModbusConnection conn);

	/**
	 * Read Modbus input registers in an address range.
	 * 
	 * @param conn
	 *        The Modbus connection.
	 * @param startAddr
	 *        The starting Modbus register address.
	 * @param endAddr
	 *        The ending Modbus register address.
	 */
	protected void readInputData(final ModbusConnection conn, final int startAddr, final int endAddr) {
		Map<Integer, Integer> data = conn.readInputValues(new Integer[] { startAddr },
				(endAddr - startAddr + 1));
		dataRegisters.putAll(data);
	}

	/**
	 * Read Modbus holding registers in an address range.
	 * 
	 * @param conn
	 *        The Modbus connection.
	 * @param startAddr
	 *        The starting Modbus register address.
	 * @param endAddr
	 *        The ending Modbus register address.
	 */
	protected void readHoldingData(final ModbusConnection conn, final int startAddr, final int endAddr) {
		int[] data = conn.readInts(startAddr, (endAddr - startAddr + 1));
		saveControlArray(data, startAddr);
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

	/**
	 * Internally store an array of 16-bit integer register control values,
	 * starting at a given address.
	 * 
	 * @param data
	 *        the control data array to save
	 * @param addr
	 *        the starting address of the data
	 */
	protected void saveControlArray(final int[] data, int addr) {
		if ( data == null || data.length < 1 ) {
			return;
		}
		for ( int v : data ) {
			controlRegisters.put(addr, v);
			addr++;
		}
	}

	/**
	 * Get a string of data values, useful for debugging. The generated string
	 * will contain a register address followed by two register values per line,
	 * printed as hexidecimal integers, with a prefix and suffix line. The
	 * register addresses will be printed as {@bold 30001-based} values, to
	 * match Deson's documentation. For example:
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
	public final String dataDebugString(BaseSDMData snapshot) {
		final StringBuilder buf = new StringBuilder(snapshot.getClass().getSimpleName()).append("{\n");
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
	 * Construct a Float from a saved data register address. This method can
	 * only be called after data register data has been passed to
	 * {@link #saveDataArray(int[], int)}.
	 * 
	 * @param addr
	 *        The address of the saved data register to read.
	 * @return The parsed value, or <em>null</em> if not available.
	 */
	protected final Float getFloat32(final int addr) {
		return ModbusHelper.parseFloat32(dataRegisters.get(addr), dataRegisters.get(addr + 1));
	}

	/**
	 * Construct a Float from a saved control register address. This method can
	 * only be called after data register data has been passed to
	 * {@link #saveControlArray(int[], int)}.
	 * 
	 * @param addr
	 *        The address of the saved control register to read.
	 * @return The parsed value, or <em>null</em> if not available.
	 */
	protected final Float getControlFloat32(final int addr) {
		return ModbusHelper.parseFloat32(controlRegisters.get(addr), controlRegisters.get(addr + 1));
	}

	@Override
	public Float getVoltage(final int addr) {
		return getFloat32(addr);
	}

	@Override
	public Float getCurrent(final int addr) {
		return getFloat32(addr);
	}

	@Override
	public Float getFrequency(final int addr) {
		return getFloat32(addr);
	}

	@Override
	public Float getPowerFactor(final int addr) {
		return getFloat32(addr);
	}

	@Override
	public Integer getPower(final int addr) {
		Float value = getFloat32(addr);
		if ( value == null ) {
			return null;
		}
		return Integer.valueOf((int) (Math.round(value.doubleValue())));
	}

	@Override
	public Long getEnergy(final int addr) {
		Float value = getFloat32(addr);
		if ( value == null ) {
			return null;
		}
		return Long.valueOf(Math.round(value.doubleValue() * 1000.0));
	}

	@Override
	public boolean isBackwards() {
		return backwards;
	}

}
