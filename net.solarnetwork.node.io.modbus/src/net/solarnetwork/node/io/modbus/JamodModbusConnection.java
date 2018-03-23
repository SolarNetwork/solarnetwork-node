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

package net.solarnetwork.node.io.modbus;

import static net.solarnetwork.node.io.modbus.ModbusHelper.integerArray;
import java.io.IOException;
import java.util.BitSet;
import java.util.Map;
import net.wimpi.modbus.io.ModbusSerialTransaction;
import net.wimpi.modbus.net.SerialConnection;

/**
 * Jamod serial implementation of {@link ModbusConnection}.
 * 
 * @author matt
 * @version 1.2
 * @since 2.1
 */
public class JamodModbusConnection implements ModbusConnection {

	private final SerialConnection connection;
	private final int unitId;
	private final boolean headless;

	public JamodModbusConnection(SerialConnection conn, int unitId) {
		this(conn, unitId, true);
	}

	public JamodModbusConnection(SerialConnection conn, int unitId, boolean headless) {
		super();
		this.connection = conn;
		this.unitId = unitId;
		this.headless = headless;
	}

	final SerialConnection getSerialConnection() {
		return connection;
	}

	@Override
	public String toString() {
		String portName;
		try {
			portName = connection.getSerialPort().getName();
		} catch ( RuntimeException e ) {
			portName = "UNKNOWN";
		}
		return "JamodModbusConnection{port=" + portName + ",unit=" + unitId + '}';
	}

	@Override
	public final int getUnitId() {
		return unitId;
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

	@Override
	public BitSet readDiscreetValues(Integer[] addresses, int count) {
		return ModbusTransactionUtils.readDiscreetValues(new ModbusSerialTransaction(connection),
				addresses, count, unitId, headless);
	}

	@Override
	public BitSet readDiscreetValues(Integer address, int count) {
		return ModbusTransactionUtils.readDiscreteValues(new ModbusSerialTransaction(connection),
				address, count, unitId, headless);
	}

	@Override
	public Boolean writeDiscreetValues(Integer[] addresses, BitSet bits) {
		return ModbusTransactionUtils.writeDiscreetValues(new ModbusSerialTransaction(connection),
				addresses, bits, unitId, headless);
	}

	@Override
	public BitSet readInputDiscreteValues(Integer address, int count) {
		return ModbusTransactionUtils.readInputDiscreteValues(new ModbusSerialTransaction(connection),
				address, count, unitId, headless);
	}

	@Override
	public Map<Integer, Integer> readInputValues(Integer[] addresses, int count) {
		return ModbusTransactionUtils.readInputValues(new ModbusSerialTransaction(connection), addresses,
				count, unitId, headless);
	}

	@Override
	public int[] readInputValues(Integer address, int count) {
		return readUnsignedShorts(ModbusReadFunction.ReadInputRegister, address, count);
	}

	@Override
	public byte[] readBytes(Integer address, int count) {
		return readBytes(ModbusReadFunction.ReadHoldingRegister, address, count);
	}

	@Override
	public String readString(Integer address, int count, boolean trim, String charsetName) {
		return readString(ModbusReadFunction.ReadHoldingRegister, address, count, trim, charsetName);
	}

	@Override
	public int[] readInts(Integer address, int count) {
		return readUnsignedShorts(ModbusReadFunction.ReadHoldingRegister, address, count);
	}

	@Override
	public short[] readSignedShorts(Integer address, int count) {
		return readSignedShorts(ModbusReadFunction.ReadHoldingRegister, address, count);
	}

	@Override
	public Integer[] readValues(Integer address, int count) {
		return integerArray(readUnsignedShorts(ModbusReadFunction.ReadHoldingRegister, address, count));
	}

	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

	@Override
	public short[] readSignedShorts(ModbusReadFunction function, Integer address, int count) {
		return ModbusTransactionUtils.readSignedShorts(new ModbusSerialTransaction(connection), unitId,
				headless, function, address, count);
	}

	@Override
	public void writeSignedShorts(ModbusWriteFunction function, Integer address, short[] values) {
		ModbusTransactionUtils.writeSignedShorts(new ModbusSerialTransaction(connection), unitId,
				headless, function, address, values);
	}

	@Override
	public int[] readUnsignedShorts(ModbusReadFunction function, Integer address, int count) {
		return ModbusTransactionUtils.readUnsignedShorts(new ModbusSerialTransaction(connection), unitId,
				headless, function, address, count);
	}

	@Override
	public void writeUnsignedShorts(ModbusWriteFunction function, Integer address, int[] values) {
		ModbusTransactionUtils.writeUnsignedShorts(new ModbusSerialTransaction(connection), unitId,
				headless, function, address, values);
	}

	@Override
	public byte[] readBytes(ModbusReadFunction function, Integer address, int count) {
		return ModbusTransactionUtils.readBytes(new ModbusSerialTransaction(connection), unitId,
				headless, function, address, count);
	}

	@Override
	public void writeBytes(ModbusWriteFunction function, Integer address, byte[] values) {
		ModbusTransactionUtils.writeBytes(new ModbusSerialTransaction(connection), unitId, headless,
				function, address, values);
	}

	@Override
	public String readString(ModbusReadFunction function, Integer address, int count, boolean trim,
			String charsetName) {
		return ModbusTransactionUtils.readString(new ModbusSerialTransaction(connection), unitId,
				headless, function, address, count, trim, charsetName);
	}

	@Override
	public void writeString(ModbusWriteFunction function, Integer address, String value,
			String charsetName) {
		ModbusTransactionUtils.writeString(new ModbusSerialTransaction(connection), unitId, headless,
				function, address, value, charsetName);
	}

}
