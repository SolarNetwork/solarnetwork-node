/* ==================================================================
 * GpsdMessageDeserializerTests.java - 12/11/2019 10:15:41 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.gpsd.util.test;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import java.io.IOException;
import java.time.Instant;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.codec.ObjectMapperFactoryBean;
import net.solarnetwork.node.io.gpsd.domain.GpsdMessage;
import net.solarnetwork.node.io.gpsd.domain.NmeaMode;
import net.solarnetwork.node.io.gpsd.domain.TpvReportMessage;
import net.solarnetwork.node.io.gpsd.domain.test.TpvReportMessageTests;
import net.solarnetwork.node.io.gpsd.util.GpsdMessageDeserializer;

/**
 * Test cases for the {@link GpsdMessageDeserializer} class.
 * 
 * @author matt
 * @version 1.0
 */
public class GpsdMessageDeserializerTests {

	private <T> T parseJsonResource(ObjectMapper mapper, Class<?> clazz, String resource,
			Class<T> type) {
		try {
			return mapper.readValue(clazz.getResourceAsStream(resource), type);
		} catch ( IOException e ) {
			throw new RuntimeException("Error reading resource [" + resource + "]", e);
		}
	}

	private ObjectMapper createObjectMapper() {
		ObjectMapperFactoryBean factory = new ObjectMapperFactoryBean();
		factory.setFeaturesToDisable(asList(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
		factory.setFeaturesToEnable(asList(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS));
		factory.setDeserializers(asList(new GpsdMessageDeserializer()));
		try {
			return factory.getObject();
		} catch ( Exception e ) {
			throw new RuntimeException(e);
		}
	}

	private ObjectMapper mapper;

	@Before
	public void setup() {
		mapper = createObjectMapper();
	}

	@Test
	public void parseTpvReport() {
		// WHEN
		GpsdMessage result = parseJsonResource(mapper, TpvReportMessageTests.class, "test-tpv-01.json",
				GpsdMessage.class);

		// THEN
		assertThat("Message parsed", result, instanceOf(TpvReportMessage.class));
		TpvReportMessage msg = (TpvReportMessage) result;
		assertThat("Device", msg.getDevice(), equalTo("/dev/pts/1"));
		assertThat("Mode", msg.getMode(), equalTo(NmeaMode.ThreeDimensional));
		assertThat("Timestamp", msg.getTimestamp(), equalTo(Instant.parse("2005-06-08T10:34:48.283Z")));
	}

}
