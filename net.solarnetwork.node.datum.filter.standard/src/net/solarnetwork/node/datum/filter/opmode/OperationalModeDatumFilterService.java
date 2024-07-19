/* ==================================================================
 * OperationalModeDatumFilterService.java - 4/07/2021 1:26:27 PM
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
import static java.util.Collections.singletonList;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.service.OptionalServiceCollection.services;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.expression.ExpressionException;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.MutableDatumSamplesOperations;
import net.solarnetwork.node.domain.ExpressionRoot;
import net.solarnetwork.node.service.OperationalModesService;
import net.solarnetwork.node.service.support.BaseDatumFilterSupport;
import net.solarnetwork.service.DatumFilterService;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.service.support.ExpressionServiceExpression;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;

/**
 * Transform service that can toggle operational modes, optionally populating
 * the mode as a datum property.
 *
 * @author matt
 * @version 1.4
 * @since 2.0
 */
public class OperationalModeDatumFilterService extends BaseDatumFilterSupport
		implements DatumFilterService, SettingSpecifierProvider {

	private OperationalModeTransformConfig[] expressionConfigs;

	/**
	 * Constructor.
	 */
	public OperationalModeDatumFilterService() {
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
		final OperationalModesService opModesService = getOpModesService();
		final OperationalModeTransformConfig[] configs = getExpressionConfigs();
		final Iterable<ExpressionService> services = services(getExpressionServices());
		if ( opModesService == null || configs == null || configs.length < 1 || services == null ) {
			incrementIgnoredStats(start);
			return samples;
		}
		Map<String, Object> params = smartPlaceholders(parameters);
		ExpressionRoot root = new ExpressionRoot(datum, samples, params, service(getDatumService()),
				getOpModesService(), service(getMetadataService()), service(getLocationService()));
		DatumSamplesOperations s = samplesForEvaluation(samples, configs);
		if ( s instanceof MutableDatumSamplesOperations ) {
			evaluateExpressions((MutableDatumSamplesOperations) s, configs, root, services,
					opModesService);
		}
		incrementStats(start, samples, s);
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
	private static DatumSamplesOperations samplesForEvaluation(DatumSamplesOperations s,
			OperationalModeTransformConfig[] configs) {
		boolean needCopy = false;
		for ( OperationalModeTransformConfig config : configs ) {
			if ( config.getName() != null && !config.getName().trim().isEmpty()
					&& config.getDatumPropertyType() != null ) {
				needCopy = true;
				break;
			}
		}
		return (needCopy ? new DatumSamples(s) : s);
	}

	private void evaluateExpressions(final MutableDatumSamplesOperations d,
			final OperationalModeTransformConfig[] configs, final Object root,
			final Iterable<ExpressionService> services, final OperationalModesService opModesService) {
		for ( OperationalModeTransformConfig config : configs ) {
			if ( config.getOperationalMode() == null || config.getOperationalMode().isEmpty()
					|| config.getExpression() == null || config.getExpression().isEmpty() ) {
				continue;
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
					Instant expire = null;
					if ( config.getExpireSeconds() > 0 ) {
						expire = Instant.ofEpochMilli(System.currentTimeMillis()
								+ TimeUnit.SECONDS.toMillis(config.getExpireSeconds()));
					}
					opModesService.enableOperationalModes(singleton(config.getOperationalMode()),
							expire);
				} else if ( config.getExpireSeconds() < 1 ) {
					opModesService.disableOperationalModes(singleton(config.getOperationalMode()));
				}
				if ( config.getName() != null && !config.getName().trim().isEmpty()
						&& config.getDatumPropertyType() != null ) {
					Object propValue;
					if ( config.getDatumPropertyType() == DatumSamplesType.Status ) {
						propValue = exprResult;
					} else if ( config.getDatumPropertyType() == DatumSamplesType.Tag ) {
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
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.samplefilter.opmode";
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
			OperationalModeTransformConfig[] exprConfs = getExpressionConfigs();
			List<OperationalModeTransformConfig> exprConfsList = (template
					? singletonList(new OperationalModeTransformConfig())
					: (exprConfs != null ? asList(exprConfs) : emptyList()));
			result.add(SettingUtils.dynamicListSettingSpecifier("expressionConfigs", exprConfsList,
					new SettingUtils.KeyedListCallback<OperationalModeTransformConfig>() {

						@Override
						public Collection<SettingSpecifier> mapListSettingKey(
								OperationalModeTransformConfig value, int index, String key) {
							SettingSpecifier configGroup = new BasicGroupSettingSpecifier(
									OperationalModeTransformConfig.settings(key + ".", exprServices));
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
