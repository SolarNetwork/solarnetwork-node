/* ==================================================================
 * PriceMap.java - 9/08/2019 5:31:07 pm
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

import static java.lang.String.format;
import static net.solarnetwork.util.NumberUtils.scaled;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.MessageSource;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.support.BasicTextFieldSettingSpecifier;

/**
 * A price map.
 *
 * @author matt
 * @version 2.0
 */
public class PriceMap {

	/**
	 * The property format used in {@link #toDetailedInfoString(MessageSource)}.
	 */
	public static final String STANDARD_DETAILED_INFO_FORMAT = "%-25s : %.3f %s";

	/**
	 * The code prefix used in {@link #toDetailedInfoString(MessageSource)}.
	 */
	public static final String STANDARD_DETAILED_INFO_CODE_PREFIX = "priceMap";

	private PowerComponents powerComponents;
	private Duration duration;
	private DurationRange responseTime;
	private PriceComponents priceComponents;

	/**
	 * Constructor.
	 */
	public PriceMap() {
		super();
	}

	/**
	 * Constructor.
	 *
	 * @param power
	 *        the power components
	 * @param duration
	 *        the duration
	 * @param responseTime
	 *        the response time
	 * @param price
	 *        the price components
	 */
	public PriceMap(PowerComponents power, Duration duration, DurationRange responseTime,
			PriceComponents price) {
		super();
		setPowerComponents(power);
		setDuration(duration);
		setResponseTime(responseTime);
		setPriceComponents(price);
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
		PowerComponents.addSettings(prefix + "powerComponents.", results);
		results.add(new BasicTextFieldSettingSpecifier(prefix + "durationMillis", ""));
		DurationRange.addSettings(prefix + "responseTime.", results);
		PriceComponents.addSettings(prefix + "priceComponents.", results);
	}

	/**
	 * Get the price map data as a Map.
	 *
	 * @return a map of the properties of this class
	 */
	public Map<String, Object> asMap() {
		Map<String, Object> map = new LinkedHashMap<>(8);
		map.put("powerComponents", powerComponents().asMap());
		map.put("durationMillis", getDurationMillis());
		map.put("responseTime", responseTime().asMap());
		map.put("priceComponents", priceComponents().asMap());
		return map;
	}

	/**
	 * Create a copy of this instance.
	 *
	 * @return the new copy
	 */
	public PriceMap copy() {
		PriceMap c = new PriceMap();

		PowerComponents power = getPowerComponents();
		if ( power != null ) {
			c.setPowerComponents(power.copy());
		}

		c.setDuration(getDuration());

		DurationRange rt = getResponseTime();
		if ( rt != null ) {
			c.setResponseTime(rt.copy());
		}

		PriceComponents price = getPriceComponents();
		if ( price != null ) {
			c.setPriceComponents(price.copy());
		}

		return c;
	}

	@Override
	public int hashCode() {
		return Objects.hash(duration, powerComponents, priceComponents, responseTime);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( !(obj instanceof PriceMap) ) {
			return false;
		}
		PriceMap other = (PriceMap) obj;
		return Objects.equals(duration, other.duration)
				&& Objects.equals(powerComponents, other.powerComponents)
				&& Objects.equals(priceComponents, other.priceComponents)
				&& Objects.equals(responseTime, other.responseTime);
	}

	@Override
	public String toString() {
		return "PriceMap{powerComponents=" + powerComponents + ", duration=" + duration
				+ ", responseTime=" + responseTime + ", priceComponents=" + priceComponents + "}";
	}

	/**
	 * Calculate the theoretical cost represented by this price map as the
	 * apparent power multiplied by the duration (in hours) multiplied by the
	 * apparent energy price.
	 *
	 * @return the apparent energy cost, in the configured currency units per
	 *         volt-amp-hours (VAh)
	 */
	public BigDecimal calculatedApparentEnergyCost() {
		BigDecimal vahPrice = priceComponents().apparentEnergyPrice();
		if ( vahPrice.equals(BigDecimal.ZERO) ) {
			return vahPrice;
		}
		PowerComponents p = powerComponents();
		double va = p.derivedApparentPower();
		double vah = va * durationHours();
		return vahPrice.multiply(new BigDecimal(String.valueOf(vah)));
	}

