/* ==================================================================
 * ExpressionDatumFilterService.java - 13/05/2021 8:27:17 PM
 *
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.filter.expr;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.service.OptionalServiceCollection.services;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.node.domain.ExpressionRoot;
import net.solarnetwork.node.service.support.BaseDatumFilterSupport;
import net.solarnetwork.node.service.support.ExpressionConfig;
import net.solarnetwork.service.DatumFilterService;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;

/**
 * Transform service that executes a list of expressions, populating the results
 * on the output samples.
 *
 * @author matt
 * @version 1.5
 * @since 2.0
 */
public class ExpressionDatumFilterService extends BaseDatumFilterSupport
		implements DatumFilterService, SettingSpecifierProvider {

	private ExpressionTransformConfig[] expressionConfigs;

	/**
	 * Constructor.
	 */
	public ExpressionDatumFilterService() {
		super();
	}

	@Override
	public DatumSamplesOperations filter(Datum datum, DatumSamplesOperations samples,
			Map<String, Object> parameters) {
		final long start = incrementInputStats();
		if ( !conditionsMatch(datum, samples, parameters) ) {
			incrementIgnoredStats(start);
			return samples;
		}
		Map<String, Object> params = smartPlaceholders(parameters);
		DatumSamples s = new DatumSamples(samples);
		ExpressionRoot root = new ExpressionRoot(datum, s, params, service(getDatumService()),
				getOpModesService(), service(getMetadataService()), service(getLocationService()));
		root.setTariffScheduleProviders(getTariffScheduleProviders());
		populateExpressionDatumProperties(s, getExpressionConfigs(), root);
		incrementStats(start, samples, s);
		return s;
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.samplefilter.expression";
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
			ExpressionTransformConfig[] exprConfs = getExpressionConfigs();
			List<ExpressionConfig> exprConfsList = (template ? singletonList(new ExpressionConfig())
					: (exprConfs != null ? asList(exprConfs) : emptyList()));
			result.add(SettingUtils.dynamicListSettingSpecifier("expressionConfigs", exprConfsList,
					new SettingUtils.KeyedListCallback<ExpressionConfig>() {

						@Override
						public Collection<SettingSpecifier> mapListSettingKey(ExpressionConfig value,
								int index, String key) {
							SettingSpecifier configGroup = new BasicGroupSettingSpecifier(
									ExpressionConfig.settings(ExpressionDatumFilterService.class,
											key + ".", exprServices));
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
	public ExpressionTransformConfig[] getExpressionConfigs() {
		return expressionConfigs;
	}

	/**
	 * Set the expression configurations to use.
	 *
	 * @param expressionConfigs
	 *        the configs to use
	 */
	public void setExpressionConfigs(ExpressionTransformConfig[] expressionConfigs) {
		this.expressionConfigs = expressionConfigs;
	}

	/**
	 * Get the number of configured {@code expressionConfigs} elements.
	 *
	 * @return the number of {@code expressionConfigs} elements
	 */
	public int getExpressionConfigsCount() {
		ExpressionTransformConfig[] confs = this.expressionConfigs;
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
				ExpressionTransformConfig.class, null);
	}

}
