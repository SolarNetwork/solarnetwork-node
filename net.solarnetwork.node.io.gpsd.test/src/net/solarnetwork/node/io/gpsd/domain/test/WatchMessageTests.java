/* ==================================================================
 * WatchMessageTests.java - 12/11/2019 11:14:43 am
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
import org.junit.Test;
import com.fasterxml.jackson.core.TreeNode;
import net.solarnetwork.node.io.gpsd.domain.GpsdMessageType;
import net.solarnetwork.node.io.gpsd.domain.WatchMessage;

/**
 * Test cases for the {@link WatchMessage} class.
 * 
 * @author matt
 * @version 1.0
 */
public class WatchMessageTests extends DomainTestSupport {

	@Test
	public void parseSample() {
		// GIVEN
		TreeNode node = readJsonResource("test-watch-01.json");

		// WHEN
		WatchMessage msg = WatchMessage.builder().parseJsonTree(node);

		// THEN
		assertThat("JSON parsed", msg, notNullValue());
		assertThat("Message type", msg.getMessageType(), equalTo(GpsdMessageType.Watch));
		assertThat("Message name", msg.getMessageName(), equalTo(GpsdMessageType.Watch.getName()));
		assertThat("Device", msg.getDevice(), equalTo("/dev/pts/1"));
		assertThat("Enable mode", msg.isEnable(), equalTo(true));
		assertThat("Dump JSON mode", msg.isDumpJson(), equalTo(true));
		assertThat("Scaled mode", msg.isScaled(), equalTo(true));
	}

}
