/* ==================================================================
 * CentameterSupport.java - Sep 20, 2010 9:24:54 PM
 * 
 * Copyright 2007-2010 SolarNetwork.net Dev Team
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
 * $Revision$
 * ==================================================================
 */

package net.solarnetwork.node.centameter;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.solarnetwork.node.DataCollector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;

/**
 * Base class for reading Centameter sensor data.
 * 
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt>dataCollectorFactory</dt>
 *   <dd>The factory for creating {@link DataCollector} instances with. 
 *   {@link ObjectFactory#getObject()} will be called on each invocation
 *   of {@link #readCurrentPowerDatum()}.</dd>
 *   
 *   <dt>voltage</dt>
 *   <dd>A hard-coded voltage value to use for the Cent-a-meter, since it
 *   only measures current. Defaults to {@link #DEFAULT_VOLTAGE}.</dd>
 *   
 *   <dt>ampSensorIndex</dt>
 *   <dd>The Cent-a-meter can report on 3 different currents. This index value
 *   is the desired current to read. Possible values for this property are 1,
 *   2, or 3. Defaults to {@code 1}. This applies only when the 
 *   {@link #readCurrentDatum()} method is used, not 
 *   {@link #readMultipleDatum()}.</dd>
 *   
 *   <dt>sourceIdFormat</dt>
 *   <dd>A string format pattern for generating the {@code sourceId} value in
 *   returned {@link PowerDatum} instances. This format will be passed
 *   the Centameter address (as a <em>short</em>) and the Centameter amp sensor
 *   index (as a <em>int</em>). Defaults to {@link #DEFAULT_SOURCE_ID_FORMAT}.
 *   </dd>
 *   
 *   <dt>multiAmpSensorIndexFlags</dt>
 *   <dd>A bitmask flag for which amp sensor index readings to return from 
 *   {@link #readMultipleDatum()}. The amp sensors number 1 - 3. Enable
 *   reading each index by adding together each index as 2 ^ (index - 1).
 *   Thus to enable reading from all 3 indexes set this value to <em>7</em>
 *   (2^0 + 2^1 + 2^2) = 7). Defaults to 7.</dd>
 *   
 *   <dt>addressSourceMapping</dt>
 *   <dd>If configured, a mapping of Centameter address ID values to
 *   PowerDatum sourceId values. This can be used to consistently collect
 *   data from Centameters, even after the Centameter has been reset and
 *   it generates a new random address ID value for itself.</dd>
 *   
 *   <dt>sourceIdFilter</dt>
 *   <dd>If configured, a set of PowerDatum sourceId values to accept
 *   data for, rejecting all others. Sometimes bogus data can be received
 *   or some other Centameter not part of this node might be received.
 *   Configuring this field prevents data from sources other than those
 *   configured here from being collected. Note the source values configured
 *   here should be the values <em>after</em> any {@code addressSourceMapping}
 *   translation has occurred.</dd>
 *   
 *   <dt>collectAllSourceIds</dt>
 *   <dd>If <em>true</em> and the {@link net.solarnetwork.node.MultiDatumDataSource}
 *   API is used, then attempt to read values for all sources configured in the
 *   {@code sourceIdFilter} property and return all the data collected. The 
 *   {@code collectAllSourceIdsTimeout}
 *   property is used to limit the amount of time spent collecting data, as 
 *   there is no guarantee the application can read from all sources: the Centamter
 *   data is captured somewhat randomly. Defaults to <em>true</em>.</dd>
 *   
 *   <dt>collectAllSourceIdsTimeout</dt>
 *   <dd>When {@code collectAllSourceIds} is configured as <em>true</em> this
 *   is a timeout value, in seconds, the application should spend attempting to
 *   collect data from all configured sources. If this amount of time is passed
 *   before data for all sources has been collected, the application will give
 *   up and just return whatever data it has collected at that point. Defaults
 *   to {@link #DEFAULT_COLLECT_ALL_SOURCE_IDS_TIMEOUT}.</dd>
 * </dl>
 * 
 * @author matt
 * @version $Revision$
 */
public class CentameterSupport {

	/** The data byte index for the Centameter's address ID. */
	public static final int CENTAMETER_ADDRESS_IDX = 3;
	
	/** 
	 * The data byte index for the Centameter's amp reading, as integer 
	 * (amps * 10) value.
	 */
	public static final int CENTAMETER_AMPS_IDX = 8;
	
	/** The default value for the {@code voltage} property. */
	public static final float DEFAULT_VOLTAGE = 230.0F;
	
	/** The default value for the {@code sourceIdFormat} property. */
	public static final String DEFAULT_SOURCE_ID_FORMAT = "%X.%d";
	
