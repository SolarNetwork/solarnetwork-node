/* ==================================================================
 * PlaceholderService.java - 25/08/2020 10:38:55 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.node.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.util.StringUtils;

/**
 * API for a service that can resolve "placeholder" variables in strings.
 * 
 * @author matt
 * @version 2.1
 * @since 1.76
 */
public interface PlaceholderService {

	/**
	 * Resolve all placeholders.
	 * 
	 * @param s
	 *        the string to resolve placeholders in
	 * @param parameters
	 *        parameters to use while resolving placeholders, or {@literal null}
	 * @return the resolved string, or {@literal null} if {@code s} is
	 *         {@literal null}
	 */
	String resolvePlaceholders(String s, Map<String, ?> parameters);

	/**
	 * Register a set of parameters for future use.
	 * 
	 * @param parameters
	 *        the parameters to register
	 */
	void registerParameters(Map<String, ?> parameters);

	/**
	 * Copy all placeholders to a map.
	 * 
	 * @param <T>
	 *        the parameters value type; typically {@code String} or
	 *        {@code Object}
	 * @param destination
	 *        the map to copy all placeholders to
	 * @since 2.1
	 */
	default <T> void copyPlaceholders(Map<String, T> destination) {
		copyPlaceholders(destination, (Predicate<Entry<String, T>>) null);
	}

	/**
	 * Copy placeholders to a map.
	 * 
	 * @param <T>
	 *        the parameters value type; typically {@code String} or
	 *        {@code Object}
	 * @param destination
	 *        the map to copy all placeholders to
	 * @param filter
	 *        an optional filter to restrict copying matching placeholders into
	 *        {@code destination}
	 * @since 2.1
	 */
	<T> void copyPlaceholders(Map<String, T> destination, Predicate<Entry<String, T>> filter);

	/**
	 * Copy placeholders to a map.
	 * 
	 * @param <T>
	 *        the parameters value type; typically {@code String} or
	 *        {@code Object}
	 * @param destination
	 *        the map to copy all placeholders to
	 * @param keyFilter
	 *        an optional pattern to match against placeholder keys to restrict
	 *        copying matches into {@code destination}; the
	 *        {@link java.util.regex.Matcher#find()} method is used so the
	 *        pattern matches anywhere within the key values
	 * @since 2.1
	 */
	default <T> void copyPlaceholders(Map<String, T> destination, Pattern keyFilter) {
		Predicate<Entry<String, T>> filter = null;
		if ( keyFilter != null ) {
			filter = (e) -> {
				return keyFilter.matcher(e.getKey()).find();
			};
		}
		copyPlaceholders(destination, filter);
	}

	/**
	 * Copy all placeholders to a destination map, converting obvious number
	 * string values into actual number instances.
	 * 
	 * @param destination
	 *        the map to copy all placeholders to
	 * @since 2.1
	 * @see #smartCopyPlaceholders(Map, Predicate)
	 */
	default void smartCopyPlaceholders(Map<String, Object> destination) {
		smartCopyPlaceholders(destination, (Predicate<Entry<String, ?>>) null);
	}

	/**
	 * Copy placeholders to a destination map, converting obvious number string
	 * values into actual number instances.
	 * 
	 * @param destination
	 *        the map to copy all placeholders to
	 * @param keyFilter
	 *        an optional pattern to match against placeholder keys to restrict
	 *        copying matches into {@code destination}; the
	 *        {@link java.util.regex.Matcher#find()} method is used so the
	 *        pattern matches anywhere within the key values
	 * @since 2.1
	 * @see #smartCopyPlaceholders(Map, Predicate)
	 */
	default void smartCopyPlaceholders(Map<String, Object> destination, Pattern keyFilter) {
		Predicate<Entry<String, ?>> filter = null;
		if ( keyFilter != null ) {
			filter = (e) -> {
				return keyFilter.matcher(e.getKey()).find();
			};
		}
		smartCopyPlaceholders(destination, filter);
	}

