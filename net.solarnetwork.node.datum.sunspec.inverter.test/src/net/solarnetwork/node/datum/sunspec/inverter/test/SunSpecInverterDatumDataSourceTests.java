/* ==================================================================
 * SunSpecInverterDatumDataSourceTests.java - 13/10/2019 7:23:55 am
 *
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.sunspec.inverter.test;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.DeviceInfo;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.datum.sunspec.inverter.SunSpecInverterDatumDataSource;
import net.solarnetwork.node.domain.datum.AcDcEnergyDatum;
import net.solarnetwork.node.hw.sunspec.inverter.InverterNameplateRatingsModelAccessor;
import net.solarnetwork.node.hw.sunspec.inverter.InverterOperatingState;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.TitleSettingSpecifier;

/**
 * Test cases for the {@link SunSpecInverterDatumDataSource} class.
 *
 * @author matt
 * @version 2.0
 */
public class SunSpecInverterDatumDataSourceTests {

	private static final String TEST_SOURCE_ID = "test.source";

	private ModbusNetwork modbusNetwork;

	private SunSpecInverterDatumDataSource dataSource;

	@Before
	public void setup() {
		modbusNetwork = EasyMock.createMock(ModbusNetwork.class);

		dataSource = new SunSpecInverterDatumDataSource();
		dataSource.setSourceId(TEST_SOURCE_ID);
		dataSource.setModbusNetwork(new StaticOptionalService<ModbusNetwork>(modbusNetwork));
	}

	private void replayAll() {
		EasyMock.replay(modbusNetwork);
	}

	@After
	public void teardown() {
		EasyMock.verify(modbusNetwork);
	}

	private ModbusConnection expectStaticDataModbusConnection(String dataResource) throws IOException {
		ModbusConnection modbusConnection = ModelDataUtils.getStaticDataConnection(getClass(),
				dataResource);
		Capture<ModbusConnectionAction<Object>> connActionCapture = Capture.newInstance();
		expect(modbusNetwork.performAction(eq(1), capture(connActionCapture)))
				.andAnswer(new IAnswer<Object>() {

					@Override
					public Object answer() throws Throwable {
						ModbusConnectionAction<?> action = connActionCapture.getValue();
						return action.doWithConnection(modbusConnection);
					}
				});
		return modbusConnection;
	}

	@Test
	public void infoSettingsMessage() throws IOException {
		// GIVEN
		expectStaticDataModbusConnection("test-data-113-01.txt");

		// WHEN
		replayAll();
		List<SettingSpecifier> settings = dataSource.getSettingSpecifiers();
		TitleSettingSpecifier info = settings.stream()
				.filter(s -> s instanceof TitleSettingSpecifier
						&& ((TitleSettingSpecifier) s).getKey().equals("info"))
				.map(s -> (TitleSettingSpecifier) s).findAny().orElse(null);

		// THEN
		assertThat("Datum info available", info, notNullValue());
		assertThat("Datum info value", info.getDefaultValue(),
				equalTo("Fronius / Symo 3.0-3-S (version 0.3.11.10) / 29251001150340235"));
	}

	@Test
	public void deviceInfo() throws IOException {
		// GIVEN
		expectStaticDataModbusConnection("test-data-113-01.txt");

		// WHEN
		replayAll();
		DeviceInfo info = dataSource.deviceInfo();

		// THEN
		assertThat("Datum info available", info, notNullValue());
		assertThat("Manufacturer resolved", info.getManufacturer(), is(equalTo("Fronius")));
		assertThat("Model name resolved", info.getModelName(),
				is(equalTo("Symo 3.0-3-S (version 0.3.11.10)")));
		assertThat("Serial number resolved", info.getSerialNumber(), is(equalTo("29251001150340235")));

		Map<String, Object> expectedNameplateRatings = new LinkedHashMap<>(17);
		expectedNameplateRatings.put(InverterNameplateRatingsModelAccessor.INFO_KEY_DER_TYPE, "PV");
		expectedNameplateRatings.put(InverterNameplateRatingsModelAccessor.INFO_KEY_DER_TYPE_CODE, 4);
		expectedNameplateRatings.put(InverterNameplateRatingsModelAccessor.INFO_KEY_ACTIVE_POWER_RATING,
				3000);
		expectedNameplateRatings
				.put(InverterNameplateRatingsModelAccessor.INFO_KEY_APPARENT_POWER_RATING, 3000);
		expectedNameplateRatings
				.put(InverterNameplateRatingsModelAccessor.INFO_KEY_REACTIVE_POWER_Q1_RATING, 2140);
		expectedNameplateRatings
				.put(InverterNameplateRatingsModelAccessor.INFO_KEY_REACTIVE_POWER_Q4_RATING, -2140);
		expectedNameplateRatings.put(InverterNameplateRatingsModelAccessor.INFO_KEY_CURRENT_RATING,
				4.2f);
		expectedNameplateRatings
				.put(InverterNameplateRatingsModelAccessor.INFO_KEY_POWER_FACTOR_Q1_RATING, -0.85f);
		expectedNameplateRatings
				.put(InverterNameplateRatingsModelAccessor.INFO_KEY_POWER_FACTOR_Q4_RATING, 0.85f);
		assertThat("Nameplate ratings resolved", info.getNameplateRatings(),
				is(equalTo(expectedNameplateRatings)));
	}

