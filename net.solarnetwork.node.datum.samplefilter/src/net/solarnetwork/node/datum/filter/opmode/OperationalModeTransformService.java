/* ==================================================================
 * OperationalModeTransformService.java - 4/07/2021 1:26:27 PM
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

package net.solarnetwork.node.datum.filter.opmode;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static net.solarnetwork.util.OptionalServiceCollection.services;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.joda.time.DateTime;
import org.springframework.expression.ExpressionException;
import net.solarnetwork.domain.DatumSamplesExpressionRoot;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.domain.MutableGeneralDatumSamplesOperations;
import net.solarnetwork.node.GeneralDatumSamplesTransformService;
import net.solarnetwork.node.OperationalModesService;
import net.solarnetwork.node.domain.Datum;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.SettingSpecifierProvider;
import net.solarnetwork.node.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.node.settings.support.SettingsUtil;
import net.solarnetwork.node.support.BaseSamplesTransformSupport;
import net.solarnetwork.support.ExpressionService;
import net.solarnetwork.support.ExpressionServiceExpression;
import net.solarnetwork.util.ArrayUtils;

/**
 * Transform service that can toggle operational modes, optionally populating
 * the mode as a datum property.
 * 
 * @author matt
 * @version 1.0
 * @since 1.8
 */
public class OperationalModeTransformService extends BaseSamplesTransformSupport
		implements GeneralDatumSamplesTransformService, SettingSpecifierProvider {

	private OperationalModeTransformConfig[] expressionConfigs;

	@Override
	public GeneralDatumSamples transformSamples(Datum datum, GeneralDatumSamples samples,
			Map<String, Object> parameters) {
		if ( !(sourceIdMatches(datum) && operationalModeMatches()) ) {
			return samples;
		}
		final OperationalModesService opModesService = getOpModesService();
		final OperationalModeTransformConfig[] configs = getExpressionConfigs();
		final Iterable<ExpressionService> services = services(getExpressionServices());
		if ( opModesService == null || configs == null || configs.length < 1 || services == null ) {
			return samples;
		}
		DatumSamplesExpressionRoot root = new DatumSamplesExpressionRoot(datum, samples, parameters);
		GeneralDatumSamples s = samplesForEvaluation(samples, configs);
		evaluateExpressions(s, configs, root, services, opModesService);
		return s;
	}

	/**
	 * Get the appropriate samples instance to use for evaluation.
	 * 
	 * <p>
	 * This method exists to avoid creating a copy of the samples unless we
	 * actually have to. We only have to if the operational mode is populated as
	 * a property. If we're just toggling operational modes there is no need.
	 * </p>
	 * 
	 * @param s
	 *        the samples
	 * @param configs
	 *        the configurations
	 * @return the samples to use
	 */
	private static GeneralDatumSamples samplesForEvaluation(GeneralDatumSamples s,
			OperationalModeTransformConfig[] configs) {
		boolean needCopy = false;
		for ( OperationalModeTransformConfig config : configs ) {
			if ( config.getName() != null && !config.getName().trim().isEmpty()
					&& config.getDatumPropertyType() != null ) {
				needCopy = true;
				break;
			}
		}
		return (needCopy ? new GeneralDatumSamples(s) : s);
	}

	private void evaluateExpressions(final MutableGeneralDatumSamplesOperations d,
			final OperationalModeTransformConfig[] configs, final Object root,
			final Iterable<ExpressionService> services, final OperationalModesService opModesService) {
		for ( OperationalModeTransformConfig config : configs ) {
			if ( config.getOperationalMode() == null || config.getOperationalMode().isEmpty()
					|| config.getExpression() == null || config.getExpression().isEmpty() ) {
				continue;
			}

			if ( config.getExpireSeconds() > 0 ) {
				if ( opModesService.isOperationalModeActive(config.getOperationalMode()) ) {
					// not expired, so don't bother evaluating expression
					log.debug(
							"Operational mode [{}] already active for configuration with expire seconds {}; not evaluating expression.",
							config.getOperationalMode(), config.getExpireSeconds());
					continue;
				}
			}

			final ExpressionServiceExpression expr;
			try {
				expr = config.getExpression(services);
			} catch ( ExpressionException e ) {
				log.warn("Error parsing property [{}] expression `{}`: {}", config.getName(),
						config.getExpression(), e.getMessage());
				return;
			}

			Boolean exprResult = null;
			if ( expr != null ) {
				try {
					exprResult = expr.getService().evaluateExpression(expr.getExpression(), null, root,
							null, Boolean.class);
					if ( log.isTraceEnabled() ) {
						log.trace(
								"Service [{}] evaluated operational mode [{}] expression `{}` \u2192 {}\n\nExpression root: {}",
								getUid(), config.getOperationalMode(), config.getExpression(),
								exprResult, root);
					} else if ( log.isDebugEnabled() ) {
						log.debug(
								"Service [{}] evaluated operational mode [{}] expression `{}` \u2192 {}",
								getUid(), config.getOperationalMode(), config.getExpression(),
								exprResult);
					}
				} catch ( ExpressionException e ) {
					log.warn(
							"Error evaluating service [{}] operational mode [{}] expression `{}`: {}\n\nExpression root: {}",
							getUid(), config.getOperationalMode(), config.getExpression(),
							e.getMessage(), root);
				}
			}
			if ( exprResult != null ) {
				if ( exprResult.booleanValue() ) {
					DateTime expire = null;
					if ( config.getExpireSeconds() > 0 ) {
						expire = new DateTime(System.currentTimeMillis()
								+ TimeUnit.SECONDS.toMillis(config.getExpireSeconds()));
					}
					opModesService.enableOperationalModes(singleton(config.getOperationalMode()),
							expire);
				} else {
					opModesService.disableOperationalModes(singleton(config.getOperationalMode()));
				}
				if ( config.getName() != null && !config.getName().trim().isEmpty()
						&& config.getDatumPropertyType() != null ) {
					Object propValue;
					if ( config.getDatumPropertyType() == GeneralDatumSamplesType.Status ) {
						propValue = exprResult;
					} else if ( config.getDatumPropertyType() == GeneralDatumSamplesType.Tag ) {
						propValue = (exprResult ? config.getName() : null);
					} else {
						propValue = (exprResult.booleanValue() ? 1 : 0);
					}
					d.putSampleValue(config.getDatumPropertyType(), config.getName(), propValue);
				}
			}
		}
	}

	@Override
	public String getSettingUID() {
		return "net.solarnetwork.node.datum.samplefilter.opmode";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = baseIdentifiableSettings("");
		populateBaseSampleTransformSupportSettings(result);

		Iterable<ExpressionService> exprServices = services(getExpressionServices());
		if ( exprServices != null ) {
			OperationalModeTransformConfig[] exprConfs = getExpressionConfigs();
			List<OperationalModeTransformConfig> exprConfsList = (exprConfs != null ? asList(exprConfs)
					: emptyList());
			result.add(SettingsUtil.dynamicListSettingSpecifier("expressionConfigs", exprConfsList,
					new SettingsUtil.KeyedListCallback<OperationalModeTransformConfig>() {

						@Override
						public Collection<SettingSpecifier> mapListSettingKey(
								OperationalModeTransformConfig value, int index, String key) {
							SettingSpecifier configGroup = new BasicGroupSettingSpecifier(
									OperationalModeTransformConfig.settings(key + ".", exprServices));
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
	public OperationalModeTransformConfig[] getExpressionConfigs() {
		return expressionConfigs;
	}

	/**
	 * Set the expression configurations to use.
	 * 
	 * @param expressionConfigs
	 *        the configs to use
	 */
	public void setExpressionConfigs(OperationalModeTransformConfig[] expressionConfigs) {
		this.expressionConfigs = expressionConfigs;
	}

	/**
	 * Get the number of configured {@code expressionConfigs} elements.
	 * 
	 * @return the number of {@code expressionConfigs} elements
	 */
	public int getExpressionConfigsCount() {
		final OperationalModeTransformConfig[] confs = getExpressionConfigs();
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code OperationalModeTransformConfig}
	 * elements.
	 * 
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link OperationalModeTransformConfig} instances.
	 * </p>
	 * 
	 * @param count
	 *        The desired number of {@code expressionConfigs} elements.
	 */
	public void setExpressionConfigsCount(int count) {
		this.expressionConfigs = ArrayUtils.arrayWithLength(getExpressionConfigs(), count,
				OperationalModeTransformConfig.class, null);
	}

}
