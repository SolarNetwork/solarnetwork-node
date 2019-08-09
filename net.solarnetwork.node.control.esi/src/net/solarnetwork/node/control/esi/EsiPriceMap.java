/* ==================================================================
 * EsiPriceMap.java - 9/08/2019 5:07:02 pm
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
import net.solarnetwork.node.control.esi.domain.PriceMap;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;

/**
 * A configurable ESI price map component.
 * 
 * @author matt
 * @version 1.0
 */
public class EsiPriceMap extends BaseEsiMetadataComponent implements SettingSpecifierProvider {

	/** The node property metadata key used for all ESI price map metadata. */
	public static final String ESI_RESOURCE_METADATA_KEY = "esi-pricemap";

	private PriceMap priceMap = new PriceMap();

	/**
	 * Constructor.
	 */
	public EsiPriceMap() {
		super(ESI_RESOURCE_METADATA_KEY);
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.control.esi.pricemap";
	}

	@Override
	public String getDisplayName() {
		return "ESI PriceMap";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = super.getSettingSpecifiers();
		PriceMap.addSettings("priceMap.", results);
		return results;
	}

	@Override
	protected Map<String, Object> getEsiComponentMetadata() {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("pricemap", priceMap.asMap());
		return result;
	}

	/**
	 * Get the price map.
	 * 
	 * @return the price map
	 */
	public final PriceMap getPriceMap() {
		return priceMap;
	}

	/**
	 * Set the price map.
	 * 
	 * @param priceMap
	 *        the price map to set
	 * @throws IllegalArgumentException
	 *         if {@code priceMap} is {@literal null}
	 */
	public final void setPriceMap(PriceMap priceMap) {
		if ( priceMap == null ) {
			throw new IllegalArgumentException("The price map must be provided.");
		}
		this.priceMap = priceMap;
	}

}
