/* ==================================================================
 * BacnetUtilsTests.java - 8/11/2022 9:32:01 am
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
import net.solarnetwork.node.io.bacnet.BacnetUtils;

/**
 * Test cases for the {@link BacnetUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class BacnetUtilsTests {

	@Test
	public void trainToCamel() {
		assertThat(BacnetUtils.trainToCamelCase("analog-input"), is(equalTo("AnalogInput")));
	}

	@Test
	public void trainToCamel_many() {
		assertThat(BacnetUtils.trainToCamelCase("the-wheels-on-the-train-go-round-and-round"),
				is(equalTo("TheWheelsOnTheTrainGoRoundAndRound")));
	}

	@Test
	public void trainToCamel_null() {
		assertThat("Null converts to null", BacnetUtils.trainToCamelCase(null), is(nullValue()));
	}

	@Test
	public void trainToCamel_empty() {
		assertThat("Empty unchanged", BacnetUtils.trainToCamelCase(""), is(equalTo("")));
	}

	@Test
	public void trainToCamel_noDashes() {
		final String input = "this does Not have? Any dashes";
		assertThat("No dashes unchanged", BacnetUtils.trainToCamelCase(input), is(equalTo(input)));
	}

	@Test
	public void trainToCamel_spacePrior() {
		final String input = "why - why this";
		assertThat("Spaces around converted", BacnetUtils.trainToCamelCase(input),
				is(equalTo("Why  why this")));
	}

	@Test
	public void trainToCamel_alreadyCapitalized() {
		final String input = "Foo-Bar";
		assertThat("Dashes removed", BacnetUtils.trainToCamelCase(input), is(equalTo("FooBar")));
	}

	@Test
	public void trainToCamel_leadingDash() {
		final String input = "-foo-bar";
		assertThat("Leading dash removed", BacnetUtils.trainToCamelCase(input), is(equalTo("FooBar")));
	}

	@Test
	public void trainToCamel_trailingDash() {
		final String input = "foo-bar-";
		assertThat("Trailing dash removed", BacnetUtils.trainToCamelCase(input), is(equalTo("FooBar")));
	}

}