	/** The default value for the {@code collectAllSourceIdsTimeout} property. */
	public static final int DEFAULT_COLLECT_ALL_SOURCE_IDS_TIMEOUT = 30;
	
	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private ObjectFactory<DataCollector> dataCollectorFactory;
	private float voltage = DEFAULT_VOLTAGE;
	private int ampSensorIndex = 1;
	private int multiAmpSensorIndexFlags = (1 | 2 | 4);
	private String sourceIdFormat = DEFAULT_SOURCE_ID_FORMAT;
	private Map<String,String> addressSourceMapping = null;
	private Set<String> sourceIdFilter = null;
	private boolean collectAllSourceIds = true;
	private int collectAllSourceIdsTimeout = DEFAULT_COLLECT_ALL_SOURCE_IDS_TIMEOUT;
	
	/**
	 * Set a {@code addressSourceMapping} Map via an encoded String value.
	 * 
	 * <p>The format of the {@code mapping} String should be:</p>
	 * 
	 * <pre>key=val[,key=val,...]</pre>
	 * 
	 * <p>Whitespace is permitted around all delimiters, and will be stripped
	 * from the keys and values.</p>
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
	 * <p>The format of the {@code filters} String should be a comma-delimited
	 * list of values. Whitespace is permitted around the commas, and will be
	 * stripped from the values.</p>
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
		
	/**
	 * @return the dataCollectorFactory
	 */
	public ObjectFactory<DataCollector> getDataCollectorFactory() {
		return dataCollectorFactory;
	}
	
	/**
	 * @param dataCollectorFactory the dataCollectorFactory to set
	 */
	public void setDataCollectorFactory(
			ObjectFactory<DataCollector> dataCollectorFactory) {
		this.dataCollectorFactory = dataCollectorFactory;
	}
	
	/**
	 * @return the voltage
	 */
	public float getVoltage() {
		return voltage;
	}
	
	/**
	 * @param voltage the voltage to set
	 */
	public void setVoltage(float voltage) {
		this.voltage = voltage;
	}
	
	/**
	 * @return the ampSensorIndex
	 */
	public int getAmpSensorIndex() {
		return ampSensorIndex;
	}
	
	/**
	 * @param ampSensorIndex the ampSensorIndex to set
	 */
	public void setAmpSensorIndex(int ampSensorIndex) {
		this.ampSensorIndex = ampSensorIndex;
	}
	
	/**
	 * @return the multiAmpSensorIndexFlags
	 */
	public int getMultiAmpSensorIndexFlags() {
		return multiAmpSensorIndexFlags;
	}
	
	/**
	 * @param multiAmpSensorIndexFlags the multiAmpSensorIndexFlags to set
	 */
	public void setMultiAmpSensorIndexFlags(int multiAmpSensorIndexFlags) {
		this.multiAmpSensorIndexFlags = multiAmpSensorIndexFlags;
	}
	
	/**
	 * @return the sourceIdFormat
	 */
	public String getSourceIdFormat() {
		return sourceIdFormat;
	}
	
	/**
	 * @param sourceIdFormat the sourceIdFormat to set
	 */
	public void setSourceIdFormat(String sourceIdFormat) {
		this.sourceIdFormat = sourceIdFormat;
	}
	
	/**
	 * @return the addressSourceMapping
	 */
	public Map<String, String> getAddressSourceMapping() {
		return addressSourceMapping;
	}
	
	/**
	 * @param addressSourceMapping the addressSourceMapping to set
	 */
	public void setAddressSourceMapping(Map<String, String> addressSourceMapping) {
		this.addressSourceMapping = addressSourceMapping;
	}
	
	/**
	 * @return the sourceIdFilter
	 */
	public Set<String> getSourceIdFilter() {
		return sourceIdFilter;
	}
	
	/**
	 * @param sourceIdFilter the sourceIdFilter to set
	 */
	public void setSourceIdFilter(Set<String> sourceIdFilter) {
		this.sourceIdFilter = sourceIdFilter;
	}

	/**
	 * @return the collectAllSourceIds
	 */
	public boolean isCollectAllSourceIds() {
		return collectAllSourceIds;
	}

	/**
	 * @param collectAllSourceIds the collectAllSourceIds to set
	 */
	public void setCollectAllSourceIds(boolean collectAllSourceIds) {
		this.collectAllSourceIds = collectAllSourceIds;
	}

	/**
	 * @return the collectAllSourceIdsTimeout
	 */
	public int getCollectAllSourceIdsTimeout() {
		return collectAllSourceIdsTimeout;
	}

	/**
	 * @param collectAllSourceIdsTimeout the collectAllSourceIdsTimeout to set
	 */
	public void setCollectAllSourceIdsTimeout(int collectAllSourceIdsTimeout) {
		this.collectAllSourceIdsTimeout = collectAllSourceIdsTimeout;
	}
	
}
