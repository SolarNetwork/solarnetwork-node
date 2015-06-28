/* ==================================================================
 * SwitchConfigPrioritycomparatorTests.java - 28/06/2015 5:53:16 pm
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.control.loadshedder.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.solarnetwork.node.control.loadshedder.SwitchConfig;
import net.solarnetwork.node.control.loadshedder.SwitchConfigPriorityComparator;
import net.solarnetwork.node.test.AbstractNodeTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test cases for the {@link SwitchConfigPriorityComparator} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SwitchConfigPrioritycomparatorTests extends AbstractNodeTest {

	@Test
	public void orderNoPriority() {
		SwitchConfig c1 = new SwitchConfig("/power/switch/3");
		SwitchConfig c2 = new SwitchConfig("/power/switch/1");
		SwitchConfig c3 = new SwitchConfig("/power/switch/2");
		List<SwitchConfig> l = Arrays.asList(c1, c2, c3);
		Collections.sort(l, SwitchConfigPriorityComparator.COMPARATOR);
		Assert.assertEquals("/power/switch/1", l.get(0).getControlId());
		Assert.assertEquals("/power/switch/2", l.get(1).getControlId());
		Assert.assertEquals("/power/switch/3", l.get(2).getControlId());
	}

	@Test
	public void orderWithPriority() {
		SwitchConfig c1 = new SwitchConfig("/power/switch/1", 3);
		SwitchConfig c2 = new SwitchConfig("/power/switch/2", 1);
		SwitchConfig c3 = new SwitchConfig("/power/switch/3", 2);
		List<SwitchConfig> l = Arrays.asList(c1, c2, c3);
		Collections.sort(l, SwitchConfigPriorityComparator.COMPARATOR);
		Assert.assertEquals("/power/switch/2", l.get(0).getControlId());
		Assert.assertEquals("/power/switch/3", l.get(1).getControlId());
		Assert.assertEquals("/power/switch/1", l.get(2).getControlId());
	}

	@Test
	public void orderWithAndWithoutPriority() {
		SwitchConfig c1 = new SwitchConfig("/power/switch/1");
		SwitchConfig c2 = new SwitchConfig("/power/switch/2");
		SwitchConfig c3 = new SwitchConfig("/power/switch/3", 1);
		List<SwitchConfig> l = Arrays.asList(c1, c2, c3);
		Collections.sort(l, SwitchConfigPriorityComparator.COMPARATOR);
		Assert.assertEquals("/power/switch/3", l.get(0).getControlId());
		Assert.assertEquals("/power/switch/1", l.get(1).getControlId());
		Assert.assertEquals("/power/switch/2", l.get(2).getControlId());
	}

}
