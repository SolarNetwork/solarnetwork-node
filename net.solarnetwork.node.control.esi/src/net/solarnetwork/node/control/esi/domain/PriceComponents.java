/* ==================================================================
 * PriceComponents.java - 9/08/2019 1:30:46 pm
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

package net.solarnetwork.node.control.esi.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * Components of price.
 *
 * @author matt
 * @version 2.0
 */
public class PriceComponents {

	private Currency currency;
	private BigDecimal apparentEnergyPrice;

	/**
	 * Default constructor.
	 */
	public PriceComponents() {
		super();
	}

	/**
	 * Construct with values.
	 *
	 * @param currency
	 *        the currency
	 * @param apparentEnergyPrice
	 *        the apparent energy price, in units per volt-amp hour (VAh)
	 */
	public PriceComponents(Currency currency, BigDecimal apparentEnergyPrice) {
		super();
		this.currency = currency;
		this.apparentEnergyPrice = apparentEnergyPrice;
	}

	/**
	 * Create a price components out of string values.
	 *
	 * @param currencyCode
	 *        the currency code
	 * @param apparentEnergyPrice
	 *        the apparent energy price, suitable for passing to
	 *        {@link BigDecimal#BigDecimal(String)}
	 * @return the new instance
	 */
	public static PriceComponents of(String currencyCode, String apparentEnergyPrice) {
		return new PriceComponents(Currency.getInstance(currencyCode),
				new BigDecimal(apparentEnergyPrice));
	}

	/**
	 * Add settings for this class to a list.
	 *
	 * @param prefix
	 *        an optional prefix to use for all setting keys
	 * @param results
	 *        the list to add settings to
	 */
	public static void addSettings(String prefix, List<SettingSpecifier> results) {
		if ( prefix == null ) {
			prefix = "";
		}
		results.add(new BasicTextFieldSettingSpecifier(prefix + "currencyCode", ""));
		results.add(new BasicTextFieldSettingSpecifier(prefix + "apparentEnergyPriceValue", ""));
	}

	/**
	 * Get the power components data as a Map.
	 *
	 * @return a map of the properties of this class
	 */
	public Map<String, Object> asMap() {
		Map<String, Object> map = new LinkedHashMap<>(8);
		map.put("currencyCode", currency().getCurrencyCode());
		map.put("apparentEnergyPriceValue", apparentEnergyPrice().toPlainString());
		return map;
	}

	/**
	 * Create a copy of this instance.
	 *
	 * @return the new copy
	 */
	public PriceComponents copy() {
		PriceComponents c = new PriceComponents();
		c.setCurrency(getCurrency());
		c.setApparentEnergyPrice(getApparentEnergyPrice());
		return c;
	}

	@Override
	public int hashCode() {
		return Objects.hash(apparentEnergyPrice, currency);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( !(obj instanceof PriceComponents) ) {
			return false;
		}
		PriceComponents other = (PriceComponents) obj;
		return Objects.equals(apparentEnergyPrice, other.apparentEnergyPrice)
				&& Objects.equals(currency, other.currency);
	}

	@Override
	public String toString() {
		return "PriceComponents{currency=" + currency + ", apparentEnergyPrice=" + apparentEnergyPrice
				+ "}";
	}

	/**
	 * Create a copy of the price components with a specific decimal scale.
	 *
	 * @param scale
	 *        the desired scale
	 * @param roundingMode
	 *        the rounding mode to use
	 * @return the new price components
	 */
	public PriceComponents scaled(int scale, RoundingMode roundingMode) {
		return new PriceComponents(currency,
				apparentEnergyPrice != null ? apparentEnergyPrice.setScale(scale, roundingMode) : null);
	}

	/**
	 * Create a copy of the price components with a specific decimal scale, with
	 * {@link RoundingMode#HALF_UP} rounding.
	 *
	 * @param scale
	 *        the desired scale
	 * @return the new price components
	 */
	public PriceComponents scaled(int scale) {
		return scaled(scale, RoundingMode.HALF_UP);
	}

	/**
	 * Create a copy of the price components with a specific decimal scale,
	 * without rounding.
	 *
	 * @param scale
	 *        the desired scale
	 * @return the new price components
	 * @throws ArithmeticException
	 *         if any price cannot be set to the given scale without rounding
	 */
	public PriceComponents scaledExactly(int scale) {
		return scaled(scale, RoundingMode.UNNECESSARY);
	}

	/**
	 * Get the currency.
	 *
	 * @return the currency
	 */
	public Currency getCurrency() {
		return currency;
	}

	/**
	 * Set the currency.
	 *
	 * @param currency
	 *        the currency to set
	 */
	public void setCurrency(Currency currency) {
		this.currency = currency;
	}

	/**
	 * Get the currency code.
	 *
	 * <p>
	 * This returns the currency code from link {@link #getCurrency()}}.
	 * </p>
	 *
	 * @return the currency code, or {@literal null}
	 */
	public String getCurrencyCode() {
		Currency c = getCurrency();
		return (c != null ? c.getCurrencyCode() : null);
	}

	/**
	 * Set the currency via a currency code.
	 *
	 * @param currencyCode
	 *        the currency code to set
	 * @throws IllegalArgumentException
	 *         if <code>currencyCode</code> is not a supported ISO 4217 code.
	 */
	public void setCurrencyCode(String currencyCode) {
		setCurrency(currencyCode != null ? Currency.getInstance(currencyCode) : null);
	}

	/**
	 * Get a non-null currency value.
	 *
	 * <p>
	 * If the currency is not set, this will return the default currency for the
	 * default locale.
	 * </p>
	 *
	 * @return the currency, or a default
	 */
	public Currency currency() {
		Currency c = getCurrency();
		return (c != null ? c : Currency.getInstance(Locale.getDefault()));
	}

	/**
	 * Get the apparent energy price.
	 *
	 * @return the apparent energy price, in units per volt-amp hour (VAh), or
	 *         {@literal null} if no price available
	 */
	public BigDecimal getApparentEnergyPrice() {
		return apparentEnergyPrice;
	}

	/**
	 * Set the apparent energy price.
	 *
	 * @param apparentEnergyPrice
	 *        the price to set, in units per volt-amp hour (VAh)
	 */
	public void setApparentEnergyPrice(BigDecimal apparentEnergyPrice) {
		this.apparentEnergyPrice = apparentEnergyPrice;
	}

	/**
	 * Get the apparent energy price as a string value.
	 *
	 * @return the apparent energy price as a string, or {@literal null}
	 */
	public String getApparentEnergyPriceValue() {
		BigDecimal d = getApparentEnergyPrice();
		return (d != null ? d.toPlainString() : null);
	}

	/**
	 * Set the apparent energy price as a string value.
	 *
	 * @param value
	 *        the apparent energy value, or {@literal null}
	 * @throws NumberFormatException
	 *         if {@code value} is not a valid representation of a
	 *         {@code BigDecimal}.
	 */
	public void setApparentEnergyPriceValue(String value) {
		setApparentEnergyPrice(value != null ? new BigDecimal(value) : null);
	}

	/**
	 * Get a non-null apparent energy price.
	 *
	 * <p>
	 * If the apparent energy price is not set, {@literal 0} will be returned.
	 * </p>
	 *
	 * @return the apparent energy price, or {@literal 0}
	 */
	public BigDecimal apparentEnergyPrice() {
		BigDecimal d = getApparentEnergyPrice();
		return (d != null ? d : BigDecimal.ZERO);
	}

}
