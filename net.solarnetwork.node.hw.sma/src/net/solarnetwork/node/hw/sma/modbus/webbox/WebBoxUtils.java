/* ==================================================================
 * WebBoxUtils.java - 11/09/2020 9:54:10 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sma.modbus.webbox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.solarnetwork.node.hw.sma.modbus.SmaModbusConstants;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;
import net.solarnetwork.node.io.modbus.ModbusDataUtils;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;

/**
 * Utilities for communicating with a WebBox via Modbus.
 * 
 * @author matt
 * @version 1.0
 */
public final class WebBoxUtils {

	private WebBoxUtils() {
		// don't construct me
	}

	/**
	 * Parse a {@link WebBoxDeviceReference} from raw Modbus register values.
	 * 
	 * @param regs
	 *        the register values
	 * @param pos
	 *        the index within the register values to read from
	 * @return the reference
	 */
	public static WebBoxDeviceReference parseDeviceReference(short[] regs, int pos) {
		// @formatter:off
		return new WebBoxDeviceReference(
				ModbusDataUtils.toUnsignedInt16(regs[pos]),
				ModbusDataUtils.toUnsignedInt16(regs[pos + 3]),
				ModbusDataUtils.toUnsignedInt32(regs[pos + 1], regs[pos + 2]));
		// @formatter:on
	}

	/**
	 * Parse a {@link WebBoxDeviceReference} from a {@link ModbusData} instance.
	 * 
	 * @param d
	 *        the data
	 * @param addr
	 *        the register address to read the device reference from
	 * @return the reference
	 */
	public static WebBoxDeviceReference parseDeviceReference(ModbusData d, int addr) {
		short[] regs = new short[4];
		d.slice(regs, 0, addr, 4);
		return parseDeviceReference(regs, 0);
	}

	/**
	 * Read the available devices from a WebBox Modbus connection.
	 * 
	 * @param conn
	 *        the connection to the WebBox
	 * @param m
	 *        the mutable data to save the device data to
	 * @return the discovered device references
	 * @throws IOException
	 *         if any communication error occurs
	 */
	public static Collection<WebBoxDeviceReference> readAvailableDevices(ModbusConnection conn,
			MutableModbusData m) throws IOException {
		final int max = WebBoxRegister.DEVICE_UNIT_IDS_STARTING_ADDRESS + (4 * 245); // 245 == max number of references, 4 registers per ref
		final List<WebBoxDeviceReference> refs = new ArrayList<>(8);
		int addr = WebBoxRegister.DEVICE_UNIT_IDS_STARTING_ADDRESS;
		int len = 32;
		OUTER: while ( addr < max ) {
			if ( (addr + len) >= max ) {
				len = (max - addr);
			}
			short[] regs = conn.readWords(ModbusReadFunction.ReadHoldingRegister, addr, len);
			m.saveDataArray(regs, addr);
			addr += len;
			// stop reading device registers if we run into any NaN for a device ID
			for ( int i = 0; i < regs.length; i += 4 ) {
				if ( regs[i] == SmaModbusConstants.NAN_UINT16 ) {
					break OUTER;
				}
				refs.add(parseDeviceReference(regs, i));
			}
		}
		return refs;
	}

}
