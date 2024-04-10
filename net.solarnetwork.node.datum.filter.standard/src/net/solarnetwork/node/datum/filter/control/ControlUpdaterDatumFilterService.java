/* ==================================================================
 * ControlUpdaterDatumFilterService.java - 13/06/2023 6:31:35 am
 *
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.filter.control;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.service.OptionalServiceCollection.services;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.expression.ExpressionException;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.node.domain.ExpressionRoot;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.support.BaseDatumFilterSupport;
import net.solarnetwork.node.service.support.ExpressionConfig;
import net.solarnetwork.service.DatumFilterService;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.support.ExpressionServiceExpression;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.ObjectUtils;

/**
 * Transform service that sets a control value based on an expression result.
 *
 * @author matt
 * @version 1.2
 */
public class ControlUpdaterDatumFilterService extends BaseDatumFilterSupport
		implements DatumFilterService, SettingSpecifierProvider {

	private final OptionalService<InstructionExecutionService> instructionExecutionService;
	private ControlConfig[] controlConfigs;

	/**
	 * Constructor.
	 *
	 * @param instructionExecutionService
	 *        the instruction execution service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ControlUpdaterDatumFilterService(
			OptionalService<InstructionExecutionService> instructionExecutionService) {
		super();
		this.instructionExecutionService = ObjectUtils
				.requireNonNullArgument(instructionExecutionService, "instructionExecutionService");
	}

	@Override
	public DatumSamplesOperations filter(Datum datum, DatumSamplesOperations samples,
			Map<String, Object> parameters) {
		final long start = incrementInputStats();
		if ( !conditionsMatch(datum, samples, parameters) ) {
			incrementIgnoredStats(start);
			return samples;
		}
		final ControlConfig[] configs = validControlConfigs();
		DatumSamplesOperations s = samples;
		if ( configs != null && configs.length > 0 ) {
			Map<String, Object> params = smartPlaceholders(parameters);
			DatumSamples mutableSamples = new DatumSamples(samples);
			ExpressionRoot root = new ExpressionRoot(datum, mutableSamples, params,
					service(getDatumService()), getOpModesService(), service(getMetadataService()));
			updateControlValues(mutableSamples, configs, root);
			s = mutableSamples;
		}
		incrementStats(start, samples, s);
		return s;
	}

	private void updateControlValues(DatumSamples s, ControlConfig[] configs, ExpressionRoot root) {
		final Iterable<ExpressionService> services = services(getExpressionServices());
		final InstructionExecutionService executor = service(instructionExecutionService);
		if ( executor == null || services == null || configs == null || configs.length < 1
				|| root == null ) {
			return;
		}
		for ( ControlConfig config : configs ) {
			final ExpressionServiceExpression expr;
			try {
				expr = config.getExpression(services);
			} catch ( ExpressionException e ) {
				log.warn("Error parsing property [{}] expression `{}`: {}", config.getName(),
						config.getExpression(), e.getMessage());
				return;
			}

			Object exprResult = null;
			if ( expr != null ) {
				try {
					exprResult = expr.getService().evaluateExpression(expr.getExpression(), null, root,
							null, Object.class);
					if ( log.isTraceEnabled() ) {
						log.trace(
								"Service [{}] evaluated control [{}] expression `{}` \u2192 {}\n\nExpression root: {}",
								getUid(), config.getControlId(), config.getExpression(), exprResult,
								root);
					} else if ( log.isDebugEnabled() ) {
						log.debug("Service [{}] evaluated control [{}] expression `{}` \u2192 {}",
								getUid(), config.getControlId(), config.getExpression(), exprResult);
					}
				} catch ( ExpressionException e ) {
					log.warn(
							"Error evaluating service [{}] control [{}] expression `{}`: {}\n\nExpression root: {}",
							getUid(), config.getControlId(), config.getExpression(), e.getMessage(),
							root);
				}
			}
			if ( exprResult != null ) {
				Instruction instr = InstructionUtils
						.createSetControlValueLocalInstruction(config.getControlId(), exprResult);
				InstructionStatus status = executor.executeInstruction(instr);
				if ( status.getInstructionState() == InstructionState.Completed ) {
					log.info("Service [{}] set control [{}] to [{}]", getUid(), config.getControlId(),
							exprResult);
					if ( config.getName() != null ) {
						s.putSampleValue(config.getDatumPropertyType(), config.getName(), exprResult);
					}
				} else {
					Object reason = status.getInstructionState();
					Map<String, ?> resultParams = status.getResultParameters();
					if ( resultParams.containsKey(InstructionStatus.MESSAGE_RESULT_PARAM) ) {
						if ( resultParams.containsKey(InstructionStatus.ERROR_CODE_RESULT_PARAM) ) {
							reason = String.format("%s (error %s)",
									resultParams.get(InstructionStatus.MESSAGE_RESULT_PARAM),
									resultParams.get(InstructionStatus.ERROR_CODE_RESULT_PARAM));
						} else {
							reason = resultParams.get(InstructionStatus.MESSAGE_RESULT_PARAM);
						}
					} else if ( resultParams.containsKey(InstructionStatus.ERROR_CODE_RESULT_PARAM) ) {
						reason = String.format("error %s",
								resultParams.get(InstructionStatus.ERROR_CODE_RESULT_PARAM));
					}
					log.error("Service [{}] failed to set control [{}] to [{}]: {}", getUid(),
							config.getControlId(), exprResult, reason);
				}
			}
		}
	}

	private ControlConfig[] validControlConfigs() {
		final ControlConfig[] configs = getControlConfigs();
		if ( configs == null || configs.length < 1 ) {
			return null;
		}
		return Arrays.stream(configs).filter(ControlConfig::isValid).toArray(ControlConfig[]::new);
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.filter.control.update";
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

		ControlConfig[] controlConfs = getControlConfigs();
		List<ExpressionConfig> controlConfsList = (template ? singletonList(new ControlConfig())
				: (controlConfs != null ? asList(controlConfs) : emptyList()));
		result.add(SettingUtils.dynamicListSettingSpecifier("controlConfigs", controlConfsList,
				new SettingUtils.KeyedListCallback<ExpressionConfig>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(ExpressionConfig value,
							int index, String key) {
						SettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								ControlConfig.settings(key + ".", exprServices));
						return singletonList(configGroup);
					}
				}));

		return result;
	}

	/**
	 * Get the control configurations.
	 *
	 * @return the control configurations
	 */
	public ControlConfig[] getControlConfigs() {
		return controlConfigs;
	}

	/**
	 * Set the control configurations to use.
	 *
	 * @param controlConfigs
	 *        the configs to use
	 */
	public void setControlConfigs(ControlConfig[] controlConfigs) {
		this.controlConfigs = controlConfigs;
	}

	/**
	 * Get the number of configured {@code controlConfigs} elements.
	 *
	 * @return the number of {@code controlConfigs} elements
	 */
	public int getControlConfigsCount() {
		ControlConfig[] confs = this.controlConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code ControlConfig} elements.
	 *
	 * <p>
	 * Any newly added element values will be set to new {@link ControlConfig}
	 * instances.
	 * </p>
	 *
	 * @param count
	 *        The desired number of {@code controlConfigs} elements.
	 */
	public void setControlConfigsCount(int count) {
		this.controlConfigs = ArrayUtils.arrayWithLength(this.controlConfigs, count, ControlConfig.class,
				null);
	}

}
