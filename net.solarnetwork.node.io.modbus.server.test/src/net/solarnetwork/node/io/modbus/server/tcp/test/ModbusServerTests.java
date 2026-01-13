/* ==================================================================
 * ModbusServerTests.java - 24/03/2022 1:12:40 PM
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

package net.solarnetwork.node.io.modbus.server.tcp.test;

import static java.lang.String.format;
import static net.solarnetwork.node.io.modbus.ModbusDataType.StringAscii;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import static net.solarnetwork.node.io.modbus.ModbusReadFunction.ReadHoldingRegister;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusDataType;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusReference;
import net.solarnetwork.node.io.modbus.ModbusRegisterBlockType;
import net.solarnetwork.node.io.modbus.server.dao.ModbusRegisterDao;
import net.solarnetwork.node.io.modbus.server.dao.ModbusRegisterEntity;
import net.solarnetwork.node.io.modbus.server.dao.ModbusRegisterKey;
import net.solarnetwork.node.io.modbus.server.domain.MeasurementConfig;
import net.solarnetwork.node.io.modbus.server.domain.ModbusRegisterData;
import net.solarnetwork.node.io.modbus.server.domain.RegisterBlockConfig;
import net.solarnetwork.node.io.modbus.server.domain.UnitConfig;
import net.solarnetwork.node.io.modbus.server.tcp.ModbusServer;
import net.solarnetwork.node.service.DatumEvents;
import net.solarnetwork.node.service.DatumQueue;
import net.solarnetwork.service.StaticOptionalService;
import net.solarnetwork.test.CallingThreadExecutorService;

/**
 * Test cases for the {@link RtuModbusServer} class.
 *
 * @author matt
 * @version 1.0
 */
public class ModbusServerTests {

	private static final String TEST_SOURCE_ID = "test";

	private ConcurrentMap<Integer, ModbusRegisterData> data = new ConcurrentHashMap<>(2, 0.9f, 2);
	private ModbusServer server;
	private ModbusRegisterDao registerDao;

	private final int getFreePort() {
		try (ServerSocket ss = new ServerSocket(0)) {
			ss.setReuseAddress(true);
			return ss.getLocalPort();
		} catch ( IOException e ) {
			throw new RuntimeException("Unable to find unused port");
		}
	}

	@Before
	public void setup() {
		server = new ModbusServer(new CallingThreadExecutorService(), data);
		server.setPort(getFreePort());
	}

	private void replayAll() {
		if ( registerDao != null ) {
			EasyMock.replay(registerDao);
		}
	}

	@After
	public void teardown() {
		if ( registerDao != null ) {
			EasyMock.verify(registerDao);
		}
	}

	private void assertRegisterValue(final String msg, final Integer unitId,
			final ModbusReadFunction regType, final Integer reg, final ModbusDataType dataType,
			final Integer regCount, Object expectedValue) {
		assertThat(format("%s unit ID %d has data", msg, unitId), data, hasKey(1));
		ModbusRegisterData unitData = data.get(1);
		ModbusData regData = null;
		switch (regType) {
			case ReadDiscreteInput:
				regData = unitData.getInputs();
				break;
			case ReadHoldingRegister:
				regData = unitData.getHoldings();
				break;
			default:
				throw new UnsupportedOperationException();
		}
		String regTypeName = regType.toString().replaceAll("(Read|Register)", "").toLowerCase();
		assertThat(format("%s unit %d %s register data available", msg, unitId, regTypeName), regData,
				is(notNullValue()));
		String regMsg = format("%s unit %d %s registger %d value", msg, unitId, regTypeName, reg);
		ModbusReference ref = new ModbusReference() {

			@Override
			public int getWordLength() {
				return (regCount != null ? regCount.intValue() : 0);
			}

			@Override
			public ModbusReadFunction getFunction() {
				return regType;
			}

			@Override
			public ModbusDataType getDataType() {
				return dataType;
			}

			@Override
			public int getAddress() {
				return (reg != null ? reg.intValue() : 0);
			}
		};
		switch (dataType) {
			case StringAscii:
				assertThat(regMsg, regData.getAsciiString(ref, true), is(expectedValue));
				break;

			default:
				assertThat(regMsg, regData.getNumber(ref), is(expectedValue));
				break;
		}
	}

