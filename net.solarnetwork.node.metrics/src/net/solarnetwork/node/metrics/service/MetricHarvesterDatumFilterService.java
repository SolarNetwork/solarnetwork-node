/* ==================================================================
 * MetricHarvesterDatumFilterService.java - 15/07/2024 12:55:55 pm
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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.node.metrics.dao.MetricDao;
import net.solarnetwork.node.metrics.domain.Metric;
import net.solarnetwork.node.service.support.BaseDatumFilterSupport;
import net.solarnetwork.service.DatumFilterService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.ObjectUtils;

/**
 * Datum filter service that "harvests" datum properties as metric values.
 *
 * @author matt
 * @version 1.0
 */
public class MetricHarvesterDatumFilterService extends BaseDatumFilterSupport
		implements DatumFilterService, SettingSpecifierProvider {

	private final MetricDao metricDao;

	private MetricHarvesterPropertyConfig[] propertyConfigs;

	/**
	 * Constructor.
	 *
	 * @param metricDao
	 *        the metric DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public MetricHarvesterDatumFilterService(MetricDao metricDao) {
		super();
		this.metricDao = ObjectUtils.requireNonNullArgument(metricDao, "metricDao");
	}

	@Override
	public DatumSamplesOperations filter(Datum datum, DatumSamplesOperations samples,
			Map<String, Object> parameters) {
		final long start = incrementInputStats();
		final MetricHarvesterPropertyConfig[] configs = getPropertyConfigs();
		if ( configs == null || configs.length < 1 || datum == null || datum.getSourceId() == null
				|| samples == null ) {
			incrementIgnoredStats(start);
			return samples;
		}
		if ( !conditionsMatch(datum, samples, parameters) ) {
			incrementIgnoredStats(start);
			return samples;
		}
		captureMetrics(datum, samples, configs, parameters);
		incrementStats(start, samples, samples);
		return samples;
	}

	private void captureMetrics(Datum datum, DatumSamplesOperations samples,
			MetricHarvesterPropertyConfig[] configs, Map<String, Object> parameters) {
		for ( MetricHarvesterPropertyConfig config : configs ) {
			final String metricName = config.getMetricName();
			if ( metricName == null || metricName.isEmpty() ) {
				continue;
			}
			Number n = config.applyTransformations(
					samples.getSampleBigDecimal(config.getPropertyType(), config.getPropertyKey()));
			if ( n != null ) {
				Metric m = Metric.sampleValue(datum.getTimestamp(), metricName, n.doubleValue());
				metricDao.save(m);
			}
		}
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.metrics.service.harvester";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		return settingSpecifiers(false);
	}

	@Override
	public List<SettingSpecifier> templateSettingSpecifiers() {
		return settingSpecifiers(true);
	}

	private List<SettingSpecifier> settingSpecifiers(final boolean template) {
		List<SettingSpecifier> result = baseIdentifiableSettings("");
		populateBaseSampleTransformSupportSettings(result);
		populateStatusSettings(result);

		MetricHarvesterPropertyConfig[] propConfs = getPropertyConfigs();
		List<MetricHarvesterPropertyConfig> propConfList = (template
				? Collections.singletonList(new MetricHarvesterPropertyConfig())
				: (propConfs != null ? asList(propConfs) : emptyList()));
		result.add(SettingUtils.dynamicListSettingSpecifier("propertyConfigs", propConfList,
				new SettingUtils.KeyedListCallback<MetricHarvesterPropertyConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(
							MetricHarvesterPropertyConfig value, int index, String key) {
						SettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								MetricHarvesterPropertyConfig.settings(key + "."));
						return Collections.singletonList(configGroup);
					}
				}));

		return result;
	}

	/**
	 * Get the property configurations.
	 *
	 * @return the property configurations
	 */
	public MetricHarvesterPropertyConfig[] getPropertyConfigs() {
		return propertyConfigs;
	}

	/**
	 * Set the property configurations to use.
	 *
	 * @param propertyConfigs
	 *        the configurations to use
	 */
	public void setPropertyConfigs(MetricHarvesterPropertyConfig[] propertyConfigs) {
		this.propertyConfigs = propertyConfigs;
	}

	/**
	 * Get the number of configured {@code propertyConfigs} elements.
	 *
	 * @return the number of {@code propertyConfigs} elements
	 */
	public int getPropertyConfigsCount() {
		MetricHarvesterPropertyConfig[] confs = this.propertyConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code propertyConfigs} elements.
	 *
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link MetricHarvesterPropertyConfig} instances.
	 * </p>
	 *
	 * @param count
	 *        The desired number of {@code propertyConfigs} elements.
	 */
	public void setPropertyConfigsCount(int count) {
		this.propertyConfigs = ArrayUtils.arrayWithLength(this.propertyConfigs, count,
				MetricHarvesterPropertyConfig.class, MetricHarvesterPropertyConfig::new);
	}

}
