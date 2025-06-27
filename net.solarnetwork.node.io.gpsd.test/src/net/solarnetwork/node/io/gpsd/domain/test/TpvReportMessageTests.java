/* ==================================================================
 * TpvReportMessageTests.java - 12/11/2019 9:51:44 am
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
import org.junit.Test;
import com.fasterxml.jackson.core.TreeNode;
import net.solarnetwork.node.io.gpsd.domain.GpsdMessageType;
import net.solarnetwork.node.io.gpsd.domain.NmeaMode;
import net.solarnetwork.node.io.gpsd.domain.TpvReportMessage;

/**
 * Test cases for the {@link TpvReportMessage} class.
 * 
 * @author matt
 * @version 1.0
 */
public class TpvReportMessageTests extends DomainTestSupport {

	@Test
	public void parseSample() {
		// GIVEN
		TreeNode node = readJsonResource("test-tpv-01.json");

		// WHEN
		TpvReportMessage msg = TpvReportMessage.builder().parseJsonTree(node);

		// THEN
		assertThat("JSON parsed", msg, notNullValue());
		assertThat("Message type", msg.getMessageType(), equalTo(GpsdMessageType.TpvReport));
		assertThat("Message name", msg.getMessageName(), equalTo(GpsdMessageType.TpvReport.getName()));
		assertThat("Device", msg.getDevice(), equalTo("/dev/pts/1"));
		assertThat("Mode", msg.getMode(), equalTo(NmeaMode.ThreeDimensional));
		assertThat("Timestamp", msg.getTimestamp(), equalTo(Instant.parse("2005-06-08T10:34:48.283Z")));
		assertThat("Timestamp error", msg.getTimestampError(), equalTo(new BigDecimal("0.005")));
		assertThat("Latitude", msg.getLatitude(), equalTo(new BigDecimal("46.498293369")));
		assertThat("Longitude", msg.getLongitude(), equalTo(new BigDecimal("7.567411672")));
		assertThat("Altitude", msg.getAltitude(), equalTo(new BigDecimal("1343.127")));
		assertThat("Longitude error", msg.getLongitudeError(), equalTo(new BigDecimal("10.01")));
		assertThat("Latitude error", msg.getLatitudeError(), equalTo(new BigDecimal("9.01")));
		assertThat("Altitude error", msg.getAltitudeError(), equalTo(new BigDecimal("32.321")));
		assertThat("Course", msg.getCourse(), equalTo(new BigDecimal("10.3788")));
		assertThat("Speed", msg.getSpeed(), equalTo(new BigDecimal("0.091")));
		assertThat("Climb rate", msg.getClimbRate(), equalTo(new BigDecimal("-0.085")));
		assertThat("Course error", msg.getCourseError(), equalTo(new BigDecimal("0.123")));
		assertThat("Speed error", msg.getSpeedError(), equalTo(new BigDecimal("1.234")));
		assertThat("Climb rate error", msg.getClimbRateError(), equalTo(new BigDecimal("2.345")));
	}

}