	@Test
	public void handleDatumAcquired_number() {
		// GIVEN
		UnitConfig unitConf = new UnitConfig();
		unitConf.setUnitId(1);
		RegisterBlockConfig regBlockConf = new RegisterBlockConfig();
		regBlockConf.setBlockType(ModbusRegisterBlockType.Holding);
		regBlockConf.setStartAddress(100);
		MeasurementConfig measConf = new MeasurementConfig();
		measConf.setDataType(ModbusDataType.UInt16);
		measConf.setSourceId(TEST_SOURCE_ID);
		measConf.setPropertyName("p1");
		regBlockConf.setMeasurementConfigs(new MeasurementConfig[] { measConf });
		unitConf.setRegisterBlockConfigs(new RegisterBlockConfig[] { regBlockConf });
		server.setUnitConfigs(new UnitConfig[] { unitConf });

		DatumSamples s = new DatumSamples();
		s.putInstantaneousSampleValue("p1", 123);
		SimpleDatum d = SimpleDatum.nodeDatum(TEST_SOURCE_ID, Instant.now(), s);
		Event evt = DatumEvents.datumEvent(DatumQueue.EVENT_TOPIC_DATUM_ACQUIRED, d);

		// WHEN
		server.handleEvent(evt);

		// THEN
		assertRegisterValue("Acquired num", 1, ReadHoldingRegister, 100, UInt16, 0, 123);
	}

	@Test
	public void handleDatumAcquired_number_persistence() {
		// GIVEN
		final String serverId = "test.uid";
		server.setUid(serverId);
		registerDao = EasyMock.createMock(ModbusRegisterDao.class);
		server.setRegisterDao(new StaticOptionalService<>(registerDao));

		Capture<ModbusRegisterEntity> daoEntityCaptor = Capture.newInstance(CaptureType.ALL);
		expect(registerDao.save(capture(daoEntityCaptor))).andAnswer(new IAnswer<ModbusRegisterKey>() {

			@Override
			public ModbusRegisterKey answer() throws Throwable {
				return daoEntityCaptor.getValues().get(daoEntityCaptor.getValues().size() - 1).getId();
			}
		});

		UnitConfig unitConf = new UnitConfig();
		unitConf.setUnitId(1);
		RegisterBlockConfig regBlockConf = new RegisterBlockConfig();
		regBlockConf.setBlockType(ModbusRegisterBlockType.Holding);
		regBlockConf.setStartAddress(100);
		MeasurementConfig measConf = new MeasurementConfig();
		measConf.setDataType(ModbusDataType.UInt16);
		measConf.setSourceId(TEST_SOURCE_ID);
		measConf.setPropertyName("p1");
		regBlockConf.setMeasurementConfigs(new MeasurementConfig[] { measConf });
		unitConf.setRegisterBlockConfigs(new RegisterBlockConfig[] { regBlockConf });
		server.setUnitConfigs(new UnitConfig[] { unitConf });

		DatumSamples s = new DatumSamples();
		s.putInstantaneousSampleValue("p1", 123);
		SimpleDatum d = SimpleDatum.nodeDatum(TEST_SOURCE_ID, Instant.now(), s);
		Event evt = DatumEvents.datumEvent(DatumQueue.EVENT_TOPIC_DATUM_ACQUIRED, d);

		// WHEN
		replayAll();
		server.handleEvent(evt);

		// THEN
		assertRegisterValue("Acquired num", 1, ReadHoldingRegister, 100, UInt16, 0, 123);

		List<ModbusRegisterEntity> daoEntities = daoEntityCaptor.getValues();
		assertThat("1 register entity saved", daoEntities, hasSize(1));

		ModbusRegisterEntity daoEntity = daoEntities.get(0);
		assertThat("Persisted register server ID", daoEntity.getServerId(), is(equalTo(serverId)));
		assertThat("Persisted register unit", daoEntity.getUnitId(), is(equalTo(unitConf.getUnitId())));
		assertThat("Persisted register block type", daoEntity.getBlockType(),
				is(equalTo(regBlockConf.getBlockType())));
		assertThat("Persisted register address", daoEntity.getAddress(),
				is(equalTo(regBlockConf.getStartAddress())));
		assertThat("Persisted register value", daoEntity.getValue(), is(equalTo((short) 123)));
	}

