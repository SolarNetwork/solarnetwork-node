/* ==================================================================
 * BacnetObjectTypeTests.java - 8/11/2022 10:05:03 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.io.bacnet.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import org.junit.Test;
import net.solarnetwork.domain.CodedValue;
import net.solarnetwork.node.io.bacnet.BacnetObjectType;

/**
 * Test cases for the {@link BacnetObjectType} class.
 * 
 * @author matt
 * @version 1.0
 */
public class BacnetObjectTypeTests {

	@Test
	public void forCode() {
		assertThat("Code resolved", CodedValue.forCodeValue(48, BacnetObjectType.class, null),
				is(equalTo(BacnetObjectType.PositiveIntegerValue)));
	}

	@Test
	public void forCode_unknown() {
		assertThat("Code not resolved", CodedValue.forCodeValue(-1, BacnetObjectType.class, null),
				is(nullValue()));
	}

	@Test
	public void forKey_code() {
		assertThat("Code value resolved", BacnetObjectType.forKey("48"),
				is(equalTo(BacnetObjectType.PositiveIntegerValue)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void forKey_code_unknown() {
		BacnetObjectType.forKey("-1");
	}

	@Test
	public void forKey_name() {
		assertThat("Name value resolved", BacnetObjectType.forKey("PositiveIntegerValue"),
				is(equalTo(BacnetObjectType.PositiveIntegerValue)));
	}

	@Test
	public void forKey_name_trainCase() {
		assertThat("Name (train case) value resolved", BacnetObjectType.forKey("positive-integer-value"),
				is(equalTo(BacnetObjectType.PositiveIntegerValue)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void forKey_name_unknown() {
		BacnetObjectType.forKey("this-is-not-one");
	}

	@Test(expected = IllegalArgumentException.class)
	public void forKey_name_null() {
		BacnetObjectType.forKey(null);
	}

}
