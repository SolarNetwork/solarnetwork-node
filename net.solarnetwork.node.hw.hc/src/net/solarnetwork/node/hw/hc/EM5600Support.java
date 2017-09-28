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
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusDeviceDatumDataSourceSupport;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.StringUtils;

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
 * @author matt
 * @version 2.3
 */
public class EM5600Support extends ModbusDeviceDatumDataSourceSupport {

	/** The default source ID applied for the total reading values. */
	public static final String MAIN_SOURCE_ID = "Main";

	private Map<ACPhase, String> sourceMapping = getDefaulSourceMapping();

	/**
	 * An instance of {@link EM5600Data} to support keeping the last-read values
	 * of data in memory.
	 */
	protected final EM5600Data sample = new EM5600Data();

	private UnitFactor unitFactor = null;

	/**
	 * Get a default {@code sourceMapping} value. This maps only the {@code 0}
	 * source to the value {@code Main}.
	 * 
	 * @return mapping
	 */
	public static Map<ACPhase, String> getDefaulSourceMapping() {
		Map<ACPhase, String> result = new EnumMap<ACPhase, String>(ACPhase.class);
		result.put(ACPhase.Total, MAIN_SOURCE_ID);
		return result;
	}

	/**
	 * Set a {@code sourceMapping} Map via an encoded String value.
	 * 
	 * <p>
	 * The format of the {@code mapping} String should be:
	 * </p>
	 * 
	 * <pre>
	 * key=val[,key=val,...]
	 * </pre>
	 * 
	 * <p>
	 * Whitespace is permitted around all delimiters, and will be stripped from
	 * the keys and values.
	 * </p>
	 * 
	 * @param mapping
	 *        the encoding mapping
	 * @see #getSourceMappingValue()
	 */
	public void setSourceMappingValue(String mapping) {
		Map<String, String> m = StringUtils.commaDelimitedStringToMap(mapping);
		Map<ACPhase, String> kindMap = new EnumMap<ACPhase, String>(ACPhase.class);
		if ( m != null )
			for ( Map.Entry<String, String> me : m.entrySet() ) {
				String k = me.getKey();
				ACPhase mk;
				try {
					mk = ACPhase.valueOf(k);
				} catch ( RuntimeException e ) {
					log.info("'{}' is not a valid ACPhase value, ignoring.", k);
					continue;
				}
				kindMap.put(mk, me.getValue());
			}
		setSourceMapping(kindMap);
	}

	/**
	 * Get a delimited string representation of the {@link #getSourceMapping()}
	 * map.
	 * 
	 * <p>
	 * The format of the {@code mapping} String should be:
	 * </p>
	 * 
	 * <pre>
	 * key=val[,key=val,...]
	 * </pre>
	 * 
	 * @return the encoded mapping
	 * @see #setSourceMappingValue(String)
	 */
	public String getSourceMappingValue() {
		return StringUtils.delimitedStringFromMap(sourceMapping);
	}

	/**
	 * Get a source ID value for a given measurement kind.
	 * 
	 * @param kind
	 *        the measurement kind
	 * @return the source ID value, or <em>null</em> if not available
	 */
	public String getSourceIdForACPhase(ACPhase kind) {
		return (sourceMapping == null ? null : sourceMapping.get(kind));
	}

