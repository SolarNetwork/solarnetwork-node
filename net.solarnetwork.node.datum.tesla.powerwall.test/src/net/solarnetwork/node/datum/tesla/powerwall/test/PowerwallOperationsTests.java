/* ==================================================================
 * PowerwallOperationsTests.java - 9/11/2023 3:09:47 pm
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

package net.solarnetwork.node.datum.tesla.powerwall.test;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.util.stream.Collectors.toMap;
import static net.solarnetwork.domain.datum.DatumSamplesType.Accumulating;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.AcPhase;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.EnergyStorageDatum;
import net.solarnetwork.node.datum.tesla.powerwall.PowerwallOperations;
import net.solarnetwork.node.domain.datum.AcDcEnergyDatum;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.test.http.AbstractHttpServerTests;
import net.solarnetwork.test.http.TestHttpHandler;

/**
 * Test cases for the {@link PowerwallOperations} class.
 *
 * @author matt
 * @version 2.0
 */
public class PowerwallOperationsTests extends AbstractHttpServerTests {

	private static final Logger log = LoggerFactory.getLogger(PowerwallOperationsTests.class);

	private String username;
	private String password;
	private PowerwallOperations ops;

	@Override
	@Before
	public void setup() {
		super.setup();
		username = UUID.randomUUID().toString();
		password = UUID.randomUUID().toString();
		ops = new PowerwallOperations(false, "localhost:" + getHttpServerPort(), username, password,
				buildRequestConfig(), JsonUtils.newObjectMapper());
	}

	private RequestConfig buildRequestConfig() {
		// @formatter:off
		return RequestConfig.custom()
				.setRedirectsEnabled(false)
				.setCookieSpec(CookieSpecs.DEFAULT)
				.build();
		// @formatter:on
	}

