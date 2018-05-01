/* ==================================================================
 * SI60KTLCTDataTests.java - 23/11/2017 5:10:24 pm
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.csi.inverter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;

/**
 * Unit tests for the {@link SI60KTLCTData} class.
 * 
 * @author Max Duncan
 * @version 1.0
 */
public class SI60KTLCTDataTests {

	// TODO replace this with actual data read from an active device
	private static final int[] TEST_DATA = new int[] { 0x4031, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0001, 0x0002, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000,
			0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000 };

	private final SI60KTLCTData data = new SI60KTLCTData();

	@Before
	public void setup() {
		data.performUpdates(new ModbusDataUpdateAction() {

			@Override
			public boolean updateModbusData(MutableModbusData m) {
				m.saveDataArray(TEST_DATA, SI60KTLCTData.ADDR_START);
				return true;
			}
		});
	}

	@Test
	public void getDeviceModel() {
		Assert.assertEquals(Integer.valueOf(16433), data.getDeviceModel());
	}

	@Test
	public void getActivePower() {
		Assert.assertEquals(Integer.valueOf(1), data.getActivePower());
	}

	@Test
	public void getApparentPower() {
		Assert.assertEquals(Integer.valueOf(2), data.getApparentPower());
	}

	// TODO test other values when populated

}
