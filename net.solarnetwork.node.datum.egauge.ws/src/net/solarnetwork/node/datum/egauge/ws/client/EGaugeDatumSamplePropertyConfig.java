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

import net.solarnetwork.domain.GeneralDatumSamplePropertyConfig;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.support.ExpressionService;;

/**
 * eGauge typed extension to GeneralDatumSamplePropertyConfig
 * 
 * @author maxieduncan
 * @version 1.1
 */
public class EGaugeDatumSamplePropertyConfig
		extends GeneralDatumSamplePropertyConfig<EGaugePropertyConfig> {

	public EGaugeDatumSamplePropertyConfig() {
		super();
		setPropertyType(GeneralDatumSamplesType.Instantaneous);
		setConfig(new EGaugePropertyConfig());
	}

	public EGaugeDatumSamplePropertyConfig(String propertyKey, GeneralDatumSamplesType propertyType,
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
	 * Get the appropriate {@link ExpressionService} to use for this property
	 * configuration, if an expression is configured and the appropriate service
	 * is available.
	 * 
	 * @param services
	 *        the available services
	 * @return the service, or {@literal null} if no expression configured or
	 *         the appropriate service is not found
	 * @since 1.1
	 */
	public ExpressionService getExpressionService(Iterable<ExpressionService> services) {
		EGaugePropertyConfig config = getConfig();
		if ( services != null && config != null && config.getExpression() != null
				&& !config.getExpression().isEmpty() && config.getExpressionServiceId() != null
				&& !config.getExpressionServiceId().isEmpty() ) {
			String id = config.getExpressionServiceId();
			for ( ExpressionService service : services ) {
				if ( service != null && id.equalsIgnoreCase(service.getUid()) ) {
					return service;
				}
			}
		}
		return null;
	}

}