	@Test
	public void collectDatum() throws IOException {
		// GIVEN
		expectStaticDataModbusConnection("test-data-113-01.txt");

		// WHEN
		replayAll();
		long start = System.currentTimeMillis();
		AcDcEnergyDatum d = dataSource.readCurrentDatum();

		// THEN
		assertThat("Datum returned", d, notNullValue());
		assertThat("Datum source ID", d.getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Datum created now", d.getTimestamp().toEpochMilli(), greaterThanOrEqualTo(start));
		assertThat("Datum frequency",
				d.asSampleOperations().getSampleFloat(DatumSamplesType.Instantaneous, "frequency"),
				equalTo(50.05f));
		assertThat("Datum voltage",
				d.asSampleOperations().getSampleFloat(DatumSamplesType.Instantaneous, "voltage"),
				equalTo(248.13335f));
		assertThat("Datum current",
				d.asSampleOperations().getSampleFloat(DatumSamplesType.Instantaneous, "current"),
				equalTo(0.7f));
		assertThat("Datum power factor",
				d.asSampleOperations().getSampleFloat(DatumSamplesType.Instantaneous, "powerFactor"),
				equalTo(1.0f));
		assertThat("Datum apparent power",
				d.asSampleOperations().getSampleInteger(DatumSamplesType.Instantaneous, "apparentPower"),
				equalTo(70));
		assertThat("Datum reactive power",
				d.asSampleOperations().getSampleInteger(DatumSamplesType.Instantaneous, "reactivePower"),
				equalTo(0));
		assertThat("Datum DC voltage",
				d.asSampleOperations().getSampleFloat(DatumSamplesType.Instantaneous, "dcVoltage"),
				equalTo(406.9f));
		assertThat("Datum DC power",
				d.asSampleOperations().getSampleInteger(DatumSamplesType.Instantaneous, "dcPower"),
				equalTo(61));
		assertThat("Datum power",
				d.asSampleOperations().getSampleInteger(DatumSamplesType.Instantaneous, "watts"),
				equalTo(70));
		assertThat("Datum energy",
				d.asSampleOperations().getSampleLong(DatumSamplesType.Accumulating, "wattHours"),
				equalTo(11937020L));
		assertThat("Datum status",
				d.asSampleOperations().getSampleString(DatumSamplesType.Status, "phase"),
				equalTo("Total"));
		assertThat("Datum opState",
				d.asSampleOperations().getSampleInteger(DatumSamplesType.Status, "opState"),
				equalTo(DeviceOperatingState.Normal.getCode()));
		assertThat("Datum sunsOpState",
				d.asSampleOperations().getSampleInteger(DatumSamplesType.Status, "sunsOpState"),
				equalTo(InverterOperatingState.Mppt.getCode()));
		assertThat("Datum events",
				d.asSampleOperations().getSampleInteger(DatumSamplesType.Status, "events"), equalTo(0));
		assertThat("Datum vendor events",
				d.asSampleOperations().getSampleString(DatumSamplesType.Status, "vendorEvents"),
				is(equalTo("0x70008000500060003000400010002")));
	}

	@Test
	public void collectDatum_phaseMeasurements() throws IOException {
		// GIVEN
		dataSource.setIncludePhaseMeasurements(true);
		expectStaticDataModbusConnection("test-data-113-01.txt");

		// WHEN
		replayAll();
		long start = System.currentTimeMillis();
		AcDcEnergyDatum d = dataSource.readCurrentDatum();

		// THEN
		assertThat("Datum returned", d, notNullValue());
		assertThat("Datum source ID", d.getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Datum created now", d.getTimestamp().toEpochMilli(), greaterThanOrEqualTo(start));
		assertThat("Datum frequency",
				d.asSampleOperations().getSampleFloat(DatumSamplesType.Instantaneous, "frequency"),
				equalTo(50.05f));
		assertThat("Datum voltage",
				d.asSampleOperations().getSampleFloat(DatumSamplesType.Instantaneous, "voltage"),
				equalTo(248.13335f));
		assertThat("Datum current",
				d.asSampleOperations().getSampleFloat(DatumSamplesType.Instantaneous, "current"),
				equalTo(0.7f));
		assertThat("Datum power factor",
				d.asSampleOperations().getSampleFloat(DatumSamplesType.Instantaneous, "powerFactor"),
				equalTo(1.0f));
		assertThat("Datum apparent power",
				d.asSampleOperations().getSampleInteger(DatumSamplesType.Instantaneous, "apparentPower"),
				equalTo(70));
		assertThat("Datum reactive power",
				d.asSampleOperations().getSampleInteger(DatumSamplesType.Instantaneous, "reactivePower"),
				equalTo(0));
		assertThat("Datum DC voltage",
				d.asSampleOperations().getSampleFloat(DatumSamplesType.Instantaneous, "dcVoltage"),
				equalTo(406.9f));
		assertThat("Datum DC power",
				d.asSampleOperations().getSampleInteger(DatumSamplesType.Instantaneous, "dcPower"),
				equalTo(61));
		assertThat("Datum power",
				d.asSampleOperations().getSampleInteger(DatumSamplesType.Instantaneous, "watts"),
				equalTo(70));
		assertThat("Datum energy",
				d.asSampleOperations().getSampleLong(DatumSamplesType.Accumulating, "wattHours"),
				equalTo(11937020L));
		assertThat("Datum status",
				d.asSampleOperations().getSampleString(DatumSamplesType.Status, "phase"),
				equalTo("Total"));
		assertThat("Datum opState",
				d.asSampleOperations().getSampleInteger(DatumSamplesType.Status, "opState"),
				equalTo(DeviceOperatingState.Normal.getCode()));
		assertThat("Datum sunsOpState",
				d.asSampleOperations().getSampleInteger(DatumSamplesType.Status, "sunsOpState"),
				equalTo(InverterOperatingState.Mppt.getCode()));
		assertThat("Datum events",
				d.asSampleOperations().getSampleInteger(DatumSamplesType.Status, "events"), equalTo(0));
		assertThat("Datum vendor events",
				d.asSampleOperations().getSampleString(DatumSamplesType.Status, "vendorEvents"),
				is(equalTo("0x70008000500060003000400010002")));

		DatumSamplesOperations ops = d.asSampleOperations();
		assertThat("Datum voltage phase A",
				ops.getSampleFloat(DatumSamplesType.Instantaneous, "voltage_a"),
				is(equalTo(247.40001f)));
		assertThat("Datum voltage phase B",
				ops.getSampleFloat(DatumSamplesType.Instantaneous, "voltage_b"),
				is(equalTo(250.90001f)));
		assertThat("Datum voltage phase C",
				ops.getSampleFloat(DatumSamplesType.Instantaneous, "voltage_c"), is(equalTo(246.1f)));
		assertThat("Datum voltage phase AB",
				ops.getSampleFloat(DatumSamplesType.Instantaneous, "voltage_ab"), is(equalTo(431.0f)));
		assertThat("Datum voltage phase BC",
				ops.getSampleFloat(DatumSamplesType.Instantaneous, "voltage_bc"), is(equalTo(427.0f)));
		assertThat("Datum voltage phase CA",
				ops.getSampleFloat(DatumSamplesType.Instantaneous, "voltage_ca"), is(equalTo(426.2f)));
		assertThat("Datum current phase A",
				ops.getSampleFloat(DatumSamplesType.Instantaneous, "current_a"),
				is(equalTo(0.19999999f)));
		assertThat("Datum voltcurrentage phase B",
				ops.getSampleFloat(DatumSamplesType.Instantaneous, "current_b"), is(equalTo(0.17f)));
		assertThat("Datum current phase C",
				ops.getSampleFloat(DatumSamplesType.Instantaneous, "current_c"),
				is(equalTo(0.32999998f)));
	}

}
