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

import static net.solarnetwork.support.ExpressionService.getGeneralExpressionReferenceLink;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.springframework.expression.Expression;
import net.solarnetwork.domain.GeneralDatumSamplePropertyConfig;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.node.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.support.ExpressionService;
import net.solarnetwork.support.ExpressionServiceExpression;

/**
 * Configuration for a single datum property to be set via an expression.
 * 
 * <p>
 * The {@link #getConfig()} value represents the expression to evaluate.
 * </p>
 * 
 * @author matt
 * @version 1.1
 * @since 1.4
 */
public class ExpressionConfig extends GeneralDatumSamplePropertyConfig<String> {

	private String expressionServiceId;

	private Expression cachedExpression;

	/**
	 * Default constructor.
	 */
	public ExpressionConfig() {
		super(null, GeneralDatumSamplesType.Instantaneous, null);
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
	public ExpressionConfig(String name, GeneralDatumSamplesType propertyType, String expression,
			String expressionServiceId) {
		super(name, propertyType, expression);
		this.expressionServiceId = expressionServiceId;
	}

	/**
	 * Get settings suitable for configuring an instance of this class.
	 * 
	 * @param prefix
	 *        a setting key prefix to use
	 * @return the settings, never {@literal null}
	 */
	public static List<SettingSpecifier> settings(String prefix,
			Iterable<ExpressionService> expressionServices) {
		ExpressionConfig defaults = new ExpressionConfig();
		List<SettingSpecifier> results = new ArrayList<>(3);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "name", ""));

		// drop-down menu for datumPropertyType
		BasicMultiValueSettingSpecifier propTypeSpec = new BasicMultiValueSettingSpecifier(
				prefix + "datumPropertyTypeKey", defaults.getDatumPropertyTypeKey());
		Map<String, String> propTypeTitles = new LinkedHashMap<>(3);
		for ( GeneralDatumSamplesType e : GeneralDatumSamplesType.values() ) {
			propTypeTitles.put(Character.toString(e.toKey()), e.toString());
		}
		propTypeSpec.setValueTitles(propTypeTitles);
		results.add(propTypeSpec);

		// the expression drop-down menu (if we have expression services available)
		if ( expressionServices != null ) {
			BasicMultiValueSettingSpecifier expressionServiceId = new BasicMultiValueSettingSpecifier(
					prefix + "expressionServiceId", "");
			Map<String, String> exprServiceTitles = new LinkedHashMap<>();
			exprServiceTitles.put("", "");
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
	 * Get a link to an expression service guide.
	 * 
	 * @return a link to an expression guide
	 */
	public static URI expressionReferenceLink() {
		String result = "https://github.com/SolarNetwork/solarnetwork-node/tree/master/net.solarnetwork.node.datum.modbus#expressions";
		Properties props = new Properties();
		try (InputStream in = ExpressionConfig.class
				.getResourceAsStream("ExpressionConfig.properties")) {
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
					expr = service.parseExpression(getExpression());
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

	/**
	 * Get a set of referenced Modbus register addresses in the configured
	 * expression.
	 * 
	 * @return the referenced addresses, never {@literal null}
	 */
	public Set<Integer> registerAddressReferences() {
		return ExpressionRoot.registerAddressReferences(getExpression());
	}

	/**
	 * Get the datum property name used for this configuration.
	 * 
	 * <p>
	 * This is an alias for {@link #getPropertyKey()}.
	 * </p>
	 * 
	 * @return the property name
	 */
	public String getName() {
		return getPropertyKey();
	}

	/**
	 * Set the datum property name to use.
	 * 
	 * <p>
	 * This is an alias for {@link #setPropertyKey(String)}.
	 * </p>
	 * 
	 * @param name
	 *        the property name
	 */
	public void setName(String name) {
		setPropertyKey(name);
	}

	/**
	 * Get the expression.
	 * 
	 * <p>
	 * This is an alias for {@link #getConfig()}.
	 * 
	 * @return the expression
	 */
	public synchronized String getExpression() {
		return getConfig();
	}

	/**
	 * Set the expression.
	 * 
	 * <p>
	 * This is an alias for {@link #setConfig(String)}.
	 * </p>
	 * 
	 * @param expression
	 *        the expression to set
	 */
	public synchronized void setExpression(String expression) {
		setConfig(expression);
		this.cachedExpression = null;
	}

	/**
	 * Get the {@link ExpressionService} ID to use when evaluating
	 * {@link #getExpression()}.
	 * 
	 * @return the service ID
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
	 */
	public synchronized void setExpressionServiceId(String expressionServiceId) {
		this.expressionServiceId = expressionServiceId;
		this.cachedExpression = null;
	}

	/**
	 * Get the datum property type.
	 * 
	 * <p>
	 * This is an alias for {@link #getPropertyType()}.
	 * </p>
	 * 
	 * @return the type
	 */
	public GeneralDatumSamplesType getDatumPropertyType() {
		return getPropertyType();
	}

	/**
	 * Set the datum property type.
	 * 
	 * <p>
	 * This is an alias for {@link #setPropertyType(GeneralDatumSamplesType)},
	 * and ignores a {@literal null} argument.
	 * </p>
	 * </p>
	 * 
	 * @param datumPropertyType
	 *        the datum property type to set
	 */
	public void setDatumPropertyType(GeneralDatumSamplesType datumPropertyType) {
		if ( datumPropertyType == null ) {
			return;
		}
		setPropertyType(datumPropertyType);
	}

	/**
	 * Get the property type key.
	 * 
	 * <p>
	 * This returns the configured {@link #getPropertyType()}
	 * {@link GeneralDatumSamplesType#toKey()} value as a string. If the type is
	 * not available, {@link GeneralDatumSamplesType#Instantaneous} will be
	 * returned.
	 * </p>
	 * 
	 * @return the property type key
	 */
	public String getDatumPropertyTypeKey() {
		GeneralDatumSamplesType type = getDatumPropertyType();
		if ( type == null ) {
			type = GeneralDatumSamplesType.Instantaneous;
		}
		return Character.toString(type.toKey());
	}

	/**
	 * Set the property type via a key value.
	 * 
	 * <p>
	 * This uses the first character of {@code key} as a
	 * {@link GeneralDatumSamplesType} key value to call
	 * {@link #setPropertyType(GeneralDatumSamplesType)}. If there is any
	 * problem parsing the type, {@link GeneralDatumSamplesType#Instantaneous}
	 * is set.
	 * </p>
	 * 
	 * @param key
	 *        the datum property type key to set
	 */
	public void setDatumPropertyTypeKey(String key) {
		GeneralDatumSamplesType type = null;
		if ( key != null && key.length() > 0 ) {
			try {
				type = GeneralDatumSamplesType.valueOf(key.charAt(0));
			} catch ( IllegalArgumentException e ) {
				// ignore
			}
		}
		if ( type == null ) {
			type = GeneralDatumSamplesType.Instantaneous;
		}
		setDatumPropertyType(type);
	}
}
