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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.TaskScheduler;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.node.DatumDataSource;
import net.solarnetwork.node.DatumMetadataService;
import net.solarnetwork.node.GeneralDatumSamplesTransformService;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.OptionalService;

/**
 * Helper class for {@link net.solarnetwork.node.DatumDataSource} and
 * {@link net.solarnetwork.node.MultiDatumDataSource} implementations to extend.
 * 
 * @author matt
 * @version 1.1
 * @since 1.51
 */
public class DatumDataSourceSupport extends BaseIdentifiable {

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
	private OptionalService<EventAdmin> eventAdmin;
	private TaskScheduler taskScheduler = null;
	private Long subSampleFrequency = null;
	private long subSampleStartDelay = DEFAULT_SUBSAMPLE_START_DELAY;
	private OptionalService<GeneralDatumSamplesTransformService> samplesTransformService;

	private ScheduledFuture<?> subSampleFuture;

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Post an {@link Event} for the
	 * {@link DatumDataSource#EVENT_TOPIC_DATUM_CAPTURED} topic.
	 * 
	 * @param datum
	 *        the datum that was stored
	 */
	protected final void postDatumCapturedEvent(Datum datum) {
		if ( datum == null ) {
			return;
		}
		Event event = createDatumCapturedEvent(datum);
		postEvent(event);
	}

	/**
	 * Create a new {@link DatumDataSource#EVENT_TOPIC_DATUM_CAPTURED}
	 * {@link Event} object out of a {@link Datum}.
	 * 
	 * <p>
	 * This method uses the result of {@link Datum#asSimpleMap()} as the event
	 * properties.
	 * </p>
	 * 
	 * @param datum
	 *        the datum to create the event for
	 * @return the new Event instance
	 */
	protected Event createDatumCapturedEvent(Datum datum) {
		Map<String, ?> props = datum.asSimpleMap();
		log.debug("Created {} event with props {}", DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED, props);
		return new Event(DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED, props);
	}

	/**
	 * Post an {@link Event}.
	 * 
	 * <p>
	 * This method only works if a {@link EventAdmin} has been configured via
	 * {@link #setEventAdmin(OptionalService)}. Otherwise the event is silently
	 * ignored.
	 * </p>
	 * 
	 * @param event
	 *        the event to post
	 */
	protected final void postEvent(Event event) {
		EventAdmin ea = (eventAdmin == null ? null : eventAdmin.service());
		if ( ea == null || event == null ) {
			return;
		}
		ea.postEvent(event);
	}

	/**
	 * Add source metadata using the configured {@link DatumMetadataService} (if
	 * available). The metadata will be cached so that subsequent calls to this
	 * method with the same metadata value will not try to re-save the unchanged
	 * value. This method will catch all exceptions and silently discard them.
	 * 
	 * @param sourceId
	 *        the source ID to add metadata to
	 * @param meta
	 *        the metadata to add
	 * @return {@literal true} if the metadata was saved successfully, or does
	 *         not need to be updated
	 */
	protected boolean addSourceMetadata(final String sourceId, final GeneralDatumMetadata meta) {
		GeneralDatumMetadata cached = SOURCE_METADATA_CACHE.get(sourceId);
		if ( cached != null && meta.equals(cached) ) {
			// we've already posted this metadata... don't bother doing it again
			log.debug("Source {} metadata already added, not posting again", sourceId);
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
			service.addSourceMetadata(sourceId, meta);
			SOURCE_METADATA_CACHE.put(sourceId, meta);
			return true;
		} catch ( Exception e ) {
			log.warn("Error saving source {} metadata: {}", sourceId, e.getMessage());
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
	 * @param datum
	 *        the datum to possibly filter
	 * @param props
	 *        optional transform properties to pass to
	 *        {@link GeneralDatumSamplesTransformService#transformSamples(Datum, GeneralDatumSamples, Map)}
	 * @return the same datum, possibly transformed, or {@literal null} if the
	 *         datum has been filtered out completely
	 * @since 1.1
	 */
	protected GeneralNodeDatum applySamplesTransformer(GeneralNodeDatum datum, Map<String, ?> props) {
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
	 * Get the {@link EventAdmin} service.
	 * 
	 * @return the EventAdmin service
	 */
	public OptionalService<EventAdmin> getEventAdmin() {
		return eventAdmin;
	}

	/**
	 * Set an {@link EventAdmin} service to use.
	 * 
	 * @param eventAdmin
	 *        the EventAdmin to use
	 */
	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
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

}
