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

import static net.solarnetwork.service.ExpressionService.getGeneralExpressionReferenceLink;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.springframework.expression.Expression;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.service.support.ExpressionServiceExpression;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Stores the configuration for accessing an eGauge register.
 * 
 * @author maxieduncan
 * @version 2.1
 */
public class EGaugePropertyConfig {

	private String registerName;
	private String expression;
	private String expressionServiceId;

	private Expression cachedExpression;

	/**
	 * Default constructor.
	 */
	public EGaugePropertyConfig() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param registerName
	 *        the register name
	 */
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

	/**
	 * Get a list of settings for this configuration.
	 * 
	 * @param prefix
	 *        a setting key prefix to use
	 * @param registerNames
	 *        the available eGauge register names
	 * @param expressionServices
	 *        the available {@link ExpressionService}
	 * @return the list of settings, never {@literal null}
	 */
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
			propTypeTitles.put("", ""); // empty value for "no selection"
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
				BasicTextFieldSettingSpecifier exprSetting = new BasicTextFieldSettingSpecifier(
						prefix + "expression", "");
				exprSetting.setDescriptionArguments(
						new Object[] { getGeneralExpressionReferenceLink(), expressionReferenceLink() });
				results.add(exprSetting);
				results.add(expressionServiceId);
			}
		}

		return results;
	}

	/**
	 * Get a link to a general expression service guide.
	 * 
	 * @return a link to a general guide
	 */
	public static URI expressionReferenceLink() {
		String result = "https://github.com/SolarNetwork/solarnetwork-node/tree/develop/net.solarnetwork.node.datum.egauge.ws#expressions";
		Properties props = new Properties();
		try (InputStream in = EGaugePropertyConfig.class
				.getResourceAsStream("EGaugePropertyConfig.properties")) {
			if ( in != null ) {
				props.load(in);
				if ( props.containsKey("expressions.url") ) {
					result = props.getProperty("expressions.url");
				}
			}
		} catch ( IOException e ) {
			// ignore this
		}
		URI uri = null;
		if ( result != null ) {
			try {
				uri = new URI(result);
			} catch ( URISyntaxException e ) {
				throw new RuntimeException(e);
			}
		}
		return uri;
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

	/**
	 * Get the appropriate {@link Expression} to use for this property
	 * configuration, if an expression is configured and the appropriate service
	 * is available.
	 * 
	 * @param services
	 *        the available services
	 * @return the expression instance, or {@literal null} if no expression
	 *         configured or the appropriate service is not found
	 * @since 1.1
	 */
	public synchronized ExpressionServiceExpression getExpression(Iterable<ExpressionService> services) {
		for ( ExpressionService service : services ) {
			if ( service != null && service.getUid().equalsIgnoreCase(expressionServiceId) ) {
				Expression expr = cachedExpression;
				if ( expr == null ) {
					expr = service.parseExpression(expression);
					if ( expr != null ) {
						cachedExpression = expr;
					}
				}
				if ( expr != null ) {
					return new ExpressionServiceExpression(service, expr);
				}
			}
		}
		return null;
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
	public synchronized void setExpression(String expression) {
		this.expression = expression;
		this.cachedExpression = null;
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
	public synchronized void setExpressionServiceId(String expressionServiceId) {
		this.expressionServiceId = expressionServiceId;
		this.cachedExpression = null;
	}

}
