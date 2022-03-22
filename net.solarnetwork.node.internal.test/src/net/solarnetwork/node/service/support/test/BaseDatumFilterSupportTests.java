/* ==================================================================
 * BaseDatumFilterSupportTests.java - 27/02/2022 11:39:02 AM
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.service.support.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import java.time.Instant;
import java.util.Map;
import org.junit.Test;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.support.BaseDatumFilterSupport;

/**
 * Test cases for the {@link BaseDatumFilterSupport} class.
 * 
 * @author matt
 * @version 1.0
 */
public class BaseDatumFilterSupportTests {

	private static class TestBaseDatumFilterSupport extends BaseDatumFilterSupport {

		@Override
		public boolean conditionsMatch(Datum datum, DatumSamplesOperations samples,
				Map<String, Object> parameters) {
			return super.conditionsMatch(datum, samples, parameters);
		}

		@Override
		public boolean tagMatches(Datum datum, DatumSamplesOperations samples) {
			return super.tagMatches(datum, samples);
		}

	}

	@Test
	public void hasTag_noneConfigured() {
		// GIVEN
		TestBaseDatumFilterSupport f = new TestBaseDatumFilterSupport();

		DatumSamples samples = new DatumSamples();
		SimpleDatum datum = SimpleDatum.nodeDatum("test.source", Instant.now(), samples);

		// WHEN
		assertThat("Tag matches if none configured", f.tagMatches(datum, samples), is(true));
	}

	@Test
	public void hasTag_oneConfigured_noMatch() {
		// GIVEN
		TestBaseDatumFilterSupport f = new TestBaseDatumFilterSupport();
		f.setRequiredTag("foo");

		DatumSamples samples = new DatumSamples();
		samples.addTag("bar");
		SimpleDatum datum = SimpleDatum.nodeDatum("test.source", Instant.now(), samples);

		// WHEN
		assertThat("Tag not found", f.tagMatches(datum, samples), is(false));
	}

	@Test
	public void hasTag_oneConfigured_match() {
		// GIVEN
		TestBaseDatumFilterSupport f = new TestBaseDatumFilterSupport();
		f.setRequiredTag("foo");

		DatumSamples samples = new DatumSamples();
		samples.addTag("foo");
		SimpleDatum datum = SimpleDatum.nodeDatum("test.source", Instant.now(), samples);

		// WHEN
		assertThat("Tag found", f.tagMatches(datum, samples), is(true));
	}

	@Test
	public void hasTag_oneConfigured_noMatch_inverted() {
		// GIVEN
		TestBaseDatumFilterSupport f = new TestBaseDatumFilterSupport();
		f.setRequiredTag("!foo");

		DatumSamples samples = new DatumSamples();
		samples.addTag("bar");
		SimpleDatum datum = SimpleDatum.nodeDatum("test.source", Instant.now(), samples);

		// WHEN
		assertThat("Tag found", f.tagMatches(datum, samples), is(true));
	}

	@Test
	public void hasTag_oneConfigured_match_inverted() {
		// GIVEN
		TestBaseDatumFilterSupport f = new TestBaseDatumFilterSupport();
		f.setRequiredTag("!foo");

		DatumSamples samples = new DatumSamples();
		samples.addTag("foo");
		SimpleDatum datum = SimpleDatum.nodeDatum("test.source", Instant.now(), samples);

		// WHEN
		assertThat("Tag not found", f.tagMatches(datum, samples), is(false));
	}

	@Test
	public void hasTag_multiConfigured_noMatch() {
		// GIVEN
		TestBaseDatumFilterSupport f = new TestBaseDatumFilterSupport();
		f.setRequiredTag("foo,moo");

		DatumSamples samples = new DatumSamples();
		samples.addTag("bar");
		SimpleDatum datum = SimpleDatum.nodeDatum("test.source", Instant.now(), samples);

		// WHEN
		assertThat("Tag not found", f.tagMatches(datum, samples), is(false));
	}

	@Test
	public void hasTag_multiConfigured_match() {
		// GIVEN
		TestBaseDatumFilterSupport f = new TestBaseDatumFilterSupport();
		f.setRequiredTag("foo,moo");

		DatumSamples samples = new DatumSamples();
		samples.addTag("foo");
		SimpleDatum datum = SimpleDatum.nodeDatum("test.source", Instant.now(), samples);

		// WHEN
		assertThat("Tag found", f.tagMatches(datum, samples), is(true));
	}

	@Test
	public void hasTag_multiConfigured_match_another() {
		// GIVEN
		TestBaseDatumFilterSupport f = new TestBaseDatumFilterSupport();
		f.setRequiredTag("foo,moo");

		DatumSamples samples = new DatumSamples();
		samples.addTag("moo");
		SimpleDatum datum = SimpleDatum.nodeDatum("test.source", Instant.now(), samples);

		// WHEN
		assertThat("Tag found", f.tagMatches(datum, samples), is(true));
	}

	@Test
	public void hasTag_multiConfigured_noMatch_inverted() {
		// GIVEN
		TestBaseDatumFilterSupport f = new TestBaseDatumFilterSupport();
		f.setRequiredTag("!foo,!moo");

		DatumSamples samples = new DatumSamples();
		samples.addTag("bar");
		SimpleDatum datum = SimpleDatum.nodeDatum("test.source", Instant.now(), samples);

		// WHEN
		assertThat("Tag found", f.tagMatches(datum, samples), is(true));
	}

	@Test
	public void hasTag_multiConfigured_match_inverted() {
		// GIVEN
		TestBaseDatumFilterSupport f = new TestBaseDatumFilterSupport();
		f.setRequiredTag("!foo,moo");

		DatumSamples samples = new DatumSamples();
		samples.addTag("foo");
		SimpleDatum datum = SimpleDatum.nodeDatum("test.source", Instant.now(), samples);

		// WHEN
		assertThat("Tag not found", f.tagMatches(datum, samples), is(false));
	}

}