	@Test
	public void datum() throws IOException {
		// GIVEN
		final String sourceId = UUID.randomUUID().toString();

		final TestHttpHandler handler = new TestHttpHandler() {

			@Override
			protected boolean handleInternal(Request request, Response response, Callback callback)
					throws Exception {
				String path = request.getHttpURI().getPath();
				switch (path) {
					case "/api/meters/aggregates":
						respondWithJsonResource(request, response, "meters-aggregates-01.json");
						break;

					case "/api/system_status":
						respondWithJsonResource(request, response, "system_status-01.json");
						break;

					case "/api/system_status/soe":
						respondWithJsonResource(request, response, "system_status-soe-01.json");
						break;

					default:
						response.setStatus(HttpStatus.NOT_FOUND.value());
						break;
				}
				return true;
			}

		};
		addHandler(handler);

		// WHEN
		Collection<NodeDatum> datum = ops.datum(sourceId);

		// THEN
		log.debug("Got datum: {}", datum);
		Map<String, NodeDatum> datumMap = datum.stream().collect(toMap(d -> d.getSourceId(), d -> d));
		assertThat("Generated datum sources", datumMap.keySet(),
				containsInAnyOrder(sourceId + PowerwallOperations.DEFAULT_BATTERY_SUFFIX,
						sourceId + PowerwallOperations.DEFAULT_LOAD_SUFFIX,
						sourceId + PowerwallOperations.DEFAULT_SITE_SUFFIX,
						sourceId + PowerwallOperations.DEFAULT_SOLAR_SUFFIX));

		/*-
		 Datum{kind=Node,sourceId=a860ecee-9653-484d-bc4e-efbd1242b400/battery,
		 	ts=2023-11-09T02:29:58.897559649Z,
		 	data={i={
		 		watts=123,
		 		reactivePower=330,
		 		apparentPower=331,
		 		frequency=50.071,
		 		voltage=241.8,
		 		current=-0.2,
		 		current_a=0.1,
		 		current_b=0.2,
		 		current_c=0.3,
		 		soc=87.5,
		 		capacityWattHours=14015,
		 		availWattHours=12366},
		 	a={
		 		wattHours=7986302,
		 		wattHoursReverse=9273303},
		 	s={gridConnected=1}}}

		    "battery": {
		        "last_communication_time": "2023-11-09T15:29:58.897559649+13:00",
		        "instant_power": 123,
		        "instant_reactive_power": 330,
		        "instant_apparent_power": 331,
		        "frequency": 50.071,
		        "energy_exported": 7986302,
		        "energy_imported": 9273303,
		        "instant_average_voltage": 241.8,
		        "instant_average_current": -0.2,
		        "i_a_current": 0.1,
		        "i_b_current": 0.2,
		        "i_c_current": 0.3,
		        "last_phase_voltage_communication_time": "0001-01-01T00:00:00Z",
		        "last_phase_power_communication_time": "0001-01-01T00:00:00Z",
		        "last_phase_energy_communication_time": "0001-01-01T00:00:00Z",
		        "timeout": 1500000000,
		        "num_meters_aggregated": 1,
		        "instant_total_current": -0.25
		    }
		    {"percentage":87.5}

		    "nominal_full_pack_energy": 14015,
		    "nominal_energy_remaining": 12366,
		    "system_island_state": "SystemGridConnected",
		 */
		NodeDatum batt = datumMap.get(sourceId + PowerwallOperations.DEFAULT_BATTERY_SUFFIX);
		DatumSamplesOperations d = batt.asSampleOperations();
		assertThat("Date from last_communication_time", batt.getTimestamp(),
				is(equalTo(ISO_DATE_TIME.parse("2023-11-09T15:29:58.897559649+13:00", Instant::from))));
		assertThat("Power from instant_power",
				d.getSampleInteger(Instantaneous, AcDcEnergyDatum.WATTS_KEY), is(equalTo(123)));
		assertThat("Reactive power from instant_reactive_power",
				d.getSampleInteger(Instantaneous, AcDcEnergyDatum.REACTIVE_POWER_KEY), is(equalTo(330)));
		assertThat("Apparent power from instant_apparent_power",
				d.getSampleInteger(Instantaneous, AcDcEnergyDatum.APPARENT_POWER_KEY), is(equalTo(331)));
		assertThat("Frequency from frequency",
				d.getSampleFloat(Instantaneous, AcDcEnergyDatum.FREQUENCY_KEY), is(equalTo(50.071f)));
		assertThat("Voltage from instant_average_voltage",
				d.getSampleFloat(Instantaneous, AcDcEnergyDatum.VOLTAGE_KEY), is(equalTo(241.8f)));
		assertThat("Current from instant_total_current",
				d.getSampleFloat(Instantaneous, AcDcEnergyDatum.CURRENT_KEY), is(equalTo(-0.25f)));
		assertThat("Current A from i_a_current",
				d.getSampleFloat(Instantaneous, AcPhase.PhaseA.withKey(AcDcEnergyDatum.CURRENT_KEY)),
				is(equalTo(0.1f)));
		assertThat("Current B from i_b_current",
				d.getSampleFloat(Instantaneous, AcPhase.PhaseB.withKey(AcDcEnergyDatum.CURRENT_KEY)),
				is(equalTo(0.2f)));
		assertThat("Current C from i_c_current",
				d.getSampleFloat(Instantaneous, AcPhase.PhaseC.withKey(AcDcEnergyDatum.CURRENT_KEY)),
				is(equalTo(0.3f)));
		assertThat("Energy from energy_exported",
				d.getSampleLong(Accumulating, AcDcEnergyDatum.WATT_HOUR_READING_KEY),
				is(equalTo(7986302L)));
		assertThat("Energy reverse from energy_imported",
				d.getSampleLong(Accumulating,
						AcDcEnergyDatum.WATT_HOUR_READING_KEY
								+ AcDcEnergyDatum.REVERSE_ACCUMULATING_SUFFIX_KEY),
				is(equalTo(9273303L)));

		assertThat("SOC from SOE",
				d.getSampleFloat(Instantaneous, EnergyStorageDatum.STATE_OF_CHARGE_PERCENTAGE_KEY),
				is(equalTo(87.5f)));

		assertThat("Capacity from nominal_full_pack_energy",
				d.getSampleInteger(Instantaneous, EnergyStorageDatum.CAPACITY_WATT_HOURS_KEY),
				is(equalTo(14015)));
		assertThat("Capacity from nominal_energy_remaining",
				d.getSampleInteger(Instantaneous, EnergyStorageDatum.AVAILABLE_WATT_HOURS_KEY),
				is(equalTo(12366)));
		assertThat("Grid connection state from system_island_state",
				d.getSampleInteger(Status, "gridConnected"), is(equalTo(1)));
	}
}
