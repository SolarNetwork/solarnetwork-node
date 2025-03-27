/* ==================================================================
 * ModbusConnectionSupport.java - 8/10/2018 8:13:09 AM
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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.BitSet;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusWriteFunction;
import net.solarnetwork.node.service.LockTimeoutException;

/**
 * Supporting class for {@link ModbusConnection} implementations to extend.
 *
 * <p>
 * This class has been created to help with Modbus testing. All write methods
 * throw an {@link UnsupportedOperationException} and all read methods return
 * {@literal null}. The {@link #open()} and {@link #close()} methods do nothing.
 * </p>
 *
 * @author matt
 * @version 3.1
 */
public class ModbusConnectionSupport extends AbstractModbusConnection implements ModbusConnection {

	/**
	 * Constructor.
	 */
	public ModbusConnectionSupport() {
		super(0, true);
	}

	@Override
	public void open() throws IOException, LockTimeoutException {
		// nothing to do
	}

	@Override
	public void close() {
		// nothing to do
	}

	@Override
	public void writeWords(ModbusWriteFunction function, int address, int[] values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void writeString(ModbusWriteFunction function, int address, String value, Charset charset) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void writeWords(ModbusWriteFunction function, int address, short[] values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void writeBytes(ModbusWriteFunction function, int address, byte[] values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int[] readWordsUnsigned(ModbusReadFunction function, int address, int count) {
		return null;
	}

	@Override
	public String readString(ModbusReadFunction function, int address, int count, boolean trim,
			Charset charset) {
		return null;
	}

	@Override
	public short[] readWords(ModbusReadFunction function, int address, int count) {
		return null;
	}

	@Override
	public void writeDiscreetValues(final int[] addresses, final BitSet bits) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void writeDiscreteValues(ModbusWriteFunction function, int address, int count, BitSet bits)
			throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public BitSet readInputDiscreteValues(final int address, final int count) {
		return null;
	}

	@Override
	public BitSet readDiscreetValues(final int address, final int count) {
		return null;
	}

	@Override
	public BitSet readDiscreetValues(final int[] addresses, final int count) {
		return null;
	}

	@Override
	public byte[] readBytes(ModbusReadFunction function, int address, int count) {
		return null;
	}

}
