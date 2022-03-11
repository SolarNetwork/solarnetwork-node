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

import static java.lang.String.format;
import static net.solarnetwork.node.datum.modbus.ModbusDatumDataSourceConfig.JOB_SERVICE_SETTING_PREFIX;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.domain.Setting;
import net.solarnetwork.node.settings.SettingValueBean;
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
 * @version 3.1
 * @since 1.4
 */
public class ExpressionConfig extends net.solarnetwork.node.service.support.ExpressionConfig {

	/**
	 * A setting type pattern for an expression configuration element.
	 * 
	 * <p>
	 * The pattern has two capture groups: the expression configuration index
	 * and the property setting name.
	 * </p>
	 * 
	 * @since 3.1
	 */
	public static final Pattern EXPR_SETTING_PATTERN = Pattern.compile(Pattern
			.quote(JOB_SERVICE_SETTING_PREFIX.concat("expressionConfigs[")).concat("(\\d+)\\]\\.(.*)"));

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
	 * Populate a setting as an expression configuration value, if possible.
	 * 
	 * @param config
	 *        the overall configuration
	 * @param setting
	 *        the setting to try to handle
	 * @return {@literal true} if the setting was handled as an expression
	 *         configuration value
	 * @since 3.1
	 */
	public static boolean populateFromSetting(ModbusDatumDataSourceConfig config, Setting setting) {
		Matcher m = EXPR_SETTING_PATTERN.matcher(setting.getType());
		if ( !m.matches() ) {
			return false;
		}
		int idx = Integer.parseInt(m.group(1));
		String name = m.group(2);
		List<ExpressionConfig> exprConfigs = config.getExpressionConfigs();
		if ( !(idx < exprConfigs.size()) ) {
			exprConfigs.add(idx, new ExpressionConfig());
		}
		ExpressionConfig exprConfig = exprConfigs.get(idx);
		String val = setting.getValue();
		if ( val != null && !val.isEmpty() ) {
			switch (name) {
				case "name":
					exprConfig.setName(val);
					break;
				case "datumPropertyTypeKey":
					exprConfig.setDatumPropertyTypeKey(val);
					break;
				case "expressionServiceId":
					exprConfig.setExpressionServiceId(val);
					break;
				case "expression":
					exprConfig.setExpression(val);
					break;
				default:
					// ignore
			}
		}
		return true;
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
	 * Test if this configuration appears to be valid.
	 * 
	 * @return {@literal true} if the configuration has all necessary properties
	 *         configured
	 * @since 3.1
	 */
	public boolean isValid() {
		final String propName = getName();
		return (propName != null && !propName.isEmpty());
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

	/**
	 * Generate a list of setting values.
	 * 
	 * @param providerId
	 *        the setting provider ID
	 * @param instanceId
	 *        the factory instance ID
	 * @param i
	 *        the expression index
	 * @return the settings
	 * @since 3.1
	 */
	public List<SettingValueBean> toSettingValues(String providerId, String instanceId, int i) {
		List<SettingValueBean> settings = new ArrayList<>(8);
		addSetting(settings, providerId, instanceId, i, "name", getName());
		addSetting(settings, providerId, instanceId, i, "datumPropertyTypeKey",
				getDatumPropertyTypeKey());
		addSetting(settings, providerId, instanceId, i, "expressionServiceId", getExpressionServiceId());
		addSetting(settings, providerId, instanceId, i, "expression", getExpression());
		return settings;
	}

	private static void addSetting(List<SettingValueBean> settings, String providerId, String instanceId,
			int i, String key, Object val) {
		if ( val == null ) {
			return;
		}
		settings.add(new SettingValueBean(providerId, instanceId,
				format("jobService.datumDataSource.expressionConfigs[%d].%s", i, key), val.toString()));
	}

}
