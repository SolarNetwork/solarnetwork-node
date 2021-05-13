/* ==================================================================
 * ExpressionTransformService.java - 13/05/2021 8:27:17 PM
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

package net.solarnetwork.node.datum.samplefilter.expr;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static net.solarnetwork.util.OptionalServiceCollection.services;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.solarnetwork.domain.DatumSamplesExpressionRoot;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.node.GeneralDatumSamplesTransformService;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.domain.ExpressionConfig;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.node.settings.support.SettingsUtil;
import net.solarnetwork.node.support.BaseSamplesTransformSupport;
import net.solarnetwork.support.ExpressionService;
import net.solarnetwork.util.ArrayUtils;

/**
 * Transform service that executes a list of expressions, populating the results
 * on the output samples.
 * 
 * @author matt
 * @version 1.0
 * @since 1.6
 */
public class ExpressionTransformService extends BaseSamplesTransformSupport
		implements GeneralDatumSamplesTransformService, SettingSpecifierProvider {

	private ExpressionTransformConfig[] expressionConfigs;

	@Override
	public GeneralDatumSamples transformSamples(Datum datum, GeneralDatumSamples samples,
			Map<String, Object> parameters) {
		if ( !sourceIdMatches(datum) ) {
			return samples;
		}
		DatumSamplesExpressionRoot root = new DatumSamplesExpressionRoot(datum, samples, parameters);
		GeneralDatumSamples s = new GeneralDatumSamples(samples);
		populateExpressionDatumProperties(s, getExpressionConfigs(), root);
		return s;
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.datum.samplefilter.expression";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = baseIdentifiableSettings("");
		result.add(new BasicTextFieldSettingSpecifier("sourceId", ""));

		Iterable<ExpressionService> exprServices = services(getExpressionServices());
		if ( exprServices != null ) {
			ExpressionTransformConfig[] exprConfs = getExpressionConfigs();
			List<ExpressionConfig> exprConfsList = (exprConfs != null ? asList(exprConfs) : emptyList());
			result.add(SettingsUtil.dynamicListSettingSpecifier("expressionConfigs", exprConfsList,
					new SettingsUtil.KeyedListCallback<ExpressionConfig>() {

						@Override
						public Collection<SettingSpecifier> mapListSettingKey(ExpressionConfig value,
								int index, String key) {
							SettingSpecifier configGroup = new BasicGroupSettingSpecifier(
									ExpressionConfig.settings(ExpressionTransformService.class,
											key + ".", exprServices));
							return Collections.singletonList(configGroup);
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
