/* ==================================================================
 * ParameterDatumFilterService.java - 5/03/2022 12:24:06 PM
 *
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.filter.param;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.service.OptionalServiceCollection.services;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.MapSampleOperations;
import net.solarnetwork.node.datum.filter.expr.ExpressionTransformConfig;
import net.solarnetwork.node.domain.ExpressionRoot;
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

/**
 * Transform service that executes a list of expressions, populating the results
 * on the active transform parameters.
 *
 * @author matt
 * @version 1.4
 * @since 2.1
 */
public class ParameterDatumFilterService extends BaseDatumFilterSupport
		implements DatumFilterService, SettingSpecifierProvider {

	private ExpressionConfig[] expressionConfigs;

	/**
	 * Constructor.
	 */
	public ParameterDatumFilterService() {
		super();
	}

	@Override
	public DatumSamplesOperations filter(Datum datum, DatumSamplesOperations samples,
			Map<String, Object> parameters) {
		final long start = incrementInputStats();
		if ( parameters == null || !conditionsMatch(datum, samples, parameters) ) {
			incrementIgnoredStats(start);
			return samples;
		}
		Map<String, Object> params = smartPlaceholders(parameters);
		MapSampleOperations s = new MapSampleOperations(new CopyingMap<>(parameters, params), samples);
		ExpressionRoot root = new ExpressionRoot(datum, s, params, service(getDatumService()),
				getOpModesService(), service(getMetadataService()), service(getLocationService()));
		root.setTariffScheduleProviders(getTariffScheduleProviders());
		populateExpressionDatumProperties(s, getExpressionConfigs(), root);
		incrementStats(start, samples, samples);
		return samples;
	}

	private static final class CopyingMap<K, V> extends AbstractMap<K, V> {

		private final Map<K, V> delegate;
		private final Map<K, V> copy;

		private CopyingMap(Map<K, V> delegate, Map<K, V> copy) {
			super();
			this.delegate = delegate;
			this.copy = copy;
		}

		@Override
		public Set<Entry<K, V>> entrySet() {
			return delegate.entrySet();
		}

		@Override
		public int size() {
			return copy.size();
		}

		@Override
		public boolean isEmpty() {
			return copy.isEmpty();
		}

		@Override
		public boolean containsKey(Object key) {
			return copy.containsKey(key);
		}

		@Override
		public V get(Object key) {
			return copy.get(key);
		}

		@Override
		public Set<K> keySet() {
			return copy.keySet();
		}

		@Override
		public V put(K key, V value) {
			copy.put(key, value);
			return delegate.put(key, value);
		}

	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.filter.std.param";
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
									ParameterDatumFilterService.class, key + ".", exprServices);
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
	 * {@link ExpressionTransformConfig} instances.
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
