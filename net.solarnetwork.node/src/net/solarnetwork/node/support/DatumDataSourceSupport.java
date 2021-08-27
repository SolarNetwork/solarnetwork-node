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

package net.solarnetwork.node.support;

import static net.solarnetwork.util.OptionalService.service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.DatumMetadataService;
import net.solarnetwork.node.DatumQueue;
import net.solarnetwork.node.GeneralDatumSamplesTransformService;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.ExpressionConfig;
import net.solarnetwork.node.domain.ExpressionRoot;
import net.solarnetwork.node.domain.GeneralDatum;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicToggleSettingSpecifier;
import net.solarnetwork.support.ExpressionService;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.OptionalServiceCollection;

/**
 * Helper class for {@link net.solarnetwork.node.DatumDataSource} and
 * {@link net.solarnetwork.node.MultiDatumDataSource} implementations to extend.
 * 
 * @author matt
 * @version 1.8
 * @since 1.51
 */
public class DatumDataSourceSupport extends BaseIdentifiable implements DatumEvents {

	/**
	 * A transform properties instance that can be used to signal "sub-sampling"
	 * mode to the transform service.2
	 * 
	 * @since 1.1
	 */
	public static final Map<String, Object> SUB_SAMPLE_PROPS = Collections.singletonMap("subsample",
			Boolean.TRUE);

	/**
	 * A global cache of source-based metadata, so only changes to metadata need
	 * be posted.
	 */
	private static final ConcurrentMap<String, GeneralDatumMetadata> SOURCE_METADATA_CACHE = new ConcurrentHashMap<String, GeneralDatumMetadata>(
			4);

	/** The {@code subSampleStartDelay} property default value. */
	public static final long DEFAULT_SUBSAMPLE_START_DELAY = 15000L;

	private OptionalService<DatumMetadataService> datumMetadataService;
	private OptionalService<DatumQueue> datumQueue;
	private TaskScheduler taskScheduler = null;
	private Long subSampleFrequency = null;
	private long subSampleStartDelay = DEFAULT_SUBSAMPLE_START_DELAY;
	private OptionalService<GeneralDatumSamplesTransformService> samplesTransformService;
	private ExpressionConfig[] expressionConfigs;
	private boolean publishDeviceInfoMetadata = false;

	private ScheduledFuture<?> subSampleFuture;

