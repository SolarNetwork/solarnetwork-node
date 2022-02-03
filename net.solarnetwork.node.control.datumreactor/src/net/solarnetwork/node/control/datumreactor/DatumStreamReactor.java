/* ==================================================================
 * DatumStreamReactor.java - 3/02/2022 9:19:38 AM
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

package net.solarnetwork.node.control.datumreactor;

import static java.util.Collections.singletonMap;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Completed;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Declined;
import static net.solarnetwork.node.reactor.InstructionHandler.PARAM_MESSAGE;
import static net.solarnetwork.node.reactor.InstructionHandler.TOPIC_SET_CONTROL_PARAMETER;
import static net.solarnetwork.node.reactor.InstructionUtils.createLocalInstruction;
import static net.solarnetwork.node.reactor.InstructionUtils.createStatus;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.service.OptionalServiceCollection.services;
import static net.solarnetwork.util.NumberUtils.bigDecimalForNumber;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.springframework.expression.ExpressionException;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionExecutionService;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.service.DatumDataSource;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.DatumService;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.support.ExpressionServiceExpression;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;
import net.solarnetwork.util.DateUtils;

/**
 * Service to monitor a datum stream and set a control value in response via an
 * expression.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumStreamReactor extends BaseIdentifiable
		implements SettingSpecifierProvider, EventHandler {

	private final ControlPropertyConfig config = new ControlPropertyConfig();
	private Executor executor;
	private Pattern sourceIdRegex;
	private OptionalService<InstructionExecutionService> instructionExecutionService;
	private OptionalService<DatumService> datumService;

	private Instruction lastInstruction;
	private InstructionStatus lastInstructionResult;

	@Override
	public void handleEvent(Event event) {
		// validate configuration and extract datum property for input load
		if ( !config.isValid() ) {
			return;
		}
		final String topic = event.getTopic();
		if ( !DatumDataSource.EVENT_TOPIC_DATUM_CAPTURED.equals(topic) ) {
			return;
		}
		final Object val = event.getProperty(DatumEvents.DATUM_PROPERTY);
		if ( val == null || !(val instanceof NodeDatum) ) {
			return;
		}
		final NodeDatum datum = (NodeDatum) val;
		final Pattern sourceIdRegex = getSourceIdRegex();
		if ( sourceIdRegex != null && (datum.getSourceId() == null
				|| !sourceIdRegex.matcher(datum.getSourceId()).find()) ) {
			log.debug("Ignoring datum: ID {} does not match pattern {}", datum.getSourceId(),
					sourceIdRegex);
			return;
		}

		// evaluate expression to determine load balance output based in input load value,
		// and then set configured control to that result
		Runnable task = new Runnable() {

			@Override
			public void run() {
				Number desiredControlValue = null;
				Instruction instr = null;
				InstructionStatus instrResult = null;
				try {
					if ( config.getExpression() != null && config.getExpressionServiceId() != null ) {
						desiredControlValue = evaluateExpression(datum);
					}
					desiredControlValue = applyConstraints(desiredControlValue);
					if ( log.isDebugEnabled() ) {
						log.debug("Load balance for input {} result for control [{}]: {}",
								datum.asSimpleMap(), config.getControlId(), desiredControlValue);
					}
					if ( desiredControlValue == null ) {
						return;
					}
					InstructionExecutionService instrService = service(instructionExecutionService);
					instr = createLocalInstruction(TOPIC_SET_CONTROL_PARAMETER, config.getControlId(),
							bigDecimalForNumber(desiredControlValue).toPlainString());
					if ( instrService != null ) {
						instrResult = instrService.executeInstruction(instr);
					} else {
						instrResult = createStatus(instr, Declined, singletonMap(PARAM_MESSAGE,
								"No InstructionExecutionService available."));
					}
				} catch ( ExpressionException e ) {
					instr = createLocalInstruction(TOPIC_SET_CONTROL_PARAMETER, config.getControlId(),
							"-1");
					instrResult = createStatus(instr, Declined,
							singletonMap(PARAM_MESSAGE,
									String.format("Exception evaluating expression [%s]: %s",
											config.getExpression(), e.getMessage())));
				} catch ( RuntimeException e ) {
					instrResult = createStatus(instr, Declined, singletonMap(PARAM_MESSAGE,
							"Exception handling instruction: " + e.toString()));
				}
				if ( instrResult == null ) {
					log.warn("Unable to set control [{}] value to [{}]: control not available",
							config.getControlId(), desiredControlValue);
				} else if ( instrResult.getInstructionState() != Completed ) {
					log.warn("Failed to set control [{}] value to [{}] (instruction result {}): {}",
							config.getControlId(), desiredControlValue, instrResult,
							instrResult.getResultParameters());
				}
				synchronized ( this ) {
					lastInstruction = instr;
					lastInstructionResult = instrResult;
				}
			}

		};
		Executor e = this.executor;
		if ( e != null ) {
			e.execute(task);
		} else {
			task.run();
		}
	}

	private Number evaluateExpression(final NodeDatum datum) {
		Number result = null;
		final Iterable<ExpressionService> services = services(getExpressionServices());
		ExpressionRoot root = ExpressionRoot.of(datum, service(datumService), config.getMinValue(),
				config.getMaxValue());
		final ExpressionServiceExpression expr;
		try {
			expr = config.getExpression(services);
		} catch ( ExpressionException e ) {
			log.warn("Error parsing expression `{}`: {}", config.getExpression(), e.getMessage());
			return null;
		}

		if ( expr != null ) {
			try {
				result = expr.getService().evaluateExpression(expr.getExpression(), null, root, null,
						Number.class);
				if ( log.isTraceEnabled() ) {
					log.trace(
							"Service [{}] evaluated control [{}] expression `{}` \u2192 {}\n\nExpression root: {}",
							getUid(), config.getControlId(), config.getExpression(), result, root);
				} else if ( log.isDebugEnabled() ) {
					log.debug("Service [{}] evaluated control [{}] expression `{}` \u2192 {}", getUid(),
							config.getControlId(), config.getExpression(), result);
				}
			} catch ( ExpressionException e ) {
				log.warn(
						"Error evaluating service [{}] control [{}] expression `{}`: {}\n\nExpression root: {}",
						getUid(), config.getControlId(), config.getExpression(), e.getMessage(), root);
				throw e;
			}
		}
		return result;
	}

	private Number applyConstraints(Number result) {
		if ( result != null && (config.getMinValue() != null || config.getMaxValue() != null) ) {
			// apply min/max
			BigDecimal resultDecimal = bigDecimalForNumber(result);
			if ( config.getMinValue() != null && resultDecimal.compareTo(config.getMinValue()) < 0 ) {
				result = config.getMinValue();
			} else if ( config.getMaxValue() != null
					&& resultDecimal.compareTo(config.getMaxValue()) > 0 ) {
				result = config.getMaxValue();
			}
		}
		return result;
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.control.datumreactor";
	}

	@Override
	public String getDisplayName() {
		return "Load Balancer";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = new ArrayList<>(4);
		results.add(new BasicTitleSettingSpecifier("status", getStatusMessage()));
		results.add(new BasicTitleSettingSpecifier("statusDate", getStatusMessageDate()));
		results.addAll(baseIdentifiableSettings(""));
		results.add(new BasicTextFieldSettingSpecifier("sourceIdRegexValue", ""));
		results.addAll(ControlPropertyConfig.settings("config.", services(getExpressionServices())));
		return results;
	}

	private String getStatusMessage() {
		final Instruction instr;
		final InstructionStatus instrResult;
		synchronized ( this ) {
			instr = this.lastInstruction;
			instrResult = this.lastInstructionResult;
		}
		if ( instr == null ) {
			return "N/A";
		}
		String controlId = instr.getParameterNames().iterator().next();
		String controlVal = instr.getParameterValue(controlId);
		if ( instrResult == null ) {
			return getMessageSource().getMessage("error.missingControl",
					new Object[] { controlId, controlVal }, "Control not available.",
					Locale.getDefault());
		}
		if ( instrResult.getInstructionState() == Completed ) {
			return getMessageSource().getMessage("status.ok", new Object[] { controlId, controlVal },
					"Control value set.", Locale.getDefault());
		}
		return getMessageSource().getMessage("error.failSetControl", new Object[] { controlId,
				controlVal, instrResult.getInstructionState(),
				(instrResult.getResultParameters() != null
						&& !instrResult.getResultParameters().isEmpty()
								? instrResult.getResultParameters()
								: getMessageSource().getMessage("error.noInstructionResultParameters",
										null, "No result information available.",
										Locale.getDefault())) },
				"Failed to set control value.", Locale.getDefault());
	}

	private String getStatusMessageDate() {
		final Instruction instr;
		synchronized ( this ) {
			instr = this.lastInstruction;
		}
		if ( instr == null ) {
			return "N/A";
		}
		return DateUtils.formatForLocalDisplay(instr.getInstructionDate());
	}

	/**
	 * Set an executor to use for internal tasks.
	 * 
	 * @param executor
	 *        the executor
	 */
	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

	/**
	 * Get the source ID regular expression.
	 * 
	 * @return the source ID expression, or {@literal null} for including all
	 *         source IDs
	 */
	public Pattern getSourceIdRegex() {
		return sourceIdRegex;
	}

	/**
	 * Set the source ID regular expression.
	 * 
	 * @param sourceIdRegex
	 *        a pattern to match against source IDs; if defined then this datum
	 *        will only be generated for controls with matching source ID
	 *        values; if {@literal null} then generate datum for all controls
	 */
	public void setSourceIdRegex(Pattern sourceIdRegex) {
		this.sourceIdRegex = sourceIdRegex;
	}

	/**
	 * Get the source ID regular expression as a string.
	 * 
	 * @return the source ID expression string, or {@literal null} for including
	 *         all source IDs
	 */
	public String getSourceIdRegexValue() {
		Pattern p = getSourceIdRegex();
		return (p != null ? p.pattern() : null);
	}

	/**
	 * Set the source ID regular expression as a string.
	 * 
	 * <p>
	 * Errors compiling {@code sourceIdRegex} into a {@link Pattern} will be
	 * silently ignored, causing the regular expression to be set to
	 * {@literal null}.
	 * </p>
	 * 
	 * @param sourceIdRegex
	 *        a pattern to match against source IDs; if defined then this datum
	 *        will only be generated for controls with matching source ID
	 *        values; if {@literal null} then generate datum for all controls
	 */
	public void setSourceIdRegexValue(String sourceIdRegex) {
		Pattern p = null;
		if ( sourceIdRegex != null ) {
			try {
				p = Pattern.compile(sourceIdRegex, Pattern.CASE_INSENSITIVE);
			} catch ( PatternSyntaxException e ) {
				// ignore
			}
		}
		setSourceIdRegex(p);
	}

	/**
	 * Get the control property configuration.
	 * 
	 * @return the configuration, never {@literal null}
	 */
	public ControlPropertyConfig getConfig() {
		return config;
	}

	/**
	 * Set the instruction service.
	 * 
	 * @param instructionExecutionService
	 *        the service to set
	 */
	public void setInstructionExecutionService(
			OptionalService<InstructionExecutionService> instructionExecutionService) {
		this.instructionExecutionService = instructionExecutionService;
	}

	/**
	 * Set the datum service.
	 * 
	 * @param datumService
	 *        the service to set
	 */
	public void setDatumService(OptionalService<DatumService> datumService) {
		this.datumService = datumService;
	}

}
