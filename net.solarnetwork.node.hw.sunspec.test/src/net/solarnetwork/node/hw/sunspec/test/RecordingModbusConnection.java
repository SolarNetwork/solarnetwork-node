/* ==================================================================
 * RecordingModbusConnection.java - 5/10/2026 5:31:07 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sunspec.test;

import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusWriteFunction;
import net.solarnetwork.node.io.modbus.support.StaticDataMapModbusConnection;
import net.solarnetwork.util.IntShortMap;

/**
 * A writable static data Modbus connection that records the read and write
 * requests made to it.
 *
 * @author matt
 * @version 1.0
 */
public class RecordingModbusConnection extends StaticDataMapModbusConnection {

	private final List<List<Integer>> reads = new ArrayList<>();
	private final List<List<Integer>> writes = new ArrayList<>();

	/**
	 * Constructor.
	 *
	 * @param data
	 *        the data
	 */
	public RecordingModbusConnection(IntShortMap data) {
		super(data);
	}

	@Override
	public short[] readWords(ModbusReadFunction function, int address, int count) {
		reads.add(List.of(address, count));
		return super.readWords(function, address, count);
	}

	@Override
	public int[] readWordsUnsigned(ModbusReadFunction function, int address, int count) {
		reads.add(List.of(address, count));
		return super.readWordsUnsigned(function, address, count);
	}

	@Override
	public void writeWords(ModbusWriteFunction function, int address, short[] values) {
		writes.add(List.of(address, values.length));
		super.writeWords(function, address, values);
	}

	@Override
	public void writeWords(ModbusWriteFunction function, int address, int[] values) {
		writes.add(List.of(address, values.length));
		super.writeWords(function, address, values);
	}

	/**
	 * Get the read requests made to this connection.
	 *
	 * @return the address and register count of each read request, in the order
	 *         they were made
	 */
	public List<List<Integer>> getReads() {
		return reads;
	}

	/**
	 * Get the write requests made to this connection.
	 *
	 * @return the address and register count of each write request, in the
	 *         order they were made
	 */
	public List<List<Integer>> getWrites() {
		return writes;
	}

}
