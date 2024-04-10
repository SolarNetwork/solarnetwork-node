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

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumExpressionRoot;
import net.solarnetwork.domain.datum.DatumMetadataOperations;
import net.solarnetwork.domain.datum.DatumSamplesExpressionRoot;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.DatumService;
import net.solarnetwork.node.service.MetadataService;
import net.solarnetwork.node.service.OperationalModesService;

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
 * <p>
 * This class also implements the {@link DatumMetadataOperations} API,
 * delegating to the metadata provided by the configured
 * {@link MetadataService#getAllMetadata()}.
 * </p>
 *
 * @author matt
 * @version 2.3
 * @since 1.79
 */
public class ExpressionRoot extends DatumSamplesExpressionRoot implements DatumMetadataOperations {

	private static final Logger log = LoggerFactory.getLogger(ExpressionRoot.class);

	private final DatumService datumService;
	private final OperationalModesService opModesService;
	private final MetadataService metadataService;

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
		this(datum, samples, parameters, datumService, opModesService, null);
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
	 * @param metadataService
	 *        the metadata service
	 * @since 2.3
	 */
	public ExpressionRoot(Datum datum, DatumSamplesOperations samples, Map<String, ?> parameters,
			DatumService datumService, OperationalModesService opModesService,
			MetadataService metadataService) {
		super(datum, samples, parameters);
		this.datumService = datumService;
		this.opModesService = opModesService;
		this.metadataService = metadataService;
	}

	@Override
	public String toString() {
		String data = super.toString();
		if ( log.isTraceEnabled() ) {
			if ( datumService != null ) {
				try {
					Collection<NodeDatum> latestDatum = datumService.latest(emptySet(), NodeDatum.class);
					if ( latestDatum != null && !latestDatum.isEmpty() ) {
						for ( NodeDatum d : latestDatum ) {
							data += "\nDatum [" + d.getSourceId() + "]: " + d;
						}
						for ( NodeDatum d : latestDatum ) {
							try {
								DatumMetadataOperations meta = datumService
										.datumMetadata(d.getSourceId());
								if ( meta != null && !meta.isEmpty() ) {
									data += "\nMeta [" + d.getSourceId() + "]: " + meta;
								}
							} catch ( Exception e2 ) {
								// ignore this
							}
						}
					}
				} catch ( Exception e ) {
					// ignore this
				}
			}
			if ( opModesService != null ) {
				try {
					Set<String> activeModes = opModesService.activeOperationalModes();
					if ( activeModes != null && !activeModes.isEmpty() ) {
						data += "\nActive op modes: " + activeModes;
					}
				} catch ( Exception e ) {
					// ignore this
				}
			}
		}
		return data;
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
	 * Test if a "latest" datum is available for a given source ID.
	 *
	 * <p>
	 * This can be used to test if {@link #latestMatching(String)} will return a
	 * non-null value.
	 * </p>
	 *
	 * @param sourceIdPattern
	 *        the Ant-style source ID pattern of the datum to look for
	 * @return {@literal true} if {@link #latestMatching(String)} for the given
	 *         {@code sourceIdPattern} will return a non-null value
	 */
	public boolean hasLatestMatching(String sourceIdPattern) {
		return latestMatching(sourceIdPattern) != null;
	}

	/**
	 * Get the latest available datum matching a given source ID pattern, as
	 * {@link DatumExpressionRoot} instances.
	 *
	 * <p>
	 * Note a non-null {@link DatumService} instance must have been provided to
	 * the constructor of this instance for this method to work.
	 * </p>
	 *
	 * @param sourceIdPattern
	 *        the Ant-style source ID pattern of the datum to look for
	 * @return the matching datum, never {@literal null}
	 */
	public Collection<DatumExpressionRoot> latestMatching(String sourceIdPattern) {
		if ( datumService == null || sourceIdPattern == null ) {
			return Collections.emptyList();
		}
		Set<String> pats = Collections.singleton(sourceIdPattern);
		Collection<NodeDatum> found = datumService.offset(pats, getTimestamp(), 0, NodeDatum.class);
		if ( found == null || found.isEmpty() ) {
			return Collections.emptyList();
		}
		List<DatumExpressionRoot> result = new ArrayList<>(found.size());
		for ( Datum d : found ) {
			result.add(new ExpressionRoot(d, null, null, datumService, opModesService, metadataService));
		}
		return result;
	}

	/**
	 * Test if a "latest" datum is available for a given source ID, excluding
	 * the {@link #getSourceId()} source ID.
	 *
	 * <p>
	 * This can be used to test if {@link #latestOthersMatching(String)} will
	 * return a non-null value.
	 * </p>
	 *
	 * @param sourceIdPattern
	 *        the Ant-style source ID pattern of the datum to look for
	 * @return {@literal true} if {@link #latestMatching(String)} for the given
	 *         {@code sourceIdPattern} will return a non-null value
	 */
	public boolean hasLatestOthersMatching(String sourceIdPattern) {
		return latestOthersMatching(sourceIdPattern) != null;
	}

	/**
	 * Get the latest available datum matching a given source ID pattern,
	 * excluding the {@link #getSourceId()} source ID, as
	 * {@link DatumExpressionRoot} instances.
	 *
	 * <p>
	 * Note a non-null {@link DatumService} instance must have been provided to
	 * the constructor of this instance for this method to work.
	 * </p>
	 *
	 * @param sourceIdPattern
	 *        the Ant-style source ID pattern of the datum to look for
	 * @return the matching datum, never {@literal null}
	 */
	public Collection<DatumExpressionRoot> latestOthersMatching(String sourceIdPattern) {
		if ( datumService == null || sourceIdPattern == null ) {
			return Collections.emptyList();
		}
		Set<String> pats = Collections.singleton(sourceIdPattern);
		Collection<NodeDatum> found = datumService.offset(pats, getTimestamp(), 0, NodeDatum.class);
		if ( found == null || found.isEmpty() ) {
			return Collections.emptyList();
		}
		final String sourceId = getSourceId();
		List<DatumExpressionRoot> result = new ArrayList<>(found.size());
		for ( Datum d : found ) {
			if ( sourceId != null && sourceId.equals(d.getSourceId()) ) {
				continue;
			}
			result.add(new ExpressionRoot(d, null, null, datumService, opModesService, metadataService));
		}
		return result;
	}

	/**
	 * Get the latest available datum matching a given source ID pattern,
	 * including this instance, as {@link DatumExpressionRoot} instances.
	 *
	 * <p>
	 * Note a non-null {@link DatumService} instance must have been provided to
	 * the constructor of this instance for this method to work.
	 * </p>
	 *
	 * @param sourceIdPattern
	 *        the Ant-style source ID pattern of the datum to look for
	 * @return the matching datum, never {@literal null} and always having at
	 *         least one value (this instance)
	 */
	public Collection<DatumExpressionRoot> selfAndLatestMatching(String sourceIdPattern) {
		if ( datumService == null || sourceIdPattern == null ) {
			return Collections.emptyList();
		}
		Set<String> pats = Collections.singleton(sourceIdPattern);
		Collection<NodeDatum> found = datumService.offset(pats, getTimestamp(), 0, NodeDatum.class);
		if ( found == null || found.isEmpty() ) {
			return Collections.singleton(this);
		}
		final String sourceId = getSourceId();
		List<DatumExpressionRoot> result = new ArrayList<>(found.size());
		result.add(this);
		for ( Datum d : found ) {
			if ( sourceId != null && sourceId.equals(d.getSourceId()) ) {
				continue;
			} else {
				result.add(new ExpressionRoot(d, null, null, datumService, opModesService,
						metadataService));
			}
		}
		return result;
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
		return new ExpressionRoot(d, null, null, datumService, opModesService, metadataService);
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
	 *        the timestamp reference point
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
		return new ExpressionRoot(d, null, null, datumService, opModesService, metadataService);
	}

	/**
	 * Get a set of available datum for a given source ID, as
	 * {@link DatumExpressionRoot} instances.
	 *
	 * <p>
	 * Note a non-null {@link DatumService} instance must have been provided to
	 * the constructor of this instance for this method to work.
	 * </p>
	 *
	 * @param sourceId
	 *        the source ID to extract a slice from
	 * @param offset
	 *        the offset from {@code timestamp}, {@literal 0} being the latest
	 *        and {@literal 1} the next later, and so on
	 * @param count
	 *        the maximum number of datum to return, starting from
	 *        {@code offset} and iterating over earlier datum
	 * @return the matching datum, never {@literal null}
	 * @since 2.2
	 */
	public Collection<DatumExpressionRoot> slice(String sourceId, int offset, int count) {
		if ( datumService == null || sourceId == null ) {
			return Collections.emptyList();
		}
		Collection<NodeDatum> found = datumService.slice(sourceId, offset, count, null);
		if ( found == null || found.isEmpty() ) {
			return Collections.singleton(this);
		}
		List<DatumExpressionRoot> result = new ArrayList<>(found.size());
		result.add(this);
		for ( NodeDatum d : found ) {
			result.add(new ExpressionRoot(d, null, null, datumService, opModesService, metadataService));
		}
		return result;
	}

	/**
	 * Get a set of available datum offset from a given timestamp, for a given
	 * source ID, as {@link DatumExpressionRoot} instances.
	 *
	 * <p>
	 * Note a non-null {@link DatumService} instance must have been provided to
	 * the constructor of this instance for this method to work.
	 * </p>
	 *
	 * @param sourceId
	 *        the source ID to extract a slice from
	 * @param timestamp
	 *        the timestamp to reference
	 * @param offset
	 *        the offset from {@code timestamp}, {@literal 0} being the latest
	 *        and {@literal 1} the next later, and so on
	 * @param count
	 *        the maximum number of datum to return, starting from
	 *        {@code offset} and iterating over earlier datum
	 * @return the matching datum, never {@literal null}
	 * @since 2.2
	 */
	public Collection<DatumExpressionRoot> slice(String sourceId, Instant timestamp, int offset,
			int count) {
		if ( datumService == null || sourceId == null ) {
			return Collections.emptyList();
		}
		Collection<NodeDatum> found = datumService.slice(sourceId, offset, count, null);
		if ( found == null || found.isEmpty() ) {
			return Collections.singleton(this);
		}
		List<DatumExpressionRoot> result = new ArrayList<>(found.size());
		result.add(this);
		for ( NodeDatum d : found ) {
			result.add(new ExpressionRoot(d, null, null, datumService, opModesService, metadataService));
		}
		return result;
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

	/**
	 * Test if metadata for the {@link #getSourceId()} datum stream is
	 * available.
	 *
	 * @return {@literal true} if metadata for {@link #getSourceId()} is
	 *         available
	 * @since 2.1
	 */
	public boolean hasMeta() {
		return hasMeta(getSourceId());
	}

	/**
	 * Get the metadata for the {@link #getSourceId()} datum stream.
	 *
	 * @return the metadata, or {@literal null} if no such metadata is available
	 * @since 2.1
	 */
	public DatumMetadataOperations getMeta() {
		return meta(getSourceId());
	}

	/**
	 * Test if metadata for a given datum stream is available.
	 *
	 * @param sourceId
	 *        the source ID of the datum metadata to get
	 * @return {@literal true} if metadata for {@code sourceId} is available
	 * @since 2.1
	 */
	public boolean hasMeta(String sourceId) {
		return meta(sourceId) != null;
	}

	/**
	 * Get the metadata for a given datum stream.
	 *
	 * @param sourceId
	 *        the source ID of the datum metadata to get
	 * @return the metadata, or {@literal null} if no such metadata is available
	 * @since 2.1
	 */
	public DatumMetadataOperations meta(String sourceId) {
		return (datumService != null && sourceId != null ? datumService.datumMetadata(sourceId) : null);
	}

	/**
	 * Get the metadata for a set of datum streams matching a filter.
	 *
	 * @param sourceIdFilter
	 *        an optional Ant-style source ID pattern to filter by; use
	 *        {@literal null} to return metadata for all available sources
	 * @return the matching metadata, never {@literal null}
	 * @since 2.1
	 */
	public Collection<DatumMetadataOperations> metaMatching(String sourceIdFilter) {
		return (datumService != null ? datumService.datumMetadata(singleton(sourceIdFilter))
				: Collections.emptyList());
	}

	/**
	 * Get the general metadata.
	 *
	 * @return the general metadata, or {@literal null} if none available
	 * @since 2.3
	 */
	public DatumMetadataOperations metadata() {
		return (metadataService != null ? metadataService.getAllMetadata() : null);
	}

	@Override
	public Map<String, ?> getInfo() {
		final DatumMetadataOperations delegate = metadata();
		return (delegate != null ? delegate.getInfo() : Collections.emptyMap());
	}

	@Override
	public Set<String> getPropertyInfoKeys() {
		final DatumMetadataOperations delegate = metadata();
		return (delegate != null ? delegate.getPropertyInfoKeys() : Collections.emptySet());
	}

	@Override
	public Map<String, ?> getPropertyInfo(String key) {
		final DatumMetadataOperations delegate = metadata();
		return (delegate != null ? delegate.getPropertyInfo(key) : Collections.emptyMap());
	}

	@Override
	public Set<String> getTags() {
		final DatumMetadataOperations delegate = metadata();
		return (delegate != null ? delegate.getTags() : Collections.emptySet());
	}

	@Override
	public Object metadataAtPath(String path) {
		final DatumMetadataOperations delegate = metadata();
		return (delegate != null ? delegate.metadataAtPath(path) : null);
	}

	@Override
	public <T> T metadataAtPath(String path, Class<T> clazz) {
		final DatumMetadataOperations delegate = metadata();
		return (delegate != null ? delegate.metadataAtPath(path, clazz) : null);
	}

}
