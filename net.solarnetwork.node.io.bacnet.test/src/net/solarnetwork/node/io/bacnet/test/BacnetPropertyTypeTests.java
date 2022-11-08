/* ==================================================================
 * BacnetPropertyTypeTests.java - 8/11/2022 10:11:08 am
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
import net.solarnetwork.node.io.bacnet.BacnetPropertyType;

/**
 * Test cases for the {@link BacnetPropertyType} class.
 * 
 * @author matt
 * @version 1.0
 */
public class BacnetPropertyTypeTests {

	@Test
	public void forCode() {
		assertThat("Code resolved", CodedValue.forCodeValue(97, BacnetPropertyType.class, null),
				is(equalTo(BacnetPropertyType.ProtocolServicesSupported)));
	}

	@Test
	public void forCode_unknown() {
		assertThat("Code not resolved", CodedValue.forCodeValue(-1, BacnetPropertyType.class, null),
				is(nullValue()));
	}

	@Test
	public void forKey_code() {
		assertThat("Code value resolved", BacnetPropertyType.forKey("97"),
				is(equalTo(BacnetPropertyType.ProtocolServicesSupported)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void forKey_code_unknown() {
		BacnetPropertyType.forKey("-1");
	}

	@Test
	public void forKey_name() {
		assertThat("Name value resolved", BacnetPropertyType.forKey("ProtocolServicesSupported"),
				is(equalTo(BacnetPropertyType.ProtocolServicesSupported)));
	}

	@Test
	public void forKey_name_trainCase() {
		assertThat("Name (train case) value resolved",
				BacnetPropertyType.forKey("protocol-services-supported"),
				is(equalTo(BacnetPropertyType.ProtocolServicesSupported)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void forKey_name_unknown() {
		BacnetPropertyType.forKey("this-is-not-one");
	}

	@Test(expected = IllegalArgumentException.class)
	public void forKey_name_null() {
		BacnetPropertyType.forKey(null);
	}

}