	/**
	 * Get the fractional hours represented by the configured duration.
	 *
	 * @return the duration, as fractional hours
	 */
	public double durationHours() {
		return duration().toMillis() / (1000.0 * 60.0 * 60.0);
	}

	/**
	 * Get a brief informational string out of the main aspects of this price
	 * map.
	 *
	 * @param locale
	 *        the locale
	 * @return the string
	 */
	public String toInfoString(Locale locale) {
		PowerComponents p = powerComponents();
		PriceComponents pr = priceComponents();
		double hours = durationHours();
		Currency c = pr.currency();
		BigDecimal cost = calculatedApparentEnergyCost();
		return String.format(locale, "%,.3f kVA ~ %,.3fh @ %s%,.2f/kVAh = %3$s%,.2f",
				p.derivedApparentPower() / 1000.0, hours, c.getSymbol(locale),
				pr.apparentEnergyPrice().movePointRight(3), cost);
	}

	/**
	 * Get the info string in the default locale.
	 *
	 * @return the info string
	 * @see #toInfoString(Locale)
	 */
	public String getInfo() {
		return toInfoString(Locale.getDefault());
	}

	/**
	 * Get a detailed informational string using the default locale and standard
	 * formatting.
	 *
	 * @param messageSource
	 *        the message source
	 * @return the detail string
	 * @see #toDetailedInfoString(Locale, MessageSource, String, String)
	 */
	public String toDetailedInfoString(MessageSource messageSource) {
		return toDetailedInfoString(Locale.getDefault(), messageSource, STANDARD_DETAILED_INFO_FORMAT,
				STANDARD_DETAILED_INFO_CODE_PREFIX);
	}

	/**
	 * Get a detailed informational string.
	 *
	 * <p>
	 * The resulting string will contain one line per property in this price
	 * map. Each property will be printed using a key/unit/value format, using
	 * {@code messageFormat} as the string format. The key unit parameters will
	 * be strings. The value will a number.
	 * </p>
	 *
	 * @param locale
	 *        the locale to render messages with
	 * @param messageSource
	 *        the message source
	 * @param messageFormat
	 *        the detailed message property format
	 * @param codePrefix
	 *        a message source code prefix
	 * @return the string
	 */
	public String toDetailedInfoString(Locale locale, MessageSource messageSource, String messageFormat,
			String codePrefix) {
		// use PrintWriter for proper line.separator support
		try (StringWriter buf = new StringWriter(); PrintWriter out = new PrintWriter(buf)) {

			PowerComponents p = powerComponents();
			out.println(format(messageFormat,
					messageSource.getMessage(codePrefix + ".power.real", null, locale),
					scaled(p.getRealPower(), -3), "kW"));
			out.println(format(messageFormat,
					messageSource.getMessage(codePrefix + ".power.reactive", null, locale),
					scaled(p.getReactivePower(), -3), "kVAR"));
			out.println(format(messageFormat,
					messageSource.getMessage(codePrefix + ".duration", null, locale),
					scaled(duration().toMillis(), -3), "s"));
			out.println(format(messageFormat,
					messageSource.getMessage(codePrefix + ".responseTime.min", null, locale),
					scaled(responseTime().min().toMillis(), -3), "s"));
			out.println(String.format(messageFormat,
					messageSource.getMessage(codePrefix + ".responseTime.max", null, locale),
					scaled(responseTime().max().toMillis(), -3), "s"));

			PriceComponents pr = priceComponents();
			out.print(String.format(messageFormat,
					messageSource.getMessage(codePrefix + ".price.apparent", null, locale),
					scaled(pr.apparentEnergyPrice(), 3), pr.currency().getCurrencyCode() + "/kVAh"));
			// note last line *no* println() because shell.print() does that
			out.flush();
			return buf.toString();
		} catch ( IOException e ) {
			throw new RuntimeException("Error rendering price map: " + e.getMessage(), e);
		}
	}

