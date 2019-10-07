/* ==================================================================
 * MeasurementHelper.java - 15/09/2019 9:53:05 am
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

package net.solarnetwork.node.io.canbus.support;

import java.math.BigDecimal;
import java.util.Set;
import javax.measure.IncommensurableException;
import javax.measure.MeasurementException;
import javax.measure.Quantity;
import javax.measure.UnconvertibleException;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.format.UnitFormat;
import javax.measure.spi.FormatService;
import javax.measure.spi.FormatService.FormatType;
import javax.measure.spi.UnitFormatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.javax.measure.MeasurementServiceProvider;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.util.OptionalServiceCollection;

/**
 * Helper for dealing with KCD units of measurement, using the
 * {@code javax.measure} API.
 * 
 * @author matt
 * @version 1.0
 */
public class MeasurementHelper {

	private final OptionalServiceCollection<MeasurementServiceProvider> measurementProviders;

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 * 
	 * @param measurementProvider
	 *        the measurement service provider to use
	 * @throws IllegalArgumentException
	 *         if {@code measurementProvider} is {@literal null}
	 */
	public MeasurementHelper(
			OptionalServiceCollection<MeasurementServiceProvider> measurementProviders) {
		super();
		if ( measurementProviders == null ) {
			throw new IllegalArgumentException("Measurement provider collection must be provided.");
		}
		this.measurementProviders = measurementProviders;
	}

	/**
	 * Get a unit for a unit string value.
	 * 
	 * <p>
	 * This method will attempt to parse the unit string using all available
	 * measurement providers, returning the first successfully parsed unit.
	 * </p>
	 * 
	 * @param unitString
	 *        the unit string value
	 * @return the unit, or {@literal null} if {@code unitString} is
	 *         {@literal null} or the unit cannot be determined
	 */
	public Unit<?> unitValue(String unitString) {
		if ( unitString == null || unitString.isEmpty() ) {
			return null;
		}
		for ( MeasurementServiceProvider measurementProvider : measurementProviders.services() ) {
			// try the UnitFormatService first, for cases when this returns a different implementation from FormatService
			UnitFormatService ufs = measurementProvider.getUnitFormatService();
			if ( ufs != null ) {
				Unit<?> unit = unitValueFromUnitFormatService(unitString, ufs);
				if ( unit != null ) {
					return unit;
				}
			}

			FormatService fs = measurementProvider.getFormatService();
			if ( fs != null && fs != ufs ) {
				Unit<?> unit = unitValueFromFormatService(unitString, fs);
				if ( unit != null ) {
					return unit;
				}
			}
		}
		log.debug("Unit not found for unit [{}]", unitString);
		return null;
	}

	public Quantity<?> quantityValue(Number n, final String unitString, final Number slope,
			final Number intercept) {
		Unit<?> unit = unitValue(unitString);
		if ( unit == null ) {
			return null;
		}

		if ( slope != null || intercept != null ) {
			BigDecimal d = NumberUtils.bigDecimalForNumber(n);
			if ( slope != null ) {
				d = d.multiply(NumberUtils.bigDecimalForNumber(slope));
			}
			if ( intercept != null ) {
				d = d.add(NumberUtils.bigDecimalForNumber(intercept));
			}
			n = d;
		}

		return quantityValue(n, unit);
	}

	private Quantity<?> quantityValue(final Number amount, final Unit<?> unit) {
		if ( amount == null || unit == null ) {
			return null;
		}
		for ( MeasurementServiceProvider measurementProvider : measurementProviders.services() ) {
			Quantity<?> q = measurementProvider.quantityForUnit(amount, unit);
			if ( q != null ) {
				return q;
			}
		}
		log.debug("Quantity not found for unit [{}]", unit);
		return null;
	}

	/**
	 * Get a "normalized" unit from an arbitrary unit.
	 * 
	 * @param unit
	 *        the unit to get a normalized variant of
	 * @return the normalized unit
	 */
	public Unit<?> normalizedUnit(final Unit<?> unit) {
		if ( unit == null ) {
			return null;
		}
		// TODO
		return unit;
	}

	/**
	 * Get a "normalized" quantity from an arbitrary quantity.
	 * 
	 * @param quantity
	 *        the quantity to get an equivalent "normalized" value for
	 * @return the normalized value, or {@literal null} if {@code quantity} is
	 *         {@literal null}
	 */
	public Quantity<?> normalizedQuantity(final Quantity<?> quantity) {
		if ( quantity == null ) {
			return null;
		}
		Unit<?> normalizedUnit = normalizedUnit(quantity.getUnit());
		if ( normalizedUnit == null ) {
			return quantity;
		}
		try {
			UnitConverter converter = quantity.getUnit().getConverterToAny(normalizedUnit);
			if ( converter.isIdentity() ) {
				return quantity;
			}
			Number convertedAmount = converter.convert(quantity.getValue());
			return quantityValue(convertedAmount, normalizedUnit);
		} catch ( UnconvertibleException | IncommensurableException e ) {
			log.debug("Unable to convert {} to {}: {}", quantity.getUnit(), normalizedUnit,
					e.toString());
			return quantity;
		}
	}

	/**
	 * Format a unit as a string value.
	 * 
	 * @param unit
	 *        the unit to format
	 * @return the formatted unit value, or {@literal null} if {@code unit} is
	 *         {@literal null}
	 */
	public String formatUnit(Unit<?> unit) {
		if ( unit == null ) {
			return null;
		}
		for ( MeasurementServiceProvider measurementProvider : measurementProviders.services() ) {
			UnitFormatService ufs = measurementProvider.getUnitFormatService();
			if ( ufs != null ) {
				UnitFormat uf = ufs.getUnitFormat();
				if ( uf != null ) {
					try {
						String result = uf.format(unit);
						if ( result != null ) {
							return result;
						}
					} catch ( Exception e ) {
						// ignore
					}
				}
			}
		}
		return unit.toString();
	}

	private Unit<?> unitValueFromUnitFormatService(String unitString, UnitFormatService formatService) {
		if ( formatService == null ) {
			return null;
		}
		@SuppressWarnings("deprecation")
		Set<String> names = formatService.getAvailableFormatNames();
		return unitValue(unitString, formatService, names);
	}

	private Unit<?> unitValueFromFormatService(String unitString, FormatService formatService) {
		if ( formatService == null ) {
			return null;
		}
		Set<String> names = formatService.getAvailableFormatNames(FormatType.UNIT_FORMAT);
		return unitValue(unitString, formatService, names);
	}

	private Unit<?> unitValue(String unitString, UnitFormatService formatService,
			Set<String> unitFormatNames) {
		for ( String formatName : unitFormatNames ) {
			UnitFormat fmt = formatService.getUnitFormat(formatName);
			try {
				Unit<?> unit = fmt.parse(unitString);
				if ( unit != null ) {
					return unit;
				}
			} catch ( MeasurementException | UnsupportedOperationException e ) {
				log.trace("Error parsing unit [{}]: {}", unitString, e.toString());
			}
		}
		return null;
	}

}
