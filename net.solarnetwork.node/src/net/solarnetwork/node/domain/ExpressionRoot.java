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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static net.solarnetwork.service.OptionalService.service;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
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
import net.solarnetwork.domain.datum.GeneralLocationSourceMetadata;
import net.solarnetwork.node.dao.LocalStateDao;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.DatumHistorian;
import net.solarnetwork.node.service.DatumService;
import net.solarnetwork.node.service.LocationService;
import net.solarnetwork.node.service.MetadataService;
import net.solarnetwork.node.service.OperationalModesService;
import net.solarnetwork.node.service.TariffScheduleProvider;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.OptionalServiceCollection;
import net.solarnetwork.util.CollectionUtils;

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
 * @version 2.9
 * @since 1.79
 */
public class ExpressionRoot extends DatumSamplesExpressionRoot
		implements DatumMetadataOperations, TariffScheduleProvidersOperations, LocalStateOperations {

	private static final Logger log = LoggerFactory.getLogger(ExpressionRoot.class);

	private final DatumService datumService;
	private final OperationalModesService opModesService;
	private final MetadataService metadataService;
	private final LocationService locationService;
	private OptionalServiceCollection<TariffScheduleProvider> tariffScheduleProviders;
	private OptionalService<LocalStateDao> localStateDao;

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
		this(datum, samples, parameters, datumService, opModesService, metadataService, null);
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
	 * @param locationService
	 *        the location service
	 * @since 2.4
	 */
	public ExpressionRoot(Datum datum, DatumSamplesOperations samples, Map<String, ?> parameters,
			DatumService datumService, OperationalModesService opModesService,
			MetadataService metadataService, LocationService locationService) {
		super(datum, samples, parameters);
		this.datumService = datumService;
		this.opModesService = opModesService;
		this.metadataService = metadataService;
		this.locationService = locationService;
	}

	/**
	 * Create a copy with a given datum value.
	 *
	 * <p>
	 * The samples and parameters values will be set to {@literal null}.
	 * </p>
	 *
	 * @param datum
	 *        the datum
	 * @return a new instance using {@code datum}
	 * @since 2.5
	 */
	public ExpressionRoot copyWith(Datum datum) {
		return copyWith(datum, null, null);
	}

	/**
	 * Create a copy with a given datum, samples, and parameters values.
	 *
	 * @param datum
	 *        the datum
	 * @param samples
	 *        the samples
	 * @param parameters
	 *        the parameters
	 * @return the new instance using {@code datum}, {@code samples}, and
	 *         {@code parameters}
	 * @since 2.5
	 */
	public ExpressionRoot copyWith(Datum datum, DatumSamplesOperations samples,
			Map<String, ?> parameters) {
		ExpressionRoot r = new ExpressionRoot(datum, samples, parameters, datumService, opModesService,
				metadataService, locationService);
		r.setTariffScheduleProviders(tariffScheduleProviders);
		r.setLocalStateDao(localStateDao);
		return r;
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
	 * Test if an unfiltered "latest" datum is available for a given source ID.
	 *
	 * <p>
	 * This can be used to test if {@link #unfilteredLatest(String)} will return
	 * a non-null value.
	 * </p>
	 *
	 * @param sourceId
	 *        the source ID of the datum to look for
	 * @return {@literal true} if {@link #unfilteredLatest(String)} for the
	 *         given {@code sourceId} will return a non-null value
	 * @since 2.6
	 */
	public boolean hasUnfilteredLatest(String sourceId) {
		return hasUnfilteredOffset(sourceId, 0);
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
	 * Get the latest available unfiltered datum for a given source ID, as an
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
	 *         {@link DatumHistorian#latest(java.util.Set, Class)} returns
	 *         {@literal null} for the given {@code sourceId}
	 * @since 2.6
	 */
	public DatumExpressionRoot unfilteredLatest(String sourceId) {
		return unfilteredOffset(sourceId, 0);
	}

	/**
	 * Test if any "latest" datum are available for a given source ID pattern.
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
	 * Test if any "latest" datum is available for a set of source ID patterns.
	 *
	 * <p>
	 * This can be used to test if {@link #latestMatching(Collection<String>)}
	 * will return a non-null value.
	 * </p>
	 *
	 * @param sourceIdPatterns
	 *        a collection of Ant-style source ID patterns of the datum to look
	 *        for
	 * @return {@literal true} if {@link #latestMatching(Collection<String>)}
	 *         for the given {@code sourceIdPattern} will return a non-null
	 *         value
	 * @since 2.9
	 */
	public boolean hasLatestMatching(Collection<String> sourceIdPatterns) {
		return latestMatching(sourceIdPatterns) != null;
	}

	/**
	 * Test if any "latest" unfiltered datum are available for a given source ID
	 * pattern.
	 *
	 * <p>
	 * This can be used to test if {@link #unfilteredLatestMatching(String)}
	 * will return a non-null value.
	 * </p>
	 *
	 * @param sourceIdPattern
	 *        the Ant-style source ID pattern of the datum to look for
	 * @return {@literal true} if {@link #unfilteredLatestMatching(String)} for
	 *         the given {@code sourceIdPattern} will return a non-null value
	 * @since 2.6
	 */
	public boolean hasUnfilteredLatestMatching(String sourceIdPattern) {
		return unfilteredLatestMatching(sourceIdPattern) != null;
	}

	/**
	 * Test if any "latest" unfiltered datum are available for a set of source
	 * ID patterns.
	 *
	 * <p>
	 * This can be used to test if {@link #unfilteredLatestMatching(String)}
	 * will return a non-null value.
	 * </p>
	 *
	 * @param sourceIdPatterns
	 *        the collection of Ant-style source ID patterns of the datum to
	 *        look for
	 * @return {@literal true} if
	 *         {@link #unfilteredLatestMatching(Collection<String>)} for the
	 *         given {@code sourceIdPattern} will return a non-null value
	 * @since 2.9
	 */
	public boolean hasUnfilteredLatestMatching(Collection<String> sourceIdPatterns) {
		return unfilteredLatestMatching(sourceIdPatterns) != null;
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
		return latestMatching(datumService, sourceIdPattern);
	}

	/**
	 * Get the latest available datum matching a set of source ID patterns, as
	 * {@link DatumExpressionRoot} instances.
	 *
	 * <p>
	 * Note a non-null {@link DatumService} instance must have been provided to
	 * the constructor of this instance for this method to work.
	 * </p>
	 *
	 * @param sourceIdPatterns
	 *        the set of Ant-style source ID patterns of the datum to look for
	 * @return the matching datum, never {@literal null}
	 * @since 2.9
	 */
	public Collection<DatumExpressionRoot> latestMatching(Collection<String> sourceIdPatterns) {
		return latestMatching(datumService, sourceIdPatterns);
	}

	/**
	 * Get the latest available unfiltered datum matching a given source ID
	 * pattern, as {@link DatumExpressionRoot} instances.
	 *
	 * <p>
	 * Note a non-null {@link DatumService} instance must have been provided to
	 * the constructor of this instance for this method to work.
	 * </p>
	 *
	 * @param sourceIdPattern
	 *        the Ant-style source ID pattern of the datum to look for
	 * @return the matching datum, never {@literal null}
	 * @since 2.6
	 */
	public Collection<DatumExpressionRoot> unfilteredLatestMatching(String sourceIdPattern) {
		return latestMatching(datumService != null ? datumService.unfiltered() : null, sourceIdPattern);
	}

	/**
	 * Get the latest available unfiltered datum matching a set of source ID
	 * patterns, as {@link DatumExpressionRoot} instances.
	 *
	 * <p>
	 * Note a non-null {@link DatumService} instance must have been provided to
	 * the constructor of this instance for this method to work.
	 * </p>
	 *
	 * @param sourceIdPatterns
	 *        the set of Ant-style source ID pattern of the datum to look for
	 * @return the matching datum, never {@literal null}
	 * @since 2.9
	 */
	public Collection<DatumExpressionRoot> unfilteredLatestMatching(
			Collection<String> sourceIdPatterns) {
		return latestMatching(datumService != null ? datumService.unfiltered() : null, sourceIdPatterns);
	}

	private Collection<DatumExpressionRoot> latestMatching(DatumHistorian history,
			String sourceIdPattern) {
		if ( history == null || sourceIdPattern == null ) {
			return emptyList();
		}
		return latestMatching(history, singleton(sourceIdPattern));
	}

	private Collection<DatumExpressionRoot> latestMatching(DatumHistorian history,
			Collection<String> sourceIdPatterns) {
		if ( history == null || sourceIdPatterns == null ) {
			return emptyList();
		}
		Set<String> pats = (sourceIdPatterns instanceof Set<?> ? (Set<String>) sourceIdPatterns
				: new LinkedHashSet<>(sourceIdPatterns));
		Collection<NodeDatum> found = history.offset(pats, getTimestamp(), 0, NodeDatum.class);
		if ( found == null || found.isEmpty() ) {
			return emptyList();
		}
		List<DatumExpressionRoot> result = new ArrayList<>(found.size());
		for ( Datum d : found ) {
			result.add(copyWith(d));
		}
		return result;
	}

	/**
	 * Test if any "latest" datum are available for a source ID pattern,
	 * excluding the {@link #getSourceId()} source ID.
	 *
	 * <p>
	 * This can be used to test if {@link #latestOthersMatching(String)} will
	 * return a non-null value.
	 * </p>
	 *
	 * @param sourceIdPattern
	 *        the Ant-style source ID pattern of the datum to look for
	 * @return {@literal true} if {@link #latestOthersMatching(String)} for the
	 *         given {@code sourceIdPattern} will return a non-null value
	 */
	public boolean hasLatestOthersMatching(String sourceIdPattern) {
		return latestOthersMatching(sourceIdPattern) != null;
	}

	/**
	 * Test if any "latest" datum are available for a set of source ID patterns,
	 * excluding the {@link #getSourceId()} source ID.
	 *
	 * <p>
	 * This can be used to test if
	 * {@link #latestOthersMatching(Collection<String>)} will return a non-null
	 * value.
	 * </p>
	 *
	 * @param sourceIdPatterns
	 *        the set of Ant-style source ID patterns of the datum to look for
	 * @return {@literal true} if
	 *         {@link #latestOthersMatching(Collection<String>)} for the given
	 *         {@code sourceIdPattern} will return a non-null value
	 * @since 2.9
	 */
	public boolean hasLatestOthersMatching(Collection<String> sourceIdPatterns) {
		return latestOthersMatching(sourceIdPatterns) != null;
	}

	/**
	 * Test if any "latest" unfiltered datum are available for a given source ID
	 * pattern, excluding the {@link #getSourceId()} source ID.
	 *
	 * <p>
	 * This can be used to test if
	 * {@link #unfilteredLatestOthersMatching(String)} will return a non-null
	 * value.
	 * </p>
	 *
	 * @param sourceIdPattern
	 *        the Ant-style source ID pattern of the datum to look for
	 * @return {@literal true} if
	 *         {@link #unfilteredLatestOthersMatching(String)} for the given
	 *         {@code sourceIdPattern} will return a non-null value
	 * @since 2.6
	 */
	public boolean hasUnfilteredLatestOthersMatching(String sourceIdPattern) {
		return unfilteredLatestOthersMatching(sourceIdPattern) != null;
	}

	/**
	 * Test if any "latest" unfiltered datum are available for a set of source
	 * ID patterns, excluding the {@link #getSourceId()} source ID.
	 *
	 * <p>
	 * This can be used to test if
	 * {@link #unfilteredLatestOthersMatching(Collection<String>)} will return a
	 * non-null value.
	 * </p>
	 *
	 * @param sourceIdPatterns
	 *        the set of Ant-style source ID patterns of the datum to look for
	 * @return {@literal true} if
	 *         {@link #unfilteredLatestOthersMatching(Collection<String>)} for
	 *         the given {@code sourceIdPatterns} will return a non-null value
	 * @since 2.9
	 */
	public boolean hasUnfilteredLatestOthersMatching(Collection<String> sourceIdPatterns) {
		return unfilteredLatestOthersMatching(sourceIdPatterns) != null;
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
		return latestOthersMatching(datumService, sourceIdPattern);
	}

	/**
	 * Get the latest available datum matching a set of source ID patterns,
	 * excluding the {@link #getSourceId()} source ID, as
	 * {@link DatumExpressionRoot} instances.
	 *
	 * <p>
	 * Note a non-null {@link DatumService} instance must have been provided to
	 * the constructor of this instance for this method to work.
	 * </p>
	 *
	 * @param sourceIdPatterns
	 *        the set of Ant-style source ID patterns of the datum to look for
	 * @return the matching datum, never {@literal null}
	 * @since 2.9
	 */
	public Collection<DatumExpressionRoot> latestOthersMatching(Collection<String> sourceIdPatterns) {
		return latestOthersMatching(datumService, sourceIdPatterns);
	}

	/**
	 * Get the latest available unfiltered datum matching a given source ID
	 * pattern, excluding the {@link #getSourceId()} source ID, as
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
	 * @since 2.6
	 */
	public Collection<DatumExpressionRoot> unfilteredLatestOthersMatching(String sourceIdPattern) {
		return latestOthersMatching(datumService != null ? datumService.unfiltered() : null,
				sourceIdPattern);
	}

	/**
	 * Get the latest available unfiltered datum matching a set of source ID
	 * patterns, excluding the {@link #getSourceId()} source ID, as
	 * {@link DatumExpressionRoot} instances.
	 *
	 * <p>
	 * Note a non-null {@link DatumService} instance must have been provided to
	 * the constructor of this instance for this method to work.
	 * </p>
	 *
	 * @param sourceIdPatterns
	 *        the set of Ant-style source ID patterns of the datum to look for
	 * @return the matching datum, never {@literal null}
	 * @since 2.9
	 */
	public Collection<DatumExpressionRoot> unfilteredLatestOthersMatching(
			Collection<String> sourceIdPatterns) {
		return latestOthersMatching(datumService != null ? datumService.unfiltered() : null,
				sourceIdPatterns);
	}

	private Collection<DatumExpressionRoot> latestOthersMatching(DatumHistorian history,
			String sourceIdPattern) {
		if ( history == null || sourceIdPattern == null ) {
			return emptyList();
		}
		return latestOthersMatching(history, singleton(sourceIdPattern));
	}

	private Collection<DatumExpressionRoot> latestOthersMatching(DatumHistorian history,
			Collection<String> sourceIdPatterns) {
		if ( history == null || sourceIdPatterns == null ) {
			return emptyList();
		}
		Set<String> pats = (sourceIdPatterns instanceof Set<?> ? (Set<String>) sourceIdPatterns
				: new LinkedHashSet<>(sourceIdPatterns));
		Collection<NodeDatum> found = history.offset(pats, getTimestamp(), 0, NodeDatum.class);
		if ( found == null || found.isEmpty() ) {
			return emptyList();
		}
		final String sourceId = getSourceId();
		List<DatumExpressionRoot> result = new ArrayList<>(found.size());
		for ( Datum d : found ) {
			if ( sourceId != null && sourceId.equals(d.getSourceId()) ) {
				continue;
			}
			result.add(copyWith(d));
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
		return selfAndLatestMatching(datumService, sourceIdPattern);
	}

	/**
	 * Get the latest available datum matching a set of source ID patterns,
	 * including this instance, as {@link DatumExpressionRoot} instances.
	 *
	 * <p>
	 * Note a non-null {@link DatumService} instance must have been provided to
	 * the constructor of this instance for this method to work.
	 * </p>
	 *
	 * @param sourceIdPatterns
	 *        the set of Ant-style source ID patterns of the datum to look for
	 * @return the matching datum, never {@literal null} and always having at
	 *         least one value (this instance)
	 * @since 2.9
	 */
	public Collection<DatumExpressionRoot> selfAndLatestMatching(Collection<String> sourceIdPatterns) {
		return selfAndLatestMatching(datumService, sourceIdPatterns);
	}

	/**
	 * Get the latest available unfiltered datum matching a given source ID
	 * pattern, including this instance, as {@link DatumExpressionRoot}
	 * instances.
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
	 * @since 2.6
	 */
	public Collection<DatumExpressionRoot> selfAndUnfilteredLatestMatching(String sourceIdPattern) {
		return selfAndLatestMatching(datumService != null ? datumService.unfiltered() : null,
				sourceIdPattern);
	}

	/**
	 * Get the latest available unfiltered datum matching a set of source ID
	 * patterns, including this instance, as {@link DatumExpressionRoot}
	 * instances.
	 *
	 * <p>
	 * Note a non-null {@link DatumService} instance must have been provided to
	 * the constructor of this instance for this method to work.
	 * </p>
	 *
	 * @param sourceIdPatterns
	 *        the set of Ant-style source ID patterns of the datum to look for
	 * @return the matching datum, never {@literal null} and always having at
	 *         least one value (this instance)
	 * @since 2.9
	 */
	public Collection<DatumExpressionRoot> selfAndUnfilteredLatestMatching(
			Collection<String> sourceIdPatterns) {
		return selfAndLatestMatching(datumService != null ? datumService.unfiltered() : null,
				sourceIdPatterns);
	}

	private Collection<DatumExpressionRoot> selfAndLatestMatching(DatumHistorian history,
			String sourceIdPattern) {
		if ( history == null || sourceIdPattern == null ) {
			return emptyList();
		}
		return selfAndLatestMatching(history, singleton(sourceIdPattern));
	}

	private Collection<DatumExpressionRoot> selfAndLatestMatching(DatumHistorian history,
			Collection<String> sourceIdPatterns) {
		if ( history == null || sourceIdPatterns == null ) {
			return emptyList();
		}
		Set<String> pats = (sourceIdPatterns instanceof Set<?> ? (Set<String>) sourceIdPatterns
				: new LinkedHashSet<>(sourceIdPatterns));
		Collection<NodeDatum> found = history.offset(pats, getTimestamp(), 0, NodeDatum.class);
		if ( found == null || found.isEmpty() ) {
			return singleton(this);
		}
		final String sourceId = getSourceId();
		List<DatumExpressionRoot> result = new ArrayList<>(found.size());
		result.add(this);
		for ( Datum d : found ) {
			if ( sourceId != null && sourceId.equals(d.getSourceId()) ) {
				continue;
			} else {
				result.add(copyWith(d));
			}
		}
		return result;
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
	 * Test if an offset from a "latest" unfiltered datum is available for the
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
	 * @since 2.6
	 * @see #hasUnfilteredOffset(String, Instant, int)
	 */
	public boolean hasUnfilteredOffset(int offset) {
		if ( offset == 0 ) {
			return true;
		}
		Datum datum = getDatum();
		if ( datum == null ) {
			return false;
		}
		return hasUnfilteredOffset(datum.getSourceId(), datum.getTimestamp(), offset);
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
	 * @since 2.6
	 * @see #unfilteredOffset(String, Instant, int)
	 */
	public DatumExpressionRoot unfilteredOffset(int offset) {
		Datum datum = getDatum();
		if ( datum == null ) {
			return null;
		}
		if ( offset == 0 ) {
			return this;
		}
		return unfilteredOffset(datum.getSourceId(), datum.getTimestamp(), offset);
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
		return hasOffset(datumService, sourceId, offset);
	}

	/**
	 * Test if an offset from an unfiltered "latest" datum is available for a
	 * given source ID.
	 *
	 * <p>
	 * This can be used to test if {@link #unfilteredOffset(String,int)} will
	 * return a non-null value.
	 * </p>
	 *
	 * @param sourceId
	 *        the source ID of the datum to look for
	 * @param offset
	 *        the offset from the latest, {@literal 0} being the latest and
	 *        {@literal 1} the next later, and so on
	 * @return {@literal true} if {@link #offset(String, int)} for the given
	 *         {@code sourceId} will return a non-null value
	 * @since 2.6
	 */
	public boolean hasUnfilteredOffset(String sourceId, int offset) {
		return hasOffset(datumService != null ? datumService.unfiltered() : null, sourceId, offset);
	}

	private boolean hasOffset(DatumHistorian history, String sourceId, int offset) {
		if ( history == null || sourceId == null ) {
			return false;
		}
		NodeDatum d = history.offset(sourceId, offset, NodeDatum.class);
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
		return offset(datumService, sourceId, offset);
	}

	/**
	 * Get an offset from latest available unfiltered datum for a given source
	 * ID, as an {@link DatumExpressionRoot}.
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
	 *         {@link DatumHistorian#offset(java.util.Set, int, Class)} returns
	 *         {@literal null} for the given {@code sourceId}
	 * @since 2.6
	 */
	public DatumExpressionRoot unfilteredOffset(String sourceId, int offset) {
		return offset(datumService != null ? datumService.unfiltered() : null, sourceId, offset);
	}

	private DatumExpressionRoot offset(DatumHistorian history, String sourceId, int offset) {
		if ( history == null || sourceId == null ) {
			return null;
		}
		NodeDatum d = history.offset(sourceId, offset, NodeDatum.class);
		if ( d == null ) {
			return null;
		}
		return copyWith(d);
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
		return hasOffset(datumService, sourceId, timestamp, offset);
	}

	/**
	 * Test if an unfiltered datum offset from a given timestamp is available
	 * for a given source ID.
	 *
	 * <p>
	 * This can be used to test if {@link #unfilteredOffset(String,Instant,int)}
	 * will return a non-null value.
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
	public boolean hasUnfilteredOffset(String sourceId, Instant timestamp, int offset) {
		return hasOffset(datumService != null ? datumService.unfiltered() : null, sourceId, timestamp,
				offset);
	}

	private boolean hasOffset(DatumHistorian history, String sourceId, Instant timestamp, int offset) {
		if ( history == null || sourceId == null || timestamp == null ) {
			return false;
		}
		NodeDatum d = history.offset(sourceId, timestamp, offset, NodeDatum.class);
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
		return offset(datumService, sourceId, timestamp, offset);
	}

	/**
	 * Get an unfiltered datum offset from a given timestamp for a given source
	 * ID, as an {@link DatumExpressionRoot}.
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
	 *         {@link DatumHistorian#offset(java.util.Set, int, Class)} returns
	 *         {@literal null} for the given {@code sourceId}
	 * @since 2.6
	 */
	public DatumExpressionRoot unfilteredOffset(String sourceId, Instant timestamp, int offset) {
		return offset(datumService != null ? datumService.unfiltered() : null, sourceId, timestamp,
				offset);
	}

	private DatumExpressionRoot offset(DatumHistorian history, String sourceId, Instant timestamp,
			int offset) {
		if ( history == null || sourceId == null || timestamp == null ) {
			return null;
		}
		NodeDatum d = history.offset(sourceId, timestamp, offset, NodeDatum.class);
		if ( d == null ) {
			return null;
		}
		return copyWith(d);
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
		return slice(datumService, sourceId, offset, count);
	}

	/**
	 * Get a set of available unfiltered datum for a given source ID, as
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
	 * @since 2.6
	 */
	public Collection<DatumExpressionRoot> unfilteredSlice(String sourceId, int offset, int count) {
		return slice(datumService != null ? datumService.unfiltered() : null, sourceId, offset, count);
	}

	private Collection<DatumExpressionRoot> slice(DatumHistorian history, String sourceId, int offset,
			int count) {
		if ( history == null || sourceId == null ) {
			return emptyList();
		}
		Collection<NodeDatum> found = history.slice(sourceId, offset, count, null);
		if ( found == null || found.isEmpty() ) {
			return singleton(this);
		}
		List<DatumExpressionRoot> result = new ArrayList<>(found.size());
		result.add(this);
		for ( NodeDatum d : found ) {
			result.add(copyWith(d));
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
		return slice(datumService, sourceId, timestamp, offset, count);
	}

	/**
	 * Get a set of available unfiltered datum offset from a given timestamp,
	 * for a given source ID, as {@link DatumExpressionRoot} instances.
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
	 * @since 2.6
	 */
	public Collection<DatumExpressionRoot> unfilteredSlice(String sourceId, Instant timestamp,
			int offset, int count) {
		return slice(datumService != null ? datumService.unfiltered() : null, sourceId, timestamp,
				offset, count);
	}

	private Collection<DatumExpressionRoot> slice(DatumHistorian history, String sourceId,
			Instant timestamp, int offset, int count) {
		if ( history == null || sourceId == null ) {
			return emptyList();
		}
		Collection<NodeDatum> found = history.slice(sourceId, offset, count, null);
		if ( found == null || found.isEmpty() ) {
			return singleton(this);
		}
		List<DatumExpressionRoot> result = new ArrayList<>(found.size());
		result.add(this);
		for ( NodeDatum d : found ) {
			result.add(copyWith(d));
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
		final Long locationId = getLocId();
		if ( locationId == null ) {
			return meta(getSourceId());
		}
		final GeneralLocationSourceMetadata m = locationSourceMetadata(locationId, getSourceId());
		if ( m == null ) {
			return null;
		}
		return m.getMeta();
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
				: emptyList());
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
		return (delegate != null ? delegate.getInfo() : emptyMap());
	}

	@Override
	public Set<String> getPropertyInfoKeys() {
		final DatumMetadataOperations delegate = metadata();
		return (delegate != null ? delegate.getPropertyInfoKeys() : emptySet());
	}

	@Override
	public Map<String, ?> getPropertyInfo(String key) {
		final DatumMetadataOperations delegate = metadata();
		return (delegate != null ? delegate.getPropertyInfo(key) : emptyMap());
	}

	@Override
	public Set<String> getTags() {
		final DatumMetadataOperations delegate = metadata();
		return (delegate != null ? delegate.getTags() : emptySet());
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

	private GeneralLocationSourceMetadata locationSourceMetadata(final Long locationId,
			final String sourceId) {
		if ( locationId == null || sourceId == null || sourceId.isEmpty() || locationService == null ) {
			return null;
		}
		return locationService.getLocationMetadata(locationId, sourceId);
	}

	/**
	 * Get a location datum stream's metadata.
	 *
	 * @param locationId
	 *        the ID of the location datum stream
	 * @param sourceId
	 *        the source ID of the location datum stream
	 * @return the location metadata, or {@literal null} if not available
	 * @since 2.4
	 */
	public DatumMetadataOperations locMeta(Long locationId, String sourceId) {
		GeneralLocationSourceMetadata m = locationSourceMetadata(locationId, sourceId);
		return (m != null ? m.getMeta() : null);
	}

	/**
	 * Sort a collection.
	 *
	 * <p>
	 * If the collection fails to sort in any way, the {@code collection} value
	 * will be returned as-is.
	 * </p>
	 *
	 * @param <T>
	 *        the collection type
	 * @param collection
	 *        the collection
	 * @param propNames
	 *        an optional list of element property names to sort by; if not
	 *        provided then the elements themselves will be compared
	 * @return the sorted list
	 * @since 2.5
	 */
	public <T> Collection<T> sort(Collection<T> collection, String... propNames) {
		return CollectionUtils.sort(collection, propNames);
	}

	/**
	 * Sort a collection.
	 *
	 * <p>
	 * If the collection fails to sort in any way, the {@code collection} value
	 * will be returned as-is.
	 * </p>
	 *
	 * @param <T>
	 *        the collection type
	 * @param collection
	 *        the collection
	 * @param reverse
	 *        {@literal true} to sort in reverse ordering
	 * @param propNames
	 *        an optional list of element property names to sort by; if not
	 *        provided then the elements themselves will be compared
	 * @return the sorted list
	 * @since 2.5
	 */
	public <T> Collection<T> sort(Collection<T> collection, boolean reverse, String... propNames) {
		return CollectionUtils.sort(collection, reverse, propNames);
	}

	private LocalStateDao localStateDao() {
		final LocalStateDao dao = service(getLocalStateDao());
		if ( dao == null ) {
			throw new UnsupportedOperationException("LocalStateDao not available");
		}
		return dao;
	}

	@Override
	public Object localState(String key, Object defaultValue) {
		LocalState state = localStateDao().get(key);
		return (state != null ? state.getValue() : defaultValue);
	}

	@Override
	public Object saveLocalState(String key, LocalStateType type, Object value) {
		LocalState state = new LocalState(key, type, value);
		localStateDao().compareAndChange(state); // not save to avoid excessive STORED entity events
		return value;
	}

	@Override
	public Object saveLocalState(String key, LocalStateType type, Object value, Object expectedValue) {
		LocalState state = new LocalState(key, type, value);
		LocalState result = localStateDao().compareAndSave(state, expectedValue);
		return result.getValue();
	}

	@Override
	public Object getAndSaveLocalState(String key, LocalStateType type, Object value) {
		LocalState state = new LocalState(key, type, value);
		LocalState result = localStateDao().getAndSave(state);
		return result != null ? result.getValue() : null;
	}

	/**
	 * Get the tariff schedule providers.
	 *
	 * @return the providers
	 * @since 2.5
	 */
	@Override
	public final OptionalServiceCollection<TariffScheduleProvider> getTariffScheduleProviders() {
		return tariffScheduleProviders;
	}

	/**
	 * Set the tariff schedule providers.
	 *
	 * @param tariffScheduleProviders
	 *        the providers to set
	 * @since 2.5
	 */
	public final void setTariffScheduleProviders(
			OptionalServiceCollection<TariffScheduleProvider> tariffScheduleProviders) {
		this.tariffScheduleProviders = tariffScheduleProviders;
	}

	/**
	 * Get the optional local state DAO.
	 *
	 * @return the DAO
	 * @since 2.8
	 */
	public OptionalService<LocalStateDao> getLocalStateDao() {
		return localStateDao;
	}

	/**
	 * Set the optional local state DAO.
	 *
	 * @param localStateDao
	 *        the DAO to set
	 * @since 2.8
	 */
	public void setLocalStateDao(OptionalService<LocalStateDao> localStateDao) {
		this.localStateDao = localStateDao;
	}

}
