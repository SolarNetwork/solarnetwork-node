/* ==================================================================
 * ControlPropertyConfig.java - 24/09/2021 12:44:43 PM
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

package net.solarnetwork.node.control.datumreactor;

import static java.util.stream.Collectors.toList;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.node.service.support.ExpressionConfig;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.settings.KeyedSettingSpecifier;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Configuration for a single datum property associate with a managed control.
 * 
 * <p>
 * The {@link #getConfig()} value represents the expression.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class ControlPropertyConfig extends ExpressionConfig {

	private BigDecimal minValue;
	private BigDecimal maxValue;
	private String controlId;

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
		List<SettingSpecifier> result = new ArrayList<>(8);

		result.add(new BasicTextFieldSettingSpecifier(prefix + "controlId", null));
		result.add(new BasicTextFieldSettingSpecifier(prefix + "minValue", null));
		result.add(new BasicTextFieldSettingSpecifier(prefix + "maxValue", null));

		result.addAll(ExpressionConfig.settings(ControlPropertyConfig.class, prefix, expressionServices)
				.stream().filter(s -> {
					// remove property name/type because we don't care about individual property in expression
					if ( s instanceof KeyedSettingSpecifier<?> ) {
						KeyedSettingSpecifier<?> kss = (KeyedSettingSpecifier<?>) s;
						final String key = kss.getKey();
						if ( key.endsWith(".datumPropertyTypeKey") || key.endsWith(".name") ) {
							return false;
						}
					}
					return true;
				}).collect(toList()));

		return result;
	}

	/**
	 * Create a new configuration instance.
	 * 
	 * @param controlId
	 *        the control ID
	 * @return the configuration, never {@literal null}
	 */
	public static ControlPropertyConfig of(String controlId) {
		ControlPropertyConfig cfg = new ControlPropertyConfig();
		cfg.setControlId(controlId);
		return cfg;
	}

	/**
	 * Constructor.
	 */
	public ControlPropertyConfig() {
		super();
	}

	/**
	 * Test if this instance has a valid configuration.
	 * 
	 * <p>
	 * This method simply verifies the minimum level of configuration is
	 * available for the control to be used.
	 * </p>
	 * 
	 * @return {@literal true} if this configuration is valid for use
	 */
	public boolean isValid() {
		final String controlId = getControlId();
		return controlId != null && controlId.trim().length() > 0;
	}

	/**
	 * Get the control ID.
	 * 
	 * @return the control ID
	 */
	public String getControlId() {
		return controlId;
	}

	/**
	 * Set the control ID.
	 * 
	 * @param controlId
	 *        the control ID to set
	 */
	public void setControlId(String controlId) {
		this.controlId = controlId;
	}

	/**
	 * Get a minimum value to limit the output to.
	 * 
	 * @return the minimum value
	 */
	public BigDecimal getMinValue() {
		return minValue;
	}

	/**
	 * Set a minimum value to limit the output to.
	 * 
	 * @param minValue
	 *        the minimum value to set, or {@literal null} for no limit
	 */
	public void setMinValue(BigDecimal minValue) {
		this.minValue = minValue;
	}

	/**
	 * Get a maximum value to limit the output to.
	 * 
	 * @return the maximum value
	 */
	public BigDecimal getMaxValue() {
		return maxValue;
	}

	/**
	 * Set a maximum value to limit the output to.
	 * 
	 * @param maxValue
	 *        the maximum value to set, or {@literal null} for no limit
	 */
	public void setMaxValue(BigDecimal maxValue) {
		this.maxValue = maxValue;
	}

}
