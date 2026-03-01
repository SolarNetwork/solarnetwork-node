/* ==================================================================
 * RoundedTimestampDatumFilterServiceTests.java - 2/03/2026 10:21:16 am
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

package net.solarnetwork.node.datum.filter.std.test;

import static java.time.ZoneOffset.UTC;
import static net.solarnetwork.domain.datum.DatumId.nodeId;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.node.datum.filter.std.RoundedTimestampDatumFilterService;
import net.solarnetwork.node.domain.datum.SimpleDatum;

/**
 * Test cases for the {@link RoundedTimestampDatumFilterService}.
 *
 * @author matt
 * @version 1.0
 */
public class RoundedTimestampDatumFilterServiceTests {

	private static final String SOURCE_ID = "TEST";
	private static final String PROP_1 = "watts";

	private RoundedTimestampDatumFilterService xform;

	@Before
	public void setup() {
		xform = new RoundedTimestampDatumFilterService();
		xform.setSourceId("^%s$".formatted(SOURCE_ID));
	}

	private SimpleDatum createTestSimpleDatum(String sourceId, String prop, Number val) {
		SimpleDatum datum = SimpleDatum.nodeDatum(sourceId);
		datum.getSamples().putInstantaneousSampleValue(prop, val);
		return datum;
	}

	@Test
	public void round_notNecessary() throws Exception {
		// GIVEN
		final Instant currSec = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		final RoundedTimestampDatumFilterService xform = new RoundedTimestampDatumFilterService(
				Clock.fixed(currSec, UTC));

		// WHEN
		final SimpleDatum d = createTestSimpleDatum(SOURCE_ID, PROP_1, 123)
				.copyWithId(nodeId(null, SOURCE_ID, currSec));

		final DatumSamplesOperations out = xform.filter(d, d.getSamples(), null);

		// THEN
		// @formatter:off
		then(out)
			.as("Duration-aligned input results in input samples")
			.isSameAs(d.getSamples())
			;
		// @formatter:on
	}

	@Test
	public void round_ms() throws Exception {
		// GIVEN
		final long msDuration = 200L;
		xform.setDuration(Duration.ofMillis(msDuration));

		// WHEN
		final List<SimpleDatum> input = new ArrayList<>();
		final List<DatumSamplesOperations> output = new ArrayList<>();
		for ( int i = 0; i < 10; i++ ) {
			final SimpleDatum d = createTestSimpleDatum(SOURCE_ID, PROP_1, 123);
			input.add(d);
			DatumSamplesOperations out = xform.filter(d, d.getSamples(), null);
			if ( out != null ) {
				output.add(out);
			}
			try {
				Thread.sleep(75L);
			} catch ( InterruptedException e ) {
				// ignore
			}
		}

		// THEN
		// @formatter:off
		then(output)
			.as("One output for every input")
			.hasSameSizeAs(input)
			;

		for (int i = 0; i < input.size(); i++) {
			final SimpleDatum in = input.get(i);
			final DatumSamplesOperations out = output.get(i);
			final Instant inputTs = in.getTimestamp();
			final long msRemainder = inputTs.toEpochMilli() % msDuration;
			if (msRemainder == 0) {
				// input was duration-aligned, so output should be input samples
				then(out)
					.as("Duration-aligned input %d (%s) results in input samples", i, inputTs)
					.isSameAs(in.getSamples())
					;
			} else {
				// output should be NodeDatum with floored timestamp
				then(out)
					.as("Non-duration aligned input %d (%s) results in new SimpleDatum instance", i, inputTs)
					.isInstanceOf(SimpleDatum.class)
					.asInstanceOf(type(SimpleDatum.class))
					.as("New datum timestamp is rounded-down to duration")
					.returns(Instant.ofEpochMilli(inputTs.toEpochMilli() - msRemainder), from(SimpleDatum::getTimestamp))
					.extracting(SimpleDatum::getSamples)
					.as("New datum samples is input samples")
					.isSameAs(in.getSamples())
					;
			}
		}
		// @formatter:on
	}

	@Test
	public void round_ms_notWholeSeconds() throws Exception {
		// GIVEN
		final long msDuration = 330L;
		xform.setDuration(Duration.ofMillis(msDuration));

		// WHEN
		final List<SimpleDatum> input = new ArrayList<>();
		final List<DatumSamplesOperations> output = new ArrayList<>();
		for ( int i = 0; i < 10; i++ ) {
			final SimpleDatum d = createTestSimpleDatum(SOURCE_ID, PROP_1, 123);
			input.add(d);
			DatumSamplesOperations out = xform.filter(d, d.getSamples(), null);
			if ( out != null ) {
				output.add(out);
			}
			try {
				Thread.sleep(175L);
			} catch ( InterruptedException e ) {
				// ignore
			}
		}

		// THEN
		// @formatter:off
		then(output)
			.as("One output for every input")
			.hasSameSizeAs(input)
			;

		for (int i = 0; i < input.size(); i++) {
			final SimpleDatum in = input.get(i);
			final DatumSamplesOperations out = output.get(i);
			final Instant inputTs = in.getTimestamp();
			final long msRemainder = inputTs.toEpochMilli() % msDuration;
			if (msRemainder == 0) {
				// input was duration-aligned, so output should be input samples
				then(out)
					.as("Duration-aligned input %d (%s) results in input samples", i, inputTs)
					.isSameAs(in.getSamples())
					;
			} else {
				// output should be NodeDatum with floored timestamp
				then(out)
					.as("Non-duration aligned input %d (%s) results in new SimpleDatum instance", i, inputTs)
					.isInstanceOf(SimpleDatum.class)
					.asInstanceOf(type(SimpleDatum.class))
					.as("New datum timestamp is rounded-down to duration")
					.returns(Instant.ofEpochMilli(inputTs.toEpochMilli() - msRemainder), from(SimpleDatum::getTimestamp))
					.extracting(SimpleDatum::getSamples)
					.as("New datum samples is input samples")
					.isSameAs(in.getSamples())
					;
			}
		}
		// @formatter:on
	}

