/* ==================================================================
 * CCSupport.java - Apr 23, 2013 3:30:30 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.currentcost;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import net.solarnetwork.node.DataCollector;
import net.solarnetwork.node.DataCollectorFactory;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.node.support.DataCollectorSerialPortBeanParameters;
import net.solarnetwork.node.support.SerialPortBeanParameters;
import net.solarnetwork.node.util.PrefixedMessageSource;
import net.solarnetwork.util.DynamicServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Support class for reading CurrentCost watt meter data.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>dataCollectorFactory</dt>
 * <dd>The factory for creating {@link DataCollector} instances with.</dd>
 * 
 * <dt>serialParams</dt>
 * <dd>The serial port parameters to use.</dd>
 * 
 * <dt>voltage</dt>
 * <dd>A hard-coded voltage value to use for the device, since it only measures
 * current. Defaults to {@link #DEFAULT_VOLTAGE}.</dd>
 * 
 * <dt>ampSensorIndex</dt>
 * <dd>The device can report on 3 different currents. This index value is the
 * desired current to read. Possible values for this property are 1, 2, or 3.
 * Defaults to {@code 1}.</dd>
 * 
 * <dt>sourceIdFormat</dt>
 * <dd>A string format pattern for generating the {@code sourceId} value in
 * returned Datum instances. This format will be passed the device address (as a
 * <em>string</em>) and the device sensor index (as an <em>integer</em>).
 * Defaults to {@link #DEFAULT_SOURCE_ID_FORMAT}.</dd>
 * 
 * <dt>multiAmpSensorIndexFlags</dt>
 * <dd>A bitmask flag for which amp sensor index readings to return from
 * {@link #readMultipleDatum()}. The amp sensors number 1 - 3. Enable reading
 * each index by adding together each index as 2 ^ (index - 1). Thus to enable
 * reading from all 3 indexes set this value to <em>7</em> (2^0 + 2^1 + 2^2) =
 * 7). Defaults to 7.</dd>
 * 
 * <dt>addressSourceMapping</dt>
 * <dd>If configured, a mapping of device address ID values to Datum sourceId
 * values. This can be used to consistently collect data from devices, even
 * after the device has been reset and it generates a new random address ID
 * value for itself.</dd>
 * 
 * <dt>sourceIdFilter</dt>
 * <dd>If configured, a set of PowerDatum sourceId values to accept data for,
 * rejecting all others. Sometimes bogus data can be received or some other
 * device not part of this node might be received. Configuring this field
 * prevents data from sources other than those configured here from being
 * collected. Note the source values configured here should be the values
 * <em>after</em> any {@code addressSourceMapping} translation has occurred.</dd>
 * 
 * <dt>collectAllSourceIds</dt>
 * <dd>If <em>true</em> and the
 * {@link net.solarnetwork.node.MultiDatumDataSource} API is used, then attempt
 * to read values for all sources configured in the {@code sourceIdFilter}
 * property and return all the data collected. The
 * {@code collectAllSourceIdsTimeout} property is used to limit the amount of
 * time spent collecting data, as there is no guarantee the application can read
 * from all sources: the device data is captured somewhat randomly. Defaults to
 * <em>true</em>.</dd>
 * 
 * <dt>collectAllSourceIdsTimeout</dt>
 * <dd>When {@code collectAllSourceIds} is configured as <em>true</em> this is a
 * timeout value, in seconds, the application should spend attempting to collect
 * data from all configured sources. If this amount of time is passed before
 * data for all sources has been collected, the application will give up and
 * just return whatever data it has collected at that point. Defaults to
 * {@link #DEFAULT_COLLECT_ALL_SOURCE_IDS_TIMEOUT}.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.0
 */
public class CCSupport {

	/** The data byte index for the device's address ID. */
	public static final int DEVICE_ADDRESS_IDX = 2;

	/** The default value for the {@code voltage} property. */
	public static final float DEFAULT_VOLTAGE = 240.0F;

	/** The default value for the {@code sourceIdFormat} property. */
	public static final String DEFAULT_SOURCE_ID_FORMAT = "%s.%d";

	/** The default value for the {@code collectAllSourceIdsTimeout} property. */
	public static final int DEFAULT_COLLECT_ALL_SOURCE_IDS_TIMEOUT = 30;

	/** The default value for the {@code multiAmpSensorIndexFlags} property. */
	public static final int DEFAULT_MULTI_AMP_SENSOR_INDEX_FLAGS = (1 | 2 | 4);

	/** The default value for the {@code ampSensorIndex} property. */
	public static final int DEFAULT_AMP_SENSOR_INDEX = 1;

	private static MessageSource MESSAGE_SOURCE;

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/** A CCMessageParser instance. */
	protected final CCMessageParser messageParser = new CCMessageParser();

