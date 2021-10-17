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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.node.control.esi.domain.DurationRange;
import net.solarnetwork.node.control.esi.domain.ResourceAccessor;
import net.solarnetwork.node.control.esi.domain.ResourceCharacteristics;
import net.solarnetwork.settings.SettingSpecifier;

/**
 * A configurable ESI resource component.
 * 
 * @author matt
 * @version 2.0
 */
public class EsiResource extends BaseEsiMetadataComponent implements ResourceAccessor {

	/** The node property metadata key used for all ESI resource metadata. */
	public static final String ESI_RESOURCE_METADATA_KEY = "esi-resource";

	private ResourceCharacteristics characteristics = new ResourceCharacteristics();

	/**
	 * Constructor.
	 */
	public EsiResource() {
		super(ESI_RESOURCE_METADATA_KEY);
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.control.esi.resource";
	}

	@Override
	public String getDisplayName() {
		return "ESI Resource";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = super.getSettingSpecifiers();
		ResourceCharacteristics.addSettings("characteristics.", results);
		return results;
	}

	@Override
	protected Map<String, Object> getEsiComponentMetadata() {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("characteristics", characteristics.asMap());
		return result;
	}

	// ----------------
	// ResourceAccessor
	// ----------------

	@Override
	public Long getLoadPowerMax() {
		return characteristics.getLoadPowerMax();
	}

	@Override
	public Float getLoadPowerFactor() {
		return characteristics.getLoadPowerFactor();
	}

	@Override
	public Long getSupplyPowerMax() {
		return characteristics.getSupplyPowerMax();
	}

	@Override
	public Float getSupplyPowerFactor() {
		return characteristics.getSupplyPowerFactor();
	}

	@Override
	public Long getStorageEnergyCapacity() {
		return characteristics.getStorageEnergyCapacity();
	}

	@Override
	public DurationRange getResponseTime() {
		return characteristics.getResponseTime();
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
	 * @throws IllegalArgumentException
	 *         if {@code characteristics} is {@literal null}
	 */
	public void setCharacteristics(ResourceCharacteristics characteristics) {
		if ( characteristics == null ) {
			throw new IllegalArgumentException("The characteristics must be provided.");
		}
		this.characteristics = characteristics;
	}

}
