/* ==================================================================
 * SunSpecDeviceDatumDataSourceSupport.java - 10/10/2018 4:06:30 PM
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

package net.solarnetwork.node.hw.sunspec.support;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import net.solarnetwork.node.hw.sunspec.CommonModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelDataFactory;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusDeviceDatumDataSourceSupport;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;

/**
 * Supporting class for {@link net.solarnetwork.node.DatumDataSource}
 * implementations for SunSpec devices.
 * 
 * @author matt
 * @version 1.3
 * @since 1.1
 */
public abstract class SunSpecDeviceDatumDataSourceSupport extends ModbusDeviceDatumDataSourceSupport {

	private final AtomicReference<ModelData> sample;

	private long sampleCacheMs = 5000;
	private String sourceId = "SunSpec-Device";

	/**
	 * Default constructor.
	 */
	public SunSpecDeviceDatumDataSourceSupport() {
		this(new AtomicReference<>());
	}

	/**
	 * Construct with a specific sample data instance.
	 * 
	 * @param sample
	 *        the sample data to use
	 */
	public SunSpecDeviceDatumDataSourceSupport(AtomicReference<ModelData> sample) {
		super();
		this.sample = sample;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "{sourceId=" + sourceId + "}";
	}

	/**
	 * Get the primary {@link ModelAccessor} type this data source deals with.
	 * 
	 * @return the primary type
	 */
	protected abstract Class<? extends ModelAccessor> getPrimaryModelAccessorType();

	/**
	 * Get the current model data.
	 * 
	 * <p>
	 * This returns cached data if possible. Otherwise it will query the device
	 * and cache the results. When refreshing data, only the data for the model
	 * returned from {@link #getPrimaryModelAccessorType()} will be refreshed.
	 * </p>
	 * 
	 * @return the model data
	 */
	protected ModelData getCurrentSample() {
		ModelData currSample = getSample();
		if ( isCachedSampleExpired(currSample) ) {
			try {
				final ModelData data = currSample;
				currSample = performAction(new ModbusConnectionAction<ModelData>() {

					@Override
					public ModelData doWithConnection(ModbusConnection connection) throws IOException {
						if ( data == null ) {
							ModelData result = ModelDataFactory.getInstance().getModelData(connection);
							if ( result != null ) {
								sample.set(result);
							}
							return result;
						}
						final ModelAccessor accessor = data
								.findTypedModel(getPrimaryModelAccessorType());
						if ( accessor != null ) {
							data.readModelData(connection, Collections.singletonList(accessor));
						}
						return data;
					}

				});
				if ( log.isTraceEnabled() && currSample != null ) {
					log.trace(currSample.dataDebugString());
				}
				log.debug("Read SunSpec inverter data: {}", currSample);
			} catch ( IOException e ) {
				throw new RuntimeException("Communication problem reading source " + this.sourceId
						+ " from SunSpec inverter device " + modbusDeviceName(), e);
			}
		}
		return (currSample != null ? currSample.getSnapshot() : null);
	}

	/**
	 * Get the currently cached model data.
	 * 
	 * <p>
	 * This does not check if the data has expired.
	 * </p>
	 * 
	 * @return the cached model data, or {@literal null}
	 */
	public ModelData getSample() {
		return sample.get();
	}

	/**
	 * Get a snapshot of the cached model data.
	 * 
	 * <p>
	 * This does not check if the data has expired. It returns the value of
	 * {@link ModelData#getSnapshot()} so that a copy of the data is returned.
	 * </p>
	 * 
	 * @return the cached model data copy, or {@literal null}
	 */
	public ModelData getSampleSnapshot() {
		ModelData data = getSample();
		return (data != null ? data.getSnapshot() : null);
	}

	@Override
	protected Map<String, Object> readDeviceInfo(ModbusConnection connection) {
		CommonModelAccessor data = getSample();
		if ( data == null ) {
			data = ModelDataFactory.getInstance().getModelData(connection);
		}
		if ( data == null ) {
			return null;
		}
		return data.getDeviceInfo();
	}

