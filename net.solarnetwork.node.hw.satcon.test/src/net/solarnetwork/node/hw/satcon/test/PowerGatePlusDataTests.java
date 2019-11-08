/* ==================================================================
 * PowerGatePlusDataTests.java - 8/11/2019 12:06:03 pm
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

package net.solarnetwork.node.hw.satcon.test;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashSet;
import org.junit.Test;
import net.solarnetwork.node.hw.satcon.Fault;
import net.solarnetwork.node.hw.satcon.PowerGateFault0;
import net.solarnetwork.node.hw.satcon.PowerGateFault3;
import net.solarnetwork.node.hw.satcon.PowerGateFault6;
import net.solarnetwork.node.hw.satcon.PowerGatePlusData;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;
import net.solarnetwork.node.test.DataUtils;

/**
 * Test cases for the {@link PowerGatePlusData} class.
 * 
 * @author matt
 * @version 1.0
 */
public class PowerGatePlusDataTests {

	private PowerGatePlusData dataInstance(String resource) {
		PowerGatePlusData data = new PowerGatePlusData();
		data.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				try {
					m.saveDataMap(DataUtils.parseModbusHexRegisterMappingLines(new BufferedReader(
							new InputStreamReader(getClass().getResourceAsStream(resource),
									Charset.forName("UTF-8")))));
				} catch ( IOException e ) {
					throw new RuntimeException(e);
				}
				;
				return true;
			}
		});
		return data;
	}

	@Test
	public void firmwareVersion() {
		PowerGatePlusData data = dataInstance("test-satcon-data-01.txt");
		assertThat("Firware version", data.getFirmwareVersion(), equalTo("6.46"));
	}

	@Test
	public void serialNumber() {
		PowerGatePlusData data = dataInstance("test-satcon-data-01.txt");
		assertThat("Firware version", data.getSerialNumber(), equalTo("304109A-001"));
	}

	@Test
	public void getFaults() {
		PowerGatePlusData data = dataInstance("test-satcon-data-01.txt");
		assertThat("Firware version", data.getFaults(),
				equalTo(new HashSet<>(asList((Fault) PowerGateFault0.DoorOpen,
						PowerGateFault3.ReactorOverTemperature, PowerGateFault6.FanFault2))));
	}

}
