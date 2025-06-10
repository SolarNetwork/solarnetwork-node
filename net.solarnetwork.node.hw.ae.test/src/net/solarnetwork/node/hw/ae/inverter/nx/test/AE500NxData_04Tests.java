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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
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
import net.solarnetwork.node.hw.ae.inverter.nx.AE500NxFault1;
import net.solarnetwork.node.hw.ae.inverter.nx.AE500NxFault2;
import net.solarnetwork.node.hw.ae.inverter.nx.AE500NxFault3;
import net.solarnetwork.node.hw.ae.inverter.nx.AE500NxSystemLimit;
import net.solarnetwork.node.hw.ae.inverter.nx.AE500NxSystemStatus;
import net.solarnetwork.node.hw.ae.inverter.nx.AE500NxWarning;
import net.solarnetwork.node.hw.ae.inverter.nx.AE500NxWarning1;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.support.StaticDataMapReadonlyModbusConnection;

/**
 * Test cases for reading AE500Nx sample data.
 * 
 * @author matt
 * @version 1.1
 */
public class AE500NxData_04Tests {

	static ModbusConnection conn = null;

	@BeforeClass
	public static void setupStatic() throws IOException {
		final int[] data = parseModbusHexRegisterLines(new BufferedReader(new InputStreamReader(
				AE500NxData_04Tests.class.getResourceAsStream("test-data-500nx-04.txt"))));
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
				equalTo(DeviceOperatingState.Standby));
	}

	@Test
	public void faults() {
		SortedSet<AE500NxFault> faults = data.getFaults();
		assertThat("Faults", faults,
				contains(AE500NxFault1.DspWatchdog, AE500NxFault1.PosCurrent, AE500NxFault1.FrequencyLow,
						AE500NxFault2.CommonMode, AE500NxFault2.OverPower, AE500NxFault3.PhaseALow,
						AE500NxFault3.PhaseBLow));
	}

	@Test
	public void systemLimits() {
		Set<AE500NxSystemLimit> limits = data.getSystemLimits();
		assertThat("Limits", limits, containsInAnyOrder(AE500NxSystemLimit.Iac, AE500NxSystemLimit.Pac,
				AE500NxSystemLimit.Headroom, AE500NxSystemLimit.MaxPowerInhibit));
	}

	@Test
	public void systemStatus() {
		Set<AE500NxSystemStatus> status = data.getSystemStatus();
		assertThat("Status", status, containsInAnyOrder(AE500NxSystemStatus.Autostart,
				AE500NxSystemStatus.Mppt, AE500NxSystemStatus.Sleep));
	}

	@Test
	public void warnings() {
		SortedSet<AE500NxWarning> warnings = data.getWarnings();
		assertThat("Warnings", warnings,
				contains(AE500NxWarning1.Fan1, AE500NxWarning1.Fan5, AE500NxWarning1.Fan8));
	}

}
