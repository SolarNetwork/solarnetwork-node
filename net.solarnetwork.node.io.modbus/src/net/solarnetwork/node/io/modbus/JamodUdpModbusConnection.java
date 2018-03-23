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

package net.solarnetwork.node.io.modbus;

import static net.solarnetwork.node.io.modbus.ModbusHelper.integerArray;
import java.io.IOException;
import java.util.BitSet;
import java.util.Map;
import net.wimpi.modbus.io.ModbusUDPTransaction;
import net.wimpi.modbus.net.UDPMasterConnection;

/**
 * Jamod UDP implementation of {@link ModbusConnection}.
 * 
 * @author matt
 * @version 1.0
 * @since 2.4
 */
public class JamodUdpModbusConnection implements ModbusConnection {

	private final UDPMasterConnection connection;
	private final int unitId;
	private final boolean headless;

	public JamodUdpModbusConnection(UDPMasterConnection conn, int unitId) {
		this(conn, unitId, false);
	}

	public JamodUdpModbusConnection(UDPMasterConnection conn, int unitId, boolean headless) {
		super();
		this.connection = conn;
		this.unitId = unitId;
		this.headless = headless;
	}

	@Override
	public String toString() {
		String portName;
		try {
			portName = connection.getAddress().toString() + ':' + connection.getPort();
		} catch ( RuntimeException e ) {
			portName = "UNKNOWN";
		}
		return "JamodUdpModbusConnection{host=" + portName + ",unit=" + unitId + '}';
	}

	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

	@Override
	public int getUnitId() {
		return unitId;
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

	@Override
	public BitSet readDiscreetValues(Integer[] addresses, int count) {
		return ModbusTransactionUtils.readDiscreetValues(new ModbusUDPTransaction(connection), addresses,
				count, unitId, headless);
	}

	@Override
	public BitSet readDiscreetValues(Integer address, int count) {
		return ModbusTransactionUtils.readDiscreteValues(new ModbusUDPTransaction(connection), address,
				count, unitId, headless);
	}

	@Override
	public Boolean writeDiscreetValues(Integer[] addresses, BitSet bits) {
		return ModbusTransactionUtils.writeDiscreetValues(new ModbusUDPTransaction(connection),
				addresses, bits, unitId, headless);
	}

	@Override
	public BitSet readInputDiscreteValues(Integer address, int count) {
		return ModbusTransactionUtils.readInputDiscreteValues(new ModbusUDPTransaction(connection),
				address, count, unitId, headless);
	}

	@Override
	public Map<Integer, Integer> readInputValues(Integer[] addresses, int count) {
		return ModbusTransactionUtils.readInputValues(new ModbusUDPTransaction(connection), addresses,
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
	public short[] readSignedShorts(ModbusReadFunction function, Integer address, int count) {
		return ModbusTransactionUtils.readSignedShorts(new ModbusUDPTransaction(connection), unitId,
				headless, function, address, count);
	}

	@Override
	public void writeSignedShorts(ModbusWriteFunction function, Integer address, short[] values) {
		ModbusTransactionUtils.writeSignedShorts(new ModbusUDPTransaction(connection), unitId, headless,
				function, address, values);
	}

	@Override
	public int[] readUnsignedShorts(ModbusReadFunction function, Integer address, int count) {
		return ModbusTransactionUtils.readUnsignedShorts(new ModbusUDPTransaction(connection), unitId,
				headless, function, address, count);
	}

	@Override
	public void writeUnsignedShorts(ModbusWriteFunction function, Integer address, int[] values) {
		ModbusTransactionUtils.writeUnsignedShorts(new ModbusUDPTransaction(connection), unitId,
				headless, function, address, values);
	}

	@Override
	public byte[] readBytes(ModbusReadFunction function, Integer address, int count) {
		return ModbusTransactionUtils.readBytes(new ModbusUDPTransaction(connection), unitId, headless,
				function, address, count);
	}

	@Override
	public void writeBytes(ModbusWriteFunction function, Integer address, byte[] values) {
		ModbusTransactionUtils.writeBytes(new ModbusUDPTransaction(connection), unitId, headless,
				function, address, values);
	}

	@Override
	public String readString(ModbusReadFunction function, Integer address, int count, boolean trim,
			String charsetName) {
		return ModbusTransactionUtils.readString(new ModbusUDPTransaction(connection), unitId, headless,
				function, address, count, trim, charsetName);
	}

	@Override
	public void writeString(ModbusWriteFunction function, Integer address, String value,
			String charsetName) {
		ModbusTransactionUtils.writeString(new ModbusUDPTransaction(connection), unitId, headless,
				function, address, value, charsetName);
	}

}
