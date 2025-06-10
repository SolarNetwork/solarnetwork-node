/* ==================================================================
 * StatActionTests.java - 13/08/2018 10:49:30 AM
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

package net.solarnetwork.node.datum.os.stat.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Test;
import net.solarnetwork.node.datum.os.stat.StatAction;

/**
 * Test cases for the {@link StatAction} class.
 * 
 * @author matt
 * @version 1.0
 */
public class StatActionTests {

	@Test
	public void forActionValue() {
		for ( StatAction a : StatAction.values() ) {
			StatAction action = StatAction.forAction(a.getAction());
			assertThat("Action returned by value", action, equalTo(a));
		}
	}

	@Test
	public void forActionEnumValue() {
		for ( StatAction a : StatAction.values() ) {
			StatAction action = StatAction.forAction(a.name());
			assertThat("Action returned by name", action, equalTo(a));
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void forActionUnknown() {
		StatAction.forAction("???");
	}

}
