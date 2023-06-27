/* ==================================================================
 * ControlConfig.java - 13/06/2023 6:32:32 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.filter.control;

import java.util.ArrayList;
import java.util.List;
import net.solarnetwork.node.service.support.ExpressionConfig;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Configuration for a control setting.
 * 
 * @author matt
 * @version 1.0
 */
public class ControlConfig extends ExpressionConfig {

	private String controlId;

	/**
	 * Constructor.
	 */
	public ControlConfig() {
		super();
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
		List<SettingSpecifier> results = new ArrayList<>(8);

		results.add(new BasicTextFieldSettingSpecifier(prefix + "controlId", null));

		results.addAll(ExpressionConfig.settings(ControlConfig.class, prefix, expressionServices));

		return results;
	}

	/**
	 * Test if this configuration appears to be valid.
	 * 
	 * @return {@literal true} if the configuration has all necessary properties
	 *         configured
	 */
	public boolean isValid() {
		final String controlId = getControlId();
		final String expr = getExpression();
		final String exprServiceId = getExpressionServiceId();
		return (controlId != null && !controlId.isEmpty() && expr != null && !expr.isEmpty()
				&& exprServiceId != null && !exprServiceId.isEmpty());
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

}
