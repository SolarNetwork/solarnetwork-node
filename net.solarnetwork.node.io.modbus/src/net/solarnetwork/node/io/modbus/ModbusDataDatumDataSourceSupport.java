/* ==================================================================
 * ModbusDataDatumDataSourceSupport.java - 30/07/2018 9:51:07 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus;

import java.io.IOException;
import java.util.Map;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.domain.DataAccessor;

/**
 * A base helper class to support {@link ModbusNetwork} based
 * {@link DatumDataSource} implementations using {@link ModbusData} as a model
 * object.
 * 
 * @author matt
 * @version 1.0
 * @since 2.9
 */
public abstract class ModbusDataDatumDataSourceSupport<T extends ModbusData & DataAccessor>
		extends ModbusDeviceDatumDataSourceSupport {

	private final T sample;
	private long sampleCacheMs = 5000;

	public ModbusDataDatumDataSourceSupport(T data) {
		super();
		this.sample = data;
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
		T currSample = null;
		if ( isCachedSampleExpired() ) {
			currSample = performAction(new ModbusConnectionAction<T>() {

				@Override
				public T doWithConnection(ModbusConnection connection) throws IOException {
					T sample = getSample();
					if ( sample.getDataTimestamp() == 0 ) {
						// first time also load info
						readDeviceInfoFirstTime(connection, sample);
					}
					refreshDeviceData(connection, sample);
					return createSampleSnapshot(sample);
				}

			});
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
	 */
	protected void readDeviceInfoFirstTime(ModbusConnection connection, T sample) {
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
	 */
	protected abstract void refreshDeviceInfo(ModbusConnection connection, T sample);

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
	 */
	protected abstract void refreshDeviceData(ModbusConnection connection, T sample);

	/**
	 * Create s snapshot copy of the sample data.
	 * 
	 * <p>
	 * This implementation calls {@link ModbusData#copy()} and casts the result
	 * to {@code T}.
	 * </p>
	 * 
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
		final long lastReadDiff = System.currentTimeMillis() - sample.getDataTimestamp();
		if ( lastReadDiff > sampleCacheMs ) {
			return true;
		}
		return false;
	}

	@Override
	protected Map<String, Object> readDeviceInfo(ModbusConnection conn) {
		try {
			T sample = getCurrentSample();
			return sample.getDeviceInfo();
		} catch ( IOException e ) {
			log.error("Communication problem reading from device {}: {}", modbusNetwork(),
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
	 * @param sampleCacheSecondsMs
	 *        the cache milliseconds
	 */
	public void setSampleCacheMs(long sampleCacheMs) {
		this.sampleCacheMs = sampleCacheMs;
	}
}
