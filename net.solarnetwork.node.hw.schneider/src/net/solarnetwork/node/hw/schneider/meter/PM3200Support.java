/* ==================================================================
 * PM3200Support.java - 28/02/2014 2:17:24 PM
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

package net.solarnetwork.node.hw.schneider.meter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.node.io.modbus.ModbusConnectionCallback;
import net.solarnetwork.node.io.modbus.ModbusHelper;
import net.solarnetwork.node.io.modbus.ModbusSerialConnectionFactory;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.StringUtils;
import net.wimpi.modbus.net.SerialConnection;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Supporting class for the PM3200 series power meter.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>connectionFactory</dt>
 * <dd>The {@link ModbusSerialConnectionFactory} to use.</dd>
 * <dt>unitId</dt>
 * <dd>The Modbus ID of the device to communicate with.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class PM3200Support {

	/** Key for the meter name, as a String. */
	public static final String INFO_KEY_METER_NAME = "Name";

	/** Key for the meter model, as a String. */
	public static final String INFO_KEY_METER_MODEL = "Model";

	/** Key for the meter serial number, as a Long. */
	public static final String INFO_KEY_METER_SERIAL_NUMBER = "Serial Number";

	/** Key for the meter manufacturer, as a String. */
	public static final String INFO_KEY_METER_MANUFACTURER = "Manufacturer";

	/** Key for the meter manufacture date, as a {@link LocalDate}. */
	public static final String INFO_KEY_METER_MANUFACTURE_DATE = "Manufacture Date";

	public static final Integer ADDR_SYSTEM_METER_NAME = 29;
	public static final Integer ADDR_SYSTEM_METER_MODEL = 49;
	public static final Integer ADDR_SYSTEM_METER_MANUFACTURER = 69;
	public static final Integer ADDR_SYSTEM_METER_SERIAL_NUMBER = 129;
	public static final Integer ADDR_SYSTEM_METER_MANUFACTURE_DATE = 131;

	public static final Integer ADDR_DATA_START = 2999;

	// current
	public static final Integer ADDR_DATA_I1 = ADDR_DATA_START;
	public static final Integer ADDR_DATA_I2 = 3001;
	public static final Integer ADDR_DATA_I3 = 3003;
	public static final Integer ADDR_DATA_I_NEUTRAL = 3005;
	public static final Integer ADDR_DATA_I_AVERAGE = 3009;

	// voltage
	public static final Integer ADDR_DATA_V_L1_L2 = 3019;
	public static final Integer ADDR_DATA_V_L2_L3 = 3021;
	public static final Integer ADDR_DATA_V_L3_L1 = 3023;
	public static final Integer ADDR_DATA_V_L_L_AVERAGE = 3025;
	public static final Integer ADDR_DATA_V_L1_NEUTRAL = 3027;
	public static final Integer ADDR_DATA_V_L2_NEUTRAL = 3029;
	public static final Integer ADDR_DATA_V_L3_NEUTRAL = 3031;
	public static final Integer ADDR_DATA_V_NEUTRAL_AVERAGE = 3035;

	// power
	public static final Integer ADDR_DATA_ACTIVE_POWER_P1 = 3053;
	public static final Integer ADDR_DATA_ACTIVE_POWER_P2 = 3055;
	public static final Integer ADDR_DATA_ACTIVE_POWER_P3 = 3057;
	public static final Integer ADDR_DATA_ACTIVE_POWER_TOTAL = 3059;
	public static final Integer ADDR_DATA_REACTIVE_POWER_P1 = 3061;
	public static final Integer ADDR_DATA_REACTIVE_POWER_P2 = 3063;
	public static final Integer ADDR_DATA_REACTIVE_POWER_P3 = 3065;
	public static final Integer ADDR_DATA_REACTIVE_POWER_TOTAL = 3067;
	public static final Integer ADDR_DATA_APPARENT_POWER_P1 = 3069;
	public static final Integer ADDR_DATA_APPARENT_POWER_P2 = 3071;
	public static final Integer ADDR_DATA_APPARENT_POWER_P3 = 3073;
	public static final Integer ADDR_DATA_APPARENT_POWER_TOTAL = 3075;
	public static final Integer ADDR_DATA_FREQUENCY = 3109;

	// total energy
	public static final Integer ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT = 3203;
	public static final Integer ADDR_DATA_TOTAL_ACTIVE_ENERGY_EXPORT = 3207;
	public static final Integer ADDR_DATA_TOTAL_REACTIVE_ENERGY_IMPORT = 3219;
	public static final Integer ADDR_DATA_TOTAL_REACTIVE_ENERGY_EXPORT = 3223;
	public static final Integer ADDR_DATA_TOTAL_APPARENT_ENERGY_IMPORT = 3235;
	public static final Integer ADDR_DATA_TOTAL_APPARENT_ENERGY_EXPORT = 3239;

	private Integer unitId = 1;
	private Map<String, Object> meterInfo;

	private OptionalService<ModbusSerialConnectionFactory> connectionFactory;

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Read the name of the meter.
	 * 
	 * @param conn
	 *        the serial connection
	 * @return the meter name, or <em>null</em> if not available
	 */
	public String getMeterName(SerialConnection conn) {
		return ModbusHelper.readUTF8String(conn, ADDR_SYSTEM_METER_NAME, 20, unitId, true);
	}

	/**
	 * Read the model of the meter.
	 * 
	 * @param conn
	 *        the serial connection
	 * @return the meter model, or <em>null</em> if not available
	 */
	public String getMeterModel(SerialConnection conn) {
		return ModbusHelper.readUTF8String(conn, ADDR_SYSTEM_METER_MODEL, 20, unitId, true);
	}

	/**
	 * Read the manufacturer of the meter.
	 * 
	 * @param conn
	 *        the serial connection
	 * @return the meter manufacturer, or <em>null</em> if not available
	 */
	public String getMeterManufacturer(SerialConnection conn) {
		return ModbusHelper.readUTF8String(conn, ADDR_SYSTEM_METER_MANUFACTURER, 20, unitId, true);
	}

	/**
	 * Read the serial number of the meter.
	 * 
	 * @param conn
	 *        the connection
	 * @return the meter serial number, or <em>null</em> if not available
	 */
	public Long getMeterSerialNumber(SerialConnection conn) {
		Long result = null;
		Integer[] data = ModbusHelper.readValues(conn, ADDR_SYSTEM_METER_SERIAL_NUMBER, 2, unitId);
		if ( data != null && data.length == 2 ) {
			int longValue = ModbusHelper.getLongWord(data[0], data[1]);
			result = (long) longValue;
		}
		return result;
	}

	/**
	 * Read the manufacture date of the meter.
	 * 
	 * @param conn
	 *        the connection
	 * @return the meter manufacture date, or <em>null</em> if not available
	 */
	public LocalDateTime getMeterManufactureDate(SerialConnection conn) {
		Integer[] data = ModbusHelper.readValues(conn, ADDR_SYSTEM_METER_MANUFACTURE_DATE, 4, unitId);
		return parseDateTime(data);
	}

	/**
	 * Read general meter info and return a map of the results. See the various
	 * {@code INFO_KEY_*} constants for information on the values returned in
	 * the result map.
	 * 
	 * @param conn
	 *        the connection to use
	 * @return a map with general meter information populated
	 */
	public Map<String, Object> readMeterInfo(SerialConnection conn) {
		Map<String, Object> result = new LinkedHashMap<String, Object>(8);
		String str = getMeterName(conn);
		if ( str != null ) {
			result.put(INFO_KEY_METER_NAME, str);
		}
		str = getMeterModel(conn);
		if ( str != null ) {
			result.put(INFO_KEY_METER_MODEL, str);
		}
		str = getMeterManufacturer(conn);
		if ( result != null ) {
			result.put(INFO_KEY_METER_MANUFACTURER, str);
		}
		LocalDateTime dt = getMeterManufactureDate(conn);
		if ( dt != null ) {
			result.put(INFO_KEY_METER_MANUFACTURE_DATE, dt.toLocalDate());
		}
		Long l = getMeterSerialNumber(conn);
		if ( l != null ) {
			result.put(INFO_KEY_METER_SERIAL_NUMBER, l);
		}
		return result;
	}

	/**
	 * Return an informational message composed of general meter info. This
	 * method will call {@link #readMeterInfo(SerialConnection)} and return a
	 * {@code /} (forward slash) delimited string of the resulting values.
	 * 
	 * @return info message
	 */
	public String getMeterInfoMessage() {
		if ( meterInfo == null ) {
			meterInfo = ModbusHelper.execute(connectionFactory,
					new ModbusConnectionCallback<Map<String, Object>>() {

						@Override
						public Map<String, Object> doInConnection(SerialConnection conn)
								throws IOException {
							return readMeterInfo(conn);
						}
					});
		}
		if ( meterInfo == null ) {
			return null;
		}
		return StringUtils.delimitedStringFromCollection(meterInfo.values(), " / ");
	}

	/**
	 * Parse a DateTime value from raw Modbus register values. The {@code data}
	 * array is expected to have a length of {@code 4}.
	 * 
	 * @param data
	 *        the data array
	 * @return the parsed date, or <em>null</em> if not available
	 */
	public static LocalDateTime parseDateTime(final Integer[] data) {
		LocalDateTime result = null;
		if ( data != null && data.length == 4 ) {
			int year = 2000 + (data[0].intValue() & 0x7F);
			int month = (data[1].intValue() & 0xF00) >> 8;
			int day = (data[1].intValue() & 0x1F);
			int hour = (data[2].intValue() & 0x1F00) >> 8;
			int minute = (data[2].intValue() & 0x3F);
			int ms = (data[3].intValue()); // this is really seconds + milliseconds
			int sec = ms / 1000;
			ms = ms - (sec * 1000);
			result = new LocalDateTime(year, month, day, hour, minute, sec, ms);
		}
		return result;
	}

	/**
	 * Parse a 32-bit float value from a data array.
	 * 
	 * @param data
	 *        the data array
	 * @param offset
	 *        the offset within the array to parse the value from
	 * @return the float, or <em>null</em> if not available
	 */
	public static Float parseFloat32(Integer[] data, int offset) {
		Float result = null;
		if ( data != null && offset >= 0 && data.length > (offset + 1) ) {
			result = ModbusHelper.parseFloat32(new Integer[] { data[offset], data[offset + 1] });
		}
		return result;
	}

	/**
	 * Parse a 64-bit integer value from a data array.
	 * 
	 * @param data
	 *        the data array
	 * @param offset
	 *        the offset within the array to parse the value from
	 * @return the long, or <em>null</em> if not available
	 */
	public static Long parseInt64(Integer[] data, int offset) {
		Long result = null;
		if ( data != null && offset >= 0 && data.length > (offset + 3) ) {
			result = ModbusHelper.parseInt64(new Integer[] { data[offset], data[offset + 1],
					data[offset + 2], data[offset + 3] });
		}
		return result;
	}

	public List<SettingSpecifier> getSettingSpecifiers() {
		PM3200Support defaults = new PM3200Support();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(10);

		// get current value
		BasicTitleSettingSpecifier info = new BasicTitleSettingSpecifier("info", "N/A", true);
		try {
			String infoMsg = getMeterInfoMessage();
			info.setDefaultValue(infoMsg);
		} catch ( RuntimeException e ) {
			log.debug("Error reading {} info: {}", unitId, e.getMessage());
		}
		results.add(info);

		results.add(new BasicTextFieldSettingSpecifier("connectionFactory.propertyFilters['UID']",
				"/dev/ttyUSB0"));
		results.add(new BasicTextFieldSettingSpecifier("unitId", defaults.unitId.toString()));

		return results;
	}

	public Integer getUnitId() {
		return unitId;
	}

	public void setUnitId(Integer unitId) {
		this.unitId = unitId;
	}

	public OptionalService<ModbusSerialConnectionFactory> getConnectionFactory() {
		return connectionFactory;
	}

	public void setConnectionFactory(OptionalService<ModbusSerialConnectionFactory> connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

}
