/* ==================================================================
 * JsonNodeMetadataService.java - 21/06/2017 1:57:46 PM
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

package net.solarnetwork.node.metadata.json;

import java.io.IOException;
import java.io.InputStream;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.node.service.MetadataService;
import net.solarnetwork.node.service.NodeMetadataService;
import net.solarnetwork.node.service.support.JsonHttpClientSupport;

/**
 * JSON based web service implementation of {@link NodeMetadataService}.
 *
 * @author matt
 * @version 2.1
 * @since 1.7
 */
public class JsonNodeMetadataService extends JsonHttpClientSupport
		implements NodeMetadataService, MetadataService {

	private String baseUrl = "/api/v1/sec/nodes/meta";

	private GeneralDatumMetadata cachedMetadata;

	/**
	 * Constructor.
	 */
	public JsonNodeMetadataService() {
		super();
	}

	@Override
	public GeneralDatumMetadata getAllMetadata() {
		return getNodeMetadata();
	}

	private String nodeMetadataUrl() {
		return (getIdentityService().getSolarInBaseUrl() + baseUrl);
	}

	private synchronized void cacheMetadata(final GeneralDatumMetadata meta) {
		GeneralDatumMetadata currMeta = cachedMetadata;
		GeneralDatumMetadata newMeta;
		if ( currMeta == null ) {
			newMeta = meta;
		} else {
			newMeta = new GeneralDatumMetadata(currMeta);
			newMeta.merge(meta, true);
		}
		if ( newMeta != null && newMeta.equals(currMeta) == false ) {
			cachedMetadata = newMeta;
		}
	}

	@Override
	public synchronized void addNodeMetadata(GeneralDatumMetadata meta) {
		GeneralDatumMetadata currMeta = cachedMetadata;
		if ( currMeta != null ) {
			currMeta.merge(meta, true);
			if ( currMeta.equals(meta) ) {
				log.debug("Node metadta has not changed");
				return;
			}
			meta = currMeta;
		}
		final String url = nodeMetadataUrl();
		try {
			final InputStream in = jsonPOST(url, meta);
			verifyResponseSuccess(in);
			cacheMetadata(meta);
		} catch ( IOException e ) {
			if ( log.isTraceEnabled() ) {
				log.trace("IOException posting node metadata at " + url, e);
			} else if ( log.isDebugEnabled() ) {
				log.debug("Unable to post data: " + e.getMessage());
			}
			throw new RuntimeException(e);
		}
	}

	@Override
	public GeneralDatumMetadata getNodeMetadata() {
		final String url = nodeMetadataUrl();
		try {
			final InputStream in = jsonGET(url);
			return extractResponseData(in, GeneralDatumMetadata.class);
		} catch ( IOException e ) {
			if ( log.isTraceEnabled() ) {
				log.trace("IOException querying for node metadata at " + url, e);
			} else if ( log.isDebugEnabled() ) {
				log.debug("Unable to get node metadata: " + e.getMessage());
			}
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the base URL.
	 *
	 * @return the baseUrl
	 */
	public String getBaseUrl() {
		return baseUrl;
	}

	/**
	 * Set the base URL.
	 *
	 * @param baseUrl
	 *        the baseUrl to set
	 */
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

}
