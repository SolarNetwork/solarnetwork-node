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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.springframework.context.support.StaticMessageSource;
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
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.TitleSettingSpecifier;
import net.solarnetwork.test.CallingThreadExecutorService;
import net.solarnetwork.util.IntShortMap;
import net.solarnetwork.util.NumberUtils;

/**
 * Test cases for the {@link BaseModbusServer} class.
 *
 * @author matt
 * @version 1.2
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

	private void configureControl(int unitId, ModbusRegisterBlockType blockType, int address,
			MeasurementConfig measConfig) {
		// @formatter:off
		server.setUnitConfigs(new UnitConfig[] {
			unit(unitId, new RegisterBlockConfig[] {
				block(blockType, address, new MeasurementConfig[] { measConfig })
			})
		});
		// @formatter:on
	}

	/**
	 * Get a message source that renders the register info in a compact form.
	 *
	 * <p>
	 * Each block renders like {@code holding:(0x0=0x1234;0x1=0xABCD;)|} and
	 * {@code coil:1;3;|}.
	 * </p>
	 *
	 * @return the message source
	 */
	private static StaticMessageSource registerInfoMessageSource() {
		final StaticMessageSource ms = new StaticMessageSource();
		ms.setUseCodeAsDefaultMessage(true);
		final Locale l = Locale.getDefault();
		ms.addMessage("serverUnitInfo.title", l, "[unit {0}]");
		ms.addMessage("serverUnitInfo.coil.label", l, "coil");
		ms.addMessage("serverUnitInfo.discrete.label", l, "discrete");
		ms.addMessage("serverUnitInfo.holding.label", l, "holding");
		ms.addMessage("serverUnitInfo.input.label", l, "input");
		ms.addMessage("serverUnitInfoBitBlock.start", l, "{0}:");
		ms.addMessage("serverUnitInfoBit.row", l, "{0};");
		ms.addMessage("serverUnitInfoBitBlock.end", l, "|");
		ms.addMessage("serverUnitInfoIntBlock.start", l, "{0}:");
		ms.addMessage("serverUnitInfoInt.start", l, "(");
		ms.addMessage("serverUnitInfoInt.row", l, "{1}={2};");
		ms.addMessage("serverUnitInfoInt.end", l, ")");
		ms.addMessage("serverUnitInfoIntBlock.end", l, "|");
		return ms;
	}

	private String registerInfo() {
		for ( SettingSpecifier s : server.getSettingSpecifiers() ) {
			if ( s instanceof TitleSettingSpecifier t && "info".equals(t.getKey()) ) {
				return t.getDefaultValue();
			}
		}
		return null;
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
	public void readControl_Int32_input() {
		// GIVEN
		final String sourceId = randomString();
		final Integer propVal = randomInt();
		final int unitId = 1;
		configureControl(unitId, ModbusRegisterBlockType.Input, 0,
				meas(sourceId, randomString(), Int32, 0, "1", CONTROL_ID_AS_SOURCE_ID));

		ModbusRegisterData data = new ModbusRegisterData();
		data.writeInputs(0, encodeInt32(propVal));
		registers.put(unitId, data);

		// WHEN
		NodeControlInfo result = server.getCurrentControlInfo(sourceId);

		// THEN
		// @formatter:off
		then(result)
			.as("Control value returned")
			.isNotNull()
			.as("Input register value returned")
			.returns(propVal.toString(), from(NodeControlInfo::getValue))
			;
		// @formatter:on
	}

	@Test
	public void readControl_coil() {
		// GIVEN
		final String sourceId = randomString();
		final int unitId = 1;
		final int address = 3;
		configureControl(unitId, ModbusRegisterBlockType.Coil, address, meas(sourceId, randomString(),
				ModbusDataType.Boolean, null, null, CONTROL_ID_AS_SOURCE_ID));

		ModbusRegisterData data = new ModbusRegisterData();
		data.writeCoil(address, true);
		registers.put(unitId, data);

		// WHEN
		NodeControlInfo result = server.getCurrentControlInfo(sourceId);

		// THEN
		// @formatter:off
		then(result)
			.as("Control value returned")
			.isNotNull()
			.as("Coil value at non-zero address returned")
			.returns("true", from(NodeControlInfo::getValue))
			;
		// @formatter:on
	}

	@Test
	public void readControl_discrete() {
		// GIVEN
		final String sourceId = randomString();
		final int unitId = 1;
		final int address = 5;
		configureControl(unitId, ModbusRegisterBlockType.Discrete, address, meas(sourceId,
				randomString(), ModbusDataType.Boolean, null, null, CONTROL_ID_AS_SOURCE_ID));

		ModbusRegisterData data = new ModbusRegisterData();
		data.writeDiscrete(address, true);
		registers.put(unitId, data);

		// WHEN
		NodeControlInfo result = server.getCurrentControlInfo(sourceId);

		// THEN
		// @formatter:off
		then(result)
			.as("Control value returned")
			.isNotNull()
			.as("Discrete value at non-zero address returned")
			.returns("true", from(NodeControlInfo::getValue))
			;
		// @formatter:on
	}

	@Test
	public void readControl_Int32_concurrentWrites() throws InterruptedException {
		// GIVEN
		final String sourceId = randomString();
		final int unitId = 1;
		configureControl(unitId, ModbusRegisterBlockType.Holding, 0,
				meas(sourceId, randomString(), Int32, 0, "1", CONTROL_ID_AS_SOURCE_ID));

		// both 16-bit words differ between the values, so a read that mixes them is detectable
		final int val1 = 0x00010001;
		final int val2 = 0x00020002;
		final ModbusRegisterData data = new ModbusRegisterData();
		data.writeHoldings(0, encodeInt32(val1));
		registers.put(unitId, data);

		final AtomicBoolean reading = new AtomicBoolean(true);
		final Thread writer = new Thread(() -> {
			boolean first = false;
			while ( reading.get() ) {
				data.writeHoldings(0, encodeInt32(first ? val1 : val2));
				first = !first;
			}
		});

		// WHEN
		final Set<String> unexpected = new LinkedHashSet<>();
		writer.start();
		try {
			for ( int i = 0; i < 20_000; i++ ) {
				final String value = server.getCurrentControlInfo(sourceId).getValue();
				if ( !(String.valueOf(val1).equals(value) || String.valueOf(val2).equals(value)) ) {
					unexpected.add(value);
				}
			}
		} finally {
			reading.set(false);
		}
		writer.join();

		// THEN
		then(unexpected).as("Only whole written values read while registers are written").isEmpty();
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

	@Test
	public void settings_registerInfo() {
		// GIVEN
		server.setMessageSource(registerInfoMessageSource());

		ModbusRegisterData data = new ModbusRegisterData();
		data.writeCoil(1, true);
		data.writeCoil(3, true);
		data.writeDiscrete(2, true);
		data.writeHoldings(0, new short[] { 0x1234 });
		data.writeInputs(5, new short[] { (short) 0xABCD });
		registers.put(1, data);

		// WHEN
		String info = registerInfo();

		// THEN
		then(info).as("Register info lists the data of every register block")
				.isEqualTo("[unit 1]coil:1;3;|discrete:2;|holding:(0x0=0x1234;)|input:(0x5=0xABCD;)|");
	}

	@Test
	public void settings_registerInfo_concurrentWrites() throws InterruptedException {
		// GIVEN
		server.setMessageSource(registerInfoMessageSource());

		final ModbusRegisterData data = new ModbusRegisterData();
		registers.put(1, data);

		final int count = 200;
		final IntShortMap regs = data.getHoldings().dataRegisters();
		final AtomicBoolean reading = new AtomicBoolean(true);
		final Thread writer = new Thread(() -> {
			while ( reading.get() ) {
				// hold the register lock, as ModbusData writers do; adding the registers in
				// descending order shifts every register already added
				synchronized ( regs ) {
					regs.clear();
					for ( int addr = count - 1; addr >= 0; addr-- ) {
						regs.putValue(addr, addr);
					}
				}
				// give the reader a chance to take the lock, as monitors are not fair
				Thread.yield();
			}
		});

		// WHEN
		final List<String> failures = new ArrayList<>();
		writer.start();
		try {
			for ( int i = 0; i < 200; i++ ) {
				String failure;
				try {
					failure = holdingRegisterInfoFailure(registerInfo(), count);
				} catch ( RuntimeException e ) {
					failure = e.toString();
				}
				if ( failure != null ) {
					failures.add(failure);
				}
			}
		} finally {
			reading.set(false);
		}
		writer.join();

		// THEN
		then(failures).as("Register info rendered while registers are written is complete").isEmpty();
	}

	/**
	 * Verify the holding register info lists registers {@code 0 - count-1},
	 * each with a value equal to its address.
	 *
	 * @param info
	 *        the register info
	 * @param count
	 *        the expected register count
	 * @return a description of the first problem found, or {@code null} if
	 *         the holding block is complete, or not rendered at all
	 */
	private static String holdingRegisterInfoFailure(String info, int count) {
		final String prefix = "holding:(";
		final int start = info.indexOf(prefix);
		if ( start < 0 ) {
			return null;
		}
		final String[] rows = info.substring(start + prefix.length(), info.indexOf(')', start))
				.split(";");
		if ( rows.length != count ) {
			return "Expected " + count + " holding registers but found " + rows.length;
		}
		for ( int addr = 0; addr < count; addr++ ) {
			final String expected = "0x" + Integer.toHexString(addr) + "="
					+ String.format("0x%04X", addr);
			if ( !expected.equals(rows[addr]) ) {
				return "Expected holding register " + expected + " but found " + rows[addr];
			}
		}
		return null;
	}

}
