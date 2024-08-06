/* ==================================================================
 * DatumDataSourceSupport.java - 26/09/2017 9:38:58 AM
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

package net.solarnetwork.node.service.support;

import static java.util.Collections.singletonMap;
import static net.solarnetwork.service.OptionalService.service;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.domain.KeyValuePair;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.node.domain.ExpressionRoot;
import net.solarnetwork.node.domain.datum.MutableNodeDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.DatumMetadataService;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.node.service.DatumService;
import net.solarnetwork.node.service.TariffScheduleProvider;
import net.solarnetwork.service.DatumFilterService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.OptionalService.OptionalFilterableService;
import net.solarnetwork.service.OptionalServiceCollection;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.util.ArrayUtils;

/**
 * Helper class for {@link net.solarnetwork.node.service.DatumDataSource} and
 * {@link net.solarnetwork.node.service.MultiDatumDataSource} implementations to
 * extend.
 *
 * @author matt
 * @version 1.3
 * @since 2.0
 */
public class DatumDataSourceSupport extends BaseIdentifiable {

	/**
	 * A transform properties instance that can be used to signal "sub-sampling"
	 * mode to the transform service.
	 */
	public static final Map<String, Object> SUB_SAMPLE_PROPS = singletonMap("subsample", true);

	/**
	 * A global cache of source-based metadata, so only changes to metadata need
	 * be posted.
	 */
	private static final ConcurrentMap<String, GeneralDatumMetadata> SOURCE_METADATA_CACHE;
	static {
		SOURCE_METADATA_CACHE = new ConcurrentHashMap<>(8, 0.9f, 2);
	}

	/** The {@code subSampleStartDelay} property default value. */
	public static final long DEFAULT_SUBSAMPLE_START_DELAY = 15000L;

	/** The {@code publishDeviceInfoMetadata} property default value. */
	public static final boolean DEFAULT_PUBLISH_DEVICE_INFO_METADATA = true;

	private OptionalService<DatumMetadataService> datumMetadataService;
	private OptionalService<DatumQueue> datumQueue;
	private OptionalService<DatumService> datumService;
	private TaskScheduler taskScheduler = null;
	private Long subSampleFrequency = null;
	private long subSampleStartDelay = DEFAULT_SUBSAMPLE_START_DELAY;
	private OptionalFilterableService<DatumFilterService> datumFilterService;
	private ExpressionConfig[] expressionConfigs;
	private boolean publishDeviceInfoMetadata = DEFAULT_PUBLISH_DEVICE_INFO_METADATA;
	private OptionalServiceCollection<TariffScheduleProvider> tariffScheduleProviders;

	private ScheduledFuture<?> subSampleFuture;

	/**
	 * Default constructor.
	 */
	public DatumDataSourceSupport() {
		super();
	}

	/**
	 * Get the collection of source IDs produced by this datum data source.
	 *
	 * <p>
	 * This is a default implementation meant to be overridden is extending
	 * classes.
	 * </p>
	 *
	 * @return the collection of published source IDs, never {@literal null}
	 * @since 1.2
	 */
	public Collection<String> publishedSourceIds() {
		return Collections.emptySet();
	}

	/**
	 * Clear the source metadata cache.
	 *
	 * <p>
	 * This is designed to support testing primarily.
	 * </p>
	 *
	 * @since 1.1
	 */
	public static final void clearSourceMetadataCache() {
		SOURCE_METADATA_CACHE.clear();
	}

	/**
	 * Offer a non-persisted datum event to the configured {@link DatumQueue},
	 * if available.
	 *
	 * @param datum
	 *        the datum that was captured
	 */
	protected final void offerDatumCapturedEvent(NodeDatum datum) {
		if ( datum != null ) {
			final DatumQueue queue = service(datumQueue);
			if ( queue != null ) {
				// offer datum to queue instead of posting as event, as we expect the queue to emit the event
				queue.offer(datum, false);
			}
		}
	}

