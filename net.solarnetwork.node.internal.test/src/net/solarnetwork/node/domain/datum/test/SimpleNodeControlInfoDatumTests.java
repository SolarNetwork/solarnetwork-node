/* ==================================================================
 * NodeControlInfoDatumTests.java - 27/09/2017 8:59:29 AM
 *
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.domain.datum.test;

import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import static net.solarnetwork.node.domain.datum.SimpleNodeControlInfoDatum.DEFAULT_INSTANT_PROPERTY_NAME;
import static net.solarnetwork.node.domain.datum.SimpleNodeControlInfoDatum.DEFAULT_PROPERTY_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import net.solarnetwork.domain.BasicNodeControlInfo;
import net.solarnetwork.domain.NodeControlPropertyType;
import net.solarnetwork.node.domain.datum.SimpleNodeControlInfoDatum;

/**
 * Test cases for the {@link SimpleNodeControlInfoDatum} class.
 *
 * @author matt
 * @version 1.1
 */
public class SimpleNodeControlInfoDatumTests {

	private static final String TEST_SOURCE = UUID.randomUUID().toString();

	private void assertBaseDatumProperties(SimpleNodeControlInfoDatum datum, Instant ts) {
		assertThat("Source ID", datum.getSourceId(), is(TEST_SOURCE));
		assertThat("Timestamp", datum.getTimestamp(), is(ts));
		Map<String, ?> map = datum.asSimpleMap();
		assertThat(map, hasEntry("_DatumType", "net.solarnetwork.domain.NodeControlInfo"));
		assertThat((String[]) map.get("_DatumTypes"),
				arrayContaining("net.solarnetwork.domain.NodeControlInfo"));
	}

	@Test
	public void boolean_true() {
		// GIVEN

		// @formatter:off
		BasicNodeControlInfo info = BasicNodeControlInfo.builder()
				.withControlId(TEST_SOURCE)
				.withReadonly(false)
				.withType(NodeControlPropertyType.Boolean)
				.withValue("true")
				.build();
		// @formatter:on

		// WHEN
		final Instant ts = Instant.now();
		SimpleNodeControlInfoDatum datum = new SimpleNodeControlInfoDatum(info, ts);

		assertBaseDatumProperties(datum, ts);
		assertThat("Cotrol property value", datum.asSampleOperations().getSampleInteger(Status, "val"),
				is(1));
	}

	@Test
	public void boolean_false() {
		// GIVEN

		// @formatter:off
		BasicNodeControlInfo info = BasicNodeControlInfo.builder()
				.withControlId(TEST_SOURCE)
				.withReadonly(false)
				.withType(NodeControlPropertyType.Boolean)
				.withValue("false")
				.build();
		// @formatter:on

		// WHEN
		final Instant ts = Instant.now();
		SimpleNodeControlInfoDatum datum = new SimpleNodeControlInfoDatum(info, ts);

		assertBaseDatumProperties(datum, ts);
		assertThat("Cotrol property value", datum.asSampleOperations().getSampleInteger(Status, "val"),
				is(0));
	}

	@Test
	public void boolean_prop_true() {
		// GIVEN

		// @formatter:off
		BasicNodeControlInfo info = BasicNodeControlInfo.builder()
				.withControlId(TEST_SOURCE)
				.withPropertyName("test-property")
				.withReadonly(false)
				.withType(NodeControlPropertyType.Boolean)
				.withValue("true")
				.build();
		// @formatter:on

		// WHEN
		final Instant ts = Instant.now();
		SimpleNodeControlInfoDatum datum = new SimpleNodeControlInfoDatum(info, ts);

		assertBaseDatumProperties(datum, ts);
		assertThat("Cotrol property value",
				datum.asSampleOperations().getSampleInteger(Status, "test-property"), is(1));
	}

