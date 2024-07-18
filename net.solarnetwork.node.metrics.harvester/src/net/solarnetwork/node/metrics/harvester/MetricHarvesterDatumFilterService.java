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

package net.solarnetwork.node.metrics.harvester;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.service.OptionalServiceCollection.services;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.node.domain.ExpressionRoot;
import net.solarnetwork.node.metrics.dao.MetricDao;
import net.solarnetwork.node.metrics.domain.Metric;
import net.solarnetwork.node.service.support.BaseDatumFilterSupport;
import net.solarnetwork.node.service.support.ExpressionConfig;
import net.solarnetwork.service.DatumFilterService;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.settings.KeyedSettingSpecifier;
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
	private ExpressionConfig[] expressionConfigs;

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
		final ExpressionConfig[] exprConfigs = getExpressionConfigs();
		if ( ((configs == null || configs.length < 1) && (exprConfigs == null || exprConfigs.length < 1))
				|| datum == null || datum.getSourceId() == null || samples == null ) {
			incrementIgnoredStats(start);
			return samples;
		}
		if ( !conditionsMatch(datum, samples, parameters) ) {
			incrementIgnoredStats(start);
			return samples;
		}
		if ( configs != null && configs.length > 0 ) {
			captureMetrics(datum, samples, configs, parameters);
		}
		if ( exprConfigs != null && exprConfigs.length > 0 ) {
			captureMetrics(datum, samples, exprConfigs, parameters);

		}
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
				saveMetric(m);
			}
		}
	}

	private void captureMetrics(Datum datum, DatumSamplesOperations samples, ExpressionConfig[] configs,
			Map<String, Object> parameters) {
		// store expression results in temp DatumSamples
		DatumSamples s = new DatumSamples(samples);
		ExpressionRoot root = new ExpressionRoot(datum, s, parameters, service(getDatumService()),
				getOpModesService(), service(getMetadataService()), service(getLocationService()));
		populateExpressionDatumProperties(s, getExpressionConfigs(), root);

		// then extract generated property values as metrics
		for ( ExpressionConfig config : configs ) {
			final String metricName = config.getPropertyKey();
			if ( metricName == null || metricName.isEmpty() ) {
				continue;
			}

			Double n = s.getSampleDouble(config.getPropertyType(), config.getPropertyKey());
			if ( n != null ) {
				Metric m = Metric.sampleValue(datum.getTimestamp(), metricName, n.doubleValue());
				saveMetric(m);
			}
		}
	}

	private void saveMetric(Metric m) {
		try {
			metricDao.save(m);
		} catch ( DuplicateKeyException e ) {
			log.debug("Ignoring metric value update.");
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

		Iterable<ExpressionService> exprServices = services(getExpressionServices());
		if ( exprServices != null ) {
			ExpressionConfig[] exprConfs = getExpressionConfigs();
			List<ExpressionConfig> exprConfsList = (template ? singletonList(new ExpressionConfig())
					: (exprConfs != null ? asList(exprConfs) : emptyList()));
			result.add(SettingUtils.dynamicListSettingSpecifier("expressionConfigs", exprConfsList,
					new SettingUtils.KeyedListCallback<ExpressionConfig>() {

						@Override
						public Collection<SettingSpecifier> mapListSettingKey(ExpressionConfig value,
								int index, String key) {
							List<SettingSpecifier> exprSettings = ExpressionConfig.settings(
									MetricHarvesterDatumFilterService.class, key + ".", exprServices);
							// remove type as not used here
							exprSettings = exprSettings.stream().filter(s -> {
								return !((s instanceof KeyedSettingSpecifier<?>)
										&& ((KeyedSettingSpecifier<?>) s).getKey()
												.endsWith(".datumPropertyTypeKey"));
							}).collect(Collectors.toList());
							SettingSpecifier configGroup = new BasicGroupSettingSpecifier(exprSettings);
							return singletonList(configGroup);
						}
					}));
		}

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

	/**
	 * Get the expression configurations.
	 *
	 * @return the expression configurations
	 */
	public ExpressionConfig[] getExpressionConfigs() {
		return expressionConfigs;
	}

	/**
	 * Set the expression configurations to use.
	 *
	 * @param expressionConfigs
	 *        the configs to use
	 */
	public void setExpressionConfigs(ExpressionConfig[] expressionConfigs) {
		this.expressionConfigs = expressionConfigs;
	}

	/**
	 * Get the number of configured {@code expressionConfigs} elements.
	 *
	 * @return the number of {@code expressionConfigs} elements
	 */
	public int getExpressionConfigsCount() {
		ExpressionConfig[] confs = this.expressionConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code ExpressionTransformConfig}
	 * elements.
	 *
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link ExpressionConfig} instances.
	 * </p>
	 *
	 * @param count
	 *        The desired number of {@code expressionConfigs} elements.
	 */
	public void setExpressionConfigsCount(int count) {
		this.expressionConfigs = ArrayUtils.arrayWithLength(this.expressionConfigs, count,
				ExpressionConfig.class, null);
	}

}
