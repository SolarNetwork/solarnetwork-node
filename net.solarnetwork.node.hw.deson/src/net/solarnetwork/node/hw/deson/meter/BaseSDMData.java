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

import gnu.trove.map.hash.TIntIntHashMap;
import java.util.Arrays;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusHelper;

/**
 * Abstract base class for other {@code SDMData} implementations to extend.
 * 
 * The normal use for extending classes is to implement
 * {@link SDMData#readMeterData(ModbusConnection)} by calling
 * {@link #saveDataArray(int[], int)} for ranges of Modbus register data.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class BaseSDMData implements SDMData, Cloneable {

	private final TIntIntHashMap dataRegisters;
	private long dataTimestamp = 0;

	/**
	 * Default constructor.
	 */
	public BaseSDMData() {
		super();
		this.dataRegisters = new TIntIntHashMap(64);
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the object to copy
	 */
	public BaseSDMData(BaseSDMData other) {
		super();
		this.dataRegisters = new TIntIntHashMap(other.dataRegisters);
		this.dataTimestamp = other.dataTimestamp;
	}

	@Override
	public final synchronized void readMeterData(ModbusConnection conn) {
		if ( readMeterDataInternal(conn) ) {
			this.dataTimestamp = System.currentTimeMillis();
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
	public long getDataTimestamp() {
		return dataTimestamp;
	}

	/**
	 * Read Modbus integer registers in an address range.
	 * 
	 * @param conn
	 *        The Modbus connection.
	 * @param startAddr
	 *        The starting Modbus register address.
	 * @param endAddr
	 *        The ending Modbus register address.
	 */
	protected void readIntData(final ModbusConnection conn, final int startAddr, final int endAddr) {
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

}
