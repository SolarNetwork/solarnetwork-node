/* ==================================================================
 * StaticDataMapModbusConnection.java - 8/11/2019 11:28:16 am
 *
 * Copyright 2019 SolarNetwork.net Dev Team
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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.BitSet;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusWriteFunction;
import net.solarnetwork.util.IntShortMap;

/**
 * {@link ModbusConnection} for reading/writing static data.
 *
 * <p>
 * This class can be useful in tests working with Modbus connections.
 * </p>
 *
 * @author matt
 * @version 2.1
 * @since 2.16
 */
public class StaticDataMapModbusConnection extends StaticDataMapReadonlyModbusConnection {

	/**
	 * Constructor.
	 *
	 * @param data
	 *        the starting data
	 */
	public StaticDataMapModbusConnection(IntShortMap data) {
		super(data);
	}

	@Override
	public void writeWords(ModbusWriteFunction function, int address, int[] values) {
		final IntShortMap data = getData();
		for ( int i = 0, len = values.length; i < len; i++ ) {
			data.putValue(address + i, values[i]);
		}
	}

	@Override
	public void writeString(ModbusWriteFunction function, int address, String value, Charset charset) {
		if ( value == null || value.isEmpty() ) {
			return;
		}
		byte[] data = value.getBytes(charset);
		writeBytes(function, address, data);
	}

	@Override
	public void writeWords(ModbusWriteFunction function, int address, short[] values) {
		final IntShortMap data = getData();
		for ( int i = 0, len = values.length; i < len; i++ ) {
			data.putValue(address + i, (int) values[i]);
		}
	}

	@Override
	public void writeDiscreetValues(final int[] addresses, final BitSet bits) {
		final IntShortMap data = getData();
		for ( int i = 0; i < addresses.length; i++ ) {
			data.putValue(addresses[i], bits.get(i) ? (short) 1 : (short) 0);
		}
	}

	@Override
	public void writeDiscreteValues(ModbusWriteFunction function, int address, int count, BitSet bits)
			throws IOException {
		final IntShortMap data = getData();
		for ( int i = 0; i < count; i++ ) {
			data.putValue(address + i, bits.get(i) ? (short) 1 : (short) 0);
		}
	}

	@Override
	public void writeBytes(ModbusWriteFunction function, int address, byte[] values) {
		int[] unsigned = new int[(int) Math.ceil(values.length / 2.0)];
		for ( int i = 0; i < values.length; i += 2 ) {
			int v = ((values[i] & 0xFF) << 8);
			if ( i + 1 < values.length ) {
				v |= (values[i + 1] & 0xFF);
			}
			unsigned[i / 2] = v;
		}
		writeWords(function, address, unsigned);
	}

}
