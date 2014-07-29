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

import java.io.IOException;
import java.util.BitSet;
import java.util.Map;
import net.wimpi.modbus.net.SerialConnection;

/**
 * Jamod implementation of {@link ModbusConnection}.
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public class JamodModbusConnection implements ModbusConnection {

	private final SerialConnection connection;
	private final int unitId;

	public JamodModbusConnection(SerialConnection conn, int unitId) {
		super();
		this.connection = conn;
		this.unitId = unitId;
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
		return ModbusHelper.readDiscreetValues(connection, addresses, count, unitId);
	}

	@Override
	public Boolean writeDiscreetValues(Integer[] addresses, BitSet bits) {
		return ModbusHelper.writeDiscreetValues(connection, addresses, bits, unitId);
	}

	@Override
	public Map<Integer, Integer> readInputValues(Integer[] addresses, int count) {
		return ModbusHelper.readInputValues(connection, addresses, count, unitId);
	}

	@Override
	public byte[] readBytes(Integer address, int count) {
		return ModbusHelper.readBytes(connection, address, count, unitId);
	}

	@Override
	public String readString(Integer address, int count, boolean trim, String charsetName) {
		return ModbusHelper.readString(connection, address, count, unitId, trim, charsetName);
	}

	@Override
	public int[] readInts(Integer address, int count) {
		return ModbusHelper.readInts(connection, address, count, unitId);
	}

	@Override
	public short[] readSignedShorts(Integer address, int count) {
		return ModbusHelper.readSignedShorts(connection, address, count, unitId);
	}

	@Override
	public Integer[] readValues(Integer address, int count) {
		return ModbusHelper.readValues(connection, address, count, unitId);
	}

	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

}
