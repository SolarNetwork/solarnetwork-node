/* ==================================================================
 * DatumEventsTests.java - 11 Nov 2026 8:30:08 am
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

package net.solarnetwork.node.service.test;

import static org.assertj.core.api.BDDAssertions.then;
import java.time.Instant;
import org.junit.Test;
import org.osgi.service.event.Event;
import net.solarnetwork.domain.datum.AcDcEnergyDatum;
import net.solarnetwork.domain.datum.AcEnergyDatum;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DcEnergyDatum;
import net.solarnetwork.domain.datum.EnergyDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.domain.datum.SimpleAcDcEnergyDatum;
import net.solarnetwork.node.domain.datum.SimpleAcEnergyDatum;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.service.DatumEvents;

/**
 * Test cases for the {@link DatumEvents} class.
 *
 * @author matt
 * @version 1.0
 */
public class DatumEventsTests {

	@Test
	public void datumTypes_simpleDatum() {
		// WHEN
		String[] result = DatumEvents.datumTypes(SimpleDatum.class);

		// THEN
		then(result).as("Datum interfaces, excluding mutable ones")
				.containsExactly(NodeDatum.class.getName(), Datum.class.getName());
	}

	@Test
	public void datumTypes_acEnergyDatum() {
		// WHEN
		String[] result = DatumEvents.datumTypes(SimpleAcEnergyDatum.class);

		// THEN
		then(result).as("Datum interfaces in class hierarchy order").containsExactly(
				EnergyDatum.class.getName(), Datum.class.getName(), NodeDatum.class.getName(),
				AcEnergyDatum.class.getName());
	}

	@Test
	public void datumTypes_acDcEnergyDatum() {
		// WHEN
		String[] result = DatumEvents.datumTypes(SimpleAcDcEnergyDatum.class);

		// THEN
		then(result).as("Datum interfaces shared by several super interfaces included once")
				.containsExactly(EnergyDatum.class.getName(), Datum.class.getName(),
						NodeDatum.class.getName(), AcEnergyDatum.class.getName(),
						DcEnergyDatum.class.getName(), AcDcEnergyDatum.class.getName());
	}

	@Test
	public void datumTypes_notDatum() {
		// WHEN
		String[] result = DatumEvents.datumTypes(String.class);

		// THEN
		then(result).as("No datum types for non-datum class").isEmpty();
	}

	@Test
	public void datumTypes_cached() {
		// WHEN
		String[] result1 = DatumEvents.datumTypes(SimpleAcEnergyDatum.class);
		String[] result2 = DatumEvents.datumTypes(SimpleAcEnergyDatum.class);

		// THEN
		then(result2).as("Cached types returned for same class").isSameAs(result1);
	}

	@Test
	public void datumEvent_typeProperties() {
		// GIVEN
		SimpleAcEnergyDatum datum = new SimpleAcEnergyDatum("test", Instant.now(),
				new DatumSamples());

		// WHEN
		Event result = DatumEvents.datumEvent("test/topic", datum);

		// THEN
		then(result.getProperty(Datum.DATUM_TYPE_PROPERTY)).as("Primary datum type")
				.isEqualTo(EnergyDatum.class.getName());
		then(result.getProperty(Datum.DATUM_TYPES_PROPERTY)).as("All datum types")
				.isSameAs(DatumEvents.datumTypes(SimpleAcEnergyDatum.class));
		then(result.getProperty(DatumEvents.DATUM_PROPERTY)).as("Datum").isSameAs(datum);
	}

}
