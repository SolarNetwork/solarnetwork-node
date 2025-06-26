/* ==================================================================
 * ModelDataFactoryTests.java - 22/05/2018 5:19:37 PM
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

package net.solarnetwork.node.hw.sunspec.test;

import static net.solarnetwork.domain.AcPhase.PhaseA;
import static net.solarnetwork.domain.AcPhase.PhaseB;
import static net.solarnetwork.domain.AcPhase.PhaseC;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelDataFactory;
import net.solarnetwork.node.hw.sunspec.ModelRegister;
import net.solarnetwork.node.hw.sunspec.meter.MeterModelAccessor;
import net.solarnetwork.node.hw.sunspec.meter.test.IntegerMeterModelAccessorTests;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.support.StaticDataMapReadonlyModbusConnection;
import net.solarnetwork.node.test.DataUtils;
import net.solarnetwork.util.ByteUtils;
import net.solarnetwork.util.IntShortMap;

/**
 * Test cases for the {@link ModelDataFactory} class.
 *
 * @author matt
 * @version 1.1
 */
public class ModelDataFactoryTests {

	public static final int[] SUNSPEC_START_00 = new int[] { 0x5375, 0x6E53 };

	private ModbusConnection conn;

	@Before
	public void setup() {
		conn = EasyMock.createMock(ModbusConnection.class);
	}

	@Test
	public void createIntegerMeterModel() throws IOException {
		expect(conn.getUnitId()).andReturn(1).anyTimes();

		// find base address
		expect(conn.readString(ModbusReadFunction.ReadHoldingRegister, 40000, 2, true, ByteUtils.ASCII))
				.andReturn(ModelRegister.BASE_ADDRESS_MAGIC_STRING);

		expect(conn.readWords(ModbusReadFunction.ReadHoldingRegister, 40002, 2))
				.andReturn(new short[] { 1, 65 });

		expect(conn.readWords(ModbusReadFunction.ReadHoldingRegister, 40004, 65))
				.andReturn(ModelDataTests.COMMON_MODEL_02);

		expect(conn.readWords(ModbusReadFunction.ReadHoldingRegister, 40069, 2))
				.andReturn(IntegerMeterModelAccessorTests.INT_METER_MODEL_HEADER_69);

		expect(conn.readWords(ModbusReadFunction.ReadHoldingRegister, 40176, 2))
				.andReturn(new short[] { (short) 0xFFFF, 0x0000 });

		expect(conn.readWords(ModbusReadFunction.ReadHoldingRegister, 40071, 105))
				.andReturn(IntegerMeterModelAccessorTests.INT_METER_MODEL_71);

		replay(conn);

		ModelData data = ModelDataFactory.getInstance().getModelData(conn, Integer.MAX_VALUE);

		verify(conn);

		assertThat("Model count", data.getModels(), hasSize(1));
		assertThat("Meter model", data.getModel(), instanceOf(MeterModelAccessor.class));

		MeterModelAccessor model = data.getTypedModel();
		assertThat("Energy export Total", model.getActiveEnergyExported(), equalTo(1090000L));
		assertThat("Energy export Phase A", model.accessorForPhase(PhaseA).getActiveEnergyExported(),
				equalTo(1009000L));
		assertThat("Energy export Phase B", model.accessorForPhase(PhaseB).getActiveEnergyExported(),
				equalTo(33600L));
		assertThat("Energy export Phase C", model.accessorForPhase(PhaseC).getActiveEnergyExported(),
				equalTo(47300L));

		assertThat("Energy import Total", model.getActiveEnergyImported(), equalTo(1001509000L));
		assertThat("Energy import Phase A", model.accessorForPhase(PhaseA).getActiveEnergyImported(),
				equalTo(350516800L));
		assertThat("Energy import Phase B", model.accessorForPhase(PhaseB).getActiveEnergyImported(),
				equalTo(273085000L));
		assertThat("Energy import Phase C", model.accessorForPhase(PhaseC).getActiveEnergyImported(),
				equalTo(377907200L));

	}

	@Test
	public void createModelForNonStandardAddress() throws IOException {
		// GIVEN
		Map<Integer, Integer> registers = DataUtils
				.parseModbusHexRegisterMappingLines(new BufferedReader(
						new InputStreamReader(getClass().getResourceAsStream("test-data-01.txt"))));
		IntShortMap map = new IntShortMap(registers.size());
		for ( Map.Entry<Integer, Integer> entry : registers.entrySet() ) {
			map.putValue(entry.getKey(), entry.getValue());
		}
		ModbusConnection conn = new StaticDataMapReadonlyModbusConnection(map);

		// WHEN
		ModelData data = ModelDataFactory.getInstance().getModelData(conn,
				ModelDataFactory.DEFAULT_MAX_READ_WORDS_COUNT, 1000);

		// THEN
		assertThat("Manufacturer", data.getManufacturer(), equalTo("Veris Industries"));
		assertThat("Model name", data.getModelName(), equalTo("E51C2"));
		assertThat("Options", data.getOptions(), equalTo("None"));
		assertThat("Version", data.getVersion(), equalTo("2.115"));
		assertThat("Serial number", data.getSerialNumber(), equalTo("4E4C3699"));
		assertThat("Device address", data.getDeviceAddress(), equalTo(7));
	}

	@Test(expected = IOException.class)
	public void findBaseAddress_noMagicBytes() throws IOException {
		// GIVEN
		IntShortMap map = new IntShortMap(8);
		ModbusConnection conn = new StaticDataMapReadonlyModbusConnection(map);

		// WHEN
		ModelDataFactory.getInstance().getModelData(conn, ModelDataFactory.DEFAULT_MAX_READ_WORDS_COUNT);
	}

	@Test(expected = IOException.class)
	public void findBaseAddress_nonMagicBytes() throws IOException {
		// GIVEN
		IntShortMap map = new IntShortMap(8);
		int i = 'a';
		for ( ModelRegister r : ModelRegister.BASE_ADDRESSES ) {
			map.putValue(r.getAddress(), (i++) << 8 | (i++));
			map.putValue(r.getAddress() + 1, (i++) << 8 | (i++));
		}
		ModbusConnection conn = new StaticDataMapReadonlyModbusConnection(map);

		// WHEN
		ModelDataFactory.getInstance().getModelData(conn, ModelDataFactory.DEFAULT_MAX_READ_WORDS_COUNT);
	}

	@Test(expected = IOException.class)
	public void fixedBaseAddress_noMagicBytes() throws IOException {
		// GIVEN
		IntShortMap map = new IntShortMap(8);
		ModbusConnection conn = new StaticDataMapReadonlyModbusConnection(map);

		// WHEN
		ModelDataFactory.getInstance().getModelData(conn, ModelDataFactory.DEFAULT_MAX_READ_WORDS_COUNT,
				0);
	}

}
