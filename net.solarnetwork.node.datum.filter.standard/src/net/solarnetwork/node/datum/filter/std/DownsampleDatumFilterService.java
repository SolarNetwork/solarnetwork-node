/* ==================================================================
 * DownsampleDatumFilterService.java - 24/08/2020 3:47:12 PM
 *
 * Copyright 2020 SolarNetwork.net Dev Team
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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.solarnetwork.domain.datum.AggregateDatumSamples;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.service.DatumFilterService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;
import net.solarnetwork.settings.support.BasicTitleSettingSpecifier;

/**
 * Samples transform service that accumulates "sub-sample" values and then
 * produces a down-sampled average with min/max added.
 *
 * <p>
 * This filter operates in two different modes: one when a
 * {@code sampleDuration} or {@code sampleCount} value is configured and another
 * otherwise. When {@code sampleDuration} is configured then the filter will
 * collect samples within time slots of this duration before generating the
 * down-sampled output samples. When {@code sampleCount} is configured (and
 * greater than {@literal 1}), then the filter will collect {@code sampleCount}
 * samples before generating the down-sampled output samples.
 * </p>
 *
 * <p>
 * When {@code sampleDuration} and {@code sampleCount} are <b>not</b>
 * configured, then sub-samples are signaled by passing the
 * {@link #SUB_SAMPLE_PROP} key in the {@code parameter} map passed to
 * {@link #filter(Datum, DatumSamplesOperations, Map)}. When invoked in this way
 * the method will always return {@literal null}. Then when a down-sampled
 * output value is needed call
 * {@link #filter(Datum, DatumSamplesOperations, Map)} again but without the
 * {@link #SUB_SAMPLE_PROP} key. Then a computed value derived from the
 * collected sub-samples will be returned:
 * </p>
 *
 * <ul>
 * <li>Each instantaneous sample property will be transformed into a simple
 * average</li>
 * <li>Each instantaneous sample property will have <i>_min</i> and <i>_max</i>
 * property names, added as a suffix to the original property name, with
 * associated minimum and maximum property values.
 * <li>
 * <li>Each accumulating sample property will be transformed into the last seen
 * value.
 * <li>
 * <li>Each status sample property will be transformed into the last seen
 * value.</li>
 * </ul>
 *
 * @author matt
 * @version 1.2
 * @since 2.0
 */
