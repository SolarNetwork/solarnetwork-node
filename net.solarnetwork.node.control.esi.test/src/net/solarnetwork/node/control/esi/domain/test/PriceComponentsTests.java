/* ==================================================================
 * PriceComponentsTests.java - 9/08/2019 2:48:39 pm
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

package net.solarnetwork.node.control.esi.domain.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Map;
import org.junit.Test;
import net.solarnetwork.node.control.esi.domain.PriceComponents;

/**
 * Test cases for the {@link PriceComponents} class.
 * 
 * @author matt
 * @version 1.0
 */
public class PriceComponentsTests {

	private static final Currency USD = Currency.getInstance("USD");

	@Test
	public void equalityEqualsAll() {
		// given
		PriceComponents p1 = new PriceComponents(USD, new BigDecimal("2.34"));
		PriceComponents p2 = new PriceComponents(USD, new BigDecimal("2.34"));

		// then
		assertThat("Equal", p1, equalTo(p2));
	}

	@Test
	public void equalityEqualsNoApparentPrice() {
		// given
		PriceComponents p1 = new PriceComponents(USD, null);
		PriceComponents p2 = new PriceComponents(USD, null);

		// then
		assertThat("Equal", p1, equalTo(p2));
	}

	@Test
	public void equalityEqualsNoCurrencyCode() {
		// given
		PriceComponents p1 = new PriceComponents(null, new BigDecimal("2.34"));
		PriceComponents p2 = new PriceComponents(null, new BigDecimal("2.34"));

		// then
		assertThat("Equal", p1, equalTo(p2));
	}

	@Test
	public void scaledExactly() {
		// given
		PriceComponents p = new PriceComponents(USD, new BigDecimal("2.34"));

		// when
		PriceComponents result = p.scaledExactly(2);

		// then
		assertThat("Results equal", result, equalTo(p));
	}

	@Test(expected = ArithmeticException.class)
	public void scaledExactlyException() {
		// given
		PriceComponents p = new PriceComponents(USD, new BigDecimal("2.34"));

		// when
		p.scaledExactly(1);
	}

	@Test
	public void scaledWithRounding() {
		// given
		PriceComponents p = new PriceComponents(USD, new BigDecimal("3.45"));

		// when
		PriceComponents result = p.scaled(1);

		// then
		assertThat("Results equal", result, equalTo(new PriceComponents(USD, new BigDecimal("3.5"))));
	}

	@Test
	public void asMap() {
		// given
		BigDecimal vahPrice = new BigDecimal("1.2345");
		PriceComponents pc = new PriceComponents(USD, vahPrice);

		// when
		Map<String, Object> m = pc.asMap();

		// then
		assertThat("Map size", m.keySet(), hasSize(2));
		assertThat("Map value", m, allOf(hasEntry("currencyCode", "USD"),
				hasEntry("apparentEnergyPriceValue", vahPrice.toPlainString())));
	}

	@Test
	public void currencyCodeAccessorGet() {
		// given
		PriceComponents pc = new PriceComponents(USD, null);

		// when
		String result = pc.getCurrencyCode();

		// then
		assertThat("Currency code", result, equalTo("USD"));
	}

	@Test
	public void currencyCodeAccessorSet() {
		// given
		PriceComponents pc = new PriceComponents();

		// when
		pc.setCurrencyCode("USD");

		// then
		assertThat("Currency", pc.getCurrency(), equalTo(USD));
	}

	@Test
	public void apparentEnergyValueAccessorGet() {
		// given
		String vah = "1.2345";
		PriceComponents pc = new PriceComponents(USD, new BigDecimal(vah));

		// when
		String result = pc.getApparentEnergyPriceValue();

		// then
		assertThat("Apparent energy price value", result, equalTo(vah));
	}

	@Test
	public void apparentEnergyValueAccessorSet() {
		// given
		PriceComponents pc = new PriceComponents(USD, null);

		// when
		final String vah = "1.2345";
		pc.setApparentEnergyPriceValue(vah);

		// then
		assertThat("Apparent energy price", pc.getApparentEnergyPrice(), equalTo(new BigDecimal(vah)));
	}

}
