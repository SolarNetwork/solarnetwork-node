/* ==================================================================
 * ModbusDatumDataSourceTests.java - 20/12/2017 4:33:10 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.datum.modbus.test;

import static net.solarnetwork.domain.GeneralDatumSamplesType.Accumulating;
import static net.solarnetwork.domain.GeneralDatumSamplesType.Instantaneous;
import static net.solarnetwork.domain.GeneralDatumSamplesType.Status;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Float32;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Float64;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.Int64;
import static net.solarnetwork.node.io.modbus.ModbusDataType.StringUtf8;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt16;
import static net.solarnetwork.node.io.modbus.ModbusDataType.UInt32;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.node.DatumMetadataService;
import net.solarnetwork.node.datum.modbus.ModbusDatumDataSource;
import net.solarnetwork.node.datum.modbus.ModbusPropertyConfig;
import net.solarnetwork.node.datum.modbus.VirtualMeterConfig;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.io.modbus.ModbusConnection;
import net.solarnetwork.node.io.modbus.ModbusConnectionAction;
import net.solarnetwork.node.io.modbus.ModbusData;
import net.solarnetwork.node.io.modbus.ModbusHelper;
import net.solarnetwork.node.io.modbus.ModbusNetwork;
import net.solarnetwork.node.io.modbus.ModbusReadFunction;
import net.solarnetwork.node.io.modbus.ModbusWordOrder;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link ModbusDatumDataSource} class.
 * 
 * @author matt
 * @version 1.2
 */
public class ModbusDatumDataSourceTests {

	private static final String TEST_SOURCE_ID = "test.source";
	private static final String TEST_STATUS_PROP_NAME = "msg";
	private static final String TEST_FLOAT32_PROP_NAME = "f32";
	private static final String TEST_FLOAT64_PROP_NAME = "f64";
	private static final String TEST_INT16_PROP_NAME = "int16";
	private static final String TEST_SINT16_PROP_NAME = "sint16";
	private static final String TEST_INT32_PROP_NAME = "i32";
	private static final String TEST_INT64_PROP_NAME = "i64";

	private ModbusNetwork modbusNetwork;
	private ModbusConnection modbusConnection;
	private DatumMetadataService datumMetadataService;

	private ModbusDatumDataSource dataSource;

	@Before
	public void setup() {
		modbusNetwork = EasyMock.createMock(ModbusNetwork.class);
		modbusConnection = EasyMock.createMock(ModbusConnection.class);
		datumMetadataService = EasyMock.createMock(DatumMetadataService.class);

		dataSource = new ModbusDatumDataSource();
		dataSource.setSourceId(TEST_SOURCE_ID);
		dataSource.setModbusNetwork(new StaticOptionalService<ModbusNetwork>(modbusNetwork));
		dataSource.setDatumMetadataService(
				new StaticOptionalService<DatumMetadataService>(datumMetadataService));
	}

	private void replayAll() {
		EasyMock.replay(modbusNetwork, modbusConnection, datumMetadataService);
	}

	@After
	public void teardown() {
		EasyMock.verify(modbusNetwork, modbusConnection, datumMetadataService);
	}

	private static int[] stringToModbusWordArray(String s, String charset, int minOutputLength) {
		byte[] bytes;
		try {
			bytes = s.getBytes(charset);
		} catch ( UnsupportedEncodingException e ) {
			throw new RuntimeException(e);
		}
		int[] ints = new int[Math.max((int) Math.ceil(bytes.length / 2.0), minOutputLength)];
		Arrays.fill(ints, 0);
		for ( int i = 0; i < bytes.length; i += 2 ) {
			int n = ((bytes[i]) & 0xFF) << 8;
			if ( i + 1 < bytes.length ) {
				n |= ((bytes[i + 1]) & 0xFF);
			}
			ints[i / 2] = n;
		}
		return ints;
	}