public class DownsampleDatumFilterService extends DatumFilterSupport
		implements DatumFilterService, SettingSpecifierProvider {

	/** The "sub sample" transform property flag. */
	public static final String SUB_SAMPLE_PROP = "subsample";

	/**
	 * A transform properties instance that can be used to signal "sub-sampling"
	 * mode to the transform service.
	 */
	public static final Map<String, Object> SUB_SAMPLE_PROPS = Collections.singletonMap(SUB_SAMPLE_PROP,
			Boolean.TRUE);

	/** The {@code decimalScale} property default value. */
	public static final int DEFAULT_DECIMAL_SCALE = 3;

	/** The {@code minPropertyFormat} property default value. */
	public static final String DEFAULT_MIN_FORMAT = "%s_min";

	/** The {@code maxPropertyFormat} property default value. */
	public static final String DEFAULT_MAX_FORMAT = "%s_max";

	/**
	 * The {@code sampleCount} property default value.
	 *
	 * @since 1.1
	 */
	public static Integer DEFAULT_SAMPLE_COUNT = 5;

	private final ConcurrentMap<String, AggregateDatumSamples> subSamplesBySource = new ConcurrentHashMap<>(
			8, 0.9f, 4);

	private int decimalScale = DEFAULT_DECIMAL_SCALE;
	private String minPropertyFormat = DEFAULT_MIN_FORMAT;
	private String maxPropertyFormat = DEFAULT_MAX_FORMAT;
	private Integer sampleCount = DEFAULT_SAMPLE_COUNT;
	private Duration sampleDuration;

	private Clock sampleClock;

	/**
	 * Constructor.
	 */
	public DownsampleDatumFilterService() {
		this(Clock.systemUTC());
	}

	/**
	 * Constructor.
	 *
	 * @param sampleClock
	 *        the sample clock to use; if {@literal null} then the system UTC
	 *        clock will be used
	 * @since 1.2
	 */
	public DownsampleDatumFilterService(Clock sampleClock) {
		super();
		this.sampleClock = (sampleClock != null ? sampleClock : Clock.systemUTC());
	}

	private AggregateDatumSamples newAggregate(String key) {
		return new AggregateDatumSamples(sampleClock.instant());
	}

	@Override
	public DatumSamplesOperations filter(Datum datum, DatumSamplesOperations samples,
			Map<String, Object> parameters) {
		final long start = incrementInputStats();
		if ( datum == null || datum.getSourceId() == null || samples == null ) {
			incrementIgnoredStats(start);
			return samples;
		}
		if ( !conditionsMatch(datum, samples, parameters) ) {
			incrementIgnoredStats(start);
			return samples;
		}
		final int count = (sampleCount != null ? sampleCount.intValue() : 0);
		final Duration dur = (sampleDuration != null ? sampleDuration : null);
		final boolean sub = (parameters != null && parameters.containsKey(SUB_SAMPLE_PROP));
		AggregateDatumSamples agg = subSamplesBySource.computeIfAbsent(datum.getSourceId(),
				this::newAggregate);
		DatumSamplesOperations out = null;
		synchronized ( agg ) {
			if ( dur != null ) {
				Instant datumTs = (datum.getTimestamp() != null ? datum.getTimestamp() : Instant.now());
				Instant nextTimeSlotStart = agg.getTimestamp().plusNanos(dur.toNanos());
				if ( datumTs.isAfter(nextTimeSlotStart) ) {
					if ( agg.addedSampleCount() > 0 ) {
						out = generateAggregateSample(datum, samples, start, agg);
					} else {
						// just move to next time slot, then add sample
						agg.setTimestamp(sampleClock.instant());
					}
				}
			}
			if ( out == null ) {
				agg.addSample(samples);
			}
			if ( dur == null
					&& ((count < 1 && !sub) || (count > 1 && agg.addedSampleCount() >= count)) ) {
				return generateAggregateSample(datum, samples, start, agg);
			}
		}
		if ( out != null ) {
			AggregateDatumSamples next = subSamplesBySource.computeIfAbsent(datum.getSourceId(),
					this::newAggregate);
			synchronized ( next ) {
				next.addSample(samples);
			}
			return out;
		}
		incrementStats(start, samples, null);
		return null;
	}

	private DatumSamplesOperations generateAggregateSample(Datum datum, DatumSamplesOperations samples,
			final long start, AggregateDatumSamples agg) {
		subSamplesBySource.remove(datum.getSourceId(), agg);
		DatumSamples out = agg.average(decimalScale, minPropertyFormat, maxPropertyFormat);
		incrementStats(start, samples, out);
		return out;
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.samplefilter.downsample";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = baseIdentifiableSettings();
		populateStatusSettings(results);

		results.add(0, new BasicTitleSettingSpecifier("status", statusValue()));
		populateBaseSampleTransformSupportSettings(results);
		results.add(new BasicTextFieldSettingSpecifier("sampleCount", DEFAULT_SAMPLE_COUNT.toString()));
		results.add(new BasicTextFieldSettingSpecifier("sampleDurationSecs", null));
		results.add(new BasicTextFieldSettingSpecifier("decimalScale",
				String.valueOf(DEFAULT_DECIMAL_SCALE)));
		results.add(new BasicTextFieldSettingSpecifier("minPropertyFormat", DEFAULT_MIN_FORMAT));
		results.add(new BasicTextFieldSettingSpecifier("maxPropertyFormat", DEFAULT_MAX_FORMAT));

		return results;
	}

	/**
	 * Get the status value.
	 *
	 * @return the status value
	 */
	public String statusValue() {
		StringBuffer buf = new StringBuffer();
		for ( Map.Entry<String, AggregateDatumSamples> me : subSamplesBySource.entrySet() ) {
			if ( buf.length() > 0 ) {
				buf.append(", ");
			}
			AggregateDatumSamples agg = me.getValue();
			synchronized ( agg ) {
				buf.append(me.getKey()).append(": ").append(agg.addedSampleCount()).append(" samples");
			}
		}
		return buf.toString();
	}

	/**
	 * Get the decimal scale.
	 *
	 * @return the scale
	 */
	public int getDecimalScale() {
		return decimalScale;
	}

	/**
	 * Set the decimal scale.
	 *
	 * <p>
	 * This is the maximum decimal scale to round averaged results to.
	 * </p>
	 *
	 * @param decimalScale
	 *        the scale to set; if less than {@literal 0} then {@literal 0} will
	 *        be used
	 */
	public void setDecimalScale(int decimalScale) {
		if ( decimalScale < 0 ) {
			decimalScale = 0;
		}
		this.decimalScale = decimalScale;
	}

	/**
	 * Get the property name format for "minimum" computed values.
	 *
	 * @return the format; defaults to {@link #DEFAULT_MIN_FORMAT}
	 * @since 1.1
	 */
	public String getMinPropertyFormat() {
		return minPropertyFormat;
	}

	/**
	 * Set the property name format for "minimum" computed values.
	 *
	 * <p>
	 * This accepts a standard {@link String#format(String, Object...)} style
	 * formatting template.
	 *
	 * @param minPropertyFormat
	 *        the format to set
	 * @since 1.1
	 */
	public void setMinPropertyFormat(String minPropertyFormat) {
		this.minPropertyFormat = minPropertyFormat;
	}

	/**
	 * Get the property name format for "maximum" computed values.
	 *
	 * @return the format; defaults to {@link #DEFAULT_MIN_FORMAT}
	 * @since 1.1
	 */
	public String getMaxPropertyFormat() {
		return maxPropertyFormat;
	}

	/**
	 * Set the property name format for "maximum" computed values.
	 *
	 * <p>
	 * This accepts a standard {@link String#format(String, Object...)} style
	 * formatting template.
	 *
	 * @param maxPropertyFormat
	 *        the format to set
	 * @since 1.1
	 */
	public void setMaxPropertyFormat(String maxPropertyFormat) {
		this.maxPropertyFormat = maxPropertyFormat;
	}

	/**
	 * Get the sample count.
	 *
	 * @return the count, or {@literal null}
	 */
	public Integer getSampleCount() {
		return sampleCount;
	}

	/**
	 * Set the sample count.
	 *
	 * <p>
	 * If a {@code sampleDuration} is configured, this value will be ignored.
	 * </p>
	 *
	 * @param sampleCount
	 *        the count to set, or {@literal null}
	 */
	public void setSampleCount(Integer sampleCount) {
		this.sampleCount = sampleCount;
	}

	/**
	 * Get the sample duration.
	 *
	 * @return the sample duration
	 * @since 1.2
	 */
	public Duration getSampleDuration() {
		return sampleDuration;
	}

	/**
	 * Set the sample duration.
	 *
	 * <p>
	 * If configured, this takes priority over any configured
	 * {@code sampleCount}.
	 * </p>
	 *
	 * @param sampleDuration
	 *        the sample duration to set; must evenly divide evenly into seconds
	 * @throws IllegalArgumentException
	 *         if the duration is invalid
	 * @since 1.2
	 */
	public void setSampleDuration(Duration sampleDuration) {
		this.sampleDuration = sampleDuration;
		if ( sampleDuration != null ) {
			sampleClock = Clock.tick(Clock.systemUTC(), sampleDuration);
		} else {
			sampleClock = Clock.systemUTC();
		}
	}

	/**
	 * Get the sample duration, as seconds.
	 *
	 * @return the sample duration, as seconds, or {@literal null} if no
	 *         duration configured
	 * @since 1.2
	 */
	public Integer getSampleDurationSecs() {
		final Duration d = getSampleDuration();
		return (d != null ? (int) d.getSeconds() : null);
	}

	/**
	 * Set the sample duration, as seconds.
	 *
	 * @param secs
	 *        the seconds to set as the sample duration
	 * @throws IllegalArgumentException
	 *         if the duration is not valid; see
	 *         {@link #setSampleDuration(Duration)}
	 * @since 1.2
	 */
	public void setSampleDurationSecs(Integer secs) {
		Duration d = (secs != null ? Duration.ofSeconds(secs.longValue()) : null);
		setSampleDuration(d);
	}

}
