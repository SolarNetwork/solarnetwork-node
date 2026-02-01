/* ==================================================================
 * BaseModbusServerTests.java - 14/01/2026 6:32:56 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.modbus.server.impl.test;

import static java.math.RoundingMode.HALF_UP;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Completed;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Float32;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int32;
import static net.solarnetwork.node.io.modbus.ModbusDataUtils.encodeInt32;
import static net.solarnetwork.node.io.modbus.server.domain.MeasurementConfig.CONTROL_ID_AS_SOURCE_ID;
import static net.solarnetwork.node.reactor.InstructionHandler.TOPIC_SET_CONTROL_PARAMETER;
import static net.solarnetwork.node.reactor.InstructionUtils.createLocalInstruction;
import static net.solarnetwork.node.test.NodeTestUtils.randomInt;
import static net.solarnetwork.node.test.NodeTestUtils.randomLong;
import static net.solarnetwork.node.test.NodeTestUtils.randomShort;
import static net.solarnetwork.node.test.NodeTestUtils.randomString;
import static net.solarnetwork.util.NumberUtils.bigDecimalForNumber;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import net.solarnetwork.domain.NodeControlInfo;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusDataUtils;
import net.solarnetwork.node.io.modbus.ModbusRegisterBlockType;
import net.solarnetwork.node.io.modbus.server.domain.MeasurementConfig;
import net.solarnetwork.node.io.modbus.server.domain.ModbusRegisterData;
import net.solarnetwork.node.io.modbus.server.domain.RegisterBlockConfig;
import net.solarnetwork.node.io.modbus.server.domain.UnitConfig;
import net.solarnetwork.node.io.modbus.server.impl.BaseModbusServer;
import net.solarnetwork.node.reactor.Instruction;
import net.solarnetwork.node.reactor.InstructionStatus;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.node.service.OperationalModesService;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.test.CallingThreadExecutorService;
import net.solarnetwork.util.NumberUtils;

/**
 * Test cases for the {@link BaseModbusServer} class.
 *
 * @author matt
 * @version 1.1
 */
public class BaseModbusServerTests {

	private static class TestModbusServer extends BaseModbusServer<Object> {

		private TestModbusServer(Executor executor,
				ConcurrentMap<Integer, ModbusRegisterData> registers) {
			super(executor, registers);
		}

		@Override
		public String getSettingUid() {
			return "test";
		}

		@Override
		protected String description() {
			return "test";
		}

		@Override
		protected Object startServer() throws IOException {
			return new Object();
		}

		@Override
		protected void stopServer(Object server) {
			// nothing
		}

	}

	private Executor executor;
	private ConcurrentMap<Integer, ModbusRegisterData> registers;
	private TestModbusServer server;

	@Before
	public void setup() {
		executor = new CallingThreadExecutorService();
		registers = new ConcurrentHashMap<>(2, 0.9f, 2);
		server = new TestModbusServer(executor, registers);
	}

	private MeasurementConfig meas(String sourceId, String propertyName, ModbusDataType type,
			Integer decimalScale, String unitMultiplier, String controlId) {
		return meas(sourceId, propertyName, type, 0, decimalScale, unitMultiplier, controlId);
	}

	private MeasurementConfig meas(String sourceId, String propertyName, ModbusDataType type,
			int wordLength, Integer decimalScale, String unitMultiplier, String controlId) {
		var config = new MeasurementConfig();
		config.setSourceId(sourceId);
		config.setPropertyName(propertyName);
		config.setDataType(type);
		config.setWordLength(wordLength);
		config.setDecimalScale(decimalScale);
		if ( unitMultiplier != null ) {
			config.setUnitMultiplier(new BigDecimal(unitMultiplier));
		}
		config.setControlId(controlId);
		return config;
	}

	private RegisterBlockConfig block(ModbusRegisterBlockType type, int startAddress,
			MeasurementConfig[] measConfigs) {
		var config = new RegisterBlockConfig();
		config.setBlockType(type);
		config.setStartAddress(startAddress);
		config.setMeasurementConfigs(measConfigs);
		return config;
	}

	private UnitConfig unit(int unitId, RegisterBlockConfig[] blockConfigs) {
		var config = new UnitConfig();
		config.setUnitId(unitId);
		config.setRegisterBlockConfigs(blockConfigs);
		return config;
	}

