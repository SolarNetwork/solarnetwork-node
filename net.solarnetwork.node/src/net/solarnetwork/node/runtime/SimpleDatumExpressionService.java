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
import static net.solarnetwork.service.OptionalServiceCollection.services;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.expression.ExpressionException;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.node.domain.ExpressionRoot;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.service.DatumService;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.node.service.support.ExpressionConfig;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.service.support.ExpressionServiceExpression;

/**
 * {@link InstructionHandler} for executing datum-related expressions.
 *
 * @author matt
 * @version 1.0
 */
public class SimpleDatumExpressionService extends BaseIdentifiable implements InstructionHandler {

	/** The instruction topic for evaluating a datum expression. */
	public static final String TOPIC_DATUM_EXPRESSION = "DatumExpression";

	/** The instruction parameter for a datum expression to evaluate. */
	public static final String PARAM_EXPRESSION = "expression";

	///** The instruction parameter for a source ID to assign. */
	//public static final String PARAM_SOURCE_ID = "sourceId";

	/** The instruction parameter for the expression language to use. */
	public static final String PARAM_EXPRESSION_LANGUAGE = "lang";

	private final DatumService datumService;

	/**
	 * Constructor.
	 *
	 * @param datumService
	 *        the datum service
	 * @param objectMapper
	 *        the object mapper to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SimpleDatumExpressionService(DatumService datumService) {
		super();
		this.datumService = requireNonNullArgument(datumService, "datumService");
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

		final String[] expressions = instruction.getAllParameterValues(PARAM_EXPRESSION);
		if ( expressions == null || expressions.length < 1 ) {
			return createStatus(instruction, Declined,
					createErrorResultParameters("No expression parameters provided.", "SDE.00001"));
		}

		final String expressionServiceId = instruction.getParameterValue(PARAM_EXPRESSION_LANGUAGE);
		if ( expressionServiceId == null || expressionServiceId.isEmpty() ) {
			return createStatus(instruction, Declined, createErrorResultParameters(
					"No expression language parameter provided.", "SDE.00002"));
		}

		ExpressionService expressionService = null;
		Iterable<ExpressionService> services = services(getExpressionServices());
		if ( services != null ) {
			for ( ExpressionService s : services ) {
				if ( expressionServiceId.equals(s.getUid()) ) {
					expressionService = s;
					break;
				}
			}
		}
		if ( expressionService == null ) {
			return createStatus(instruction, Declined, createErrorResultParameters(
					"Requested expression language service not available.", "SDE.00003"));
		}

		GeneralDatum d = new GeneralDatum(UUID.randomUUID().toString());

		ExpressionRoot root = new ExpressionRoot(d, null, null, datumService);

		List<Object> results = new ArrayList<>(expressions.length);

		for ( int i = 0; i < expressions.length; i++ ) {
			ExpressionConfig config = new ExpressionConfig("e" + i, DatumSamplesType.Status,
					expressions[i], expressionServiceId);

			final ExpressionServiceExpression expr;
			try {
				expr = config.getExpression(services);
			} catch ( ExpressionException e ) {
				return createStatus(instruction, Declined,
						createErrorResultParameters(String.format("Error parsing expression [%s]: %s",
								config.getExpression(), e.getMessage()), "SDE.00004"));
			}

			Object exprResult = null;
			if ( expr != null ) {
				try {
					exprResult = expr.getService().evaluateExpression(expr.getExpression(), null, root,
							null, Object.class);
					if ( log.isTraceEnabled() ) {
						log.trace(
								"Instruction [{}] evaluated datum property [{}] expression `{}` \u2192 {}\n\nExpression root: {}",
								instruction.getIdentifier(), config.getName(), config.getExpression(),
								exprResult, root);
					} else if ( log.isDebugEnabled() ) {
						log.debug(
								"Instruction [{}] evaluated datum property [{}] expression `{}` \u2192 {}",
								instruction.getIdentifier(), config.getName(), config.getExpression(),
								exprResult);
					}
				} catch ( ExpressionException e ) {
					log.warn(
							"Error evaluating instruction [{}] datum property [{}] expression `{}`: {}\n\nExpression root: {}",
							instruction.getIdentifier(), config.getName(), config.getExpression(),
							e.getMessage(), root);
					return createStatus(instruction, Declined,
							createErrorResultParameters(
									String.format("Error evaluating expression [%s]: %s",
											config.getExpression(), e.getMessage()),
									"SDE.00005"));
				}
			}
			if ( exprResult != null ) {
				results.add(exprResult);
				d.putSampleValue(config.getDatumPropertyType(), config.getName(), exprResult);
			}
		}

		return createStatus(instruction, Completed, singletonMap(PARAM_SERVICE_RESULT, results));
	}

}
