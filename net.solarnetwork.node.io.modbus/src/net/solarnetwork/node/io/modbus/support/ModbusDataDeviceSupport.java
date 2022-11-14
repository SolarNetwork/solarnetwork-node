/* ==================================================================
 * ModbusDataDeviceSupport.java - 2/09/2019 10:03:41 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus.support;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.solarnetwork.node.domain.DataAccessor;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * A base helper class to support {@link ModbusNetwork} based device
 * implementations using {@link ModbusData} as a model object.
 * 
 * @param <T>
 *        the {@link ModbusData} type
 * @author matt
 * @version 2.0
 * @since 2.14
 */
public abstract class ModbusDataDeviceSupport<T extends ModbusData & DataAccessor>
		extends ModbusDeviceSupport {

	/** The default value for the {@code sampleCacheMs} property. */
	public static final long DEFAULT_SAMPLE_CACHE_MS = 5000L;

	private final T sample;
	private long sampleCacheMs = DEFAULT_SAMPLE_CACHE_MS;

	/**
	 * Constructor.
	 * 
	 * @param data
	 *        the data
	 */
	public ModbusDataDeviceSupport(T data) {
		super();
		this.sample = data;
	}

	/**
	 * Get setting specifiers for the {@literal unitId} and
	 * {@literal modbusNetwork.propertyFilters['uid']} properties.
	 * 
	 * @param prefix
	 *        a setting prefix to prepend
	 * @return list of setting specifiers
	 * @since 1.2
	 */
	public static List<SettingSpecifier> modbusDeviceNetworkSettings(String prefix) {
		if ( prefix == null ) {
			prefix = "";
		}
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(1);
		results.add(new BasicTextFieldSettingSpecifier(prefix + "sampleCacheMs",
				String.valueOf(DEFAULT_SAMPLE_CACHE_MS)));
		return results;
	}

	/**
	 * Get an up-to-date snapshot of the device data.
	 * 
	 * <p>
	 * If the sample data has expired, or never been read, this method will
	 * refresh it from the device by calling
	 * {@link #refreshDeviceData(ModbusConnection, ModbusData)}. If the data has
	 * never been read, it will also first call
	 * {@link #refreshDeviceInfo(ModbusConnection, ModbusData)}. A copy of the
	 * sample data is returned via {@link #createSampleSnapshot(ModbusData)}.
	 * </p>
	 * 
	 * @return the sample data copy
	 * @throws IOException
	 *         if a communication error occurs
	 */
	protected T getCurrentSample() throws IOException {
		return getCurrentSample(null);
	}

	/**
	 * Get an up-to-date snapshot of the device data.
	 * 
	 * <p>
	 * If the sample data has expired, or never been read, this method will
	 * refresh it from the device by calling
	 * {@link #refreshDeviceData(ModbusConnection, ModbusData)}. If the data has
	 * never been read, it will also first call
	 * {@link #refreshDeviceInfo(ModbusConnection, ModbusData)}. A copy of the
	 * sample data is returned via {@link #createSampleSnapshot(ModbusData)}.
	 * </p>
	 * 
	 * @param connection
	 *        an optional existing connection to use; otherwise a new connection
	 *        will be acquired
	 * @return the sample data copy
	 * @throws IOException
	 *         if a communication error occurs
	 */
	protected T getCurrentSample(ModbusConnection connection) throws IOException {
		T currSample = null;
		if ( isCachedSampleExpired() ) {
			ModbusConnectionAction<T> action = new ModbusConnectionAction<T>() {

				@Override
				public T doWithConnection(ModbusConnection conn) throws IOException {
					T sample = getSample();
					if ( sample.getDataTimestamp() == null ) {
						// first time also load info
						readDeviceInfoFirstTime(conn, sample);
					}
					refreshDeviceData(conn, sample);
					return createSampleSnapshot(sample);
				}

			};
			if ( connection != null ) {
				currSample = action.doWithConnection(connection);
			} else {
				currSample = performAction(action);
			}
			if ( log.isTraceEnabled() && currSample != null ) {
				log.trace(currSample.dataDebugString());
			}
			log.debug("Read {} data: {}", sample.getClass().getSimpleName(), currSample);
		} else {
			currSample = createSampleSnapshot(getSample());
		}
		return currSample;
	}

	/**
	 * Read device information when first attempting to communicate with the
	 * device.
	 * 
	 * <p>
	 * This method will be called only once by {@link #getCurrentSample()},
	 * before calling {@link #refreshDeviceData(ModbusConnection, ModbusData)}.
	 * This implementation simply calls
	 * {@link #refreshDeviceInfo(ModbusConnection, ModbusData)}.
	 * </p>
	 * 
	 * @param connection
	 *        the Modbus connection
	 * @param sample
	 *        the sample to refresh
	 * @throws IOException
	 *         if any communication error occurs
	 */
	protected void readDeviceInfoFirstTime(ModbusConnection connection, T sample) throws IOException {
		refreshDeviceInfo(connection, sample);
	}

	/**
	 * Refresh the device info data.
	 * 
	 * <p>
	 * This should refresh the Modbus registers that contain device information
	 * such as the serial number, name, etc.
	 * </p>
	 * 
	 * @param connection
	 *        the Modbus connection
	 * @param sample
	 *        the sample to refresh
	 * @throws IOException
	 *         if any communication error occurs
	 */
	protected abstract void refreshDeviceInfo(ModbusConnection connection, T sample) throws IOException;

	/**
	 * Refresh the device data.
	 * 
	 * <p>
	 * This should refresh the Modbus registers that contain the actual
	 * information being captured by this class and stored in datum instances.
	 * The {@link #getCurrentSample()} method calls this when the sample data is
	 * expired.
	 * </p>
	 * 
	 * @param connection
	 *        the Modbus connection
	 * @param sample
	 *        the sample to refresh
	 * @throws IOException
	 *         if any communication error occurs
	 */
	protected abstract void refreshDeviceData(ModbusConnection connection, T sample) throws IOException;

	/**
	 * Create s snapshot copy of the sample data.
	 * 
	 * <p>
	 * This implementation calls {@link ModbusData#copy()} and casts the result
	 * to {@code T}.
	 * </p>
	 * 
	 * @param sample
	 *        the sample to copy
	 * @return the copy of the sample data
	 */
	@SuppressWarnings("unchecked")
	protected T createSampleSnapshot(T sample) {
		return (T) sample.copy();
	}

	/**
	 * Test if the sample data has expired.
	 * 
	 * @return {@literal true} if the sample data has expired
	 */
	protected boolean isCachedSampleExpired() {
		final T sample = getSample();
		Instant ts = (sample != null ? sample.getDataTimestamp() : null);
		if ( ts == null ) {
			ts = Instant.EPOCH;
		}
		final long lastReadDiff = ts.until(Instant.now(), ChronoUnit.MILLIS);
		if ( lastReadDiff > sampleCacheMs ) {
			return true;
		}
		return false;
	}

	@Override
	protected Map<String, Object> readDeviceInfo(ModbusConnection conn) {
		try {
			T sample = getCurrentSample(conn);
			return sample.getDeviceInfo();
		} catch ( IOException e ) {
			log.error("Communication problem reading device info from device {}: {}", modbusDeviceName(),
					e.getMessage());
			return null;
		}
	}

	/**
	 * Get the Modbus data instance.
	 * 
	 * @return the data
	 */
	public T getSample() {
		return sample;
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

}