	private final SortedSet<CCDatum> knownAddresses = new ConcurrentSkipListSet<CCDatum>();

	private DynamicServiceTracker<DataCollectorFactory<DataCollectorSerialPortBeanParameters>> dataCollectorFactory;
	private DataCollectorSerialPortBeanParameters serialParams = getDefaultSerialParams();

	private float voltage = DEFAULT_VOLTAGE;
	private int ampSensorIndex = DEFAULT_AMP_SENSOR_INDEX;
	private int multiAmpSensorIndexFlags = DEFAULT_MULTI_AMP_SENSOR_INDEX_FLAGS;
	private String sourceIdFormat = DEFAULT_SOURCE_ID_FORMAT;
	private Map<String, String> addressSourceMapping = null;
	private Set<String> sourceIdFilter = null;
	private boolean collectAllSourceIds = true;
	private int collectAllSourceIdsTimeout = DEFAULT_COLLECT_ALL_SOURCE_IDS_TIMEOUT;

	protected static final DataCollectorSerialPortBeanParameters getDefaultSerialParams() {
		DataCollectorSerialPortBeanParameters defaults = new DataCollectorSerialPortBeanParameters();
		try {
			defaults.setMagic("<msg>".getBytes("US-ASCII"));
			defaults.setMagicEOF("</msg>".getBytes("US-ASCII"));
		} catch ( UnsupportedEncodingException e ) {
			// should never get here
		}
		defaults.setBaud(9600);
		defaults.setBufferSize(2048);
		defaults.setReceiveThreshold(4);
		defaults.setMaxWait(15000);
		defaults.setToggleDtr(true);
		defaults.setToggleRts(false);
		return defaults;
	}

	/**
	 * Add a new cached "known" address value.
	 * 
	 * <p>
	 * This adds the address to the cached set of <em>known</em> addresses,
	 * which are shown as a read-only setting property to aid in mapping the
	 * right device address.
	 * </p>
	 * 
	 * @param datum
	 *        the datum to add
	 */
	protected void addKnownAddress(CCDatum datum) {
		knownAddresses.add(datum);
	}

	/**
	 * Get a read-only set of known addresses.
	 * 
	 * <p>
	 * This will contain all the addresses previously passed to
	 * {@link #addKnownAddress(String)} and that have not been removed via
	 * {@link #clearKnownAddresses(Collection)}.
	 * </p>
	 * 
	 * @return a read-only set of known addresses
	 */
	protected SortedSet<CCDatum> getKnownAddresses() {
		return Collections.unmodifiableSortedSet(knownAddresses);
	}

	/**
	 * Remove known address values from the known address cache.
	 * 
	 * <p>
	 * You can clear out the entire cache by passing in the result of
	 * {@link #getKnownAddresses()}.
	 * </p>
	 * 
	 * @param toRemove
	 *        the collection of addresses to remove
	 */
	protected void clearKnownAddresses(Collection<CCDatum> toRemove) {
		knownAddresses.removeAll(toRemove);
	}

	/**
	 * Set a {@code addressSourceMapping} Map via an encoded String value.
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
	 */
	public void setAddressSourceMappingValue(String mapping) {
		if ( mapping == null || mapping.length() < 1 ) {
			setAddressSourceMapping(null);
			return;
		}
		String[] pairs = mapping.split("\\s*,\\s*");
		Map<String, String> map = new LinkedHashMap<String, String>();
		for ( String pair : pairs ) {
			String[] kv = pair.split("\\s*=\\s*");
			if ( kv == null || kv.length != 2 ) {
				continue;
			}
			map.put(kv[0], kv[1]);
		}
		setAddressSourceMapping(map);
	}

	/**
	 * Set a {@link sourceIdFilter} List via an encoded String value.
	 * 
	 * <p>
	 * The format of the {@code filters} String should be a comma-delimited list
	 * of values. Whitespace is permitted around the commas, and will be
	 * stripped from the values.
	 * </p>
	 * 
	 * @param filters
	 */
	public void setSourceIdFilterValue(String filters) {
		if ( filters == null || filters.length() < 1 ) {
			setSourceIdFilter(null);
			return;
		}
		String[] data = filters.split("\\s*,\\s*");
		Set<String> s = new LinkedHashSet<String>(data.length);
		for ( String d : data ) {
			s.add(d);
		}
		setSourceIdFilter(s);
	}

