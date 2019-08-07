/* ==================================================================
 * EsiResource.java - 7/08/2019 4:34:04 pm
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.node.NodeMetadataService;
import net.solarnetwork.node.control.esi.domain.ResourceCharacteristics;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.node.support.BaseIdentifiable;
import net.solarnetwork.util.OptionalService;

/**
 * A configurable ESI resource component.
 * 
 * @author matt
 * @version 1.0
 */
public class EsiResource extends BaseIdentifiable implements SettingSpecifierProvider {

	/** The node property metadata key used for all ESI resource metadata. */
	public static final String ESI_RESOURCE_METADATA_KEY = "esi-resource";

	private final Logger log = LoggerFactory.getLogger(getClass());

	private OptionalService<NodeMetadataService> nodeMetadataService;

	private ResourceCharacteristics characteristics = new ResourceCharacteristics();

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.control.esi.resource";
	}

	@Override
	public String getDisplayName() {
		return "ESI Resource";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(12);
		results.add(new BasicTitleSettingSpecifier("status", getStatus(Locale.getDefault()), true));
		results.addAll(baseIdentifiableSettings(null));
		ResourceCharacteristics.addSettings("characteristics.", results);
		return results;
	}

	/**
	 * Callback after properties have been changed.
	 * 
	 * @param properties
	 *        the changed properties
	 */
	public void configurationChanged(Map<String, Object> properties) {
		updateNodeMetadata();
	}

	private String getStatus(Locale locale) {
		final String uid = getUid();
		if ( uid == null || uid.trim().isEmpty() ) {
			return getMessageSource().getMessage("status.error.noUid", null, locale);
		}
		NodeMetadataService nms = nodeMetadataService();
		if ( nms == null ) {
			return getMessageSource().getMessage("status.error.noMetadataService", null, locale);
		}
		return getMessageSource().getMessage("status.ok", null, locale);
	}

	private void updateNodeMetadata() {
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
		Map<String, Object> props = getEsiResourceProperties();
		meta.putInfoValue(ESI_RESOURCE_METADATA_KEY, uid, props);
		log.info("Publishing ESI Resource metadata for {}: {}", uid, props);
		service.addNodeMetadata(meta);
	}

	private Map<String, Object> getEsiResourceProperties() {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("characteristics", characteristics.asMap());
		return result;
	}

	private NodeMetadataService nodeMetadataService() {
		return (this.nodeMetadataService != null ? this.nodeMetadataService.service() : null);
	}

	/**
	 * Get the characteristics.
	 * 
	 * @return the characteristics, never {@literal null}
	 */
	public ResourceCharacteristics getCharacteristics() {
		return characteristics;
	}

	/**
	 * Set the characteristics.
	 * 
	 * @param characteristics
	 *        the characteristics
	 */
	public void setCharacteristics(ResourceCharacteristics characteristics) {
		if ( characteristics == null ) {
			throw new IllegalArgumentException("The characteristics must be provided.");
		}
		this.characteristics = characteristics;
	}

	/**
	 * Configure a {@link NodeMetadataService} to publish ESI resource
	 * information to.
	 * 
	 * @param nodeMetadataService
	 *        the node metadata service to use
	 */
	public void setNodeMetadataService(OptionalService<NodeMetadataService> nodeMetadataService) {
		this.nodeMetadataService = nodeMetadataService;
	}

}
