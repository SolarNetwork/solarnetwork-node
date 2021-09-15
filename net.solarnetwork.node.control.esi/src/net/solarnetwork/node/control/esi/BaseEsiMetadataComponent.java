/* ==================================================================
 * BaseEsiMetadataComponent.java - 9/08/2019 5:08:41 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.esi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.node.service.NodeMetadataService;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;

/**
 * An abstract implementation of an ESI component that publishes information as
 * node metadata.
 * 
 * @author matt
 * @version 2.0
 */
public abstract class BaseEsiMetadataComponent extends BaseIdentifiable
		implements SettingSpecifierProvider {

	private final String rootMetadataKey;

	private OptionalService<NodeMetadataService> nodeMetadataService;

	/**
	 * Constructor.
	 * 
	 * @param rootMetadataKey
	 *        the root node metadata key to use
	 * @throws IllegalArgumentException
	 *         if {@code rootMetadataKey} is {@literal null} or empty
	 */
	public BaseEsiMetadataComponent(String rootMetadataKey) {
		super();
		if ( rootMetadataKey == null || rootMetadataKey.isEmpty() ) {
			throw new IllegalArgumentException("The rootMetadataKey must be provided.");
		}
		this.rootMetadataKey = rootMetadataKey;
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(12);
		results.add(
				new BasicTitleSettingSpecifier("status", getStatusMessage(Locale.getDefault()), true));
		results.addAll(baseIdentifiableSettings(null));
		return results;
	}

	/**
	 * Get a brief status message.
	 * 
	 * <p>
	 * This implementation reports an error if no {@code uid} is configured, or
	 * if no {@code NodeMetadataService} is available.
	 * </p>
	 * 
	 * @param locale
	 *        the locale to get the message for
	 * @return the message, never {@literal null}
	 */
	public String getStatusMessage(Locale locale) {
		String err = getConfigurationErrorStatusMessage(locale);
		if ( err != null ) {
			return err;
		}
		return getConfigurationOkStatusMessage(locale);
	}

	/**
	 * Get a configuration OK status message.
	 * 
	 * <p>
	 * This method returns a default {@code status.ok} message.
	 * </p>
	 * 
	 * @param locale
	 *        the locale to get the message for
	 * @return the configuration OK status message, or {@literal null} if none
	 */
	protected String getConfigurationOkStatusMessage(Locale locale) {
		return getMessageSource().getMessage("status.ok", null, locale);
	}

	/**
	 * Get a configuration error status message.
	 * 
	 * <p>
	 * This returns an error if no {@code uid} is configured, or if no
	 * {@code NodeMetadataService} is available.
	 * </p>
	 * 
	 * @param locale
	 *        the locale to get the message for
	 * @return the configuration error status message, or {@literal null} if
	 *         none
	 */
	protected String getConfigurationErrorStatusMessage(Locale locale) {
		final String uid = getUid();
		if ( uid == null || uid.trim().isEmpty() ) {
			return getMessageSource().getMessage("status.error.noUid", null, locale);
		}
		NodeMetadataService nms = nodeMetadataService();
		if ( nms == null ) {
			return getMessageSource().getMessage("status.error.noMetadataService", null, locale);
		}
		return null;
	}

	/**
	 * Callback after properties have been changed.
	 * 
	 * <p>
	 * This method calls the {@link #publishEsiComponentMetadata()} method.
	 * </p>
	 * 
	 * @param properties
	 *        the changed properties
	 */
	public void configurationChanged(Map<String, Object> properties) {
		publishEsiComponentMetadata();
	}

	/**
	 * Publish the ESI metadata.
	 * 
	 * <p>
	 * This method calls the {@link #getEsiComponentMetadata()} method, and
	 * publishes that as node metadata using the configured root
	 * 
	 */
	protected final void publishEsiComponentMetadata() {
		String uid = getUid();
		if ( uid == null || uid.trim().isEmpty() ) {
			log.warn("Cannot publish ESI Resource metadata because no UID configured.");
			return;
		}
		NodeMetadataService service = nodeMetadataService();
		if ( service == null ) {
			return;
		}
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		Map<String, Object> props = getEsiComponentMetadata();

		// automatically add the groupUid key
		String guid = getGroupUid();
		if ( guid != null ) {
			props.put("groupUid", guid);
		}

		meta.putInfoValue(rootMetadataKey, uid, props);
		log.info("Publishing ESI {} metadata for {}: {}", rootMetadataKey, uid, props);
		service.addNodeMetadata(meta);
	}

	/**
	 * Get a map of all the ESI component properties to publish as metadata.
	 * 
	 * @return the metadata properties
	 */
	protected abstract Map<String, Object> getEsiComponentMetadata();

	/**
	 * Get the {@link NodeMetadataService} if available.
	 * 
	 * @return the metadata service, or {@literal null} if not available
	 */
	protected final NodeMetadataService nodeMetadataService() {
		return (this.nodeMetadataService != null ? this.nodeMetadataService.service() : null);
	}

	/**
	 * Configure a {@link NodeMetadataService} to publish ESI resource
	 * information to.
	 * 
	 * @param nodeMetadataService
	 *        the node metadata service to use
	 */
	public final void setNodeMetadataService(OptionalService<NodeMetadataService> nodeMetadataService) {
		this.nodeMetadataService = nodeMetadataService;
	}

	/**
	 * Get the root metadata key.
	 * 
	 * <p>
	 * This is the root metadata key that all node metadata is published under.
	 * </p>
	 * 
	 * @return the root metadata key, never {@literal null}
	 */
	public final String getRootMetadataKey() {
		return rootMetadataKey;
	}

}
