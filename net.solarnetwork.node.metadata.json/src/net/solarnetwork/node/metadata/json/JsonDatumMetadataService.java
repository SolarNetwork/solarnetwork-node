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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.node.DatumMetadataService;
import net.solarnetwork.node.dao.SettingDao;
import net.solarnetwork.node.support.JsonHttpClientSupport;

/**
 * JSON based web service implementation of {@link DatumMetadataService}.
 * 
 * <p>
 * This implementation caches the metadata for each source using
 * {@link SettingDao} and thus might have limitations on how much metadata can
 * be effectively associated with a given source ID.
 * </p>
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * <dt>baseUrl</dt>
 * <dd>The SolarIn relative source-level metadata base URL path.</dd>
 * 
 * <dt>compress</dt>
 * <dd>Flag to compress the HTTP body content, defaults to <em>true</em>.</dd>
 * 
 * <dt>objectMapper</dt>
 * <dd>The {@link ObjectMapper} to marshall objects to JSON with and parse the
 * response with.</dd>
 * </dl>
 * 
 * @author matt
 * @version 1.2
 */
public class JsonDatumMetadataService extends JsonHttpClientSupport implements DatumMetadataService {

	public static final String SETTING_KEY_SOURCE_META = "JsonDatumMetadataService.sourceMeta";

	private String baseUrl = "/api/v1/sec/datum/meta";

	public SettingDao settingDao;

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
			String json;
			try {
				json = getObjectMapper().writeValueAsString(newMeta);
				settingDao.storeSetting(SETTING_KEY_SOURCE_META, sourceId, json);
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

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public SettingDao getSettingDao() {
		return settingDao;
	}

	public void setSettingDao(SettingDao settingDao) {
		this.settingDao = settingDao;
	}

}