	@Test
	public void round_default() throws Exception {
		// GIVEN
		final long msDuration = RoundedTimestampDatumFilterService.DEFAULT_DURATION.toMillis();

		// WHEN
		final List<SimpleDatum> input = new ArrayList<>();
		final List<DatumSamplesOperations> output = new ArrayList<>();
		for ( int i = 0; i < 10; i++ ) {
			final SimpleDatum d = createTestSimpleDatum(SOURCE_ID, PROP_1, 123);
			input.add(d);
			DatumSamplesOperations out = xform.filter(d, d.getSamples(), null);
			if ( out != null ) {
				output.add(out);
			}
			try {
				Thread.sleep(275L);
			} catch ( InterruptedException e ) {
				// ignore
			}
		}

		// THEN
		// @formatter:off
		then(output)
			.as("One output for every input")
			.hasSameSizeAs(input)
			;

		for (int i = 0; i < input.size(); i++) {
			final SimpleDatum in = input.get(i);
			final DatumSamplesOperations out = output.get(i);
			final Instant inputTs = in.getTimestamp();
			final long msRemainder = inputTs.toEpochMilli() % msDuration;
			if (msRemainder == 0) {
				// input was duration-aligned, so output should be input samples
				then(out)
					.as("Duration-aligned input %d (%s) results in input samples", i, inputTs)
					.isSameAs(in.getSamples())
					;
			} else {
				// output should be NodeDatum with floored timestamp
				then(out)
					.as("Non-duration aligned input %d (%s) results in new SimpleDatum instance", i, inputTs)
					.isInstanceOf(SimpleDatum.class)
					.asInstanceOf(type(SimpleDatum.class))
					.as("New datum timestamp is rounded-down to duration")
					.returns(Instant.ofEpochMilli(inputTs.toEpochMilli() - msRemainder), from(SimpleDatum::getTimestamp))
					.extracting(SimpleDatum::getSamples)
					.as("New datum samples is input samples")
					.isSameAs(in.getSamples())
					;
			}
		}
		// @formatter:on
	}

	@Test
	public void round_multiSeconds() throws Exception {
		// GIVEN
		final Duration dur = Duration.ofSeconds(3);
		final long msDuration = dur.toMillis();
		xform.setDuration(dur);

		// WHEN
		final List<SimpleDatum> input = new ArrayList<>();
		final List<DatumSamplesOperations> output = new ArrayList<>();
		for ( int i = 0; i < 10; i++ ) {
			final SimpleDatum d = createTestSimpleDatum(SOURCE_ID, PROP_1, 123);
			input.add(d);
			DatumSamplesOperations out = xform.filter(d, d.getSamples(), null);
			if ( out != null ) {
				output.add(out);
			}
			try {
				Thread.sleep(1333L);
			} catch ( InterruptedException e ) {
				// ignore
			}
		}

		// THEN
		// @formatter:off
		then(output)
			.as("One output for every input")
			.hasSameSizeAs(input)
			;

		for (int i = 0; i < input.size(); i++) {
			final SimpleDatum in = input.get(i);
			final DatumSamplesOperations out = output.get(i);
			final Instant inputTs = in.getTimestamp();
			final long msRemainder = inputTs.toEpochMilli() % msDuration;
			if (msRemainder == 0) {
				// input was duration-aligned, so output should be input samples
				then(out)
					.as("Duration-aligned input %d (%s) results in input samples", i, inputTs)
					.isSameAs(in.getSamples())
					;
			} else {
				// output should be NodeDatum with floored timestamp
				then(out)
					.as("Non-duration aligned input %d (%s) results in new SimpleDatum instance", i, inputTs)
					.isInstanceOf(SimpleDatum.class)
					.asInstanceOf(type(SimpleDatum.class))
					.as("New datum timestamp is rounded-down to duration")
					.returns(Instant.ofEpochMilli(inputTs.toEpochMilli() - msRemainder), from(SimpleDatum::getTimestamp))
					.extracting(SimpleDatum::getSamples)
					.as("New datum samples is input samples")
					.isSameAs(in.getSamples())
					;
			}
		}
		// @formatter:on
	}

}
