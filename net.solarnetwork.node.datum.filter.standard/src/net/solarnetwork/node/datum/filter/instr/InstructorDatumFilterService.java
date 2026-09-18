/* ==================================================================
 * InstructorDatumFilterService.java - 9/07/2026 12:01:38 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.filter.instr;

import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.service.OptionalServiceCollection.services;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Clock;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.ExpressionException;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.MutableDatumSamplesOperations;
import net.solarnetwork.node.domain.ExpressionRoot;
import net.solarnetwork.node.reactor.BasicInstruction;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.ReactorService;
import net.solarnetwork.node.service.support.BaseDatumFilterSupport;
import net.solarnetwork.node.service.support.ExpressionConfig;
import net.solarnetwork.service.DatumFilterService;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.service.support.ExpressionConfiguration;
import net.solarnetwork.service.support.ExpressionServiceExpression;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;

/**
 * Datum filter service that generates instructions if an expression-based
 * condition is met.
 *
 * @author matt
 * @version 1.0
 * @since 4.6
 */
public class InstructorDatumFilterService extends BaseDatumFilterSupport
		implements DatumFilterService, SettingSpecifierProvider {

	/** The setting UID. */
	public static final String SETTING_UID = "net.solarnetwork.node.datum.filter.instructor";

	/**
	 * The parameter name for an {@link Instruction} instance.
	 */
	public static final String INSTRUCTION_PARAM_NAME = "instruction";

	/**
	 * The parameter name for a {@link InstructionStatus} instance representing
	 * the instruction result.
	 */
	public static final String INSTRUCTION_RESULT_PARAM_NAME = "instructionResult";

	private final InstantSource clock;
	private final OptionalService<ReactorService> reactorService;
	private final OptionalService<InstructionExecutionService> instructionExecutionService;
	private InstructorConfig @Nullable [] instructorConfigs;

	/**
	 * Constructor.
	 *
	 * @param instructionExecutionService
	 *        the instruction execution service
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public InstructorDatumFilterService(
			OptionalService<InstructionExecutionService> instructionExecutionService) {
		this(new StaticOptionalService<>(null), instructionExecutionService);
	}

	/**
	 * Constructor.
	 *
	 * @param reactorService
	 *        the reactor service
	 * @param instructionExecutionService
	 *        the instruction execution service
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 * @since 4.6
	 */
	public InstructorDatumFilterService(OptionalService<ReactorService> reactorService,
			OptionalService<InstructionExecutionService> instructionExecutionService) {
		this(Clock.systemUTC(), reactorService, instructionExecutionService);
	}

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the clock to use
	 * @param reactorService
	 *        the reactor service
	 * @param instructionExecutionService
	 *        the instruction execution service
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 * @since 4.6
	 */
	public InstructorDatumFilterService(InstantSource clock,
			OptionalService<ReactorService> reactorService,
			OptionalService<InstructionExecutionService> instructionExecutionService) {
		super();
		this.clock = requireNonNullArgument(clock, "clock");
		this.reactorService = requireNonNullArgument(reactorService, "reactorService");
		this.instructionExecutionService = requireNonNullArgument(instructionExecutionService,
				"instructionExecutionService");
	}

	@Override
	public @Nullable DatumSamplesOperations filter(Datum datum, DatumSamplesOperations samples,
			@Nullable Map<String, Object> parameters) {
		final long start = incrementInputStats();
		if ( !conditionsMatch(datum, samples, parameters) ) {
			incrementIgnoredStats(start);
			return samples;
		}
		DatumSamplesOperations s = samples;

		// iterate over each valid configuration
		final ReactorService reactor = service(reactorService);
		final InstructionExecutionService execService = service(instructionExecutionService);
		final InstructorConfig[] configs = getInstructorConfigs();
		final Iterable<ExpressionService> expressionServices = services(getExpressionServices());
		if ( execService != null && configs != null && configs.length > 0
				&& expressionServices != null ) {
			final Map<String, Object> filterParams = smartPlaceholders(parameters);
			final DatumSamples mutableSamples = new DatumSamples(samples);
			final ExpressionRoot root = new ExpressionRoot(datum, mutableSamples, filterParams,
					service(getDatumService()), getOpModesService(), service(getMetadataService()),
					service(getLocationService()));
			root.setTariffScheduleProviders(getTariffScheduleProviders());
			root.setLocalStateDao(getLocalStateDao());

			final String instructorDescription = (getUid() != null ? getUid() : toString());

			s = mutableSamples;

			for ( InstructorConfig config : configs ) {
				if ( !config.isValid(expressionServices) ) {
					continue;
				}

				// evaluate the predicate to see if we need to generate an instruction
				final ExpressionConfiguration predicateConfig = nonnull(config.getPredicate(),
						"Predicate");

				try {
					final ExpressionServiceExpression predicate = predicateConfig
							.expression(expressionServices);
					if ( predicate == null ) {
						continue;
					}
					final Boolean predicateResult = predicate.getService().evaluateExpression(
							predicate.getExpression(), filterParams, root, null, Boolean.class);

					if ( log.isTraceEnabled() ) {
						log.trace(
								"Service [{}] evaluated predicate expression `{}` \u2192 {}\n\nExpression root: {}",
								instructorDescription, predicateConfig.getExpression(), predicateResult,
								root);
					} else if ( log.isDebugEnabled() ) {
						log.debug("Service [{}] evaluated predicate expression `{}` \u2192 {}",
								instructorDescription, predicateConfig.getExpression(), predicateResult);
					}

					if ( predicateResult == null || !predicateResult ) {
						continue;
					}
				} catch ( EvaluationException e ) {
					log.error("Error evaluating [{}] predicate expression `{}`: {}",
							instructorDescription, predicateConfig.getExpression(), e.getMessage());
					break;
				} catch ( ExpressionException e ) {
					log.error("Error parsing [{}] predicate expression `{}`: {}", instructorDescription,
							predicateConfig.getExpression(), e.getMessage());
					break;
				}

				// the predicate matched, so generate the instruction(s) now and execute
				for ( InstructionConfig instructionConfig : nonnull(config.getInstructions(),
						"Instruction configurations") ) {
					if ( !instructionConfig.isValid() ) {
						continue;
					}
					final BasicInstruction instr = createInstruction(instructorDescription,
							instructionConfig, root, filterParams, expressionServices);
					if ( instr != null ) {
						InstructionStatus instrResult = null;
						if ( reactor != null ) {
							final Instant executeAt = instr.getExecutionDate();
							final boolean futureExecution = (executeAt != null
									&& executeAt.isAfter(instr.getInstructionDate()));
							if ( futureExecution ) {
								instrResult = reactor.processInstruction(instr);
								log.info(
										"Deferred instruction {} {} saved as {} state for execution @ {}",
										instr.getId(), instr.getTopic(),
										instrResult.getInstructionState(), executeAt);
							} else {
								reactor.storeInstruction(instr);
							}
						}
						if ( instrResult == null ) {
							instrResult = execService.executeInstruction(instr);
							if ( instrResult != null && reactor != null ) {
								var instrWithStatus = new BasicInstruction(instr, instrResult);
								reactor.storeInstruction(instrWithStatus);
							}
						}
						if ( instrResult != null ) {
							processInstructionResult(instructorDescription, instructionConfig, root,
									filterParams, expressionServices, mutableSamples, instr,
									instrResult);
						} else {
							log.warn("Service [{}] instruction [{}] was not handled; parameters: {}",
									instructorDescription, instr.getTopic(),
									instr.getParameterMultiMap());
						}
					}
				}
			}
		}

		incrementStats(start, samples, s);
		return s != null && s.differsFrom(samples) ? s : samples;
	}

	private @Nullable BasicInstruction createInstruction(String instructorDescription,
			InstructionConfig config, ExpressionRoot root, Map<String, Object> filterParams,
			Iterable<ExpressionService> expressionServices) {
		final String topic = nonnull(config.getTopic(), "Topic");
		final Instant instructionDate = clock.instant();
		final BasicInstruction result = new BasicInstruction(
				net.solarnetwork.domain.Instruction.localId(), topic, instructionDate,
				Instruction.LOCAL_INSTRUCTION_ID, null);

		final ExpressionConfig[] paramConfigs = config.getParameters();
		if ( paramConfigs != null ) {
			for ( ExpressionConfig paramConfig : paramConfigs ) {
				final String paramName = paramConfig.getName();
				final String paramExpression = paramConfig.getExpression();
				if ( paramName == null || paramName.isEmpty() || paramExpression == null
						|| paramExpression.isEmpty() ) {
					continue;
				}
				try {
					// the parameter could be an expression or a plain string
					final ExpressionServiceExpression expr = paramConfig
							.getExpression(expressionServices);
					Object paramValue = null;
					if ( expr != null ) {
						// evaluate parameter value as expression
						paramValue = expr.getService().evaluateExpression(expr.getExpression(),
								filterParams, root, null, Object.class);

						if ( log.isTraceEnabled() ) {
							log.trace(
									"Service [{}] evaluated parameter [{}] expression `{}` \u2192 {}\n\nExpression root: {}",
									instructorDescription, paramName, paramExpression, paramValue, root);
						} else if ( log.isDebugEnabled() ) {
							log.debug("Service [{}] evaluated parameter [{}] expression `{}` \u2192 {}",
									instructorDescription, paramName, paramExpression, paramValue);
						}
					} else {
						// parameter value is plain string
						paramValue = paramExpression;
					}
					if ( paramValue != null ) {
						result.addParameter(paramName, paramValue.toString());
					}
				} catch ( EvaluationException e ) {
					log.error("Error evaluating [{}] parameter [{}] expression `{}`: {}",
							instructorDescription, paramName, paramExpression, e.getMessage());
					return null;
				} catch ( ExpressionException e ) {
					log.error("Error parsing [{}] parameter [{}] expression `{}`: {}",
							instructorDescription, paramName, paramExpression, e.getMessage());
					return null;
				}
			}
		}

		return result;
	}

	private void processInstructionResult(String instructorDescription, InstructionConfig config,
			ExpressionRoot root, Map<String, Object> filterParams,
			Iterable<ExpressionService> expressionServices, MutableDatumSamplesOperations samples,
			Instruction instruction, InstructionStatus instructionResult) {
		final ExpressionConfig[] responseConfigs = config.getResponses();
		if ( responseConfigs == null || responseConfigs.length < 1 ) {
			return;
		}
		filterParams.put(INSTRUCTION_PARAM_NAME, instruction);
		filterParams.put(INSTRUCTION_RESULT_PARAM_NAME, instructionResult);
		for ( ExpressionConfig responseConfig : responseConfigs ) {
			final String responseExpression = responseConfig.getExpression();
			if ( responseExpression == null || responseExpression.isEmpty() ) {
				continue;
			}
			final String datumPropName = responseConfig.getName();
			try {
				// the response could be an expression or a plain string
				final ExpressionServiceExpression expr = responseConfig
						.getExpression(expressionServices);
				Object responseValue = null;
				if ( expr != null ) {
					// evaluate response value as expression
					responseValue = expr.getService().evaluateExpression(expr.getExpression(),
							filterParams, root, null, Object.class);

					if ( log.isTraceEnabled() ) {
						log.trace(
								"Service [{}] evaluated response property [{}] expression `{}` \u2192 {}\n\nExpression root: {}",
								instructorDescription, datumPropName, responseExpression, responseValue,
								root);
					} else if ( log.isDebugEnabled() ) {
						log.debug(
								"Service [{}] evaluated response property [{}] expression `{}` \u2192 {}",
								instructorDescription, datumPropName, responseExpression, responseValue);
					}
				} else {
					// response value is plain string
					responseValue = responseExpression;
				}
				if ( responseConfig.getPropertyType() != null && datumPropName != null
						&& responseValue != null ) {
					samples.putSampleValue(responseConfig.getPropertyType(), datumPropName,
							responseValue);
				}
			} catch ( EvaluationException e ) {
				log.error("Error evaluating [{}] response property [{}] expression `{}`: {}",
						instructorDescription, datumPropName, responseExpression, e.getMessage());
				return;
			} catch ( ExpressionException e ) {
				log.error("Error parsing [{}] response property [{}] expression `{}`: {}",
						instructorDescription, datumPropName, responseExpression, e.getMessage());
				return;
			}
		}
	}

	@Override
	public String getSettingUid() {
		return SETTING_UID;
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
		final List<SettingSpecifier> result = baseIdentifiableSettings("");
		populateBaseSampleTransformSupportSettings(result);
		populateStatusSettings(result);

		final Iterable<ExpressionService> exprServices = services(getExpressionServices());

		final InstructorConfig[] instructorConfs = getInstructorConfigs();
		final List<InstructorConfig> instructorConfsList = (template
				? List.of(InstructorConfig.template())
				: (instructorConfs != null ? List.of(instructorConfs) : List.of()));
		result.add(SettingUtils.dynamicListSettingSpecifier("instructorConfigs", instructorConfsList,
				new SettingUtils.KeyedListCallback<>() {

					@Override
					public Collection<SettingSpecifier> mapListSettingKey(
							@Nullable InstructorConfig value, int index, String key) {
						SettingSpecifier configGroup = new BasicGroupSettingSpecifier(
								nonnull(value, "InstructorConfig").settings(template, key + ".",
										exprServices != null ? exprServices : List.of()));
						return List.of(configGroup);
					}
				}));

		return result;
	}

	/**
	 * Get the instruction configurations.
	 *
	 * @return the instruction configurations
	 */
	public final InstructorConfig @Nullable [] getInstructorConfigs() {
		return instructorConfigs;
	}

	/**
	 * Set the instruction configurations to use.
	 *
	 * @param instructorConfigs
	 *        the configs to use
	 */
	public final void setInstructorConfigs(InstructorConfig @Nullable [] instructorConfigs) {
		this.instructorConfigs = instructorConfigs;
	}

	/**
	 * Get the number of configured {@code instructorConfigs} elements.
	 *
	 * @return the number of {@code instructorConfigs} elements
	 */
	public final int getInstructorConfigsCount() {
		InstructorConfig[] confs = this.instructorConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code InstructorConfig} elements.
	 *
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link InstructorConfig} instances.
	 * </p>
	 *
	 * @param count
	 *        The desired number of {@code instructorConfigs} elements.
	 */
	public final void setInstructorConfigsCount(int count) {
		this.instructorConfigs = ArrayUtils.arrayWithLength(this.instructorConfigs, count,
				InstructorConfig.class, null);
	}

}
