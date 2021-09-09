/* ==================================================================
 * OperationalModeTransformConfig.java - 4/07/2021 1:30:13 PM
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

package net.solarnetwork.node.datum.filter.opmode;

import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.service.support.ExpressionConfig;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Operational mode expression configuration.
 * 
 * @author matt
 * @version 2.0
 * @since 1.8
 */
public class OperationalModeTransformConfig extends ExpressionConfig {

	private String operationalMode;
	private int expireSeconds;

	/**
	 * Constructor.
	 */
	public OperationalModeTransformConfig() {
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
	public OperationalModeTransformConfig(String name, DatumSamplesType propertyType, String expression,
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
		List<SettingSpecifier> result = new ArrayList<>(5);
		result.add(new BasicTextFieldSettingSpecifier(prefix + "operationalMode", null));
		result.add(new BasicTextFieldSettingSpecifier(prefix + "expireSeconds", null));
		result.addAll(ExpressionConfig.settings(OperationalModeTransformConfig.class, prefix,
				expressionServices));
		return result;
	}

	/**
	 * Get the operational mode to toggle.
	 * 
	 * @return the operational mode
	 */
	public String getOperationalMode() {
		return operationalMode;
	}

	/**
	 * Set the operational mode to toggle.
	 * 
	 * @param operationalMode
	 *        the operational mode
	 */
	public void setOperationalMode(String operationalMode) {
		if ( operationalMode != null && operationalMode.trim().isEmpty() ) {
			operationalMode = null;
		}
		this.operationalMode = operationalMode;
	}

	/**
	 * Get the expire seconds.
	 * 
	 * @return the number of seconds to automatically expire operational mode
	 *         activation
	 */
	public int getExpireSeconds() {
		return expireSeconds;
	}

	/**
	 * Set the expire seconds.
	 * 
	 * <p>
	 * If configured, then the configured expression will only be evaluated when
	 * the given operational mode is <b>not already active</b>. In this way, the
	 * operational mode will always become inactive after {@code expireSeconds},
	 * and only after then can this expression re-activate it.
	 * </p>
	 * 
	 * @param expireSeconds
	 *        the number of seconds to automatically expire operational mode
	 *        activation
	 */
	public void setExpireSeconds(int expireSeconds) {
		this.expireSeconds = expireSeconds;
	}

}
