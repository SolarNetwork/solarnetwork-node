/* ==================================================================
 * EGaugePropertyConfig.java - 14/03/2018 10:08:29 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.egauge.ws.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.support.ExpressionService;

/**
 * Stores the configuration for accessing an eGauge register.
 * 
 * @author maxieduncan
 * @version 1.1
 */
public class EGaugePropertyConfig {

	private String registerName;
	private String expression;
	private String expressionServiceId;

	public EGaugePropertyConfig() {
		super();
	}

	public EGaugePropertyConfig(String registerName) {
		this();
		this.registerName = registerName;
	}

	/**
	 * Construct with an expression and expression service ID.
	 * 
	 * @param expression
	 *        the expression
	 * @param expressionServiceId
	 *        the expression service ID
	 * @since 1.1
	 */
	public EGaugePropertyConfig(String expression, String expressionServiceId) {
		this();
		this.expression = expression;
		this.expressionServiceId = expressionServiceId;
	}

	public static List<SettingSpecifier> settings(String prefix, List<String> registerNames,
			Iterable<ExpressionService> expressionServices) {
		List<SettingSpecifier> results = new ArrayList<>();

		// main register name
		if ( registerNames == null || registerNames.isEmpty() ) {
			results.add(new BasicTextFieldSettingSpecifier(prefix + "registerName", ""));
		} else {
			BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
					prefix + "registerName", registerNames.get(0));
			Map<String, String> propTypeTitles = new LinkedHashMap<>();
			for ( String registerName : registerNames ) {
				propTypeTitles.put(registerName, registerName);
			}
			propTypeSpec.setValueTitles(propTypeTitles);
			results.add(propTypeSpec);
		}

		// the expression drop-down menu (if we have expression services available)
		if ( expressionServices != null ) {
			BasicMultiValueSettingSpecifier expressionServiceId = new BasicMultiValueSettingSpecifier(
					prefix + "expressionServiceId", "");
			Map<String, String> exprServiceTitles = new LinkedHashMap<>();
			for ( ExpressionService service : expressionServices ) {
				exprServiceTitles.put(service.getUid(), service.getDisplayName());
			}
			expressionServiceId.setValueTitles(exprServiceTitles);

			// only populate settings for expressions if we have at least one ExpressionService available
			if ( !exprServiceTitles.isEmpty() ) {
				results.add(expressionServiceId);
				results.add(new BasicTextFieldSettingSpecifier(prefix + "expression", ""));
			}
		}

		return results;
	}

	/**
	 * Test if this configuration appears to be valid.
	 * 
	 * <p>
	 * This only verifies that expected properties have non-empty values.
	 * </p>
	 * 
	 * @return {@literal true} if the configuration appears valid,
	 *         {@literal false} otherwise
	 */
	public boolean isValid() {
		return (registerName != null && !registerName.trim().isEmpty())
				|| (expression != null && !expression.trim().isEmpty() && expressionServiceId != null
						&& !expressionServiceId.trim().isEmpty());
	}

	@Override
	public String toString() {
		return "EGaugePropertyConfig{registerName=" + registerName + "}";
	}

	/**
	 * Get the register name to read.
	 * 
	 * @return the register name
	 */
	public String getRegisterName() {
		return registerName;
	}

	/**
	 * Set the register name to read.
	 * 
	 * @param register
	 *        the register to set
	 * @since 1.1
	 */
	public void setRegisterName(String register) {
		this.registerName = register;
	}

	/**
	 * Get the expression to use for evaluating a dynamic property value at
	 * runtime.
	 * 
	 * @return the expression
	 * @since 1.1
	 */
	public String getExpression() {
		return expression;
	}

	/**
	 * Set an expression to use for evaluating a dynamic property value at
	 * runtime.
	 * 
	 * <p>
	 * The expression will use a {@link ExpressionRoot} instance as the root
	 * object, which will be populated with the data read from the EGauge
	 * device. The expression language is determined by the configured
	 * {@link ExpressionService}, set via
	 * {@link #setExpressionServiceId(String)}.
	 * </p>
	 * 
	 * @param expression
	 *        the expression
	 * @since 1.1
	 */
	public void setExpression(String expression) {
		this.expression = expression;
	}

	/**
	 * Get the {@link ExpressionService} ID to use when evaluating
	 * {@link #getExpression()}.
	 * 
	 * @return the service ID
	 * @since 1.1
	 */
	public String getExpressionServiceId() {
		return expressionServiceId;
	}

	/**
	 * Set the {@link ExpressionService} ID to use when evaluating
	 * {@link #getExpression()}.
	 * 
	 * @param expressionServiceId
	 *        the service ID, or {@literal null} to not evaluate
	 * @since 1.1
	 */
	public void setExpressionServiceId(String expressionServiceId) {
		this.expressionServiceId = expressionServiceId;
	}

}
