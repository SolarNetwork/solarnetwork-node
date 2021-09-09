/* ==================================================================
 * YrAtmosphericDatum.java - 19/05/2017 4:34:26 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.weather.yr;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.node.domain.datum.SimpleAtmosphericDatum;

/**
 * Extension of {@link GeneralAtmosphericDatum} to support Yr data.
 * 
 * @author matt
 * @version 2.0
 */
public class YrAtmosphericDatum extends SimpleAtmosphericDatum {

	private static final long serialVersionUID = 2265538882543293937L;

	/**
	 * A {@link net.solarnetwork.domain.GeneralDatumSamples} status sample key
	 * for the date the samples are valid to.
	 */
	public static final String VALID_TO_KEY = "validTo";

	/**
	 * A {@link net.solarnetwork.domain.GeneralDatumSamples} status sample key
	 * for the Yr sky conditions symbol.
	 */
	public static final String SYMBOL_VAR_KEY = "symbolVar";

	private final YrLocation location;
	private final DateTimeFormatter dateParseFormatter;
	private Instant fromTimestamp;

	public YrAtmosphericDatum(YrLocation location, DateTimeFormatter dateParseFormatter) {
		super(null, null, new DatumSamples());
		this.location = location;
		this.dateParseFormatter = dateParseFormatter;
	}

	private YrAtmosphericDatum(YrAtmosphericDatum other) {
		super(other.getSourceId(), other.fromTimestamp, other.getSamples());
		this.location = other.location;
		this.dateParseFormatter = null;
	}

	/**
	 * Get the location.
	 * 
	 * @return the location
	 */
	public YrLocation getLocation() {
		return location;
	}

	/**
	 * Get the "from" timestamp.
	 * 
	 * @return the timestamp
	 */
	public Instant getFromTimestamp() {
		return fromTimestamp;
	}

	/**
	 * Create a copy using the {@code fromTimestamp} as the datum timestamp.
	 * 
	 * @return the new datum
	 */
	public YrAtmosphericDatum copyUsingFromTimestamp() {
		return new YrAtmosphericDatum(this);
	}

	/**
	 * Set the created date via a string.
	 * 
	 * <p>
	 * The configured {@code dateParseFormat} will be used to parse the date
	 * string.
	 * </p>
	 * 
	 * @param ts
	 *        the date string to parse
	 * @throws DateTimeParseException
	 *         if a parsing error occurs
	 */
	public void setCreatedTimestamp(String ts) throws DateTimeParseException {
		this.fromTimestamp = dateParseFormatter.parse(ts, Instant::from);
	}

	/**
	 * Set the valid to date via a string.
	 * 
	 * <p>
	 * The configured {@code dateParseFormat} will be used to parse the date
	 * string. Then the configured {@code dateFormat} will be used to format the
	 * date onto the {@link #VALID_TO_KEY} setting key.
	 * </p>
	 * 
	 * @param ts
	 *        the date string to parse
	 * @throws DateTimeParseException
	 *         if a parsing error occurs
	 */
	public void setValidToTimestamp(String ts) throws DateTimeParseException {
		Instant date = dateParseFormatter.parse(ts, Instant::from);
		getSamples().putStatusSampleValue(VALID_TO_KEY, DateTimeFormatter.ISO_INSTANT.format(date));
	}

	/**
	 * Set the symbol status variable value.
	 * 
	 * <p>
	 * This sets the {@link #SYMBOL_VAR_KEY} setting key.
	 * </p>
	 * 
	 * @param value
	 *        the value to use
	 */
	public void setSymbolVariable(String value) {
		getSamples().putStatusSampleValue(SYMBOL_VAR_KEY, value);
	}

	/**
	 * Set the wind direction as a decimal value.
	 * 
	 * <p>
	 * This will round the decimal to an integer and call
	 * {@link #setWindDirection(Integer)}.
	 * </p>
	 * 
	 * @param value
	 *        the wind direction to set
	 */
	public void setWindDirectionDecimal(BigDecimal value) {
		setWindDirection(value.setScale(0, RoundingMode.HALF_UP).intValue());
	}

	/**
	 * Set the rain as a decimal value.
	 * 
	 * <p>
	 * This will round the decimal to an integer and call
	 * {@link #setRain(Integer)}.
	 * </p>
	 * 
	 * @param value
	 *        the rain to set
	 */
	public void setRainDecimal(BigDecimal value) {
		setRain(value.setScale(0, RoundingMode.HALF_UP).intValue());
	}

	/**
	 * Set the atmospheric pressure as hectopascals.
	 * 
	 * @param value
	 *        the pressure, in hectopascals
	 */
	public void setAtmosphericPressureHectopascal(BigDecimal value) {
		setAtmosphericPressure(
				value.multiply(new BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).intValue());
	}

}
