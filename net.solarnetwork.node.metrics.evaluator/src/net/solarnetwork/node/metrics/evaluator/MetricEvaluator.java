/* ==================================================================
 * MetricEvaluator.java - 22/07/2024 12:24:25 pm
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

package net.solarnetwork.node.metrics.evaluator;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static net.solarnetwork.node.metrics.evaluator.NamedMetricAggregate.namedMetricAggregate;
import static net.solarnetwork.node.reactor.InstructionUtils.createErrorResultParameters;
import static net.solarnetwork.node.reactor.InstructionUtils.createStatus;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.service.OptionalServiceCollection.services;
import static net.solarnetwork.util.NumberUtils.bigDecimalForNumber;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringNaturalSortComparator.CASE_INSENSITIVE_NATURAL_SORT;
import static net.solarnetwork.util.StringUtils.commaDelimitedStringToSet;
import static net.solarnetwork.util.StringUtils.delimitedStringFromCollection;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.expression.ExpressionException;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.domain.ExpressionRoot;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.job.JobService;
import net.solarnetwork.node.metrics.dao.BasicMetricFilter;
import net.solarnetwork.node.metrics.dao.MetricDao;
import net.solarnetwork.node.metrics.domain.BasicMetricAggregate;
import net.solarnetwork.node.metrics.domain.Metric;
import net.solarnetwork.node.metrics.domain.MetricAggregate;
import net.solarnetwork.node.metrics.domain.MetricKey;
import net.solarnetwork.node.metrics.domain.ParameterizedMetricAggregate;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.reactor.InstructionUtils;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.node.service.DatumService;
import net.solarnetwork.node.service.DatumSourceIdProvider;
import net.solarnetwork.node.service.MetadataService;
import net.solarnetwork.node.service.OperationalModesService;
import net.solarnetwork.node.service.TariffScheduleProvider;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.node.service.support.ExpressionConfig;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.service.FilterableService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.OptionalServiceCollection;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.service.support.ExpressionServiceExpression;
import net.solarnetwork.settings.KeyedSettingSpecifier;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingsChangeObserver;
import net.solarnetwork.settings.support.BasicGroupSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextAreaSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.ArrayUtils;
import net.solarnetwork.util.DateUtils;
import net.solarnetwork.util.StatTracker;
import net.solarnetwork.util.StatTracker.Accumulation;
import net.solarnetwork.util.StringUtils;

/**
 * Query a set of metric aggregate values and evaluate a set of expressions
 * using the metric values as input parameters.
 *
 * <p>
 * The first expression to return a non-null result will then trigger an
 * instruction to be issued, passing the result as an instruction parameter.
 * </p>
 *
 * <p>
 * The configured {@link MetricDao} must provide the necessary metrics to
 * satisfy this service.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class MetricEvaluator extends BaseIdentifiable
		implements JobService, SettingsChangeObserver, ServiceLifecycleObserver, DatumSourceIdProvider {

	/** The {@code metricAggregateTimeOffsetStart} property default value. */
	public static final Duration DEFAULT_METRIC_AGGREGATE_TIME_OFFSET_START = Duration.ofDays(5);

	/** The {@code metricAggregateTimeOffsetEnd} property default value. */
	public static final Duration DEFAULT_METRIC_AGGREGATE_TIME_OFFSET_END = Duration.ZERO;

	/** A metric name for a minute-of day integer value automatically added. */
	public static final String METRIC_MINUTE_OF_DAY = "minuteOfDay";

	/** The {@code metricAggregates} property default value. */
	public List<NamedMetricAggregate> DEFAULT_METRIC_AGGREGATES = Collections
			.unmodifiableList(Arrays.asList(namedMetricAggregate(BasicMetricAggregate.Minimum),
					namedMetricAggregate(BasicMetricAggregate.Maximum),
					namedMetricAggregate(BasicMetricAggregate.Average),
					namedMetricAggregate(ParameterizedMetricAggregate.METRIC_TYPE_QUANTILE_25, "p1"),
					namedMetricAggregate(ParameterizedMetricAggregate.METRIC_TYPE_QUANTILE_75, "p2")));

	/** The {@code stats.logFrequency} default value. */
	public static final int DEFAULT_STAT_LOG_FREQUENCY = 100;

	/** The maximum age to query for the most-recent available latestMetrics. */
	private static final Duration MOST_RECENT_METRIC_MAX_AGE = Duration.ofDays(1);

	private final Clock clock;
	private final StatTracker stats;
	private final MetricDao metricDao;
	private final OptionalService<InstructionExecutionService> instructionExecutionService;

	private final ConcurrentMap<String, Metric> latestMetrics = new ConcurrentHashMap<>(8, 0.9f, 2);
	private final ConcurrentMap<String, ConcurrentMap<String, Metric>> latestMetricAggregations = new ConcurrentHashMap<>(
			8, 0.9f, 2);

	private OptionalService<OperationalModesService> opModesService;
	private OptionalService<DatumService> datumService;
	private OptionalService<DatumQueue> datumQueue;
	private OptionalServiceCollection<TariffScheduleProvider> tariffScheduleProviders;

	private Set<String> metrics;
	private Duration metricAggregateTimeOffsetStart = DEFAULT_METRIC_AGGREGATE_TIME_OFFSET_START;
	private Duration metricAggregateTimeOffsetEnd = DEFAULT_METRIC_AGGREGATE_TIME_OFFSET_END;
	private Map<String, NamedMetricAggregate> metricAggregates = aggregateMap(DEFAULT_METRIC_AGGREGATES);
	private String parametersMetadataPath;
	private String outputInstructionTopic;
	private String outputInstructionParam;
	private String evaluationSourceId;
	private String requiredOperationalMode;
	private ExpressionConfig[] paramExpressionConfigs;
	private ExpressionConfig[] expressionConfigs;

	private String[] metricNames;
	private EvaluationResult lastEvaluationResult;

	/**
	 * Generate a key mapping of aggregates.
	 *
	 * @param aggregates
	 *        the aggregates to generate the mapping from
	 * @return a map of {@code key} values to associated aggregates
	 */
	private static Map<String, NamedMetricAggregate> aggregateMap(
			Collection<NamedMetricAggregate> aggregates) {
		Map<String, NamedMetricAggregate> result = new LinkedHashMap<>(aggregates.size());
		for ( NamedMetricAggregate agg : aggregates ) {
			result.put(agg.key(), agg);
		}
		return result;
	}

	/**
	 * Constructor.
	 *
	 * @param metricDao
	 *        the metric DAO
	 * @param instructionExecutionService
	 *        the instruction execution service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public MetricEvaluator(MetricDao metricDao,
			OptionalService<InstructionExecutionService> instructionExecutionService) {
		this(Clock.systemUTC(), new StatTracker("MetricEvaluator", null,
				LoggerFactory.getLogger(MetricEvaluator.class), DEFAULT_STAT_LOG_FREQUENCY), metricDao,
				instructionExecutionService);
	}

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the clock to use
	 * @param stats
	 *        the statistics instance to use
	 * @param metricDao
	 *        the metric DAO
	 * @param instructionExecutionService
	 *        the instruction execution service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public MetricEvaluator(Clock clock, StatTracker stats, MetricDao metricDao,
			OptionalService<InstructionExecutionService> instructionExecutionService) {
		super();
		this.clock = requireNonNullArgument(clock, "clock");
		this.stats = requireNonNullArgument(stats, "stats");
		this.metricDao = requireNonNullArgument(metricDao, "metricDao");
		this.instructionExecutionService = requireNonNullArgument(instructionExecutionService,
				"instructionExecutionService");
		configurationChanged(null);
	}

	@Override
	public void serviceDidStartup() {
		configurationChanged(null);
	}

	@Override
	public void serviceDidShutdown() {
		// nothing here
	}

	@Override
	public synchronized void configurationChanged(Map<String, Object> properties) {
		metricNames = metrics != null ? metrics.toArray(new String[metrics.size()]) : new String[0];

		// paramExpression types forced to status to allow string values
		final ExpressionConfig[] exprConfigs = getParamExpressionConfigs();
		if ( exprConfigs != null ) {
			for ( ExpressionConfig config : exprConfigs ) {
				config.setDatumPropertyType(DatumSamplesType.Status);
			}
		}
	}

	@Override
	public Collection<String> publishedSourceIds() {
		final String sourceId = resolvePlaceholders(evaluationSourceId);
		return (sourceId != null ? Collections.singleton(sourceId) : Collections.emptyList());
	}

	@Override
	public void executeJobService() throws Exception {
		stats.increment(MetricEvaluatorStat.EvaluationCount);
		final Instant now = clock.instant();
		try {
			updateLatestMetrics();

			final ExpressionConfig[] expressionConfs = getExpressionConfigs();
			if ( expressionConfs == null || expressionConfs.length < 1 ) {
				return;
			}

			final Map<String, Object> inputs = new LinkedHashMap<>(32);
			inputs.putAll(parameters());

			for ( Metric m : latestMetrics.values() ) {
				inputs.put(m.getName(), m.getValue());
			}
			for ( ConcurrentMap<String, Metric> mapping : latestMetricAggregations.values() ) {
				for ( Metric m : mapping.values() ) {
					NamedMetricAggregate agg = metricAggregates.get(m.getType());
					if ( agg != null ) {
						inputs.put(agg.parameterNameValue(m), m.getValue());
					}
				}
			}

			final Instant exprStart = clock.instant();
			EvaluationResult result = null;
			try {
				// evaluate parameter expressions
				String error = populateParamExpressionParameters(inputs, getParamExpressionConfigs());
				if ( error != null ) {
					result = new EvaluationResult(now, error, inputs);
				} else {
					// add minute-of-day value
					inputs.put(METRIC_MINUTE_OF_DAY,
							now.atZone(ZoneId.systemDefault()).get(ChronoField.MINUTE_OF_DAY));

					log.debug("MetricEvaluator [{}] evaluating inputs: {}", getUid(), inputs);

					result = evaluate(now, expressionConfs, inputs);

					log.debug("MetricEvaluator [{}] evaluated result: {}", getUid(), result.output);
				}
			} finally {
				stats.add(MetricEvaluatorStat.ExpressionExeucutionTime,
						ChronoUnit.MILLIS.between(exprStart, clock.instant()));
				if ( result != null && result.outputExpressionIndex >= 0 ) {
					stats.increment(MetricEvaluatorStat.ExpressionCount.name() + ' '
							+ (result.outputExpressionIndex + 1));
				}
			}
			if ( result != null && result.output != null && operationalModeMatches() ) {
				final String instrTopic = getOutputInstructionTopic();
				if ( instrTopic != null && !instrTopic.isEmpty() ) {
					result = executeInstruction(result, instrTopic);
				}
			}
			lastEvaluationResult = result;
			publishDatum(result);
		} finally {
			stats.add(MetricEvaluatorStat.ProcessingTime,
					ChronoUnit.MILLIS.between(now, clock.instant()));
		}
	}

	private EvaluationResult executeInstruction(EvaluationResult result, String instrTopic) {
		final InstructionExecutionService service = service(instructionExecutionService);
		if ( service != null ) {
			final Instant now = clock.instant();
			try {
				final String paramName = getOutputInstructionParam();
				final String paramVal = (paramName != null && !paramName.isEmpty()
						? (result.output instanceof BigDecimal
								? ((BigDecimal) result.output).toPlainString()
								: result.output.toString())
						: null);
				Instruction instr = InstructionUtils.createLocalInstruction(instrTopic, paramName,
						paramVal);
				InstructionStatus status = service.executeInstruction(instr);
				if ( status == null ) {
					status = createStatus(instr, InstructionState.Declined, createErrorResultParameters(
							"Instruction not handled. Missing control?", "MEV.0001"));
				}
				if ( status.getInstructionState() != InstructionState.Completed ) {
					log.warn(
							"Instruction [{}] with [{}] -> [{}] ended in [{}] state, result parameters: {}",
							instrTopic, paramName, paramVal, status.getInstructionState(),
							status.getResultParameters());
				} else {
					log.debug("Instruction [{}] with [{}] -> [{}] completed.", instrTopic, paramName,
							paramVal);
				}
				return result.withInstructionStatus(instr, status);
			} finally {
				stats.add(MetricEvaluatorStat.InstructionExecutionTime,
						ChronoUnit.MILLIS.between(now, clock.instant()));
			}
		} else {
			log.warn(
					"No InstructionExecutionService available to execute [{}] instruction with [{}] -> [{}]",
					instrTopic, getOutputInstructionParam(), result);
			return new EvaluationResult(result.ts,
					getMessageSource().getMessage("evaluation.noInstructionExecutionService", null,
							"No InstructionExecutionService avaialble.", Locale.getDefault()),
					result.inputs, result.output, result.outputExpressionIndex);
		}
	}

	/**
	 * Evaluate expressions and save the results as new parameter values.
	 *
	 * @param parameters
	 *        the expression parameters, and where to store expression results
	 * @param expressionConfs
	 *        the expressions to evaluate
	 * @return an error message, or {@literal null} if no error occurred
	 */
	private String populateParamExpressionParameters(final Map<String, Object> parameters,
			final ExpressionConfig[] expressionConfs) {
		if ( expressionConfs == null || expressionConfs.length < 1 ) {
			return null;
		}

		Iterable<ExpressionService> services = services(getExpressionServices());
		if ( services == null ) {
			return getMessageSource().getMessage("evaluation.noExpressionServices", null,
					"No expression services available.", Locale.getDefault());
		}

		final ExpressionRoot root = new ExpressionRoot(null, null, parameters,
				service(getDatumService()), service(getOpModesService()), service(getMetadataService()),
				service(getLocationService()));
		root.setTariffScheduleProviders(tariffScheduleProviders);
		root.setLocalStateDao(getLocalStateDao());

		int exprIdx = -1;
		for ( ExpressionConfig config : expressionConfs ) {
			exprIdx++;
			if ( config.getExpression() == null || config.getExpression().isEmpty() ) {
				continue;
			}
			final ExpressionServiceExpression expr;
			try {
				expr = config.getExpression(services);
			} catch ( ExpressionException e ) {
				log.warn("Error parsing MetricEvaluator [{}] parameter expression `{}`: {}", getUid(),
						config.getExpression(), e.getMessage());
				return getMessageSource().getMessage("evaluation.expressionSyntaxError",
						new Object[] { (exprIdx + 1), e.getMessage() },
						"Error parsing parameter expression " + (exprIdx + 1) + ": " + e.getMessage(),
						Locale.getDefault());
			}

			Object exprResult = null;
			if ( expr != null ) {
				try {
					exprResult = expr.getService().evaluateExpression(expr.getExpression(), null, root,
							null, Object.class);
					if ( log.isTraceEnabled() ) {
						log.trace(
								"MetricEvaluator [{}] evaluated parameter expression `{}` \u2192 {}\n\nExpression root: {}",
								getUid(), config.getExpression(), exprResult, root);
					} else if ( log.isDebugEnabled() ) {
						log.debug("MetricEvaluator [{}] evaluated parameter expression `{}` \u2192 {}",
								getUid(), config.getExpression(), exprResult);
					}
				} catch ( ExpressionException e ) {
					log.warn(
							"Error evaluating MetricEvaluator [{}] parameter expression `{}`: {}\n\nExpression root: {}",
							getUid(), config.getExpression(), e.getMessage(), root);
					return getMessageSource().getMessage("evaluation.expressionEvaluationError",
							new Object[] { (exprIdx + 1), e.getMessage() },
							"Error evaluating expression " + (exprIdx + 1) + ": " + e.getMessage(),
							Locale.getDefault());
				}
			}
			if ( exprResult != null ) {
				parameters.put(config.getPropertyKey(), exprResult);
			}
		}

		return null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<String, ?> parameters() {
		final String metadataPath = getParametersMetadataPath();
		if ( metadataPath == null || metadataPath.isEmpty() ) {
			return emptyMap();
		}
		final MetadataService service = service(getMetadataService());
		if ( service == null ) {
			log.warn("MetadataService service [{}] not available, unable to resolve parameters.",
					getMetadataServiceUid());
			return emptyMap();
		}
		Object o = service.metadataAtPath(metadataPath);
		if ( o == null ) {
			log.warn("No parameters found at metadata path [{}], unable to resolve parameters.",
					metadataPath);
			return emptyMap();
		}
		if ( !(o instanceof Map<?, ?>) ) {
			log.warn("Unsupported object found at metadata path [{}], unable to resolve parameters.",
					metadataPath);
			return emptyMap();
		}
		return (Map) o;
	}

	private synchronized EvaluationResult evaluate(final Instant ts,
			final ExpressionConfig[] expressionConfs, final Map<String, ?> parameters) {
		Iterable<ExpressionService> services = services(getExpressionServices());
		if ( services == null || expressionConfs == null || expressionConfs.length < 1 ) {
			return new EvaluationResult(ts,
					getMessageSource().getMessage(
							services == null ? "evaluation.noExpressionServices"
									: "evaluation.noExpressions",
							null, services == null ? "No expression services available."
									: "No expressions configured.",
							Locale.getDefault()),
					parameters);
		}

		ExpressionRoot root = new ExpressionRoot(null, null, parameters, service(getDatumService()),
				service(getOpModesService()), service(getMetadataService()),
				service(getLocationService()));
		root.setTariffScheduleProviders(tariffScheduleProviders);
		root.setLocalStateDao(getLocalStateDao());

		int exprIdx = -1;
		for ( ExpressionConfig config : expressionConfs ) {
			exprIdx++;
			if ( config.getExpression() == null || config.getExpression().isEmpty() ) {
				continue;
			}
			final ExpressionServiceExpression expr;
			try {
				expr = config.getExpression(services);
			} catch ( ExpressionException e ) {
				log.warn("Error parsing MetricEvaluator [{}] expression `{}`: {}", getUid(),
						config.getExpression(), e.getMessage());
				return new EvaluationResult(ts,
						getMessageSource().getMessage("evaluation.expressionSyntaxError",
								new Object[] { (exprIdx + 1), e.getMessage() },
								"Error parsing expression " + (exprIdx + 1) + ": " + e.getMessage(),
								Locale.getDefault()),
						parameters);
			}

			Object exprResult = null;
			if ( expr != null ) {
				try {
					exprResult = expr.getService().evaluateExpression(expr.getExpression(), null, root,
							null, Object.class);
					if ( log.isTraceEnabled() ) {
						log.trace(
								"MetricEvaluator [{}] evaluated expression `{}` \u2192 {}\n\nExpression root: {}",
								getUid(), config.getExpression(), exprResult, root);
					} else if ( log.isDebugEnabled() ) {
						log.debug("MetricEvaluator [{}] evaluated expression `{}` \u2192 {}", getUid(),
								config.getExpression(), exprResult);
					}
				} catch ( ExpressionException e ) {
					log.warn(
							"Error evaluating MetricEvaluator [{}] expression `{}`: {}\n\nExpression root: {}",
							getUid(), config.getExpression(), e.getMessage(), root);
					return new EvaluationResult(ts, getMessageSource().getMessage(
							"evaluation.expressionEvaluationError",
							new Object[] { (exprIdx + 1), e.getMessage() },
							"Error evaluating expression " + (exprIdx + 1) + ": " + e.getMessage(),
							Locale.getDefault()), parameters);
				}
			}
			if ( exprResult instanceof Number ) {
				Number result = (Number) exprResult;
				return new EvaluationResult(ts, null, parameters, result, exprIdx);
			}
		}

		return new EvaluationResult(ts, getMessageSource().getMessage("evaluation.noResult", null,
				"No result produced.", Locale.getDefault()), parameters);
	}

	private void updateLatestMetrics() {
		final Instant now = clock.instant();
		try {

			final BasicMetricFilter filter = new BasicMetricFilter();
			filter.setType(Metric.METRIC_TYPE_SAMPLE);
			filter.setNames(metricNames);
			filter.setStartDate(now.minus(MOST_RECENT_METRIC_MAX_AGE));
			filter.setMostRecent(true);
			final FilterResults<Metric, MetricKey> latestMetricResults = metricDao.findFiltered(filter);
			for ( Metric m : latestMetricResults ) {
				latestMetrics.put(m.getName(), m);
			}

			filter.setStartDate(now.minus(metricAggregateTimeOffsetStart));
			filter.setEndDate(now.minus(metricAggregateTimeOffsetEnd));
			filter.setMostRecent(false);
			filter.setAggregates(
					metricAggregates.values().toArray(new MetricAggregate[metricAggregates.size()]));
			final FilterResults<Metric, MetricKey> latestMetricAggResults = metricDao
					.findFiltered(filter);
			for ( Metric m : latestMetricAggResults ) {
				latestMetricAggregations
						.computeIfAbsent(m.getName(), k -> new ConcurrentHashMap<>(6, 0.9f, 2))
						.put(m.getType(), m);
			}
		} finally {
			stats.add(MetricEvaluatorStat.MetricQueryTime,
					ChronoUnit.MILLIS.between(now, clock.instant()));
		}
	}

	@Override
	public String getSettingUid() {
		return "s10k.f8n.node.control.metricevaluator";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = new ArrayList<>(16);
		result.add(new BasicTitleSettingSpecifier("status", statusMessage(Locale.getDefault()), true,
				true));
		result.add(new BasicTitleSettingSpecifier("evaluationStatus",
				evaluationStatusMessage(Locale.getDefault()), true, true));
		result.addAll(baseIdentifiableSettings(""));
		result.add(new BasicTextFieldSettingSpecifier("requiredOperationalMode", null));

		result.add(new BasicTextAreaSettingSpecifier("metricsValue", null, true));

		result.add(new BasicTextFieldSettingSpecifier("metricAggregatesValue", DEFAULT_METRIC_AGGREGATES
				.stream().map(NamedMetricAggregate::keyNameValue).collect(joining(", "))));
		result.add(new BasicTextFieldSettingSpecifier("metricAggregateTimeOffsetStartSecs",
				String.valueOf(DEFAULT_METRIC_AGGREGATE_TIME_OFFSET_START.getSeconds())));
		result.add(new BasicTextFieldSettingSpecifier("metricAggregateTimeOffsetEndSecs",
				String.valueOf(DEFAULT_METRIC_AGGREGATE_TIME_OFFSET_END.getSeconds())));

		result.add(new BasicTextFieldSettingSpecifier("parametersMetadataPath", null));
		result.add(new BasicTextFieldSettingSpecifier("metadataServiceUid", null, false,
				"(objectClass=net.solarnetwork.node.service.MetadataService)"));

		result.add(new BasicTextFieldSettingSpecifier("outputInstructionTopic", null));
		result.add(new BasicTextFieldSettingSpecifier("outputInstructionParam", null));

		result.add(new BasicTextFieldSettingSpecifier("evaluationSourceId", null));

		result.add(new BasicTextFieldSettingSpecifier("statLogFrequency",
				String.valueOf(DEFAULT_STAT_LOG_FREQUENCY)));

		final Iterable<ExpressionService> exprServices = services(getExpressionServices());
		if ( exprServices != null ) {
			ExpressionConfig[] preExprConfs = getParamExpressionConfigs();
			List<ExpressionConfig> preExprConfsList = (preExprConfs != null ? asList(preExprConfs)
					: emptyList());
			result.add(SettingUtils.dynamicListSettingSpecifier("paramExpressionConfigs",
					preExprConfsList, new SettingUtils.KeyedListCallback<ExpressionConfig>() {

						@Override
						public Collection<SettingSpecifier> mapListSettingKey(ExpressionConfig value,
								int index, String key) {
							List<SettingSpecifier> exprSettings = ExpressionConfig
									.settings(MetricEvaluator.class, key + ".", exprServices);
							// remove type as not used here
							exprSettings = exprSettings.stream().filter((s) -> {
								return !((s instanceof KeyedSettingSpecifier<?>)
										&& (((KeyedSettingSpecifier<?>) s).getKey()
												.endsWith(".datumPropertyTypeKey")));
							}).collect(toList());
							SettingSpecifier configGroup = new BasicGroupSettingSpecifier(exprSettings);
							return singletonList(configGroup);
						}
					}));

			ExpressionConfig[] exprConfs = getExpressionConfigs();
			List<ExpressionConfig> exprConfsList = (exprConfs != null ? asList(exprConfs) : emptyList());
			result.add(SettingUtils.dynamicListSettingSpecifier("expressionConfigs", exprConfsList,
					new SettingUtils.KeyedListCallback<ExpressionConfig>() {

						@Override
						public Collection<SettingSpecifier> mapListSettingKey(ExpressionConfig value,
								int index, String key) {
							List<SettingSpecifier> exprSettings = ExpressionConfig
									.settings(MetricEvaluator.class, key + ".", exprServices);
							// remove type and name as not used here
							exprSettings = exprSettings.stream().filter((s) -> {
								return !((s instanceof KeyedSettingSpecifier<?>)
										&& (((KeyedSettingSpecifier<?>) s).getKey()
												.endsWith(".datumPropertyTypeKey")
												|| (((KeyedSettingSpecifier<?>) s).getKey()
														.endsWith(".name"))));
							}).collect(toList());
							SettingSpecifier configGroup = new BasicGroupSettingSpecifier(exprSettings);
							return singletonList(configGroup);
						}
					}));
		}
		return result;
	}

	private String statusMessage(final Locale locale) {
		final StringBuilder buf = new StringBuilder();
		final MessageSource msg = getMessageSource();
		final String none = msg.getMessage("evaluationStatus.none", null, locale);
		final Accumulation evalProcessingTime = stats
				.getAccumulation(MetricEvaluatorStat.ProcessingTime);
		// @formatter:off
		buf.append(msg.getMessage("status.msg",
				new Object[] {
						stats.get(MetricEvaluatorStat.EvaluationCount),
						evalProcessingTime != null
								? (long)Math.round(evalProcessingTime.avg())
								: none,
						},
				locale));
		// @formatter:on

		final Accumulation queryTime = stats.getAccumulation(MetricEvaluatorStat.MetricQueryTime);
		final Accumulation exprTime = stats
				.getAccumulation(MetricEvaluatorStat.ExpressionExeucutionTime);
		final Accumulation instrTime = stats
				.getAccumulation(MetricEvaluatorStat.InstructionExecutionTime);
		// @formatter:off
		buf.append(msg.getMessage("statusTable.head", new Object[] {
				queryTime != null
						? (long)Math.round(queryTime.avg())
						: none,
				exprTime != null
						? (long)Math.round(exprTime.avg())
						: none,
				instrTime != null
						? (long)Math.round(instrTime.avg())
						: none,
		}, locale));
		// @formatter:on

		for ( Entry<String, Long> entry : stats.allCounts().entrySet() ) {
			if ( entry.getKey().startsWith(MetricEvaluatorStat.ExpressionCount.name()) ) {
				int spaceIdx = entry.getKey().indexOf(' ');
				if ( spaceIdx > 0 ) {
					buf.append(msg.getMessage("evaluationTable.row",
							new Object[] { msg.getMessage("status.expressionNumber",
									new Object[] { entry.getKey().substring(spaceIdx + 1) }, locale),
									entry.getValue() },
							locale));
				}
			}
		}

		buf.append(msg.getMessage("evaluationTable.foot", null, locale));

		return buf.toString();
	}

	private String evaluationStatusMessage(final Locale locale) {
		final MessageSource msg = getMessageSource();
		final EvaluationResult result = this.lastEvaluationResult;
		if ( result == null ) {
			return msg.getMessage("evaluationStatus.none", null, locale);
		}

		StringBuilder buf = new StringBuilder();

		if ( result.message != null ) {
			buf.append(msg.getMessage("evaluationStatus.error",
					new Object[] { DateUtils.DISPLAY_DATE_LONG_TIME_SHORT
							.format(result.ts.atZone(ZoneId.systemDefault())), result.message },
					locale));
		} else {
			buf.append(msg.getMessage("evaluationStatus.ok",
					new Object[] {
							DateUtils.DISPLAY_DATE_LONG_TIME_SHORT
									.format(result.ts.atZone(ZoneId.systemDefault())),
							result.output, result.outputExpressionIndex + 1 },
					locale));
		}

		buf.append(msg.getMessage("evaluationTable.head", null, locale));
		for ( Entry<String, ?> e : result.inputs.entrySet() ) {
			buf.append(msg.getMessage("evaluationTable.row",
					new Object[] { e.getKey(),
							e.getValue() instanceof Number
									? format(locale, "%.3f", bigDecimalForNumber((Number) e.getValue()))
									: e.getValue() },
					locale));
		}
		buf.append(msg.getMessage("evaluationTable.foot", null, locale));

		if ( result.outputInstructionStatus != null ) {
			Map<String, String> params = result.outputInstruction.getParameterMap();
			Entry<String, String> param = (params != null && !params.isEmpty()
					? params.entrySet().iterator().next()
					: null);
			buf.append(msg.getMessage("evaluationInstr.result",
					new Object[] { result.outputInstruction.getTopic(),
							param != null ? param.getKey() : null,
							param != null ? param.getValue() : null },
					locale));
		}

		return buf.toString();
	}

	private static class EvaluationResult {

		private final Instant ts;
		private final String message;
		private final Map<String, ?> inputs;
		private final Number output;
		private final int outputExpressionIndex;
		private final Instruction outputInstruction;
		private final InstructionStatus outputInstructionStatus;

		private EvaluationResult(Instant ts, String message, Map<String, ?> inputs) {
			this(ts, message, inputs, null, -1, null, null);
		}

		private EvaluationResult(Instant ts, String message, Map<String, ?> inputs, Number output,
				int outputExpressionIndex) {
			this(ts, message, inputs, output, outputExpressionIndex, null, null);
		}

		private EvaluationResult(Instant ts, String message, Map<String, ?> inputs, Number output,
				int outputExpressionIndex, Instruction outputInstruction,
				InstructionStatus outputInstructionStatus) {
			super();
			this.ts = ts;
			this.message = message;

			Map<String, Object> in = new TreeMap<>(CASE_INSENSITIVE_NATURAL_SORT);
			in.putAll(inputs);
			this.inputs = in;
			this.output = output;
			this.outputExpressionIndex = outputExpressionIndex;
			this.outputInstruction = outputInstruction;
			this.outputInstructionStatus = outputInstructionStatus;
		}

		private EvaluationResult withInstructionStatus(Instruction instruction,
				InstructionStatus status) {
			return new EvaluationResult(ts, message, inputs, output, outputExpressionIndex, instruction,
					status);
		}

		private DatumSamples toDatumSamples() {
			DatumSamples s = new DatumSamples();
			for ( Entry<String, ?> e : inputs.entrySet() ) {
				Object val = e.getValue();
				if ( val instanceof Number ) {
					s.putInstantaneousSampleValue(e.getKey(), (Number) val);
				} else {
					s.putStatusSampleValue(e.getKey(), val);
				}
			}
			if ( outputExpressionIndex >= 0 ) {
				s.putInstantaneousSampleValue("expressionNum", outputExpressionIndex + 1);
			}
			if ( output != null ) {
				s.putInstantaneousSampleValue("output", output);
			}
			if ( outputInstructionStatus != null
					&& outputInstructionStatus.getInstructionState() != InstructionState.Completed ) {
				s.putStatusSampleValue("outputInstruction", outputInstruction.getTopic());
				s.putStatusSampleValue("instructionState",
						outputInstructionStatus.getInstructionState());
			}
			return s;
		}

	}

	private void publishDatum(EvaluationResult result) {
		if ( result == null ) {
			return;
		}
		final DatumQueue q = service(datumQueue);
		if ( q == null ) {
			return;
		}
		final String sourceId = resolvePlaceholders(evaluationSourceId);
		if ( sourceId == null || sourceId.isEmpty() ) {
			return;
		}
		NodeDatum d = SimpleDatum.nodeDatum(sourceId, result.ts, result.toDatumSamples());
		q.offer(d);
	}

	/**
	 * Get the datum source ID provider.
	 *
	 * <p>
	 * This accessor is provided to work with {@code SimpleManagedJob}'s
	 * {@code serviceProviderConfigurations} runtime configuration.
	 * </p>
	 *
	 * @return this instance
	 */
	public DatumSourceIdProvider getDatumSourceIdProvider() {
		return this;
	}

	/**
	 * Get the metric names to use.
	 *
	 * @return the metric names
	 */
	public final Set<String> getMetrics() {
		return metrics;
	}

	/**
	 * Set the metric name to use for the grid price.
	 *
	 * @param metrics
	 *        the metric names to set
	 */
	public final void setMetrics(Set<String> metrics) {
		this.metrics = metrics;
	}

	/**
	 * Get the metric names as a comma-delimited key list.
	 *
	 * @return the metric names as a key list
	 */
	public final String getMetricsValue() {
		return delimitedStringFromCollection(metrics, ", ");
	}

	/**
	 * Set the metric names as a comma-delimited key list.
	 *
	 * @param value
	 *        the metric names to set as a key list
	 */
	public final void setMetricsValue(String value) {
		setMetrics(commaDelimitedStringToSet(value));
	}

	/**
	 * Get the time offset start (from now) to aggregate latestMetrics over.
	 *
	 * @return the time range; defaults to
	 *         {@link #DEFAULT_METRIC_AGGREGATE_TIME_OFFSET_START}
	 */
	public final Duration getMetricAggregateTimeOffsetStart() {
		return metricAggregateTimeOffsetStart;
	}

	/**
	 * Set the time offset start (from now) to aggregate latestMetrics over.
	 *
	 * @param metricAggregateTimeOffsetStart
	 *        the time range to set; if {@literal null} then
	 *        {@link #DEFAULT_METRIC_AGGREGATE_TIME_OFFSET_START} will be used
	 */
	public final void setMetricAggregateTimeOffsetStart(Duration metricAggregateTimeOffsetStart) {
		this.metricAggregateTimeOffsetStart = (metricAggregateTimeOffsetStart != null
				? metricAggregateTimeOffsetStart
				: DEFAULT_METRIC_AGGREGATE_TIME_OFFSET_START);
	}

	/**
	 * Get the time offset start (from now) to aggregate latestMetrics over, in
	 * seconds.
	 *
	 * @return the time range in seconds; defaults to
	 *         {@link #DEFAULT_METRIC_AGGREGATE_TIME_OFFSET_START}
	 */
	public final long getMetricAggregateTimeOffsetStartSecs() {
		return metricAggregateTimeOffsetStart.getSeconds();
	}

	/**
	 * Set the time offset start (from now) to aggregate latestMetrics over, in
	 * seconds.
	 *
	 * @param seconds
	 *        the time range to set; if {@literal null} then
	 *        {@link #DEFAULT_METRIC_AGGREGATE_TIME_OFFSET_START} will be used
	 */
	public final void setMetricAggregateTimeOffsetStartSecs(long seconds) {
		setMetricAggregateTimeOffsetStart(Duration.ofSeconds(seconds));
	}

	/**
	 * Get the time offset end (from now) to aggregate latestMetrics over.
	 *
	 * @return the time range; defaults to
	 *         {@link #DEFAULT_METRIC_AGGREGATE_TIME_OFFSET_END}
	 */
	public final Duration getMetricAggregateTimeOffsetEnd() {
		return metricAggregateTimeOffsetEnd;
	}

	/**
	 * Set the time offset end (from now) to aggregate latestMetrics over.
	 *
	 * @param metricAggregateTimeOffsetEnd
	 *        the time range to set; if {@literal null} then
	 *        {@link #DEFAULT_METRIC_AGGREGATE_TIME_OFFSET_END} will be used
	 */
	public final void setMetricAggregateTimeOffsetEnd(Duration metricAggregateTimeOffsetEnd) {
		this.metricAggregateTimeOffsetEnd = (metricAggregateTimeOffsetEnd != null
				? metricAggregateTimeOffsetEnd
				: DEFAULT_METRIC_AGGREGATE_TIME_OFFSET_END);
	}

	/**
	 * Get the time offset end (from now) to aggregate latestMetrics over, in
	 * seconds.
	 *
	 * @return the time range; defaults to
	 *         {@link #DEFAULT_METRIC_AGGREGATE_TIME_OFFSET_END}
	 */
	public final long getMetricAggregateTimeOffsetEndSecs() {
		return metricAggregateTimeOffsetEnd.getSeconds();
	}

	/**
	 * Set the time offset end (from now) to aggregate latestMetrics over, in
	 * seconds.
	 *
	 * @param seconds
	 *        the time range to set; if {@literal null} then
	 *        {@link #DEFAULT_METRIC_AGGREGATE_TIME_OFFSET_END} will be used
	 */
	public final void setMetricAggregateTimeOffsetEndSecs(long seconds) {
		setMetricAggregateTimeOffsetEnd(Duration.ofSeconds(seconds));
	}

	/**
	 * Get the metric aggregates to query.
	 *
	 * @return the metric aggregates; default to
	 *         {@link #DEFAULT_METRIC_AGGREGATES}.
	 */
	public final Collection<NamedMetricAggregate> getMetricAggregates() {
		return metricAggregates.values();
	}

	/**
	 * Set the metric aggregates to query.
	 *
	 * @param metricAggregates
	 *        the metric aggregates to set; if {@literal null} or empty then
	 *        {@link #DEFAULT_METRIC_AGGREGATES} will be used
	 */
	public final void setMetricAggregates(Collection<NamedMetricAggregate> metricAggregates) {
		this.metricAggregates = aggregateMap(
				metricAggregates == null || metricAggregates.isEmpty() ? DEFAULT_METRIC_AGGREGATES
						: metricAggregates);
	}

	/**
	 * Get the metric aggregates as a comma-delimited key list.
	 *
	 * @return the metric aggregates as a key list; defaults to
	 *         {@link #DEFAULT_METRIC_AGGREGATES}.
	 */
	public final String getMetricAggregatesValue() {
		return metricAggregates.values().stream().map(NamedMetricAggregate::keyNameValue)
				.collect(joining(", "));
	}

	/**
	 * Set the metric aggregates as a comma-delimited key list.
	 *
	 * @param value
	 *        the metric aggregates to set as a key list; if {@literal null} or
	 *        empty then {@link #DEFAULT_METRIC_AGGREGATES} will be used
	 */
	public final void setMetricAggregatesValue(String value) {
		Set<String> keys = StringUtils.commaDelimitedStringToSet(value);
		List<NamedMetricAggregate> aggs = new ArrayList<>(8);
		if ( keys != null ) {
			for ( String key : keys ) {
				String name = key;

				// support mapping of keys to names using k=n syntax
				if ( key.indexOf('=') > 0 ) {
					Map<String, String> m = StringUtils.commaDelimitedStringToMap(key);
					if ( !m.isEmpty() ) {
						for ( Entry<String, String> e : m.entrySet() ) {
							key = e.getKey();
							name = e.getValue();
							break;
						}
					}
				}

				if ( MetricAggregate.METRIC_TYPE_MINIMUM.equalsIgnoreCase(key) ) {
					aggs.add(namedMetricAggregate(BasicMetricAggregate.Minimum, name));
				} else if ( MetricAggregate.METRIC_TYPE_MAXIMUM.equalsIgnoreCase(key) ) {
					aggs.add(namedMetricAggregate(BasicMetricAggregate.Maximum, name));
				} else if ( MetricAggregate.METRIC_TYPE_AVERAGE.equalsIgnoreCase(key) ) {
					aggs.add(namedMetricAggregate(BasicMetricAggregate.Average, name));
				} else if ( (key.startsWith("q:") || key.startsWith("Q:")) && key.length() > 2 ) {
					try {
						int p = Integer.parseInt(key.substring(2));
						aggs.add(
								namedMetricAggregate(
										new ParameterizedMetricAggregate("q", new Object[] { p / 100.0 },
												ParameterizedMetricAggregate.INTEGER_PERCENT_KEY),
										name));
					} catch ( NumberFormatException e ) {
						// ignore and continue
					}
				}
			}
		}
		setMetricAggregates(aggs);
	}

	/**
	 * Test if the configured required operational mode is active.
	 *
	 * <p>
	 * If {@link #getRequiredOperationalMode()} is configured but
	 * {@code #getOpModesService()} is not, this method will always return
	 * {@literal false}.
	 * </p>
	 *
	 * @return {@literal true} if an operational mode is required and that mode
	 *         is currently active
	 * @since 1.1
	 */
	private boolean operationalModeMatches() {
		final String mode = getRequiredOperationalMode();
		if ( mode == null ) {
			// no mode required, so automatically matches
			return true;
		}
		final OperationalModesService service = service(getOpModesService());
		if ( service == null ) {
			// service not available, so automatically does not match
			return false;
		}
		boolean result = service.isOperationalModeActive(mode);
		if ( !result && log.isTraceEnabled() ) {
			log.trace("MetricEvaluator [{}] required operational mode [{}] not active; not executing.",
					getUid(), mode);
		}
		return result;
	}

	/**
	 * Get the {@link MetadataService} service filter UID.
	 *
	 * @return the service UID
	 */
	public final String getMetadataServiceUid() {
		final OptionalService<MetadataService> service = getMetadataService();
		if ( service instanceof FilterableService ) {
			return ((FilterableService) service).getPropertyValue(UID_PROPERTY);
		}
		return null;
	}

	/**
	 * Set the {@link MetadataService} service filter UID.
	 *
	 * @param uid
	 *        the service UID
	 */
	public final void setMetadataServiceUid(String uid) {
		final OptionalService<MetadataService> service = getMetadataService();
		if ( service instanceof FilterableService ) {
			((FilterableService) service).setPropertyFilter(UID_PROPERTY, uid);
		}
	}

	/**
	 * Get the operational modes service to use.
	 *
	 * @return the service, or {@literal null}
	 */
	public OptionalService<OperationalModesService> getOpModesService() {
		return opModesService;
	}

	/**
	 * Set the operational modes service to use.
	 *
	 * @param opModesService
	 *        the service to use
	 * @since 1.1
	 */
	public void setOpModesService(OptionalService<OperationalModesService> opModesService) {
		this.opModesService = opModesService;
	}

	/**
	 * Get the datum queue.
	 *
	 * @return the datum queue
	 */
	public OptionalService<DatumQueue> getDatumQueue() {
		return datumQueue;
	}

	/**
	 * Set the datum queue.
	 *
	 * @param datumQueue
	 *        the datum queue to use
	 */
	public void setDatumQueue(OptionalService<DatumQueue> datumQueue) {
		this.datumQueue = datumQueue;
	}

	/**
	 * Get the datum service.
	 *
	 * @return the datum service
	 */
	public OptionalService<DatumService> getDatumService() {
		return datumService;
	}

	/**
	 * Set the datum service.
	 *
	 * @param datumService
	 *        the datum service
	 */
	public void setDatumService(OptionalService<DatumService> datumService) {
		this.datumService = datumService;
	}

	/**
	 * Get a metadata path for input parameters.
	 *
	 * @return the metadata path
	 */
	public final String getParametersMetadataPath() {
		return parametersMetadataPath;
	}

	/**
	 * Set a metadata path for input parameters.
	 *
	 * @param parametersMetadataPath
	 *        the metadata path to set
	 */
	public final void setParametersMetadataPath(String parametersMetadataPath) {
		this.parametersMetadataPath = parametersMetadataPath;
	}

	/**
	 * Get the pre expression configurations.
	 *
	 * @return the expression configurations
	 */
	public ExpressionConfig[] getParamExpressionConfigs() {
		return paramExpressionConfigs;
	}

	/**
	 * Set the pre expression configurations to use.
	 *
	 * @param expressionConfigs
	 *        the configs to use
	 */
	public void setParamExpressionConfigs(ExpressionConfig[] expressionConfigs) {
		this.paramExpressionConfigs = expressionConfigs;
	}

	/**
	 * Get the number of configured {@code paramExpressionConfigs} elements.
	 *
	 * @return the number of {@code paramExpressionConfigs} elements
	 */
	public int getParamExpressionConfigsCount() {
		ExpressionConfig[] confs = this.paramExpressionConfigs;
		return (confs == null ? 0 : confs.length);
	}

	/**
	 * Adjust the number of configured {@code paramExpressionConfigs} elements.
	 *
	 * <p>
	 * Any newly added element values will be set to new
	 * {@link ExpressionConfig} instances.
	 * </p>
	 *
	 * @param count
	 *        The desired number of {@code paramExpressionConfigs} elements.
	 */
	public void setParamExpressionConfigsCount(int count) {
		this.paramExpressionConfigs = ArrayUtils.arrayWithLength(this.paramExpressionConfigs, count,
				ExpressionConfig.class, null);
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
	 * Adjust the number of configured {@code expressionConfigs} elements.
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

	/**
	 * Get the output instruction topic.
	 *
	 * @return the instruction topic
	 */
	public final String getOutputInstructionTopic() {
		return outputInstructionTopic;
	}

	/**
	 * Set the output instruction topic.
	 *
	 * @param outputInstructionTopic
	 *        the instruction topic to set
	 */
	public final void setOutputInstructionTopic(String outputInstructionTopic) {
		this.outputInstructionTopic = outputInstructionTopic;
	}

	/**
	 * Get the output instruction parameter name.
	 *
	 * @return the parameter name
	 */
	public final String getOutputInstructionParam() {
		return outputInstructionParam;
	}

	/**
	 * Set the output instruction parameter name.
	 *
	 * @param outputInstructionParam
	 *        the parameter name to set
	 */
	public final void setOutputInstructionParam(String outputInstructionParam) {
		this.outputInstructionParam = outputInstructionParam;
	}

	/**
	 * Get the statistics.
	 *
	 * @return the statistics, never {@literal null}
	 */
	public final StatTracker getStats() {
		return stats;
	}

	/**
	 * Get the statistic log frequency.
	 *
	 * @return the log frequency
	 */
	public final int getStatLogFrequency() {
		return stats.getLogFrequency();
	}

	/**
	 * Set the statistic log frequency.
	 *
	 * @param logFrequency
	 *        the log frequency to set
	 */
	public final void setStatLogFrequency(int logFrequency) {
		stats.setLogFrequency(logFrequency);
	}

	/**
	 * Get a source ID to use for generated evaluation datum.
	 *
	 * @return the source ID, or {@literal null} to not generate any evaluation
	 *         datum
	 */
	public final String getEvaluationSourceId() {
		return evaluationSourceId;
	}

	/**
	 * Set a source ID to use for generated evaluation datum.
	 *
	 * @param evaluationSourceId
	 *        the source ID to set, or {@literal null} to not generate any
	 *        evaluation datum
	 */
	public final void setEvaluationSourceId(String evaluationSourceId) {
		this.evaluationSourceId = evaluationSourceId;
	}

	/**
	 * Get the tariff schedule providers.
	 *
	 * @return the providers
	 */
	public final OptionalServiceCollection<TariffScheduleProvider> getTariffScheduleProviders() {
		return tariffScheduleProviders;
	}

	/**
	 * Set the tariff schedule providers.
	 *
	 * @param tariffScheduleProviders
	 *        the providers to set
	 */
	public final void setTariffScheduleProviders(
			OptionalServiceCollection<TariffScheduleProvider> tariffScheduleProviders) {
		this.tariffScheduleProviders = tariffScheduleProviders;
	}

	/**
	 * Get an operational mode that is required to execute this service.
	 *
	 * @return the required operational mode, or {@literal null} for none
	 * @since 1.2
	 */
	public String getRequiredOperationalMode() {
		return requiredOperationalMode;
	}

	/**
	 * Set an operational mode that is required to execute this service.
	 *
	 * @param requiredOperationalMode
	 *        the required operational mode, or {@literal null} or an empty
	 *        string that will be treated as {@literal null}
	 * @since 1.2
	 */
	public void setRequiredOperationalMode(String requiredOperationalMode) {
		if ( requiredOperationalMode != null && requiredOperationalMode.trim().isEmpty() ) {
			requiredOperationalMode = null;
		}
		this.requiredOperationalMode = requiredOperationalMode;
	}

}