	/**
	 * Test if the sample data has expired.
	 * 
	 * @return {@literal true} if the sample data has expired
	 */
	protected boolean isCachedSampleExpired(ModelData data) {
		if ( data == null ) {
			return true;
		}
		final long lastReadDiff = System.currentTimeMillis() - data.getDataTimestamp();
		if ( lastReadDiff > sampleCacheMs ) {
			return true;
		}
		return false;
	}

	// SettingSpecifierProvider

	protected abstract SunSpecDeviceDatumDataSourceSupport getSettingsDefaultInstance();

	/**
	 * Get a list of {@link SettingSpecifier} instances.
	 * 
	 * <p>
	 * This method gets a defaults instance via
	 * {@link #getSettingsDefaultInstance()} and then passes that to
	 * {@link #getSettingSpecifiersWithDefaults(SunSpecDeviceDatumDataSourceSupport)},
	 * returning the result.
	 * </p>
	 * 
	 * @return list of {@link SettingSpecifier}
	 */
	public List<SettingSpecifier> getSettingSpecifiers() {
		SunSpecDeviceDatumDataSourceSupport defaults = getSettingsDefaultInstance();
		return getSettingSpecifiersWithDefaults(defaults);
	}

	/**
	 * Get a list of {@link SettingSpecifier} instances.
	 * 
	 * <p>
	 * The settings returned by this method include the following items:
	 * </p>
	 * 
	 * <ol>
	 * <li>A <code>info</code> title with {@link #getInfoMessage()}</li>
	 * <li>A <code>status</code> title with
	 * {@link #getStatusMessage(ModelData)}</li>
	 * <li>A <code>sample</code> title with
	 * {@link #getSampleMessage(ModelData)}</li>
	 * <li>All items returned by
	 * {@link #getIdentifiableSettingSpecifiers()}</li>
	 * <li>All items returned by
	 * {@link #getModbusNetworkSettingSpecifiers()}</li>
	 * <li>A <code>sampleCacheMs</code> text field.</li>
	 * <li>A <code>sourceId</code> text field.</li>
	 * </ol>
	 * 
	 * @param defaults
	 *        the defaults to use
	 * @return list of {@link SettingSpecifier}, never {@literal null}
	 */
	protected List<SettingSpecifier> getSettingSpecifiersWithDefaults(
			SunSpecDeviceDatumDataSourceSupport defaults) {
		final ModelData sample = getSampleSnapshot();

		List<SettingSpecifier> results = new ArrayList<>(12);
		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(), true));
		results.add(new BasicTitleSettingSpecifier("status", getStatusMessage(sample), true));
		results.add(new BasicTitleSettingSpecifier("sample", getSampleMessage(sample), true));

		results.addAll(getIdentifiableSettingSpecifiers());
		results.addAll(getModbusNetworkSettingSpecifiers());

		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(defaults.getSampleCacheMs())));
		results.add(new BasicTextFieldSettingSpecifier("sourceId", defaults.sourceId));

		return results;
	}

	/**
	 * Get the device info message.
	 * 
	 * <p>
	 * This calls {@link #getDeviceInfoMessage()}, trapping all exceptions.
	 * </p>
	 * 
	 * @return the message, or {@literal N/A} if no message is available
	 */
	protected String getInfoMessage() {
		String msg = null;
		try {
			msg = getDeviceInfoMessage();
		} catch ( RuntimeException e ) {
			log.debug("Error reading info: {}", e.getMessage());
		}
		return (msg == null ? "N/A" : msg);
	}

	/**
	 * Get a status message.
	 * 
	 * <p>
	 * This method simply returns {@literal N/A}.
	 * </p>
	 * 
	 * @param sample
	 *        the sample to derive the status message from
	 * @return the message, or {@literal N/A} if no message is available
	 */
	protected String getStatusMessage(ModelData sample) {
		return "N/A";
	}

	/**
	 * Get a sample message.
	 * 
	 * <p>
	 * This method simply returns {@literal N/A}.
	 * </p>
	 * 
	 * @param sample
	 *        the sample to derive the message from
	 * @return the message, or {@literal N/A} if no message is available
	 */
	protected String getSampleMessage(ModelData sample) {
		return "N/A";
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

	/**
	 * Get the source ID used for datum.
	 * 
	 * @return the source ID
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the source ID to use for returned datum.
	 * 
	 * @param soruceId
	 *        the source ID to use; defaults to {@literal modbus}
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

}
