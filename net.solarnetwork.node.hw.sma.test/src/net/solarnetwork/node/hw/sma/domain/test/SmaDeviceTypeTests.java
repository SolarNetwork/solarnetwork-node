/* ==================================================================
 * SmaDeviceTypeTests.java - 11/09/2020 11:36:32 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.hw.sma.domain.test;

import static org.junit.Assert.fail;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Test;
import net.solarnetwork.node.hw.sma.domain.SmaDeviceType;

/**
 * Unit tests for the {@link SmaDeviceType} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SmaDeviceTypeTests {

	@Test
	public void noDuplicateCodes() {
		Map<Integer, SmaDeviceType> types = new TreeMap<>();
		for ( SmaDeviceType t : SmaDeviceType.values() ) {
			Integer c = t.getCode();
			SmaDeviceType prev = types.get(c);
			if ( prev != null ) {
				fail("Duplicate code " + c + " in " + t + ", already used by " + prev);
			}
			types.put(c, t);
		}
	}

	@Test
	public void noDuplicateDescriptions() {
		Map<String, SmaDeviceType> descs = new TreeMap<>();
		for ( SmaDeviceType t : SmaDeviceType.values() ) {
			String s = t.getDescription();
			SmaDeviceType prev = descs.get(s);
			if ( prev != null ) {
				fail("Duplicate description " + s + " in " + t + ", already used by " + prev);
			}
			descs.put(s, t);
		}
	}

}
