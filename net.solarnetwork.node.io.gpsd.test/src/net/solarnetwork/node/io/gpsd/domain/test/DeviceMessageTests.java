/* ==================================================================
 * DeviceMessageTests.java - 14/11/2019 4:42:44 pm
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

package net.solarnetwork.node.io.gpsd.domain.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import org.junit.Test;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.node.io.gpsd.domain.DeviceFlags;
import net.solarnetwork.node.io.gpsd.domain.DeviceMessage;
import net.solarnetwork.node.io.gpsd.domain.GpsdMessageType;
import net.solarnetwork.node.io.gpsd.domain.Parity;

/**
 * Test cases for the {@link DeviceMessage} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DeviceMessageTests extends DomainTestSupport {

	@Test
	public void parseSample() {
		// GIVEN
		TreeNode node = readJsonResource("test-device-01.json");

		// WHEN
		DeviceMessage msg = DeviceMessage.builder().parseJsonTree(node);

		// THEN
		assertThat("JSON parsed", msg, notNullValue());
		assertThat("Message type", msg.getMessageType(), equalTo(GpsdMessageType.Device));
		assertThat("Message name", msg.getMessageName(), equalTo(GpsdMessageType.Device.getName()));
		assertThat("Path", msg.getPath(), equalTo("/dev/pts/1"));
		assertThat("Activated", msg.getActivated(), equalTo(Instant.parse("2005-06-08T10:34:48.283Z")));
		assertThat("Flags", msg.getFlags(),
				equalTo(EnumSet.of(DeviceFlags.SeenGps, DeviceFlags.SeenRtcm3)));
		assertThat("BPS", msg.getBitsPerSecond(), equalTo(4800));
		assertThat("Parity", msg.getParity(), equalTo(Parity.None));
		assertThat("Stopbits", msg.getStopbits(), equalTo(1));
		assertThat("Native", msg.isNativeMode(), equalTo(false));
		assertThat("Cycle", msg.getCycleSeconds(), equalTo(new BigDecimal("1.5")));
		assertThat("Mincycle", msg.getMinimumCycleSeconds(), equalTo(new BigDecimal("0.5")));
	}

	@Test
	public void serializeSample() throws Exception {
		// GIVEN
		ObjectMapper m = new ObjectMapper();
		DeviceMessage msg = DeviceMessage.builder().withPath("/dev/pts/2").withBitsPerSecond(9600)
				.withParity(Parity.Even).withStopbits(2).withNativeMode(true).build();

		// WHEN
		String json = m.writeValueAsString(msg);

		// THEN
		assertThat("JSON", json, equalTo(
				"{\"class\":\"DEVICE\",\"path\":\"/dev/pts/2\",\"bps\":9600,\"parity\":\"E\",\"stopbits\":2,\"native\":1}"));
	}

}
