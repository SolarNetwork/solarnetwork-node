/* ==================================================================
 * TempestUdpDatumDataSourceTests.java - 5/11/2023 11:25:16 am
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

package net.solarnetwork.node.datum.weather.weatherflow.tempest.test;

import static net.solarnetwork.domain.datum.DatumSamplesType.Accumulating;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import static net.solarnetwork.util.StringUtils.commaDelimitedStringFromCollection;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.domain.DeviceInfo;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.node.datum.weather.weatherflow.tempest.SensorStatus;
import net.solarnetwork.node.datum.weather.weatherflow.tempest.TempestUdpDatumDataSource;
import net.solarnetwork.node.domain.datum.NodeDatum;
import net.solarnetwork.node.service.DatumMetadataService;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link TempestUdpDatumDataSource} class.
 * 
 * @author matt
 * @version 1.0
 */
public class TempestUdpDatumDataSourceTests {

	private DatumQueue datumQueue;
	private DatumMetadataService datumMetadataService;
	private TempestUdpDatumDataSource dataSource;

	@Before
	public void setup() {
		datumQueue = EasyMock.createMock(DatumQueue.class);
		datumMetadataService = EasyMock.createMock(DatumMetadataService.class);

		dataSource = new TempestUdpDatumDataSource();
		dataSource.setSourceId(UUID.randomUUID().toString());
		dataSource.setDatumQueue(new StaticOptionalService<>(datumQueue));
		dataSource.setDatumMetadataService(new StaticOptionalService<>(datumMetadataService));
	}

	private void replayAll() {
		EasyMock.replay(datumQueue, datumMetadataService);
	}

	private void resetAll() {
		EasyMock.reset(datumQueue, datumMetadataService);
	}

	@After
	public void teardown() {
		EasyMock.verify(datumQueue, datumMetadataService);
	}

	private String textResource(String name) {
		try {
			return FileCopyUtils.copyToString(
					new InputStreamReader(getClass().getResourceAsStream(name), StandardCharsets.UTF_8));
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void processHubStatus() throws IOException {
		// GIVEN
		final String msgJson = textResource("hub-status-01.json");

		// metadata published
		Capture<GeneralDatumMetadata> metaCaptor = Capture.newInstance();
		datumMetadataService.addSourceMetadata(eq(dataSource.getSourceId()), capture(metaCaptor));

		// WHEN
		replayAll();
		dataSource.processJsonMessage(msgJson);

		// THEN
		GeneralDatumMetadata meta = metaCaptor.getValue();
		assertThat("Metadata generated from hub status", meta, is(notNullValue()));

		Map<String, ?> info = meta.getPropertyInfo(DeviceInfo.DEVICE_INFO_METADATA_KEY);
		assertThat("Device manufacturer populated", info, hasEntry("manufacturer", "WeatherFlow"));
		assertThat("Device model populated", info, hasEntry("modelName", "Tempest"));
		assertThat("Device version populated", info, hasEntry("version", "Hub: 171"));
		assertThat("Device serial populated", info, hasEntry("serialNumber", "Hub: HB-00046392"));
		assertThat("Expected info populated", info.keySet(), hasSize(4));
	}

	@Test
	public void processDeviceStatus() throws IOException {
		// GIVEN
		final String msgJson = textResource("device-status-01.json");

		// metadata published
		Capture<GeneralDatumMetadata> metaCaptor = Capture.newInstance();
		datumMetadataService.addSourceMetadata(eq(dataSource.getSourceId()), capture(metaCaptor));

		// datum generated
		Capture<NodeDatum> datumCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(datumCaptor))).andReturn(true);

		// WHEN
		replayAll();
		dataSource.processJsonMessage(msgJson);

		// THEN
		GeneralDatumMetadata meta = metaCaptor.getValue();
		assertThat("Metadata generated from device status", meta, is(notNullValue()));

		Map<String, ?> info = meta.getPropertyInfo(DeviceInfo.DEVICE_INFO_METADATA_KEY);
		assertThat("Device manufacturer populated", info, hasEntry("manufacturer", "WeatherFlow"));
		assertThat("Device model populated", info, hasEntry("modelName", "Tempest"));
		assertThat("Device version populated", info, hasEntry("version", "Sensor: 17"));
		assertThat("Device serial populated", info,
				hasEntry("serialNumber", "Sensor: AR-00004049, Hub: HB-00000001"));
		assertThat("Expected info populated", info.keySet(), hasSize(4));

		NodeDatum d = datumCaptor.getValue();
		assertThat("Datum generated from device status", d, is(notNullValue()));
		assertThat("Status source ID used", d.getSourceId(), is(equalTo(
				dataSource.getSourceId() + TempestUdpDatumDataSource.STATUS_EVENT_SOURCE_ID_SUFFIX)));
		assertThat("Timestamp from event", d.getTimestamp(),
				is(equalTo(Instant.ofEpochSecond(1510855923L))));
		assertThat("Voltage populated", d.asSampleOperations().getSampleFloat(Instantaneous, "voltage"),
				is(equalTo(3.5f)));
		assertThat("Uptime populated", d.asSampleOperations().getSampleLong(Accumulating, "uptime"),
				is(equalTo(2189L)));
		assertThat("Status bitmask populated",
				d.asSampleOperations().getSampleInteger(Status, "opStates"), is(equalTo(0x10090)));
		assertThat("Status bitmask populated", d.asSampleOperations().getSampleString(Status, "status"),
				is(equalTo(commaDelimitedStringFromCollection(EnumSet.of(SensorStatus.TemperatureFailed,
						SensorStatus.PrecipFailed, SensorStatus.PowerBoosterShorePower)))));
		assertThat("Expected properties populated", d.asSimpleMap().keySet(), Matchers
				.containsInAnyOrder("created", "sourceId", "voltage", "uptime", "opStates", "status"));
	}

