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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.measure.IncommensurableException;
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
import net.solarnetwork.service.OptionalServiceCollection;
import net.solarnetwork.util.NumberUtils;

/**
 * Helper for dealing with KCD units of measurement, using the
 * {@code javax.measure} API.
 *
 * @author matt
 * @version 2.0
 */
public class MeasurementHelper {

	/** The properties file with the standard unit mapping data. */
	public static final String STANDARD_UNIT_MAPPING_RESOURCE = "MeasurementHelper-standard-units.properties";

	private final ConcurrentMap<String, Unit<?>> unitCache = new ConcurrentHashMap<>(16, 0.9f, 1);
	private final Map<Integer, Set<Unit<?>>> standardUnits;
	private final Map<String, Unit<?>> altUnits;
	private final OptionalServiceCollection<MeasurementServiceProvider> measurementProviders;

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 *
	 * @param measurementProviders
	 *        the measurement service providers to use
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

		Map<Integer, Set<Unit<?>>> std = new LinkedHashMap<>(8);
		Map<String, Unit<?>> alts = new LinkedHashMap<>(8);
		Properties props = new Properties();
		try (Reader in = new InputStreamReader(MeasurementHelper.class.getResourceAsStream(
				"MeasurementHelper-standard-units.properties"), Charset.forName("UTF-8"))) {
			props.load(in);
			for ( Map.Entry<Object, Object> me : props.entrySet() ) {
				String k = me.getKey().toString();
				if ( k.startsWith("std.") ) {
					int split = k.indexOf('.', 4);
					if ( split < 1 ) {
						continue;
					}
					try {
						Integer count = Integer.valueOf(k.substring(4, split));
						Unit<?> unit = unitValueInternal(me.getValue().toString());
						if ( unit != null ) {
							std.computeIfAbsent(count, LinkedHashSet::new).add(unit);
						} else {
							log.warn("Unit not found for standard unit mapping [{}] value [{}]", k,
									me.getValue());
						}
					} catch ( NumberFormatException e ) {
						log.warn("Error parsing standard unit base count from key [{}]", k);
					}
				} else if ( k.startsWith("alt.") ) {
					String unitString = k.substring(4);
					Unit<?> parentUnit = unitValueInternal(me.getValue().toString());
					Unit<?> parentSystemUnit = parentUnit.getSystemUnit();
					Unit<?> unit = null;
					if ( parentUnit == parentSystemUnit ) {
						unit = parentUnit.alternate(unitString);
						alts.putIfAbsent(unitString, unit);
					}
				}
			}
		} catch ( IOException e ) {
			log.warn("Error loading standard units from resource {}: {}", STANDARD_UNIT_MAPPING_RESOURCE,
					e.toString());
		}
		this.standardUnits = std;
		this.altUnits = alts;
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
		if ( altUnits.containsKey(unitString) ) {
			return altUnits.get(unitString);
		}
		return unitCache.computeIfAbsent(unitString, u -> {
			return unitValueInternal(unitString);
		});
	}

	private Unit<?> unitValueInternal(String unitString) {
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

	/**
	 * Get a quantity instance.
	 *
	 * @param n
	 *        the number
	 * @param unitString
	 *        the unit
	 * @param slope
	 *        the slope
	 * @param intercept
	 *        the intercept
	 * @return the quantity, or {@literal null} if the unit is not available or
	 *         {@code n} is {@literal null}
	 */
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

	private <Q extends Quantity<Q>> Quantity<Q> quantityValue(final Number amount, final Unit<Q> unit) {
		if ( amount == null || unit == null ) {
			return null;
		}
		for ( MeasurementServiceProvider measurementProvider : measurementProviders.services() ) {
			Quantity<Q> q = measurementProvider.quantityForUnit(amount, unit);
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
	 * @param <Q>
	 *        the quantity type
	 * @param unit
	 *        the unit to get a normalized variant of
	 * @return the normalized unit
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public <Q extends Quantity<Q>> Unit<Q> normalizedUnit(final Unit<Q> unit) {
		if ( unit == null ) {
			return null;
		}

		Map<? extends Unit<?>, Integer> baseUnits = unit.getBaseUnits();
		if ( baseUnits == null ) {
			Unit<Q> sysUnit = unit.getSystemUnit();
			if ( sysUnit.getConverterTo(unit).isIdentity() ) {
				// already a base unit
				return unit;
			}
		}
		Integer baseUnitSize = (baseUnits != null ? baseUnits.size() : 0);
		if ( standardUnits.containsKey(baseUnitSize) ) {
			for ( Unit<?> stdUnit : standardUnits.get(baseUnitSize) )
				// look for some SolarNetwork standard units
				if ( stdUnit.isCompatible(unit) ) {
					return (Unit) stdUnit;
				}
		}

		return unit.getSystemUnit();
	}

	/**
	 * Get a "normalized" quantity from an arbitrary quantity.
	 *
	 * @param <Q>
	 *        the type of quantity
	 * @param quantity
	 *        the quantity to get an equivalent "normalized" value for
	 * @return the normalized value, or {@literal null} if {@code quantity} is
	 *         {@literal null}
	 */
	public <Q extends Quantity<Q>> Quantity<Q> normalizedQuantity(final Quantity<Q> quantity) {
		if ( quantity == null ) {
			return null;
		}
		Unit<Q> normalizedUnit = normalizedUnit(quantity.getUnit());
		if ( normalizedUnit == null ) {
			return quantity;
		}
		return convertedQuantity(quantity, normalizedUnit);
	}

	/**
	 * Convert a quantity into another (compatible) unit.
	 *
	 * @param <Q>
	 *        the type of quantity
	 * @param src
	 *        the quantity to convert
	 * @param dest
	 *        the desired output unit
	 * @return the converted quantity
	 */
	public <Q extends Quantity<Q>> Quantity<Q> convertedQuantity(final Quantity<Q> src,
			final Unit<Q> dest) {
		if ( src == null || dest == null ) {
			return src;
		}
		try {
			UnitConverter converter = src.getUnit().getConverterToAny(dest);
			if ( converter.isIdentity() ) {
				return src;
			}
			Number convertedAmount = converter.convert(src.getValue());
			return quantityValue(convertedAmount, dest);
		} catch ( UnconvertibleException | IncommensurableException e ) {
			log.debug("Unable to convert {} to {}: {}", src.getUnit(), dest, e.toString());
			return src;
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
		String symbol = unit.getSymbol();
		if ( symbol != null ) {
			return symbol;
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
			} catch ( Exception e ) {
				log.trace("Error parsing unit [{}]: {}", unitString, e.toString());
			}
		}
		return null;
	}

}
