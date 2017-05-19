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
import java.math.MathContext;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import net.solarnetwork.node.domain.GeneralAtmosphericDatum;

/**
 * Extension of {@link GeneralAtmosphericDatum} to support Yr data.
 * 
 * <p>
 * TODO
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class YrAtmosphericDatum extends GeneralAtmosphericDatum {

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
	private final DateFormat dateParseFormat;
	private final DateFormat dateFormat;

	public YrAtmosphericDatum(YrLocation location, DateFormat dateParseFormat, DateFormat dateFormat) {
		super();
		this.location = location;
		this.dateParseFormat = dateParseFormat;
		this.dateFormat = dateFormat;
		setSamples(newSamplesInstance());
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
	 * Set the created date via a string.
	 * 
	 * <p>
	 * The configured {@code dateParseFormat} will be used to parse the date
	 * string.
	 * </p>
	 * 
	 * @param ts
	 *        the date string to parse
	 * @throws ParseException
	 *         if a parsing error occurs
	 */
	public void setCreatedTimestamp(String ts) throws ParseException {
		setCreated(dateParseFormat.parse(ts));
	}

	/**
	 * Set the created date via a string.
	 * 
	 * <p>
	 * The configured {@code dateParseFormat} will be used to parse the date
	 * string. Then the configured {@code dateFormat} will be used to format the
	 * date onto the {@link #VALID_TO_KEY} setting key.
	 * </p>
	 * 
	 * @param ts
	 *        the date string to parse
	 * @throws ParseException
	 *         if a parsing error occurs
	 */
	public void setValidToTimestamp(String ts) throws ParseException {
		Date date = dateParseFormat.parse(ts);
		getSamples().putStatusSampleValue(VALID_TO_KEY, dateFormat.format(date));
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
		setWindDirection(value.round(MathContext.DECIMAL32).intValue());
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
		setRain(value.round(MathContext.DECIMAL32).intValue());
	}

	/**
	 * Set the atmospheric pressure as hectopascals.
	 * 
	 * @param value
	 *        the pressure, in hectopascals
	 */
	public void setAtmosphericPressureHectopascal(BigDecimal value) {
		setAtmosphericPressure(
				value.multiply(new BigDecimal(100)).round(MathContext.DECIMAL32).intValue());
	}

}
