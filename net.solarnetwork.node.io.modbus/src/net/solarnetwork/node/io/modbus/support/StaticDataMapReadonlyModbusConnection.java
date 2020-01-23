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
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusDataUtils;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.util.IntShortMap;

/**
 * {@link ModbusConnection} for reading static data.
 * 
 * <p>
 * This class can be useful in tests working with Modbus connections.
 * </p>
 * 
 * @author matt
 * @version 2.0
 * @since 2.16
 */
public class StaticDataMapReadonlyModbusConnection extends ModbusConnectionSupport {

	private final IntShortMap data;

	/**
	 * Construct with the data.
	 */
	public StaticDataMapReadonlyModbusConnection(IntShortMap data) {
		super();
		this.data = data;
	}

	/**
	 * Constructor.
	 * 
	 * @param data
	 *        the starting data, starting from Modbus register address
	 *        {@literal 0}
	 */
	public StaticDataMapReadonlyModbusConnection(int[] data) {
		this(data, 0);

	}

	/**
	 * Constructor.
	 * 
	 * @param data
	 *        the starting data
	 * @param address
	 *        the starting Modbus register address for {@code data}
	 */
	public StaticDataMapReadonlyModbusConnection(int[] data, int address) {
		this(ModbusDataUtils.shortArray(data), address);

	}

	/**
	 * Constructor.
	 * 
	 * @param data
	 *        the starting data
	 * @param address
	 *        the starting Modbus register address for {@code data}
	 */
	public StaticDataMapReadonlyModbusConnection(short[] data, int address) {
		this(new IntShortMap());
		for ( int i = 0; i < data.length; i++ ) {
			this.data.putValue(address + i, data[i]);
		}
	}

	/**
	 * Get the data map.
	 * 
	 * @return the data
	 */
	protected IntShortMap getData() {
		return data;
	}

	@Override
	public Integer[] readValues(Integer address, int count) {
		Integer[] out = new Integer[count];
		for ( int i = address, len = address + count; i < len; i++ ) {
			out[i - address] = data.getValue(i) & 0xFFFF;
		}
		return out;
	}

	@Override
	public int[] readUnsignedShorts(ModbusReadFunction function, Integer address, int count) {
		int[] out = new int[count];
		data.forEachOrdered(address, address + count, (k, v) -> {
			out[k - address] = v & 0xFFFF;
		});
		return out;
	}

	@Override
	public String readString(ModbusReadFunction function, Integer address, int count, boolean trim,
			String charsetName) {
		return readString(address, count, trim, charsetName);
	}

	@Override
	public String readString(Integer address, int count, boolean trim, String charsetName) {
		final byte[] bytes = readBytes(ModbusReadFunction.ReadHoldingRegister, address, count);
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
		data.forEachOrdered(address, address + count, (k, v) -> {
			out[k - address] = v;
		});
		return out;
	}

	@Override
	public BitSet readInputDiscreteValues(final int address, final int count) {
		return readDiscreetValues(address, count);
	}

	@Override
	public BitSet readDiscreetValues(final int address, final int count) {
		return readDiscreetValues(new int[] { address }, count);
	}

	@Override
	public BitSet readDiscreetValues(final int[] addresses, final int count) {
		BitSet out = new BitSet();
		for ( int i = 0, w = 0; i < addresses.length; i++ ) {
			final int d = data.getValue(addresses[i]);
			for ( int j = 0; j < count; j++, w++ ) {
				if ( ((d >> j) & 0x1) == 1 ) {
					out.set(w);
				}
			}
		}
		return out;
	}

	@Override
	public byte[] readBytes(ModbusReadFunction function, Integer address, int count) {
		byte[] result = new byte[count * 2];
		for ( int i = 0; i < count; i++ ) {
			final int d = data.getValue(address + i);
			result[i * 2] = (byte) ((d >> 8) & 0xFF);
			result[i * 2 + 1] = (byte) (d & 0xFF);
		}
		return result;
	}

}