	/**
	 * Copy placeholders to a destination map, converting obvious number string
	 * values into actual number instances.
	 * 
	 * <p>
	 * Each placeholder value is examined:
	 * </p>
	 * 
	 * <ol>
	 * <li>if it is a {@link Number} instance it is copied as-is</li>
	 * <li>if its string value looks like a valid integer or decimal number, it
	 * is copied as a new {@link BigInteger} or {@link BigDecimal} instance</li>
	 * </ol>
	 * 
	 * @param destination
	 *        the map to copy all placeholders to
	 * @param filter
	 *        an optional filter to restrict copying matching placeholders into
	 *        {@code destination}
	 * @since 2.1
	 */
	default void smartCopyPlaceholders(Map<String, Object> destination,
			Predicate<Entry<String, ?>> filter) {
		mapPlaceholders(destination, s -> {
			if ( filter != null ) {
				s = s.filter(filter);
			}
			return s.map(entry -> {
				Object v = entry.getValue();
				if ( v != null && !(v instanceof Number) ) {
					Number n = StringUtils.numberValue(v.toString());
					if ( n != null ) {
						v = n;
					}
				}
				return new SimpleEntry<>(entry.getKey(), v);
			});
		});
	}

	/**
	 * Copy placeholders to a map using a stream filter.
	 * 
	 * <p>
	 * This method allows you the most flexibility in copying placeholders to a
	 * map. The {@code filter} is a function that is passed a stream of
	 * placeholder entries and can then filter and/or map that stream to a new
	 * stream, whose output entries will be copied to {@code destination}. For
	 * example, you could copy specific placeholders converted to integer
	 * values:
	 * </p>
	 * 
	 * <pre>
	 * <code>
	 * mapPlaceholders(params, s -&gt; {
	 *     return s.filter(e -&gt; {
	 *         return e.getKey().startsWith("max");
	 *     }).map(e -&gt; {
	 *         return new SimpleEntry&lt;&gt;(e.getKey(), Integer.valueOf(e.getValue().toString()));
	 *     });
	 * });
	 * </code>
	 * </pre>
	 * 
	 * @param <T>
	 *        the parameters value type; typically {@code String} or
	 *        {@code Object}
	 * @param destination
	 *        the map to copy all placeholders to
	 * @param filter
	 *        an optional filter to restrict copying matching placeholders into
	 *        {@code destination}; the function takes a stream of placeholder
	 *        entries as input and returns a stream of desired output entries to
	 *        add to {@code destination}
	 */
	<T> void mapPlaceholders(Map<String, T> destination,
			Function<Stream<Entry<String, ?>>, Stream<Entry<String, T>>> filter);

	/**
	 * Helper to resolve placeholders from an optional
	 * {@link PlaceholderService}.
	 * 
	 * @param service
	 *        the optional service
	 * @param s
	 *        the string to resolve placeholders
	 * @param parameters
	 *        to use while resolving placeholders, or {@literal null}
	 * @return the resolved string, or {@literal null} if {@code s} is
	 *         {@literal null}
	 */
	static String resolvePlaceholders(OptionalService<PlaceholderService> service, String s,
			Map<String, ?> parameters) {
		PlaceholderService ps = OptionalService.service(service);
		return (ps != null ? ps.resolvePlaceholders(s, parameters) : s);
	}

	/**
	 * Helper to copy plaeholders to a map.
	 * 
	 * @param <T>
	 *        the parameters value type; typically {@code String} or
	 *        {@code Object}
	 * @param service
	 *        the optional service
	 * @param destination
	 *        the map to copy all placeholders to
	 * @since 2.1
	 */
	static <T> void copyPlaceholders(OptionalService<PlaceholderService> service,
			Map<String, T> destination) {
		copyPlaceholders(service, destination, (Predicate<Entry<String, T>>) null);
	}

	/**
	 * Helper to copy plaeholders to a map.
	 * 
	 * @param <T>
	 *        the parameters value type; typically {@code String} or
	 *        {@code Object}
	 * @param service
	 *        the optional service
	 * @param destination
	 *        the map to copy all placeholders to
	 * @param filter
	 *        an optional filter to restrict copying matching placeholders into
	 *        {@code destination}
	 * @since 2.1
	 */
	static <T> void copyPlaceholders(OptionalService<PlaceholderService> service,
			Map<String, T> destination, Predicate<Entry<String, T>> filter) {
		if ( destination == null ) {
			return;
		}
		PlaceholderService ps = OptionalService.service(service);
		if ( ps != null ) {
			ps.copyPlaceholders(destination, filter);
		}
	}

