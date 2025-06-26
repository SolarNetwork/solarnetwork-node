/* ==================================================================
 * SolarNodeExperssionObjectFactory.java - 18/06/2025 6:12:04 am
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

import java.util.Set;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.expression.IExpressionObjectFactory;

/**
 * SolarNode expression support.
 *
 * @author matt
 * @version 1.0
 */
public class SolarNodeExperssionObjectFactory implements IExpressionObjectFactory {

	/** The {@code snObjects} object. */
	public static final String SN_UTILS_EXPRESSION_OBJECT_NAME = "snUtils";

	private static final Set<String> ALL_EXPRESSION_OBJECT_NAMES = Set
			.of(SN_UTILS_EXPRESSION_OBJECT_NAME);

	/**
	 * Constructor.
	 */
	public SolarNodeExperssionObjectFactory() {
		super();
	}

	@Override
	public Set<String> getAllExpressionObjectNames() {
		return ALL_EXPRESSION_OBJECT_NAMES;
	}

	@Override
	public boolean isCacheable(String expressionObjectName) {
		return true;
	}

	@Override
	public Object buildObject(IExpressionContext context, String expressionObjectName) {
		if ( SN_UTILS_EXPRESSION_OBJECT_NAME.equals(expressionObjectName) ) {
			return SolarNodeUtils.INSTANCE;
		}
		return null;
	}

}
