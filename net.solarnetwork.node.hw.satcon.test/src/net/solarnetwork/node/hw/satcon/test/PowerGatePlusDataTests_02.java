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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.DeviceOperatingState;
import net.solarnetwork.node.hw.satcon.PowerGateOperatingState;
import net.solarnetwork.node.hw.satcon.PowerGatePlusData;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;
import net.solarnetwork.node.test.DataUtils;

/**
 * Test cases for the {@link PowerGatePlusData} class.
 * 
 * @author matt
 * @version 2.0
 */
public class PowerGatePlusDataTests_02 {

	private PowerGatePlusData data;

	private static PowerGatePlusData dataInstance(String resource) {
		PowerGatePlusData data = new PowerGatePlusData();
		try {
			data.performUpdates(new ModbusDataUpdateAction() {

				@Override
				public boolean updateModbusData(MutableModbusData m) {
					try {
						m.saveDataMap(DataUtils.parseModbusHexRegisterMappingLines(
								new BufferedReader(new InputStreamReader(
										PowerGatePlusDataTests_02.class.getResourceAsStream(resource),
										Charset.forName("UTF-8")))));
					} catch ( IOException e ) {
						throw new RuntimeException(e);
					}
					;
					return true;
				}
			});
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		return data;
	}

	@Before
	public void setup() {
		data = dataInstance("test-satcon-data-02.txt");
	}

	@Test
	public void firmwareVersion() {
		assertThat("Firware version", data.getFirmwareVersion(), equalTo("6.46"));
	}

	@Test
	public void serialNumber() {
		assertThat("Firware version", data.getSerialNumber(), equalTo("304109A-001"));
	}

	@Test
	public void getFaults() {
		assertThat("Firware version", data.getFaults(), hasSize(0));
	}

	@Test
	public void operatingState() {
		assertThat("Operating state", data.getOperatingState(), equalTo(PowerGateOperatingState.Run));
	}

	@Test
	public void deviceOperatingState() {
		assertThat("Device operating state", data.getDeviceOperatingState(),
				equalTo(DeviceOperatingState.Normal));
	}

	@Test
	public void frequency() {
		assertThat("Frequency", data.getFrequency(), equalTo(59.96f));
	}

	@Test
	public void current() {
		assertThat("Current", data.getCurrent(), equalTo(6f));
	}

	@Test
	public void neutralCurrent() {
		assertThat("Neutral current", data.getNeutralCurrent(), equalTo(0.1f));
	}

	@Test
	public void voltage() {
		assertThat("Voltage", data.getVoltage(), equalTo(279f));
	}

	@Test
	public void lineVoltage() {
		assertThat("Line voltage", data.getLineVoltage(), equalTo(279f));
	}

	@Test
	public void powerFactor() {
		assertThat("Power factor", data.getPowerFactor(), equalTo(0.928f));
	}

	@Test
	public void activePower() {
		assertThat("Active power", data.getActivePower(), equalTo(5200));
	}

	@Test
	public void aoparentPower() {
		assertThat("Apparent power", data.getApparentPower(), equalTo(5600));
	}

	@Test
	public void reactivePower() {
		assertThat("Reactive power", data.getReactivePower(), equalTo(-200));
	}

	@Test
	public void activeEnergyDelivered() {
		// 979 + 660000 + 922000000
		assertThat("Active energy delivered", data.getActiveEnergyDelivered(), equalTo(922660979L));
	}

	@Test
	public void activeEnergyDeliveredToday() {
		assertThat("Active energy delivered today", data.getActiveEnergyDeliveredToday(),
				equalTo(2000L));
	}

	@Test
	public void dcVoltage() {
		assertThat("DC voltage", data.getDcVoltage(), equalTo(387f));
	}

	@Test
	public void dcPower() {
		// 387 * 13
		assertThat("DC power", data.getDcPower(), equalTo(5031));
	}

	@Test
	public void internalTemperature() {
		assertThat("Internal temperature", data.getInternalTemperature(), equalTo(16f));
	}

	@Test
	public void inverterTemperature() {
		assertThat("Inverter temperature", data.getInverterTemperature(), equalTo(28f));
	}

	@Test
	public void heatsinkTemperatures() {
		assertThat("Heatsink temperature 1", data.getHeatsinkTemperature(1), equalTo(12f));
		for ( int i = 2; i <= 6; i++ ) {
			assertThat("Heatsink temperature " + i, data.getHeatsinkTemperature(i), equalTo(0f));
		}
	}

}
