/* ==================================================================
 * J2ModSerialModbusConnection.java - 8/07/2022 2:45:43 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus.j2mod;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.BitSet;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.net.SerialConnection;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusWriteFunction;
import net.solarnetwork.node.io.modbus.support.AbstractModbusConnection;

/**
 * j2mod implementation of {@link ModbusConnection} using a serial connection.
 * 
 * @author matt
 * @version 1.0
 */
public class J2ModSerialModbusConnection extends AbstractModbusConnection implements ModbusConnection {

	private final SerialConnection connection;
	private final String name;

	/**
	 * Constructor.
	 * 
	 * @param conn
	 *        the connection
	 * @param unitId
	 *        the unit ID
	 */
	public J2ModSerialModbusConnection(SerialConnection conn, int unitId) {
		this(conn, unitId, true, "UNKNOWN");
	}

	/**
	 * Constructor.
	 * 
	 * @param conn
	 *        the connection
	 * @param unitId
	 *        the unit ID
	 * @param headless
	 *        the headless flag
	 * @param name
	 *        the connection name
	 */
	public J2ModSerialModbusConnection(SerialConnection conn, int unitId, boolean headless,
			String name) {
		super(unitId, headless);
		this.connection = conn;
		this.name = name;
	}

	final SerialConnection getSerialConnection() {
		return connection;
	}

	@Override
	public String toString() {
		return "J2ModModbusConnection{port=" + name + ",unit=" + getUnitId() + '}';
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
		connection.close();
	}

	private ModbusSerialTransaction createTransaction() {
		ModbusSerialTransaction tx = new ModbusSerialTransaction(connection);
		tx.setRetries(getRetries());
		return tx;
	}

	@Override
	public BitSet readDiscreetValues(final int[] addresses, final int count) throws IOException {
		return ModbusTransactionUtils.readDiscreetValues(createTransaction(), addresses, count,
				getUnitId(), isHeadless());
	}

	@Override
	public BitSet readDiscreetValues(final int address, final int count) throws IOException {
		return ModbusTransactionUtils.readDiscreteValues(createTransaction(), address, count,
				getUnitId(), isHeadless());
	}

	@Override
	public void writeDiscreetValues(final int[] addresses, final BitSet bits) throws IOException {
		ModbusTransactionUtils.writeDiscreetValues(createTransaction(), addresses, bits, getUnitId(),
				isHeadless());
	}

	@Override
	public BitSet readInputDiscreteValues(final int address, final int count) throws IOException {
		return ModbusTransactionUtils.readInputDiscreteValues(createTransaction(), address, count,
				getUnitId(), isHeadless());
	}

	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

	@Override
	public short[] readWords(final ModbusReadFunction function, final int address, final int count)
			throws IOException {
		return ModbusTransactionUtils.readWords(createTransaction(), getUnitId(), isHeadless(), function,
				address, count);
	}

	@Override
	public void writeWords(ModbusWriteFunction function, int address, short[] values)
			throws IOException {
		ModbusTransactionUtils.writeWords(createTransaction(), getUnitId(), isHeadless(), function,
				address, values);
	}

	@Override
	public int[] readWordsUnsigned(ModbusReadFunction function, int address, int count)
			throws IOException {
		return ModbusTransactionUtils.readWordsUnsigned(createTransaction(), getUnitId(), isHeadless(),
				function, address, count);
	}

	@Override
	public void writeWords(ModbusWriteFunction function, int address, int[] values) throws IOException {
		ModbusTransactionUtils.writeWords(createTransaction(), getUnitId(), isHeadless(), function,
				address, values);
	}

	@Override
	public byte[] readBytes(ModbusReadFunction function, int address, int count) throws IOException {
		return ModbusTransactionUtils.readBytes(createTransaction(), getUnitId(), isHeadless(), function,
				address, count);
	}

	@Override
	public void writeBytes(ModbusWriteFunction function, int address, byte[] values) throws IOException {
		ModbusTransactionUtils.writeBytes(createTransaction(), getUnitId(), isHeadless(), function,
				address, values);
	}

	@Override
	public String readString(ModbusReadFunction function, int address, int count, boolean trim,
			Charset charset) throws IOException {
		return ModbusTransactionUtils.readString(createTransaction(), getUnitId(), isHeadless(),
				function, address, count, trim, charset);
	}

	@Override
	public void writeString(ModbusWriteFunction function, int address, String value, Charset charset)
			throws IOException {
		ModbusTransactionUtils.writeString(createTransaction(), getUnitId(), isHeadless(), function,
				address, value, charset);
	}

}
