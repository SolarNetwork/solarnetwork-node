/* ==================================================================
 * JsonDatumMetadataService.java - Oct 6, 2014 12:25:40 PM
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

package net.solarnetwork.node.metadata.json;

import static java.util.Collections.singleton;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.DigestUtils;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.node.DatumMetadataService;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.settings.SettingResourceHandler;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.SettingsService;
import net.solarnetwork.node.settings.SettingsUpdates;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.support.JsonHttpClientSupport;
import net.solarnetwork.settings.SettingsChangeObserver;

/**
 * JSON based web service implementation of {@link DatumMetadataService}.
 * 
 * <p>
 * This implementation caches the metadata for each source using
 * {@link SettingsService} using the {@link SettingResourceHandler} API, saving
 * one JSON resource per source ID.
 * </p>
 * 
 * @author matt
 * @version 1.5
 */
public class JsonDatumMetadataService extends JsonHttpClientSupport implements DatumMetadataService,
		SettingResourceHandler, SettingSpecifierProvider, SettingsChangeObserver, Runnable {

	/** The settings key for source metadata. */
	public static final String SETTING_KEY_SOURCE_META = "JsonDatumMetadataService.sourceMeta";

	/** The default {@code updateThrottleSeconds} property value. */
	public static final int DEFAULT_UPDATE_THROTTLE_SECONDS = 60;

	private static final Charset UTF8 = Charset.forName("UTF-8");

	private String baseUrl = "/api/v1/sec/datum/meta";

	private final SettingsService settingsService;
	private final TaskScheduler taskScheduler;
	private final ConcurrentMap<String, CachedMetadata> sourceMetadata;
	private SettingDao settingDao;
	private int updateThrottleSeconds = DEFAULT_UPDATE_THROTTLE_SECONDS;
	private ScheduledFuture<?> syncTask;

	/**
	 * Constructor.
	 * 
	 * @param settingsService
	 *        the settings service to use
	 * @param taskScheduler
	 *        the task scheduler to use
	 */
	public JsonDatumMetadataService(SettingsService settingsService, TaskScheduler taskScheduler) {
		super();
		this.settingsService = settingsService;
		this.taskScheduler = taskScheduler;
		this.sourceMetadata = new ConcurrentHashMap<>(8, 0.9f, 4);
	}

	private final class CachedMetadata {

		private final GeneralDatumMetadata metadata;
		private long lastChange = 0;
		private long lastSync = 0;

		private CachedMetadata(GeneralDatumMetadata metadata) {
			super();
			this.metadata = (metadata != null ? metadata : new GeneralDatumMetadata());
		}

		private boolean addMetadata(GeneralDatumMetadata newMetadata) {
			if ( newMetadata == null ) {
				return false;
			}
			boolean changed = false;
			if ( newMetadata.getM() != null ) {
				Map<String, Object> currM = metadata.getM();
				for ( Map.Entry<String, Object> me : newMetadata.getM().entrySet() ) {
					Object curr = (currM != null ? currM.get(me.getKey()) : null);
					if ( curr == null || !curr.equals(me.getValue()) ) {
						changed = true;
						break;
					}
				}
			}
			if ( !changed && newMetadata.getPm() != null ) {
				Map<String, Map<String, Object>> currPm = metadata.getPm();
				for ( Map.Entry<String, Map<String, Object>> me : newMetadata.getPm().entrySet() ) {
					Map<String, Object> curr = (currPm != null ? currPm.get(me.getKey()) : null);
					if ( curr == null || !curr.equals(me.getValue()) ) {
						changed = true;
						break;
					}
				}
			}
			if ( !changed && newMetadata.getT() != null
					&& !newMetadata.getT().equals(metadata.getT()) ) {
				changed = true;
			}
			if ( changed ) {
				metadata.merge(newMetadata, true);
				lastChange = System.currentTimeMillis();
			}
			return changed;
		}

		private void setSynced() {
			lastSync = System.currentTimeMillis();
		}

		private boolean needsSync() {
			return (lastChange - lastSync) > TimeUnit.SECONDS.toMillis(updateThrottleSeconds);
		}
	}

	/**
	 * Call once after properties have been configured.
	 */
	public void startup() {
		if ( syncTask != null ) {
			syncTask.cancel(false);
			syncTask = null;
		}
		if ( updateThrottleSeconds > 1 ) {
			syncTask = taskScheduler.scheduleWithFixedDelay(this,
					new Date(System.currentTimeMillis()
							+ TimeUnit.SECONDS.toMillis(updateThrottleSeconds)),
					TimeUnit.SECONDS.toMillis(updateThrottleSeconds));
		}
	}

	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		if ( properties == null ) {
			return;
		}
		startup();
	}

	@Override
	public void run() {
		// sync all metadata that requires it
		for ( Map.Entry<String, CachedMetadata> me : sourceMetadata.entrySet() ) {
			if ( me.getValue().needsSync() ) {
				syncMetadata(me.getKey(), me.getValue().metadata);
			}
		}
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.metadata.json.JsonDatumMetadataService";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<SettingSpecifier>();
		result.add(new BasicTextFieldSettingSpecifier("updateThrottleSeconds",
				String.valueOf(DEFAULT_UPDATE_THROTTLE_SECONDS)));
		return result;
	}

	@Override
	public Iterable<Resource> currentSettingResources(String settingKey) {
		// not supported
		return null;
	}

	@Override
	public SettingsUpdates applySettingResources(String settingKey, Iterable<Resource> resources)
			throws IOException {
		// not used
		return null;
	}

	private String nodeSourceMetadataUrl(String sourceId) {
		StringBuilder buf = new StringBuilder();
		appendXWWWFormURLEncodedValue(buf, "sourceId", sourceId);
		return (getIdentityService().getSolarInBaseUrl() + baseUrl + '/'
				+ getIdentityService().getNodeId() + '?' + buf);
	}

	@Override
	public GeneralDatumMetadata getSourceMetadata(String sourceId) {
		CachedMetadata cachedMetadata = getCachedMetadata(sourceId);
		GeneralDatumMetadata meta = cachedMetadata.metadata;
		return meta != null ? new GeneralDatumMetadata(meta) : null;
	}

	private CachedMetadata getCachedMetadata(String sourceId) {
		return sourceMetadata.computeIfAbsent(sourceId, k -> {
			GeneralDatumMetadata m = loadPersistedMetadata(sourceId);
			if ( m == null ) {
				m = fetchMetadata(sourceId);
			}
			return new CachedMetadata(m);
		});
	}

	private GeneralDatumMetadata fetchMetadata(String sourceId) {
		final String url = nodeSourceMetadataUrl(sourceId);
		try {
			final InputStream in = jsonGET(url);
			Collection<GeneralDatumMetadata> results = extractFilterResultsCollectionResponseData(in,
					GeneralDatumMetadata.class);
			if ( results != null && !results.isEmpty() ) {
				return results.iterator().next();
			}
			return null;
		} catch ( IOException e ) {
			if ( log.isTraceEnabled() ) {
				log.trace("IOException querying for source metadata at " + url, e);
			} else if ( log.isDebugEnabled() ) {
				log.debug("Unable to get source metadata: " + e.getMessage());
			}
			throw new RuntimeException(e);
		}
	}

	private GeneralDatumMetadata loadPersistedMetadata(String sourceId) {
		final String sourceKey = DigestUtils.md5DigestAsHex(sourceId.getBytes(UTF8));
		try {
			Iterable<Resource> resources = settingsService.getSettingResources(getSettingUID(), null,
					sourceKey);
			if ( resources != null ) {
				// use only first resource
				Iterator<Resource> itr = resources.iterator();
				if ( itr.hasNext() ) {
					Resource r = itr.next();
					if ( r != null ) {
						try (InputStream in = r.getInputStream()) {
							return getObjectMapper().readValue(in, GeneralDatumMetadata.class);
						}
					}
				}
			}
		} catch ( IOException e ) {
			log.debug("Error loading cached metadata for source {}: {}", sourceId, e.getMessage());
		}

		// check for legacy DAO data
		return legacyCachedMetadata(sourceId);
	}

	private GeneralDatumMetadata legacyCachedMetadata(String sourceId) {
		String json = settingDao.getSetting(SETTING_KEY_SOURCE_META, sourceId);
		if ( json != null ) {
			try {
				return getObjectMapper().readValue(json, GeneralDatumMetadata.class);
			} catch ( IOException e ) {
				log.debug("Error parsing cached metadata for source {}: {}", sourceId, e.getMessage());
			}
		}
		return null;
	}

	private synchronized void persistMetadataLocally(final String sourceId,
			final GeneralDatumMetadata meta) {
		try {
			final String sourceKey = DigestUtils.md5DigestAsHex(sourceId.getBytes(UTF8));
			byte[] json = getObjectMapper().writeValueAsBytes(meta);
			ByteArrayResource r = new ByteArrayResource(json, sourceId + " metadata");
			settingsService.importSettingResources(getSettingUID(), null, sourceKey, singleton(r));
		} catch ( IOException e ) {
			log.error("Error generating cached metadata JSON for source {}: {}", sourceId,
					e.getMessage());
		}
	}

	@Override
	public void addSourceMetadata(String sourceId, GeneralDatumMetadata meta) {
		log.debug("Adding metadata to source {}: {}", sourceId, meta.getPm());
		CachedMetadata cachedMetadata = getCachedMetadata(sourceId);
		if ( cachedMetadata.addMetadata(meta) && updateThrottleSeconds < 1 ) {
			syncMetadata(sourceId, cachedMetadata.metadata);
		} else if ( log.isDebugEnabled() ) {
			log.debug("Metadata has not changed for source {}", sourceId);
		}
	}

	private void syncMetadata(String sourceId, GeneralDatumMetadata metadata) {
		final GeneralDatumMetadata meta = new GeneralDatumMetadata(metadata);
		final String url = nodeSourceMetadataUrl(sourceId);
		log.info("Posting metadata for source {}", sourceId);
		try {
			final InputStream in = jsonPOST(url, meta);
			verifyResponseSuccess(in);
			persistMetadataLocally(sourceId, meta);
			CachedMetadata cachedMetadata = sourceMetadata.get(sourceId);
			if ( cachedMetadata != null ) {
				cachedMetadata.setSynced();
			}
		} catch ( IOException e ) {
			if ( log.isTraceEnabled() ) {
				log.trace("IOException posting source metadata at " + url, e);
			} else if ( log.isDebugEnabled() ) {
				log.debug("Unable to post data: " + e.getMessage());
			}
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the setting DAO.
	 * 
	 * @return the settingDao the setting DAO
	 */
	public SettingDao getSettingDao() {
		return settingDao;
	}

	/**
	 * Set the setting DAO.
	 * 
	 * <p>
	 * This is used for backwards compatibility in migrating cached metadata
	 * from the DAO to setting resources.
	 * </p>
	 * 
	 * @param settingDao
	 *        the settingDao to set
	 */
	public void setSettingDao(SettingDao settingDao) {
		this.settingDao = settingDao;
	}

	/**
	 * Get the SolarIn relative source-level metadata base URL path.
	 * 
	 * @return the metadata base URL path
	 */
	public String getBaseUrl() {
		return baseUrl;
	}

	/**
	 * Set the SolarIn relative source-level metadata base URL path.
	 * 
	 * @param baseUrl
	 *        the metadata base URL path to use
	 */
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	/**
	 * Get the update throttle seconds.
	 * 
	 * @return the seconds to coalesce updates to SolarNetwork by; defaults to
	 *         {@link #DEFAULT_UPDATE_THROTTLE_SECONDS}
	 * @since 1.5
	 */
	public int getUpdateThrottleSeconds() {
		return updateThrottleSeconds;
	}

	/**
	 * Set the update throttle seconds.
	 * 
	 * <p>
	 * When greater than {@literal 0} then limit the number of updates posted to
	 * SolarNetwork to at most this many seconds apart. This can be useful for
	 * data sources that capture data at high rates, where it would be
	 * impractical to post every update to SolarNetwork. Instead after this many
	 * seconds has passed since the last update, the current metadata value will
	 * be posted.
	 * </p>
	 * 
	 * @param updateThrottleSeconds
	 *        the seconds to coalesce updates to SolarNetwork by
	 * @since 1.5
	 */
	public void setUpdateThrottleSeconds(int updateThrottleSeconds) {
		this.updateThrottleSeconds = updateThrottleSeconds;
	}

}