	@Test
	public void processDeviceStatus_afterHubStatus() throws IOException {
		// GIVEN
		processHubStatus();
		resetAll();

		final String msgJson = textResource("device-status-01.json");

		// metadata published
		Capture<GeneralDatumMetadata> metaCaptor = Capture.newInstance();
		datumMetadataService.addSourceMetadata(eq(dataSource.getSourceId()), capture(metaCaptor));

		// datum generated
		Capture<NodeDatum> datumCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(datumCaptor))).andReturn(true);

		// WHEN
		replayAll();
		dataSource.processJsonMessage(msgJson);

		// THEN
		GeneralDatumMetadata meta = metaCaptor.getValue();
		assertThat("Metadata generated from device status", meta, is(notNullValue()));

		Map<String, ?> info = meta.getPropertyInfo(DeviceInfo.DEVICE_INFO_METADATA_KEY);
		assertThat("Device manufacturer populated", info, hasEntry("manufacturer", "WeatherFlow"));
		assertThat("Device model populated", info, hasEntry("modelName", "Tempest"));
		assertThat("Device version populated", info, hasEntry("version", "Sensor: 17, Hub: 171"));
		assertThat("Device serial populated", info,
				hasEntry("serialNumber", "Sensor: AR-00004049, Hub: HB-00046392"));
		assertThat("Expected info populated", info.keySet(), hasSize(4));

		NodeDatum d = datumCaptor.getValue();
		assertThat("Datum generated from device status", d, is(notNullValue()));
		assertThat("Status source ID used", d.getSourceId(), is(equalTo(
				dataSource.getSourceId() + TempestUdpDatumDataSource.STATUS_EVENT_SOURCE_ID_SUFFIX)));
		assertThat("Timestamp from event", d.getTimestamp(),
				is(equalTo(Instant.ofEpochSecond(1510855923L))));
		assertThat("Voltage populated", d.asSampleOperations().getSampleFloat(Instantaneous, "voltage"),
				is(equalTo(3.5f)));
		assertThat("Uptime populated", d.asSampleOperations().getSampleLong(Accumulating, "uptime"),
				is(equalTo(2189L)));
		assertThat("Status bitmask populated",
				d.asSampleOperations().getSampleInteger(Status, "opStates"), is(equalTo(0x10090)));
		assertThat("Status bitmask populated", d.asSampleOperations().getSampleString(Status, "status"),
				is(equalTo(commaDelimitedStringFromCollection(EnumSet.of(SensorStatus.TemperatureFailed,
						SensorStatus.PrecipFailed, SensorStatus.PowerBoosterShorePower)))));
		assertThat("Expected properties populated", d.asSimpleMap().keySet(), Matchers
				.containsInAnyOrder("created", "sourceId", "voltage", "uptime", "opStates", "status"));
	}

	@Test
	public void processEventPrecip() throws IOException {
		// GIVEN
		final String msgJson = textResource("evt-precip-01.json");

		// datum generated
		Capture<NodeDatum> datumCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(datumCaptor))).andReturn(true);

		// WHEN
		replayAll();
		dataSource.processJsonMessage(msgJson);

		// THEN
		DeviceInfo info = dataSource.deviceInfo();
		assertThat("Info generated for hub SN", info, is(notNullValue()));
		assertThat("Device serial populated", info.getSerialNumber(), is(equalTo("Hub: HB-00000001")));

		NodeDatum d = datumCaptor.getValue();
		assertThat("Datum generated from event", d, is(notNullValue()));
		assertThat("Status source ID used", d.getSourceId(), is(equalTo(
				dataSource.getSourceId() + TempestUdpDatumDataSource.PRECIP_EVENT_SOURCE_ID_SUFFIX)));
		assertThat("Timestamp from event", d.getTimestamp(),
				is(equalTo(Instant.ofEpochSecond(1493322445))));
		assertThat("Start status", d.asSampleOperations().getSampleInteger(Status, "start"),
				is(equalTo(1)));
		assertThat("Expected properties populated", d.asSimpleMap().keySet(),
				Matchers.containsInAnyOrder("created", "sourceId", "start"));
	}

	@Test
	public void processEventStrike() throws IOException {
		// GIVEN
		final String msgJson = textResource("evt-strike-01.json");

		// datum generated
		Capture<NodeDatum> datumCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(datumCaptor))).andReturn(true);

		// WHEN
		replayAll();
		dataSource.processJsonMessage(msgJson);

		// THEN
		DeviceInfo info = dataSource.deviceInfo();
		assertThat("Info generated for hub SN", info, is(notNullValue()));
		assertThat("Device serial populated", info.getSerialNumber(), is(equalTo("Hub: HB-00000001")));

		NodeDatum d = datumCaptor.getValue();
		assertThat("Datum generated from event", d, is(notNullValue()));
		assertThat("Status source ID used", d.getSourceId(), is(equalTo(dataSource.getSourceId()
				+ TempestUdpDatumDataSource.LIGHTNING_STRIKE_EVENT_SOURCE_ID_SUFFIX)));
		assertThat("Timestamp from event", d.getTimestamp(),
				is(equalTo(Instant.ofEpochSecond(1493322445))));
		assertThat("Distance populated in meters",
				d.asSampleOperations().getSampleInteger(Instantaneous, "distance"), is(equalTo(27000)));
		assertThat("Energy populated", d.asSampleOperations().getSampleInteger(Instantaneous, "energy"),
				is(equalTo(3848)));
		assertThat("Expected properties populated", d.asSimpleMap().keySet(),
				Matchers.containsInAnyOrder("created", "sourceId", "distance", "energy"));
	}

	@Test
	public void processRapidWind() throws IOException {
		// GIVEN
		final String msgJson = textResource("rapid-wind-01.json");

		// datum generated
		Capture<NodeDatum> datumCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(datumCaptor))).andReturn(true);

		// WHEN
		replayAll();
		dataSource.processJsonMessage(msgJson);

		// THEN
		DeviceInfo info = dataSource.deviceInfo();
		assertThat("Info generated for hub SN", info, is(notNullValue()));
		assertThat("Device serial populated", info.getSerialNumber(), is(equalTo("Hub: HB-00000001")));

		NodeDatum d = datumCaptor.getValue();
		assertThat("Datum generated from event", d, is(notNullValue()));
		assertThat("Status source ID used", d.getSourceId(), is(equalTo(dataSource.getSourceId()
				+ TempestUdpDatumDataSource.RAPID_WIND_EVENT_SOURCE_ID_SUFFIX)));
		assertThat("Timestamp from event", d.getTimestamp(),
				is(equalTo(Instant.ofEpochSecond(1493322445))));
		assertThat("Wind speed populated",
				d.asSampleOperations().getSampleFloat(Instantaneous, "wspeed"), is(equalTo(2.3f)));
		assertThat("Wind direction populated",
				d.asSampleOperations().getSampleInteger(Instantaneous, "wdir"), is(equalTo(128)));
		assertThat("Expected properties populated", d.asSimpleMap().keySet(), containsInAnyOrder(
				"_DatumType", "_DatumTypes", "created", "sourceId", "wspeed", "wdir"));
	}

	@Test
	public void processObsAir() throws IOException {
		// GIVEN
		final String msgJson = textResource("obs-air-01.json");

		// datum generated
		Capture<NodeDatum> datumCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(datumCaptor))).andReturn(true);

		// WHEN
		replayAll();
		dataSource.processJsonMessage(msgJson);

		// THEN
		DeviceInfo info = dataSource.deviceInfo();
		assertThat("Info generated for hub SN", info, is(notNullValue()));
		assertThat("Device serial populated", info.getSerialNumber(), is(equalTo("Hub: HB-00000001")));

		NodeDatum d = datumCaptor.getValue();
		assertThat("Datum generated from observation", d, is(notNullValue()));
		assertThat("Source ID used", d.getSourceId(), is(equalTo(dataSource.getSourceId())));
		assertThat("Timestamp from event", d.getTimestamp(),
				is(equalTo(Instant.ofEpochSecond(1493164835))));
		assertThat("Air pressure populated as pascals",
				d.asSampleOperations().getSampleInteger(Instantaneous, "atm"), is(equalTo(83500)));
		assertThat("Temperature populated",
				d.asSampleOperations().getSampleInteger(Instantaneous, "temp"), is(equalTo(10)));
		assertThat("Humidity populated",
				d.asSampleOperations().getSampleInteger(Instantaneous, "humidity"), is(equalTo(45)));
		assertThat("Strike count populated",
				d.asSampleOperations().getSampleInteger(Instantaneous, "strikes"), is(equalTo(3)));
		assertThat("Avg strike distance populated as meters",
				d.asSampleOperations().getSampleInteger(Instantaneous, "avgStrikeDistance"),
				is(equalTo(4000)));
		assertThat("Battery populated",
				d.asSampleOperations().getSampleFloat(Instantaneous, "batteryVoltage"),
				is(equalTo(3.46f)));
		assertThat("Interval populated as seconds",
				d.asSampleOperations().getSampleInteger(Instantaneous, "duration"), is(equalTo(60)));
		assertThat("Expected properties populated", d.asSimpleMap().keySet(),
				containsInAnyOrder("_DatumType", "_DatumTypes", "created", "sourceId", "atm", "temp",
						"humidity", "strikes", "avgStrikeDistance", "batteryVoltage", "duration"));
	}

	@Test
	public void processObsSky() throws IOException {
		// GIVEN
		final String msgJson = textResource("obs-sky-01.json");

		// datum generated
		Capture<NodeDatum> datumCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(datumCaptor))).andReturn(true);

		// WHEN
		replayAll();
		dataSource.processJsonMessage(msgJson);

		// THEN
		DeviceInfo info = dataSource.deviceInfo();
		assertThat("Info generated for hub SN", info, is(notNullValue()));
		assertThat("Device serial populated", info.getSerialNumber(), is(equalTo("Hub: HB-00000001")));

		NodeDatum d = datumCaptor.getValue();
		assertThat("Datum generated from observation", d, is(notNullValue()));
		assertThat("Source ID used", d.getSourceId(), is(equalTo(dataSource.getSourceId())));
		assertThat("Timestamp from event", d.getTimestamp(),
				is(equalTo(Instant.ofEpochSecond(1493321340))));
		assertThat("Lux populated as pascals",
				d.asSampleOperations().getSampleInteger(Instantaneous, "lux"), is(equalTo(9000)));
		assertThat("UV index populated",
				d.asSampleOperations().getSampleInteger(Instantaneous, "uvIndex"), is(equalTo(10)));
		assertThat("Rain populated", d.asSampleOperations().getSampleInteger(Instantaneous, "rain"),
				is(equalTo(0)));
		assertThat("Wind speed (lull) populated",
				d.asSampleOperations().getSampleFloat(Instantaneous, "wspeed_lull"), is(equalTo(2.6f)));
		assertThat("Wind speed populated",
				d.asSampleOperations().getSampleFloat(Instantaneous, "wspeed"), is(equalTo(4.6f)));
		assertThat("Wind speed (gust) populated",
				d.asSampleOperations().getSampleFloat(Instantaneous, "wspeed_gust"), is(equalTo(7.4f)));
		assertThat("Wind direction populated",
				d.asSampleOperations().getSampleInteger(Instantaneous, "wdir"), is(equalTo(187)));
		assertThat("Battery populated",
				d.asSampleOperations().getSampleFloat(Instantaneous, "batteryVoltage"),
				is(equalTo(3.12f)));
		assertThat("Interval populated as seconds",
				d.asSampleOperations().getSampleInteger(Instantaneous, "duration"), is(equalTo(60)));
		assertThat("Irradiance populated",
				d.asSampleOperations().getSampleInteger(Instantaneous, "irradiance"), is(equalTo(130)));
		assertThat("Day rain populated",
				d.asSampleOperations().getSampleInteger(Accumulating, "rain_day"), is(equalTo(5)));
		assertThat("Precip type populated",
				d.asSampleOperations().getSampleInteger(Status, "precipType"), is(equalTo(1)));
		assertThat("Wind sample interval populated",
				d.asSampleOperations().getSampleInteger(Instantaneous, "windDuration"), is(equalTo(3)));
		assertThat("Expected properties populated", d.asSimpleMap().keySet(),
				containsInAnyOrder("_DatumType", "_DatumTypes", "created", "sourceId", "lux", "uvIndex",
						"rain", "wspeed_lull", "wspeed", "wspeed_gust", "wdir", "batteryVoltage",
						"duration", "irradiance", "rain_day", "precipType", "windDuration"));
	}

	@Test
	public void processObsSt() throws IOException {
		// GIVEN
		final String msgJson = textResource("obs-st-01.json");

		// datum generated
		Capture<NodeDatum> datumCaptor = Capture.newInstance();
		expect(datumQueue.offer(capture(datumCaptor))).andReturn(true);

		// WHEN
		replayAll();
		dataSource.processJsonMessage(msgJson);

		// THEN
		DeviceInfo info = dataSource.deviceInfo();
		assertThat("Info generated for hub SN", info, is(notNullValue()));
		assertThat("Device serial populated", info.getSerialNumber(), is(equalTo("Hub: HB-00013030")));

		NodeDatum d = datumCaptor.getValue();
		assertThat("Datum generated from observation", d, is(notNullValue()));
		assertThat("Source ID used", d.getSourceId(), is(equalTo(dataSource.getSourceId())));
		assertThat("Timestamp from event", d.getTimestamp(),
				is(equalTo(Instant.ofEpochSecond(1588948614))));

		assertThat("Wind speed (lull) populated",
				d.asSampleOperations().getSampleFloat(Instantaneous, "wspeed_lull"), is(equalTo(0.18f)));
		assertThat("Wind speed populated",
				d.asSampleOperations().getSampleFloat(Instantaneous, "wspeed"), is(equalTo(0.22f)));
		assertThat("Wind speed (gust) populated",
				d.asSampleOperations().getSampleFloat(Instantaneous, "wspeed_gust"), is(equalTo(0.27f)));
		assertThat("Wind direction populated",
				d.asSampleOperations().getSampleInteger(Instantaneous, "wdir"), is(equalTo(144)));
		assertThat("Wind sample interval populated",
				d.asSampleOperations().getSampleInteger(Instantaneous, "windDuration"), is(equalTo(6)));

		assertThat("Air pressure populated as pascals",
				d.asSampleOperations().getSampleInteger(Instantaneous, "atm"), is(equalTo(101757)));
		assertThat("Temperature populated", d.asSampleOperations().getSampleFloat(Instantaneous, "temp"),
				is(equalTo(22.37f)));
		assertThat("Humidity populated",
				d.asSampleOperations().getSampleInteger(Instantaneous, "humidity"), is(equalTo(50)));

		assertThat("Lux populated as pascals",
				d.asSampleOperations().getSampleInteger(Instantaneous, "lux"), is(equalTo(328)));
		assertThat("UV index populated", d.asSampleOperations().getSampleFloat(Instantaneous, "uvIndex"),
				is(equalTo(0.03f)));
		assertThat("Irradiance populated",
				d.asSampleOperations().getSampleInteger(Instantaneous, "irradiance"), is(equalTo(3)));
		assertThat("Rain populated", d.asSampleOperations().getSampleInteger(Instantaneous, "rain"),
				is(equalTo(0)));
		assertThat("Precip type populated",
				d.asSampleOperations().getSampleInteger(Status, "precipType"), is(equalTo(0)));

		assertThat("Avg strike distance populated as meters",
				d.asSampleOperations().getSampleInteger(Instantaneous, "avgStrikeDistance"),
				is(equalTo(2000)));
		assertThat("Strike count populated",
				d.asSampleOperations().getSampleInteger(Instantaneous, "strikes"), is(equalTo(12)));

		assertThat("Battery populated",
				d.asSampleOperations().getSampleFloat(Instantaneous, "batteryVoltage"),
				is(equalTo(2.41f)));
		assertThat("Interval populated as seconds",
				d.asSampleOperations().getSampleInteger(Instantaneous, "duration"), is(equalTo(60)));
		assertThat("Expected properties populated", d.asSimpleMap().keySet(),
				containsInAnyOrder("_DatumType", "_DatumTypes", "created", "sourceId", "wspeed_lull",
						"wspeed", "wspeed_gust", "wdir", "atm", "temp", "humidity", "lux", "uvIndex",
						"irradiance", "rain", "avgStrikeDistance", "strikes", "batteryVoltage",
						"windDuration", "precipType", "duration"));
	}

}
