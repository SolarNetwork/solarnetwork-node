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
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.util.List;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.node.datum.sunspec.inverter.SunSpecInverterDatumDataSource;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.hw.sunspec.inverter.InverterOperatingState;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.settings.SettingSpecifier;
import net.solarnetwork.node.settings.TitleSettingSpecifier;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link SunSpecInverterDatumDataSource} class.
 * 
 * @author matt
 * @version 1.0
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
		Capture<ModbusConnectionAction<Object>> connActionCapture = new Capture<>();
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
	public void collectDatum() throws IOException {
		// GIVEN
		expectStaticDataModbusConnection("test-data-113-01.txt");

		// WHEN
		replayAll();
		long start = System.currentTimeMillis();
		GeneralNodeDatum d = dataSource.readCurrentDatum();

		// THEN
		assertThat("Datum returned", d, notNullValue());
		assertThat("Datum source ID", d.getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Datum created now", d.getCreated().getTime(), greaterThanOrEqualTo(start));
		assertThat("Datum frequency", d.getInstantaneousSampleFloat("frequency"), equalTo(50.05f));
		assertThat("Datum voltage", d.getInstantaneousSampleFloat("voltage"), equalTo(248.13335f));
		assertThat("Datum current", d.getInstantaneousSampleFloat("current"), equalTo(0.7f));
		assertThat("Datum power factor", d.getInstantaneousSampleFloat("powerFactor"), equalTo(1.0f));
		assertThat("Datum apparent power", d.getInstantaneousSampleInteger("apparentPower"),
				equalTo(70));
		assertThat("Datum reactive power", d.getInstantaneousSampleInteger("reactivePower"), equalTo(0));
		assertThat("Datum DC voltage", d.getInstantaneousSampleFloat("dcVoltage"), equalTo(406.9f));
		assertThat("Datum DC power", d.getInstantaneousSampleInteger("dcPower"), equalTo(61));
		assertThat("Datum power", d.getInstantaneousSampleInteger("watts"), equalTo(70));
		assertThat("Datum energy", d.getAccumulatingSampleLong("wattHours"), equalTo(11937020L));
		assertThat("Datum status", d.getStatusSampleString("phase"), equalTo("Total"));
		assertThat("Datum opState", d.getStatusSampleInteger("opState"),
				equalTo(DeviceOperatingState.Normal.getCode()));
		assertThat("Datum sunsOpState", d.getStatusSampleInteger("sunsOpState"),
				equalTo(InverterOperatingState.Mppt.getCode()));
		assertThat("Datum eve ts", d.getStatusSampleInteger("events"), equalTo(0));
	}

}