	/**
	 * Add source metadata using the configured {@link DatumMetadataService} (if
	 * available). The metadata will be cached so that subsequent calls to this
	 * method with the same metadata value will not try to re-save the unchanged
	 * value. This method will catch all exceptions and silently discard them.
	 *
	 * @param sourceId
	 *        the source ID to add metadata to; place holders will be resolved
	 *        via {@link #resolvePlaceholders(String)}
	 * @param meta
	 *        the metadata to add
	 * @return {@literal true} if the metadata was saved successfully, or does
	 *         not need to be updated
	 */
	protected boolean addSourceMetadata(final String sourceId, final GeneralDatumMetadata meta) {
		final String resolvedSourceId = resolvePlaceholders(sourceId);
		if ( resolvedSourceId == null ) {
			return false;
		}
		GeneralDatumMetadata cached = SOURCE_METADATA_CACHE.get(resolvedSourceId);
		if ( cached != null && meta.equals(cached) ) {
			// we've already posted this metadata... don't bother doing it again
			log.debug("Source {} metadata already added, not posting again", resolvedSourceId);
			return true;
		}
		DatumMetadataService service = null;
		if ( datumMetadataService != null ) {
			service = datumMetadataService.service();
		}
		if ( service == null ) {
			return false;
		}
		try {
			service.addSourceMetadata(resolvedSourceId, meta);
			SOURCE_METADATA_CACHE.put(resolvedSourceId, meta);
			return true;
		} catch ( Exception e ) {
			log.warn("Error saving source {} metadata: {}", resolvedSourceId, e.getMessage());
		}
		return false;
	}

	/**
	 * Get setting specifiers for the {@code uid} and {@code groupUid}
	 * properties.
	 *
	 * @return list of setting specifiers
	 */
	protected List<SettingSpecifier> getIdentifiableSettingSpecifiers() {
		return baseIdentifiableSettings("");
	}