	@Test
	public void handleDatumAcquired_string() {
		// GIVEN
		UnitConfig unitConf = new UnitConfig();
		unitConf.setUnitId(1);
		RegisterBlockConfig regBlockConf = new RegisterBlockConfig();
		regBlockConf.setBlockType(ModbusRegisterBlockType.Holding);
		regBlockConf.setStartAddress(100);
		MeasurementConfig measConf = new MeasurementConfig();
		measConf.setDataType(StringAscii);
		measConf.setSourceId(TEST_SOURCE_ID);
		measConf.setPropertyName("p1");
		measConf.setWordLength(16);
		regBlockConf.setMeasurementConfigs(new MeasurementConfig[] { measConf });
		unitConf.setRegisterBlockConfigs(new RegisterBlockConfig[] { regBlockConf });
		server.setUnitConfigs(new UnitConfig[] { unitConf });

		DatumSamples s = new DatumSamples();
		s.putStatusSampleValue("p1", "Hello, world.");
		SimpleDatum d = SimpleDatum.nodeDatum(TEST_SOURCE_ID, Instant.now(), s);
		Event evt = DatumEvents.datumEvent(DatumQueue.EVENT_TOPIC_DATUM_ACQUIRED, d);

		// WHEN
		server.handleEvent(evt);

		// THEN
		assertRegisterValue("Acquired string", 1, ReadHoldingRegister, 100, StringAscii, 16,
				"Hello, world.");
	}

	@Test
	public void handleDatumAcquired_string_persistence() {
		// GIVEN
		final String serverId = "test.uid";
		server.setUid(serverId);
		registerDao = EasyMock.createMock(ModbusRegisterDao.class);
		server.setRegisterDao(new StaticOptionalService<>(registerDao));

		Capture<ModbusRegisterEntity> daoEntityCaptor = Capture.newInstance(CaptureType.ALL);
		expect(registerDao.save(capture(daoEntityCaptor))).andAnswer(new IAnswer<ModbusRegisterKey>() {

			@Override
			public ModbusRegisterKey answer() throws Throwable {
				return daoEntityCaptor.getValues().get(daoEntityCaptor.getValues().size() - 1).getId();
			}
		}).times(7);

		UnitConfig unitConf = new UnitConfig();
		unitConf.setUnitId(1);
		RegisterBlockConfig regBlockConf = new RegisterBlockConfig();
		regBlockConf.setBlockType(ModbusRegisterBlockType.Holding);
		regBlockConf.setStartAddress(100);
		MeasurementConfig measConf = new MeasurementConfig();
		measConf.setDataType(StringAscii);
		measConf.setSourceId(TEST_SOURCE_ID);
		measConf.setPropertyName("p1");
		measConf.setWordLength(16);
		regBlockConf.setMeasurementConfigs(new MeasurementConfig[] { measConf });
		unitConf.setRegisterBlockConfigs(new RegisterBlockConfig[] { regBlockConf });
		server.setUnitConfigs(new UnitConfig[] { unitConf });

		DatumSamples s = new DatumSamples();
		s.putStatusSampleValue("p1", "Hello, world.");
		SimpleDatum d = SimpleDatum.nodeDatum(TEST_SOURCE_ID, Instant.now(), s);
		Event evt = DatumEvents.datumEvent(DatumQueue.EVENT_TOPIC_DATUM_ACQUIRED, d);

		// WHEN
		replayAll();
		server.handleEvent(evt);

		// THEN
		assertRegisterValue("Acquired string", 1, ReadHoldingRegister, 100, StringAscii, 16,
				"Hello, world.");

		List<ModbusRegisterEntity> daoEntities = daoEntityCaptor.getValues();
		assertThat("1 register entity saved", daoEntities, hasSize(7));

		// verify first register, holding first 2 characters of string
		ModbusRegisterEntity daoEntity = daoEntities.get(0);
		assertThat("Persisted register server ID", daoEntity.getServerId(), is(equalTo(serverId)));
		assertThat("Persisted register unit", daoEntity.getUnitId(), is(equalTo(unitConf.getUnitId())));
		assertThat("Persisted register block type", daoEntity.getBlockType(),
				is(equalTo(regBlockConf.getBlockType())));
		assertThat("Persisted register address", daoEntity.getAddress(),
				is(equalTo(regBlockConf.getStartAddress())));
		assertThat("Persisted register value", daoEntity.getValue(),
				is(equalTo((short) ('H' << 8 | 'e'))));

		// verify last register, holding last character of string
		daoEntity = daoEntities.get(6);
		assertThat("Persisted register server ID", daoEntity.getServerId(), is(equalTo(serverId)));
		assertThat("Persisted register unit", daoEntity.getUnitId(), is(equalTo(unitConf.getUnitId())));
		assertThat("Persisted register block type", daoEntity.getBlockType(),
				is(equalTo(regBlockConf.getBlockType())));
		assertThat("Persisted register address", daoEntity.getAddress(),
				is(equalTo(regBlockConf.getStartAddress() + 6)));
		assertThat("Persisted register value", daoEntity.getValue(), is(equalTo((short) ('.' << 8))));
	}

}
