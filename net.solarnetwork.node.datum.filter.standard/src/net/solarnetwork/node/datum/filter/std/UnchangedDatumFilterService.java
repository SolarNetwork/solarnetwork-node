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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.springframework.context.MessageSource;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.service.support.BaseDatumFilterSupport;
import net.solarnetwork.service.DatumFilterService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicMultiValueSettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Datum filter service that can discard unchanged datum, within a maximum time
 * range.
 *
 * <p>
 * If a {@code debounceThreshold} is configured, then changed datum are only
 * published after the changed property values have remained stable for at least
 * that amount of time. Changes that revert before the threshold elapses are
 * discarded. The {@code debounceMode} determines how the time is counted: see
 * {@link DebounceMode}.
 * </p>
 *
 * @author matt
 * @version 1.2
 * @since 3.1
 */
public class UnchangedDatumFilterService extends BaseDatumFilterSupport
		implements DatumFilterService, SettingSpecifierProvider {

	/** The {@code unchangedPublishMaxSeconds} property default value. */
	public static final int DEFAULT_UNCHANGED_PUBLISH_MAX_SECONDS = 3599;

	/**
	 * The {@code debounceMode} property default value.
	 *
	 * @since 1.2
	 */
	public static final DebounceMode DEFAULT_DEBOUNCE_MODE = DebounceMode.Consecutive;

	/**
	 * Modes for determining when a changed datum has become stable.
	 *
	 * @since 1.2
	 */
	public enum DebounceMode {

		/**
		 * A changed value must remain unchanged for the entire debounce
		 * threshold, and is abandoned if the previously published value is seen
		 * again.
		 */
		Consecutive,

		/**
		 * Time at a changed value counts towards the debounce threshold, while
		 * time back at the previously published value counts against it. A
		 * changed value is abandoned if its accumulated time drops below zero.
		 */
		Integrator,

		;

	}

	private final ConcurrentMap<String, StreamState> streamStates = new ConcurrentHashMap<>(8, 0.9f, 2);

	private int unchangedPublishMaxSeconds = DEFAULT_UNCHANGED_PUBLISH_MAX_SECONDS;
	private Pattern propertyIncludePattern;
	private Duration debounceThreshold;
	private DebounceMode debounceMode = DEFAULT_DEBOUNCE_MODE;

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
		 * Test if the configured maximum seconds threshold has elapsed since
		 * this sample.
		 *
		 * @param ts
		 *        the timestamp to test
		 * @return {@literal true} if an unchanged datum at {@code ts} should be
		 *         published
		 */
		private boolean isPublishDue(Instant ts) {
			return (unchangedPublishMaxSeconds > 0
					&& !timestamp.plusSeconds(unchangedPublishMaxSeconds).isAfter(ts));
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

		/**
		 * Copy a samples instance, replacing all monitored property values with
		 * those from this sample.
		 *
		 * <p>
		 * If no property include pattern is configured then all properties are
		 * monitored, and a copy of this sample is returned.
		 * </p>
		 *
		 * @param samples
		 *        the samples to copy
		 * @return the new samples instance
		 */
		private DatumSamples debounced(DatumSamplesOperations samples) {
			final Pattern p = getPropertyIncludePattern();
			if ( p == null ) {
				return new DatumSamples(sample);
			}
			final DatumSamples result = new DatumSamples(samples);

			// remove all monitored properties first, in case a property changed type
			for ( DatumSamplesType type : DatumSamplesOperations.KEYED_TYPES ) {
				final Map<String, ?> data = result.getSampleData(type);
				if ( data == null ) {
					continue;
				}
				for ( String k : new ArrayList<>(data.keySet()) ) {
					if ( k != null && p.matcher(k).find() ) {
						result.putSampleValue(type, k, null);
					}
				}
			}

			for ( DatumSamplesType type : DatumSamplesOperations.KEYED_TYPES ) {
				final Map<String, ?> data = sample.getSampleData(type);
				if ( data != null ) {
					for ( Entry<String, ?> e : data.entrySet() ) {
						final String k = e.getKey();
						if ( k != null && p.matcher(k).find() ) {
							result.putSampleValue(type, k, e.getValue());
						}
					}
				}
				final Map<String, ?> m = result.getSampleData(type);
				if ( m != null && m.isEmpty() ) {
					result.setSampleData(type, null);
				}
			}
			return result;
		}
	}

	/**
	 * Filter state for a single datum stream.
	 */
	private final class StreamState {

		/** The last published sample. */
		private SeenSample published;

		/** A changed sample that has not yet been stable for long enough. */
		private SeenSample pending;

		/** The time accumulated towards the pending sample becoming stable. */
		private Duration pendingTime = Duration.ZERO;

		/** The timestamp of the previous datum. */
		private Instant previousTimestamp;

		/**
		 * Filter a datum within this stream.
		 *
		 * @param datum
		 *        the datum
		 * @param samples
		 *        the samples
		 * @return the samples to publish, or {@literal null} to discard
		 */
		private synchronized DatumSamplesOperations filter(Datum datum, DatumSamplesOperations samples) {
			final Instant ts = (datum.getTimestamp() != null ? datum.getTimestamp() : Instant.now());
			final Instant prevTs = previousTimestamp;
			previousTimestamp = ts;
			if ( published == null ) {
				published = new SeenSample(ts, samples);
				return samples;
			}

			// the time since the previous datum is attributed to this datum's values
			final Duration dt = (prevTs.isBefore(ts) ? Duration.between(prevTs, ts) : Duration.ZERO);

			final boolean publishDue = published.isPublishDue(ts);
			if ( published.sampleEquals(samples) ) {
				if ( pending != null ) {
					if ( getDebounceMode() == DebounceMode.Integrator ) {
						// time at the published value counts against the pending change
						pendingTime = pendingTime.minus(dt);
						if ( pendingTime.isNegative() ) {
							pending = null;
						}
					} else {
						// pending change reverted before becoming stable
						pending = null;
					}
				}
				if ( !publishDue ) {
					log.trace(
							"Unchanged filter [{}] discarding source [{}] @ {} as not changed in the past {}s",
							getUid(), datum.getSourceId(), ts, unchangedPublishMaxSeconds);
					return null;
				}
				published = new SeenSample(ts, samples);
				return samples;
			}

			final Duration threshold = getDebounceThreshold();
			if ( threshold == null || threshold.compareTo(Duration.ZERO) <= 0 ) {
				pending = null;
				published = new SeenSample(ts, samples);
				return samples;
			}

			if ( pending == null || !pending.sampleEquals(samples) ) {
				pending = new SeenSample(ts, samples);
				pendingTime = Duration.ZERO;
			} else {
				pendingTime = pendingTime.plus(dt);
				if ( pendingTime.compareTo(threshold) >= 0 ) {
					pending = null;
					published = new SeenSample(ts, samples);
					return samples;
				}
			}

			if ( !publishDue ) {
				log.trace(
						"Unchanged filter [{}] discarding source [{}] @ {} as change since {} only stable for {} of {}",
						getUid(), datum.getSourceId(), ts, pending.timestamp, pendingTime, threshold);
				return null;
			}

			// publish with the last stable property values, while the change remains pending
			final DatumSamples result = published.debounced(samples);
			log.trace(
					"Unchanged filter [{}] publishing source [{}] @ {} with last stable values as change since {} only stable for {} of {}",
					getUid(), datum.getSourceId(), ts, pending.timestamp, pendingTime, threshold);
			published = new SeenSample(ts, result);
			return result;
		}
	}

	@Override
	public DatumSamplesOperations filter(Datum datum, DatumSamplesOperations samples,
			Map<String, Object> params) {
		final long start = incrementInputStats();
		if ( samples == null || !conditionsMatch(datum, samples, params) ) {
			incrementIgnoredStats(start);
			return samples;
		}

		final DatumSamplesOperations out = streamStates
				.computeIfAbsent(datum.getSourceId(), k -> new StreamState()).filter(datum, samples);

		incrementStats(start, samples, out);
		return out;
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
		result.add(new BasicTextFieldSettingSpecifier("debounceThresholdMillis", null));

		// drop-down menu for debounceMode
		final MessageSource messageSource = getMessageSource();
		final BasicMultiValueSettingSpecifier debounceModeSpec = new BasicMultiValueSettingSpecifier(
				"debounceModeName", DEFAULT_DEBOUNCE_MODE.name());
		final Map<String, String> debounceModeTitles = new LinkedHashMap<>(2);
		for ( DebounceMode e : DebounceMode.values() ) {
			String title = e.name();
			if ( messageSource != null ) {
				title = messageSource.getMessage("debounceMode." + e.name(), null, title,
						Locale.getDefault());
			}
			debounceModeTitles.put(e.name(), title);
		}
		debounceModeSpec.setValueTitles(debounceModeTitles);
		result.add(debounceModeSpec);

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

	/**
	 * Get the debounce threshold.
	 *
	 * @return the minimum amount of time a changed datum must remain unchanged
	 *         before it is published, or {@literal null} to publish changes
	 *         immediately
	 * @since 1.2
	 */
	public Duration getDebounceThreshold() {
		return debounceThreshold;
	}

	/**
	 * Set the debounce threshold.
	 *
	 * <p>
	 * When configured, a changed datum is only published after the examined
	 * properties have remained unchanged for at least this amount of time, as
	 * counted by the configured {@code debounceMode}. A change that reverts
	 * before the threshold elapses is discarded. If the
	 * {@code unchangedPublishMaxSeconds} elapses while a change is not yet
	 * stable, the datum is published with the examined properties set to their
	 * last published values.
	 * </p>
	 *
	 * @param debounceThreshold
	 *        the minimum amount of time a changed datum must remain unchanged
	 *        before it is published, or {@literal null} to publish changes
	 *        immediately
	 * @since 1.2
	 */
	public void setDebounceThreshold(Duration debounceThreshold) {
		this.debounceThreshold = debounceThreshold;
	}

	/**
	 * Get the debounce threshold, in milliseconds.
	 *
	 * @return the debounce threshold, in milliseconds
	 * @see #getDebounceThreshold()
	 * @since 1.2
	 */
	public long getDebounceThresholdMillis() {
		final Duration threshold = getDebounceThreshold();
		return (threshold != null ? threshold.toMillis() : 0L);
	}

	/**
	 * Set the debounce threshold, in milliseconds.
	 *
	 * @param millis
	 *        the debounce threshold to set, in milliseconds
	 * @see #setDebounceThreshold(Duration)
	 * @since 1.2
	 */
	public void setDebounceThresholdMillis(long millis) {
		setDebounceThreshold(millis > 0 ? Duration.ofMillis(millis) : null);
	}

	/**
	 * Get the debounce mode.
	 *
	 * @return the debounce mode, never {@literal null}; defaults to
	 *         {@link #DEFAULT_DEBOUNCE_MODE}
	 * @since 1.2
	 */
	public DebounceMode getDebounceMode() {
		return debounceMode;
	}

	/**
	 * Set the debounce mode.
	 *
	 * @param debounceMode
	 *        the debounce mode to set; if {@literal null} then
	 *        {@link #DEFAULT_DEBOUNCE_MODE} will be used
	 * @since 1.2
	 */
	public void setDebounceMode(DebounceMode debounceMode) {
		this.debounceMode = (debounceMode != null ? debounceMode : DEFAULT_DEBOUNCE_MODE);
	}

	/**
	 * Get the debounce mode, as a name.
	 *
	 * @return the debounce mode name
	 * @see #getDebounceMode()
	 * @since 1.2
	 */
	public String getDebounceModeName() {
		return getDebounceMode().name();
	}

	/**
	 * Set the debounce mode, as a name.
	 *
	 * @param name
	 *        the {@link DebounceMode} name to set; if {@literal null} or not
	 *        supported then {@link #DEFAULT_DEBOUNCE_MODE} will be used
	 * @see #setDebounceMode(DebounceMode)
	 * @since 1.2
	 */
	public void setDebounceModeName(String name) {
		DebounceMode mode = null;
		if ( name != null && !name.isEmpty() ) {
			try {
				mode = DebounceMode.valueOf(name);
			} catch ( IllegalArgumentException e ) {
				log.warn("Unsupported debounce mode [{}], using {}", name, DEFAULT_DEBOUNCE_MODE);
			}
		}
		setDebounceMode(mode);
	}

}
