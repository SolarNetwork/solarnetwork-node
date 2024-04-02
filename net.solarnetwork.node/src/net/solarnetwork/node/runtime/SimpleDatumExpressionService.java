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

import static net.solarnetwork.service.OptionalService.service;
import java.util.UUID;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.node.domain.ExpressionRoot;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionHandler;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.service.DatumService;
import net.solarnetwork.node.service.support.BaseIdentifiable;
import net.solarnetwork.node.service.support.ExpressionConfig;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.util.ObjectUtils;

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

	private final OptionalService<DatumService> datumService;

	/**
	 * Constructor.
	 *
	 * @param datumService
	 *        the datum service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SimpleDatumExpressionService(OptionalService<DatumService> datumService) {
		super();
		this.datumService = ObjectUtils.requireNonNullArgument(datumService, "datumService");
	}

	@Override
	public boolean handlesTopic(String topic) {
		return TOPIC_DATUM_EXPRESSION.equals(topic);
	}

	@Override
	public InstructionStatus processInstruction(Instruction instruction) {
		// TODO Auto-generated method stub
		GeneralDatum d = new GeneralDatum(UUID.randomUUID().toString());
		ExpressionRoot root = new ExpressionRoot(d, null, null, service(datumService));

		String[] expressions = instruction.getAllParameterValues(PARAM_EXPRESSION);
		String expressionServiceId = instruction.getParameterValue(PARAM_EXPRESSION_LANGUAGE);

		ExpressionConfig[] confs = new ExpressionConfig[expressions.length];

		for ( int i = 0; i < confs.length; i++ ) {
			ExpressionConfig conf = new ExpressionConfig(UUID.randomUUID().toString(),
					DatumSamplesType.Status, expressions[i], expressionServiceId);
			confs[i] = conf;
		}

		populateExpressionDatumProperties(d, confs, root);

		return null;
	}

}
