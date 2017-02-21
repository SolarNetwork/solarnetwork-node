/* ==================================================================
 * SDMSupport.java - 26/01/2016 7:21:53 am
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.deson.meter;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.domain.ACPhase;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusDeviceSupport;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicRadioGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.node.util.ClassUtils;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.StringUtils;

/**
 * Supporting class for the SDM series power meters.
 * 
 * @author matt
 * @version 1.1
 */
public class SDMSupport extends ModbusDeviceSupport {

	/** The default source ID applied for the total reading values. */
	public static final String MAIN_SOURCE_ID = "Main";

	// a mapping of AC phase to source ID
	private Map<ACPhase, String> sourceMapping = getDefaulSourceMapping();

	// an optional EventAdmin service
	private OptionalService<EventAdmin> eventAdmin;

	// the type of device to use
	private SDMDeviceType deviceType = SDMDeviceType.SDM120;

	// the "installed backwards" setting
	private boolean backwards = false;

	/**
	 * An instance of {@link SDMData} to support keeping the last-read values of
	 * data in memory.
	 */
	protected SDMData sample = new SDM120Data();

	/**
	 * Get a default {@code sourceMapping} value. This maps only the
	 * {@code Total} phase to the value {@code Main}.
	 * 
	 * @return mapping
	 */
	public static Map<ACPhase, String> getDefaulSourceMapping() {
		Map<ACPhase, String> result = new EnumMap<ACPhase, String>(ACPhase.class);
		result.put(ACPhase.Total, MAIN_SOURCE_ID);
		return result;
	}

	@Override
	protected Map<String, Object> readDeviceInfo(ModbusConnection conn) {
		sample.readControlData(conn);
		return sample.getDeviceInfo();
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

	private String getSampleMessage(SDMData data) {
		if ( data.getMeterDataTimestamp() < 1 ) {
			return "N/A";
		}
		StringBuilder buf = new StringBuilder();
		buf.append(data.getOperationStatusMessage());
		buf.append("; sampled at ").append(
				DateTimeFormat.forStyle("LS").print(new DateTime(sample.getMeterDataTimestamp())));
		return buf.toString();
	}

	public List<SettingSpecifier> getSettingSpecifiers() {
		SDMSupport defaults = new SDMSupport();
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(10);

		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(), true));
		results.add(new BasicTitleSettingSpecifier("sample", getSampleMessage(sample), true));

		results.add(new BasicTextFieldSettingSpecifier("uid", defaults.getUid()));
		results.add(new BasicTextFieldSettingSpecifier("groupUID", defaults.getGroupUID()));
		results.add(new BasicTextFieldSettingSpecifier("modbusNetwork.propertyFilters['UID']",
				"Modbus Port"));
		results.add(new BasicTextFieldSettingSpecifier("unitId", String.valueOf(defaults.getUnitId())));

		BasicRadioGroupSettingSpecifier deviceTypeSpec = new BasicRadioGroupSettingSpecifier(
				"deviceTypeValue", defaults.getDeviceTypeValue());
		Map<String, String> deviceTypeValues = new LinkedHashMap<String, String>(3);
		for ( SDMDeviceType model : SDMDeviceType.values() ) {
			deviceTypeValues.put(model.toString(), model.toString());
		}
		deviceTypeSpec.setValueTitles(deviceTypeValues);
		results.add(deviceTypeSpec);

		results.add(new BasicToggleSettingSpecifier("backwards", Boolean.valueOf(defaults.backwards)));

		results.add(new BasicTextFieldSettingSpecifier("sourceMappingValue",
				defaults.getSourceMappingValue()));

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

	/**
	 * Test if the {@code Total} phase should be captured.
	 * 
	 * @return <em>true</em> if the {@code sourceMapping} contains a
	 *         {@code Total} key
	 */
	public boolean isCaptureTotal() {
		return (sourceMapping != null && sourceMapping.containsKey(ACPhase.Total));
	}

	/**
	 * Test if the {@code PhaseA} phase should be captured.
	 * 
	 * @return <em>true</em> if the {@code sourceMapping} contains a
	 *         {@code PhaseA} key
	 */
	public boolean isCapturePhaseA() {
		return (sourceMapping != null && sourceMapping.containsKey(ACPhase.PhaseA));
	}

	/**
	 * Test if the {@code PhaseB} phase should be captured.
	 * 
	 * @return <em>true</em> if the {@code sourceMapping} contains a
	 *         {@code PhaseB} key
	 */
	public boolean isCapturePhaseB() {
		return (sourceMapping != null && sourceMapping.containsKey(ACPhase.PhaseB));
	}

	/**
	 * Test if the {@code PhaseC} phase should be captured.
	 * 
	 * @return <em>true</em> if the {@code sourceMapping} contains a
	 *         {@code PhaseC} key
	 */
	public boolean isCapturePhaseC() {
		return (sourceMapping != null && sourceMapping.containsKey(ACPhase.PhaseC));
	}

	/**
	 * Get the configured mapping from AC phase constants to source ID values.
	 * 
	 * @return The source mapping.
	 */
	public Map<ACPhase, String> getSourceMapping() {
		return sourceMapping;
	}

	/**
	 * Configure a mapping from AC phase constants to source ID values.
	 * 
	 * @param sourceMapping
	 *        The source mappinng to set.
	 */
	public void setSourceMapping(Map<ACPhase, String> sourceMapping) {
		this.sourceMapping = sourceMapping;
	}

	/**
	 * Get the configured optional EventAdmin service.
	 * 
	 * @return The service.
	 */
	public OptionalService<EventAdmin> getEventAdmin() {
		return eventAdmin;
	}

	/**
	 * Set an optional {@code EventAdmin} service for posting events with.
	 * 
	 * @param eventAdmin
	 *        The service to use.
	 */
	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	public SDMDeviceType getDeviceType() {
		return deviceType;
	}

	/**
	 * Set the type of device to use. If this value changes, any cached sample
	 * data will be cleared.
	 * 
	 * @param deviceType
	 *        The type of device to use.
	 */
	public void setDeviceType(final SDMDeviceType deviceType) {
		if ( deviceType == null ) {
			throw new IllegalArgumentException("The deviceType cannot be null.");
		}
		if ( this.deviceType.equals(deviceType) ) {
			// no change
			return;
		}
		this.deviceType = deviceType;
		setupNewSample(deviceType);
	}

	private void setupNewSample(final SDMDeviceType deviceType) {
		switch (deviceType) {
			case SDM630:
				sample = new SDM630Data(backwards);
				break;

			default:
				sample = new SDM120Data(backwards);
				break;
		}
	}

	/**
	 * Get the device type, as a string.
	 * 
	 * @return The device type, as a string.
	 */
	public String getDeviceTypeValue() {
		final SDMDeviceType type = getDeviceType();
		return (type == null ? "" : type.toString());
	}

	/**
	 * Set the device type, as a string.
	 * 
	 * @param type
	 *        The {@link SDMDeviceType} string value to set.
	 */
	public void setDeviceTypeValue(String type) {
		try {
			setDeviceType(SDMDeviceType.valueOf(type));
		} catch ( IllegalArgumentException e ) {
			// not supported type
		}
	}

	/**
	 * Set the backwards setting.
	 * 
	 * @param backwards
	 *        the backwards setting
	 * @since 1.1
	 */
	public void setBackwards(boolean value) {
		if ( value == backwards ) {
			return;
		}
		this.backwards = value;
		setupNewSample(this.deviceType);
	}

}
