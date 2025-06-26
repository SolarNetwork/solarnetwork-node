/* ==================================================================
 * ADAM411xDataTests.java - 22/11/2018 7:16:47 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.advantech.adam.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Map;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.hw.advantech.adam.ADAM411xData;
import net.solarnetwork.node.hw.advantech.adam.InputRangeType;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;
import net.solarnetwork.node.test.DataUtils;

/**
 * Test cases for the {@link ADAM411xData} class.
 * 
 * @author matt
 * @version 1.1
 */
public class ADAM411xDataTests {

	private static final Logger log = LoggerFactory.getLogger(ADAM411xDataTests.class);

	private static Map<Integer, Integer> parseTestData(String resource) {
		try {
			return DataUtils.parseModbusHexRegisterMappingLines(new BufferedReader(
					new InputStreamReader(ADAM411xDataTests.class.getResourceAsStream(resource))));
		} catch ( IOException e ) {
			log.error("Error reading modbus data resource [{}]", resource, e);
			return Collections.emptyMap();
		}
	}

	private ADAM411xData getDataInstance(String resource) {
		Map<Integer, Integer> registers = parseTestData(resource);
		ADAM411xData data = new ADAM411xData();
		try {
			data.performUpdates(new ModbusDataUpdateAction() {

				@Override
				public boolean updateModbusData(MutableModbusData m) {
					m.saveDataMap(registers);
					return true;
				}
			});
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		return data;
	}

	@Test
	public void adam4117_01() {
		ADAM411xData data = getDataInstance("test-4117-01.txt");
		assertThat("Model", data.getModelName(), equalTo("4117"));
		assertThat("Firmware revision", data.getFirmwareRevision(), equalTo("A104"));
		assertThat("Enabled channels", data.getEnabledChannelNumbers(),
				contains(0, 1, 2, 3, 4, 5, 6, 7));
		for ( int i = 0; i < 8; i++ ) {
			assertThat("Channel " + i + " type", data.getChannelType(i),
					equalTo(InputRangeType.ZeroToOneHundredFiftyMilliVolts));
		}
		BigDecimal[] expected = new BigDecimal[] {
			// @formatter:off
			new BigDecimal("0.00157"), // 684; (684 / 65535) * 150
			new BigDecimal("0.00166"), // 724
			new BigDecimal("0.00178"), // 778
			// @formatter:on
		};
		for ( int i = 0; i < expected.length; i++ ) {
			BigDecimal val = data.getChannelValue(i).setScale(5, RoundingMode.HALF_UP);
			assertThat("Channel " + i + " value", val, equalTo(expected[i]));
		}
	}

	@Test
	public void adam4117_02() {
		ADAM411xData data = getDataInstance("test-4117-02.txt");
		assertThat("Model", data.getModelName(), equalTo("4117"));
		assertThat("Firmware revision", data.getFirmwareRevision(), equalTo("A104"));
		assertThat("Enabled channels", data.getEnabledChannelNumbers(), contains(0, 1, 2, 3));
		for ( int i = 0; i < 8; i++ ) {
			assertThat("Channel " + i + " type", data.getChannelType(i),
					equalTo(InputRangeType.PlusMinusOneHundredFiftyMilliVolts));
		}
		BigDecimal[] expected = new BigDecimal[] {
			// @formatter:off
			new BigDecimal("0.00230"), // 33271; (((33271 / 65535) * 300) - 150) / 1000
			new BigDecimal("0.00306"), // 33437; (((33437 / 65535) * 300) - 150) / 1000
			new BigDecimal("0.00290"), // 33402; (((33402 / 65535) * 300) - 150) / 1000
			new BigDecimal("0.00257"), // 33330; (((33330 / 65535) * 300) - 150) / 1000
			// @formatter:on
		};
		for ( int i = 0; i < expected.length; i++ ) {
			BigDecimal val = data.getChannelValue(i).setScale(5, RoundingMode.HALF_UP);
			assertThat("Channel " + i + " value", val, equalTo(expected[i]));
		}
	}

	@Test
	public void adam4118_01() {
		ADAM411xData data = getDataInstance("test-4118-01.txt");
		assertThat("Model", data.getModelName(), equalTo("4118"));
		assertThat("Firmware revision", data.getFirmwareRevision(), equalTo("A106"));
		assertThat("Enabled channels", data.getEnabledChannelNumbers(),
				contains(0, 1, 2, 3, 4, 5, 6, 7));
		InputRangeType[] expectedTypes = new InputRangeType[] {
				// @formatter:off
				InputRangeType.TypeTThermocouple,
				InputRangeType.TypeTThermocouple,
				InputRangeType.PlusMinusFifteenMilliVolts,
				InputRangeType.FourToTwentyMilliAmps,
				InputRangeType.FourToTwentyMilliAmps,
				InputRangeType.PlusMinusFifteenMilliVolts,
				InputRangeType.PlusMinusFifteenMilliVolts,
				InputRangeType.PlusMinusFifteenMilliVolts,
				// @formatter:on
		};
		for ( int i = 0; i < 8; i++ ) {
			assertThat("Channel " + i + " type", data.getChannelType(i), equalTo(expectedTypes[i]));
		}
		BigDecimal[] expected = new BigDecimal[] {
				// @formatter:off
				new BigDecimal("7.49218"),  // 14089; (14089 / 65535) * 500 - 100
				new BigDecimal("7.88892"),  // 14141; (14141 / 65535) * 500 - 100
				new BigDecimal("0.00049"),  // 33838; ((33838 / 65535) * 30 - 15) / 1000
				new BigDecimal("0.02000"),  // 65535; ((65535 / 65535) * 16 + 4) / 1000
				new BigDecimal("0.02000"),  // 65535; ((65535 / 65535) * 16 + 4) / 1000
				new BigDecimal("0.00056"),  // 33986; ((33986 / 65535) * 30 - 15) / 1000
				new BigDecimal("0.00381"),  // 41091; ((41091 / 65535) * 30 - 15) / 1000
				new BigDecimal("0.00335"),  // 40078; ((40078 / 65535) * 30 - 15) / 1000
				// @formatter:on
		};
		for ( int i = 0; i < expected.length; i++ ) {
			BigDecimal val = data.getChannelValue(i).setScale(5, RoundingMode.HALF_UP);
			assertThat("Channel " + i + " value", val, equalTo(expected[i]));
		}
	}

	@Test
	public void adam4118_02() {
		ADAM411xData data = getDataInstance("test-4118-02.txt");
		assertThat("Model", data.getModelName(), equalTo("4118"));
		assertThat("Firmware revision", data.getFirmwareRevision(), equalTo("A106"));
		assertThat("Enabled channels", data.getEnabledChannelNumbers(),
				contains(0, 1, 2, 3, 4, 5, 6, 7));
		InputRangeType[] expectedTypes = new InputRangeType[] {
				// @formatter:off
				InputRangeType.TypeTThermocouple,
				InputRangeType.TypeTThermocouple,
				InputRangeType.PlusMinusFifteenMilliVolts,
				InputRangeType.FourToTwentyMilliAmps,
				InputRangeType.FourToTwentyMilliAmps,
				InputRangeType.PlusMinusFifteenMilliVolts,
				InputRangeType.PlusMinusFifteenMilliVolts,
				InputRangeType.PlusMinusFifteenMilliVolts,
				// @formatter:on
		};
		for ( int i = 0; i < 8; i++ ) {
			assertThat("Channel " + i + " type", data.getChannelType(i), equalTo(expectedTypes[i]));
		}
		BigDecimal[] expected = new BigDecimal[] {
				// @formatter:off
				new BigDecimal("23.78882"), // 16225; (16225 / 65535) * 500 - 100
				new BigDecimal("25.78775"), // 16487; (16487 / 65535) * 500 - 100
				new BigDecimal("0.00193"),  // 36976; ((36976 / 65535) * 30 - 15) / 1000
				new BigDecimal("0.02000"),  // 65535; ((65535 / 65535) * 16 + 4) / 1000
				new BigDecimal("0.02000"),  // 65535; ((65535 / 65535) * 16 + 4) / 1000
				new BigDecimal("0.00278"),  // 38841; ((38841 / 65535) * 30 - 15) / 1000
				new BigDecimal("0.00442"),  // 42432; ((42432 / 65535) * 30 - 15) / 1000
				new BigDecimal("0.00373"),  // 40926; ((40926 / 65535) * 30 - 15) / 1000
				// @formatter:on
		};
		for ( int i = 0; i < expected.length; i++ ) {
			BigDecimal val = data.getChannelValue(i).setScale(5, RoundingMode.HALF_UP);
			assertThat("Channel " + i + " value", val, equalTo(expected[i]));
		}
	}

	@Test
	public void adam4118_34_01() {
		ADAM411xData data = getDataInstance("test-4118-34-01.txt");
		assertThat("Model", data.getModelName(), equalTo("4118"));
		assertThat("Firmware revision", data.getFirmwareRevision(), equalTo("A106"));
		assertThat("Enabled channels", data.getEnabledChannelNumbers(),
				contains(0, 1, 2, 3, 4, 5, 6, 7));
		InputRangeType[] expectedTypes = new InputRangeType[] {
				// @formatter:off
				InputRangeType.TypeTThermocouple,
				InputRangeType.TypeTThermocouple,
				InputRangeType.PlusMinusFifteenMilliVolts,
				InputRangeType.FourToTwentyMilliAmps,
				InputRangeType.FourToTwentyMilliAmps,
				InputRangeType.PlusMinusFifteenMilliVolts,
				InputRangeType.PlusMinusFifteenMilliVolts,
				InputRangeType.PlusMinusFifteenMilliVolts,
				// @formatter:on
		};
		for ( int i = 0; i < 8; i++ ) {
			assertThat("Channel " + i + " type", data.getChannelType(i), equalTo(expectedTypes[i]));
		}
		BigDecimal[] expected = new BigDecimal[] {
				// @formatter:off
				new BigDecimal("10.68132"), // 14507; (14507 / 65535) * 500 - 100
				new BigDecimal("24.78065"), // 16355; (16355 / 65535) * 500 - 100
				new BigDecimal("0.00616"),  // 46215; ((46215 / 65535) * 30 - 15) / 1000
				new BigDecimal("0.02000"),  // 65535; ((65535 / 65535) * 16 + 4) / 1000
				new BigDecimal("0.02000"),  // 65535; ((65535 / 65535) * 16 + 4) / 1000
				new BigDecimal("0.00520"),  // 44134; ((44134 / 65535) * 30 - 15) / 1000
				new BigDecimal("0.00390"),  // 41284; ((41284 / 65535) * 30 - 15) / 1000
				new BigDecimal("0.00339"),  // 40173; ((40173 / 65535) * 30 - 15) / 1000
				// @formatter:on
		};
		for ( int i = 0; i < expected.length; i++ ) {
			BigDecimal val = data.getChannelValue(i).setScale(5, RoundingMode.HALF_UP);
			assertThat("Channel " + i + " value", val, equalTo(expected[i]));
		}
	}

	@Test
	public void adam4117_58_02() {
		ADAM411xData data = getDataInstance("test-4117-58-02.txt");
		assertThat("Model", data.getModelName(), equalTo("4117"));
		assertThat("Firmware revision", data.getFirmwareRevision(), equalTo("A104"));
		assertThat("Enabled channels", data.getEnabledChannelNumbers(),
				contains(0, 1, 2, 3, 4, 5, 6, 7));
		InputRangeType[] expectedTypes = new InputRangeType[] {
				// @formatter:off
				InputRangeType.ZeroToOneHundredFiftyMilliVolts,
				InputRangeType.ZeroToOneHundredFiftyMilliVolts,
				InputRangeType.ZeroToOneHundredFiftyMilliVolts,
				InputRangeType.ZeroToOneHundredFiftyMilliVolts,
				InputRangeType.ZeroToOneHundredFiftyMilliVolts,
				InputRangeType.ZeroToOneHundredFiftyMilliVolts,
				InputRangeType.ZeroToOneHundredFiftyMilliVolts,
				InputRangeType.ZeroToOneHundredFiftyMilliVolts,
				// @formatter:on
		};
		for ( int i = 0; i < 8; i++ ) {
			assertThat("Channel " + i + " type", data.getChannelType(i), equalTo(expectedTypes[i]));
		}
		BigDecimal[] expected = new BigDecimal[] {
				// @formatter:off
				new BigDecimal("0.00216"), // 944; ((944 / 65535) * (150 - 0)) / 1000
				new BigDecimal("0.00245"), // 1072;((1072 / 65535) * (150 - 0)) / 1000
				new BigDecimal("0.00256"), // 1118; ((1118 / 65535) * (150 - 0)) / 1000
				new BigDecimal("0.00000"),
				new BigDecimal("0.00000"),
				new BigDecimal("0.00000"),
				new BigDecimal("0.00000"),
				new BigDecimal("0.00000"),
				// @formatter:on
		};
		for ( int i = 0; i < expected.length; i++ ) {
			BigDecimal val = data.getChannelValue(i).setScale(5, RoundingMode.HALF_UP);
			assertThat("Channel " + i + " value", val, equalTo(expected[i]));
		}
	}

}
