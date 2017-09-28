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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.io.serial.SerialDeviceDatumDataSourceSupport;
import net.solarnetwork.node.io.serial.SerialNetwork;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;

/**
 * Support class for reading CurrentCost watt meter data from a serial
 * connection.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>serialNetwork</dt>
 * <dd>The {@link SerialNetwork} to use.</dd>
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
 * <em>after</em> any {@code addressSourceMapping} translation has
 * occurred.</dd>
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
 * @version 2.2
 */
public class CCSupport extends SerialDeviceDatumDataSourceSupport {

	/** The data byte index for the device's address ID. */
	public static final int DEVICE_ADDRESS_IDX = 2;

	/** The default value for the {@code voltage} property. */
	public static final float DEFAULT_VOLTAGE = 240.0F;

	/** The default value for the {@code sourceIdFormat} property. */
	public static final String DEFAULT_SOURCE_ID_FORMAT = "%s.%d";

	/**
	 * The default value for the {@code collectAllSourceIdsTimeout} property.
	 */
	public static final int DEFAULT_COLLECT_ALL_SOURCE_IDS_TIMEOUT = 30;

	/** The default value for the {@code multiAmpSensorIndexFlags} property. */
	public static final int DEFAULT_MULTI_AMP_SENSOR_INDEX_FLAGS = (1 | 2 | 4);

	/** The default value for the {@code ampSensorIndex} property. */
	public static final int DEFAULT_AMP_SENSOR_INDEX = 1;

	/** The starting message marker, which is the opening XML element. */
	public static final String MESSAGE_START_MARKER = "<msg>";

	/** The ending message marker, which is the closing XML element. */
	public static final String MESSAGE_END_MARKER = "</msg>";

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/** A CCMessageParser instance. */
	protected final CCMessageParser messageParser = new CCMessageParser();

	private final SortedSet<CCDatum> knownAddresses = new ConcurrentSkipListSet<CCDatum>();

	private float voltage = DEFAULT_VOLTAGE;
	private int ampSensorIndex = DEFAULT_AMP_SENSOR_INDEX;
	private int multiAmpSensorIndexFlags = DEFAULT_MULTI_AMP_SENSOR_INDEX_FLAGS;
	private String sourceIdFormat = DEFAULT_SOURCE_ID_FORMAT;
	private Map<String, String> addressSourceMapping = null;
	private Set<String> sourceIdFilter = null;
	private boolean collectAllSourceIds = true;
	private int collectAllSourceIdsTimeout = DEFAULT_COLLECT_ALL_SOURCE_IDS_TIMEOUT;
	private long sampleCacheMs = 5000;

	/**
	 * Add a new cached "known" address value.
	 * 
	 * <p>
	 * This adds the address to the cached set of <em>known</em> addresses,
	 * which are shown as a read-only setting property to aid in mapping the
	 * right device address, as long as the {@link CCDatum#getDeviceAddress()}
	 * value is not <em>null</em>.
	 * </p>
	 * 
	 * @param datum
	 *        the datum to add
	 */
	protected void addKnownAddress(CCDatum datum) {
		if ( datum != null && datum.getDeviceAddress() != null ) {
			knownAddresses.remove(datum); // remove old copy, if present
			knownAddresses.add(datum);
		}
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
	 * Get an address value for a given sample and sensor index.
	 * 
	 * @param datum
	 *        the sample data
	 * @param ampIndex
	 *        the sensor index
	 * @return the address value to use
	 */
	protected String addressValue(CCDatum datum, int ampIndex) {
		return String.format(sourceIdFormat, datum.getDeviceAddress(), ampIndex);
	}

	/**
	 * Return all cached data from the {@code knownAddresses} Map whose address
	 * is currently configured in the {@link #getAddressSourceMapping()} map.
	 * The data is only returned if it is not older than
	 * {@link #getSampleCacheMs()} (if that is configured as anything greater
	 * than zero).
	 * 
	 * @return set of cached {@link CCDatum}, or an empty Set if none available
	 */
	protected Set<CCDatum> allCachedDataForConfiguredAddresses() {
		Set<String> captureAddresses = (addressSourceMapping == null ? null
				: addressSourceMapping.keySet());
		if ( captureAddresses == null ) {
			return Collections.emptySet();
		}
		Set<CCDatum> result = new HashSet<CCDatum>(4);
		final long now = System.currentTimeMillis();
		for ( CCDatum datum : knownAddresses ) {
			for ( int i = 1; i <= 3; i++ ) {
				String anAddress = addressValue(datum, i);
				if ( captureAddresses.contains(anAddress) ) {
					if ( sampleCacheMs < 1 || (now - datum.getCreated()) <= sampleCacheMs ) {
						result.add(datum);
						break;
					}
				}
			}
		}
		if ( log.isDebugEnabled() && result.size() > 0 ) {
			log.debug("Returning cached CCDatum samples: {}", result);
		}
		return result;
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
		CCSupport defaults = new CCSupport();
		results.add(new BasicTitleSettingSpecifier("knownAddresses", status.toString(), true));
		results.addAll(getIdentifiableSettingSpecifiers());
		results.add(new BasicTextFieldSettingSpecifier("serialNetwork.propertyFilters['UID']",
				"Serial Port"));
		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(defaults.getSampleCacheMs())));
		results.add(new BasicTextFieldSettingSpecifier("voltage", String.valueOf(DEFAULT_VOLTAGE)));

