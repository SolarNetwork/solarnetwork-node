/* ==================================================================
 * ExpressionRoot.java - 3/02/2022 10:22:14 AM
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

import java.util.HashMap;
import java.util.Map;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.DatumService;

/**
 * Load balancer expression root object.
 * 
 * @author matt
 * @version 1.0
 */
public class ExpressionRoot extends net.solarnetwork.node.domain.ExpressionRoot {

	/** The parameter name for a number minimum value. */
	public static final String PARAM_MIN_VALUE = "minValue";

	/** The parameter name for a number maximum value. */
	public static final String PARAM_MAX_VALUE = "maxValue";

	/**
	 * Create a new instance.
	 * 
	 * @param datum
	 *        the datum
	 * @param datumService
	 *        the optional datum service
	 * @param minValue
	 *        the minimum desired value, will be configured as the parameter
	 *        {@link #PARAM_MIN_VALUE}
	 * @param maxValue
	 *        the maximum desired value, will be configured as the parameter
	 *        {@link #PARAM_MAX_VALUE}
	 * @param parameters
	 *        optional additional parameters to use with the expression
	 * @return the new instance
	 */
	public static ExpressionRoot of(NodeDatum datum, DatumService datumService, Number minValue,
			Number maxValue, Map<String, ?> parameters) {
		Map<String, Object> params = new HashMap<>(2 + (parameters != null ? parameters.size() : 0));
		if ( parameters != null ) {
			params.putAll(parameters);
		}
		if ( minValue != null ) {
			params.put(PARAM_MIN_VALUE, minValue);
		}
		if ( maxValue != null ) {
			params.put(PARAM_MAX_VALUE, maxValue);
		}
		return new ExpressionRoot(datum, datumService, params);
	}

	/**
	 * Constructor.
	 * 
	 * @param datum
	 *        the datum
	 * @param datumService
	 *        the optional datum service
	 * @param parameters
	 *        the parameters
	 */
	public ExpressionRoot(NodeDatum datum, DatumService datumService, Map<String, ?> parameters) {
		super(datum, null, parameters, datumService);
	}

}