	/**
	 * Helper to copy plaeholders to a map.
	 * 
	 * @param <T>
	 *        the parameters value type; typically {@code String} or
	 *        {@code Object}
	 * @param service
	 *        the optional service
	 * @param destination
	 *        the map to copy all placeholders to
	 * @param keyFilter
	 *        an optional pattern to match against placeholder keys to restrict
	 *        copying matches into {@code destination}; the
	 *        {@link java.util.regex.Matcher#find()} method is used so the
	 *        pattern matches anywhere within the key values
	 * @since 2.1
	 */
	static <T> void copyPlaceholders(OptionalService<PlaceholderService> service,
			Map<String, T> destination, Pattern keyFilter) {
		if ( destination == null ) {
			return;
		}
		PlaceholderService ps = OptionalService.service(service);
		if ( ps != null ) {
			ps.copyPlaceholders(destination, keyFilter);
		}
	}

	/**
	 * Helper to copy plaeholders to a map.
	 * 
	 * @param <T>
	 *        the parameters value type; typically {@code String} or
	 *        {@code Object}
	 * @param service
	 *        the optional service
	 * @param destination
	 *        the map to copy all placeholders to
	 * @param filter
	 *        an optional filter to restrict copying matching placeholders into
	 *        {@code destination}; the function takes a stream of placeholder
	 *        entries as input and returns a stream of desired output entries to
	 *        add to {@code destination}
	 * @since 2.1
	 */
	static <T> void mapPlaceholders(OptionalService<PlaceholderService> service,
			Map<String, T> destination,
			Function<Stream<Entry<String, ?>>, Stream<Entry<String, T>>> filter) {
		if ( destination == null ) {
			return;
		}
		PlaceholderService ps = OptionalService.service(service);
		if ( ps != null ) {
			ps.mapPlaceholders(destination, filter);
		}
	}

	/**
	 * Copy placeholders to a destination map, converting obvious number string
	 * values into actual number instances.
	 * 
	 * @param service
	 *        the optional service
	 * @param destination
	 *        the map to copy all placeholders to
	 * @since 2.1
	 * @see PlaceholderService#smartCopyPlaceholders(Map, Pattern)
	 */
	static void smartCopyPlaceholders(OptionalService<PlaceholderService> service,
			Map<String, Object> destination) {
		if ( destination == null ) {
			return;
		}
		PlaceholderService ps = OptionalService.service(service);
		if ( ps != null ) {
			ps.smartCopyPlaceholders(destination);
		}
	}

	/**
	 * Copy placeholders to a destination map, converting obvious number string
	 * values into actual number instances.
	 * 
	 * @param service
	 *        the optional service
	 * @param destination
	 *        the map to copy all placeholders to
	 * @param keyFilter
	 *        an optional pattern to match against placeholder keys to restrict
	 *        copying matches into {@code destination}; the
	 *        {@link java.util.regex.Matcher#find()} method is used so the
	 *        pattern matches anywhere within the key values
	 * @since 2.1
	 * @see PlaceholderService#smartCopyPlaceholders(Map, Pattern)
	 */
	static void smartCopyPlaceholders(OptionalService<PlaceholderService> service,
			Map<String, Object> destination, Pattern keyFilter) {
		if ( destination == null ) {
			return;
		}
		PlaceholderService ps = OptionalService.service(service);
		if ( ps != null ) {
			ps.smartCopyPlaceholders(destination, keyFilter);
		}
	}

	/**
	 * Copy placeholders to a destination map, converting obvious number string
	 * values into actual number instances.
	 * 
	 * <p>
	 * Each placeholder value is examined:
	 * </p>
	 * 
	 * <ol>
	 * <li>if it is a {@link Number} instance it is copied as-is</li>
	 * <li>if its string value looks like a valid integer or decimal number, it
	 * is copied as a new {@link BigInteger} or {@link BigDecimal} instance</li>
	 * </ol>
	 * 
	 * @param service
	 *        the optional service
	 * @param destination
	 *        the map to copy all placeholders to
	 * @param filter
	 *        an optional filter to restrict copying matching placeholders into
	 *        {@code destination}
	 * @since 2.1
	 * @see PlaceholderService#smartCopyPlaceholders(Map, Predicate)
	 */
	static void smartCopyPlaceholders(OptionalService<PlaceholderService> service,
			Map<String, Object> destination, Predicate<Entry<String, ?>> filter) {
		if ( destination == null ) {
			return;
		}
		PlaceholderService ps = OptionalService.service(service);
		if ( ps != null ) {
			ps.smartCopyPlaceholders(destination, filter);
		}
	}

}
