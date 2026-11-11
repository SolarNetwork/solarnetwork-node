/* ==================================================================
 * NodeDatumFactoryTests.java - 11 Nov 2026 12:04:51 am
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

package net.solarnetwork.node.datumhistorian.jdbc.test;

import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.thenIllegalArgumentException;
import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.node.datumhistorian.jdbc.DefaultNodeDatumFactory;
import net.solarnetwork.node.datumhistorian.jdbc.StoredDatum;
import net.solarnetwork.node.domain.datum.AcEnergyDatum;
import net.solarnetwork.node.domain.datum.AtmosphericDatum;
import net.solarnetwork.node.domain.datum.EnergyDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleAcDcEnergyDatum;
import net.solarnetwork.node.domain.datum.SimpleAcEnergyDatum;
import net.solarnetwork.node.domain.datum.SimpleAtmosphericDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.domain.datum.SimpleEnergyDatum;

/**
 * Test cases for the {@link DefaultNodeDatumFactory} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
public class DefaultNodeDatumFactoryTests {

	private static final Long TEST_LOC_ID = 123L;
	private static final Long TEST_NODE_ID = 234L;

	private final DefaultNodeDatumFactory factory = new DefaultNodeDatumFactory();
	private final Instant ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);

	private StoredDatum stored(Class<?> type) {
		return stored(type.getName(), null, factory.typeNames(type));
	}

	private StoredDatum stored(String className, Long locationId, String... typeNames) {
		return stored(className, locationId, new LinkedHashSet<>(List.of(typeNames)));
	}

	private StoredDatum stored(String className, Long locationId, Set<String> typeNames) {
		DatumSamples s = new DatumSamples();
		s.putInstantaneousSampleValue("watts", 1);
		return new StoredDatum(1, "a", locationId, ts, className, typeNames, s);
	}

	private StoredDatum storedWithNodeId(Class<?> type, Long locationId) {
		final StoredDatum d = stored(type.getName(), locationId, factory.typeNames(type));
		return new StoredDatum(d.streamId(), TEST_NODE_ID, d.sourceId(), d.locationId(), d.timestamp(),
				d.className(), d.typeNames(), d.samples());
	}

	private String[] with(String className, Class<?> type) {
		Set<String> names = new LinkedHashSet<>();
		names.add(className);
		names.addAll(factory.typeNames(type));
		return names.toArray(String[]::new);
	}

	@Test
	public void typeNames() {
		// WHEN
		Set<String> result = factory.typeNames(SimpleAcEnergyDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Class name first, then superclasses")
			.startsWith(SimpleAcEnergyDatum.class.getName(), SimpleEnergyDatum.class.getName(),
					SimpleDatum.class.getName())
			.as("Interfaces included")
			.contains(AcEnergyDatum.class.getName(), EnergyDatum.class.getName(),
					NodeDatum.class.getName(), net.solarnetwork.domain.datum.Datum.class.getName(),
					Serializable.class.getName())
			.as("Object not included")
			.doesNotContain(Object.class.getName())
			.as("No duplicates")
			.doesNotHaveDuplicates()
			;
		and.then(factory.typeNames(SimpleAcEnergyDatum.class))
			.as("Same names returned again")
			.isSameAs(result)
			;
		// @formatter:on
	}

	@Test
	public void create_storedClass() {
		// GIVEN
		StoredDatum d = stored(SimpleAcEnergyDatum.class);

		// WHEN
		NodeDatum result = factory.createDatum(d, null);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Stored class created")
			.isExactlyInstanceOf(SimpleAcEnergyDatum.class)
			.returns(ObjectDatumKind.Node, from(NodeDatum::getKind))
			.returns("a", from(NodeDatum::getSourceId))
			.returns(ts, from(NodeDatum::getTimestamp))
			.returns(1, from(r -> r.asSampleOperations().getSampleInteger(
					DatumSamplesType.Instantaneous, "watts")))
			;
		// @formatter:on
	}

	@Test
	public void create_requestedType() {
		// GIVEN
		StoredDatum d = stored(SimpleAcEnergyDatum.class);

		// WHEN
		AcEnergyDatum ac = factory.createDatum(d, AcEnergyDatum.class);
		EnergyDatum energy = factory.createDatum(d, EnergyDatum.class);
		AtmosphericDatum atmos = factory.createDatum(d, AtmosphericDatum.class);
		SimpleAcDcEnergyDatum subclass = factory.createDatum(d, SimpleAcDcEnergyDatum.class);

		// THEN
		// @formatter:off
		and.then(ac).as("Created as interface type").isExactlyInstanceOf(SimpleAcEnergyDatum.class);
		and.then(energy).as("Created as super interface type").isExactlyInstanceOf(SimpleAcEnergyDatum.class);
		and.then(atmos).as("Not created as other type").isNull();
		and.then(subclass).as("Not created as subclass type").isNull();
		// @formatter:on
	}

	@Test
	public void create_location() {
		// GIVEN
		StoredDatum d = stored(SimpleAtmosphericDatum.class.getName(), TEST_LOC_ID,
				factory.typeNames(SimpleAtmosphericDatum.class));

		// WHEN
		AtmosphericDatum result = factory.createDatum(d, AtmosphericDatum.class);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Location datum created")
			.isExactlyInstanceOf(SimpleAtmosphericDatum.class)
			.returns(ObjectDatumKind.Location, from(NodeDatum::getKind))
			.returns(TEST_LOC_ID, from(NodeDatum::getObjectId))
			;
		// @formatter:on
	}

	@Test
	public void create_nodeConstructor() {
		// GIVEN
		StoredDatum d = stored(TestDatum.NodeOnly.class);

		// WHEN
		NodeDatum result = factory.createDatum(d, null);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Created with (String, Instant, DatumSamples) constructor")
			.isExactlyInstanceOf(TestDatum.NodeOnly.class)
			.returns("a", from(NodeDatum::getSourceId))
			.returns(ts, from(NodeDatum::getTimestamp))
			;
		// @formatter:on
	}

	@Test
	public void create_locationConstructor() {
		// GIVEN
		StoredDatum node = stored(TestDatum.NodeOrLocation.class);
		StoredDatum loc = stored(TestDatum.NodeOrLocation.class.getName(), TEST_LOC_ID,
				factory.typeNames(TestDatum.NodeOrLocation.class));

		// WHEN
		NodeDatum nodeResult = factory.createDatum(node, null);
		NodeDatum locResult = factory.createDatum(loc, null);

		// THEN
		// @formatter:off
		and.then(nodeResult)
			.as("Node datum created with node constructor")
			.isExactlyInstanceOf(TestDatum.NodeOrLocation.class)
			.returns(ObjectDatumKind.Node, from(NodeDatum::getKind))
			;
		and.then(locResult)
			.as("Location datum created with location constructor")
			.isExactlyInstanceOf(TestDatum.NodeOrLocation.class)
			.returns(ObjectDatumKind.Location, from(NodeDatum::getKind))
			.returns(TEST_LOC_ID, from(NodeDatum::getObjectId))
			;
		// @formatter:on
	}

	@Test
	public void fallback_noConstructor() {
		// GIVEN
		StoredDatum d = stored(TestDatum.NoConstructor.class);

		// WHEN
		NodeDatum result = factory.createDatum(d, null);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Class without supported constructor created as closest Simple class")
			.isExactlyInstanceOf(SimpleDatum.class)
			.returns("a", from(NodeDatum::getSourceId))
			;
		// @formatter:on
	}

	@Test
	public void fallback_missingClass() {
		// GIVEN
		StoredDatum ac = stored("com.example.MissingAc", null,
				with("com.example.MissingAc", SimpleAcEnergyDatum.class));
		StoredDatum atmos = stored("com.example.MissingAtmos", TEST_LOC_ID,
				with("com.example.MissingAtmos", AtmosphericDatum.class));
		StoredDatum plain = stored("com.example.MissingPlain", null,
				with("com.example.MissingPlain", NodeDatum.class));

		// WHEN
		AcEnergyDatum acResult = factory.createDatum(ac, AcEnergyDatum.class);
		AtmosphericDatum atmosResult = factory.createDatum(atmos, AtmosphericDatum.class);
		NodeDatum plainResult = factory.createDatum(plain, NodeDatum.class);

		// THEN
		// @formatter:off
		and.then(acResult)
			.as("Missing class created as Simple class with the same types")
			.isExactlyInstanceOf(SimpleAcEnergyDatum.class)
			;
		and.then(atmosResult)
			.as("Missing class created as Simple class of interface")
			.isExactlyInstanceOf(SimpleAtmosphericDatum.class)
			.returns(TEST_LOC_ID, from(NodeDatum::getObjectId))
			;
		and.then(plainResult)
			.as("Missing class without known types created as SimpleDatum")
			.isExactlyInstanceOf(SimpleDatum.class)
			;
		// @formatter:on
	}

	@Test
	public void fallback_requestedTypeClassLoader() {
		// GIVEN
		// a factory whose class loader cannot see the test classes
		DefaultNodeDatumFactory isolated = new DefaultNodeDatumFactory(new ClassLoader(null) {
			// no classes
		});
		StoredDatum d = stored(TestDatum.NodeOnly.class);

		// WHEN
		NodeDatum any = isolated.createDatum(d, null);
		TestDatum.NodeOnly result = isolated.createDatum(d, TestDatum.NodeOnly.class);
		NodeDatum again = isolated.createDatum(d, null);

		// THEN
		// @formatter:off
		and.then(any)
			.as("Class not visible to factory created as fallback")
			.isExactlyInstanceOf(SimpleDatum.class)
			;
		and.then(result)
			.as("Class loaded with class loader of requested type")
			.isExactlyInstanceOf(TestDatum.NodeOnly.class)
			;
		and.then(again)
			.as("Class loaded with requested type remembered")
			.isExactlyInstanceOf(TestDatum.NodeOnly.class)
			;
		// @formatter:on
	}

	@Test
	public void brokenConstructor() {
		// GIVEN
		StoredDatum d = stored(TestDatum.Broken.class);

		// WHEN
		NodeDatum result = factory.createDatum(d, null);

		// THEN
		// @formatter:off
		and.then(result)
			.as("No datum when constructor throws exception")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void notNodeDatumClass() {
		// GIVEN
		StoredDatum d = stored(String.class.getName(), null, String.class.getName(),
				NodeDatum.class.getName());

		// WHEN
		NodeDatum result = factory.createDatum(d, null);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Class that is not a NodeDatum created as fallback")
			.isExactlyInstanceOf(SimpleDatum.class)
			;
		// @formatter:on
	}

	@Test
	public void nullClassLoader() {
		thenIllegalArgumentException().isThrownBy(() -> new DefaultNodeDatumFactory(null));
	}


	@Test
	public void create_nodeId() {
		// GIVEN
		StoredDatum d = storedWithNodeId(SimpleAcEnergyDatum.class, null);

		// WHEN
		NodeDatum result = factory.createDatum(d, null);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Stored class created")
			.isExactlyInstanceOf(SimpleAcEnergyDatum.class)
			.as("Node ID set")
			.returns(TEST_NODE_ID, from(NodeDatum::getObjectId))
			.returns(ObjectDatumKind.Node, from(NodeDatum::getKind))
			;
		// @formatter:on
	}

	@Test
	public void create_nodeId_location() {
		// GIVEN
		StoredDatum d = storedWithNodeId(SimpleAtmosphericDatum.class, TEST_LOC_ID);

		// WHEN
		NodeDatum result = factory.createDatum(d, null);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Location ID set, not node ID")
			.returns(TEST_LOC_ID, from(NodeDatum::getObjectId))
			.returns(ObjectDatumKind.Location, from(NodeDatum::getKind))
			;
		// @formatter:on
	}

	@Test
	public void create_nodeConstructor_nodeIdCopied() {
		// GIVEN
		StoredDatum d = storedWithNodeId(TestDatum.NodeOnlyCopyable.class, null);

		// WHEN
		NodeDatum result = factory.createDatum(d, null);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Created with (String, Instant, DatumSamples) constructor and copied with node ID")
			.isExactlyInstanceOf(TestDatum.NodeOnlyCopyable.class)
			.returns(TEST_NODE_ID, from(NodeDatum::getObjectId))
			.returns("a", from(NodeDatum::getSourceId))
			.returns(ts, from(NodeDatum::getTimestamp))
			.returns(1, from(r -> r.asSampleOperations().getSampleInteger(
					DatumSamplesType.Instantaneous, "watts")))
			;
		// @formatter:on
	}

	@Test
	public void create_nodeConstructor_nodeIdNotCopiedWhenClassChanges() {
		// GIVEN
		StoredDatum d = storedWithNodeId(TestDatum.NodeOnly.class, null);

		// WHEN
		NodeDatum result = factory.createDatum(d, null);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Copy with ID returns a different class, so class kept without node ID")
			.isExactlyInstanceOf(TestDatum.NodeOnly.class)
			.returns(null, from(NodeDatum::getObjectId))
			;
		// @formatter:on
	}

}