	/**
	 * Offer a non-persisted datum event to the configured {@link DatumQueue},
	 * if available.
	 * 
	 * @param datum
	 *        the datum that was captured
	 * @since 1.8
	 */
	protected final void offerDatumCapturedEvent(Datum datum) {
		if ( datum instanceof GeneralDatum ) {
			final DatumQueue queue = service(datumQueue);
			if ( queue != null ) {
				// offer datum to queue instead of posting as event, as we expect the queue to emit the event
				queue.offer((GeneralDatum) datum, false);
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
	 * Get setting specifiers for the {@code uid} and {@code groupUID}
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
	 * @since 1.1
	 */
	protected List<SettingSpecifier> getSubSampleSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<SettingSpecifier>(16);
		results.add(new BasicTextFieldSettingSpecifier("samplesTransformService.propertyFilters['UID']",
				null));
		results.add(new BasicTextFieldSettingSpecifier("subSampleFrequency", null));
		results.add(new BasicTextFieldSettingSpecifier("subSampleStartDelay",
				String.valueOf(DEFAULT_SUBSAMPLE_START_DELAY)));
		return results;
	}

	/**
	 * Get setting specifiers for device info metadata publishing support.
	 * 
	 * @return list of settings
	 * @since 1.7
	 */
	protected List<SettingSpecifier> getDeviceInfoMetadataSettingSpecifiers() {
		return Collections.singletonList(
				new BasicToggleSettingSpecifier("publishDeviceInfoMetadata", Boolean.FALSE));
	}

	/**
	 * Schedule sub-sampling with the given data source.
	 * 
	 * @param dataSource
	 *        the data source
	 * @return the scheduled future or {@literal null} if sub-sampling support
	 *         is not configured; canceling this will stop the sub-sampling task
	 * @see #stopSubSampling()
	 * @since 1.1
	 */
	protected synchronized ScheduledFuture<?> startSubSampling(
			DatumDataSource<? extends GeneralNodeDatum> dataSource) {
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
	 * @since 1.1
	 */
	protected void readSubSampleDatum(DatumDataSource<? extends GeneralNodeDatum> dataSource) {
		GeneralNodeDatum datum = dataSource.readCurrentDatum();
		log.debug("Got sub-sample datum: {}", datum);
	}

	/**
	 * Stop any running sub-sampling task.
	 * 
	 * @see DatumDataSourceSupport#startSubSampling(DatumDataSource)
	 * @since 1.1
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
	 * @since 1.1
	 */
	protected boolean isSubSampling() {
		ScheduledFuture<?> f = this.subSampleFuture;
		return f != null && !f.isDone();
	}

	/**
	 * Apply the configured samples transformer service to a given datum.
	 * 
	 * @param <T>
	 *        the type of datum
	 * @param datum
	 *        the datum to possibly filter
	 * @param props
	 *        optional transform properties to pass to
	 *        {@link GeneralDatumSamplesTransformService#transformSamples(Datum, GeneralDatumSamples, Map)}
	 * @return the same datum, possibly transformed, or {@literal null} if the
	 *         datum has been filtered out completely
	 * @since 1.1
	 */
	protected <T extends GeneralNodeDatum> T applySamplesTransformer(T datum,
			Map<String, Object> props) {
		GeneralDatumSamplesTransformService xformService = OptionalService
				.service(getSamplesTransformService());
		if ( xformService != null ) {
			GeneralDatumSamples out = xformService.transformSamples(datum, datum.getSamples(), props);
			if ( out == null ) {
				return null;
			} else if ( out != datum.getSamples() ) {
				datum.setSamples(out);
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
	 * {@link #populateExpressionDatumProperties(GeneralNodeDatum, ExpressionConfig[], Object)}.
	 * </p>
	 * 
	 * @param d
	 *        the datum to store the results of expression evaluations on
	 * @param expressionConfs
	 *        the expression configurations
	 * @see #populateExpressionDatumProperties(GeneralNodeDatum,
	 *      ExpressionConfig[], Object)
	 * @since 1.3
	 */
	protected void populateExpressionDatumProperties(final GeneralNodeDatum d,
			final ExpressionConfig[] expressionConfs) {
		populateExpressionDatumProperties(d, expressionConfs, new ExpressionRoot(d));
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
	 * @since 1.3
	 */
	protected void populateExpressionDatumProperties(final GeneralNodeDatum d,
			final ExpressionConfig[] expressionConfs, final Object root) {
		super.populateExpressionDatumProperties(d.asMutableSampleOperations(), expressionConfs, root);
	}

	@Override
	public String getUID() {
		return super.getUID();
	}

	@Override
	public String getUid() {
		return super.getUid();
	}

	@Override
	public void setUid(String uid) {
		super.setUid(uid);
	}

	@Override
	public String getGroupUID() {
		return super.getGroupUID();
	}

	@Override
	public void setGroupUID(String groupUID) {
		super.setGroupUID(groupUID);
	}

	/**
	 * Set an event admin service to use.
	 * 
	 * @param eventAdmin
	 *        the service to use
	 * @deprecated since 1.8 this is ignored
	 */
	@Deprecated
	public void setEventAdmin(OptionalService<?> eventAdmin) {
		throw new UnsupportedOperationException();
	}

	@Override
	public MessageSource getMessageSource() {
		return super.getMessageSource();
	}

	@Override
	public void setMessageSource(MessageSource messageSource) {
		super.setMessageSource(messageSource);
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
	 * @since 1.1
	 */
	public TaskScheduler getTaskScheduler() {
		return taskScheduler;
	}

	/**
	 * Set the task scheduler.
	 * 
	 * @param taskScheduler
	 *        the task scheduler to set
	 * @since 1.1
	 */
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	/**
	 * Get a sub-sample frequency at which to request datum.
	 * 
	 * @return the sub-sample frequency, in milliseconds, or {@literal null} or
	 *         anything less than {@literal 1} to disable sub-sampling
	 * @since 1.1
	 */
	public Long getSubSampleFrequency() {
		return subSampleFrequency;
	}

	/**
	 * Set a sub-sample frequency at which to request datum.
	 * 
	 * <p>
	 * This is designed to work with a
	 * {@link #setSamplesTransformService(OptionalService)} transformer that
	 * performs down-sampling of higher frequency data.
	 * 
	 * @param subSampleFrequency
	 *        the frequency, to set, in milliseconds
	 * @since 1.1
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
	 * @since 1.1
	 */
	public void setSubSampleStartDelay(long subSampleStartDelay) {
		this.subSampleStartDelay = subSampleStartDelay;
	}

	/**
	 * Get a samples transformer to use.
	 * 
	 * @return the service
	 * @since 1.1
	 */
	public OptionalService<GeneralDatumSamplesTransformService> getSamplesTransformService() {
		return samplesTransformService;
	}

	/**
	 * Set a samples transformer to use.
	 * 
	 * @param samplesTransformService
	 *        the service to set
	 * @since 1.1
	 */
	public void setSamplesTransformService(
			OptionalService<GeneralDatumSamplesTransformService> samplesTransformService) {
		this.samplesTransformService = samplesTransformService;
	}

	/**
	 * Get the expression configurations.
	 * 
	 * @return the expression configurations
	 * @since 1.3
	 */
	public ExpressionConfig[] getExpressionConfigs() {
		return expressionConfigs;
	}

	/**
	 * Set the expression configurations to use.
	 * 
	 * @param expressionConfigs
	 *        the configs to use
	 * @since 1.3
	 */
	public void setExpressionConfigs(ExpressionConfig[] expressionConfigs) {
		this.expressionConfigs = expressionConfigs;
	}

	/**
	 * Get the number of configured {@code expressionConfigs} elements.
	 * 
	 * @return the number of {@code expressionConfigs} elements
	 * @since 1.3
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
	 * @since 1.5
	 */
	public void setExpressionConfigsCount(int count) {
		this.expressionConfigs = ArrayUtils.arrayWithLength(this.expressionConfigs, count,
				ExpressionConfig.class, null);
	}

	/**
	 * Get an optional collection of {@link ExpressionService}.
	 * 
	 * @return the optional {@link ExpressionService} collection to use
	 * @since 1.3
	 */
	@Override
	public OptionalServiceCollection<ExpressionService> getExpressionServices() {
		return super.getExpressionServices();
	}

	/**
	 * Configure an optional collection of {@link ExpressionService}.
	 * 
	 * <p>
	 * Configuring these services allows expressions to be defined to calculate
	 * dynamic datum property values at runtime.
	 * </p>
	 * 
	 * @param expressionServices
	 *        the optional {@link ExpressionService} collection to use
	 * @since 1.3
	 */
	@Override
	public void setExpressionServices(OptionalServiceCollection<ExpressionService> expressionServices) {
		super.setExpressionServices(expressionServices);
	}

	/**
	 * Get the desired device info metadata publish mode.
	 * 
	 * @return {@literal true} to publish device metadata
	 * @since 1.7
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
	 * @since 1.7
	 */
	public void setPublishDeviceInfoMetadata(boolean publishDeviceInfoMetadata) {
		this.publishDeviceInfoMetadata = publishDeviceInfoMetadata;
	}

	/**
	 * Get the datum queue.
	 * 
	 * @return the queue
	 * @since 1.8
	 */
	public OptionalService<DatumQueue> getDatumQueue() {
		return datumQueue;
	}

	/**
	 * Set the datum queue.
	 * 
	 * @param datumQueue
	 *        the queue to set
	 * @since 1.8
	 */
	public void setDatumQueue(OptionalService<DatumQueue> datumQueue) {
		this.datumQueue = datumQueue;
	}

}