	@Test
	public void boolean_prop_false() {
		// GIVEN

		// @formatter:off
		BasicNodeControlInfo info = BasicNodeControlInfo.builder()
				.withControlId(TEST_SOURCE)
				.withPropertyName("test-property")
				.withReadonly(false)
				.withType(NodeControlPropertyType.Boolean)
				.withValue("false")
				.build();
		// @formatter:on

		// WHEN
		final Instant ts = Instant.now();
		SimpleNodeControlInfoDatum datum = new SimpleNodeControlInfoDatum(info, ts);

		assertBaseDatumProperties(datum, ts);
		assertThat("Cotrol property value",
				datum.asSampleOperations().getSampleInteger(Status, "test-property"), is(0));
	}

	@Test
	public void integer_valprop() {
		// GIVEN

		// @formatter:off
		BasicNodeControlInfo info = BasicNodeControlInfo.builder()
				.withControlId(TEST_SOURCE)
				.withPropertyName(DEFAULT_PROPERTY_NAME)
				.withReadonly(false)
				.withType(NodeControlPropertyType.Integer)
				.withValue("12345")
				.build();
		// @formatter:on

		// WHEN
		final Instant ts = Instant.now();
		SimpleNodeControlInfoDatum datum = new SimpleNodeControlInfoDatum(info, ts);

		assertBaseDatumProperties(datum, ts);
		assertThat("Cotrol property value",
				datum.asSampleOperations().getSampleValue(Status, DEFAULT_PROPERTY_NAME), is(12345));
		assertThat("Cotrol instantaneous property value also populated",
				datum.asSampleOperations().getSampleValue(Instantaneous, DEFAULT_INSTANT_PROPERTY_NAME),
				is(12345));
	}

	@Test
	public void integer_valprop_long() {
		// GIVEN
		final String longString = "123456789012";

		// @formatter:off
		BasicNodeControlInfo info = BasicNodeControlInfo.builder()
				.withControlId(TEST_SOURCE)
				.withPropertyName(DEFAULT_PROPERTY_NAME)
				.withReadonly(false)
				.withType(NodeControlPropertyType.Integer)
				.withValue(longString)
				.build();
		// @formatter:on

		// WHEN
		final Instant ts = Instant.now();
		SimpleNodeControlInfoDatum datum = new SimpleNodeControlInfoDatum(info, ts);

		assertBaseDatumProperties(datum, ts);
		assertThat("Cotrol property value",
				datum.asSampleOperations().getSampleValue(Status, DEFAULT_PROPERTY_NAME),
				is((Object) Long.valueOf(longString)));
		assertThat("Cotrol instantaneous property value also populated",
				datum.asSampleOperations().getSampleValue(Instantaneous, DEFAULT_INSTANT_PROPERTY_NAME),
				is((Object) Long.valueOf(longString)));
	}

	@Test
	public void integer_valprop_bigInteger() {
		// GIVEN
		final String bigIntegerString = "123456789012345678901234567890";

		// @formatter:off
		BasicNodeControlInfo info = BasicNodeControlInfo.builder()
				.withControlId(TEST_SOURCE)
				.withPropertyName(DEFAULT_PROPERTY_NAME)
				.withReadonly(false)
				.withType(NodeControlPropertyType.Integer)
				.withValue(bigIntegerString)
				.build();
		// @formatter:on

		// WHEN
		final Instant ts = Instant.now();
		SimpleNodeControlInfoDatum datum = new SimpleNodeControlInfoDatum(info, ts);

		assertBaseDatumProperties(datum, ts);
		assertThat("Cotrol property value",
				datum.asSampleOperations().getSampleValue(Status, DEFAULT_PROPERTY_NAME),
				is(new BigInteger(bigIntegerString)));
		assertThat("Cotrol instantaneous property value also populated",
				datum.asSampleOperations().getSampleValue(Instantaneous, DEFAULT_INSTANT_PROPERTY_NAME),
				is(new BigInteger(bigIntegerString)));
	}

}
