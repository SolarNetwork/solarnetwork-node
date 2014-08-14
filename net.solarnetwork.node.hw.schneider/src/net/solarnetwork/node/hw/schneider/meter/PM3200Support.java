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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusDeviceSupport;
import net.solarnetwork.node.io.modbus.ModbusHelper;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.util.ClassUtils;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * Supporting class for the PM3200 series power meter.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>sourceMapping</dt>
 * <dd>A mapping of {@link ACPhase} to associated Source ID values to assign to
 * collected datum. Defaults to a mapping of {@code Total = Main}.</dd>
 * 
 * <dt>eventAdmin</dt>
 * <dd>An optional {@link EventAdmin} service to use for posting events.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.4
 */
public class PM3200Support extends ModbusDeviceSupport {

	public static final Integer ADDR_SYSTEM_METER_NAME = 29;
	public static final Integer ADDR_SYSTEM_METER_MODEL = 49;
	public static final Integer ADDR_SYSTEM_METER_MANUFACTURER = 69;
	public static final Integer ADDR_SYSTEM_METER_SERIAL_NUMBER = 129;
	public static final Integer ADDR_SYSTEM_METER_MANUFACTURE_DATE = 131;

	/** The default source ID applied for the total reading values. */
	public static final String MAIN_SOURCE_ID = "Main";

	private Map<ACPhase, String> sourceMapping = getDefaulSourceMapping();

	private OptionalService<EventAdmin> eventAdmin;

	/**
	 * An instance of {@link PM3200Data} to support keeping the last-read values
	 * of data in memory.
	 */
	protected final PM3200Data sample = new PM3200Data();

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
	 * Read the name of the meter.
	 * 
	 * @param conn
	 *        the serial connection
	 * @return the meter name, or <em>null</em> if not available
	 */
	public String getMeterName(ModbusConnection conn) {
		return conn.readString(ADDR_SYSTEM_METER_NAME, 20, true, ModbusConnection.UTF8_CHARSET);
	}

	/**
	 * Read the model of the meter.
	 * 
	 * @param conn
	 *        the serial connection
	 * @return the meter model, or <em>null</em> if not available
	 */
	public String getMeterModel(ModbusConnection conn) {
		return conn.readString(ADDR_SYSTEM_METER_MODEL, 20, true, ModbusConnection.UTF8_CHARSET);
	}

	/**
	 * Read the manufacturer of the meter.
	 * 
	 * @param conn
	 *        the serial connection
	 * @return the meter manufacturer, or <em>null</em> if not available
	 */
	public String getMeterManufacturer(ModbusConnection conn) {
		return conn.readString(ADDR_SYSTEM_METER_MANUFACTURER, 20, true, ModbusConnection.UTF8_CHARSET);
	}

