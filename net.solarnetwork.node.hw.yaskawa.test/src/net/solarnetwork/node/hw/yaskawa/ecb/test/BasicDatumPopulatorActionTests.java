/* ==================================================================
 * BasicDatumPopulatorActionTests.java - 5/10/2022 12:23:35 pm
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

package net.solarnetwork.node.hw.yaskawa.ecb.test;

import static java.lang.String.format;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.io.IOException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.DcEnergyDatum;
import net.solarnetwork.node.domain.datum.AcDcEnergyDatum;
import net.solarnetwork.node.hw.yaskawa.ecb.BasicDatumPopulatorAction;
import net.solarnetwork.node.hw.yaskawa.ecb.PVI3800Command;
import net.solarnetwork.node.hw.yaskawa.ecb.PacketEnvelope;
import net.solarnetwork.node.io.serial.SerialConnection;

/**
 * Test cases for the {@link BasicDatumPopulatorAction} class.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicDatumPopulatorActionTests {

	private static final int TEST_UNIT_ID = 1;
	private static final String TEST_SOURCE_ID = "test.source";

	private SerialConnection conn;

	@Before
	public void setup() {
		conn = EasyMock.createMock(SerialConnection.class);
	}

	@After
	public void teardown() {
		EasyMock.verify(conn);
	}

	private void replayAll() {
		EasyMock.replay(conn);
	}

	private static byte[] decodeHex(String hexData) {
		if ( hexData == null ) {
			return null;
		}
		try {
			return Hex.decodeHex(hexData.replaceAll("\\s+", "").toCharArray());
		} catch ( DecoderException e ) {
			throw new RuntimeException(e);
		}
	}

	private void expectMessage(PVI3800Command cmd, String resp) throws IOException {
		conn.writeMessage(cmd.request(TEST_UNIT_ID).getBytes());
		final byte[] header = decodeHex(resp.substring(0, 8));
		expect(conn.readMarkedMessage(new byte[] { PacketEnvelope.Start.getCode() }, 4))
				.andReturn(header);
		final byte[] body = decodeHex(resp.substring(8));
		expect(conn.readMarkedMessage(new byte[] { cmd.getCommand(), cmd.getSubCommand() }, body.length))
				.andReturn(body);
	}

	@Test
	public void populate() throws IOException {
		// GIVEN
		expectMessage(PVI3800Command.MeterReadAcCombinedActivePower, "020601044704002a42bb03");
		expectMessage(PVI3800Command.MeterReadLifetimeTotalEnergy, "0206010a1901000000000030ab80776803");
		expectMessage(PVI3800Command.MeterReadAcCombinedFrequency, "0206010447021771ecb103");
		expectMessage(PVI3800Command.MeterReadAcCombinedCurrent, "0206010447030012f2a803");
		expectMessage(PVI3800Command.MeterReadAcCombinedVoltage, "02060104470100e4d32e03");
		expectMessage(PVI3800Command.MeterReadAcCombinedPowerFactor, "020601044706FC1B63AF03");
		expectMessage(PVI3800Command.MeterReadAcCombinedReactivePower, "020601044705FE9652aa03");
		expectMessage(PVI3800Command.MeterReadTemperatureAmbient, "020601040301FFFC862403");
		expectMessage(PVI3800Command.MeterReadTemperatureHeatsink, "0206010403020021f78d03");

		expectMessage(PVI3800Command.MeterReadPv1Voltage, "020601041C0101A501aa03");
		expectMessage(PVI3800Command.MeterReadPv1Current, "020601041C02002Eb05d03");
		expectMessage(PVI3800Command.MeterReadPv1Power, "020601041c050070806403");

		expectMessage(PVI3800Command.MeterReadPv2Voltage, "02060104240101A50cca03");
		expectMessage(PVI3800Command.MeterReadPv2Current, "020601042402002Ebd3d03");
		expectMessage(PVI3800Command.MeterReadPv2Power, "02060104240500288cfe03");

		expectMessage(PVI3800Command.MeterReadPv3Voltage, "020601042C0101A50eaa03");
		expectMessage(PVI3800Command.MeterReadPv3Current, "020601042C02002Ebf5d03");
		expectMessage(PVI3800Command.MeterReadPv3Power, "020601042C0500288e9e03");

		expectMessage(PVI3800Command.MeterReadPv1Status, "020601061A0100000004b25503");
		expectMessage(PVI3800Command.MeterReadPv2Status, "02060106220100000006372c03");
		expectMessage(PVI3800Command.MeterReadPv3Status, "020601062A0100000007f7a403");

		expectMessage(PVI3800Command.MeterReadPv1IsoStatus, "020601061A0200000002765703");
		expectMessage(PVI3800Command.MeterReadPv2IsoStatus, "02060106220200000004f2ed03");
		expectMessage(PVI3800Command.MeterReadPv3IsoStatus, "020601062A0200000008f3a003");

		expectMessage(PVI3800Command.MeterReadAc1Status, "02060106320100000001747e03");
		expectMessage(PVI3800Command.MeterReadAc2Status, "02060106390100000010b50903");
		expectMessage(PVI3800Command.MeterReadAc3Status, "02060106400100000400bdac03");

		// WHEN
		replayAll();
		AcDcEnergyDatum result = new BasicDatumPopulatorAction(TEST_UNIT_ID, TEST_SOURCE_ID)
				.doWithConnection(conn);

		// THEN
		assertThat("Datum created", result, is(notNullValue()));
		assertThat("AC power", result.getWatts(), is(equalTo(42)));
		assertThat("Total energy", result.getWattHourReading(), is(equalTo(3189632L)));
		assertThat("AC freq", result.getFrequency(), is(equalTo(60.01f)));
		assertThat("AC current", result.getCurrent(), is(equalTo(1.8f)));
		assertThat("AC voltage", result.getVoltage(), is(equalTo(228f)));
		assertThat("AC power factor", result.getPowerFactor(), is(equalTo(-0.997f)));
		assertThat("AC reactive power", result.getReactivePower(), is(equalTo(-362)));
		assertThat("Ambient temp",
				result.asSampleOperations().getSampleInteger(DatumSamplesType.Instantaneous, "temp"),
				is(equalTo(-4)));
		assertThat("Heat sink temp", result.asSampleOperations()
				.getSampleInteger(DatumSamplesType.Instantaneous, "temp_heatSink"), is(equalTo(33)));

		for ( int i = 1; i <= 3; i++ ) {
			assertThat(format("PV %d voltage", i),
					result.asSampleOperations().getSampleInteger(DatumSamplesType.Instantaneous,
							format("%s_%d", DcEnergyDatum.DC_VOLTAGE_KEY, i)),
					is(equalTo(421)));
			assertThat(format("PV %d current", i),
					result.asSampleOperations().getSampleFloat(DatumSamplesType.Instantaneous,
							format("%s_%d", DcEnergyDatum.DC_CURRENT_KEY, i)),
					is(equalTo(4.6f)));
			assertThat(format("PV %d power", i),
					result.asSampleOperations().getSampleInteger(DatumSamplesType.Instantaneous,
							format("%s_%d", DcEnergyDatum.DC_POWER_KEY, i)),
					is(equalTo((i == 1 ? 112 : 40))));
		}

		assertThat("PV voltage", result.getDcVoltage(), is(equalTo((421f * 3) / 3f)));
		assertThat("PV current", result.getDcCurrent(), is(equalTo(4.6f * 3)));
		assertThat("PV power", result.getDcPower(), is(equalTo(112 + 40 + 40)));

		assertThat("PV status 1",
				result.asSampleOperations().getSampleInteger(DatumSamplesType.Status, "status_pv1"),
				is(equalTo(0x4)));
		assertThat("PV status 2",
				result.asSampleOperations().getSampleInteger(DatumSamplesType.Status, "status_pv2"),
				is(equalTo(0x6)));
		assertThat("PV status 3",
				result.asSampleOperations().getSampleInteger(DatumSamplesType.Status, "status_pv3"),
				is(equalTo(0x7)));

		assertThat("PV ISO status 1",
				result.asSampleOperations().getSampleInteger(DatumSamplesType.Status, "status_pvIso1"),
				is(equalTo(0x2)));
		assertThat("PV ISO status 2",
				result.asSampleOperations().getSampleInteger(DatumSamplesType.Status, "status_pvIso2"),
				is(equalTo(0x4)));
		assertThat("PV ISO status 3",
				result.asSampleOperations().getSampleInteger(DatumSamplesType.Status, "status_pvIso3"),
				is(equalTo(0x8)));

		assertThat("AC status 1",
				result.asSampleOperations().getSampleInteger(DatumSamplesType.Status, "status_ac1"),
				is(equalTo(0x1)));
		assertThat("AC status 2",
				result.asSampleOperations().getSampleInteger(DatumSamplesType.Status, "status_ac2"),
				is(equalTo(0x10)));
		assertThat("AC status 3",
				result.asSampleOperations().getSampleInteger(DatumSamplesType.Status, "status_ac3"),
				is(equalTo(0x400)));

		assertThat("Events",
				result.asSampleOperations().getSampleInteger(DatumSamplesType.Status, "events"),
				is(equalTo(0x501))); // 0, 8, 11 
		assertThat("Vendor events",
				result.asSampleOperations().getSampleString(DatumSamplesType.Status, "vendorEvents"),
				is(equalTo("0x4110000000e00000007")));
	}
}
