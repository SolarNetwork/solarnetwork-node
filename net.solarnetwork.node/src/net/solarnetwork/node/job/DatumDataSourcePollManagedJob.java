/* ==================================================================
 * DatumDataSourceManagedLoggerJob.java - Aug 26, 2014 2:44:36 PM
 *
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.job;

import static net.solarnetwork.service.OptionalService.service;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import org.springframework.context.MessageSource;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.DeviceInfo;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.DatumMetadataService;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.node.service.DeviceInfoProvider;
import net.solarnetwork.node.service.LockTimeoutException;
import net.solarnetwork.node.service.MultiDatumDataSource;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.settings.MappableSpecifier;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;

/**
 * Job to poll a {@link DatumDataSource} or {@link MultiDatumDataSource} for
 * datum and offer them to a {@link DatumQueue}.
 *
 * <p>
 * The multi-datum data source will be used if configured. Otherwise the datum
 * data source will be used. This class implements
 * {@link SettingSpecifierProvider} but delegates that API to the configured
 * data source.
 * </p>
 *
 * @author matt
 * @version 1.1
 * @since 2.0
 */
public class DatumDataSourcePollManagedJob extends BaseIdentifiable
		implements JobService, SettingsChangeObserver, ServiceLifecycleObserver {

	// a static concurrent set to tack the publication of device infos
	private static final Set<String> PUBLISHED_DEVICE_INFO_SOURCE_IDS = new ConcurrentSkipListSet<>();

	private DatumDataSource datumDataSource = null;
	private MultiDatumDataSource multiDatumDataSource = null;
	private OptionalService<DatumQueue> datumQueue = null;
	private OptionalService<DatumMetadataService> datumMetadataService = null;

	/**
	 * Default constructor.
	 */
	public DatumDataSourcePollManagedJob() {
		super();
	}

	@Override
	public void executeJobService() throws Exception {
		try {
			if ( multiDatumDataSource != null ) {
				executeForMultiDatumDataSource();
			} else {
				executeForDatumDataSource();
			}
		} catch ( Throwable e ) {
			logThrowable(e);
		}
	}

	@Override
	public String toString() {
		Object delegate = multiDatumDataSource != null ? multiDatumDataSource : datumDataSource;
		return String.format("DatumDataSourcePollManagedJob{%s}", delegate);
	}

	/**
	 * Handle service startup.
	 *
	 * <p>
	 * This method will delegate to the configured {@code datumDataSource} or
	 * {@code multiDatumDataSource} properties if they also implement
	 * {@link ServiceLifecycleObserver}.
	 * </p>
	 *
	 * {@inheritDoc}
	 */
	@Override
	public void serviceDidStartup() {
		ServiceLifecycleObserver delegate = serviceLifecycleObserver();
		if ( delegate != null ) {
			delegate.serviceDidStartup();
		}
	}

	/**
	 * Handle service shutdown.
	 *
	 * <p>
	 * This method will delegate to the configured {@code datumDataSource} or
	 * {@code multiDatumDataSource} properties if they also implement
	 * {@link ServiceLifecycleObserver}.
	 * </p>
	 *
	 * {@inheritDoc}
	 */
	@Override
	public void serviceDidShutdown() {
		ServiceLifecycleObserver delegate = serviceLifecycleObserver();
		if ( delegate != null ) {
			delegate.serviceDidShutdown();
		}
	}

	/**
	 * Handle configuration changes.
	 *
	 * <p>
	 * This method will delegate to the configured {@code datumDataSource} or
	 * {@code multiDatumDataSource} properties if they also implement
	 * {@link SettingsChangeObserver}.
	 * </p>
	 *
	 * {@inheritDoc}
	 */
	@Override
	public void configurationChanged(Map<String, Object> properties) {
		if ( datumDataSource instanceof SettingsChangeObserver ) {
			((SettingsChangeObserver) datumDataSource).configurationChanged(properties);
		}
		if ( multiDatumDataSource instanceof SettingsChangeObserver ) {
			((SettingsChangeObserver) multiDatumDataSource).configurationChanged(properties);
		}
	}

	private void executeForDatumDataSource() {
		if ( log.isDebugEnabled() ) {
			log.debug("Collecting [{}] from [{}]", datumDataSource.getDatumType().getSimpleName(),
					datumDataSource);
		}
		NodeDatum datum = datumDataSource.readCurrentDatum();
		if ( datum != null ) {
			persistDatum(datumDataSource, Collections.singleton(datum));
		} else {
			log.debug("No data returned from [{}]", datumDataSource);
		}
	}

	private void executeForMultiDatumDataSource() {
		if ( log.isDebugEnabled() ) {
			log.debug("Collecting [{}] from [{}]",
					multiDatumDataSource.getMultiDatumType().getSimpleName(), multiDatumDataSource);
		}
		Collection<NodeDatum> datum = multiDatumDataSource.readMultipleDatum();
		if ( datum != null && datum.size() > 0 ) {
			persistDatum(multiDatumDataSource, datum);
		} else {
			log.debug("No data returned from [{}]", multiDatumDataSource);
		}
	}

	private void logThrowable(Throwable e) {
		Throwable ioEx = null;
		Throwable t = e;
		while ( true ) {
			if ( t instanceof IOException || t instanceof LockTimeoutException ) {
				ioEx = t;
				break;
			}
			if ( t.getCause() == null ) {
				break;
			}
			t = t.getCause();
		}
		if ( ioEx != null ) {
			log.warn("Communication problem collecting data from {}: {}",
					multiDatumDataSource != null ? multiDatumDataSource : datumDataSource,
					ioEx.toString());
		} else {
			log.error("Error collecting data from {}: {}",
					multiDatumDataSource != null ? multiDatumDataSource : datumDataSource, t.toString(),
					t);
		}
	}

	private void persistDatum(DeviceInfoProvider infoProvider, Collection<NodeDatum> datumList) {
		if ( datumList == null || datumList.size() < 1 ) {
			return;
		}
		if ( log.isDebugEnabled() ) {
			log.debug("Got Datum to persist: {}",
					(datumList.size() == 1 ? datumList.iterator().next().toString()
							: datumList.toString()));
		}
		DatumQueue queue = OptionalService.service(datumQueue);
		if ( queue == null ) {
			log.info("No DatumQueue available to offer {}, not saving", datumList);
			return;
		}
		for ( NodeDatum datum : datumList ) {
			queue.offer(datum, true);
			log.debug("Offered datum {} to queue", datum);
		}
		if ( infoProvider.canPublishDeviceInfo() ) {
			publishDeviceInfoMetadata(infoProvider);
		}
	}

	private void publishDeviceInfoMetadata(DeviceInfoProvider infoProvider) {
		final String metaSourceId = infoProvider.deviceInfoSourceId();
		if ( metaSourceId == null ) {
			return;
		}
		final DatumMetadataService metadataService = service(datumMetadataService);
		if ( metadataService == null ) {
			return;
		}
		if ( PUBLISHED_DEVICE_INFO_SOURCE_IDS.contains(metaSourceId) ) {
			return;
		}
		DeviceInfo info = infoProvider.deviceInfo();
		Map<String, Object> m = JsonUtils.getStringMapFromObject(info);
		if ( m != null && !m.isEmpty() ) {
			GeneralDatumMetadata meta = new GeneralDatumMetadata(null,
					Collections.singletonMap(DeviceInfo.DEVICE_INFO_METADATA_KEY, m));
			metadataService.addSourceMetadata(metaSourceId, meta);
		}
		PUBLISHED_DEVICE_INFO_SOURCE_IDS.add(metaSourceId);
	}

	private SettingSpecifierProvider settingSpecifierProvider() {
		if ( multiDatumDataSource instanceof SettingSpecifierProvider ) {
			return (SettingSpecifierProvider) multiDatumDataSource;
		}
		if ( datumDataSource instanceof SettingSpecifierProvider ) {
			return (SettingSpecifierProvider) datumDataSource;
		}
		return null;
	}

	private ServiceLifecycleObserver serviceLifecycleObserver() {
		if ( multiDatumDataSource instanceof ServiceLifecycleObserver ) {
			return (ServiceLifecycleObserver) multiDatumDataSource;
		}
		if ( datumDataSource instanceof ServiceLifecycleObserver ) {
			return (ServiceLifecycleObserver) datumDataSource;
		}
		return null;
	}

	@Override
	public String getSettingUid() {
		SettingSpecifierProvider delegate = settingSpecifierProvider();
		if ( delegate != null ) {
			return delegate.getSettingUid();
		}
		return getDatumDataSource().getClass().getName();
	}

	@Override
	public String getDisplayName() {
		SettingSpecifierProvider delegate = settingSpecifierProvider();
		if ( delegate != null ) {
			return delegate.getDisplayName();
		}
		return null;
	}

	@Override
	public MessageSource getMessageSource() {
		SettingSpecifierProvider delegate = settingSpecifierProvider();
		if ( delegate != null ) {
			return delegate.getMessageSource();
		}
		return super.getMessageSource();
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		SettingSpecifierProvider delegate = settingSpecifierProvider();
		if ( delegate == null ) {
			return Collections.emptyList();
		}
		List<SettingSpecifier> result = new ArrayList<>();
		final String prefix = (multiDatumDataSource != null ? "multiDatumDataSource."
				: "datumDataSource.");
		for ( SettingSpecifier spec : delegate.getSettingSpecifiers() ) {
			if ( spec instanceof MappableSpecifier ) {
				MappableSpecifier mappable = (MappableSpecifier) spec;
				result.add(mappable.mappedTo(prefix));
			} else {
				result.add(spec);
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T unwrap(Class<T> type) {
		T result = JobService.super.unwrap(type);
		if ( result != null ) {
			return result;
		}
		if ( multiDatumDataSource != null && type.isAssignableFrom(multiDatumDataSource.getClass()) ) {
			result = (T) multiDatumDataSource;
		} else if ( datumDataSource != null && type.isAssignableFrom(datumDataSource.getClass()) ) {
			result = (T) datumDataSource;
		} else {
			SettingSpecifierProvider delegate = settingSpecifierProvider();
			if ( delegate != null ) {
				result = delegate.unwrap(type);
			}
		}
		return result;
	}

	/**
	 * Get the datum data source.
	 *
	 * @return the data source
	 */
	public DatumDataSource getDatumDataSource() {
		return datumDataSource;
	}

	/**
	 * Set the datum data source.
	 *
	 * @param datumDataSource
	 *        the data source
	 */
	public void setDatumDataSource(DatumDataSource datumDataSource) {
		this.datumDataSource = datumDataSource;
	}

	/**
	 * Get the multi-datum data source.
	 *
	 * @return the data source
	 */
	public MultiDatumDataSource getMultiDatumDataSource() {
		return multiDatumDataSource;
	}

	/**
	 * Set the multi-datum data source.
	 *
	 * @param multiDatumDataSource
	 *        the data source
	 */
	public void setMultiDatumDataSource(MultiDatumDataSource multiDatumDataSource) {
		this.multiDatumDataSource = multiDatumDataSource;
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
	 *        the queue
	 */
	public void setDatumQueue(OptionalService<DatumQueue> datumQueue) {
		this.datumQueue = datumQueue;
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

}
