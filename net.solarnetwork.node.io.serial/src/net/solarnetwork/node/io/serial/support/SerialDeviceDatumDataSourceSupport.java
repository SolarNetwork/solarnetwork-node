/* ==================================================================
 * SerialDeviceDatumDataSourceSupport.java - 26/09/2017 9:56:36 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.serial.support;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.node.io.serial.SerialConnection;
import net.solarnetwork.node.io.serial.SerialConnectionAction;
import net.solarnetwork.node.io.serial.SerialNetwork;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.support.DatumDataSourceSupport;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.util.StringUtils;

/**
 * A base helper class to support {@link SerialNetwork} based
 * {@link DatumDataSource} implementations.
 * 
 * @param <S>
 *        the sample type
 * @author matt
 * @version 2.0
 * @since 1.3
 */
public abstract class SerialDeviceDatumDataSourceSupport<S extends Datum>
		extends DatumDataSourceSupport {

	/** Key for the device name, as a String. */
	public static final String INFO_KEY_DEVICE_NAME = SerialDeviceSupport.INFO_KEY_DEVICE_NAME;

	/** Key for the device model, as a String. */
	public static final String INFO_KEY_DEVICE_MODEL = SerialDeviceSupport.INFO_KEY_DEVICE_MODEL;

	/** Key for the device serial number, as a Long. */
	public static final String INFO_KEY_DEVICE_SERIAL_NUMBER = SerialDeviceSupport.INFO_KEY_DEVICE_SERIAL_NUMBER;

	/** Key for the device manufacturer, as a String. */
	public static final String INFO_KEY_DEVICE_MANUFACTURER = SerialDeviceSupport.INFO_KEY_DEVICE_MANUFACTURER;

	/**
	 * Key for the device manufacture date, as a
	 * {@link org.joda.time.ReadablePartial}.
	 */
	public static final String INFO_KEY_DEVICE_MANUFACTURE_DATE = SerialDeviceSupport.INFO_KEY_DEVICE_MANUFACTURE_DATE;

	/** The {@code sampleCacheMs} property default value. */
	public static final long DEFAULT_SAMPLE_CACHE_MS = 5000L;

	private final AtomicReference<S> sample;

	private String sourceId;
	private long sampleCacheMs = DEFAULT_SAMPLE_CACHE_MS;
	private Map<String, Object> deviceInfo;
	private OptionalService<SerialNetwork> serialNetwork;

	/**
	 * Construct with a specific sample data instance.
	 * 
	 * @param sample
	 *        the sample data to use
	 */
	public SerialDeviceDatumDataSourceSupport() {
		this(new AtomicReference<>());
	}

	/**
	 * Construct with a specific sample data instance.
	 * 
	 * @param sample
	 *        the sample data to use
	 */
	public SerialDeviceDatumDataSourceSupport(AtomicReference<S> sample) {
		super();
		this.sample = sample;
	}

	/**
	 * Test if the sample data has expired.
	 * 
	 * @return {@literal true} if the sample data has expired
	 */
	protected boolean isCachedSampleExpired() {
		return isCachedSampleExpired(getSample());
	}

	/**
	 * Test if the sample data has expired.
	 * 
	 * @return {@literal true} if the sample data has expired
	 */
	protected boolean isCachedSampleExpired(S sample) {
		if ( sample == null || sample.getTimestamp() == null ) {
			return true;
		}
		final long diffMs = sample.getTimestamp().until(Instant.now(), ChronoUnit.MILLIS);
		if ( diffMs > sampleCacheMs ) {
			return true;
		}
		return false;
	}

	/**
	 * Get the {@link SerialNetwork} from the configured {@code serialNetwork}
	 * service, or {@literal null} if not available or not configured.
	 * 
	 * @return SerialNetwork
	 */
	protected final SerialNetwork serialNetwork() {
		return (serialNetwork == null ? null : serialNetwork.service());
	}

	/**
	 * Read general device info and return a map of the results. See the various
	 * {@code INFO_KEY_*} constants for information on the values returned in
	 * the result map.
	 * 
	 * @param conn
	 *        the connection to use
	 * @return a map with general device information populated
	 * @throws IOException
	 *         if any IO error occurrs
	 */
	protected abstract Map<String, Object> readDeviceInfo(SerialConnection conn) throws IOException;

	/**
	 * Return an informational message composed of general device info. This
	 * method will call {@link #getDeviceInfo()} and return a {@code /} (forward
	 * slash) delimited string of the resulting values, or {@literal null} if
	 * that method returns {@literal null}.
	 * 
	 * @return info message
	 */
	public String getDeviceInfoMessage() {
		Map<String, ?> info = getDeviceInfo();
		if ( info == null ) {
			return null;
		}
		return StringUtils.delimitedStringFromCollection(info.values(), " / ");
	}

	/**
	 * Get the device info data as a Map. This method will call
	 * {@link #readDeviceInfo(SerialConnection)}. The map is cached so
	 * subsequent calls will not attempt to read from the device. Note the
	 * returned map cannot be modified.
	 * 
	 * @return the device info, or {@literal null}
	 * @see #readDeviceInfo(SerialConnection)
	 */
	public Map<String, ?> getDeviceInfo() {
		Map<String, Object> info = deviceInfo;
		if ( info == null ) {
			try {
				info = performAction(new SerialConnectionAction<Map<String, Object>>() {

					@Override
					public Map<String, Object> doWithConnection(SerialConnection conn)
							throws IOException {
						return readDeviceInfo(conn);
					}
				});
				deviceInfo = info;
			} catch ( Exception e ) {
				log.warn("Communcation problem with {}: {}", getUid(), e.getMessage());
			}
		}
		return (info == null ? null : Collections.unmodifiableMap(info));
	}

	/**
	 * Perform some work with a Serial {@link SerialConnection}. This method
	 * attempts to obtain a {@link SerialNetwork} from the configured
	 * {@code serialNetwork} service, calling
	 * {@link SerialNetwork#performAction(SerialConnectionAction)} if one can be
	 * obtained.
	 * 
	 * @param <T>
	 *        the action result type
	 * @param action
	 *        the connection action
	 * @return the result of the callback, or {@literal null} if the action is
	 *         never invoked
	 * @throws IOException
	 *         if any IO error occurs
	 */
	protected final <T> T performAction(final SerialConnectionAction<T> action) throws IOException {
		T result = null;
		SerialNetwork device = (serialNetwork == null ? null : serialNetwork.service());
		if ( device != null ) {
			result = device.performAction(action);
		}
		return result;
	}

	/**
	 * Get the non-expired cached sample instance.
	 * 
	 * @return the cached sample, or {@literal null} if the instance is not
	 *         available or has expired
	 */
	public S getSample() {
		S sample = getCachedSample();
		if ( isCachedSampleExpired(sample) ) {
			return null;
		}
		return sample;
	}

	/**
	 * Get the cached sample data instance.
	 * 
	 * @return the data, or {@literal null}
	 */
	public S getCachedSample() {
		return sample.get();
	}

	/**
	 * Set the cached sample data instance.
	 * 
	 * @param sample
	 *        the data to cache
	 */
	protected void setCachedSample(S sample) {
		this.sample.set(sample);
	}

	/**
	 * Get direct access to the device info data.
	 * 
	 * @return the device info, or {@literal null}
	 */
	protected Map<String, Object> getDeviceInfoMap() {
		return deviceInfo;
	}

	/**
	 * Set the device info data. Setting the {@code deviceInfo} to
	 * {@literal null} will force the next call to {@link #getDeviceInfo()} to
	 * read from the device to populate this data, and setting this to anything
	 * else will force all subsequent calls to {@link #getDeviceInfo()} to
	 * simply return that map.
	 * 
	 * @param deviceInfo
	 *        the device info map to set
	 */
	protected void setDeviceInfoMap(Map<String, Object> deviceInfo) {
		this.deviceInfo = deviceInfo;
	}

	/**
	 * Get the configured {@link SerialNetwork}.
	 * 
	 * @return the serial network
	 */
	public OptionalService<SerialNetwork> getSerialNetwork() {
		return serialNetwork;
	}

	/**
	 * Set the {@link SerialNetwork} to use.
	 * 
	 * @param serialNetwork
	 *        the serial network to use
	 */
	public void setSerialNetwork(OptionalService<SerialNetwork> serialNetwork) {
		this.serialNetwork = serialNetwork;
	}

	/**
	 * Get the sample cache maximum age, in milliseconds.
	 * 
	 * @return the cache milliseconds
	 */
	public long getSampleCacheMs() {
		return sampleCacheMs;
	}

	/**
	 * Set the sample cache maximum age, in milliseconds.
	 * 
	 * @param sampleCacheMs
	 *        the cache milliseconds
	 */
	public void setSampleCacheMs(long sampleCacheMs) {
		this.sampleCacheMs = sampleCacheMs;
	}

	/**
	 * Get the source ID to use for returned datum.
	 * 
	 * @return the source ID
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the source ID to use for returned datum.
	 * 
	 * @param sourceId
	 *        the source ID to use; defaults to {@literal PVI-3800}
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

}
