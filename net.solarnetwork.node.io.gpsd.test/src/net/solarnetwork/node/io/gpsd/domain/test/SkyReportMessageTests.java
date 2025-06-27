/* ==================================================================
 * SkyReportMessageTests.java - 15/11/2019 11:38:24 am
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

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.Test;
import com.fasterxml.jackson.core.TreeNode;
import net.solarnetwork.node.io.gpsd.domain.GpsdMessageType;
import net.solarnetwork.node.io.gpsd.domain.SatelliteInfo;
import net.solarnetwork.node.io.gpsd.domain.SkyReportMessage;

/**
 * Test cases for the {@link SkyReportMessage} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SkyReportMessageTests extends DomainTestSupport {

	@Test
	public void parseSample() {
		// GIVEN
		TreeNode node = readJsonResource("test-sky-01.json");

		// WHEN
		SkyReportMessage msg = SkyReportMessage.builder().parseJsonTree(node);

		// THEN
		assertThat("JSON parsed", msg, notNullValue());
		assertThat("Message type", msg.getMessageType(), equalTo(GpsdMessageType.SkyReport));
		assertThat("Message name", msg.getMessageName(), equalTo(GpsdMessageType.SkyReport.getName()));
		assertThat("Device", msg.getDevice(), equalTo("/dev/pts/1"));
		assertThat("Timestamp", msg.getTimestamp(), equalTo(Instant.parse("2005-07-08T11:28:07.114Z")));
		assertThat("Longitude DOP", msg.getLongitudeDop(), equalTo(new BigDecimal("1.55")));
		assertThat("Latitude DOP", msg.getLatitudeDop(), equalTo(new BigDecimal("2.66")));
		assertThat("Altitude DOP", msg.getAltitudeDop(), equalTo(new BigDecimal("3.77")));
		assertThat("Timestamp DOP", msg.getTimestampDop(), equalTo(new BigDecimal("4.88")));
		assertThat("Horizontal DOP", msg.getHorizontalDop(), equalTo(new BigDecimal("1.24")));
		assertThat("Spherical DOP", msg.getSphericalDop(), equalTo(new BigDecimal("1.99")));
		assertThat("Hyperspherical DOP", msg.getHypersphericalDop(), equalTo(new BigDecimal("2.88")));

		List<SatelliteInfo> satellites = stream(msg.getSatellites().spliterator(), false)
				.collect(toList());
		assertThat("Satellites count", satellites, hasSize(8));

		// @formatter:off
		assertThat("Satellite 1", satellites.get(0), equalTo(SatelliteInfo.builder()
				.withPrn(23)
				.withElevation(6)
				.withAzimuth(84)
				.withSignalStrength(0)
				.withUsed(false).build()));
		assertThat("Satellite 8", satellites.get(7), equalTo(SatelliteInfo.builder()
				.withPrn(27)
				.withElevation(71)
				.withAzimuth(76)
				.withSignalStrength(43)
				.withUsed(true).build()));
		// @formatter:on
	}

}
