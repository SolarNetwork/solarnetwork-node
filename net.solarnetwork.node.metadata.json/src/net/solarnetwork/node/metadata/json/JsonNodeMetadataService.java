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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.MetadataService;
import net.solarnetwork.node.service.NodeMetadataService;
import net.solarnetwork.node.service.support.JsonHttpClientSupport;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.CachedResult;

/**
 * JSON based web service implementation of {@link NodeMetadataService}.
 *
 * <p>
 * The cache can be cleared using a {@code Signal} instruction with a
 * {@link #SETTING_UID} parameter set to {@code clear-cache}.
 * </p>
 *
 * @author matt
 * @version 2.4
 * @since 1.7
 */
public class JsonNodeMetadataService extends JsonHttpClientSupport
		implements NodeMetadataService, MetadataService, NodeMetadataInstructions, InstructionHandler,
		SettingSpecifierProvider, SettingsChangeObserver {

	/** The {@code cacheSeconds} property default value. */
	public static final int DEFAULT_CACHE_SECONDS = 3600;

	/**
	 * The setting UID.
	 *
	 * @since 2.4
	 */
	public static final String SETTING_UID = "net.solarnetwork.node.metadata.json";

	private int cacheSeconds = DEFAULT_CACHE_SECONDS;

	private String baseUrl = "/api/v1/sec/nodes/meta";

	private CachedResult<GeneralDatumMetadata> cachedMetadata;

	private GeneralDatumMetadata localMetadata;

	/**
	 * Constructor.
	 */
	public JsonNodeMetadataService() {
		super();
	}

	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		cachedMetadata = null;
	}

	@Override
	public String getSettingUid() {
		return SETTING_UID;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		try {
			getAllMetadata();
		} catch ( RuntimeException e ) {
			log.warn("Error getting metadata: {}", e.getMessage());
		}
		List<SettingSpecifier> result = new ArrayList<>(2);
		result.add(0, new BasicTitleSettingSpecifier("status", getStatusMessage(), true));
		result.add(new BasicTextFieldSettingSpecifier("cacheSeconds",
				String.valueOf(DEFAULT_CACHE_SECONDS)));
		return result;
	}

	@Override
	public synchronized GeneralDatumMetadata getAllMetadata() {
		return getNodeMetadata();
	}

	private String nodeMetadataUrl() {
		return (getIdentityService().getSolarInBaseUrl() + baseUrl);
	}

	private synchronized void cacheMetadata(final GeneralDatumMetadata meta) {
		GeneralDatumMetadata currMeta = localMetadata;
		GeneralDatumMetadata newMeta;
		if ( currMeta == null ) {
			newMeta = meta;
		} else {
			newMeta = new GeneralDatumMetadata(currMeta);
			newMeta.merge(meta, true);
		}
		if ( newMeta != null && newMeta.equals(currMeta) == false ) {
			localMetadata = newMeta;
		}

		CachedResult<GeneralDatumMetadata> cached = this.cachedMetadata;
		if ( cached != null && cached.isValid() ) {
			if ( cached.getResult() != null ) {
				cached.getResult().merge(localMetadata, true);
			} else if ( cacheSeconds > 0 ) {
				this.cachedMetadata = new CachedResult<>(newMeta, cacheSeconds, TimeUnit.SECONDS);
			}
		}
	}

	@Override
	public synchronized void addNodeMetadata(GeneralDatumMetadata meta) {
		GeneralDatumMetadata currMeta = localMetadata != null ? localMetadata : getNodeMetadata();
		if ( currMeta != null ) {
			currMeta.merge(meta, true);
			if ( currMeta.equals(meta) ) {
				log.debug("Node metadta has not changed");
				return;
			}
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
		final CachedResult<GeneralDatumMetadata> cachedMetadata = this.cachedMetadata;
		if ( cachedMetadata != null && cachedMetadata.isValid() ) {
			return cachedMetadata.getResult();
		}
		final String url = nodeMetadataUrl();
		try {
			final InputStream in = jsonGET(url);
			GeneralDatumMetadata meta = extractResponseData(in, GeneralDatumMetadata.class);
			if ( cacheSeconds > 0 ) {
				synchronized ( this ) {
					this.cachedMetadata = new CachedResult<>(meta, cacheSeconds, TimeUnit.SECONDS);
				}
			}
			return meta;
		} catch ( IOException e ) {
			if ( log.isTraceEnabled() ) {
				log.trace("IOException querying for node metadata at " + url, e);
			} else if ( log.isDebugEnabled() ) {
				log.debug("Unable to get node metadata: " + e.getMessage());
			}
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean handlesTopic(String topic) {
		return InstructionHandler.TOPIC_SIGNAL.equalsIgnoreCase(topic);
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
		if ( instruction == null || !handlesTopic(instruction.getTopic()) ) {
			return null;
		}
		for ( String paramName : instruction.getParameterNames() ) {
			if ( SETTING_UID.equals(paramName) ) {
				if ( CLEAR_CACHE_SIGNAL.equals(instruction.getParameterValue(paramName)) ) {
					synchronized ( this ) {
						cachedMetadata = null;
					}
					return InstructionUtils.createStatus(instruction, InstructionState.Completed);
				}
				// signal not supported
				return InstructionUtils.createStatus(instruction, InstructionState.Declined,
						InstructionUtils.createErrorResultParameters("Signal name not supported.",
								"JNMS.0001"));
			}
		}
		return null;
	}

	private String getStatusMessage() {
		final CachedResult<GeneralDatumMetadata> cached = this.cachedMetadata;
		final MessageSource msgSource = getMessageSource();
		if ( cached == null ) {
			return msgSource.getMessage("status.noneCached", null, Locale.getDefault());
		}
		GeneralDatumMetadata meta = cached.getResult();
		if ( meta == null ) {
			return msgSource.getMessage("status.none",
					new Object[] { new Date(cached.getCreated()), new Date(cached.getExpires()) },
					Locale.getDefault());
		}
		Map<String, Object> info = meta.getInfo();
		Map<String, Map<String, Object>> propInfo = meta.getPropertyInfo();
		return msgSource.getMessage("status.msg",
				new Object[] { new Date(cached.getCreated()), new Date(cached.getExpires()),
						info != null ? info.size() : 0, propInfo != null ? propInfo.size() : 0 },
				Locale.getDefault());
	}

	/**
	 * Set the number of seconds to cache metadata.
	 *
	 * @param cacheSeconds
	 *        the maximum number of seconds to cache metadata for, or anything
	 *        less than {@literal 1} to disable
	 */
	public void setCacheSeconds(int cacheSeconds) {
		this.cacheSeconds = cacheSeconds;
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
