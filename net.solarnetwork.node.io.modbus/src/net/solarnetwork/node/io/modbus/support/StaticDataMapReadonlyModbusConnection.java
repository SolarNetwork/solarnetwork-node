/* ==================================================================
 * StaticDataReadonlyModbusConnection.java - 8/10/2018 8:25:30 AM
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

package net.solarnetwork.node.io.modbus.support;

import java.io.UnsupportedEncodingException;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * {@link ModbusConnection} for reading static data.
 * 
 * <p>
 * This class can be useful in tests working with Modbus connections.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 2.16
 */
public class StaticDataMapReadonlyModbusConnection extends ModbusConnectionSupport {

	private final Map<Integer, Integer> data;

	/**
	 * Construct with the data.
	 */
	public StaticDataMapReadonlyModbusConnection(Map<Integer, Integer> data) {
		super();
		this.data = data;
	}

	/**
	 * Get the data map.
	 * 
	 * @return the data
	 */
	protected Map<Integer, Integer> getData() {
		return data;
	}

	@Override
	public Integer[] readValues(Integer address, int count) {
		Integer[] out = new Integer[count];
		for ( int i = address, len = address + count; i < len; i++ ) {
			out[i - address] = data.get(i);
		}
		return out;
	}

	@Override
	public int[] readUnsignedShorts(ModbusReadFunction function, Integer address, int count) {
		return readInts(address, count);
	}

	@Override
	public String readString(ModbusReadFunction function, Integer address, int count, boolean trim,
			String charsetName) {
		return readString(address, count, trim, charsetName);
	}

	@Override
	public String readString(Integer address, int count, boolean trim, String charsetName) {
		final byte[] bytes = readBytes(address, count);
		String result = null;
		if ( bytes != null ) {
			try {
				result = new String(bytes, charsetName);
				if ( trim ) {
					result = result.trim();
				}
			} catch ( UnsupportedEncodingException e ) {
				throw new RuntimeException(e);
			}
		}
		return result;
	}

	@Override
	public short[] readSignedShorts(ModbusReadFunction function, Integer address, int count) {
		return readSignedShorts(address, count);
	}

	@Override
	public short[] readSignedShorts(Integer address, int count) {
		short[] out = new short[count];
		for ( int i = 0; i < count; i++ ) {
			Integer val = data.get(i);
			out[i] = (short) (val != null ? val : 0);
		}
		return out;
	}

	@Override
	public int[] readInts(Integer address, int count) {
		int[] out = new int[count];
		for ( int i = address, len = address + count; i < len; i++ ) {
			Integer val = data.get(i);
			out[i - address] = (val != null ? val : 0);
		}
		return out;
	}

	@Override
	public int[] readInputValues(Integer address, int count) {
		return readInts(address, count);
	}

	@Override
	public Map<Integer, Integer> readInputValues(Integer[] addresses, int count) {
		Map<Integer, Integer> out = new LinkedHashMap<Integer, Integer>();
		for ( Integer a : addresses ) {
			for ( int i = a, len = a + count; i < len; i++ ) {
				Integer val = data.get(i);
				if ( val != null ) {
					out.put(i, val);
				}
			}
		}
		return out;
	}

	@Override
	public BitSet readInputDiscreteValues(Integer address, int count) {
		return readDiscreetValues(address, count);
	}

	@Override
	public BitSet readDiscreetValues(Integer address, int count) {
		return readDiscreetValues(new Integer[] { address }, count);
	}

	@Override
	public BitSet readDiscreetValues(Integer[] addresses, int count) {
		BitSet out = new BitSet();
		for ( Integer a : addresses ) {
			Integer d = data.get(a);
			if ( d != null ) {
				for ( int i = 0; i < count; i++ ) {
					if ( ((d >> i) & 0x1) == 1 ) {
						out.set(a + i);
					}
				}
			}
		}
		return out;
	}

	@Override
	public byte[] readBytes(ModbusReadFunction function, Integer address, int count) {
		return readBytes(address, count);
	}

	@Override
	public byte[] readBytes(Integer address, int count) {
		byte[] result = new byte[count * 2];
		for ( int i = 0; i < count; i++ ) {
			Integer d = data.get(address + i);
			if ( d != null ) {
				result[i * 2] = (byte) ((d >> 8) & 0xFF);
				result[i * 2 + 1] = (byte) (d & 0xFF);
			}
		}
		return result;
	}

}
