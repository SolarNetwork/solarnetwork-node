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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singleton;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.DigestUtils;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.DatumMetadataService;
import net.solarnetwork.node.service.support.JsonHttpClientSupport;
import net.solarnetwork.node.settings.SettingResourceHandler;
import net.solarnetwork.node.settings.SettingsService;
import net.solarnetwork.node.settings.SettingsUpdates;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.util.CachedResult;
import net.solarnetwork.util.StringUtils;

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
 * @version 2.4
 */
public class JsonDatumMetadataService extends JsonHttpClientSupport
		implements DatumMetadataService, SettingResourceHandler, SettingSpecifierProvider,
		SettingsChangeObserver, InstructionHandler, Runnable {

	/** The settings key for source metadata. */
	public static final String SETTING_KEY_SOURCE_META = "JsonDatumMetadataService.sourceMeta";

	/** The default {@code updateThrottleSeconds} property value. */
	public static final int DEFAULT_UPDATE_THROTTLE_SECONDS = 60;

	/** The default {@code updatePersistDelaySeconds} property value. */
	public static final int DEFAULT_UPDATE_PERSIST_DELAY_SECONDS = 2;

	/**
	 * The default {@code datumStreamMetadataCacheSeconds} property value.
	 *
	 * @since 1.7
	 */
	public static final int DEFATUL_DATUM_STREAM_METADATA_CACHE_SECONDS = 86400;

	/**
	 * The instruction topic to clear locally cached datum source metadata.
	 *
	 * @since 2.3
	 */
	public static final String DATUM_SOURCE_METADATA_CACHE_CLEAR_TOPIC = "DatumSourceMetadataClearCache";

	/**
	 * The instruction parameter for an optional comma-delimited list of source
	 * IDs.
	 *
	 * <p>
	 * If not provided, then "all available" source IDs is assumed.
	 * </p>
	 *
	 * @since 2.3
	 */
	public static final String SOURCE_IDS_PARAM = "sourceIds";

	private String baseUrl = "/api/v1/sec/datum/meta";

	private final SettingsService settingsService;
	private final TaskScheduler taskScheduler;
	private final ConcurrentMap<String, CachedMetadata> sourceMetadata;
	private final ConcurrentMap<ObjectDatumStreamMetadataId, CachedResult<ObjectDatumStreamMetadata>> datumStreamMetadata;
	private SettingDao settingDao;
	private int updateThrottleSeconds = DEFAULT_UPDATE_THROTTLE_SECONDS;
	private int updatePersistDelaySeconds = DEFAULT_UPDATE_PERSIST_DELAY_SECONDS;
	private ScheduledFuture<?> syncTask;
	private int datumStreamMetadataCacheSeconds = DEFATUL_DATUM_STREAM_METADATA_CACHE_SECONDS;

	/**
	 * Constructor.
	 *
	 * @param settingsService
	 *        the settings service to use
	 * @param taskScheduler
	 *        the task scheduler to use
	 */
	public JsonDatumMetadataService(SettingsService settingsService, TaskScheduler taskScheduler) {
		this(settingsService, taskScheduler, null, null);
	}

	/**
	 * Constructor.
	 *
	 * @param settingsService
	 *        the settings service to use
	 * @param taskScheduler
	 *        the task scheduler to use
	 * @param sourceMetadata
	 *        the source metadata cache to use
	 * @param datumStreamMetadata
	 *        the stream metadata cache to use
	 * @since 1.7
	 */
	public JsonDatumMetadataService(SettingsService settingsService, TaskScheduler taskScheduler,
			ConcurrentMap<String, CachedMetadata> sourceMetadata,
			ConcurrentMap<ObjectDatumStreamMetadataId, CachedResult<ObjectDatumStreamMetadata>> datumStreamMetadata) {
		super();
		this.settingsService = settingsService;
		this.taskScheduler = taskScheduler;
		this.sourceMetadata = (sourceMetadata != null ? sourceMetadata
				: new ConcurrentHashMap<>(8, 0.9f, 4));
		this.datumStreamMetadata = (datumStreamMetadata != null ? datumStreamMetadata
				: new ConcurrentHashMap<>(8, 0.9f, 4));
	}

	@Override
	public Set<String> availableSourceMetadataSourceIds() {
		Set<String> sourceIds = sourceMetadata.keySet();
		log.debug("Available source metadata sourceIds: {}", sourceIds);
		return sourceIds;
	}

	/**
	 * Create a new cached metadata instance.
	 *
	 * <p>
	 * This method is designed to support unit tests.
	 * </p>
	 *
	 * @param sourceId
	 *        the source ID
	 * @param meta
	 *        the metadata
	 * @return the cached metadata
	 */
	public CachedMetadata createCachedMetadata(String sourceId, GeneralDatumMetadata meta) {
		return new CachedMetadata(sourceId, meta);
	}

	@Override
	public boolean handlesTopic(String topic) {
		return DATUM_SOURCE_METADATA_CACHE_CLEAR_TOPIC.equalsIgnoreCase(topic);
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
		if ( instruction == null || !handlesTopic(instruction.getTopic()) ) {
			return null;
		}
		final String sourceIds = instruction.getParameterValue(SOURCE_IDS_PARAM);
		if ( sourceIds == null || sourceIds.isEmpty() ) {
			return InstructionUtils.createStatus(instruction, InstructionState.Declined, InstructionUtils
					.createErrorResultParameters("No sourceIds parameter provided,", "JDMS.0001"));
		}
		final Set<String> sources = StringUtils.commaDelimitedStringToSet(sourceIds);
		for ( String sourceId : sources ) {
			CachedMetadata m = sourceMetadata.remove(sourceId);
			if ( m != null ) {
				synchronized ( m ) {
					removePersistedMetadata(sourceId);
				}
			} else {
				removePersistedMetadata(sourceId);
			}
		}
		return InstructionUtils.createStatus(instruction, InstructionState.Completed);
	}

	/**
	 * Internal class ued for cached metadata.
	 */
	public final class CachedMetadata implements Runnable {

		private final String sourceId;
		private final GeneralDatumMetadata metadata;
		private long lastChange = 0;
		private long lastSync = 0;
		private long lastPersist = 0;
		private ScheduledFuture<?> persistTask;

		private CachedMetadata(String sourceId, GeneralDatumMetadata metadata) {
			super();
			this.sourceId = sourceId;
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
				if ( updatePersistDelaySeconds > 0 ) {
					persistLaterUnlessUpdated(updatePersistDelaySeconds);
				} else {
					persistMetadataLocally(new GeneralDatumMetadata(metadata), lastChange);
				}
			}
			return changed;
		}

		private void setSynced(long timestamp) {
			lastSync = timestamp;
		}

		private void setPersisted(long timestamp) {
			lastPersist = timestamp;
		}

		private boolean needsSync() {
			return (lastChange - lastSync) > TimeUnit.SECONDS.toMillis(updateThrottleSeconds);
		}

		private boolean needsPersist() {
			return (lastChange - lastPersist) > TimeUnit.SECONDS.toMillis(updatePersistDelaySeconds);
		}

		private void persistLaterUnlessUpdated(int delaySeconds) {
			if ( persistTask != null && !persistTask.isDone() ) {
				persistTask.cancel(false);
			}
			persistTask = taskScheduler.schedule(this,
					Instant.now().truncatedTo(ChronoUnit.MILLIS).plusSeconds(delaySeconds));
		}

		@Override
		public void run() {
			// sync all metadata that requires it
			synchronized ( this ) {
				if ( needsPersist() ) {
					long timestamp = System.currentTimeMillis();
					// create thread-safe copy
					GeneralDatumMetadata metaToPersist = new GeneralDatumMetadata(metadata);
					persistMetadataLocally(metaToPersist, timestamp);
				}
			}
		}

		private synchronized void persistMetadataLocally(final GeneralDatumMetadata meta,
				final long timestamp) {
			try {
				final String sourceKey = DigestUtils.md5DigestAsHex(sourceId.getBytes(UTF_8));
				byte[] json = getObjectMapper().writeValueAsBytes(meta);
				ByteArrayResource r = new ByteArrayResource(json, sourceId + " metadata");
				settingsService.importSettingResources(getSettingUid(), null, sourceKey, singleton(r));
				setPersisted(timestamp);
			} catch ( IOException e ) {
				log.error("Error generating cached metadata JSON for source {}: {}", sourceId,
						e.getMessage());
			}
		}

	}

	/**
	 * Call once after properties have been configured.
	 */
	public synchronized void startup() {
		boolean cancelled = false;
		if ( syncTask != null ) {
			syncTask.cancel(true);
			syncTask = null;
			cancelled = true;
		}
		if ( updateThrottleSeconds > 1 ) {
			log.info("{}cheduling metdata synchroniztion at {} seconds", (cancelled ? "Res" : "S"),
					updateThrottleSeconds);
			syncTask = taskScheduler.scheduleWithFixedDelay(this,
					Instant.now().truncatedTo(ChronoUnit.MILLIS)
							.plusSeconds(TimeUnit.SECONDS.toMillis(updateThrottleSeconds)),
					Duration.ofSeconds(updateThrottleSeconds));
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
			CachedMetadata m = me.getValue();
			GeneralDatumMetadata metaToSync = null;
			long timestamp = 0;
			synchronized ( m ) {
				if ( m.needsSync() || m.needsPersist() ) {
					// create thread-safe copy
					metaToSync = new GeneralDatumMetadata(m.metadata);
					timestamp = System.currentTimeMillis();
				}
			}
			if ( metaToSync != null ) {
				if ( m.needsPersist() ) {
					m.persistMetadataLocally(metaToSync, timestamp);
				}
				try {
					syncMetadata(me.getKey(), metaToSync, timestamp);
				} catch ( IOException e ) {
					log.warn("Communication error synchronizing datum metadata for [{}]: {}",
							me.getKey(), e.toString());
				} catch ( Exception e ) {
					log.error("Error synchronizing datum metadata for [{}]: {}", me.getKey(),
							e.toString(), e);
				}
			}
		}
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.metadata.json.JsonDatumMetadataService";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<SettingSpecifier>();
		result.add(new BasicTextFieldSettingSpecifier("updatePersistDelaySeconds",
				String.valueOf(DEFAULT_UPDATE_PERSIST_DELAY_SECONDS)));
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
		log.debug("Source metadata for [{}]: {}", sourceId, meta);
		return meta != null ? new GeneralDatumMetadata(meta) : null;
	}

	private CachedMetadata getCachedMetadata(String sourceId) {
		return sourceMetadata.computeIfAbsent(sourceId, k -> {
			GeneralDatumMetadata m = loadPersistedMetadata(sourceId);
			if ( m == null ) {
				m = fetchMetadata(sourceId);
			}
			return new CachedMetadata(sourceId, m);
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

	private void removePersistedMetadata(String sourceId) {
		final String sourceKey = DigestUtils.md5DigestAsHex(sourceId.getBytes(UTF_8));
		final Resource r = new ByteArrayResource(new byte[0], sourceId + " metadata");
		try {
			settingsService.removeSettingResources(getSettingUid(), null, sourceKey, singleton(r));
		} catch ( IOException e ) {
			log.debug("Error loading cached metadata for source {}: {}", sourceId, e.getMessage());
		}
	}

	private GeneralDatumMetadata loadPersistedMetadata(String sourceId) {
		final String sourceKey = DigestUtils.md5DigestAsHex(sourceId.getBytes(UTF_8));
		try {
			Iterable<Resource> resources = settingsService.getSettingResources(getSettingUid(), null,
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

	@Override
	public void addSourceMetadata(String sourceId, GeneralDatumMetadata meta) {
		log.debug("Adding metadata to source {}: {}", sourceId, meta.getPm());
		CachedMetadata cachedMetadata = getCachedMetadata(sourceId);
		boolean changed = false;
		GeneralDatumMetadata metaToSync = null;
		long timestamp = 0;
		synchronized ( cachedMetadata ) {
			changed = cachedMetadata.addMetadata(meta);
			if ( changed && updateThrottleSeconds < 1 ) {
				timestamp = System.currentTimeMillis();
				metaToSync = new GeneralDatumMetadata(cachedMetadata.metadata);
			}
		}
		if ( metaToSync != null ) {
			try {
				syncMetadata(sourceId, metaToSync, timestamp);
			} catch ( IOException e ) {
				throw new RuntimeException(
						String.format("Communication problem synchronizing datum metadata for [%s]: %s",
								sourceId, e.toString()),
						e);
			}
		} else if ( !changed && log.isDebugEnabled() ) {
			log.debug("Metadata has not changed for source {}", sourceId);
		}
	}

	private void syncMetadata(final String sourceId, final GeneralDatumMetadata metadata,
			final long timestamp) throws IOException {
		final String url = nodeSourceMetadataUrl(sourceId);
		log.info("Posting metadata for source {}", sourceId);
		try {
			final InputStream in = jsonPOST(url, metadata);
			verifyResponseSuccess(in);
			CachedMetadata cachedMetadata = sourceMetadata.get(sourceId);
			if ( cachedMetadata != null ) {
				synchronized ( cachedMetadata ) {
					cachedMetadata.setSynced(timestamp);
				}
			}
		} catch ( IOException e ) {
			if ( log.isTraceEnabled() ) {
				log.trace("IOException posting source metadata at " + url, e);
			} else if ( log.isDebugEnabled() ) {
				log.debug("Unable to post data: " + e.getMessage());
			}
		}
	}

	@Override
	public ObjectDatumStreamMetadata getDatumStreamMetadata(ObjectDatumKind kind, Long objectId,
			String sourceId) {
		if ( kind == null || sourceId == null || sourceId.isEmpty() ) {
			return null;
		} else if ( kind == ObjectDatumKind.Location && objectId == null ) {
			return null;
		} else if ( kind == ObjectDatumKind.Node ) {
			objectId = getIdentityService().getNodeId();
		}
		ObjectDatumStreamMetadataId id = new ObjectDatumStreamMetadataId(kind, objectId, sourceId);
		CachedResult<ObjectDatumStreamMetadata> cached = datumStreamMetadata.get(id);
		if ( cached != null && cached.isValid() ) {
			return cached.getResult();
		}
		ObjectDatumStreamMetadata meta = fetchStreamMetadata(id);
		if ( meta != null ) {
			if ( datumStreamMetadataCacheSeconds > 0 ) {
				CachedResult<ObjectDatumStreamMetadata> toCache = new CachedResult<>(meta,
						datumStreamMetadataCacheSeconds, TimeUnit.SECONDS);
				datumStreamMetadata.compute(id, (k, v) -> {
					// update cache only if not currently in cache or replacing expired element
					if ( v == null || v == cached ) {
						return toCache;
					}
					return v;
				});
			}
		}
		return meta;
	}

	private ObjectDatumStreamMetadata fetchStreamMetadata(ObjectDatumStreamMetadataId id) {
		final String url = streamMetadataUrl(id);
		try {
			final InputStream in = jsonGET(url);
			ObjectDatumStreamMetadata result = extractResponseData(in, ObjectDatumStreamMetadata.class);
			if ( result != null ) {
				log.info("Fetched datum stream {} metadata from SolarIn: {}", id, result);
			}
			return result;
		} catch ( IOException e ) {
			if ( log.isTraceEnabled() ) {
				log.trace("IOException querying for datum stream metadata at " + url, e);
			} else if ( log.isDebugEnabled() ) {
				log.debug("Unable to get datum stream metadata: " + e.getMessage());
			}
			throw new RuntimeException(e);
		}
	}

	private String streamMetadataUrl(ObjectDatumStreamMetadataId id) {
		StringBuilder buf = new StringBuilder();
		appendXWWWFormURLEncodedValue(buf, "sourceId", id.getSourceId());
		if ( id.getKind() != null ) {
			appendXWWWFormURLEncodedValue(buf, "kind", id.getKind().name());
		}
		return (getIdentityService().getSolarInBaseUrl() + baseUrl + '/' + id.getObjectId() + "/stream?"
				+ buf);
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

	/**
	 * Get the amount of seconds to wait after an update occurs before
	 * persisting the metadata locally.
	 *
	 * @return the seconds; defaults to
	 *         {@link #DEFAULT_UPDATE_PERSIST_DELAY_SECONDS}
	 * @since 1.6
	 */
	public int getUpdatePersistDelaySeconds() {
		return updatePersistDelaySeconds;
	}

	/**
	 * Set the amount of seconds to wait after an update occurs before
	 * persisting the metadata locally.
	 *
	 * <p>
	 * When greater than {@literal 0} then when updates occur they will not
	 * immediately be persisted locally. Instead the service will wait this many
	 * seconds before persisting the changes. If another update occurs, then the
	 * timer will be reset and the service will wait this many seconds again,
	 * and so on. This can be combined with the {@code updateThrottleSeconds} to
	 * ensure that even if updates are occurring continuously, the changes
	 * eventually get persisted. When changes are infrequent, however, this
	 * setting can be set to a small value to ensure the changes are persisted
	 * relatively quickly, instead of waiting for {@code updateThrottleSeconds}.
	 * </p>
	 *
	 * @param updatePersistDelaySeconds
	 *        the updatePersistDelaySeconds to set
	 * @since 1.6
	 */
	public void setUpdatePersistDelaySeconds(int updatePersistDelaySeconds) {
		this.updatePersistDelaySeconds = updatePersistDelaySeconds;
	}

	/**
	 * Get the maximum number of seconds to cache datum stream metadata before
	 * fetching it again from SolarIn.
	 *
	 * @return the seconds, defaults to
	 *         {@link #DEFATUL_DATUM_STREAM_METADATA_CACHE_SECONDS}
	 * @since 1.7
	 */
	public int getDatumStreamMetadataCacheSeconds() {
		return datumStreamMetadataCacheSeconds;
	}

	/**
	 * Set the maximum number of seconds to cache datum stream metadata before
	 * fetching it again from SolarIn.
	 *
	 * @param datumStreamMetadataCacheSeconds
	 *        the number of seconds to set
	 * @since 1.7
	 */
	public void setDatumStreamMetadataCacheSeconds(int datumStreamMetadataCacheSeconds) {
		this.datumStreamMetadataCacheSeconds = datumStreamMetadataCacheSeconds;
	}

}
