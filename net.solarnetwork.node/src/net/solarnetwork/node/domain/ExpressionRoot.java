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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import net.solarnetwork.domain.DatumExpressionRoot;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamplesExpressionRoot;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.DatumService;
import net.solarnetwork.node.service.OperationalModesService;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.util.StringUtils;

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
 * @version 2.1
 * @since 1.79
 */
public class ExpressionRoot extends DatumSamplesExpressionRoot {

	private final DatumService datumService;
	private final OperationalModesService opModesService;

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
		this(datum, samples, parameters, datumService, null);
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
	 * @param opModesService
	 *        the optional operational modes service
	 * @since 2.1
	 */
	public ExpressionRoot(Datum datum, DatumSamplesOperations samples, Map<String, ?> parameters,
			DatumService datumService, OperationalModesService opModesService) {
		super(datum, samples, parameters);
		this.datumService = datumService;
		this.opModesService = opModesService;
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
		return hasOffset(sourceId, 0);
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
		return offset(sourceId, 0);
	}

	/**
	 * Get the datum's source ID.
	 * 
	 * @return the source ID, or {@literal null}
	 * @since 2.1
	 */
	public String getSourceId() {
		Datum datum = getDatum();
		return (datum != null ? datum.getSourceId() : null);
	}

	/**
	 * Get the datum's timestamp.
	 * 
	 * @return the timestamp, or {@literal null}
	 * @since 2.1
	 */
	public Instant getTimestamp() {
		Datum datum = getDatum();
		return (datum != null ? datum.getTimestamp() : null);
	}

	/**
	 * Test if an offset from a "latest" datum is available for the
	 * {@link #getDatum()} source ID and timestamp.
	 * 
	 * <p>
	 * This can be used to test if {@link #offset(int)} will return a non-null
	 * value.
	 * </p>
	 * 
	 * @param offset
	 *        the offset from the latest, {@literal 0} being the latest and
	 *        {@literal 1} the next later, and so on
	 * @return {@literal true} if {@link #offset(String, Instant, int)} will
	 *         return a non-null value
	 * @since 2.1
	 * @see #hasOffset(String, Instant, int)
	 */
	public boolean hasOffset(int offset) {
		if ( offset == 0 ) {
			return true;
		}
		Datum datum = getDatum();
		if ( datum == null ) {
			return false;
		}
		return hasOffset(datum.getSourceId(), datum.getTimestamp(), offset);
	}

	/**
	 * Get an offset from latest available datum for the {@link #getDatum()}
	 * source ID and timestamp, as an {@link DatumExpressionRoot}.
	 * 
	 * <p>
	 * Note a non-null {@link DatumService} instance must have been provided to
	 * the constructor of this instance for this method to work.
	 * </p>
	 * 
	 * @param offset
	 *        the offset from the latest, {@literal 0} being the latest and
	 *        {@literal 1} the next later, and so on
	 * @return the offset from the latest datum, or {@literal null} if
	 *         {@link #getSourceId()} or {@link #getTimestamp()} are
	 *         {@literal null}, the {@link DatumService} provided to this
	 *         instance's constructor was {@literal null}, or
	 *         {@link DatumService#offset(java.util.Set, int, Class)} returns
	 *         {@literal null} for {@link #getSourceId()}
	 * @since 2.1
	 * @see #offset(String, Instant, int)
	 */
	public DatumExpressionRoot offset(int offset) {
		Datum datum = getDatum();
		if ( datum == null ) {
			return null;
		}
		if ( offset == 0 ) {
			return this;
		}
		return offset(datum.getSourceId(), datum.getTimestamp(), offset);
	}

	/**
	 * Test if an offset from a "latest" datum is available for a given source
	 * ID.
	 * 
	 * <p>
	 * This can be used to test if {@link #offset(String,int)} will return a
	 * non-null value.
	 * </p>
	 * 
	 * @param sourceId
	 *        the source ID of the datum to look for
	 * @param offset
	 *        the offset from the latest, {@literal 0} being the latest and
	 *        {@literal 1} the next later, and so on
	 * @return {@literal true} if {@link #offset(String, int)} for the given
	 *         {@code sourceId} will return a non-null value
	 * @since 2.1
	 */
	public boolean hasOffset(String sourceId, int offset) {
		if ( datumService == null || sourceId == null ) {
			return false;
		}
		NodeDatum d = datumService.offset(sourceId, offset, NodeDatum.class);
		return (d != null);
	}

