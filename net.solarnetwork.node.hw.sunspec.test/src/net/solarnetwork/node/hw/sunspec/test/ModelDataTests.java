/* ==================================================================
 * ModelDataTests.java - 22/05/2018 1:47:01 PM
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.node.hw.sunspec.CommonModelAccessor;
import net.solarnetwork.node.hw.sunspec.CommonModelId;
import net.solarnetwork.node.hw.sunspec.CommonModelRegister;
import net.solarnetwork.node.hw.sunspec.ModelData;
import net.solarnetwork.node.hw.sunspec.ModelRegister;
import net.solarnetwork.node.io.modbus.ModbusData.ModbusDataUpdateAction;
import net.solarnetwork.node.io.modbus.ModbusData.MutableModbusData;

/**
 * Test cases for the {@link ModelData} class.
 *
 * @author matt
 * @version 1.2
 */
public class ModelDataTests {

	// @formatter:off
	public static final short[] COMMON_MODEL_02 = new short[] {
			0x0001,
			0x0041,
			0x5665,
			0x7269,
			0x7320,
			0x496E,
			0x6475,
			0x7374,
			0x7269,
			0x6573,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x4535,
			0x3143,
			0x3200,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x4E6F,
			0x6E65,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x322E,
			0x3130,
			0x3300,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x3445,
			0x3339,
			0x3034,
			0x3736,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x0000,
			0x000A,
	};
		
	// @formatter:on

	private static final Logger log = LoggerFactory.getLogger(ModelData.class);

	private ModelData getTestDataInstance() {
		final int baseAddress = ModelRegister.BaseAddress.getAddress();
		ModelData data = new ModelData(baseAddress + 2);
		try {
			data.performUpdates(new ModbusDataUpdateAction() {

				@Override
				public boolean updateModbusData(MutableModbusData m) {
					m.saveDataArray(COMMON_MODEL_02, baseAddress + 2);
					return true;
				}
			});
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		return data;
	}

	@Test
	public void dataDebugString() {
		ModelData data = getTestDataInstance();
		log.debug("Got test data: " + data.dataDebugString());
	}

	@Test
	public void findTypedModel() {
		ModelData data = getTestDataInstance();
		CommonModelAccessor commonAccessor = data.findTypedModel(CommonModelAccessor.class);
		assertThat(commonAccessor, instanceOf(ModelData.class));
	}

	@Test
	public void block() {
		ModelData data = getTestDataInstance();
		assertThat("Model base address", data.getBaseAddress(), equalTo(40002));
		assertThat("Model ID", data.getModelId().getId(), equalTo(CommonModelId.CommonModel.getId()));
		assertThat("Model fixed length", data.getFixedBlockLength(), equalTo(65));
		assertThat("Model repeating instance length", data.getRepeatingBlockInstanceLength(),
				equalTo(0));
		assertThat("Model length", data.getModelLength(), equalTo(65));
		assertThat("Model length", data.getRepeatingBlockInstanceCount(), equalTo(0));
	}

	@Test
	public void commonModelProperties() {
		CommonModelAccessor data = getTestDataInstance();
		assertThat("Manufacturer", data.getManufacturer(), equalTo("Veris Industries"));
		assertThat("Model name", data.getModelName(), equalTo("E51C2"));
		assertThat("Options", data.getOptions(), equalTo("None"));
		assertThat("Version", data.getVersion(), equalTo("2.103"));
		assertThat("Serial number", data.getSerialNumber(), equalTo("4E390476"));
		assertThat("Device address", data.getDeviceAddress(), equalTo(10));
	}

	private static ModelData stringData(byte[] bytes) {
		final CommonModelRegister ref = CommonModelRegister.Options;
		ModelData data = new ModelData(0);
		try {
			data.performUpdates(m -> {
				m.saveBytes(Arrays.copyOf(bytes, ref.getWordLength() * 2), ref.getAddress());
				return true;
			});
		} catch ( IOException e ) {
			throw new RuntimeException(e);
		}
		return data;
	}

	@Test
	public void stringValue_utf8() {
		ModelData data = stringData("Größe".getBytes(StandardCharsets.UTF_8));
		assertThat("UTF-8 decoded", data.getStringValue(CommonModelRegister.Options, 0),
				is(equalTo("Größe")));
	}

	@Test
	public void stringValue_latin1() {
		// a Latin-1 encoded e-acute is not valid UTF-8
		ModelData data = stringData(new byte[] { 'C', 'a', 'f', (byte) 0xE9 });
		assertThat("Invalid UTF-8 replaced", data.getStringValue(CommonModelRegister.Options, 0),
				is(equalTo("Caf�")));
	}

	@Test
	public void stringValue_nullTerminated() {
		ModelData data = stringData(new byte[] { 'A', 'B', 'C', 0, 'x', 'y', 'z' });
		assertThat("Bytes after NULL ignored", data.getStringValue(CommonModelRegister.Options, 0),
				is(equalTo("ABC")));
	}

	@Test
	public void stringValue_trimmed() {
		ModelData data = stringData("  ABC ".getBytes(StandardCharsets.UTF_8));
		assertThat("Whitespace removed", data.getStringValue(CommonModelRegister.Options, 0),
				is(equalTo("ABC")));
	}

	@Test
	public void stringValue_empty() {
		// SunSpec recommends 0x0080 in the first register to represent an empty string
		ModelData data = stringData(new byte[] { 0x00, (byte) 0x80 });
		assertThat("Empty string", data.getStringValue(CommonModelRegister.Options, 0), is(nullValue()));
	}

	@Test
	public void stringValue_notImplemented() {
		ModelData data = stringData(new byte[0]);
		assertThat("All NULL not implemented", data.getStringValue(CommonModelRegister.Options, 0),
				is(nullValue()));
	}

}
