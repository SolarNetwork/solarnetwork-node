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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import java.io.IOException;
import java.math.BigDecimal;
import org.apache.commons.codec.binary.Hex;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.node.domain.datum.SimpleDatum;
import net.solarnetwork.node.io.protobuf.DatumFieldConfig;
import net.solarnetwork.node.io.protobuf.DatumProtobufObjectCodec;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link DatumProtobufObjectCodec}.
 * 
 * @author matt
 * @version 2.0
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

		DatumFieldConfig energyConfig = new DatumFieldConfig("wattHours", DatumSamplesType.Accumulating,
				"energy");
		DatumFieldConfig powerConfig = new DatumFieldConfig("volts", DatumSamplesType.Instantaneous,
				"voltage");
		DatumFieldConfig latConfig = new DatumFieldConfig("lat", DatumSamplesType.Instantaneous,
				"location.lat");
		DatumFieldConfig lonConfig = new DatumFieldConfig("lon", DatumSamplesType.Instantaneous,
				"location.lon");
		codec.setPropConfigs(new DatumFieldConfig[] { energyConfig, powerConfig, latConfig, lonConfig });

		Resource protoResource = new ClassPathResource("my-datum.proto", getClass());
		codec.applySettingResources(RESOURCE_KEY_PROTO_FILES, singleton(protoResource));
	}

	@Test
	public void encode_datum() throws IOException {
		// GIVEN
		SimpleDatum d = SimpleDatum.nodeDatum(null);
		d.getSamples().putInstantaneousSampleValue("volts", 1.234);
		d.getSamples().putInstantaneousSampleValue("lat", 2.345);
		d.getSamples().putInstantaneousSampleValue("lon", 3.456);
		d.getSamples().putAccumulatingSampleValue("wattHours", 123456);

		// WHEN
		byte[] data = codec.encodeAsBytes(d, null);

		// THEN
		String hex = Hex.encodeHexString(data);
		assertThat("Data encoded", hex, equalTo(TEST_MSG));
	}

	@Test
	public void encode_datum_map() throws IOException {
		// GIVEN
		SimpleDatum d = SimpleDatum.nodeDatum(null);
		d.getSamples().putInstantaneousSampleValue("volts", 1.234);
		d.getSamples().putInstantaneousSampleValue("lat", 2.345);
		d.getSamples().putInstantaneousSampleValue("lon", 3.456);
		d.getSamples().putAccumulatingSampleValue("wattHours", 123456);

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
		assertThat("Data decoded", result, instanceOf(SimpleDatum.class));
		SimpleDatum d = (SimpleDatum) result;
		assertThat("Volts decoded", d.getSamples().getInstantaneousSampleBigDecimal("volts"),
				equalTo(new BigDecimal("1.234")));
		assertThat("Lat decoded", d.getSamples().getInstantaneousSampleBigDecimal("lat"),
				equalTo(new BigDecimal("2.345")));
		assertThat("Lon decoded", d.getSamples().getInstantaneousSampleBigDecimal("lon"),
				equalTo(new BigDecimal("3.456")));
		assertThat("wattHours decoded", d.getSamples().getAccumulatingSampleLong("wattHours"),
				equalTo(123456L));
	}

}