	@Test
	public void readControl_Int32() {
		// GIVEN
		final String sourceId = randomString();
		final String propName = randomString();
		final Integer propVal = randomInt();
		final int unitId = 1;

		// @formatter:off
		final MeasurementConfig[] measConfigs = new MeasurementConfig[] {
			meas(sourceId, propName, Int32, 0, "1", CONTROL_ID_AS_SOURCE_ID)
		};
		final RegisterBlockConfig[] blockConfigs = new RegisterBlockConfig[] {
			block(ModbusRegisterBlockType.Holding, 0, measConfigs)
		};
		final UnitConfig[] unitConfigs = new UnitConfig[] {
			unit(unitId, blockConfigs)
		};
		// @formatter:on

		server.setUnitConfigs(unitConfigs);

		// populate starting data
		ModbusRegisterData data = new ModbusRegisterData();
		data.writeHoldings(0, ModbusDataUtils.encodeInt32(propVal));
		registers.put(unitId, data);

		// WHEN
		NodeControlInfo result = server.getCurrentControlInfo(sourceId);

		// THEN
		// @formatter:off
		then(result)
			.as("Control value returned")
			.isNotNull()
			.as("Control ID returned")
			.returns(sourceId, from(NodeControlInfo::getControlId))
			.as("Control value as string returned")
			.returns(propVal.toString(), from(NodeControlInfo::getValue))
			;
		// @formatter:on
	}

	@Test
	public void readControl_Int32_unitMultiplier() {
		// GIVEN
		final String sourceId = randomString();
		final String propName = randomString();
		final Integer propVal = randomInt();
		final int unitId = 1;

		// @formatter:off
		final MeasurementConfig[] measConfigs = new MeasurementConfig[] {
			meas(sourceId, propName, Int32, 0, "10", CONTROL_ID_AS_SOURCE_ID)
		};
		final RegisterBlockConfig[] blockConfigs = new RegisterBlockConfig[] {
			block(ModbusRegisterBlockType.Holding, 0, measConfigs)
		};
		final UnitConfig[] unitConfigs = new UnitConfig[] {
			unit(unitId, blockConfigs)
		};
		// @formatter:on

		server.setUnitConfigs(unitConfigs);

		// populate starting data
		ModbusRegisterData data = new ModbusRegisterData();
		data.writeHoldings(0, ModbusDataUtils.encodeInt32(propVal));
		registers.put(unitId, data);

		// WHEN
		NodeControlInfo result = server.getCurrentControlInfo(sourceId);

		// THEN
		// @formatter:off
		then(result)
			.as("Control value returned")
			.isNotNull()
			.as("Control ID returned")
			.returns(sourceId, from(NodeControlInfo::getControlId))
			.as("Control value with reversed unit multiplier returned")
			.returns(NumberUtils.scaled(propVal, -1).toString(), from(NodeControlInfo::getValue))
			;
		// @formatter:on
	}

	@Test
	public void readControl_Float32_decimalScale() {
		// GIVEN
		final String sourceId = randomString();
		final String propName = randomString();
		final Float propVal = randomShort().floatValue() + 0.1f;
		final int unitId = 1;

		// @formatter:off
		final MeasurementConfig[] measConfigs = new MeasurementConfig[] {
			meas(sourceId, propName, Float32, 1, "1", CONTROL_ID_AS_SOURCE_ID)
		};
		final RegisterBlockConfig[] blockConfigs = new RegisterBlockConfig[] {
			block(ModbusRegisterBlockType.Holding, 0, measConfigs)
		};
		final UnitConfig[] unitConfigs = new UnitConfig[] {
			unit(unitId, blockConfigs)
		};
		// @formatter:on

		server.setUnitConfigs(unitConfigs);

		// populate starting data
		ModbusRegisterData data = new ModbusRegisterData();
		data.writeHoldings(0, ModbusDataUtils.encodeFloat32(propVal));
		registers.put(unitId, data);

		// WHEN
		NodeControlInfo result = server.getCurrentControlInfo(sourceId);

		// THEN
		// @formatter:off
		then(result)
			.as("Control value returned")
			.isNotNull()
			.as("Control ID returned")
			.returns(sourceId, from(NodeControlInfo::getControlId))
			.as("Control value with reversed unit multiplier returned")
			.returns(propVal.toString(), from(NodeControlInfo::getValue))
			;
		// @formatter:on
	}

	@Test
	public void writeControl_Int32() {
		// GIVEN
		final String sourceId = randomString();
		final String propName = randomString();
		final Integer propVal = randomInt();
		final int unitId = 1;

		// @formatter:off
		final MeasurementConfig[] measConfigs = new MeasurementConfig[] {
			meas(sourceId, propName, Int32, 0, "1", CONTROL_ID_AS_SOURCE_ID)
		};
		final RegisterBlockConfig[] blockConfigs = new RegisterBlockConfig[] {
			block(ModbusRegisterBlockType.Holding, 0, measConfigs)
		};
		final UnitConfig[] unitConfigs = new UnitConfig[] {
			unit(unitId, blockConfigs)
		};
		// @formatter:on

		server.setUnitConfigs(unitConfigs);

		// WHEN
		Instruction instr = createLocalInstruction(TOPIC_SET_CONTROL_PARAMETER, sourceId,
				propVal.toString());
		InstructionStatus result = server.processInstruction(instr);

		// THEN
		// @formatter:off
		then(result)
			.as("Status returned")
			.isNotNull()
			.as("Control ID returned")
			.returns(Completed, from(InstructionStatus::getInstructionState))
			;

		final ModbusRegisterData data = registers.get(unitId);
		then(data)
			.as("Value saved to register data")
			.isNotNull()
			.extracting(ModbusRegisterData::getHoldings)
			.as("Value saved as holding")
			.isNotNull()
			.returns(propVal, from(d -> d.getInt32(0)))
			;
		// @formatter:on
	}

