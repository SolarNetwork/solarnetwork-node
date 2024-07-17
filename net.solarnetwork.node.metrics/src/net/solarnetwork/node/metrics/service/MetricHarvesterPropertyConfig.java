/* ==================================================================
 * MetricHarvesterPropertyConfig.java - 15/07/2024 1:07:06 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.metrics.service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.domain.datum.DatumSamplePropertyConfig;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.NumberDatumSamplePropertyConfig;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Configuration for datum proeprty metric harvesting.
 *
 * @author matt
 * @version 1.0
 */
public class MetricHarvesterPropertyConfig extends NumberDatumSamplePropertyConfig<String> {

	/**
	 * Get a list of settings suitable for configuring an instance of this
	 * class.
	 *
	 * @param prefix
	 *        the message key prefix
	 * @return the settings
	 */
	public static List<SettingSpecifier> settings(String prefix) {
		List<SettingSpecifier> results = new ArrayList<>(8);

		// drop-down menu for propertyTypeKey
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "propertyTypeKey", Character.toString(DEFAULT_PROPERTY_TYPE.toKey()));
		Map<String, String> propTypeTitles = new LinkedHashMap<String, String>(3);
		for ( DatumSamplesType e : EnumSet.of(DatumSamplesType.Instantaneous,
				DatumSamplesType.Accumulating, DatumSamplesType.Status) ) {
			propTypeTitles.put(Character.toString(e.toKey()), e.toString());
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		results.add(propTypeSpec);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "propertyKey", null));

		results.addAll(numberTransformSettings(prefix));

		results.add(new BasicTextFieldSettingSpecifier(prefix + "metricName", null));

		return results;
	}

	/**
	 * Constructor.
	 */
	public MetricHarvesterPropertyConfig() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param propertyKey
	 *        the property key
	 * @param propertyType
	 *        the property type
	 * @param metricName
	 *        the metric name
	 */
	public MetricHarvesterPropertyConfig(String propertyKey, DatumSamplesType propertyType,
			String metricName) {
		super(propertyKey, propertyType, metricName);
	}

	/**
	 * Get the desired metric name.
	 *
	 * <p>
	 * This is an alias for {@link DatumSamplePropertyConfig#getConfig()}.
	 * </p>
	 *
	 * @return the metric name
	 */
	public String getMetricName() {
		return getConfig();
	}

	/**
	 * Set the metric name.
	 *
	 * <p>
	 * This is an alias for {@link DatumSamplePropertyConfig#setConfig(Object)}.
	 * </p>
	 *
	 * @param name
	 *        the metric name to set
	 */
	public void setMetricName(String name) {
		setConfig(name);
	}

}