	/**
	 * Read the serial number of the meter.
	 * 
	 * @param conn
	 *        the connection
	 * @return the meter serial number, or <em>null</em> if not available
	 */
	public Long getMeterSerialNumber(ModbusConnection conn) {
		Long result = null;
		Integer[] data = conn.readValues(ADDR_SYSTEM_METER_SERIAL_NUMBER, 2);
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
	public LocalDateTime getMeterManufactureDate(ModbusConnection conn) {
		int[] data = conn.readInts(ADDR_SYSTEM_METER_MANUFACTURE_DATE, 4);
		return parseDateTime(data);
	}

	@Override
	protected Map<String, Object> readDeviceInfo(ModbusConnection conn) {
		Map<String, Object> result = new LinkedHashMap<String, Object>(8);
		String str = getMeterName(conn);
		if ( str != null ) {
			result.put(INFO_KEY_DEVICE_NAME, str);
		}
		str = getMeterModel(conn);
		if ( str != null ) {
			result.put(INFO_KEY_DEVICE_MODEL, str);
		}
		str = getMeterManufacturer(conn);
		if ( result != null ) {
			result.put(INFO_KEY_DEVICE_MANUFACTURER, str);
		}
		LocalDateTime dt = getMeterManufactureDate(conn);
		if ( dt != null ) {
			result.put(INFO_KEY_DEVICE_MANUFACTURE_DATE, dt.toLocalDate());
		}
		Long l = getMeterSerialNumber(conn);
		if ( l != null ) {
			result.put(INFO_KEY_DEVICE_SERIAL_NUMBER, l);
		}
		return result;
	}

	/**
	 * Parse a DateTime value from raw Modbus register values. The {@code data}
	 * array is expected to have a length of {@code 4}.
	 * 
	 * @param data
	 *        the data array
	 * @return the parsed date, or <em>null</em> if not available
	 */
	public static LocalDateTime parseDateTime(final int[] data) {
		LocalDateTime result = null;
		if ( data != null && data.length == 4 ) {
			int year = 2000 + (data[0] & 0x7F);
			int month = (data[1] & 0xF00) >> 8;
			int day = (data[1] & 0x1F);
			int hour = (data[2] & 0x1F00) >> 8;
			int minute = (data[2] & 0x3F);
			int ms = (data[3]); // this is really seconds + milliseconds
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
		return parseBigEndianFloat32(data, offset);
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
		return parseBigEndianInt64(data, offset);
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

	private String getInfoMessage() {
		String msg = null;
		try {
			msg = getDeviceInfoMessage();
		} catch ( RuntimeException e ) {
			log.debug("Error reading info: {}", e.getMessage());
		}
		return (msg == null ? "N/A" : msg);
	}

	private String getSampleMessage(PM3200Data data) {
		if ( data.getDataTimestamp() < 1 ) {
			return "N/A";
		}
		StringBuilder buf = new StringBuilder();
		buf.append("W = ").append(sample.getPower(PM3200Data.ADDR_DATA_ACTIVE_POWER_TOTAL));
		buf.append(", VA = ").append(sample.getPower(PM3200Data.ADDR_DATA_APPARENT_POWER_TOTAL));
		buf.append(", Wh = ").append(sample.getEnergy(PM3200Data.ADDR_DATA_TOTAL_ACTIVE_ENERGY_IMPORT));
		buf.append(", cos \ud835\udf11 = ").append(sample.getEffectiveTotalPowerFactor());
		buf.append("; sampled at ").append(
				DateTimeFormat.forStyle("LS").print(new DateTime(sample.getDataTimestamp())));
		return buf.toString();
	}

	public List<SettingSpecifier> getSettingSpecifiers() {
		PM3200Support defaults = new PM3200Support();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(10);

		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(), true));
		results.add(new BasicTitleSettingSpecifier("sample", getSampleMessage(sample), true));

		results.add(new BasicTextFieldSettingSpecifier("uid", defaults.getUid()));
		results.add(new BasicTextFieldSettingSpecifier("groupUID", defaults.getGroupUID()));
		results.add(new BasicTextFieldSettingSpecifier("modbusNetwork.propertyFilters['UID']",
				"Serial Port"));
		results.add(new BasicTextFieldSettingSpecifier("unitId", String.valueOf(defaults.getUnitId())));
		results.add(new BasicTextFieldSettingSpecifier("sourceMappingValue", defaults
				.getSourceMappingValue()));

		return results;
	}

	/**
	 * Post a {@link DatumDataSource#EVENT_TOPIC_DATUM_CAPTURED} {@link Event}.
	 * 
	 * <p>
	 * This method calls {@link #createDatumCapturedEvent(Datum, Class)} to
	 * create the actual Event, which may be overridden by extending classes.
	 * </p>
	 * 
	 * @param datum
	 *        the {@link Datum} to post the event for
	 * @param eventDatumType
	 *        the Datum class to use for the
	 *        {@link DatumDataSource#EVENT_DATUM_CAPTURED_DATUM_TYPE} property
	 * @since 1.3
	 */
	protected final void postDatumCapturedEvent(final Datum datum,
			final Class<? extends Datum> eventDatumType) {
		EventAdmin ea = (eventAdmin == null ? null : eventAdmin.service());
		if ( ea == null || datum == null ) {
			return;
		}
		Event event = createDatumCapturedEvent(datum, eventDatumType);
		ea.postEvent(event);
	}

	/**
	 * Create a new {@link DatumDataSource#EVENT_TOPIC_DATUM_CAPTURED}
	 * {@link Event} object out of a {@link Datum}.
	 * 
	 * <p>
	 * This method will populate all simple properties of the given
	 * {@link Datum} into the event properties, along with the
	 * {@link DatumDataSource#EVENT_DATUM_CAPTURED_DATUM_TYPE}.
	 * 
	 * @param datum
	 *        the datum to create the event for
	 * @param eventDatumType
	 *        the Datum class to use for the
	 *        {@link DatumDataSource#EVENT_DATUM_CAPTURED_DATUM_TYPE} property
	 * @return the new Event instance
	 * @since 1.3
	 */
	protected Event createDatumCapturedEvent(final Datum datum,
			final Class<? extends Datum> eventDatumType) {
		Map<String, Object> props = ClassUtils.getSimpleBeanProperties(datum, null);
		props.put(DatumDataSource.EVENT_DATUM_CAPTURED_DATUM_TYPE, eventDatumType.getName());
		log.debug("Created {} event with props {}", DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED, props);
		return new Event(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED, props);
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

	public Map<ACPhase, String> getSourceMapping() {
		return sourceMapping;
	}

	public void setSourceMapping(Map<ACPhase, String> sourceMapping) {
		this.sourceMapping = sourceMapping;
	}

	public OptionalService<EventAdmin> getEventAdmin() {
		return eventAdmin;
	}

	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

}