	@Test
	public void writeControl_Int32_decimalScale_unitMultiplier() {
		// GIVEN
		final String sourceId = randomString();
		final String propName = randomString();
		final Float propVal = randomShort().floatValue() + 0.125f;
		final int unitId = 1;

		// @formatter:off
		final MeasurementConfig[] measConfigs = new MeasurementConfig[] {
			meas(sourceId, propName, Int32, 1, "10", CONTROL_ID_AS_SOURCE_ID)
		};
		final RegisterBlockConfig[] blockConfigs = new RegisterBlockConfig[] {
			block(ModbusRegisterBlockType.Holding, 0, measConfigs)
		};
		final UnitConfig[] unitConfigs = new UnitConfig[] {
			unit(unitId, blockConfigs)
		};
		// @formatter:on

		server.setUnitConfigs(unitConfigs);

		// WHEN
		Instruction instr = createLocalInstruction(TOPIC_SET_CONTROL_PARAMETER, sourceId,
				propVal.toString());
		InstructionStatus result = server.processInstruction(instr);
		NodeControlInfo roundtrip = server.getCurrentControlInfo(sourceId);

		// THEN
		// @formatter:off
		then(result)
			.as("Status returned")
			.isNotNull()
			.as("Control ID returned")
			.returns(Completed, from(InstructionStatus::getInstructionState))
			;

		final ModbusRegisterData data = registers.get(unitId);
		then(data)
			.as("Value saved to register data")
			.isNotNull()
			.extracting(ModbusRegisterData::getHoldings)
			.as("Value saved as holding")
			.isNotNull()
			.as("Property float scaled and multiplied into Int32 value")
			.returns(bigDecimalForNumber(propVal).setScale(1, HALF_UP).scaleByPowerOfTen(1).intValue(), from(d -> d.getInt32(0)))
			;

		then(roundtrip)
			.as("Control value returned")
			.isNotNull()
			.as("Control ID returned")
			.returns(sourceId, from(NodeControlInfo::getControlId))
			.as("Control value with reversed unit multiplier returned")
			.returns(bigDecimalForNumber(propVal).setScale(1, HALF_UP).toPlainString(), from(NodeControlInfo::getValue))
			;
		// @formatter:on
	}

	@Test
	public void writeControl_Utf8() {
		// GIVEN
		final String sourceId = randomString();
		final String propName = randomString();
		final String propVal = randomString();
		final int unitId = 1;

		// @formatter:off
		final MeasurementConfig[] measConfigs = new MeasurementConfig[] {
			meas(sourceId, propName, ModbusDataType.StringUtf8, 16, 1, "1", CONTROL_ID_AS_SOURCE_ID)
		};
		final RegisterBlockConfig[] blockConfigs = new RegisterBlockConfig[] {
			block(ModbusRegisterBlockType.Holding, 0, measConfigs)
		};
		final UnitConfig[] unitConfigs = new UnitConfig[] {
			unit(unitId, blockConfigs)
		};
		// @formatter:on

		server.setUnitConfigs(unitConfigs);

		// WHEN
		Instruction instr = createLocalInstruction(TOPIC_SET_CONTROL_PARAMETER, sourceId,
				propVal.toString());
		InstructionStatus result = server.processInstruction(instr);
		NodeControlInfo roundtrip = server.getCurrentControlInfo(sourceId);

		// THEN
		// @formatter:off
		then(result)
			.as("Status returned")
			.isNotNull()
			.as("Control ID returned")
			.returns(Completed, from(InstructionStatus::getInstructionState))
			;

		final ModbusRegisterData data = registers.get(unitId);
		then(data)
			.as("Value saved to register data")
			.isNotNull()
			.extracting(ModbusRegisterData::getHoldings)
			.as("Value saved as holding")
			.isNotNull()
			.as("Property encoded as Utf8 value")
			.returns(propVal, from(d -> d.getUtf8String(0, 16, true)))
			;

		then(roundtrip)
			.as("Control value returned")
			.isNotNull()
			.as("Control ID returned")
			.returns(sourceId, from(NodeControlInfo::getControlId))
			.as("Control value with reversed unit multiplier returned")
			.returns(propVal, from(NodeControlInfo::getValue))
			;
		// @formatter:on
	}

