/* ==================================================================
 * ExpressionTransformConfig.java - 13/05/2021 9:44:27 PM
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

package net.solarnetwork.node.datum.filter.expr;

import java.util.List;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.service.support.ExpressionConfig;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.settings.SettingSpecifier;

/**
 * Expression transform service configuration.
 * 
 * @author matt
 * @version 2.0
 * @since 1.6
 */
public class ExpressionTransformConfig extends ExpressionConfig {

	/**
	 * Constructor.
	 */
	public ExpressionTransformConfig() {
		super();
	}

	/**
	 * Construct with values.
	 * 
	 * @param name
	 *        the datum property name
	 * @param propertyType
	 *        the datum property type
	 * @param expression
	 *        the expression
	 * @param expressionServiceId
	 *        the expression service ID
	 */
	public ExpressionTransformConfig(String name, DatumSamplesType propertyType, String expression,
			String expressionServiceId) {
		super(name, propertyType, expression, expressionServiceId);
	}

	/**
	 * Get settings suitable for configuring an instance of this class.
	 * 
	 * @param prefix
	 *        a setting key prefix to use
	 * @param expressionServices
	 *        the available expression services
	 * @return the settings, never {@literal null}
	 */
	public static List<SettingSpecifier> settings(String prefix,
			Iterable<ExpressionService> expressionServices) {
		return ExpressionConfig.settings(ExpressionTransformConfig.class, prefix, expressionServices);
	}

}