	/**
	 * Get setting specifiers for the sub-sample supporting properties.
	 *
	 * @return list of setting specifiers
	 */
	protected List<SettingSpecifier> getSubSampleSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(16);
		results.add(
				new BasicTextFieldSettingSpecifier("datumFilterService.propertyFilters['uid']", null));
		results.add(new BasicTextFieldSettingSpecifier("subSampleFrequency", null));
		results.add(new BasicTextFieldSettingSpecifier("subSampleStartDelay",
				String.valueOf(DEFAULT_SUBSAMPLE_START_DELAY)));
		return results;
	}

	/**
	 * Get setting specifiers for device info metadata publishing support.
	 *
	 * @return list of settings
	 */
	protected List<SettingSpecifier> getDeviceInfoMetadataSettingSpecifiers() {
		return Collections.singletonList(new BasicToggleSettingSpecifier("publishDeviceInfoMetadata",
				DEFAULT_PUBLISH_DEVICE_INFO_METADATA));
	}

	/**
	 * Support the {@code DeviceInfoProvider} publish setting.
	 *
	 * @return the {@link #isPublishDeviceInfoMetadata()} value
	 */
	public boolean canPublishDeviceInfo() {
		return isPublishDeviceInfoMetadata();
	}

	/**
	 * Schedule sub-sampling with the given data source.
	 *
	 * @param dataSource
	 *        the data source
	 * @return the scheduled future or {@literal null} if sub-sampling support
	 *         is not configured; canceling this will stop the sub-sampling task
	 * @see #stopSubSampling()
	 */
	protected synchronized ScheduledFuture<?> startSubSampling(DatumDataSource dataSource) {
		stopSubSampling();
		final long freq = (subSampleFrequency != null ? subSampleFrequency.longValue() : 0);
		if ( taskScheduler == null || freq < 1 ) {
			return null;
		}
		log.info("Starting sub-sampling @ {}ms, after {}ms delay in {}", freq, subSampleStartDelay,
				this);
		ScheduledFuture<?> f = taskScheduler.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				try {
					readSubSampleDatum(dataSource);
				} catch ( Exception e ) {
					log.warn("Error reading sub-sample datum for {}", DatumDataSourceSupport.this, e);
				}
			}
		}, new Date(System.currentTimeMillis() + subSampleStartDelay), freq);
		this.subSampleFuture = f;
		return f;
	}

	/**
	 * Read a sub-sample datum value.
	 *
	 * <p>
	 * This method is invoked by a task scheduled after calling
	 * {@link #startSubSampling(DatumDataSource)}. It simply calls
	 * {@link DatumDataSource#readCurrentDatum()}. Extending classes may want to
	 * override this behavior.
	 * </p>
	 *
	 * @param dataSource
	 *        the data source previously passed to
	 *        {@link #startSubSampling(DatumDataSource)}
	 */
	protected void readSubSampleDatum(DatumDataSource dataSource) {
		NodeDatum datum = dataSource.readCurrentDatum();
		log.debug("Got sub-sample datum: {}", datum);
	}

	/**
	 * Stop any running sub-sampling task.
	 *
	 * @see DatumDataSourceSupport#startSubSampling(DatumDataSource)
	 */
	protected synchronized void stopSubSampling() {
		if ( subSampleFuture != null ) {
			log.info("Stopping sub-sampling in {}", this);
			subSampleFuture.cancel(true);
			this.subSampleFuture = null;
		}
	}

	/**
	 * Test if sub-sampling is currently active.
	 *
	 * @return {@literal true} if sub-sampling is active
	 */
	protected boolean isSubSampling() {
		ScheduledFuture<?> f = this.subSampleFuture;
		return f != null && !f.isDone();
	}

	/**
	 * Apply the configured datum filter service to a given datum.
	 *
	 * @param datum
	 *        the datum to possibly filter
	 * @param props
	 *        optional transform properties to pass to
	 *        {@link DatumFilterService#filter(net.solarnetwork.domain.datum.Datum, DatumSamplesOperations, Map)}
	 * @return the same datum, possibly transformed, or {@literal null} if the
	 *         datum has been filtered out completely
	 */
	protected NodeDatum applyDatumFilter(NodeDatum datum, Map<String, Object> props) {
		DatumFilterService xformService = service(getDatumFilterService());
		if ( xformService != null ) {
			DatumSamplesOperations out = xformService.filter(datum, datum.asSampleOperations(), props);
			if ( out == null ) {
				return null;
			} else if ( out != datum.asSampleOperations() ) {
				datum = datum.copyWithSamples(out);
			}
		}
		return datum;
	}

	/**
	 * Evaluate a set of expression configurations and store the results as
	 * properties on a datum.
	 *
	 * <p>
	 * This method will create a new {@link ExpressionRoot} instance for the
	 * expression root object and pass that to
	 * {@link #populateExpressionDatumProperties(MutableNodeDatum, ExpressionConfig[], Object)}.
	 * </p>
	 *
	 * @param d
	 *        the datum to store the results of expression evaluations on
	 * @param expressionConfs
	 *        the expression configurations
	 * @see #populateExpressionDatumProperties(MutableNodeDatum,
	 *      ExpressionConfig[], Object)
	 */
	protected void populateExpressionDatumProperties(final MutableNodeDatum d,
			final ExpressionConfig[] expressionConfs) {
		ExpressionRoot root = new ExpressionRoot(d, null, null, service(datumService), null,
				service(getMetadataService()), service(getLocationService()));
		root.setTariffScheduleProviders(tariffScheduleProviders);
		populateExpressionDatumProperties(d, expressionConfs, root);
	}

	/**
	 * Evaluate a set of expression configurations and store the results as
	 * properties on a datum.
	 *
	 * @param d
	 *        the datum to store the results of expression evaluations on
	 * @param expressionConfs
	 *        the expression configurations
	 * @param root
	 *        the expression root object
	 */
	protected void populateExpressionDatumProperties(final MutableNodeDatum d,
			final ExpressionConfig[] expressionConfs, final Object root) {
		super.populateExpressionDatumProperties(d.asMutableSampleOperations(), expressionConfs, root);
	}

	/**
	 * Save the {@link #getMetadata()} data as datum source metadata.
	 *
	 * @param sourceId
	 *        the source ID to save the metadata on
	 * @since 1.1
	 */
	protected void saveMetadata(String sourceId) {
		if ( sourceId == null || sourceId.isEmpty() ) {
			return;
		}
		KeyValuePair[] data = getMetadata();
		if ( data == null || data.length < 1 ) {
			return;
		}
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.populate(data);
		try {
			addSourceMetadata(sourceId, meta);
		} catch ( Exception e ) {
			log.warn("Error saving metadata values {} for source [{}]: {}", meta,
					resolvePlaceholders(sourceId), e);
		}
	}

	/**
	 * Get the configured {@link DatumMetadataService}.
	 *
	 * @return the service to use
	 */
	public OptionalService<DatumMetadataService> getDatumMetadataService() {
		return datumMetadataService;
	}

	/**
	 * Set a {@link DatumMetadataService} to use for managing datum metadata.
	 *
	 * @param datumMetadataService
	 *        the service to use
	 */
	public void setDatumMetadataService(OptionalService<DatumMetadataService> datumMetadataService) {
		this.datumMetadataService = datumMetadataService;
	}

	/**
	 * Get the task scheduler.
	 *
	 * @return the task scheduler
	 */
	public TaskScheduler getTaskScheduler() {
		return taskScheduler;
	}

	/**
	 * Set the task scheduler.
	 *
	 * @param taskScheduler
	 *        the task scheduler to set
	 */
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	/**
	 * Get a sub-sample frequency at which to request datum.
	 *
	 * @return the sub-sample frequency, in milliseconds, or {@literal null} or
	 *         anything less than {@literal 1} to disable sub-sampling
	 */
	public Long getSubSampleFrequency() {
		return subSampleFrequency;
	}

	/**
	 * Set a sub-sample frequency at which to request datum.
	 *
	 * <p>
	 * This is designed to work with a
	 * {@link #setDatumFilterService(OptionalFilterableService)} transformer
	 * that performs down-sampling of higher frequency data.
	 *
	 * @param subSampleFrequency
	 *        the frequency, to set, in milliseconds
	 */
	public void setSubSampleFrequency(Long subSampleFrequency) {
		this.subSampleFrequency = subSampleFrequency;
	}

	/**
	 * Get the sub-sample start delay.
	 *
	 * @return the delay, in milliseconds
	 */
	public long getSubSampleStartDelay() {
		return subSampleStartDelay;
	}

	/**
	 * Set the sub-sample start delay.
	 *
	 * @param subSampleStartDelay
	 *        the sub-sample start delay to set, in milliseconds
	 */
	public void setSubSampleStartDelay(long subSampleStartDelay) {
		this.subSampleStartDelay = subSampleStartDelay;
	}

	/**
	 * Get a datum filter service to use.
	 *
	 * @return the service
	 */
	public OptionalFilterableService<DatumFilterService> getDatumFilterService() {
		return datumFilterService;
	}

	/**
	 * Set a datum filter service to use.
	 *
	 * @param datumFilterService
	 *        the service to set
	 */
	public void setDatumFilterService(OptionalFilterableService<DatumFilterService> datumFilterService) {
		this.datumFilterService = datumFilterService;
	}

	/**
	 * Get the expression configurations.
	 *
	 * @return the expression configurations
	 */
	public ExpressionConfig[] getExpressionConfigs() {
		return expressionConfigs;
	}

	/**
	 * Set the expression configurations to use.
	 *
	 * @param expressionConfigs
	 *        the configs to use
	 */
	public void setExpressionConfigs(ExpressionConfig[] expressionConfigs) {
		this.expressionConfigs = expressionConfigs;
	}

	/**
	 * Get the number of configured {@code expressionConfigs} elements.
	 *
	 * @return the number of {@code expressionConfigs} elements
	 */
	public int getExpressionConfigsCount() {
		ExpressionConfig[] confs = this.expressionConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code ExpressionConfig} elements.
	 *
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link ExpressionConfig} instances.
	 * </p>
	 *
	 * @param count
	 *        The desired number of {@code expressionConfigs} elements.
	 */
	public void setExpressionConfigsCount(int count) {
		this.expressionConfigs = ArrayUtils.arrayWithLength(this.expressionConfigs, count,
				ExpressionConfig.class, null);
	}

	/**
	 * Get the desired device info metadata publish mode.
	 *
	 * @return {@literal true} to publish device metadata; defaults to
	 *         {@link DEFAULT_PUBLISH_DEVICE_INFO_METADATA}
	 */
	public boolean isPublishDeviceInfoMetadata() {
		return publishDeviceInfoMetadata;
	}

	/**
	 * Get the desired device info metadata publish mode.
	 *
	 * @param publishDeviceInfoMetadata
	 *        {@literal true} to publish device metadata once, after the first
	 *        datum has been captured
	 */
	public void setPublishDeviceInfoMetadata(boolean publishDeviceInfoMetadata) {
		this.publishDeviceInfoMetadata = publishDeviceInfoMetadata;
	}

	/**
	 * Get the datum queue.
	 *
	 * @return the queue
	 */
	public OptionalService<DatumQueue> getDatumQueue() {
		return datumQueue;
	}

	/**
	 * Set the datum queue.
	 *
	 * @param datumQueue
	 *        the queue to set
	 */
	public void setDatumQueue(OptionalService<DatumQueue> datumQueue) {
		this.datumQueue = datumQueue;
	}

	/**
	 * Get the datum service.
	 *
	 * @return the datum service
	 */
	public OptionalService<DatumService> getDatumService() {
		return datumService;
	}

	/**
	 * Set the datum service.
	 *
	 * @param datumService
	 *        the datum service
	 */
	public void setDatumService(OptionalService<DatumService> datumService) {
		this.datumService = datumService;
	}

	/**
	 * Get the tariff schedule providers.
	 *
	 * @return the providers
	 * @since 1.3
	 */
	public final OptionalServiceCollection<TariffScheduleProvider> getTariffScheduleProviders() {
		return tariffScheduleProviders;
	}

	/**
	 * Set the tariff schedule providers.
	 *
	 * @param tariffScheduleProviders
	 *        the providers to set
	 * @since 1.3
	 */
	public final void setTariffScheduleProviders(
			OptionalServiceCollection<TariffScheduleProvider> tariffScheduleProviders) {
		this.tariffScheduleProviders = tariffScheduleProviders;
	}

}
