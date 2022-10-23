/* ==================================================================
 * EGaugeDatumSamplePropertyConfig.java - 18/03/2018 12:56:13 PM
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

import org.springframework.expression.Expression;
import net.solarnetwork.domain.datum.DatumSamplePropertyConfig;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.service.ExpressionService;
import net.solarnetwork.service.support.ExpressionServiceExpression;

/**
 * eGauge typed extension to DatumSamplePropertyConfig
 * 
 * @author maxieduncan
 * @version 2.0
 */
public class EGaugeDatumSamplePropertyConfig extends DatumSamplePropertyConfig<EGaugePropertyConfig> {

	/**
	 * Default constructor.
	 */
	public EGaugeDatumSamplePropertyConfig() {
		super();
		setPropertyType(DatumSamplesType.Instantaneous);
		setConfig(new EGaugePropertyConfig());
	}

	/**
	 * Constructor.
	 * 
	 * @param propertyKey
	 *        the property key
	 * @param propertyType
	 *        the property type
	 * @param propertyConfig
	 *        the property configuration
	 */
	public EGaugeDatumSamplePropertyConfig(String propertyKey, DatumSamplesType propertyType,
			EGaugePropertyConfig propertyConfig) {
		super(propertyKey, propertyType, propertyConfig);
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
		if ( getPropertyType() == null || getPropertyKey() == null || getPropertyKey().trim().isEmpty()
				|| getConfig() == null || !getConfig().isValid() ) {
			return false;
		}
		return true;
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
	public ExpressionServiceExpression getExpression(Iterable<ExpressionService> services) {
		EGaugePropertyConfig config = getConfig();
		if ( services != null && config != null ) {
			return config.getExpression(services);
		}
		return null;
	}

}
