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

import static java.util.Collections.singleton;
import java.util.Collection;
import java.util.Map;
import net.solarnetwork.domain.DatumExpressionRoot;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamplesExpressionRoot;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.DatumService;

/**
 * An object to use as the "root" for
 * {@link net.solarnetwork.service.ExpressionService} evaluation.
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

	private final DatumService datumService;

	/**
	 * Constructor.
	 * 
	 * @param datum
	 *        the datum currently being populated
	 */
	public ExpressionRoot(Datum datum) {
		this(datum, null, null, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param datum
	 *        the datum currently being populated
	 * @param samples
	 *        the samples
	 */
	public ExpressionRoot(Datum datum, DatumSamplesOperations samples) {
		this(datum, samples, null, null);
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
		this(datum, null, data, null);
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
	 * @param datumService
	 *        the optional datum service
	 */
	public ExpressionRoot(Datum datum, DatumSamplesOperations samples, Map<String, ?> parameters,
			DatumService datumService) {
		super(datum, samples, parameters);
		this.datumService = datumService;
	}

	/**
	 * Test if a "latest" datum is available for a given source ID.
	 * 
	 * <p>
	 * This can be used to test if {@link #latest(String)} will return a
	 * non-null value.
	 * </p>
	 * 
	 * @param sourceId
	 *        the source ID of the datum to look for
	 * @return {@literal true} if {@link #latest(String)} for the given
	 *         {@code sourceId} will return a non-null value
	 */
	public boolean hasLatest(String sourceId) {
		if ( datumService == null || sourceId == null ) {
			return false;
		}
		Collection<NodeDatum> d = datumService.latest(singleton(sourceId), NodeDatum.class);
		return (d != null && !d.isEmpty());
	}

	/**
	 * Get the latest available datum for a given source ID, as an
	 * {@link DatumExpressionRoot}.
	 * 
	 * <p>
	 * Note a non-null {@link DatumService} instance must have been provided to
	 * the constructor of this instance for this method to work.
	 * </p>
	 * 
	 * @param sourceId
	 *        the source ID of the datum to look for
	 * @return the latest datum, or {@literal null} if {@code sourceId} is
	 *         {@literal null}, the {@link DatumService} provided to this
	 *         instance's constructor was {@literal null}, or
	 *         {@link DatumService#latest(java.util.Set, Class)} returns
	 *         {@literal null} for the given {@code sourceId}
	 */
	public DatumExpressionRoot latest(String sourceId) {
		if ( datumService == null || sourceId == null ) {
			return null;
		}
		Collection<NodeDatum> d = datumService.latest(singleton(sourceId), NodeDatum.class);
		if ( d == null || d.isEmpty() ) {
			return null;
		}
		return new ExpressionRoot(d.iterator().next());
	}

}