	@Test
	public void readDatumWithInstantaneousValues() throws IOException {
		// GIVEN

		// we will collect 2 ranges of data; 0 - 7 for some integers; 200 - 205 for some floating points
		ModbusPropertyConfig[] propConfigs = new ModbusPropertyConfig[] {
				new ModbusPropertyConfig(TEST_INT16_PROP_NAME, Instantaneous, UInt16, 0),
				new ModbusPropertyConfig(TEST_SINT16_PROP_NAME, Instantaneous, Int16, 1),
				new ModbusPropertyConfig(TEST_INT32_PROP_NAME, Instantaneous, UInt32, 2),
				new ModbusPropertyConfig(TEST_INT64_PROP_NAME, Instantaneous, Int64, 4),
				new ModbusPropertyConfig(TEST_FLOAT32_PROP_NAME, Instantaneous, Float32, 200,
						BigDecimal.ONE, -1),
				new ModbusPropertyConfig(TEST_FLOAT64_PROP_NAME, Instantaneous, Float64, 202,
						BigDecimal.ONE, -1), };
		dataSource.setPropConfigs(propConfigs);

		Capture<ModbusConnectionAction<ModbusData>> connActionCapture = new Capture<>();
		expect(modbusNetwork.performAction(capture(connActionCapture), eq(1)))
				.andAnswer(new IAnswer<ModbusData>() {

					@Override
					public ModbusData answer() throws Throwable {
						ModbusConnectionAction<ModbusData> action = connActionCapture.getValue();
						return action.doWithConnection(modbusConnection);
					}
				});

		final int[] range1 = new int[] { 0xfc1e, 0xf0c3, 0x02e3, 0x68e7, 0x0002, 0x1376, 0x1512,
				0xdfee };
		final int[] range2 = new int[] { 0x44f6, 0xc651, 0x4172, 0xd3d1, 0x6328, 0x8ce7 };
		expect(modbusConnection.readUnsignedShorts(ModbusReadFunction.ReadHoldingRegister, 0, 8))
				.andReturn(range1);
		expect(modbusConnection.readUnsignedShorts(ModbusReadFunction.ReadHoldingRegister, 200, 6))
				.andReturn(range2);

		replayAll();

		// WHEN
		GeneralNodeDatum datum = dataSource.readCurrentDatum();

		// THEN
		assertThat("Datum returned", datum, notNullValue());
		assertThat("Created", datum.getCreated(), notNullValue());
		assertThat("Source ID", datum.getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Int16 value", datum.getInstantaneousSampleInteger(TEST_INT16_PROP_NAME),
				equalTo(64542));
		assertThat("SInt16 value", datum.getInstantaneousSampleInteger(TEST_SINT16_PROP_NAME),
				equalTo(-3901));
		assertThat("Int32 value", datum.getInstantaneousSampleInteger(TEST_INT32_PROP_NAME),
				equalTo(48457959));
		assertThat("Int64 value", datum.getInstantaneousSampleLong(TEST_INT64_PROP_NAME),
				equalTo(584347834048494L));
		assertThat("Float32 value",
				Float.floatToIntBits(datum.getInstantaneousSampleFloat(TEST_FLOAT32_PROP_NAME)),
				equalTo(Integer.parseInt("44f6c651", 16))); // Hamcrest missing closeTo() for floats
		assertThat("Float64 value", datum.getInstantaneousSampleDouble(TEST_FLOAT64_PROP_NAME),
				closeTo(19741974.1974, 0.00001));
	}

