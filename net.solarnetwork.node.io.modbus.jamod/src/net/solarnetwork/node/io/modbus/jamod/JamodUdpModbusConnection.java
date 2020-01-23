/* ==================================================================
 * JamodUdpModbusConnection.java - 3/02/2018 8:30:47 AM
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
import java.util.BitSet;
import net.solarnetwork.node.io.modbus.AbstractModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusWriteFunction;
import net.wimpi.modbus.io.ModbusUDPTransaction;
import net.wimpi.modbus.net.UDPMasterConnection;

/**
 * Jamod UDP implementation of {@link ModbusConnection}.
 * 
 * @author matt
 * @version 2.0
 */
public class JamodUdpModbusConnection extends AbstractModbusConnection implements ModbusConnection {

	private final UDPMasterConnection connection;

	public JamodUdpModbusConnection(UDPMasterConnection conn, int unitId) {
		this(conn, unitId, false);
	}

	public JamodUdpModbusConnection(UDPMasterConnection conn, int unitId, boolean headless) {
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
		return "JamodUdpModbusConnection{host=" + portName + ",unit=" + getUnitId() + '}';
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
		if ( connection.isConnected() ) {
			connection.close();
		}
	}

	private ModbusUDPTransaction createTransaction() {
		ModbusUDPTransaction tx = new ModbusUDPTransaction(connection);
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
	public short[] readWords(ModbusReadFunction function, int address, int count) {
		return ModbusTransactionUtils.readWords(createTransaction(), getUnitId(), isHeadless(),
				function, address, count);
	}

	@Override
	public void writeSignedShorts(ModbusWriteFunction function, Integer address, short[] values) {
		ModbusTransactionUtils.writeSignedShorts(createTransaction(), getUnitId(), isHeadless(),
				function, address, values);
	}

	@Override
	public int[] readUnsignedShorts(ModbusReadFunction function, Integer address, int count) {
		return ModbusTransactionUtils.readUnsignedShorts(createTransaction(), getUnitId(), isHeadless(),
				function, address, count);
	}

	@Override
	public void writeUnsignedShorts(ModbusWriteFunction function, Integer address, int[] values) {
		ModbusTransactionUtils.writeUnsignedShorts(createTransaction(), getUnitId(), isHeadless(),
				function, address, values);
	}

	@Override
	public byte[] readBytes(ModbusReadFunction function, Integer address, int count) {
		return ModbusTransactionUtils.readBytes(createTransaction(), getUnitId(), isHeadless(), function,
				address, count);
	}

	@Override
	public void writeBytes(ModbusWriteFunction function, Integer address, byte[] values) {
		ModbusTransactionUtils.writeBytes(createTransaction(), getUnitId(), isHeadless(), function,
				address, values);
	}

	@Override
	public String readString(ModbusReadFunction function, Integer address, int count, boolean trim,
			String charsetName) {
		return ModbusTransactionUtils.readString(createTransaction(), getUnitId(), isHeadless(),
				function, address, count, trim, charsetName);
	}

	@Override
	public void writeString(ModbusWriteFunction function, Integer address, String value,
			String charsetName) {
		ModbusTransactionUtils.writeString(createTransaction(), getUnitId(), isHeadless(), function,
				address, value, charsetName);
	}

}
