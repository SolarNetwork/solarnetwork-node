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

import java.math.BigDecimal;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.solarnetwork.node.control.esi.domain.DurationRange;
import net.solarnetwork.node.control.esi.domain.PowerComponents;
import net.solarnetwork.node.control.esi.domain.PriceComponents;
import net.solarnetwork.node.control.esi.domain.PriceMap;
import net.solarnetwork.node.control.esi.domain.PriceMapAccessor;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;

/**
 * A configurable ESI price map component.
 * 
 * @author matt
 * @version 1.0
 */
public class EsiPriceMap extends BaseEsiMetadataComponent implements PriceMapAccessor {

	/** The node property metadata key used for all ESI price map metadata. */
	public static final String ESI_PRICEMAP_METADATA_KEY = "esi-pricemap";

	/** The default {@code controlId} value. */
	public static final String DEFAULT_CONTROL_ID = "/load/1";

	private String controlId = DEFAULT_CONTROL_ID;
	private PriceMap priceMap = new PriceMap();

	/**
	 * Constructor.
	 */
	public EsiPriceMap() {
		super(ESI_PRICEMAP_METADATA_KEY);
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
		results.add(new BasicTextFieldSettingSpecifier("controlId", DEFAULT_CONTROL_ID));
		PriceMap.addSettings("priceMap.", results);
		return results;
	}

	@Override
	protected Map<String, Object> getEsiComponentMetadata() {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("priceMap", priceMap.asMap());
		return result;
	}

	// ----------------
	// PriceMapAccessor
	// ----------------

	@Override
	public BigDecimal calculatedApparentEnergyCost() {
		return priceMap.calculatedApparentEnergyCost();
	}

	@Override
	public double durationHours() {
		return priceMap.durationHours();
	}

	@Override
	public String toInfoString(Locale locale) {
		return priceMap.toInfoString(locale);
	}

	@Override
	public String getInfo() {
		return priceMap.getInfo();
	}

	@Override
	public PowerComponents getPowerComponents() {
		return priceMap.getPowerComponents();
	}

	@Override
	public Duration getDuration() {
		return priceMap.getDuration();
	}

	@Override
	public long getDurationMillis() {
		return priceMap.getDurationMillis();
	}

	@Override
	public DurationRange getResponseTime() {
		return priceMap.getResponseTime();
	}

	@Override
	public PriceComponents getPriceComponents() {
		return priceMap.getPriceComponents();
	}

	// ----------------
	// Accessors
	// ----------------

	/**
	 * Get the control ID.
	 * 
	 * @return the control ID
	 */
	public String getControlId() {
		return controlId;
	}

	/**
	 * Set the control ID.
	 * 
	 * @param controlId
	 *        the control ID to set
	 */
	public void setControlId(String controlId) {
		this.controlId = controlId;
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