	/**
	 * Get the power components.
	 *
	 * @return the power components
	 */
	public PowerComponents getPowerComponents() {
		return powerComponents;
	}

	/**
	 * Set the power components.
	 *
	 * @param powerComponents
	 *        the power components to set
	 */
	public void setPowerComponents(PowerComponents powerComponents) {
		this.powerComponents = powerComponents;
	}

	/**
	 * Get the power component details, creating a new one if it doesn't already
	 * exist.
	 *
	 * <p>
	 * If a new power component is created, its values will be initialized to
	 * zero.
	 * </p>
	 *
	 * @return the power component details
	 */
	public PowerComponents powerComponents() {
		PowerComponents e = getPowerComponents();
		if ( e == null ) {
			e = new PowerComponents(0L, 0L);
			setPowerComponents(e);
		}
		return e;
	}

	/**
	 * Get the duration of time for this price map.
	 *
	 * @return the duration
	 */
	public Duration getDuration() {
		return duration;
	}

	/**
	 * Set the duration of time for this price map.
	 *
	 * @param duration
	 *        the duration to set
	 */
	public void setDuration(Duration duration) {
		this.duration = duration;
	}

	/**
	 * Get the duration, never {@literal null}.
	 *
	 * @return the duration
	 */
	public Duration duration() {
		Duration d = getDuration();
		if ( d == null ) {
			d = Duration.ZERO;
		}
		return d;
	}

	/**
	 * Get the duration, in milliseconds
	 *
	 * @return the duration, in milliseconds
	 */
	public long getDurationMillis() {
		Duration d = getDuration();
		return (d != null ? d.toMillis() : 0);
	}

	/**
	 * Set the duration, in milliseconds.
	 *
	 * @param dur
	 *        the duration to set, in milliseconds
	 */
	public void setDurationMillis(long dur) {
		setDuration(Duration.ofMillis(dur));
	}

	/**
	 * Get the response time range.
	 *
	 * @return the response time range
	 */
	public DurationRange getResponseTime() {
		return responseTime;
	}

	/**
	 * Set the response time range.
	 *
	 * @param responseTime
	 *        the response time range to set
	 */
	public void setResponseTime(DurationRange responseTime) {
		this.responseTime = responseTime;
	}

	/**
	 * Get the response time details, creating a new one if it doesn't already
	 * exist.
	 *
	 * @return the response time details
	 */
	public DurationRange responseTime() {
		DurationRange e = getResponseTime();
		if ( e == null ) {
			e = new DurationRange();
			setResponseTime(e);
		}
		return e;
	}

	/**
	 * Get the price components.
	 *
	 * @return the price components
	 */
	public PriceComponents getPriceComponents() {
		return priceComponents;
	}

	/**
	 * Set the price components.
	 *
	 * @param priceComponents
	 *        the price components to set
	 */
	public void setPriceComponents(PriceComponents priceComponents) {
		this.priceComponents = priceComponents;
	}

	/**
	 * Get the price component details, creating a new one if it doesn't already
	 * exist.
	 *
	 * <p>
	 * If a new instance is created, it will be initialized with the currency
	 * for the default locale and zero-values for all prices.
	 * </p>
	 *
	 * @return the price component details
	 */
	public PriceComponents priceComponents() {
		PriceComponents e = getPriceComponents();
		if ( e == null ) {
			e = new PriceComponents(Currency.getInstance(Locale.getDefault()), BigDecimal.ZERO);
			setPriceComponents(e);
		}
		return e;
	}
}