	@Test
	public void start_restrictedUnitIds() throws IOException {
		// GIVEN
		final String sourceId = randomString();
		final String propName = randomString();
		final int unitId = 1;

		// @formatter:off
		final MeasurementConfig[] measConfigs = new MeasurementConfig[] {
			meas(sourceId, propName, ModbusDataType.StringUtf8, 16, 1, "1", null)
		};
		final RegisterBlockConfig[] blockConfigs = new RegisterBlockConfig[] {
			block(ModbusRegisterBlockType.Holding, 0, measConfigs)
		};
		final UnitConfig[] unitConfigs = new UnitConfig[] {
			unit(unitId, blockConfigs)
		};
		// @formatter:on

		server.setUnitConfigs(unitConfigs);
		server.setRestrictUnitIds(true);

		// WHEN
		server.start();

		// THEN
		// @formatter:off
		then(registers)
			.as("Data for configured unit IDs created")
			.containsOnlyKeys(unitId)
			;
		// @formatter:on
	}

	@Test
	public void requiredOpMode_match() {
		// GIVEN
		final String sourceId = randomString();
		final String propName = randomString();
		final Integer propVal = randomInt();
		final int unitId = 1;

		// @formatter:off
		final MeasurementConfig[] measConfigs = new MeasurementConfig[] {
			meas(sourceId, propName, Int32, 0, "1", CONTROL_ID_AS_SOURCE_ID)
		};
		final RegisterBlockConfig[] blockConfigs = new RegisterBlockConfig[] {
			block(ModbusRegisterBlockType.Holding, 0, measConfigs)
		};
		final UnitConfig[] unitConfigs = new UnitConfig[] {
			unit(unitId, blockConfigs)
		};
		// @formatter:on

		server.setUnitConfigs(unitConfigs);

		final String reqMode = randomString();
		server.setRequiredOperationalMode(reqMode);

		final OperationalModesService opModesService = createMock(OperationalModesService.class);
		server.setOpModesService(new StaticOptionalService<>(opModesService));

		// test for required mode
		expect(opModesService.isOperationalModeActive(reqMode)).andReturn(true);

		// WHEN
		replay(opModesService);

		SimpleDatum datum = new SimpleDatum(DatumId.nodeId(randomLong(), sourceId, null),
				new DatumSamples(Map.of(propName, propVal), null, null));
		Event evt = new Event(DatumQueue.EVENT_TOPIC_DATUM_ACQUIRED,
				Map.of(DatumEvents.DATUM_PROPERTY, datum));
		server.handleEvent(evt);

		// THEN
		// @formatter:off
		then(registers)
			.as("Server data contains unit ID for updated measurement")
			.containsOnlyKeys(unitId)
			.extractingByKey(unitId)
			.as("Measurement data contains updated datum property value")
			.returns(encodeInt32(propVal), from(r -> r.readHoldings(0, 2)))
			;
		// @formatter:on

		verify(opModesService);
	}

	@Test
	public void requiredOpMode_noMatch() {
		// GIVEN
		final String sourceId = randomString();
		final String propName = randomString();
		final Integer propVal = randomInt();
		final int unitId = 1;

		// @formatter:off
		final MeasurementConfig[] measConfigs = new MeasurementConfig[] {
			meas(sourceId, propName, Int32, 0, "1", CONTROL_ID_AS_SOURCE_ID)
		};
		final RegisterBlockConfig[] blockConfigs = new RegisterBlockConfig[] {
			block(ModbusRegisterBlockType.Holding, 0, measConfigs)
		};
		final UnitConfig[] unitConfigs = new UnitConfig[] {
			unit(unitId, blockConfigs)
		};
		// @formatter:on

		server.setUnitConfigs(unitConfigs);

		final String reqMode = randomString();
		server.setRequiredOperationalMode(reqMode);

		final OperationalModesService opModesService = createMock(OperationalModesService.class);
		server.setOpModesService(new StaticOptionalService<>(opModesService));

		// test for required mode
		expect(opModesService.isOperationalModeActive(reqMode)).andReturn(false);

		// WHEN
		replay(opModesService);

		SimpleDatum datum = new SimpleDatum(DatumId.nodeId(randomLong(), sourceId, null),
				new DatumSamples(Map.of(propName, propVal), null, null));
		Event evt = new Event(DatumQueue.EVENT_TOPIC_DATUM_ACQUIRED,
				Map.of(DatumEvents.DATUM_PROPERTY, datum));
		server.handleEvent(evt);

		// THEN
		// @formatter:off
		then(registers)
			.as("Server data does not contain unit ID becauase op mode does not match so update ignored")
			.isEmpty()
			;
		// @formatter:on

		verify(opModesService);
	}

}