	@Test
	public void readDatumWithStatusString() throws IOException {
		// GIVEN
		ModbusPropertyConfig propConfig = new ModbusPropertyConfig();
		propConfig.setPropertyKey(TEST_STATUS_PROP_NAME);
		propConfig.setAddress(0);
		propConfig.setDataType(StringUtf8);
		propConfig.setWordLength(8);
		propConfig.setPropertyType(Status);
		dataSource.setPropConfigs(new ModbusPropertyConfig[] { propConfig });

		Capture<ModbusConnectionAction<ModbusData>> connActionCapture = new Capture<>();
		expect(modbusNetwork.performAction(capture(connActionCapture), eq(1)))
				.andAnswer(new IAnswer<ModbusData>() {

					@Override
					public ModbusData answer() throws Throwable {
						ModbusConnectionAction<ModbusData> action = connActionCapture.getValue();
						return action.doWithConnection(modbusConnection);
					}
				});

		final String message = "Hello, world.";
		final int[] strWords = stringToModbusWordArray(message, ModbusHelper.UTF8_CHARSET,
				propConfig.getWordLength());
		expect(modbusConnection.readUnsignedShorts(ModbusReadFunction.ReadHoldingRegister, 0, 8))
				.andReturn(strWords);

		replayAll();

		// WHEN
		GeneralNodeDatum datum = dataSource.readCurrentDatum();

		// THEN
		assertThat("Datum returned", datum, notNullValue());
		assertThat("Created", datum.getCreated(), notNullValue());
		assertThat("Source ID", datum.getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Prop value", datum.getStatusSampleString(TEST_STATUS_PROP_NAME), equalTo(message));
	}

	@Test
	public void readDatumWithUnitMultipier() throws IOException {
		// GIVEN

		ModbusPropertyConfig[] propConfigs = new ModbusPropertyConfig[] {
				new ModbusPropertyConfig(TEST_INT32_PROP_NAME, Instantaneous, UInt32, 0,
						new BigDecimal("0.1"), -1),
				new ModbusPropertyConfig(TEST_INT64_PROP_NAME, Instantaneous, Int64, 2,
						new BigDecimal("0.01"), -1),
				new ModbusPropertyConfig(TEST_FLOAT32_PROP_NAME, Accumulating, Float32, 6,
						new BigDecimal("0.001"), -1),
				new ModbusPropertyConfig(TEST_FLOAT64_PROP_NAME, Accumulating, Float64, 8,
						new BigDecimal("0.0001"), -1), };
		dataSource.setPropConfigs(propConfigs);

		Capture<ModbusConnectionAction<ModbusData>> connActionCapture = new Capture<>();
		expect(modbusNetwork.performAction(capture(connActionCapture), eq(1)))
				.andAnswer(new IAnswer<ModbusData>() {

					@Override
					public ModbusData answer() throws Throwable {
						ModbusConnectionAction<ModbusData> action = connActionCapture.getValue();
						return action.doWithConnection(modbusConnection);
					}
				});

		final int[] range1 = new int[] { 0x02e3, 0x68e7, 0x0002, 0x1376, 0x1512, 0xdfee, 0x44f6, 0xc651,
				0x4172, 0xd3d1, 0x6328, 0x8ce7 };
		expect(modbusConnection.readUnsignedShorts(ModbusReadFunction.ReadHoldingRegister, 0, 12))
				.andReturn(range1);

		replayAll();

		// WHEN
		GeneralNodeDatum datum = dataSource.readCurrentDatum();

		// THEN
		assertThat("Datum returned", datum, notNullValue());
		assertThat("Created", datum.getCreated(), notNullValue());
		assertThat("Source ID", datum.getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Int32 value", datum.getInstantaneousSampleBigDecimal(TEST_INT32_PROP_NAME),
				equalTo(new BigDecimal("4845795.9")));
		assertThat("Int64 value", datum.getInstantaneousSampleBigDecimal(TEST_INT64_PROP_NAME),
				equalTo(new BigDecimal("5843478340484.94")));
		assertThat("Float32 value", datum.getAccumulatingSampleBigDecimal(TEST_FLOAT32_PROP_NAME),
				equalTo(new BigDecimal("1.9741974")));
		assertThat("Float64 value", datum.getAccumulatingSampleBigDecimal(TEST_FLOAT64_PROP_NAME),
				equalTo(new BigDecimal("1974.19741974")));
	}

	@Test
	public void readDatumWithDecimalScale() throws IOException {
		// GIVEN

		ModbusPropertyConfig[] propConfigs = new ModbusPropertyConfig[] {
				new ModbusPropertyConfig(TEST_INT32_PROP_NAME, Instantaneous, UInt32, 0,
						new BigDecimal("0.1"), 0),
				new ModbusPropertyConfig(TEST_INT64_PROP_NAME, Instantaneous, Int64, 2,
						new BigDecimal("0.01"), 1),
				new ModbusPropertyConfig(TEST_FLOAT32_PROP_NAME, Accumulating, Float32, 6,
						new BigDecimal("0.001"), 4),
				new ModbusPropertyConfig(TEST_FLOAT64_PROP_NAME, Accumulating, Float64, 8,
						new BigDecimal("0.0001"), -1), };
		dataSource.setPropConfigs(propConfigs);

		Capture<ModbusConnectionAction<ModbusData>> connActionCapture = new Capture<>();
		expect(modbusNetwork.performAction(capture(connActionCapture), eq(1)))
				.andAnswer(new IAnswer<ModbusData>() {

					@Override
					public ModbusData answer() throws Throwable {
						ModbusConnectionAction<ModbusData> action = connActionCapture.getValue();
						return action.doWithConnection(modbusConnection);
					}
				});

		final int[] range1 = new int[] { 0x02e3, 0x68e7, 0x0002, 0x1376, 0x1512, 0xdfee, 0x44f6, 0xc651,
				0x4172, 0xd3d1, 0x6328, 0x8ce7 };
		expect(modbusConnection.readUnsignedShorts(ModbusReadFunction.ReadHoldingRegister, 0, 12))
				.andReturn(range1);

		replayAll();

		// WHEN
		GeneralNodeDatum datum = dataSource.readCurrentDatum();

		// THEN
		assertThat("Datum returned", datum, notNullValue());
		assertThat("Created", datum.getCreated(), notNullValue());
		assertThat("Source ID", datum.getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Int32 value", datum.getInstantaneousSampleBigDecimal(TEST_INT32_PROP_NAME),
				equalTo(new BigDecimal("4845796")));
		assertThat("Int64 value", datum.getInstantaneousSampleBigDecimal(TEST_INT64_PROP_NAME),
				equalTo(new BigDecimal("5843478340484.9")));
		assertThat("Float32 value", datum.getAccumulatingSampleBigDecimal(TEST_FLOAT32_PROP_NAME),
				equalTo(new BigDecimal("1.9742")));
		assertThat("Float64 value", datum.getAccumulatingSampleBigDecimal(TEST_FLOAT64_PROP_NAME),
				equalTo(new BigDecimal("1974.19741974")));
	}

	@Test
	public void readDatumWithDecimalScaleLeastSignificantWordOrder() throws IOException {
		// GIVEN

		ModbusPropertyConfig[] propConfigs = new ModbusPropertyConfig[] {
				new ModbusPropertyConfig(TEST_INT32_PROP_NAME, Instantaneous, UInt32, 0,
						new BigDecimal("0.1"), 0),
				new ModbusPropertyConfig(TEST_INT64_PROP_NAME, Instantaneous, Int64, 2,
						new BigDecimal("0.01"), 1),
				new ModbusPropertyConfig(TEST_FLOAT32_PROP_NAME, Accumulating, Float32, 6,
						new BigDecimal("0.001"), 4),
				new ModbusPropertyConfig(TEST_FLOAT64_PROP_NAME, Accumulating, Float64, 8,
						new BigDecimal("0.0001"), -1), };
		dataSource.setPropConfigs(propConfigs);
		dataSource.setWordOrder(ModbusWordOrder.LeastToMostSignificant);

		Capture<ModbusConnectionAction<ModbusData>> connActionCapture = new Capture<>();
		expect(modbusNetwork.performAction(capture(connActionCapture), eq(1)))
				.andAnswer(new IAnswer<ModbusData>() {

					@Override
					public ModbusData answer() throws Throwable {
						ModbusConnectionAction<ModbusData> action = connActionCapture.getValue();
						return action.doWithConnection(modbusConnection);
					}
				});

		final int[] range1 = new int[] { 0x68e7, 0x02e3, 0xdfee, 0x1512, 0x1376, 0x0002, 0xc651, 0x44f6,
				0x8ce7, 0x6328, 0xd3d1, 0x4172 };
		expect(modbusConnection.readUnsignedShorts(ModbusReadFunction.ReadHoldingRegister, 0, 12))
				.andReturn(range1);

		replayAll();

		// WHEN
		GeneralNodeDatum datum = dataSource.readCurrentDatum();

		// THEN
		assertThat("Datum returned", datum, notNullValue());
		assertThat("Created", datum.getCreated(), notNullValue());
		assertThat("Source ID", datum.getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Int32 value", datum.getInstantaneousSampleBigDecimal(TEST_INT32_PROP_NAME),
				equalTo(new BigDecimal("4845796")));
		assertThat("Int64 value", datum.getInstantaneousSampleBigDecimal(TEST_INT64_PROP_NAME),
				equalTo(new BigDecimal("5843478340484.9")));
		assertThat("Float32 value", datum.getAccumulatingSampleBigDecimal(TEST_FLOAT32_PROP_NAME),
				equalTo(new BigDecimal("1.9742")));
		assertThat("Float64 value", datum.getAccumulatingSampleBigDecimal(TEST_FLOAT64_PROP_NAME),
				equalTo(new BigDecimal("1974.19741974")));
	}

	@Test
	public void readDatumWithInputRegisters() throws IOException {
		// GIVEN
		ModbusPropertyConfig propConfig = new ModbusPropertyConfig();
		propConfig.setPropertyKey(TEST_FLOAT32_PROP_NAME);
		propConfig.setAddress(0);
		propConfig.setDataType(Float32);
		propConfig.setPropertyType(Instantaneous);
		propConfig.setFunction(ModbusReadFunction.ReadInputRegister);
		propConfig.setDecimalScale(-1);
		dataSource.setPropConfigs(new ModbusPropertyConfig[] { propConfig });

		Capture<ModbusConnectionAction<ModbusData>> connActionCapture = new Capture<>();
		expect(modbusNetwork.performAction(capture(connActionCapture), eq(1)))
				.andAnswer(new IAnswer<ModbusData>() {

					@Override
					public ModbusData answer() throws Throwable {
						ModbusConnectionAction<ModbusData> action = connActionCapture.getValue();
						return action.doWithConnection(modbusConnection);
					}
				});

		final int[] range1 = new int[] { 0x44f6, 0xc651 };
		expect(modbusConnection.readUnsignedShorts(ModbusReadFunction.ReadInputRegister, 0, 2))
				.andReturn(range1);

		replayAll();

		// WHEN
		GeneralNodeDatum datum = dataSource.readCurrentDatum();

		// THEN
		assertThat("Datum returned", datum, notNullValue());
		assertThat("Created", datum.getCreated(), notNullValue());
		assertThat("Source ID", datum.getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Float32 value", datum.getInstantaneousSampleBigDecimal(TEST_FLOAT32_PROP_NAME),
				equalTo(new BigDecimal("1974.1974")));
	}

	@Test
	public void readDatumWithVirtualMeterFirstTime() throws IOException {
		// GIVEN
		ModbusPropertyConfig propConfig = new ModbusPropertyConfig();
		propConfig.setPropertyKey(TEST_FLOAT32_PROP_NAME);
		propConfig.setAddress(0);
		propConfig.setDataType(Float32);
		propConfig.setPropertyType(Instantaneous);
		propConfig.setDecimalScale(-1);
		dataSource.setPropConfigs(new ModbusPropertyConfig[] { propConfig });

		VirtualMeterConfig meterConfig = new VirtualMeterConfig();
		meterConfig.setPropertyKey(TEST_FLOAT32_PROP_NAME);
		meterConfig.setTimeUnit(TimeUnit.SECONDS);
		dataSource.setVirtualMeterConfigs(new VirtualMeterConfig[] { meterConfig });

		Capture<ModbusConnectionAction<ModbusData>> connActionCapture = new Capture<>();
		expect(modbusNetwork.performAction(capture(connActionCapture), eq(1)))
				.andAnswer(new IAnswer<ModbusData>() {

					@Override
					public ModbusData answer() throws Throwable {
						ModbusConnectionAction<ModbusData> action = connActionCapture.getValue();
						return action.doWithConnection(modbusConnection);
					}
				});

		final int[] range1 = new int[] { 0x44f6, 0xc651 };
		expect(modbusConnection.readUnsignedShorts(ModbusReadFunction.ReadHoldingRegister, 0, 2))
				.andReturn(range1);

		// no metadata returned
		expect(datumMetadataService.getSourceMetadata(TEST_SOURCE_ID)).andReturn(null);

		// but we will save initial metadata
		Capture<GeneralDatumMetadata> metadataCaptor = new Capture<>();
		datumMetadataService.addSourceMetadata(eq(TEST_SOURCE_ID), capture(metadataCaptor));

		final String meterPropName = TEST_FLOAT32_PROP_NAME + "Seconds";

		replayAll();

		// WHEN
		GeneralNodeDatum datum = dataSource.readCurrentDatum();

		// THEN
		BigDecimal expectedPropValue = new BigDecimal("1974.1974");
		assertThat("Datum returned", datum, notNullValue());
		assertThat("Created", datum.getCreated(), notNullValue());
		assertThat("Source ID", datum.getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Float32 value", datum.getInstantaneousSampleBigDecimal(TEST_FLOAT32_PROP_NAME),
				equalTo(expectedPropValue));
		assertThat("No virtual meter value", datum.getAccumulatingSampleBigDecimal(meterPropName),
				nullValue());

		assertThat("Metadata saved", metadataCaptor.hasCaptured(), equalTo(true));
		GeneralDatumMetadata savedMetadata = metadataCaptor.getValue();
		assertThat("Virtual meter metadata date",
				savedMetadata.getInfoLong(meterPropName, ModbusDatumDataSource.VIRTUAL_METER_DATE_KEY),
				notNullValue());
		assertThat("Virtual meter metadata value",
				savedMetadata.getInfoString(meterPropName,
						ModbusDatumDataSource.VIRTUAL_METER_VALUE_KEY),
				equalTo(expectedPropValue.toString()));
		assertThat("Virtual meter metadata reading", savedMetadata.getInfoString(meterPropName,
				ModbusDatumDataSource.VIRTUAL_METER_READING_KEY), equalTo("0"));
	}

	@Test
	public void readDatumWithVirtualMeterSecondTime() throws IOException {
		// GIVEN
		ModbusPropertyConfig propConfig = new ModbusPropertyConfig();
		propConfig.setPropertyKey(TEST_FLOAT32_PROP_NAME);
		propConfig.setAddress(0);
		propConfig.setDataType(Float32);
		propConfig.setPropertyType(Instantaneous);
		propConfig.setDecimalScale(-1);
		dataSource.setPropConfigs(new ModbusPropertyConfig[] { propConfig });

		VirtualMeterConfig meterConfig = new VirtualMeterConfig();
		meterConfig.setPropertyKey(TEST_FLOAT32_PROP_NAME);
		meterConfig.setTimeUnit(TimeUnit.SECONDS);
		dataSource.setVirtualMeterConfigs(new VirtualMeterConfig[] { meterConfig });

		Capture<ModbusConnectionAction<ModbusData>> connActionCapture = new Capture<>();
		expect(modbusNetwork.performAction(capture(connActionCapture), eq(1)))
				.andAnswer(new IAnswer<ModbusData>() {

					@Override
					public ModbusData answer() throws Throwable {
						ModbusConnectionAction<ModbusData> action = connActionCapture.getValue();
						return action.doWithConnection(modbusConnection);
					}
				});

		final int[] range1 = new int[] { 0x44f6, 0xc614 }; // 1974.1900
		expect(modbusConnection.readUnsignedShorts(ModbusReadFunction.ReadHoldingRegister, 0, 2))
				.andReturn(range1);

		final long now = System.currentTimeMillis();

		// prev metadata to start with
		final String meterPropName = TEST_FLOAT32_PROP_NAME + "Seconds";
		GeneralDatumMetadata metadata = new GeneralDatumMetadata();
		metadata.putInfoValue(meterPropName, ModbusDatumDataSource.VIRTUAL_METER_DATE_KEY, now - 500); // 0.5 seconds ago
		metadata.putInfoValue(meterPropName, ModbusDatumDataSource.VIRTUAL_METER_VALUE_KEY, "1974.1974");
		metadata.putInfoValue(meterPropName, ModbusDatumDataSource.VIRTUAL_METER_READING_KEY, "0");
		expect(datumMetadataService.getSourceMetadata(TEST_SOURCE_ID)).andReturn(metadata);

		// but we will save initial metadata
		Capture<GeneralDatumMetadata> metadataCaptor = new Capture<>();
		datumMetadataService.addSourceMetadata(eq(TEST_SOURCE_ID), capture(metadataCaptor));

		replayAll();

		// WHEN
		GeneralNodeDatum datum = dataSource.readCurrentDatum();

		// THEN
		BigDecimal expectedPropValue = new BigDecimal("1974.19");
		BigDecimal expectedMeterValue = new BigDecimal("996.9678185");
		assertThat("Datum returned", datum, notNullValue());
		assertThat("Created", datum.getCreated(), notNullValue());
		assertThat("Source ID", datum.getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Float32 value", datum.getInstantaneousSampleBigDecimal(TEST_FLOAT32_PROP_NAME),
				equalTo(expectedPropValue));
		assertThat("Virtual meter value", datum.getAccumulatingSampleBigDecimal(meterPropName),
				closeTo(expectedMeterValue, new BigDecimal("50")));

		assertThat("Metadata saved", metadataCaptor.hasCaptured(), equalTo(true));
		GeneralDatumMetadata savedMetadata = metadataCaptor.getValue();
		assertThat("Virtual meter metadata date",
				savedMetadata.getInfoLong(meterPropName, ModbusDatumDataSource.VIRTUAL_METER_DATE_KEY),
				greaterThanOrEqualTo(now));
		assertThat("Virtual meter metadata value",
				savedMetadata.getInfoString(meterPropName,
						ModbusDatumDataSource.VIRTUAL_METER_VALUE_KEY),
				equalTo(expectedPropValue.toString()));
		assertThat("Virtual meter metadata reading",
				savedMetadata.getInfoString(meterPropName,
						ModbusDatumDataSource.VIRTUAL_METER_READING_KEY),
				equalTo(datum.getAccumulatingSampleBigDecimal(meterPropName).toString()));
	}

	@Test
	public void readDatumWithVirtualMeterSecondTimeViaCachedMetadata()
			throws IOException, InterruptedException {
		// GIVEN
		ModbusPropertyConfig propConfig = new ModbusPropertyConfig();
		propConfig.setPropertyKey(TEST_FLOAT32_PROP_NAME);
		propConfig.setAddress(0);
		propConfig.setDataType(Float32);
		propConfig.setPropertyType(Instantaneous);
		propConfig.setDecimalScale(-1);
		dataSource.setPropConfigs(new ModbusPropertyConfig[] { propConfig });

		VirtualMeterConfig meterConfig = new VirtualMeterConfig();
		meterConfig.setPropertyKey(TEST_FLOAT32_PROP_NAME);
		meterConfig.setTimeUnit(TimeUnit.SECONDS);
		dataSource.setVirtualMeterConfigs(new VirtualMeterConfig[] { meterConfig });

		// disable sample cache
		dataSource.setSampleCacheMs(0);

		Capture<ModbusConnectionAction<ModbusData>> connActionCapture = new Capture<>();
		expect(modbusNetwork.performAction(capture(connActionCapture), eq(1)))
				.andAnswer(new IAnswer<ModbusData>() {

					@Override
					public ModbusData answer() throws Throwable {
						ModbusConnectionAction<ModbusData> action = connActionCapture.getValue();
						return action.doWithConnection(modbusConnection);
					}
				}).times(2);

		final int[] range1 = new int[] { 0x44f6, 0xc651 }; // 1974.1974
		expect(modbusConnection.readUnsignedShorts(ModbusReadFunction.ReadHoldingRegister, 0, 2))
				.andReturn(range1);

		final int[] range2 = new int[] { 0x44f6, 0xc614 }; // 1974.1900
		expect(modbusConnection.readUnsignedShorts(ModbusReadFunction.ReadHoldingRegister, 0, 2))
				.andReturn(range2);

		final long now = System.currentTimeMillis();

		// metadata starts as null
		expect(datumMetadataService.getSourceMetadata(TEST_SOURCE_ID)).andReturn(null);

		// save initial metadata
		Capture<GeneralDatumMetadata> metadataCaptor = new Capture<>(CaptureType.ALL);
		datumMetadataService.addSourceMetadata(eq(TEST_SOURCE_ID), capture(metadataCaptor));

		// 2nd time will use cached metadata; save again
		EasyMock.expectLastCall().times(2);

		final String meterPropName = TEST_FLOAT32_PROP_NAME + "Seconds";

		replayAll();

		// WHEN
		GeneralNodeDatum datum = dataSource.readCurrentDatum();

		Thread.sleep(500);

		final long now2 = System.currentTimeMillis();

		GeneralNodeDatum datum2 = dataSource.readCurrentDatum();

		// THEN
		BigDecimal expectedPropValue = new BigDecimal("1974.1974");
		assertThat("Datum returned", datum, notNullValue());
		assertThat("Created", datum.getCreated(), notNullValue());
		assertThat("Source ID", datum.getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Float32 value", datum.getInstantaneousSampleBigDecimal(TEST_FLOAT32_PROP_NAME),
				equalTo(expectedPropValue));
		assertThat("No virtual meter value", datum.getAccumulatingSampleBigDecimal(meterPropName),
				nullValue());

		BigDecimal expectedPropValue2 = new BigDecimal("1974.19");
		BigDecimal expectedMeterValue = new BigDecimal((1974.1974 + 1974.19) / 2 * (now2 - now) / 1000);
		assertThat("Datum 2 returned", datum2, notNullValue());
		assertThat("Created 2", datum2.getCreated(), notNullValue());
		assertThat("Source ID 2", datum2.getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Float32 value 2", datum2.getInstantaneousSampleBigDecimal(TEST_FLOAT32_PROP_NAME),
				equalTo(expectedPropValue2));
		assertThat("Virtual meter value", datum2.getAccumulatingSampleBigDecimal(meterPropName),
				closeTo(expectedMeterValue, new BigDecimal("50")));

		assertThat("Metadata saved twice", metadataCaptor.getValues(), hasSize(2));
		assertThat("Same metadata saved", metadataCaptor.getValues().get(1),
				sameInstance(metadataCaptor.getValues().get(0)));
		GeneralDatumMetadata savedMetadata = metadataCaptor.getValues().get(1);
		assertThat("Virtual meter metadata date",
				savedMetadata.getInfoLong(meterPropName, ModbusDatumDataSource.VIRTUAL_METER_DATE_KEY),
				greaterThanOrEqualTo(now));
		assertThat("Virtual meter metadata value",
				savedMetadata.getInfoString(meterPropName,
						ModbusDatumDataSource.VIRTUAL_METER_VALUE_KEY),
				equalTo(expectedPropValue2.toString()));
		assertThat("Virtual meter metadata reading",
				savedMetadata.getInfoString(meterPropName,
						ModbusDatumDataSource.VIRTUAL_METER_READING_KEY),
				equalTo(datum2.getAccumulatingSampleBigDecimal(meterPropName).toString()));
	}
}