		results.add(new BasicToggleSettingSpecifier("multiCollectSensor1",
				defaults.isMultiCollectSensor1()));
		results.add(new BasicToggleSettingSpecifier("multiCollectSensor2",
				defaults.isMultiCollectSensor2()));
		results.add(new BasicToggleSettingSpecifier("multiCollectSensor3",
				defaults.isMultiCollectSensor3()));

		results.add(new BasicTextFieldSettingSpecifier("sourceIdFormat", DEFAULT_SOURCE_ID_FORMAT));
		results.add(new BasicTextFieldSettingSpecifier("addressSourceMappingValue", ""));
		results.add(new BasicTextFieldSettingSpecifier("sourceIdFilterValue", ""));
		results.add(new BasicToggleSettingSpecifier("collectAllSourceIds", Boolean.TRUE));
		results.add(new BasicTextFieldSettingSpecifier("collectAllSourceIdsTimeout",
				String.valueOf(DEFAULT_COLLECT_ALL_SOURCE_IDS_TIMEOUT)));

		return results;
	}

	/**
	 * Returns an empty Map. Extending classes can override as appropriate.
	 * 
	 * @param conn
	 *        the serial connection
	 * @return empty map
	 */
	@Override
	protected Map<String, Object> readDeviceInfo(SerialConnection conn) {
		return Collections.emptyMap();
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

	/**
	 * Get the bitmask flag of amp sensor index values to return when requesting
	 * multiple datum samples.
	 * 
	 * @return The current bitmask value.
	 */
	public int getMultiAmpSensorIndexFlags() {
		return multiAmpSensorIndexFlags;
	}

	/**
	 * Set the bitmask flag for which amp sensor index readings to return when
	 * requesting multiple datum samples.
	 * 
	 * The amp sensors number 1 - 3. Enable reading each index by adding
	 * together each index as 2 ^ (index - 1). Thus to enable reading from all 3
	 * indexes set this value to <em>7</em> (2^0 + 2^1 + 2^2) = 7). Defaults to
	 * {@code 7}.
	 * 
	 * @param multiAmpSensorIndexFlags
	 *        The bitmask to set.
	 */
	public void setMultiAmpSensorIndexFlags(int multiAmpSensorIndexFlags) {
		this.multiAmpSensorIndexFlags = multiAmpSensorIndexFlags;
	}

	private boolean isMultiCollectSensor(int index) {
		return (this.multiAmpSensorIndexFlags & index) == index;
	}

	private void setMultiCollectSensor(int index, boolean value) {
		if ( value ) {
			this.multiAmpSensorIndexFlags |= index;
		} else {
			this.multiAmpSensorIndexFlags &= ~index;
		}
	}

	/**
	 * Test if sensor 1 should be collected when requesting multiple datum
	 * samples.
	 * 
	 * @return <em>true</em> if sensor 1 should be collected
	 * @see #getMultiAmpSensorIndexFlags()
	 * @since 2.1
	 */
	public boolean isMultiCollectSensor1() {
		return isMultiCollectSensor(1);
	}

	/**
	 * Set if sensor 1 should be collected when requesting multiple datum
	 * samples.
	 * 
	 * @param value
	 *        <em>true</em> if sensor 1 should be collected
	 * @since 2.1
	 */
	public void setMultiCollectSensor1(boolean value) {
		setMultiCollectSensor(1, value);
	}

	/**
	 * Test if sensor 2 should be collected when requesting multiple datum
	 * samples.
	 * 
	 * @return <em>true</em> if sensor 2 should be collected
	 * @see #getMultiAmpSensorIndexFlags()
	 * @since 2.1
	 */
	public boolean isMultiCollectSensor2() {
		return isMultiCollectSensor(2);
	}

	/**
	 * Set if sensor 2 should be collected when requesting multiple datum
	 * samples.
	 * 
	 * @param value
	 *        <em>true</em> if sensor 2 should be collected
	 * @since 2.1
	 */
	public void setMultiCollectSensor2(boolean value) {
		setMultiCollectSensor(2, value);
	}

	/**
	 * Test if sensor 3 should be collected when requesting multiple datum
	 * samples.
	 * 
	 * @return <em>true</em> if sensor 3 should be collected
	 * @see #getMultiAmpSensorIndexFlags()
	 * @since 2.1
	 */
	public boolean isMultiCollectSensor3() {
		return isMultiCollectSensor(3);
	}

	/**
	 * Set if sensor 3 should be collected when requesting multiple datum
	 * samples.
	 * 
	 * @param value
	 *        <em>true</em> if sensor 3 should be collected
	 * @since 2.1
	 */
	public void setMultiCollectSensor3(boolean value) {
		setMultiCollectSensor(3, value);
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

	@Override
	public String getUID() {
		return getUid();
	}

	public long getSampleCacheMs() {
		return sampleCacheMs;
	}

	public void setSampleCacheMs(long sampleCacheMs) {
		this.sampleCacheMs = sampleCacheMs;
	}

}
