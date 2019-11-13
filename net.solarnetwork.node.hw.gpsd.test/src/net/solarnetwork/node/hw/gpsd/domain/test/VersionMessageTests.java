/* ==================================================================
 * VersionMessageTests.java - 13/11/2019 3:00:40 pm
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

package net.solarnetwork.node.hw.gpsd.domain.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import com.fasterxml.jackson.core.TreeNode;
import net.solarnetwork.node.hw.gpsd.domain.GpsdMessageType;
import net.solarnetwork.node.hw.gpsd.domain.VersionMessage;

/**
 * Test cases for the {@link VersionMessage} class.
 * 
 * @author matt
 * @version 1.0
 */
public class VersionMessageTests extends DomainTestSupport {

	@Test
	public void parseSample() {
		// GIVEN
		TreeNode node = readJsonResource("test-version-01.json");

		// WHEN
		VersionMessage msg = VersionMessage.builder().parseJsonTree(node);

		// THEN
		assertThat("JSON parsed", msg, notNullValue());
		assertThat("Message type", msg.getMessageType(), equalTo(GpsdMessageType.Version));
		assertThat("Message name", msg.getMessageName(), equalTo(GpsdMessageType.Version.getName()));
		assertThat("Release", msg.getRelease(), equalTo("2.40dev"));
		assertThat("Revision", msg.getRevision(), equalTo("06f62e14eae9886cde907dae61c124c53eb1101f"));
		assertThat("Protocol major", msg.getProtocolMajor(), equalTo(3));
		assertThat("Protocol minor", msg.getProtocolMinor(), equalTo(1));
		assertThat("Remote URL", msg.getRemoteUrl(), equalTo("http://gpsd.localdomain:9999"));
	}

}