	/**
	 * Get an offset from latest available datum for a given source ID, as an
	 * {@link DatumExpressionRoot}.
	 * 
	 * <p>
	 * Note a non-null {@link DatumService} instance must have been provided to
	 * the constructor of this instance for this method to work.
	 * </p>
	 * 
	 * @param sourceId
	 *        the source ID of the datum to look for
	 * @param offset
	 *        the offset from the latest, {@literal 0} being the latest and
	 *        {@literal 1} the next later, and so on
	 * @return the offset from the latest datum, or {@literal null} if
	 *         {@code sourceId} is {@literal null}, the {@link DatumService}
	 *         provided to this instance's constructor was {@literal null}, or
	 *         {@link DatumService#offset(java.util.Set, int, Class)} returns
	 *         {@literal null} for the given {@code sourceId}
	 * @since 2.1
	 */
	public DatumExpressionRoot offset(String sourceId, int offset) {
		if ( datumService == null || sourceId == null ) {
			return null;
		}
		NodeDatum d = datumService.offset(sourceId, offset, NodeDatum.class);
		if ( d == null ) {
			return null;
		}
		return new ExpressionRoot(d);
	}

	/**
	 * Test if a datum offset from a given timestamp is available for a given
	 * source ID.
	 * 
	 * <p>
	 * This can be used to test if {@link #offset(String,Instant,int)} will
	 * return a non-null value.
	 * </p>
	 * 
	 * @param sourceId
	 *        the source ID of the datum to look for
	 * @param timestamp
	 *        the timestamp refernce point
	 * @param offset
	 *        the offset from the latest, {@literal 0} being the latest and
	 *        {@literal 1} the next later, and so on
	 * @return {@literal true} if {@link #offset(String, int)} for the given
	 *         {@code sourceId} will return a non-null value
	 * @since 2.1
	 */
	public boolean hasOffset(String sourceId, Instant timestamp, int offset) {
		if ( datumService == null || sourceId == null || timestamp == null ) {
			return false;
		}
		NodeDatum d = datumService.offset(sourceId, timestamp, offset, NodeDatum.class);
		return (d != null);
	}

	/**
	 * Get a datum offset from a given timestamp for a given source ID, as an
	 * {@link DatumExpressionRoot}.
	 * 
	 * <p>
	 * Note a non-null {@link DatumService} instance must have been provided to
	 * the constructor of this instance for this method to work.
	 * </p>
	 * 
	 * @param sourceId
	 *        the source ID of the datum to look for
	 * @param timestamp
	 *        the timestamp refernce point
	 * @param offset
	 *        the offset from the latest, {@literal 0} being the latest and
	 *        {@literal 1} the next later, and so on
	 * @return the offset from the latest datum, or {@literal null} if
	 *         {@code sourceId} is {@literal null}, the {@link DatumService}
	 *         provided to this instance's constructor was {@literal null}, or
	 *         {@link DatumService#offset(java.util.Set, int, Class)} returns
	 *         {@literal null} for the given {@code sourceId}
	 * @since 2.1
	 */
	public DatumExpressionRoot offset(String sourceId, Instant timestamp, int offset) {
		if ( datumService == null || sourceId == null || timestamp == null ) {
			return null;
		}
		NodeDatum d = datumService.offset(sourceId, timestamp, offset, NodeDatum.class);
		if ( d == null ) {
			return null;
		}
		return new ExpressionRoot(d);
	}

	/**
	 * Return a {@link BigDecimal} for a given value.
	 * 
	 * @param value
	 *        the object to get as a {@link BigDecimal}
	 * @return the decimal instance, or {@literal null} if {@code value} is
	 *         {@literal null} or cannot be parsed as a decimal
	 * @since 2.1
	 */
	public BigDecimal decimal(Object value) {
		if ( value == null ) {
			return null;
		}
		Number n = null;
		if ( value instanceof Number ) {
			n = (Number) value;
		} else {
			n = StringUtils.numberValue(value.toString());
		}
		return NumberUtils.bigDecimalForNumber(n);

	}

	/**
	 * Test if an operational mode is active.
	 * 
	 * @param mode
	 *        the mode to test
	 * @return {@literal true} if the {@link OperationalModesService} provided
	 *         to this instance's constructor was not {@literal null} and
	 *         {@link OperationalModesService#isOperationalModeActive(String)}
	 *         returns {@literal true} for the given mode
	 * @see OperationalModesService#isOperationalModeActive(String)
	 * @since 2.1
	 */
	public boolean isOpMode(String mode) {
		if ( opModesService == null ) {
			return false;
		}
		return opModesService.isOperationalModeActive(mode);
	}

}
