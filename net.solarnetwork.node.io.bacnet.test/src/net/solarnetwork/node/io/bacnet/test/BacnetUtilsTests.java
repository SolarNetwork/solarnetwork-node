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
	public void kebabToCamel() {
		assertThat(BacnetUtils.kebabToCamelCase("analog-input"), is(equalTo("AnalogInput")));
	}

	@Test
	public void kebabToCamel_many() {
		assertThat(BacnetUtils.kebabToCamelCase("the-wheels-on-the-kebab-go-round-and-round"),
				is(equalTo("TheWheelsOnTheKebabGoRoundAndRound")));
	}

	@Test
	public void kebabToCamel_null() {
		assertThat("Null converts to null", BacnetUtils.kebabToCamelCase(null), is(nullValue()));
	}

	@Test
	public void kebabToCamel_empty() {
		assertThat("Empty unchanged", BacnetUtils.kebabToCamelCase(""), is(equalTo("")));
	}

	@Test
	public void kebabToCamel_noDashes() {
		final String input = "This does Not have? Any dashes";
		assertThat("No dashes capitalized", BacnetUtils.kebabToCamelCase(input), is(equalTo(input)));
	}

	@Test
	public void kebabToCamel_spacePrior() {
		final String input = "why - why this";
		assertThat("Spaces around converted", BacnetUtils.kebabToCamelCase(input),
				is(equalTo("Why  why this")));
	}

	@Test
	public void kebabToCamel_alreadyCapitalized() {
		final String input = "Foo-Bar";
		assertThat("Dashes removed", BacnetUtils.kebabToCamelCase(input), is(equalTo("FooBar")));
	}

	@Test
	public void kebabToCamel_leadingDash() {
		final String input = "-foo-bar";
		assertThat("Leading dash removed", BacnetUtils.kebabToCamelCase(input), is(equalTo("FooBar")));
	}

	@Test
	public void kebabToCamel_trailingDash() {
		final String input = "foo-bar-";
		assertThat("Trailing dash removed", BacnetUtils.kebabToCamelCase(input), is(equalTo("FooBar")));
	}

	@Test
	public void camelToKebab() {
		assertThat(BacnetUtils.camelToKebabCase("AnalogInput"), is(equalTo("analog-input")));
	}

	@Test
	public void camelToKebab_many() {
		assertThat(BacnetUtils.camelToKebabCase("TheWheelsOnTheKebabGoRoundAndRound"),
				is(equalTo("the-wheels-on-the-kebab-go-round-and-round")));
	}

	@Test
	public void camelToKebab_null() {
		assertThat("Null converts to null", BacnetUtils.camelToKebabCase(null), is(nullValue()));
	}

	@Test
	public void camelToKebab_empty() {
		assertThat("Empty unchanged", BacnetUtils.camelToKebabCase(""), is(equalTo("")));
	}

	@Test
	public void camelToKebab_noCaps() {
		final String input = "this does not have? any capital letters";
		assertThat("No caps unchanged", BacnetUtils.camelToKebabCase(input), is(equalTo(input)));
	}

	@Test
	public void camelToKebab_trailingCap() {
		final String input = "FooBarZ";
		assertThat("Trailing cap dashed", BacnetUtils.camelToKebabCase(input), is(equalTo("foo-bar-z")));
	}
}
