/* ==================================================================
 * JamodTcpModbusConnection.java - 3/02/2018 8:30:47 AM
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

package net.solarnetwork.node.io.modbus.jamod;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusWriteFunction;
import net.solarnetwork.node.io.modbus.support.AbstractModbusConnection;
import net.solarnetwork.util.ClassUtils;
import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.net.TCPMasterConnection;

/**
 * Jamod TCP implementation of {@link ModbusConnection}.
 * 
 * @author matt
 * @version 2.1
 */
public class JamodTcpModbusConnection extends AbstractModbusConnection implements ModbusConnection {

	private final TCPMasterConnection connection;

	public JamodTcpModbusConnection(TCPMasterConnection conn, int unitId) {
		this(conn, unitId, false);
	}

	public JamodTcpModbusConnection(TCPMasterConnection conn, int unitId, boolean headless) {
		super(unitId, headless);
		this.connection = conn;
	}

	@Override
	public String toString() {
		String portName;
		try {
			portName = connection.getAddress().toString() + ':' + connection.getPort();
		} catch ( RuntimeException e ) {
			portName = "UNKNOWN";
		}
		return "JamodTcpModbusConnection{host=" + portName + ",unit=" + getUnitId() + '}';
	}

	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

	@Override
	public void open() throws IOException {
		if ( !connection.isConnected() ) {
			try {
				connection.connect();
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

	private ModbusTCPTransaction createTransaction() {
		ModbusTCPTransaction tx = new ModbusTCPTransaction(connection);
		tx.setRetries(getRetries());

		// SN extended feature support; don't assume we are using this
		Map<String, Object> extendedProperties = new HashMap<String, Object>(2);
		extendedProperties.put("retryDelayMillis", getRetryDelayMs());
		extendedProperties.put("retryReconnect", isRetryReconnect());
		ClassUtils.setBeanProperties(tx, extendedProperties, true);

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
	public short[] readWords(ModbusReadFunction function, int address, int count) {
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
