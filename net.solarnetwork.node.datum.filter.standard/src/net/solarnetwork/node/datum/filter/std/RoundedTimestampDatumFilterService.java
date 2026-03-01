/* ==================================================================
 * RoundedTimestampDatumFilterService.java - 2/03/2026 10:06:21 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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
import java.time.InstantSource;
import java.util.List;
import java.util.Map;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.service.DatumFilterService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Round datum timestamp values down to duration-aligned values.
 *
 * @author matt
 * @version 1.0
 * @since 4.4
 */
public class RoundedTimestampDatumFilterService extends DatumFilterSupport
		implements DatumFilterService, SettingSpecifierProvider {

	/** The {@code duration} property default value. */
	public static final Duration DEFAULT_DURATION = Duration.ofSeconds(1);

	private Duration duration;

	private InstantSource baseClock;
	private InstantSource sampleClock;

	/**
	 * Constructor.
	 */
	public RoundedTimestampDatumFilterService() {
		this(Clock.systemUTC());
	}

	/**
	 * Constructor.
	 *
	 * @param baseClock
	 *        the base clock to use; if {@literal null} then the system UTC
	 *        clock will be used
	 */
	public RoundedTimestampDatumFilterService(InstantSource baseClock) {
		super();
		this.baseClock = (baseClock != null ? baseClock : Clock.systemUTC());
		setDuration(DEFAULT_DURATION);
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

		DatumSamplesOperations result = samples;

		final Instant clockTs = sampleClock.instant();

		if ( clockTs.compareTo(datum.getTimestamp()) != 0 ) {
			// create a new datum with the given timestamp
			final DatumId newId = new DatumId(datum.getKind(), datum.getObjectId(), datum.getSourceId(),
					clockTs);
			result = new SimpleDatum(newId,
					samples instanceof DatumSamples s ? s : new DatumSamples(samples));
		}
		incrementStats(start, samples, result);
		return result;
	}

	@Override
	public String getSettingUid() {
		return "net.solarnetwork.node.datum.filter.std.timestamp";
	}

	@Override
	public List<SettingSpecifier> getSettingSpecifiers() {
		List<SettingSpecifier> results = baseIdentifiableSettings();
		populateStatusSettings(results);

		populateBaseSampleTransformSupportSettings(results);
		results.add(new BasicTextFieldSettingSpecifier("durationMs",
				String.valueOf(DEFAULT_DURATION.toMillis())));

		return results;
	}

	/**
	 * Get the duration.
	 *
	 * @return the duration
	 */
	public final Duration getDuration() {
		return duration;
	}

	/**
	 * Set the sample duration.
	 *
	 * <p>
	 * If configured, this takes priority over any configured
	 * {@code sampleCount}.
	 * </p>
	 *
	 * @param duration
	 *        the duration to set
	 * @throws IllegalArgumentException
	 *         if the duration is invalid
	 */
	public final synchronized void setDuration(final Duration duration) {
		this.duration = duration;
		if ( duration != null ) {
			sampleClock = InstantSource.tick(baseClock, duration);
		} else {
			sampleClock = baseClock;
		}
	}

	/**
	 * Get the duration, as milliseconds.
	 *
	 * @return the duration, as milliseconds, or {@literal null} if no duration
	 *         configured
	 */
	public final Integer getDurationMs() {
		final Duration d = getDuration();
		return (d != null ? (int) d.toMillis() : null);
	}

	/**
	 * Set the duration, as milliseconds.
	 *
	 * @param ms
	 *        the milliseconds to set as the duration
	 * @throws IllegalArgumentException
	 *         if the duration is not valid; see {@link #setDuration(Duration)}
	 */
	public final void setDurationMs(final Integer ms) {
		Duration d = (ms != null ? Duration.ofMillis(ms.longValue()) : null);
		setDuration(d);
	}

}
