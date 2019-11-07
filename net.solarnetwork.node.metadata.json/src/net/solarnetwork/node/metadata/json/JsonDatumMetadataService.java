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
import java.util.Collection;
import java.util.Iterator;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.DigestUtils;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.node.DatumMetadataService;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.settings.SettingResourceHandler;
import net.solarnetwork.node.settings.SettingsService;
import net.solarnetwork.node.settings.SettingsUpdates;
import net.solarnetwork.node.support.JsonHttpClientSupport;

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
 * @version 1.4
 */
public class JsonDatumMetadataService extends JsonHttpClientSupport
		implements DatumMetadataService, SettingResourceHandler {

	public static final String SETTING_KEY_SOURCE_META = "JsonDatumMetadataService.sourceMeta";

	private static final Charset UTF8 = Charset.forName("UTF-8");

	private String baseUrl = "/api/v1/sec/datum/meta";

	private final SettingsService settingsService;
	private SettingDao settingDao;

	/**
	 * Constructor.
	 * 
	 * @param settingsService
	 *        the settings service to use
	 */
	public JsonDatumMetadataService(SettingsService settingsService) {
		super();
		this.settingsService = settingsService;
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.metadata.json.JsonDatumMetadataService";
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

	private GeneralDatumMetadata cachedMetadata(String sourceId) {
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

	private synchronized void cacheMetadata(final String sourceId, final GeneralDatumMetadata meta) {
		GeneralDatumMetadata currMeta = cachedMetadata(sourceId);
		GeneralDatumMetadata newMeta;
		if ( currMeta == null ) {
			newMeta = meta;
		} else {
			newMeta = new GeneralDatumMetadata(currMeta);
			newMeta.merge(meta, true);
		}
		if ( newMeta != null && newMeta.equals(currMeta) == false ) {
			// have changes, so persist
			try {
				final String sourceKey = DigestUtils.md5DigestAsHex(sourceId.getBytes(UTF8));
				byte[] json = getObjectMapper().writeValueAsBytes(newMeta);
				ByteArrayResource r = new ByteArrayResource(json, sourceId + " metadata");
				settingsService.importSettingResources(getSettingUID(), null, sourceKey, singleton(r));
			} catch ( IOException e ) {
				log.error("Error generating cached metadata JSON for source {}: {}", sourceId,
						e.getMessage());
			}
		}
	}

	@Override
	public void addSourceMetadata(String sourceId, GeneralDatumMetadata meta) {
		log.debug("Adding metadata to source {}: {}", sourceId, meta.getPm());
		GeneralDatumMetadata currMeta = cachedMetadata(sourceId);
		if ( currMeta != null ) {
			log.debug("Merging metadata for source {} into {}", sourceId, currMeta.getPm());
			GeneralDatumMetadata mergedMeta = new GeneralDatumMetadata(currMeta);
			mergedMeta.merge(meta, true);
			if ( currMeta.equals(mergedMeta) ) {
				log.debug("Metadata has not changed for source {}", sourceId);
				return;
			}
			meta = mergedMeta;
		}
		final String url = nodeSourceMetadataUrl(sourceId);
		log.info("Posting metadata for source {}", sourceId);
		try {
			final InputStream in = jsonPOST(url, meta);
			verifyResponseSuccess(in);
			cacheMetadata(sourceId, meta);
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

}
