/* ==================================================================
 * AE500NxData_01Tests.java - 22/04/2020 3:29:14 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.ae.inverter.nx.test;

import static net.solarnetwork.node.test.DataUtils.parseModbusHexRegisterLines;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.SortedSet;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.node.hw.ae.inverter.nx.AE500NxData;
import net.solarnetwork.node.hw.ae.inverter.nx.AE500NxFault;
import net.solarnetwork.node.hw.ae.inverter.nx.AE500NxSystemLimit;
import net.solarnetwork.node.hw.ae.inverter.nx.AE500NxSystemStatus;
import net.solarnetwork.node.hw.ae.inverter.nx.AE500NxWarning;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.support.StaticDataMapReadonlyModbusConnection;

/**
 * Test cases for reading AE500Nx sample data.
 * 
 * @author matt
 * @version 2.0
 */
public class AE500NxData_02Tests {

	static ModbusConnection conn = null;

	@BeforeClass
	public static void setupStatic() throws IOException {
		final int[] data = parseModbusHexRegisterLines(new BufferedReader(new InputStreamReader(
				AE500NxData_02Tests.class.getResourceAsStream("test-data-500nx-02.txt"))));
		conn = new StaticDataMapReadonlyModbusConnection(data);
	}

	private AE500NxData data;

	@Before
	public void setup() throws IOException {
		data = new AE500NxData();
		data.readConfigurationData(conn);
	}

	@Test
	public void deviceOperatingState() {
		assertThat("Device operating state", data.getDeviceOperatingState(),
				equalTo(DeviceOperatingState.Normal));
	}

	@Test
	public void faults() {
		SortedSet<AE500NxFault> faults = data.getFaults();
		assertThat("Faults", faults, empty());
	}

	@Test
	public void firmwareVersion() {
		assertThat("Firmware version", data.getFirmwareVersion(),
				equalTo("CHKSM: 0xd36c; APP: 7450015J.00; CFG: 7200082H.00; FPGA: 7/8/08"));
	}

	@Test
	public void serialNumber() {
		assertThat("Serial number", data.getSerialNumber(),
				equalTo("M/N 3159500-1000 AD; S/N 780348; F/R AD"));
	}

	@Test
	public void systemLimits() {
		Set<AE500NxSystemLimit> limits = data.getSystemLimits();
		assertThat("Limits", limits, empty());
	}

	@Test
	public void systemStatus() {
		Set<AE500NxSystemStatus> status = data.getSystemStatus();
		assertThat("Status", status, containsInAnyOrder(AE500NxSystemStatus.Autostart,
				AE500NxSystemStatus.Enabled, AE500NxSystemStatus.Mppt, AE500NxSystemStatus.Power));
	}

	@Test
	public void warnings() {
		SortedSet<AE500NxWarning> warnings = data.getWarnings();
		assertThat("Warnings", warnings, empty());
	}

	@Test
	public void dcPower() {
		assertThat("DC power", data.getDcPower(), equalTo((int) (45.1f * 757f)));
	}

	@Test
	public void dcVoltage() {
		assertThat("DC voltage", data.getDcVoltage(), equalTo(757f));
	}

	@Test
	public void activeEnergyDelivered() {
		assertThat("Active energy delivered", data.getActiveEnergyDelivered(), equalTo(215367200L));
	}

	@Test
	public void activeEnergyReceived() {
		assertThat("Active energy received", data.getActiveEnergyReceived(), nullValue());
	}

	@Test
	public void activePower() {
		assertThat("Active energy power", data.getActivePower(), equalTo(33700));
	}

	@Test
	public void apparentEnergyDelivered() {
		assertThat("Apparent energy delivered", data.getApparentEnergyDelivered(), nullValue());
	}

	@Test
	public void apparentEnergyReceived() {
		assertThat("Apparent energy received", data.getApparentEnergyReceived(), nullValue());
	}

	@Test
	public void apparentPower() {
		assertThat("Apparent power", data.getApparentPower(), nullValue());
	}

	@Test
	public void current() {
		assertThat("Current", data.getCurrent(), equalTo(38.6f));
	}

	@Test
	public void frequency() {
		assertThat("Frequency", data.getFrequency(), equalTo(59.96f));
	}

	@Test
	public void lineVoltage() {
		assertThat("Line voltage", data.getLineVoltage(), nullValue());
	}

	@Test
	public void neutralCurrent() {
		assertThat("Neutral current", data.getNeutralCurrent(), equalTo(0.74f));
	}

	@Test
	public void powerFactor() {
		assertThat("Power factor", data.getPowerFactor(), nullValue());
	}

	@Test
	public void reactiveEnergyDelivered() {
		assertThat("Reactive energy delivered", data.getReactiveEnergyDelivered(), nullValue());
	}

	@Test
	public void reactiveEnergyReceived() {
		assertThat("Reactive energy received", data.getReactiveEnergyReceived(), nullValue());
	}

	@Test
	public void reactivePower() {
		assertThat("Reactive power", data.getReactivePower(), equalTo(0));
	}

	@Test
	public void voltage() {
		assertThat("Voltage", data.getVoltage(), equalTo(5.5f));
	}

}
