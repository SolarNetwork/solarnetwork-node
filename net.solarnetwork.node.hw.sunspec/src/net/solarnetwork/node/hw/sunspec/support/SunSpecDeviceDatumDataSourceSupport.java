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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import net.solarnetwork.domain.DeviceInfo;
import net.solarnetwork.node.hw.sunspec.CommonModelAccessor;
import net.solarnetwork.node.hw.sunspec.GenericModelId;
import net.solarnetwork.node.hw.sunspec.ModelAccessor;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelDataFactory;
import net.solarnetwork.node.hw.sunspec.ModelDataProvider;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.io.modbus.support.ModbusDeviceDatumDataSourceSupport;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.StringUtils;

/**
 * Supporting class for {@link net.solarnetwork.node.service.DatumDataSource}
 * implementations for SunSpec devices.
 *
 * @author matt
 * @version 2.3
 * @since 1.1
 */
public abstract class SunSpecDeviceDatumDataSourceSupport extends ModbusDeviceDatumDataSourceSupport
		implements ModelDataProvider {

	private final AtomicReference<ModelData> sample;

	private long sampleCacheMs = 5000;
	private String sourceId = null;
	private Integer baseAddress = null;
	private Set<Integer> secondaryModelIds;

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
	public Collection<String> publishedSourceIds() {
		final String sourceId = resolvePlaceholders(getSourceId());
		return (sourceId == null || sourceId.isEmpty() ? Collections.emptySet()
				: Collections.singleton(sourceId));
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
	 * and cache the results. Only the data for the model returned from
	 * {@link #getPrimaryModelAccessorType()} and any accessors in
	 * {@link #getSecondaryModelAccessors(ModelData)} will be populated with
	 * data.
	 * </p>
	 *
	 * @return the model data
	 * @throws IOException
	 *         if a communication error occurs
	 */
	protected ModelData getCurrentSample() throws IOException {
		ModelData currSample = getSample();
		if ( isCachedSampleExpired(currSample) ) {
			final ModelData data = currSample;
			currSample = performAction(new ModbusConnectionAction<ModelData>() {

				@Override
				public ModelData doWithConnection(ModbusConnection connection) throws IOException {
					ModelData result = data;
					if ( result == null ) {
						result = modelData(connection);
						if ( result != null ) {
							sample.set(result);
						}
					}
					final ModelAccessor accessor = result.findTypedModel(getPrimaryModelAccessorType());
					final List<ModelAccessor> secondaryAccessors = getSecondaryModelAccessors(result);
					List<ModelAccessor> accessors = null;
					if ( secondaryAccessors == null || secondaryAccessors.isEmpty() ) {
						accessors = (accessor != null ? Collections.singletonList(accessor) : null);
					} else {
						accessors = new ArrayList<>(secondaryAccessors.size() + 1);
						if ( accessor != null ) {
							accessors.add(accessor);
						}
						accessors.addAll(secondaryAccessors);
					}
					if ( accessors != null && !accessors.isEmpty() ) {
						result.readModelData(connection, accessors);
					}
					return result;
				}

			});
			if ( log.isTraceEnabled() && currSample != null ) {
				log.trace(currSample.dataDebugString());
			}
			log.debug("Read SunSpec data: {}", currSample);
		}
		return (currSample != null ? currSample.getSnapshot() : null);
	}

	private ModelData modelData(ModbusConnection conn) throws IOException {
		Integer manualBaseAddress = getBaseAddress();
		if ( manualBaseAddress != null && manualBaseAddress.intValue() >= 0 ) {
			return ModelDataFactory.getInstance().getModelData(conn,
					ModelDataFactory.DEFAULT_MAX_READ_WORDS_COUNT, manualBaseAddress, false);
		}
		return ModelDataFactory.getInstance().getModelData(conn, false);
	}

	private List<ModelAccessor> getSecondaryModelAccessors(ModelData data) {
		final Set<Integer> modelIds = getSecondaryModelIds();
		if ( modelIds == null || modelIds.isEmpty() ) {
			return null;
		}
		final List<ModelAccessor> allModels = data.getModels();
		final List<ModelAccessor> accessors = new ArrayList<>(modelIds.size());
		for ( Integer modelId : modelIds ) {
			for ( ModelAccessor accessor : allModels ) {
				if ( accessor.getModelId() != null && accessor.getModelId().getId() == modelId ) {
					accessors.add(accessor);
					break;
				}
			}
		}
		return accessors;
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
	public final ModelData getSample() {
		return sample.get();
	}

	@Override
	public ModelData modelData() {
		try {
			return getCurrentSample();
		} catch ( IOException e ) {
			log.warn("Communication problem reading model data: {}", e.toString());
			return null;
		}
	}

	@Override
	public ModbusConnection modelDataModbusConnection() {
		ModbusNetwork network = OptionalService.service(getModbusNetwork());
		if ( network == null ) {
			return null;
		}
		return network.createConnection(getUnitId());
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
	protected Map<String, Object> readDeviceInfo(ModbusConnection connection) throws IOException {
		CommonModelAccessor data = getSample();
		if ( data == null ) {
			data = modelData(connection);
		}
		if ( data == null ) {
			return null;
		}
		return data.getDeviceInfo();
	}

	/**
	 * Support for {@code DeviceInfoProvider}.
	 *
	 * @return the configured source ID
	 */
	public String deviceInfoSourceId() {
		return resolvePlaceholders(getSourceId());
	}

	@Override
	public DeviceInfo deviceInfo() {
		getDeviceInfo(); // make sure common model populated
		ModelData s = sample.get();
		return (s != null ? s.deviceInfo() : null);
	}

	/**
	 * Test if the sample data has expired.
	 *
	 * @param data
	 *        the model data
	 * @return {@literal true} if the sample data has expired
	 */
	protected boolean isCachedSampleExpired(ModelData data) {
		if ( data == null || data.getDataTimestamp() == null ) {
			return true;
		}
		final long lastReadDiff = data.getDataTimestamp().until(Instant.now(), ChronoUnit.MILLIS);
		if ( lastReadDiff > sampleCacheMs ) {
			return true;
		}
		return false;
	}

	// SettingSpecifierProvider

	/**
	 * Get an instance of this object to use as default values for settings.
	 *
	 * @return the default settings instance
	 */
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
	 * <li>A <code>secondaryModels</code> title with
	 * {@link #getSecondaryModelAccessors(ModelData)}</li>
	 * <li>All items returned by
	 * {@link #getIdentifiableSettingSpecifiers()}</li>
	 * <li>All items returned by
	 * {@link #getModbusNetworkSettingSpecifiers()}</li>
	 * <li>A <code>sampleCacheMs</code> text field.</li>
	 * <li>A <code>sourceId</code> text field.</li>
	 * <li>A <code>secondaryModelIdsValue</code> text field.</li>
	 * </ol>
	 *
	 * @param defaults
	 *        the defaults to use
	 * @return list of {@link SettingSpecifier}, never {@literal null}
	 */
	protected List<SettingSpecifier> getSettingSpecifiersWithDefaults(
			SunSpecDeviceDatumDataSourceSupport defaults) {
		final ModelData sample = getSampleSnapshot();

		List<SettingSpecifier> results = new ArrayList<>(16);
		results.add(new BasicTitleSettingSpecifier("info", getInfoMessage(), true));
		results.add(new BasicTitleSettingSpecifier("status", getStatusMessage(sample), true));
		results.add(new BasicTitleSettingSpecifier("sample", getSampleMessage(sample), true));
		results.add(new BasicTitleSettingSpecifier("secondaryModels", getSecondaryTypesMessage(sample),
				true));

		results.addAll(getIdentifiableSettingSpecifiers());
		results.addAll(modbusNetworkSettingSpecifiers(null, DEFAULT_UNIT_ID));

		results.add(new BasicTextFieldSettingSpecifier("sampleCacheMs",
				String.valueOf(defaults.getSampleCacheMs())));
		results.add(new BasicTextFieldSettingSpecifier("sourceId", defaults.sourceId));

		results.add(new BasicTextFieldSettingSpecifier("secondaryModelIdsValue",
				defaults.getSecondaryModelIdsValue()));
		results.add(new BasicTextFieldSettingSpecifier("baseAddress", ""));

		results.addAll(getDeviceInfoMetadataSettingSpecifiers());

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
	 * Get the secondary types message.
	 *
	 * <p>
	 * The returned message is a comma-delimited list of model IDs with their
	 * associated descriptions. The first model is ignored, which is assumed to
	 * be the primary model.
	 * </p>
	 *
	 * @param sample
	 *        the model data
	 * @return the message, or {@literal N/A} if no secondary types are
	 *         available
	 */
	protected String getSecondaryTypesMessage(ModelData sample) {
		List<ModelAccessor> accessors = (sample != null ? sample.getModels() : null);
		if ( accessors == null || accessors.size() < 2 ) {
			return "N/A";
		}
		return accessors.subList(1, accessors.size()).stream().filter(a -> a.getModelId() != null)
				.map(a -> {
					if ( a.getModelId() instanceof GenericModelId ) {
						return String.valueOf(a.getModelId().getId());
					}
					return String.format("%d (%s)", a.getModelId().getId(),
							a.getModelId().getDescription());

				}).collect(Collectors.joining(", "));
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
	 * @param sourceId
	 *        the source ID to use; defaults to {@literal modbus}
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Get a set of secondary models to acquire data from.
	 *
	 * @return an optional set of model IDs
	 * @since 1.4
	 */
	public Set<Integer> getSecondaryModelIds() {
		return secondaryModelIds;
	}

	/**
	 * Set a set of secondary models to acquire data from.
	 *
	 * @param secondaryModelIds
	 *        the secondary model IDs
	 * @since 1.4
	 */
	public void setSecondaryModelIds(Set<Integer> secondaryModelIds) {
		this.secondaryModelIds = secondaryModelIds;
	}

	/**
	 * Get the secondary model IDs as a comma-delimited string value.
	 *
	 * @return the secondary model IDs as a delimited string
	 * @since 1.4
	 */
	public String getSecondaryModelIdsValue() {
		return StringUtils.commaDelimitedStringFromCollection(getSecondaryModelIds());
	}

	/**
	 * Set the secondary model IDs as a comma-delimited string value.
	 *
	 * @param value
	 *        the secondary model IDs as a delimited string
	 * @since 1.4
	 */
	public void setSecondaryModelIdsValue(String value) {
		Set<String> idStrings = StringUtils.commaDelimitedStringToSet(value);
		Set<Integer> ids = null;
		if ( idStrings != null && !idStrings.isEmpty() ) {
			ids = new LinkedHashSet<>(idStrings.size());
			for ( String idString : idStrings ) {
				try {
					ids.add(Integer.valueOf(idString));
				} catch ( NumberFormatException e ) {
					// ignore
				}
			}
		}
		setSecondaryModelIds(ids);
	}

	/**
	 * Get the manual base address to use.
	 *
	 * @return the manual base address, or {@literal null} to automatically
	 *         discover the base address
	 * @since 1.5
	 */
	public Integer getBaseAddress() {
		return baseAddress;
	}

	/**
	 * Set the manual base address to set.
	 *
	 * @param baseAddress
	 *        the base address to set, or anything less than {@literal 0} to
	 *        automatically discover the base address
	 * @since 1.5
	 */
	public void setBaseAddress(Integer baseAddress) {
		if ( baseAddress != null && baseAddress.intValue() < 0 ) {
			baseAddress = null;
		}
		this.baseAddress = baseAddress;
	}

}
