/* ==================================================================
 * UnchangedDatumFilterService.java - 28/03/2023 6:50:32 am
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

package net.solarnetwork.node.datum.filter.std;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.service.support.BaseDatumFilterSupport;
import net.solarnetwork.service.DatumFilterService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Datum filter service that can discard unchanged datum, within a maximum time
 * range.
 * 
 * @author matt
 * @version 1.1
 * @since 3.1
 */
public class UnchangedDatumFilterService extends BaseDatumFilterSupport
		implements DatumFilterService, SettingSpecifierProvider {

	/** The {@code unchangedPublishMaxSeconds} property default value. */
	public static final int DEFAULT_UNCHANGED_PUBLISH_MAX_SECONDS = 3599;

	private final ConcurrentMap<String, SeenSample> seenSamples = new ConcurrentHashMap<>(8, 0.9f, 2);

	private int unchangedPublishMaxSeconds = DEFAULT_UNCHANGED_PUBLISH_MAX_SECONDS;
	private Pattern propertyIncludePattern;

	/**
	 * Constructor.
	 */
	public UnchangedDatumFilterService() {
		super();
	}

	private final class SeenSample {

		private final Instant timestamp;
		private final DatumSamplesOperations sample;

		private SeenSample(Instant timestamp, DatumSamplesOperations sample) {
			super();
			this.timestamp = (timestamp != null ? timestamp : Instant.now());
			this.sample = new DatumSamples(sample);
		}

		/**
		 * Test if a datum samples should be discarded because it is unchanged
		 * within the configured maximum seconds threshold.
		 * 
		 * @param datum
		 *        the datum
		 * @param samples
		 *        the samples
		 * @return {@literal true} if the samples should be discarded
		 */
		private boolean shouldDiscard(Datum datum, DatumSamplesOperations samples) {
			if ( datum == null || samples == null ) {
				return false;
			}
			final Instant datumTimestamp = (datum.getTimestamp() != null ? datum.getTimestamp()
					: Instant.now());
			return ((unchangedPublishMaxSeconds < 1
					|| timestamp.plusSeconds(unchangedPublishMaxSeconds).isAfter(datumTimestamp))
					&& sampleEquals(samples));
		}

		private boolean sampleEquals(DatumSamplesOperations samples) {
			final Pattern p = getPropertyIncludePattern();
			if ( p == null ) {
				return !samples.differsFrom(sample);
			}
			final Set<String> seen = new HashSet<>(8);
			boolean equal = propertiesEqual(p, seen,
					samples.getSampleData(DatumSamplesType.Instantaneous),
					sample.getSampleData(DatumSamplesType.Instantaneous));
			if ( !equal ) {
				return false;
			}
			equal = propertiesEqual(p, seen, samples.getSampleData(DatumSamplesType.Accumulating),
					sample.getSampleData(DatumSamplesType.Accumulating));
			if ( !equal ) {
				return false;
			}
			return propertiesEqual(p, seen, samples.getSampleData(DatumSamplesType.Status),
					sample.getSampleData(DatumSamplesType.Status));
		}

		private boolean propertiesEqual(final Pattern p, Set<String> seen, Map<String, ?> l,
				Map<String, ?> r) {
			if ( l != null ) {
				for ( Entry<String, ?> e : l.entrySet() ) {
					String k = e.getKey();
					if ( k == null ) {
						continue;
					}
					if ( seen.contains(k) ) {
						continue;
					}
					if ( p.matcher(k).find() ) {
						Object vl = e.getValue();
						if ( r == null ) {
							if ( vl != null ) {
								return false;
							}
							continue;
						}
						Object vr = r.get(k);
						if ( !Objects.equals(vl, vr) ) {
							return false;
						}
					}
					seen.add(k);
				}
			}
			if ( r != null ) {
				for ( Entry<String, ?> e : r.entrySet() ) {
					String k = e.getKey();
					if ( k == null ) {
						continue;
					}
					if ( seen.contains(k) ) {
						continue;
					}
					if ( p.matcher(k).find() ) {
						Object vr = e.getValue();
						if ( l == null ) {
							if ( vr != null ) {
								return false;
							}
							continue;
						}
						Object vl = l.get(k);
						if ( !Objects.equals(vl, vr) ) {
							return false;
						}
					}
					seen.add(k);
				}
			}
			return true;
		}
	}

	@Override
	public DatumSamplesOperations filter(Datum datum, DatumSamplesOperations samples,
			Map<String, Object> params) {
		final long start = incrementInputStats();
		if ( !conditionsMatch(datum, samples, params) ) {
			incrementIgnoredStats(start);
			return samples;
		}

		final String sourceId = datum.getSourceId();
		final SeenSample seen = seenSamples.get(sourceId);
		if ( seen == null ) {
			seenSamples.putIfAbsent(sourceId, new SeenSample(datum.getTimestamp(), samples));
		} else if ( seen.shouldDiscard(datum, samples) ) {
			log.trace("Unchanged filter [{}] discarding source [{}] @ {} as not changed in the past {}s",
					getUid(), sourceId, datum.getTimestamp(), unchangedPublishMaxSeconds);
			samples = null;
		} else {
			// not discarding and seen not null: replace seen with current copy
			seenSamples.replace(sourceId, seen, new SeenSample(datum.getTimestamp(), samples));
		}

		incrementStats(start, samples, samples);
		return samples;
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.filter.std.unchanged";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> result = baseIdentifiableSettings("");
		populateBaseSampleTransformSupportSettings(result);
		populateStatusSettings(result);

		result.add(new BasicTextFieldSettingSpecifier("unchangedPublishMaxSeconds",
				String.valueOf(DEFAULT_UNCHANGED_PUBLISH_MAX_SECONDS)));
		result.add(new BasicTextFieldSettingSpecifier("propertyIncludePatternValue", null));

		return result;
	}

	/**
	 * Get the unchanged publish maximum seconds.
	 * 
	 * @return the maximum seconds to refrain from publishing an unchanged
	 *         status value, or {@literal 0} for no limit
	 */
	public int getUnchangedPublishMaxSeconds() {
		return unchangedPublishMaxSeconds;
	}

	/**
	 * Set the unchanged publish maximum seconds.
	 * 
	 * @param unchangedPublishMaxSeconds
	 *        the maximum seconds to refrain from publishing an unchanged status
	 *        value, or {@literal 0} for no limit
	 */
	public void setUnchangedPublishMaxSeconds(int unchangedPublishMaxSeconds) {
		this.unchangedPublishMaxSeconds = unchangedPublishMaxSeconds;
	}

	/**
	 * Get the property include pattern.
	 * 
	 * @return a regular expression to restrict the examined properties
	 * @since 1.1
	 */
	public Pattern getPropertyIncludePattern() {
		return propertyIncludePattern;
	}

	/**
	 * Set the property include pattern.
	 * 
	 * <p>
	 * This expression restricts the properties examined by this filter for
	 * difference. Only the properties that match this expression will be
	 * considered when determining if a sample differs from the previous sample.
	 * This can be useful when sampling data at a high frequency but you are
	 * only interested in one or two specific properties change.
	 * </p>
	 * 
	 * @param propertyIncludePattern
	 *        a regular expression to restrict the examined properties
	 * @since 1.1
	 */
	public void setPropertyIncludePattern(Pattern propertyIncludePattern) {
		this.propertyIncludePattern = propertyIncludePattern;
	}

	/**
	 * Get the property include pattern, as a string.
	 * 
	 * @return a regular expression to restrict the examined properties
	 * @see #getPropertyIncludePattern()
	 * @since 1.1
	 */
	public String getPropertyIncludePatternValue() {
		final Pattern p = getPropertyIncludePattern();
		return (p != null ? p.pattern() : null);
	}

	/**
	 * Set the property include pattern, as a string.
	 * 
	 * @param propertyIncludePatternValue
	 *        a regular expression to restrict the examined properties
	 * @see #setPropertyIncludePattern(Pattern)
	 * @since 1.1
	 */
	public void setPropertyIncludePatternValue(String propertyIncludePatternValue) {
		Pattern p = null;
		try {
			p = Pattern.compile(propertyIncludePatternValue);
		} catch ( PatternSyntaxException e ) {
			log.warn("Invalid property include pattern [{}], ignoring: {}", propertyIncludePatternValue,
					e.getMessage());
		}
		setPropertyIncludePattern(p);
	}

}
