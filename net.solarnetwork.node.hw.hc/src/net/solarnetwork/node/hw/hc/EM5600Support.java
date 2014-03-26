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

import static net.solarnetwork.node.hw.hc.EM5600Data.ADDR_SYSTEM_METER_MANUFACTURE_DATE;
import static net.solarnetwork.node.hw.hc.EM5600Data.ADDR_SYSTEM_METER_MODEL;
import static net.solarnetwork.node.hw.hc.EM5600Data.ADDR_SYSTEM_METER_SERIAL_NUMBER;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.node.io.modbus.ModbusHelper;
import net.solarnetwork.node.io.modbus.ModbusSupport;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
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

	private boolean captureTotal = true;
	private boolean capturePhaseA = false;
	private boolean capturePhaseB = false;
	private boolean capturePhaseC = false;

	/**
	 * An instance of {@link EM5600Data} to support keeping the last-read values
	 * of data in memory.
	 */
	protected final EM5600Data sample = new EM5600Data();

	public List<SettingSpecifier> getSettingSpecifiers() {
		EM5600Support defaults = new EM5600Support();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(10);

		// get current value
		BasicTitleSettingSpecifier info = new BasicTitleSettingSpecifier("info", "N/A", true);
		try {
			String infoMsg = getDeviceInfoMessage();
			info.setDefaultValue(infoMsg);
		} catch ( RuntimeException e ) {
			log.debug("Error reading {} info: {}", getUnitId(), e.getMessage());
		}
		results.add(info);
		results.add(new BasicTextFieldSettingSpecifier("uid", defaults.getUid()));
		results.add(new BasicTextFieldSettingSpecifier("groupUID", defaults.getGroupUID()));
		results.add(new BasicTextFieldSettingSpecifier("connectionFactory.propertyFilters['UID']",
				"/dev/ttyUSB0"));
		results.add(new BasicTextFieldSettingSpecifier("unitId", defaults.getUnitId().toString()));

		return results;
	}

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
		int[] data = ModbusHelper.readInts(conn, ADDR_SYSTEM_METER_MANUFACTURE_DATE, 2, getUnitId());
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
	public static LocalDateTime parseDateTime(final int[] data) {
		LocalDateTime result = null;
		if ( data != null && data.length == 2 ) {
			int day = (data[0] & 0x1F00) >> 8; // 1 - 31
			int year = 2000 + (data[1] & 0xFF00) >> 8; // 0 - 255
			int month = (data[1] & 0xC); //1-12
			result = new LocalDateTime(year, month, day, 0, 0, 0, 0);
		}
		return result;
	}

	public boolean isCaptureTotal() {
		return captureTotal;
	}

	public void setCaptureTotal(boolean captureTotal) {
		this.captureTotal = captureTotal;
	}

	public boolean isCapturePhaseA() {
		return capturePhaseA;
	}

	public void setCapturePhaseA(boolean capturePhaseA) {
		this.capturePhaseA = capturePhaseA;
	}

	public boolean isCapturePhaseB() {
		return capturePhaseB;
	}

	public void setCapturePhaseB(boolean capturePhaseB) {
		this.capturePhaseB = capturePhaseB;
	}

	public boolean isCapturePhaseC() {
		return capturePhaseC;
	}

	public void setCapturePhaseC(boolean capturePhaseC) {
		this.capturePhaseC = capturePhaseC;
	}

}
