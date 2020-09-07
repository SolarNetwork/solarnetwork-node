/* ==================================================================
 * JamodModbusConnection.java - Jul 29, 2014 12:58:25 PM
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

package net.solarnetwork.node.io.modbus.jamod;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.BitSet;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusWriteFunction;
import net.solarnetwork.node.io.modbus.support.AbstractModbusConnection;
import net.wimpi.modbus.io.ModbusSerialTransaction;
import net.wimpi.modbus.net.SerialConnection;

/**
 * Jamod serial implementation of {@link ModbusConnection}.
 * 
 * @author matt
 * @version 2.0
 */
public class JamodModbusConnection extends AbstractModbusConnection implements ModbusConnection {

	private final SerialConnection connection;
	private final String name;

	public JamodModbusConnection(SerialConnection conn, int unitId) {
		this(conn, unitId, true, "UNKNOWN");
	}

	public JamodModbusConnection(SerialConnection conn, int unitId, boolean headless, String name) {
		super(unitId, headless);
		this.connection = conn;
		this.name = name;
	}

	final SerialConnection getSerialConnection() {
		return connection;
	}

	@Override
	public String toString() {
		return "JamodModbusConnection{port=" + name + ",unit=" + getUnitId() + '}';
	}

	@Override
	public void open() throws IOException {
		if ( !connection.isOpen() ) {
			try {
				connection.open();
			} catch ( IOException e ) {
				throw e;
			} catch ( Exception e ) {
				throw new IOException(e);
			}
		}
	}

	@Override
	public void close() {
		if ( connection.isOpen() ) {
			connection.close();
		}
	}

	private ModbusSerialTransaction createTransaction() {
		ModbusSerialTransaction tx = new ModbusSerialTransaction(connection);
		tx.setRetries(getRetries());
		return tx;
	}

	@Override
	public BitSet readDiscreetValues(final int[] addresses, final int count) {
		return ModbusTransactionUtils.readDiscreetValues(createTransaction(), addresses, count,
				getUnitId(), isHeadless());
	}

	@Override
	public BitSet readDiscreetValues(final int address, final int count) {
		return ModbusTransactionUtils.readDiscreteValues(createTransaction(), address, count,
				getUnitId(), isHeadless());
	}

	@Override
	public void writeDiscreetValues(final int[] addresses, final BitSet bits) {
		ModbusTransactionUtils.writeDiscreetValues(createTransaction(), addresses, bits, getUnitId(),
				isHeadless());
	}

	@Override
	public BitSet readInputDiscreteValues(final int address, final int count) {
		return ModbusTransactionUtils.readInputDiscreteValues(createTransaction(), address, count,
				getUnitId(), isHeadless());
	}

	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

	@Override
	public short[] readWords(final ModbusReadFunction function, final int address, final int count) {
		return ModbusTransactionUtils.readWords(createTransaction(), getUnitId(), isHeadless(), function,
				address, count);
	}

	@Override
	public void writeWords(ModbusWriteFunction function, int address, short[] values) {
		ModbusTransactionUtils.writeWords(createTransaction(), getUnitId(), isHeadless(), function,
				address, values);
	}

	@Override
	public int[] readWordsUnsigned(ModbusReadFunction function, int address, int count) {
		return ModbusTransactionUtils.readWordsUnsigned(createTransaction(), getUnitId(), isHeadless(),
				function, address, count);
	}

	@Override
	public void writeWords(ModbusWriteFunction function, int address, int[] values) {
		ModbusTransactionUtils.writeWords(createTransaction(), getUnitId(), isHeadless(), function,
				address, values);
	}

	@Override
	public byte[] readBytes(ModbusReadFunction function, int address, int count) {
		return ModbusTransactionUtils.readBytes(createTransaction(), getUnitId(), isHeadless(), function,
				address, count);
	}

	@Override
	public void writeBytes(ModbusWriteFunction function, int address, byte[] values) {
		ModbusTransactionUtils.writeBytes(createTransaction(), getUnitId(), isHeadless(), function,
				address, values);
	}

	@Override
	public String readString(ModbusReadFunction function, int address, int count, boolean trim,
			Charset charset) {
		return ModbusTransactionUtils.readString(createTransaction(), getUnitId(), isHeadless(),
				function, address, count, trim, charset);
	}

	@Override
	public void writeString(ModbusWriteFunction function, int address, String value, Charset charset) {
		ModbusTransactionUtils.writeString(createTransaction(), getUnitId(), isHeadless(), function,
				address, value, charset);
	}

}
