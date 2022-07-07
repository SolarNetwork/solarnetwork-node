/* ==================================================================
 * FeedDatumTests.java - 7/07/2022 11:49:47 am
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

package net.solarnetwork.node.datum.overlay.cloud.test;

import static net.solarnetwork.domain.AcPhase.PhaseA;
import static net.solarnetwork.domain.AcPhase.PhaseB;
import static net.solarnetwork.domain.AcPhase.PhaseC;
import static net.solarnetwork.domain.datum.AcEnergyDatum.CURRENT_KEY;
import static net.solarnetwork.domain.datum.AcEnergyDatum.FREQUENCY_KEY;
import static net.solarnetwork.domain.datum.AcEnergyDatum.POWER_FACTOR_KEY;
import static net.solarnetwork.domain.datum.AcEnergyDatum.VOLTAGE_KEY;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import static net.solarnetwork.domain.datum.EnergyDatum.WATTS_KEY;
import static net.solarnetwork.domain.datum.EnergyStorageDatum.PERCENTAGE_KEY;
import static net.solarnetwork.node.datum.overlay.cloud.FeedDatum.CAPACITY_WATT_HOURS_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.datum.overlay.cloud.FeedData;
import net.solarnetwork.node.datum.overlay.cloud.FeedDatum;

/**
 * Test cases for the {@link FeedDatum} class.
 * 
 * @author matt
 * @version 1.0
 */
public class FeedDatumTests {

	private ObjectMapper mapper = JsonUtils.newObjectMapper();

	@Test
	public void populateFromData() throws IOException {
		// GIVEN
		FeedData data = mapper.readValue(getClass().getResourceAsStream("feed-latest-01.json"),
				FeedData.class);

		// WHEN
		FeedDatum result = new FeedDatum(data, "/foo/bar");

		// THEN
		assertThat("Datum timestamp", result.getTimestamp(),
				is(equalTo(LocalDateTime.of(2022, 3, 1, 0, 10, 0).toInstant(ZoneOffset.UTC))));

		DatumSamples samples = result.getSamples();

		assertThat("Watts A", samples.getSampleInteger(Instantaneous, PhaseA.withKey(WATTS_KEY)),
				is(equalTo(8000)));
		assertThat("Watts B", samples.getSampleInteger(Instantaneous, PhaseB.withKey(WATTS_KEY)),
				is(equalTo(7000)));
		assertThat("Watts C", samples.getSampleInteger(Instantaneous, PhaseC.withKey(WATTS_KEY)),
				is(equalTo(6000)));
		assertThat("Watts total", samples.getSampleInteger(Instantaneous, WATTS_KEY),
				is(equalTo(21000)));

		assertThat("Frequency", samples.getSampleFloat(Instantaneous, FREQUENCY_KEY),
				is(equalTo(50.0f)));

		assertThat("Current A", samples.getSampleFloat(Instantaneous, PhaseA.withKey(CURRENT_KEY)),
				is(equalTo(36.0f)));
		assertThat("Current B", samples.getSampleFloat(Instantaneous, PhaseB.withKey(CURRENT_KEY)),
				is(equalTo(8.0f)));
		assertThat("Current C", samples.getSampleFloat(Instantaneous, PhaseC.withKey(CURRENT_KEY)),
				is(equalTo(12.0f)));
		assertThat("Current avg", samples.getSampleFloat(Instantaneous, CURRENT_KEY),
				is(equalTo(36.0f + 8.0f + 12.0f)));

		assertThat("Voltage AN", samples.getSampleFloat(Instantaneous, PhaseA.withKey(VOLTAGE_KEY)),
				is(equalTo(240.0f)));
		assertThat("Voltage BN", samples.getSampleFloat(Instantaneous, PhaseB.withKey(VOLTAGE_KEY)),
				is(equalTo(240.1f)));
		assertThat("Voltage CN", samples.getSampleFloat(Instantaneous, PhaseC.withKey(VOLTAGE_KEY)),
				is(equalTo(240.2f)));
		assertThat("Voltage avg", samples.getSampleFloat(Instantaneous, VOLTAGE_KEY),
				is(equalTo(240.1f)));

		assertThat("Power factor A",
				samples.getSampleFloat(Instantaneous, PhaseA.withKey(POWER_FACTOR_KEY)),
				is(equalTo(0.99f)));
		assertThat("Power factor B",
				samples.getSampleFloat(Instantaneous, PhaseB.withKey(POWER_FACTOR_KEY)),
				is(equalTo(0.98f)));
		assertThat("Power factor C",
				samples.getSampleFloat(Instantaneous, PhaseC.withKey(POWER_FACTOR_KEY)),
				is(equalTo(0.97f)));
		assertThat("Power factor avg", samples.getSampleFloat(Instantaneous, POWER_FACTOR_KEY),
				is(equalTo(0.98f)));

		assertThat("Charge capacity", samples.getSampleLong(Status, CAPACITY_WATT_HOURS_KEY),
				is(equalTo(12345678L)));
		assertThat("Available charge",
				samples.getSampleLong(Instantaneous, FeedDatum.AVAILABLE_WATT_HOURS_KEY),
				is(equalTo(10000000L)));
		assertThat("Charge percentage", samples.getSampleFloat(Instantaneous, PERCENTAGE_KEY),
				is(equalTo(0.81000006f)));
		assertThat("State of health",
				samples.getSampleFloat(Instantaneous, FeedDatum.STATE_OF_HEALTH_PERCENTAGE_KEY),
				is(equalTo(0.98f)));
	}

}
