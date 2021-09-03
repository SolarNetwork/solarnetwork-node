/* ==================================================================
 * ExpressionRoot.java - 20/02/2019 10:21:55 am
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

package net.solarnetwork.node.domain;

import java.util.Map;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesExpressionRoot;

/**
 * An object to use as the "root" for
 * {@link net.solarnetwork.support.ExpressionService} evaluation.
 * 
 * <p>
 * This object extends {@link DatumSamplesExpressionRoot} to allow all datum
 * sample properties to be exposed as top-level expression properties (via the
 * {@code Map} API).
 * </p>
 * 
 * @author matt
 * @version 2.0
 * @since 1.79
 */
public class ExpressionRoot extends DatumSamplesExpressionRoot {

	/**
	 * Constructor.
	 * 
	 * @param datum
	 *        the datum currently being populated
	 */
	public ExpressionRoot(Datum datum) {
		this(datum, null, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param datum
	 *        the datum currently being populated
	 * @param samples
	 *        the samples
	 */
	public ExpressionRoot(Datum datum, DatumSamples samples) {
		this(datum, samples, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param data
	 *        the map data
	 * @param datum
	 *        the datum currently being populated
	 * @since 1.2
	 */
	public ExpressionRoot(Map<String, ?> data, Datum datum) {
		this(datum, null, data);
	}

	/**
	 * Constructor.
	 * 
	 * @param datum
	 *        the datum currently being populated
	 * @param samples
	 *        the samples
	 * @param parameters
	 *        the parameters
	 */
	public ExpressionRoot(Datum datum, DatumSamples samples, Map<String, ?> parameters) {
		super(datum, samples, parameters);
	}

}