	/**
	 * Get settings supported by this class. Extending classes can use this to
	 * support the Settings API.
	 * 
	 * @return list of settings
	 */
	public List<SettingSpecifier> getSettingSpecifiers() {
		EM5600Support defaults = new EM5600Support();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(10);

		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(), true));
		results.add(new BasicTitleSettingSpecifier("sample", getSampleMessage(sample), true));

		/*
		 * BasicRadioGroupSettingSpecifier unitFactorSpec = new
		 * BasicRadioGroupSettingSpecifier( "unitFactor",
		 * defaults.getUnitFactor().toString()); Map<String, String> radioValues
		 * = new LinkedHashMap<String, String>(3); for ( UnitFactor f :
		 * UnitFactor.values() ) { radioValues.put(f.toString(),
		 * f.getDisplayName()); } unitFactorSpec.setValueTitles(radioValues);
		 * results.add(unitFactorSpec);
		 */

		results.add(new BasicTextFieldSettingSpecifier("uid", defaults.getUid()));
		results.add(new BasicTextFieldSettingSpecifier("groupUID", defaults.getGroupUID()));
		results.add(new BasicTextFieldSettingSpecifier("modbusNetwork.propertyFilters['UID']",
				"Serial Port"));
		results.add(new BasicTextFieldSettingSpecifier("unitId", String.valueOf(defaults.getUnitId())));
		results.add(new BasicTextFieldSettingSpecifier("sourceMappingValue",
				defaults.getSourceMappingValue()));

		results.add(new BasicToggleSettingSpecifier("backwards", Boolean.FALSE));

		return results;
	}

	private String getInfoMessage() {
		String msg = null;
		try {
			msg = getDeviceInfoMessage();
		} catch ( RuntimeException e ) {
			log.debug("Error reading {} info: {}", getUnitId(), e.getMessage());
		}
		return (msg == null ? "N/A" : msg);
	}

	private String getSampleMessage(EM5600Data data) {
		if ( data.getDataTimestamp() < 1 ) {
			return "N/A";
		}
		StringBuilder buf = new StringBuilder();
		buf.append("W = ").append(sample.getPower(EM5600Data.ADDR_DATA_ACTIVE_POWER_TOTAL));
		buf.append(", VA = ").append(sample.getPower(EM5600Data.ADDR_DATA_APPARENT_POWER_TOTAL));
		buf.append(", Wh = ").append(sample.getEnergy(EM5600Data.ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT));
		buf.append(", \ud835\udf11 = ")
				.append(sample.getPowerFactor(EM5600Data.ADDR_DATA_POWER_FACTOR_TOTAL));
		buf.append("; sampled at ")
				.append(DateTimeFormat.forStyle("LS").print(new DateTime(sample.getDataTimestamp())));
		return buf.toString();
	}

	@Override
	protected Map<String, Object> readDeviceInfo(ModbusConnection conn) {
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
	public String getMeterSerialNumber(ModbusConnection conn) {
		return conn.readString(ADDR_SYSTEM_METER_SERIAL_NUMBER, 4, true, ModbusConnection.ASCII_CHARSET);
	}

	/**
	 * Read the model number of the meter. The {@code unitFactor} will also be
	 * populated if not already available.
	 * 
	 * @param conn
	 *        the connection
	 * @return the meter model number, or <em>null</em> if not available
	 */
	public Integer getMeterModel(ModbusConnection conn) {
		Integer[] data = conn.readValues(ADDR_SYSTEM_METER_MODEL, 1);
		if ( data != null && data.length > 0 ) {
			if ( unitFactor == null ) {
				switch (data[0].intValue()) {
					case 5630:
						setUnitFactor(UnitFactor.EM5630_30A);
						break;

					// TODO: how tell if EM5630_5A

					default:
						setUnitFactor(UnitFactor.EM5610);
						break;

				}
			}
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
	public LocalDateTime getMeterManufactureDate(ModbusConnection conn) {
		int[] data = conn.readInts(ADDR_SYSTEM_METER_MANUFACTURE_DATE, 2);
		return parseDate(data);
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
	public static LocalDateTime parseDate(final int[] data) {
		LocalDateTime result = null;
		if ( data != null && data.length == 2 ) {
			int day = (data[0] & 0x1F00) >> 8; // 1 - 31
			int year = 2000 + ((data[1] & 0xFF00) >> 8); // 0 - 255
			int month = (data[1] & 0xF); //1-12
			result = new LocalDateTime(year, month, day, 0, 0, 0, 0);
		}
		return result;
	}

	public boolean isCaptureTotal() {
		return (sourceMapping != null && sourceMapping.containsKey(ACPhase.Total));
	}

	public boolean isCapturePhaseA() {
		return (sourceMapping != null && sourceMapping.containsKey(ACPhase.PhaseA));
	}

	public boolean isCapturePhaseB() {
		return (sourceMapping != null && sourceMapping.containsKey(ACPhase.PhaseB));
	}

	public boolean isCapturePhaseC() {
		return (sourceMapping != null && sourceMapping.containsKey(ACPhase.PhaseC));
	}

	/**
	 * Get the source mapping.
	 * 
	 * @return the mapping
	 */
	public Map<ACPhase, String> getSourceMapping() {
		return sourceMapping;
	}

	/**
	 * Set a mapping of {@link ACPhase} to associated Source ID values to assign
	 * to collected datum.
	 * 
	 * @param sourceMapping
	 *        the source mapping; defaults to a mapping of {@code Total = Main}
	 */
	public void setSourceMapping(Map<ACPhase, String> sourceMapping) {
		this.sourceMapping = sourceMapping;
	}

	public UnitFactor getUnitFactor() {
		return unitFactor;
	}

	public void setUnitFactor(UnitFactor unitFactor) {
		assert unitFactor != null;
		this.unitFactor = unitFactor;
		this.sample.setUnitFactor(unitFactor);
	}

	/**
	 * Set the backwards setting.
	 * 
	 * @param backwards
	 *        the backwards setting
	 * @since 2.3
	 */
	public void setBackwards(boolean backwards) {
		sample.setBackwards(backwards);
	}
}