	public List<SettingSpecifier> getDefaultSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(20);
		StringBuilder status = new StringBuilder();
		for ( CCDatum datum : knownAddresses ) {
			if ( status.length() > 0 ) {
				status.append(",\n");
			}
			status.append(datum.getStatusMessage());
		}
		results.add(new BasicTitleSettingSpecifier("knownAddresses", status.toString(), true));
		results.add(new BasicTextFieldSettingSpecifier("dataCollectorFactory.propertyFilters['UID']",
				"/dev/ttyUSB0"));
		results.add(new BasicTextFieldSettingSpecifier("voltage", String.valueOf(DEFAULT_VOLTAGE)));
		results.add(new BasicTextFieldSettingSpecifier("multiAmpSensorIndexFlags", String
				.valueOf(DEFAULT_MULTI_AMP_SENSOR_INDEX_FLAGS)));
		results.add(new BasicTextFieldSettingSpecifier("sourceIdFormat", DEFAULT_SOURCE_ID_FORMAT));
		results.add(new BasicTextFieldSettingSpecifier("addressSourceMappingValue", ""));
		results.add(new BasicTextFieldSettingSpecifier("sourceIdFilterValue", ""));
		results.add(new BasicToggleSettingSpecifier("collectAllSourceIds", Boolean.TRUE));
		results.add(new BasicTextFieldSettingSpecifier("collectAllSourceIdsTimeout", String
				.valueOf(DEFAULT_COLLECT_ALL_SOURCE_IDS_TIMEOUT)));

		// we don't need to provide magic byte configuration: we know what that is, so don't
		// bother exposing that here
		List<SettingSpecifier> serialSpecs = SerialPortBeanParameters.getDefaultSettingSpecifiers(
				getDefaultSerialParams(), "serialParams.");
		results.addAll(serialSpecs);
		return results;
	}

	public synchronized MessageSource getDefaultSettingsMessageSource() {
		if ( MESSAGE_SOURCE == null ) {
			ResourceBundleMessageSource serial = new ResourceBundleMessageSource();
			serial.setBundleClassLoader(SerialPortBeanParameters.class.getClassLoader());
			serial.setBasenames(new String[] { SerialPortBeanParameters.class.getName(),
					DataCollectorSerialPortBeanParameters.class.getName() });

			PrefixedMessageSource serialSource = new PrefixedMessageSource();
			serialSource.setDelegate(serial);
			serialSource.setPrefix("serialParams.");

			ResourceBundleMessageSource source = new ResourceBundleMessageSource();
			source.setBundleClassLoader(CCSupport.class.getClassLoader());
			source.setBasename(CCSupport.class.getName());
			source.setParentMessageSource(serialSource);
			MESSAGE_SOURCE = source;
		}
		return MESSAGE_SOURCE;
	}

	public DynamicServiceTracker<DataCollectorFactory<DataCollectorSerialPortBeanParameters>> getDataCollectorFactory() {
		return dataCollectorFactory;
	}

	public void setDataCollectorFactory(
			DynamicServiceTracker<DataCollectorFactory<DataCollectorSerialPortBeanParameters>> dataCollectorFactory) {
		this.dataCollectorFactory = dataCollectorFactory;
	}

	public DataCollectorSerialPortBeanParameters getSerialParams() {
		return serialParams;
	}

	public void setSerialParams(DataCollectorSerialPortBeanParameters serialParams) {
		this.serialParams = serialParams;
	}

	public float getVoltage() {
		return voltage;
	}

	public void setVoltage(float voltage) {
		this.voltage = voltage;
	}

	public int getAmpSensorIndex() {
		return ampSensorIndex;
	}

	public void setAmpSensorIndex(int ampSensorIndex) {
		this.ampSensorIndex = ampSensorIndex;
	}

	public int getMultiAmpSensorIndexFlags() {
		return multiAmpSensorIndexFlags;
	}

	public void setMultiAmpSensorIndexFlags(int multiAmpSensorIndexFlags) {
		this.multiAmpSensorIndexFlags = multiAmpSensorIndexFlags;
	}

	public String getSourceIdFormat() {
		return sourceIdFormat;
	}

	public void setSourceIdFormat(String sourceIdFormat) {
		this.sourceIdFormat = sourceIdFormat;
	}

	public Map<String, String> getAddressSourceMapping() {
		return addressSourceMapping;
	}

	public void setAddressSourceMapping(Map<String, String> addressSourceMapping) {
		this.addressSourceMapping = addressSourceMapping;
	}

	public Set<String> getSourceIdFilter() {
		return sourceIdFilter;
	}

	public void setSourceIdFilter(Set<String> sourceIdFilter) {
		this.sourceIdFilter = sourceIdFilter;
	}

	public boolean isCollectAllSourceIds() {
		return collectAllSourceIds;
	}

	public void setCollectAllSourceIds(boolean collectAllSourceIds) {
		this.collectAllSourceIds = collectAllSourceIds;
	}

	public int getCollectAllSourceIdsTimeout() {
		return collectAllSourceIdsTimeout;
	}

	public void setCollectAllSourceIdsTimeout(int collectAllSourceIdsTimeout) {
		this.collectAllSourceIdsTimeout = collectAllSourceIdsTimeout;
	}

}
