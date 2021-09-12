/* ==================================================================
 * ExpressionConfig.java - 20/02/2019 7:36:01 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.modbus;

import java.util.List;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.util.IntRangeSet;

/**
 * Configuration for a single datum property to be set via an expression.
 * 
 * <p>
 * The {@link #getConfig()} value represents the expression to evaluate.
 * </p>
 * 
 * @author matt
 * @version 3.0
 * @since 1.4
 */
public class ExpressionConfig extends net.solarnetwork.node.service.support.ExpressionConfig {

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
		return net.solarnetwork.node.service.support.ExpressionConfig.settings(ExpressionConfig.class,
				prefix, expressionServices);
	}

	/**
	 * Default constructor.
	 */
	public ExpressionConfig() {
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
	public ExpressionConfig(String name, DatumSamplesType propertyType, String expression,
			String expressionServiceId) {
		super(name, propertyType, expression, expressionServiceId);
	}

	/**
	 * Get a set of referenced Modbus register addresses in the configured
	 * expression.
	 * 
	 * @return the referenced addresses, never {@literal null}
	 */
	public IntRangeSet registerAddressReferences() {
		return ExpressionRoot.registerAddressReferences(getExpression());
	}

}
