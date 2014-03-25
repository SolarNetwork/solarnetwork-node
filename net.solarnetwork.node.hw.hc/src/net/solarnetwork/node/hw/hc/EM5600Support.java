/* ==================================================================
 * EM5600Support.java - Mar 26, 2014 6:00:50 AM
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

package net.solarnetwork.node.hw.hc;

import java.util.LinkedHashMap;
import java.util.Map;
import net.solarnetwork.node.io.modbus.ModbusHelper;
import net.solarnetwork.node.io.modbus.ModbusSupport;
import net.wimpi.modbus.net.SerialConnection;
import org.joda.time.LocalDateTime;

/**
 * Supporting class for the EM5600 series power meter.
 * 
 * <p>
 * The EM5600 series watt-hour meter supports the following serial port
 * configuration:
 * </p>
 * 
 * <ul>
 * <li><b>Baud</b> - 1200, 2400, 4800, 9600, 19200</li>
 * <li><b>Mode</b> - RTU</li>
 * <li><b>Start bit</b> - 1</li>
 * <li><b>Data bits</b> - 8</li>
 * <li><b>Parity</b> - None</li>
 * <li><b>Stop bit</b> - 1</li>
 * <li><b>Error checking</b> - CRC</li>
 * </ul>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt></dt>
 * <dd></dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class EM5600Support extends ModbusSupport {

	// unit constants: note these are expressed as integers, values are derived
	// by dividing the unscaled value by these
	public static final int UNIT_U = 100;
	public static final int UNIT_A = 10000;
	public static final int UNIT_P = 20;

	// meter info
	public static final Integer ADDR_SYSTEM_METER_MODEL = 0x0;
	public static final Integer ADDR_SYSTEM_METER_HARDWARE_VERSION = 0x2; // length 2 ASCII characters
	public static final Integer ADDR_SYSTEM_METER_SERIAL_NUMBER = 0x10; // length 4 ASCII characters
	public static final Integer ADDR_SYSTEM_METER_MANUFACTURE_DATE = 0x18; // length 2 F10 encoding

	// current
	public static final Integer ADDR_DATA_I1 = 0x130;
	public static final Integer ADDR_DATA_I2 = 0x131;
	public static final Integer ADDR_DATA_I3 = 0x132;
	public static final Integer ADDR_DATA_I_AVERAGE = 0x133;

	// voltage
	public static final Integer ADDR_DATA_V_L1_NEUTRAL = 0x136;
	public static final Integer ADDR_DATA_V_L2_NEUTRAL = 0x137;
	public static final Integer ADDR_DATA_V_L3_NEUTRAL = 0x138;
	public static final Integer ADDR_DATA_V_NEUTRAL_AVERAGE = 0x139;
	public static final Integer ADDR_DATA_V_L1_L2 = 0x13B;
	public static final Integer ADDR_DATA_V_L2_L3 = 0x13C;
	public static final Integer ADDR_DATA_V_L3_L1 = 0x13D;
	public static final Integer ADDR_DATA_V_L_L_AVERAGE = 0x13E;

	// power
	public static final Integer ADDR_DATA_ACTIVE_POWER_TOTAL = 0x140;
	public static final Integer ADDR_DATA_REACTIVE_POWER_TOTAL = 0x141;
	public static final Integer ADDR_DATA_APPARENT_POWER_TOTAL = 0x142;
	public static final Integer ADDR_DATA_POWER_FACTOR_TOTAL = 0x143;
	public static final Integer ADDR_DATA_FREQUENCY = 0x144;
	public static final Integer ADDR_DATA_ACTIVE_POWER_P1 = 0x145;
	public static final Integer ADDR_DATA_REACTIVE_POWER_P1 = 0x146;
	public static final Integer ADDR_DATA_APPARENT_POWER_P1 = 0x147;
	public static final Integer ADDR_DATA_POWER_FACTOR_P1 = 0x148;
	public static final Integer ADDR_DATA_ACTIVE_POWER_P2 = 0x149;
	public static final Integer ADDR_DATA_REACTIVE_POWER_P2 = 0x14A;
	public static final Integer ADDR_DATA_APPARENT_POWER_P2 = 0x14B;
	public static final Integer ADDR_DATA_POWER_FACTOR_P2 = 0x14C;
	public static final Integer ADDR_DATA_ACTIVE_POWER_P3 = 0x14D;
	public static final Integer ADDR_DATA_REACTIVE_POWER_P3 = 0x14E;
	public static final Integer ADDR_DATA_APPARENT_POWER_P3 = 0x14F;
	public static final Integer ADDR_DATA_POWER_FACTOR_P3 = 0x150;
	public static final Integer ADDR_DATA_PHASE_ROTATION = 0x151;

	// energy
	public static final Integer ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT = 0x160; // length 2
	public static final Integer ADDR_DATA_TOTAL_ACTIVE_ENERGY_EXPORT = 0x162;
	public static final Integer ADDR_DATA_TOTAL_REACTIVE_ENERGY_IMPORT = 0x164;
	public static final Integer ADDR_DATA_TOTAL_REACTIVE_ENERGY_EXPORT = 0x166;

	// units
	public static final Integer ADDR_DATA_ENERGY_UNIT = 0x17E;
	public static final Integer ADDR_DATA_PT_RATIO = 0x200A;
	public static final Integer ADDR_DATA_CT_RATIO = 0x200B;

	public static final int ENERGY_UNIT_WH = 0;

	@Override
	protected Map<String, Object> readDeviceInfo(SerialConnection conn) {
		// note the order of these elements determines the output of getDeviceInfoMessage()
		Map<String, Object> result = new LinkedHashMap<String, Object>(8);
		String str;
		Integer i;
		i = getMeterModel(conn);
		if ( i != null ) {
			result.put(INFO_KEY_DEVICE_MODEL, i);
		}
		LocalDateTime dt = getMeterManufactureDate(conn);
		if ( dt != null ) {
			result.put(INFO_KEY_DEVICE_MANUFACTURE_DATE, dt.toLocalDate());
		}
		str = getMeterSerialNumber(conn);
		if ( str != null ) {
			result.put(INFO_KEY_DEVICE_SERIAL_NUMBER, str);
		}
		return result;
	}

	/**
	 * Read the serial number of the meter.
	 * 
	 * @param conn
	 *        the connection
	 * @return the meter serial number, or <em>null</em> if not available
	 */
	public String getMeterSerialNumber(SerialConnection conn) {
		return ModbusHelper.readASCIIString(conn, ADDR_SYSTEM_METER_SERIAL_NUMBER, 4, getUnitId(), true);
	}

	/**
	 * Read the model number of the meter.
	 * 
	 * @param conn
	 *        the connection
	 * @return the meter model number, or <em>null</em> if not available
	 */
	public Integer getMeterModel(SerialConnection conn) {
		Integer[] data = ModbusHelper.readValues(conn, ADDR_SYSTEM_METER_MODEL, 1, getUnitId());
		if ( data != null && data.length > 0 ) {
			return data[0];
		}
		return null;
	}

	/**
	 * Read the manufacture date of the meter.
	 * 
	 * @param conn
	 *        the connection
	 * @return the meter manufacture date, or <em>null</em> if not available
	 */
	public LocalDateTime getMeterManufactureDate(SerialConnection conn) {
		Integer[] data = ModbusHelper.readValues(conn, ADDR_SYSTEM_METER_MANUFACTURE_DATE, 2,
				getUnitId());
		return parseDateTime(data);
	}

	/**
	 * Parse a DateTime value from raw Modbus register values. The {@code data}
	 * array is expected to have a length of {@code 2} and follow the documented
	 * F10 and F9 formats.
	 * 
	 * @param data
	 *        the data array
	 * @return the parsed date, or <em>null</em> if not available
	 */
	public static LocalDateTime parseDateTime(final Integer[] data) {
		LocalDateTime result = null;
		if ( data != null && data.length == 2 ) {
			int day = (data[0].intValue() & 0x1F00) >> 8; // 1 - 31
			int year = 2000 + (data[1].intValue() & 0xFF00) >> 8; // 0 - 255
			int month = (data[1].intValue() & 0xC); //1-12
			result = new LocalDateTime(year, month, day, 0, 0, 0, 0);
		}
		return result;
	}
}
