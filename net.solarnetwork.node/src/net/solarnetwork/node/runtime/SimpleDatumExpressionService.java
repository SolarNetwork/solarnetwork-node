/* ==================================================================
 * SimpleDatumExpressionService.java - 2/04/2024 11:06:56 am
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

package net.solarnetwork.node.runtime;

import static java.util.Collections.singletonMap;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Completed;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Declined;
import static net.solarnetwork.node.reactor.InstructionUtils.createErrorResultParameters;
import static net.solarnetwork.node.reactor.InstructionUtils.createStatus;
import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.service.OptionalServiceCollection.services;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Map;
import java.util.UUID;
import org.springframework.expression.ExpressionException;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.node.domain.ExpressionRoot;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.service.DatumService;
import net.solarnetwork.node.service.OperationalModesService;
import net.solarnetwork.node.service.TariffScheduleProvider;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.node.service.support.ExpressionConfig;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.service.OptionalServiceCollection;
import net.solarnetwork.service.support.ExpressionServiceExpression;

/**
 * {@link InstructionHandler} for executing datum-related expressions.
 *
 * <p>
 * If no {@link #PARAM_EXPRESSION_LANGUAGE} instruction parameter is provided,
 * the first-available {@link ExpressionService} will be used.
 * </p>
 *
 * @author matt
 * @version 1.4
 */
public class SimpleDatumExpressionService extends BaseIdentifiable implements InstructionHandler {

	/** The instruction topic for evaluating a datum expression. */
	public static final String TOPIC_DATUM_EXPRESSION = "DatumExpression";

	/** The instruction parameter for a datum expression to evaluate. */
	public static final String PARAM_EXPRESSION = "expression";

	/** The instruction parameter for the expression language to use. */
	public static final String PARAM_EXPRESSION_LANGUAGE = "lang";

	private final DatumService datumService;
	private final OperationalModesService opModesService;
	private OptionalServiceCollection<TariffScheduleProvider> tariffScheduleProviders;

	/**
	 * Constructor.
	 *
	 * @param datumService
	 *        the datum service
	 * @param opModesService
	 *        the operational modes service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SimpleDatumExpressionService(DatumService datumService,
			OperationalModesService opModesService) {
		super();
		this.datumService = requireNonNullArgument(datumService, "datumService");
		this.opModesService = requireNonNullArgument(opModesService, "opModesService");
	}

	@Override
	public boolean handlesTopic(String topic) {
		return TOPIC_DATUM_EXPRESSION.equals(topic);
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
		if ( instruction == null || !handlesTopic(instruction.getTopic()) ) {
			return null;
		}

		final Map<String, String> params = instruction.params();
		final String expression = params.get(PARAM_EXPRESSION);
		if ( expression == null || expression.isEmpty() ) {
			return createStatus(instruction, Declined,
					createErrorResultParameters("No expression parameter provided.", "SDE.00001"));
		}

		final String expressionServiceId = instruction.getParameterValue(PARAM_EXPRESSION_LANGUAGE);

		ExpressionService expressionService = null;
		Iterable<ExpressionService> services = services(getExpressionServices());
		if ( services != null ) {
			for ( ExpressionService s : services ) {
				// if expressionServiceId not provided, just take first available service
				if ( expressionServiceId == null || expressionServiceId.isEmpty()
						|| expressionServiceId.equals(s.getUid()) ) {
					expressionService = s;
					break;
				}
			}
		}
		if ( expressionService == null ) {
			return createStatus(instruction, Declined, createErrorResultParameters(
					"Requested expression language service not available.", "SDE.00002"));
		}

		GeneralDatum d = new GeneralDatum(UUID.randomUUID().toString());

		ExpressionRoot root = new ExpressionRoot(d, null, null, datumService, opModesService,
				service(getMetadataService()), service(getLocationService()));
		root.setTariffScheduleProviders(tariffScheduleProviders);
		root.setLocalStateDao(getLocalStateDao());

		ExpressionConfig config = new ExpressionConfig("result", DatumSamplesType.Status, expression,
				expressionService.getUid());

		final ExpressionServiceExpression expr;
		try {
			expr = config.getExpression(services);
		} catch ( ExpressionException e ) {
			return createStatus(instruction, Declined,
					createErrorResultParameters(String.format("Error parsing expression [%s]: %s",
							config.getExpression(), e.getMessage()), "SDE.00003"));
		}

		Object exprResult = null;
		if ( expr != null ) {
			try {
				exprResult = expr.getService().evaluateExpression(expr.getExpression(), null, root, null,
						Object.class);
				if ( log.isTraceEnabled() ) {
					log.trace(
							"Instruction [{}] evaluated datum property [{}] expression `{}` \u2192 {}\n\nExpression root: {}",
							instruction.getIdentifier(), config.getName(), config.getExpression(),
							exprResult, root);
				} else if ( log.isDebugEnabled() ) {
					log.debug("Instruction [{}] evaluated datum property [{}] expression `{}` \u2192 {}",
							instruction.getIdentifier(), config.getName(), config.getExpression(),
							exprResult);
				}
			} catch ( ExpressionException e ) {
				log.warn(
						"Error evaluating instruction [{}] datum property [{}] expression `{}`: {}\n\nExpression root: {}",
						instruction.getIdentifier(), config.getName(), config.getExpression(),
						e.getMessage(), root);
				return createStatus(instruction, Declined,
						createErrorResultParameters(String.format("Error evaluating expression [%s]: %s",
								config.getExpression(), e.getMessage()), "SDE.00005"));
			}
		}

		Map<String, Object> resultParams = (exprResult != null
				? singletonMap(PARAM_SERVICE_RESULT, exprResult)
				: null);
		return createStatus(instruction, Completed, resultParams);
	}

	/**
	 * Get the tariff schedule providers.
	 *
	 * @return the providers
	 * @since 1.3
	 */
	public final OptionalServiceCollection<TariffScheduleProvider> getTariffScheduleProviders() {
		return tariffScheduleProviders;
	}

	/**
	 * Set the tariff schedule providers.
	 *
	 * @param tariffScheduleProviders
	 *        the providers to set
	 * @since 1.3
	 */
	public final void setTariffScheduleProviders(
			OptionalServiceCollection<TariffScheduleProvider> tariffScheduleProviders) {
		this.tariffScheduleProviders = tariffScheduleProviders;
	}
}
