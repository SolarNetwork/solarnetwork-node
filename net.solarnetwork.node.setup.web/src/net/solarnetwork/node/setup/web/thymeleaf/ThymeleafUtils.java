/* ==================================================================
 * ThymeleafUtils.java - 17/06/2025 6:19:29 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.setup.web.thymeleaf;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.engine.EngineEventUtils;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.FragmentExpression;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.StandardExpressionExecutionContext;

/**
 * Utility support for Thymeleaf.
 *
 * @author matt
 * @version 1.0
 */
public final class ThymeleafUtils {

	private ThymeleafUtils() {
		// not available
	}

	/** A default processor precedence. */
	public static final int DEFAULT_PROCESSOR_PRECEDENCE = 500;

	/**
	 * Evaluate an expression.
	 *
	 * @param context
	 * @param tag
	 * @param attributeName
	 * @param attributeValue
	 * @param structureHandler
	 * @param restrictedExpressionExecution
	 * @return
	 */
	public static Object evaulateAttributeExpression(final ITemplateContext context,
			final IProcessableElementTag tag, final AttributeName attributeName,
			final String attributeValue, final IElementTagStructureHandler structureHandler,
			final boolean restrictedExpressionExecution) {
		final Object expressionResult;

		if ( attributeValue != null ) {
			final IStandardExpression expression = EngineEventUtils.computeAttributeExpression(context,
					tag, attributeName, attributeValue);
			if ( expression != null && expression instanceof FragmentExpression ) {
				final FragmentExpression.ExecutedFragmentExpression executedFragmentExpression = FragmentExpression
						.createExecutedFragmentExpression(context, (FragmentExpression) expression);

				expressionResult = FragmentExpression.resolveExecutedFragmentExpression(context,
						executedFragmentExpression, true);
			} else {
				final StandardExpressionExecutionContext expressionExecutionContext = restrictedExpressionExecution
						? StandardExpressionExecutionContext.RESTRICTED
						: StandardExpressionExecutionContext.NORMAL;

				expressionResult = expression.execute(context, expressionExecutionContext);
			}
		} else {
			expressionResult = null;
		}

		return expressionResult;
	}
}
