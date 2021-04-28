/* ==================================================================
 * DatumProtobufObjectCodecTests.java - 28/04/2021 10:38:24 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.protobuf.test;

import static java.util.Collections.singleton;
import static net.solarnetwork.node.io.protobuf.DatumProtobufObjectCodec.RESOURCE_KEY_PROTO_FILES;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.math.BigDecimal;
import org.apache.commons.codec.binary.Hex;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.node.domain.GeneralNodeDatum;
import net.solarnetwork.node.io.protobuf.DatumFieldConfig;
import net.solarnetwork.node.io.protobuf.DatumProtobufObjectCodec;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link DatumProtobufObjectCodec}.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumProtobufObjectCodecTests extends BaseProtocProtobufCompilerServiceTestSupport {

	private static final String TEST_MSG = "095839b4c876bef33f18c0c4072a1209c3f5285c8fc2024011d9cef753e3a50b40";

	private DatumProtobufObjectCodec codec;

	@Override
	@Before
	public void setup() throws IOException {
		super.setup();

		codec = new DatumProtobufObjectCodec();
		codec.setCompilerService(new StaticOptionalService<>(protocService));
		codec.setMessageClassName("sn.PowerDatum");

		DatumFieldConfig energyConfig = new DatumFieldConfig("wattHours",
				GeneralDatumSamplesType.Accumulating, "energy");
		DatumFieldConfig powerConfig = new DatumFieldConfig("volts",
				GeneralDatumSamplesType.Instantaneous, "voltage");
		DatumFieldConfig latConfig = new DatumFieldConfig("lat", GeneralDatumSamplesType.Instantaneous,
				"location.lat");
		DatumFieldConfig lonConfig = new DatumFieldConfig("lon", GeneralDatumSamplesType.Instantaneous,
				"location.lon");
		codec.setPropConfigs(new DatumFieldConfig[] { energyConfig, powerConfig, latConfig, lonConfig });

		Resource protoResource = new ClassPathResource("my-datum.proto", getClass());
		codec.applySettingResources(RESOURCE_KEY_PROTO_FILES, singleton(protoResource));
	}

	@Test
	public void encode_datum() throws IOException {
		// GIVEN
		GeneralNodeDatum d = new GeneralNodeDatum();
		d.putInstantaneousSampleValue("volts", 1.234);
		d.putInstantaneousSampleValue("lat", 2.345);
		d.putInstantaneousSampleValue("lon", 3.456);
		d.putAccumulatingSampleValue("wattHours", 123456);

		// WHEN
		byte[] data = codec.encodeAsBytes(d, null);

		// THEN
		String hex = Hex.encodeHexString(data);
		assertThat("Data encoded", hex, equalTo(TEST_MSG));
	}

	@Test
	public void encode_datum_map() throws IOException {
		// GIVEN
		GeneralNodeDatum d = new GeneralNodeDatum();
		d.putInstantaneousSampleValue("volts", 1.234);
		d.putInstantaneousSampleValue("lat", 2.345);
		d.putInstantaneousSampleValue("lon", 3.456);
		d.putAccumulatingSampleValue("wattHours", 123456);

		// WHEN
		byte[] data = codec.encodeAsBytes(d.asSimpleMap(), null);

		// THEN
		String hex = Hex.encodeHexString(data);
		assertThat("Data encoded", hex, equalTo(TEST_MSG));
	}

	@Test
	public void decode_datum() throws Exception {
		// GIVEN

		// WHEN
		Object result = codec.decodeFromBytes(Hex.decodeHex(TEST_MSG), null);

		// THEN
		assertThat("Data decoded", result, instanceOf(GeneralNodeDatum.class));
		GeneralNodeDatum d = (GeneralNodeDatum) result;
		assertThat("Volts decoded", d.getInstantaneousSampleBigDecimal("volts"),
				equalTo(new BigDecimal("1.234")));
		assertThat("Lat decoded", d.getInstantaneousSampleBigDecimal("lat"),
				equalTo(new BigDecimal("2.345")));
		assertThat("Lon decoded", d.getInstantaneousSampleBigDecimal("lon"),
				equalTo(new BigDecimal("3.456")));
		assertThat("wattHours decoded", d.getAccumulatingSampleLong("wattHours"), equalTo(123456L));
	}

}
